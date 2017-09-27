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
import com.teligen.yuvosd.YuvUtils;

import org.easydarwin.sw.JNIUtil;
import org.easydarwin.sw.TxtOverlay;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar;

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
    // 时间水印
    private TxtOverlay overlay;
    private String overlayContent;
    private boolean enableOverlay;
    private Enum<OverlayType> type;

    public enum OverlayType{
        TIME,WORDS,BOTH
    }

    private RecordMp4(){}

    public static RecordMp4 getRecordMp4Instance(){
        if(mRecMp4 == null){
            mRecMp4 = new RecordMp4();
        }
        return mRecMp4;
    }

    // 预览数据处理
    private CameraManager.OnPreviewFrameResult mPreviewListener = new CameraManager.OnPreviewFrameResult() {
        @Override
        public void onPreviewResult(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            int width = 0;
            int height = 0;
            if(parameters != null){
                width = parameters.getPreviewSize().width;
                height = parameters.getPreviewSize().height;
            }
            // 图片抓拍
            if(listener != null){
                YUVBean bean = new YUVBean();
                bean.setEnableSoftCodec(false);
                bean.setDegree(0);
                bean.setFrontCamera(isFrontCamera());
                bean.setWidth(width);
                bean.setHeight(height);
                bean.setPicPath(getPicPath());
                bean.setYuvData(data);
                new SaveYuvImageTask(bean, listener)
                        .execute();
                listener = null;
            }
            // 旋转yuv
            byte[] rotateYuv = rotateYuvData(data,width,height);

            // 叠加水印
            if(rotateYuv != null){
                if(overlay != null){
                    String txt = null;
                    if(type == OverlayType.WORDS){
                        txt = overlayContent;
                    }else if(type == OverlayType.BOTH){
                        txt = new SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss").format(new Date()) +"  " +overlayContent;
                    }else if(type == OverlayType.TIME){
                        txt = new SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss").format(new Date());
                    }
                    overlay.overlay(rotateYuv, txt);
                }

                // 编码原始数据
                if (mH264Consumer != null) {
                    mH264Consumer.addData(rotateYuv);
                }
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
        // 初始化水印引擎
        // SIMYOU.ttf文件存在/data/data/程序Package Name/files
        overlay = new TxtOverlay(context);

        // 竖屏显示水印，适用于先旋转，再叠加水印
        overlay.init(CameraManager.PREVIEW_HEIGHT, CameraManager.PREVIEW_WIDTH,
                (context).getFileStreamPath("SIMYOU.ttf").getPath());
//        // 横屏显示水印，适用于先叠加水印，再旋转
//        overlay.init(CameraManager.PREVIEW_WIDTH, CameraManager.PREVIEW_HEIGHT,
//                (context).getFileStreamPath("SIMYOU.ttf").getPath());
    }

    private byte[] rotateYuvData(byte[] data,int width,int height){
        if(CameraManager.PREVIEW_WIDTH != width || CameraManager.PREVIEW_HEIGHT != height){
            CameraManager.PREVIEW_WIDTH = width;
            CameraManager.PREVIEW_HEIGHT = height;
            return null;
        }
        byte[] rotateNv21 = new byte[width * height * 3 / 2];
        boolean isFrontCamera = mCamManager!=null && mCamManager.getCameraDirection() ? true : false;
        if (isFrontCamera) {
            // 前置旋转270度(即竖屏采集，此时isPhoneHorizontal=false)
            YuvUtils.Yuv420spRotateOfFront(data, rotateNv21, width, height, 270);
        } else {
            // 后置旋转90度(即竖直采集，此时isPhoneHorizontal=false)
            YuvUtils.YUV420spRotateOfBack(data, rotateNv21, width, height, 90);
            // 后置旋转270度(即倒立采集，此时isPhoneHorizontal=false)
//			YuvUtils.YUV420spRotateOfBack(rawFrame, rotateNv21, mWidth, mHeight, 270);
            // 后置旋转180度(即反向横屏采集，此时isPhoneHorizontal=true)
//			YuvUtils.YUV420spRotateOfBack(rawFrame, rotateNv21, mWidth, mHeight, 180);
            // 如果是正向横屏，则无需旋转YUV，此时isPhoneHorizontal=true
        }
        return  rotateNv21;
    }

    // 设置水印类型
    public void setOverlayType(OverlayType type){
        this.type = type;
    }

    // 设置水印内容
    public void setOverlayContent(String overlayContent){
        this.overlayContent = overlayContent;
    }

    public void release(){
        // 释放水印引擎
        if (overlay != null)
            overlay.release();
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
