/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denbar.XMPP_Robot;


import android.app.Application;
import android.content.res.Configuration;

public class XMPPApplication extends Application {

    private static XMPPApplication singleton;

    // global variables
    private String _robotName, _host, _port, _service, _userid, _password, _bluetooth, _recipient;
    private boolean _bluetoothConnected;
    private String _bluetoothAddress;
    private int _bluetoothAttemptsCounter;

    public static XMPPApplication getInstance()
    {
    		return singleton;
    }

    // setup global variables here
    // these methods can be called from any of the routines.
    // For example,
    // int valuetest = 10;
    // XMPPApplication.getInstance().setGlobalStateValue(valuetest);
    // or
    // int valuetest = XMPPApplication.getInstance().getGlobalStateValue();

    public void setBluetoothAddress(String value)
    {
    	_bluetoothAddress = value.toUpperCase();
    }

    public String getBluetoothAddress()
    {
    	return _bluetoothAddress;
    }

    public boolean getBluetoothConnected()
    {
    	return _bluetoothConnected;
    }

    public void setBluetoothConnected(boolean value)
    {
    	_bluetoothConnected = value;
    	if (value) _bluetoothAttemptsCounter = 0;
    }

    public int getBluetoothAttemptsCounter()
    {
    	return _bluetoothAttemptsCounter;
    }

    public void setBluetoothAttemptsCounter(int value)
    {
    	_bluetoothAttemptsCounter = value;
    }

    public void incrementBluetoothAttemptsCounter()
    {
    	_bluetoothAttemptsCounter++;
    }

    public void setGlobalStrings(String robotName, String host,
    		String port, String service, String userid, String password, String recipient)
    {
    	_robotName = robotName;
    	_host = host;
    	_port = port;
    	_service = service;
    	_userid = userid;
    	_password = password;
    	_recipient = recipient;
    }

    public String getrobotName()
    {
    	return _robotName;
    }

    public String gethost()
    {
    	return _host;
    }

    public String getservice()
    {
    	return _service;
    }

    public String getuserid()
    {
    	return _userid;
    }
    public String getpassword()
    {
    	return _password;
    }

    public String getrecipient()
    {
    	return _recipient;
    }

    @Override
    public final void onCreate() {
    	super.onCreate();
    	singleton = this;
    	_bluetoothAddress = "incorrect address";  // this needs to set by the program,
		// otherwise every bot would needs its own compiled software version
    	_bluetoothConnected = false;
    }

    // other life cycle events are included here just as a reminder of what overrides are available
    @Override
    public final void onTerminate() {
    	super.onTerminate();
    }

    @Override
    public final void onLowMemory() {
    	super.onLowMemory();
    }

    @Override
    public final void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);
    }
}
