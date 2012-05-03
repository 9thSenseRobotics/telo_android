package com.denbar.XMPP_Robot;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import at.abraxas.amarino.AmarinoIntent;

//Service that registers with XMPP server

public class StartupService extends Service {

	//public static final String ROBOT_COMMAND_INTENT = "com.denbar.action.ROBOT_COMMMAND";
    private Handler mHandler = new Handler();
    private XMPPConnection connection;
    private String robotName, host, port, service, forwardToAddress, userid, password, bluetooth, recipient;
    private int portNumber;

	@Override
	public void onCreate() {
		super.onCreate();
		XMPPApplication.getInstance().setBluetoothAttemptsCounter(0);
		connection = null;

        Resources robotResources = getResources();
        SharedPreferences prefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );
        robotName = prefs.getString("robotName",robotResources.getString(R.string.robot_name));
        host = prefs.getString("host",robotResources.getString(R.string.host));
        port = prefs.getString("port",robotResources.getString(R.string.port));
        service = prefs.getString("service",robotResources.getString(R.string.service));
        userid = prefs.getString("userid",robotResources.getString(R.string.userid));
        password = prefs.getString("password",robotResources.getString(R.string.password));
        bluetooth = prefs.getString("bluetooth",robotResources.getString(R.string.bluetooth));
        recipient = prefs.getString("recipient",robotResources.getString(R.string.recipient));

        forwardToAddress = robotResources.getString(R.string.forwardToAddress);

    	if (EntriesTest())
        {
        	XMPPApplication.getInstance().setBluetoothAddress(bluetooth);
        	if (!XMPPApplication.getInstance().getBluetoothConnected()) bluetoothInquire();
        	if (connection == null) serverConnect();
        }
        else
        {
        	Toast.makeText(this, "Entries test in server failed, opening XMPPClient", Toast.LENGTH_LONG).show();
        	Intent XMPPClientIntent = new Intent(this, XMPPClient.class);
        	startActivity(XMPPClientIntent);
        }

		Toast.makeText(this, "XMPP Service started, attempting log in...", Toast.LENGTH_SHORT).show();
		Log.w("XMPP", "start log in process");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if ((flags & START_FLAG_RETRY) == 0)
		{
			Toast.makeText(this, "Recovering from abnormal shutdown.....", Toast.LENGTH_LONG).show();
		}

		if (intent != null)	// intent is null if it is system restart
		{
			String messageToXMPP_Server = intent.getStringExtra("message");
			String robotCommandToAmarino = intent.getStringExtra("RobotCommand");
			if (messageToXMPP_Server != null) sendDataToServer(messageToXMPP_Server);
			if (robotCommandToAmarino != null) sendDataToAmarino(robotCommandToAmarino);
		}
		if (!XMPPApplication.getInstance().getBluetoothConnected()) bluetoothInquire();
		return Service.START_STICKY; // service will restart after being terminated by the runtime
	}

	// tell amarino to send the addresses of the bluetooth devices that it is connected to
	private void bluetoothInquire()
	{
		// only take an action if it is not already in work
		if (XMPPApplication.getInstance().getBluetoothAttemptsCounter() == 0)
		{
			Intent intent1 = new Intent(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
			intent1.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, XMPPApplication.getInstance().getBluetoothAddress());
			sendBroadcast(intent1);
		}
	}


    // Create a connection to the XMPP server
    private void serverConnect()
    {
        //if (connection != null) return; // already connected
        // we need to find out how to detect if we lost the connection
        // some kind of heartbeat
        ConnectionConfiguration connConfig = new ConnectionConfiguration(host, portNumber, service);
        	//new ConnectionConfiguration(host, portNumber); // service is optional

        connConfig.setSASLAuthenticationEnabled(false);
	    //connConfig.setReconnectionAllowed(true);
	    XMPPConnection connection = new XMPPConnection(connConfig);

	    try {
	        connection.connect();
	        Log.i("XMPPClient", "[SettingsDialog] Connected to " + connection.getHost());
	    } catch (XMPPException ex) {
	        Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connection.getHost());
	        Log.e("XMPPClient", ex.toString());
	        setConnection(null);
	        Toast.makeText(this, "XMPP Server connection failed", Toast.LENGTH_SHORT).show();
	        return;
	    }
	    try {
	        connection.login(userid,password);
	        Log.i("XMPPClient", "Logged in as " + connection.getUser());

	        // Set the status to available
	        Presence presence = new Presence(Presence.Type.available);
	        connection.sendPacket(presence);
	        setConnection(connection);
	        Toast.makeText(this, "XMPP Log in successful", Toast.LENGTH_SHORT).show();
	    } catch (XMPPException ex) {
	        Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + userid);
	        Log.e("XMPPClient", ex.toString());
	        setConnection(null);
	        Toast.makeText(this, "XMPP Log in failed", Toast.LENGTH_SHORT).show();
	    }

