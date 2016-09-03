package com.ucar.flowmonitor;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OpenStartReceiver extends BroadcastReceiver {
	private static final String DEMO_ACTION="com.demo.action";
	public static final String WIFI_AP_INFO_CHANGED_ACTION  = "com.chuanlian.action.WIFI_AP_INFO";
	public static String wifyAPName="T3-WIFI";
	public static String wifyAPPassward="12345678";
	@SuppressLint("NewApi")
	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.d("onReceive action="+intent.getAction());
		if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())||DEMO_ACTION.equals(intent.getAction())){
			Logger.d("startService");
			Intent intent1=new Intent(context, FlowService.class);
			context.startService(intent1);
		}else if(WIFI_AP_INFO_CHANGED_ACTION.equals(intent.getAction())){
			String name=intent.getStringExtra("name");
			String pwd=intent.getStringExtra("pwd");
			if(name!=null&&!name.isEmpty()){
				wifyAPName=name;
			}
			if(pwd!=null&&!pwd.isEmpty()){
				wifyAPPassward=pwd;
			}
			Logger.d("wifyAPName="+wifyAPName+"  pwd="+pwd);
		}
	}
}
