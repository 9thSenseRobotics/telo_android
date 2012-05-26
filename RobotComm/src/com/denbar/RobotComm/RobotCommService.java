package com.denbar.RobotComm;

import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.PendingIntent;
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
import android.widget.Toast;


//Service that supports the 9th Sense robots
public class RobotCommService extends Service {
	private static final String LOG = "RobotCommService";
	private Handler _Handler = new Handler();
	private XMPPConnection _connection;
	private String _robotCommand, _robotArguments;
	private String host, port, service;
	private String robotName, userid, password, bluetooth, recipient, recipientForEcho;
	private int portNumber;
	private arduinoBT BT;
	private XMPP xmpp;
	public String _bluetoothStatus = "Not connected yet", _XMPPstatus = "Not Connected Yet";
	public String _C2DMstatus = "Not connected yet";
	public String _messageReceivedFromRobot = "", _messageSentToRobot = "";
	public String _messageSentToServer = "", _messageReceivedFromServer = "";
	public String _robotStatus;
	public long _lastXMPPreceivedTime, _lastC2DMreceivedTime, _lastArduinoReceivedTime;
	public long _latencyXMPP = 0, _echoReceivedTimeXMPP,_echoSentTimeServer;
	public long _latencyC2DM = 0, _echoReceivedTimeC2DM;
	public long _latencyBT= 0, _echoReceviedTimeBT, _echoSentTimeBT;
	private boolean _echoReceivedBT = false, _echoReceivedXMPP = false, _echoReceivedC2DM = false;
	private double TIME_OUT_ARDUINO = 30000, TIME_OUT_XMPP = 30000, TIME_OUT_C2DM = 30000;
	private static final long timerUpdateRate = 9000;
	private Timer _ckCommTimer;
	private checkCommTimer _commTimer;
	private boolean _commFlagBT = false, _commFlagC2DM = false, _commFlagXMPP = false;
	private boolean _bluetoothProblem = false, _C2DMproblem = false, _XMPPproblem = false;
	private Context _context;


	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG, "in onCreate");
		Toast.makeText(this, "RobotCommService created", Toast.LENGTH_SHORT).show();
		_context = this;
		_robotStatus = "Nominal ";

		// setup timer to periodically check that the comm channels are working
		_lastArduinoReceivedTime = System.currentTimeMillis();
		Log.d(LOG, "_lastArduinoReceivedTime initialized");
		_lastXMPPreceivedTime = System.currentTimeMillis();
		Log.d(LOG, "_lastArduinoReceivedTime initialized");
		_lastC2DMreceivedTime = System.currentTimeMillis();
		Log.d(LOG, "_lastArduinoReceivedTime initialized");
		_commTimer = new checkCommTimer();
		_ckCommTimer = new Timer("ckComm");
		_ckCommTimer.scheduleAtFixedRate(_commTimer, 0, timerUpdateRate);

		BT = new arduinoBT(this);
		xmpp = new XMPP(this);

		getPreferences();

		if (!EntriesTest()) {
			Toast.makeText(this, "Entries test in server failed, opening credentialsActivity", Toast.LENGTH_LONG).show();
			Intent RobotCommIntent = new Intent(this, credentialsActivity.class);
			RobotCommIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(RobotCommIntent);
		} else {
			connectBluetooth(bluetooth);
			connectXMPP();
		}

	}

	private void getPreferences()
	{
		Resources robotResources = getResources();
		SharedPreferences prefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );
		robotName = prefs.getString("robotName",robotResources.getString(R.string.robot_name));
		host = prefs.getString("host",robotResources.getString(R.string.host));
		port = prefs.getString("port",robotResources.getString(R.string.port));
		service = prefs.getString("service",robotResources.getString(R.string.service));
		userid = prefs.getString("userid",robotResources.getString(R.string.userid));
		password = prefs.getString("password",robotResources.getString(R.string.password));
		bluetooth = prefs.getString("bluetooth",robotResources.getString(R.string.bluetooth)).toUpperCase();
		recipientForEcho = prefs.getString("recipient",robotResources.getString(R.string.sendCommandEchoServerAddress));
		recipient = prefs.getString("recipientForEcho",robotResources.getString(R.string.sendMessageToXMPPserverAddress));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.d(LOG, "in onStartCommand");
		if ((flags & START_FLAG_RETRY) == 0)
		{
			Toast.makeText(this, "Recovering from abnormal shutdown.....", Toast.LENGTH_LONG).show();
		}

		Toast.makeText(this, "RobotComm service started", Toast.LENGTH_SHORT).show();

		// here is where we interpret intents that are aimed at startService
		// note that commands from the user via RobotCommActivity to the arduino
		// call sendDataToArduino directly, through the binding rather than
		// here with an intent
		// the same is true for an XMPP incoming command to the arduino-- it also
		// calls sendDataToArduino directly, rather than using an intent

		// With regard to messages that are going out to the server
		// from this service via XMPP.java,
		// currently there are two commands you can issue: ‘a’ (for “alive”) and ‘m’ (for “message”).
		// The command should go into MessageFromRobot.responseValue and get sent to receiver@9thsense.com.
		// If you send an “a” message, it responds to the query with a MessageToRobot that contains ”!” as the commandChar (so, if you see a ! in the commandChar, don’t send it to the Arduino),
		// “receiver@9thsense.com” as the driver, and the word “alive” as the commandArguments.

		if (intent != null)	// intent is null if it is system restart
		{
			// from the arduino, via arduinoBT, could be just an echo, could be data
			String messageFromRobot = intent.getStringExtra("messageFromRobot");

			// from this service, sending out a bluetooth comm check to the arduino
			String commCheckBT = intent.getStringExtra("commCheckBT");

			// from this service, trying to recover a lost connection
			String reset = intent.getStringExtra("reset");

			// from either RobotCommActivity (user chat) or from this service,
			// sending out a server comm check
			String requestServer = intent.getStringExtra("SendToServer");

			// received a message from the arduino via the bluetooth, intent sent from arduinoBT
			if (messageFromRobot != null) {
				Log.d(LOG, "messageFromRobot: " + messageFromRobot);
				_lastArduinoReceivedTime = System.currentTimeMillis(); // record the time
				Log.d(LOG, "_lastArduinoReceivedTime updated");

				// don't send along the echos to the server, for those we just want to capture the time
				// so that we know we still have a good BT connection
				if (messageFromRobot.startsWith("c") && _commFlagBT) // be sure it is a response to echo request
				{
					_echoReceviedTimeBT = System.currentTimeMillis();	// just a local BT echo, record the echo time
					_echoReceivedBT = true;
					Log.d(LOG, "_echoReceivedTimeBT updated");
				}
				else
				{
					_messageReceivedFromRobot = messageFromRobot;
					// check to see if it is a message that
					if (messageFromRobot.startsWith("m")) messageToServer(messageFromRobot, "m");	// data from robot was sent
					else sendCommandEchoFromArduino(messageFromRobot); // just command echo
				}
			}

			// received from the bluetooth comm check
			// both XMPP and RobotCommActivity call sendDataToArduino directly rather than with an intent
			if (commCheckBT != null) sendDataToArduino(commCheckBT);

			// received when a connection has gone bad
			if (reset != null)
			{
				if (reset.equals("bluetooth")) tryResetBluetooth();
				else if (reset.equals("XMPP")) tryResetXMPP();
				else if (reset.equals("C2DM")) tryResetC2DM();
			}

			// we got a request to send a message to the server that did not come from BT
			// so it is either a user chat from RobotCommActivity or
			// a Server comm check requesting an echo
			if (requestServer != null)
			{
				if (requestServer.equals("commCheckServer")) messageToServer(requestServer,"a");	// asking for an echo
				else  messageToServer(requestServer, "m");	// just send data or a message
			}
		}
		else	// the intent was null
		{
			// this else will run on system start or restart,
			// so we check connections and, if they are good,
			// we let the XMPP server know we are up
			if (BT == null) connectBluetooth(bluetooth);
			if (!BT.getConnectionState()) connectBluetooth(bluetooth);
			if (xmpp == null) connectXMPP();
			if (!xmpp.getConnectionState()) connectXMPP();
			if (BT.getConnectionState() && xmpp.getConnectionState()) messageToServer("<m><re>1.0</re></m>", recipientForEcho);
		}
		return Service.START_STICKY; // service will restart after being terminated by the runtime
	}

	public void tryResetBluetooth()
	{
		Log.d(LOG, "in tryResetBluetooth");
		if (BT != null)
		{
			if (BT.resetConnection())
			{
				_commFlagBT = false;
				_echoReceivedBT = false;
				_robotStatus = "Resetting BT";
				_lastArduinoReceivedTime = System.currentTimeMillis(); // give it some time to settle down before testing it
				Log.d(LOG, "_lastArduinoReceivedTime set in tryResetBluetooth");

			}
			else
			{
				_bluetoothProblem = true;
				_commFlagBT = false;
				_echoReceivedBT = false;
				_bluetoothStatus = "Hardware problem, check power";
				_robotStatus = "Not operating";
			}
		}
	}

	private void tryResetXMPP()
	{
		Log.d(LOG, "in tryResetXMPP");
		//_XMPPproblem = true;
		//_commFlagXMPP = false;
		_XMPPstatus = "XMPP lost";
		//_robotStatus = "Not operating";
	}

	private void tryResetC2DM()
	{
		Log.d(LOG, "checking reset C2DM");
		_C2DMproblem = true;
		_commFlagC2DM = false;
		_C2DMstatus = "C2DM lost";
		//_robotStatus = "Not operating";
	}

	public void connectBluetooth(String bluetoothAddress)
	{
		if (BT.getConnectionState() && (!_commFlagBT)) {
			Log.d(LOG, "bluetooth connection requested, already connected");
			_bluetoothStatus = "still connected";
			updateWidget();
			return;
		}
		getPreferences();
		if (!EntriesTest()) {
			Log.d(LOG, "Asking for bluetooth connection with bad address");
			Toast.makeText(this, "bad bluetooth address", Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(LOG, "in connectBluetooth");
		BT.setBTaddress(bluetoothAddress);
		if (BT.Connect()) {  // this blocks until BT connects or times out
			// might want to run in Async
			_bluetoothStatus = "Connected";
			updateWidget();
			Log.d(LOG, "Bluetooth connected");
			Toast.makeText(this, "Bluetooth connected", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(this, "Bluetooth connection failed, opening RobotCommActivity", Toast.LENGTH_LONG).show();
			Log.d(LOG, "bluetooth connection failed");
			_bluetoothStatus = "Connection failed";
			updateWidget();
			Intent RobotCommIntent = new Intent(this, RobotCommActivity.class);
			RobotCommIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(RobotCommIntent);
		}
	}

	public void connectXMPP()
	{
		if (xmpp.getConnectionState()) {
			_XMPPstatus = "still connected";
			updateWidget();
			Log.d(LOG, "XMPP connection requested, already connected");
			return;
		}
		getPreferences();
		if (!EntriesTest()) {
			Log.d(LOG, "Asking for XMPP connection with bad parameters");
			Toast.makeText(this, "bad parameters", Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(LOG, "in connectXMPP");
		xmpp.setCommParameters(host, portNumber, service, userid, password, recipient, recipientForEcho);
		if (xmpp.Connect())
		{
			_connection = xmpp.getXMPPConnection();
			Toast.makeText(this, "XMPP connected", Toast.LENGTH_SHORT).show();
			Log.d(LOG, "XMPP connected");
			_XMPPstatus = "Connected";
			updateWidget();
			setupPacketListener();
		}
		else {
			Toast.makeText(this, "XMPP connection failed, opening RobotCommActivity", Toast.LENGTH_LONG).show();
			Log.d(LOG, "XMPP connection failed");
			_XMPPstatus = "Not connected";
			updateWidget();
			_connection = null;
			Intent RobotCommIntent = new Intent(this, RobotCommActivity.class);
			RobotCommIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(RobotCommIntent);
		}
	}

	public void connectC2DM()
	{
		// attempt a registration
		Log.d(LOG, "in connectC2DM");

		// set up the registration intent
		Intent intentC2dm = new Intent("com.google.android.c2dm.intent.REGISTER");
		intentC2dm.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), 0));

		// this must be the registered gmail account, the comment in the tutorial is wrong
		intentC2dm.putExtra("sender", "telebotphone@gmail.com");

		// fire the registration intent
		startService(intentC2dm);

	}

	public boolean sendCommandEchoFromArduino(String data)
	{
		if (xmpp == null)
		{
			Log.d(LOG, "tried to send data: " + data + " to XMPP server when xmpp not created");
			return false;
		}
		Log.d(LOG, "in sendCommandEchoFromArduino: " + data);
		if (xmpp.sendCommandEchoFromArduino(data))
		{
			_messageSentToServer = data;
			updateWidget();
			return true;
		}
		return false;
	}

	public boolean messageToServer(String data, String responseValue)
	{
		if (xmpp == null)
		{
			Log.d(LOG, "tried to send data: " + data + " to XMPP server when xmpp not created");
			return false;
		}
		Log.d(LOG, "in messageToServer: " + data + ", " + responseValue);
		if (xmpp.sendMessage(data, responseValue))
		{
			_messageSentToServer = data;
			updateWidget();
			return true;
		}
		return false;
	}


	public boolean sendDataToArduino(String robotCommand)
	{
		if (BT == null)
		{
			Log.d(LOG, "tried to sendDataToArduino: " + robotCommand + " when BT not created");
			messageToServer("command received: error: bluetooth not created", recipient);	// let the user know
			return false;
		}
		Log.d(LOG, "in sendDataToArduino");
		if (BT._isConnected)
		{
			Toast.makeText(this, robotCommand, Toast.LENGTH_SHORT).show();
			// only send an echo of the command the server sent,
			// don't send local echos to the server, they are just for local BT latency monitoring
			//if (!robotCommand.startsWith("commCheckBT")  && (!robotCommand.startsWith("!")))
			//	messageToServer("command received: " + robotCommand, recipientForEcho);	// echo command
			// we echo the command from here to let the server know it was received at this level
			// a second echo should come back as the arduino echos commands it receives
			// and those will come through to RobotCommServer as an intent
			// with a string in "messageToServer"
			if (BT.sendMessage(robotCommand)) {
				_messageSentToRobot = robotCommand;
				updateWidget();
				return true;
			}
			else return false;
		}
		Log.d(LOG, "tried to sendDataToArduino: " + robotCommand + " when BT not connected");
		messageToServer("error: bluetooth not connected", "m");	// let the user know
		return false;
	}

	public boolean EntriesTest()
	{
		Log.d(LOG, "in EntriesTest");
		if ( userid.contains("@") || userid.contains("not set")) return false;
		if ( password.contains("not set")) return false;
		try { portNumber = Integer.parseInt(port); }
		catch(NumberFormatException nfe) { return false; }
		if ( (!recipient.contains("@")) || (!recipient.contains(".")))return false;
		if ( (!recipientForEcho.contains("@")) || (!recipientForEcho.contains(".")))return false;
		if ( !host.contains(".")) return false;
		if ( !service.contains(".")) return false;
		if ( userid.contains("@")) return false;
		if (!BluetoothAdapter.checkBluetoothAddress(bluetooth)) return false;
		return true;
	}

	private final IBinder binder = new MyBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(LOG, "in onBind");
		return binder;
	}

	public class MyBinder extends Binder {
		RobotCommService getService() {
			Log.d(LOG, "in MyBinder");
			return RobotCommService.this;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG, "in onDestroy");
		Toast.makeText(this, "RobotCommService Destroyed", Toast.LENGTH_SHORT).show();
	}

	// once we are connected, we want to listen for packets
	// so set up a packet listener
	// don't know why this does not work when it is in the XMPP class
	// but having it here does avoid bothering with processing the intent
	public void setupPacketListener()
	{
		Log.d(LOG, "in setupPacketListener");
		if (_connection == null) {
			Log.d(LOG, "tried to setup packetListener with no connection present");
			return;
		}
		// Add a packet listener to get messages sent to us
		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		_connection.addPacketListener(new PacketListener() {
			public void processPacket(Packet packet) {
				Log.d(LOG, "in processPacket");
				Message message = (Message) packet;
				if (message.getBody() != null) {
					String body = message.getBody();
					if (body.contains("<"))
					{
						MessageToRobot  myMessage  = new MessageToRobot(message.getBody());
						_robotCommand = myMessage.commandChar + myMessage.commandArguments;
					} else 	_robotCommand = body;
					Log.d(LOG, "message received in processPacket: " + _robotCommand);
					_messageReceivedFromServer = _robotCommand;

					_lastXMPPreceivedTime = System.currentTimeMillis();
					Log.d(LOG, "_lastArduinoReceivedTime updated in processPacket");

					if (_robotCommand.startsWith("!")) // see if it is just an XMPP server echo
					{
						_echoReceivedXMPP = true;
						_echoReceivedTimeXMPP = System.currentTimeMillis(); // just an echo
						Log.d(LOG, "_echoReceivedTimeXMPP updated in processPacket");
					}
					else	// don't send along the message if was just an echo
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

	private void updateWidget()
	{
		Log.d(LOG, "in updateWidget");
		// Build the intent to call the service
		Intent intent = new Intent(this, UpdateWidgetService.class);
		intent.putExtra("bluetooth", "    BT: " + _bluetoothStatus);
		intent.putExtra("XMPP", "    XMPP: " + _XMPPstatus);
		intent.putExtra("sentToServer", "    Sent To Server: " + _messageSentToServer);
		intent.putExtra("sentToRobot", "    Sent To Robot: "+  _messageSentToRobot);
		this.startService(intent);
	}

	final class checkCommTimer extends TimerTask
	{
		public void run() {
			Log.d(LOG, "in checkCommTimer");
			updateWidget();

			if (!_bluetoothProblem)
			{
				if (System.currentTimeMillis() - _lastArduinoReceivedTime > TIME_OUT_ARDUINO)
				{
					// if we sent a character for echo and heard nothing, we need to reset BT
					if (_commFlagBT)
					{
						Log.d(LOG, "Arduino comm timed out after an echo request, so we will reset bluetooth");
						Intent serviceIntent = new Intent();
						serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
						serviceIntent.putExtra("reset", "bluetooth");
						_context.startService(serviceIntent);
					}
					else
					{
						// we have not heard anything from the arduino for a while
						// so we will send a character to be echoed
						_commFlagBT = true;
						_echoReceivedBT = false;
						_bluetoothStatus = "Checking bluetooth connection";
						Log.d(LOG, "Arduino comm timed out, so we will send an echo request to see if BT is still connected");
						Intent serviceIntent = new Intent();
						serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
						serviceIntent.putExtra("commCheckBT", "c");
						_echoSentTimeBT = System.currentTimeMillis();
						Log.d(LOG, "_echoSentTimeBT set");
						// need to update received time too, so that if a real command comes back
						// before the echo is returned (so _echoReceivedTimeBT is not updated)
						// we will still get a nonnegative number for latency
						_echoReceviedTimeBT = System.currentTimeMillis();
						Log.d(LOG, "_echoReceivedTimeBT set");
						_lastArduinoReceivedTime = System.currentTimeMillis();
						Log.d(LOG, "_lastArduinoReceivedTime reset for echo");
						_context.startService(serviceIntent);
						// and reset the ArduinoReceivedTime so that if we again exceed the timeout,
						// we know we have a real bluetooth comm issue, not just a long
						// time between user commands

					}
				}
				else if (_echoReceivedBT)
				{
					Log.d(LOG, "arduino echo request returned good");
					// our BT comm check came back good
					// note that if a real command happens to return before the echo is returned
					// that's OK, we will end up calculating a faster than real latency
					// but it does not impact us and will be a rare occurrence.
					_commFlagBT = false;
					_echoReceivedBT = false;
					_robotStatus = "bluetooth OK";
					_latencyBT = _echoReceviedTimeBT - _echoSentTimeBT;
					_bluetoothStatus = "latency = " + String.valueOf(_latencyBT);
					Log.d(LOG, "_latencyBT calculated = " + _latencyBT);
				}
			}
			/*
			boolean sendServerEcho = false;
			// now check on XMPP
			if ( (!_XMPPproblem) && System.currentTimeMillis() - _lastXMPPreceivedTime > TIME_OUT_XMPP)
			{
				// if we sent a character for echo and heard nothing, we need to reset XMPP
				// unless we already tried and failed to reset, in which case we give up
				if (_commFlagXMPP)
				{
					Log.d(LOG, "XMPP comm timed out after an echo request, so we will try to reset XMPP");
					Intent serviceIntent = new Intent();
					serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
					serviceIntent.putExtra("reset", "XMPP");
					_context.startService(serviceIntent);
				}
				else
				{
					Log.d(LOG, "XMPP comm timed out, so we will request an echo to see if XMPP is still connected");
					sendServerEcho = true;
				}
			}
			else if (_commFlagXMPP)
			{
				Log.d(LOG, "XMPP echo request returned good");
				// our XMPP comm check came back good
				// note that if a real command happens to return before the echo is returned
				// that's OK, we will end up calculating a faster than real latency
				// but it does not impact us and will be a rare occurrence.
				_commFlagXMPP = false;
				_robotStatus = "XMPP OK";
				_latencyXMPP = _echoReceivedTimeXMPP - _echoSentTimeServer;
				_XMPPstatus = "latency = " + String.valueOf(_latencyXMPP);
				Log.d(LOG, "_latencyXMPP calculated = " + _latencyXMPP);
			}

			if ( (!_C2DMproblem) && System.currentTimeMillis() - _lastC2DMreceivedTime > TIME_OUT_C2DM)
			{
				if (_commFlagC2DM)
				{
					Log.d(LOG, "C2DM comm timed out after an echo request, so we will try to reset C2DM");
					Intent serviceIntent = new Intent();
					serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
					serviceIntent.putExtra("reset", "C2DM");
					_context.startService(serviceIntent);
				}
				else
				{
					Log.d(LOG, "C2DM comm timed out, so we will request and echo to see if C2DM is still connected");
					sendServerEcho = true;
				}
			}
			else if (_commFlagC2DM)
			{
				Log.d(LOG, "C2DM echo came back good");
				// our C2DM comm check came back good
				// note that if a real command happens to return before the echo is returned
				// that's OK, we will end up calculating a faster than real latency
				// but it does not impact us and will be a rare occurrence.
				_commFlagC2DM = false;
				_robotStatus = "C2DM OK";
				_latencyC2DM = _echoReceivedTimeC2DM - _echoSentTimeServer;
				_C2DMstatus = "latency = " + String.valueOf(_latencyC2DM);
				Log.d(LOG, "_latencyC2DM calculated = " + _latencyC2DM);
			}

			if (sendServerEcho)
			{
				_robotStatus = "Checking XMPP and C2DM connections";
				_commFlagC2DM = true;
				_commFlagXMPP = true;
				_echoSentTimeServer = System.currentTimeMillis();
				Log.d(LOG, "_echoSentTimeServer set");
				// need to update received time too, so that if a real command comes back
				// before the echo is returned (so _echoReceivedTimeXMPP is not updated)
				// we will still get a nonnegative number for latency
				_echoReceivedTimeXMPP = System.currentTimeMillis();
				Log.d(LOG, "_echoReceivedTimeXMPP set");
				_echoReceivedTimeC2DM = System.currentTimeMillis();
				Log.d(LOG, "_echoReceivedTimeC2DM set");
				_lastXMPPreceivedTime = System.currentTimeMillis();
				Log.d(LOG, "_lastXMPPreceivedTime reset for echo");
				_lastC2DMreceivedTime = System.currentTimeMillis();
				Log.d(LOG, "_lastC2DMreceivedTime reset for echo");
				Intent serviceIntent = new Intent();
				serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
				serviceIntent.putExtra("SendToServer", "commCheckServer");
				_context.startService(serviceIntent);
				// and reset the ReceivedTimes so that if we again exceed the timeout,
				// we know we have a real server comm issue, not just a long
				// time between user commands
			}
			 */
		}
	}
}

