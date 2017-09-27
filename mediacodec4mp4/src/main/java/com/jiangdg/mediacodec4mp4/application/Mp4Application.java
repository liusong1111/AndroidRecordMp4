package com.jiangdg.mediacodec4mp4.application;

import android.app.Application;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**全局类
 *
 * Created by jiangdongguo on 2017/9/27.
 */

public class Mp4Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 将assets目录下的SIMYOU.ttf文件
        // 保存到data目录的files下
        saveFrontFile();
    }

    private void saveFrontFile() {
        File youyuan = getFileStreamPath("SIMYOU.ttf");
        if (!youyuan.exists()){
            AssetManager am = getAssets();
            try {
                InputStream is = am.open("zk/SIMYOU.ttf");
                FileOutputStream os = openFileOutput("SIMYOU.ttf", MODE_PRIVATE);
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
}
