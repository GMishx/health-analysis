package com.example.sarkar.smart_bicycle;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class RideActivity extends AppCompatActivity {

    TextView pulse, sp;
    DatabaseHandler db;
    ProgressDialog progress = null;
    ProgressBar pb = null;
    TextView per = null;
    protected int max_BPM = 220;
    ConnectedThread mConnectedThread;

    volatile protected int BPM = 0;

    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private int id;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        db = new DatabaseHandler(getBaseContext());

        progress = new ProgressDialog(this);
        progress.setTitle("Connecting");
        progress.setMessage("Wait while connecting...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();

        //Link the buttons and textViews to respective views
        pulse = (TextView) findViewById(R.id.BPM);
        sp = (TextView) findViewById(R.id.speed);
        pb = (ProgressBar)findViewById(R.id.progressBar);
        per = (TextView)findViewById(R.id.percent);
        id = db.userExist();
        if(id>-1) {
            max_BPM = 220 - db.getUserData(id).getAge();
        }
        ((TextView)findViewById(R.id.maxPulse)).setText("Your max pulse: " + max_BPM + "BMP");

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("}")+1;                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        //int dataLength = dataInPrint.length();                          //get length of data received
                        JSONObject jobb = null;
                        try {
                            jobb = new JSONObject(dataInPrint);
                        } catch (JSONException | NullPointerException e) {
                            e.printStackTrace();
                        }
                        try {
                            assert jobb != null;
                            float speed;
                            try {
                                BPM = Integer.parseInt(jobb.getString("BPM"));
                                speed = Float.valueOf(jobb.getString("speed"));
                                rideData ride = new rideData(BPM,speed);
                                db.addRide(ride);
                                if(id>-1){
                                    float dist = db.getDistance(id);
                                    float ndist = 0;
                                    try{
                                        ndist = (ride.getSpeed() /(float)3600);
                                    }catch (ArithmeticException e){
                                        e.printStackTrace();
                                    }
                                    db.updateDistance(id,dist+ndist);
                                }
                                pulse.setText(String.format(Locale.ENGLISH, "%d", ride.getBPM()));
                                sp.setText(String.format(Locale.ENGLISH, "%.2f", ride.getSpeed()));
                                int BPMper = (int) (((float) ride.getBPM() / (float) max_BPM) * 100);
                                if (BPMper < 60) {
                                    pb.setProgressDrawable(getBaseContext().getResources().getDrawable(R.drawable.progress_circle_accent));
                                    per.setTextColor(getResources().getColor(R.color.colorAccent));
                                } else if (BPMper >= 60 && BPMper <= 80) {
                                    pb.setProgressDrawable(getBaseContext().getResources().getDrawable(R.drawable.progress_circle_green));
                                    per.setTextColor(getResources().getColor(R.color.colorPrimary));
                                } else {
                                    pb.setProgressDrawable(getBaseContext().getResources().getDrawable(R.drawable.progress_circle_red));
                                    per.setTextColor(getResources().getColor(R.color.colorRed));
                                }
                                pb.setProgress(BPMper);
                                per.setText(String.format(Locale.ENGLISH, "%d", BPMper).concat("%"));
                            }catch (NumberFormatException e){
                                e.printStackTrace();
                            }
                        } catch (JSONException | NullPointerException e) {
                            e.printStackTrace();
                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }
                }
            }
        };

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        /*btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("0");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("1");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws Exception {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBTState();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            mConnectedThread.write("t");
            btSocket.close();
        } catch (Exception e2) {
            //insert code to deal with this
            e2.printStackTrace();
        }
        this.finish();
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
            this.finish();
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
                Toast.makeText(getBaseContext(), "Can't work with Bluetooth disabled", Toast.LENGTH_LONG).show();
                this.finish();
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
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
                this.finish();
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
                Toast.makeText(getBaseContext(), "SmartCycle is not connected", Toast.LENGTH_LONG).show();
                this.finish();
            } finally {
                progress.dismiss();
            }
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and this.finish() will be called
            mConnectedThread.write("r");
        }
        else {
            Toast.makeText(getBaseContext(), "Pair with SmartCycle first", Toast.LENGTH_LONG).show();
            this.finish();
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
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
                mmOutStream.flush();
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
            }
        }
    }
}
