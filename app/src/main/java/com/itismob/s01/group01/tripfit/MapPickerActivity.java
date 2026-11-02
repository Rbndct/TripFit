package com.itismob.s01.group01.tripfit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.itismob.s01.group01.tripfit.data.AppExecutors;
import com.itismob.s01.group01.tripfit.data.NominatimReverseGeocoder;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

/**
 * Drag-and-drop pin picker: an osmdroid map (free, OSM-based, no API key) centered on the
 * trip's selected city, with a draggable marker. On confirm, reverse-geocodes the dropped
 * pin via Nominatim (one request, not bulk) and returns the result to the caller.
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
    private final NominatimReverseGeocoder geocoder = new NominatimReverseGeocoder();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("osmdroid_config", MODE_PRIVATE);
        Configuration.getInstance().load(this, prefs);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(), "osmdroid/tiles"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        double cityLat = getIntent().getDoubleExtra(EXTRA_CITY_LAT, 0);
        double cityLng = getIntent().getDoubleExtra(EXTRA_CITY_LNG, 0);
        GeoPoint center = new GeoPoint(cityLat, cityLng);

        mapView = findViewById(R.id.map_view);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(center);

        marker = new Marker(mapView);
        marker.setPosition(center);
        marker.setDraggable(true);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(getString(R.string.map_picker_marker_hint));
        mapView.getOverlays().add(marker);

        findViewById(R.id.button_use_location).setOnClickListener(v -> confirmLocation());
    }

    private void confirmLocation() {
        GeoPoint position = marker.getPosition();
        double lat = position.getLatitude();
        double lng = position.getLongitude();

        AppExecutors.getInstance().diskIO().execute(() -> {
            String name = geocoder.reverseGeocode(lat, lng);
            AppExecutors.getInstance().mainThread().execute(() -> {
                Intent result = new Intent();
                result.putExtra(EXTRA_RESULT_LAT, lat);
                result.putExtra(EXTRA_RESULT_LNG, lng);
                result.putExtra(EXTRA_RESULT_NAME, name);
                setResult(RESULT_OK, result);
                finish();
            });
        });
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
