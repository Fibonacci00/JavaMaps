package com.kutayacaar.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.kutayacaar.javamaps.R;
import com.kutayacaar.javamaps.databinding.ActivityMapsBinding;
import com.kutayacaar.javamaps.model.Place;
import com.kutayacaar.javamaps.roomdb.PlaceDao;
import com.kutayacaar.javamaps.roomdb.PlaceDataBase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
ActivityResultLauncher<String> permissionlauncher;
LocationManager locationManager;
LocationListener locationListener;
SharedPreferences sharedPreferences;
PlaceDataBase db;
PlaceDao placeDao;
boolean info;
Double selectedLat;
Double selectedLong;
private CompositeDisposable compositeDisposable = new CompositeDisposable();
Place selectedPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();
sharedPreferences= this.getSharedPreferences("com.kutayacaar.javamaps",MODE_PRIVATE);
info = false;
db = Room.databaseBuilder(getApplicationContext(),PlaceDataBase.class,"Places")
        //.allowMainThreadQueries()
        .build();
placeDao = db.placeDao();
selectedLat = 0.0;
selectedLong = 0.0;

        binding.saveButton.setEnabled(false);



    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
 mMap = googleMap;
 mMap.setOnMapLongClickListener(this);
 Intent intent = getIntent();
 String intentInfo = intent.getStringExtra("info");
 if (intentInfo.equals("new")){
binding.saveButton.setVisibility(View.VISIBLE);
binding.deleteButton.setVisibility(View.GONE);

//casting yani contexti bağlıyoruz
     locationManager =(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
     locationListener = new LocationListener() {
         @Override
         public void onLocationChanged(@NonNull Location location) {
             info = sharedPreferences.getBoolean("info",false);
             if (!info ){
                 LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                 mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                 sharedPreferences.edit().putBoolean("info",true).apply();
             }
         }
     };
     if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
         //request permission
         if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
             Snackbar.make(binding.getRoot(),"Haritalar için izin isteniyor",Snackbar.LENGTH_INDEFINITE).setAction("İzinin olmadan açamam", new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     permissionlauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
//request permission 2
                 }
             }).show();
         }else{
             permissionlauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
         }
     }else {
         locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
         Location lastLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
         if (lastLocation != null){
             LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
             mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
         }
         mMap.setMyLocationEnabled(true);
     }



 }else{
mMap.clear();
selectedPlace = (Place) intent.getSerializableExtra("place");
LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);
mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,16));
binding.placeNameText.setText(selectedPlace.name);
binding.saveButton.setVisibility(View.GONE);
binding.deleteButton.setVisibility(View.VISIBLE);
 }





    }
    private void registerLauncher(){
        permissionlauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
if(ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                    Location lastLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
                    if (lastLocation != null){
                        LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
                    }
                    //izin verildi
                }else{
                    //Permission denied
                    Toast.makeText(MapsActivity.this, "İzin lazım devam etmek için", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();
mMap.addMarker(new MarkerOptions().position(latLng));
selectedLat = latLng.latitude;
selectedLong = latLng.longitude;

binding.saveButton.setEnabled(true);
    }
    public void save(View view){
        Place place = new Place(binding.placeNameText.getText().toString(),selectedLat,selectedLong);
//placeDao.insert(place );
        compositeDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse)

        );
    }
    private void handleResponse(){
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void delete(View view){
if (selectedPlace != null ) {
    compositeDisposable.add(placeDao.delete(selectedPlace)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(MapsActivity.this::handleResponse)
    );
}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
compositeDisposable.clear();
    }
}