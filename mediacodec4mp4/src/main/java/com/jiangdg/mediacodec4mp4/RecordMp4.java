package com.jiangdg.mediacodec4mp4;

import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.jiangdg.mediacodec4mp4.bean.EncoderParams;
import com.jiangdg.mediacodec4mp4.bean.YUVBean;
import com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.MediaMuxerUtil;
import com.jiangdg.mediacodec4mp4.model.SaveYuvImageTask;
import com.jiangdg.mediacodec4mp4.utils.CameraManager;
import com.jiangdg.mediacodec4mp4.utils.SensorAccelerometer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * MpuMain业务逻辑实现类
 * <p>
 * Created by jianddongguo on 2017/7/20.
 */

public class RecordMp4 {
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory()
            .getAbsolutePath();
    public static final boolean DEBUG = false;
    private static final String TAG = "RecordMp4";
    private AACEncodeConsumer mAacConsumer;
    private H264EncodeConsumer mH264Consumer;
    private MediaMuxerUtil mMuxer;
    private EncoderParams mParams;
    private SensorAccelerometer mSensorAccelerometer;
    private SaveYuvImageTask.OnSaveYuvResultListener listener;
    private String picPath;
    private static RecordMp4 mRecMp4;
    private CameraManager mCamManager;

    private RecordMp4(){}

    public static RecordMp4 getRecordMp4Instance(){
        if(mRecMp4 == null){
            mRecMp4 = new RecordMp4();
        }
        return mRecMp4;
    }

    private CameraManager.OnPreviewFrameResult mPreviewListener = new CameraManager.OnPreviewFrameResult() {
        @Override
        public void onPreviewResult(byte[] data, Camera camera) {
            // 编码原始数据
            if (mH264Consumer != null) {
                mH264Consumer.addData(data);
            }
            // 图片抓拍
            if(listener != null){
                YUVBean bean = new YUVBean();
                bean.setEnableSoftCodec(false);
                bean.setDegree(0);
                bean.setFrontCamera(isFrontCamera());
                bean.setWidth(CameraManager.PREVIEW_WIDTH);
                bean.setHeight(CameraManager.PREVIEW_HEIGHT);
                bean.setPicPath(getPicPath());
                bean.setYuvData(data);
                new SaveYuvImageTask(bean, listener)
                        .execute();
                listener = null;
            }
            mCamManager.getCameraIntance().addCallbackBuffer(data);
        }
    };

    public void init(Context context){
        // 实例化摄像头管理类
        mCamManager = CameraManager.getCamManagerInstance(context);
        // 实例化加速传感器
        mSensorAccelerometer = SensorAccelerometer.getSensorInstance();
        mSensorAccelerometer.initSensor(context);
    }

    public void setEncodeParams(EncoderParams mParams) {
        this.mParams = mParams;
    }

    public void startRecord(){
        if(mParams == null)
            throw new IllegalStateException("EncoderParams can not be null,need call setEncodeParams method!");
        mH264Consumer = new H264EncodeConsumer();
        mAacConsumer = new AACEncodeConsumer();
        //new File(mParams.getVideoPath(), new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date())).toString()
        mMuxer = new MediaMuxerUtil(mParams.getVideoPath(), 1000000);
        if (mH264Consumer != null) {
            mH264Consumer.setTmpuMuxer(mMuxer,mParams);
        }
        if (mAacConsumer != null) {
            mAacConsumer.setTmpuMuxer(mMuxer,mParams);
        }
        // 配置好混合器后启动线程
        mH264Consumer.start();
        mAacConsumer.start();
    }

    public void stopRecord(){
        // 停止混合器
        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
            if(RecordMp4.DEBUG)
                Log.i(TAG, TAG + "---->停止本地录制");
        }
        if (mH264Consumer != null) {
            mH264Consumer.setTmpuMuxer(null,null);
        }
        if (mAacConsumer != null) {
            mAacConsumer.setTmpuMuxer(null,null);
        }
        // 停止视频编码线程
        if (mH264Consumer != null) {
            mH264Consumer.exit();
            try {
                Thread t2 = mH264Consumer;
                mH264Consumer = null;
                if (t2 != null) {
                    t2.interrupt();
                    t2.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 停止音频编码线程
        if (mAacConsumer != null) {
            mAacConsumer.exit();
            try {
                Thread t1 = mAacConsumer;
                mAacConsumer = null;
                if (t1 != null) {
                    t1.interrupt();
                    t1.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void startCamera(SurfaceHolder surfaceHolder){
        if(mCamManager == null)
            return;
        mCamManager.setSurfaceHolder(surfaceHolder);
        mCamManager.setOnPreviewResult(mPreviewListener);
        mCamManager.createCamera();
        mCamManager.startPreview();
        startSensorAccelerometer();
    }

    public void stopCamera(){
        if(mCamManager == null)
            return;
        mCamManager.stopPreivew();
        mCamManager.destoryCamera();
        stopSensorAccelerometer();
    }

    public void enableFocus(CameraManager.OnCameraFocusResult listener){
        if(mCamManager != null){
            mCamManager.cameraFocus(listener);
        }
    }

    public void switchCamera(){
        if(mCamManager != null){
            mCamManager.switchCamera();
        }
    }

    public void setPreviewSize(int width, int height){
        if(mCamManager != null){
            mCamManager.modifyPreviewSize(width,height);
        }
    }

    public boolean isFrontCamera(){
        return (mCamManager!=null &&
                mCamManager.getCameraDirection()) ? true : false;
    }

    private void startSensorAccelerometer() {
        // 启动加速传感器，注册结果事件监听器
        if (mSensorAccelerometer != null) {
            mSensorAccelerometer.startSensorAccelerometer(new SensorAccelerometer.OnSensorChangedResult() {
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

    public void capturePicture(String picPath, SaveYuvImageTask.OnSaveYuvResultListener listener) {
        this.picPath = picPath;
        this.listener = listener;
    }

    private String getPicPath(){
        return picPath;
    }
}
