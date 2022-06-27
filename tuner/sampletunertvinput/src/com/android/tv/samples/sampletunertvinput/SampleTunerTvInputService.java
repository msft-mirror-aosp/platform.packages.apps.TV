package com.android.tv.samples.sampletunertvinput;

import static android.media.tv.TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.LinearBlock;
import android.media.MediaFormat;
import android.media.tv.tuner.dvr.DvrPlayback;
import android.media.tv.tuner.dvr.DvrSettings;
import android.media.tv.tuner.filter.Filter;
import android.media.tv.tuner.filter.FilterCallback;
import android.media.tv.tuner.filter.FilterEvent;
import android.media.tv.tuner.filter.MediaEvent;
import android.media.tv.tuner.Tuner;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


/** SampleTunerTvInputService */
public class SampleTunerTvInputService extends TvInputService {
    private static final String TAG = "SampleTunerTvInput";
    private static final boolean DEBUG = true;

    private static final int TIMEOUT_US = 100000;
    private static final boolean SAVE_DATA = false;
    private static final String ES_FILE_NAME = "test.es";
    private static final MediaFormat VIDEO_FORMAT;

    static {
        // format extracted for the specific input file
        VIDEO_FORMAT = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 320, 240);
        VIDEO_FORMAT.setInteger(MediaFormat.KEY_TRACK_ID, 1);
        VIDEO_FORMAT.setLong(MediaFormat.KEY_DURATION, 9933333);
        VIDEO_FORMAT.setInteger(MediaFormat.KEY_LEVEL, 32);
        VIDEO_FORMAT.setInteger(MediaFormat.KEY_PROFILE, 65536);
        ByteBuffer csd = ByteBuffer.wrap(
                new byte[] {0, 0, 0, 1, 103, 66, -64, 20, -38, 5, 7, -24, 64, 0, 0, 3, 0, 64, 0,
                        0, 15, 35, -59, 10, -88});
        VIDEO_FORMAT.setByteBuffer("csd-0", csd);
        csd = ByteBuffer.wrap(new byte[] {0, 0, 0, 1, 104, -50, 60, -128});
        VIDEO_FORMAT.setByteBuffer("csd-1", csd);
    }

    public static final String INPUT_ID =
            "com.android.tv.samples.sampletunertvinput/.SampleTunerTvInputService";
    private String mSessionId;

    @Override
    public TvInputSessionImpl onCreateSession(String inputId, String sessionId) {
        TvInputSessionImpl session =  new TvInputSessionImpl(this);
        if (DEBUG) {
            Log.d(TAG, "onCreateSession(inputId=" + inputId + ", sessionId=" + sessionId + ")");
        }
        mSessionId = sessionId;
        return session;
    }

    @Override
    public TvInputSessionImpl onCreateSession(String inputId) {
        if (DEBUG) {
            Log.d(TAG, "onCreateSession(inputId=" + inputId + ")");
        }
        return new TvInputSessionImpl(this);
    }

    class TvInputSessionImpl extends Session {

        private final Context mContext;
        private Handler mHandler;

        private Surface mSurface;
        private Filter mAudioFilter;
        private Filter mVideoFilter;
        private Filter mSectionFilter;
        private DvrPlayback mDvr;
        private Tuner mTuner;
        private MediaCodec mMediaCodec;
        private Thread mDecoderThread;
        private Deque<MediaEvent> mDataQueue;
        private List<MediaEvent> mSavedData;
        private boolean mDataReady = false;


        public TvInputSessionImpl(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public void onRelease() {
            if (DEBUG) {
                Log.d(TAG, "onRelease");
            }
            if (mDecoderThread != null) {
                mDecoderThread.interrupt();
                mDecoderThread = null;
            }
            if (mMediaCodec != null) {
                mMediaCodec.release();
                mMediaCodec = null;
            }
            if (mAudioFilter != null) {
                mAudioFilter.close();
            }
            if (mVideoFilter != null) {
                mVideoFilter.close();
            }
            if (mSectionFilter != null) {
                mSectionFilter.close();
            }
            if (mDvr != null) {
                mDvr.close();
                mDvr = null;
            }
            if (mTuner != null) {
                mTuner.close();
                mTuner = null;
            }
            mDataQueue = null;
            mSavedData = null;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            if (DEBUG) {
                Log.d(TAG, "onSetSurface");
            }
            this.mSurface = surface;
            return true;
        }

        @Override
        public void onSetStreamVolume(float v) {
            if (DEBUG) {
                Log.d(TAG, "onSetStreamVolume " + v);
            }
        }

        @Override
        public boolean onTune(Uri uri) {
            if (DEBUG) {
                Log.d(TAG, "onTune " + uri);
            }
            if (!initCodec()) {
                Log.e(TAG, "null codec!");
                return false;
            }
            mHandler = new Handler();
            mDecoderThread =
                    new Thread(
                            this::decodeInternal,
                            "sample-tuner-tis-decoder-thread");
            mDecoderThread.start();
            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean b) {
            if (DEBUG) {
                Log.d(TAG, "onSetCaptionEnabled " + b);
            }
        }

        private FilterCallback videoFilterCallback() {
            return new FilterCallback() {
                @Override
                public void onFilterEvent(Filter filter, FilterEvent[] events) {
                    if (DEBUG) {
                        Log.d(TAG, "onFilterEvent video, size=" + events.length);
                    }
                    for (int i = 0; i < events.length; i++) {
                        if (DEBUG) {
                            Log.d(TAG, "events[" + i + "] is "
                                    + events[i].getClass().getSimpleName());
                        }
                        if (events[i] instanceof MediaEvent) {
                            MediaEvent me = (MediaEvent) events[i];
                            mDataQueue.add(me);
                            if (SAVE_DATA) {
                                mSavedData.add(me);
                            }
                        }
                    }
                }

                @Override
                public void onFilterStatusChanged(Filter filter, int status) {
                    if (DEBUG) {
                        Log.d(TAG, "onFilterEvent video, status=" + status);
                    }
                    if (status == Filter.STATUS_DATA_READY) {
                        mDataReady = true;
                    }
                }
            };
        }

        private boolean initCodec() {
            if (mMediaCodec != null) {
                mMediaCodec.release();
                mMediaCodec = null;
            }
            try {
                mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                mMediaCodec.configure(VIDEO_FORMAT, mSurface, null, 0);
            } catch (IOException e) {
                Log.e(TAG, "Error in initCodec: " + e.getMessage());
            }

            if (mMediaCodec == null) {
                Log.e(TAG, "null codec!");
                notifyVideoUnavailable(VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            }
            return true;
        }

        private void decodeInternal() {
            mDataQueue = new ArrayDeque<>();
            mSavedData = new ArrayList<>();
            mTuner = new Tuner(mContext, mSessionId,
                    TvInputService.PRIORITY_HINT_USE_CASE_TYPE_LIVE);

            mAudioFilter = SampleTunerTvInputUtils.createAvFilter(mTuner, mHandler,
                    SampleTunerTvInputUtils.createDefaultLoggingFilterCallback("audio"), true);
            mVideoFilter = SampleTunerTvInputUtils.createAvFilter(mTuner, mHandler,
                    videoFilterCallback(), false);
            mSectionFilter = SampleTunerTvInputUtils.createSectionFilter(mTuner, mHandler,
                    SampleTunerTvInputUtils.createDefaultLoggingFilterCallback("section"));
            mAudioFilter.start();
            mVideoFilter.start();
            mSectionFilter.start();
            // use dvr playback to feed the data on platform without physical tuner
            mDvr = SampleTunerTvInputUtils.createDvrPlayback(mTuner, mHandler,
                    mContext, ES_FILE_NAME, DvrSettings.DATA_FORMAT_ES);
            SampleTunerTvInputUtils.tune(mTuner, mHandler, mDvr);
            mDvr.start();
            mMediaCodec.start();

            try {
                while (!Thread.interrupted()) {
                    if (!mDataReady) {
                        Thread.sleep(100);
                        continue;
                    }
                    if (!mDataQueue.isEmpty()) {
                        if (handleDataBuffer(mDataQueue.getFirst())) {
                            // data consumed, remove.
                            mDataQueue.pollFirst();
                        }
                    }
                    if (SAVE_DATA) {
                        mDataQueue.addAll(mSavedData);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in decodeInternal: " + e.getMessage());
            }
        }

        private boolean handleDataBuffer(MediaEvent mediaEvent) {
            if (mediaEvent.getLinearBlock() == null) {
                if (DEBUG) Log.d(TAG, "getLinearBlock() == null");
                return true;
            }
            boolean success = false;
            LinearBlock block = mediaEvent.getLinearBlock();
            if (queueCodecInputBuffer(block, mediaEvent.getDataLength(), mediaEvent.getOffset(),
                                  mediaEvent.getPts())) {
                releaseCodecOutputBuffer();
                success = true;
            }
            mediaEvent.release();
            return success;
        }

        private boolean queueCodecInputBuffer(LinearBlock block, long sampleSize,
                                              long offset, long pts) {
            int res = mMediaCodec.dequeueInputBuffer(TIMEOUT_US);
            if (res >= 0) {
                ByteBuffer buffer = mMediaCodec.getInputBuffer(res);
                if (buffer == null) {
                    throw new RuntimeException("Null decoder input buffer");
                }

                ByteBuffer data = block.map();
                if (offset > 0 && offset < data.limit()) {
                    data.position((int) offset);
                } else {
                    data.position(0);
                }

                if (DEBUG) {
                    Log.d(
                        TAG,
                        "Decoder: Send data to decoder."
                            + " Sample size="
                            + sampleSize
                            + " pts="
                            + pts
                            + " limit="
                            + data.limit()
                            + " pos="
                            + data.position()
                            + " size="
                            + (data.limit() - data.position()));
                }
                // fill codec input buffer
                int size = sampleSize > data.limit() ? data.limit() : (int) sampleSize;
                if (DEBUG) Log.d(TAG, "limit " + data.limit() + " sampleSize " + sampleSize);
                if (data.hasArray()) {
                    Log.d(TAG, "hasArray");
                    buffer.put(data.array(), 0, size);
                } else {
                    byte[] array = new byte[size];
                    data.get(array, 0, size);
                    buffer.put(array, 0, size);
                }

                mMediaCodec.queueInputBuffer(res, 0, (int) sampleSize, pts, 0);
            } else {
                if (DEBUG) Log.d(TAG, "queueCodecInputBuffer res=" + res);
                return false;
            }
            return true;
        }

        private void releaseCodecOutputBuffer() {
            // play frames
            BufferInfo bufferInfo = new BufferInfo();
            int res = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            if (res >= 0) {
                mMediaCodec.releaseOutputBuffer(res, true);
                notifyVideoAvailable();
                if (DEBUG) {
                    Log.d(TAG, "notifyVideoAvailable");
                }
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = mMediaCodec.getOutputFormat();
                if (DEBUG) {
                    Log.d(TAG, "releaseCodecOutputBuffer: Output format changed:" + format);
                }
            } else if (res == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (DEBUG) {
                    Log.d(TAG, "releaseCodecOutputBuffer: timeout");
                }
            } else {
                if (DEBUG) {
                    Log.d(TAG, "Return value of releaseCodecOutputBuffer:" + res);
                }
            }
        }

    }
}
