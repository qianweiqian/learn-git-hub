/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telecom.DisconnectCause;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.contacts.common.util.MaterialColorMapUtils.MaterialPalette;
import com.android.contacts.common.widget.FloatingActionButtonController;
import com.android.incallui.service.PhoneNumberService;
import com.android.phone.common.animation.AnimUtils;
import com.mediatek.incallui.ext.ExtensionManager;

import java.util.List;

// BEGIN BOWAY, yinling.zhu, 20150702, wake up when call is disconnected.
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
// END BOWAY, yinling.zhu
// BEGIN BOWAY, wanjun.ouyang, 20150513, for add disconnect call vibrator.
import android.os.Vibrator;
// END BOWAY

//BEGIN BOWAY get new location from TMS tencent 2015 9 16 weiqiang.qian
import tmsdk.common.module.location.LocationManager;
import tmsdk.common.creator.ManagerCreatorC;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import tmsdk.common.ErrorCode;
import tmsdk.common.module.numbermarker.INumQueryRetListener;
import tmsdk.common.module.numbermarker.NumMarkerManager;
import tmsdk.common.module.numbermarker.NumQueryReq;
import tmsdk.common.module.numbermarker.NumQueryRet;
import tmsdk.common.module.numbermarker.NumberMarkEntity;
import java.util.ArrayList;
import java.util.List;
import android.text.Html;
import android.content.ContentValues;
//END BOWAY 2015 9 16

/**
 * Fragment for call card.
 */
public class CallCardFragment extends BaseFragment<CallCardPresenter, CallCardPresenter.CallCardUi>
        implements CallCardPresenter.CallCardUi {

    private AnimatorSet mAnimatorSet;
    private int mRevealAnimationDuration;
    private int mShrinkAnimationDuration;
    private int mFabNormalDiameter;
    private int mFabSmallDiameter;
    private boolean mIsLandscape;
    private boolean mIsDialpadShowing;

    // Primary caller info
    private TextView mPhoneNumber;
    private TextView mNumberLabel;
    private TextView mPrimaryName;
    private View mCallStateButton;
    private ImageView mCallStateIcon;
    private ImageView mCallStateVideoCallIcon;
    private TextView mCallStateLabel;
    private TextView mCallTypeLabel;
    private View mCallNumberAndLabel;
    private ImageView mPhoto;
    private TextView mElapsedTime;
    private Drawable mPrimaryPhotoDrawable;

    // Container view that houses the entire primary call card, including the call buttons
    private View mPrimaryCallCardContainer;
    // Container view that houses the primary call information
    private ViewGroup mPrimaryCallInfo;
    private View mCallButtonsContainer;

    private View mOtherCallInfo;
    // Secondary caller info
    private CallInfoView mSecondaryCallInfo;
    private TextView mSecondaryCallName;
    private View mSecondaryCallProviderInfo;
    private TextView mSecondaryCallProviderLabel;
    private View mSecondaryCallConferenceCallIcon;
    private View mProgressSpinner;

    // Third caller info
    private CallInfoView mThirdCallInfo;
    private View mManageConferenceCallButton;

    // Dark number info bar
    private TextView mInCallMessageLabel;

    // BEGIN BOWAY, yulong.tan, 20150505, InCall UI.
    /*private FloatingActionButtonController mFloatingActionButtonController;
    private View mFloatingActionButtonContainer;
    private ImageButton mFloatingActionButton;*/
    private ImageButton mEndCallButton;
    private TextView mSimIndicator;
    private TextView mLocation;
    private TextView mIncomingName;
    private TextView mIncomingLocation;
    private RelativeLayout mIncomingCallInfo;
    // END BOWAY, yulong.tan

    // BEGIN BOWAY, yulong.tan, 20150630, InCall UI.
    private LinearLayout mCallButtonArea;
    // END BOWAY, yulong.tan
     
    //BEGIN BOWAY get new location from TMS tencent 2015 9 16 weiqiang.qian
    private LocationManager mLocationManager;
    private TextView mMarkLable;
    private NumMarkerManager mNumMarkerManager;
    private    String mark;
    private ContentResolver resolver;
    private boolean marked = false;
    private boolean isCheat = false;
    private int  markID = -1;
	private Drawable cheatPhoto;
    //END BOWAY 2015 9 16
   
    private int mFloatingActionButtonVerticalOffset;

    // Cached DisplayMetrics density.
    private float mDensity;

    private float mTranslationOffset;
    private Animation mPulseAnimation;

    private int mVideoAnimationDuration;

    private MaterialPalette mCurrentThemeColors;
    /// M: For second/third call color @{
    private int mCurrentSecondCallColor;
    private int mCurrentThirdCallColor;
    /// @}

    /// M: recording indication icon
    private ImageView mVoiceRecorderIcon;

//BEGIN BOWAY ADD FOR number mark weiqiang.qian 2015 11 04
    private Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case 0:
                markID = -1;
                boolean markSuccess = false;
                List<NumQueryRet> retList = (List<NumQueryRet>)msg.obj;
                if(retList != null){
                StringBuilder strBuilder = new StringBuilder();
                String result = null;
                String resultNumber = "";
                for(NumQueryRet item : retList){        
                 if(item.tagType == 51)
                 {     
                     markSuccess = true;
                     markID = 3;
                     result = item.tagCount + mark+" " + mNumMarkerManager.getTagName(item.tagType);
                     resultNumber = item.number;
                     strBuilder.append(item.tagCount + mark+" " + mNumMarkerManager.getTagName(item.tagType));
                 } else if (item.tagType == 53)
                 {   markSuccess = true;
                     markID = 2;
                     resultNumber = item.number;
                     result = item.tagCount + mark+" " + mNumMarkerManager.getTagName(item.tagType);
                     strBuilder.append(item.tagCount + mark+" " + mNumMarkerManager.getTagName(item.tagType));
                 } else if (item.tagType == 54)
                 {   
                     markSuccess = true;
                     isCheat = true;
                     markID = 1;
                     result = item.tagCount + mark+" "+"<font color='#cc0000'>" + mNumMarkerManager.getTagName(item.tagType)+"</font>";
                     resultNumber = item.number;
                 } else if (item.tagType == 55)
                 {   
                     markSuccess = true;
                     strBuilder.append(item.tagCount + mark+" " + mNumMarkerManager.getTagName(item.tagType));
                 } else if (item.tagType == 56)
                 {
                     markSuccess = true;
                     strBuilder.append(item.tagCount + mark+" "+ mNumMarkerManager.getTagName(item.tagType));
                 } else if (item.tagType == 50)
                         {  
                     markSuccess = true;
                     result = item.tagCount + mark+" "+"<font color='#cc0000'>"+ mNumMarkerManager.getTagName(item.tagType)+"</font>";
                        resultNumber = item.number;
                     } else if (item.tagType == 52)
                     {
                     markSuccess = true;
                         strBuilder.append(item.tagCount + mark+" "+ mNumMarkerManager.getTagName(item.tagType));
                     } else {
                     markSuccess = false;
                 }     
              }
                if (markSuccess) {
                mMarkLable.setVisibility(View.VISIBLE);
                     if(result == null) {
                mMarkLable.setText(strBuilder.toString());
                     } else {
                //BOWAY set photo for cheat number weiqiang.qian 2015 12 01
                if(!marked && markID == 1) {
                            if(cheatPhoto != null)
                            setDrawableToImageView(mPhoto, cheatPhoto);
                }
                //END BOWAY
                            mMarkLable.setText(Html.fromHtml(result));
                            if(!marked && markID > 0) {
                            ContentValues values = new ContentValues();
                            values.put("number", resultNumber+"");
                            values.put("mid",markID);
                            Uri markRecordUri = Uri.parse("content://com.boway.securitycenter.arescontentprovider/MarkRecord"); 
                            boolean hasFound = false;
                Cursor markCur = resolver.query(markRecordUri,new String[]{"_id"}, "number=? AND ("  
                           + "mid=?"+")",new String[]{resultNumber+"",markID + ""},null);
               if(markCur != null && markCur.getCount() > 0)
               {
                               hasFound = true;
               }
                     markCur.close();
                           if(!hasFound) {
                               resolver.insert(markRecordUri, values);
                           }
                           marked = true;
                           }
                     }
                    }
            }
            break;
          }
        }
        
    };
