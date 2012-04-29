package com.denbar.XMPP_Robot;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Gather the xmpp settings and create an XMPPConnection
 */
public class SettingsDialog extends Dialog implements android.view.View.OnClickListener {
    private XMPPClient xmppClient;
    String host, port, service, username, password;

    public SettingsDialog(XMPPClient xmppClient) {
        super(xmppClient);
        this.xmppClient = xmppClient;
    }

    protected void onStart() {
        super.onStart();
        setContentView(R.layout.settings);
        getWindow().setFlags(4, 4);
        setTitle("XMPP Settings");
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(this);
    	// enter the server values
        host = "9thsense.com";
        port = "5222";
        service = "9thsense.com";
        username = "droidbot";
        password = "9thsense";

        /*
        host = "talk.google.com";
        port = "5222";
        service = "gmail.com";
        username = "telebotphone";
        password = "9thsense&";
        */

        setText(R.id.host, host);
        setText(R.id.port, port);
        setText(R.id.service, service);
        setText(R.id.userid, username);
        setText(R.id.password, password);
    }

    public void onClick(View v) {

        host = getText(R.id.host);
        port = getText(R.id.port);
        service = getText(R.id.service);
        username = getText(R.id.userid);
        password = getText(R.id.password);

        // Create a connection
        ConnectionConfiguration connConfig =
                //new ConnectionConfiguration(host, Integer.parseInt(port)); // service is optional
        	new ConnectionConfiguration(host, Integer.parseInt(port), service);
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
        }
        try {
            connection.login(username, password);
            Log.i("XMPPClient", "Logged in as " + connection.getUser());

            // Set the status to available
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
            xmppClient.setConnection(connection);
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + username);
            Log.e("XMPPClient", ex.toString());
                xmppClient.setConnection(null);
        }
        dismiss();
    }

    private String getText(int id) {
        EditText widget = (EditText) this.findViewById(id);
        return widget.getText().toString();
    }

    private void setText(int id, String value) {
        EditText widget = (EditText) this.findViewById(id);
        widget.setText(value);
    }
}
