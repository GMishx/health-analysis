package com.example.sarkar.smart_bicycle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static java.lang.Math.random;

public class Encryption extends AppCompatActivity {

    public long n,g,x,a,b;

    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /*void onUpdate(View v){
        EditText ge = (EditText) findViewById(R.id.g);
        g = BigInteger.valueOf(Long.parseLong(String.valueOf(ge.getText())));
        mConnectedThread.write(String.valueOf(g)+"g"+'\n');
        x = BigInteger.valueOf((long) (random()*100));
        a = g.modPow(x,n);
        mConnectedThread.write(String.valueOf(a)+"e"+'\n');
    }*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_encryption);
        n = 9973;
        g = 0;
        x = (long) (random()*10000)%9973;

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("}");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        int in;
                        if((in=dataInPrint.indexOf("{"))>-1){
                            Intent returnIntent = new Intent();
                            char c=dataInPrint.charAt(in+1);
                            returnIntent.putExtra("key",String.valueOf(c));
                            setResult(RESULT_OK,returnIntent);
                            finish();
                        }
                        else {
                            a = Long.parseLong(dataInPrint);
                            long k = modPow(a, x, n);
                            mConnectedThread.write(String.valueOf(k));
                            mConnectedThread.write("k");
                            mConnectedThread.write(String.valueOf('\n'));
                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }
                }
            }
        };
    }

    void processIntent(){
        new CountDownTimer(10000,1000){
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                mConnectedThread.write("e");
                mConnectedThread.write(String.valueOf('\n'));
                errorProcess("Timeout");
            }
        }.start();
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        if(extras.containsKey("g")) {       //User wants to unlock
            String value1 = extras.getString("g");
            if (value1 != null) {
                g = Long.parseLong(value1);
                b = modPow(g,x,n);
                mConnectedThread.write(String.valueOf(b));
                mConnectedThread.write("e");
                mConnectedThread.write(String.valueOf('\n'));
            }
        }
        else if(extras.containsKey("l")){   //User wants to lock
            mConnectedThread.write("l");
            mConnectedThread.write(String.valueOf('\n'));
            setResult(RESULT_OK);
            finish();
        }
        else if(extras.containsKey("p")){   //User change password
            String pass = extras.getString("p");
            mConnectedThread.write(pass);
            mConnectedThread.write("g");
            mConnectedThread.write(String.valueOf('\n'));
            setResult(RESULT_OK);
            finish();
        }
        else {
            errorProcess("Unknown operation");
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws Exception {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    @Override
    public void onPause() {
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (Exception e2) {
            //insert code to deal with this
        }
        super.onPause();
    }
    @Override
    public void onDestroy() {
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (Exception e2) {
            //insert code to deal with this
        }
        super.onDestroy();
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (btAdapter == null) {
            errorProcess("Device does not support bluetooth");
        } else {
            if (btAdapter.isEnabled()) {
                startProcessing();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode==1){
            if (resultCode == RESULT_CANCELED) {
                errorProcess("Can't work with Bluetooth disabled");
            } else {
                startProcessing();
            }
        }
    }

    public void startProcessing(){
        BluetoothDevice device = null;
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice mdevice : pairedDevices)
            {
                if(mdevice.getName().equals("SmartCycle"))
                {
                    device = mdevice;
                    break;
                }
            }
        }

        if(device!=null) {

            try {
                btSocket = createBluetoothSocket(device);
            } catch (Exception e) {
                errorProcess("Socket creation failed");
            }
            // Establish the Bluetooth socket connection.
            try {
                btSocket.connect();
            } catch (Exception e) {
                try {
                    btSocket.close();
                } catch (Exception e2) {
                    //insert code to deal with this
                }
                errorProcess("SmartCycle is not connected");
            }
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();
            processIntent();

            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and finish() will be called
        }
        else {
            errorProcess("Pair with SmartCycle first");
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        void write(String input) {
            int l=input.length();
            int i=0;
            while(i<=l-1){
                char c = input.charAt(i++);
                byte[] msgBuffer = String.valueOf(c).getBytes();             //converts entered String into bytes
                try {
                    mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
                    mmOutStream.flush();
                } catch (IOException e) {
                    //if you cannot write, close the application
                    errorProcess("Connection Failure");
                }
            }
            /*byte[] msgBuffer = input.getBytes();             //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
                mmOutStream.flush();
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
            }*/
        }
    }

    public long modPow(long base, long exponent, long modulus)
    {
        // To do this naively by first raising this to the power of exponent
        // and then performing modulo m would be extremely expensive, especially
        // for very large numbers.  The solution is found in Number Theory
        // where a combination of partial powers and moduli can be done easily.
        //
        // We'll use the algorithm for Additive Chaining which can be found on
        // p. 244 of "Applied Cryptography, Second Edition" by Bruce Schneier.
        if ((base < 1) || (exponent < 0) || (modulus < 1)) {
            throw new ArithmeticException("invalid");
        }
        long result = 1;
        while (exponent > 0) {
            if ((exponent % 2) == 1) {
                result = (result * base) % modulus;
            }
            base = (base * base) % modulus;
            exponent >>= 1;
        }
        return (result);
    }
    void errorProcess(String err){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("error",err);
        setResult(RESULT_CANCELED,returnIntent);
        this.finish();
    }
}
