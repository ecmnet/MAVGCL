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

package org.lodgon.openmapfx.desktop;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import org.lodgon.openmapfx.core.BaseMapProvider;
import org.lodgon.openmapfx.core.DefaultBaseMapProvider;
import org.lodgon.openmapfx.core.TileProvider;
import org.lodgon.openmapfx.core.TileType;

/**
 *
 * @author Geoff Capper
 */
public class SimpleProviderPicker extends HBox {
    
    private final ComboBox<TileProvider> cmbProviders;
    private final HBox buttonBox;
    
    private final ObjectProperty<TileType> selectedTileType = new SimpleObjectProperty<>();
    private final DefaultBaseMapProvider provider;
    
    public SimpleProviderPicker(DefaultBaseMapProvider provider) {
        super(4);
        this.setStyle("-fx-padding:4px");
        this.provider = provider;
        
        if (provider.getTileProviders().isEmpty()) {
            throw new IllegalArgumentException("Providers array passed to SimpleProviderPicker cannot be null or empty.");
        }
        
        cmbProviders = new ComboBox<>(FXCollections.observableArrayList(provider.getTileProviders()));
        cmbProviders.valueProperty().addListener((ObservableValue<? extends TileProvider> obs, TileProvider o, TileProvider n) -> {
            setCurrentTileProvider(n);
        });
        
        buttonBox = new HBox(4);
        
        getChildren().addAll(cmbProviders, buttonBox);
        
        cmbProviders.getSelectionModel().select(provider.getTileProviders().get(0));
        provider.tileProviderProperty().bind(cmbProviders.getSelectionModel().selectedItemProperty());//set(tp);
        provider.tileTypeProperty().bind(selectedTileType);
		
    }
    
    private void setCurrentTileProvider(TileProvider tp) {
        
        buttonBox.getChildren().clear();
        
        final ToggleGroup group = new ToggleGroup();
        
        for (TileType tt : tp.getTileTypes()) {
            ToggleButton tb = new ToggleButton(tt.getTypeName());
            tb.setUserData(tt);
            tb.setToggleGroup(group);
            if (tt.equals(tp.getDefaultType())) {
                tb.setSelected(true);
                selectedTileType.set(tt);
            }
            buttonBox.getChildren().add(tb);
        }
        
        group.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle o, Toggle n) -> {
            if (n == null) {
                // ignore - but we should reset the button.
            } else {
                selectedTileType.set((TileType) n.getUserData());
            }
        });
        
    }
    
    public ObjectProperty<TileType> selectedTileTypeProperty() {
        return selectedTileType;
    }
    
}
