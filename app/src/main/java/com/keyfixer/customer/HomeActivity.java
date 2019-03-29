package com.keyfixer.customer;

import android.Manifest;
import android.os.Bundle;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.keyfixer.customer.Common.Common;
import com.keyfixer.customer.Fragments.BottomSheetCustomerFragment;
import com.keyfixer.customer.Helper.CustomInfoWindow;
import com.keyfixer.customer.Model.Notification;
import com.keyfixer.customer.Model.FCMResponse;
import com.keyfixer.customer.Model.Sender;
import com.keyfixer.customer.Model.Token;
import com.keyfixer.customer.Model.User;
import com.keyfixer.customer.Remote.IFCMService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.keyfixer.customer.Common.Common.fix_request_tbl;
import static com.keyfixer.customer.Common.Common.fixer_inf_tbl;
import static com.keyfixer.customer.Common.Common.fixer_tbl;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    SupportMapFragment mapFragment;
    private GoogleMap mMap;
    //play services
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;
    private static final int PLAY_SERVICE_RES_REQUEST = 300193;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleAPiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference ref;
    GeoFire geoFire;
    Marker mUserMarker;

    private ImageView imgExpandable;
    private BottomSheetCustomerFragment mBottomSheet;
    private Button btnPickupRequest;
    private TextView txtPickupSnippet;
    private TextView txtFixInfo;

    boolean isFixerFound = false;
    String fixerid = "";
    int radius = 1;
    int distance = 3;
    private static final int LIMIT = 5;
    private String KEY;
    //send alert
    IFCMService ifcmService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ifcmService = Common.getFCMService();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this , drawer , toolbar , R.string.navigation_drawer_open , R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /*
        * ref = FirebaseDatabas e.getInstance().getReference(fixer_tbl);
        geoFire = new GeoFire(ref);
        * */

        imgExpandable = (ImageView) findViewById(R.id.ic_showup);
        mBottomSheet = BottomSheetCustomerFragment.newInstance("Customer bottom sheet");
        imgExpandable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
            }
        });

        btnPickupRequest = (Button) findViewById(R.id.btn_GoiThoSuaKhoa);
        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFixerFound)
                    requestFixHere(FirebaseAuth.getInstance().getCurrentUser().getUid());
                else{
                    sendRequestToFixer(fixerid);
                    Log.d("Fixer id","" + fixerid);
                }
            }
        });

        setupLocation();
        UpdateFireBaseToken();
    }

    private void UpdateFireBaseToken() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);
        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
    }

    private void sendRequestToFixer(String fixerid) {
        DatabaseReference token = FirebaseDatabase.getInstance().getReference(Common.token_tbl);
        token.orderByKey().equalTo(fixerid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot:dataSnapshot.getChildren()){
                    Token token1 = postSnapshot.getValue(Token.class); // get token object from database with key
                    //make raw payload - convert latlng to json
                    String json_lat_lng = new Gson().toJson(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                    Notification notification = new Notification("MikuRoot",json_lat_lng); // send it to fixer app and we'll deserialize it again
                    Sender content = new Sender(notification , token1.getToken()); // send this notification to token
                    ifcmService.sendMessage(content).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call , Response<FCMResponse> response) {
                            if (response.body().success == 1)
                                Toast.makeText(HomeActivity.this , "Đã gửi yêu cầu" , Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(HomeActivity.this , "Gửi yêu cầu thất bại" , Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call , Throwable t) {
                            Log.e("ERROR",t.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void requestFixHere(String uid) {
        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(fix_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

        if (mUserMarker.isVisible())
            mUserMarker.remove();
        //add new marker
        mUserMarker = mMap.addMarker(new MarkerOptions().title("Sửa ở đây").snippet("").position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mUserMarker.showInfoWindow();

        btnPickupRequest.setText("Đang tìm thợ sửa khóa cho bạn");
        findFixer();
    }

    private void findFixer() {
        DatabaseReference fixers = FirebaseDatabase.getInstance().getReference(fixer_tbl);
        GeoFire gfFixer = new GeoFire(fixers);

        GeoQuery geoQuery = gfFixer.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key , GeoLocation location) {
                //if found
                if (!isFixerFound){
                    isFixerFound = true;
                    fixerid = key;
                    btnPickupRequest.setText("Gọi cho thợ sửa khóa");
                    Toast.makeText(HomeActivity.this , "Đã tìm thấy!" , Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key , GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //if still not found fixer, increase distance
                if (!isFixerFound){
                    radius++;
                    findFixer();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (checkPlaySerives()){
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    private void setupLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //Request runtime permission
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }
        else{
            if (checkPlaySerives()){
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    private void displayLocation() {if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
        return;
    }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPiClient);
        if (mLastLocation != null){//co bug cho nay _ mlastLocation = null
            final double latitude = mLastLocation.getLatitude();
            final double longtitude = mLastLocation.getLongitude();
            //Add marker
            if (mUserMarker != null){
                mUserMarker.remove(); //remove already marker
            }
            Log.d("message","/////////////////////////////////////////////////////");
            mUserMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longtitude)).title("Bạn"));
            //Move camera to this position
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longtitude),15.0f));
            loadAllAvailableFixer();

        }
        else{
            Log.d("Ối!", "Không thể xác định được vị trí của bạn");
            Toast.makeText(this, "Bạn bật GPS chưa nhỉ ?!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAllAvailableFixer() {
        DatabaseReference fixerLocation = FirebaseDatabase.getInstance().getReference(fixer_tbl);
        GeoFire gfLocation = new GeoFire(fixerLocation);

        GeoQuery geoQuery = gfLocation.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), distance);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key , final GeoLocation location) {
                //use key to get email from table Users
                //table Users is a table contain fixer's information
                FirebaseDatabase.getInstance().getReference(fixer_inf_tbl).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class); // user get null
                        Log.d("Key","" + key);
                        //add fixer to map
                        mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude))
                                                            .flat(true).title(user.getStrName() + "\n" + "Số điện thoại: " + user.getStrPhone())
                                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                        KEY = key;
                        Log.d("key","" + KEY);
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key , GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <= LIMIT){
                    distance++;
                    loadAllAvailableFixer();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode , KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            FirebaseDatabase.getInstance().getReference(fixer_inf_tbl).child(KEY).removeValue();

            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleAPiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleAPiClient.connect();
    }

    private boolean checkPlaySerives() {
        int result_code = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result_code != ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(result_code))
                GooglePlayServicesUtil.getErrorDialog(result_code, this, PLAY_SERVICE_RES_REQUEST).show();
            else{
                Toast.makeText(this, "Thiết bị này chưa hỗ trợ google play services", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_activity2 , menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        /*
        googleMap.addMarker(new MarkerOptions().position(new LatLng(37.7750,122.4183)).title("San Francisco"));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7750,122.4183), 12));
        */
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdate();
    }

    private void startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleAPiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this , "Cyka blyat! The fucking capitalism take over the internet .. take them now! ☭" , Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }
}
