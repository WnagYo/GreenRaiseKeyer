package com.seu.magicfilter.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.seu.magicfilter.camera.CameraEngine;
import com.seu.magicfilter.camera.utils.CameraInfo;
import com.seu.magicfilter.encoder.video.TextureMovieEncoder;
import com.seu.magicfilter.filter.advanced.MagicAAFilter;
import com.seu.magicfilter.filter.advanced.MagicBeautyFilter;
import com.seu.magicfilter.filter.base.MagicCameraInputFilter;
import com.seu.magicfilter.helper.SavePictureTask;
import com.seu.magicfilter.utils.MagicParams;
import com.seu.magicfilter.utils.OpenGlUtils;
import com.seu.magicfilter.utils.Rotation;
import com.seu.magicfilter.utils.TextureRotationUtil;
import com.seu.magicfilter.widget.base.MagicBaseView;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by why8222 on 2016/2/25.
 */
public class MagicCameraView extends MagicBaseView {

    private MagicCameraInputFilter cameraInputFilter;
    private MagicBeautyFilter beautyFilter;

    private SurfaceTexture surfaceTexture;

    public MagicCameraView(Context context) {
        this(context, null);
    }

    private boolean recordingEnabled;
    private int recordingStatus;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static TextureMovieEncoder videoEncoder = new TextureMovieEncoder();

    private File outputFile;

    private RectF mRectF = new RectF();
    int mScreenWidth;
    int mScreenHeight;
    public float[] mMVP = new float[16];
    float mScalerFactor = 1.f;
    float mMoveX = 0.f;
    float mMoveY = 0.f;

    private final LinkedList<Runnable> mRunOnDrawEnd = new LinkedList<Runnable>();

    public MagicCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.getHolder().addCallback(this);
        outputFile = new File(MagicParams.videoPath, MagicParams.videoName);
        recordingStatus = -1;
        recordingEnabled = false;
        scaleType = ScaleType.CENTER_CROP;
    }

    private OnCameraInitedListener onCameraInitedListener;

    public OnCameraInitedListener getOnCameraInitedListener() {
        return onCameraInitedListener;
    }

    public void setOnCameraInitedListener(OnCameraInitedListener onCameraInitedListener) {
        this.onCameraInitedListener = onCameraInitedListener;
    }

    public interface OnCameraInitedListener {

        public void onCameraInited();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        recordingEnabled = videoEncoder.isRecording();
        if (recordingEnabled) {
            recordingStatus = RECORDING_RESUMED;
        } else {
            recordingStatus = RECORDING_OFF;
        }
        if (cameraInputFilter == null) {
            cameraInputFilter = new MagicCameraInputFilter();
        }
        cameraInputFilter.init();
        if (textureId == OpenGlUtils.NO_TEXTURE) {
            textureId = OpenGlUtils.getExternalOESTextureID();
            if (textureId != OpenGlUtils.NO_TEXTURE) {
                surfaceTexture = new SurfaceTexture(textureId);
                surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        mScreenWidth = width;
        mScreenHeight = height;

        mRectF.left = 0;
        mRectF.top = 0;

        mRectF.right = mScreenWidth;
        mRectF.bottom = mScreenHeight;

        openCamera();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        if (surfaceTexture == null) {
            return;
        }
        surfaceTexture.updateTexImage();
        if (recordingEnabled) {
            switch (recordingStatus) {
                case RECORDING_OFF:
                    CameraInfo info = CameraEngine.getCameraInfo();
                    videoEncoder.setPreviewSize(info.previewWidth, info.pictureHeight);
                    videoEncoder.setTextureBuffer(gLTextureBuffer);
                    videoEncoder.setCubeBuffer(gLCubeBuffer);
                    videoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            outputFile, info.previewWidth, info.pictureHeight,
                            1000000, EGL14.eglGetCurrentContext(),
                            info));
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    videoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        } else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    videoEncoder.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        }
        float[] mtx = new float[16];
        surfaceTexture.getTransformMatrix(mtx);
        cameraInputFilter.setTextureTransformMatrix(mtx);

//        float[] projectMatrix = new float[16];
//        Matrix.setIdentityM(projectMatrix, 0);


        String messsage = String.format("(%f, %f, %f, %f)", mRectF.left, mRectF.top,
                mRectF.right, mRectF.bottom);
//        Log.d("MagicCameraView", messsage);
        calculateMatrix(mRectF, mScreenWidth, mScreenHeight);
        cameraInputFilter.setPositionTransformMatrix(mMVP);

        int id = textureId;
        if (filter == null) {
            cameraInputFilter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
        } else {
            id = cameraInputFilter.onDrawToTexture(textureId);
//            ((MagicAAFilter) filter).setPositionTransformMatrix(mMVP);
            resetMatrix();
            ((MagicAAFilter) filter).setPositionTransformMatrix(mMVP);
            filter.onDrawFrame(id, gLCubeBuffer, gLTextureBuffer);
        }
        videoEncoder.setTextureId(id);
        videoEncoder.frameAvailable(surfaceTexture);
        runAll();
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestRender();
        }
    };


    //    @Override
