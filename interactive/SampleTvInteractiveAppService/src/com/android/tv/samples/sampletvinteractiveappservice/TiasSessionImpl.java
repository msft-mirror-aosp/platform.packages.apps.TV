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
import android.media.tv.interactive.TvInteractiveAppService;
import android.util.Log;
import android.view.Surface;

public class TiasSessionImpl extends TvInteractiveAppService.Session {
    private static final String TAG = "SampleTvInteractiveAppService";
    private static final boolean DEBUG = true;

    public TiasSessionImpl(Context context) {
        super(context);
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
}
