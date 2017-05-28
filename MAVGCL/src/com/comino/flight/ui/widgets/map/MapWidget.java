package com.comino.flight.ui.widgets.map;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.comino.flight.FXMLLoadHelper;
import com.comino.flight.ui.widgets.map.event.MapEvent;
import com.comino.flight.ui.widgets.map.event.MapEvent.EventType;
import com.comino.flight.ui.widgets.map.event.MapEventListener;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MapWidget extends Pane {

	private              WebView                      webView;
	private              WebEngine                    webEngine;
	private              boolean                      readyToGo;

	private              MapProvider                  _mapProvider = MapProvider.TOPO;
	private              ObjectProperty<MapProvider>   mapProvider;


	private              Location               currentLocation;

	private  final MapEvent        MAP_PROVIDER_EVENT    = new MapEvent(EventType.MAP_PROVIDER);
	private List<MapEventListener>  listenerList         = new CopyOnWriteArrayList<>();

	public MapWidget() {

		FXMLLoadHelper.load(this, "MapWidget.fxml");
		currentLocation = new Location(47.1,11.8);
	}

	@FXML
	private void initialize() {
		initGraphics();
	}

	protected void initGraphics() {

		webView = new WebView();
		webEngine = webView.getEngine();
		webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
			if (Worker.State.SUCCEEDED == n) {
				readyToGo = true;
				if (MapProvider.BW != getMapProvider()) { changeMapProvider(getMapProvider()); }
				updateLocation();
				//				updateLocationColor();
				//				tile.getPoiList().forEach(poi -> addPoi(poi));
				//				addTrack(tile.getTrack());
				//				updateTrackColor();
			}
		});

		URL maps = this.getClass().getResource("resources/osm.html");
		webEngine.load(maps.toExternalForm());

		this.getChildren().addAll(webView);
	}

	private void updateLocation() {
		if (readyToGo) {
			Platform.runLater(() -> {
				Location      location      = currentLocation;
				double        lat           = location.getLatitude();
				double        lon           = location.getLongitude();
				String        name          = location.getName();
				String        info          = location.getInfo();
				int           zoomLevel     = location.getZoomLevel();
				StringBuilder scriptCommand = new StringBuilder();
				scriptCommand.append("window.lat = ").append(lat).append(";")
				.append("window.lon = ").append(lon).append(";")
				.append("window.locationName = \"").append(name).append("\";")
				.append("window.locationInfo = \"").append(info.toString()).append("\";")
				.append("window.zoomLevel = ").append(zoomLevel).append(";")
				.append("document.moveMarker(window.locationName, window.locationInfo, window.lat, window.lon, window.zoomLevel);");
				webEngine.executeScript(scriptCommand.toString());
			});
		}
	}

	private void changeMapProvider(final MapProvider PROVIDER) {
		if (readyToGo) {
			System.out.println(PROVIDER.name);
			Platform.runLater(() -> {
				StringBuilder scriptCommand = new StringBuilder();
				scriptCommand.append("window.provider = '").append(PROVIDER.name).append("';")
				.append("document.changeMapProvider(window.provider);");
				webEngine.executeScript(scriptCommand.toString());
			});
		}
	}

	public MapProvider getMapProvider() { return null == mapProvider ? _mapProvider : mapProvider.get(); }
	public void setMapProvider(final MapProvider PROVIDER) {
		if (null == mapProvider) {
			_mapProvider = PROVIDER;
			fireTileEvent(MAP_PROVIDER_EVENT);
		} else {
			mapProvider.set(PROVIDER);
		}
	}
	public ObjectProperty<MapProvider> mapProviderProperty() {
		if (null == mapProvider) {
			mapProvider = new ObjectPropertyBase<MapProvider>(_mapProvider) {
				@Override protected void invalidated() { fireTileEvent(MAP_PROVIDER_EVENT); }
				@Override public Object getBean() { return this; }
				@Override public String getName() { return "mapProvider"; }
			};
			_mapProvider = null;
		}
		return mapProvider;
	}

	public void fireTileEvent(final MapEvent EVENT) {
		for (MapEventListener listener : listenerList) { listener.onTileEvent(EVENT); }
	}

}
