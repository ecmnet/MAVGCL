package com.comino.flight.model.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.mavlink.messages.lquac.msg_msp_micro_grid;

import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.model.DataModel;

import georegression.struct.point.Point2D_F32;

public class MAVGCLMap {

	private static final float LIMIT = 0.5f;
	private static final float MAX_DISTANCE_SQUARED = 100.0f;

	private static MAVGCLMap mav2dmap = null;

	private final DataModel model;

	private final Point2D_F32 indicator = new Point2D_F32();

	private final BlockingQueue<Long> set = new ArrayBlockingQueue<Long>(50000);
	private final Map<Long, Box> map = new HashMap<Long, Box>();
	private final Point2D_F32 origin = new Point2D_F32();

	private final long dimension;
	private final long dimensionxy;
	private final long dimensionxyz;

	private long last_update = -1;
	private int transfer_count = 0;

	public static MAVGCLMap getInstance(IMAVController control, long dimension) {
		if (mav2dmap == null)
			mav2dmap = new MAVGCLMap(control, dimension);
		return mav2dmap;
	}

	public static MAVGCLMap getInstance() {
		return mav2dmap;
	}

	private MAVGCLMap(IMAVController control, long dimension) {

		this.model = control.getCurrentModel();
		this.dimension = dimension;
		this.dimensionxy = dimension * dimension;
		this.dimensionxyz = dimension * dimension * dimension;

		new Thread(new Mapper()).start();

		control.addMAVLinkListener((o) -> {
			if (o instanceof msg_msp_micro_grid) {

				msg_msp_micro_grid grid = (msg_msp_micro_grid) o;

				if (grid.count == 0) {
					clear();
					origin.setTo(grid.cx, grid.cy);
					return;
				}

				for (int i = 0; i < grid.count; i++) {
					long mpi = grid.data[i];
					if (check(mpi, LIMIT))
						set.add(mpi);
				}
				cleanUp(model.state.l_x, model.state.l_y);
				last_update = System.currentTimeMillis();
			}
		});

	}

	public Map<Long, Box> getMap() {
		return map;
	}

	public Point2D_F32 getIndicator() {
		return indicator;
	}

	public Point2D_F32 getOrigin() {
		return indicator;
	}

	public void clear() {
		last_update = -1;
		map.clear();
	}

	public long getLastUpdate() {
		return last_update;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	public void cleanUp(float x, float y) {
		final ArrayList<Long> del = new ArrayList<Long>();
//		map.forEach((mpi, box) -> {		
//			if (box.p.)
//				del.add(mpi);
//		});
//		del.forEach((mpi) -> {
//			map.remove(mpi);
//		});
	}

	private boolean check(long mpi, float limit) {
		if (((long) (mpi / dimensionxyz) / 100.0) < limit)
			return false;
		return true;
	}

	public class Box {
		public Point2D_F32 p;
		public long tms;
		public float width;

		Box(long mpi) {
			p = new Point2D_F32();
			float w2 = ((int) (mpi / dimensionxy % dimension) / 200.0f);
			this.width = w2 * 2.0f;
			this.p.x = ((int) (mpi % dimension) * this.width);
			this.p.y = ((int) (mpi / dimension % dimension) * this.width);
			this.tms = System.currentTimeMillis();
			// System.out.println("X: "+p.x+" Y: "+p.y+" W: "+width);
		}

	}

	public float getResolution() {
		return 0.2f;
	}

	private class Mapper implements Runnable {

		@Override
		public void run() {
			while (true) {
				set.forEach((mpi) -> {
					if(map.containsKey(mpi))
						map.get(mpi).tms = System.currentTimeMillis();
					else
					  map.put(mpi, new Box(mpi));
				});
				set.clear();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}

}
