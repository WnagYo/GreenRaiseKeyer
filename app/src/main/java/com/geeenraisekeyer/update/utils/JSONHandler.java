package com.geeenraisekeyer.update.utils;

import android.text.TextUtils;

import com.geeenraisekeyer.update.pojo.UpdateInfo;
import com.geeenraisekeyer.utils.JSONUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by Shelwee on 14-5-8.
 */
public class JSONHandler {

    public static UpdateInfo toUpdateInfo(InputStream is) throws Exception {
        if (is == null) {
            return null;
        }
        String byteData = new String(readStream(is));
        is.close();
        JSONObject jsonObject = new JSONObject(byteData);
        UpdateInfo updateInfo = JSONUtil.toBean(jsonObject,UpdateInfo.class);
        if(TextUtils.isEmpty(updateInfo.getVersionCode())){
            return null;
        }else {
            return updateInfo;
        }
    }

    private static byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] array = new byte[1024];
        int len = 0;
        while ((len = inputStream.read(array)) != -1) {
            outputStream.write(array, 0, len);
        }
        inputStream.close();
        outputStream.close();
        return outputStream.toByteArray();
    }

}
