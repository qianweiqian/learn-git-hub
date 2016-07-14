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

package com.android.dialer;

import java.util.HashMap;
import java.util.Map;


import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.extensions.ExtensionsFactory;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.mediatek.dialer.calllog.PhoneAccountInfoHelper;
import com.mediatek.dialer.dialersearch.DialerSearchHelper;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;

//qianweiqiang add 
import com.android.dialer.R;

public class DialerApplication extends Application {

    private ContactPhotoManager mContactPhotoManager;
    private static Context sContext;
    
  //qianweiqiang add for dtmf new sound 4 16 
    private Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    private SoundPool spool;
    //private AudioManager am;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        /// M: for ALPS01907201, init GlobalEnv
        GlobalEnv.setApplicationContext(sContext);
        ExtensionsFactory.init(sContext);
        AnalyticsUtil.initialize(this);
        PhoneAccountInfoHelper.INSTANCE.init(sContext);
        /// M: for Plug-in @{
        ExtensionManager.getInstance().init(this);
        com.mediatek.contacts.ExtensionManager.registerApplicationContext(this);
        /// @}

        /// M: for ALPS01762713 @{
        // workaround for code defect in ContactsPreferences, init it in main tread
        DialerSearchHelper.initContactsPreferences(sContext);
        
       // boolean plugStarted = PutaoSdkManager.getInstance().initPlug(this.getApplicationContext());
        //am = (AudioManager) sContext.getSystemService(Context.AUDIO_SERVICE);
        //qianweiqiang add for dtmf new sound 4 16 
        spool = new SoundPool(12, AudioManager.STREAM_SYSTEM, 5);
        //qianweiqiang set piano tone in new thread to speed up 2015 6 23
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try{
                    map.put(0, spool.load(sContext, R.raw.dtmf0, 0));
                    map.put(1, spool.load(sContext, R.raw.dtmf1, 0));
                    map.put(2, spool.load(sContext, R.raw.dtmf2, 0));
                    map.put(3, spool.load(sContext, R.raw.dtmf3, 0));
                    map.put(4, spool.load(sContext, R.raw.dtmf4, 0));
                    map.put(5, spool.load(sContext, R.raw.dtmf5, 0));
                    map.put(6, spool.load(sContext, R.raw.dtmf6, 0));
                    map.put(7, spool.load(sContext, R.raw.dtmf7, 0));
                    map.put(8, spool.load(sContext, R.raw.dtmf8, 0));
                    map.put(9, spool.load(sContext, R.raw.dtmf9, 0));
                    map.put(10, spool.load(sContext, R.raw.dtmf11, 0));
                    map.put(11, spool.load(sContext, R.raw.dtmf12, 0));
                    } catch(Exception e)
                    {
                        e.printStackTrace();
                    } 
            }
        }).start();
        //end
        /// @}
    }

    public SoundPool getSoundPool()
    {
        return spool;
    }
    
    @Override
    public Object getSystemService(String name) {
        if (ContactPhotoManager.CONTACT_PHOTO_SERVICE.equals(name)) {
            if (mContactPhotoManager == null) {
                mContactPhotoManager = ContactPhotoManager.createContactPhotoManager(this);
                registerComponentCallbacks(mContactPhotoManager);
                mContactPhotoManager.preloadPhotosInBackground();
            }
            return mContactPhotoManager;
        }

        return super.getSystemService(name);
    }

    public static Context getDialerContext() {
        return sContext;
    }
}
