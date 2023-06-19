package com.comino.flight.model.map;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOccupancyOcTreeNode;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOctoMap3D;

import us.ihmc.jOctoMap.key.OcTreeKey;

public class MAVGCLOctoMap extends MAVOctoMap3D {

	private final DataModel model;

	private static MAVGCLOctoMap instance = null;
	private int count;

	public static MAVGCLOctoMap getInstance(IMAVController control) {
		if(instance==null)
			instance = new MAVGCLOctoMap(control);
		return instance;
	}


	public MAVGCLOctoMap(IMAVController control) {
		super();
		
		super.enableRemoveOutdated(true);

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

				for(int i=0;i< grid.data.length;i++) {
					if(grid.data[i] > 0) {
						insertBoolean(grid.data[i]);
					}
				}
			}
			
		//	removeOutdatedNodes(1000);

		});	
	}

	public void insertBoolean(long encoded) {
		OcTreeKey key = new OcTreeKey ();
		int value = decode(encoded,key);
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
