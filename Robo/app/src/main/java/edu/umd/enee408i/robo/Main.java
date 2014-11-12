package edu.umd.enee408i.robo;

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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Main extends Activity {

    public static final String TAG = "ROBO";

    // Mapping Thread, Run all functions in here
    class MappingTask extends AsyncTask<Void, String, Void>{

        boolean isRunning;

        // called when ArduinoController gets data from arduino
        public void receivedData(byte[] data){
            statusText.setText("Received: " + new String(data));
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            // update found clients
            if (progress[0].equals("status")){
                statusText.setText(progress[1]);
            } else if (progress[0].equals("command")) {
                sendCommandText.setText(progress[1]);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            isRunning = true;
            while(isRunning) {
                Mat m = new Mat(5, 10, CvType.CV_8UC1, new Scalar(0));
                Log.i(TAG, "Success creating m");
                publishProgress("status", "OpenCV Mat: " + m.toString());
                Log.i(TAG, "Published creating m");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.i(TAG, "Thread interrupted!");
                    e.printStackTrace();
                }
                Log.i(TAG, "done waiting!");
                //Mat mr1 = m.row(1);
                //mr1.setTo(new Scalar(1));
                //Mat mc5 = m.col(5);
                //mc5.setTo(new Scalar(5));
                //publishProgress("status", "OpenCV Mat data:\n" + m.dump());



//                publishProgress("status", scanDevices());
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
////                    e.printStackTrace();
//                }

                // Test send command
                // TODO: make helper functions like move_robot and rotate_robot
//                String string = "D" + (new Float(2)).toString() + "\r\n";
//                ArduinoController.write(ByteUtils.stringToBytes(string)
//                       );
//                publishProgress("command", "D2");
//                ArduinoController.move_robot(new Float(2), true);

                // sleeping

//                Log.i(TAG, "Sending R180 to arduino");
//                ArduinoController.write(
//                        ByteUtils.stringToBytes("R" + (new Float(180)).toString() + "\r\n"));
//                publishProgress("command", "R180");
//                ArduinoController.rotate_robot(new Float(180), true);

                // sleeping
//                try {
//                    Thread.sleep(5000);
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
        finish();

        // Wifi stuff
        boolean wifiApIsOn = wifiAP.getWifiAPState()==wifiAP.WIFI_AP_STATE_ENABLED || wifiAP.getWifiAPState()==wifiAP.WIFI_AP_STATE_ENABLING;
        if (wifiApIsOn) {
            wasAPEnabled = true;
            wifiAP.toggleWiFiAP(wifi, Main.this);
        } else {
            wasAPEnabled = false;
        }
        updateButtonStatus();
    }

    @Override
    protected void onResume(){
        super.onResume();
        statusText.setText(ArduinoController.start(this));
        Log.i(TAG, "Starting mappingThread.");
        mappingTask.execute();

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
        mappingTask.receivedData(data);  // send over to mapping thread
    }
}
