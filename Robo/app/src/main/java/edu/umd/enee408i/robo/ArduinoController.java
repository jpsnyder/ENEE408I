package edu.umd.enee408i.robo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ByteUtils {
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);

    public static byte[] floatToBytes(float x) {
        buffer.putFloat(0, x);
        return buffer.array();
    }

    public static float bytesToFloat(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getFloat();
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

// static class that uses the usbSerialForAndroid library to communicate with the arduino
public class ArduinoController {

    public static final String TAG = "ROBO_ArduinoController";

    private static UsbSerialPort arduinoPort = null;
    private static UsbManager usbManager = null;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static SerialInputOutputManager serialIOManager;

    // queue of received data from the Arduino
    public static Queue<String> retrievedData = new LinkedList<String>();

    // listener function that runs when the arduino sends out data to the android
    private static final SerialInputOutputManager.Listener serialListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    Log.i(TAG, "Received Data: " + ByteUtils.bytesToString(data));
                    if (retrievedData == null){
                        retrievedData = new LinkedList<String>();
                    }
                    retrievedData.add(ByteUtils.bytesToString(data));
                }
            };

    private static void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private static void stopIoManager() {
        if (serialIOManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            serialIOManager.stop();
            serialIOManager = null;
        }
    }

    private static void startIoManager() {
        if (arduinoPort != null) {
            Log.i(TAG, "Starting io manager ..");
            serialIOManager = new SerialInputOutputManager(arduinoPort, serialListener);
            executor.submit(serialIOManager);
        }
    }

    // writes data to the arduino
    public static void write(byte[] data){
        if (arduinoPort == null) {
            Log.i(TAG, "write failed");
            return;
        }
        serialIOManager.writeAsync(data);
    }

    // sends 'S' to stop the arduino
    public static void stop_robot(){
        retrievedData.clear();
        write(ByteUtils.stringToBytes("S"));
    }

    // follow the wall on left of robot
    public static void wall_follow_left(){
        retrievedData.clear();
        write(ByteUtils.stringToBytes("<"));
    }

    // follow the wall on the right of robot
    public static void wall_follow_right(){
        retrievedData.clear();
        write(ByteUtils.stringToBytes(">"));
    }

    // moves the robot forward for the given distance (calls move_robot() in the arduino code)
    // if wait == True, the function is blocking
    public static void move_robot(Float distance, boolean wait){
        String string = "D" + distance.toString() + "\r\n";
        retrievedData.clear();
        write(ByteUtils.stringToBytes(string));
        if (wait){
            Log.i(TAG, "move_robot is waiting....");
            while(retrievedData.isEmpty());  // waits for any response from arduino
            Log.i(TAG, "move_robot: RETRIEVED DATA: " + retrievedData.peek());
        }
    }

    // rotates the robot counter-clockwise the given angle (calls rotate_robot() in the arduino code)
    // if wait == True, the function is blocking
    public static void rotate_robot(Float angle, boolean wait){
        String string = "R" + angle.toString() + "\r\n";
        retrievedData.clear();
        write(ByteUtils.stringToBytes(string));
        if (wait){
            Log.i(TAG, "rotate_robot is waiting....");
            while(retrievedData.isEmpty());  // waits for any response from arduino
            Log.i(TAG, "rotate_robot: RETRIEVED DATA: " + retrievedData.peek());
        }
    }

    // initializes the usb connection to the arduino
    // returns the the status to be displayed on the GUI
    public static String start(Context context){
        Log.i(TAG, "Starting ArduinoController");

        // set up usb manager
        if (usbManager == null)
            usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        // get all available usb drivers
        Log.i(TAG, "Listing Drivers");
        SystemClock.sleep(1000);
        List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.isEmpty()){
            Log.i(TAG, "No drivers found!");
            return "No drivers found!";
        }

        // look for a supported usb driver
        Log.i(TAG, "Drivers found: " + drivers.toString());
        arduinoPort = drivers.get(0).getPorts().get(0); // assume first one is the arduino

        if (arduinoPort == null) {
            Log.i(TAG, "Did not find arduino device.");
            return "Arduino Not Found!";
        }

        // open the usb device
        UsbDevice device = arduinoPort.getDriver().getDevice();
        Log.i(TAG, "Connected to productID: " + device.getVendorId());
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null){
            Log.i(TAG, "Could not open device");
            return "Opening device failed!";
        }

        // open usb connection and set parity and baud rate
        try {
            arduinoPort.open(connection);
            // set to send over 8 bits
            arduinoPort.setParameters(9600, UsbSerialPort.DATABITS_8,
                    UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            String result = "Error opening device: " + e.getMessage();
            Log.i(TAG, result);
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
