package com.example.fypv11;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kircherelectronics.fsensor.filter.BaseFilter;
import com.kircherelectronics.fsensor.filter.averaging.LowPassFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    public float [] gravity = {0.00f,0.00f,0.00f};
    public float [] linear_acceleration = {0.00f,0.00f,0.00f};
    static final float ALPHA = 0.01f;
    protected float[] gravSensorVals;


    private SensorManager sensorManager;
    Sensor accelerometer;
    double xValueRounded;
    double yValueRounded;
    double zValueRounded;
    double vibration;
    double vibrationTotal;
    double longitude,latitude;

    private RequestQueue requestQueue;
    TextView xValue, yValue, zValue, totalValue;


    TextView tvAddress;
    String AddressName;
    String oldAddressName = "empty";
    public ArrayList<String> dataArrayList = new ArrayList<String>();
    public String currentTime;
    public String id;
    private int timeInterval = 200;
    private long mLastUpdate;
    private static double distanceInMeters;
    private static Location lastLocation = null;
    private static Location currLocation;

    protected LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xValue = (TextView) findViewById(R.id.xValue);
        yValue = (TextView) findViewById(R.id.yValue);
        zValue = (TextView) findViewById(R.id.zValue);
        tvAddress = (TextView) findViewById(R.id.tvAddress);

        id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        mLastUpdate = System.currentTimeMillis();
        Log.d(TAG, "onCreate: Registered Accelerometer listner");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);



    }

    private BaseFilter filter;
