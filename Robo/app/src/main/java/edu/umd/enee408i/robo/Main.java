package edu.umd.enee408i.robo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Main extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener{
    // initialize static use of openCV (no using opencv manager)
    //   - I know this is bad but WHO CARES!!
    //  from: http://stackoverflow.com/questions/20259309/how-to-integrate-opencv-manager-in-android-app
//    static {
//        if (!OpenCVLoader.initDebug()) {
//            // Handle initialization error
//        }
//    }



    public static final String TAG = "ROBO";

    // Mapping Thread, Run all functions in here
    @SuppressLint("NewApi")
    class MappingTask extends AsyncTask<Void, String, Void>{

        boolean isRunning;

        // called when ArduinoController gets data from arduino
//        public void receivedData(byte[] data){
//            statusText.setText("Received: " + new String(data));
//        }

        @Override
        protected void onProgressUpdate(String... progress) {
            // update found clients
            if (progress[0].equals("status")){
                statusText.setText(progress[1]);
            } else if (progress[0].equals("command")) {
                sendCommandText.setText(progress[1]);
            }
        }

        @SuppressLint("NewApi")
        @Override
        protected Void doInBackground(Void... voids) {
            isRunning = true;
            while(isRunning) {
//                publishProgress("status", scanDevices());
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
////                    e.printStackTrace();
//                }

                Log.i(TAG, "Sending R10 to arduino");
                publishProgress("command", "R10");
                ArduinoController.rotate_robot(new Float(180), true);

                // sleeping
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.i(TAG, "Thread interrupted again!");
                    e.printStackTrace();
                }

                Log.i(TAG, "Sending D1 to arduino");
                publishProgress("command", "D1");
                ArduinoController.move_robot(new Float(1), true);



            }
            return null;
        }
    }

    TextView statusText;
    TextView sendCommandText;  // Center text that displays command being sent to arduino
    MappingTask mappingTask = new MappingTask();

    // OpenCv camera stuff (from: http://stackoverflow.com/questions/19213230/opencv-with-android-camera-surfaceview)
    protected CameraBridgeViewBase cameraPreview;
    protected Mat mRgba;
    protected BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(ColorRegionDetectionActivity.this);
                    cameraPreview.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    // Wifi stuff
    boolean wasAPEnabled = false;
    static WifiAP wifiAP;
    private WifiManager wifi;
    static Button btnWifiToggle;
    public static void updateButtonStatus() {
        if (wifiAP.getWifiAPState()==wifiAP.WIFI_AP_STATE_ENABLED || wifiAP.getWifiAPState()==wifiAP.WIFI_AP_STATE_ENABLING) {
            btnWifiToggle.setText("Turn off WifiAP");
            //findViewById(R.id.bg).setBackgroundResource(R.drawable.bg_wifi_on);
        } else {
            btnWifiToggle.setText("Turn on WifiAP");
            //findViewById(R.id.bg).setBackgroundResource(R.drawable.bg_wifi_off);
        }
    }

    private String scanDevices() {
        ArrayList<ClientScanResult> clients = wifiAP.getClientList(false, 300);

        String result = "Clients: \n";
        for (ClientScanResult clientScanResult : clients) {
            result += "####################\n";
            result += "IpAddr: " + clientScanResult.getIpAddr() + "\n";
            result += "Device: " + clientScanResult.getDevice() + "\n";
            result += "HWAddr: " + clientScanResult.getHWAddr() + "\n";
            result += "isReachable: " + clientScanResult.isReachable() + "\n";
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup text
        sendCommandText = (TextView) findViewById(R.id.sendCommandText);
        statusText = (TextView) findViewById(R.id.statusText);

        cameraPreview = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        cameraPreview.setCvCameraViewListener(this);

        // Wifi stuff
        btnWifiToggle = (Button) findViewById(R.id.btnWifiToggle);
        wifiAP = new WifiAP();
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        btnWifiToggle.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                wifiAP.toggleWiFiAP(wifi, Main.this);
            }
        });

        Log.i(TAG, "Finished onCreate");
    }

    // Camera stuff
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba =  new Mat(height, width, CvType.CV_8UC4);
    }
    @Override
    public void onCameraViewStopped() {
        mRgba.release();

    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // TODO Auto-generated method stub
        mRgba = inputFrame.rgba();

        return mRgba;
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }


    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        super.onPause();
        ArduinoController.pause();
        mappingTask.isRunning = false;  // TODO: probably some data races.. but who cares?!
//        try {
            mappingTask.cancel(true);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // Wifi stuff
        boolean wifiApIsOn = wifiAP.getWifiAPState()==wifiAP.WIFI_AP_STATE_ENABLED || wifiAP.getWifiAPState()==wifiAP.WIFI_AP_STATE_ENABLING;
        if (wifiApIsOn) {
            wasAPEnabled = true;
            wifiAP.toggleWiFiAP(wifi, Main.this);
        } else {
            wasAPEnabled = false;
        }
        updateButtonStatus();

        // Camera stuff
        if(cameraPreview != null){
            cameraPreview.disableView();
        }

        finish();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume(){
        super.onResume();
        statusText.setText(ArduinoController.start(this));
        Log.i(TAG, "Starting mappingThread.");
        mappingTask.execute();

        // Camera stuff
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);

        // Wifi stuff
        if (wasAPEnabled) {
            if (wifiAP.getWifiAPState()!=wifiAP.WIFI_AP_STATE_ENABLED && wifiAP.getWifiAPState()!=wifiAP.WIFI_AP_STATE_ENABLING){
                wifiAP.toggleWiFiAP(wifi, Main.this);
            }
        }
        updateButtonStatus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.arduino_controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    // Called when arduino controller receives data
    public void receivedData(byte[] data){
        Log.i(TAG, "Recieved from Arduino: " + data);
    }
}