//    public void setFilter(MagicFilterType type) {
//        super.setFilter(type);
//        videoEncoder.setFilter(type);
//    }

    private void openCamera() {
        if (CameraEngine.getCamera() == null) {
            CameraEngine.openCamera();
        }
        CameraInfo info = CameraEngine.getCameraInfo();
        if (info.orientation == 90 || info.orientation == 270) {
            imageWidth = info.pictureHeight;
            imageHeight = info.pictureWidth;
        } else {
            imageWidth = info.pictureWidth;
            imageHeight = info.pictureHeight;
        }
        cameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);
        adjustSize(info.orientation, info.isFront, true);
        if (surfaceTexture != null) {
            CameraEngine.startPreview(surfaceTexture);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        CameraEngine.releaseCamera();
    }

    public void changeRecordingState(boolean isRecording) {
        recordingEnabled = isRecording;
    }

    @Override
    protected void onFilterChanged() {
        super.onFilterChanged();
        cameraInputFilter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
        if (filter != null) {
            cameraInputFilter.initCameraFrameBuffer(imageWidth, imageHeight);
        } else {
            cameraInputFilter.destroyFramebuffers();
        }
    }

    private static final String TAG = "yitu";

    @Override
    public void savePicture(final SavePictureTask savePictureTask) {
        CameraEngine.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                CameraEngine.stopPreview();
                final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        final Bitmap photo = drawPhoto(bitmap, CameraEngine.getCameraInfo().isFront);
                        //在onSurfaceChanged中设置设置视图窗口
                        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
                        if (photo != null) {
                            savePictureTask.execute(photo);
                        }
                    }
                });
                CameraEngine.startPreview();
            }
        });
    }

