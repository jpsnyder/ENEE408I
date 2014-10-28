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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.ByteBuffer;

//import org.opencv.core;

class ByteUtils {
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);

    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    public static byte[] stringToBytes(String str) {
        byte[] b = new byte[str.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) str.charAt(i);
        }
        return b;
    }

    public static String bytesToString(byte[] bytes){
        return new String(bytes);
    }

    public static byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}

public class Main extends Activity {

    public static final String TAG = "ROBO";

    // Mapping Thread, Run all functions in here
    class MappingTask implements Runnable {

        boolean isRunning;

        // called when ArduinoController gets data from arduino
        public void receivedData(byte[] data){
            statusText.setText("Received: " + new String(data));
        }

        @Override
        public void run() {
            isRunning = true;
            while(isRunning) {
                // Test send command
                // TODO: make helper functions like move_robot and rotate_robot
                Log.i(TAG, "Sending D100 to arduino");
                ArduinoController.write(ByteUtils.concatenateByteArrays(
                        ByteUtils.stringToBytes("D"),
                        ByteUtils.longToBytes((long) 100)));
                sendCommandText.setText("D100");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.i(TAG, "Thread interrupted!");
                    statusText.setText("Thread interrupted");
                    e.printStackTrace();
                }
                Log.i(TAG, "Sending D1000 to arduino");
                ArduinoController.write(ByteUtils.concatenateByteArrays(
                        ByteUtils.stringToBytes("D"),
                        ByteUtils.longToBytes((long) 1000)));
                sendCommandText.setText("D1000");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.i(TAG, "Thread interrupted again!");
                    statusText.setText("Thread interrupted");
                    e.printStackTrace();
                }
            }
        }
    }

    TextView statusText;
    TextView sendCommandText;  // Center text that displays command being sent to arduino
    MappingTask mappingTask = new MappingTask();
    Thread mappingThread = new Thread(mappingTask);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup text
        sendCommandText = (TextView) findViewById(R.id.sendCommandText);
        statusText = (TextView) findViewById(R.id.statusText);

        // Wifi stuff
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
        try {
            mappingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        mappingThread.start();

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