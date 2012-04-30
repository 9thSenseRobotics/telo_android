package com.denbar.XMPP_Robot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Gets the startup intent and calls the service that will register with our XMPP server
 * This exists as glue between the "hey we just finished booting" code and the
 * registration code
 *
 */
public class StartupIntentReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.denbar.XMPP_Robot.StartupService");

		// start up the service that does registration
		context.startService(serviceIntent);
	}
}