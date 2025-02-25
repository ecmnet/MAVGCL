package com.comino.flight.model.map;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavcom.model.segment.Status;
import com.comino.mavcom.status.StatusManager;
import com.comino.mavmap.map.map3D.Map3DSpacialInfo;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOccupancyOcTreeNode;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOctoMap3D;

import georegression.struct.point.Point3D_F32;
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
		
		super.enableRemoveOutdated(false);
		this.info = new Map3DSpacialInfo(0.2,200,200,200);

		this.model = control.getCurrentModel();

		control.getStatusManager().addListener(StatusManager.TYPE_MSP_STATUS, Status.MSP_ARMED,
				StatusManager.EDGE_RISING, (a) -> {
						clear();
						model.grid.count = -1;
				});

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
			
	  // removeOutdatedNodes(1000);

		});	
	}
	
	private Point3D_F32 pg = new Point3D_F32();

	public void insertBoolean(long encoded, float resolution, Point3D_F32 pos) {
		
		final float value = decode(encoded,pg,10.0f,0.2f);
		pg.z = pos.z;

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
	
	private float decode(long mpi, Point3D_F32 p, float extension, float resolution) {
		final short f = (short)(extension / resolution);
		final float value = p.x = (mpi & 0xFFFL) / 4096.0f;
		p.x = (mpi >> 12  & 0x1FFFFL )/ 2 / (float) f - f;
		p.y = (mpi >> 29 & 0x1FFFFL) / 2 / (float) f - f;
		p.z = (mpi >> 46 & 0x1FFFFL) / 2 / (float) f - f;
		return value;
	}

}
