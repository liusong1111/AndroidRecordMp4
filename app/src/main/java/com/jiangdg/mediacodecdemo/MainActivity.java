package com.jiangdg.mediacodecdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.jiangdg.mediacodec4mp4.RecordMp4;
import com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.SaveYuvImageTask;
import com.jiangdg.mediacodec4mp4.utils.CameraManager;
import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;
import com.jiangdg.mediacodec4mp4.bean.EncoderParams;

import java.io.File;


public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";
    public Button mBtnRecord;
    public Button mBtnSwitchCam;
    public SurfaceView mSurfaceView;
    public Button mBtnCaputrePic;

    private LectureRecorder recorder;
    private boolean isRecording;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnRecord = (Button) findViewById(R.id.main_record_btn);
        mBtnSwitchCam = (Button) findViewById(R.id.main_switch_camera_btn);
        mSurfaceView = (SurfaceView) findViewById(R.id.main_record_surface);
        mSurfaceView.getHolder().addCallback(this);
        mBtnCaputrePic = (Button) findViewById(R.id.main_capture_picture);

        mBtnRecord.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    // 开始录制
                    recorder.start();
                    mBtnRecord.setText("停止录像");
                } else {
                    // 停止录制
                    recorder.stop();
                    mBtnRecord.setText("开始录像");
                }
                isRecording = !isRecording;
            }
        });

        mBtnSwitchCam.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    showMsg("正在录像，无法切换");
                } else {
                    if (recorder.getRecordMp4() != null) {
                        recorder.getRecordMp4().switchCamera();
                    }
                }
            }
        });

//        mSurfaceView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (recorder.getRecordMp4() != null) {
//                    recorder.getRecordMp4().enableFocus(new CameraManager.OnCameraFocusResult() {
//                        @Override
//                        public void onFocusResult(boolean result) {
//                            if (result) {
//                                showMsg("对焦成功");
//                            }
//                        }
//                    });
//                    recorder.getRecordMp4().startRecord();
//                }
//            }
//        });

        mBtnCaputrePic.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String picPath = RecordMp4.ROOT_PATH + File.separator + System.currentTimeMillis() + ".jpg";
                if (recorder.getRecordMp4() != null)
                    recorder.getRecordMp4().capturePicture(picPath, new SaveYuvImageTask.OnSaveYuvResultListener() {
                        @Override
                        public void onSaveResult(boolean result, String savePath) {
                            Log.i("MainActivity", "抓拍结果：" + result + "保存路径：" + savePath);
                        }
                    });
            }
        });

        EncoderParams encoderParams = this.getEncodeParams();
        this.recorder = new LectureRecorder(this, "000000", "lecture1", "person1", encoderParams, "minioadmin", "minioadmin", "http://172.16.1.109:9000");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private EncoderParams getEncodeParams() {
        EncoderParams mParams = new EncoderParams();
//        mParams.setVideoPath(RecordMp4.ROOT_PATH + File.separator + System.currentTimeMillis() + ".mp4");    // 视频文件路径
        mParams.setFrameWidth(CameraManager.PREVIEW_WIDTH);             // 分辨率
        mParams.setFrameHeight(CameraManager.PREVIEW_HEIGHT);
        mParams.setBitRateQuality(H264EncodeConsumer.Quality.MIDDLE);   // 视频编码码率
        mParams.setFrameRateDegree(H264EncodeConsumer.FrameRate._30fps);// 视频编码帧率
//        mParams.setFrontCamera((mRecMp4!=null && mRecMp4.isFrontCamera()) ? true:false);       // 摄像头方向
//        mParams.setPhoneHorizontal(false);  // 是否为横屏拍摄
        mParams.setAudioBitrate(AACEncodeConsumer.DEFAULT_BIT_RATE);        // 音频比特率
        mParams.setAudioSampleRate(AACEncodeConsumer.DEFAULT_SAMPLE_RATE);  // 音频采样率
        mParams.setAudioChannelConfig(AACEncodeConsumer.CHANNEL_IN_MONO);// 单声道
        mParams.setAudioChannelCount(AACEncodeConsumer.CHANNEL_COUNT_MONO);       // 单声道通道数量
        mParams.setAudioFormat(AACEncodeConsumer.ENCODING_PCM_16BIT);       // 采样精度为16位
        mParams.setAudioSouce(AACEncodeConsumer.SOURCE_MIC);                // 音频源为MIC
        return mParams;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
//        Log.i(TAG, "create mp4=" + recorder.getRecordMp4());
        if (recorder.getRecordMp4() != null) {
            // 修改默认分辨率
            recorder.getRecordMp4().startCamera(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//        Log.i(TAG, "destroyed mp4=" + recorder.getRecordMp4());
        if (recorder.getRecordMp4() != null) {
            recorder.getRecordMp4().stopCamera();
        }
    }


    private void showMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
