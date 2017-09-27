package com.jiangdg.mediacodecdemo;

import android.app.Activity;
import android.hardware.Camera;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity implements SurfaceHolder.Callback{
    @BindView(R.id.main_record_btn)
    public Button mBtnRecord;
    @BindView(R.id.main_switch_camera_btn)
    public Button mBtnSwitchCam;
    @BindView(R.id.main_record_surface)
    public SurfaceView mSurfaceView;
    @BindView(R.id.main_capture_picture)
    public Button mBtnCaputrePic;

    private boolean isRecording;
    private RecordMp4 mRecMp4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 绑定View
        ButterKnife.bind(this);
        mSurfaceView.getHolder().addCallback(this);

        // 1. 初始化引擎
        mRecMp4 = RecordMp4.getRecordMp4Instance();
        mRecMp4.init(this);

        mRecMp4.setOverlayType(RecordMp4.OverlayType.BOTH);
        mRecMp4.setOverlayContent("我爱你，中国！");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 5. 释放引擎所占用的资源
        if(mRecMp4 != null){
            mRecMp4.release();
        }
    }

    @OnClick({R.id.main_record_btn,R.id.main_switch_camera_btn,R.id.main_record_surface,R.id.main_capture_picture})
    public void onViewClick(View v){
        int vId = v.getId();
        switch (vId){
            // 录制
            case R.id.main_record_btn:
                if(!isRecording){
                    // 2. 配置参数
                    mRecMp4.setEncodeParams(getEncodeParams());
                    // 3. 开始录制
                    mRecMp4.startRecord();
                    mBtnRecord.setText("停止录像");
                }else{
                    // 4. 停止录制
                    mRecMp4.stopRecord();
                    mBtnRecord.setText("开始录像");
                }
                isRecording = !isRecording;
                break;
            // 切换摄像头
            case R.id.main_switch_camera_btn:
                if(isRecording){
                    showMsg("正在录像，无法切换");
                }else {
                    if(mRecMp4 != null){
                        mRecMp4.switchCamera();
                    }
                }
                break;
            // 对焦
            case R.id.main_record_surface:
                if(mRecMp4 != null){
                    mRecMp4.enableFocus(new CameraManager.OnCameraFocusResult() {
                        @Override
                        public void onFocusResult(boolean result) {
                            if(result){
                                showMsg("对焦成功");
                            }
                        }
                    });
                }
                break;
            // 抓拍
            case R.id.main_capture_picture:
                String picPath = RecordMp4.ROOT_PATH+File.separator+System.currentTimeMillis()+".jpg";
                if(mRecMp4 != null)
                    mRecMp4.capturePicture(picPath, new SaveYuvImageTask.OnSaveYuvResultListener() {
                        @Override
                        public void onSaveResult(boolean result, String savePath) {
                            Log.i("MainActivity","抓拍结果："+result+"保存路径："+savePath);
                        }
                    });
                break;
        }
    }



    private EncoderParams getEncodeParams() {
        EncoderParams mParams = new EncoderParams();
        mParams.setVideoPath(RecordMp4.ROOT_PATH+ File.separator + System.currentTimeMillis() + ".mp4");    // 视频文件路径
        mParams.setFrameWidth(CameraManager.PREVIEW_WIDTH);             // 分辨率
        mParams.setFrameHeight(CameraManager.PREVIEW_HEIGHT);
        mParams.setBitRateQuality(H264EncodeConsumer.Quality.MIDDLE);   // 视频编码码率
        mParams.setFrameRateDegree(H264EncodeConsumer.FrameRate._30fps);// 视频编码帧率
        mParams.setFrontCamera((mRecMp4!=null&&mRecMp4.isFrontCamera()) ? true:false);       // 摄像头方向
        mParams.setPhoneHorizontal(false);  // 是否为横屏拍摄
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
        if(mRecMp4 != null){
            mRecMp4.startCamera(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if(mRecMp4 != null){
            mRecMp4.stopCamera();
        }
    }
    


    private void showMsg(String msg){
        Toast.makeText(MainActivity.this, msg,Toast.LENGTH_SHORT).show();
    }
}
