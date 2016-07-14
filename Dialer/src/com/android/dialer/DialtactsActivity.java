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
 * limitations under the License.
 */

package com.android.dialer;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.net.nsd.NsdManager.RegistrationListener;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.contacts.common.dialog.ClearFrequentsDialog;
import com.android.contacts.common.interactions.ImportExportDialogFragment;
import com.android.contacts.common.interactions.TouchPointManager;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.common.widget.FloatingActionButtonController;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.android.dialer.calllog.CallLogActivity;
import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.dialpad.DialpadFragment;
import com.android.dialer.dialpad.SmartDialNameMatcher;
import com.android.dialer.dialpad.SmartDialPrefix;
import com.android.dialer.interactions.PhoneNumberInteraction;
import com.android.dialer.list.DragDropController;
import com.android.dialer.list.ListsFragment;
import com.android.dialer.list.OnDragDropListener;
import com.android.dialer.list.OnListFragmentScrolledListener;
import com.android.dialer.list.PhoneFavoriteSquareTileView;
import com.android.dialer.list.RegularSearchFragment;
import com.android.dialer.list.SearchFragment;
import com.android.dialer.list.SmartDialSearchFragment;
import com.android.dialer.list.SpeedDialFragment;
import com.android.dialer.settings.DialerSettingsActivity;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.widget.ActionBarController;
import com.android.dialer.widget.SearchEditTextLayout;
import com.android.dialer.widget.SearchEditTextLayout.OnBackButtonClickedListener;
import com.android.dialerbind.DatabaseHelperManager;
import com.android.incallui.CallCardFragment;
import com.android.ims.ImsManager;
import com.android.phone.common.animation.AnimUtils;
import com.android.phone.common.animation.AnimationListenerAdapter;

import com.mediatek.dialer.dialersearch.DialerSearchHelper;
import com.mediatek.dialer.dialersearch.RegularSearchFragmentEx;
import com.mediatek.dialer.dialersearch.SearchFragmentEx;
import com.mediatek.dialer.dialersearch.SmartDialSearchFragmentEx;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.util.CallAccountSelectionNotificationUtil;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.util.DialerVolteUtils;
import com.mediatek.dialer.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
//qianweiqiang add for new UI 2015 7 3
import android.content.BroadcastReceiver;
import com.mediatek.dialer.dialersearch.DialerSearchHelper;
import com.android.incallui.CallCardFragment;
import android.view.Window;
import android.view.WindowManager;
import android.content.IntentFilter;
import android.net.nsd.NsdManager.RegistrationListener;
import android.app.Service;

//BEGIN BOWAY weiqiang.qian 2015 8 27 set double card
import com.mediatek.telephony.TelephonyManagerEx;
//END BOWAY
/**
 * The dialer tab's title is 'phone', a more common name (see strings.xml).
 */
