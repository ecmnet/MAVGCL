/****************************************************************************
 *
 *   Copyright (c) 2017,2018 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.ui.tabs;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mavlink.messages.SERIAL_CONTROL_DEV;
import org.mavlink.messages.SERIAL_CONTROL_FLAG;
import org.mavlink.messages.lquac.msg_serial_control;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.observables.StateProperties;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.mavlink.IMAVLinkListener;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.util.Duration;


public class MavLinkShellTab extends Pane implements IMAVLinkListener  {


	private final static String[] defaults = { "reboot", "work_queue status", "gps status", "sensors status", "dmesg" };


	private IMAVController control;

	@FXML
	private TextArea console;

	private StateProperties     state  = null;
	private LinkedList<String> last    = null;
	private int lastindex = 0;

	private int index = 0;

	private AnimationTimer out = null;
	private ConcurrentLinkedQueue<String> buffer = new ConcurrentLinkedQueue<String>();

	private char[] bytes = new char[132];

	private msg_serial_control msg = new msg_serial_control(1,1);


	public MavLinkShellTab() {
		FXMLLoadHelper.load(this, "MavLinkShellTab.fxml");
		last = new LinkedList<String>();
		for(int i=0; i<defaults.length;i++)
			last.add(defaults[i]);
	}

	@FXML
	private void initialize() {

		
		this.out = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if(buffer.isEmpty())
					return;

				while(!buffer.isEmpty())
					console.appendText(buffer.poll());
				index = console.getText().length();
				scrollIntoView();
			}
		};


		state = StateProperties.getInstance();

		console.prefHeightProperty().bind(heightProperty().subtract(2));
		console.prefWidthProperty().bind(widthProperty());

		console.setOnKeyPressed(ke -> {
			if (ke.getCode().equals(KeyCode.ESCAPE)) {
				reloadShell();
			}
			else if (ke.getCode().equals(KeyCode.ENTER)) {
				int end = console.getText().length();
				if(end > index) {
					String command = console.getText(index,end).trim();
					if(command.equalsIgnoreCase("reboot")) {
						buffer.add("..waiting for FCU to ");
						console.setEditable(false);
						new Timeline(new KeyFrame(Duration.millis(6000), ae ->  { reloadShell(); })).play();
					}
					console.deleteText(index, end);
					writeToShell(command+"\n");
					if(!command.equals(last.peekLast()) && command.length()>1) {
						last.add(command);
					}
					scrollIntoView();
				}
				else
					writeToShell("\n");
				lastindex = last.size();
			} else if (ke.getCode().equals(KeyCode.UP)) {
				Platform.runLater(() -> {
					int end = console.getText().length();
					if(!last.isEmpty() && lastindex > 0 && console.getText().length() > 0) {
						try {
						console.deleteText(index, console.getText().length());
						} catch(Exception e) { }
						console.appendText(last.get(--lastindex));
						end = console.getText().length();
						console.selectRange(end,end);
					} else {
						console.selectRange(end,end);
					}
				});
			} else if (ke.getCode().equals(KeyCode.DOWN)) {
				Platform.runLater(() -> {
					if(!last.isEmpty() && lastindex <= last.size()-1) {
						console.deleteText(index, console.getText().length());
						if(lastindex++ < last.size()-1)
							console.appendText(last.get(lastindex));
						int end = console.getText().length();
						console.selectRange(end,end);
					}
				});
			} else if (ke.getCode().equals(KeyCode.LEFT)) {
				Platform.runLater(() -> {
					int end = console.getText().length();
					console.selectRange(end, end);
				});
			}  else if (ke.isControlDown() && ke.getCode().equals(KeyCode.C)) {
				System.out.println("CRTL+C");
				writeToShell("\u0003");
			}  else if (ke.getCode().equals(KeyCode.BACK_SPACE)) {
				Platform.runLater(() -> {
					int end = console.getText().length();
					if(end > index) {
						console.deleteText(end, end);
						console.selectRange(end, end);
					}
				});
			}
			ke.consume();
		});

		console.setWrapText(true);

		state.getConnectedProperty().addListener((v,ov,nv) -> {
			if(nv.booleanValue())
				reloadShell();
			console.setDisable(!nv.booleanValue());
		});

	}


	public MavLinkShellTab setup(IMAVController control) {
		this.control = control;
		this.state   = StateProperties.getInstance();
		control.addMAVLinkListener(this);

		this.disabledProperty().addListener((v,ov,nv) -> {
			if(!nv.booleanValue()) {
				out.start();
				scrollIntoView();

			} else {
				out.stop();
			}
			lastindex = last.size();
		});

		return this;
	}



	@Override
	public void requestFocus() {
		Platform.runLater(() -> {
			console.requestFocus(); });
	}


	@Override
	public void received(Object _msg) {
		if(!isDisabled() && _msg instanceof msg_serial_control) {
			final msg_serial_control msg = (msg_serial_control)_msg;
			int j=0;
			if(msg.count > 0) {
				for(int i=0;i<=msg.count && i < msg.data.length;i++) {
					if((msg.data[i] & 0x007F)!=0)
						bytes[j++] = (char)(msg.data[i] & 0x007F);	
				}
				String line = String.copyValueOf(bytes,0,j).replace("[K", "");
				if(line.contains(">"))
					buffer.add(line.substring(0,line.length()-1));
				else
				buffer.add(line);
			}
		}
	}

	private void reloadShell() {
		buffer.clear();
		Platform.runLater(() -> {
			console.clear();
			console.setEditable(true);
			if(!isDisabled())
				writeToShell("\n");
		});
	}


	private void writeToShell(String s) {

		if(s!=null) {
			Arrays.fill(msg.data, 0);
			try {
				if(s.equals("\n")) {
					msg.data[0] = 13;
					msg.count = 1;
				}
				else {
					byte[] bytes = s.getBytes("US-ASCII");
					for(int i =0;i<bytes.length && i<70;i++)
						msg.data[i] = bytes[i];
					msg.count = bytes.length;
				}
				msg.device = SERIAL_CONTROL_DEV.SERIAL_CONTROL_DEV_SHELL;
				msg.flags  = SERIAL_CONTROL_FLAG.SERIAL_CONTROL_FLAG_RESPOND;

			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			control.sendMAVLinkMessage(msg);
		} 
	}

	private void scrollIntoView() {
		Platform.runLater(() -> {
			console.requestFocus();
			console.selectEnd();
			//		console.selectRange(index, index);
		});
	}


}
