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

package com.android.tv.samples.sampletunertvinput;

import android.content.Context;
import android.media.tv.tuner.Tuner;
import android.media.tv.tuner.dvr.DvrPlayback;
import android.media.tv.tuner.dvr.DvrSettings;
import android.media.tv.tuner.frontend.DvbtFrontendSettings;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

public class SampleTunerTvInputUtils {
    private static final String TAG = "SampleTunerTvInput";
    private static final boolean DEBUG = true;

    private static final int STATUS_MASK = 0xf;
    private static final int LOW_THRESHOLD = 0x1000;
    private static final int HIGH_THRESHOLD = 0x07fff;
    private static final int DVR_BUFFER_SIZE = 4000000;
    private static final int PACKET_SIZE = 188;
    private static final long FREQUENCY = 578000;
    private static final int INPUT_FILE_MAX_SIZE = 700000;

    public static DvrPlayback createDvrPlayback(Tuner tuner, Handler handler,
            Context context, String fileName) {
        DvrPlayback dvr = tuner.openDvrPlayback(DVR_BUFFER_SIZE, new HandlerExecutor(handler),
                status -> {
                    if (DEBUG) {
                        Log.d(TAG, "onPlaybackStatusChanged status=" + status);
                    }
                });
        int res = dvr.configure(
                DvrSettings.builder()
                        .setStatusMask(STATUS_MASK)
                        .setLowThreshold(LOW_THRESHOLD)
                        .setHighThreshold(HIGH_THRESHOLD)
                        .setDataFormat(DvrSettings.DATA_FORMAT_ES)
                        .setPacketSize(PACKET_SIZE)
                        .build());
        if (DEBUG) {
            Log.d(TAG, "config res=" + res);
        }
        String testFile = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(testFile);
        if (file.exists()) {
            try {
                dvr.setFileDescriptor(
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed to create FD");
            }
        } else {
            Log.w(TAG, "File not existing");
        }
        return dvr;
    }

    public static void tune(Tuner tuner, Handler handler, DvrPlayback dvr) {
        DvbtFrontendSettings feSettings = DvbtFrontendSettings.builder()
                .setFrequencyLong(FREQUENCY)
                .setTransmissionMode(DvbtFrontendSettings.TRANSMISSION_MODE_AUTO)
                .setBandwidth(DvbtFrontendSettings.BANDWIDTH_8MHZ)
                .setConstellation(DvbtFrontendSettings.CONSTELLATION_AUTO)
                .setHierarchy(DvbtFrontendSettings.HIERARCHY_AUTO)
                .setHighPriorityCodeRate(DvbtFrontendSettings.CODERATE_AUTO)
                .setLowPriorityCodeRate(DvbtFrontendSettings.CODERATE_AUTO)
                .setGuardInterval(DvbtFrontendSettings.GUARD_INTERVAL_AUTO)
                .setHighPriority(true)
                .setStandard(DvbtFrontendSettings.STANDARD_T)
                .build();

        tuner.setOnTuneEventListener(new HandlerExecutor(handler), tuneEvent -> {
            if (DEBUG) {
                Log.d(TAG, "onTuneEvent " + tuneEvent);
            }
            long read = dvr.read(INPUT_FILE_MAX_SIZE);
            if (DEBUG) {
                Log.d(TAG, "read=" + read);
            }
        });

        tuner.tune(feSettings);
    }
}
