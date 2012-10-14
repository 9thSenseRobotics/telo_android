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


import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

//Service that supports the 9th Sense robots
public class RobotCommService extends Service {
	private static final String TAG = "RobotCommService";
	private Handler _Handler = new Handler();
	private XMPPConnection _connection;
	private String _robotCommand, _XMPPcommand;
	private String host, port, service;
	private String userid, password, bluetooth, recipient, recipientForEcho;
	private int portNumber;
	private Orientation orientation;
	private arduinoBT BT;
	private XMPP xmpp;
	PacketListener _packetListener;
	public String _bluetoothStatus = "Not connected yet", _XMPPstatus = "Not Connected Yet";
	public String _C2DMstatus = "Not connected yet";
	public String _messageReceivedFromRobot = "", _messageSentToRobot = "";
	public String _messageSentToServer = "", _messageReceivedFromServer = "", _batteryPercentage = "checking";
	public String _robotStatus, _startingTiltString, _currentTiltString, _rotationSinceLastTurnCommandSentString;
	public int _startingTilt, _currentTilt;
	public long _lastXMPPreceivedTime, _lastC2DMreceivedTime, _lastArduinoReceivedTime, _lastCommandSentToArduinoTime;
	public long _latencyXMPP = 0, _echoReceivedTimeXMPP, _echoSentTimeServer;
	public long _latencyC2DM = 0, _echoReceivedTimeC2DM;
	public long _latencyBT = 0, _echoReceviedTimeBT, _echoSentTimeBT;
	private boolean _sleeping = false;
	private long _timeBTconnectionLost, _timeXMPPconnectionLost, _timeC2DMconnectionLost, _lastTimeValue = 0;
	private long _lastArrivalTime = 0, _lastPacketSentTime;
	private boolean _echoReceivedBT = false, _echoReceivedXMPP = false,	_echoReceivedC2DM = false;
	private double TIME_OUT_ARDUINO = 60000, TIME_OUT_XMPP = 59000, TIME_OUT_C2DM = 30000000;
	private long MIN_TIME_BETWEEN_ARDUINO_COMMANDS = 100, MIN_TIME_BETWEEN_PACKET_OUTPUTS = 100;
	private static final long timerUpdateRate = 19000;
	private Timer _ckCommTimer;
	private checkCommTimer _commTimer;
	private boolean _commFlagBT = false, _commFlagC2DM = false,	_commFlagXMPP = false;
	private boolean _bluetoothProblem = false, _C2DMproblem = false, _XMPPproblem = false;
	private boolean _triedBTconnect = false, _triedXMPPconnect = false, _triedC2DMconnect = false;
	private Context _context;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "in onCreate");
		//Toast.makeText(this, "RobotCommService created", Toast.LENGTH_SHORT).show();
		_context = this;
		_robotStatus = "Starting service ";

		BT = new arduinoBT(this);
		xmpp = new XMPP(this);
		orientation = new Orientation(this);
		
		_packetListener = createPacketListener();

		getPreferences();

		if (!EntriesTest()) {
			//Toast.makeText(this, "Failed EntriesTest, opening credentialsActivity", Toast.LENGTH_LONG).show();
			Intent RobotCommIntent = new Intent(this, credentialsActivity.class);
			RobotCommIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(RobotCommIntent);
		}

		// setup timer to periodically check that the comm channels are working
		// note that this has to be setup after getPreferences or we will get
		// a null pointer exception.
		_lastArduinoReceivedTime = System.currentTimeMillis();
		Log.d(TAG, "_lastArduinoReceivedTime initialized");
		_lastXMPPreceivedTime = System.currentTimeMillis();
		Log.d(TAG, "_lastXMPPreceivedTime initialized");
		_lastC2DMreceivedTime = System.currentTimeMillis();
		Log.d(TAG, "_lastC2DMreceivedTime initialized");
		//_lastCommandSentToArduinoTime = System.currentTimeMillis();
		_lastPacketSentTime = System.currentTimeMillis();
		_commTimer = new checkCommTimer();
		_ckCommTimer = new Timer("ckComm");
		_ckCommTimer.scheduleAtFixedRate(_commTimer, 0, timerUpdateRate);

	}

	private void getPreferences() {
		Resources robotResources = getResources();
		SharedPreferences prefs = getSharedPreferences("RobotPreferences",	MODE_WORLD_WRITEABLE);
		host = prefs.getString("host", robotResources.getString(R.string.host));
		port = prefs.getString("port", robotResources.getString(R.string.port));
		service = prefs.getString("service", robotResources.getString(R.string.service));
		userid = prefs.getString("userid", robotResources.getString(R.string.userid));
		password = prefs.getString("password", robotResources.getString(R.string.password));
		bluetooth = prefs.getString("bluetooth",robotResources.getString(R.string.bluetooth)).toUpperCase();
		recipientForEcho = prefs.getString("recipientForEcho", robotResources.getString(R.string.sendCommandEchoServerAddress));
		recipient = prefs.getString("recipient", robotResources.getString(R.string.sendMessageToXMPPserverAddress));
		_startingTiltString = prefs.getString("startingTilt", robotResources.getString(R.string.startingTilt));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "in onStartCommand");
		if ((flags & START_FLAG_RETRY) == 0) {
			//Toast.makeText(this, "Recovering from abnormal shutdown.....", Toast.LENGTH_LONG).show();
		}
		//Toast.makeText(this, "RobotComm service started", Toast.LENGTH_SHORT).show();

		// here is where we interpret intents that are aimed at startService
		// note that commands from the user via RobotCommActivity to the arduino
		// call sendDataToArduino directly, through the binding rather than
		// here with an intent
		// the same is true for an XMPP incoming command to the arduino-- it
		// also
		// calls sendDataToArduino directly, rather than using an intent

		// With regard to messages that are going out to the server
		// from this service via XMPP.java,
		// currently there are two commands you can issue: a for alive) and
		// m (for message).
		// The command should go into MessageFromRobot.responseValue and get
		// sent to receiver@9thsense.com.
		// If you send an alive message, it responds to the query with a
		// MessageToRobot that contains ! as the commandChar (so, if you see a
		// ! in the commandChar, dont send it to the Arduino),
		// receiver@9thsense.com as the driver, and the word alive as the
		// commandArguments.

		if (intent != null  && (!_sleeping)) // intent is null if it is system restart
		{
			// from MonitorActivity
			String messageToConnect = intent.getStringExtra("Connect");
			String messageToChangeSleepState = intent.getStringExtra("SleepState");
			
			// from C2DM server
			String messageFromC2DM = intent.getStringExtra("C2DMmessage");

			// from the arduino, via arduinoBT, could be just an echo, could be
			// data
			String messageFromRobot = intent.getStringExtra("messageFromRobot");

			// from this service, sending out a bluetooth comm check to the
			// arduino
			String commCheckBT = intent.getStringExtra("commCheckBT");

			// from this service or from MonitorActivity, trying to recover a lost connection
			String reset = intent.getStringExtra("reset");

			// from either RobotCommActivity (user chat) or from this service,
			// sending out a server comm check
			String requestServer = intent.getStringExtra("SendToServer");

			// from StartupIntentReceiver, boot up is done and it is time to connect
			String bootup = intent.getStringExtra("Bootup");

			
			if (messageToConnect != null)
			{
				connectXMPP();
				connectBluetooth(messageToConnect);  // messageToConnect has the bluetooth address
				Log.d(TAG, "MonitorActivity sent Connect command");
			}

			else if (messageFromC2DM != null)
			{
				processMessageFromC2DMServer(messageFromC2DM);
				Log.d(TAG, "C2DM message received: " + messageFromC2DM);
			}
			// received a message from the arduino via the bluetooth, intent
			// sent from arduinoBT
			else if (messageFromRobot != null) {
				Log.d(TAG, "messageFromRobot: " + messageFromRobot);
				_lastArduinoReceivedTime = System.currentTimeMillis(); // record the time
				Log.d(TAG, "_lastArduinoReceivedTime updated");

				// don't send along the echos to the server, for those we just
				// want to capture the time
				// so that we know we still have a good BT connection
				if (messageFromRobot.startsWith("c")) // be sure it is a response to echo request
				{
					// pickup the battery percentage as something to do when comm checking
					_batteryPercentage = messageFromRobot.substring(1);
					RobotCommApplication.getInstance().setBatteryPercentage(_batteryPercentage);
					updateWidget();
					
					//try  {
					//	_batteryPercentage = Integer.parseInt(batteryPercentageString);	
					//}
					//catch (NumberFormatException nfe) {
					//	Log.d(TAG, "in comm check string from arduino, batteryPercentageString is not an integer, it is: " + batteryPercentageString );
					//}
					if (_commFlagBT)
					{
						_echoReceviedTimeBT = System.currentTimeMillis(); // just a local BT echo, record the echo time
						_echoReceivedBT = true;				
						Log.d(TAG, "_echoReceivedTimeBT updated");
					}
				} else {
					_messageReceivedFromRobot = messageFromRobot;
					// check to see if it is a message that
					if (messageFromRobot.startsWith("m"))
						messageToServer(messageFromRobot, "m"); // data from robot was sent
					else
						sendCommandEchoFromArduino(messageFromRobot, "e"); // just command echo
				}
			}

			// received from the bluetooth comm check
			// both XMPP and RobotCommActivity call sendDataToArduino directly
			// rather than with an intent
			else if (commCheckBT != null) sendDataToArduino(commCheckBT);			

			// received when a connection has gone bad
			else if (reset != null) {
				if (reset.equals("bluetooth"))
					tryResetBluetooth();
				else if (reset.equals("XMPP"))
					tryResetXMPP();
				else if (reset.equals("C2DM"))
					tryResetC2DM();
				else if (reset.equals("userCommanded"))
				{
					tryResetBluetooth();
					tryResetXMPP();
				}
			}

			// we got a request to send a message to the server that did not
			// come from BT
			// so it is either a user chat from RobotCommActivity or
			// a Server comm check requesting an echo
			else if (requestServer != null) {
				if (requestServer.equals("commCheckServer")) messageToServer(requestServer, "a"); // asking for an echo
				else messageToServer(requestServer, "m"); // just send data or a message
			}

			// this else will run on system start or restart,
			// so we check connections and, if they are good,
			// we let the XMPP server know we are up
			else if (bootup != null)
			{
				if (EntriesTest()) {
					Log.d(TAG, "Connecting to servers and BT at bootup or restart");
					if (BT == null)	connectBluetooth(bluetooth);
					else if (!BT.getConnectionState()) connectBluetooth(bluetooth);
					if (xmpp == null) connectXMPP();
					else if (!xmpp.getConnectionState()) connectXMPP(); 
					if (BT.getConnectionState() && xmpp.getConnectionState() && (bootup != null))
					{
						moveToStartTilt();
					}
				} else	Log.d(TAG, "Failed entriesTest in onStartCommand");
			}
			
		} else if (!_sleeping) // the intent was null
		{
			Log.d(TAG, "in onStartCommand with null intent");
		}
		return Service.START_STICKY; // service will restart after being terminated by the runtime
	}

	// moves the tablet to the correct position to start a session
	private void moveToStartTilt()
	{
		int tempTilt;
		try  {
			tempTilt = Integer.parseInt(orientation._yawString);
			
		}
		catch (NumberFormatException nfe) {
			tempTilt = 0;
			Log.d(TAG, "in startTilt, orientation._yawString is not an integer, it is: " + orientation._yawString );
			RobotCommApplication.getInstance().addNoteString("in startTilt, orientation._yawString is not an integer, it is: " + orientation._yawString );
		}
		int degreesToMove =  tempTilt - _startingTilt;
		Log.d(TAG, "in startTilt, degreesToMove = " + degreesToMove);
		RobotCommApplication.getInstance().addNoteString("in startTilt, degreesToMove = " + degreesToMove);
		String command = "u";
		if (degreesToMove < 0)
		{
			command = "n";
			degreesToMove = -degreesToMove;
		}
		command += degreesToMove;
		
		sendDataToArduino(command);
	}
	
	public void tryResetBluetooth() {
		Log.d(TAG, "in tryResetBluetooth");
		if (BT != null) {
			if (BT.resetConnection()) {
				_commFlagBT = false;
				_echoReceivedBT = false;
				_robotStatus = "Resetting BT";
				_bluetoothStatus = "Trying to reset";
				_lastArduinoReceivedTime = System.currentTimeMillis(); // give it some time to settle down before testing it
				Log.d(TAG, "_lastArduinoReceivedTime set in tryResetBluetooth");
				RobotCommApplication.getInstance().addNoteString("Trying a bluetooth reset");

			} else {
				_bluetoothProblem = true;
				_commFlagBT = false;
				_echoReceivedBT = false;
				_bluetoothStatus = "Hardware problem, check power";
				RobotCommApplication.getInstance().setBluetoothConnected(false);
				RobotCommApplication.getInstance().setBluetoothStatus(_bluetoothStatus);
				_robotStatus = "Not operating";
				RobotCommApplication.getInstance().addNoteString("Bluetooth reset failed");
				_timeBTconnectionLost = System.currentTimeMillis(); // we will set up an alarm to periodically retry
			}
		}
	}

	private void tryResetXMPP() {
		Log.d(TAG, "in tryResetXMPP");
		_XMPPstatus = "resetting XMPP";
		RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
		if (xmpp == null)
			xmpp = new XMPP(this);
		if (xmpp.getConnectionState())
			xmpp.resetConnection();
		if (connectXMPP()) {
			Log.d(TAG, "reconnected to XMPP server");
			_lastXMPPreceivedTime = System.currentTimeMillis(); // give it some time to settle down before testing it
			Log.d(TAG, "_lastXMPPreceivedTime set in tryResetXMPP");
		} else {
			_XMPPproblem = true;
			_XMPPstatus = "XMPP lost connection";
			RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
			_robotStatus = "Not operating";
			_timeXMPPconnectionLost = System.currentTimeMillis(); // we will set up an alarm to periodically retry
			Log.d(TAG, "resetting XMPP failed");
		}
		_commFlagXMPP = false;
		_echoReceivedXMPP = false;
	}

	private void tryResetC2DM() {
		Log.d(TAG, "in tryResetC2DM");
		//_C2DMstatus = "resetting C2DM";
		// if (c2dm == null) c2dm = new C2DM(this);
		// if (c2dm.getConnectionState()) c2dm.resetConnection();
		// if (connectC2DM())
		// {
		// Log.d(TAG, "reconnected to C2DM server");
		// _lastC2DMreceivedTime = System.currentTimeMillis(); // give it some
		// time to settle down before testing it
		// Log.d(TAG, "_lastC2DMreceivedTime set in tryResetXMPP");
		// }
		// else
		//{
			//_C2DMproblem = true;
			//_C2DMstatus = "lost connection";
			// _robotStatus = "Not operating";
			_timeC2DMconnectionLost = System.currentTimeMillis(); // we will set up an alarm to periodically retry
			//Log.d(TAG, "resetting C2DM failed");
		//}
		//_commFlagC2DM = false;
		//_echoReceivedC2DM = false;
	}

	public void connectBluetooth(String bluetoothAddress) {
		if (BT == null) {
			Log.d(TAG, "bluetooth connection requested when BT == null");
			BT = new arduinoBT(this);
		}
		_triedBTconnect = true; // tell the timers to back off if we have not
								// even tried connecting yet
		if (BT.getConnectionState() && (!_commFlagBT)) {
			Log.d(TAG, "bluetooth connection requested, already connected");
			_bluetoothStatus = "checking bluetooth connection";
			RobotCommApplication.getInstance().setBluetoothStatus(_bluetoothStatus);
			updateWidget();
			//return;
		}
		getPreferences();
		if (!EntriesTest()) {
			Log.d(TAG, "Failed EntriesTest in connectBT");
			// Toast.makeText(this, "bad bluetooth address",
			// Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(TAG, "in connectBluetooth");
		BT.setBTaddress(bluetoothAddress);
		if (BT.Connect()) { // this blocks until BT connects or times out
			// might want to run in Async
			_bluetoothStatus = "Connected";
			_robotStatus = "Bluetooth OK";
			RobotCommApplication.getInstance().setBluetoothConnected(true);
			RobotCommApplication.getInstance().setBluetoothStatus(_bluetoothStatus);
			_bluetoothProblem = false;
			updateWidget();
			Log.d(TAG, "Bluetooth connected");
			sendDataToArduino("c");	// send a comm check so that we get an initial battery percentage
			//Toast.makeText(this, "Bluetooth connected", Toast.LENGTH_SHORT).show();
		} else {
			//Toast.makeText(this, "Bluetooth connection failed, opening RobotCommActivity", Toast.LENGTH_LONG).show();
			Log.d(TAG, "bluetooth connection failed");
			_bluetoothStatus = "Connection failed";
			_robotStatus = "Not operating";
			RobotCommApplication.getInstance().setBluetoothConnected(false);
			RobotCommApplication.getInstance().setBluetoothStatus(_bluetoothStatus);
			updateWidget();
			Intent RobotCommIntent = new Intent(this, RobotCommActivity.class);
			RobotCommIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(RobotCommIntent);
		}
	}

	public boolean connectXMPP() {
		if (xmpp == null) {
			Log.d(TAG, "XMPP connection requested when xmpp == null");
			xmpp = new XMPP(this);
		}
		_triedXMPPconnect = true;
		_triedC2DMconnect = true;
		if (xmpp.getConnectionState()) {
			_XMPPstatus = "still connected";
			RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
			updateWidget();
			Log.d(TAG, "XMPP connection requested, already connected");
			return true;
		}
		getPreferences();
		if (!EntriesTest()) {
			Log.d(TAG, "Failed EntriesTest in connectXMPP");
			// Toast.makeText(this, "bad parameters",
			// Toast.LENGTH_SHORT).show();
			return false;
		}
		Log.d(TAG, "in connectXMPP");
		xmpp.setCommParameters(host, portNumber, service, userid, password,
				recipient, recipientForEcho);
		if (xmpp.Connect())
		{
			_connection = xmpp.getXMPPConnection();
			//Toast.makeText(this, "XMPP connected", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "XMPP connected");
			_XMPPstatus = "Connected";
			RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
			RobotCommApplication.getInstance().setXMPPconnected(true);
			_XMPPproblem = false;
			updateWidget();
			//setupPacketListener();
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			_connection.addPacketListener(_packetListener, filter);
			return true;
		} else {
			//Toast.makeText(this, "XMPP connection failed, opening RobotCommActivity", Toast.LENGTH_LONG).show();
			Log.d(TAG, "XMPP connection failed");
			_XMPPstatus = "Not connected";
			RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
			RobotCommApplication.getInstance().setXMPPconnected(false);
			updateWidget();
			_connection = null;
			Intent RobotCommIntent = new Intent(this, RobotCommActivity.class);
			RobotCommIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(RobotCommIntent);
			return false;
		}
	}
/*
	public void connectC2DM() {
		// attempt a registration
		Log.d(TAG, "in connectC2DM");
		_triedC2DMconnect = true;
		// set up the registration intent
		Intent intentC2dm = new Intent(
				"com.google.android.c2dm.intent.REGISTER");
		intentC2dm.putExtra("app", PendingIntent.getBroadcast(this, 0,
				new Intent(), 0));

		// this must be the registered gmail account, the comment in the
		// tutorial is wrong
		intentC2dm.putExtra("sender", "telebotphone@gmail.com");

		// fire the registration intent
		startService(intentC2dm);

	}
*/	
	
	// this sends a message to controller@9thsense.com
	// generally just echoing commands that it sent to the arduino
	// so that controller knows the connection is alive
	// and it can calculate the latency
	public boolean sendCommandEchoFromArduino(String data, String responseValue)
	{
		if (xmpp == null) {
			Log.d(TAG, "tried to send data: " + data + " to XMPP server when xmpp not created");
			return false;
		}
		Log.d(TAG, "in sendCommandEchoFromArduino: " + data);
		if (xmpp.sendCommandEchoFromArduino(data, responseValue))
		{
			_messageSentToServer = data;
			updateWidget();
			return true;
		}
		return false;
	}

	// this message goes to receiver@9thsense.com
	// if the response value = "a" then the server will send out an echo
	// if the response value = "m" or anything else, it will just note the message
	public boolean messageToServer(String data, String responseValue) {
		if (xmpp == null) {
			Log.d(TAG, "tried to send data: " + data + " to XMPP server when xmpp not created");
			return false;
		}
		Log.d(TAG, "in messageToServer: " + data + ", " + responseValue);
		if (xmpp.sendMessage(data, responseValue)) {
			_messageSentToServer = data;
			updateWidget();
			return true;
		}
		return false;
	}

	public boolean sendDataToArduino(String robotCommand) {
		if (BT == null) {
			Log.d(TAG, "tried to sendDataToArduino: " + robotCommand + " when BT not created");
			messageToServer("command received: error: bluetooth not created", "m"); // let the user know
			return false;
		}
		Log.d(TAG, "in sendDataToArduino");
		if (BT._isConnected) {
			//Toast.makeText(this, "Sending command to robot: " + robotCommand, Toast.LENGTH_SHORT).show();
			if (BT.sendMessage(robotCommand)) {
				if (_messageSentToRobot.length() > 80) _messageSentToRobot = _messageSentToRobot.substring(0,80);
				_messageSentToRobot = robotCommand + " " + _messageSentToRobot;
				Log.d(TAG, "Message successfully sent to robot: " + robotCommand + " at time = " + System.currentTimeMillis());
				
				// if we sent a rotation command, we want to reset the gyros, so we can tell how far we have turned.
				if (robotCommand.startsWith("h") || robotCommand.startsWith("L") || robotCommand.startsWith("l")
						|| robotCommand.startsWith("y") || robotCommand.startsWith("R") || robotCommand.startsWith("r"))
						orientation.resetIntegrals();
				updateWidget();
				return true;
			} else
				return false;
		}
		Log.d(TAG, "tried to sendDataToArduino: " + robotCommand + " when BT not connected");
		messageToServer("error: bluetooth not connected", "m"); // let the user know
		return false;
	}

	public boolean EntriesTest() {
		Log.d(TAG, "in EntriesTest");
		if (userid.contains("not set")) {
			Log.d(TAG, "uerid failed passed EntriesTest");
			return false;
		}
		if (password.contains("not set"))
			return false;
		try {
			portNumber = Integer.parseInt(port);
		} catch (NumberFormatException nfe) {
			return false;
		}
		if ((!recipient.contains("@")) || (!recipient.contains(".")))
			return false;
		if ((!recipientForEcho.contains("@"))
				|| (!recipientForEcho.contains(".")))
			return false;
		if (!host.contains("."))
			return false;
		if (!service.contains("."))
			return false;
		if (userid.contains("@"))
			return false;
		if (!BluetoothAdapter.checkBluetoothAddress(bluetooth))
			return false;
		Log.d(TAG, "passed EntriesTest");
		return true;
	}

	private final IBinder binder = new MyBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "in onBind");
		return binder;
	}

	public class MyBinder extends Binder {
		RobotCommService getService() {
			Log.d(TAG, "in MyBinder");
			return RobotCommService.this;
		}
	}

	public void goToSleep()
	{
		/*
		 Log.d(TAG, "going to sleep now, good night zzzz");
		 
		_sleeping = true;
		_commTimer.cancel();
		if (!_bluetoothProblem) _bluetoothStatus = "Sleeping";
		RobotCommApplication.getInstance().setBluetoothConnected(false);
		RobotCommApplication.getInstance().setBluetoothStatus(_bluetoothStatus);
		_robotStatus = "Sleeping";
		if (BT != null)
		{
			try {
				BT.closeBT();
			} catch (IOException ex) {
				Log.d(TAG, " trying to closeBT returned exception" + ex);
			}
			BT = null;
		}
		*/
		/*
		if (xmpp != null)
		{
			if (_connection != null) _connection.removePacketListener(_packetListener);
			xmpp.resetConnection();
			_connection = null;
			xmpp = null;
			_XMPPstatus = "Sleeping";
			RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
			RobotCommApplication.getInstance().setXMPPconnected(false);
		}

		if (C2DM != null)
		{
			C2DM.resetConnection();
			C2DM = null;
		}
		*/
		//Toast.makeText(this, "RobotCommService going to sleep", Toast.LENGTH_SHORT).show();
	}

	public void wakeUp()
	{
		/*
		 _robotStatus = "Waking up";
		_bluetoothStatus = "Waking up";
		_sleeping = false;
		if (BT == null)	connectBluetooth(bluetooth);
		else if (!BT.getConnectionState()) connectBluetooth(bluetooth);
		// setup timer to periodically check that the comm channels are working
		// note that this has to be setup after getPreferences or we will get
		// a null pointer exception.
		_lastArduinoReceivedTime = System.currentTimeMillis();
		Log.d(TAG, "_lastArduinoReceivedTime initialized");
		_lastXMPPreceivedTime = System.currentTimeMillis();
		Log.d(TAG, "_lastXMPPReceivedTime initialized");
		_lastC2DMreceivedTime = System.currentTimeMillis();
		Log.d(TAG, "_lastC2DMReceivedTime initialized");
		_commTimer = new checkCommTimer();
		_ckCommTimer.scheduleAtFixedRate(_commTimer, 0, timerUpdateRate);
		*/
		// send the intent, just to check connections and get stuff going again
		//Intent serviceIntent = new Intent();
		//serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
		//serviceIntent.putExtra("wakeup", "connect");
		//_context.startService(serviceIntent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "in onDestroy");
		_commTimer.cancel();
		if (BT != null)
		{
			try {
				BT.closeBT();
			} catch (IOException ex) {
				Log.d(TAG, " trying to closeBT returned exception" + ex);
			}
			BT = null;
		}
		if (xmpp != null)
		{
			if (_connection != null) _connection.removePacketListener(_packetListener);
			xmpp.resetConnection();
			_connection = null;
			xmpp = null;
		}
		/*
		if (C2DM != null)
		{
			C2DM.resetConnection();
			C2DM = null;
		}
		*/
		//Toast.makeText(this, "RobotCommService Destroyed", Toast.LENGTH_SHORT).show();
	}

	// once we are connected, we want to listen for packets
	// so set up a packet listener
	// don't know why this does not work when it is in the XMPP class
	// but having it here does avoid bothering with processing the intent
	PacketListener createPacketListener()
	{
		PacketListener localPacketListener = new PacketListener() {
			public void processPacket(Packet packet) {

				Message message = (Message) packet;
				Log.d(TAG, "in processPacket, message: " + message);
				String testString = message.toString();
				Log.d(TAG, "in processPacket, testString: " + testString);
				String testXML = message.toXML();
				Log.d(TAG, "in processPacket, testXML: " + testXML);
				_lastXMPPreceivedTime = System.currentTimeMillis();
				if (message.getBody() != null) {
					//_messageReceivedFromServer = message.getBody();
					if (System.currentTimeMillis() - _lastPacketSentTime < MIN_TIME_BETWEEN_PACKET_OUTPUTS)
					{
						Log.d(TAG, "Waiting for min time until we can change the value of the global variable _XMPPcommand");
						while (System.currentTimeMillis() - _lastPacketSentTime  < MIN_TIME_BETWEEN_PACKET_OUTPUTS);
						Log.d(TAG, "Finished waiting for min time until we can change XMPP_command");
					}
					_lastPacketSentTime = System.currentTimeMillis();
					_XMPPcommand = message.getBody();
					_Handler.post(new Runnable() {
						public void run() {
							Log.d(TAG, "in processPacket, _XMPPcommand: " + _XMPPcommand + " successfully sent at time = " + System.currentTimeMillis() );
							processMessageFromXMPPServer(_XMPPcommand);
						}
					});



					/*if (body.contains("<")) {
						MessageToRobot myMessage = new MessageToRobot(message
								.getBody());
						_robotCommand = myMessage.commandChar
								+ myMessage.commandArguments;
					} else
						_robotCommand = body;
					Log.d(TAG, "message received in processPacket: "
							+ _robotCommand);
					_messageReceivedFromServer = _robotCommand;

					_lastXMPPreceivedTime = System.currentTimeMillis();
					Log
							.d(TAG,
									"_lastArduinoReceivedTime updated in processPacket");

					if (_robotCommand.startsWith("!")) // see if it is just an XMPP server echo
					{
						_echoReceivedXMPP = true;
						_echoReceivedTimeXMPP = System.currentTimeMillis(); // just an echo
						Log
								.d(TAG,	"_echoReceivedTimeXMPP updated in processPacket");
					} else // don't send along the message if was just an echo
					{
						_Handler.post(new Runnable() {
							public void run() {
								sendDataToArduino(_robotCommand);
								processMessageFromServer(_XMPPcommand, "XMPP");

							}
						});
					}
				*/
				}
				else Log.d(TAG, "in, message.getBody() was null");
			}
		};
	return localPacketListener;
	}
