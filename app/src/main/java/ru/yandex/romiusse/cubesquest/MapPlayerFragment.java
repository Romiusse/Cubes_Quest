package ru.yandex.romiusse.cubesquest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

import java.util.Objects;

public class MapPlayerFragment extends Fragment {

    MapView mapView;
    GoogleMap map;
    public static double latitude, longitude;
    public static String name;
    public static int idto;
    public static float distance;
    public static boolean isNew = true;
    public static FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map_player, container, false);


        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) v.findViewById(R.id.playermapview);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this::onMapReady);


        return v;
    }

    private class UpdatePlayerLayout4 extends AsyncTask<Void, Integer, Void> {

        public boolean isRunning = true;
        long lastTime = 0;

        @Override
        protected Void doInBackground(Void... voids) {
            while (MapPlayerFragment.isNew) {
                if (System.currentTimeMillis() - lastTime > 500) {
                    lastTime = System.currentTimeMillis();
                    publishProgress(23);
                }
            }
            return null;
        }

        @SuppressLint("MissingPermission")
        @Override
        protected void onProgressUpdate(Integer... values) {
            if(MapPlayerFragment.isNew)
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            float[] result = new float[1];
                            Location.distanceBetween(location.getLatitude(), location.getLongitude(), latitude, longitude, result);
                            MapPlayerFragment.distance = result[0];
                            //map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                        }
                        Log.println(Log.ERROR, "LOG", Float.toString(MapPlayerFragment.distance));
                    });
        }
    }

    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        float[] result = new float[1];
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(), latitude, longitude, result);
                        MapPlayerFragment.distance = result[0];
                        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                    }
                    Log.println(Log.ERROR, "LOG", Float.toString(MapPlayerFragment.distance));
                });
        new UpdatePlayerLayout4().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(MapPlayerFragment.latitude, MapPlayerFragment.longitude));
        markerOptions.title(name);
        map.clear();
        map.addMarker(markerOptions);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(MapPlayerFragment.latitude, MapPlayerFragment.longitude), 15f));

    }


    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}