package com.denbar.RobotComm;

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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RobotCommActivity extends Activity {

    private EditText editReceivedFromServer, editReceivedFromRobot;
    private EditText editSendToRobot, editSentToRobot, editSendToServer, editSentToServer;
    private EditText editHost, editPort, editUserID, editService, editPassword, editBluetooth;
    private EditText editRecipient, editRobotName;
    private EditText editXMPPstatus, editBluetoothStatus, editStatus;
    private String robotName, host, port, service, userid, password, bluetooth, recipient;
    private static final int timerUpdateRate = 5000;
	private Timer checkStateTimer;
	private GUItimer MyGUItimer;
	private Context _context;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Toast.makeText(this, "RobotComm activity created", Toast.LENGTH_SHORT).show();

		checkStateTimer = new Timer("checkState");	// setup timer
		MyGUItimer = new GUItimer();

		_context = this;

        Log.i("RobotComm", "onCreate called");
        setContentView(R.layout.main);

        editRobotName = (EditText) findViewById(R.id.robotname);
        editXMPPstatus = (EditText) findViewById(R.id.XMPPstatus);
        editBluetoothStatus = (EditText) findViewById(R.id.bluetoothStatus);
        editStatus = (EditText) findViewById(R.id.status);
        editReceivedFromRobot = (EditText) findViewById(R.id.receivedFromRobot);
        editSentToRobot = (EditText) findViewById(R.id.sentToRobot);
        editSendToRobot = (EditText) findViewById(R.id.sendToRobot);
        editReceivedFromServer = (EditText) findViewById(R.id.receivedFromServer);
        editSentToServer = (EditText) findViewById(R.id.sentToServer);
        editSendToServer = (EditText) findViewById(R.id.sendToServer);


        // Set a button listener to send a message to the server (chat)
        Button sendToServer = (Button) this.findViewById(R.id.btnSendToServer);
        sendToServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
        	    String text = editSendToServer.getText().toString();
        	    if (serviceBinder.sendDataToXMPPserver(text)) {
        	    	editSentToServer.setText(text);
        	    	editSendToServer.setText("");
        	    }
        	    else editSentToRobot.setText("Failed trying to send text to robot");
            	//Intent serviceIntent = new Intent();
        		//serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
        		//serviceIntent.putExtra("messageToServer", text);
        		//startService(serviceIntent);
        		//editSendToServer.setText("");
        		//editSentToServer.setText(text);
            }
        });


        // Set a button listener to send a chat text message
        Button sendToRobot = (Button) this.findViewById(R.id.btnSendToRobot);
        sendToRobot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
        	    String text = editSendToRobot.getText().toString();
        	    if (serviceBinder.sendDataToArduino(text)) {
        	    	editSentToRobot.setText(text);
        	    	editSendToRobot.setText("");
        	    }
        	    else editSentToRobot.setText("Failed trying to send text to robot");
        		//Intent serviceIntent = new Intent();
        		//serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
        		//serviceIntent.putExtra("RobotCommand", text);
        		//startService(serviceIntent);
        		//editSendToRobot.setText("");
        		//editSentToRobot.setText(text);
            }
        });

        // Set a button listener to connect to bluetooth and servers
        Button btnConnect = (Button) this.findViewById(R.id.Connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
        		Connect();
        		//Intent serviceIntent = new Intent();
        		//serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
        		//serviceIntent.putExtra("Connect", bluetooth);
        		//startService(serviceIntent);
        		//editStatus.setText("Connecting to bluetooth and servers");

            }
        });

        // Set a button listener to switch to settings activity
        Button btnSettings = (Button) this.findViewById(R.id.Settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	Intent startIntent = new Intent(_context, com.denbar.RobotComm.credentialsActivity.class);
        	    startActivity(startIntent);
            }
        });

    }	// ends on Create


    // We will get the strings needed for logging in from a preferences file
    // that contains the last set used.
    // If this is the first boot, then we will fill in the values
    // from the resource file strings.xml, which means that we can change those
    // values by simply swapping the strings.xml file, no code changes required.

    private void getPreferences()
    {
        Resources robotResources = getResources();
        SharedPreferences prefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );

        robotName = prefs.getString("robotname",robotResources.getString(R.string.robot_name));
        host = prefs.getString("host",robotResources.getString(R.string.host));
        port = prefs.getString("port",robotResources.getString(R.string.port));
        service = prefs.getString("service",robotResources.getString(R.string.service));
        userid = prefs.getString("userid",robotResources.getString(R.string.userid));
        password = prefs.getString("password",robotResources.getString(R.string.password));
        bluetooth = prefs.getString("bluetooth",robotResources.getString(R.string.bluetooth));
        recipient = prefs.getString("recipient",robotResources.getString(R.string.recipient));

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

        if (!EntriesTest())
        {

        }
    }

	@Override
	protected void onStart() {
		super.onStart();
		Toast.makeText(this, "RobotComm activty started", Toast.LENGTH_SHORT).show();
        getPreferences();
		Connect();
	}

	private void Connect()
	{
    	if (EntriesTest()) {
    		editSendToServer.setText("");
    		editSendToRobot.setText("");
        	editStatus.setText("Connecting to server.");
    		Intent serviceIntent = new Intent();
    		serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
    		startService(serviceIntent);
    		bindToService();
    		if (serviceBinder != null) {
    			serviceBinder.connectBluetooth(bluetooth);
    			serviceBinder.connectXMPP();
    		} else {
				editStatus.setText("Binding to server failed.");
				Log.d(this.getClass().getName(), "Binding failed");
    		}
    		updateGUI();
		} else {
        		editStatus.setText("Error in user entries, check setup parameters");
    	    	Intent startIntent = new Intent(this, com.denbar.RobotComm.credentialsActivity.class);
    		    startActivity(startIntent);
		}
	}

	private com.denbar.RobotComm.RobotCommService serviceBinder =  null;

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceBinder = ((com.denbar.RobotComm.RobotCommService.MyBinder)service).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			serviceBinder = null;
		}
	};

	private void bindToService()
	{
	    if (serviceBinder == null){
	    	Intent bindIntent = new Intent();
	    	bindIntent = new Intent(com.denbar.RobotComm.RobotCommActivity.this, com.denbar.RobotComm.RobotCommService.class);
			bindService(bindIntent, serviceConnection,Context.BIND_AUTO_CREATE);
		    Log.d(this.getClass().getName(), "Binding to service.");
	    }
    }

	private boolean EntriesTest()
	{
    	boolean returnResult = true;
    	String message = "There is a error in ";
    	try { int portNumber = Integer.parseInt(port); }
    	catch(NumberFormatException nfe)
    	{
    		message += "port, ";
    		returnResult = false;
    	}
    	if ( (!recipient.contains("@")) || (!recipient.contains(".")))
    	{
    		message += "recipient, ";
        	returnResult = false;
    	}
    	if ( !host.contains("."))
    	{
    		message += "host,";
        	returnResult = false;
    	}
    	if ( !service.contains("."))
    	{
    		message += "service, ";
        	returnResult = false;
    	}
    	if ( userid.contains("@"))
    	{
    		message += "userid,";
        		returnResult = false;
    	}
    	if (!BluetoothAdapter.checkBluetoothAddress(bluetooth))
    	{
    		message += "bluetooth ";
        	returnResult = false;
    	}
    	if (!returnResult) editStatus.setText(message);
    	return returnResult;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(this.getClass().getName(), "in onResume");
		MyGUItimer.cancel();
		MyGUItimer = new GUItimer();
		checkStateTimer.scheduleAtFixedRate(MyGUItimer, 0, timerUpdateRate);
		updateGUI();
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(this.getClass().getName(), "in onPause");
		MyGUItimer.cancel();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(this.getClass().getName(), "in onStop");
		MyGUItimer.cancel();
	    if (serviceConnection != null) {
	        unbindService(serviceConnection);
		    if (serviceBinder != null) serviceBinder._runningRobotCommActivity = false;
	        Log.d(this.getClass().getName(), "Unbinding from service from onStop");
	        serviceConnection = null;
        }
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    Log.d(this.getClass().getName(), "in onDestroy");
	    MyGUItimer.cancel();
	    if (serviceBinder != null) serviceBinder._runningRobotCommActivity = false;

	    //unbind the service and null it out
	    // this slows down the app when changing orientation, since it has to go through the whole process of unbinding,
	    // shutting down the server, and starting it back up, however, if we don't do this, it tends to throw a memory leak
	    // as the server has a binding to an activity that does not exist.
	    // That may just be a debugger issue, since garbage collection will take care of the binding in due time
	    // so perhaps it is better to allow the leak so that the performance is better.
	    // For now, we'll keep this.
	    if (serviceConnection != null) {
	        unbindService(serviceConnection);
	        Log.d(this.getClass().getName(), "Unbinding from service from onDestroy");
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
				if (serviceBinder == null) return;
				serviceBinder._runningRobotCommActivity = true;
		    	editBluetoothStatus.setText(serviceBinder._bluetoothStatus);
		    	editXMPPstatus.setText(serviceBinder._XMPPstatus);
		        editStatus.setText(serviceBinder._robotStatus);
		        editReceivedFromServer.setText(serviceBinder._messageReceivedFromServer);
		    	editSentToServer.setText(serviceBinder._messageSentToServer);
		        editReceivedFromRobot.setText(serviceBinder._messageReceivedFromRobot);
		        editSentToRobot.setText(serviceBinder._messageSentToRobot);
			}
		});
	}

	// This section is for when we want to show help and user preferences
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.optionsmenu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		Intent startIntent;
		switch (item.getItemId()) {

	    case R.id.help:
	    	startIntent = new Intent(this, com.denbar.RobotComm.Help.class);
		    startActivity(startIntent);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	*/

}