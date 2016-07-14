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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telecom.VideoProfile;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.incallui.ContactInfoCache.ContactCacheEntry;
import com.android.phone.common.widget.ResizingTextTextView;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class WindowAnswerFragment extends
		BaseFragment<WindowAnswerPresenter, WindowAnswerPresenter.AnswerUi>
		implements WindowAnswerPresenter.AnswerUi {	

	private ImageButton mAnswerButton;
	private ImageButton mDeclineButton;
	private RelativeLayout mAnswerButtonContainer;

	public WindowAnswerFragment() {
	}

	@Override
	public WindowAnswerPresenter createPresenter() {
		return new WindowAnswerPresenter();
	}

	@Override
	WindowAnswerPresenter.AnswerUi getUi() {
		return this;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View parent = inflater.inflate(R.layout.window_answer_fragment,
				container, false);
		mAnswerButton = (ImageButton) parent.findViewById(R.id.answer_btn);
		mDeclineButton = (ImageButton) parent.findViewById(R.id.decline_btn);
		mAnswerButtonContainer = (RelativeLayout) parent.findViewById(R.id.openWindowIncomingCallLy);
		mAnswerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getPresenter().onAnswer(VideoProfile.VideoState.AUDIO_ONLY);
				showAnswerUi(false);
			}
		});
		mDeclineButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getPresenter().onDecline();
				showAnswerUi(false);
			}
		});

		Log.d(this, "Creating view for answer fragment ", this);
		Log.d(this, "Created from activity", getActivity());

		return parent;
	}

	@Override
	public void onDestroyView() {
		Log.d(this, "onDestroyView");
		super.onDestroyView();
	}

	@Override
	public void showAnswerUi(boolean show) {
		mAnswerButtonContainer.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@Override
	public Context getContext() {
		return getActivity();
	}
	
}
