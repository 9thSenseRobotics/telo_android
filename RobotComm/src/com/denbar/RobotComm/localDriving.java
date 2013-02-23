package com.denbar.RobotComm;

//import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class localDriving extends Activity {
	
	private static final String TAG = "localDriving";
	private Context _context;
	private String _command;
	EditText _editSpeed, _editAcceleration, _editDegrees;
	
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.d(TAG, "in onCreate");
		//Toast.makeText(this, "RobotComm activity created", Toast.LENGTH_SHORT).show();
	
		setContentView(R.layout.driving);
		_context = this;
		
		_editSpeed = (EditText) findViewById(R.id.Speed);
		//_editAcceleration = (EditText) findViewById(R.id.Acceleration);
		_editDegrees = (EditText) findViewById(R.id.Degrees);
	

		// Set a button listener to move forward
		ImageButton btnMoveForward = (ImageButton) this.findViewById(R.id.move_forward);
		btnMoveForward.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				_command = "F" + _editSpeed.getText();
				sendCommand();
			}
		});
	
		// Set a button listener to move backward
		ImageButton btnMoveBackward = (ImageButton) this.findViewById(R.id.move_backward);
		btnMoveBackward.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				_command = "B" + _editSpeed.getText().toString();
				sendCommand();
			}
		});
		
		// Set a button listener to turn left
		ImageButton btnTurnLeft = (ImageButton) this.findViewById(R.id.turn_left);
		btnTurnLeft.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				_command = "l" + _editSpeed.getText() + "," +_editDegrees.getText();
				sendCommand();
			}
		});
			
		// Set a button listener to turn right
		ImageButton btnTurnRight = (ImageButton) this.findViewById(R.id.turn_right);
		btnTurnRight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				_command = "r" + _editSpeed.getText() + "," + _editDegrees.getText();
				sendCommand();
			}
		});
		
		// Set a button listener to tilt up
		ImageButton btnTiltUp = (ImageButton) this.findViewById(R.id.tilt_up);
		btnTiltUp.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				_command = "u";
				sendCommand();
			}
		});		
		
		// Set a button listener to tilt down
		ImageButton btnTiltDown = (ImageButton) this.findViewById(R.id.tilt_down);
		btnTiltDown.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				_command = "n";
				sendCommand();
			}
		});			
		
		// Set a button listener to stop
		ImageButton btnStop = (ImageButton) this.findViewById(R.id.stop);
		btnStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				_command = "x";
				sendCommand();
			}
		});		
	
		// Set a button listener to return
		Button btnReturn = (Button) this.findViewById(R.id.Return);
		btnReturn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();
			}
		});	
	
	} // ends onCreate
	
	void sendCommand()
	{
		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.denbar.RobotComm.RobotCommService");
		serviceIntent.putExtra("messageToRobot", _command);
		_context.startService(serviceIntent);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "in onResume");
		// get the strings values for speed and degrees from when the window was last closed
		Resources robotResources = getResources();
		SharedPreferences prefs = getSharedPreferences("RobotPreferences", MODE_WORLD_WRITEABLE);
		_editSpeed.setText(prefs.getString("speed", robotResources.getString(R.string.default_speed)));
		//_editAcceleration.setText(prefs.getString("acceleration", robotResources.getString(R.string.acceleration)));
		_editDegrees.setText(prefs.getString("degrees", robotResources.getString(R.string.degrees)));
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "in onStop");
		SharedPreferences newprefs = getSharedPreferences(
				"RobotPreferences", MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = newprefs.edit(); // store the values for speed and degrees
		editor.putString("speed", _editSpeed.getText().toString());	
		//editor.putInt("acceleration", _accel);
		editor.putString("degrees", _editDegrees.getText().toString());
		editor.commit();
	}
}
