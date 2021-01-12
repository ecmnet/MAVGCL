/*
 * Copyright (c) 2014, 2015, OpenMapFX and LodgON
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of LodgON, OpenMapFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL LODGON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lodgon.openmapfx.core;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author Geoff Capper
 */
public class LicenceLayer extends AnchorPane implements MapLayer {

    private Label lblLicence;

    private BaseMapProvider provider;

    private final ChangeListener<TileProvider> tileProviderListener = (ObservableValue<? extends TileProvider> obs, TileProvider o, TileProvider n) -> {
        updateLicence(n);
    };

    public LicenceLayer(BaseMapProvider provider) {
        this.provider = provider;
        this.provider.tileProviderProperty().addListener(tileProviderListener);

        lblLicence = new Label();
        lblLicence.setText("");
        lblLicence.setStyle("-fx-background-color:rgba(66%,66%,66%,0.5)");

        AnchorPane.setLeftAnchor(lblLicence, 0.0);
        AnchorPane.setBottomAnchor(lblLicence, 0.0);
        //setRightAnchor(lblLicence, 0.0);

        getChildren().add(lblLicence);

        updateLicence(provider.tileProviderProperty().get());
    }

    public void setBaseMapProvider(BaseMapProvider provider) {
        this.provider.tileProviderProperty().removeListener(tileProviderListener);
        this.provider = provider;
        this.provider.tileProviderProperty().addListener(tileProviderListener);
        updateLicence(provider.tileProviderProperty().get());
    }

    private void updateLicence(TileProvider tileProvider) {
        if (tileProvider != null) {
            lblLicence.setText(tileProvider.getAttributionNotice());
        } else {
            lblLicence.setText("");
        }
    }

    @Override
    public Node getView() {
        return this;
    }

    @Override
    public void gotLayeredMap(LayeredMap map) {
        this.minWidthProperty().bind(map.widthProperty());
        this.minHeightProperty().bind(map.heightProperty());
    }

}
