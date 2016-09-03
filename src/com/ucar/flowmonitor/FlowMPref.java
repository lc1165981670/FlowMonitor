package com.ucar.flowmonitor;


import android.content.Context;
import android.content.SharedPreferences;

public class FlowMPref {
	
	public static final String TAG = "FlowMPref";
	public static final String PREFS_FLOW  = "flow";
	
	public static void setTime(Context context, String date) {
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		
		SharedPreferences.Editor edit = sp.edit();
		edit.putString("time", date);
		edit.commit();
	}
	
	public static String getTime(Context context) {
		String date=null;
		
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		date=sp.getString("time", null);
		
		return date;
	}	
	
	public static void setMonthTotialFlow(Context context, long baseFlow) {
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		
		SharedPreferences.Editor edit = sp.edit();
		edit.putLong("monthTotialFlow", baseFlow);
		
		edit.commit();
	}
	
	public static long getMonthTotialFlow(Context context) {
		long baseFlow;
		
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		baseFlow=sp.getLong("monthTotialFlow", 0);
		
		return baseFlow;
	}	
	
	public static void setWifiAPFlow(Context context, long baseFlow) {
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		
		SharedPreferences.Editor edit = sp.edit();
		edit.putLong("wifiAPFlow", baseFlow);
		
		edit.commit();
	}
	
	public static long getWifiAPFlow(Context context) {
		long baseFlow;
		
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		baseFlow=sp.getLong("wifiAPFlow", 0);
		
		return baseFlow;
	}	
	
	public static void setAppFlow(Context context, long baseFlow) {
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		
		SharedPreferences.Editor edit = sp.edit();
		edit.putLong("appFlow", baseFlow);
		
		edit.commit();
	}
	
	public static long getAppFlow(Context context) {
		long baseFlow;
		
		SharedPreferences sp = context.getSharedPreferences(PREFS_FLOW, 0);
		baseFlow=sp.getLong("appFlow", 0);
		
		return baseFlow;
	}	
}
