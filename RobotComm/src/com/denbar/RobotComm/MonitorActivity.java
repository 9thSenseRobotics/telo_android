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

//import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MonitorActivity extends Activity {

	private static final String TAG = "MonitorActivity";

	private Button _btnSleep;
	private EditText editUserID;
	private EditText editXMPPstatus, editC2DMstatus, editBluetoothStatus;
	private String  bluetooth, userid;
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

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);
		
		_context = this;

		checkStateTimer = new Timer("checkState"); // setup timer
		MyGUItimer = new GUItimer();

		setContentView(R.layout.monitor);

		editUserID = (EditText) findViewById(R.id.userid);
		editXMPPstatus = (EditText) findViewById(R.id.XMPPstatus);
		//editC2DMstatus = (EditText) findViewById(R.id.C2DMstatus);
		editBluetoothStatus = (EditText) findViewById(R.id.bluetoothStatus);

		// Set a button listener to tell the robot to sleep/wakeup
		/*
		_btnSleep = (Button) this.findViewById(R.id.Sleep);
		_btnSleep.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent serviceIntent = new Intent();
				
					serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
					if(_sleeping)
					{
						serviceIntent.putExtra("SleepState", "WakeUp");
						_sleeping = false;
						_btnSleep.setText("Sleep");
					}
					else
					{
						serviceIntent.putExtra("SleepState", "Sleep");
						_sleeping = true;
						_btnSleep.setText("Wake Up");
					}
					_context.startService(serviceIntent);

			}
		});
		
		
		// Set a button listener to reset the connection to bluetooth and servers
		Button btnReset = (Button) this.findViewById(R.id.Reset);
		btnReset.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				editBluetoothStatus.setText("Trying to reset...");
				RobotCommApplication.getInstance().setBluetoothStatus("Trying to reset...");
				editXMPPstatus.setText("Trying to reset...");
				RobotCommApplication.getInstance().setXMPPstatus("Trying to reset...");
				Intent serviceIntent = new Intent();
				serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
				serviceIntent.putExtra("reset", "userCommanded");
				startService(serviceIntent);
			}
		});
	    */

		// Set a button listener to connect to bluetooth and servers
		Button btnConnect = (Button) this.findViewById(R.id.Connect);
		btnConnect.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				editBluetoothStatus.setText("Trying to connect...");
				RobotCommApplication.getInstance().setBluetoothStatus("Trying to connect...");
				editXMPPstatus.setText("Trying to connect...");
				RobotCommApplication.getInstance().setXMPPstatus("Trying to connect...");
				Intent serviceIntent = new Intent();
				serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
				serviceIntent.putExtra("Connect", bluetooth);
				startService(serviceIntent);
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
		
		// Set a button listener to go to details page
		Button btnDetails = (Button) this.findViewById(R.id.Details);
		btnDetails.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				RobotCommApplication.getInstance().setDisplayDetails(true);
				finish();
			}
		});

		// Set a button listener to switch to local driving UI
		Button btnDriving = (Button) this.findViewById(R.id.Drive);
		btnDriving.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				RobotCommApplication.getInstance().setDisplayDetails(false);
				Intent startIntent = new Intent(_context,
						com.denbar.RobotComm.localDriving.class);
				startActivity(startIntent);
			}
		});		
		
		
	} // ends on Create

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "in onStart");
	//	updateGUI();
	}



	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "in onResume");
		Resources robotResources = getResources();
		SharedPreferences prefs = getSharedPreferences("RobotPreferences",
				MODE_WORLD_WRITEABLE);

		userid = prefs.getString("userid", robotResources.getString(R.string.userid));
		bluetooth = prefs.getString("bluetooth", robotResources.getString(R.string.bluetooth));
		
		editUserID.setText(userid);
		if (MyGUItimer != null) MyGUItimer.cancel();
		MyGUItimer = new GUItimer();
		checkStateTimer.scheduleAtFixedRate(MyGUItimer, 0, timerUpdateRate);
		updateGUI();
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
				editBluetoothStatus.setText(RobotCommApplication.getInstance().getBluetoothStatus());
				editXMPPstatus.setText(RobotCommApplication.getInstance().getXMPPstatus());
			}
		});
	}
}
