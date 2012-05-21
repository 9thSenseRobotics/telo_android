package com.denbar.RobotComm;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


//Service that supports the 9th Sense robots
public class RobotCommService extends Service {

	private Handler _Handler = new Handler();
	private XMPPConnection _connection;
	private String _robotCommand, _robotArguments;
	private String host, port, service;
	private String userid, password, bluetooth, recipient;
	private int portNumber;
	private arduinoBT BT;
	private XMPP xmpp;

	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this, "RobotCommService created", Toast.LENGTH_SHORT).show();

		BT = new arduinoBT(this);
		xmpp = new XMPP(this);

		Resources robotResources = getResources();
		SharedPreferences prefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );
		//robotName = prefs.getString("robotName",robotResources.getString(R.string.robot_name));
		host = prefs.getString("host",robotResources.getString(R.string.host));
		port = prefs.getString("port",robotResources.getString(R.string.port));
		service = prefs.getString("service",robotResources.getString(R.string.service));
		userid = prefs.getString("userid",robotResources.getString(R.string.userid));
		password = prefs.getString("password",robotResources.getString(R.string.password));
		bluetooth = prefs.getString("bluetooth",robotResources.getString(R.string.bluetooth)).toUpperCase();
		recipient = prefs.getString("recipient",robotResources.getString(R.string.recipient));

		if (!EntriesTest()) {
			Toast.makeText(this, "Entries test in server failed, opening RobotCommActivity", Toast.LENGTH_LONG).show();
			Intent RobotCommIntent = new Intent(this, RobotCommActivity.class);
			//startActivity(RobotCommIntent);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if ((flags & START_FLAG_RETRY) == 0)
		{
			Toast.makeText(this, "Recovering from abnormal shutdown.....", Toast.LENGTH_LONG).show();
		}

		Toast.makeText(this, "RobotComm service started", Toast.LENGTH_SHORT).show();
		if (intent != null)	// intent is null if it is system restart
		{
			String messageToXMPP_Server = intent.getStringExtra("messageToServer");
			String robotCommandToArduino = intent.getStringExtra("RobotCommand");
			String connectAddress = intent.getStringExtra("Connect");
			if (messageToXMPP_Server != null) sendDataToXMPPserver(messageToXMPP_Server);
			if (robotCommandToArduino != null)
			{
				sendDataToArduino(robotCommandToArduino);	// send command

			}
			if (connectAddress != null)
			{
				connectBluetooth(connectAddress);
				connectXMPP();
			}
		}
		else if (BT.getConnectionState() && xmpp.getConnectionState()) sendDataToXMPPserver("<m><re>1.0</re></m>");
		// the else will run on system start or restart, so we let the XMPP server know we are up

		return Service.START_STICKY; // service will restart after being terminated by the runtime
	}

	private void connectBluetooth(String bluetoothAddress)
	{
		if (BT.getConnectionState()) {
			Log.d("RobotCommService", "bluetooth connection requested, already connected");
			return;
		}

		BT.setBTaddress(bluetoothAddress);
		if (BT.Connect()) {
			Log.d("RobotCommService", "Bluetooth connected");
			Toast.makeText(this, "Bluetooth connected", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(this, "Bluetooth connection failed, opening RobotCommActivity", Toast.LENGTH_LONG).show();
			Intent RobotCommIntent = new Intent(this, RobotCommActivity.class);
			//startActivity(RobotCommIntent);
		}
	}

	private void connectXMPP()
	{
		if (xmpp.getConnectionState()) {
			Log.d("RobotCommService", "XMPP connection requested, already connected");
			return;
		}

		xmpp.setCommParameters(host, portNumber, service, userid, password, recipient);
		if (xmpp.Connect())
		{
			_connection = xmpp.getXMPPConnection();
			Toast.makeText(this, "XMPP connected", Toast.LENGTH_SHORT).show();
			Log.d("RobotCommService", "XMPP connected");
			setupPacketListener();
		}
		else {
			Toast.makeText(this, "XMPP connection failed, opening RobotCommActivity", Toast.LENGTH_LONG).show();
			Log.d("RobotCommService", "xmppConnect failed");
			_connection = null;
			Intent RobotCommIntent = new Intent(this, RobotCommActivity.class);
			//startActivity(RobotCommIntent);
		}
	}

	private boolean sendDataToXMPPserver(String data)
	{
		if (xmpp == null)
		{
			Log.d("RobotCommService", "tried to send data to XMPP server when xmpp not created");
			return false;
		}
		if (xmpp.sendData(data)) return true;
		return false;
	}

	private boolean sendDataToArduino(String robotCommand)
	{
		if (BT == null)
		{
			Log.d("RobotCommService", "tried to send message when BT not created");
			sendDataToXMPPserver("command received: error: bluetooth not created");	// let the user know
			return false;
		}
		if (BT._isConnected)
		{
			Toast.makeText(this, robotCommand, Toast.LENGTH_SHORT).show();
			sendDataToXMPPserver("command received: " + robotCommand);	// echo command
			// we echo the command from here to let the server know it was received at this level
			// a second echo should come back as the arduino echos commands it receives
			// and those will come through to RobotCommServer as an intent
			// with a string in "messageToServer"
			if (BT.sendMessage(robotCommand)) return true;
			else return false;
		}
		Log.d("RobotCommService", "tried to send message when BT not connected");
		Log.d("RobotCommService", "message: " + robotCommand);
		Toast.makeText(this, robotCommand, Toast.LENGTH_SHORT).show();
		sendDataToXMPPserver("command received: error: bluetooth not connected");	// let the user know
		return false;
	}

	boolean EntriesTest()
	{
		try { portNumber = Integer.parseInt(port); }
		catch(NumberFormatException nfe) { return false; }
		if ( (!recipient.contains("@")) || (!recipient.contains(".")))return false;
		if ( !host.contains(".")) return false;
		if ( !service.contains(".")) return false;
		if ( userid.contains("@")) return false;
		if (!BluetoothAdapter.checkBluetoothAddress(bluetooth)) return false;
		return true;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "RobotCommService Destroyed", Toast.LENGTH_SHORT).show();
	}

	// once we are connected, we want to listen for packets
	// so set up a packet listener
	// don't know why this does not work when it is in the XMPP class
	// but having it here does avoid bothering with processing the intent
	public void setupPacketListener()
	{
		if (_connection == null) {
			Log.d("XMPP", "tried to setup packetListener with no connectoin present");
			return;
		}
		// Add a packet listener to get messages sent to us
		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		_connection.addPacketListener(new PacketListener() {
			public void processPacket(Packet packet) {
				Message message = (Message) packet;
				if (message.getBody() != null) {
					String body = message.getBody();
					if (body.contains("<"))
					{
						MessageToRobot  myMessage  = new MessageToRobot(message.getBody());
						_robotCommand = myMessage.commandChar;
						_robotArguments = myMessage.commandArguments;
					} else {
						_robotCommand = body;
						_robotArguments = null;
					}
					_Handler.post(new Runnable() {
						public void run() {
							if (_robotArguments != null)
							{
								sendDataToArduino(_robotCommand + _robotArguments);
							} else {
								sendDataToArduino(_robotCommand);
							}
						}
					});
				}
			}
		}, filter);
	}
}

