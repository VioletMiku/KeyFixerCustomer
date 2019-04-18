package com.keyfixer.customer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseError;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.keyfixer.customer.Common.Common;
import com.keyfixer.customer.Fragments.BottomSheetCustomerFragment;
import com.keyfixer.customer.Helper.CustomInfoWindow;
import com.keyfixer.customer.Model.DataMessage;
import com.keyfixer.customer.Model.FCMResponse;
import com.keyfixer.customer.Model.Token;
import com.keyfixer.customer.Model.User;
import com.keyfixer.customer.Remote.IFCMService;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.keyfixer.customer.Common.Common.fix_request_tbl;
import static com.keyfixer.customer.Common.Common.fixer_inf_tbl;
import static com.keyfixer.customer.Common.Common.fixer_tbl;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, View.OnClickListener, GoogleMap.OnInfoWindowClickListener {

    SupportMapFragment mapFragment;
    private GoogleMap mMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
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

    private BottomSheetCustomerFragment mBottomSheet;
    private Button btnPickupRequest, btnServiceFare;
    private TextView txtPickupSnippet;
    private TextView txtFixInfo;

    int radius = 1;
    int distance = 3;
    private static final int LIMIT = 5;
    private String KEY;
    //send alert
    IFCMService ifcmService;
    DatabaseReference fixerAvailable;

    //firebase storage
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //init firebase storage
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        ifcmService = Common.getFCMService();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this , drawer , toolbar , R.string.navigation_drawer_open , R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = navigationView.getHeaderView(0);
        TextView txt_Customer_Name = (TextView) navigationHeaderView.findViewById(R.id.txt_CustomerName);
        CircleImageView imgAvatar = (CircleImageView) navigationHeaderView.findViewById(R.id.imgAvatar);

        txt_Customer_Name.setText(Common.currentUser.getStrName());
        if (Common.currentUser.getAvatarUrl() != null && !TextUtils.isEmpty(Common.currentUser.getAvatarUrl()))
            Picasso.with(this).load(Common.currentUser.getAvatarUrl()).into(imgAvatar);

        /*
        * ref = FirebaseDatabas e.getInstance().getReference(fixer_tbl);
        geoFire = new GeoFire(ref);
        * */
        btnServiceFare = (Button) findViewById(R.id.ic_showup);
        mBottomSheet = BottomSheetCustomerFragment.newInstance("Customer bottom sheet");
        btnServiceFare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
            }
        });

        btnPickupRequest = (Button) findViewById(R.id.btn_GoiThoSuaKhoa);
        if (Common.isFixDone)
            btnPickupRequest.setText("Đặt thợ sửa khóa");
        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Common.isFixerFound){
                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(Account account) {
                            requestFixHere(account.getId());
                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {

                        }
                    });
                }
                else{
                    /*Common.sendRequestToFixer(Common.fixerid, ifcmService, HomeActivity.this, Common.mLastLocation);
                    Log.d("Fixer id","" + Common.fixerid);*/
                    Intent intent = new Intent(HomeActivity.this, CallFixer.class);
                    intent.putExtra("fixerId", Common.fixerid);
                    intent.putExtra("lat", Common.mLastLocation.getLatitude());
                    intent.putExtra("lng", Common.mLastLocation.getLongitude());
                    startActivity(intent);
                }
            }
        });

        setupLocation();
        UpdateFireBaseToken();
    }

    private void UpdateFireBaseToken() {
        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
            @Override
            public void onSuccess(Account account) {
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                DatabaseReference tokens = db.getReference(Common.token_tbl);
                Token token = new Token(FirebaseInstanceId.getInstance().getToken());
                tokens.child(account.getId()).setValue(token);
            }

            @Override
            public void onError(AccountKitError accountKitError) {

            }
        });
    }

    private void requestFixHere(String uid) {
        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(fix_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));

        if (mUserMarker.isVisible())
            mUserMarker.remove();
        //add new marker
        mUserMarker = mMap.addMarker(new MarkerOptions().title("Sửa ở đây").snippet("").position(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mUserMarker.showInfoWindow();

        btnPickupRequest.setText("Đang tìm thợ sửa khóa cho bạn");
        findFixer();
    }

    private void findFixer() {
        DatabaseReference fixers = FirebaseDatabase.getInstance().getReference(fixer_tbl);
        GeoFire gfFixer = new GeoFire(fixers);

        final GeoQuery geoQuery = gfFixer.queryAtLocation(new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key , GeoLocation location) {
                //if found
                if (!Common.isFixerFound){
                    Common.isFixerFound = true;
                    Common.fixerid = key;
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
                if (!Common.isFixerFound){
                    radius++;
                    findFixer();
                } else{
                    if (!Common.isFixerFound){
                        Toast.makeText(HomeActivity.this , "Không có thợ sửa khóa nào ở gần bạn" , Toast.LENGTH_SHORT).show();
                        btnPickupRequest.setText("Đặt thợ sửa khóa");
                        geoQuery.removeAllListeners();
                    }
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
                    setupLocation();
                }
                break;
        }
    }

    private void setupLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            //Request runtime permission
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE
            },MY_PERMISSION_REQUEST_CODE);
        }
        else{
            buildLocationCallBack();
            createLocationRequest();
            displayLocation();
        }
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Common.mLastLocation = locationResult.getLocations().get(locationResult.getLocations().size() - 1);
                displayLocation();
            }
        };
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                Common.mLastLocation = location;
                if (Common.mLastLocation != null){//co bug cho nay _ mlastLocation = null
                    //presense system
                    fixerAvailable = FirebaseDatabase.getInstance().getReference(Common.fixer_tbl);
                    fixerAvailable.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            //if has any change from fixer table, we will load all fixers available
                            loadAllAvailableFixer();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    //
                    final double latitude = Common.mLastLocation.getLatitude();
                    final double longtitude = Common.mLastLocation.getLongitude();
                    //Add marker
                    if (mUserMarker != null){
                        mUserMarker.remove(); //remove already marker
                    }
                    Log.d("message","/////////////////////////////////////////////////////");
                    mUserMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longtitude)).title("Bạn"));
                    //Move camera to this position
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longtitude),15.0f));
                    loadAllAvailableFixer();

                } else{
                    Log.d("Ối!", "Không thể xác định được vị trí của bạn");
                }
            }
        });
    }

    private void loadAllAvailableFixer() {
        //remove all marker, include fixer or customer
        mMap.clear();
        //after that, just add our location again
        mMap.addMarker(new MarkerOptions().position(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude())).title("Bạn"));
        DatabaseReference fixerLocation = FirebaseDatabase.getInstance().getReference(fixer_tbl);
        GeoFire gfLocation = new GeoFire(fixerLocation);

        GeoQuery geoQuery = gfLocation.queryAtLocation(new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), distance);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key , final GeoLocation location) {
                //use key to get email from table Users
                //table Users is a table contain fixer's information
                FirebaseDatabase.getInstance().getReference(Common.fixer_inf_tbl).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class); // user get null
                        Log.d("Key","" + key);
                        //add fixer to map
                        mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude))
                                                            .flat(true).title("Tên thợ sửa khóa: " + user.getStrName()).snippet("ID thợ sửa khóa: " + dataSnapshot.getKey())
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
        }

        return true;
    }

    private void signOut() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        else
            builder = new AlertDialog.Builder(this);

        builder.setMessage("Thật sự muốn thoát!?").setPositiveButton(android.R.string.ok , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog , int which) {
                AccountKit.logOut();
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }).setNegativeButton(android.R.string.cancel , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog , int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_signout) {
            signOut();
        } else if (id == R.id.nav_update_information) {
            ShowUpdateDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void ShowUpdateDialog() {
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(HomeActivity.this);
        alertdialog.setTitle("Thay đổi thông tin");
        alertdialog.setMessage("Xin hãy điền đầy đủ thông tin");

        LayoutInflater inflater = this.getLayoutInflater();
        View layout_changeInf = inflater.inflate(R.layout.layout_update_information, null);

        final MaterialEditText edtName = (MaterialEditText)layout_changeInf.findViewById(R.id.edt_Name);
        final MaterialEditText edtPhone = (MaterialEditText)layout_changeInf.findViewById(R.id.edt_Phone);
        final ImageView imgAvatar = (ImageView)layout_changeInf.findViewById(R.id.image_upload);
        imgAvatar.setOnClickListener(this);
        alertdialog.setView(layout_changeInf);

        //set button
        alertdialog.setPositiveButton("Cập nhật" , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog , int which) {
                dialog.dismiss();
                AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                    @Override
                    public void onSuccess(Account account) {
                        final AlertDialog waitingdialog = new SpotsDialog(HomeActivity.this);
                        waitingdialog.show();

                        String name = edtName.getText().toString();
                        String phone = edtPhone.getText().toString();

                        Map<String, Object> updateInfo = new HashMap<>();
                        if (!TextUtils.isEmpty(name))
                            updateInfo.put("strName", name);
                        if (!TextUtils.isEmpty(phone))
                            updateInfo.put("strPhone", phone);


                        DatabaseReference customerInformation = FirebaseDatabase.getInstance().getReference(Common.customer_tbl);
                        customerInformation.child(account.getId()).updateChildren(updateInfo)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful())
                                            Toast.makeText(HomeActivity.this , "Thông tin được thay đổi hoàn tất" , Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(HomeActivity.this , "Xin lỗi, đã có lỗi trong quá trình cập nhật thông tin" , Toast.LENGTH_SHORT).show();
                                        waitingdialog.dismiss();
                                    }
                                });
                    }

                    @Override
                    public void onError(AccountKitError accountKitError) {

                    }
                });
            }
        });
        alertdialog.setNegativeButton("Hủy" , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog , int which) {
                dialog.dismiss();
            }
        });
        alertdialog.show();
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
        mMap.setOnInfoWindowClickListener(this);
        buildLocationCallBack();
        createLocationRequest();
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.image_upload:
                chooseImage();
                break;
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn hình đại diện..."), Common.PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode , int resultCode , Intent data) {
        super.onActivityResult(requestCode , resultCode , data);
        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            Uri saveuri = data.getData();
            if (saveuri != null){
                final ProgressDialog mDialog = new ProgressDialog(this);
                mDialog.setMessage("Đang tải ... ");
                mDialog.show();

                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder = storageReference.child("images/" + imageName);
                imageFolder.putFile(saveuri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        mDialog.dismiss();
                        imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(final Uri uri) {
                                Toast.makeText(HomeActivity.this , "Đang tải ... " , Toast.LENGTH_SHORT).show();
                                AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                                    @Override
                                    public void onSuccess(Account account) {
                                        Map<String, Object> avatarUpdate = new HashMap<>();
                                        avatarUpdate.put("strAvatarUrl", uri.toString());
                                        DatabaseReference customerInformation = FirebaseDatabase.getInstance().getReference(Common.customer_tbl);
                                        customerInformation.child(account.getId()).updateChildren(avatarUpdate)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful())
                                                            Toast.makeText(HomeActivity.this , "Tải hoàn tất" , Toast.LENGTH_SHORT).show();
                                                        else
                                                            Toast.makeText(HomeActivity.this , "Tải thất bại" , Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onError(AccountKitError accountKitError) {

                                    }
                                });
                            }
                        });
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        mDialog.setMessage("Đã tải được " + progress + "%");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (!marker.getTitle().equals("Bạn")){
            Intent intent = new Intent(HomeActivity.this, CallFixer.class);
            intent.putExtra("fixerId", marker.getSnippet().replaceAll("\\D+", ""));
            intent.putExtra("lat", Common.mLastLocation.getLatitude());
            intent.putExtra("lng", Common.mLastLocation.getLongitude());
            startActivity(intent);
        }
    }
}
