package com.denbar.RobotComm;

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


		// amarino broadcasted an intent containing the devices it is connected to
		// this might be a response to our inquiry or to someone elses (or internal
		// request in amarino
		// we use this as a way to know we are still connected to our bluetooth
		// device and also that amarino is still working
		else if ("amarino.intent.action.ACTION_CONNECTED_DEVICES".equals(action))
		{
			String[] addresses = intent.getStringArrayExtra(AmarinoIntent.EXTRA_CONNECTED_DEVICE_ADDRESSES);
			if (addresses != null)
			{
				for (int i=0; i < addresses.length; i++)
				{
					if (addresses[i].equals(RobotCommApplication.getInstance().getBluetoothAddress()))
					{
						RobotCommApplication.getInstance().setBluetoothConnected(true); // also zeros the counter
						Toast.makeText(context, "Bluetooth Connected, counter zeroed", Toast.LENGTH_SHORT).show();
						return;
					}
				}
				Toast.makeText(context, "Bluetooth connected to device(s), but not ours", Toast.LENGTH_LONG).show();
			}

			else Toast.makeText(context, "Not connected to any bluetooth device", Toast.LENGTH_LONG).show();

			// we are not connected to our bluetooth device, so try to connect
			// if we have not already tried too much

			RobotCommApplication.getInstance().setBluetoothConnected(false);

			// send this to keep track of where we are
			String text = "IN ACTION_CONNECTED_DEVICES, attempt " + RobotCommApplication.getInstance().getBluetoothAttemptsCounter();
    		Intent serviceIntentTest = new Intent();
    		serviceIntentTest.setAction("com.denbar.XMPP_Robot.StartupService");
    		serviceIntentTest.putExtra("message", text);
    		//context.startService(serviceIntentTest);

			if (RobotCommApplication.getInstance().getBluetoothAttemptsCounter() < 5)
			{
				if (RobotCommApplication.getInstance().getBluetoothAttemptsCounter() > 2)
				{
					// things are getting desperate if we are here
					// try disconnecting
					Intent intent3 = new Intent(AmarinoIntent.ACTION_DISCONNECT);
					intent3.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
					//context.sendBroadcast(intent3);

					// give it a little time to work, 5 seconds
					try {Thread.sleep(10000);
			        } catch (InterruptedException e) {
			            e.printStackTrace();
			        }
				}

				Intent intent1 = new Intent(AmarinoIntent.ACTION_CONNECT);
				intent1.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
				//intent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, "00:06:66:46:5A:91");
				//context.sendBroadcast(intent1);

				// give it a little time to work, 5 seconds
				try {Thread.sleep(10000);
		        } catch (InterruptedException e) {
		            e.printStackTrace();
		        }

		        //ask again
				Intent intent2 = new Intent(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
				intent2.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
				//context.sendBroadcast(intent2);
				// give it a little time to work, 5 seconds
				try {Thread.sleep(10000);
		        } catch (InterruptedException e) {
		            e.printStackTrace();
		        }

				// increment the counter, so we don't ask too many times
				RobotCommApplication.getInstance().incrementBluetoothAttemptsCounter();
			}
			else	// time to give up, something is seriously wrong (bluetooth hardware stopped?).
			{
				RobotCommApplication.getInstance().setBluetoothConnected(false);
				// we could send an intent to the service, but
				// there is not much to do about it, so we don't
				// note that it is better to send a string rather than a boolean
				// so that the service can check for null and determine if an intent
				// was sent from here, whereas the boolean has to have a default value
				//Intent serviceIntent = new Intent();
				//serviceIntent.setAction("com.denbar.XMPP_Robot.StartupService");
				//serviceIntent.putExtra("bluetooth", "unconnected");
				//context.startService(serviceIntent);
			}
		}

		else if ("amarino.intent.action.CONNECTED".equals(action))
		{
			// we need to be sure that the connection is to our bluetooth device
			String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			if (address.equals(RobotCommApplication.getInstance().getBluetoothAddress()))
			{
				RobotCommApplication.getInstance().setBluetoothConnected(true); // also zeros the counter
				Toast.makeText(context, "Bluetooth connected to correct device", Toast.LENGTH_SHORT).show();
			}
			// this broadcast was not about our bluetooth device
			// only take an action if it is not already in work
			else if (RobotCommApplication.getInstance().getBluetoothAttemptsCounter() == 0)
			{
				Toast.makeText(context, "Bluetooth connected to some other device", Toast.LENGTH_SHORT).show();
				RobotCommApplication.getInstance().setBluetoothConnected(false);
				Intent intent1 = new Intent(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
				intent1.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
			}
		}

		else if ("amarino.intent.action.DISCONNECTED".equals(action))
		{
			// we need to be sure that the disconnection is to our bluetooth device
			String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			if (!address.equals(RobotCommApplication.getInstance().getBluetoothAddress()))
			{
				Toast.makeText(context, "Bluetooth disconnected from some other bluetooth device", Toast.LENGTH_SHORT).show();
			}
			// we are not connected to our bluetooth device
			// only take an action if it is not already in work
			else if (RobotCommApplication.getInstance().getBluetoothAttemptsCounter() == 0)
			{
				Intent intent1 = new Intent(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
				intent1.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
				//context.sendBroadcast(intent1);
				RobotCommApplication.getInstance().setBluetoothConnected(false);
				Toast.makeText(context, "Bluetooth disconnected from our device", Toast.LENGTH_SHORT).show();
			}
		}

		else if ("amarino.intent.action.CONNECTION_FAILED".equals(action))
		{
			// we need to be sure that the disconnection is to our bluetooth device
			String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			if (!address.equals(RobotCommApplication.getInstance().getBluetoothAddress()))
			{
				Toast.makeText(context, "Bluetooth failed to connect to some other bluetooth device", Toast.LENGTH_SHORT).show();
			}
			// we are not connected to our bluetooth device
			// only take an action if it is not already in work
			else if (RobotCommApplication.getInstance().getBluetoothAttemptsCounter() == 0)
			{
				Intent intent1 = new Intent(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
				intent1.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
				//context.sendBroadcast(intent1);
				RobotCommApplication.getInstance().setBluetoothConnected(false);
				Toast.makeText(context, "Bluetooth failed to connect to our device", Toast.LENGTH_SHORT).show();
			}
		}
		else if ("amarino.intent.action.COMMUNICATIONS_HALTED".equals(action))
		{
			// this is the signature of a momentary bluetooth power fail
			// this is my addition to the original Amarino_2 code
			//added a function and a call to say when communications are halted.
			//This is needed because if the bluetooth has a momentary power fail,
			//it disconnects but the amarino program thinks it is still connected.
			//But it knows it is not communicating.
			//So it responds to standard inquiries with
			//amarino.intent.action.ACTION_CONNECTED_DEVICES
			//indicating that the device is connected, when it really is not.
			//So we need to pick up this state and send a disconnect/connect combination to get reconnected.
			//
			// we need to be sure that the comm halt is to our bluetooth device
			String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			if (!address.equals(RobotCommApplication.getInstance().getBluetoothAddress()))
			{
				Toast.makeText(context, "Bluetooth comm halted to some other bluetooth device", Toast.LENGTH_SHORT).show();
			}
			// our bluetooth device probably had a momentary power fail
			// first we need to tell amarino to disconnect

			// limit the attempts, so we don't burn up trying
			if (RobotCommApplication.getInstance().getBluetoothAttemptsCounter() < 5)
			{
				Intent intent5 = new Intent(AmarinoIntent.ACTION_DISCONNECT);
				intent5.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
				//context.sendBroadcast(intent5);

				// give it a little time to work, 5 seconds
				try {Thread.sleep(5000);
		        } catch (InterruptedException e) {
		            e.printStackTrace();
		        }

		        // and now go to our standard attempt to reconnect:
				Intent intent1 = new Intent(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
				intent1.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, RobotCommApplication.getInstance().getBluetoothAddress());
				//context.sendBroadcast(intent1);
				RobotCommApplication.getInstance().setBluetoothConnected(false);
				Toast.makeText(context, "Bluetooth comm halted to our device", Toast.LENGTH_SHORT).show();
			}
		}
	}	// closes onReceive
}
