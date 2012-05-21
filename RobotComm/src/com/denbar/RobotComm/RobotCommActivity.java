package com.denbar.RobotComm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RobotCommActivity extends Activity {

	public static final String ROBOT_COMMAND_INTENT = "com.denbar.action.ROBOT_COMMMAND";
    private EditText editRecipient, editSend, editRobotName, editHost;
    private EditText editPort, editUserID, editService, editPassword, editBluetooth;
    private EditText editXMPPstatus, editBluetoothStatus, editStatus, editReceived, editSent;
    private String robotName, host, port, service, userid, password, bluetooth, recipient;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Toast.makeText(this, "RobotComm activity created", Toast.LENGTH_SHORT).show();


        Log.i("RobotComm", "onCreate called");
        setContentView(R.layout.main);


        editRobotName = (EditText) findViewById(R.id.robotname);
        //editHost = (EditText) findViewById(R.id.host);
        //editPort = (EditText) findViewById(R.id.port);
        //editService = (EditText) findViewById(R.id.service);
        //editUserID = (EditText) findViewById(R.id.userid);
        //editPassword = (EditText) findViewById(R.id.password);
        //editBluetooth = (EditText) findViewById(R.id.bluetooth);
        //editRecipient = (EditText) findViewById(R.id.recipient);
        //editRecipient = (EditText) findViewById(R.id.recipient);
        editSend = (EditText) findViewById(R.id.sendText);
        editXMPPstatus = (EditText) findViewById(R.id.XMPPstatus);
        editBluetoothStatus = (EditText) findViewById(R.id.bluetoothStatus);
        editStatus = (EditText) findViewById(R.id.status);
        editReceived = (EditText) findViewById(R.id.received);
        editSent = (EditText) findViewById(R.id.sent);

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
        bluetooth = prefs.getString("bluetooth",robotResources.getString(R.string.bluetooth));
        recipient = prefs.getString("recipient",robotResources.getString(R.string.recipient));

    	// set the global variable for the bluetooth device address
        // do this early so that AmarinoIntentReceiver does not think
        // we are connected to the wrong device simply due to
        // the bluetooth address being not set in the global variable
    	//XMPPApplication.getInstance().setBluetoothAddress(bluetooth);

        // display the values for the user:
        editRobotName.setText(robotName);
        //editHost.setText(host);
        //editPort.setText(port);
        //editService.setText(service);
        //editUserID.setText(userid);
        //editPassword.setText(password);
        //editBluetooth.setText(bluetooth);
        //editRecipient.setText(recipient);
        editSend.setText("Enter text here that you want to send");
        editXMPPstatus.setText("not connected yet");
        editBluetoothStatus.setText("not connected yet");
        editStatus.setText("Robot status is displayed here");
        editReceived.setText("Received messages are displayed here");
        editSent.setText("Sent messages are displayed here");

        // Set a button listener to login
        Button setup = (Button) this.findViewById(R.id.Connect);
        setup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	robotName = editRobotName.getText().toString();
            	host = editHost.getText().toString();
            	port = editPort.getText().toString();
            	service = editService.getText().toString();
            	userid = editUserID.getText().toString();
            	password = editPassword.getText().toString();
            	bluetooth = editBluetooth.getText().toString();
            	recipient = editRecipient.getText().toString();

                SharedPreferences newprefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );
                SharedPreferences.Editor editor = newprefs.edit();
            	editor.putString("robotName",robotName);
            	editor.putString("host", host);
            	editor.putString("port", port);
            	editor.putString("service", service);
            	editor.putString("userid", userid);
            	editor.putString("password", password);
            	editor.putString("bluetooth",bluetooth);
            	editor.putString("recipient", recipient);
            	editor.commit();

            	// set the global variable for the bluetooth device address
            	//XMPPApplication.getInstance().setBluetoothAddress(bluetooth);

            	if (EntriesTest())
            	{
            		editSend.setText("");
                	editStatus.setText("Connecting to server.");
	        		Intent serviceIntent = new Intent();
	        		serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
	        		// start up the service that does registration
	        		startService(serviceIntent);
            	}
            	else editStatus.setText("Error connecting, check setup parameters");

            }
        });

        // Set a button listener to send a chat text message
        Button send = (Button) this.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
        	    String text = editSend.getText().toString();
        		Intent serviceIntent = new Intent();
        		serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
        		serviceIntent.putExtra("messageToServer", text);
        		startService(serviceIntent);
        		editSend.setText("");
        		editSent.setText(text);
            }
        });

        // Set a button listener to connect to bluetooth and servers
        Button Connect = (Button) this.findViewById(R.id.Connect);
        Connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
        		Intent serviceIntent = new Intent();
        		serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
        		serviceIntent.putExtra("Connect", bluetooth);
        		startService(serviceIntent);
        		editStatus.setText("Connecting to bluetooth and servers");
            }
        });
    }	// ends on Create


	@Override
	protected void onStart() {
		super.onStart();
		Toast.makeText(this, "RobotComm activty started", Toast.LENGTH_SHORT).show();
    	EntriesTest();
	}

	boolean EntriesTest()
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
    	if (!returnResult) editSend.setText(message);
    	return returnResult;
	}

}