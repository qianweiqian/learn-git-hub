package com.android.incallui;

public class SetTouchSwitch {

	public native int enableTouch();
	public native int disenableTouch();
	
	public native int readCurrentStatus();
    
    static{
            System.loadLibrary("boway_mtktpd_dialer_jni");
    }
	
}
