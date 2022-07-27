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

package com.android.tv.samples.sampletvinteractiveappservice;

import android.content.Context;
import android.media.tv.TvTrackInfo;
import android.media.tv.interactive.TvInteractiveAppManager;
import android.media.tv.interactive.TvInteractiveAppService;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import java.util.List;

public class TiasSessionImpl extends TvInteractiveAppService.Session {
    private static final String TAG = "SampleTvInteractiveAppService";
    private static final boolean DEBUG = true;

    private final Handler mHandler;
    private final int mType;

    public TiasSessionImpl(Context context, String iAppServiceId, int type) {
        super(context);
        if (DEBUG) {
            Log.d(TAG, "Constructing service with iAppServiceId=" + iAppServiceId
                    + " type=" + type);
        }
        mType = type;
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void onRelease() {
        if (DEBUG) {
            Log.d(TAG, "onRelease");
        }
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        if (DEBUG) {
            Log.d(TAG, "onSetSurface");
        }
        return false;
    }

    @Override
    public void onStartInteractiveApp() {
        if (DEBUG) {
            Log.d(TAG, "onStartInteractiveApp");
        }
        mHandler.post(
                () -> {
                    requestCurrentTvInputId();
                    requestCurrentChannelUri();
                    requestTrackInfoList();
                }
        );
    }

    @Override
    public void onStopInteractiveApp() {
        if (DEBUG) {
            Log.d(TAG, "onStopInteractiveApp");
        }
    }

    public void prepare(TvInteractiveAppService serviceCaller) {
        // Slightly delay our post to ensure the Manager has had time to register our Session
        mHandler.postDelayed(
                () -> {
                    if (serviceCaller != null) {
                        serviceCaller.notifyStateChanged(mType,
                                TvInteractiveAppManager.SERVICE_STATE_READY,
                                TvInteractiveAppManager.ERROR_NONE);
                    }
                },
                100);
    }

    @Override
    public void onCurrentChannelUri(Uri channelUri) {
        if (DEBUG) {
            Log.d(TAG, "onCurrentChannelUri uri=" + channelUri);
        }
    }

    @Override
    public void onTrackInfoList(List<TvTrackInfo> tracks) {
        if (DEBUG) {
            Log.d(TAG, "onTrackInfoList size=" + tracks.size());
            for (int i = 0; i < tracks.size(); i++) {
                TvTrackInfo trackInfo = tracks.get(i);
                if (trackInfo != null) {
                    Log.d(TAG, "track " + i + ": type=" + trackInfo.getType() +
                            " id=" + trackInfo.getId());
                }
            }
        }
    }

    @Override
    public void onCurrentTvInputId(String inputId) {
        if (DEBUG) {
            Log.d(TAG, "onCurrentTvInputId id=" + inputId);
        }
    }
}
