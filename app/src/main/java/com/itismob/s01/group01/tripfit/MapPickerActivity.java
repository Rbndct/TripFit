package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

/**
 * Drag-and-drop pin picker: an osmdroid map (free, OSM-based, no API key) centered on the
 * trip's selected city. Tap, long-press, or drag to select a pin. Confirmation returns
 * the coordinates immediately without depending on a geocoding service.
 */
public class MapPickerActivity extends ScreenActivity {

    public static final String EXTRA_CITY_LAT = "extra_city_lat";
    public static final String EXTRA_CITY_LNG = "extra_city_lng";
    public static final String EXTRA_RESULT_LAT = "extra_result_lat";
    public static final String EXTRA_RESULT_LNG = "extra_result_lng";
    public static final String EXTRA_RESULT_NAME = "extra_result_name";

    private static final String STATE_PIN_LAT = "state_pin_lat";
    private static final String STATE_PIN_LNG = "state_pin_lng";
    private static final double DEFAULT_ZOOM = 15.0;

    private MapView mapView;
    private Marker marker;
    private TextView coordinatesText;
    private MaterialButton useLocationButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("osmdroid_config", MODE_PRIVATE);
        Configuration.getInstance().load(this, prefs);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_map_picker, R.string.map_picker_title);

        coordinatesText = findViewById(R.id.text_coordinates);
        useLocationButton = findViewById(R.id.button_use_location);

        GeoPoint center = new GeoPoint(
                getIntent().getDoubleExtra(EXTRA_CITY_LAT, 14.5995),
                getIntent().getDoubleExtra(EXTRA_CITY_LNG, 120.9842));
        GeoPoint pin = center;
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_PIN_LAT)) {
            pin = new GeoPoint(savedInstanceState.getDouble(STATE_PIN_LAT),
                    savedInstanceState.getDouble(STATE_PIN_LNG));
        }

        setUpMap(pin);
        useLocationButton.setOnClickListener(v -> confirmLocation());
    }

    private void setUpMap(GeoPoint pin) {
        mapView = findViewById(R.id.map_view);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(pin);

        // Added first so the marker (above it) gets touches before a tap places the pin.
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                marker.setPosition(p);
                updateCoordinates(p);
                mapView.invalidate();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                marker.setPosition(p);
                updateCoordinates(p);
                mapView.invalidate();
                return true;
            }
        }));

        marker = new Marker(mapView);
        marker.setPosition(pin);
        marker.setDraggable(true);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_pin_40));
        marker.setInfoWindow(null);
        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker m) {
                updateCoordinates(m.getPosition());
            }

            @Override
            public void onMarkerDragEnd(Marker m) {
                updateCoordinates(m.getPosition());
            }

            @Override
            public void onMarkerDragStart(Marker m) {
                // coordinates update while dragging and on drop
            }
        });
        mapView.getOverlays().add(marker);
        mapView.getOverlays().add(new CopyrightOverlay(this));
        updateCoordinates(pin);
    }

    private void updateCoordinates(GeoPoint point) {
        coordinatesText.setText(getString(R.string.map_picker_coords,
                point.getLatitude(), point.getLongitude()));
    }

    private void confirmLocation() {
        GeoPoint position = marker.getPosition();
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_LAT, position.getLatitude());
        result.putExtra(EXTRA_RESULT_LNG, position.getLongitude());
        result.putExtra(EXTRA_RESULT_NAME, String.format(java.util.Locale.US,
                "Pinned location (%.5f, %.5f)", position.getLatitude(), position.getLongitude()));
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        GeoPoint position = marker.getPosition();
        outState.putDouble(STATE_PIN_LAT, position.getLatitude());
        outState.putDouble(STATE_PIN_LNG, position.getLongitude());
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDetach();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
