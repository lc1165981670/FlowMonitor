package com.ucar.flowmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSspeed {
	private LocationManager mLocationManager;
	private float mSpeed=0;

	public GPSspeed(Context context) {
		Logger.d("GPSspeed构造方法");
		// 得到LocationManager对象
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		setCriteria();
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0, 0, new MyLocationListener());
	}

	@SuppressLint("NewApi")
	private Criteria setCriteria() {
		Logger.d("setCriteria");
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE); 			// 设置为最大精度
		criteria.setAltitudeRequired(true); 					// 要求海拔信息
		criteria.setBearingRequired(true); 						// 要求方位信息
		criteria.setBearingAccuracy(Criteria.ACCURACY_HIGH); 	// 要求方位信息 的精确度
		criteria.setCostAllowed(false); 						// 是否允许付费
		criteria.setPowerRequirement(Criteria.POWER_LOW); 		// 对电量的要求
		criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);	 	// 对速度的精确度
		criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH); // 对水平的精确度
		criteria.setSpeedRequired(true); 						// 要求速度信息
		criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH); 	// 对垂直精度
		mLocationManager.getBestProvider(criteria, true);		// 找到最好的能用的Provider。
		return criteria;
	}

	private class MyLocationListener implements LocationListener {

		@Override
		public void onProviderDisabled(String arg0) {
		}

		@Override
		public void onProviderEnabled(String arg0) {
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}

		@Override
		public void onLocationChanged(Location location) {
			// 设备位置发生改变Location为对象的位置
			if(location!=null){
				mSpeed=location.getSpeed();	
			}
			Logger.d("mSpeed="+mSpeed);
		}
	}
	
	public float getSpeed(){
		return mSpeed;
	}
}
