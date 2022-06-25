package com.android.tv.samples.sampletunertvinput;

import android.app.Activity;
import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputService;
import android.media.tv.tuner.Tuner;
import android.media.tv.tuner.dvr.DvrPlayback;
import android.media.tv.tuner.filter.Filter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.android.tv.testing.data.ChannelInfo;
import com.android.tv.testing.data.ChannelUtils;
import com.android.tv.testing.data.ProgramInfo;
import java.util.Collections;

/** Setup activity for SampleTunerTvInput */
public class SampleTunerTvInputSetupActivity extends Activity {
    private static final String ES_FILE_NAME = "test.es";

    private Tuner mTuner;
    private DvrPlayback mDvr;
    private Filter mSectionFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ChannelInfo channel =
            new ChannelInfo.Builder()
                .setNumber("1-1")
                .setName("Sample Channel")
                .setLogoUrl(
                    ChannelInfo.getUriStringForChannelLogo(this, 100))
                .setOriginalNetworkId(1)
                .setVideoWidth(640)
                .setVideoHeight(480)
                .setAudioChannel(2)
                .setAudioLanguageCount(1)
                .setHasClosedCaption(false)
                .setProgram(
                    new ProgramInfo(
                        "Sample Program",
                        "",
                        0,
                        0,
                        ProgramInfo.GEN_POSTER,
                        "Sample description",
                        ProgramInfo.GEN_DURATION,
                        null,
                        ProgramInfo.GEN_GENRE,
                        null))
                .build();

        Intent intent = getIntent();
        String inputId = intent.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        ChannelUtils.updateChannels(this, inputId, Collections.singletonList(channel));

        initTuner();

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onDestroy() {
        if (mTuner != null) {
            mTuner.close();
            mTuner = null;
        }
        if (mDvr != null) {
            mDvr.close();
            mDvr = null;
        }
        if (mSectionFilter != null) {
            mSectionFilter.close();
            mSectionFilter = null;
        }
    }

    private void initTuner() {
        mTuner = new Tuner(getApplicationContext(), null,
                TvInputService.PRIORITY_HINT_USE_CASE_TYPE_LIVE);
        Handler handler = new Handler(Looper.myLooper());

        mSectionFilter = SampleTunerTvInputUtils.createSectionFilter(mTuner, handler,
                SampleTunerTvInputUtils.createDefaultLoggingFilterCallback("section"));
        mSectionFilter.start();

        mDvr = SampleTunerTvInputUtils.createDvrPlayback(mTuner, handler,
                getApplicationContext(), ES_FILE_NAME);
        SampleTunerTvInputUtils.tune(mTuner, handler, mDvr);
        mDvr.start();
    }

}
