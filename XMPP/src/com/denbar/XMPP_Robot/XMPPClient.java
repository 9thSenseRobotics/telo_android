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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import at.abraxas.amarino.AmarinoIntent;

public class XMPPClient extends Activity {

	public static final String ROBOT_COMMAND_INTENT = "com.denbar.action.ROBOT_COMMMAND";
    private Handler mHandler = new Handler();
    private EditText editRecipient, editSend, editMessages, editRobotName, editHost;
    private EditText editPort, editUserID, editService, editPassword;
    private String robotName, host, port, service, userid, password, recipient;
    private XMPPClient xmppClient;
    private XMPPConnection connection;
    private ArduinoReceiver arduinoReceiver;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i("XMPPClient", "onCreate called");
        setContentView(R.layout.main);

        editRobotName = (EditText) findViewById(R.id.robotname);
        editHost = (EditText) findViewById(R.id.host);
        editPort = (EditText) findViewById(R.id.port);
        editService = (EditText) findViewById(R.id.service);
        editUserID = (EditText) findViewById(R.id.userid);
        editPassword = (EditText) findViewById(R.id.password);
        editRecipient = (EditText) findViewById(R.id.recipient);
        editMessages= (EditText) findViewById(R.id.listMessages);
        editRecipient = (EditText) findViewById(R.id.recipient);
        editSend = (EditText) findViewById(R.id.sendText);

        // We will get the strings needed for logging in from a preferences file
        // that contains the last set used.
        // If this is the first boot, then we will fill in the values
        // from the resource file strings.xml, which means that we can change those
        // values by simply swapping the strings.xml file, no code changes required.

        Resources robotResources = getResources();

        SharedPreferences prefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );

        robotName = prefs.getString("robotName",robotResources.getString(R.string.robot_name));
        host = prefs.getString("host",robotResources.getString(R.string.host));
        port = prefs.getString("port",robotResources.getString(R.string.port));
        service = prefs.getString("service",robotResources.getString(R.string.service));
        userid = prefs.getString("userid",robotResources.getString(R.string.userid));
        password = prefs.getString("password",robotResources.getString(R.string.password));
        recipient = prefs.getString("recipient",robotResources.getString(R.string.recipient));

        // now set the global variables because the dialog type is not able to get shared preferences.
        // So it will get the values as global variables.
        // XMPPApplication.getInstance().setGlobalStrings(robotName,host, port, service, userid, password, recipient);

        // display the values for the user:
        editRobotName.setText(robotName);
        editHost.setText(host);
        editPort.setText(port);
        editService.setText(service);
        editUserID.setText(userid);
        editPassword.setText(password);
        editRecipient.setText(recipient);
        editMessages.setText("Received messages and status are displayed here");
        editSend.setText("Enter text here that you want to send");

        // Set a button listener to update entries
        Button setup = (Button) this.findViewById(R.id.OK);
        setup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SharedPreferences newprefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );
                SharedPreferences.Editor editor = newprefs.edit();
            	editor.putString("robotName", editRobotName.getText().toString());
            	editor.putString("host", editHost.getText().toString());
            	editor.putString("port", editPort.getText().toString());
            	editor.putString("service", editService.getText().toString());
            	editor.putString("userid", editUserID.getText().toString());
            	editor.putString("password", editPassword.getText().toString());
            	editor.putString("recipient", editRecipient.getText().toString());
            	editor.commit();

            	editMessages.setText("Entries saved, connecting to server.");

            	serverConnect();

        		//Intent serviceIntent = new Intent();
        		//serviceIntent.setAction("com.denbar.XMPP_Robot.StartupService");

        		// start up the service that does registration
        		//startService(serviceIntent);

            }
        });
    }

    private void serverConnect()
    {
    	int portNumber;
    	try {
    	    portNumber = Integer.parseInt(port);
    	} catch(NumberFormatException nfe) {
    		editMessages.setText("port number is not an integer, please fix!");
    		return;
    	}

        ConnectionConfiguration connConfig =
    	//new ConnectionConfiguration(host, portNumber, service);
        new ConnectionConfiguration(host,portNumber); // service is optional
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
    }
    try {
        connection.login(userid,password);
        Log.i("XMPPClient", "Logged in as " + connection.getUser());

        // Set the status to available
        Presence presence = new Presence(Presence.Type.available);
        connection.sendPacket(presence);
        editMessages.setText("Logged in to server");
        setConnection(connection);
        //Toast.makeText(this, "XMPP Log in successful", Toast.LENGTH_SHORT).show();
    } catch (XMPPException ex) {
        Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + userid);
        Log.e("XMPPClient", ex.toString());
        setConnection(null);
        editMessages.setText("Log in to server failed");
        Toast.makeText(this, "XMPP Log in failed", Toast.LENGTH_SHORT).show();
    }


        // Set a button listener to send a chat text message
        Button send = (Button) this.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
        	    String text = editSend.getText().toString();
        	    sendDataToServer(text);
            }
        });
    }

	void sendDataToServer(String data)
	{
	    String to = editRecipient.getText().toString();
	    if (connection != null)
	    {
	    	Log.i("XMPPClient", "Sending text [" + data + "] to [" + to + "]");
	    	Message msg = new Message(to, Message.Type.chat);
	    	msg.setBody(data);
	    	connection.sendPacket(msg);
	    	editSend.setText("");
	    }
	    else
	    {
	    	editMessages.setText("failed to send, no connection set");
	    }
	}

	@Override
	protected void onStart() {
		super.onStart();
		// in order to receive broadcasted intents we need to register our receiver
		//registerReceiver(arduinoReceiver, new IntentFilter(AmarinoIntent.ACTION_RECEIVED));
	}


	@Override
	protected void onStop() {
		super.onStop();

		// never forget to unregister a registered receiver
		//unregisterReceiver(arduinoReceiver);
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
                        	   broadcastIntent(payload);
                            }
                        });
                    }
                }
            }, filter);
        }
    }

	void broadcastIntent(String RobotCommand) {
		Intent BroadcastIntent = new Intent(ROBOT_COMMAND_INTENT);
		BroadcastIntent.putExtra("robotCommand", RobotCommand);
		this.sendBroadcast(BroadcastIntent);
        //Toast.makeText(this, RobotCommand, Toast.LENGTH_SHORT).show();
		editMessages.setText("received: " + RobotCommand);

	}

	public class ArduinoReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String data = null;

			// the device address from which the data was sent, we don't need it here but to demonstrate how you retrieve it
			//final String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);

			// the type of data which is added to the intent
			final int dataType = intent.getIntExtra(AmarinoIntent.EXTRA_DATA_TYPE, -1);

			// we only expect String data though, but it is better to check if really string was sent
			// later Amarino will support different data types, so far data comes always as string and
			// you have to parse the data to the type you have sent from Arduino, like it is shown below
			if (dataType == AmarinoIntent.STRING_EXTRA){
				data = intent.getStringExtra(AmarinoIntent.EXTRA_DATA);

				if (data != null){
					sendDataToServer(data);

				}
			}
		}
	}

    private void setText(int id, String value) {
        EditText widget = (EditText) this.findViewById(id);
        widget.setText(value);
    }
 }