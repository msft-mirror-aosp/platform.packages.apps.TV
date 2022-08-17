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
import android.media.tv.interactive.TvInteractiveAppView;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.android.tv.MainActivity;
import com.android.tv.R;
import com.android.tv.common.SoftPreconditions;
import com.android.tv.features.TvFeatures;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TargetApi(Build.VERSION_CODES.TIRAMISU)
public class IAppManager {
    private static final String TAG = "IAppManager";

    private final MainActivity mMainActivity;
    private final TvInteractiveAppManager mTvIAppManager;
    private final TvInteractiveAppView mTvIAppView;

    public IAppManager(MainActivity parentActivity) {
        SoftPreconditions.checkFeatureEnabled(parentActivity, TvFeatures.HAS_TIAF, TAG);

        mMainActivity = parentActivity;
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
                int err) {}
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
