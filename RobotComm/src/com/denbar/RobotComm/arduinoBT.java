package com.denbar.RobotComm;

//	  Copyright (c) 2012, 9th Sense, Inc.
//	  All rights reserved.
//
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as published by
//     the Free Software Foundation, either version 3 of the License, or
//     (at your option) any later version.
// 
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
// 
//     You should have received a copy of the GNU General Public License
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class arduinoBT {
	private static final String TAG = "ArduinoBT";
	private BluetoothDevice _bluetoothTarget;
	private BluetoothAdapter _bluetoothAdapter;
	private BluetoothSocket _socket;
	private OutputStream _outputStream;
	private InputStream _inputStream;
	Thread workerThread;
	byte[] readBuffer;
	String _lastMessage = "x";
	long _lastCommandSentToArduinoTime;
	long MIN_TIME_BETWEEN_ARDUINO_COMMANDS = 100;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker, _checkReceived = false;
	boolean _isConnected;
	String _BTaddress;
	Context _context;
	public String _echoData = "";
	// common machine UUID that we need to communicate with FireFly Bluetooth
	// module:
	private static final UUID uuid = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	public arduinoBT(Context CallingContext) {
		_context = CallingContext;
		_BTaddress = "invalid address"; // just so it is not an empty string
		_isConnected = false;
		_lastCommandSentToArduinoTime = System.currentTimeMillis();
	}

	public void setBTaddress(String BTaddress) {
		_BTaddress = BTaddress;
	}

	public String getBTaddress() {
		return _BTaddress;
	}

	public boolean getConnectionState() {
		return _isConnected;
	}

	public void closeBT() throws IOException {
		Log.d(TAG, "closeBT: closing BT connection");
		stopWorker = true;
		if (_outputStream != null)
			_outputStream.close();
		if (_inputStream != null)
			_inputStream.close();
		if (_socket != null)
			_socket.close();
		_socket = null;
		_isConnected = false;
	}

	public boolean resetConnection() {
		// start by forcing close
		try {
			closeBT();
		} catch (IOException ex) {
			Log.d(TAG, "closeBT returned exception" + ex);
		}
		_isConnected = false;
		if (!Connect())
			return false;
		return true;
	}

	public boolean Connect() {
		if (_isConnected) {
			// we got a call to Connect() even though we think we are already
			// connected
			Log.d(TAG, "Connect requested when already connected");
			if (checkConnection()) return true;
			else _isConnected = false;
		}
		try {
			if (!findBT()) {
				Log.d(TAG, "findBT returned false");
				return false;
			}
		} catch (IOException ex) {
			Log.d(TAG, "FindBT returned exception" + ex);
			return false;

		}
		RobotCommApplication.getInstance().addNoteString("Trying to connect to bluetooth...");

		try {
			if (!openBT()) {
				Log.d(TAG, "openBT returned false");
				RobotCommApplication.getInstance().addNoteString("Unable to find the paired robot's bluetooth device");
				return false;
			}
		} catch (IOException ex) {
			Log.d(TAG, "OpenBT returned exception" + ex);
			RobotCommApplication.getInstance().addNoteString("OpenBT returned an Android exception" + ex);
			return false;
		}
		_isConnected = true;
		ListenForData();
		Log.d(TAG, "OpenBT: BT connected");
		RobotCommApplication.getInstance().addNoteString("Bluetooth successfully connected");
		return true;
	}

	private boolean findBT() throws IOException {
		_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (_bluetoothAdapter == null) {
			Log.d(TAG, "No BT adapter available");
			return false;
		}
		if (!_bluetoothAdapter.isEnabled()) {
			if (_bluetoothAdapter.enable()) {
				// The adapter state will immediately transition from STATE_OFF
				// to STATE_TURNING_ON,
				// and some time later transition to either STATE_OFF or
				// STATE_ON.
				// If this call returns false then there was an immediate
				// problem that
				// will prevent the adapter from being turned on - such as
				// Airplane mode,
				// or the adapter is already turned on.
				// So, basically, the bluetooth device is turning on and we need
				// to wait for it to finish
				int counter = 0;
				while (_bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
					}
					if (counter > 3) {
						return false;
					}
					counter++;
				}

				if (_bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON)
					Log.d(TAG, "BT sucessfully powered up");
				else {
					if (_bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON)
						Log.d(TAG, "BT is turning on, wait a bit and try again");
					else
						Log.d(TAG, "BT failed to turn on");
					return false;
				}
			} else {
				Log.d(TAG, "BT did not enable");
				return false;
			}
		} else
			Log.d(TAG, "BT previously enabled");

		// get remote BTs that are paired with us
		Set<BluetoothDevice> pairedDevices = _bluetoothAdapter.getBondedDevices();
		if (pairedDevices == null) {
			Log.d(TAG, "No paired BT devices");
			RobotCommApplication.getInstance().addNoteString("No paired BT devices");
			return false;
		}
		if (pairedDevices.size() > 0) {
			RobotCommApplication.getInstance().addNoteString("We are paired to the following BT devices: ");
			Log.d(TAG, "We are paired to the following BT devices: ");
			for (BluetoothDevice device : pairedDevices) {
				//if (device.getName().startsWith("FireFly-")
				//		|| device.getName().startsWith("RN42-")) {
					Log.d(TAG, device.getName() + " device address is " + device.getAddress());
					RobotCommApplication.getInstance().addNoteString(device.getName() + " device address is " + device.getAddress());
					if (_BTaddress.equals(device.getAddress()))	// we are paired with the robot's bluetooth-- yay!
					{
						_bluetoothTarget = device;
					}
				//}
			}
			if (_bluetoothTarget != null)
			{
				Log.d(TAG,"We are paired with the robot's bluetooth, it's address is " + _bluetoothTarget.getAddress() + " matching our target " + _BTaddress);
				RobotCommApplication.getInstance().addNoteString("We are paired with the robot's bluetooth, it's address is " + _bluetoothTarget.getAddress() + " matching our target " + _BTaddress);				
			}
			else
			{
				Log.d(TAG,"No paired device matches with the robot's bluetooth address: " + _BTaddress);
				RobotCommApplication.getInstance().addNoteString("No paired device matches with the robot's bluetooth address: " + _BTaddress);
				return false;
			}				
		} else {
			Log.d(TAG, "BT adapter is not STATE_ON so we could not list paired BT devices");
			RobotCommApplication.getInstance().addNoteString("BT is not enabled, please turn on the tablet's bluetooth");
			return false;
		}

		return true;
		// for some reason, the device found through the above routine does not
		// work properly
		// when we use it and call connect, it generates an exception
		// "Service failed discovery"
		// so we have to find the device using the MAC address instead.
/*
		if (BluetoothAdapter.checkBluetoothAddress(_BTaddress)) {
			_bluetoothTarget = _bluetoothAdapter.getRemoteDevice(_BTaddress);
			// A BluetoothDevice will always be returned for a valid hardware address,
			// even if this adapter has never seen that device
			// so we need to check that we are paired with this one
			if (_bluetoothTarget.getBondState() == BluetoothDevice.BOND_BONDED) {
				Log.d(TAG, "Our arduino BT device is recognized as paired");
				RobotCommApplication.getInstance().addNoteString("Our arduino BT device is recognized as paired");
			} else {
				Log.d(TAG, "The BT found with our address is not paired");
				RobotCommApplication.getInstance().addNoteString("The BT found with our address is not paired");
				return false;
			}
		} else {
			Log.d(TAG, "invalid BT address");
			RobotCommApplication.getInstance().addNoteString("invalid BT address");
			return false;
		}
		return true;
		*/
	}

	// open the connection
	private boolean openBT() throws IOException {
		// in case discovery is still going on,
		// it is important to cancel it before trying to connect or the connect
		// may timeout.
		if (_bluetoothAdapter != null)
			_bluetoothAdapter.cancelDiscovery();
		else {
			Log.d(TAG, "trying to connect without a BT adapter");
			RobotCommApplication.getInstance().addNoteString("trying to connect without a BT adapter");
			return false;
		}
		// if the socket was used before, we have to close it before trying to reconnect
		// otherwise _scoket.connect() throws exception java.io.IOException:
		// Device or resource busy
		if (_socket != null) {
			try {
				_socket.close();
			} catch (IOException ex) {
				Log.d(TAG, "socket close exeception" + ex);
				RobotCommApplication.getInstance().addNoteString("socket close returned exception " + ex);
			}
		}

		try {
			//_socket = _bluetoothTarget.createRfcommSocketToServiceRecord(uuid);
			_socket = _bluetoothTarget.createInsecureRfcommSocketToServiceRecord(uuid);
		} catch (IOException ex) {
			Log.d(TAG, "createRfcomm returned exception " + ex);
			RobotCommApplication.getInstance().addNoteString("createRfcomm returned exception " + ex);
			return false;
		}

		RobotCommApplication.getInstance().addNoteString("socket created successfully");
		// if we try to connect multiple times very fast
		// _socket.connect still throws the exception: java.io.IOException:
		// Service discovery failed
		// but the app survives and works OK if we just try connecting again
		try {
			_socket.connect();
		} catch (IOException ex) {
			Log.d(TAG, "socket.connect returned exception " + ex);
			RobotCommApplication.getInstance().addNoteString("socket.connect returned exception " + ex);
			return false;
		}
		RobotCommApplication.getInstance().addNoteString("socket connected successfully");
		try {
			_outputStream = _socket.getOutputStream();
			_inputStream = _socket.getInputStream();
		} catch (IOException ex) {
			Log.d(TAG, "getting streams returned exception " + ex.getMessage());
			RobotCommApplication.getInstance().addNoteString("getting streams returned exception " + ex.getMessage());
			return false;
		}
		return true;
	}

	
	private boolean checkConnection()
	{
		_checkReceived = false;
		if (!sendMessage("check")) return false;
		// now we need to wait for a response
		Log.d(TAG, "waiting for bluetooth response ");
	    // SLEEP 2 seconds HERE ...
		long counter = 0;
		boolean noDataYet = true;
		int bytesAvailable = 0;
		while (counter < 100000 && noDataYet)
		{
			try {
				bytesAvailable = _inputStream.available();
			}
			catch (IOException ex) {
			}
			if (bytesAvailable > 0) 
			{
				Log.d(TAG, "bluetooth connection checked good ");
				RobotCommApplication.getInstance().addNoteString("bluetooth connection checked good ");
				return true;
			}
			counter++;
		}
		Log.d(TAG, "bluetooth connection checked bad");
		RobotCommApplication.getInstance().addNoteString("bluetooth connection checked bad ");
		return false;
	    /*
	    Handler handler = new Handler(); 
	    handler.postDelayed(new Runnable() { 
	         public void run() { 
	     	    if (_checkReceived) Log.d(TAG, "bluetooth connection checked good ");
	    	    else Log.d(TAG, "bluetooth connection checked bad ");
	         } 
	    }, 2000); 
	    if (_checkReceived) Log.d(TAG, "2nd out bluetooth connection checked good ");
	    else Log.d(TAG, "2nd out bluetooth connection checked bad ");
	    return _checkReceived;	
	    */
	}
	
	
	
	
	// send a message to the arduino
	public boolean sendMessage(String msg) {
		if (_outputStream == null) {
			Log.d(TAG, "tried to send message with null outputStream ");
			return false;
		}
		 if (System.currentTimeMillis() - _lastCommandSentToArduinoTime < MIN_TIME_BETWEEN_ARDUINO_COMMANDS)
		{
			Log.d(TAG, "Waiting for min time until we can send a command to the arduino");
			while (System.currentTimeMillis() - _lastCommandSentToArduinoTime  < MIN_TIME_BETWEEN_ARDUINO_COMMANDS);
			Log.d(TAG, "Finished waiting for min time until we can send a command to the arduino");
		}
		_lastCommandSentToArduinoTime = System.currentTimeMillis();
		_lastMessage = msg;
		String msgTest = msg + "#";
		try {
			byte[] byteString = (msgTest + " ").getBytes();
			byteString[byteString.length - 1] = 0;				//  *********** might not need this *************
			_outputStream.write(byteString);
			Log.d(TAG, "sendMessage to arduino successfully: " + msgTest + " at time " + System.currentTimeMillis());
		} catch (IOException e) {
			Log.d(TAG, "exception from sendMessage: " + e.getMessage());
			return false;
		}
		return true;
	}

	private void ListenForData() {
		final Handler handler = new Handler();
		final byte delimiter = 10; // This is the ASCII code for a newline character

		stopWorker = false;
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		for (int i = 0; i < 1024; i++) readBuffer[i] = 0;
		workerThread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted() && !stopWorker) {
					try {
						int bytesAvailable = _inputStream.available();
						if (bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							_inputStream.read(packetBytes);
							for (int i = 0; i < bytesAvailable; i++) {
								byte b = packetBytes[i];
								if (b == 0 || b == delimiter)
								{
									if ( b== 0) Log.d(TAG, "character " + i + " in message from arduino was null!");
									else if (b == delimiter) Log.d(TAG, "character " + i + " was delimiter, bytesAvailable = " + bytesAvailable);
									Log.d(TAG, "buffer = " + readBuffer + " packetBytes = " + packetBytes);								
								}
								if (b == delimiter) {
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0,
											encodedBytes, 0,
											encodedBytes.length);
									final String data = new String(
											encodedBytes, "US-ASCII");
									readBufferPosition = 0;
									handler.post(new Runnable() {
										public void run() {
											Log.d(TAG, "message from arduino: "	+ data);
											if (data.contains("check"))
											{
												_checkReceived = true;	// bluetooth connection check
											}
											else
											{
												Intent serviceIntent = new Intent();
												serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
												serviceIntent.putExtra("messageFromRobot", data);
												_context.startService(serviceIntent);
											}
										}
									});
								} else {
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					} catch (IOException ex) {
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
	}
}
