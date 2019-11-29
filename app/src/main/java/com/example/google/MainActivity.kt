package com.example.google

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.location.Geocoder
import android.location.Location
import android.media.browse.MediaBrowser
import android.os.Handler
import android.os.ResultReceiver
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker

class MainActivity : AppCompatActivity(),
  OnMapReadyCallback ,
  GoogleMap.OnMyLocationButtonClickListener,
  GoogleApiClient.ConnectionCallbacks,
  GoogleMap.OnMarkerDragListener,
  GoogleApiClient.OnConnectionFailedListener,
  ActivityCompat.OnRequestPermissionsResultCallback {


  private val LOCATION_PERMISSION_REQUEST_CODE = 1
  private var mMap: GoogleMap? = null
  private var mLastLocation: Location? = null
  private var mGoogleApiClient: GoogleApiClient? = null
  private var mResultReceiver: AddressResultReceiver? = null

  private var mPermissionDenied = false

  /**
   * 用来判断用户在连接上Google Play services之前是否有请求地址的操作
   */
  private var mAddressRequested: Boolean = false
  /**
   * 地图上锚点
   */
  private var perth: Marker? = null
  private var lastLatLng: LatLng? = null
  private var perthLatLng: LatLng? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

//    var mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
//    mapFragment.getMapAsync(this)

    //接收FetchAddressIntentService返回的结果
    mResultReceiver = AddressResultReceiver(Handler())

    //创建GoogleAPIClient实例
    if (mGoogleApiClient == null) {
      mGoogleApiClient = GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build()
      Log.e("eee","mGoogleApiClient创建")
    }

    var mMap: MapView = findViewById(R.id.mapview)
    mMap.onCreate(savedInstanceState)
    mMap.onResume()

    try {
      MapsInitializer.initialize(this)

    }
    catch (e: Exception) {
      e.printStackTrace()
    }

    var errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
    if(ConnectionResult.SUCCESS != errorCode){
      GooglePlayServicesUtil.getErrorDialog(errorCode,this,0).show()
    }else{
      mMap.getMapAsync(this)
    }
  }


  override fun onConnected(p0: Bundle?) {
    Log.e("eee", "--onConnected--")
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(applicationContext, "Permission to access the location is missing.", Toast.LENGTH_LONG).show()
      return
    }
    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
    if (mLastLocation != null) {
      lastLatLng = LatLng(mLastLocation!!.getLatitude(), mLastLocation!!.getLongitude())
      Log.e("eee","lat=="+mLastLocation?.latitude+"lng=="+mLastLocation?.longitude)
      displayPerth(true, lastLatLng!!)
      initCamera(lastLatLng!!)

      if (!Geocoder.isPresent()) {
        Toast.makeText(this, "No geocoder available", Toast.LENGTH_LONG).show()
        return
      }
      if (mAddressRequested) {
        startIntentService(LatLng(mLastLocation!!.getLatitude(), mLastLocation!!.getLongitude()))
      }
    }
  }

  override fun onConnectionSuspended(p0: Int) {

  }

  override fun onMarkerDragEnd(marker: Marker?) {
    perthLatLng = marker?.getPosition()
    startIntentService(perthLatLng!!)
  }

  override fun onMarkerDragStart(p0: Marker?) {

  }

  override fun onMarkerDrag(p0: Marker?) {

  }

  override fun onConnectionFailed(p0: ConnectionResult) {

  }

  protected override fun onStart() {
    mGoogleApiClient?.connect()
    Log.e("eee"," mGoogleApiClient?.connect()")
    super.onStart()
  }

  protected override fun onStop() {
    mGoogleApiClient?.disconnect()
    Log.e("eee","mGoogleApiClient?.disconnect()")
    super.onStop()
  }


  override fun onMapReady(googleMap: GoogleMap?) {

    //测试
//    var sydney = LatLng(-34.0,151.0)
//    googleMap?.addMarker( MarkerOptions().position(sydney).title("Marker in Sydney"));
//    googleMap?.moveCamera(CameraUpdateFactory.newLatLng(sydney));


    mMap = googleMap
    mMap?.setOnMyLocationButtonClickListener(this)
    mMap?.setOnMarkerDragListener(this)
    enableMyLocation()
  }


  /**
   * 如果取得了权限,显示地图定位层
   */
  private fun enableMyLocation() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      // Permission to access the location is missing.
      PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
        Manifest.permission.ACCESS_FINE_LOCATION, true)
    }
    else if (mMap != null) {
      // Access to the location has been granted to the app.
      mMap?.setMyLocationEnabled(true)
    }
  }

  /**
   * '我的位置'按钮点击时的调用
   * @return
   */
  override fun onMyLocationButtonClick(): Boolean {
    Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show()
    // Return false so that we don't consume the event and the default behavior still occurs
    // (the camera animates to the user's current position).

    //修改 注释掉
//    if (mLastLocation != null) {
//      Log.e("eee", "Latitude-->" + mLastLocation!!.getLatitude().toString())
//      Log.e("eee", "Longitude-->" + mLastLocation!!.getLongitude().toString())
//    }
//    if (lastLatLng != null)
//      perth?.setPosition(lastLatLng!!)
//    checkIsGooglePlayConn()

    return false
  }

  /**
   * 检查是否已经连接到 Google Play services
   */
  private fun checkIsGooglePlayConn() {
    Log.e("eee", "checkIsGooglePlayConn-->" + mGoogleApiClient?.isConnected())
    if (mGoogleApiClient!!.isConnected() && mLastLocation != null) {
      startIntentService(LatLng(mLastLocation!!.getLatitude(), mLastLocation!!.getLongitude()))
    }
    mAddressRequested = true
  }

  /**
   * 启动地址搜索Service
   */
  protected fun startIntentService(latLng: LatLng) {
    val intent = Intent(this, FetchAddressIntentService::class.java)
    intent.putExtra(FetchAddressIntentService.RECEIVER, mResultReceiver)
    intent.putExtra(FetchAddressIntentService.LATLNG_DATA_EXTRA, latLng)
    startService(intent)
  }

  override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
    if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
      Toast.makeText(applicationContext, "Permission to access the location is missing.", Toast.LENGTH_LONG).show()
      return
    }
    if (PermissionUtils.isPermissionGranted(permissions, grantResults,
        Manifest.permission.ACCESS_FINE_LOCATION)) {
      // Enable the my location layer if the permission has been granted.
      enableMyLocation()
    }
    else {
      // Display the missing permission error dialog when the fragments resume.
      mPermissionDenied = true
    }
  }

  protected override fun onResumeFragments() {
    super.onResumeFragments()
    if (mPermissionDenied) {
      // Permission was not granted, display error dialog.
      showMissingPermissionError()
      mPermissionDenied = false
    }
  }

  private fun showMissingPermissionError() {
    PermissionUtils.PermissionDeniedDialog
      .newInstance(true).show(supportFragmentManager, "dialog")
  }

  internal inner class AddressResultReceiver(handler: Handler) : ResultReceiver(handler) {
    private var mAddressOutput: String? = null

    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {

      mAddressOutput = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY)//中国广东省广州市保利中达广场
      if (resultCode == FetchAddressIntentService.SUCCESS_RESULT) {
        Toast.makeText(this@MainActivity,mAddressOutput, Toast.LENGTH_SHORT).show()
        Log.e("eee", "mAddressOutput-->" + mAddressOutput!!)
        //弹窗
//        AlertDialog.Builder(this@MainActivity)
//          .setTitle("Position")
//          .setMessage(mAddressOutput)
//          .create()
//          .show()
      }

    }
  }

  /**
   * 添加标记
   */
  private fun displayPerth(isDraggable: Boolean, latLng: LatLng) {
    if (perth == null) {
      perth = mMap?.addMarker(MarkerOptions().position(latLng).title("Your Position"))
      perth?.setDraggable(isDraggable) //设置可移动
    }

  }

  /**
   * 将地图视角切换到定位的位置
   */
  private fun initCamera(sydney: LatLng) {
    Thread(Runnable {
      try {
        Thread.sleep(500)
      }
      catch (e: InterruptedException) {
        e.printStackTrace()
      }

      runOnUiThread {
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 14f))

        //自己添加 测试
        if (mLastLocation != null) {
          Log.e("eee", "Latitude-->" + mLastLocation!!.getLatitude().toString())
          Log.e("eee", "Longitude-->" + mLastLocation!!.getLongitude().toString())
        }
        if (lastLatLng != null)
          perth?.setPosition(lastLatLng!!)
        checkIsGooglePlayConn()



      }
    }).start()
  }
}
