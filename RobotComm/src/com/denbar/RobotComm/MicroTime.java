package com.denbar.RobotComm;

//package java_xml_library;
import java.text.NumberFormat;

import java.text.DecimalFormat;

public class MicroTime
{
	public double microtime()
	{
		return (System.currentTimeMillis());
	}

	public String smicrotime ()
	{
		double fullTime = System.currentTimeMillis();
		double epochTime = fullTime / 1000;
		double microSecRemainder = fullTime - epochTime;

		NumberFormat f = new DecimalFormat("#0");
		return ("" + f.format(epochTime) + "." + f.format(microSecRemainder));
	}
}