//    @Override
//    public void savePicture(final SavePictureTask savePictureTask) {
//        runOnDrawEnd(new Runnable() {
//            @Override
//            public void run() {
//                GLES20.glFinish();
//                ByteBuffer pixelBuffer = ByteBuffer.allocate(getWidth() * getHeight() * 4);
//
//                GLES20.glReadPixels(0, 0, getWidth(), getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
//                Bitmap result = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
//                result.copyPixelsFromBuffer(pixelBuffer);
//                if (result != null) {
//                    savePictureTask.execute(result);
//                    //加入回收
//                    result.recycle();
//                }
//            }
//        });
//    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.addLast(runnable);
        }
    }

    private void runAll() {
        synchronized (mRunOnDrawEnd) {
            while (!mRunOnDrawEnd.isEmpty()) {
                mRunOnDrawEnd.removeFirst().run();
            }
        }

    }


    /**
     * 这个方法在绿募拍照之后，合成的图片会有部分黑屏并且图片方向不正确。
     *
     * @param bitmap
     * @param isRotated
     * @return
     */
    private Bitmap drawPhoto(Bitmap bitmap, boolean isRotated) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.e(TAG, "\nwidth:" + width + "\nheight:" + height);
        int[] mFrameBuffers = new int[1];
        int[] mFrameBufferTextures = new int[1];
        if (beautyFilter == null) {
            beautyFilter = new MagicBeautyFilter();
        }
        beautyFilter.init();
        beautyFilter.onDisplaySizeChanged(width, height);
        beautyFilter.onInputSizeChanged(width, height);

        if (filter != null) {
            Log.e(TAG, "drawPhoto: filter!=null");
            filter.onInputSizeChanged(width, height);
            filter.onDisplaySizeChanged(width, height);
        }
        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        GLES20.glViewport(0, 0, width, height);
        int textureId = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, true);

        //申请底层空间并将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        FloatBuffer gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        FloatBuffer gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_ROTATED_180.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);
        if (isRotated) {
            gLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, false)).position(0);
        } else {
            gLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);
        }


        if (filter == null) {
            Log.e(TAG, "onDrawFrame: filter==null");
            beautyFilter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
        } else {
            Log.e(TAG, "onDrawFrame: filter!=null");
            beautyFilter.onDrawFrame(textureId);
            filter.onDrawFrame(mFrameBufferTextures[0], gLCubeBuffer, gLTextureBuffer);
        }
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(ib);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
        GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);

        beautyFilter.destroy();
        beautyFilter = null;
        if (filter != null) {
            Log.e(TAG, "imageWidth: " + imageWidth + "\nimageHeight:" + imageHeight);
            Log.e(TAG, "surfaceWidth:" + surfaceWidth + "\nsurfaceHeight:" + surfaceHeight);
            filter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
            filter.onInputSizeChanged(imageWidth, imageHeight);
        }
        return result;
    }

    public void onBeautyLevelChanged() {
        cameraInputFilter.onBeautyLevelChanged();
    }


    public void calculateMatrix(RectF rectF, float screenWidth, float screenHeight) {
        Matrix.setIdentityM(mMVP, 0);
        float scaleX = 1f / 4f;
        float scaleY = 1f / 4f;
        float ratioX = (rectF.left - .5f * (1 - scaleX) * screenWidth) / rectF.width();
        float ratioY = (rectF.top - .5f * (1 + scaleY) * screenHeight) / rectF.height();
        Matrix.scaleM(mMVP, 0, mScalerFactor, mScalerFactor, 0);
        Matrix.translateM(mMVP, 0, mMoveX * mScalerFactor, mMoveY * mScalerFactor, 0);
    }

    public void resetMatrix() {
        //yitu
//        Log.e("yitu", "resetMatrix: ");
        Matrix.setIdentityM(mMVP, 0);
        Matrix.scaleM(mMVP, 0, 1, 1, 0);
        Matrix.translateM(mMVP, 0, 0, 0, 0);
//        Matrix.translateM(yourModelMatrix,offsetValue,translationInx,translationInY,translationInZ);
//        Matrix.rotateM(yourModelMatrix,offsetValue,andgle in degrees,inDirectionX,inDirectioY,inDirectionZ);
    }


    public void move(float x, float y, int leftRight) {
        mRectF.left = mRectF.left + x;
        mRectF.right = mRectF.right + x;

        mRectF.top = mRectF.top + y;
        mRectF.bottom = mRectF.bottom + y;

        if (filter == null) {
            mMoveX = x;
            mMoveY = y;
        } else {
            if (leftRight == 1) {
                mMoveX = y;
                mMoveY = x;
            } else {
                mMoveX = -y;
                mMoveY = x;
            }
        }

    }

    public void scale(float scaleFactor) {
        mScalerFactor = scaleFactor;
    }

}
