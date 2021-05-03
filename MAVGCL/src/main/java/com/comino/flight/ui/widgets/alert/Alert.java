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

package com.comino.flight.ui.widgets.alert;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_statustext;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.prefs.MAVPreferences;
import com.comino.jfx.extensions.ChartControlPane;
import com.comino.mavcom.control.IMAVController;
import com.comino.mavcom.mavlink.IMAVLinkListener;
import com.comino.mavcom.model.segment.LogMessage;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;


// NOT USED CURRENTLY

public class Alert extends ChartControlPane    {

	@FXML
	private Label message;

	public Alert() {
		super(5000,false);
		FXMLLoadHelper.load(this, "Alert.fxml");
	}


	@FXML
	private void initialize() {	
		message.setLayoutX(this.getWidth()/2);
		
	}

	public void setup(IMAVController control) {
		
		control.addMAVLinkListener(new IMAVLinkListener() {
			@Override
			public void received(Object o) {
				
				if(isDisabled())
					return;
				
				if(o instanceof msg_statustext) {
					msg_statustext msg = (msg_statustext) o;
					if(
					   (msg.severity == MAV_SEVERITY.MAV_SEVERITY_EMERGENCY ||
					    msg.severity == MAV_SEVERITY.MAV_SEVERITY_ALERT     ||
					    msg.severity == MAV_SEVERITY.MAV_SEVERITY_CRITICAL  ) &&
						MAVPreferences.getInstance().getBoolean(MAVPreferences.ALERT, false)
						&& !control.isSimulation()) {
						
						Platform.runLater(() -> {
							String m = "["+LogMessage.severity_texts[msg.severity]+"] "+(new String(msg.text)).trim();
							message.setText(m.length() > 60 ? m.substring(0, 58)+".." : m );
							message.setAlignment(Pos.CENTER);
							fadeProperty().set(true);	
							fadeProperty().set(false);	
						});
					}
				}
			}
		});



	}
}
