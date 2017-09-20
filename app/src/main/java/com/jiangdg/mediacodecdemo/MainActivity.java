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
import com.jiangdg.mediacodec4mp4.CameraManager;
import com.jiangdg.mediacodec4mp4.SensorAccelerometer;
import com.jiangdg.mediacodec4mp4.runnable.EncoderParams;
import com.jiangdg.mediacodec4mp4.runnable.EncoderVideoRunnable;
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

    byte[] nv21 = new byte[CameraManager.PREVIEW_WIDTH * CameraManager.PREVIEW_HEIGHT * 3/2];

    private CameraManager.OnPreviewFrameResult mPreviewListener = new CameraManager.OnPreviewFrameResult() {
        @Override
        public void onPreviewResult(byte[] data, Camera camera) {
            mCamManager.getCameraIntance().addCallbackBuffer(data);
            if(CameraManager.isUsingYv12 ){
                YuvUtils.swapYV12ToNV21(data, nv21, CameraManager.PREVIEW_WIDTH, CameraManager.PREVIEW_HEIGHT);
                RecordMp4.getMuxerRunnableInstance().addVideoFrameData(nv21);
            }else{
                RecordMp4.getMuxerRunnableInstance().addVideoFrameData(data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCamManager = CameraManager.getCamManagerInstance(MainActivity.this);
		//实例化加速传感器
		mSensorAccelerometer = SensorAccelerometer.getSensorInstance();
		
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
                RecordMp4 mMuxerUtils = RecordMp4.getMuxerRunnableInstance();
                if(!isRecording){
                    // 配置参数
                    EncoderParams params = new EncoderParams();
                    params.setPath(RecordMp4.ROOT_PATH+ File.separator + System.currentTimeMillis() + ".mp4");
                    params.setFrameWidth(CameraManager.PREVIEW_WIDTH);
                    params.setFrameHeight(CameraManager.PREVIEW_HEIGHT);
                    params.setBitRateQuality(EncoderVideoRunnable.Quality.LOW);
                    params.setFrameRateDegree(EncoderVideoRunnable.FrameRate._30fps);
                    params.setFrontCamera(mCamManager.getCameraDirection());
                    params.setPhoneHorizontal(false);
                    // 开始录制
                    mMuxerUtils.startMuxerThread(params, new RecordMp4.OnRecordResultListener() {
                        @Override
                        public void onSuccuss(String path) {
                            showMsg("保存路径："+path);
                        }

                        @Override
                        public void onFailed(String tipMsg) {
                            showMsg(tipMsg);
                        }
                    });
                    mBtnRecord.setText("停止录像");
                }else{
                    mMuxerUtils.stopMuxerThread();
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
