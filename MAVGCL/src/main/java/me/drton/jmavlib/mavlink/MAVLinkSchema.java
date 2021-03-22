package me.drton.jmavlib.mavlink;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkSchema {
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private final Map<Integer, MAVLinkMessageDefinition> definitionsByID
        = new HashMap<Integer, MAVLinkMessageDefinition>();
    private final Map<String, MAVLinkMessageDefinition> definitionsByName
        = new HashMap<String, MAVLinkMessageDefinition>();
    private DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    public MAVLinkSchema(String xmlFileName) throws ParserConfigurationException, IOException,
        SAXException {
        processXMLFile(xmlFileName);
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    private void processXMLFile(String xmlFileName) throws IOException, SAXException,
        ParserConfigurationException {
        File xmlFile = new File(xmlFileName);
        xmlBuilder.reset();
        Document doc = xmlBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        if (!root.getNodeName().equals("mavlink")) {
            throw new RuntimeException("Root element is not <mavlink>");
        }

        // Process includes
        NodeList includeElems = root.getElementsByTagName("include");
        for (int i = 0; i < includeElems.getLength(); i++) {
            String includeFile = includeElems.item(i).getTextContent();
            processXMLFile(new File(xmlFile.getParentFile(), includeFile).getPath());
        }

        NodeList msgElems = ((Element) root.getElementsByTagName("messages").item(
                                 0)).getElementsByTagName("message");
        for (int i = 0; i < msgElems.getLength(); i++) {
            Element msg = (Element) msgElems.item(i);
            int msgID = Integer.parseInt(msg.getAttribute("id"));
            String msgName = msg.getAttribute("name");
            NodeList nodeList = msg.getChildNodes();
            List<MAVLinkField> fields = new ArrayList<MAVLinkField> ();
            int extensionIndex = -1;
            for (int j = 0; j < nodeList.getLength(); j++) {
                Node node = nodeList.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element fieldElem = (Element) node;
                    String name = fieldElem.getNodeName();
                    if (name == "field") {
                        String[] typeStr = fieldElem.getAttribute("type").split("\\[");
                        MAVLinkDataType fieldType = MAVLinkDataType.fromCType(typeStr[0]);
                        int arraySize = -1;
                        if (typeStr.length > 1) {
                            arraySize = Integer.parseInt(typeStr[1].split("\\]")[0]);
                        }
                        MAVLinkField field = new MAVLinkField(fieldType, arraySize, fieldElem.getAttribute("name"));
                        fields.add(field);
                    } else if (name == "extensions") {
                        // remember where the special <extensions/> tag is defined in the list of fields.
                        extensionIndex = fields.size();
                    }
                }
            }
            if (extensionIndex == -1) {
                extensionIndex = fields.size();
            }
            int numFields = fields.size();

            // as per mavparse.py: when we have extensions we only sort up to the first extended field
            // Note: sortedFields is a view, so we do in-place sorting.
            List<MAVLinkField> sortedFields = fields.subList(0, extensionIndex);
            Collections.sort(sortedFields, new Comparator<MAVLinkField>() {
                @Override
                public int compare(MAVLinkField field2, MAVLinkField field1) {
                    // Sort on type size
                    if (field1.type.size > field2.type.size) {
                        return 1;
                    } else if (field1.type.size < field2.type.size) {
                        return -1;
                    }
                    return 0;
                }
            });

            // 0 to uint24 max (2^24)
            if (msgID >= 0 && msgID < 16777215) {
                addMessageDefinition(new MAVLinkMessageDefinition(msgID, msgName,
                                                                  fields.toArray(new MAVLinkField[numFields]), extensionIndex));
            }
        }
    }

    public MAVLinkMessageDefinition getMessageDefinition(int msgID) {
        return definitionsByID.get(msgID);
    }

    public MAVLinkMessageDefinition getMessageDefinition(String msgName) {
        return definitionsByName.get(msgName);
    }

    public Map<String, MAVLinkMessageDefinition> getMessageDefinitions() {
        return definitionsByName;
    }

    public void addMessageDefinition(MAVLinkMessageDefinition definition) {
        definitionsByID.put(definition.id, definition);
        definitionsByName.put(definition.name, definition);
    }
}
