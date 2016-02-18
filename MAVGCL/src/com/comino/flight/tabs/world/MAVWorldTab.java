/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight.tabs.world;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.mav.control.IMAVController;
import com.comino.msp.utils.ExecutorService;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;

public class MAVWorldTab extends BorderPane  {

	@FXML
	private ImageView worldimage;


	private World world;
	private FrameBuffer buffer;
	private Object3D box;

	private Task<Long> task;

	private WritableImage image ;
	private BufferedImage bimg;

	public MAVWorldTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVWorldTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				while(true) {
					try {
						Thread.sleep(20);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled() ) {
						break;
					}

					updateValue(System.currentTimeMillis());
				}
				return System.currentTimeMillis();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Long>() {

			@Override
			public void changed(ObservableValue<? extends Long> observableValue, Long oldData, Long newData) {
				try {

					draw();


				} catch(Exception e) { e.printStackTrace(); }

			}
		});

		world = new World();
		world.setAmbientLight(0, 255, 0);

		TextureManager.getInstance().addTexture("box", new Texture(getClass().getResourceAsStream("box.jpg")));

		box = Primitives.getBox(13f, 2f);
		box.setTexture("box");
		box.setEnvmapped(Object3D.ENVMAP_ENABLED);
		box.build();
		world.addObject(box);

		world.getCamera().setPosition(50, -50, -5);
		world.getCamera().lookAt(box.getTransformedCenter());

		buffer = new FrameBuffer(600, 600, FrameBuffer.SAMPLINGMODE_NORMAL);
		image = new WritableImage(600,600);
		bimg = new BufferedImage(600,600, 8);
		worldimage.setImage(image);


	}


	@FXML
	private void initialize() {





	}


	public MAVWorldTab setup(ChartControlWidget recordControl, IMAVController control) {
		ExecutorService.get().execute(task);
		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return null;
	}

	public IntegerProperty getTimeFrameProperty() {
		return null;
	}



	private void draw() throws Exception {


		box.rotateY(0.01f);
		buffer.clear(java.awt.Color.BLUE);
		world.renderScene(buffer);
		world.draw(buffer);
		buffer.update();
		buffer.display(bimg.getGraphics());
		//			BufferedImage img = (BufferedImage) buffer.getOutputBuffer();
		SwingFXUtils.toFXImage(bimg, image);





	}


}