/*
		Intent intentRetry = new Intent("com.denbar.XMPP_Robot.RETRY");

		PendingIntent pending = PendingIntent.getBroadcast(this, 0, intentRetry, 0);

		// setup for checking to make sure things worked
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + backoffTimeMs,
                pending);
		backoffTimeMs *= 2;
	*/
		Log.w("XMPP", "registration process done");
	}

	// once we are connected, we want to listen for packets
	// so set up a packet listener
    public void setConnection(XMPPConnection connection)
    {
        this.connection = connection;	// get the class variable assigned (used for sending as well as here)
        if (connection != null) {
            // Add a packet listener to get messages sent to us
    		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            connection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        //String fromName = StringUtils.parseBareAddress(message.getFrom());
                        //Log.i("XMPPClient", "Got text [" + message.getBody() + "] from [" + fromName + "]");
                        // broadcast the incoming message
                        String body = StringUtils.parseBareAddress(message.getBody());
                        MessageToRobot  myMessage  = new MessageToRobot(body);
                        if (myMessage.driverAddr != null) forwardToAddress = myMessage.driverAddr;
                        if (myMessage.commandChar != null)
                        {
	                        final String robotCommand = myMessage.commandChar;
	                        mHandler.post(new Runnable() {
	                           public void run() {
	                        	   //broadcastIntent(payload);
	                        	   sendDataToAmarino(robotCommand);
	                            }
	                        });
                        }
                    }
                }
            }, filter);
        }
    }

	private void sendDataToServer(String data)
	{
	    if (connection != null)
	    {
	    	Log.i("XMPPClient", "Sending text [" + data + "] to [" + recipient + "]");
	    	//MessageFromRobot myFromMessage = new MessageFromRobot("test@test.com", userid, data);
	    	//MessageFromRobot myFromMessage = new MessageFromRobot(forwardToAddress, userid, data);
	    	Message msg = new Message(recipient, Message.Type.chat);
	    	msg.setBody(data);
	    	//msg.setBody(myFromMessage.XMLStr);
	    	connection.sendPacket(msg);
	    }
	    else
	    {
	    	Toast.makeText(this, "failed to send XMPP data, no connection set", Toast.LENGTH_SHORT).show();
	    }
	}

	private void sendDataToAmarino(String robotCommand)
	{
		if (!XMPPApplication.getInstance().getBluetoothConnected())
		{
			bluetoothInquire();
			Toast.makeText(this, "Skipping sendDataToAmarino, bluetooth not connected", Toast.LENGTH_SHORT).show();
	        return;
		}

		char cmdChar = robotCommand.charAt(0);
		int commandedSpeed = 178;	// we will get this as a commanded value later
		Intent intentAmarino = new Intent(AmarinoIntent.ACTION_SEND);
		intentAmarino.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, bluetooth);
		intentAmarino.putExtra(AmarinoIntent.EXTRA_DATA_TYPE, AmarinoIntent.INT_EXTRA);
		intentAmarino.putExtra(AmarinoIntent.EXTRA_FLAG, cmdChar);
		intentAmarino.putExtra(AmarinoIntent.EXTRA_DATA, commandedSpeed);
		sendBroadcast(intentAmarino);
        Toast.makeText(this, robotCommand, Toast.LENGTH_SHORT).show();
        sendDataToServer(robotCommand);	// echo the command back to the XMPP server
	}

	boolean EntriesTest()
	{
    	try { portNumber = Integer.parseInt(port); }
    	catch(NumberFormatException nfe) { return false; }
    	if ( (!recipient.contains("@")) || (!recipient.contains(".")))return false;
    	if ( !host.contains(".")) return false;
    	if ( !service.contains(".")) return false;
    	if ( userid.contains("@")) return false;
    	if ( !bluetooth.contains(":")) return false;
    	return true;
	}

    /*
    // we got a command from the XMPP server, so send it out as a broadcast
	void broadcastIntent(String RobotCommand) {
		Intent BroadcastIntent = new Intent(ROBOT_COMMAND_INTENT);
		BroadcastIntent.putExtra("robotCommand", RobotCommand);
		this.sendBroadcast(BroadcastIntent);
        Toast.makeText(this, RobotCommand, Toast.LENGTH_SHORT).show();
        sendDataToServer(RobotCommand);	// echo the command back to the XMPP server

	}
	*/

	@Override
	public IBinder onBind(Intent intent) {

		return null;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        //unregisterReceiver(connectionStateReceiver);
		Toast.makeText(this, "XMPP Service Destroyed", Toast.LENGTH_SHORT).show();
	}
}