package com.jiangdg.mediacodec4mp4;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.jiangdg.mediacodec4mp4.bean.EncoderParams;
import com.jiangdg.mediacodec4mp4.bean.YUVBean;
import com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.MediaMuxerUtil;
import com.jiangdg.mediacodec4mp4.model.SaveYuvImageTask;
import com.jiangdg.mediacodec4mp4.utils.CameraManager;
import com.jiangdg.mediacodec4mp4.utils.SensorAccelerometer;
import com.jiangdg.yuvosd.YuvUtils;

import org.easydarwin.sw.JNIUtil;
import org.easydarwin.sw.TxtOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    // 时间水印
    private TxtOverlay overlay;
    private String overlayContent;
    private String frontPath;
    private Enum<OverlayType> type;
    private int mDegree = 0;


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
            // 处理1：旋转YUV
            rotateYuv2(data,camera,width,height);

            // 处理2：yuv叠加水印
            if(overlay != null){
                String txt = null;
                if(type == OverlayType.WORDS){
                    txt = overlayContent;
                }else if(type == OverlayType.BOTH){
                    txt = new SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss").format(new Date()) +"  " +overlayContent;
                }else if(type == OverlayType.TIME){
                    txt = new SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss").format(new Date());
                }
                if(! TextUtils.isEmpty(txt)){
                    overlay.overlay(data, txt);
                }
            }
            // 处理3：yuv转换颜色格式，再编码
            if (mH264Consumer != null) {
                mH264Consumer.addData(data);
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
        // 将assets目录下的SIMYOU.ttf文件
        // 保存到data目录的files下
        saveFrontFile(context);

        getDgree(context);

        // 初始化水印引擎
        // SIMYOU.ttf文件存在/data/data/程序Package Name/files
        overlay = new TxtOverlay(context);
        frontPath = (context).getFileStreamPath("SIMYOU.ttf").getPath();
    }



    private byte[] rotateYuv1(byte[] data,int width,int height){
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
            // 如果是正向横屏，则无需旋转YUV，此时isPhoneHorizontal=true
//            if(mParams!= null && mParams.isPhoneHorizontal()){
//                return data;
//            }
            // 后置旋转90度(即竖直采集，此时isPhoneHorizontal=false)
            YuvUtils.YUV420spRotateOfBack(data, rotateNv21, width, height, 90);
            // 后置旋转270度(即倒立采集，此时isPhoneHorizontal=false)
//			YuvUtils.YUV420spRotateOfBack(rawFrame, rotateNv21, mWidth, mHeight, 270);
            // 后置旋转180度(即反向横屏采集，此时isPhoneHorizontal=true)
//			YuvUtils.YUV420spRotateOfBack(rawFrame, rotateNv21, mWidth, mHeight, 180);

        }
        return  rotateNv21;
    }

    private void rotateYuv2(byte[] data, Camera camera,int width,int height){
        if(CameraManager.PREVIEW_WIDTH != width || CameraManager.PREVIEW_HEIGHT != height){
            CameraManager.PREVIEW_WIDTH = width;
            CameraManager.PREVIEW_HEIGHT = height;
            return;
        }
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        if(isFrontCamera()){
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, camInfo);
        }else{
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);
        }
        int cameraRotationOffset = camInfo.orientation;
        if (cameraRotationOffset % 180 != 0) {
                yuvRotate(data, 1, width, height, cameraRotationOffset);
        }
    }

    /**
     * 旋转YUV格式数据
     *
     * src    YUV数据
     * format 0，420P；1，420SP
     * width  宽度
     * height 高度
     * degree 旋转度数
     */
    private static void yuvRotate(byte[] src, int format, int width, int height, int degree) {
        int offset = 0;
        if (format == 0) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += (width * height);
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
            offset += width * height / 4;
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
        } else if (format == 1) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += width * height;
            JNIUtil.rotateShortMatrix(src, offset, width / 2, height / 2, degree);
        }
    }

    // 设置水印类型
    public void setOverlayType(OverlayType type){
        this.type = type;
    }

    // 设置水印内容
    public void setOverlayContent(String overlayContent){
        this.overlayContent = overlayContent;
    }


    public void setEncodeParams(EncoderParams mParams) {
        this.mParams = mParams;
    }

    public void startRecord(){
        if(mParams == null)
            throw new IllegalStateException("EncoderParams can not be null,need call setEncodeParams method!");
        // 判断手机方向
        boolean rotate = false;
        if(mDegree == 0){
            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(isFrontCamera()? Camera.CameraInfo.CAMERA_FACING_FRONT
                    : Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);
            int cameraRotationOffset = camInfo.orientation;
            if (cameraRotationOffset == 90) {
                rotate = true;
            } else if (cameraRotationOffset == 270) {
                rotate = true;
            }
        }
        if(! rotate){
            // 垂直水印
            overlay.init(mParams.getFrameWidth(), mParams.getFrameHeight(),frontPath);
        }else{
            // 水平
            overlay.init(mParams.getFrameHeight(), mParams.getFrameWidth(), frontPath);
        }
        mParams.setVertical(rotate);
        Log.i(TAG,"-------------------->rotate = "+rotate);

        // 创建音视频编码线程
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
        // 释放水印引擎
        if (overlay != null)
            overlay.release();
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

    public void restartCamera(){
        mCamManager.createCamera();
        mCamManager.startPreview();
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

    private void saveFrontFile(Context context) {
        File youyuan = context.getFileStreamPath("SIMYOU.ttf");
        if (!youyuan.exists()){
            AssetManager am = context.getAssets();
            try {
                InputStream is = am.open("zk/SIMYOU.ttf");
                FileOutputStream os = context.openFileOutput("SIMYOU.ttf", Context.MODE_PRIVATE);
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.close();
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getPicPath(){
        return picPath;
    }

    private void getDgree(Context context) {
        int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                mDegree = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                mDegree = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                mDegree = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                mDegree = 270;
                break;// Landscape right
        }
    }
}
