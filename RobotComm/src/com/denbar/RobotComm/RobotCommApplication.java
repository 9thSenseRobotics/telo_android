package com.denbar.RobotComm;

import android.app.Application;

public class RobotCommApplication extends Application {

    private static RobotCommApplication singleton;

    // global variables
    //private String _robotName, _host, _port, _service, _userid, _password, _bluetooth, _recipient;
    private boolean _bluetoothConnected, _tabletBluetoothStatus;
    private String _bluetoothAddress;
    private int _bluetoothAttemptsCounter;

    public static RobotCommApplication getInstance()
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

    public void setTabletBluetoothStatus(boolean value)
    {
    	_tabletBluetoothStatus = value;
    }

    public boolean getTabletBluetoothStatus()
    {
    	return _tabletBluetoothStatus;
    }

}
