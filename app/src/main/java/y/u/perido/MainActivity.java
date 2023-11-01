package y.u.perido;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity implements WifiSerial.ConnectionListener, WifiSerial2.ConnectionListener {

    TextView outputView1, outputView2;
    private TextInputEditText ip1, port1, ip2, port2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputView1 = findViewById(R.id.output1);
        outputView2 = findViewById(R.id.output2);

        ip1 = findViewById(R.id.ip1);
        port1 = findViewById(R.id.port1);

        ip2 = findViewById(R.id.ip2);
        port2 = findViewById(R.id.port2);

        String callIP = MyApp.getInstance().getSharedPreferences().getString("ip", "");
        String callPort = MyApp.getInstance().getSharedPreferences().getString("port", "");

        ip1.setText(callIP);
        port1.setText(callPort);


        String callIP2 = MyApp.getInstance().getSharedPreferences().getString("ip2", "");
        String callPort2 = MyApp.getInstance().getSharedPreferences().getString("port2", "");

        ip2.setText(callIP2);
        port2.setText(callPort2);

    }

    @Override
    public int rawReading(int bufferSize, byte[] buffer) {
        return 0;
    }

    @Override
    public void weighBridgeValue(String rawValue, String value) {
        Log.e("raw", rawValue);
        Log.e("new Value", value);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outputView1.setText(value);
            }
        });

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void successConnect(String ip, int port) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Connected with " + ip + ":" + port, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void indicatorUnplugged() {

    }

    public void onSubmit(View view) {

        String ipaddress = ip1.getText().toString();
        String getPort = port1.getText().toString();

        WifiSerial.getInstance().setListener(this);
        WifiSerial.getInstance().connect(ipaddress, getPort);

        MyApp.getInstance().getSharedPreferences().edit().putString("ip", ipaddress).apply();
        MyApp.getInstance().getSharedPreferences().edit().putString("port", getPort).apply();
    }

    public void onSubmit2(View view) {

        String ipaddress = ip2.getText().toString();
        String getPort = port2.getText().toString();

        WifiSerial2.getInstance().setListener(this);
        WifiSerial2.getInstance().connect(ipaddress, getPort);

        MyApp.getInstance().getSharedPreferences().edit().putString("ip2", ipaddress).apply();
        MyApp.getInstance().getSharedPreferences().edit().putString("port2", getPort).apply();

    }

    @Override
    public int rawReading2(int bufferSize, byte[] buffer) {
        return 0;
    }

    @Override
    public void weighBridgeValue2(String rawValue, String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outputView2.setText(value);
            }
        });
    }

    @Override
    public void disconnect2() {

    }

    @Override
    public void indicatorUnplugged2() {

    }
}