package org.fxyz3d.shapes.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fxyz3d.scene.paint.Patterns;
import org.fxyz3d.shapes.primitives.TexturedMesh;
import org.fxyz3d.shapes.primitives.helper.MarkerFactory;
import org.fxyz3d.shapes.primitives.helper.MeshHelper;
import org.fxyz3d.shapes.primitives.helper.TriangleMeshHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.helper.TextureMode;
import org.fxyz3d.scene.paint.Palette.ColorPalette;


public class MAVScatterMesh extends Group implements TextureMode {

	private final static List<Point3D> DEFAULT_SCATTER_DATA = new ArrayList<>();
	private final static double DEFAULT_HEIGHT = 0.1d;
	private final static int DEFAULT_LEVEL = 0;
	private final static boolean DEFAULT_JOIN_SEGMENTS = true;

	protected ObservableList<TexturedMesh> meshes=null;
	protected List<Point3D> added=new ArrayList<Point3D>();;

	public MAVScatterMesh(){
		this(DEFAULT_SCATTER_DATA,DEFAULT_JOIN_SEGMENTS,DEFAULT_HEIGHT,DEFAULT_LEVEL);
	}

	public MAVScatterMesh(Collection<Point3D> scatterData){
		this(scatterData,DEFAULT_JOIN_SEGMENTS,DEFAULT_HEIGHT,DEFAULT_LEVEL);
	}


	public MAVScatterMesh(Collection<Point3D> scatterData, double height){
		this(scatterData,DEFAULT_JOIN_SEGMENTS,height,DEFAULT_LEVEL);
	}

	public MAVScatterMesh(Collection<Point3D> scatterData, boolean joinSegments, double height, int level){
		setScatterData(scatterData);
		setJoinSegments(joinSegments);
		setHeight(height);
		setLevel(level);
		setMarker(MarkerFactory.Marker.CUBE);
		
		DEFAULT_SCATTER_DATA.add(new Point3D(0,0,0,1));

		idProperty().addListener(o -> updateMesh());
		updateMesh();
	}

	private final ObjectProperty<Collection<Point3D>> scatterData = new SimpleObjectProperty<Collection<Point3D>>(DEFAULT_SCATTER_DATA){

		@Override
		protected void invalidated() {
			if (meshes != null) {
				updateMesh();
			}
		}
	};

	public void addMeshPoint(float x, float y, float z) {
//	added.add(new Point3D(x,y,z,1));
	scatterData.get().add(new Point3D(x,y,z,1));
	}

	public void update() {
	//	if(added.size()>0) {
			
		//	scatterData.get().addAll(added);
			updateMesh();
		added.clear();
//		}
	}

	public Collection<Point3D> getScatterData() {
		return scatterData.get();
	}

	public final void setScatterData(Collection<Point3D> value) {
		scatterData.set(value);
	}

	public ObjectProperty<Collection<Point3D>> scatterDataProperty() {
		return scatterData;
	}

	private final ObjectProperty<List<Number>> functionData = new SimpleObjectProperty<List<Number>>(){
		@Override
		protected void invalidated() {
			if (meshes != null) {
				updateF(get());
			}
		}
	};

	public List<Number> getFunctionData() {
		return functionData.get();
	}

	public void setFunctionData(List<Number> value) {
		functionData.set(value);
	}

	public ObjectProperty<List<Number>> functionDataProperty() {
		return functionData;
	}

	// dot
	private final ObjectProperty<MarkerFactory.Marker> marker = new SimpleObjectProperty<MarkerFactory.Marker>(this, "dot", MarkerFactory.Marker.TETRAHEDRA) {
		@Override
		protected void invalidated() {
			if (meshes != null) {
				updateMesh();
			}
		}
	};
	public final ObjectProperty<MarkerFactory.Marker> markerProperty() {
		return marker;
	}
	public final MarkerFactory.Marker getMarker() {
		return marker.get();
	}
	public final void setMarker(MarkerFactory.Marker value) {
		marker.set(value);
	}

	private final DoubleProperty height = new SimpleDoubleProperty(DEFAULT_HEIGHT){
		@Override
		protected void invalidated() {
			if (meshes != null) {
				updateMesh();
			}
		}
	};

	public double getHeight() {
		return height.get();
	}

	public final void setHeight(double value) {
		height.set(value);
	}

	public DoubleProperty heightProperty() {
		return height;
	}

	private final IntegerProperty level = new SimpleIntegerProperty(DEFAULT_LEVEL){

		@Override
		protected void invalidated() {
			if (meshes != null) {
				updateMesh();
			}
		}

	};

	public final int getLevel() {
		return level.get();
	}

	public final void setLevel(int value) {
		level.set(value);
	}

	public final IntegerProperty levelProperty() {
		return level;
	}

	private final BooleanProperty joinSegments = new SimpleBooleanProperty(DEFAULT_JOIN_SEGMENTS){
		@Override
		protected void invalidated() {
			if (meshes != null) {
				updateMesh();
			}
		}
	};

