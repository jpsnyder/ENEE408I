package edu.umd.enee408i.robo;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

//import org.opencv.core;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ArduinoController extends Activity {

    TextView statusText;
    TextView sendCommandText;  // Center text that displays command being sent to arduino
    // TODO:  make a receive one
    private static UsbSerialPort arduinoPort = null;  // arduino port
    private UsbManager usbManager;
    //Innocuous comment

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SerialInputOutputManager serialIOManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
//                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    ArduinoController.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArduinoController.this.updateReceivedData(data);
                        }
                    });
                }
            };

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        sendCommandText.setText(message);  // display received data
//        mDumpTextView.append(message);
//        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

    private void stopIoManager() {
        if (serialIOManager != null) {
//            Log.i(TAG, "Stopping io manager ..");
            serialIOManager.stop();
            serialIOManager = null;
        }
    }

    private void startIoManager() {
        if (arduinoPort != null) {
//            Log.i(TAG, "Starting io manager ..");
            serialIOManager = new SerialInputOutputManager(arduinoPort, mListener);
            executor.submit(serialIOManager);
        }
    }



    public static byte[] stringToBytesASCII(String str) {
        char[] buffer = str.toCharArray();
        byte[] b = new byte[buffer.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) buffer[i];
        }
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: don't have displayable activity?
        setContentView(R.layout.activity_arduino_controller);

        // setup text
        sendCommandText = (TextView) findViewById(R.id.sendCommandText);
        statusText = (TextView) findViewById(R.id.statusText);

        // Find all available drivers from attached devices
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (arduinoPort != null) {
            try {
                arduinoPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            arduinoPort = null;
        }
        finish();
    }

    @Override
    protected void onResume(){
        super.onResume();

        SystemClock.sleep(1000);
        List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.isEmpty()){
            statusText.setText("No drivers found!");
            return;
        }
        // TODO: for now we are assuming the first one is the arduino....
        arduinoPort = drivers.get(0).getPorts().get(0);

        if (arduinoPort == null){
            statusText.setText("Arduino Not Found!");
        } else {

            UsbDeviceConnection connection = usbManager.openDevice(arduinoPort.getDriver().getDevice());
            if (connection == null){
                statusText.setText("Opening device failed!");
                return;
            }

//            // Open a connection to the first available driver.
//            UsbSerialDriver driver = availableDrivers.get(0);
//            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
//            if (connection == null) {
//                statusText.setText("Opening device Failed!");
//                return;
//            }
//
//            // TODO: create helper functions for this
//            // Write some data..
//            arduinoPort = driver.getPorts().get(0);
            try {
                arduinoPort.open(connection);
                // set to send over 8 bits
                arduinoPort.setParameters(115200, UsbSerialPort.DATABITS_8,
                        UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                // Send command to go forward then backward  for testing purposes
                arduinoPort.write(stringToBytesASCII("F"), 1000);
//                Thread.sleep(5000);
                arduinoPort.write(stringToBytesASCII("B"), 1000);
//                Thread.sleep(5000);

                arduinoPort.close();
            } catch (IOException e) {
                statusText.setText("Error opening device: " + e.getMessage());
                try {
                    arduinoPort.close();
                } catch (IOException e2){
                    // Ignore
                }
                return;
            }
            statusText.setText("Connected to: " + arduinoPort.getClass().getSimpleName());
        }
        onDeviceStateChange();  // Restart the IO manager
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
