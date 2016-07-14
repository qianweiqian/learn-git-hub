package com.android.dialer;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.dialer.R;
import android.os.SystemProperties;

public class DeviceInfoActivity extends Activity implements SensorEventListener {

	private static final String TAG = "DeviceInfoActivity";
   
	private static final int EVENT_GET_BARCODE = 0;

	TextView device_BaseInfo;
	TextView device_VerInfo;
	TextView device_MemInfo;
	TextView device_MainLcdInfo;
	TextView device_MainCamInfo;
	TextView device_G_SensorInfo;
	TextView device_TpChip;
	TextView device_TpVer;
	ImageButton device_TpTest;
	TextView device_Barcode;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_info);

		device_BaseInfo = (TextView) findViewById(R.id.baseInfo);
		device_BaseInfo.setText("ALPS.L1.MP3.V1_BOWAY6735M_65C_L");
		
		device_VerInfo = (TextView) findViewById(R.id.verInfo);
		device_VerInfo.setText(Build.DISPLAY);
		
		device_MemInfo = (TextView) findViewById(R.id.memInfo);
		device_MemInfo.setText("SD7DP28C-4G");
		
		device_MainLcdInfo = (TextView) findViewById(R.id.mainLcdInfo);
		device_MainLcdInfo.setText(fetch_lcm_info());

		device_MainCamInfo = (TextView) findViewById(R.id.mainCamInfo);
		device_MainCamInfo.setText(fetch_camera_info());
		
		device_G_SensorInfo = (TextView) findViewById(R.id.G_SensorInfo);
		device_G_SensorInfo.setText(getGSensorInformation());
		
		device_TpChip = (TextView) findViewById(R.id.tpChip);
		device_TpChip.setText("GT9157");
		
		//device_TpVer = (TextView) findViewById(R.id.tpVer);
		//device_TpVer.setText("1.007");
		
		device_TpTest = (ImageButton) findViewById(R.id.tpTest);
		device_TpTest.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClassName("com.goodix.tools",
						"com.goodix.tools.GuitarTools");
				startActivity(intent);
			}

		});
    //by zhouhuiwen delete start 20140408
    
		device_Barcode = (TextView) findViewById(R.id.barcode);
		device_Barcode.setText(getBarcodeInfo());
		
		//by zhouhuiwen delete end 20140408
	}

	@Override
	protected void onResume() {
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		super.onResume();
	}

	/**
	 * Get CPU info
	 * @return
	 */
	public static String fetch_cpu_info() {
		String result = null;
		CMDExecute cmdexe = new CMDExecute();
		try {
			String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
			result = cmdexe.run(args, "/system/bin/");
			Log.i("result", "result=" + result);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Get Main LCM info
	 * @return
	 */
	public static String fetch_lcm_info() {
		String result = null;
		CMDExecute cmdexe = new CMDExecute();
		try {
			String[] args = { "/system/bin/cat", "/proc/lcm_reg" };
			result = cmdexe.run(args, "/system/bin/");
			Log.i("result", "result=" + result);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Get Camera Info.
	 * @return
	 */
	public static String fetch_camera_info() {
		String result = null;
		CMDExecute cmdexe = new CMDExecute();
		try {
			String[] args = { "/system/bin/cat", "/proc/driver/camera_info" };
			result = cmdexe.run(args, "/system/bin/");
			Log.i("result", "result=" + result);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @return
	 */
	public static String fetch_disk_info() {
		String result = null;
		CMDExecute cmdexe = new CMDExecute();
		try {
			String[] args = { "/system/bin/df" };
			result = cmdexe.run(args, "/system/bin/");
			Log.i("result", "result=" + result);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @param cx
	 * @return
	 */
	public static String getDisplayMetrics(Context cx) {
		String str = "";
		DisplayMetrics dm = new DisplayMetrics();
		dm = cx.getApplicationContext().getResources().getDisplayMetrics();
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;
		float density = dm.density;
		float xdpi = dm.xdpi;
		float ydpi = dm.ydpi;
		str += "The absolute width: " + String.valueOf(screenWidth) + "pixels ";
		str += "The absolute heightin: " + String.valueOf(screenHeight)
				+ "pixels ";
		str += "The logical density of the display. : "
				+ String.valueOf(density) + " ";
		str += "X dimension : " + String.valueOf(xdpi) + "pixels per inch ";
		str += "Y dimension : " + String.valueOf(ydpi) + "pixels per inch ";
		return str;
	}

	/**
	 * 
	 * @return
	 */
	public String getGsensorInfo() {
		SensorManager sensorManager = (SensorManager)this.getSystemService(SENSOR_SERVICE); 
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL); 
		String sensorName = null;
		for (Sensor s:sensors){ 
			sensorName = s.getName(); 
		}
		return sensorName;
	}
	

	/**
	 * 
	 * @return
	 */
	public String getGSensorInformation() {
		SensorManager sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		return sensor.getName();
	}
	
        /**
	 * Get Barcode
	 * @return
	 */
	//by zhouhuiwen modify start 20140408
	private String getBarcodeInfo() {
	    String serial = SystemProperties.get("gsm.serial").trim();
		
	    return serial;
  }
        
   //by zhouhuiwen modify end 20140408

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

	}

}
