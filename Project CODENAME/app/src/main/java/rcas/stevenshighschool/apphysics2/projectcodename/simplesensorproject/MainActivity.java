package rcas.stevenshighschool.apphysics2.projectcodename.simplesensorproject;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
//import java.util.List;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    /**
     * Sensor manager
     */
    private SensorManager sensorManager;

    /**
     * Main loops for the sensors
     */
    final Handler h = new Handler();
    Runnable r;
    Runnable rCamera;

    /**
     * Data values
     */
    float t;
    float a_y;
    float a_x;
    float a_z;
    float p;
    float m_x;
    float m_y;
    float m_z;
    float rh;
    float rot_x;
    float rot_y;
    float rot_z;
    float g_x;
    float g_y;
    float g_z;
    float ext_lat;
    float ext_lon;
    float ext_alt;
    float ext_p;
    float ext_BMP180temp;
    float ext_BMP180altEst;
    float ext_ST21temp;
    float ext_rh;
    Location mLastLocation;

    /**
     * Sensors and their listeners
     */
    Sensor accelerometer;
    Sensor pressure;
    Sensor magnet;
    Sensor humidity;
    Sensor rotation;
    Sensor gravity;
    Sensor temperature;
    SensorEventListener accelerometerListener;
    SensorEventListener pressureListener;
    SensorEventListener magnetListener;
    SensorEventListener humidityListener;
    SensorEventListener rotationListener;
    SensorEventListener gravityListener;
    SensorEventListener temperatureListener;


    /**
     * Root Access variables
     */
    Button rootTest, reboot, sysui;

    /**
     * Arduino USB connection variables
     */
    UsbDevice device;
    UsbDeviceConnection usbConnection;
    public static final String ACTION_USB_PERMISSION = "rcas.stevenshighschool.apphysics2.projectcodename.simplesensorproject.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    Camera mVideoCamera;
    MediaRecorder mMediaRecorder;
    private boolean isRecording = false;

    byte ch, buffer[] = new byte[1024];
    int iterReading = 0;
    String arduinoInRecent;


    /**
     * Array list of datapoints
     */
    ArrayList<DataPoint> dataPointArrayList;

    /**
     * Google Location things
     */
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;

    /**
     * Logging TAG
     */
    private final String TAG = "SENSORS:";


    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Starts by getting the location and requesting location updates
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            startLocationUpdates();
        }
    }


    /**
     * ARDUINO CONNECTION CODE
     */
    //TODO variables taken from TrackSoar be written to ext_p, ext_t, lat, long, and alt

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            Log.d(TAG, "receive");
            ByteArrayInputStream mIn = new ByteArrayInputStream(arg0);
            try {
                while((ch=(byte)mIn.read())!=-1){
                    if (ch != '#') {
                        buffer[iterReading++] = ch;
                    } else {
                        buffer[iterReading] = '\0';
                        arduinoInRecent = new String(buffer);
                        buffer = new byte[1024];
                        iterReading = 0;
                        processMessage();
                    }
                }

                tvAppend(textView, data);
            } catch (Exception e) {
                e.printStackTrace();
            }


       }
    };

    private void processMessage() {
        if(arduinoInRecent != null) {
            String[] parts = arduinoInRecent.split("-");
            int i=0;
            while(parts[i].equals("")){
                i++;
            }
            ext_lat = Float.parseFloat(parts[i]);
            i++;
            while(parts[i].equals("")){
                i++;
            }
            ext_lon=Float.parseFloat(parts[i]);
            i++;
            while(parts[i].equals("")){
                i++;
            }
            ext_alt=Float.parseFloat(parts[i]);
            i++;
            while(parts[i].equals("")){
                i++;
            }
            ext_p=Float.parseFloat(parts[i]);
            i++;
            while(parts[i].equals("")){
                i++;
            }
            ext_BMP180temp=Float.parseFloat(parts[i]);
            i++;
            while(parts[i].equals("")){
                i++;
            }
            ext_BMP180altEst=Float.parseFloat(parts[i]);
            i++;
            while(parts[i].equals("")){
                i++;
            }
            ext_ST21temp=Float.parseFloat(parts[i]);
            i++;
            while(parts[i].equals("")){
                i++;
            }
            ext_rh = Float.parseFloat(parts[i]);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                Log.d(TAG, "TEST!");
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView, "Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

        ;
    };

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    public void onClickStart(View view) {
        Log.d(TAG, "clickstart1");
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            Log.d(TAG, "clickstart2");
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                Log.d(TAG, deviceVID+"");
                if (deviceVID == 9025)//Arduino Vendor ID
                {

                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }


    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        serialPort.write(string.getBytes());
        tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView, "\nSerial Connection Closed! \n");

    }

    public void onClickClear(View view) {
        textView.setText(" ");
        editText.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(ftext!=null) {
                    ftv.append(ftext);
                }
            }
        });
    }


    /**
     * ------------------------------------- END ARDUINO CONNECTION CODE
     * -------------------------------------
     */


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // In case the connection fails
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    //Initialization of the activity


    public class StartUp extends AsyncTask<String, Void, Void> {
        public Context context = null;
        boolean suAvailable = false;

        public MainActivity.StartUp setContext(Context context) {
            this.context = context;
            return this;
        }

        @Override
        protected Void doInBackground(String... params) {
            suAvailable = Shell.SU.available();
            if (suAvailable) {

                // suResult = Shell.SU.run(new String[] {"cd data; ls"}); Shell.SU.run("reboot");
                switch (params[0]) {
                    //case "reboot"  : Shell.SU.run("reboot");break;
                    case "rootTest":
                        runOnUiThread(new Runnable() {
                            public void run() {

                                Toast.makeText(getApplicationContext(), "Your Phone Is Rooted, Begin The Asian Invasion", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {

                        Toast.makeText(getApplicationContext(), "The Asian Invasion Cannot Begin, Your Phone Is Not Rooted", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //starts things
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Arduino Connection Stuff */

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


        /** Root Actions */
        rootTest = (Button) findViewById(R.id.rootTest);
        reboot = (Button) findViewById(R.id.btn_reb);
        sysui = (Button) findViewById(R.id.SysUi);

        rootTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                (new MainActivity.StartUp()).setContext(v.getContext()).execute("rootTest");
            }
        });

        reboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                (new MainActivity.StartUp()).setContext(v.getContext()).execute("reboot");
            }
        });

        sysui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                (new MainActivity.StartUp()).setContext(v.getContext()).execute("sysui");

            }
        });


        /** gets sensors and checks for permissions */
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    250);

        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    251);

        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    23);

        }

        /** Initializes sensors and their listeners */
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        humidity = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        /** TODO incorporate low-power mode for payload in emergency low-power situation */


        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //changes a to most recent value
                a_x = sensorEvent.values[0];
                a_y = sensorEvent.values[1];
                a_z = sensorEvent.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                //do nothing
            }
        };

        pressureListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //changes p to most recent value
                p = sensorEvent.values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                //do nothing
            }
        };

        magnetListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //changes m to most recent value
                m_x = sensorEvent.values[0];
                m_y = sensorEvent.values[1];
                m_z = sensorEvent.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                //do nothing
            }
        };
        humidityListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //changes rh to most recent value
                rh = sensorEvent.values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                //do nothing
            }
        };
        rotationListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //changes rot to most recent value
                rot_x = sensorEvent.values[0];
                rot_y = sensorEvent.values[1];
                rot_z = sensorEvent.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                //do nothing
            }
        };
        gravityListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //changes g to most recent value
                g_x = sensorEvent.values[0];
                g_y = sensorEvent.values[1];
                g_z = sensorEvent.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                //do nothing
            }
        };
        temperatureListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //changes t to most recent value
                t = sensorEvent.values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                //do nothing
            }
        };


        /** Initializes Array */
        dataPointArrayList = new ArrayList<DataPoint>();

        /** Initializes Google Location Things */
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }

    protected void startLocationUpdates() {
        //Starts location updates
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    /**
     * Recording function that starts running things
     */
    public void record(View view) {
        final int delay = 1000; //milliseconds
        final int delayCamera = 1000 * 15; //milliseconds

        /** Initializes and starts Runnable */
        r = new Runnable() {
            public void run() {
                Log.d(TAG, "RUN!");

                /** Initializes the data point class */
                /** TODO decide on preferred order order of variables - not hugely important but deserves some consideration */
                DataPoint point = new DataPoint(t, g_x, g_y, g_z, rot_x, rot_y, rot_z, rh, m_x, m_y, m_z, a_x, a_y, a_z, p, new Date());
                if (mLastLocation != null) {
                    point.lat = mLastLocation.getLatitude();
                    point.alt = mLastLocation.getAltitude();
                    point.lon = mLastLocation.getLongitude();
                }

                point.ext_alt = ext_alt;
                point.ext_lon = ext_lon;
                point.ext_lat = ext_lat;
                point.ext_BMP180altEst = ext_BMP180altEst;
                point.ext_BMP180temp = ext_BMP180temp;
                point.ext_p = ext_p;
                point.ext_rh = ext_rh;
                point.ext_ST21temp = ext_ST21temp;

                dataPointArrayList.add(point);

                /** Object is serialized here, and the datafile is saved to the documents folder */
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File file = new File(path, "SENSORDATA.txt");
                try {
                    path.mkdirs();
                    OutputStream os = new FileOutputStream(file);
                    ObjectOutputStream out = new ObjectOutputStream(os);
                    out.writeObject(dataPointArrayList);
                    out.close();
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, mGoogleApiClient.isConnected() + "");
                //schedules the next job
                h.postDelayed(this, delay);
            }
        };
        rCamera = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "PHOTO!");
                Camera camera = openCamera();
                SurfaceView surface = new SurfaceView(getBaseContext());
                try {
                    camera.setPreviewTexture(new SurfaceTexture(0));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                camera.startPreview();
                final String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/High Altitude Photos/";
                final File file = new File(path);
                file.mkdirs();
                Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
                    public void onPictureTaken(byte[] data, Camera camera) {
                        FileOutputStream outStream = null;
                        try {
                            String finalPath = path + new Date() + ".jpg";// set your directory path here
                            outStream = new FileOutputStream(finalPath);
                            outStream.write(data);
                            outStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            camera.stopPreview();
                            camera.release();
                            camera = null;
                        }
                    }
                };
                camera.takePicture(null, null, jpegCallback);
                h.postDelayed(this, delayCamera);
            }
        };
        //schedules the first job
        h.postDelayed(r, delay);
        h.postDelayed(rCamera, delay);
    }

    public void stopRecord(View view) {
        h.removeCallbacks(r);
        h.removeCallbacks(rCamera);
    }

    /**
     * Initializes sensors that should not be done in onCreate
     */
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        //TODO add null checks to allow testing on certain phone models
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(pressureListener, pressure, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(magnetListener, magnet, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(humidityListener, humidity, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(rotationListener, rotation, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(gravityListener, gravity, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(temperatureListener, temperature, SensorManager.SENSOR_DELAY_NORMAL);
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    /**
     * de-initializes sensors that should be destroyed before onDestroyed
     */
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        sensorManager.unregisterListener(accelerometerListener);
        sensorManager.unregisterListener(pressureListener);
        sensorManager.unregisterListener(magnetListener);
        sensorManager.unregisterListener(humidityListener);
        sensorManager.unregisterListener(rotationListener);
        sensorManager.unregisterListener(gravityListener);
        sensorManager.unregisterListener(temperatureListener);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 251: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mGoogleApiClient.isConnected()) {
                        /** TODO fix permission issues...several possible solutions to entertain */
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        startLocationUpdates();
                    }
                } else {

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    /**
     * Code that runs camera and takes a photograph every 15 seconds
     */
    //TODO add video capabilities, even if they are commented out
    private Camera openCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        return cam;
    }

    private boolean prepareVideoRecorder() {

        mVideoCamera = openCamera();
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mVideoCamera.unlock();
        mMediaRecorder.setCamera(mVideoCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        final String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/High Altitude Videos/";
        final File file = new File(path);
        file.mkdirs();
        mMediaRecorder.setOutputFile(path + new Date() + ".mp4");

        // Step 5: Set the preview output - nothing in this case
        Surface surface = new Surface(new SurfaceTexture(0));
        mMediaRecorder.setPreviewDisplay(surface);

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mVideoCamera.release();
        }
    }

    //this can be called to start and stop recording - it currently isn't at all
    public void recordButton() {
        if (isRecording) {
            mMediaRecorder.stop();
            releaseMediaRecorder();
            isRecording = false;
        } else {
            if (prepareVideoRecorder()) {
                mMediaRecorder.start();
                isRecording = true;
            } else {
                releaseMediaRecorder();
            }
        }
    }

}
