package y.u.perido;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class WifiSerial2 {

    private static boolean connected = false;

    private static String BMX_BLUETOOTH = "BMXBluetooth";

    public static String BLUETOOTH_CONNECTED = "bluetooth-connection-started";

    public static String BLUETOOTH_DISCONNECTED = "bluetooth-connection-lost";

    public static String BLUETOOTH_FAILED = "bluetooth-connection-failed";

    private static Socket socket;

    private static InputStream serialInputStream;

    private static OutputStream serialOutputStream;

    private static SerialReader serialReader;

    private static String devicePrefix;

    private connectionTask theTask;

    public static String ipAddress, port;

    public interface ConnectionListener {
        int rawReading2(int bufferSize, byte[] buffer);
        void weighBridgeValue2(String rawValue, String value);
        void disconnect2();
        void successConnect(String ip, int port);
        void indicatorUnplugged2();
    }

    private static WifiSerial2 instance;
    private static ConnectionListener mListener;
    private WifiSerial2(){};

    public static WifiSerial2 getInstance() {
        if (instance == null) {
            instance = new WifiSerial2();
        }

        return instance;
    }

    public void setListener(ConnectionListener listener) {
        mListener = listener;
    }

    public void stopBluetooth() {
//        onPause();
        close();
    }

    public void connectBluetooth(String devicePrefix) {
        this.devicePrefix = devicePrefix;
        onResume();
    }

    public boolean isConnected() {
        return connected;
    }

    public void onResume() {
        //listen for bluetooth disconnect
//        IntentFilter disconnectIntent = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//        context.registerReceiver(bluetoothReceiver, disconnectIntent);

        //reestablishes a connection is one doesn't exist
        if(!connected){
            connect(ipAddress, port);
        } else {
//            Intent intent = new Intent(BLUETOOTH_CONNECTED);
//            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }


    /**
     * Initializes the bluetooth serial connections, uses the LocalBroadcastManager when
     * connection is established
     *
     //     * @param localBroadcastManager
     * @param ipAddress
     */
    public void connect(String ipAddress, String port){

        this.ipAddress = ipAddress;
        this.port = port;

        if (connected){
            Log.e(BMX_BLUETOOTH,"Connection request while already connected");
            return;
        }

        if (theTask != null && theTask.getStatus() == AsyncTask.Status.RUNNING){
            Log.e(BMX_BLUETOOTH,"Connection request while attempting connection");
            return;
        }

        new connectionTask().execute(ipAddress, port);
    }

    private static class connectionTask extends AsyncTask<String, Void, Boolean>{

        int MAX_ATTEMPTS = 30;

        int attemptCounter = 0;

        @Override
        protected Boolean doInBackground(String... params) {
            while(!isCancelled()){ //need to kill without calling onCancel

                try {
                            try {

                                socket = new Socket(params[0], Integer.parseInt(params[1]));

                                //setup the connect streams
                                serialInputStream = socket.getInputStream();
                                serialOutputStream = socket.getOutputStream();

                                connected = true;

                                return connected;

                            } catch (Exception e) {
                                serialInputStream=null;
                                serialOutputStream=null;
                                Log.i(BMX_BLUETOOTH, e.getMessage());
                            }
                } catch (Exception e) {
                    Log.e("jahanam", "hee");
                    e.printStackTrace();
                }

                try {
                    attemptCounter++;
                    if (attemptCounter>MAX_ATTEMPTS)
                        this.cancel(false);
                    else
                        Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            Log.i(BMX_BLUETOOTH, "Stopping connection attempts");

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            //start thread responsible for reading from inputstream
            serialReader = new SerialReader();
            serialReader.start();

            mListener.successConnect(ipAddress, Integer.parseInt(port));

            //send connection message
//                    Intent intent = new Intent(BLUETOOTH_CONNECTED);
//                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

    };

    // see: http://stackoverflow.com/questions/3397071/service-discovery-failed-exception-using-bluetooth-on-android
    private static BluetoothSocket connectViaReflection(BluetoothDevice device) throws Exception {
        Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
        return (BluetoothSocket) m.invoke(device, 1);
    }

    public static int available() throws IOException {
        if (connected)
            return serialInputStream.available();

        Log.e(BMX_BLUETOOTH, "available() is connected : " + connected);

        throw new RuntimeException("Connection lost, reconnecting now.");
    }

    public int read() throws IOException{
        if (connected)
            return serialInputStream.read();

        throw new RuntimeException("Connection lost, reconnecting now.");
    }

    public int read(byte[] buffer) throws IOException{
        if (connected)
            return serialInputStream.read(buffer);

        Log.e(BMX_BLUETOOTH, "read() 1 is connected : " + connected);

        throw new RuntimeException("Connection lost, reconnecting now.");
    }

    public static int read(byte[] buffer, int byteOffset, int byteCount) throws IOException{
        if (connected)
            return serialInputStream.read(buffer, byteOffset, byteCount);

        Log.e(BMX_BLUETOOTH, "read() 2 is connected : " + connected);

        throw new RuntimeException("Connection lost, reconnecting now.");
    }

    public void write(byte[] buffer) throws IOException{
        if (connected)
            serialOutputStream.write(buffer);

        Log.e(BMX_BLUETOOTH, "read() 3 is connected : " + connected);

        throw new RuntimeException("Connection lost, reconnecting now.");
    }

    public void write(int oneByte) throws IOException{
        if (connected)
            serialOutputStream.write(oneByte);

        throw new RuntimeException("Connection lost, reconnecting now.");
    }

    public void write(byte[] buffer, int offset, int count) throws IOException {
        serialOutputStream.write(buffer, offset, count);

        throw new RuntimeException("Connection lost, reconnecting now.");
    }

    private static class SerialReader extends Thread {
        private static final int MAX_BYTES = 125;

        byte[] buffer = new byte[MAX_BYTES];

        int bufferSize = 0;

        public void run() {
            Log.i("serialReader", "Starting serial loop");
            while (!isInterrupted()) {
                try {

                    /*
                     * check for some bytes, or still bytes still left in
                     * buffer
                     */
                    if (available() > 0){

                        int newBytes = read(buffer, bufferSize, MAX_BYTES - bufferSize);
                        if (newBytes > 0)
                            bufferSize += newBytes;

//                        Log.d(BMX_BLUETOOTH, "read " + newBytes);

                        try {

                            String str = new String(buffer, StandardCharsets.UTF_8).toLowerCase().trim().replaceAll("\\s", "").replaceAll("\\D", "|");

                            String newFormat = str;

                            try {
                                newFormat = str.split(Pattern.quote("||||"))[1];
                                newFormat = newFormat.substring(2, 8);
                            } catch (Exception e) {
                                newFormat = str.split(Pattern.quote("|||"))[1];
                                newFormat = newFormat.substring(1, 8);
                            }

//                            Log.e("wifiserial", str);

                            mListener.weighBridgeValue2(str, newFormat);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mListener.rawReading2(bufferSize, buffer);
                        buffer = new byte[MAX_BYTES];
                        bufferSize = 0;
                    } else {
//                        Log.e("lembu line 381", "kah kah");
                    }

                    if (bufferSize > 0) {
                        int read = mListener.rawReading2(bufferSize, buffer);

                        // shift unread data to start of buffer
                        if (read > 0) {
                            int index = 0;
                            for (int i = read; i < bufferSize; i++) {
                                buffer[index++] = buffer[i];
                            }
                            bufferSize = index;
                        }
                    } else {

                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                } catch (Exception e) {
//                    Log.e(BMX_BLUETOOTH, "Error reading serial data", e);
                }
            }
            Log.i(BMX_BLUETOOTH, "Shutting serial loop");
        }
    };

    /**
     * Reads from the serial buffer, processing any available messages.  Must return the number of bytes
     * consumer from the buffer
     *
     * @author jpetrocik
     *
     */

    public void close() {

        connected = false;

        if (theTask != null) {
            theTask.cancel(false);
        }

        if (serialReader != null) {
            serialReader.interrupt();

            try {
                serialReader.join(1000);
            } catch (InterruptedException ie) {}
        }

        try {
            serialInputStream.close();
        } catch (Exception e) {
            Log.e(BMX_BLUETOOTH, "Failed releasing inputstream connection");
        }

        try {
            serialOutputStream.close();
        } catch (Exception e) {
            Log.e(BMX_BLUETOOTH, "Failed releasing outputstream connection");
        }

        try {
            socket.close();
        } catch (Exception e) {
            Log.e(BMX_BLUETOOTH, "Failed closing socket");
        }

        Log.i(BMX_BLUETOOTH, "Released bluetooth connections");
        mListener.disconnect2();

    }

}