public class DialtactsActivity extends TransactionSafeActivity implements View.OnClickListener,
        DialpadFragment.OnDialpadQueryChangedListener,
        OnListFragmentScrolledListener,
        ListsFragment.HostInterface,
        SpeedDialFragment.HostInterface,
        SearchFragment.HostInterface,
        OnDragDropListener,
        OnPhoneNumberPickerActionListener,
        PopupMenu.OnMenuItemClickListener,
        ViewPager.OnPageChangeListener,
        ActionBarController.ActivityUi,
        SearchFragmentEx.HostInterface {
    private static final String TAG = "DialtactsActivity";

  //BEGIN BOWAY weiqiang.qian 2015 8 27 set double card
    private boolean isTwoSimInserted =false;
    private SimStateReceiver mSimStateReceiver;
    //END BOWAY
    /// M: For the purpose of debugging in eng load
    public static final boolean DEBUG = Build.TYPE.equals("eng");

    public static final String SHARED_PREFS_NAME = "com.android.dialer_preferences";

    /** @see #getCallOrigin() */
    private static final String CALL_ORIGIN_DIALTACTS =
            "com.android.dialer.DialtactsActivity";

    private static final String KEY_IN_REGULAR_SEARCH_UI = "in_regular_search_ui";
    private static final String KEY_IN_DIALPAD_SEARCH_UI = "in_dialpad_search_ui";
    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_IS_DIALPAD_SHOWN = "is_dialpad_shown";

    private static final String TAG_DIALPAD_FRAGMENT = "dialpad";
    private static final String TAG_REGULAR_SEARCH_FRAGMENT = "search";
    private static final String TAG_SMARTDIAL_SEARCH_FRAGMENT = "smartdial";
    private static final String TAG_FAVORITES_FRAGMENT = "favorites";

    /**
     * Just for backward compatibility. Should behave as same as {@link Intent#ACTION_DIAL}.
     */
    private static final String ACTION_TOUCH_DIALER = "com.android.phone.action.TOUCH_DIALER";

    private static final int ACTIVITY_REQUEST_CODE_VOICE_SEARCH = 1;

    private FrameLayout mParentLayout;

    /**
     * Fragment containing the dialpad that slides into view
     */
    protected DialpadFragment mDialpadFragment;

    /**
     * Fragment for searching phone numbers using the alphanumeric keyboard.
     */
    private RegularSearchFragment mRegularSearchFragment;

    /**
     * Fragment for searching phone numbers using the dialpad.
     */
    private SmartDialSearchFragment mSmartDialSearchFragment;

    /**
     * Animation that slides in.
     */
    private Animation mSlideIn;

    /**
     * Animation that slides out.
     */
    private Animation mSlideOut;

    /**
     * Listener for after slide out animation completes on dialer fragment.
     */
    AnimationListenerAdapter mSlideOutListener = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            commitDialpadFragmentHide();
        }
    };

    /**
     * Fragment containing the speed dial list, recents list, and all contacts list.
     */
    private ListsFragment mListsFragment;

    /**
     * Tracks whether onSaveInstanceState has been called. If true, no fragment transactions can
     * be commited.
     */
    private boolean mStateSaved;
    private boolean mIsRestarting;
    private boolean mInDialpadSearch;
    private boolean mInRegularSearch;
    private boolean mClearSearchOnPause;
    private boolean mIsDialpadShown;
    private boolean mShowDialpadOnResume;
    
    //BEGIN BOWAY  weiqiang.qian 2015 9 8 
    private boolean isActivityShow = true;
    private boolean needRefresh = false;
    //END BOWAY

    /**
     * Whether or not the device is in landscape orientation.
     */
    private boolean mIsLandscape;

    /**
     * The position of the currently selected tab in the attached {@link ListsFragment}.
     */
    private int mCurrentTabPosition = 0;

    /**
     * True if the dialpad is only temporarily showing due to being in call
     */
    private boolean mInCallDialpadUp;

    /**
     * True when this activity has been launched for the first time.
     */
    private boolean mFirstLaunch;

    /**
     * Search query to be applied to the SearchView in the ActionBar once
     * onCreateOptionsMenu has been called.
     */
    private String mPendingSearchViewQuery;

    private PopupMenu mOverflowMenu;
    private EditText mSearchView;
    private View mVoiceSearchButton;

    private String mSearchQuery;

    private DialerDatabaseHelper mDialerDatabaseHelper;
    private DragDropController mDragDropController;
    private ActionBarController mActionBarController;
    
    //qianweiqiang set action bar 
    SearchEditTextLayout searchEditTextLayout;
    EnterRegularSearchReceiver receiver;
    //

    private FloatingActionButtonController mFloatingActionButtonController;

    private int mActionBarHeight;

    /**
     * The text returned from a voice search query.  Set in {@link #onActivityResult} and used in
     * {@link #onResume()} to populate the search box.
     */
    private String mVoiceSearchQuery;

    private class OptionsPopupMenu extends PopupMenu {
        public OptionsPopupMenu(Context context, View anchor) {
            super(context, anchor, Gravity.END);
        }

        @Override
        public void show() {
            final Menu menu = getMenu();
////qianweiqiang Boway 2015 5 9  add for new view not have a frequent contact	
//            final MenuItem clearFrequents = menu.findItem(R.id.menu_clear_frequents);
//            clearFrequents.setVisible(mListsFragment != null &&
//                    mListsFragment.getSpeedDialFragment() != null &&
//                    mListsFragment.getSpeedDialFragment().hasFrequents());
//end boway
            super.show();
        }
    }

    /**
     * Listener that listens to drag events and sends their x and y coordinates to a
     * {@link DragDropController}.
     */
    private class LayoutOnDragListener implements OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                mDragDropController.handleDragHovered(v, (int) event.getX(), (int) event.getY());
            }
            return true;
        }
    }

    /**
     * Listener used to send search queries to the phone search fragment.
     */
    private final TextWatcher mPhoneSearchQueryTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            final String newText = s.toString();
            if (newText.equals(mSearchQuery)) {
                // If the query hasn't changed (perhaps due to activity being destroyed
                // and restored, or user launching the same DIAL intent twice), then there is
                // no need to do anything here.
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "onTextChange for mSearchView called with new query: " + newText);
                Log.d(TAG, "Previous Query: " + mSearchQuery);
            }
            mSearchQuery = newText;

            // Show search fragment only when the query string is changed to non-empty text.
            if (!TextUtils.isEmpty(newText)) {
                // Call enterSearchUi only if we are switching search modes, or showing a search
                // fragment for the first time.
                final boolean sameSearchMode = (mIsDialpadShown && mInDialpadSearch) ||
                        (!mIsDialpadShown && mInRegularSearch);
                if (!sameSearchMode) {
                    enterSearchUi(mIsDialpadShown, mSearchQuery);
                }
            }/*qianweiqiang add set exit Search UI when input is none*/
            else 
            {
                if(mCurrentTabPosition == ListsFragment.TAB_INDEX_RECENTS) 
                exitSearchUi();
            }//end add

            /// M: Support MTK-DialerSearch @{
            if (DialerFeatureOptions.isDialerSearchEnabled()) {
                if (mEnhancedSmartDialSearchFragment != null && mEnhancedSmartDialSearchFragment.isVisible()) {
                    LogUtils.d(TAG, "MTK-DialerSearch, mEnhancedSmartDialSearchFragment");

                    mEnhancedSmartDialSearchFragment.setQueryString(mSearchQuery, false);
                } else if (mEnhancedRegularSearchFragment != null && mEnhancedRegularSearchFragment.isVisible()) {
                    LogUtils.d(TAG, "MTK-DialerSearch, mEnhancedRegularSearchFragment");

                    mEnhancedRegularSearchFragment.setQueryString(mSearchQuery, false);
                }
           /// @}
            } else {
                if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()) {
                    mSmartDialSearchFragment.setQueryString(mSearchQuery, false /* delaySelection */);
                } else if (mRegularSearchFragment != null && mRegularSearchFragment.isVisible()) {
                    mRegularSearchFragment.setQueryString(mSearchQuery, false /* delaySelection */);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };


    /**
     * Open the search UI when the user clicks on the search box.
     */
    private final View.OnClickListener mSearchViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isInSearchUi()) {
                mActionBarController.onSearchBoxTapped();
                enterSearchUi(false /* smartDialSearch */, mSearchView.getText().toString());
            }
        }
    };

    /**
     * If the search term is empty and the user closes the soft keyboard, close the search UI.
     */
    private final View.OnKeyListener mSearchEditTextLayoutListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN &&
                    TextUtils.isEmpty(mSearchView.getText().toString())) {
                maybeExitSearchUi();
            }
            return false;
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);

    }
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        //BEGIN BOWAY 
        unregisterReceiver(mSimStateReceiver);
        //END BOWAY
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirstLaunch = true;

        final Resources resources = getResources();
        mActionBarHeight = resources.getDimensionPixelSize(R.dimen.action_bar_height_large);

        setContentView(R.layout.dialtacts_activity);
        getWindow().setBackgroundDrawable(null);

        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.search_edittext);
        actionBar.setDisplayShowCustomEnabled(true);
//qianweiqiang Boway 2015 5 9  add for change the view to yellowpage view    BOWAY 2016 3 31    
        actionBar.setBackgroundDrawable(null);
//end boway       
        

        mActionBarController = new ActionBarController(this,
                (SearchEditTextLayout) actionBar.getCustomView());

//        SearchEditTextLayout searchEditTextLayout =
//                (SearchEditTextLayout) actionBar.getCustomView();
//        qianweiqiang set  2015 5 - 26
        searchEditTextLayout =
                (SearchEditTextLayout) actionBar.getCustomView();
        
        searchEditTextLayout.setPreImeKeyListener(mSearchEditTextLayoutListener);
//qianweiqiang Boway 2015 5 9  add for change the view to yellowpage view
        //searchEditTextLayout.setVisible(false);
    //BOWAY
	    searchEditTextLayout.setVisibility(View.VISIBLE);
    //BOWAY  qianweiqiang 3016 3 31  
	    actionBar.getCustomView().setVisibility(View.VISIBLE);
