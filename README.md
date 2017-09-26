# AndroidRecordMp4
**本地录制Mp4、抓拍jpg图片**


**1. 添加依赖**  

(1) 在工程build.gradle中添加
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
  
(2) 在module的gradle中添加
```
dependencies {
    compile 'com.github.jiangdongguo:AndroidRecordMp4:v1.0.0'
}
```

**2. 使用方法**  

(1) 初始化引擎 
 
```
 RecordMp4 mRecMp4 = RecordMp4.getRecordMp4Instance();
 mRecMp4.init(this);  // 上下文

```

(2) 配置编码参数

```  
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
  mRecMp4.setEncodeParams(getEncodeParams());
```
(3) 开始 /停止录制
 
```
 mRecMp4.startRecord();
 mRecMp4.stopRecord();
```  

(4) Camera渲染  

```  
public class MainActivity extends Activity implements SurfaceHolder.Callback{
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
```  

(5) 摄像头控制  

```  
// 对焦
 mRecMp4.enableFocus(new CameraManager.OnCameraFocusResult() {
         @Override
         public void onFocusResult(boolean result) {
                   if(result){
                        showMsg("对焦成功");
                    }
               }
            });
 // 切换摄像头
  if(mRecMp4 != null){
       mRecMp4.switchCamera();
  }
  // 切换分辨率
   if(mRecMp4 != null){
       mRecMp4.setPreviewSize(1280,720);
   }
```  

（6） JPG图片抓拍  

```  
   mRecMp4.capturePicture(picPath, new SaveYuvImageTask.OnSaveYuvResultListener() {
          @Override
          public void onSaveResult(boolean result, String savePath) {
                  Log.i("MainActivity","抓拍结果："+result+"保存路径："+savePath);
               }
           });
```  

最后，不要忘记添加权限哈  

```  
  <uses-permission android:name="android.permission.RECORD_AUDIO"/>
  <uses-permission android:name="android.permission.CAMERA"/>
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```  


csdn博文地址：http://blog.csdn.net/andrexpert/article/details/72523408
