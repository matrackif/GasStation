package pl.edu.pw.student.mini.gasstation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.renderscript.Double2;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

/**
 * A fragment that launches other parts of the demo application.
 */

public class MapViewFragment extends Fragment implements OnMapReadyCallback {

    MapView mMapView;
    public GoogleMap googleMap;
    LocationManager locationManager;
    public LatLng loc;
    PlacesTask placesTask;
    private HashMap<String, String> gasStations = null;

    private GoogleApiClient mGoogleApiClient;
    FloatingActionButton theButton;

    public boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate and return the layout
        View v = inflater.inflate(R.layout.fragment_map, container,
                false);
        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();// needed to get the map to display immediately
        mMapView.getMapAsync(this);
        gasStations = new HashMap<>();
        theButton=(FloatingActionButton)v.findViewById(R.id.searchButton);



        if(mGoogleApiClient==null)
        {
            mGoogleApiClient = new GoogleApiClient
                    .Builder(getActivity())
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(getActivity(), new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Intent intent = new Intent(Settings.ACTION_SETTINGS);
                            startActivity(intent);
                            Toast.makeText(getActivity().getBaseContext(), "NO Internet!! Please open Wifi or MobileData",
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .build();


        }




        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }


        return v;
    }
    public static MapViewFragment newInstance() {
        MapViewFragment fragment = new MapViewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public StringBuilder sbMethod(LatLng location) {

        //use your current location here
        double mLatitude = location.latitude;
        double mLongitude = location.longitude;

        StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json");
        sb.append("?location=" + mLatitude + "," + mLongitude);
        sb.append("&radius=20000");
        sb.append("&keyword=" + "gas-station");
        sb.append("&key=AIzaSyCe3VBHxfxSFOly5Uzt1rhjmZ_f7oayd9A");

        Log.d("Map", "api: " + sb.toString());

        return sb;

    }



    @Override
    public void onMapReady(GoogleMap map) {
        if (ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("GPS_ERROR", "Could not get GPS permission");
            return;
        }

        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        this.googleMap = map;

        locationManager = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        final Location location = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);

        if (location != null) {
            mapInit(location);

        }
        locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 2000, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                loc = new LatLng(latitude, longitude);



                //googleMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
               // googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0f));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

                Toast.makeText(getActivity().getBaseContext(), "Gps is turned on!! ",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String provider) {


            }
        });
        this.googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                final Activity mapActivity = getActivity();
                final String markerTitle = marker.getTitle();
                Toast.makeText(mapActivity, "you clicked on: "+markerTitle, Toast.LENGTH_SHORT);
                        final EditText editText = new EditText(getActivity());
                        AlertDialog.Builder alert = new AlertDialog.Builder(mapActivity);
                        String markerSnippet = marker.getSnippet(); // this stores the price of the given gas station
                        if(markerSnippet == null || markerSnippet.equals("")){
                            // if marker snippet is null it means we have no price from the database yet for this station
                            alert.setMessage("Enter price to database for given marker? \n " + markerTitle + " No price found for this station");
                        }
                        else{
                            alert.setMessage("Enter price to database for given marker? \n " + markerTitle + " " + markerSnippet);
                        }


                        alert.setTitle("Manual input");
                        alert.setView(editText);
                        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                String price = editText.getText().toString();
                                if(isDouble(price)){
                                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                                    databaseReference.child(markerTitle).setValue(price);
                                }
                                else{
                                    Toast.makeText(mapActivity,"Invalid price input, please try again",Toast.LENGTH_SHORT);
                                }

                            }
                        });

                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // do nothing
                            }
                        });
                        alert.show();

                return true;
            }
        });

    }




    private void mapInit(Location location) {
        double latitude = location.getLatitude();
        double longitude=location.getLongitude();

        loc = new LatLng(latitude, longitude);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.getRoot().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("onDataChanged() " ,"NumOfChildren: " + dataSnapshot.getChildrenCount());
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    String value = snapshot.getValue(String.class);
                    String key = snapshot.getKey();
                    Log.i("onDataChanged() " ,"Key,value: " + key + ", " + value);
                    gasStations.put(key, value);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        findallStation();

        theButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findallStation();
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0f));

            }
        });


        googleMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 14.0f));

    }


    public void findallStation(){
        StringBuilder sbValue = new StringBuilder(sbMethod(loc));
        placesTask = new PlacesTask(googleMap, gasStations);
        if(isNetworkAvailable()) {
            placesTask.execute(sbValue.toString());
        }
        else
        {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            Toast.makeText(getActivity().getBaseContext(), "NO Internet!! Please open Wifi or MobileData",
                    Toast.LENGTH_LONG).show();

        }
    }
    @Override
    public void onStart(){
        super.onStart();
        if( mGoogleApiClient != null )
            mGoogleApiClient.connect();

    }
    @Override
    public void onStop() {
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }



}