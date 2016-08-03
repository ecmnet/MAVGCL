/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
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

package com.comino.flight.tabs.shell;

import java.io.UnsupportedEncodingException;

import org.mavlink.messages.SERIAL_CONTROL_DEV;
import org.mavlink.messages.SERIAL_CONTROL_FLAG;
import org.mavlink.messages.lquac.msg_serial_control;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.observables.StateProperties;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMAVLinkListener;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;


public class MavLinkShellTab extends Pane implements IMAVLinkListener  {


	private IMAVController control;

	@FXML
	private TextArea console;

	private StateProperties state  = null;
	private String         last    = null;

	private int index = 0;


	public MavLinkShellTab() {
		FXMLLoadHelper.load(this, "MavLinkShellTab.fxml");
	}

	@FXML
	private void initialize() {


		state = StateProperties.getInstance();

		console.prefHeightProperty().bind(heightProperty());
		console.prefWidthProperty().bind(widthProperty().subtract(5));

		console.setOnKeyPressed(ke -> {
			if (ke.getCode().equals(KeyCode.ENTER)) {
				int end = console.getText().length();
				if(end > index) {
					String command = console.getText(index,end).trim()+"\n";
					writeToShell(command);
					index = end+1;
					last = command;
					scrollIntoView();
				}
			} else if (ke.getCode().equals(KeyCode.UP)) {
				if(last!=null) {
					Platform.runLater(() -> {
						console.appendText(last);
						scrollIntoView();
					});
				}
			} else if (ke.getCode().equals(KeyCode.LEFT)) {
				Platform.runLater(() -> {
					int end = console.getText().length();
					console.selectRange(end, end);
				});
			}  else if (ke.isControlDown() && ke.getCode().equals(KeyCode.C)) {
				System.out.println("CRTL+C");
				writeToShell("\u0003");
			}
		});

		console.mouseTransparentProperty().set(true);
		console.setWrapText(true);


		state.getConnectedProperty().addListener((v,ov,nv) -> {
			if(nv.booleanValue()) {
				Platform.runLater(() -> {
					console.setText("");
					if(!isDisabled())
						writeToShell("\n");
					scrollIntoView();
				});
			}
			console.setDisable(!nv.booleanValue());
		});
	}


	public MavLinkShellTab setup(IMAVController control) {
		this.control = control;
		this.state   = StateProperties.getInstance();
		control.addMAVLinkListener(this);

		this.disabledProperty().addListener((v,ov,nv) -> {
			if(!nv.booleanValue()) {
				Platform.runLater(() -> {
					if(console.getText().length()==0)
						writeToShell("\n");
					scrollIntoView();
				});

			} else {
				writeToShell(null);
				Platform.runLater(() -> {
					console.clear();
				});
			}
		});

		return this;
	}

	@Override
	public void received(Object _msg) {
		if(!this.isDisabled()) {
			if(_msg instanceof msg_serial_control) {
				msg_serial_control msg = (msg_serial_control)_msg;
				byte[] bytes = new byte[msg.count];
				for(int i=0;i<msg.count;i++) {
					if(msg.data[i]==0x1b)
						break;
					bytes[i] = (byte)(msg.data[i] & 0xFF);
				}
				Platform.runLater(() -> {
					try {
						console.appendText(new String(bytes,"US-ASCII"));
						index = console.getText().length();
						scrollIntoView();
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

	public void setWidthBinding(double horizontal_space) {
		console.prefWidthProperty().bind(widthProperty().subtract(horizontal_space+5));
	}


	private void writeToShell(String s) {
		msg_serial_control msg = new msg_serial_control(1,1);
		if(s!=null) {
			System.out.println(">"+s);
			try {
				byte[] bytes = s.getBytes("US-ASCII");
				for(int i =0;i<bytes.length && i<70;i++)
					msg.data[i] = bytes[i];
				msg.count = bytes.length;
				msg.device = SERIAL_CONTROL_DEV.SERIAL_CONTROL_DEV_SHELL;
				msg.flags  = SERIAL_CONTROL_FLAG.SERIAL_CONTROL_FLAG_RESPOND;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else {
			msg.count = 0;
			msg.device = SERIAL_CONTROL_DEV.SERIAL_CONTROL_DEV_SHELL;
		}

		control.sendMAVLinkMessage(msg);
	}

	private void scrollIntoView() {
		console.requestFocus();
		console.setScrollTop(Double.MAX_VALUE);
		console.selectRange(index, index);
	}


}
