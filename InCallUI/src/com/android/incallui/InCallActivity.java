/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.incallui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.ims.ImsManager;
import com.android.phone.common.animation.AnimUtils;
import com.android.phone.common.animation.AnimationListenerAdapter;
import com.android.contacts.common.interactions.TouchPointManager;
import com.android.contacts.common.util.MaterialColorMapUtils;
import com.android.contacts.common.util.MaterialColorMapUtils.MaterialPalette;
import com.android.contacts.common.widget.SelectPhoneAccountDialogFragment;
import com.android.contacts.common.widget.SelectPhoneAccountDialogFragment.SelectPhoneAccountListener;
import com.android.incallui.Call.State;
import com.mediatek.incallui.DMLockBroadcastReceiver;
import com.mediatek.incallui.InCallUtils;
import com.mediatek.incallui.SmartBookUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mediatek.incallui.ext.ExtensionManager;
import com.mediatek.incallui.ext.IInCallExt;
import com.mediatek.incallui.recorder.PhoneRecorderUtils;
import com.mediatek.incallui.wfc.InCallUiWfcUtils;
import android.os.PowerManager;// Add For synchronize ringer and UI

/**
 * Phone app "in call" screen.
 */
public class InCallActivity extends Activity {

    public static final String SHOW_DIALPAD_EXTRA = "InCallActivity.show_dialpad";
    public static final String DIALPAD_TEXT_EXTRA = "InCallActivity.dialpad_text";
    public static final String NEW_OUTGOING_CALL_EXTRA = "InCallActivity.new_outgoing_call";
    public static final String SHOW_CIRCULAR_REVEAL_EXTRA = "InCallActivity.show_circular_reveal";
    // BEGIN BOWAY, yulong.tan, 20150505, InCall UI.
    public static boolean isSimpleSystem;
    // public static Bitmap blurredWallpaperBitmap; // MODIFY BOWAY, yulong.tan, 20151029, Using a fixed background.
    // END BOWAY, yulong.tan
    private CallButtonFragment mCallButtonFragment;
    private CallCardFragment mCallCardFragment;
    private AnswerFragment mAnswerFragment;    
    
    // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
    private WindowAnswerFragment mWindowAnswerFragment;
    private static boolean isWindowLock = false;
    private WakeLock mWakeLock;
    private Context mContext;
    private static final String TAG = "InCallActivity";

    public static final String WINDOW_LID_CLOSED_CHANGED =
        "android.intent.action.WINDOW_LID_CLOSED_CHANGED";
    public static final String KEY_LID_CLOSED =
        "sys.key_lid_closed";
    // END BOWAY, kailong.liu
    
    // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
    private SimpleAnswerFragment mSimpleAnswerFragment;
    // END BOWAY, yulong.tan
    private DialpadFragment mDialpadFragment;
    private ConferenceManagerFragment mConferenceManagerFragment;
    private FragmentManager mChildFragmentManager;
    /// M:for ALPS01825589, need to dismiss post dialog when add another call. @{
    private PostCharDialogFragment mPostCharDialogfragment;
    /// @}

    private boolean mIsForegroundActivity;
    private AlertDialog mDialog;

    /** Use to pass 'showDialpad' from {@link #onNewIntent} to {@link #onResume} */
    private boolean mShowDialpadRequested;

    /** Use to determine if the dialpad should be animated on show. */
    private boolean mAnimateDialpadOnShow;

    /** Use to determine the DTMF Text which should be pre-populated in the dialpad. */
    private String mDtmfText;

    /** Use to pass parameters for showing the PostCharDialog to {@link #onResume} */
    private boolean mShowPostCharWaitDialogOnResume;
    private String mShowPostCharWaitDialogCallId;
    private String mShowPostCharWaitDialogChars;

    private boolean mIsLandscape;
    private Animation mSlideIn;
    private Animation mSlideOut;
    private boolean mDismissKeyguard = false;

    //begin Add For synchronize ringer and UI
    private PowerManager pm = null; 
    private boolean mIsHadStop = false;
    //end Add For synchronize ringer and UI

