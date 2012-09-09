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


import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class XMPP {
	private static final String TAG = "XMPP";
	private boolean _commParametersSet;
	private Context _context;
	private XMPPConnection _connection;
	private String _host, _service, _userid, _password, _recipient,
			_recipientForEcho;
	private int _portNumber;

	public XMPP(Context CallingContext) {
		_context = CallingContext;
		_commParametersSet = false;
		_connection = null;
	}

	public void setCommParameters(String host, int portNumber, String service,
			String userid, String password, String recipient,
			String recipientForEcho) {
		_host = host;
		_portNumber = portNumber;
		_service = service;
		_userid = userid;
		_password = password;
		_recipient = recipient;
		_recipientForEcho = recipientForEcho;
		_commParametersSet = true;
	}

	// Create a connection to the XMPP server
	public boolean Connect() {
		if (_connection != null) {
			Log.d(TAG, "tried to connect when already connected");
			return false; // already connected
		}
		if (!_commParametersSet) {
			Log.d(TAG, "tried to connect without setting comm Parameters");
			return false;
		} else {
			Log.d(TAG, "XMPP parameters:" + _host + " " + _portNumber + " "
					+ _service + " " + _userid + " " + _password);
		}
		ConnectionConfiguration connConfig = new ConnectionConfiguration(_host,
				_portNumber, _service);
		connConfig.setSASLAuthenticationEnabled(false);
		XMPPConnection connection = new XMPPConnection(connConfig);
		try {
			connection.connect();
			Log.i(TAG, "[SettingsDialog] Connected to " + connection.getHost());
		} catch (XMPPException ex) {
			Log.e(TAG, "[SettingsDialog] Failed to connect to "
					+ connection.getHost());
			Log.e(TAG, ex.toString());
			//Toast.makeText(_context, "XMPP Server connection failed", Toast.LENGTH_SHORT).show();
			return false;
		}
		try {
			connection.login(_userid, _password);
			Log.i(TAG, "Logged in as " + connection.getUser());

			// Set the status to available
			Presence presence = new Presence(Presence.Type.available);
			presence.setStatus("Robot v1.0");
			connection.sendPacket(presence);
			_connection = connection;
			//Toast.makeText(_context, "XMPP Log in successful", Toast.LENGTH_SHORT).show();
			// now that we have a connection, we need to set up a packet listener setupPacketListener();
			return true;
		} catch (XMPPException ex) {
			//Toast.makeText(_context, "XMPP Server login failed", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "[SettingsDialog] Failed to log in as " + _userid);
			Log.e(TAG, ex.toString());
			_connection = null;
			return false;
		}
	}

	public void resetConnection() {
		if (_connection != null) {
			_connection.disconnect();
			Log.d(TAG, "trying to disconnect from XMPP server");
		}
		_connection = null;
	}

	public XMPPConnection getXMPPConnection() {
		return _connection;
	}


	// echos the command that the server sent, as the echo comes back from the
	// arduino
	// the server ignores the content of the data, it is just as a heartbeat and
	// for latency
	public boolean sendCommandEchoFromArduino(String data, String responseValue) {
		if (_connection == null) {
			Log.d(TAG, "tried to send XMPP message when not connected");
			return false;
		}
		MessageFromRobot responseMessage = new MessageFromRobot(_recipientForEcho, _userid + "@9thsense.com", responseValue, data);
		Log.d(TAG, "Sending text [" + data + "] to [" + _recipientForEcho + "]");
		Message msg = new Message(_recipientForEcho, Message.Type.chat);
		msg.setBody(responseMessage.XMLStr);
		try {
			_connection.sendPacket(msg);
		} catch (Exception e) {
			Log.d(TAG, "exception from sendData: " + e.getMessage());
			return false;
		}
		return true;
	}

	// sendMessage sends a string to the server.
	// if responseValue == "a" then the string is ignored and the server returns
	// an acknowledgement "!alive"
	// if responseValue == "m" then the string is used by the server to display
	// to the user or something
	public boolean sendMessage(String data, String responseValue) {
		if (_connection == null) {
			Log.d(TAG, "tried to send XMPP message when not connected");
			return false;
		}
		MessageFromRobot responseMessage = new MessageFromRobot(_recipient,
				_userid + "@9thsense.com", responseValue, data);
		Log.i(TAG, "Sending text [ responseValue + data ] to [" + _recipient + "]");
		Message msg = new Message(_recipient, Message.Type.chat);
		msg.setBody(responseMessage.XMLStr);
		try {
			_connection.sendPacket(msg);
		} catch (Exception e) {
			Log.d(TAG, "exception from sendData: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean SendRobotMessageToServer(MessageFromRobot message)
	{
		Message msg = new Message(_recipient, Message.Type.chat);
		msg.setBody(message.XMLStr);
		try {
			_connection.sendPacket(msg);
		} catch (Exception e) {
			Log.d(TAG, "exception from sendData: " + e.getMessage());
			return false;
		}
		return true;		
	}

	public boolean getConnectionState() {
		if (_connection == null)
			return false;
		else
			return true;
	}
}
