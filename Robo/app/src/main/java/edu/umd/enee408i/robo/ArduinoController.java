package edu.umd.enee408i.robo;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ArduinoController {

    private static UsbSerialPort arduinoPort = null;  // arduino port
    private static UsbManager usbManager = null;

    // keep track of our main activity
    private static Main mainActivity = null;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static SerialInputOutputManager serialIOManager;

    // queue of received data from the Arduino
    public static Queue<String> retrievedData = new LinkedList<String>();

    private static final SerialInputOutputManager.Listener serialListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
//                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    updateReceivedData(data);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            updateReceivedData(data);
//                        }
//                    });
                }
            };

    private static void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private static void updateReceivedData(byte[] data) {
        // TODO: parse data first? call another function?
        String stringData = new String(data);
        retrievedData.add(stringData);
        mainActivity.receivedData(stringData);
//        final String message = "Read " + data.length + " bytes: \n"
//                + HexDump.dumpHexString(data) + "\n\n";
//        sendCommandText.setText(message);  // display received data
//        mDumpTextView.append(message);
//        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

    private static void stopIoManager() {
        if (serialIOManager != null) {
//            Log.i(TAG, "Stopping io manager ..");
            serialIOManager.stop();
            serialIOManager = null;
        }
    }

    private static void startIoManager() {
        if (arduinoPort != null) {
//            Log.i(TAG, "Starting io manager ..");
            serialIOManager = new SerialInputOutputManager(arduinoPort, serialListener);
            executor.submit(serialIOManager);
        }
    }


    public static int write(byte[] data){
        if (arduinoPort == null)
            return 0;
        int bytesWritten;
        try {
            bytesWritten = arduinoPort.write(data, 1000);
        } catch (IOException e) {
            return 0;
        }
        return bytesWritten;
    }

    public static String start(Context context){
        // Find all available drivers from attached devices
        if (usbManager == null)
            usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mainActivity = (Main) context;

        SystemClock.sleep(1000);
        List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.isEmpty()){
            return "No drivers found!";
        }
        // TODO: for now we are assuming the first one is the arduino....
        arduinoPort = drivers.get(0).getPorts().get(0);

        if (arduinoPort == null)
            return "Arduino Not Found!";


        UsbDeviceConnection connection = usbManager.openDevice(arduinoPort.getDriver().getDevice());
        if (connection == null){
            return "Opening device failed!";
        }

        try {
            arduinoPort.open(connection);
            // set to send over 8 bits
            arduinoPort.setParameters(115200, UsbSerialPort.DATABITS_8,
                    UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            arduinoPort.close();
        } catch (IOException e) {
            String result = "Error opening device: " + e.getMessage();
            try {
                arduinoPort.close();
            } catch (IOException e2){
                // Ignore
            }
            return result;
        }

        onDeviceStateChange();  // Restart the IO manager
        return "Connected to: " + arduinoPort.getClass().getSimpleName();
    }

    public static void pause() {
        if (arduinoPort != null) {
            try {
                arduinoPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            arduinoPort = null;
        }
    }

}
