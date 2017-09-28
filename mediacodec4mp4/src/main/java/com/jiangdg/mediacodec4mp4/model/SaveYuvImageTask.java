package com.jiangdg.mediacodec4mp4.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.jiangdg.mediacodec4mp4.bean.YUVBean;

/**保存YUV格式图片
 *	YV12格式，则先转换为NV21格式，实现Java回调机制返回操作结果
 * @author Created by jiangdongguo on 2017-2-25下午9:13:01
 */
public class SaveYuvImageTask extends AsyncTask<Void, Void, Void> {
	private static final String TAG = "SaveYuvImageTask";	
	private YUVBean yuvBean;
	//转换结果回调接口
	private OnSaveYuvResultListener mListener;
	
	public interface OnSaveYuvResultListener{
		void onSaveResult(boolean result, String savePath);
	}

	public SaveYuvImageTask(YUVBean yuvBean, OnSaveYuvResultListener mListener) {
		this.yuvBean = yuvBean;
		this.mListener = mListener;
	}
	
	@Override
	protected Void doInBackground(Void... params) {   
		if (yuvBean == null || yuvBean.getWidth() == 0
				|| yuvBean.getHeight() == 0 || yuvBean.getYuvData() == null) {
			return null;
		}
		int width = yuvBean.getWidth();
		int height = yuvBean.getHeight();
		// 复制一份，防止再操作原始数据时出现异常
		byte[] mData = new byte[width * height * 3 /2];
		System.arraycopy(yuvBean.getYuvData(),0,mData,0,yuvBean.getYuvData().length);

		boolean isEnableSoftCodec = yuvBean.isEnableSoftCodec();
		if(isEnableSoftCodec){
			byte[] mFrameData = new byte[width*height*3/2];
			YV12toNV21(mData,mFrameData,width,height);				
			saveYuv2Jpeg(mFrameData,width,height);
			Log.i(TAG, "使用软编码，将YV12转换为NV21");
		}else{			
			saveYuv2Jpeg(mData,width,height);			
			Log.i(TAG, "使用硬编码，无需转换");
		} 
		return null;
	}

	private void saveYuv2Jpeg(byte[] data,int width,int height){
		YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		boolean result = yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, bos);
		if(result){
			byte[] buffer = bos.toByteArray();
			Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
	        Bitmap bitmap = rotatingImageView(bmp);
	        bmp.recycle();
			
			File file = new File(yuvBean.getPicPath());
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			try {
				fos.flush();
				fos.close();
				//传递转换结果给调用者
				mListener.onSaveResult(true,yuvBean.getPicPath());
			} catch (IOException e) {
				e.printStackTrace();
				mListener.onSaveResult(false,null);
			}
		}
		try {
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
    private void YV12toNV21(final byte[] input, final byte[] output,
			final int width, final int height) {
		final int frameSize = width * height;  //YUV格式Y分量的长度  一帧Y =w*h个像素(字节);  U=Z=Y/4字节   一帧图像内存大小 = Y+U+Z字节
		final int qFrameSize = frameSize / 4;
		final int tempFrameSize = frameSize * 5 / 4;
		if(input==null || output==null){
			return;
		}
		//处理修改分辨率时，报的ArrayOutOfBoundsException
		if(input.length < frameSize){
			return;
		}
		System.arraycopy(input, 0, output, 0, frameSize); // Y
		for (int i = 0; i < qFrameSize; i++) {
			output[frameSize + i * 2] = input[frameSize + i]; // Cb (U)
			output[frameSize + i * 2 + 1] = input[tempFrameSize + i]; // Cr (V)
		}
	}
   
	private Bitmap rotatingImageView(Bitmap bitmap){
		int result = 0;
		int phoneDegree = yuvBean.getDegree();
		CameraInfo cameraInfo = new CameraInfo();
		if(yuvBean.isFrontCamera()){			
			Camera.getCameraInfo(CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
			result = (cameraInfo.orientation - phoneDegree +360) % 360;
		}else{
			Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, cameraInfo);
			result = (cameraInfo.orientation + phoneDegree) % 360;  
		}
		//旋转图片动作
		Matrix matrix = new Matrix();
		matrix.postRotate(result);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),bitmap.getHeight(),matrix,true);
	}
}
