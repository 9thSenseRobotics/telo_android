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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Gets the startup intent and calls the service that will register with our
 * XMPP and C2DM servers This exists as glue between the
 * "hey we just finished booting" code and the registration code
 *
 */

// also gets robot command intents

public class StartupIntentReceiver extends BroadcastReceiver {
	private final String TAG = "StartupIntentReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "in onReceive");

		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");


		// from C2DM server
		String messageFromC2DM = intent.getStringExtra("robotCommand");
		if (messageFromC2DM != null)
		{
			serviceIntent.putExtra("C2DMmessage", messageFromC2DM);
			Log.d(TAG, "C2DM message received, forwarding to RobotCommService: " + messageFromC2DM);
		}
		else
		{
			serviceIntent.putExtra("Bootup", "connect");
			Log.d(TAG, "bootup complete, sending intent to RobotCommService to connect");
		}
		// start up the service
		context.startService(serviceIntent);
	}
}
