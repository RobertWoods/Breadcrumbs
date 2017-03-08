package edu.temple.breadcrumbs;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity {

    GoogleMap gMap;
    GoogleApiClient mGoogleApiClient = null;
    LocationManager lm;

    Location oldLocation = null;
    private boolean toggleFollow = false;
    private boolean toggleDrop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment map = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap = googleMap;
            }
        });
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                            setupLocationListener();
                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
        setupButtons();
//        setupLocationListener();
    }

    LatLng getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permissions", "Bad permissions");
        }
        Log.d("Api", mGoogleApiClient.isConnected() + "");
        Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (l == null) {
            Toast.makeText(getBaseContext(), "Failed to get Location", Toast.LENGTH_SHORT).show();
            return null;
        }
        oldLocation = l;
        return new LatLng(l.getLatitude(), l.getLongitude());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void setupButtons() {
        findViewById(R.id.clear_markers_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gMap.clear();
            }
        });
        findViewById(R.id.place_marker_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng latLng = getLocation();
                if (latLng != null)
                    gMap.addMarker(new MarkerOptions().position(latLng));

            }
        });
        findViewById(R.id.toggle_autodrop_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDrop = !toggleDrop;
                Toast.makeText(getBaseContext(), "Dropping Crumbs: " + toggleDrop, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.toggle_follow_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!toggleFollow && oldLocation!=null) {
                    gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(oldLocation.getLatitude(),
                            oldLocation.getLongitude())));
                }
                gMap.getUiSettings().setScrollGesturesEnabled(toggleFollow);
                gMap.getUiSettings().setZoomGesturesEnabled(toggleFollow);
                gMap.getUiSettings().setRotateGesturesEnabled(toggleFollow);
                toggleFollow = !toggleFollow;
                Toast.makeText(getBaseContext(), "Following User: " + toggleFollow, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void setupLocationListener(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                LocationRequest.create().setInterval(1000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(5), new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(toggleFollow){
                    gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),
                            location.getLongitude())));
                }
                if(toggleDrop){
                    if(oldLocation!=null){
                        if(location.distanceTo(oldLocation)>=5){
                            gMap.addMarker(new MarkerOptions().position(new LatLng(
                                    location.getLatitude(), location.getLongitude()
                            )));
                        }
                    }
                }
                oldLocation = location;
            }
        });
    }

}
