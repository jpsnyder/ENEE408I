package edu.umd.enee408i.robo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

//import org.opencv.core;


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
        ArduinoController.write("F");
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
