package edu.umd.enee408i.robo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Main extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener{

    public static final String TAG = "ROBO";

    // Mapping Thread, Run all functions in here
    @SuppressLint("NewApi")
    class MappingTask extends AsyncTask<Void, String, Void>{

        boolean isRunning;

        @Override
        protected void onProgressUpdate(String... progress) {
            // update found clients
            if (progress[0].equals("status")){
                statusText.setText(progress[1]);
            } else if (progress[0].equals("command")) {
                sendCommandText.setText(progress[1]);
            } else if (progress[0].equals("camera")){
                assert(cameraPreview2Mat != null);
                Mat m = cameraPreview2Mat;
                Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(m, bm);
                cameraPreview2.setImageBitmap(bm);
            }
        }


        @SuppressLint("NewApi")
        @Override
        protected Void doInBackground(Void... voids) {
            isRunning = true;
            while(isRunning) {
                // Show found devices on wifi
//                publishProgress("status", scanDevices());
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
////                    e.printStackTrace();
//                }

                // wait till camera is ready
                while(mRgba == null);
                // create new greyscale image
                Mat wallImage = new Mat(mRgba.size(), CvType.CV_8UC1);
                //mRgba.convertTo(tempImage, -1, 1, 0);
                Imgproc.cvtColor(mRgba, wallImage, Imgproc.COLOR_RGB2GRAY, 4);
                //Imgproc.threshold(tempImage, tempImage, 100, 255, Imgproc.THRESH_BINARY);

                // The following may be helpful for finding walls
                Imgproc.GaussianBlur(wallImage, wallImage, new Size(7,7), 0);
                Imgproc.adaptiveThreshold(wallImage, wallImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 23, 12);
                int dilation_size = 3;
                Mat morphElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*dilation_size + 1, 2*dilation_size+1));
                Imgproc.dilate(wallImage, wallImage, morphElement);

                Mat invertColorMatrix= new Mat(wallImage.rows(),wallImage.cols(), wallImage.type(), new Scalar(255,0,0));
                Core.subtract(invertColorMatrix, wallImage, wallImage);
                //Imgproc.adaptiveThreshold(tempImage, tempImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 17, 20);

                //

                //Imgproc.blur(tempImage, tempImage, new Size(3,3));
                //int thresh = 30, ratio = 3;
                //Imgproc.Canny(tempImage, tempImage, thresh, ratio*thresh);

                //Mat thresholdImage = new Mat(mRgba.height() + mRgba.height() / 2, mRgba.width(), CvType.CV_8UC1);
                Mat thresholdImage = new Mat(mRgba.size(), CvType.CV_8UC1);
                Imgproc.cvtColor(mRgba, thresholdImage, Imgproc.COLOR_RGB2GRAY, 4);  // convert to greyscale
                int thresh = 50, ratio = 3;
                Imgproc.Canny(thresholdImage, thresholdImage, thresh, ratio*thresh);
                Mat lines = new Mat();  // mat to draw lines

                // find the door color
                Mat mask = new Mat(mRgba.size(), mRgba.type());
                Mat mRgba_temp = new Mat(mRgba.size(), mRgba.type());
                Imgproc.cvtColor(mRgba, mRgba_temp, Imgproc.COLOR_RGBA2BGR, 0);
                Core.inRange(mRgba_temp, new Scalar(0, 0,115), new Scalar(60, 255, 150), mask);



                int count = 0;
                for(int row_i = 0; row_i < mask.rows(); row_i++){
                    for(int col_i = 0; col_i < mask.cols(); col_i++){
                        double pixel[] = mask.get(row_i, col_i);
                        if ((pixel[0] > 0)){
                            count++;
                        }
                    }
                }
                Log.i(TAG, "Count = " + count);
                if(count > 400){
                    Log.i(TAG, "FOUND DOOR!");
                    if(doorPressed) {
                        publishProgress("command", "Door Found");
                    }
                } else {
                    Log.i(TAG, "NO DOOR!");
                    if(doorPressed) {
                    publishProgress("command", "Door Not Found");
                    }
                }

                // magic
                if(gridPressed) {
                    Imgproc.HoughLinesP(thresholdImage, lines, 1, Math.PI / 180, threshold, minLineSize, lineGap);
                } else {
                    Imgproc.HoughLinesP(wallImage, lines, 1, Math.PI / 180, threshold, minLineSize, lineGap);
                }

                double xRange = wallImage.cols();
                boolean safe = false;

                // draw the lines onto lines mat
                for (int x = 0; x < lines.cols(); x++)
                {
                    double[] vec = lines.get(0, x);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];
                    Point start = new Point(x1, y1);
                    Point end = new Point(x2, y2);
                    if(start.x < xRange / 3 && end.x > xRange * 2/3 || end.x < xRange / 3 && start.x > xRange * 2/3){
                        safe = true;
                    }


//                    draw onto our camera
                 if(gridPressed) {
                     Core.line(thresholdImage, start, end, new Scalar(135, 0, 0), 3);
                 } else {
                     Core.line(wallImage, start, end, new Scalar(135, 0, 0), 3);
                 }

                    // Find closest lines, check if point is near bottom and close to center
                }
                /*
                if(safe) {
                    ArduinoController.move_robot(100f, true);
                    // at least a 1 second sleep is necessary between commands
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Thread interrupted again!");
                        e.printStackTrace();
                    }
                    ArduinoController.rotate_robot(90f, true);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Thread interrupted again!");
                        e.printStackTrace();
                    }
                    ArduinoController.wall_follow_right();
                    while(!false);

                } else {
                    ArduinoController.rotate_robot(10f, true);
                    // at least a 1 second sleep is necessary between commands
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Thread interrupted again!");
                        e.printStackTrace();
                    }
                }*/
                // Update camera image to see if corners are found
                /*Point[] corner = corners.toArray();
                for(Point px: corner)
                { Core.circle(thresholdImage, px, 15, new Scalar(255,0,0)); }*/

                if(wallPressed) {
                    cameraPreview2Mat = wallImage;
                } else if (doorPressed){
                    cameraPreview2Mat = mask;
                } else if (gridPressed){
                    cameraPreview2Mat = thresholdImage;
                } else {
                    cameraPreview2Mat = wallImage;
                }
                //cameraPreview2Mat = wallImage;
                //cameraPreview2Mat = thresholdImage;
                publishProgress("camera");


                // follow left wall
//                Log.i(TAG, "Sending < to arduino");
                //publishProgress("command", "<");
                //ArduinoController.wall_follow_left();
//                Log.i(TAG, "< command sent");

//                Log.i(TAG, "Sending " + angle + " to arduino");
//                publishProgress("command", "R" + Math.round(angle));
//                ArduinoController.rotate_robot(new Float(angle), true);
//                Log.i(TAG, "YAYY, IT WORKED!");



                // stop the wall follow
                Log.i(TAG, "Stop wall follow");
                //ArduinoController.stop_robot();
                Log.i(TAG, "Stopped");

//                Log.i(TAG, "Sending D1 to arduino");
//                publishProgress("command", "D1");
//                ArduinoController.move_robot(new Float(1), true);
//                Log.i(TAG, "YAYY, IT WORKED again!");
//
//                // at least a 1 second sleep is necessary between commands
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    Log.i(TAG, "Thread interrupted again!");
//                    e.printStackTrace();
//                }

            }
            return null;
        }
    }

    TextView statusText;
    TextView sendCommandText;  // Center text that displays command being sent to arduino
    MappingTask mappingTask = new MappingTask();
    EditText thresholdText;
    EditText minLineSizeText;
    EditText lineGapText;
    Button updateBtn;
    RadioButton doorRadioBtn;
    RadioButton wallRadioBtn;
    RadioButton gridRadioBtn;

    // OpenCv camera stuff (from: http://stackoverflow.com/questions/19213230/opencv-with-android-camera-surfaceview)
    protected CameraBridgeViewBase cameraPreview;
    protected ImageView cameraPreview2;
    public Mat mRgba;
    public Mat cameraPreview2Mat;
    public boolean gridPressed = false;
    public boolean doorPressed = false;
    public boolean wallPressed = false;


    protected BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraPreview.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    int threshold = 10;
    int minLineSize = 50;
    int lineGap = 20;

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
        // test camera
        cameraPreview2 = (ImageView) findViewById(R.id.cameraView2);

        // Wifi stuff
        btnWifiToggle = (Button) findViewById(R.id.btnWifiToggle);
        wifiAP = new WifiAP();
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        btnWifiToggle.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                wifiAP.toggleWiFiAP(wifi, Main.this);
            }
        });

        doorRadioBtn = (RadioButton) findViewById(R.id.doorRadio);
        wallRadioBtn = (RadioButton) findViewById(R.id.wallRadio);
        gridRadioBtn = (RadioButton) findViewById(R.id.gridRadio);
        doorRadioBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                doorPressed = true;
                wallPressed = false;
                gridPressed = false;
            }
        });
        gridRadioBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                doorPressed = false;
                wallPressed = false;
                gridPressed = true;
            }
        });
        wallRadioBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                doorPressed = false;
                wallPressed = true;
                gridPressed = false;
            }
        });

        Log.i(TAG, "Finished onCreate");
    }

    // Camera stuff
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba =  new Mat(height, width, CvType.CV_8UC3);
    }
    @Override
    public void onCameraViewStopped() {
        mRgba.release();

    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
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

        // Camera stuff
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);

        // Wifi stuff
        if (wasAPEnabled) {
            if (wifiAP.getWifiAPState()!=wifiAP.WIFI_AP_STATE_ENABLED && wifiAP.getWifiAPState()!=wifiAP.WIFI_AP_STATE_ENABLING){
                wifiAP.toggleWiFiAP(wifi, Main.this);
            }
        }
        updateButtonStatus();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Starting ArduinoController");
        // wait a few seconds for opencv to get its shit together before starting arduinoController and mapping task
        statusText.setText(ArduinoController.start(this));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Starting mappingThread.");
        mappingTask.execute();
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

}
