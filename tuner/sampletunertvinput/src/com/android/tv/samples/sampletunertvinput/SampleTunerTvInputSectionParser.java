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

import android.util.Log;

import java.util.Locale;

/** Parser for ATSC PSIP sections */
public class SampleTunerTvInputSectionParser {
    private static final String TAG = "SampleTunerTvInput";

    /**
     * Parses a single TVCT section, as defined in A/65 6.4
     * @param data, a ByteBuffer containing a single TVCT section which describes only one channel
     * @return null if there is an error while parsing, the channel with parsed data otherwise
     */
    public static TvctChannelInfo parseTvctSection(byte[] data) {
        if (!checkValidPsipSection(data)) {
            return null;
        }
        // TODO: Parse the data for channel information
        return new TvctChannelInfo("Sample Channel", 1, 1);
    }

    private static boolean checkValidPsipSection(byte[] data) {
        if (data.length < 13) {
            Log.e(TAG, "Section was too small");
            return false;
        }
        if ((data[0] & 0xff) == 0xff) {
            // Should clear stuffing bytes as detailed by H222.0 section 2.4.4.
            Log.e(TAG, "Unexpected stuffing bytes while parsing section");
            return false;
        }
        int sectionLength = (((data[1] & 0x0f) << 8) | (data[2] & 0xff)) + 3;
        if (sectionLength != data.length) {
            Log.e(TAG, "Length mismatch while parsing section");
            return false;
        }
        int sectionNumber = data[6] & 0xff;
        int lastSectionNumber = data[7] & 0xff;
        if(sectionNumber > lastSectionNumber) {
            Log.e(TAG, "Found sectionNumber > lastSectionNumber while parsing section");
            return false;
        }
        // TODO: Check CRC 32/MPEG for validity
        return true;
    }

    // Contains the portion of the data contained in the TVCT used by
    // our SampleTunerTvInputSetupActivity
    public static class TvctChannelInfo {
        private final String mChannelName;
        private final int mMajorChannelNumber;
        private final int mMinorChannelNumber;

        public TvctChannelInfo(
                String channelName,
                int majorChannelNumber,
                int minorChannelNumber) {
            mChannelName = channelName;
            mMajorChannelNumber = majorChannelNumber;
            mMinorChannelNumber = minorChannelNumber;
        }

        public String getChannelName() {
            return mChannelName;
        }

        public int getMajorChannelNumber() {
            return mMajorChannelNumber;
        }

        public int getMinorChannelNumber() {
            return mMinorChannelNumber;
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.US,
                    "ChannelName: %s ChannelNumber: %d-%d",
                    mChannelName,
                    mMajorChannelNumber,
                    mMinorChannelNumber);
        }
    }
}
