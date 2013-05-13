package com.example.acoloid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
	
	//variables for bluetooth connection
	BluetoothAdapter btAdapter;
	Set<BluetoothDevice> pairedDevices;
	ArrayList<String> btAdapterArray;
	ArrayList<BluetoothDevice> pbtDevices;
	private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	ConnectedThread conThread;
	
	//Color Picker
	ColorPickerView cpicker;
	
	//color in a byte array
	byte[] b = new byte[3];
	
	@Override
    public void onCreate(Bundle savedInstanceState) {         
       super.onCreate(savedInstanceState);  
    
       setContentView(R.layout.activity_main);
       //initBluetooth();
       configureColorPicker();
   }

	@SuppressLint("NewApi")
	private void configureColorPicker() {
		//init
		cpicker = (ColorPickerView)findViewById(R.id.colorPickerView1);

		/*redBar =(SeekBar)findViewById(R.id.seekBar1);
		greenBar =(SeekBar)findViewById(R.id.seekBar2);
		blueBar =(SeekBar)findViewById(R.id.seekBar3);*/
		//set maximum
		/*redBar.setMax(255);
		greenBar.setMax(255);
		blueBar.setMax(255);
		//set the ChangeListener
		redBar.setOnSeekBarChangeListener(this);
		greenBar.setOnSeekBarChangeListener(this);
		blueBar.setOnSeekBarChangeListener(this);*/
	}

	private void initBluetooth() {
		// Check if bluetooth is available on this device
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
		    Toast.makeText(getApplicationContext(), "Nice try but bluetooth is not on your phone...", Toast.LENGTH_SHORT).show();
		    finish();
		}
		// Check if bluetooth is enabled
		if (!btAdapter.isEnabled()) {
		    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(intent, 1);
		}
		
		//Search for pairedDevices
		pairedDevices = btAdapter.getBondedDevices();
		btAdapterArray = new ArrayList<String>();
		pbtDevices = new ArrayList<BluetoothDevice>();
		// if exists
		if (pairedDevices.size() > 0) {
		    // add to an array
		    for (BluetoothDevice device : pairedDevices) {
		        btAdapterArray.add(device.getName() + "\n" + device.getAddress());
		        pbtDevices.add(device);
		    }
		}
		
		Log.i("acoloid", btAdapterArray.get(0));
		
		//Start discovery (optional)
		
		//Connect to a remote Device
		ConnectThread cThread = new ConnectThread(pbtDevices.get(0));
		cThread.start();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		   if (resultCode == Activity.RESULT_OK) {
		      // Bluetooth is enabled.
		   } else {
			   Toast.makeText(getApplicationContext(), "Bluetooth must be enabled!", Toast.LENGTH_SHORT).show();
			   finish();
		   }
		}
	
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(uuid);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	    	Log.i("acoloid", "run...");
	        // Cancel discovery because it will slow down the connection
	        btAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	        	Log.i("acoloid", "Connection failed...");
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) {
	            	
	            }
	            return;
	        }
	 
	        conThread = new ConnectedThread(mmSocket);
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	 
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}

	@Override
	public void onProgressChanged(SeekBar seekbar, int progress, boolean arg2) {
	   /* switch (seekbar.getId()) {
	    case R.id.seekBar1:
	    	if(conThread != null){
	    		b[0] = (byte) progress;
	    		conThread.write(b);
	    	}
	    	else{
	    		Toast.makeText(getApplicationContext(), "The connection is not established yet", Toast.LENGTH_LONG).show();
	    	}
	    	break;
	    case R.id.seekBar2:
	    	if(conThread != null){
	    		b[1] = (byte) progress;
	    		conThread.write(b);
	    	}
	    	else{
	    		Toast.makeText(getApplicationContext(), "The connection is not established yet", Toast.LENGTH_LONG).show();
	    	}
	    	break;
	    case R.id.seekBar3:
	    	if(conThread != null){
	    		b[2] = (byte) progress;
	    		conThread.write(b);
	    	}
	    	else{
	    		Toast.makeText(getApplicationContext(), "The connection is not established yet", Toast.LENGTH_LONG).show();
	    	}
	    	break;
	    }*/
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}
	
}