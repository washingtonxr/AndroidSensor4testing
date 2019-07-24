package com.location.sensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    SensorManager mSensorManager;
    private TextView tv_1;
    private TextView tv_2;
    private TextView tv_3;
    private TextView tv_4;
    private TextView tv_5;
    private TextView tv_6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            //  判断是否有这个权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //第一请求权限被取消显示的判断，一般可以不写
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.i("readTosdCard", "我们需要这个权限给你提供存储服务");
                    // showAlert();
                } else {
                    //2、申请权限: 参数二：权限的数组；参数三：请求码
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            } else {
                //   String sensortext = new String(a + "acc" + "x" + event.values[0] + event.values[1] + event.values[2] + "\r\n");
                //   SavefileExample.writeToTXT(sensortext);
            }
        } else {
            // String sensortext = new String(a + "acc" + "x" + event.values[0] + event.values[1] + event.values[2] + "\r\n");
            //      SavefileExample.writeToTXT(sensortext);
        }

        tv_1 = (TextView) this.findViewById(R.id.tv_1);
        tv_2 = (TextView) findViewById(R.id.tv_2);
        tv_3 = (TextView) findViewById(R.id.tv_3);
        tv_4 = (TextView) findViewById(R.id.tv_4);
        tv_5 = (TextView) findViewById(R.id.tv_5);
        tv_6 = (TextView) findViewById(R.id.tv_6);

        // 获取传感器管理对象
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 为加速度传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        // 为方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
        // 为陀螺仪传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
        // 为磁场传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        // 为重力传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_GAME);
        // 为线性加速度传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 取消监听
        mSensorManager.unregisterListener(this);
    }

    // 当传感器的值改变的时候回调该方法
    @Override
    public void onSensorChanged(SensorEvent event) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String a=df.format(new Date());
        float[] values = event.values;

        // 获取传感器类型
        int type = event.sensor.getType();
        StringBuilder sb;
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                sb = new StringBuilder();
                sb.append("加速度传感器返回数据：");
                sb.append("\nX方向的加速度：");
                sb.append(values[0]);
                sb.append("\nY方向的加速度：");
                sb.append(values[1]);
                sb.append("\nZ方向的加速度：");
                sb.append(values[2]);
                tv_1.setText(sb.toString());
                String sensortext= new String(a+"acc"+"  "+event.values[0]  + "  "+ event.values[1]   + "  "+ event.values[2]+"\r\n");
                Save.writeToTXT("acc.txt",sensortext);
                break;
            case Sensor.TYPE_ORIENTATION:
                sb = new StringBuilder();
                sb.append("\n方向传感器返回数据：");
                sb.append("\n绕Z轴转过的角度：");
                sb.append(values[0]);
                sb.append("\n绕X轴转过的角度：");
                sb.append(values[1]);
                sb.append("\n绕Y轴转过的角度：");
                sb.append(values[2]);
                tv_2.setText(sb.toString());
                String orientation= new String(a+"ori" + " "+event.values[0]  + " "+ event.values[1]   + " "+ event.values[2]+"\r\n");
                Save.writeToTXT("ori.txt",orientation);
                break;
            case Sensor.TYPE_GYROSCOPE:
                sb = new StringBuilder();
                sb.append("\n陀螺仪传感器返回数据：");
                sb.append("\n绕X轴旋转的角速度：");
                sb.append(values[0]);
                sb.append("\n绕Y轴旋转的角速度：");
                sb.append(values[1]);
                sb.append("\n绕Z轴旋转的角速度：");
                sb.append(values[2]);
                tv_3.setText(sb.toString());
                String gyroscope= new String(a+"gyr" + " "+event.values[0]  + " "+ event.values[1]   + " "+ event.values[2]+"\r\n");
                Save.writeToTXT("gyr.txt",gyroscope);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sb = new StringBuilder();
                sb.append("\n磁场传感器返回数据：");
                sb.append("\nX轴方向上的磁场强度：");
                sb.append(values[0]);
                sb.append("\nY轴方向上的磁场强度：");
                sb.append(values[1]);
                sb.append("\nZ轴方向上的磁场强度：");
                sb.append(values[2]);
                tv_4.setText(sb.toString());
                String magnetic= new String(a+"mag" + " "+event.values[0]  + " "+ event.values[1]   + " "+ event.values[2]+"\r\n");
                Save.writeToTXT("mag.txt",magnetic);
                break;
            case Sensor.TYPE_GRAVITY:
                sb = new StringBuilder();
                sb.append("\n重力传感器返回数据：");
                sb.append("\nX轴方向上的重力：");
                sb.append(values[0]);
                sb.append("\nY轴方向上的重力：");
                sb.append(values[1]);
                sb.append("\nZ轴方向上的重力：");
                sb.append(values[2]);
                tv_5.setText(sb.toString());
                String gravity= new String(a+"gra" + " "+event.values[0]  + " "+ event.values[1]   + " "+ event.values[2]+"\r\n");
                Save.writeToTXT("gra.txt",gravity);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                sb = new StringBuilder();
                sb.append("\n线性加速度传感器返回数据：");
                sb.append("\nX轴方向上的线性加速度：");
                sb.append(values[0]);
                sb.append("\nY轴方向上的线性加速度：");
                sb.append(values[1]);
                sb.append("\nZ轴方向上的线性加速度：");
                sb.append(values[2]);
                tv_6.setText(sb.toString());
                String lineraacc= new String(a+"Lacc" + " "+event.values[0]  + " "+ event.values[1]   + " "+ event.values[2]+"\r\n");
                Save.writeToTXT("linacc.txt",lineraacc);
                break;
        }
    }

    // 当传感器精度发生改变时回调该方法
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
