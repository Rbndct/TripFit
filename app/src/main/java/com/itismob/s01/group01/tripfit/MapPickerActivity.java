package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.io.File;

/**
 * Drag-and-drop pin picker: an osmdroid map (free, OSM-based, no API key) centered on the
 * trip's selected city, with a draggable marker and tap selection. Confirmation returns
 * coordinates immediately so the frontend does not depend on a geocoding service.
 */
public class MapPickerActivity extends AppCompatActivity {

    public static final String EXTRA_CITY_LAT = "extra_city_lat";
    public static final String EXTRA_CITY_LNG = "extra_city_lng";
    public static final String EXTRA_RESULT_LAT = "extra_result_lat";
    public static final String EXTRA_RESULT_LNG = "extra_result_lng";
    public static final String EXTRA_RESULT_NAME = "extra_result_name";

    private static final double DEFAULT_ZOOM = 15.0;

    private MapView mapView;
    private Marker marker;
    private TextView coordinates;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("osmdroid_config", MODE_PRIVATE);
        Configuration.getInstance().load(this, prefs);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map_picker_root), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        double cityLat = savedInstanceState != null ? savedInstanceState.getDouble("pin_lat")
                : getIntent().getDoubleExtra(EXTRA_CITY_LAT, 14.5995);
        double cityLng = savedInstanceState != null ? savedInstanceState.getDouble("pin_lng")
                : getIntent().getDoubleExtra(EXTRA_CITY_LNG, 120.9842);
        GeoPoint center = new GeoPoint(cityLat, cityLng);

        mapView = findViewById(R.id.map_view);
        coordinates = findViewById(R.id.text_map_coordinates);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(center);

        marker = new Marker(mapView);
        marker.setPosition(center);
        marker.setDraggable(true);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(getString(R.string.map_picker_marker_hint));
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint point) {
                movePin(point);
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint point) {
                movePin(point);
                return true;
            }
        }));
        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override public void onMarkerDrag(Marker item) { updateCoordinates(); }
            @Override public void onMarkerDragStart(Marker item) { item.closeInfoWindow(); }
            @Override public void onMarkerDragEnd(Marker item) { updateCoordinates(); }
        });
        mapView.getOverlays().add(marker);
        mapView.getOverlays().add(new CopyrightOverlay(this));
        updateCoordinates();

        findViewById(R.id.button_use_location).setOnClickListener(v -> confirmLocation());
    }

    private void confirmLocation() {
        GeoPoint position = marker.getPosition();
        double lat = position.getLatitude();
        double lng = position.getLongitude();

        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_LAT, lat);
        result.putExtra(EXTRA_RESULT_LNG, lng);
        result.putExtra(EXTRA_RESULT_NAME, String.format(java.util.Locale.US,
                "Pinned location (%.5f, %.5f)", lat, lng));
        setResult(RESULT_OK, result);
        finish();
    }

    private void movePin(GeoPoint point) {
        marker.setPosition(point);
        marker.closeInfoWindow();
        updateCoordinates();
        mapView.invalidate();
    }

    private void updateCoordinates() {
        coordinates.setText(String.format(java.util.Locale.US,
                "Tap the map or hold and drag the pin\nSelected: %.5f, %.5f\nMap tiles need internet; pin selection does not.",
                marker.getPosition().getLatitude(), marker.getPosition().getLongitude()));
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putDouble("pin_lat", marker.getPosition().getLatitude());
        state.putDouble("pin_lng", marker.getPosition().getLongitude());
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
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
