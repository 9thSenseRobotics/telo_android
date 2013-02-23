package com.denbar.RobotComm;

//Copyright (c) 2012, 9th Sense, Inc.
//All rights reserved.
//
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.


import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class RobotCommActivity extends Activity {

	private static final String TAG = "RobotCommActivity";
	public final static String AUTH_C2DM = "authentication";
	public final static String REGISTERED_C2DM = "registered";

	private com.denbar.RobotComm.RobotCommService serviceBinder = null;
	private EditText editReceivedFromServer, editReceivedFromRobot, editNotes;
	private EditText editSendToRobot, editSentToRobot, editSendToServer,
			editSentToServer;
	// private EditText editHost, editPort, editUserID, editService,
	// editPassword, editBluetooth, editRecipient;
	private Button _btnSleep;
	private EditText editRobotName;
	private EditText editXMPPstatus, editC2DMstatus, editBluetoothStatus,
			editStatus;
	private String robotName, host, port, service, userid, password, bluetooth,
			recipient, recipientForEcho;
	private static final int timerUpdateRate = 500;
	private Timer checkStateTimer;
	private GUItimer MyGUItimer;
	private boolean _sleeping = false;
	private Context _context;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.d(TAG, "in onCreate");
		//Toast.makeText(this, "RobotComm activity created", Toast.LENGTH_SHORT).show();

		_context = this;
		
	    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);

		checkStateTimer = new Timer("checkState"); // setup timer
		MyGUItimer = new GUItimer();

		setContentView(R.layout.main);

		editRobotName = (EditText) findViewById(R.id.user_name);
		editXMPPstatus = (EditText) findViewById(R.id.XMPPstatus);
		editC2DMstatus = (EditText) findViewById(R.id.C2DMstatus);
		editBluetoothStatus = (EditText) findViewById(R.id.bluetoothStatus);
		editStatus = (EditText) findViewById(R.id.status);
		editReceivedFromRobot = (EditText) findViewById(R.id.receivedFromRobot);
		editSentToRobot = (EditText) findViewById(R.id.sentToRobot);
		editSendToRobot = (EditText) findViewById(R.id.sendToRobot);
		editReceivedFromServer = (EditText) findViewById(R.id.receivedFromServer);
		editSentToServer = (EditText) findViewById(R.id.sentToServer);
		editSendToServer = (EditText) findViewById(R.id.sendToServer);
		editNotes = (EditText) findViewById(R.id.Notes);

		// Set a button listener to send a message to the server (chat)
		Button sendToServer = (Button) this.findViewById(R.id.btnSendToServer);
		sendToServer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String text = editSendToServer.getText().toString();
				if (serviceBinder != null)
				{
					if (serviceBinder.messageToServer(text, "a")) { // in the future, this should not be "a", it should be "m"
						// but this gets the response tested
						editSentToServer.setText(text);
						editSendToServer.setText("");
					} else
					editSentToServer.setText("Failed sending text to server");
				// Intent serviceIntent = new Intent();
				// serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
				// serviceIntent.putExtra("messageToServer", text);
				// startService(serviceIntent);
				// editSendToServer.setText("");
				// editSentToServer.setText(text);
				}
			}
		});

		// Set a button listener to tell the robot to sleep/wakeup
		_btnSleep = (Button) this.findViewById(R.id.btnSleep);
		_btnSleep.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if(_sleeping)
				{
					if (serviceBinder == null)
					{
						// bind to service
						// remember that this causes the service to run onCreate
						// but it does not run onStartCommand
						Intent bindIntent = new Intent(
							com.denbar.RobotComm.RobotCommActivity.this,
							com.denbar.RobotComm.RobotCommService.class);
						bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
						Log.d(TAG, "Binding to service.");
						editStatus.setText("Bindng to service");
						// pause 2 seconds
				        final Handler handler = new Handler(); 
				        Timer t = new Timer(); 
				        t.schedule(new TimerTask() { 
				                public void run() { 
				                        handler.post(new Runnable() { 
				                                public void run() { 
				                                	finish();
				                                } 
				                        }); 
				                } 
				        }, 2000); 
					}
					if (serviceBinder != null)  // not an "else" because we want to check this again here
					{
						serviceBinder.wakeUp();
						Log.d(TAG, "waking up, connecting");
						Connect();
						_sleeping = false;
						editStatus.setText("Waking up");
						editBluetoothStatus.setText("Waking up");
						updateGUI();
						_btnSleep.setText("Sleep");
						MyGUItimer.cancel();
						MyGUItimer = new GUItimer();
						checkStateTimer.scheduleAtFixedRate(MyGUItimer, 0, timerUpdateRate);
					}
				}
				else
				{
					_sleeping = true;
					editStatus.setText("Sleeping");
					editBluetoothStatus.setText("Sleeping");
					MyGUItimer.cancel();
					if (serviceBinder != null) serviceBinder.goToSleep();
					_btnSleep.setText("Wake Up");
				}
			}
		});

		// Set a button listener to send a command to the robot
		Button sendToRobot = (Button) this.findViewById(R.id.btnSendToRobot);
		sendToRobot.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String text = editSendToRobot.getText().toString();
				if (serviceBinder != null)
				{
					if (serviceBinder.sendDataToArduino(text)) {
						editSentToRobot.setText(text);
						editSendToRobot.setText("");
					} else
						editSentToRobot.setText("Failed trying to send text to robot");
				}
			}
		});


		// Set a button listener to connect to bluetooth and servers
		Button btnConnect = (Button) this.findViewById(R.id.Connect);
		btnConnect.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Connect();
				// Intent serviceIntent = new Intent();
				// serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
				// serviceIntent.putExtra("Connect", bluetooth);
				// startService(serviceIntent);
				// editStatus.setText("Connecting to bluetooth and servers");

			}
		});

		// Set a button listener to switch to settings activity
		Button btnSettings = (Button) this.findViewById(R.id.Settings);
		btnSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent startIntent = new Intent(_context,
						com.denbar.RobotComm.credentialsActivity.class);
				startActivity(startIntent);
			}
		});
		
		// Set a button listener to switch to standard UI
		Button btnMonitor = (Button) this.findViewById(R.id.Monitor);
		btnMonitor.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				RobotCommApplication.getInstance().setDisplayDetails(false);
				Intent startIntent = new Intent(_context,
						com.denbar.RobotComm.MonitorActivity.class);
				startActivity(startIntent);
			}
		});
		
	} // ends on Create

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "in onStart");
		//Toast.makeText(this, "RobotComm activty started", Toast.LENGTH_SHORT).show();
		getPreferences();
	}

	// We will get the strings needed for logging in from a preferences file
	// that contains the last set used.
	// If this is the first boot, then we will fill in the values
	// from the resource file strings.xml, which means that we can change those
	// values by simply swapping the strings.xml file, no code changes required.

	private void getPreferences() {
		Log.d(TAG, "in getPreferences");
		Resources robotResources = getResources();
		SharedPreferences prefs = getSharedPreferences("RobotPreferences",
				MODE_WORLD_WRITEABLE);

		robotName = prefs.getString("userid", robotResources.getString(R.string.userid));
			// for now, just put username here
			//prefs.getString("robotname", robotResources.getString(R.string.user_name));

		host = prefs.getString("host", robotResources.getString(R.string.host));
		port = prefs.getString("port", robotResources.getString(R.string.port));
		service = prefs.getString("service", robotResources
				.getString(R.string.service));
		userid = prefs.getString("userid", robotResources
				.getString(R.string.userid));
		password = prefs.getString("password", robotResources
				.getString(R.string.password));
		bluetooth = prefs.getString("bluetooth", robotResources
				.getString(R.string.bluetooth));
		recipientForEcho = prefs.getString("recipient", robotResources
				.getString(R.string.sendCommandEchoServerAddress));
		recipient = prefs.getString("recipientForEcho", robotResources
				.getString(R.string.sendMessageToXMPPserverAddress));

		// display status for the user:

		editRobotName.setText(robotName);
		editXMPPstatus.setText("not connected yet");
		editBluetoothStatus.setText("not connected yet");
		editStatus.setText("Robot status is displayed here");
		editReceivedFromRobot.setText("");
		editSentToRobot.setText("");
		editSendToRobot.setText("");
		editReceivedFromServer.setText("");
		editSentToRobot.setText("");
		editSendToServer.setText("");
	}

	private void Connect() {
		Log.d(TAG, "in Connect");
		editStatus.setText("Trying to connect, standby.........");
		if (EntriesTest()) {
			editSendToServer.setText("");
			editSendToRobot.setText("");
			editStatus.setText("Connecting to BT and servers.");
			if (serviceBinder != null) {
				serviceBinder.connectBluetooth(bluetooth); // this blocks until BT connects or times out
				// // might want to run in Async
				serviceBinder.connectXMPP();
				updateGUI();
			} else {
				editStatus.setText("Binding to server failed.");
				Log.d(TAG, "Binding failed");
			}
		} else {
			editStatus.setText("Error in user entries, check setup parameters");
			Log.d(TAG, "Failed entriesTest in Connect");
			Intent startIntent = new Intent(this,
					com.denbar.RobotComm.credentialsActivity.class);
			startActivity(startIntent);
		}
	}



	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(TAG, "in onServiceConnected");
			serviceBinder = ((com.denbar.RobotComm.RobotCommService.MyBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "in onServiceDisconnected");
			serviceBinder = null;
		}
	};

	private boolean EntriesTest() {
		Log.d(TAG, "in EntriesTest");
		boolean returnResult = true;
		String message = "There is a error in ";
		try {
			int portNumber = Integer.parseInt(port);
		} catch (NumberFormatException nfe) {
			message += "port, ";
			returnResult = false;
		}
		if ((!recipient.contains("@")) || (!recipient.contains("."))) {
			message += "recipient, ";
			returnResult = false;
		}
		if ((!recipientForEcho.contains("@"))
				|| (!recipientForEcho.contains("."))) {
			message += "recipientForEcho, ";
			returnResult = false;
		}
		if (!host.contains(".")) {
			message += "host,";
			returnResult = false;
		}
		if (!service.contains(".")) {
			message += "service, ";
			returnResult = false;
		}
		//if ( (!userid.contains("@"))) {
		//	message += "userid,";
		//	returnResult = false;
		//}
		if (!BluetoothAdapter.checkBluetoothAddress(bluetooth)) {
			message += "bluetooth ";
			returnResult = false;
		}
		if (!returnResult) {
			Log.d(TAG, "failed EntriesTest");
			editStatus.setText(message);
		} else
			Log.d(TAG, "passed EntriesTest");
		return returnResult;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "in onResume");
		if (MyGUItimer != null) MyGUItimer.cancel();
		if (serviceBinder == null)
		{
			// bind to service
			// remember that this causes the service to run onCreate
			// but it does not run onServiceCommand
			Intent bindIntent = new Intent(
				com.denbar.RobotComm.RobotCommActivity.this,
				com.denbar.RobotComm.RobotCommService.class);
			bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "Binding to service.");
		}
		MyGUItimer = new GUItimer();
		checkStateTimer.scheduleAtFixedRate(MyGUItimer, 0, timerUpdateRate);
		updateGUI();
		if (!RobotCommApplication.getInstance().getDisplayDetails())
		{
			Intent startIntent = new Intent(this,com.denbar.RobotComm.MonitorActivity.class);
			startActivity(startIntent);
		}
				
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "in onPause");
		if (MyGUItimer != null) MyGUItimer.cancel();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "in onStop");
		if (MyGUItimer != null) MyGUItimer.cancel();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "in onDestroy");
		if (MyGUItimer != null) MyGUItimer.cancel();
		if (serviceConnection != null) {
			unbindService(serviceConnection);
			Log.d(TAG, "Unbinding from service from onDestroy");
			serviceConnection = null;
		}
	}

	// setup timer for updating GUI
	final class GUItimer extends TimerTask {
		public void run() {
			updateGUI();
		}
	}

	private void updateGUI() {
		runOnUiThread(new Runnable() {
			public void run() {
				if (serviceBinder == null) {
					Log.d(TAG, "update GUI called when not bound");
					return;
				}
				editBluetoothStatus.setText(serviceBinder._bluetoothStatus);
				editXMPPstatus.setText(serviceBinder._XMPPstatus);
				editC2DMstatus.setText(serviceBinder._C2DMstatus);
				editStatus.setText("battery percent = " + serviceBinder._batteryPercentage); //_robotStatus);
				editReceivedFromServer.setText(serviceBinder._messageReceivedFromServer);
				editSentToServer.setText(serviceBinder._messageSentToServer);
				editReceivedFromRobot.setText(serviceBinder._messageReceivedFromRobot);
				editSentToRobot.setText(serviceBinder._messageSentToRobot);
				String Notes = "";
				Notes += "Number of notes = " + RobotCommApplication.getInstance().getNumberOfNoteStrings() + "\n";
				for (int i = 1; i <= RobotCommApplication.getInstance().getNumberOfNoteStrings(); i++)
				{
					Notes += RobotCommApplication.getInstance().getNoteString(i) + "\n";
				}
				editNotes.setText(Notes);
				
			}
		});
	}

	// This section is for when we want to show help and user preferences
	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { MenuInflater
	 * inflater = getMenuInflater(); inflater.inflate(R.menu.optionsmenu, menu);
	 * return true; }
	 *
	 * @Override public boolean onOptionsItemSelected(MenuItem item) { // Handle
	 * item selection Intent startIntent; switch (item.getItemId()) {
	 *
	 * case R.id.help: startIntent = new Intent(this,
	 * com.denbar.RobotComm.Help.class); startActivity(startIntent); return
	 * true; default: return super.onOptionsItemSelected(item); } }
	 */

}