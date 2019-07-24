package com.example.liandi.sensorapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//import gravity.TestSensor;
import java.text.SimpleDateFormat;
import java.util.Date;
//import com.fastaccess.permission.base.PermissionHelper;
//import com.fastaccess.permission.base.callback.OnPermissionCallback;

import gravity.save;

public class MainActivity extends AppCompatActivity implements LocationListener{
    private TextView mTextMessage;
    private float x, y, z, o;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String GPS_LOCATION_NAME = android.location.LocationManager.GPS_PROVIDER;
    private static final int REQUEST_PRESSMION_CODE = 10000;
    private final static String[] MULTI_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    private LocationManager locationManager;
    private boolean isGpsEnabled;
    private String locateType;
    private TextView textLocationShow;
    private Button btnLocation;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        getLocation();
    }

    public void  getLocation(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // String providerName = sensorManager.getBestProvider(Criteria criteria,boolean enabledOnly);
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.v("sensorAPP", "checkSelfPermission");
                if (
                        ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION)
                        ) {
                    Log.i("readTosdCard", "我们需要这个权限给你提供存储服务");
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            100);
                }
            }

            //判断是否有这个权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                //第一请求权限被取消显示的判断，一般可以不写
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.i("readTosdCard", "我们需要这个权限给你提供存储服务");
                    // showAlert();
                }else{
                    //2、申请权限: 参数二：权限的数组；参数三：请求码
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
        }

        locateType = locationManager.GPS_PROVIDER;
        boolean provide = locationManager.isProviderEnabled(GPS_LOCATION_NAME);
        Log.v("sensorAPP", (provide == true)?"true":"false");
        Log.v("sensorAPP", "begin get location");

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);//高精度
        criteria.setAltitudeRequired(false);//无海拔要求
        criteria.setBearingRequired(false);//无方位要求
        criteria.setCostAllowed(true);//允许产生资费
        criteria.setPowerRequirement(Criteria.POWER_LOW);//低功耗
        // 获取最佳服务对象
        String provider = locationManager.getBestProvider(criteria,true);
        provider = locationManager.GPS_PROVIDER;
        Log.v("sensorAPP", "provider = "+provider);
        mTextMessage.setText(" provider：" + provider);
        Location location = null;
       // while (location == null)
        {
            //SystemClock.sleep(5000);
            Log.v("sensorAPP", location==null?"null":"!null");

             location = locationManager.getLastKnownLocation(provider);
            mTextMessage.setText(" location：" + location==null?"null":"!null");
        }
        locationManager.requestLocationUpdates(locateType,0,0,this);
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i("sensorAPP", "时间：" + location.getTime());
        Log.i("sensorAPP", "经度：" + location.getLongitude());
        Log.i("sensorAPP", "纬度：" + location.getLatitude());
        Log.i("sensorAPP", "海拔：" + location.getAltitude());
        mTextMessage.setText(" 时间：" + location.getTime() + " 经度：" + location.getLongitude() + " 纬度：" + location.getLatitude()+"海拔："+location.getAltitude());
        String sensortext = new String("Loc" + "o" +  location.getTime() + "x:" + location.getLongitude() + "y:" +location.getLatitude() + "z" + location.getAltitude() + "\r\n");
        SavefileExample.writeToTXT(sensortext);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.v("sensorAPP", "onStatusChanged");
        Log.v("StatusChanged:provider:", provider);
        Log.v("onStatusChanged:status:", Integer.toString(status));
        mTextMessage.setText(" onStatusChanged：provider" + provider + " status：" + Integer.toString(status));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.v("sensorAPP", "onProviderEnabled");
        Log.v("Enabled:provider:", provider);
        mTextMessage.setText(" onProviderEnabled：provider" + provider );
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.v("sensorAPP", "onProviderDisabled");
        Log.v("Disabled:provider:", provider);
        mTextMessage.setText(" onProviderDisabled：provider" + provider );
    }
}
