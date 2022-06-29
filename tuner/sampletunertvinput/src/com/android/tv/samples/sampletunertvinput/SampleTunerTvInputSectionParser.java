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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/** Parser for ATSC PSIP sections */
public class SampleTunerTvInputSectionParser {
    private static final String TAG = "SampleTunerTvInput";
    private static final boolean DEBUG = true;

    /**
     * Parses a single TVCT section, as defined in A/65 6.4
     * @param data, a ByteBuffer containing a single TVCT section which describes only one channel
     * @return null if there is an error while parsing, the channel with parsed data otherwise
     */
    public static TvctChannelInfo parseTvctSection(byte[] data) {
        if (!checkValidPsipSection(data)) {
            return null;
        }
        int numChannels = data[9] & 0xff;
        if(numChannels != 1) {
            Log.e(TAG, "parseTVCTSection expected 1 channel, found " + numChannels);
            return null;
        }
        // TVCT Sections are a minimum of 16 bytes, with a minimum of 32 bytes per channel
        if(data.length < 48) {
            Log.e(TAG, "parseTVCTSection found section under minimum length");
            return null;
        }

        // shortName begins at data[10] and ends at either the first stuffing
        // UTF-16 character of value 0x0000, or at a length of 14 Bytes
        int shortNameLength = 14;
        for(int i = 0; i < 14; i += 2) {
            int charValue = ((data[10 + i] & 0xff) << 8) | (data[10 + (i + 1)] & 0xff);
            if (charValue == 0x0000) {
                shortNameLength = i;
                break;
            }
        }
        // Data field positions are as defined by A/65 Section 6.4 for one channel
        String shortName = new String(Arrays.copyOfRange(data, 10, 10 + shortNameLength),
                        StandardCharsets.UTF_16);
        int majorNumber = ((data[24] & 0x0f) << 6) | ((data[25] & 0xff) >> 2);
        int minorNumber = ((data[25] & 0x03) << 8) | (data[26] & 0xff);
        if (DEBUG) {
            Log.d(TAG, "parseTVCTSection found shortName: " + shortName
                    + " channel number: " + majorNumber + "-" + minorNumber);
        }

        return new TvctChannelInfo(shortName, majorNumber, minorNumber);
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
