package com.denbar.RobotComm;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Config;
import android.util.Log;

public class Orientation {

	private static final String TAG = "OrientationClass";
    private static final boolean DEBUG = true;
    private static final boolean localLOGV = DEBUG ? Config.LOGD : Config.LOGV;
    public static final String ORIENTATION_UNKNOWN = "-1";
    public String _yawString = ORIENTATION_UNKNOWN, _pitchString = ORIENTATION_UNKNOWN, _rollString = ORIENTATION_UNKNOWN;
    public float  _AccelX, _AccelY, _AccelZ;
    public float  _GyroX, _GyroY, _GyroZ;
    public float _GyroXintegral = 0, _GyroYintegral = 0, _GyroZintegral = 0;
    public String _AccelXstring = "0", _AccelYstring = "0", _AccelZstring = "0";
    public String _GyroXstring = "0", _GyroYstring = "0", _GyroZstring = "0";
    public String _GyroXintegralString, _GyroYintegralString, _GyroZintegralString;
    private SensorManager mSensorManager;
    private boolean mEnabled = false;
    private int mRate;
    private Sensor mLight, mRotation,  mLinearAccel, mGravity ,mGyro, mAccel, mMagnetic, mOrientation;


public Orientation(Context context) {
	        this(context, SensorManager.SENSOR_DELAY_NORMAL);
}

public Orientation(Context context, int rate)
{
    mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    mRate = rate;
    mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
   // mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    enable();
    List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
    for (Sensor sensor : sensors) {
        Log.d("Sensors", "" + sensor.getName());
    }
    //Log.d("Sensors", + sensors[0].getName());
}

	private SensorEventListener myMagenticEventListener = new SensorEventListener()
	{
		public void onSensorChanged(SensorEvent event)
		{
			
		}
		
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};	
	

	private SensorEventListener myAccelEventListener = new SensorEventListener() 
	{	  
		public void onSensorChanged(SensorEvent event) {
	        _AccelX = event.values[0];
	        _AccelY = event.values[1];
	        _AccelZ = event.values[2];
	        
	        int pitch = Angle(_AccelX,_AccelY,_AccelZ,0);
	        int roll = Angle(_AccelX,_AccelY,_AccelZ,1);
	        int yaw = Angle(_AccelX,_AccelY,_AccelZ,2);
	        
	        _AccelXstring = String.valueOf(_AccelX);
	        _AccelYstring = String.valueOf(_AccelY);
	        _AccelZstring = String.valueOf(_AccelZ);
	        
	        _pitchString = String.valueOf(pitch); 	// X-- this corresponds to robot tilt
	        _rollString = String.valueOf(roll);		// Y
	        _yawString =  String.valueOf(yaw);		// Z
		}
		
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
	private SensorEventListener myGyroEventListener = new SensorEventListener() 
	{	  
		public void onSensorChanged(SensorEvent event) {
	        _GyroX = event.values[0];
	        _GyroY = event.values[1];
	        _GyroZ = event.values[2];
	     
	        _GyroXintegral += _GyroX;
	        _GyroYintegral += _GyroY;
	        _GyroZintegral += _GyroZ;
	       // int pitch = Angle(_GyroX,_GyroY,_GyroZ,0);
	       // int roll = Angle(_GyroX,_GyroY,_GyroZ,1);
	       // int yaw = Angle(_GyroX,_GyroY,_GyroZ,2);
	        
	        _GyroXstring = String.valueOf(_GyroX);
	        _GyroYstring = String.valueOf(_GyroY);
	        _GyroZstring = String.valueOf(_GyroZ);
	        
	        _GyroXintegralString = String.valueOf(_GyroXintegral); 	// X
	        _GyroYintegralString = String.valueOf(_GyroYintegral);		// Y
	        _GyroZintegralString =  String.valueOf(_GyroZintegral);		// Z
		}
		
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};


private int Angle(float X, float Y, float Z, int axis)
{
	float magnitude = 0, angle = 0, OneEightyOverPi = 57.29577957855f;
	if (axis == 0)	// X
	{
		magnitude = X*X + Y*Y;
		// Don't trust the angle if the magnitude is small compared to the z value
		if (magnitude * 4 >= Z*Z) angle = (float)Math.atan2(-Y, X) * OneEightyOverPi;
	}
	else if (axis == 1) 
	{
		magnitude = X*X + Z*Z;
		// Don't trust the angle if the magnitude is small compared to the y value
		if (magnitude * 4 >= Y*Y) angle = (float)Math.atan2(-Z, X) * OneEightyOverPi;	
	}
	else
	{
		magnitude = Z*Z + Y*Y;
		// Don't trust the angle if the magnitude is small compared to the x value
		if (magnitude * 4 >= X*X) angle = (float)Math.atan2(-Y, Z) * OneEightyOverPi;
	}
		
	int orientation = 90 - (int)Math.round(angle);
	// normalize to 0 - 359 range
	orientation %= 360;
	if (orientation < 0) orientation += 360;
	return orientation;
}


	 // Enables the EventListeners so they will monitor the sensors 
	public boolean enable() {
	    if (mAccel == null || mGyro == null) {
	       Log.d(TAG, "Cannot detect sensors. Not enabled");
	       mEnabled = false;
	       return false;
	    }
	    if (mEnabled == false) {
	        if (localLOGV) Log.d(TAG, "EventListeners enabled");
	       // mSensorManager.registerListener(myAccelEventListener, mAccel, mRate);
	      //  mSensorManager.registerListener(myGyroEventListener, mGyro, mRate);
	        mEnabled = true;
	        return true;
	    }
	    return true;	// already enabled
	}


	// Disables the EventListeners
	public void disable() {
	    if (mAccel == null) {
	        Log.d(TAG, "Cannot detect sensors. Invalid disable");
	        return;
	    }
	    if (mEnabled == true) {
	        if (localLOGV) Log.d(TAG, "AccelEventListener disabled");
	       // mSensorManager.unregisterListener(myAccelEventListener);
	       // mSensorManager.unregisterListener(myGyroEventListener);
	        mEnabled = false;
	    }
	}
	
	
	//Returns true if sensor is enabled and false otherwise
	public boolean canDetectAccel() {
	    return mAccel != null;
	}
	
	public void resetIntegrals()
	{
		_GyroXintegral = 0;
		_GyroYintegral = 0;
		_GyroZintegral = 0;
	}
}
