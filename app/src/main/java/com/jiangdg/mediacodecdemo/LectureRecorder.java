package com.jiangdg.mediacodecdemo;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.jiangdg.mediacodec4mp4.RecordMp4;
import com.jiangdg.mediacodec4mp4.bean.EncoderParams;

import java.io.File;

public class LectureRecorder {
    private static final String TAG = "LectureRecorder";
    private String tenantId;
    private String lectureId;
    private String personId;
    private String s3AccessKey;
    private String s3SecretKey;
    private String s3Endpoint;
    private Handler handler;
    private EncoderParams encoderParams;
    private AmazonS3Client s3;
    private boolean isRecoding;
    private String localVideoPath;
    private String s3VideoPath;
    private RecordMp4 mRecMp4;
    //    private static final long TIMER_INTERVAL_MS = 60 * 1000L;
    private static final long TIMER_INTERVAL_MS = 10 * 1000L;

    public LectureRecorder(android.content.Context context, String tenantId, String lectureId, String personId, EncoderParams encoderParams, String s3AccessKey, String s3SecretKey, String s3Endpoint) {
        this.tenantId = tenantId;
        this.lectureId = lectureId;
        this.personId = personId;
        this.s3AccessKey = s3AccessKey;
        this.s3SecretKey = s3SecretKey;
        this.s3Endpoint = s3Endpoint;
        this.encoderParams = encoderParams;
        this.isRecoding = false;
        this.mRecMp4 = RecordMp4.getRecordMp4Instance();
        this.mRecMp4.init(context);
        this.mRecMp4.setOverlayType(RecordMp4.OverlayType.TIME);

        this.s3 = new AmazonS3Client((new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return s3AccessKey;
            }

            @Override
            public String getAWSSecretKey() {
                return s3SecretKey;
            }
        }), Region.getRegion(Regions.CN_NORTH_1), new ClientConfiguration());
        this.s3.setEndpoint(s3Endpoint);
    }

    public RecordMp4 getRecordMp4() {
        return this.mRecMp4;
    }

    private void startMp4() {
        String filename = System.currentTimeMillis() + ".mp4";
        String relativeDir = this.tenantId + File.separator + this.lectureId + File.separator + this.personId;
        String localDir = RecordMp4.ROOT_PATH + File.separator + relativeDir;
        boolean mkdirOk = new File(localDir).mkdirs();
//        Log.i(TAG, "mkdir " + localDir + "=" + mkdirOk);
        this.s3VideoPath = relativeDir + File.separator + filename;
        this.localVideoPath = localDir + File.separator + filename;

        this.encoderParams.setVideoPath(this.localVideoPath);
        this.mRecMp4.setEncodeParams(this.encoderParams);
        this.mRecMp4.startRecord();
    }

    private void stopMp4() {
        this.mRecMp4.stopRecord();
        final String localVideoPath = this.localVideoPath;
        Thread t = new Thread() {
            @Override
            public void run() {
                if (localVideoPath == null) {
                    return;
                }
                File file = new File(localVideoPath);
                s3.putObject("content", s3VideoPath, file);
            }
        };
        t.start();
    }

    public void start() {
        if (this.isRecoding) {
            Log.w(TAG, "call LectureRecorder.start while current status is recording");
            return;
        }
        this.isRecoding = true;
        Log.i(TAG, "LectureRecorder is starting");
        if (this.handler == null) {
            this.handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (!isRecoding) {
                        return;
                    }
                    stopMp4();
                    startMp4();
                    if (handler != null) {
                        handler.sendEmptyMessageDelayed(0, TIMER_INTERVAL_MS);
                    }
                }
            };
        }
        handler.sendEmptyMessageDelayed(0, TIMER_INTERVAL_MS);
        startMp4();
    }

    public void stop() {
        if (this.isRecoding) {
            stopMp4();
        }
        this.isRecoding = false;
    }
}
