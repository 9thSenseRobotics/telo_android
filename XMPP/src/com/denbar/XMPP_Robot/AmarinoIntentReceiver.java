package com.denbar.XMPP_Robot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import at.abraxas.amarino.AmarinoIntent;

// we need a receiver to listen for amarino sending broadcasts
// with data from the arduino
public class AmarinoIntentReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		// check to see if this is data being sent from the arduino through bluetooth to amarino
		if ("amarino.intent.action.RECEIVED".equals(action)) {
			String data = null;

			// the device address from which the data was sent, we don't need it here but to demonstrate how you retrieve it
			//final String bluetoothAddress = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);

			// the type of data which is added to the intent
			final int dataType = intent.getIntExtra(AmarinoIntent.EXTRA_DATA_TYPE, -1);

			// we only expect String data though, but it is better to check if really string was sent
			// later Amarino will support different data types, so far data comes always as string and
			// you have to parse the data to the type you have sent from Arduino, like it is shown below
			if (dataType == AmarinoIntent.STRING_EXTRA){
				data = intent.getStringExtra(AmarinoIntent.EXTRA_DATA);

				if (data != null)	// send the data that came from arduino out to the XMPP server
				{
	        		Intent serviceIntent = new Intent();
	        		serviceIntent.setAction("com.denbar.XMPP_Robot.StartupService");
	        		serviceIntent.putExtra("message", data);
	        		context.startService(serviceIntent);
				}
			}
		}

		else if ("amarino.intent.action.ACTION_CONNECTED_DEVICES".equals(action))
		{
			//String[] result = new String[addresses.size()];
			//result = addresses.toArray(result);
			//int test = intent.getExtra(AmarinoIntent.EXTRA_CONNECTED_DEVICES);
			//String[] result = intent.getExtra(AmarinoIntent.EXTRA_CONNECTED_DEVICE_ADDRESSES);
		}

		else if ("amarino.intent.action.CONNECTED".equals(action))
		{
			Toast.makeText(context, "Bluetooth Connected", Toast.LENGTH_SHORT).show();
			XMPPApplication.getInstance().setBluetoothConnected(true);

		}

		else if ("amarino.intent.action.DISCONNECTED".equals(action))
		{
			Toast.makeText(context, "Bluetooth disonnected!!!", Toast.LENGTH_LONG).show();
			XMPPApplication.getInstance().setBluetoothConnected(false);
    		Intent serviceIntent = new Intent();
    		serviceIntent.setAction("com.denbar.XMPP_Robot.StartupService");
    		serviceIntent.putExtra("bluetooth", false);
    		//context.startService(serviceIntent);
		}

		else if ("amarino.intent.action.CONNECTION_FAILED".equals(action))
		{
			Toast.makeText(context, "Bluetooth connection failed!!!", Toast.LENGTH_LONG).show();
			XMPPApplication.getInstance().setBluetoothConnected(false);
    		Intent serviceIntent = new Intent();
    		serviceIntent.setAction("com.denbar.XMPP_Robot.StartupService");
    		serviceIntent.putExtra("bluetooth", false);
    		//context.startService(serviceIntent);

		}

	}
}