/*
	// once we are connected, we want to listen for packets
	// so set up a packet listener
	// don't know why this does not work when it is in the XMPP class
	// but having it here does avoid bothering with processing the intent
	public void setupPacketListener1() {
		Log.d(TAG, "in setupPacketListener");
		if (_connection == null) {
			Log.d(TAG, "tried to setup packetListener with no connection present");
			return;
		}
		// Add a packet listener to get messages sent to us
		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		_connection.addPacketListener(new PacketListener() {
			public void processPacket(Packet packet) {
				Log.d(TAG, "in processPacket");
				Message message = (Message) packet;
				if (message.getBody() != null) {
					String body = message.getBody();
					if (body.contains("<")) {
						MessageToRobot myMessage = new MessageToRobot(message.getBody());
						_robotCommand = myMessage.commandChar + myMessage.commandArguments;
					} else
						_robotCommand = body;
					Log.d(TAG, "message received in processPacket: " + _robotCommand);
					_messageReceivedFromServer = _robotCommand;

					_lastXMPPreceivedTime = System.currentTimeMillis();
					Log.d(TAG,	"_lastXMPPReceivedTime updated in processPacket");

					if (_robotCommand.startsWith("!")) // see if it is just an XMPP server echo
					{
						_echoReceivedXMPP = true;
						_echoReceivedTimeXMPP = System.currentTimeMillis(); // just an echo, don't send it along
						Log.d(TAG, "_echoReceivedTimeXMPP updated in processPacket");
					}
					else if (_robotCommand.startsWith("P"))	// server is asking for status check, sleeping or not
					{
						String status = "0";
						if ( (!_sleeping) && (!_XMPPproblem) && (!_bluetoothProblem)) status = "1";
						sendCommandEchoFromArduino(status, "P");
						// this is not from Arduino, but it goes to controller, so we use this call
					}
					else if (_robotCommand.startsWith("p"))	// server is asking to toggle sleep state
					{
						String status = "1";
						if (_sleeping  && (!_XMPPproblem) && (!_bluetoothProblem))
						{
							sendCommandEchoFromArduino(status, "P");
							wakeUp();
						}
						else
						{
							status = "0";
							sendCommandEchoFromArduino(status, "P");
							sleep();
						}
					}
					else if (!_sleeping)
					{
						_Handler.post(new Runnable() {
							public void run() {
								sendDataToArduino(_robotCommand);
							}
						});
					}

				}
			}
		}, filter);
	}
*/
	private void updateWidget() {
		Log.d(TAG, "in updateWidget");
		// Build the intent to call the service
		Intent intent = new Intent(this, UpdateWidgetService.class);
		intent.putExtra("bluetooth", "    BT: " + _bluetoothStatus);
		intent.putExtra("XMPP", "    XMPP: " + _XMPPstatus);
		intent.putExtra("C2DM", "    C2DM: " + _C2DMstatus);
		intent.putExtra("sentToServer", "    Sent To Server: " + _messageSentToServer);
		intent.putExtra("sentToRobot", "    Sent To Robot: " + _messageSentToRobot);
        double rotation = 12. * orientation._GyroYintegral;
        int rotationInt = (int) rotation;
        //textviewRotation.setText(rotationString);
		_rotationSinceLastTurnCommandSentString = "Tablet Rotation " + rotationInt;
		RobotCommApplication.getInstance().addNoteString("Tablet rotation since last turn command sent: " + rotationInt);
		RobotCommApplication.getInstance().addNoteString("Tablet tilt: " + orientation._yawString);
		this.startService(intent);
	}

	final class checkCommTimer extends TimerTask {
		public void run() {
			Log.d(TAG, "in checkCommTimer");
			updateWidget();
			if (EntriesTest()) {
				if ((!_bluetoothProblem) && _triedBTconnect) // skip if BT is out or never tried
				{
					if (System.currentTimeMillis() - _lastArduinoReceivedTime > TIME_OUT_ARDUINO) {
						// if we sent a character for echo and heard nothing, we
						// need to reset BT
						if (_commFlagBT) {Log.d(TAG,"Arduino comm timed out after an echo request, so we will reset bluetooth");
							Intent serviceIntent = new Intent();
							serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
							serviceIntent.putExtra("reset", "bluetooth");
							_context.startService(serviceIntent);
						} else {
							// we have not heard anything from the arduino for a
							// while
							// so we will send a character to be echoed
							_commFlagBT = true;
							_echoReceivedBT = false;
							_bluetoothStatus = "Checking bluetooth connection";
							RobotCommApplication.getInstance().setBluetoothStatus(_bluetoothStatus);
							Log	.d(TAG,	"Arduino comm timed out, so we will send an echo request to see if BT is still connected");
							Intent serviceIntent = new Intent();
							serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
							serviceIntent.putExtra("commCheckBT", "c");
							_echoSentTimeBT = System.currentTimeMillis();
							Log.d(TAG, "_echoSentTimeBT set");
							_lastArduinoReceivedTime = System.currentTimeMillis(); // makes us wait for another timeout
							Log.d(TAG,"_lastArduinoReceivedTime reset for echo");
							_context.startService(serviceIntent);
						}
					} else if (_echoReceivedBT) {
						Log.d(TAG, "arduino echo request returned good");
						// our BT comm check came back good
						_commFlagBT = false;
						_echoReceivedBT = false;
						_robotStatus = "bluetooth OK";
						_latencyBT = _echoReceviedTimeBT - _echoSentTimeBT;
						if (_latencyBT < TIME_OUT_ARDUINO)_bluetoothStatus = "latency = " + String.valueOf(_latencyBT);
						RobotCommApplication.getInstance().setBluetoothStatus(_bluetoothStatus);
						Log.d(TAG, "_latencyBT calculated = " + _latencyBT);
					}
				}

				boolean sendServerEcho = false;
				// now check on XMPP
				if ((!_XMPPproblem) && _triedXMPPconnect) // skip if XMPP is out or never tried
				{

					if (System.currentTimeMillis() - _lastXMPPreceivedTime > TIME_OUT_XMPP) {
						// if we sent a character for echo and heard nothing, we
						// need to reset XMPP
						// unless we already tried and failed to reset, in which
						// case we give up
						if (_commFlagXMPP) {
							Log.d(TAG, "XMPP comm timed out after an echo request, so we will try to reset XMPP");
							Intent serviceIntent = new Intent();
							serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
							serviceIntent.putExtra("reset", "XMPP");
							_context.startService(serviceIntent);
						} else {
							Log	.d(TAG,	"XMPP comm timed out, so we will request an echo to see if XMPP is still connected");
							sendServerEcho = true;
							_XMPPstatus = "Checking XMPP connection";
							RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
						}
					} else if (_echoReceivedXMPP) {
						Log.d(TAG, "XMPP echo request returned good");
						// our XMPP comm check came back good
						_commFlagXMPP = false;
						_echoReceivedXMPP = false;
						_robotStatus = "XMPP OK";
						_latencyXMPP = _echoReceivedTimeXMPP - _echoSentTimeServer;
						if (_latencyXMPP < TIME_OUT_XMPP) _XMPPstatus = "latency = " + String.valueOf(_latencyXMPP);
						RobotCommApplication.getInstance().setXMPPstatus(_XMPPstatus);
						Log.d(TAG, "_latencyXMPP calculated = " + _latencyXMPP);
					}
				}
				/*
				if ((!_C2DMproblem) && _triedC2DMconnect) // skip if C2DM is out or never tried
				{
					if (System.currentTimeMillis() - _lastC2DMreceivedTime > TIME_OUT_C2DM) {
						if (_commFlagC2DM) {
							Log.d(TAG, "C2DM comm timed out after an echo request, so we will try to reset C2DM");
							Intent serviceIntent = new Intent();
							serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
							serviceIntent.putExtra("reset", "C2DM");
							_context.startService(serviceIntent);
						} else {
							Log.d(TAG, "C2DM comm timed out, so we will request and echo to see if C2DM is still connected");
							// wake up the app, just to be sure it is running
							//Intent C2DMIntent = new Intent();
							//C2DMIntent.setAction("com.denbar.C2DM_Receiver.TeloStartupIntentReceiver.WAKEUP");
							//C2DMIntent.putExtra("wakeup", "RobotCommService");
							//_context.sendBroadcast(C2DMIntent);
							sendServerEcho = true;
							_C2DMstatus = "Checking C2DM connection";
						}
					} else if (_echoReceivedC2DM) {
						Log.d(TAG, "C2DM echo came back good");
						// our C2DM comm check came back good
						_commFlagC2DM = false;
						_echoReceivedC2DM = false;
						_robotStatus = "C2DM OK";
						_latencyC2DM = _echoReceivedTimeC2DM - _echoSentTimeServer;
						if (_latencyC2DM < TIME_OUT_C2DM) _C2DMstatus = "latency = " + String.valueOf(_latencyC2DM);
						Log.d(TAG, "_latencyC2DM calculated = " + _latencyC2DM);
					}
				}
				*/
				if (sendServerEcho) {
					_robotStatus = "Checking XMPP and C2DM connections";
					_commFlagXMPP = true;
					_echoReceivedXMPP = false;
					_commFlagC2DM = true;
					_echoReceivedC2DM = false;
					_echoSentTimeServer = System.currentTimeMillis();
					Log.d(TAG, "_echoSentTimeServer set");
					_echoReceivedTimeXMPP = System.currentTimeMillis();
					Log.d(TAG, "_echoReceivedTimeXMPP set");
					_echoReceivedTimeC2DM = System.currentTimeMillis();
					Log.d(TAG, "_echoReceivedTimeC2DM set");
					_lastXMPPreceivedTime = System.currentTimeMillis();
					Log.d(TAG, "_lastXMPPreceivedTime reset for echo");
					_lastC2DMreceivedTime = System.currentTimeMillis();
					Log.d(TAG, "_lastC2DMreceivedTime reset for echo");
					Intent serviceIntent = new Intent();
					serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
					serviceIntent.putExtra("SendToServer", "commCheckServer");
					_context.startService(serviceIntent);
				}
				
			} else	// corresponds to if(entriesTest())
				Log.d(TAG, "failed EntriesTest in timer task");
		}
	}

	void processMessageFromXMPPServer(String messageFromServer)
	{
		Log.d(TAG, "in processMessageFromXMPPServer " + messageFromServer);
		String robotCommand = null, timeStamp = null;
		MessageToRobot serverMessage;
		if (messageFromServer.contains("<"))
		{
			serverMessage = new MessageToRobot(messageFromServer);
			if (serverMessage.commandChar != null)
			{
				robotCommand = serverMessage.commandChar;
				if (serverMessage.commandArguments != null) robotCommand += serverMessage.commandArguments;
			}
			if ( serverMessage.timeStamp != null) timeStamp = serverMessage.timeStamp;	// note that timeStamp might be null here, careful
			if (timeStamp != null) Log.d(TAG, "in processMessageFromXMPPServer, timeStamp: " + timeStamp);
		}
		else
		{
			Log.d(TAG, "in processMessageFromXMPPServer, message is not in valid format: " + messageFromServer);
			return;
		}
		if (robotCommand == null)
		{
			Log.d(TAG, "in processMessageFromXMPPServer, robotCommand is null ");
			return;
		}
		if (timeStamp == null)
		{
			Log.d(TAG, "in processMessageFromXMPPServer, timeStamp is null ");
			return;
		}

		if (robotCommand.startsWith("!")) // see if it is just a server echo
		{
					_echoReceivedXMPP = true; // just an echo, don't send it along
					_echoReceivedTimeXMPP = System.currentTimeMillis();
					if (_messageReceivedFromServer.length() > 80) _messageReceivedFromServer = _messageReceivedFromServer.substring(0, 80);
					_messageReceivedFromServer = "connected " + _messageReceivedFromServer;
					Log.d(TAG, "in processMessageFromXMPPServer, _echoReceivedTimeXMPP updated ");
					return;
		}
		
		if (robotCommand.startsWith("s"))
		{
			_lastTimeValue = 0;
			Log.d(TAG, "in processMessageFromXMPPServer, resetting timeValue");
			return;
		}
		
		// check timestamp to see if we have already processed this message
		Log.d(TAG, "in processMessageFromXMPPServer, testing for previous timeStamp: " + timeStamp);
		
		long timeValue;
		try  {
			timeValue = Long.valueOf(timeStamp);
		}
		catch (NumberFormatException nfe) {
			timeValue = 0;
			Log.d(TAG, "in processMessageFromXMPPServer, timeStamp is not a long number, it is: " + timeStamp);
			return;
		}
		Log.d(TAG, "in processMessageFromXMPPServer  timeValue = " + timeValue);
		if (timeValue == _lastTimeValue)
		{
			long delayTime = System.currentTimeMillis() - _lastArrivalTime;
			Log.d(TAG, "in processMessageFromXMPPServer, message arrived " + delayTime + " msec later than C2DM one");
			return;
		}
		if (timeValue < _lastTimeValue)
		{
			Log.d(TAG, "in processMessageFromXMPPServer, message is later than multiple messages");
			return;
		}
		_lastTimeValue = timeValue;
		Log.d(TAG, "in processMessageFromXMPPServer, processing new message: " + robotCommand + " timeValue = " + timeValue);

		// now we know we have the first arrival of a new command from the server
		// set the global variable
		
		MessageFromRobot mfr = new MessageFromRobot(serverMessage.driverAddr, serverMessage.robotAddr, serverMessage.commandChar, "echo", serverMessage.timeStamp);
		
		if (xmpp != null) xmpp.SendRobotMessageToServer(mfr);
		
		_robotCommand = robotCommand;
		if (_messageReceivedFromServer.length() > 80) _messageReceivedFromServer = _messageReceivedFromServer.substring(0, 80);
		_messageReceivedFromServer = robotCommand + " " + _messageReceivedFromServer;
		_lastArrivalTime = System.currentTimeMillis();
		

		if (_robotCommand.startsWith("P"))	// server is asking for status check, sleeping or not
		{
			Log.d(TAG, "in processMessageFromXMPPServer,server is asking for status check");
			String status = "0";
			if ( (!_sleeping) && (! (_XMPPproblem && _C2DMproblem) ) && (!_bluetoothProblem)) status = "1";
			sendCommandEchoFromArduino(status, "P");
			return;

			// this is not from Arduino, but it goes to controller, so we use this call
		}
		else if (_robotCommand.startsWith("p"))	// server is asking to change
		{
			Log.d(TAG, "in processMessageFromXMPPServer, server sent sleep/wake command");
			if(_robotCommand.contains("0")) // go to sleep
			{
				Log.d(TAG, "in processMessageFromXMPPServer, server is asking us to sleep");
				sendCommandEchoFromArduino("0", "P");
				if (!_sleeping) goToSleep();
			}
			else	// wake up
			{
				Log.d(TAG, "in processMessageFromXMPPServer, server is asking us to wake up");
				sendCommandEchoFromArduino("1", "P");
				if (_sleeping) //  && (! (_XMPPproblem && _C2DMproblem) ) && (!_bluetoothProblem))
					wakeUp();
			}
			return;
		}
		else if (!_sleeping)	// finally we are able to just send the command to the robot
		{
			_Handler.post(new Runnable() {
				public void run() {
					Log.d(TAG, "in processMessageFromXMPPServer, successfully sending command to arduino, time = " + System.currentTimeMillis());
					sendDataToArduino(_robotCommand);
				}
			});
		}
	}


	void processMessageFromC2DMServer(String messageFromServer)
	{
		Log.d(TAG, "in processMessageFromC2DMServer " + messageFromServer);
		//_C2DMstatus = "Connected";
		_messageReceivedFromServer = messageFromServer;
		String robotCommand = null, timeStamp = null;
		if (messageFromServer.contains("<"))
		{
			MessageToRobot serverMessage = new MessageToRobot(messageFromServer);
			robotCommand = serverMessage.commandChar + serverMessage.commandArguments;
			timeStamp = serverMessage.timeStamp;	// note that timeStamp might be null here, careful
			if (timeStamp != null) Log.d(TAG, "in processMessageFromC2DMServer, timeStamp: " + timeStamp);
		}
		else
		{
			Log.d(TAG, "in processMessageFromC2DMServer, message is not in valid format: " + messageFromServer);
			return;
		}
		if (robotCommand == null)
		{
			Log.d(TAG, "in processMessageFromC2DMServer, robotCommand is null ");
			return;
		}
		if (timeStamp == null)
		{
			Log.d(TAG, "in processMessageFromC2DMServer, timeStamp is null ");
			return;
		}

		Log.d(TAG, "in processMessageFromC2DMServer, robotCommand = " + robotCommand);
		if (robotCommand.startsWith("!")) // see if it is just a server echo
		{
			_echoReceivedC2DM = true; // just an echo, don't send it along
			_echoReceivedTimeC2DM = System.currentTimeMillis(); // just an echo, don't send it along
			Log.d(TAG, "in processMessageFromC2DMServer, _echoReceivedTimeC2DM updated");
			return;
		}
		// check timestamp to see if we have already processed this message
		Log.d(TAG, "checking to see if we already processed this message");
		Log.d(TAG, "in processMessageFromC2DMServer, testing for previous timeStamp: " + timeStamp);

		long timeValue;
		try  {
			timeValue = Long.valueOf(timeStamp);
		}
		catch (NumberFormatException nfe) {
			timeValue = 0;
			Log.d(TAG, "in processMessageFromC2DMServer, timeStamp is not a long number, it is: " + timeStamp);
			return;
		}
		Log.d(TAG, "in processMessageFromC2DMServer  timeValue = " + timeValue);
		if (timeValue == _lastTimeValue)
		{
			long delayTime = System.currentTimeMillis() - _lastArrivalTime;
			Log.d(TAG, "in processMessageFromXMPPServer, message arrived " + delayTime + " msec later than XMPP one");

			return;
		}
		if (timeValue < _lastTimeValue)
		{
			Log.d(TAG, "in processMessageFromC2DMServer, message is later than multiple messages");
			return;
		}
		_lastTimeValue = timeValue;
		Log.d(TAG, "in processMessageFromC2DMServer, processing new message: " + robotCommand + " timeValue = " + timeValue);

		// now we know we have the first arrival of a new command from the server
		// set the global variable
		_robotCommand = robotCommand;
		_messageReceivedFromServer = _robotCommand;
		_lastArrivalTime = System.currentTimeMillis();

		if (_robotCommand.startsWith("s"))
		{
			_lastTimeValue = 0;
			Log.d(TAG, "in processMessageFromC2DMServer, resetting _lastTimeValue");
			_lastTimeValue = 0;
			return;
		}
		
		if (_robotCommand.startsWith("P"))	// server is asking for status check, sleeping or not
		{
			Log.d(TAG, "in processMessageFromC2DMServer,server is asking for status check");
			String status = "0";
			if ( (!_sleeping) && (! (_XMPPproblem && _C2DMproblem) ) && (!_bluetoothProblem)) status = "1";
			sendCommandEchoFromArduino(status, "P");
			return;

			// this is not from Arduino, but it goes to controller, so we use this call
		}
		else if (_robotCommand.startsWith("p"))	// server is asking to toggle sleep state
		{
			Log.d(TAG, "in processMessageFromC2DMServer, server is asking to toggle sleep state");
			String status = "1";
			if (_sleeping  && (! (_XMPPproblem && _C2DMproblem) ) && (!_bluetoothProblem))
			{
				sendCommandEchoFromArduino(status, "P");
				wakeUp();
			}
			else
			{
				status = "0";
				sendCommandEchoFromArduino(status, "P");
				goToSleep();
			}
			return;
		}
		else if (!_sleeping)	// finally we are able to just send the command to the robot
		{
			_Handler.post(new Runnable() {
				public void run() {
					Log.d(TAG, "in processMessageFromC2DMServer, sending command to arduino");
					//sendDataToArduino(_robotCommand);
					// *****************************************not using C2DM ***********************************************
				}
			});
		}
	}
}
