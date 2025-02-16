package com.comino.flight.model.map;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOccupancyOcTreeNode;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOctoMap3D;

import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point3D_I32;
import us.ihmc.jOctoMap.key.OcTreeKey;
import us.ihmc.jOctoMap.tools.OcTreeKeyConversionTools;

public class MAVGCLOctoMap extends MAVOctoMap3D {

	private final DataModel model;
	private final Map3DSpacialInfo info;

	private static MAVGCLOctoMap instance = null;
	private int count;
	private Point3D_F32 pos = new Point3D_F32();

	public static MAVGCLOctoMap getInstance(IMAVController control) {
		if(instance==null)
			instance = new MAVGCLOctoMap(control);
		return instance;
	}


	public MAVGCLOctoMap(IMAVController control) {
		super();
		
		super.enableRemoveOutdated(true);
		this.info = new Map3DSpacialInfo(0.2,200,200,200);

		this.model = control.getCurrentModel();

		//		control.getStatusManager().addListener(StatusManager.TYPE_MSP_STATUS, Status.MSP_CONNECTED, StatusManager.EDGE_RISING, (a) -> {
		//			if(!model.sys.isStatus(Status.MSP_ARMED)) {
		//				clear(); 
		//				model.grid.count = -1;
		//			}
		//		});

		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_msp_micro_grid) {

				msg_msp_micro_grid grid = (msg_msp_micro_grid) o;

				if(grid.count < 0) {
					clear(); 
					model.grid.count = -1;
					return;
				}
				
				if(grid.resolution != this.getResolution()) {
					clearAndChangeResolution(grid.resolution);
				}
				
				pos.x = grid.cx;
				pos.y = grid.cy;
				pos.z = grid.cz;
				
				

				for(int i=0;i< grid.data.length;i++) {
					if(grid.data[i] > 0) {
						insertBoolean(grid.data[i],grid.resolution,pos);
					}
				}
			}
			
	//	removeOutdatedNodes(1000);

		});	
	}

	public void insertBoolean(long encoded, float resolution, Point3D_F32 pos) {
//		OcTreeKey key = new OcTreeKey ();	
//		int value = decode(encoded,key);
		Point3D_I32 p = new Point3D_I32(); Point3D_F64 pg = new Point3D_F64();
		double value = info.decodeMapPointXY(encoded, p);
		p.x = p.x + info.getCenter().x;
		p.y = p.y + info.getCenter().y;
		info.mapToGlobal(p, pg);
		pg.z = pos.z;
		pg.x = pg.x - 50.0f;
		pg.y = pg.y - 50.0f;
	//	System.out.println(pg.x+":"+pg.y+":"+pg.z+" => "+value);
		OcTreeKey key = OcTreeKeyConversionTools.coordinateToKey(pg.x, -pg.y, pg.z, resolution, this.getTree().getTreeDepth());
		if(key==null)
			return;
		
		if(value >= 0.5) {
			this.getTree().updateNode(key, true);
		}
		else {
			MAVOccupancyOcTreeNode node = this.getTree().search(key);
			if(node!=null) {
				node.clear();
				this.getTree().getChangedKeys().put(key, (byte)1);
			}
		
		}
	}


}
