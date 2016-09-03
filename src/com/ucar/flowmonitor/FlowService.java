package com.ucar.flowmonitor;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

public class FlowService extends Service {
	/**
	 * 视频APP每天可使用的最大流量
	 */
	private static final long APP_DAY_MAX_FLOW = 50*1024*1024L;//1024*1024*1024
	/**
	 * WIFI热点每天使用流量达到该值关闭热点
	 */
	private static final long WIFIAP_DAY_MAX_FLOW=15*1024*1024L;    //500*1024*1024 
	/**
	 * 一个月总流量达到该值后，每天wifi热点达到上限后关闭热点
	 */
	private static final long WIFIAP_MONTH_MAX_FLOW=100*1024*1024L; //4*1024*1024*1024
	/**
	 * 视频APP包名
	 */
	private static final String PACKAGENAME_OF_VIDEO="com.qiyi.video.pad";
	
	private static final int PORT=1819;
	private static final String IP="localhost";
	
	private static final String NETWORK_URL="http://www.baidu.com";
	private static final int CYCLE_JUDGEMENT = 0;
//	private static final int PREENT_APP_NETWORK = 1;
//	private static final int CTRL_FLOW_SPEED_3G =2;
	private static final int CTRL_FLOW_SPEED_WIFIAP =3;
	private static final int CLEAN_ALL_RULES=4;
	private String privTime;
	/**
	 * wifi热点使用的总流量
	 */
	private long dayWifiAPToaial; 
	/**
	 * wifi热点上次流量计数
	 */
	private long privWifiAPFlow;
	/**
	 * wifi热点当前流量计数
	 */
	private long currentWifiAPFlow;
	
	/**
	 * 本机一个月使用的总流量计数
	 */
	private long monthTotialFlow;
	/**
	 * 上次总流量计数
	 */
	private long privTotialFlow;
	/**
	 * 本次总流量计数
	 */
	private long currentTotialFlow;
	
	/**
	 * 视频APP使用的总流量计数
	 */
	private long appTotialFlow;
	/**
	 * 视频APP上次总流量计数
	 */
	private long privAppFlow;
	/**
	 * 视频APP本次总流量计数
	 */
	private long currentAppFlow;
	
	private String currentTime;
	private SimpleDateFormat mFormat;
	private ConnectivityManager mConManager;
	public static WifiAPHandler mWifiAPHandler;
	
	private GPSspeed mGPSspeed;
	
	private boolean isSendSocket=true;   //判断是否对视频APP禁过网
	private boolean isNeedSendClean=true;   //判断是否需要清除所有规则，只和是否发送过清除有关
	private boolean isSend3G=true;       //判断是否3G限速
	
	private boolean isApplyRules=true;   //是否启用规则判断
//	private boolean isApplyRules=true;   //是否启用规则判断
	private int ipSize=0; 
	private Timer mTimer;
	private Date mDate;
	
