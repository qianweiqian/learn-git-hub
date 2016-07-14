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

import android.app.KeyguardManager;
import android.content.Context;

import java.util.List;

/**
 * Presenter for the Incoming call widget.
 */
public class SimpleAnswerPresenter extends Presenter<SimpleAnswerPresenter.AnswerUi>
        implements CallList.CallUpdateListener, CallList.Listener {

    private static final String TAG = SimpleAnswerPresenter.class.getSimpleName();

    private String mCallId;
    private Call mCall = null;

    @Override
    public void onUiReady(AnswerUi ui) {
        super.onUiReady(ui);

        final CallList calls = CallList.getInstance();
        Call call;
        call = calls.getIncomingCall();
        if (call != null) {
            processIncomingCall(call);
        }
        call = calls.getVideoUpgradeRequestCall();
        if (call != null) {
            processVideoUpgradeRequestCall(call);
        }

        // Listen for incoming calls.
        calls.addListener(this);
    }

    @Override
    public void onUiUnready(AnswerUi ui) {
        super.onUiUnready(ui);

        CallList.getInstance().removeListener(this);

        // This is necessary because the activity can be destroyed while an incoming call exists.
        // This happens when back button is pressed while incoming call is still being shown.
        if (mCallId != null) {
            CallList.getInstance().removeCallUpdateListener(mCallId, this);
        }
    }

    @Override
    public void onCallListChange(CallList callList) {
        /// M: Added for DSDA case. @{
        if (getUi() == null) {
            Log.d(this, "onCallListChange, ui is null, do nothing! ");
            return;
        }

        // Show prompt message to user if needed.
        Call call = callList.getIncomingCall();
        Log.d(this, "onCallListChange " + call);
        if (call != null) {
            // When there has two incoming calls, after user switch these two
            // calls, we need update incoming call at here.
            if (!call.getId().equals(mCallId)) {
                processIncomingCall(call);
            }
        } 
        /// @}
    }

    @Override
    public void onDisconnect(Call call) {
        // no-op
    }

    @Override
    public void onIncomingCall(Call call) {
        // TODO: Ui is being destroyed when the fragment detaches.  Need clean up step to stop
        // getting updates here.
        Log.d(this, "onIncomingCall: " + this);
        if (getUi() != null) {
            if (!call.getId().equals(mCallId)) {
                // A new call is coming in.
                // M: when another incoming call coming, need dismiss dialog.
                processIncomingCall(call);
            }
        }
    }

    private void processIncomingCall(Call call) {
        mCallId = call.getId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);

        Log.d(TAG, "Showing incoming for call id: " + mCallId + " " + this);
        getUi().showAnswerUi(true);
    }

    private void processVideoUpgradeRequestCall(Call call) {
        /*mCallId = call.getId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);
        getUi().showAnswerUi(true);*/

        //getUi().showTargets(SimpleAnswerFragment.TARGET_SET_FOR_VIDEO_UPGRADE_REQUEST);
    }

    @Override
    public void onCallChanged(Call call) {
        Log.d(this, "onCallStateChange() " + call + " " + this);
        if (call.getState() != Call.State.INCOMING) {
            // Stop listening for updates.
            CallList.getInstance().removeCallUpdateListener(mCallId, this);

            final Call incomingCall = CallList.getInstance().getIncomingCall();
            if (incomingCall != null) {
                processIncomingCall(incomingCall);
                return;
            }
            
            getUi().showAnswerUi(false);

            // mCallId will hold the state of the call. We don't clear the mCall variable here as
            // it may be useful for sending text messages after phone disconnects.
            mCallId = null;
        } 
    }

    public void onAnswer(int videoState) {
        if (mCallId == null) {
            return;
        }

        Log.d(this, "onAnswer " + mCallId);
        TelecomAdapter.getInstance().answerCall(mCall.getId(), videoState);
    }

    /**
     * TODO: We are using reject and decline interchangeably. We should settle on
     * reject since it seems to be more prevalent.
     */
    public void onDecline() {
        Log.d(this, "onDecline " + mCallId);
        TelecomAdapter.getInstance().rejectCall(mCall.getId(), false, null);
    }

    interface AnswerUi extends Ui {
        public void showAnswerUi(boolean show);
        public Context getContext();
    }

    @Override
    public void onStorageFull() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUpdateRecordState(int state, int customValue) {
        // TODO Auto-generated method stub
    }
    
}
