package com.jiangdg.mediacodec4mp4.runnable;

/** 音、视频编码参数
 *
 * Created by jiangdongguo on 2017/9/19.
 */

public class EncoderParams {
    private int frameWidth;     // 图像宽度
    private int frameHeight;    // 图像高度
    private EncoderVideoRunnable.Quality bitRateQuality;   // 视频编码码率,0(低),1(中),2(高)
    private EncoderVideoRunnable.Quality frameRateQuality; // 视频编码帧率,0(低),1(中),2(高)
    private boolean isFrontCamera;  // 前置摄像头
    private boolean isPhoneHorizontal;   // 横屏拍摄

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public boolean isFrontCamera() {
        return isFrontCamera;
    }

    public void setFrontCamera(boolean frontCamera) {
        isFrontCamera = frontCamera;
    }

    public boolean isPhoneHorizontal() {
        return isPhoneHorizontal;
    }

    public void setPhoneHorizontal(boolean phoneHorizontal) {
        isPhoneHorizontal = phoneHorizontal;
    }

    public EncoderVideoRunnable.Quality getFrameRateQuality() {
        return frameRateQuality;
    }

    public void setFrameRateQuality(EncoderVideoRunnable.Quality frameRateQuality) {
        this.frameRateQuality = frameRateQuality;
    }

    public EncoderVideoRunnable.Quality getBitRateQuality() {
        return bitRateQuality;
    }

    public void setBitRateQuality(EncoderVideoRunnable.Quality bitRateQuality) {
        this.bitRateQuality = bitRateQuality;
    }

}