	public enum WIFI_AP_STATE {
		WIFI_AP_STATE_DISABLING, 
		WIFI_AP_STATE_DISABLED, 
		WIFI_AP_STATE_ENABLING, 
		WIFI_AP_STATE_ENABLED, 
		WIFI_AP_STATE_FAILED
		}
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Logger.d("onCreate");
		mGPSspeed=new GPSspeed(FlowService.this);
		HandlerThread flowThread=new HandlerThread("wifiAPflow");
		flowThread.start();
		mWifiAPHandler=new WifiAPHandler(flowThread.getLooper());
		initData();
		mWifiAPHandler.sendEmptyMessage(CYCLE_JUDGEMENT);
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logger.d("onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void initData() {
		Logger.d("initData");
		privTime = FlowMPref.getTime(this);
		dayWifiAPToaial = FlowMPref.getWifiAPFlow(this);
		monthTotialFlow=FlowMPref.getMonthTotialFlow(this);
		appTotialFlow=FlowMPref.getAppFlow(this);
		
		Logger.d("initData  privTime="+privTime+"  dayWifiAPToaial="+dayWifiAPToaial+
				"  dayWifiAPToaial="+dayWifiAPToaial+"  appTotialFlow="+appTotialFlow);
		privTotialFlow =0;
		privAppFlow=0;
		privWifiAPFlow=0;
		
		currentWifiAPFlow = 0;
		currentAppFlow=0;
		currentTotialFlow=0;
		
		mFormat = new SimpleDateFormat("yyyyMMdd");
		mConManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	/**
	 * 判断是不是同一个月       27号到下个月26号
	 * @throws Exception 
	 */
	private boolean isSameMonth(){
		boolean isSameMonth=false;
		currentTime=mFormat.format(mDate);
		int currentMonth=mDate.getMonth()+1;
		int currentDay=mDate.getDay();
		if(privTime==null){
			return false;
		}
		int privMonth=Integer.parseInt(privTime.substring(4, 6));
		int privDay=Integer.parseInt(privTime.substring(6));
		Logger.d("isSameMonth currentMonth="+currentMonth+" currentDay="+currentDay+
				" privMonth="+privMonth+" privDay="+privDay);
		if(privDay>=27){
			if(currentMonth==privMonth){
				if(currentDay>=27){
					isSameMonth=true;
				}else{
					isSameMonth=false;
				}
			}else if(currentMonth==privMonth+1){
				if(currentDay>=27){
					isSameMonth=false;
				}else{
					isSameMonth=true;
				}
			}
		}else{
			if(currentMonth==privMonth){
				if(currentDay>=27){
					isSameMonth=false;
				}else{
					isSameMonth=true;
				}
			}else if(currentMonth==privMonth+1){
				isSameMonth=false;
			}
		}
		return isSameMonth;
	}
	
	/**
	 * 判断是不是同一天
	 * @throws Exception 
	 */
	private boolean isSameDay(){
		boolean isSameDay=false;
		currentTime=mFormat.format(mDate);
		if(currentTime.equals(privTime)){
			isSameDay=true;
		}else{
			isSameDay=false;
		}
		return isSameDay;
	}
	
	/**
	 * 判断网络状态及类型是否可用
	 * @return
	 */
	private boolean judgeNetwork(){
		boolean flag = false;
		NetworkInfo info=mConManager.getActiveNetworkInfo();
		if (info!= null) {                          //判断是否有网络连接
			if(info.isAvailable()){					//连接是否可用
				if(info.getType() == ConnectivityManager.TYPE_WIFI){      //判断是wifi还是手机数据流量
					if(isWifiApEnabled()){   //如果wifi热点可用则不是在使用wifi上网
						flag=true;
					}else{
						flag=false;
						Logger.d("CLEAN_ALL_RULES");
						mWifiAPHandler.sendEmptyMessage(CLEAN_ALL_RULES);        //如果是wifi联网则清除所有过滤规则
					}
				}else{
					flag=true;	
				}
			}else{
				flag=false;	
			}
		}
		Logger.d("judgeNetwork flag="+flag);
		return flag;
	}
	/**
	 * 获取网络时间
	 * @return
	 * @throws Exception
	 */
	private Date getNetworkTime(){
		if(!judgeNetwork()){
			return null;
		}
		URLConnection uc=null;
		Date date=null;
		try {
			URL urlTime = new URL(NETWORK_URL);
			uc = urlTime.openConnection();
			uc.connect();
			long ld = uc.getDate(); 
			date = new Date(ld);
		} catch (Exception e) {
			Logger.d( "getNetworkTime e="+e.toString());
			e.printStackTrace();
		} 
		return date;
	}
	
	/**
	 * 获取wifi热点当前状态
	 */
	private WIFI_AP_STATE getWifiAPState() {
		int tmp;
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		try {
			Method method = wifi.getClass().getMethod("getWifiApState");
			tmp = ((Integer) method.invoke(wifi));
			Logger.d( "getWifiAPState tmp="+tmp);
			// Fix for Android 4
			if (tmp >= 10) {
				tmp = tmp - 10;
			}
			return WIFI_AP_STATE.class.getEnumConstants()[tmp];
		} catch (Exception e) {
			Logger.d( "getWifiAPState e="+e.toString());
			e.printStackTrace();
			return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
		}
	}
	
	/**
	 * 设置wifi热点     open or close
	 * @param enable
	 */
	private void setWifiAP(boolean enable){
		Logger.d( "setWifiAP");
		try {
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if(enable){
				wifi.setWifiEnabled(false);	
			}
			 WifiConfiguration apConfig = new WifiConfiguration(); 
			 apConfig.SSID = OpenStartReceiver.wifyAPName;  
		     apConfig.preSharedKey= OpenStartReceiver.wifyAPPassward;
		     apConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		     apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		     apConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		     apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		     apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		     apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		     apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		     apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		     
			Method method1 = wifi.getClass().getMethod("setWifiApEnabled",WifiConfiguration.class,Boolean.TYPE);
			method1.invoke(wifi, apConfig, enable);
		} catch (Exception e) {
			Logger.d( "setWifiAP e="+e.toString());
			e.printStackTrace();
		}
	}
	/**
	 * 获取wifi产生的流量
	 */
	private long getWifiFlow(){
		long wifiFlow=(TrafficStats.getTotalRxBytes()+TrafficStats.getTotalTxBytes())-
				TrafficStats.getMobileRxBytes()+ TrafficStats.getMobileTxBytes();
		return wifiFlow;
	}
	/**
	 * 根据月流量总和判断是否关闭wifi热点，根据视频APP一天产生的流量判断是否对视频APP禁网
	 * @throws Exception
	 */
	private void judgeWifiAPAble(){
		Logger.d( "judgeWifiAPAble");
		mDate=getNetworkTime();
		if(mDate==null){               //是3G网络，且网络时间获取正确
			Logger.d( "getNetworkTime()==null");
			return;
		}
		//判断车是否在移动，如果移动则解除所有限制，如果停下来的时间超过4min则启用限制
		Logger.d( "mGPSspeed="+mGPSspeed.getSpeed());
		
		if(mGPSspeed.getSpeed()<1.00){
			isNeedSendClean=true;
			if(mTimer==null){
				mTimer=new Timer();
				TimerTask	mTimeTask=new TimerTask() {
						@Override
						public void run() {
							if(!isApplyRules){
								isApplyRules=true;
							}
						}
					};
				mTimer.schedule(mTimeTask, 4*60*1000);
			}
		}else{
			if(mTimer!=null){
				mTimer.cancel();
				mTimer.purge();
				mTimer=null;
			}
			if(!isWifiApEnabled()){
				setWifiAP(true);
			}
			mWifiAPHandler.sendEmptyMessage(CLEAN_ALL_RULES);      //清除所有规则并将isApplyRules置为false
		}
		Logger.d( "isApplyRules="+isApplyRules);
		
		if(isSend3G){                       //判断是否要对3G网络限速     
//			socketSend(Api.getCleanAllRules());
			socketSend(Api.getScript(true,null));
			isSend3G=false;
		}
		
		boolean isEnable=isWifiApEnabled();
		Logger.d("isWifiApEnabled="+isEnable);
		if(privTotialFlow==0){
			privTotialFlow=TrafficStats.getMobileRxBytes()+ TrafficStats.getMobileTxBytes();
		}
		currentTotialFlow =TrafficStats.getMobileRxBytes()+ TrafficStats.getMobileTxBytes();
		
		if(privAppFlow==0){
			privAppFlow=TrafficStats.getUidRxBytes(getVideoUid())+TrafficStats.getUidTxBytes(getVideoUid());
		}
		currentAppFlow=TrafficStats.getUidRxBytes(getVideoUid())+TrafficStats.getUidTxBytes(getVideoUid());
		
		if(isEnable){
//			socketSend(Api.getCleanAllRules());
			mWifiAPHandler.sendEmptyMessage(CTRL_FLOW_SPEED_WIFIAP);     //如果wifi热点可用则限速
			if(privWifiAPFlow==0){
				privWifiAPFlow=getWifiFlow();
			}
			currentWifiAPFlow = getWifiFlow();
		}
		
		Logger.d( "judgeWifiAPAble  privTime="+privTime+"  currentTotialFlow="+currentTotialFlow+
				"  currentAppFlow="+currentAppFlow+"  currentWifiAPFlow="+currentWifiAPFlow);
		Logger.d( "judgeWifiAPAble  privTime="+privTime+"  privTotialFlow="+privTotialFlow+
				"  privAppFlow="+privAppFlow+"  privWifiAPFlow="+privWifiAPFlow);
		
		//一个月总流量超过4G则关闭wifir热点
		if (!isSameMonth()) {       //不是同一个月    那么就不可能是同一天
			Logger.d("!isSameMonth()");
			privTotialFlow = currentTotialFlow;
			monthTotialFlow = 0;
			FlowMPref.setMonthTotialFlow(FlowService.this, monthTotialFlow);
			//wifi热点相关判断
			if(isEnable){
				privWifiAPFlow = currentWifiAPFlow;
			}
			dayWifiAPToaial = 0;
			FlowMPref.setWifiAPFlow(FlowService.this, dayWifiAPToaial);
			//视频APP相关判断
			privAppFlow = currentAppFlow;
			appTotialFlow = 0;
			FlowMPref.setAppFlow(FlowService.this, appTotialFlow);
		} else {									//是同一个月
			Logger.d("是同一个月");
			if (!isSameDay()) {       //不是同一天
				Logger.d("是同一个月 但是不是同一天");
				
				//wifi热点相关判断
				if(isEnable){
					privWifiAPFlow = currentWifiAPFlow;
				}
				dayWifiAPToaial = 0;
				FlowMPref.setWifiAPFlow(FlowService.this, dayWifiAPToaial);
				//视频APP相关判断
				privAppFlow = currentAppFlow;
				appTotialFlow = 0;
				FlowMPref.setAppFlow(FlowService.this, appTotialFlow);
			} else {	
				Logger.d("是同一个月同一天");//是同一天
				monthTotialFlow += (currentTotialFlow - privTotialFlow);
				privTotialFlow = currentTotialFlow;
				FlowMPref.setMonthTotialFlow(FlowService.this, monthTotialFlow);
				if(isEnable){
					dayWifiAPToaial += (currentWifiAPFlow - privWifiAPFlow);
					privWifiAPFlow = currentWifiAPFlow;
					FlowMPref.setWifiAPFlow(FlowService.this, dayWifiAPToaial);
				}
				if (isApplyRules&&(monthTotialFlow >=WIFIAP_MONTH_MAX_FLOW)) {
					Logger.d("月流量达到最大值，关闭wifi热点    monthTotialFlow="+monthTotialFlow+"  WIFIAP_MONTH_MAX_FLOW="+WIFIAP_MONTH_MAX_FLOW);
					if (dayWifiAPToaial >= WIFIAP_DAY_MAX_FLOW) {
						Logger.d("wifi热点天流量达到最大值，关闭wifi热点dayWifiAPToaial="+dayWifiAPToaial+"  WIFIAP_DAY_MAX_FLOW="+WIFIAP_DAY_MAX_FLOW);
						if(isWifiApEnabled()){
							setWifiAP(false);
						}
					}
				}
				
				appTotialFlow += (currentAppFlow - privAppFlow);
				privAppFlow = currentAppFlow;
				FlowMPref.setAppFlow(FlowService.this, appTotialFlow);
				if (isApplyRules&&(appTotialFlow >= APP_DAY_MAX_FLOW)) {
					if(getVideoUid()!=-1){
						Logger.d("视频APP流量达到最大值，禁止视频APP联网appTotialFlow="+appTotialFlow+"  APP_DAY_MAX_FLOW="+APP_DAY_MAX_FLOW);
						if(isSendSocket){
							Logger.d("开始发送脚本指令");
							socketSend(Api.scriptHeader(this, getVideoUid()));
							isSendSocket=false;
						}
					}
				}
			}
		}
		privTime = currentTime;
		FlowMPref.setTime(FlowService.this, privTime);
		Logger.d("judgeWifiAPAble  privTime="+privTime+"  dayWifiAPToaial="+dayWifiAPToaial+
				"  monthTotialFlow="+monthTotialFlow+"  appTotialFlow="+appTotialFlow);
	}
	
	/**
	 * 获取视频APP的Uid
	 */
	private int getVideoUid(){
		int uid=-1;
		PackageManager pkgmanager = this.getPackageManager();
		List<ApplicationInfo> installed = pkgmanager.getInstalledApplications(0);
		for(int i=0;i<installed.size();i++){
			if(installed.get(i).packageName.equals(PACKAGENAME_OF_VIDEO)){
				Logger.d("packageName="+installed.get(i).packageName);
				uid=installed.get(i).uid;
				Logger.d("uid="+uid);
				break;
			}
		}
		return uid;
	}
	
	/**
	 * 使用socket发送UID
	 * @param data
	 * @throws Exception
	 */
	private void socketSend(String data){
		Logger.d("socketSend data=\n"+data);
		try {
			Socket client=new Socket(IP, PORT);
			DataOutputStream dos=new DataOutputStream(client.getOutputStream());
			
//			dos.writeUTF(data);
			dos.writeBytes(data);
			
			Thread.sleep(2000);
			if(dos!=null){
				dos.close();
			}
			if(client!=null){
				client=null;
			}
		} catch (Exception e) {
			Logger.d("socketSend e="+e.toString());
			e.printStackTrace();
		}  
	}
	/**判断热点状态*/
	public boolean isWifiApEnabled() {
		Logger.d("getWifiAPState()="+getWifiAPState());
		return getWifiAPState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
	}
	
	class  WifiAPHandler extends Handler{
		public WifiAPHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
				switch(msg.what){
				case CYCLE_JUDGEMENT:
					Logger.d("CYCLE_JUDGEMENT");
					judgeWifiAPAble();
					mWifiAPHandler.sendEmptyMessageDelayed(CYCLE_JUDGEMENT,5000);
					break;
//				case PREENT_APP_NETWORK:               //开机做判断如果是同一天且视频APP流量已超过限定值，则直接禁网
//					Logger.d( "PREENT_APP_NETWORK");
//					if(appTotialFlow>=APP_DAY_MAX_FLOW){
//						if(getVideoUid()!=-1){
//							if(isSendSocket){
//								Logger.d( "开始发送脚本指令");
//								socketSend(Api.scriptHeader(FlowService.this, getVideoUid()));
//								isSendSocket=false;
//							}
//						}
//					}
//					mWifiAPHandler.sendEmptyMessage(CYCLE_JUDGEMENT);
//					break;
//				case CTRL_FLOW_SPEED_3G:                         //开机对3G网络限速
//					socketSend(Api.getScript(true));
//					break;
				case CTRL_FLOW_SPEED_WIFIAP:         //wifi热点打开时设置wifi热点限速
					ArrayList<String> ipList=Api.getConnectedIP();
					if(ipList.size()!=ipSize){
						if(ipList.size()>0){
							for(int i=0;i<ipList.size();i++){
								socketSend(Api.getScript(false,ipList.get(i)));
							}
						}
					}
					ipSize=ipList.size();
					break;
				case CLEAN_ALL_RULES:
					if(isNeedSendClean){
						socketSend(Api.getCleanAllRules());
						isNeedSendClean=false;
						isSendSocket=true;
						isSend3G=true;
						isApplyRules=false;         //不启用规则判断
					}
					break;
				}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
