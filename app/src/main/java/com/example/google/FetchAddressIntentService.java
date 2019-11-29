package com.example.google;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by lyd on 2017/1/7.
 *
 * @Description:地址查找服务
 */

public class FetchAddressIntentService extends IntentService {
    protected ResultReceiver mReceiver;
    private final String TAG = "FetchAddress";

    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public static final String PACKAGE_NAME =
            "com.example.mylocationdemo";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME +
            ".RESULT_DATA_KEY";
    public static final String LATLNG_DATA_EXTRA = PACKAGE_NAME +
            ".LATLNG_DATA_EXTRA";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FetchAddressIntentService(String name) {
        super(name);
    }

    public FetchAddressIntentService(){
        this("AddressIntentService");
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String errorMessage = "";

        // Get the location passed to this service through an extra.
        LatLng latLng = intent.getParcelableExtra(
                LATLNG_DATA_EXTRA);
        mReceiver = intent.getParcelableExtra(RECEIVER);

        List<Address> addresses = null;
        // 通过经纬度来获取地址，由于地址可能有多个，这和经纬度的精确度有关，本例限制最大返回数为1
        try {
            addresses = geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = "service_not_available";
            Log.e(TAG,errorMessage);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "invalid_lat_long_used";
            Log.e(TAG,errorMessage);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "no_address_found";
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
//
//            ArrayList<String> addressFragments = new ArrayList<String>();
//
//            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
//                //getAddressLine 名称
//                addressFragments.add(address.getAddressLine(i));
//            }
//            Log.i(TAG, "address_found");
//            deliverResultToReceiver(SUCCESS_RESULT,
//                    TextUtils.join(System.getProperty("line.separator"),
//                            addressFragments));


            //修改成以下
            Log.i("位置", "得到位置当前" + address + "'\n"
              + "经度：" + String.valueOf(address.getLongitude()) + "\n"
              + "纬度：" + String.valueOf(address.getLatitude()) + "\n"
              + "国家：" + address.getCountryName() + "\n"
              + "省："+address.getAdminArea() + "\n"
              + "城市：" + address.getLocality() + "\n"
              + "区：" + address.getSubLocality()+"\n"
              + "名称：" + address.getAddressLine(1)
            );

            //Address[addressLines=[0:"广东省广州市天河区兴民路221号",1:"保利中达广场",2:"广州汽车集团股份有限公司",3:"天河区猎德广物中心",4:"广物中心",5:"天汇广场",6:"天空别墅",7:"利民社区",8:"启赋幼儿园",9:"IGC天汇广场",10:"天台花园"],feature=null,admin=广东省,sub-admin=null,locality=广州市,thoroughfare=兴民路,postalCode=null,countryCode=,countryName=中国,hasLatitude=true,latitude=23.12204235990144,hasLongitude=true,longitude=113.3370748973398,phone=null,url=null,extras=null]'
            String s = address.getCountryName()+address.getAdminArea()+address.getLocality()+address.getAddressLine(1);//保利中达广场

            deliverResultToReceiver(SUCCESS_RESULT,s);
        }
    }
}
