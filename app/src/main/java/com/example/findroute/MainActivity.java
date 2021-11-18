package com.example.findroute;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.findroute.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_CODE = 50;
    private GoogleMap mMap;
    private Polyline currentPolyline;
    ActivityMainBinding binding;
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentLat = -1, currentLong = -1;
    SupportMapFragment mapFragment;
    private static final int REQUEST_CHECK_SETTINGS = 10;
    private Geocoder geocoder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapNearBy);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        binding.btnGetDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                 This is for showing route within this map
//                new FetchURL(MainActivity.this).execute(getUrl(place1.getPosition(), place2.getPosition(), "driving"), "driving");
                checkField();
            }
        });

        binding.destination.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    checkField();
                    return true;
                }
                return false;
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();

        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);
        }

        binding.swipeAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (binding.source.getText().toString().trim().isEmpty() &&
                            binding.destination.getText().toString().trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Enter Location", Toast.LENGTH_SHORT).show();
                    } else {
                        String s = binding.source.getText().toString().trim();
                        String d = binding.destination.getText().toString().trim();
                        binding.source.setText(d);
                        binding.destination.setText(s);
                    }
                } catch (NullPointerException w) {
                    Toast.makeText(MainActivity.this, "Enter Location", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void checkField() {
        try {
            if (binding.source.getText().toString().trim().isEmpty() &&
                    binding.destination.getText().toString().trim().isEmpty()) {
                Toast.makeText(MainActivity.this, "Enter Location", Toast.LENGTH_SHORT).show();
            } else {
                DisplayTrack(binding.source.getText().toString().trim(),
                        binding.destination.getText().toString().trim());
            }
        } catch (NullPointerException w) {
            Toast.makeText(MainActivity.this, "Enter Location", Toast.LENGTH_SHORT).show();
        }
    }

    private void DisplayTrack(String source, String destination) {
        try {
            Uri uri = Uri.parse("https://www.google.co.in/maps/dir/" + source + "/" + destination);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }


    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLong = location.getLongitude();
                    if (mapFragment != null) {
                        mapFragment.getMapAsync(MainActivity.this);
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(getTheAddress(latLng.latitude, latLng.longitude));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
                mMap.addMarker(markerOptions);
                detectSearch(getTheAddress(latLng.latitude, latLng.longitude));
            }
        });
        if (currentLat != -1 && currentLong != -1) {
            LatLng latLng = new LatLng(currentLat, currentLong);

            MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                    .title("My Location: ")
                    .snippet(getTheAddress(currentLat, currentLong));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
            mMap.addMarker(markerOptions);
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(@NonNull Marker marker) {
                    if (currentLat == marker.getPosition().latitude &&
                            currentLong == marker.getPosition().longitude) {
                        detectSearch(getTheAddress(currentLat, currentLong));
                    }
                    return false;
                }
            });
        }

    }

    private void detectSearch(String theAddress) {
        if (binding.source.isFocused()) {
            binding.source.setText(theAddress);
        } else if (binding.destination.isFocused()) {
            binding.destination.setText(theAddress);
        } else {
            binding.source.setText(getTheAddress(currentLat, currentLong));
        }
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    private String getTheAddress(double latitude, double longitude) {
        List<Address> addresses;
        String address = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();

            return address;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapFragment.onDestroy();
    }
}
