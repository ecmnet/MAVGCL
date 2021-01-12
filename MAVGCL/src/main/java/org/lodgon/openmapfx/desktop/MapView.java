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
package org.lodgon.openmapfx.desktop;

import java.net.URL;

import org.lodgon.openmapfx.core.DefaultBaseMapProvider;
import org.lodgon.openmapfx.core.LayeredMap;
import org.lodgon.openmapfx.core.LicenceLayer;
import org.lodgon.openmapfx.core.Position;
import org.lodgon.openmapfx.core.PositionLayer;
import org.lodgon.openmapfx.core.TileProvider;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class MapView extends Application {

    LayeredMap map;

    TileProvider[] tileProviders;
    SimpleProviderPicker spp;

    LicenceLayer licenceLayer;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        DefaultBaseMapProvider provider = new DefaultBaseMapProvider();

        spp = new SimpleProviderPicker(provider);

        map = new LayeredMap(provider);

        BorderPane cbp = new BorderPane();
        cbp.setCenter(map);

        Rectangle clip = new Rectangle(700, 600);
        cbp.setClip(clip);
        clip.heightProperty().bind(cbp.heightProperty());
        clip.widthProperty().bind(cbp.widthProperty());

        BorderPane bp = new BorderPane();
        bp.setTop(spp);
        bp.setCenter(cbp);

        Scene scene = new Scene(bp, 700, 600);
        stage.setScene(scene);
        stage.show();
     //   map.setZoom(4);
        map.setPrefHeight(600);
        map.setPrefWidth(700);
        map.setViewport(52.0,4.9,50.1,4.0);
        map.setCenter(48.142899,11.577723);
        showMyLocation();
        map.setZoom(12);



//        licenceLayer = new LicenceLayer(provider);
//        map.getLayers().add(licenceLayer);
        Position p = map.getMapArea().getMapPosition(300,250);
        System.out.println ("position = "+p.getLatitude()+", "+p.getLongitude());
    }

    private void showMyLocation() {
        URL im = this.getClass().getResource("airplane.png");
        Image image = new Image(im.toString());
        PositionLayer positionLayer = new PositionLayer(image);
        map.getLayers().add(positionLayer);
        positionLayer.updatePosition(48.142899,11.577723);
        map.centerLatitudeProperty().addListener(i -> {
            System.out.println("center of map: lat = " + map.centerLatitudeProperty().get() + ", lon = " + map.centerLongitudeProperty().get());
        });
    }

}
