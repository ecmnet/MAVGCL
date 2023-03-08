package com.comino.flight.model.map;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;
import com.comino.mavmap.map.map3D.impl.octomap.MAVOctoMap3D;

public class MAVGCLOctoMap extends MAVOctoMap3D {
	
	private final DataModel model;
	
	private static MAVGCLOctoMap instance = null;
	
	public static MAVGCLOctoMap getInstance(IMAVController control) {
		if(instance==null)
			instance = new MAVGCLOctoMap(control);
		return instance;
	}

	
	public MAVGCLOctoMap(IMAVController control) {
		super();
		
		this.model = control.getCurrentModel();
		
		control.addMAVLinkListener((o) -> {
			if(o instanceof msg_msp_micro_grid) {
				
				msg_msp_micro_grid grid = (msg_msp_micro_grid) o;
				
				if(grid.count < 0) {
					clear(); 
					model.grid.count = -1;
					return;
				}
				
				for(int i=0;i< grid.data.length;i++) {
					if(grid.data[i] > 0)
						insert(grid.data[i]);
				}
				
				model.grid.count = this.getNumberOfNodes();

			}
		});	
	}


}
