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

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Help extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.xml.help);
		TextView MyHelpView = (TextView) findViewById(R.id.HelpView);
		String HelpMessage = " Welcome to 9th Sense's Robot Communication"
				+ "\n\nDrive a robot from anywhere in the world" +

				"\n\nYou can set user preferences for"
				+ "\n 1) Your robot's bluetooth address"
				+ "\n 2) Your login information"
				+ "\n 3) If you want to auto connect "
				+ "\n 4) To display connection events";
		MyHelpView.setText(HelpMessage);
	}
}