	public boolean isJoinSegments() {
		return joinSegments.get();
	}

	public final void setJoinSegments(boolean value) {
		joinSegments.set(value);
	}

	public BooleanProperty joinSegmentsProperty() {
		return joinSegments;
	}

	protected final void updateMesh() {

		meshes=FXCollections.<TexturedMesh>observableArrayList();

		createMarkers();
		if(joinSegments.get()){
			//            System.out.println("Single mesh created");
		}
		getChildren().setAll(meshes);
		updateTransforms();
	}




	private AtomicInteger index;
	private void createMarkers() {
		
		if(scatterData.get().isEmpty())
			return;

		if(!joinSegments.get()){
			List<TexturedMesh> markers = new ArrayList<>();
			index = new AtomicInteger();
			scatterData.get().forEach(point3d ->
			markers.add(getMarker().getMarker(getId() + "-" + index.getAndIncrement(), height.get(), level.get(), point3d)));
			meshes.addAll(markers);
		} else {
			// Set marker id as f
			AtomicInteger i = new AtomicInteger();
			List<Point3D> indexedData = scatterData.get().stream()
					.map(p -> new Point3D(p.x, p.y, p.z, i.getAndIncrement()))
					.collect(Collectors.toList());

			TexturedMesh marker = getMarker().getMarker(getId(), height.get(), level.get(), indexedData.get(0));
			/*
            Combine new polyMesh with previous polyMesh into one single polyMesh
			 */
			MeshHelper mh = new MeshHelper((TriangleMesh) marker.getMesh());
			TexturedMesh dot1 = getMarker().getMarker("", height.get(), level.get(), null);
			MeshHelper mh1 = new MeshHelper((TriangleMesh) dot1.getMesh());
			mh.addMesh(mh1, indexedData.stream().skip(1).collect(Collectors.toList()));
			marker.updateMesh(mh);
			meshes.add(marker);
		}
		
	}
	


	@Override
	public void setTextureModeNone() {
		meshes.stream().forEach(m->m.setTextureModeNone());
	}

	@Override
	public void setTextureModeNone(Color color) {
		meshes.stream().forEach(m->m.setTextureModeNone(color));
	}

	@Override
	public void setTextureModeNone(Color color, String image) {
		meshes.stream().forEach(m->m.setTextureModeNone(color,image));
	}

	@Override
	public void setTextureModeImage(String image) {
		meshes.stream().forEach(m->m.setTextureModeImage(image));
	}

	@Override
	public void setTextureModePattern(Patterns.CarbonPatterns pattern, double scale) {
		meshes.stream().forEach(m->m.setTextureModePattern(pattern, scale));
	}

	@Override
	public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens) {
		meshes.stream().forEach(m->m.setTextureModeVertices3D(colors, dens));
	}

	@Override
	public void setTextureModeVertices3D(ColorPalette palette, Function<Point3D, Number> dens) {
		meshes.stream().forEach(m->m.setTextureModeVertices3D(palette, dens));
	}

	@Override
	public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens, double min, double max) {
		meshes.stream().forEach(m->m.setTextureModeVertices3D(colors, dens, min, max));
	}

	@Override
	public void setTextureModeVertices1D(int colors, Function<Number, Number> function) {
		meshes.stream().forEach(m->m.setTextureModeVertices1D(colors, function));
	}

	@Override
	public void setTextureModeVertices1D(ColorPalette palette, Function<Number, Number> function) {
		meshes.stream().forEach(m->m.setTextureModeVertices1D(palette, function));
	}

	@Override
	public void setTextureModeVertices1D(int colors, Function<Number, Number> function, double min, double max) {
		meshes.stream().forEach(m->m.setTextureModeVertices1D(colors, function, min, max));
	}

	@Override
	public void setTextureModeFaces(int colors) {
		meshes.stream().forEach(m->m.setTextureModeFaces(colors));
	}

	@Override
	public void setTextureModeFaces(ColorPalette palette) {
		meshes.stream().forEach(m->m.setTextureModeFaces(palette));
	}

	@Override
	public void updateF(List<Number> values) {
		meshes.stream().forEach(m->m.updateF(values));
	}

	@Override
	public void setTextureOpacity(double value) {
		meshes.stream().forEach(m->m.setTextureOpacity(value));
	}

	public void setDrawMode(DrawMode mode) {
		meshes.stream().forEach(m->m.setDrawMode(mode));
	}

	private void updateTransforms() {
		meshes.stream().forEach(m->m.updateTransforms());
	}

	public TexturedMesh getMeshFromId(String id){
		return meshes.stream().filter(p->p.getId().equals(id)).findFirst().orElse(meshes.get(0));
	}

	public Color getDiffuseColor() {
		return meshes.stream()
				.findFirst()
				.map(m -> m.getDiffuseColor())
				.orElse(TriangleMeshHelper.DEFAULT_DIFFUSE_COLOR);
	}

}
