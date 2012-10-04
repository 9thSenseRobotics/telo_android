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


import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateWidgetService extends Service {
	private static final String LOG = "UpdatewidgetService";
	private String _XMPPstatus, _C2DMstatus, _bluetoothStatus, _sentToRobot, _sentToServer;
	private AppWidgetManager appWidgetManager;
	private RemoteViews remoteViews;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG, "in onCreate");
		appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
		remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.widget_layout);

	}

	@Override
	public void onStart(Intent intent, int startId) {

		ComponentName thisWidget = new ComponentName(getApplicationContext(),
				RobotCommWidget.class);
		int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);

		//_XMPPstatus = intent.getStringExtra("XMPP");
		//_C2DMstatus = intent.getStringExtra("C2DM");
		//_bluetoothStatus = intent.getStringExtra("bluetooth");
		//_sentToServer = intent.getStringExtra("sentToServer");
		//_sentToRobot = intent.getStringExtra("sentToRobot");

		for (int widgetId : allWidgetIds2) {
			_bluetoothStatus = "Robot status: Bluetooth " + RobotCommApplication.getInstance().getBluetoothStatus();
			_XMPPstatus = "Remote Server status: " + RobotCommApplication.getInstance().getXMPPstatus();
			//if (_XMPPstatus != null)
				remoteViews.setTextViewText(R.id.XMPP, _XMPPstatus);
			//if (_C2DMstatus != null)
				//remoteViews.setTextViewText(R.id.C2DM, _C2DMstatus);
			//if (_bluetoothStatus != null)
				remoteViews.setTextViewText(R.id.bluetooth, _bluetoothStatus);
			//if (_sentToServer != null)
				//remoteViews.setTextViewText(R.id.sentToServer, _sentToServer);
			//if (_sentToRobot != null)
				//remoteViews.setTextViewText(R.id.sentToRobot, _sentToRobot);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);

			/*
			 * // Register an onClickListener. to use this, set up a broadcast
			 * listener in RobotCommService Intent clickIntent = new
			 * Intent(this.getApplicationContext(), RobotCommService.class);
			 * //Intent clickIntent = new Intent();
			 * clickIntent.setAction("com.denbar.RobotComm.UpdateWidgetService.OPEN"
			 * ); //clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			 * clickIntent.putExtra("widgetCalling","showActivity");
			 *
			 * PendingIntent pendingIntent = PendingIntent.getBroadcast(
			 * getApplicationContext(), 0, clickIntent,
			 * PendingIntent.FLAG_UPDATE_CURRENT);
			 * remoteViews.setOnClickPendingIntent(R.id.btnOpen, pendingIntent);
			 * appWidgetManager.updateAppWidget(widgetId, remoteViews);
			 */
		}
		stopSelf();
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
