/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.tv.interactive;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.media.tv.interactive.TvInteractiveAppManager;
import android.media.tv.AitInfo;
import android.media.tv.interactive.TvInteractiveAppServiceInfo;
import android.media.tv.interactive.TvInteractiveAppView;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.tv.MainActivity;
import com.android.tv.R;
import com.android.tv.common.SoftPreconditions;
import com.android.tv.features.TvFeatures;
import com.android.tv.ui.TunableTvView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TargetApi(Build.VERSION_CODES.TIRAMISU)
public class IAppManager {
    private static final String TAG = "IAppManager";
    private static final boolean DEBUG = false;

    private final MainActivity mMainActivity;
    private final TvInteractiveAppManager mTvIAppManager;
    private final TvInteractiveAppView mTvIAppView;
    private final TunableTvView mTvView;
    private AitInfo mCurrentAitInfo;

    public IAppManager(MainActivity parentActivity, TunableTvView tvView) {
        SoftPreconditions.checkFeatureEnabled(parentActivity, TvFeatures.HAS_TIAF, TAG);

        mMainActivity = parentActivity;
        mTvView = tvView;
        mTvIAppManager = mMainActivity.getSystemService(TvInteractiveAppManager.class);
        mTvIAppView = mMainActivity.findViewById(R.id.tv_app_view);
        if (mTvIAppManager == null || mTvIAppView == null) {
            Log.e(TAG, "Could not find interactive app view or manager");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        mTvIAppManager.registerCallback(
                executor,
                new MyInteractiveAppManagerCallback()
        );
        mTvIAppView.setCallback(
                executor,
                new MyInteractiveAppViewCallback()
        );
    }

    public void stop() {
        mTvIAppView.stopInteractiveApp();
        mTvIAppView.reset();
        mCurrentAitInfo = null;
    }

    public void onAitInfoUpdated(AitInfo aitInfo) {
        if (mTvIAppManager == null || aitInfo == null) {
            return;
        }
        if (mCurrentAitInfo != null && mCurrentAitInfo.getType() == aitInfo.getType()) {
            if (DEBUG) {
                Log.d(TAG, "Ignoring AIT update: Same type as current");
            }
            return;
        }

        List<TvInteractiveAppServiceInfo> tvIAppInfoList =
                mTvIAppManager.getTvInteractiveAppServiceList();
        if (tvIAppInfoList.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "Ignoring AIT update: No interactive app services registered");
            }
            return;
        }

        // App Type ID numbers allocated by DVB Services
        int type = -1;
        switch (aitInfo.getType()) {
            case 0x0010: // HBBTV
                type = TvInteractiveAppServiceInfo.INTERACTIVE_APP_TYPE_HBBTV;
                break;
            case 0x0006: // DCAP-J: DCAP Java applications
            case 0x0007: // DCAP-X: DCAP XHTML applications
                type = TvInteractiveAppServiceInfo.INTERACTIVE_APP_TYPE_ATSC;
                break;
            case 0x0001: // Ginga-J
            case 0x0009: // Ginga-NCL
            case 0x000b: // Ginga-HTML5
                type = TvInteractiveAppServiceInfo.INTERACTIVE_APP_TYPE_GINGA;
                break;
            default:
                Log.e(TAG, "AIT info contained unknown type: " + aitInfo.getType());
                return;
        }

        // TODO: Only open interactive app if enabled through settings
        for (TvInteractiveAppServiceInfo info : tvIAppInfoList) {
            if ((info.getSupportedTypes() & type) > 0) {
                mCurrentAitInfo = aitInfo;
                if (mTvIAppView != null) {
                    mTvIAppView.setVisibility(View.VISIBLE);
                    mTvIAppView.prepareInteractiveApp(info.getId(), type);
                }
                break;
            }
        }
    }

    private class MyInteractiveAppManagerCallback extends
            TvInteractiveAppManager.TvInteractiveAppCallback {
        @Override
        public void onInteractiveAppServiceAdded(String iAppServiceId) {}

        @Override
        public void onInteractiveAppServiceRemoved(String iAppServiceId) {}

        @Override
        public void onInteractiveAppServiceUpdated(String iAppServiceId) {}

        @Override
        public void onTvInteractiveAppServiceStateChanged(String iAppServiceId, int type, int state,
                int err) {
            if (state == TvInteractiveAppManager.SERVICE_STATE_READY && mTvIAppView != null) {
                mTvIAppView.startInteractiveApp();
                mTvIAppView.setTvView(mTvView.getTvView());
                if (mTvView.getTvView() != null) {
                    mTvView.getTvView().setInteractiveAppNotificationEnabled(true);
                }
            }
        }
    }

    private class MyInteractiveAppViewCallback extends
            TvInteractiveAppView.TvInteractiveAppCallback {
        @Override
        public void onPlaybackCommandRequest(String iAppServiceId, String cmdType,
                Bundle parameters) {}

        @Override
        public void onStateChanged(String iAppServiceId, int state, int err) {}

        @Override
        public void onBiInteractiveAppCreated(String iAppServiceId, Uri biIAppUri,
                String biIAppId) {}

        @Override
        public void onTeletextAppStateChanged(String iAppServiceId, int state) {}

        @Override
        public void onSetVideoBounds(String iAppServiceId, Rect rect) {}

        @Override
        public void onRequestCurrentChannelUri(String iAppServiceId) {}

        @Override
        public void onRequestCurrentChannelLcn(String iAppServiceId) {}

        @Override
        public void onRequestStreamVolume(String iAppServiceId) {}

        @Override
        public void onRequestTrackInfoList(String iAppServiceId) {}

        @Override
        public void onRequestCurrentTvInputId(String iAppServiceId) {}

        @Override
        public void onRequestSigning(String iAppServiceId, String signingId, String algorithm,
                String alias, byte[] data) {}
    }
}
