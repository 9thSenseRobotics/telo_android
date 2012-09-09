package com.denbar.RobotComm;

//package java_xml_library;
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



public class MicroTime
// Reproduces PHP microtime() function, which gives two versions of response:
// 1. microtime(), which I don't use
// 2. microtime(true) returns a string version of the epoch time in seconds and
// fractions of second,
// so I just called this smicrotime() below.
{

	public String smicrotime()
	// return a string with the millisecond time. It's millisecond time, not
	// microtime. microtime()
	// is what PHP calls it, and I am emulating a PHP function because the
	// protocol was originally
	// implemented in PHP, so, yeah, here we are.
	{
		return String.valueOf(System.currentTimeMillis()); // get the current epoch time in milliseconds
		/*int length = fullTime.length();
		// assuming here that length > 2, otherwise we will get a Number
		String millis = fullTime.substring(length - 2);
		String seconds = fullTime.substring(0,length - 2);
		return seconds + "." + millis;
		*/
	}
}