    /// M: record error dialog info when need Show error dialog after activity resume @{
    private boolean mDelayShowErrorDialogRequest = false;
    private CharSequence mDisconnectCauseDescription;
    /// @}
    AnimationListenerAdapter mSlideOutListener = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            showDialpad(false);
        }
    };

    /**
     * Stores the current orientation of the activity.  Used to determine if a change in orientation
     * has occurred.
     */
    private int mCurrentOrientation;

    /// M: DMLock, PPL
    private DMLockBroadcastReceiver mDMLockReceiver;
    
    //add by kailong.liu
    IntentFilter mInfofilter = new IntentFilter();
	// BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
    private final BroadcastReceiver mInfoReceiver = new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();
		 
		 		if (action.equals(WINDOW_LID_CLOSED_CHANGED)) {
		 	   		isWindowLock = SystemProperties.get(KEY_LID_CLOSED).equals("true");
		 	   		mCallCardFragment.hideWindowLayout(isWindowLock);
		 	   		if (mCallCardFragment != null){
				  	    mCallCardFragment.windowUpdateProximitySensorMode();
				 	}
				 	updateAnswerUi(isWindowLock,mCallCardFragment.slideVisible());
				 	if (!isWindowLock){
				 		quitFullScreen();
				 		releaseWakeLock();
				 		android.util.Log.e(TAG,"onReceive,releaseWakeLock");
				 	} else{
				 		setFullScreen();
				 		acquireWakeLock(mContext);
				 		android.util.Log.e(TAG,"onReceive,acquireWakeLock");
				 	}

                    // BEGIN BOWAY, yinling.zhu, 20150701, restart incallActivity when it's not foreground.
                    Log.i(TAG, "isWindowLock: " + isWindowLock + ", mIsForegroundActivity: " + mIsForegroundActivity);
                    if (isWindowLock && !mIsForegroundActivity) {
                        Intent i = new Intent(InCallActivity.this, InCallActivity.class);
                        i.setAction(Intent.ACTION_MAIN);
                        context.startActivity(i);
                    }
                    // END BOWAY, yinling.zhu
		 		}
			}
		};
		// END BOWAY, kailong.liu
    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(this, "onCreate()...  this = " + this);

        super.onCreate(icicle);
        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        mContext = this;
        isWindowLock = SystemProperties.get(KEY_LID_CLOSED).equals("true");
        if (isWindowLock){
        	 setFullScreen();
        }
        // END BOWAY, kailong.liu
        
        // BEGIN BOWAY, yulong.tan, 20150505, InCall UI.
        isRunning(this, "com.android.bowaybiglauncher");
        // END BOWAY, yulong.tan

        /// M: set the window flags @{
        /// Original code:
        /*
        // set this flag so this activity will stay in front of the keyguard
        // Have the WindowManager filter out touch events that are "too fat".
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        getWindow().addFlags(flags);
        */
        setWindowFlag();
        /// @}

        // Setup action bar for the conference call manager.
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.hide();
        }

        // TODO(klp): Do we need to add this back when prox sensor is not available?
        // lp.inputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;

        /// M: Add for plugin.@{
        ExtensionManager.registerApplicationContext(getApplicationContext());
        ExtensionManager.getRCSeInCallExt().onCreate(icicle, this, CallList.getInstance());
        /// @}

        // Inflate everything in incall_screen.xml and add it to the screen.
        setContentView(R.layout.incall_screen);
        // BEGIN BOWAY, yulong.tan, 20150505, InCall UI.
        FrameLayout frameLayout = (FrameLayout)findViewById(R.id.main);
        if(isSimpleSystem) {
            frameLayout.setBackgroundColor(R.color.incall_call_banner_text_color);
        } else {
            /*if (blurredWallpaperBitmap == null) {
                final WallpaperManager wallpaperManager = WallpaperManager  
                        .getInstance(this);  
                final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                Bitmap wallpaperBitmap = ((BitmapDrawable) wallpaperDrawable).getBitmap();
                blurredWallpaperBitmap = blur(wallpaperBitmap, this);
            }
            frameLayout.setBackground(new BitmapDrawable(getResources(), blurredWallpaperBitmap));*/
            frameLayout.setBackground(getResources().getDrawable(R.drawable.incall_screen_background));
        }
        // END BOWAY, yulong.tan
        

        mDMLockReceiver = DMLockBroadcastReceiver.getInstance(this);
        initializeInCall();

        internalResolveIntent(getIntent());

        mCurrentOrientation = getResources().getConfiguration().orientation;
        mIsLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        final boolean isRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_RTL;

        if (mIsLandscape) {
            mSlideIn = AnimationUtils.loadAnimation(this,
                    isRtl ? R.anim.dialpad_slide_in_left : R.anim.dialpad_slide_in_right);
            mSlideOut = AnimationUtils.loadAnimation(this,
                    isRtl ? R.anim.dialpad_slide_out_left : R.anim.dialpad_slide_out_right);
        } else {
            mSlideIn = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_in_bottom);
            mSlideOut = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_out_bottom);
        }

        mSlideIn.setInterpolator(AnimUtils.EASE_IN);
        mSlideOut.setInterpolator(AnimUtils.EASE_OUT);

        mSlideOut.setAnimationListener(mSlideOutListener);

        if (icicle != null) {
            // If the dialpad was shown before, set variables indicating it should be shown and
            // populated with the previous DTMF text.  The dialpad is actually shown and populated
            // in onResume() to ensure the hosting CallCardFragment has been inflated and is ready
            // to receive it.
            mShowDialpadRequested = icicle.getBoolean(SHOW_DIALPAD_EXTRA);
            mAnimateDialpadOnShow = false;
            mDtmfText = icicle.getString(DIALPAD_TEXT_EXTRA);
        }
        
        
        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        if (isWindowLock){
        	FrameLayout windowFrameLayout = (FrameLayout)findViewById(R.id.main);
        	windowFrameLayout.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        	//acquireWakeLock(mContext);
        	android.util.Log.e(TAG,"onCreate,acquireWakeLock");
		}
		// END BOWAY, kailong.liu
				
        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        mInfofilter.addAction(WINDOW_LID_CLOSED_CHANGED);
        registerReceiver(mInfoReceiver, mInfofilter);
        // END BOWAY, kailong.liu

        //begin Add For synchronize ringer and UI
        pm = (PowerManager)getSystemService(Context.POWER_SERVICE); 
        Log.d(this, "AAAscreen is on ? ----- " + pm.isScreenOn());
        //end Add For synchronize ringer and UI
        Log.d(this, "onCreate(): exit");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        Log.i(this, "onSaveInstanceState...");
        out.putBoolean(SHOW_DIALPAD_EXTRA, mCallButtonFragment.isDialpadVisible());
        ///M: ALPS01855248 @{
        // override SHOW_DIALPAD_EXTRA
        // because sometimes activity is killed, the activity will be created twice
        // ex, connect or disconnect smartbook
        // if the first time DialpadFragment has not enough time to show
        // this extra will be set false, the finally dialpad will not show in Phone
        if (mShowDialpadRequested) {
            out.putBoolean(SHOW_DIALPAD_EXTRA, mShowDialpadRequested);
            mShowDialpadRequested = false;
        }
        /// @}
        if (mDialpadFragment != null) {
            out.putString(DIALPAD_TEXT_EXTRA, mDialpadFragment.getDtmfText());
            ///M: ALPS01855248 @{
            // override DIALPAD_TEXT_EXTRA
            // because sometimes activity is killed, the activity will be created twice
            // ex, connect or disconnect smartbook
            // if the first time DialpadFragment has not enough time to show
            // this extra will be set null, the finally dialpad will not show in Phone
            if (mDtmfText != null) {
                out.putString(DIALPAD_TEXT_EXTRA, mDtmfText);
                mDtmfText = null;
            }
            /// @}
        }
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onStart() {
        Log.d(this, "onStart()...");
        super.onStart();

        // setting activity should be last thing in setup process
        InCallPresenter.getInstance().setActivity(this);
    }

    @Override
    protected void onResume() {
        Log.i(this, "onResume()...");
        super.onResume();

        mIsForegroundActivity = true;

        InCallPresenter.getInstance().setThemeColors();
        InCallPresenter.getInstance().onUiShowing(true);

        if (mShowDialpadRequested) {
            mCallButtonFragment.displayDialpad(true /* show */,
                    mAnimateDialpadOnShow /* animate */);
            //mShowDialpadRequested = false;
            //mAnimateDialpadOnShow = false;

            if (mDialpadFragment != null) {
                mDialpadFragment.setDtmfText(mDtmfText);
               //mDtmfText = null;
            }
        }

        if (mShowPostCharWaitDialogOnResume) {
            showPostCharWaitDialog(mShowPostCharWaitDialogCallId, mShowPostCharWaitDialogChars);
        }

        /// M: Fix ALPS01825035. @{
        // When there has incoming call, we need cancel this pending outgoing call.
        if (CallList.getInstance().getIncomingCall() != null) {
            dismissSelectAccountDialog();
			
            // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
            updateAnswerUi(isWindowLock,mCallCardFragment.slideVisible());
            // END BOWAY, kailong.liu			
			
            /// M: Fix ALPS01991506 we set CallCardFragment visible,before showAnswerUi
            mCallCardFragment.setVisible(true);
            // when resume from backlight off state, need to start ping again.
            // BEGIN BOWAY, yulong.tan, 20150513, InCall UI.
            if (isSimpleSystem) {
                mSimpleAnswerFragment.showAnswerUi(true);
            } else {
                mAnswerFragment.showAnswerUi(true);
            }
            // END BOWAY, yulong.tan
        }
        /// @}

        /// M:  Show error dialog after activity resume @{
        if (mDelayShowErrorDialogRequest) {
            if (mDisconnectCauseDescription != null && (!TextUtils.isEmpty(mDisconnectCauseDescription.toString())));
                showErrorDialog(mDisconnectCauseDescription);
            mDelayShowErrorDialogRequest = false;
        }
        /// @}

        /// M: For SmartBook
        // light screen on if InCallScreen is paused by new Intent.
        // onPause() will light screen off, see ALPS01052168.
        InCallPresenter.getInstance().lightOnScreenForSmartBook();

        //begin Add For synchronize ringer and UI
        final Call call = CallList.getInstance().getIncomingCall();
        Log.d(this, "onResum:call: " + call);
        if(call != null){
            Log.d(this, "BBBscreen is on ? ----- " + pm.isScreenOn());          
            if((call.getState() == Call.State.CALL_WAITING)
                    ||((call.getState() == Call.State.INCOMING)&&(pm.isScreenOn()&&mIsHadStop))){
                InCallPresenter.getInstance().playIncomingCallRingtone(call.getTelecommCall());                          
                mIsHadStop = false;                                             
            }
        }              
        //end Add For synchronize ringer and UI
    }
    
    // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
    private void updateAnswerUi(boolean isWindowLock ,boolean slideVisible){
        if(isWindowLock){
            FrameLayout windowFrameLayout = (FrameLayout)findViewById(R.id.main);
            windowFrameLayout.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        }else{
            FrameLayout windowFrameLayout = (FrameLayout)findViewById(R.id.main);
            /*if (blurredWallpaperBitmap == null) {
                final WallpaperManager wallpaperManager = WallpaperManager  
                        .getInstance(this);  
                final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                Bitmap wallpaperBitmap = ((BitmapDrawable) wallpaperDrawable).getBitmap();
                blurredWallpaperBitmap = blur(wallpaperBitmap, this);
            }
            windowFrameLayout.setBackground(new BitmapDrawable(getResources(), blurredWallpaperBitmap));*/
            windowFrameLayout.setBackground(getResources().getDrawable(R.drawable.incall_screen_background));
        }
        if(slideVisible){
            mWindowAnswerFragment.showAnswerUi(false);
            if (isSimpleSystem) {
                mSimpleAnswerFragment.showAnswerUi(false);
            } else {
                mAnswerFragment.showAnswerUi(false);
            }
        }else{
            //mCallButtonFragment.updateCallButtonUi(isWindowLock);
            if (isWindowLock) {
                mWindowAnswerFragment.showAnswerUi(true);
                if (isSimpleSystem) {
                    mSimpleAnswerFragment.showAnswerUi(false);
                } else {
                    mAnswerFragment.showAnswerUi(false);
                }
            } else {
                if (isSimpleSystem) {
                    mSimpleAnswerFragment.showAnswerUi(true);
                } else {
                    mAnswerFragment.showAnswerUi(true);
                }
                mWindowAnswerFragment.showAnswerUi(false);
            }
        }
    }
    // END BOWAY, kailong.liu
    
    

    // onPause is guaranteed to be called when the InCallActivity goes
    // in the background.
    @Override
    protected void onPause() {
        Log.d(this, "onPause()...");
        super.onPause();

        mIsForegroundActivity = false;

        if (mDialpadFragment != null ) {
            mDialpadFragment.onDialerKeyUp(null);
        }
        /// M: add for smrat book. When incall screen go to background, power off backlight.
        // And if have ringing call, we always should keep the screen on.
        if (CallList.getInstance().getIncomingCall() == null) {
            SmartBookUtils.updatePowerForSmartBook(getApplicationContext(), false);
        }
        /// @}
        InCallPresenter.getInstance().onUiShowing(false);
        if (isFinishing()) {
            InCallPresenter.getInstance().unsetActivity(this);
        }
    }

    @Override
    protected void onStop() {
        Log.d(this, "onStop()...");
        /// M: ALPS01786201. @{
        // Dismiss any dialogs we may have brought up, just to be 100%
        // sure they won't still be around when we get back here.
        dismissPendingDialogs();
        /// @}

        /// M: ALPS01855248 @{
        // postpone reset these three variables values from onResume to onStop
        mShowDialpadRequested = false;
        mAnimateDialpadOnShow = false;
        mDtmfText = null;
        /// @}
        super.onStop();
        mIsHadStop = true; // Add For synchronize ringer and UI
    }

    @Override
    protected void onDestroy() {
        Log.d(this, "onDestroy()...  this = " + this);
        InCallPresenter.getInstance().unsetActivity(this);
        /// M: DM lock Feature.
        mDMLockReceiver.unregister(this);

        super.onDestroy();

        /// M: Add for Extension.@{
        ExtensionManager.getRCSeInCallExt().onDestroy(this);
        /// @}
        
       // BEGIN BOWAY, kailong.liu, 20150618, InCall UI. 
			 unregisterReceiver(mInfoReceiver);
	   // END BOWAY, kailong.liu
	   		releaseWakeLock();
	   		android.util.Log.e(TAG,"onDestroy,releaseWakeLock");
        // BEGIN BOWAY, yulong.tan, 20151216, To prevent memory leaks.
        releaseResource();
        setContentView(R.layout.view_null);
        // END BOWAY, yulong.tan
    }

    /**
     * Returns true when theActivity is in foreground (between onResume and onPause).
     */
    /* package */ boolean isForegroundActivity() {
        return mIsForegroundActivity;
    }

    /*
    private boolean hasPendingErrorDialog() {
    */
    public boolean hasPendingErrorDialog() {
        return mDialog != null;
    }

    /**
     * Dismisses the in-call screen.
     *
     * We never *really* finish() the InCallActivity, since we don't want to get destroyed and then
     * have to be re-created from scratch for the next call.  Instead, we just move ourselves to the
     * back of the activity stack.
     *
     * This also means that we'll no longer be reachable via the BACK button (since moveTaskToBack()
     * puts us behind the Home app, but the home app doesn't allow the BACK key to move you any
     * farther down in the history stack.)
     *
     * (Since the Phone app itself is never killed, this basically means that we'll keep a single
     * InCallActivity instance around for the entire uptime of the device.  This noticeably improves
     * the UI responsiveness for incoming calls.)
     */
    @Override
    public void finish() {
        Log.i(this, "finish().  Dialog showing: " + (mDialog != null));

        // skip finish if we are still showing a dialog.
        if (!hasPendingErrorDialog() && (isSimpleSystem ? true : !mAnswerFragment.hasPendingDialogs())) {
            /// M: sometimes it will call finish() from onResume() and the finish will delay too long time
            // to disturb the new call to process, so just put the activity to back instead of finish it.
            // when new call need to show it only need to restore the instance.

            // rollback to google default solution since too many side effects.
            //TODO still need to find a solution to avoid destroy activity take too long time @{
            super.finish();
            // moveTaskToBack(true);
            /// @}
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(this, "onNewIntent: intent = " + intent);
        // We're being re-launched with a new Intent.  Since it's possible for a
        // single InCallActivity instance to persist indefinitely (even if we
        // finish() ourselves), this sequence can potentially happen any time
        // the InCallActivity needs to be displayed.

        // Stash away the new intent so that we can get it in the future
        // by calling getIntent().  (Otherwise getIntent() will return the
        // original Intent from when we first got created!)
        setIntent(intent);

        // Activities are always paused before receiving a new intent, so
        // we can count on our onResume() method being called next.

        // Just like in onCreate(), handle the intent.
        internalResolveIntent(intent);
    }

    @Override
    public void onBackPressed() {
        Log.i(this, "onBackPressed");

        // BACK is also used to exit out of any "special modes" of the
        // in-call UI:

        if (!mConferenceManagerFragment.isVisible() && !mCallCardFragment.isVisible()) {
            return;
        }

        if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            mCallButtonFragment.displayDialpad(false /* show */, true /* animate */);
            return;
        } else if (mConferenceManagerFragment.isVisible()) {
            showConferenceCallManager(false);
            return;
        }

        // Always disable the Back key while an incoming call is ringing
        final Call call = CallList.getInstance().getIncomingCall();
        if (call != null) {
            Log.d(this, "Consume Back press for an incoming call");
            return;
        }

        // Nothing special to do.  Fall back to the default behavior.
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // push input to the dialer.
        if (mDialpadFragment != null && (mDialpadFragment.isVisible()) &&
                (mDialpadFragment.onDialerKeyUp(event))){
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
            // Always consume CALL to be sure the PhoneWindow won't do anything with it
            return true;
        }
        // BEGIN BOWAY, yulong.tan, 20151013, Disable Home key when there has incoming call.
        else if (keyCode == KeyEvent.KEYCODE_HOME) {
            if (CallList.getInstance().getIncomingCall() == null) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME); 
                startActivity(intent);
            }
        }
        // END BOWAY, yulong.tan
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                boolean handled = InCallPresenter.getInstance().handleCallKey();
                if (!handled) {
                    Log.w(this, "InCallActivity should always handle KEYCODE_CALL in onKeyDown");
                }
                // Always consume CALL to be sure the PhoneWindow won't do anything with it
                return true;

            // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
            // The standard system-wide handling of the ENDCALL key
            // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
            // already implements exactly what the UI spec wants,
            // namely (1) "hang up" if there's a current active call,
            // or (2) "don't answer" if there's a current ringing call.

            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                // Ringer silencing handled by PhoneWindowManager.
                break;

            case KeyEvent.KEYCODE_MUTE:
                // toggle mute
                TelecomAdapter.getInstance().mute(!AudioModeProvider.getInstance().getMute());
                return true;

            // Various testing/debugging features, enabled ONLY when VERBOSE == true.
            case KeyEvent.KEYCODE_SLASH:
                if (Log.VERBOSE) {
                    Log.v(this, "----------- InCallActivity View dump --------------");
                    // Dump starting from the top-level view of the entire activity:
                    Window w = this.getWindow();
                    View decorView = w.getDecorView();
                    Log.d(this, "View dump:" + decorView);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_EQUALS:
                // TODO: Dump phone state?
                break;
        }

        if (event.getRepeatCount() == 0 && handleDialerKeyDown(keyCode, event)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {
        Log.v(this, "handleDialerKeyDown: keyCode " + keyCode + ", event " + event + "...");

        // As soon as the user starts typing valid dialable keys on the
        // keyboard (presumably to type DTMF tones) we start passing the
        // key events to the DTMFDialer's onDialerKeyDown.
        if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            return mDialpadFragment.onDialerKeyDown(event);

            // TODO: If the dialpad isn't currently visible, maybe
            // consider automatically bringing it up right now?
            // (Just to make sure the user sees the digits widget...)
            // But this probably isn't too critical since it's awkward to
            // use the hard keyboard while in-call in the first place,
            // especially now that the in-call UI is portrait-only...
        }

        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        InCallPresenter.getInstance().getProximitySensor().onConfigurationChanged(config);
        Log.d(this, "onConfigurationChanged "+config.orientation);

        // Check to see if the orientation changed to prevent triggering orientation change events
        // for other configuration changes.
        if (config.orientation != mCurrentOrientation) {
            mCurrentOrientation = config.orientation;
            InCallPresenter.getInstance().onDeviceRotationChange(
                    getWindowManager().getDefaultDisplay().getRotation());
            InCallPresenter.getInstance().onDeviceOrientationChange(mCurrentOrientation);
        }
        /// M: ALPS01494693 SmartBook
        // If configuration changed, the screen me be light off by on Pause. it should be re-light on.
        InCallPresenter.getInstance().lightOnScreenForSmartBook();

        super.onConfigurationChanged(config);
    }

    public CallButtonFragment getCallButtonFragment() {
        return mCallButtonFragment;
    }

    public CallCardFragment getCallCardFragment() {
        return mCallCardFragment;
    }

    private void internalResolveIntent(Intent intent) {
        final String action = intent.getAction();

        if (action.equals(intent.ACTION_MAIN)) {
            // This action is the normal way to bring up the in-call UI.
            //
            // But we do check here for one extra that can come along with the
            // ACTION_MAIN intent:

            if (intent.hasExtra(SHOW_DIALPAD_EXTRA)) {
                // SHOW_DIALPAD_EXTRA can be used here to specify whether the DTMF
                // dialpad should be initially visible.  If the extra isn't
                // present at all, we just leave the dialpad in its previous state.

                final boolean showDialpad = intent.getBooleanExtra(SHOW_DIALPAD_EXTRA, false);
                Log.d(this, "- internalResolveIntent: SHOW_DIALPAD_EXTRA: " + showDialpad);

                relaunchedFromDialer(showDialpad);
            }

            if (intent.getBooleanExtra(NEW_OUTGOING_CALL_EXTRA, false)) {
                intent.removeExtra(NEW_OUTGOING_CALL_EXTRA);
                Call call = CallList.getInstance().getOutgoingCall();
                if (call == null) {
                    call = CallList.getInstance().getPendingOutgoingCall();
                }

                Bundle extras = null;
                if (call != null) {
                    extras = call.getTelecommCall().getDetails().getExtras();
                }
                if (extras == null) {
                    // Initialize the extras bundle to avoid NPE
                    extras = new Bundle();
                }

                Point touchPoint = null;
                if (TouchPointManager.getInstance().hasValidPoint()) {
                    // Use the most immediate touch point in the InCallUi if available
                    touchPoint = TouchPointManager.getInstance().getPoint();
                } else {
                    // Otherwise retrieve the touch point from the call intent
                    if (call != null) {
                        touchPoint = (Point) extras.getParcelable(TouchPointManager.TOUCH_POINT);
                    }
                }

                // This is only true in the case where an outgoing call is initiated by tapping
                // on the "Select account dialog", in which case we skip the initial animation. In
                // most other cases the circular reveal is done by OutgoingCallAnimationActivity.
                final boolean showCircularReveal =
                        intent.getBooleanExtra(SHOW_CIRCULAR_REVEAL_EXTRA, false);
                mCallCardFragment.animateForNewOutgoingCall(touchPoint, showCircularReveal);

                // InCallActivity is responsible for disconnecting a new outgoing call if there
                // is no way of making it (i.e. no valid call capable accounts)
                if (InCallPresenter.isCallWithNoValidAccounts(call)) {
                    TelecomAdapter.getInstance().disconnectCall(call.getId());
                }

                dismissKeyguard(true);
            }

            Call pendingAccountSelectionCall = CallList.getInstance().getWaitingForAccountCall();
            if (pendingAccountSelectionCall != null) {
                /// M: [@Modification for finishing Transparent InCall Screen if necessary]
                /// add for resolve finish incall screen issue. @{
                mIsLunchedAccountSelectDlg = true;
                /// @}
                mCallCardFragment.setVisible(false);
                Bundle extras = pendingAccountSelectionCall
                        .getTelecommCall().getDetails().getExtras();

                final List<PhoneAccountHandle> phoneAccountHandles;
                if (extras != null) {
                    phoneAccountHandles = extras.getParcelableArrayList(
                            android.telecom.Call.AVAILABLE_PHONE_ACCOUNTS);
                } else {
                    phoneAccountHandles = new ArrayList<>();
                }

                SelectPhoneAccountListener listener = new SelectPhoneAccountListener() {
                    @Override
                    public void onPhoneAccountSelected(PhoneAccountHandle selectedAccountHandle,
                            boolean setDefault) {
                        InCallPresenter.getInstance().handleAccountSelection(selectedAccountHandle,
                                setDefault);
                    }
                    @Override
                    public void onDialogDismissed() {
                        InCallPresenter.getInstance().cancelAccountSelection();
                    }
                };

                /// M: Modified for suggesting phone account feature. @{
                SelectPhoneAccountDialogFragment.showAccountDialog(getFragmentManager(),
                        R.string.select_phone_account_for_calls,
                        (Boolean) ExtensionManager.getInCallExt()
                                .replaceValue(true, IInCallExt.HINT_BOOLEAN_SHOW_ACCOUNT_DIALOG),
                        phoneAccountHandles,
                        listener,
                        InCallUtils.getSuggestedPhoneAccountHandle(pendingAccountSelectionCall));
                /// @}
            } else {
                /// M: Fix ALPS01922620. @{
                // After pressed home key when showing Account dialog, the activity will not been
                // finished and when start activity with new intent, the account dialog will show
                // again but there has no pending call, so need dismiss accout dialog at here.
                dismissSelectAccountDialog();
                /// @}
                mCallCardFragment.setVisible(true);

                /// M: [@Modification for finishing Transparent InCall Screen if necessary]
                /// add for resolve finish incall screen issue. @{
                mIsLunchedAccountSelectDlg = false;
                /// @}
            }

            return;
        }
    }

    private void relaunchedFromDialer(boolean showDialpad) {
        mShowDialpadRequested = showDialpad;
        mAnimateDialpadOnShow = true;

        if (mShowDialpadRequested) {
            // If there's only one line in use, AND it's on hold, then we're sure the user
            // wants to use the dialpad toward the exact line, so un-hold the holding line.
            final Call call = CallList.getInstance().getActiveOrBackgroundCall();
            if (call != null && call.getState() == State.ONHOLD) {
                TelecomAdapter.getInstance().unholdCall(call.getId());
            }
        }
    }

    private void initializeInCall() {
        if (mCallCardFragment == null) {
            mCallCardFragment = (CallCardFragment) getFragmentManager()
                    .findFragmentById(R.id.callCardFragment);
        }

        mChildFragmentManager = mCallCardFragment.getChildFragmentManager();

        if (mCallButtonFragment == null) {
            mCallButtonFragment = (CallButtonFragment) mChildFragmentManager
                    .findFragmentById(R.id.callButtonFragment);
            mCallButtonFragment.getView().setVisibility(View.INVISIBLE);
        }

        // BEGIN BOWAY, yulong.tan, 20150513, InCall UI.
        if (isSimpleSystem && mSimpleAnswerFragment == null) {
            mSimpleAnswerFragment = (SimpleAnswerFragment) mChildFragmentManager
                    .findFragmentById(R.id.answerFragment);
        } else if (mAnswerFragment == null) {
            mAnswerFragment = (AnswerFragment) mChildFragmentManager
                    .findFragmentById(R.id.answerFragment);
        }
        // END BOWAY, yulong.tan
        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
				if (mWindowAnswerFragment == null) {
            mWindowAnswerFragment = (WindowAnswerFragment) mChildFragmentManager
                    .findFragmentById(R.id.windowAnswerFragment);
        }
        // END BOWAY, kailong.liu

        if (mConferenceManagerFragment == null) {
            mConferenceManagerFragment = (ConferenceManagerFragment) getFragmentManager()
                    .findFragmentById(R.id.conferenceManagerFragment);
            mConferenceManagerFragment.getView().setVisibility(View.INVISIBLE);
        }

        /// M: DM lock Feature
        mDMLockReceiver.register(this);
    }

    /**
     * Simulates a user click to hide the dialpad. This will update the UI to show the call card,
     * update the checked state of the dialpad button, and update the proximity sensor state.
     */
    public void hideDialpadForDisconnect() {
        mCallButtonFragment.displayDialpad(false /* show */, true /* animate */);
    }

    public void dismissKeyguard(boolean dismiss) {
        if (mDismissKeyguard == dismiss) {
            return;
        }
        mDismissKeyguard = dismiss;
        if (dismiss) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    private void showDialpad(boolean showDialpad) {
        // If the dialpad is being shown and it has not already been loaded, replace the dialpad
        // placeholder with the actual fragment before continuing.
        if (mDialpadFragment == null && showDialpad) {
            final FragmentTransaction loadTransaction = mChildFragmentManager.beginTransaction();
            View fragmentContainer = findViewById(R.id.dialpadFragmentContainer);
            mDialpadFragment = new DialpadFragment();
            loadTransaction.replace(fragmentContainer.getId(), mDialpadFragment,
                    DialpadFragment.class.getName());
            loadTransaction.commitAllowingStateLoss();
            mChildFragmentManager.executePendingTransactions();
        }
        // BEGIN BOWAY, xiao.shen, 20151019, modify InCall UI.
        mCallCardFragment.showCallButtonArea(!showDialpad);
        // END BOWAY, xiao.shen
        final FragmentTransaction ft = mChildFragmentManager.beginTransaction();
        if (showDialpad) {
            ft.show(mDialpadFragment);
        } else {
            ft.hide(mDialpadFragment);
        }
        ft.commitAllowingStateLoss();
    }

    public void displayDialpad(boolean showDialpad, boolean animate) {
        // If the dialpad is already visible, don't animate in. If it's gone, don't animate out.
        if ((showDialpad && isDialpadVisible()) || (!showDialpad && !isDialpadVisible())) {
            return;
        }
        // We don't do a FragmentTransaction on the hide case because it will be dealt with when
        // the listener is fired after an animation finishes.
        if (!animate) {
            showDialpad(showDialpad);
            ///M: ALPS01855248 @{
            // resize end button size when dialpad shows
            // to avoid the overlap between dialpad and end button
            mCallCardFragment.onDialpadVisiblityChange(showDialpad);
            /// @}
        } else {
            if (showDialpad) {
                showDialpad(true);
                mDialpadFragment.animateShowDialpad();
            }
            mCallCardFragment.onDialpadVisiblityChange(showDialpad);
            mDialpadFragment.getView().startAnimation(showDialpad ? mSlideIn : mSlideOut);
        }

        InCallPresenter.getInstance().getProximitySensor().onDialpadVisible(showDialpad);
    }
    // BEGIN BOWAY, xiao.shen, 20151019, modify InCall UI.
    public void hideDiapad() {
        displayDialpad(false, false);
    }
    // END BOWAY, xiao.shen
    public boolean isDialpadVisible() {
        return mDialpadFragment != null && mDialpadFragment.isVisible();
    }

    /**
     * Hides or shows the conference manager fragment.
     *
     * @param show {@code true} if the conference manager should be shown, {@code false} if it
     *                         should be hidden.
     */
    public void showConferenceCallManager(boolean show) {
        Log.d(this, "[showConferenceCallManager] show:" + show);
        mConferenceManagerFragment.setVisible(show);

        /**
         * M: [ALPS02025119]The InCallActivity is transparent, so that when the VoLTE
         * conference invitation dialog Activity appears, the AMS will change its
         * background to Launcher. We should change the InCallActivity to non-transparent
         * when the conference manager appears.
         */
        changeToTransparent(!show);

        // Need to hide the call card fragment to ensure that accessibility service does not try to
        // give focus to the call card when the conference manager is visible.
        mCallCardFragment.getView().setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public void showPostCharWaitDialog(String callId, String chars) {
        if (isForegroundActivity()) {
            /// M:for ALPS01825589, need to dismiss post dialog when add another call. @{
            mPostCharDialogfragment = new PostCharDialogFragment(callId,  chars);
            mPostCharDialogfragment.show(getFragmentManager(), "postCharWait");
            /// @}

            mShowPostCharWaitDialogOnResume = false;
            mShowPostCharWaitDialogCallId = null;
            mShowPostCharWaitDialogChars = null;
        } else {
            mShowPostCharWaitDialogOnResume = true;
            mShowPostCharWaitDialogCallId = callId;
            mShowPostCharWaitDialogChars = chars;
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (mCallCardFragment != null) {
            mCallCardFragment.dispatchPopulateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    public void maybeShowErrorDialogOnDisconnect(DisconnectCause disconnectCause) {
        Log.d(this, "maybeShowErrorDialogOnDisconnect Disconnect cause code " + disconnectCause.getCode());
        if (!isFinishing() && !TextUtils.isEmpty(disconnectCause.getDescription())
                && (disconnectCause.getCode() == DisconnectCause.ERROR ||
                        disconnectCause.getCode() == DisconnectCause.RESTRICTED)) {
            showErrorDialog(disconnectCause.getDescription());
        } 
	/// M: if InCallActivity has not resumed already, show error dialog later @{
        else if(!isResumed() && !TextUtils.isEmpty(disconnectCause.getDescription())
                && (disconnectCause.getCode() == DisconnectCause.ERROR ||
                        disconnectCause.getCode() == DisconnectCause.RESTRICTED)) {
	    Log.d(this, "maybeShowErrorDialogOnDisconnect, activity not resumed");
            mDelayShowErrorDialogRequest = true;
	    mDisconnectCauseDescription = disconnectCause.getDescription();
	    return;
        /// M: [@Modification for finishing Transparent InCall Screen if necessary] @{
        }
        ///M: WFC <handle wfc call disconnect error and first wifi call ends popup> @{
        else if (ImsManager.isWfcEnabledByUser(this) && !isFinishing() &&
                disconnectCause.getCode() == DisconnectCause.WFC_CALL_ERROR) {
            Log.d(this, "[wfc]maybeShowErrorDialogOnDisconnect WFC_CALL_ERROR ");
            InCallUiWfcUtils.maybeShowWfcError(this , disconnectCause.getLabel(),
                    disconnectCause.getDescription());
        } else if ( ImsManager.isWfcEnabledByUser(this) && !isFinishing()
                &&!(disconnectCause.getCode() == DisconnectCause.OTHER)//comeswhen merge two conference call
                && !(disconnectCause.getCode() == DisconnectCause.REJECTED)
                && !(disconnectCause.getCode() == DisconnectCause.MISSED)
                && !(disconnectCause.getCode() == DisconnectCause.ERROR)) {
            Log.d(this, "[wfc]maybeShowErrorDialogOnDisconnect maybeShowCongratsPopup ");
            InCallUiWfcUtils.maybeShowCongratsPopup(this);
      
        } else {
            dismissInCallActivityIfNecessary();
        }
        /// @}
    }

    public void dismissPendingDialogs() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        
        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        if (!isSimpleSystem&&!isWindowLock) {
        	mAnswerFragment.dismissPendingDialogues();
        }
        // END BOWAY, kailong.liu

        /// M: For ALPS01786201. @{
        // dismiss all popup menu when user leave activity.
        if (mCallButtonFragment != null) {
            mCallButtonFragment.dismissPopupMenu();
        }
        /// @}
    }

    /**
     * Utility function to bring up a generic "error" dialog.
     */
    private void showErrorDialog(CharSequence msg) {
        Log.i(this, "Show Dialog: " + msg);

        dismissPendingDialogs();

        mDialog = new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDialogDismissed();
                    }})
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onDialogDismissed();
                    }})
                .create();

        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mDialog.show();
    }

    private void onDialogDismissed() {
        mDialog = null;
        /// M: [@Modification for finishing Transparent InCall Screen if necessary]
        /// Fix ALPS02012202. Finish activity and no need show transition animation.@{
        dismissInCallActivityIfNecessary();
        /// @}
        InCallPresenter.getInstance().onDismissDialog();
    }

    /// --------------------------------Mediatek----------------------------------------------

    /// M: For Recording @{
    public void showStorageFullDialog(final int resid, final boolean isSDCardExist) {
        Log.d(this, "showStorageDialog... ");
        dismissPendingDialogs();

        CharSequence msg = getResources().getText(resid);

        // create the clicklistener and cancel listener as needed.
        OnCancelListener cancelListener = new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        };

        DialogInterface.OnClickListener cancelClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d(this, "showStorageDialog... , on click, which=" + which);
                if (null != mDialog) {
                    mDialog.dismiss();
                }
            }
        };

        CharSequence cancelButtonText = isSDCardExist ? getResources().getText(
                R.string.alert_dialog_dismiss) : getResources().getText(android.R.string.ok);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this).setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getText(R.string.reminder))
                .setNegativeButton(cancelButtonText, cancelClickListener)
                .setOnCancelListener(cancelListener)
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        onDialogDismissed();
                    }
                });

        if (isSDCardExist) {
            DialogInterface.OnClickListener oKClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(this, "showStorageDialog... , on click, which=" + which);
                    if (null != mDialog) {
                        mDialog.dismiss();
                    }
                    // To Setting Storage
                    Intent intent = new Intent(PhoneRecorderUtils.STORAGE_SETTING_INTENT_NAME);
                    startActivity(intent);
                }
            };
            dialogBuilder.setPositiveButton(
                    getResources().getText(R.string.change_my_pic), oKClickListener);
        }

        mDialog = dialogBuilder.create();
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mDialog.show();
    }
    /// @}

    private void setWindowFlag() {
        // set this flag so this activity will stay in front of the keyguard
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        Call call = CallList.getInstance().getActiveOrBackgroundCall();
        if (call != null && Call.State.isConnectingOrConnected(call.getState())) {
            // While we are in call, the in-call screen should dismiss the keyguard.
            // This allows the user to press Home to go directly home without going through
            // an insecure lock screen.
            // But we do not want to do this if there is no active call so we do not
            // bypass the keyguard if the call is not answered or declined.

            /// M: DM lock@{
            if (!InCallUtils.isDMLocked()) {
                flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
                Log.d(this, "set window FLAG_DISMISS_KEYGUARD flag ");
            }
            /// @}
        }

        final WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags |= flags;
        getWindow().setAttributes(lp);
    }

    /**
     * Dismiss select account dialog when there has incoming call.
     */
    public void dismissSelectAccountDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(
                "selectAccount");
        Log.d(this, "dismissSelectAccountDialog(), fragment is " + fragment);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    /***
     * M: [@Modification for finishing Transparent InCall Screen if necessary]
     * add for resolving finish incall activity issue. @{
     */
    private boolean mIsLunchedAccountSelectDlg = false;
    /***
     * @}
     */

    /**
     * M: [@Modification for finishing Transparent InCall Screen if necessary]
     * Finish Incall activity if the current incall-activity is transparent after phone account dialog exit:
     * 1. After select card, Telecom cancel this call due to call amount is full;
     * 2. After select card, Telephony cancel this call due to checking CellConnMgr failure.
     * 3. Dial a Ipcall after select some account but without IP Prefix;
     * 4. After dialing number, but back from account selection dialog, call out without account;
     * 5. MMI execution fail or succeed after select account.
     * 6. ECC Call[ALPS02063322] will cancel ACTIVE Call, but not to finish incall screen.
     * 7. Call error[ALPS02029221] will cancel the current call, but not to finish incall screen.
     */
    private void dismissInCallActivityIfNecessary() {
        // / Fix ALPS01992679.
        // Sometimes, second call can not select account because activity will
        // been finished
        // when first call disconnected. So in this case, no need finish
        // InCallActivity.
        boolean hasPreDialWaitCall = CallList.getInstance().getWaitingForAccountCall() != null;
        Log.d(this, "[dismissInCallActivityIfNecessary] mIsLunchedAccountSelectDlg:" + mIsLunchedAccountSelectDlg
                + " hasPreDialWaitCall:" + hasPreDialWaitCall);
        if (mIsLunchedAccountSelectDlg && (CallList.getInstance().getIncomingCall() == null) && !isFinishing()
                && !hasPreDialWaitCall) {
            Log.d(this, "[dismissInCallActivityIfNecessary], finish activity if necessary for transparent"
                    + " account incallactivity.");
            finish();
            overridePendingTransition(0, 0);
            return;
        }

    }

    /**
     * M: [ALPS02025119]The InCallActivity is transparent, so that when the VoLTE
     * conference invitation dialog Activity appears, the AMS will change its
     * background to Launcher. We should change the InCallActivity to non-transparent
     * when the conference manager appears.
     */
    private void changeToTransparent(boolean transparent) {
        if (transparent) {
            convertToTranslucent(null, null);
        } else {
            convertFromTranslucent();
        }
    }
    

    // BEGIN BOWAY, yulong.tan, 20150505, InCall UI.
    /**
    * To determine whether Simple system is running
    * @param context
    * @param packageName
    * @return 
    */
    public void isRunning(Context context, String packageName) {
        isSimpleSystem = false;
        if(packageName.equals(getLauncherPackageName(context))) {
            isSimpleSystem = true;
        }
    }
    
    /**
     * get default launcher from system
     * @param context
     * @return
     */
    public String getLauncherPackageName(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo == null) {
            return null;
        }
        if (res.activityInfo.packageName.equals("android")) {
            return null;
        } else {
            return res.activityInfo.packageName;
        }
    }
    
    /*public Bitmap blur(Bitmap bg, Context context) {
        Bitmap bitmap = bg.copy(bg.getConfig(), true);
        float radius = 20;
        final RenderScript rs = RenderScript.create(context);
        final Allocation input = Allocation.createFromBitmap(rs, bg, Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blur.setRadius(radius);
        blur.setInput(input);
        blur.forEach(output);
        output.copyTo(bitmap);
        rs.destroy();
        return bitmap;
    }*/
    
    private void acquireWakeLock(Context context) {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            //BEGIN BOWAY,huiwen.zhou,20150813,modify newWakeLock tag
            /*
			mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                       "handleAudioEvent");
              */
			mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                       "InCallActivity");
			//END BOWAY,huiwen.zhou.
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    // END BOWAY, yulong.tan
    
    
   // BEGIN BOWAY, kailong.liu, 20150627, WindowLock UI.
	 private void setFullScreen() {
		  getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				  WindowManager.LayoutParams.FLAG_FULLSCREEN);
	 }

	 private void quitFullScreen() {
		  final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		  attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		  getWindow().setAttributes(attrs);
		  getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	 }
	 // END BOWAY, kailong.liu

    // BEGIN BOWAY, yulong.tan, 20151216, To prevent memory leaks.
    private void releaseResource() {
        mCallButtonFragment = null;
        mCallCardFragment = null;
        mAnswerFragment = null;
        mWindowAnswerFragment = null;
        mWakeLock = null;
        mContext = null;
        mSimpleAnswerFragment = null;
        mDialpadFragment = null;
        mConferenceManagerFragment = null;
        mChildFragmentManager = null;
        mPostCharDialogfragment = null;
        mDialog = null;
        mDtmfText = null;
        mShowPostCharWaitDialogCallId = null;
        mShowPostCharWaitDialogChars = null;
        mSlideIn = null;
        mSlideOut = null;
        pm = null;
        mDisconnectCauseDescription = null;
        mDMLockReceiver = null;
    }
    // END BOWAY, yulong.tan
}
