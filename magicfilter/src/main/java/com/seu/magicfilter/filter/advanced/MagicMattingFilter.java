package com.seu.magicfilter.filter.advanced;

import android.opengl.GLES20;
import android.util.Log;

import com.momo.zgreenmattingshader.ZGreenMattingUtil;
import com.seu.magicfilter.filter.base.gpuimage.GPUImageFilter;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2016/5/22.
 */
public class MagicMattingFilter extends GPUImageFilter {
    private int mParamsLocation;
    private int keyRLocation;
    private int keyGLocation;
    private int keyBLocation;
    private int agLocation;
    private int noiseRdLocation;
    private int transRdLocation;
    private int blackRdLocation;


    private float[] mPositionTransformMatrix;
    private int mPositionTransformMatrixLocation;


    public MagicMattingFilter() {
//        super(NO_FILTER_VERTEX_SHADER_1 ,
//                OpenGlUtils.readShaderFromRawResource(R.raw.fragment_shader_matting));
//        super(mMattingVertexShader, OpenGlUtils.readShaderFromRawResource(R.raw.fragment_shader_matting));
        super(ZGreenMattingUtil.getMattingVertexShader(), ZGreenMattingUtil.getMattingFragmentShader()   /*ZGreenMattingUtil.getFra()*/);
    }

    protected void onInit() {
        super.onInit();

        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "uTexture");
        mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
        mPositionTransformMatrixLocation = GLES20.glGetUniformLocation(mGLProgId, "uMVPMatrix");
        keyRLocation = GLES20.glGetUniformLocation(getProgram(), "key_r");
        keyGLocation = GLES20.glGetUniformLocation(getProgram(), "key_g");
        keyBLocation = GLES20.glGetUniformLocation(getProgram(), "key_b");
        agLocation = GLES20.glGetUniformLocation(getProgram(), "ag");
        noiseRdLocation = GLES20.glGetUniformLocation(getProgram(), "noise_rd");
        transRdLocation = GLES20.glGetUniformLocation(getProgram(), "trans_rd");
        blackRdLocation = GLES20.glGetUniformLocation(getProgram(), "black_rd");

    }


    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
    }

    public void setParams(float params) {
        setFloat(mParamsLocation, params);
    }

    public void setKeyParams(float R, float G, float B, float ag, float noise_rd, float trans_rd, float black_rd) {
        setFloat(keyRLocation, R);
        setFloat(keyGLocation, G);
        setFloat(keyBLocation, B);
        setFloat(agLocation, ag);
        setFloat(noiseRdLocation, noise_rd);
        setFloat(transRdLocation, trans_rd);
        setFloat(blackRdLocation, black_rd);

    }


    //这里设置背景色rgb
    //noise_rd  噪声抑制，范围是 0-0.5   trans_rd  透明抑制， 范围是 0-0.5 ， black_rd  范围是 0- 1.0
    //modified@ 20180829
    protected void onInitialized() {
        super.onInitialized();
        setParams(0.5f);

        ByteBuffer key_color = ByteBuffer.allocate(4);
        GLES20.glReadPixels(10, 10, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, key_color);
        // GLES20.get
        int _r = key_color.get(0) & 0XFF;
        int _g = key_color.get(1) & 0XFF;
        int _b = key_color.get(2) & 0XFF;
        float rf = _r / 255.0f;
        float gf = _g / 255.0f;
        float bf = _b / 255.0f;
        String ss = String.format(" key color :%d, %d, %d", _r, _g, _b);
        Log.e("matting", ss);
        setKeyParams(0.0f, 0.8f, 0.0f, 80.0f, 0.08f, 0.1f, 0.5f);
    }

    @Override
    protected void onDrawArraysPre() {
        super.onDrawArraysPre();
        GLES20.glUniformMatrix4fv(mPositionTransformMatrixLocation, 1, false, mPositionTransformMatrix, 0);
    }

    public void setPositionTransformMatrix(float[] mtx) {
        mPositionTransformMatrix = mtx;
    }
}