//end boway	

        mSearchView = (EditText) searchEditTextLayout.findViewById(R.id.search_view);
        mSearchView.addTextChangedListener(mPhoneSearchQueryTextListener);
        mVoiceSearchButton = searchEditTextLayout.findViewById(R.id.voice_search_button);
        searchEditTextLayout.findViewById(R.id.search_magnifying_glass)
                .setOnClickListener(mSearchViewOnClickListener);
        searchEditTextLayout.findViewById(R.id.search_box_start_search)
                .setOnClickListener(mSearchViewOnClickListener);
        searchEditTextLayout.setOnBackButtonClickedListener(new OnBackButtonClickedListener() {
            @Override
            public void onBackButtonClicked() {
                onBackPressed();
            }
        });

        mIsLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        final View floatingActionButtonContainer = findViewById(
                R.id.floating_action_button_container);
        ImageButton floatingActionButton = (ImageButton) findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener(this);
        mFloatingActionButtonController = new FloatingActionButtonController(this,
                floatingActionButtonContainer, floatingActionButton);

        ImageButton optionsMenuButton =
                (ImageButton) searchEditTextLayout.findViewById(R.id.dialtacts_options_menu_button);
        optionsMenuButton.setOnClickListener(this);
        mOverflowMenu = buildOptionsMenu(searchEditTextLayout);
        optionsMenuButton.setOnTouchListener(mOverflowMenu.getDragToOpenListener());

        // Add the favorites fragment, and the dialpad fragment, but only if savedInstanceState
        // is null. Otherwise the fragment manager takes care of recreating these fragments.
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.dialtacts_frame, new ListsFragment(), TAG_FAVORITES_FRAGMENT)
                    .add(R.id.dialtacts_container, new DialpadFragment(), TAG_DIALPAD_FRAGMENT)
                    .commit();
        } else {
            mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
            mInRegularSearch = savedInstanceState.getBoolean(KEY_IN_REGULAR_SEARCH_UI);
            mInDialpadSearch = savedInstanceState.getBoolean(KEY_IN_DIALPAD_SEARCH_UI);
            mFirstLaunch = savedInstanceState.getBoolean(KEY_FIRST_LAUNCH);
            mShowDialpadOnResume = savedInstanceState.getBoolean(KEY_IS_DIALPAD_SHOWN);
            mActionBarController.restoreInstanceState(savedInstanceState);
            /** M: [ALPS01940938] add the global listener during restore @{ */
            final View contentView = findViewById(android.R.id.content);
            contentView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mActionBarController != null) {
                                if (DEBUG) {
                                    Log.d(TAG, "finish layout, and do restore actionbar");
                                }
                                mActionBarController.restoreActionBarOffset(mListsFragment != null
                                        && mListsFragment.isResumed());
                                ViewTreeObserver treeObserver = contentView.getViewTreeObserver();
                                if (treeObserver.isAlive()) {
                                    treeObserver.removeOnGlobalLayoutListener(this);
                                }
                            }
                        }
                    });
            /** @} */
        }

        final boolean isLayoutRtl = DialerUtils.isRtl();
        if (mIsLandscape) {
            mSlideIn = AnimationUtils.loadAnimation(this,
                    isLayoutRtl ? R.anim.dialpad_slide_in_left : R.anim.dialpad_slide_in_right);
            mSlideOut = AnimationUtils.loadAnimation(this,
                    isLayoutRtl ? R.anim.dialpad_slide_out_left : R.anim.dialpad_slide_out_right);
        } else {
            mSlideIn = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_in_bottom);
            mSlideOut = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_out_bottom);
        }

        mSlideIn.setInterpolator(AnimUtils.EASE_IN);
        mSlideOut.setInterpolator(AnimUtils.EASE_OUT);

        mSlideOut.setAnimationListener(mSlideOutListener);

        mParentLayout = (FrameLayout) findViewById(R.id.dialtacts_mainlayout);
        mParentLayout.setOnDragListener(new LayoutOnDragListener());
        /// M: for ALPS01660901 @{
        ViewTreeObserver observer = floatingActionButtonContainer.getViewTreeObserver();
        if (observer != null) {
        /// @}
            observer.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final ViewTreeObserver observer =
                                floatingActionButtonContainer.getViewTreeObserver();
                        if (!observer.isAlive()) {
                            return;
                        }
                        observer.removeOnGlobalLayoutListener(this);
                        int screenWidth = mParentLayout.getWidth();
                        mFloatingActionButtonController.setScreenWidth(screenWidth);
                        updateFloatingActionButtonControllerAlignment(false /* animate */);
                    }
                });
        } else {
            Log.e(TAG, "observer is null !!!");
        }

        setupActivityOverlay();

        /// M: Support MTK-DialerSearch @{
        if (!DialerFeatureOptions.isDialerSearchEnabled()) {
            mDialerDatabaseHelper = DatabaseHelperManager.getDatabaseHelper(this);
            SmartDialPrefix.initializeNanpSettings(this);
        }
        /// @}
//qianweiqiang Boway 2015 5 9  add for change the view to yellowpage view  hide actionbar
	   //BOWAY 2016 3 31 actionBar.hide();