//END BOWAY ADD FOR number mark weiqiang.qian 2015 11 04
    
    // BEGIN BOWAY, wanjun.ouyang, 20150526, for add disconnect call vibrator.
      private Vibrator mVibrator;
      private static final int SCREEN_SHOT_VIBRATE_TIME = 100;
      // END BOWAY
      
    // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        private static final int TOUCH_SWITCH = 10001;
        private static final int TOUCH_SWITCH_DELAY = 2000;
    
      private TextView mWindowName;
        private TextView mWindowNumber;
        private TextView mWindowElapsedTime;
        private RelativeLayout mWindowPrimaryCallInfoFrame;
        private FrameLayout mPrimaryCallInfoFrame;
        private boolean isWinodwLock = true;
        //private SlideView mSlideView; // MODIFY BOWAY, yulong.tan, 20151217, Remove SlideView, to prevent memory leaks.
        private boolean mIncalling = false;
        private SetTouchSwitch mSts;
        Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
              case TOUCH_SWITCH:
                 if(SystemProperties.get(InCallActivity.KEY_LID_CLOSED).equals("true")){
                      mSts.enableTouch();
                 }else{
                      mSts.disenableTouch();
                 }
                 break;

              default:
                 break;
          }
        }
        };
    
      // END BOWAY, kailong.liu
      
    @Override
    CallCardPresenter.CallCardUi getUi() {
        return this;
    }

    @Override
    CallCardPresenter createPresenter() {
        return new CallCardPresenter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRevealAnimationDuration = getResources().getInteger(R.integer.reveal_animation_duration);
        mShrinkAnimationDuration = getResources().getInteger(R.integer.shrink_animation_duration);
        mVideoAnimationDuration = getResources().getInteger(R.integer.video_animation_duration);
        mFloatingActionButtonVerticalOffset = getResources().getDimensionPixelOffset(
                R.dimen.floating_action_bar_vertical_offset);
        mFabNormalDiameter = getResources().getDimensionPixelOffset(
                R.dimen.end_call_floating_action_button_diameter);
        mFabSmallDiameter = getResources().getDimensionPixelOffset(
                R.dimen.end_call_floating_action_button_small_diameter);
        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        mSts = new SetTouchSwitch();
        // END BOWAY

        //BEGIN BOWAY get new location from TMS tencent 2015 9 16 weiqiang.qian 
        mLocationManager = ManagerCreatorC.getManager(LocationManager.class);
        mNumMarkerManager = ManagerCreatorC.getManager(NumMarkerManager.class);
        resolver = getActivity().getContentResolver();
        //END  BOWAY
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final CallList calls = CallList.getInstance();
        final Call call = calls.getFirstCall();
        getPresenter().init(getActivity(), call);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mDensity = getResources().getDisplayMetrics().density;
        mTranslationOffset =
                getResources().getDimensionPixelSize(R.dimen.call_card_anim_translate_y_offset);

        return inflater.inflate(InCallActivity.isSimpleSystem ? R.layout.simple_call_card_content : R.layout.call_card_content, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPulseAnimation =
                AnimationUtils.loadAnimation(view.getContext(), R.anim.call_status_pulse);

        mPhoneNumber = (TextView) view.findViewById(R.id.phoneNumber);
        mPrimaryName = (TextView) view.findViewById(R.id.name);
        mNumberLabel = (TextView) view.findViewById(R.id.label);
        mOtherCallInfo = view.findViewById(R.id.other_call_info_container);
        mSecondaryCallInfo = (CallInfoView) view.findViewById(R.id.secondary_call_info);
        mSecondaryCallProviderInfo = view.findViewById(R.id.secondary_call_provider_info);
        mThirdCallInfo = (CallInfoView) view.findViewById(R.id.third_call_info);
        mPhoto = (ImageView) view.findViewById(R.id.photo);
        // BEGIN BOWAY, yulong.tan, 20150506, InCall UI.
        /*mCallStateIcon = (ImageView) view.findViewById(R.id.callStateIcon);*/
        /*mCallStateVideoCallIcon = (ImageView) view.findViewById(R.id.videoCallIcon);*/
        // END BOWAY, yulong.tan
        // BEGIN BOWAY, yulong.tan, 20150506, InCall UI.
        mCallButtonArea = (LinearLayout) view.findViewById(R.id.callButtonArea);
        // END BOWAY, yulong.tan
        mCallStateLabel = (TextView) view.findViewById(R.id.callStateLabel);
        mCallNumberAndLabel = view.findViewById(R.id.labelAndNumber);
        mCallTypeLabel = (TextView) view.findViewById(R.id.callTypeLabel);
        mElapsedTime = (TextView) view.findViewById(R.id.elapsedTime);
        mPrimaryCallCardContainer = view.findViewById(R.id.primary_call_info_container);
        mPrimaryCallInfo = (ViewGroup) view.findViewById(R.id.primary_call_banner);
        mCallButtonsContainer = view.findViewById(R.id.callButtonFragment);
        mInCallMessageLabel = (TextView) view.findViewById(R.id.connectionServiceMessage);
        mProgressSpinner = view.findViewById(R.id.progressSpinner);

        // BEGIN BOWAY, yulong.tan, 20150505, InCall UI.
        mSimIndicator = (TextView) view.findViewById(R.id.simIndicator);
        mLocation = (TextView) view.findViewById(R.id.location);
        //BEGIN BOWAY,weiqiang.qian,2015 11 2 set Lable
        mMarkLable = (TextView) view.findViewById(R.id.mark_label);
		cheatPhoto = getResources().getDrawable(R.drawable.ic_cheat_mark);
        //END BOWAY
        if (InCallActivity.isSimpleSystem) {
            mIncomingName = (TextView) view.findViewById(R.id.incomingName);
            mIncomingLocation = (TextView) view.findViewById(R.id.incomingLocation);
            mIncomingCallInfo = (RelativeLayout) view.findViewById(R.id.incoming_call_banner);
        }
       
        // END BOWAY, yulong.tan
        
        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        isWinodwLock = SystemProperties.get(InCallActivity.KEY_LID_CLOSED).equals("true");
        mWindowPrimaryCallInfoFrame = (RelativeLayout) view.findViewById(R.id.window_primary_call_frame);
        mPrimaryCallInfoFrame = (FrameLayout) view.findViewById(R.id.primary_call_frame);
        if(isWinodwLock){
                mWindowPrimaryCallInfoFrame.setVisibility(View.VISIBLE);
                mPrimaryCallInfoFrame.setVisibility(View.GONE);
          }else {
                mWindowPrimaryCallInfoFrame.setVisibility(View.GONE);
                mPrimaryCallInfoFrame.setVisibility(View.VISIBLE);        
          }
                  
                mWindowName = (TextView) view.findViewById(R.id.window_name);
                mWindowNumber = (TextView) view.findViewById(R.id.window_number);
                mWindowElapsedTime = (TextView) view.findViewById(R.id.window_elapsedTime);
                Typeface typeface = Typeface.createFromAsset(view.getContext().getAssets(), "fonts/Date.ttf");
                mWindowName.setTypeface(typeface);
                mWindowNumber.setTypeface(typeface);
                mWindowElapsedTime.setTypeface(typeface);
        
        // BEGIN BOWAY, yulong.tan, 20150425, InCall UI.
        /*mFloatingActionButtonContainer = view.findViewById(
                R.id.floating_end_call_action_button_container);
        mFloatingActionButton = (ImageButton) view.findViewById(
                R.id.floating_end_call_action_button);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().endCallClicked();
            }
        });*/
        /*mSlideView = (SlideView) view.findViewById(R.id.slider);
        mSlideView.setSlideListener(new SlideView.SlideListener() {
            @Override
            public void onDone() {
                getPresenter().endCallClicked();
                mSlideView.setVisibility(View.GONE);
            }
        });*/ // MODIFY BOWAY, yulong.tan, 20151217, Remove SlideView, to prevent memory leaks.
        // END BOWAY, kailong.liu
        
        
        mEndCallButton = (ImageButton)view.findViewById(R.id.end_call_button);
        mEndCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().endCallClicked();
            }
        });
        /*mFloatingActionButtonController = new FloatingActionButtonController(getActivity(),
                mFloatingActionButtonContainer, mFloatingActionButton);*/
        // END BOWAY, yulong.tan

        mSecondaryCallInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().secondaryInfoClicked();
                updateFabPositionForSecondaryCallInfo();
            }
        });

        mThirdCallInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().thirdInfoClicked();
            }
        });

        mCallStateButton = view.findViewById(R.id.callStateButton);
        mCallStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().onCallStateButtonTouched();
            }
        });

        mManageConferenceCallButton = view.findViewById(R.id.manage_conference_call_button);
        mManageConferenceCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(this, "[onClick] mManageConferenceCallButton.onclick func.");
                InCallActivity activity = (InCallActivity) getActivity();
                activity.showConferenceCallManager(true);
            }
        });

        mPrimaryName.setElegantTextHeight(false);
        mCallStateLabel.setElegantTextHeight(false);

        //add for plug in. @{
        ExtensionManager.getCallCardExt().onViewCreated(InCallPresenter.getInstance().getContext(), view);
        ExtensionManager.getRCSeCallCardExt().onViewCreated(InCallPresenter.getInstance().getContext(), view);
        //add for plug in. @}

        /// M: Add for recording.
        initVoiceRecorderIcon(view);
    }

    @Override
    public void setVisible(boolean on) {
        if (on) {
            getView().setVisibility(View.VISIBLE);
        } else {
            getView().setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Hides or shows the progress spinner.
     *
     * @param visible {@code True} if the progress spinner should be visible.
     */
    @Override
    public void setProgressSpinnerVisible(boolean visible) {
        mProgressSpinner.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Sets the visibility of the primary call card.
     * Ensures that when the primary call card is hidden, the video surface slides over to fill the
     * entire screen.
     *
     * @param visible {@code True} if the primary call card should be visible.
     */
    @Override
    public void setCallCardVisible(final boolean visible) {
        // When animating the hide/show of the views in a landscape layout, we need to take into
        // account whether we are in a left-to-right locale or a right-to-left locale and adjust
        // the animations accordingly.
        final boolean isLayoutRtl = InCallPresenter.isRtl();

        // Retrieve here since at fragment creation time the incoming video view is not inflated.
        final View videoView = getView().findViewById(R.id.incomingVideo);

        // Determine how much space there is below or to the side of the call card.
        final float spaceBesideCallCard = getSpaceBesideCallCard();

        // We need to translate the video surface, but we need to know its position after the layout
        // has occurred so use a {@code ViewTreeObserver}.
        final ViewTreeObserver observer = getView().getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // We don't want to continue getting called.
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(this);
                }

                float videoViewTranslation = 0f;

                // Translate the call card to its pre-animation state.
                if (mIsLandscape) {
                    float translationX = mPrimaryCallCardContainer.getWidth();
                    translationX *= isLayoutRtl ? 1 : -1;

                    mPrimaryCallCardContainer.setTranslationX(visible ? translationX : 0);

                    if (visible) {
                        videoViewTranslation = videoView.getWidth() / 2 - spaceBesideCallCard / 2;
                        videoViewTranslation *= isLayoutRtl ? -1 : 1;
                    }
                } else {
                    mPrimaryCallCardContainer.setTranslationY(visible ?
                            -mPrimaryCallCardContainer.getHeight() : 0);

                    if (visible) {
                        videoViewTranslation = videoView.getHeight() / 2 - spaceBesideCallCard / 2;
                    }
                }

                // Perform animation of video view.
                ViewPropertyAnimator videoViewAnimator = videoView.animate()
                        .setInterpolator(AnimUtils.EASE_OUT_EASE_IN)
                        .setDuration(mVideoAnimationDuration);
                if (mIsLandscape) {
                    videoViewAnimator
                            .translationX(videoViewTranslation)
                            .start();
                } else {
                    videoViewAnimator
                            .translationY(videoViewTranslation)
                            .start();
                }
                videoViewAnimator.start();

                // Animate the call card sliding.
                ViewPropertyAnimator callCardAnimator = mPrimaryCallCardContainer.animate()
                        .setInterpolator(AnimUtils.EASE_OUT_EASE_IN)
                        .setDuration(mVideoAnimationDuration)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (!visible) {
                                    mPrimaryCallCardContainer.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                if (visible) {
                                    mPrimaryCallCardContainer.setVisibility(View.VISIBLE);
                                }
                            }
                        });

                if (mIsLandscape) {
                    float translationX = mPrimaryCallCardContainer.getWidth();
                    translationX *= isLayoutRtl ? 1 : -1;
                    callCardAnimator
                            .translationX(visible ? 0 : translationX)
                            .start();
                } else {
                    callCardAnimator
                            .translationY(visible ? 0 : -mPrimaryCallCardContainer.getHeight())
                            .start();
                }

                return true;
            }
        });
    }

    /**
     * Determines the amount of space below the call card for portrait layouts), or beside the
     * call card for landscape layouts.
     *
     * @return The amount of space below or beside the call card.
     */
    public float getSpaceBesideCallCard() {
        if (mIsLandscape) {
            return getView().getWidth() - mPrimaryCallCardContainer.getWidth();
        } else {
            return getView().getHeight() - mPrimaryCallCardContainer.getHeight();
        }
    }

    @Override
    public void setPrimaryName(String name, boolean nameIsNumber) {
        // BEGIN BOWAY, yulong.tan, 20150508, InCall UI.
        if (TextUtils.isEmpty(name)) {
            mPrimaryName.setText(null);
            // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
            mWindowName.setText(null);
            if (InCallActivity.isSimpleSystem) {
                mIncomingName.setText(null);
            }
        } else {
                      mWindowName.setText(nameIsNumber
                    ? PhoneNumberUtils.ttsSpanAsPhoneNumber(name)
                    : name);
            mWindowName.setTextSize(nameIsNumber ? 25 : 35);
            // END BOWAY, kailong.liu
                    
            mPrimaryName.setText(nameIsNumber
                    ? PhoneNumberUtils.ttsSpanAsPhoneNumber(name)
                    : name);
            if (InCallActivity.isSimpleSystem) {
                mIncomingName.setText(nameIsNumber
                    ? PhoneNumberUtils.ttsSpanAsPhoneNumber(name)
                    : name);
            }

            // Set direction of the name field
            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            mPrimaryName.setTextDirection(nameDirection);
            if (InCallActivity.isSimpleSystem) {
                mIncomingName.setTextDirection(nameDirection);
            }
        }
        // END BOWAY, yulong.tan
    }

    @Override
    public void setPrimaryImage(Drawable image) {
        if (image != null) {
            setDrawableToImageView(mPhoto, image);
        }
    }

    //BEGIN BOWAY,huiwen.zhou,20150810,delete the window_lock location
    public void setPrimaryPhoneNumberEx(boolean nameIsNumber,String number) {
        // Set the number
        if (TextUtils.isEmpty(number)) {
            mPhoneNumber.setText(null);
            mWindowNumber.setText(null);
            mPhoneNumber.setVisibility(View.GONE);
            mWindowNumber.setVisibility(View.GONE);
        } else {
            mPhoneNumber.setText(PhoneNumberUtils.ttsSpanAsPhoneNumber(number));
            if(nameIsNumber){
               mWindowNumber.setText("");
            }else{
               mWindowNumber.setText(PhoneNumberUtils.ttsSpanAsPhoneNumber(number));
            }
            mPhoneNumber.setVisibility(View.VISIBLE);
            mWindowNumber.setVisibility(View.VISIBLE);
            mPhoneNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
            mWindowNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
    }
    //END BOWAY,huiwen.zhou


        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
    @Override
    public void setPrimaryPhoneNumber(String number) {
        // Set the number
        if (TextUtils.isEmpty(number)) {
            mPhoneNumber.setText(null);
            mWindowNumber.setText(null);
            mPhoneNumber.setVisibility(View.GONE);
            mWindowNumber.setVisibility(View.GONE);
        } else {
            mPhoneNumber.setText(PhoneNumberUtils.ttsSpanAsPhoneNumber(number));
            mWindowNumber.setText(PhoneNumberUtils.ttsSpanAsPhoneNumber(number));
            mPhoneNumber.setVisibility(View.VISIBLE);
            mWindowNumber.setVisibility(View.VISIBLE);
            mPhoneNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
            mWindowNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
    }
    // END BOWAY, kailong.liu

    @Override
    public void setPrimaryLabel(String label) {
        if (!TextUtils.isEmpty(label)) {
            mNumberLabel.setText(label);
            mNumberLabel.setVisibility(View.VISIBLE);
        } else {
            mNumberLabel.setVisibility(View.GONE);
        }

    }

    @Override
    public void setPrimary(String number, String name, boolean nameIsNumber, String label,
            // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
            /*Drawable photo, boolean isSipCall) {*/
            Drawable photo, boolean isSipCall, String location) {
            // END BOWAY, yulong.tan
        Log.d(this, "Setting primary call");

        // set the name field.
        //BEGIN BOWAY weiqiang.qian 2015 8 20     
          String bowaySales = getResources().getString(R.string.boway_sales);
        //BEGIN BOWAY weiqiang.qian 2015 8 20    
          if(nameIsNumber)
          {   String check  = name;//PhoneNumberUtils.ttsSpanAsPhoneNumber(number).toString();
              //System.out.println("@@@@@@@@++++++"+"----"+check);
              if(check.contains(" "))
              {
                 if(check.equals("138 2654 9022") || check.equals("138 2654 8382") || check.equals("138 2654 3572") || check.equals("138 2654 3892") || check.equals("400 700 6005")) 
                 {
                     name = bowaySales;
                 }
              }
              else 
              {
                 if(check.equals("13826549022") || check.equals("13826548382") || check.equals("13826543572") || check.equals("13826543892") || check.equals("4007006005")) {
                     name = bowaySales;
                 }
              }
          }
        // END BOWAY, weiqiang.qian 2015 8 20
        setPrimaryName(name, nameIsNumber);

        // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
        /*if (TextUtils.isEmpty(number) && TextUtils.isEmpty(label)) {
            mCallNumberAndLabel.setVisibility(View.GONE);
            mElapsedTime.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        } else {
            mCallNumberAndLabel.setVisibility(View.VISIBLE);
            mElapsedTime.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }*/
        mCallNumberAndLabel.setVisibility(View.GONE);
        // END BOWAY, yulong.tan

        //BEGIN BOWAY,huiwen.zhou,20150810,delete the window_lock location
        //setPrimaryPhoneNumber(number);
        setPrimaryPhoneNumberEx(nameIsNumber,number);
        //END BOWAY,huiwen.zhou.

        // Set the label (Mobile, Work, etc)
        setPrimaryLabel(label);

        showInternetCallLabel(isSipCall);

        setDrawableToImageView(mPhoto, photo);
        //BEGIN BOWAY get new location from TMS tencent 2015 9 16 weiqiang.qian 
        if(nameIsNumber){
        //BEGIN BOWAY FIX EMERGENCY weiqiang.qian 2015 10 24
         mark = getResources().getString(R.string.mark);
        String newNumber = name;
        if(name.contains(" "))
        newNumber = name.replaceAll(" ", "");
        String geocode = mLocationManager.getLocation(newNumber);
        // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
        //  setLocation(location);
        setLocation(geocode);
        final String mNewNumber = newNumber;
        //BEGIN BOWAY weiqiang.qian,2015 11 2 set Lable by cloud check and local check
        NumQueryRet item = mNumMarkerManager.localFetchNumberInfo(mNewNumber);
             if (item == null) {
            //cloud check
            new Thread() {
                @Override
                public void run() {
                    List<NumQueryReq> numbers = new ArrayList<NumQueryReq>();

                    NumQueryReq queryInfo = new NumQueryReq(mNewNumber, NumQueryReq.TYPE_Called);
                    numbers.add(queryInfo);
                    
                    mNumMarkerManager.cloudFetchNumberInfo(numbers,new INumQueryRetListener(){
                        @Override
                        public void onResult(int arg0, List<NumQueryRet> arg1) {
                                        myHandler.obtainMessage(0, arg0, 0, arg1).sendToTarget();
                 
                 }
                    });
                }
            }.start();
        } else {
            StringBuilder strBuilder = new StringBuilder("");
            strBuilder.append(item.tagCount + mark+" "+mNumMarkerManager.getTagName(item.tagType));
            mMarkLable.setVisibility(View.VISIBLE);
            mMarkLable.setText(strBuilder.toString());
        }



        //END BOWAY 
       

        }else{
        //BEGIN BOWAY FIX EMERGENCY weiqiang.qian 2015 10 24
        if(number != null) {
        String newNumber = number;
        if(number.contains(" ")) 
        newNumber = number.replaceAll(" ", "");
        String geocode = mLocationManager.getLocation(newNumber);
        // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
        //  setLocation(location);
        setLocation(geocode);
        } else {
        setLocation("");
        }

     


        }
        // END BOWAY, yulong.tan
        //END BOWAY
        
    }

    /// M: For MTK DSDA Feature. @{
    /* Google code:
    @Override
    public void setSecondary(boolean show, String name, boolean nameIsNumber, String label,
            String providerLabel, boolean isConference) {

        if (show != mSecondaryCallInfo.isShown()) {
            updateFabPositionForSecondaryCallInfo();
        }

        if (show) {
            boolean hasProvider = !TextUtils.isEmpty(providerLabel);
            showAndInitializeSecondaryCallInfo(hasProvider);

            mSecondaryCallConferenceCallIcon.setVisibility(isConference ? View.VISIBLE : View.GONE);

            mSecondaryCallName.setText(nameIsNumber
                    ? PhoneNumberUtils.ttsSpanAsPhoneNumber(name)
                    : name);
            if (hasProvider) {
                mSecondaryCallProviderLabel.setText(providerLabel);
            }

            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            mSecondaryCallName.setTextDirection(nameDirection);
        } else {
            mSecondaryCallInfo.setVisibility(View.GONE);
        }
    }
    */

    @Override
    public void setSecondary(boolean show, String name, boolean nameIsNumber, String label,
            String providerLabel, boolean isConference, boolean isIncoming) {

        if (show != mSecondaryCallInfo.isShown()) {
            updateFabPositionForSecondaryCallInfo();
        }

        if (show) {
            mOtherCallInfo.setVisibility(View.VISIBLE);
            int providerColor = getPresenter().getSecondCallColor();
            // M: add for OP09 plug in @{
            if (ExtensionManager.getCallCardExt().shouldShowCallAccountIcon()) {
                if (null == providerLabel) {
                    providerLabel = ExtensionManager.getCallCardExt().getSecondCallProviderLabel();
                }
                mSecondaryCallInfo.mCallProviderIcon.setVisibility(View.VISIBLE);
                mSecondaryCallInfo.mCallProviderIcon.setImageBitmap(ExtensionManager.getCallCardExt()
                        .getSecondCallPhoneAccountBitmap());
            }
            // add for OP09 plug in @}
            updateCallInfoView(mSecondaryCallInfo, name, nameIsNumber, label, providerLabel,
                    providerColor, isConference, isIncoming);
            mCurrentSecondCallColor = providerColor;
        } else {
            mSecondaryCallInfo.setVisibility(View.GONE);
            mOtherCallInfo.setVisibility(View.GONE);
        }

        // Need update AnswerFragment bottom padding when there
        // has another incoming call.
        updateAnswerFragmentBottomPadding();
    }

    @Override
    public void setThird(boolean show, String name, boolean nameIsNumber, String label,
            String providerLabel, boolean isConference) {

        if (show != mThirdCallInfo.isShown()) {
            updateFabPositionForSecondaryCallInfo();
        }

        if (show) {
            int providerColor = getPresenter().getThirdCallColor();
            // M: add for OP09 plug in @{
            if (ExtensionManager.getCallCardExt().shouldShowCallAccountIcon()) {
                if (null == providerLabel) {
                    providerLabel = ExtensionManager.getCallCardExt().getThirdCallProviderLabel();
                }
                mThirdCallInfo.mCallProviderIcon.setVisibility(View.VISIBLE);
                mThirdCallInfo.mCallProviderIcon.setImageBitmap(ExtensionManager.getCallCardExt()
                        .getThirdCallPhoneAccountBitmap());
            }
            // add for OP09 plug in @}
            updateCallInfoView(mThirdCallInfo, name, nameIsNumber, label, providerLabel,
                    providerColor, isConference, false);
            mCurrentThirdCallColor = providerColor;
        } else {
            mThirdCallInfo.setVisibility(View.GONE);
        }
    }

    private void updateCallInfoView(CallInfoView callInfoView, String name, boolean nameIsNumber,
            String label, String providerLabel, int providerColor, boolean isConference, boolean isIncoming) {
        // Initialize CallInfo view.
        callInfoView.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(providerLabel)) {
            callInfoView.mCallProviderInfo.setVisibility(View.VISIBLE);
            callInfoView.mCallProviderLabel.setText(providerLabel);
            callInfoView.mCallProviderLabel.setTextColor(providerColor);
        }

        callInfoView.mCallConferenceCallIcon.setVisibility(isConference ? View.VISIBLE : View.GONE);

        callInfoView.mCallName.setText(nameIsNumber ? PhoneNumberUtils.ttsSpanAsPhoneNumber(name)
                : name);
        int nameDirection = View.TEXT_DIRECTION_INHERIT;
        if (nameIsNumber) {
            nameDirection = View.TEXT_DIRECTION_LTR;
        }
        callInfoView.mCallName.setTextDirection(nameDirection);

        int resId = isIncoming ? R.string.card_title_incoming_call : R.string.onHold;
        callInfoView.mCallStatus.setText(getView().getResources().getString(resId));
    }
    /// @}

    @Override
    public void setCallState(
            int state,
            int videoState,
            int sessionModificationState,
            DisconnectCause disconnectCause,
            String connectionLabel,
            Drawable callStateIcon,
            String gatewayNumber) {
        boolean isGatewayCall = !TextUtils.isEmpty(gatewayNumber);
        CharSequence callStateLabel = getCallStateLabelFromState(state, videoState,
                sessionModificationState, disconnectCause, connectionLabel, isGatewayCall);

        Log.v(this, "setCallState " + callStateLabel);
        Log.v(this, "DisconnectCause " + disconnectCause.toString());
        Log.v(this, "gateway " + connectionLabel + gatewayNumber);

        if ((TextUtils.equals(callStateLabel, mCallStateLabel.getText()))
                /// M: For ALPS02036232, add this filter then can update
                // callstateIcon if icon changed. @{
                // BEGIN BOWAY, yulong.tan, 20150707, InCall UI.
                //&& isCallStateIconUnChanged(callStateIcon)) {
                ) {
                // END BOWAY, yulong.tan
                /// @}
            // Nothing to do if the labels are the same
            return;
        }

        // Update the call state label and icon.
        if (!TextUtils.isEmpty(callStateLabel)) {
            mCallStateLabel.setText(callStateLabel);
            mCallStateLabel.setAlpha(1);
            mCallStateLabel.setVisibility(View.VISIBLE);
            // BEGIN BOWAY, yulong.tan, 20150506, InCall UI.
            if (state == Call.State.ACTIVE || state == Call.State.CONFERENCED) {
                mCallStateLabel.clearAnimation();
            } else {
                mCallStateLabel.startAnimation(mPulseAnimation);
            }
        } else {
            Animation callStateLabelAnimation = mCallStateLabel.getAnimation();
            if (callStateLabelAnimation != null) {
                callStateLabelAnimation.cancel();
            }
            mCallStateLabel.setText(null);
            mCallStateLabel.setAlpha(0);
            mCallStateLabel.setVisibility(View.GONE);
        }

        /*if (callStateIcon != null) {
            mCallStateIcon.setVisibility(View.VISIBLE);
            // Invoke setAlpha(float) instead of setAlpha(int) to set the view's alpha. This is
            // needed because the pulse animation operates on the view alpha.
            mCallStateIcon.setAlpha(1.0f);
            mCallStateIcon.setImageDrawable(callStateIcon);

            if (state == Call.State.ACTIVE || state == Call.State.CONFERENCED
                    || TextUtils.isEmpty(callStateLabel)) {
                mCallStateIcon.clearAnimation();
            } else {
                mCallStateIcon.startAnimation(mPulseAnimation);
            }

            if (callStateIcon instanceof AnimationDrawable) {
                ((AnimationDrawable) callStateIcon).start();
            }
        } else {
            Animation callStateIconAnimation = mCallStateIcon.getAnimation();
            if (callStateIconAnimation != null) {
                callStateIconAnimation.cancel();
            }

            // Invoke setAlpha(float) instead of setAlpha(int) to set the view's alpha. This is
            // needed because the pulse animation operates on the view alpha.
            mCallStateIcon.setAlpha(0.0f);
            mCallStateIcon.setVisibility(View.GONE);*/
            /**
             * M: [ALPS01841247]Once the ImageView was shown, it would show again even when
             * setVisibility(GONE). This is caused by View system, when complex interaction
             * combined by Visibility/Animation/Alpha. This root cause need further discussion.
             * As a solution, set the drawable to null can fix this specific problem of
             * ALPS01841247 directly.
             */
            /*mCallStateIcon.setImageDrawable(null);
        }

        if (VideoProfile.VideoState.isBidirectional(videoState)
                || (state == Call.State.ACTIVE && sessionModificationState
                        == Call.SessionModificationState.WAITING_FOR_RESPONSE)) {
            mCallStateVideoCallIcon.setVisibility(View.VISIBLE);
        } else {
            mCallStateVideoCallIcon.setVisibility(View.GONE);
        }*/
        // END BOWAY, yulong.tan
        if (state == Call.State.INCOMING) {
            if (callStateLabel != null) {
                getView().announceForAccessibility(callStateLabel);
            }
            if (mPrimaryName.getText() != null) {
                getView().announceForAccessibility(mPrimaryName.getText());
            }
        }
    }

    @Override
    public void setCallbackNumber(String callbackNumber, boolean isEmergencyCall) {
        if (mInCallMessageLabel == null) {
            return;
        }
        /// M: for ALPS02044888, remove the show callback number feature.
        // PhoneAccount sets sub number as address in MTK Turnkey solution for
        // bug fix ALPS01804842, and causes callbackNumber and simNumber may be
        // different when dial ECC before SIM LOADED. We need never show
        // callback number in L1, so remove it and waiting for M release. @{
        callbackNumber = null;
        /// @}
        if (TextUtils.isEmpty(callbackNumber)) {
            mInCallMessageLabel.setVisibility(View.GONE);
            return;
        }

        // TODO: The new Locale-specific methods don't seem to be working. Revisit this.
        callbackNumber = PhoneNumberUtils.formatNumber(callbackNumber);

        int stringResourceId = isEmergencyCall ? R.string.card_title_callback_number_emergency
                : R.string.card_title_callback_number;

        String text = getString(stringResourceId, callbackNumber);
        mInCallMessageLabel.setText(text);

        mInCallMessageLabel.setVisibility(View.VISIBLE);
    }

    private void showInternetCallLabel(boolean show) {
        if (show) {
            final String label = getView().getContext().getString(
                    R.string.incall_call_type_label_sip);
            mCallTypeLabel.setVisibility(View.VISIBLE);
            mCallTypeLabel.setText(label);
        } else {
            mCallTypeLabel.setVisibility(View.GONE);
        }
    }

        // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
    @Override
    public void setPrimaryCallElapsedTime(boolean show, long duration) {
        if (show) {
            if (mElapsedTime.getVisibility() != View.VISIBLE) {
                AnimUtils.fadeIn(mElapsedTime, AnimUtils.DEFAULT_DURATION);
            }
            if (mWindowElapsedTime.getVisibility() != View.VISIBLE){
                    mWindowElapsedTime.setVisibility(View.VISIBLE);
            }
            String callTimeElapsed = DateUtils.formatElapsedTime(duration / 1000);
            String durationDescription = InCallDateUtils.formatDetailedDuration(duration);
            mElapsedTime.setText(callTimeElapsed);
            mWindowElapsedTime.setText(callTimeElapsed);
            mElapsedTime.setContentDescription(durationDescription);
            mWindowElapsedTime.setContentDescription(durationDescription);
        } else {
            // hide() animation has no effect if it is already hidden.
            AnimUtils.fadeOut(mElapsedTime, AnimUtils.DEFAULT_DURATION);
            mWindowElapsedTime.setVisibility(View.GONE);
        }
    }
    // END BOWAY, kailong.liu

    private void setDrawableToImageView(ImageView view, Drawable photo) {
        if (photo == null) {
            photo = ContactInfoCache.getInstance(
                    view.getContext()).getDefaultContactPhotoDrawable();
        }

        if (mPrimaryPhotoDrawable == photo) {
            return;
        }
        mPrimaryPhotoDrawable = photo;
        // BEGIN BOWAY, yulong.tan, 20150625, InCall UI.
        if (photo.getIntrinsicHeight() == photo.getIntrinsicWidth()) {
            photo = toRoundDrawable(photo);
        }
        // END BOWAY, yulong.tan

        final Drawable current = view.getDrawable();
        if (current == null) {
            view.setImageDrawable(photo);
            AnimUtils.fadeIn(mElapsedTime, AnimUtils.DEFAULT_DURATION);
        } else {
            // Cross fading is buggy and not noticable due to the multiple calls to this method
            // that switch drawables in the middle of the cross-fade animations. Just set the
            // photo directly instead.
            view.setImageDrawable(photo);
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Gets the call state label based on the state of the call or cause of disconnect.
     *
     * Additional labels are applied as follows:
     *         1. All outgoing calls with display "Calling via [Provider]".
     *         2. Ongoing calls will display the name of the provider.
     *         3. Incoming calls will only display "Incoming via..." for accounts.
     *         4. Video calls, and session modification states (eg. requesting video).
     */
    private CharSequence getCallStateLabelFromState(int state, int videoState,
            int sessionModificationState, DisconnectCause disconnectCause, String label,
            boolean isGatewayCall) {
        final Context context = getView().getContext();
        CharSequence callStateLabel = null;  // Label to display as part of the call banner

        boolean isSpecialCall = label != null;
        boolean isAccount = isSpecialCall && !isGatewayCall;

                // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
        switch  (state) {
            case Call.State.IDLE:
                // "Call state" is meaningless in this state.
                mIncalling = true;
                break;
            case Call.State.ACTIVE:
            //mSlideView.setVisibility(SystemProperties.get(InCallActivity.KEY_LID_CLOSED).equals("true") ? View.VISIBLE : View.GONE); // MODIFY BOWAY, yulong.tan, 20151217, Remove SlideView, to prevent memory leaks.
            mIncalling = true;
                // We normally don't show a "call state label" at all in this state
                // (but we can use the call state label to display the provider name).
                if (isAccount) {
                    // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
                    /*callStateLabel = label;*/
                    // END BOWAY, yulong.tan
                } else if (sessionModificationState
                        == Call.SessionModificationState.REQUEST_FAILED) {
                    callStateLabel = context.getString(R.string.card_title_video_call_error);
                } else if (sessionModificationState
                        == Call.SessionModificationState.WAITING_FOR_RESPONSE) {
                    callStateLabel = context.getString(R.string.card_title_video_call_requesting);
                } else if (VideoProfile.VideoState.isBidirectional(videoState)) {
                    callStateLabel = context.getString(R.string.card_title_video_call);
                }
                break;
            case Call.State.ONHOLD:
                callStateLabel = context.getString(R.string.card_title_on_hold);
                mIncalling = true;
                break;
            case Call.State.CONNECTING:
            case Call.State.DIALING:
                // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
                /*if (isSpecialCall) {
                    callStateLabel = context.getString(R.string.calling_via_template, label);
                } else {
                    callStateLabel = context.getString(R.string.card_title_dialing);
                }*/
                callStateLabel = context.getString(R.string.card_title_dialing);
                //mSlideView.setVisibility(SystemProperties.get(InCallActivity.KEY_LID_CLOSED).equals("true") ? View.VISIBLE : View.GONE); // MODIFY BOWAY, yulong.tan, 20151217, Remove SlideView, to prevent memory leaks.
                mIncalling = true;
                // END BOWAY, yulong.tan
                break;
            case Call.State.REDIALING:
                callStateLabel = context.getString(R.string.card_title_redialing);
                mIncalling = true;
                break;
            case Call.State.INCOMING:
                mEndCallButton.setVisibility(View.GONE);
                mIncalling = false;
            case Call.State.CALL_WAITING:
                mIncalling = false;
                /// M: [VoLTE conference]incoming volte conference @{
                if (isIncomingVolteConferenceCall()) {
                    callStateLabel = context.getString(R.string.card_title_incoming_conference);
                    break;
                }
                /// @}
                // BEGIN BOWAY, yulong.tan, 20150506, InCall UI.
                /*if (isAccount) {
                    callStateLabel = context.getString(R.string.incoming_via_template, label);
                } else */ 
                // END BOWAY, yulong.tan
                if (VideoProfile.VideoState.isBidirectional(videoState)) {
                    callStateLabel = context.getString(R.string.notification_incoming_video_call);
                } else {
                    callStateLabel = context.getString(R.string.card_title_incoming_call);
                }
                break;
            case Call.State.DISCONNECTING:
                // While in the DISCONNECTING state we display a "Hanging up"
                // message in order to make the UI feel more responsive.  (In
                // GSM it's normal to see a delay of a couple of seconds while
                // negotiating the disconnect with the network, so the "Hanging
                // up" state at least lets the user know that we're doing
                // something.  This state is currently not used with CDMA.)
                mIncalling = true;
                callStateLabel = context.getString(R.string.card_title_hanging_up);
                break;
            case Call.State.DISCONNECTED:
                callStateLabel = disconnectCause.getLabel();
                if (TextUtils.isEmpty(callStateLabel)) {
                    callStateLabel = context.getString(R.string.card_title_call_ended);
                    // BEGIN BOWAY, yinling.zhu, 20150702, wake up when call is disconnected.
                    PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                    //BEGIN BOWAY,huiwen.zhou,20150813,modify newWakeLock tag
                    /*WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                              "handleAudioEvent");
                        */
                    WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                               "CallCardFragment");  
                    //END BOWAY,huiwen.zhou.
                    wakeLock.acquire();
                    // END BOWAY, yinling.zhu
 
                    // BEGIN BOWAY, wanjun.ouyang, 20150526, for add disconnect call vibrator.
                        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        mVibrator.vibrate(SCREEN_SHOT_VIBRATE_TIME);
                        // END BOWAY
                }
                    mIncalling = true;
                break;
            case Call.State.CONFERENCED:
                callStateLabel = context.getString(R.string.card_title_conf_call);
                mIncalling = true;
                break;
            default:
                Log.wtf(this, "updateCallStateWidgets: unexpected call: " + state);
                mIncalling = true;
        }
        // END BOWAY, kailong.liu
        return callStateLabel;
    }

    private void showAndInitializeSecondaryCallInfo(boolean hasProvider) {
        mSecondaryCallInfo.setVisibility(View.VISIBLE);

        // mSecondaryCallName is initialized here (vs. onViewCreated) because it is inaccessible
        // until mSecondaryCallInfo is inflated in the call above.
        if (mSecondaryCallName == null) {
            mSecondaryCallName = (TextView) getView().findViewById(R.id.secondaryCallName);
            mSecondaryCallConferenceCallIcon =
                    getView().findViewById(R.id.secondaryCallConferenceCallIcon);
        }

        if (mSecondaryCallProviderLabel == null && hasProvider) {
            mSecondaryCallProviderInfo.setVisibility(View.VISIBLE);
            mSecondaryCallProviderLabel = (TextView) getView()
                    .findViewById(R.id.secondaryCallProviderLabel);
        }
    }

    public void dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            dispatchPopulateAccessibilityEvent(event, mCallStateLabel);
            dispatchPopulateAccessibilityEvent(event, mPrimaryName);
            dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
            return;
        }
        dispatchPopulateAccessibilityEvent(event, mCallStateLabel);
        dispatchPopulateAccessibilityEvent(event, mPrimaryName);
        dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
        dispatchPopulateAccessibilityEvent(event, mCallTypeLabel);
        dispatchPopulateAccessibilityEvent(event, mSecondaryCallInfo.mCallName);
        dispatchPopulateAccessibilityEvent(event, mSecondaryCallInfo.mCallProviderLabel);

        return;
    }

    @Override
    public void setEndCallButtonEnabled(boolean enabled, boolean animate) {
        // BEGIN BOWAY, yulong.tan, 20150429, InCall UI.
        /// MTK add this log. @{
        /*Log.d(this, "setEndCallButtonEnabled, old state is %s", mFloatingActionButton.isEnabled());
        Log.d(this, "mFloatingActionButtonContainer visible is %s",
                mFloatingActionButtonContainer.getVisibility() != View.VISIBLE);
        Log.d(this, "enabled = " + enabled + "; animate = ", animate);
        /// @}
        if (enabled != mFloatingActionButton.isEnabled()) {
            if (animate) {
                if (enabled) {
                    mFloatingActionButtonController.scaleIn(AnimUtils.NO_DELAY);
                } else {
                    mFloatingActionButtonController.scaleOut();
                }
            } else {
                if (enabled) {
                    mFloatingActionButtonContainer.setScaleX(1);
                    mFloatingActionButtonContainer.setScaleY(1);
                    mFloatingActionButtonContainer.setVisibility(View.VISIBLE);
                } else {
                    mFloatingActionButtonContainer.setVisibility(View.GONE);
                }
            }
            mFloatingActionButton.setEnabled(enabled);
            updateFabPosition();
        }*/
        if (animate) {
            mEndCallButton.setVisibility(View.VISIBLE);
        } else {
            mEndCallButton.setVisibility(View.GONE);
        }
        // END BOWAY, yulong.tan
    }

    /**
     * Changes the visibility of the contact photo.
     *
     * @param isVisible {@code True} if the UI should show the contact photo.
     */
    @Override
    public void setPhotoVisible(boolean isVisible) {
        mPhoto.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * Changes the visibility of the "manage conference call" button.
     *
     * @param visible Whether to set the button to be visible or not.
     */
    @Override
    public void showManageConferenceCallButton(boolean visible) {
        mManageConferenceCallButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Determines the current visibility of the manage conference button.
     *
     * @return {@code true} if the button is visible.
     */
    @Override
    public boolean isManageConferenceVisible() {
        return mManageConferenceCallButton.getVisibility() == View.VISIBLE;
    }

    /**
     * Get the overall InCallUI background colors and apply to call card.
     */
    @Override
    public void updateColors() {
        MaterialPalette themeColors = InCallPresenter.getInstance().getThemeColors();

        if (mCurrentThemeColors != null && mCurrentThemeColors.equals(themeColors)) {
            return;
        }
        if (themeColors == null) {
            return;
        }

        // BEGIN BOWAY, yulong.tan, 20150429, InCall UI.
        //mPrimaryCallCardContainer.setBackgroundColor(themeColors.mPrimaryColor);
        //mCallButtonsContainer.setBackgroundColor(themeColors.mPrimaryColor);
        // END BOWAY, yulong.tan

        mCurrentThemeColors = themeColors;
    }

    private void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        if (view == null) return;
        final List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // if no text added write null to keep relative position
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }

    public void animateForNewOutgoingCall(final Point touchPoint,
            final boolean showCircularReveal) {
        final ViewGroup parent = (ViewGroup) mPrimaryCallCardContainer.getParent();

        final ViewTreeObserver observer = getView().getViewTreeObserver();

        mPrimaryCallInfo.getLayoutTransition().disableTransitionType(LayoutTransition.CHANGING);

        observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ViewTreeObserver observer = getView().getViewTreeObserver();
                if (!observer.isAlive()) {
                    return;
                }
                observer.removeOnGlobalLayoutListener(this);

                final LayoutIgnoringListener listener = new LayoutIgnoringListener();
                mPrimaryCallCardContainer.addOnLayoutChangeListener(listener);

                // Prepare the state of views before the circular reveal animation
                final int originalHeight = mPrimaryCallCardContainer.getHeight();
                mPrimaryCallCardContainer.setBottom(parent.getHeight());

                // Set up FAB.
                // BEGIN BOWAY, yulong.tan, 20150429, InCall UI.
                /* mFloatingActionButtonContainer.setVisibility(View.GONE);
                mFloatingActionButtonController.setScreenWidth(parent.getWidth());*/
                mEndCallButton.setVisibility(View.GONE);
                // END BOWAY, yulong.tan
                mCallButtonsContainer.setAlpha(0);
                mCallStateLabel.setAlpha(0);
                mPrimaryName.setAlpha(0);
                mCallTypeLabel.setAlpha(0);
                mCallNumberAndLabel.setAlpha(0);

                final Animator animator = getOutgoingCallAnimator(touchPoint,
                        parent.getHeight(), originalHeight, showCircularReveal);

                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setViewStatePostAnimation(listener);
                    }
                });
                animator.start();
            }
        });
    }

    public void onDialpadVisiblityChange(boolean isShown) {
        mIsDialpadShowing = isShown;
        updateFabPosition();
    }

    private void updateFabPosition() {
        int offsetY = 0;
        if (!mIsDialpadShowing) {
            offsetY = mFloatingActionButtonVerticalOffset;
            if (mOtherCallInfo.isShown()) {
                offsetY = -1 * mOtherCallInfo.getHeight();
            }
        }

        // BEGIN BOWAY, yulong.tan, 20150429, InCall UI.
        /*mFloatingActionButtonController.align(
                mIsLandscape ? FloatingActionButtonController.ALIGN_QUARTER_END
                        : FloatingActionButtonController.ALIGN_MIDDLE,
                0  offsetX ,
                offsetY,
                true);

        mFloatingActionButtonController.resize(
                mIsDialpadShowing ? mFabSmallDiameter : mFabNormalDiameter, true);*/
        // END BOWAY, yulong.tan
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the previous launch animation is still running, cancel it so that we don't get
        // stuck in an intermediate animation state.
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel();
        }

        mIsLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        final ViewGroup parent = ((ViewGroup) mPrimaryCallCardContainer.getParent());
        final ViewTreeObserver observer = parent.getViewTreeObserver();
        parent.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver viewTreeObserver = observer;
                if (!viewTreeObserver.isAlive()) {
                    viewTreeObserver = parent.getViewTreeObserver();
                }
                viewTreeObserver.removeOnGlobalLayoutListener(this);
                // BEGIN BOWAY, yulong.tan, 20150429, InCall UI.
                //mFloatingActionButtonController.setScreenWidth(parent.getWidth());
                // END BOWAY, yulong.tan
                updateFabPosition();
            }
        });

        updateColors();
        /// M: For second/third call color @{
        updateSecondCallColor();
        updateThirdCallColor();
        /// @}
    }

    /**
     * Adds a global layout listener to update the FAB's positioning on the next layout. This allows
     * us to position the FAB after the secondary call info's height has been calculated.
     */
    private void updateFabPositionForSecondaryCallInfo() {
        mOtherCallInfo.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final ViewTreeObserver observer = mOtherCallInfo.getViewTreeObserver();
                        if (!observer.isAlive()) {
                            return;
                        }
                        observer.removeOnGlobalLayoutListener(this);

                        onDialpadVisiblityChange(mIsDialpadShowing);

                        // Need update AnswerFragment bottom padding when there
                        // has another incoming call.
                        updateAnswerFragmentBottomPadding();
                    }
                });
    }

    /**
     * Animator that performs the upwards shrinking animation of the blue call card scrim.
     * At the start of the animation, each child view is moved downwards by a pre-specified amount
     * and then translated upwards together with the scrim.
     */
    private Animator getShrinkAnimator(int startHeight, int endHeight) {
        final Animator shrinkAnimator =
                ObjectAnimator.ofInt(mPrimaryCallCardContainer, "bottom", startHeight, endHeight);
        shrinkAnimator.setDuration(mShrinkAnimationDuration);
        shrinkAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                assignTranslateAnimation(mCallStateLabel, 1);
                // BEGIN BOWAY, yulong.tan, 20150506, InCall UI.
                /*assignTranslateAnimation(mCallStateIcon, 1);*/
                // END BOWAY, yulong.tan
                assignTranslateAnimation(mPrimaryName, 2);
                assignTranslateAnimation(mCallNumberAndLabel, 3);
                assignTranslateAnimation(mCallTypeLabel, 4);
                assignTranslateAnimation(mCallButtonsContainer, 5);

                // BEGIN BOWAY, yulong.tan, 20150429, InCall UI.
                //mFloatingActionButton.setEnabled(true);
                mEndCallButton.setEnabled(true);
                // END BOWAY, yulong.tan
            }
        });
        shrinkAnimator.setInterpolator(AnimUtils.EASE_IN);
        return shrinkAnimator;
    }

    private Animator getRevealAnimator(Point touchPoint) {
        final Activity activity = getActivity();
        final View view  = activity.getWindow().getDecorView();
        final Display display = activity.getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);

        int startX = size.x / 2;
        int startY = size.y / 2;
        if (touchPoint != null) {
            startX = touchPoint.x;
            startY = touchPoint.y;
        }

        final Animator valueAnimator = ViewAnimationUtils.createCircularReveal(view,
                startX, startY, 0, Math.max(size.x, size.y));
        valueAnimator.setDuration(mRevealAnimationDuration);
        return valueAnimator;
    }

    private Animator getOutgoingCallAnimator(Point touchPoint, int startHeight, int endHeight,
            boolean showCircularReveal) {

        final Animator shrinkAnimator = getShrinkAnimator(startHeight, endHeight);

        if (!showCircularReveal) {
            return shrinkAnimator;
        }

        final Animator revealAnimator = getRevealAnimator(touchPoint);
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(revealAnimator, shrinkAnimator);
        return animatorSet;
    }

    private void assignTranslateAnimation(View view, int offset) {
        view.setTranslationY(mTranslationOffset * offset);
        view.animate().translationY(0).alpha(1).withLayer()
                .setDuration(mShrinkAnimationDuration).setInterpolator(AnimUtils.EASE_IN);
    }

    private void setViewStatePostAnimation(View view) {
        view.setTranslationY(0);
        view.setAlpha(1);
    }

    private void setViewStatePostAnimation(OnLayoutChangeListener layoutChangeListener) {
        setViewStatePostAnimation(mCallButtonsContainer);
        setViewStatePostAnimation(mPrimaryName);
        setViewStatePostAnimation(mCallTypeLabel);
        setViewStatePostAnimation(mCallNumberAndLabel);
        // BEGIN BOWAY, yulong.tan, 20150506, InCall UI.
        /*setViewStatePostAnimation(mCallStateLabel);*/
        /*setViewStatePostAnimation(mCallStateIcon);*/
        // END BOWAY, yulong.tan

        mPrimaryCallCardContainer.removeOnLayoutChangeListener(layoutChangeListener);
        mPrimaryCallInfo.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        /// M: For ALPS01761179 & ALPS01794859, don't show end button if state
        // is incoming or disconnected. @{
        final Call call = CallList.getInstance().getFirstCall();
        if (call != null) {
            int state = call.getState();
            // BEGIN BOWAY, yulong.tan, 20150429, InCall UI.
            if (!Call.State.isIncoming(state) && Call.State.isConnectingOrConnected(state)) {
                mEndCallButton.setVisibility(View.VISIBLE);
                //mFloatingActionButtonController.scaleIn(AnimUtils.NO_DELAY);
                //Log.d(this, "setViewStatePostAnimation end.");
            /// M: For ALPS01828090 disable end call button when end button do not show under call state is disconnected.
            // in order to setEndCallButtonEnabled() can get right mFloatingActionButton state 
            // to show end button to other connecting or connected calls @{
            /*} else if (mFloatingActionButton.isEnabled()) {
                Log.i(this, "mFloatingActionButton.setEnabled(false) when end button do not show");
                mFloatingActionButton.setEnabled(false);
            }*/
            /// @}
            } else {
                mEndCallButton.setVisibility(View.GONE);
            }
            // END BOWAY, yulong.tan
        }
        /// @}
    }

    private final class LayoutIgnoringListener implements View.OnLayoutChangeListener {
        @Override
        public void onLayoutChange(View v,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
            v.setLeft(oldLeft);
            v.setRight(oldRight);
            v.setTop(oldTop);
            v.setBottom(oldBottom);
        }
    }

    /**
     * Need update AnswerFragment bottom padding when there has another incoming call.
     */
    public void updateAnswerFragmentBottomPadding() {
        int bottomPadding = 0;
        if (mSecondaryCallInfo != null && CallList.getInstance().getSecondaryIncomingCall() != null) {
            bottomPadding = mSecondaryCallInfo.getHeight();
        }

        View answerView = getView().findViewById(R.id.answerFragment);
        if (answerView == null) {
            return;
        }

        int oldBottomPadding = answerView.getPaddingBottom();
        if (bottomPadding != oldBottomPadding) {
            answerView.setPadding(answerView.getPaddingLeft(), answerView.getPaddingTop(),
                    answerView.getPaddingRight(), bottomPadding);
            answerView.invalidate();
        }
    }

    /**
     * Make dim effect for secondary CallInfoView if needed.
     * @param dim true indicates that needs dim and false otherwise.
     */
    public void updateDimEffectForSecondaryCallInfo(boolean dim) {
        View view = getView().findViewById(R.id.dim_effect_for_secondary);
        if (dim) {
            mSecondaryCallInfo.setEnabled(false);
            view.setVisibility(View.VISIBLE);
        } else {
            mSecondaryCallInfo.setEnabled(true);
            view.setVisibility(View.GONE);
        }
    }

    // -----------------------------Medaitek---------------------------------------

    private void initVoiceRecorderIcon(View view) {
        mVoiceRecorderIcon = (ImageView) view.findViewById(R.id.voiceRecorderIcon);
        mVoiceRecorderIcon.setImageResource(R.drawable.voice_record);
        mVoiceRecorderIcon.setVisibility(View.INVISIBLE);
    }

    @Override
    public void updateVoiceRecordIcon(boolean show) {
        mVoiceRecorderIcon.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        AnimationDrawable ad = (AnimationDrawable) mVoiceRecorderIcon.getDrawable();
        if (ad != null) {
            if (show && !ad.isRunning()) {
                ad.start();
            } else if (!show && ad.isRunning()) {
                ad.stop();
            }
        }
    }

    // Fix ALPS01759672. @{
    @Override
    public void setSecondaryEnabled(boolean enabled) {
        if (mSecondaryCallInfo != null) {
            mSecondaryCallInfo.setEnabled(enabled);
        }
    }

    @Override
    public void setThirdEnabled(boolean enabled) {
        if (mThirdCallInfo != null) {
            mThirdCallInfo.setEnabled(enabled);
        }
    }
    // @}


    /// M: For second/third call color @{
    /**
     * Get the second call color and apply to second call provider label.
     */
    public void updateSecondCallColor() {
        int secondCallColor = getPresenter().getSecondCallColor();
        if (mCurrentSecondCallColor == secondCallColor) {
            return;
        }
        mSecondaryCallInfo.mCallProviderLabel.setTextColor(secondCallColor);
        mCurrentSecondCallColor = secondCallColor;
    }

    /**
     * Get the third call color and apply to third call provider label.
     */
    public void updateThirdCallColor() {
        int thirdCallColor = getPresenter().getThirdCallColor();
        if (mCurrentThirdCallColor == thirdCallColor) {
            return;
        }
        mThirdCallInfo.mCallProviderLabel.setTextColor(thirdCallColor);
        mCurrentThirdCallColor = thirdCallColor;
    }
    /**
     * check whether the callStateIcon has no change.
     * @param callStateIcon call state icon
     * @return true if no change
     */
    public boolean isCallStateIconUnChanged(Drawable callStateIcon) {
        return (mCallStateIcon.getDrawable() == null && callStateIcon == null)
                || (mCallStateIcon.getDrawable() != null && callStateIcon != null);
    }
    /// @}

    private boolean isIncomingVolteConferenceCall() {
        Call call = CallList.getInstance().getIncomingCall();
        return call != null
                && call.isConferenceCall()
                && call.can(android.telecom.Call.Details.CAPABILITY_VOLTE);
    }

    // BEGIN BOWAY, yulong.tan, 20150507, InCall UI.
    @Override
    public void setSimIndicator(int slotId) {
            int simId =slotId + 1;
            mSimIndicator.setText("SIM" + simId);
            mSimIndicator.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void showCallButtonsFragment(boolean isShow) {
        if(mCallButtonsContainer != null) {
            if(isShow) {
                mCallButtonsContainer.setVisibility(View.GONE);
            } else {
                mCallButtonsContainer.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    public void setLocation(String location) {
        Log.d(this, "setLocation = " + location);

        if (TextUtils.isEmpty(location)) {
            mLocation.setText("");
            if (InCallActivity.isSimpleSystem) {
                mIncomingLocation.setText("");
            }
        } else {
            mLocation.setText(location);
            if (InCallActivity.isSimpleSystem) {
                mIncomingLocation.setText(location);
            }
        }
    }
    
    @Override
    public void showIncomingCallBanner(boolean isShow) {
        mIncomingCallInfo.setVisibility(isShow ? View.VISIBLE : View.GONE);
        mPrimaryCallInfo.setVisibility(isShow ? View.GONE : View.VISIBLE);
    }
    // END BOWAY, yulong.tan
    
    // BEGIN BOWAY, kailong.liu, 20150618, InCall UI.
    public void hideWindowLayout(boolean isWindowLock){
        mWindowPrimaryCallInfoFrame.setVisibility(isWindowLock ? View.VISIBLE : View.GONE);
        mPrimaryCallInfoFrame.setVisibility(!isWindowLock ? View.VISIBLE : View.GONE);
        //mSlideView.setVisibility(isWindowLock && mIncalling ? View.VISIBLE : View.GONE); // MODIFY BOWAY, yulong.tan, 20151217, Remove SlideView, to prevent memory leaks.
        //mEndCallButton.setVisibility(!isWindowLock ? View.VISIBLE : View.GONE);
        handler.sendEmptyMessageDelayed(TOUCH_SWITCH, TOUCH_SWITCH_DELAY);
       }
       
       public boolean slideVisible(){
           return mIncalling;
    }
    
    public void windowUpdateProximitySensorMode(){
            getPresenter().windowUpdateProximitySensorMode();
    }
    // END BOWAY, kailong.liu
       
    // BEGIN BOWAY, yulong.tan, 20151216, To prevent memory leaks.
    private Bitmap circleBitmap = null;
    private Bitmap photoBitmap = null;
    private Drawable toRoundDrawable(Drawable drawable) {
        photoBitmap = ((BitmapDrawable)drawable).getBitmap();
        int radius = photoBitmap.getWidth() / 2;
        BitmapShader bitmapShader = new BitmapShader(photoBitmap, BitmapShader.TileMode.REPEAT,
        BitmapShader.TileMode.REPEAT);
        try {
            circleBitmap = Bitmap.createBitmap(photoBitmap.getWidth(), photoBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(circleBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(bitmapShader);
            c.drawCircle(radius,radius, radius, paint);
            drawable = new BitmapDrawable(circleBitmap);
        }catch (OutOfMemoryError e) {
            Log.e(this, "toRoundDrawable() OutOfMemoryError");
            // return Unprocessed drawable
        }
        return drawable;
    }

    @Override
    public void onDestroy() {
        Log.d(this, "onDestroy()");
        super.onDestroy();
        if (photoBitmap != null && !photoBitmap.isRecycled()) {
            photoBitmap = null;
        }
        if (circleBitmap != null && !circleBitmap.isRecycled()) {
            circleBitmap.recycle();
            circleBitmap = null;
        }
       System.gc();
    }
    // END BOWAY, yulong.tan

    // BEGIN BOWAY, yulong.tan, 20150625, InCall UI.
    @Override
    public void showCallButtonArea(boolean isShow) {
        mCallButtonArea.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }
    // END BOWAY, yulong.tan
}