//    private void init() {
//        filter = new LowPassFilter(), MeanFilter(), MedianFilter();
//        filter.setTimeConstant(0.18f);
//    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");

                    break;
                default:
                    locationAddress = null;
            }

            AddressName = locationAddress.toString();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int i){

    }

    public void onLocationChanged(Location location) {

        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            LocationAddress locationAddress = new LocationAddress();
            locationAddress.getAddressFromLocation(latitude, longitude,
                    getApplicationContext(), new GeocoderHandler());

            currLocation = location;

//            if(AddressName != null) {
//                if (AddressName == "") {
//
//                } else if ((AddressName.equals(oldAddressName)) || (oldAddressName.equalsIgnoreCase("empty"))) {
//                    if(lastLocation == null){
//                        lastLocation = location;
//                    }
//                    distanceInMeters += location.distanceTo(lastLocation);
//                    lastLocation = location;
//
//                } else if (!AddressName.equals(oldAddressName)) {
//                    lastLocation = null;
//                    distanceInMeters = 0.00;
//                }
//                oldAddressName = AddressName;
//            }



//            Log.d(TAG, "onLocationChanged: " + distanceInMeters);
//            Log.d(TAG, "vibration Total: " + vibrationTotal);

//            Object dataArray = new Object();

        } else {
            showSettingsAlert();
        }
    }

    public void onSensorChanged(SensorEvent sensorEvent) {

//        long actualTime = System.currentTimeMillis();
//        Log.d(TAG, "onSensorChanged: X: " + sensorEvent.values[0]   +" " +time + " "+"Y: " + sensorEvent.values[1] + "Z: " + sensorEvent.values[2]);

//        if(actualTime - mLastUpdate > timeInterval) {
//
//            mLastUpdate = actualTime;

            gravSensorVals = lowPass(sensorEvent.values.clone(), gravSensorVals);

            final float alpha = 0.8f;

            gravity[0] = (alpha * gravity[0] + (1 - alpha) * gravSensorVals[0]);
            gravity[1] = (alpha * gravity[1] + (1 - alpha) * gravSensorVals[1]);
            gravity[2] = (alpha * gravity[2] + (1 - alpha) * gravSensorVals[2]);

            linear_acceleration[0] = gravSensorVals[0] - gravity[0];
            linear_acceleration[1] = gravSensorVals[1] - gravity[1];
            linear_acceleration[2] = gravSensorVals[2] - gravity[2];

            xValueRounded = Math.round(linear_acceleration[0] * 100.00) / 100.00;
            yValueRounded = Math.round(linear_acceleration[1] * 100.00) / 100.00;
            zValueRounded = Math.round(linear_acceleration[2] * 100.00) / 100.00;


            xValue.setText("xValue:" + xValueRounded + "   " + sensorEvent.values[0]);
            yValue.setText("yValue:" + yValueRounded + "   " + sensorEvent.values[1]);
            zValue.setText("zValue:" + zValueRounded + "   " + (sensorEvent.values[2] - 9.81));

            vibration = Math.abs(0.5 *  zValueRounded * (0.02*0.02)) ;

//        Log.d(TAG, "ontesterChanged: X: " + gravSensorVals[0] + "Y: " + gravSensorVals[1] + "Z: " + gravSensorVals[2]);
//            Log.d(TAG, "onjasperChanged: X: "+xValueRounded + "Y: " + yValueRounded + "Z: " + zValueRounded);

        if(AddressName != null) {
            if (AddressName == "") {

            } else if ((AddressName.equals(oldAddressName)) || (oldAddressName.equalsIgnoreCase("empty"))) {

                vibrationTotal = vibrationTotal + vibration;

                String addData = zValueRounded + " " + longitude + " " + latitude;
                //     Reading rd = new Reading(zValueRounded, longitude, latitude);
                dataArrayList.add(addData);

                if(lastLocation == null){
                    lastLocation = currLocation;
                }
                distanceInMeters += currLocation.distanceTo(lastLocation);
                lastLocation = currLocation;

            } else if (!AddressName.equals(oldAddressName)) {
                sendData();
                dataArrayList.clear();

                vibrationTotal = 0.00;

                lastLocation = null;
                distanceInMeters = 0.00;
            }
            oldAddressName = AddressName;
        }

//        Log.d(TAG, "address " + AddressName);
//        Log.d(TAG, " old address " + oldAddressName);
//
//        //  Log.d(TAG, "data array: " + dataArray);
//        Log.d(TAG, "data array list: " + dataArrayList);
//        Log.d(TAG, "vibration: " + vibration);
//        Log.d(TAG, "vibration Total: " + vibrationTotal);



    }


        protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }



    private void sendData() {
        currentTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());

        double iriValue = vibrationTotal / (distanceInMeters/1000);

        JSONArray array = new JSONArray(dataArrayList);
        String data = "[{ " +
                "\"userId\"" + ":" +  "\"" + id + "\","+
                "\"roadName\"" + ":" + "\"" + oldAddressName + "\" ,"+
                "\"iriValue\"" + ":" + "\"" + iriValue + "\" ,"+
                "\"distanceInMeters\"" + ":" + "\"" + distanceInMeters +  "\" ,"+
                "\"logDate\"" + ":" + "\"" + currentTime + "\"" +
                "}]";

        Submit(data);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                MainActivity.this);
        alertDialog.setTitle("SETTINGS");
        alertDialog.setMessage("Enable Location Provider! Go to settings menu?");
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        MainActivity.this.startActivity(intent);
                    }
                });
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }





    private void Submit(String data){
        final String savedata = data;
        String URL = "https://my-fyp-1551939769568.appspot.com/data/add";
//        String URL = "http://192.168.1.13:8080/data/add";

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject objres = new JSONObject(response);
                    Toast.makeText(getApplicationContext(), objres.toString(), Toast.LENGTH_LONG).show();

                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Server Error", Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        })
        {
            public String getBodyContentType() {return "application/json; charset=utf-8";}

            public byte[] getBody() throws AuthFailureError {
                try {
                    return savedata == null ? null : savedata.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee){
                    return null;
                }
            }

        };
        requestQueue.add(stringRequest);
    }
}

//class Reading {
//
//    private Double accReading;
//    private Double longitudeReading;
//    private Double latitudeReading;
//
//    public Reading(Double accReading, Double longitudeReading, Double latitudeReading) {
//        this.accReading = accReading;
//        this.longitudeReading = longitudeReading;
//        this.latitudeReading = latitudeReading;
//    }
//
//    public Double getAccReading() {
//        return accReading;
//    }
//
//    public void setAccReading(Double accReading) {
//        this.accReading = accReading;
//    }
//
//    public Double getLongitudeReading() {
//        return longitudeReading;
//    }
//
//    public void setLongitudeReading(Double longitudeReading) {
//        this.longitudeReading = longitudeReading;
//    }
//
//    public Double getLatitudeReading() {
//        return latitudeReading;
//    }
//
//    public void setLatitudeReading(Double latitudeReading) {
//        this.latitudeReading = latitudeReading;
//    }
//}
