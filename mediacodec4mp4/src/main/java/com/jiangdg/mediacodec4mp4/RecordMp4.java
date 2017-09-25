package com.jiangdg.mediacodec4mp4;

import android.os.Environment;
import android.util.Log;

import com.jiangdg.mediacodec4mp4.bean.EncoderParams;
import com.jiangdg.mediacodec4mp4.bean.YUVBean;
import com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.MediaMuxerUtil;

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

    private static RecordMp4 mRecMp4;

    private RecordMp4(){}

    public static RecordMp4 getRecordMp4Instance(){
        if(mRecMp4 == null){
            mRecMp4 = new RecordMp4();
        }
        return mRecMp4;
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


    public void feedYUV2Consumer(YUVBean rawYuv) {
        if (mH264Consumer != null) {
            mH264Consumer.addData(rawYuv.getYuvData());
        }
    }

//    public void capturePicture(YUVBean bean, SaveYuvImageTask.OnSaveYuvResultListener listener) {
//        new SaveYuvImageTask(bean, listener)
//                .execute();
//    }
}
