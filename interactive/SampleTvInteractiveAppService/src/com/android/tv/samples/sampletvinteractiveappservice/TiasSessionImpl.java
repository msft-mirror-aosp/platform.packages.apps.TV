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

import android.app.Presentation;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.tv.TvTrackInfo;
import android.media.tv.interactive.TvInteractiveAppManager;
import android.media.tv.interactive.TvInteractiveAppService;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class TiasSessionImpl extends TvInteractiveAppService.Session {
    private static final String TAG = "SampleTvInteractiveAppService";
    private static final boolean DEBUG = true;

    private static final String VIRTUAL_DISPLAY_NAME = "sample_tias_display";

    private final Context mContext;
    private final Handler mHandler;
    private final String mAppServiceId;
    private final int mType;
    private final ViewGroup mViewContainer;
    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;

    private TextView mTvInputIdView;
    private TextView mChannelUriView;
    private TextView mVideoTrackView;
    private TextView mAudioTrackView;
    private TextView mSubtitleTrackView;

    public TiasSessionImpl(Context context, String iAppServiceId, int type) {
        super(context);
        if (DEBUG) {
            Log.d(TAG, "Constructing service with iAppServiceId=" + iAppServiceId
                    + " type=" + type);
        }
        mContext = context;
        mAppServiceId = iAppServiceId;
        mType = type;
        mHandler = new Handler(context.getMainLooper());

        mViewContainer = new LinearLayout(context);
        mViewContainer.setBackground(new ColorDrawable(0));
    }

    @Override
    public void onRelease() {
        if (DEBUG) {
            Log.d(TAG, "onRelease");
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        if (DEBUG) {
            Log.d(TAG, "onSetSurface");
        }
        if (mSurface != null) {
            mSurface.release();
        }
        mSurface = surface;
        return true;
    }

    @Override
    public void onSurfaceChanged(int format, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged format=" + format + " width=" + width +
                    " height=" + height);
        }
        if (mSurface != null) {
            updateSurface(mSurface, width, height);
        }
    }

    @Override
    public void onStartInteractiveApp() {
        if (DEBUG) {
            Log.d(TAG, "onStartInteractiveApp");
        }
        mHandler.post(
                () -> {
                    initSampleView();
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

    private void updateSurface(Surface surface, int width, int height) {
        mHandler.post(
                () -> {
                    // Update our virtualDisplay if it already exists, create a new one otherwise
                    if (mVirtualDisplay != null) {
                        mVirtualDisplay.setSurface(surface);
                        mVirtualDisplay.resize(width, height, DisplayMetrics.DENSITY_DEFAULT);
                    } else {
                        DisplayManager displayManager =
                                mContext.getSystemService(DisplayManager.class);
                        if (displayManager == null) {
                            Log.e(TAG, "Failed to get DisplayManager");
                            return;
                        }
                        mVirtualDisplay = displayManager.createVirtualDisplay(VIRTUAL_DISPLAY_NAME,
                                        width,
                                        height,
                                        DisplayMetrics.DENSITY_DEFAULT,
                                        surface,
                                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);

                        Presentation presentation =
                                new Presentation(mContext, mVirtualDisplay.getDisplay());
                        presentation.setContentView(mViewContainer);
                        presentation.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                        presentation.show();
                    }
                });
    }

    private void initSampleView() {
        View sampleView = LayoutInflater.from(mContext).inflate(R.layout.sample_layout, null);
        TextView appServiceIdText = sampleView.findViewById(R.id.app_service_id);
        appServiceIdText.setText("App Service ID: " + mAppServiceId);

        mTvInputIdView = sampleView.findViewById(R.id.tv_input_id);
        mChannelUriView = sampleView.findViewById(R.id.channel_uri);
        mVideoTrackView = sampleView.findViewById(R.id.video_track_selected);
        mAudioTrackView = sampleView.findViewById(R.id.audio_track_selected);
        mSubtitleTrackView = sampleView.findViewById(R.id.subtitle_track_selected);
        // Set default values for the selected tracks, since we cannot request data on them directly
        // TODO: Implement onTrackSelected() to fill these values
        mVideoTrackView.setText("No video track selected");
        mAudioTrackView.setText("No audio track selected");
        mSubtitleTrackView.setText("No subtitle track selected");

        mViewContainer.addView(sampleView);
    }

    @Override
    public void onCurrentChannelUri(Uri channelUri) {
        if (DEBUG) {
            Log.d(TAG, "onCurrentChannelUri uri=" + channelUri);
        }
        mChannelUriView.setText("Channel URI: " + channelUri);
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
        mTvInputIdView.setText("TV Input ID: " + inputId);
    }
}
