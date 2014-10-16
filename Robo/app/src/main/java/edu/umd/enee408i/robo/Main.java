package edu.umd.enee408i.robo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.nio.ByteBuffer;

//import org.opencv.core;

public class ByteUtils {
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

    TextView statusText;
    TextView sendCommandText;  // Center text that displays command being sent to arduino

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: don't have displayable activity?
        setContentView(R.layout.activity_main);

        // setup text
        sendCommandText = (TextView) findViewById(R.id.sendCommandText);
        statusText = (TextView) findViewById(R.id.statusText);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ArduinoController.pause();
        finish();
    }

    @Override
    protected void onResume(){
        super.onResume();
        ArduinoController.start(this);

        // Test send command
        ArduinoController.write(ByteUtils.concatenateByteArrays(
                ByteUtils.stringToBytes("D"),
                ByteUtils.longToBytes((long) 100)));
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
    public void receivedData(String data){
        statusText.setText("Received: " + data);
    }
}
