package com.jiangdg.mediacodecdemo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.jiangdg.mediacodec4mp4.RecordMp4;
import com.jiangdg.mediacodec4mp4.bean.YUVBean;
import com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer;
import com.jiangdg.mediacodec4mp4.utils.CameraManager;
import com.jiangdg.mediacodec4mp4.utils.SensorAccelerometer;
import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;
import com.jiangdg.mediacodec4mp4.bean.EncoderParams;
import com.jiangdg.yuvosd.YuvUtils;

import java.io.File;

public class MainActivity extends Activity implements SurfaceHolder.Callback{
    private Button mBtnRecord;
    private Button mBtnSwitchCam;
    private SurfaceView mSurfaceView;
    private CameraManager mCamManager;
    private boolean isRecording;
	//加速传感器
	private static SensorAccelerometer mSensorAccelerometer;
    private RecordMp4 mRecMp4;

    byte[] nv21 = new byte[CameraManager.PREVIEW_WIDTH * CameraManager.PREVIEW_HEIGHT * 3/2];

    private CameraManager.OnPreviewFrameResult mPreviewListener = new CameraManager.OnPreviewFrameResult() {
        @Override
        public void onPreviewResult(byte[] data, Camera camera) {
            if(mRecMp4 != null){
                mRecMp4.feedYUV2Consumer(new YUVBean(data,System.currentTimeMillis()));
            }
            mCamManager.getCameraIntance().addCallbackBuffer(data);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCamManager = CameraManager.getCamManagerInstance(MainActivity.this);
		// 实例化加速传感器
		mSensorAccelerometer = SensorAccelerometer.getSensorInstance();
        // 初始化录制参数
        mRecMp4 = RecordMp4.getRecordMp4Instance();
		
        mSurfaceView = (SurfaceView)findViewById(R.id.main_record_surface);
        mSurfaceView.getHolder().addCallback(this);
        mSurfaceView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mCamManager.cameraFocus(new CameraManager.OnCameraFocusResult() {
					@Override
					public void onFocusResult(boolean result) {
						if(result){
							Toast.makeText(MainActivity.this, "对焦成功", Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		});
        
        mBtnRecord = (Button)findViewById(R.id.main_record_btn);
        mBtnRecord.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!isRecording){
                    // 开始录制
                    EncoderParams mParams = new EncoderParams();
                    mParams.setVideoPath(RecordMp4.ROOT_PATH+ File.separator + System.currentTimeMillis() + ".mp4");    // 视频文件路径
                    mParams.setFrameWidth(CameraManager.PREVIEW_WIDTH);             // 分辨率
                    mParams.setFrameHeight(CameraManager.PREVIEW_HEIGHT);
                    mParams.setBitRateQuality(H264EncodeConsumer.Quality.MIDDLE);   // 视频编码码率
                    mParams.setFrameRateDegree(H264EncodeConsumer.FrameRate._30fps);// 视频编码帧率
                    mParams.setFrontCamera(mCamManager.getCameraDirection());       // 摄像头方向
                    mParams.setPhoneHorizontal(false);  // 是否为横屏拍摄
                    mParams.setAudioBitrate(AACEncodeConsumer.DEFAULT_BIT_RATE);        // 音频比特率
                    mParams.setAudioSampleRate(AACEncodeConsumer.DEFAULT_SAMPLE_RATE);  // 音频采样率
                    mParams.setAudioChannelConfig(AACEncodeConsumer.CHANNEL_IN_MONO);// 单声道
                    mParams.setAudioChannelCount(AACEncodeConsumer.CHANNEL_COUNT_MONO);       // 单声道通道数量
                    mParams.setAudioFormat(AACEncodeConsumer.ENCODING_PCM_16BIT);       // 采样精度为16位
                    mParams.setAudioSouce(AACEncodeConsumer.SOURCE_MIC);                // 音频源为MIC
                    mRecMp4.setEncodeParams(mParams);

                    mRecMp4.startRecord();
                    mBtnRecord.setText("停止录像");
                }else{
                    mRecMp4.stopRecord();
                    mBtnRecord.setText("开始录像");
                }
                isRecording = !isRecording;
            }
        });
        
        mBtnSwitchCam = (Button)findViewById(R.id.main_switch_camera_btn);
        mBtnSwitchCam.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(isRecording){
					Toast.makeText(MainActivity.this, "正在录像，无法切换",
							Toast.LENGTH_SHORT).show();
					return;
				}
				if(mCamManager != null){
					mCamManager.switchCamera();
				}
			}
		});
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamManager.setSurfaceHolder(surfaceHolder);
        mCamManager.setOnPreviewResult(mPreviewListener);
        mCamManager.createCamera();
        mCamManager.startPreview();
        startSensorAccelerometer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamManager.stopPreivew();
        mCamManager.destoryCamera();
        stopSensorAccelerometer();
    }
    
	private void startSensorAccelerometer() {
		// 启动加速传感器，注册结果事件监听器
		if (mSensorAccelerometer != null) {
			mSensorAccelerometer.startSensorAccelerometer(MainActivity.this,
					new SensorAccelerometer.OnSensorChangedResult() {
						@Override
						public void onStopped() {					
							// 对焦成功，隐藏对焦图标
							mCamManager.cameraFocus(new CameraManager.OnCameraFocusResult() {
								@Override
								public void onFocusResult(boolean reslut) {
						
								}
							});
						}

						@Override
						public void onMoving(int x, int y, int z) {
//							Log.i(TAG, "手机移动中：x=" + x + "；y=" + y + "；z=" + z);
						}
					});
		}
	}

	private void stopSensorAccelerometer() {
		// 释放加速传感器资源
		if (mSensorAccelerometer == null) {
			return;
		}
		mSensorAccelerometer.stopSensorAccelerometer();
	}

    private void showMsg(String msg){
        Toast.makeText(MainActivity.this, msg,Toast.LENGTH_SHORT).show();
    }
}
