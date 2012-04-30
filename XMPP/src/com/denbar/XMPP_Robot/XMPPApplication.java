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
    private String _robotName, _host, _port, _service, _userid, _password, _recipient, _packetString;
    private boolean _packetFlag;
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

    public String getpacketString()
    {
    	return _packetString;
    }

    public void setpacketString(String value)
    {
    	_packetString = value;
    }

    public boolean getpacketFlag()
    {
    	return _packetFlag;
    }

    public void setpacketFlag(boolean value)
    {
    	_packetFlag = value;
    }

    @Override
    public final void onCreate() {
    	super.onCreate();
    	singleton = this;
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
