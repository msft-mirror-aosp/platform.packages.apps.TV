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

    /**
     * Parses a single TVCT section, as defined in A/65 6.4
     * @param data, a ByteBuffer containing a single TVCT section which describes only one channel
     * @return null if there is an error while parsing, the channel with parsed data otherwise
     */
    public static TvctChannelInfo parseTvctSection(byte[] data) {
        // TODO: Parse the data for channel information
        return new TvctChannelInfo("Sample Channel", 1, 1);
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
