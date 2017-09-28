package com.jiangdg.mediacodec4mp4.bean;

import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;

/** 音、视频编码参数
 *
 * Created by jiangdongguo on 2017/9/19.
 */

public class EncoderParams {
    private String videoPath;
    private int frameWidth;     // 图像宽度
    private int frameHeight;    // 图像高度
    private H264EncodeConsumer.Quality bitRateQuality;   // 视频编码码率,0(低),1(中),2(高)
    private H264EncodeConsumer.FrameRate frameRateDegree; // 视频编码帧率,0(低),1(中),2(高)
    private boolean isVertical;

    private String picPath;     // 图片抓拍路径
    private int audioBitrate;   // 音频编码比特率
    private int audioChannelCount; // 通道数据
    private int audioSampleRate;   // 采样率

    private int audioChannelConfig; // 单声道或立体声
    private int audioFormat;    // 采样精度
    private int audioSouce;     // 音频来源

    public EncoderParams(){}

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public int getAudioChannelConfig() {
        return audioChannelConfig;
    }

    public void setAudioChannelConfig(int audioChannelConfig) {
        this.audioChannelConfig = audioChannelConfig;
    }

    public boolean isVertical() {
        return isVertical;
    }

    public void setVertical(boolean vertical) {
        isVertical = vertical;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(int audioFormat) {
        this.audioFormat = audioFormat;
    }

    public int getAudioSouce() {
        return audioSouce;
    }

    public void setAudioSouce(int audioSouce) {
        this.audioSouce = audioSouce;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    public int getAudioChannelCount() {
        return audioChannelCount;
    }

    public void setAudioChannelCount(int audioChannelCount) {
        this.audioChannelCount = audioChannelCount;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

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

    public H264EncodeConsumer.FrameRate getFrameRateDegree() {
        return frameRateDegree;
    }

    public void setFrameRateDegree(H264EncodeConsumer.FrameRate frameRateDegree) {
        this.frameRateDegree = frameRateDegree;
    }

    public H264EncodeConsumer.Quality getBitRateQuality() {
        return bitRateQuality;
    }

    public void setBitRateQuality(H264EncodeConsumer.Quality bitRateQuality) {
        this.bitRateQuality = bitRateQuality;
    }

}
