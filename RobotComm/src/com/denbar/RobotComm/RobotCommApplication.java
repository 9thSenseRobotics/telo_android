package com.denbar.RobotComm;

import android.app.Application;
import android.content.res.Configuration;


public class RobotCommApplication extends Application {

    private static RobotCommApplication singleton;

    // global variables
    private boolean _bluetoothConnected, _XMPPconnected, _googleCloudConnected, _displayDetails;
    private String _bluetoothAddress;
    private String _bluetoothStatus, _XMPPstatus, _googleCloudStatus;


    public static RobotCommApplication getInstance()
    {
    		return singleton;
    }

    // setup global variables here
    // these methods can be called from any of the routines.
    // For example,
    // int valuetest = 10;
    // RobotCommApplication.getInstance().setGlobalStateValue(valuetest);
    // or
    // int valuetest = RobotCommApplication.getInstance().getGlobalStateValue();
    
    public boolean getDisplayDetails()
    {
    	return _displayDetails;
    }
    
    public void setDisplayDetails(boolean value)
    {
    	_displayDetails = value;
    }

    public void setBluetoothAddress(String value)
    {
    	_bluetoothAddress = value;
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
    }
    
    public String getBluetoothStatus()
    {
    	return _bluetoothStatus;
    }
    
    public void setBluetoothStatus(String value)
    {
    	_bluetoothStatus = value;
    }

    public boolean getXMPPconnected()
    {
    	return _XMPPconnected;
    }

    public void setXMPPconnected(boolean value)
    {
    	_XMPPconnected = value;
    }
    
    public String getXMPPstatus()
    {
    	return _XMPPstatus;
    }
    
    public void setXMPPstatus(String value)
    {
    	_XMPPstatus = value;
    }
    
    public boolean getGoogleCloudConnected()
    {
    	return _googleCloudConnected;
    }

    public void setGoogleCloudConnected(boolean value)
    {
    	_googleCloudConnected = value;
    }
    
    public String getGoogleCloudStatus()
    {
    	return _googleCloudStatus;
    }
    
    public void setGoogleCloudStatus(String value)
    {
    	_googleCloudStatus = value;
    }
    
    @Override
    public final void onCreate() {
    	super.onCreate();
    	singleton = this;
    	_bluetoothAddress = "incorrect address";  // this needs to set by the program,
    		// otherwise every bot would needs its own compiled software version
    	_bluetoothConnected = false;
    	_bluetoothStatus = "Not connected yet";
    	_XMPPconnected = false;
    	_XMPPstatus = "Not connected yet";
    	_googleCloudConnected = false;
    	_googleCloudStatus = "Not connected yet";
    	_displayDetails = false;
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
