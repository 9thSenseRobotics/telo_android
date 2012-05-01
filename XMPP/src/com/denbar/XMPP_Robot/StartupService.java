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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import at.abraxas.amarino.AmarinoIntent;

/**
 * Service that attempts to register with XMPP server from 9th Sense
 * Is implemented as a service so that it's easy to call on boot.
 *
 */
public class StartupService extends Service {

	public static final String ROBOT_COMMAND_INTENT = "com.denbar.action.ROBOT_COMMMAND";
    private Handler mHandler = new Handler();
    private XMPPConnection connection;
    private String robotName, host, port, service, userid, password, bluetooth, recipient;
    private int portNumber;
    private boolean amarinoBluetoothConnected;


	@Override
	public IBinder onBind(Intent intent) {

		return null;

	}


	@Override
	public void onDestroy() {
		super.onDestroy();
        unregisterReceiver(connectionStateReceiver);
		Toast.makeText(this, "XMPP Service Destroyed", Toast.LENGTH_SHORT).show();
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if ((flags & START_FLAG_RETRY) == 0)
		{
			Toast.makeText(this, "Recovering from abnormal shutdown.....", Toast.LENGTH_LONG).show();
		}


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

        if (EntriesTest())
        {
        	amarinoConnect();
        	serverConnect();
        }
        else
        {
        	Intent XMPPClientIntent = new Intent(this, XMPPClient.class);
        	startActivity(XMPPClientIntent);
        }

		if (intent != null)	// intent is null if it is system restart
		{
			String messageToXMPP_Server = intent.getStringExtra("message");
			String robotCommandToAmarino = intent.getStringExtra("RobotCommand");
			String BluetoothState = intent.getStringExtra("bluetooth");
			if (messageToXMPP_Server != null) sendDataToServer(messageToXMPP_Server);
			if (robotCommandToAmarino != null) sendDataToAmarino(robotCommandToAmarino);
			if (BluetoothState != null) // we make this a string so we can test if this was sent or not
			{
				if (BluetoothState.equals("true")) amarinoBluetoothConnected = true;
				else
				{
					amarinoBluetoothConnected = false;
					amarinoConnect();
				}
			}
		}
		return Service.START_STICKY; // service will restart after being terminated by the runtime
	}

	@Override
	public void onCreate() {
		super.onCreate();
		amarinoBluetoothConnected = false;
        registerReceiver(connectionStateReceiver, new IntentFilter(AmarinoIntent.ACTION_CONNECTED));

		Toast.makeText(this, "XMPP Service started, attempting log in...", Toast.LENGTH_SHORT).show();

		// attempt a registration
		Log.w("XMPP", "start log in process");


	}

	// tell amarino to connect to the bluetooth device we specify
	private void amarinoConnect()
	{
		//Amarino.connect(this, bluetooth);
		//Intent intent = new Intent(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
		//sendBroadcast(intent);
		Intent intent1 = new Intent(AmarinoIntent.ACTION_CONNECT);
		intent1.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, bluetooth);
		//sendBroadcast(intent1); // does not work right
	}

	//We want to know when the connection has been established, so that we can send commands
	private BroadcastReceiver connectionStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null){
				String action = intent.getAction();
				if (AmarinoIntent.ACTION_CONNECTED.equals(action)){
					amarinoBluetoothConnected = true;
				}
			}
		}
	};
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
                        final String payload = StringUtils.parseBareAddress(message.getBody());
                        mHandler.post(new Runnable() {
                           public void run() {
                        	   //broadcastIntent(payload);
                        	   sendDataToAmarino(payload);
                            }
                        });
                    }
                }
            }, filter);
        }
    }

    // we got a command from the XMPP server, so send it out as a broadcast
	void broadcastIntent(String RobotCommand) {
		Intent BroadcastIntent = new Intent(ROBOT_COMMAND_INTENT);
		BroadcastIntent.putExtra("robotCommand", RobotCommand);
		this.sendBroadcast(BroadcastIntent);
        Toast.makeText(this, RobotCommand, Toast.LENGTH_SHORT).show();
        sendDataToServer(RobotCommand);	// echo the command back to the XMPP server

	}

	private void sendDataToServer(String data)
	{
	    if (connection != null)
	    {
	    	Log.i("XMPPClient", "Sending text [" + data + "] to [" + recipient + "]");
	    	Message msg = new Message(recipient, Message.Type.chat);
	    	msg.setBody(data);
	    	connection.sendPacket(msg);
	    }
	    else
	    {
	    	Toast.makeText(this, "failed to send XMPP data, no connection set", Toast.LENGTH_SHORT).show();
	    }
	}

	private void sendDataToAmarino(String robotCommand)
	{
		//if (!amarinoBluetoothConnected)
		//{
		//	Toast.makeText(this, "attempting to get amarino to connect to bluetooth", Toast.LENGTH_SHORT).show();
		//	amarinoConnect();
		//}
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
    	try {
    	    portNumber = Integer.parseInt(port);
    	} catch(NumberFormatException nfe) {
    		return false;
    	}

    	if ( (!recipient.contains("@")) || (!recipient.contains(".")))
    	{
    		return false;
    	}

    	if ( !host.contains("."))
    	{
    		return false;
    	}

    	if ( !service.contains("."))
    	{
    		return false;
    	}

    	if ( userid.contains("@"))
    	{
    		return false;
    	}

    	if ( !bluetooth.contains(":"))
    	{
    		return false;
    	}

    	return true;
	}
}