//end boway	
	   IntentFilter intentFilter = new IntentFilter("ACTION_GO_TO_REGULAR_SEARCH");
       //
	   receiver = new EnterRegularSearchReceiver();
       //
       registerReceiver(receiver, intentFilter);
       //BEGIN BOWAY weiqiang.qian 2015 8 27 set double card       
       //
       IntentFilter simintentFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
       mSimStateReceiver = new SimStateReceiver();
       registerReceiver(mSimStateReceiver, simintentFilter);
       //END BOWAY
    }

    private void setupActivityOverlay() {
        final View activityOverlay = findViewById(R.id.activity_overlay);
        activityOverlay.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mIsDialpadShown) {
                    maybeExitSearchUi();
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStateSaved = false;
        //BEGIN BOWAY
        isActivityShow = true;
        //END BOWAY
        if (mFirstLaunch) {
            displayFragment(getIntent());
            //qwq
            showDialpadFragment(false);
            //qwq
        } else if (!phoneIsInUse() && mInCallDialpadUp) {
            hideDialpadFragment(false, true);
            mInCallDialpadUp = false;
        } else if (mShowDialpadOnResume) {
            showDialpadFragment(false);
            mShowDialpadOnResume = false;
        }

        // If there was a voice query result returned in the {@link #onActivityResult} callback, it
        // will have been stashed in mVoiceSearchQuery since the search results fragment cannot be
        // shown until onResume has completed.  Active the search UI and set the search term now.
        if (!TextUtils.isEmpty(mVoiceSearchQuery)) {
            mActionBarController.onSearchBoxTapped();
            mSearchView.setText(mVoiceSearchQuery);
            mVoiceSearchQuery = null;
        }

        mFirstLaunch = false;

        if (mIsRestarting) {
            // This is only called when the activity goes from resumed -> paused -> resumed, so it
            // will not cause an extra view to be sent out on rotation
            if (mIsDialpadShown) {
                AnalyticsUtil.sendScreenView(mDialpadFragment, this);
            }
            mIsRestarting = false;
        }
        prepareVoiceSearchButton();

        /// M: Support MTK-DialerSearch @{
        if (!DialerFeatureOptions.isDialerSearchEnabled()) {
            mDialerDatabaseHelper.startSmartDialUpdateThread();
        }
        /// @}

        /** M: for ALPS01942029, make sure the listsfragment is user invisible,
         * and ActionBar is slided right @{ */
        if (isInSearchUi() && mActionBarController.isActionBarShowing()) {
            mListsFragment.getView().animate().alpha(0).withLayer();
        }
        /** @} */
        updateFloatingActionButtonControllerAlignment(false /* animate */);

        /// M: [Call Account Notification] Show the call account selection notification
        CallAccountSelectionNotificationUtil.getInstance(this).showNotification(true, this);
        
        //BEGIN BOWAY weiqiang.qian 2015 8 27 set double card
        /*if(needRefresh)
        {
        	needRefresh = false;
        	refreshDialPad();
        }*/
        //BEGIN BOWAY weiqiang.qian 2015 10 12 set single view when make second call 
        if(phoneIsInUse())
        {
                if(mDialpadFragment != null)
        	mDialpadFragment.updateSingleCardView();
        } else {
        	refreshDialPad();
        }
	//END BOWAY
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mIsRestarting = true;
    }

    @Override
    protected void onPause() {
        if (mClearSearchOnPause) {
            hideDialpadAndSearchUi();
            mClearSearchOnPause = false;
        }
        /// M: [Call Account Notification] Hide the call account selection notification
        CallAccountSelectionNotificationUtil.getInstance(this).showNotification(false, this);
        if (mSlideOut.hasStarted() && !mSlideOut.hasEnded()) {
            commitDialpadFragmentHide();
        }
		//BEGIN BOWAY weiqiang.qian 2015 8 27 set double card
        isActivityShow = false;
		//END BOWAY
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
        outState.putBoolean(KEY_IN_REGULAR_SEARCH_UI, mInRegularSearch);
        outState.putBoolean(KEY_IN_DIALPAD_SEARCH_UI, mInDialpadSearch);
        outState.putBoolean(KEY_FIRST_LAUNCH, mFirstLaunch);
        outState.putBoolean(KEY_IS_DIALPAD_SHOWN, mIsDialpadShown);
        mActionBarController.saveInstanceState(outState);
        mStateSaved = true;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof DialpadFragment) {
            mDialpadFragment = (DialpadFragment) fragment;
            if (!mShowDialpadOnResume) {
                final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.hide(mDialpadFragment);
                transaction.commit();
            }

            /// M: Support MTK-DialerSearch @{
        } else if (DialerFeatureOptions.isDialerSearchEnabled() && fragment instanceof SmartDialSearchFragmentEx) {
            mEnhancedSmartDialSearchFragment = (SmartDialSearchFragmentEx) fragment;
            mEnhancedSmartDialSearchFragment.setOnPhoneNumberPickerActionListener(this);
        } else if (DialerFeatureOptions.isDialerSearchEnabled() && fragment instanceof RegularSearchFragmentEx) {
            mEnhancedRegularSearchFragment = (RegularSearchFragmentEx) fragment;
            mEnhancedRegularSearchFragment.setOnPhoneNumberPickerActionListener(this);
            /// @}

        } else if (fragment instanceof SmartDialSearchFragment) {
            mSmartDialSearchFragment = (SmartDialSearchFragment) fragment;
            mSmartDialSearchFragment.setOnPhoneNumberPickerActionListener(this);
        } else if (fragment instanceof SearchFragment) {
            mRegularSearchFragment = (RegularSearchFragment) fragment;
            mRegularSearchFragment.setOnPhoneNumberPickerActionListener(this);
        } else if (fragment instanceof ListsFragment) {
            mListsFragment = (ListsFragment) fragment;
            mListsFragment.addOnPageChangeListener(this);
        }
    }

    protected void handleMenuSettings() {
        final Intent intent = new Intent(this, DialerSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.floating_action_button:
                if (!mIsDialpadShown) {
                    mInCallDialpadUp = false;
                    showDialpadFragment(true);
                }
                break;
            case R.id.voice_search_button:
                try {
                    startActivityForResult(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                            ACTIVITY_REQUEST_CODE_VOICE_SEARCH);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(DialtactsActivity.this, R.string.voice_search_not_available,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.dialtacts_options_menu_button:
                mOverflowMenu.show();
                break;
            default: {
                Log.wtf(TAG, "Unexpected onClick event from " + view);
                break;
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_history:

                /// M: support calllog performance @{
                LogUtils.i("sera","[Performance test][Dialer] Calllog_Performance_001 start [" + System.currentTimeMillis() + "]");
                /// @}

                showCallHistory();
                break;
            case R.id.menu_add_contact:
                try {
                    startActivity(new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
                } catch (ActivityNotFoundException e) {
                    Toast toast = Toast.makeText(this,
                            R.string.add_contact_not_available,
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
                break;

                /// M: Use this feature in Contacts instead of Dialer @{
//            case R.id.menu_import_export:
//                // We hard-code the "contactsAreAvailable" argument because doing it properly would
//                // involve querying a {@link ProviderStatusLoader}, which we don't want to do right
//                // now in Dialtacts for (potential) performance reasons. Compare with how it is
//                // done in {@link PeopleActivity}.
//                ImportExportDialogFragment.show(getFragmentManager(), true,
//                        DialtactsActivity.class);
//                return true;
                /// @}
//qianweiqiang Boway 2015 5 9  change for no frequent exist not use it
//            case R.id.menu_clear_frequents:
//                ClearFrequentsDialog.show(getFragmentManager());
//                return true;
//end boway
            case R.id.menu_call_settings:
                handleMenuSettings();
                return true;
            /** M: [VoLTE ConfCall] handle conference call menu. @{ */
//qianweiqiang boway 2015 6 27 remove conference call
           /* case R.id.menu_volte_conf_call:
                DialerVolteUtils.handleMenuVolteConfCall(this);
                return true;*/
//qianweiqiang boway 2015 6 27 remove conference call
            /** @} */
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CODE_VOICE_SEARCH) {
            if (resultCode == RESULT_OK) {
                final ArrayList<String> matches = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                if (matches.size() > 0) {
                    final String match = matches.get(0);
                    mVoiceSearchQuery = match;
                } else {
                    Log.e(TAG, "Voice search - nothing heard");
                }
            } else {
                Log.e(TAG, "Voice search failed");
            }
        }
        /** M: [VoLTE ConfCall] Handle the volte conference call. @{ */
        else if (requestCode == DialerVolteUtils.ACTIVITY_REQUEST_CODE_PICK_PHONE_CONTACTS) {
            if (resultCode == RESULT_OK) {
                DialerVolteUtils.launchVolteConfCall(this, data);
            } else {
                Log.e(TAG, "Volte conference call not pick contacts");
            }
        }
        /** @} */

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Initiates a fragment transaction to show the dialpad fragment. Animations and other visual
     * updates are handled by a callback which is invoked after the dialpad fragment is shown.
     * @see #onDialpadShown
     */
    private void showDialpadFragment(boolean animate) {
        if (mIsDialpadShown || mStateSaved) {
            return;
        }
        mIsDialpadShown = true;
        mDialpadFragment.setAnimate(animate);
        mListsFragment.setUserVisibleHint(false);
        AnalyticsUtil.sendScreenView(mDialpadFragment);

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.show(mDialpadFragment);
        /// M: fix CR: ALPS01608178, avoid commit JE @{
        /*
        ft.commit();
        */
        ft.commitAllowingStateLoss();
        /// @}

        if (animate) {
            mFloatingActionButtonController.scaleOut();
        } else {
            mFloatingActionButtonController.setVisible(false);
        }
        mActionBarController.onDialpadUp();
////qwq
//        if (!isInSearchUi()) {
//            enterSearchUi(true /* isSmartDial */, mSearchQuery);
//        }
    }

    /**
     * Callback from child DialpadFragment when the dialpad is shown.
     */
    public void onDialpadShown() {
        if (mDialpadFragment.getAnimate()) {
            mDialpadFragment.getView().startAnimation(mSlideIn);
        } else {
            mDialpadFragment.setYFraction(0);
        }

        /// M: Support MTK-DialerSearch @[
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            updateSearchFragmentExPosition();
        } else {
        /// @}
            updateSearchFragmentPosition();
        }
    }

    /**
     * Initiates animations and other visual updates to hide the dialpad. The fragment is hidden in
     * a callback after the hide animation ends.
     * @see #commitDialpadFragmentHide
     */
    public void hideDialpadFragment(boolean animate, boolean clearDialpad) {
        if (mDialpadFragment == null) {
            return;
        }
        if (clearDialpad) {
            mDialpadFragment.clearDialpad();
        }
        if (!mIsDialpadShown) {
            return;
        }
        mIsDialpadShown = false;
        mDialpadFragment.setAnimate(animate);
        mListsFragment.setUserVisibleHint(true);
        mListsFragment.sendScreenViewForCurrentPosition();

        /// M: Support MTK-DialerSearch @{
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            updateSearchFragmentExPosition();
        /// @}
        } else {
            updateSearchFragmentPosition();
        }

        updateFloatingActionButtonControllerAlignment(animate);
        if (animate) {
            mDialpadFragment.getView().startAnimation(mSlideOut);
        } else {
            commitDialpadFragmentHide();
        }

        mActionBarController.onDialpadDown();

        if (isInSearchUi()) {
            if (TextUtils.isEmpty(mSearchQuery)) {
                exitSearchUi();
            }
        }
    }

    /**
     * Finishes hiding the dialpad fragment after any animations are completed.
     */
    private void commitDialpadFragmentHide() {
        if (!mStateSaved && !mDialpadFragment.isHidden()) {
            final FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.hide(mDialpadFragment);
            /// M: Fix CR ALPS01821946. @{
            /* original code:
            ft.commit();
            */
            ft.commitAllowingStateLoss();
            /// @}
        }
        mFloatingActionButtonController.scaleIn(AnimUtils.NO_DELAY);
    }

    private void updateSearchFragmentPosition() {
        SearchFragment fragment = null;
        if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()) {
            fragment = mSmartDialSearchFragment;
        } else if (mRegularSearchFragment != null && mRegularSearchFragment.isVisible()) {
            fragment = mRegularSearchFragment;
        }
        if (fragment != null && fragment.isVisible()) {
            fragment.updatePosition(true /* animate */);
        }
    }

    @Override
    public boolean isInSearchUi() {
        return mInDialpadSearch || mInRegularSearch;
    }

    @Override
    public boolean hasSearchQuery() {
        return !TextUtils.isEmpty(mSearchQuery);
    }

    @Override
    public boolean shouldShowActionBar() {
        return mListsFragment.shouldShowActionBar();
    }

    private void setNotInSearchUi() {
        mInDialpadSearch = false;
        mInRegularSearch = false;
    }

    private void hideDialpadAndSearchUi() {
        if (mIsDialpadShown) {
            hideDialpadFragment(false, true);
        } else {
            exitSearchUi();
        }
    }

    private void prepareVoiceSearchButton() {
        final Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        /**
         * M: [ALPS01748267] set value for view to record the voice search
         * button status @{
         */
        boolean canBeHandled = canIntentBeHandled(voiceIntent);
        SearchEditTextLayout searchBox = (SearchEditTextLayout) getActionBar().getCustomView();
        if (searchBox != null) {
            searchBox.setCanHandleSpeech(canBeHandled);
        }
        /** @} */
        if (canBeHandled) {
            mVoiceSearchButton.setVisibility(View.VISIBLE);
            mVoiceSearchButton.setOnClickListener(this);
        } else {
            mVoiceSearchButton.setVisibility(View.GONE);
        }
    }

    private OptionsPopupMenu buildOptionsMenu(View invoker) {
        /** M: [VoLTE ConfCall] Show conference call menu for volte. @{ */
        final OptionsPopupMenu popupMenu = new OptionsPopupMenu(this, invoker) {
            @Override
            public void show() {
                boolean visible = DialerVolteUtils
                        .isVoLTEConfCallEnable(DialtactsActivity.this);
//qianweiqiang boway 2015 6 27 remove conference call
             //   getMenu().findItem(R.id.menu_volte_conf_call).setVisible(visible);
//qianweiqiang boway 2015 6 27 remove conference call
                super.show();
            }
        };
        /** @} */
        popupMenu.inflate(R.menu.dialtacts_options);
        final Menu menu = popupMenu.getMenu();
        ExtensionManager.getInstance().getDialPadExtension().buildOptionsMenu(this, menu);
        popupMenu.setOnMenuItemClickListener(this);
        return popupMenu;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mPendingSearchViewQuery != null) {
            mSearchView.setText(mPendingSearchViewQuery);
            mPendingSearchViewQuery = null;
        } 
//qianweiqiang Boway 2015 5 9  create new option menu as the menu click    
//        mActionBarController.restoreActionBarOffset();
//        return false;
        getMenuInflater().inflate(R.menu.dialtacts_options, menu);
        return true;
        
    }
//end boway	
    
//qianweiqiang Boway 2015 5 9  create new option menu as the menu click    	
    public boolean onMenuItemSelected(int featureId, MenuItem item) { 
        switch(item.getItemId()) {
        case R.id.menu_history:
            showCallHistory();
        //show call log
        break;

        case R.id.menu_add_contact:
            try {
                startActivity(new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
            } catch (ActivityNotFoundException e) {
                Toast toast = Toast.makeText(this,
                        R.string.add_contact_not_available,
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        //add new contact
           break;
  /** M: [VoLTE ConfCall] handle conference call menu. @{ */
//qianweiqiang boway 2015 6 27 remove conference call
         /*   case R.id.menu_volte_conf_call:
                DialerVolteUtils.handleMenuVolteConfCall(this);
                break;*/
//qianweiqiang boway 2015 6 27 remove conference call
            /** @} */
    
//goto call setting     
        case R.id.menu_call_settings:
            handleMenuSettings();
          break;
        }
        return super.onMenuItemSelected(featureId, item);
        }
//end boway		

    /**
     * Returns true if the intent is due to hitting the green send key (hardware call button:
     * KEYCODE_CALL) while in a call.
     *
     * @param intent the intent that launched this activity
     * @return true if the intent is due to hitting the green send key while in a call
     */
    private boolean isSendKeyWhileInCall(Intent intent) {
        // If there is a call in progress and the user launched the dialer by hitting the call
        // button, go straight to the in-call screen.
        final boolean callKey = Intent.ACTION_CALL_BUTTON.equals(intent.getAction());

        if (callKey) {
            getTelecomManager().showInCallScreen(false);
            return true;
        }

        return false;
    }

    /**
     * Sets the current tab based on the intent's request type
     *
     * @param intent Intent that contains information about which tab should be selected
     */
    private void displayFragment(Intent intent) {
        // If we got here by hitting send and we're in call forward along to the in-call activity
        if (isSendKeyWhileInCall(intent)) {
            finish();
            return;
        }

        if (mDialpadFragment != null) {
            final boolean phoneIsInUse = phoneIsInUse();
            if (phoneIsInUse || (intent.getData() !=  null && isDialIntent(intent))) {
                mDialpadFragment.setStartedFromNewIntent(true);
                if (phoneIsInUse && !mDialpadFragment.isVisible()) {
                    mInCallDialpadUp = true;
                }
                showDialpadFragment(false);
            }
        }
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        setIntent(newIntent);
        mStateSaved = false;
        displayFragment(newIntent);

        invalidateOptionsMenu();
    }

    /** Returns true if the given intent contains a phone number to populate the dialer with */
    private boolean isDialIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || ACTION_TOUCH_DIALER.equals(action)) {
            return true;
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            final Uri data = intent.getData();
            if (data != null && PhoneAccount.SCHEME_TEL.equals(data.getScheme())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an appropriate call origin for this Activity. May return null when no call origin
     * should be used (e.g. when some 3rd party application launched the screen. Call origin is
     * for remembering the tab in which the user made a phone call, so the external app's DIAL
     * request should not be counted.)
     */
    public String getCallOrigin() {
        return !isDialIntent(getIntent()) ? CALL_ORIGIN_DIALTACTS : null;
    }

    /**
     * Shows the search fragment
     */
    private void enterSearchUi(boolean smartDialSearch, String query) {
        if (mStateSaved || getFragmentManager().isDestroyed()) {
            // Weird race condition where fragment is doing work after the activity is destroyed
            // due to talkback being on (b/10209937). Just return since we can't do any
            // constructive here.
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "Entering search UI - smart dial " + smartDialSearch);
        }

        final FragmentTransaction transaction = getFragmentManager().beginTransaction();

        /// M: Support MTK-DialerSearch @{
        SearchFragmentEx enFragment = null;
        SearchFragment fragment = null;

        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            if (mInDialpadSearch && mEnhancedSmartDialSearchFragment != null) {
                transaction.remove(mEnhancedSmartDialSearchFragment);
            } else if (mInRegularSearch && mEnhancedRegularSearchFragment != null) {
                transaction.remove(mEnhancedRegularSearchFragment);
            }
        /// @}
        } else {
            if (mInDialpadSearch && mSmartDialSearchFragment != null) {
                transaction.remove(mSmartDialSearchFragment);
            } else if (mInRegularSearch && mRegularSearchFragment != null) {
                transaction.remove(mRegularSearchFragment);
            }
        }

        final String tag;
        if (smartDialSearch) {
            tag = TAG_SMARTDIAL_SEARCH_FRAGMENT;
        } else {
            tag = TAG_REGULAR_SEARCH_FRAGMENT;
        }
        mInDialpadSearch = smartDialSearch;
        mInRegularSearch = !smartDialSearch;

        /// M: Support MTK-DialerSearch @{
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            enFragment = (SearchFragmentEx) getFragmentManager().findFragmentByTag(tag);
        } else {
        /// @}
            fragment = (SearchFragment) getFragmentManager().findFragmentByTag(tag);
        }

        transaction.setCustomAnimations(android.R.animator.fade_in, 0);

        /// M: Support MTK-DialerSearch @{
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            if (enFragment == null) {
                if (smartDialSearch) {
                    enFragment = new SmartDialSearchFragmentEx();
                } else {
                    enFragment = new RegularSearchFragmentEx();
                }
                transaction.add(R.id.dialtacts_frame, enFragment, tag);
            } else {
                transaction.show(enFragment);
            }

            enFragment.setHasOptionsMenu(false);
            enFragment.setShowEmptyListForNullQuery(true);
            enFragment.setQueryString(query, false /* delaySelection */);
        /// @}
        } else {
            if (fragment == null) {
                if (smartDialSearch) {
                    fragment = new SmartDialSearchFragment();
                } else {
                    fragment = new RegularSearchFragment();
                }
                transaction.add(R.id.dialtacts_frame, fragment, tag);
            } else {
                transaction.show(fragment);
            }
            // DialtactsActivity will provide the options menu
            fragment.setHasOptionsMenu(false);
            fragment.setShowEmptyListForNullQuery(true);
            fragment.setQueryString(query, false /* delaySelection */);
        }
        /// M: for ALPS01763072 @{
        // avoid illegalstate exception in fragment
        // transaction.commit();
        transaction.commitAllowingStateLoss();
        /// @}

        mListsFragment.getView().animate().alpha(0).withLayer();
        mListsFragment.setUserVisibleHint(false);
    }

    /**
     * Hides the search fragment
     */
    private void exitSearchUi() {
        // See related bug in enterSearchUI();
        if (getFragmentManager().isDestroyed() || mStateSaved) {
            return;
        }
   //qianweiqiang add 
        if(mCurrentTabPosition == ListsFragment.TAB_INDEX_ALL_CONTACTS){
           // mActionBarHeight = 0;
            //BOWAY 2016 3 31 getActionBar().hide();
           // searchEditTextLayout.setVisibility(View.GONE);
        }//

        mSearchView.setText(null);
        mDialpadFragment.clearDialpad();
        setNotInSearchUi();

        final FragmentTransaction transaction = getFragmentManager().beginTransaction();

        /// M: Support MTK-DialerSearch @{
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            if (mEnhancedSmartDialSearchFragment != null) {
                transaction.remove(mEnhancedSmartDialSearchFragment);
            }
            if (mEnhancedRegularSearchFragment != null) {
                transaction.remove(mEnhancedRegularSearchFragment);
            }
        /// @}
        } else {
            if (mSmartDialSearchFragment != null) {
                transaction.remove(mSmartDialSearchFragment);
            }
            if (mRegularSearchFragment != null) {
                transaction.remove(mRegularSearchFragment);
            }
        }
        /// M: fix CR:ALPS01798991, use commitAllowingStateLoss() instead of commit(). @{
        /**
         * original code: transaction.commit();
         */
        transaction.commitAllowingStateLoss();
        /// @}

        mListsFragment.getView().animate().alpha(1).withLayer();
        if (!mDialpadFragment.isVisible()) {
            // If the dialpad fragment wasn't previously visible, then send a screen view because
            // we are exiting regular search. Otherwise, the screen view will be sent by
            // {@link #hideDialpadFragment}.
            mListsFragment.sendScreenViewForCurrentPosition();
            mListsFragment.setUserVisibleHint(true);
        }

        mActionBarController.onSearchUiExited();
    }

    /** Returns an Intent to launch Call Settings screen */
    public static Intent getCallSettingsIntent() {
        final Intent intent = new Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public void onBackPressed() {
        if (mStateSaved) {
            return;
        }
        if (mIsDialpadShown) {

            /// M: Support MTK-DialerSearch @{
            if (DialerFeatureOptions.isDialerSearchEnabled()) {
                if (TextUtils.isEmpty(mSearchQuery) ||
                        (mEnhancedSmartDialSearchFragment != null && mEnhancedSmartDialSearchFragment.isVisible()
                                && mEnhancedSmartDialSearchFragment.getAdapter().getCount() == 0)) {
                    exitSearchUi();
                }
            /// @}
            } else {
                if (TextUtils.isEmpty(mSearchQuery) ||
                        (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()
                                && mSmartDialSearchFragment.getAdapter().getCount() == 0)) {
                    exitSearchUi();
                }
            }
            hideDialpadFragment(true, false);
        } else if (isInSearchUi()) {
            exitSearchUi();
            DialerUtils.hideInputMethod(mParentLayout);
        /// M: for ALPS01814904 @{
        // for better performance do not finish in this case
        } else if (!DialerFeatureOptions.isPerfResponseTimeEnabled() && isTaskRoot()) {
            /// M: for ALPS01933053 rapidly double back press may lead to DialpadFragment
             // slide out animation fail to end. The onAnimationEnd() will be called as soon
             // as next DialpadFragment show up and then make it hidden again at once
            mDialpadFragment.getView().clearAnimation();
            moveTaskToBack(false);
            Log.d(TAG, "onBackPressed, moveTaskToBack~");
        /// @}
        } else {
            super.onBackPressed();
        }
    }

    /**
     * @return True if the search UI was exited, false otherwise
     */
    private boolean maybeExitSearchUi() {
        if (isInSearchUi() && TextUtils.isEmpty(mSearchQuery)) {
            exitSearchUi();
            DialerUtils.hideInputMethod(mParentLayout);
            return true;
        }
        return false;
    }

    @Override
    public void onDialpadQueryChanged(String query) {

        Log.d(TAG, "---query---:" + query);
        /// M: Support MTK-DialerSearch @{
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            if (mEnhancedSmartDialSearchFragment != null) {
                mEnhancedSmartDialSearchFragment.setAddToContactNumber(query);
            }
        /// @}
        } else {
            if (mSmartDialSearchFragment != null) {
                mSmartDialSearchFragment.setAddToContactNumber(query);
            }
        }
        final String normalizedQuery = SmartDialNameMatcher.normalizeNumber(query,
                SmartDialNameMatcher.LATIN_SMART_DIAL_MAP);

        if (!TextUtils.equals(mSearchView.getText(), normalizedQuery)) {
            if (DEBUG) {
                Log.d(TAG, "onDialpadQueryChanged - new query: " + query);
            }
            if (mDialpadFragment == null || !mDialpadFragment.isVisible()) {
                // This callback can happen if the dialpad fragment is recreated because of
                // activity destruction. In that case, don't update the search view because
                // that would bring the user back to the search fragment regardless of the
                // previous state of the application. Instead, just return here and let the
                // fragment manager correctly figure out whatever fragment was last displayed.
                if (!TextUtils.isEmpty(normalizedQuery)) {
                    mPendingSearchViewQuery = normalizedQuery;
                }
                return;
            }
            mSearchView.setText(normalizedQuery);
        }
    }

    @Override
    public void onListFragmentScrollStateChange(int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            hideDialpadFragment(true, false);
            DialerUtils.hideInputMethod(mParentLayout);
        }
    }

    @Override
    public void onListFragmentScroll(int firstVisibleItem, int visibleItemCount,
                                     int totalItemCount) {
        // TODO: No-op for now. This should eventually show/hide the actionBar based on
        // interactions with the ListsFragments.
    }

    private boolean phoneIsInUse() {
        return getTelecomManager().isInCall();
    }

    public static Intent getAddNumberToContactIntent(CharSequence text) {
        return getAddToContactIntent(null /* name */, text /* phoneNumber */,
                -1 /* phoneNumberType */);
    }

    public static Intent getAddToContactIntent(CharSequence name, CharSequence phoneNumber,
            int phoneNumberType) {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Intents.Insert.PHONE, phoneNumber);
        // Only include the name and phone type extras if they are specified (the method
        // getAddNumberToContactIntent does not use them).
        if (name != null) {
            intent.putExtra(Intents.Insert.NAME, name);
        }
        if (phoneNumberType != -1) {
            intent.putExtra(Intents.Insert.PHONE_TYPE, phoneNumberType);
        }
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        return intent;
    }

    private boolean canIntentBeHandled(Intent intent) {
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.size() > 0;
    }

    @Override
    public void showCallHistory() {
        // Use explicit CallLogActivity intent instead of ACTION_VIEW +
        // CONTENT_TYPE, so that we always open our call log from our dialer
        final Intent intent = new Intent(this, CallLogActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user has long-pressed a contact tile to start a drag operation.
     */
    @Override
    public void onDragStarted(int x, int y, PhoneFavoriteSquareTileView view) {
        if (mListsFragment.isPaneOpen()) {
            mActionBarController.setAlpha(ListsFragment.REMOVE_VIEW_SHOWN_ALPHA);
        }
        mListsFragment.showRemoveView(true);
    }

    @Override
    public void onDragHovered(int x, int y, PhoneFavoriteSquareTileView view) {
    }

    /**
     * Called when the user has released a contact tile after long-pressing it.
     */
    @Override
    public void onDragFinished(int x, int y) {
        if (mListsFragment.isPaneOpen()) {
            mActionBarController.setAlpha(ListsFragment.REMOVE_VIEW_HIDDEN_ALPHA);
        }
        mListsFragment.showRemoveView(false);
    }

    @Override
    public void onDroppedOnRemove() {}

    /**
     * Allows the SpeedDialFragment to attach the drag controller to mRemoveViewContainer
     * once it has been attached to the activity.
     */
    @Override
    public void setDragDropController(DragDropController dragController) {
        mDragDropController = dragController;
        mListsFragment.getRemoveView().setDragDropController(dragController);
    }

    @Override
    public void onPickPhoneNumberAction(Uri dataUri) {
        // Specify call-origin so that users will see the previous tab instead of
        // CallLog screen (search UI will be automatically exited).
        PhoneNumberInteraction.startInteractionForPhoneCall(
                DialtactsActivity.this, dataUri, getCallOrigin());
        mClearSearchOnPause = true;
    }

    @Override
    public void onCallNumberDirectly(String phoneNumber) {
        onCallNumberDirectly(phoneNumber, false /* isVideoCall */);
    }

    @Override
    public void onCallNumberDirectly(String phoneNumber, boolean isVideoCall) {
        Intent intent = isVideoCall ?
                CallUtil.getVideoCallIntent(phoneNumber, getCallOrigin()) :
                CallUtil.getCallIntent(phoneNumber, getCallOrigin());
        DialerUtils.startActivityWithErrorToast(this, intent);
        mClearSearchOnPause = true;
    }

    @Override
    public void onShortcutIntentCreated(Intent intent) {
        Log.w(TAG, "Unsupported intent has come (" + intent + "). Ignoring.");
    }

    @Override
    public void onHomeInActionBarSelected() {
        exitSearchUi();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        position = mListsFragment.getRtlPosition(position);
        // Only scroll the button when the first tab is selected. The button should scroll from
        // the middle to right position only on the transition from the first tab to the second
        // tab.
        // If the app is in RTL mode, we need to check against the second tab, rather than the
        // first. This is because if we are scrolling between the first and second tabs, the
        // viewpager will report that the starting tab position is 1 rather than 0, due to the
        // reversal of the order of the tabs.
        final boolean isLayoutRtl = DialerUtils.isRtl();
        final boolean shouldScrollButton = position == (isLayoutRtl
                ? ListsFragment.TAB_INDEX_RECENTS : ListsFragment.TAB_INDEX_SPEED_DIAL);
        if (shouldScrollButton && !mIsLandscape) {
            mFloatingActionButtonController.onPageScrolled(
                    isLayoutRtl ? 1 - positionOffset : positionOffset);
        } else if (position != ListsFragment.TAB_INDEX_RECENTS/*qianweiqiang set TAB_INDEX_SPEED_DIAL*/) {
            mFloatingActionButtonController.onPageScrolled(1);
        }
//qianweiqiang Boway 2015 5 9  not show the floating button in other tab except dialer tab
        if(position == ListsFragment.TAB_INDEX_ALL_CONTACTS)
        {
            exitSearchUi();
            mFloatingActionButtonController.setVisible(false);
        }//add end
      //
        if(position == ListsFragment.TAB_INDEX_RECENTS)
        {
            //mFloatingActionButtonController.setVisible(false);
            //showDialpadFragment(true);
        }//add end
        if(position == ListsFragment.TAB_INDEX_SPEED_DIAL)
        {
            mFloatingActionButtonController.setVisible(false);
        }
//end boway        
    }

    @Override
    public void onPageSelected(int position) {
        position = mListsFragment.getRtlPosition(position);
        mCurrentTabPosition = position;
//qianweiqiang add 
        mFloatingActionButtonController.setVisible(false);
        if(position == ListsFragment.TAB_INDEX_RECENTS)
        {
            showDialpadFragment(true);    
        }
      //qianweiqiang set search view 2015 5 27 
        if(position == ListsFragment.TAB_INDEX_ALL_CONTACTS)
        {
         //   exitSearchUi();
        }else
        {
            //BOWAY 2016 3  31getActionBar().hide();
           // searchEditTextLayout.setVisibility(View.GONE);
        }
        
        
        
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
    }

    @Override
    public boolean isActionBarShowing() {
        return mActionBarController.isActionBarShowing();
    }

    @Override
    public ActionBarController getActionBarController() {
        return mActionBarController;
    }

    public boolean isDialpadShown() {
        return mIsDialpadShown;
    }

    @Override
    public int getActionBarHideOffset() {
        return getActionBar().getHideOffset();
    }

    @Override
    public void setActionBarHideOffset(int offset) {
        getActionBar().setHideOffset(offset);
    }

    @Override
    public int getActionBarHeight() {
        return mActionBarHeight;
    }

    /**
     * Updates controller based on currently known information.
     *
     * @param animate Whether or not to animate the transition.
     */
    private void updateFloatingActionButtonControllerAlignment(boolean animate) {
        int align = (!mIsLandscape && mCurrentTabPosition == ListsFragment.TAB_INDEX_RECENTS/*qianweiqiang set TAB_INDEX_SPEED_DIAL*/) ?
                FloatingActionButtonController.ALIGN_MIDDLE :
                        FloatingActionButtonController.ALIGN_END;
        mFloatingActionButtonController.align(align, 0 /* offsetX */, 0 /* offsetY */, animate);
    }

    //-------------------------------------------------MTK -------------------------------------------------
    /// M: Support MTK-DialerSearch @{
    private SmartDialSearchFragmentEx mEnhancedSmartDialSearchFragment;
    private RegularSearchFragmentEx mEnhancedRegularSearchFragment;

    private void updateSearchFragmentExPosition() {
        SearchFragmentEx enFragment = null;
        if (mEnhancedSmartDialSearchFragment != null && mEnhancedSmartDialSearchFragment.isVisible()) {
            enFragment = mEnhancedSmartDialSearchFragment;
        } else if (mEnhancedRegularSearchFragment != null && mEnhancedRegularSearchFragment.isVisible()) {
            enFragment = mEnhancedRegularSearchFragment;
        }
        if (enFragment != null && enFragment.isVisible()) {
            enFragment.updatePosition(true /* animate */);
        }
    }

    
    class EnterRegularSearchReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if(intent.getAction() != null)
            {
                if(intent.getAction().equals("ACTION_GO_TO_REGULAR_SEARCH"))
                {
                    mActionBarHeight = 80;
                    getActionBar().show();
                    prepareVoiceSearchButton();
                    searchEditTextLayout.setVisibility(View.VISIBLE);
                    mActionBarController.onSearchBoxTapped();
                    enterSearchUi(false /* smartDialSearch */, mSearchView.getText().toString()); 
                }
            }
        }
          
      }

    /// @}
    
    
    
    //BEGIN BOWAY weiqiang.qian 20150908 a Receiver to receive the SIM card insert or pulled broadcast
    public class SimStateReceiver extends BroadcastReceiver {  
        private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";  
        private final static int SIM_VALID = 0;  
        private final static int SIM_INVALID = 1;  
        private int simState = SIM_INVALID;  
          
        public int getSimState() {  
            return simState;  
        }  
      
        @Override  
        public void onReceive(Context context, Intent intent) {  
            System.out.println("sim state changed");  
            if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {  
                TelephonyManager tm = (TelephonyManager)context.getSystemService(Service.TELEPHONY_SERVICE);   
                int state = tm.getSimState();  
                //isTwoSimInserted = isDualSimCard();
                switch (state) {  
                case TelephonyManager.SIM_STATE_READY :  
                    simState = SIM_VALID;  
                    break;  
                case TelephonyManager.SIM_STATE_UNKNOWN :  
                case TelephonyManager.SIM_STATE_ABSENT :
                	simState = SIM_INVALID;  
					//refreshDialPad();
                	break;
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED :  
                default:  
                    simState = SIM_INVALID;  
                    break;  
                }  
            if(simState == SIM_INVALID)
            {
            	//if(!isDualSimCard()){ 
            	if(isDialpadShown()){
				mDialpadFragment.clearDialpad();
            	//hideDialpadFragment(false, false);
            	}
            	
            	if(isActivityShow){
            	refreshDialPad();
            	} else
            	  {
            		//case when the activity is not on the focus or activity is background 
            		needRefresh = true;
            	  }
            	//}
            }
            
            }  
        }
    }
    
    
    //BEGIN BOWAY weiqiang.qian use to refresh the dialpad fragment 2015 09 08 
    private void refreshDialPad()
    {
    	
    	//final FragmentTransaction ft = getFragmentManager().beginTransaction();
    	//DialpadFragment baker = mDialpadFragment;
    	if(isDualSimCard()) {
    	mDialpadFragment.updateDoubleCardView();
    	}else{
    	mDialpadFragment.updateSingleCardView();	
    	}
    	//ft.replace(R.id.dialtacts_container, mDialpadFragment);
    	//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    	//ft.commit();
    }
    //END BOWAY 2015 0908

 //BEGIN BOWAY weiqiang.qian 2015 8 27 set double card
 	public boolean isDualSimCard() {
 		TelephonyManagerEx tm = TelephonyManagerEx.getDefault();
 		String i0 = tm.getSimSerialNumber(0);
 		String i1 = tm.getSimSerialNumber(1);
 		if (i0 != null && i1 != null) {
 			return true;
 		}
 		return false;
 	}
     	//END BOWAY
    
    
}
