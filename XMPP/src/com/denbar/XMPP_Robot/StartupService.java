package com.denbar.XMPP_Robot;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

/**
 * Service that attempts to register with XMPP server from 9th Sense
 * Is implemented as a service so that it's easy to call on boot.
 *
 */
public class StartupService extends Service {

    private XMPPClient xmppClient;
    //private String host, port, service, username, password;
	private long backoffTimeMs = 10000;

	@Override
	public IBinder onBind(Intent intent) {

		return null;

	}

	@Override
	public void onCreate() {
		super.onCreate();

		Toast.makeText(this, "XMPP Service Created", Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Toast.makeText(this, "XMPP Service Destroyed", Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onStart(Intent intent, int startId) {

		super.onStart(intent, startId);

		Toast.makeText(this, "XMPP Service started, attempting registration...", Toast.LENGTH_SHORT).show();

		// attempt a registration
		Log.w("XMPP", "start registration process");

        Resources robotResources = getResources();

        SharedPreferences prefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE );

        String robotName = prefs.getString("robotName",robotResources.getString(R.string.robot_name));
        String host = prefs.getString("host",robotResources.getString(R.string.host));
        String port = prefs.getString("port",robotResources.getString(R.string.port));
        String service = prefs.getString("service",robotResources.getString(R.string.service));
        String username = prefs.getString("userid",robotResources.getString(R.string.userid));
        String password = prefs.getString("password",robotResources.getString(R.string.password));
        String recipient = prefs.getString("recipient",robotResources.getString(R.string.recipient));

        /*
		host = "9thsense.com";
        port = "5222";
        service = "9thsense.com";
        username = "droidbot";
        password = "9thsense";

        // Create a connection

        ConnectionConfiguration connConfig =
        	new ConnectionConfiguration(host, Integer.parseInt(port), service);
            //new ConnectionConfiguration(host, Integer.parseInt(port)); // service is optional
        connConfig.setSASLAuthenticationEnabled(false);
        //connConfig.setReconnectionAllowed(true);
        XMPPConnection connection = new XMPPConnection(connConfig);


        try {
            connection.connect();
            Log.i("XMPPClient", "[SettingsDialog] Connected to " + connection.getHost());
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connection.getHost());
            Log.e("XMPPClient", ex.toString());
            xmppClient.setConnection(null);
            Toast.makeText(this, "XMPP Server connection failed", Toast.LENGTH_SHORT).show();
        }
        try {
            connection.login(username, password);
            Log.i("XMPPClient", "Logged in as " + connection.getUser());

            // Set the status to available
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
            xmppClient.setConnection(connection);
            Toast.makeText(this, "XMPP Log in successful", Toast.LENGTH_SHORT).show();
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + username);
            Log.e("XMPPClient", ex.toString());
                xmppClient.setConnection(null);
            Toast.makeText(this, "XMPP Log in failed", Toast.LENGTH_SHORT).show();
        }

*/
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
}