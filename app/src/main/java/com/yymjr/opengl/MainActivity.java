package com.yymjr.opengl;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Optional;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private AssetFileDescriptor fileDescriptor;
    @Nullable
    private SurfaceTexture mSurfaceTexture;
    @Nullable
    private Surface mSurface;
    private final static int COORDS_PER_VERTEX = 3;
    private final static int VERTEX_Stride = COORDS_PER_VERTEX << 2; // 4 bytes per vertex
    final static String vertexShaderSource = "attribute vec3 inPos;" +
            "attribute vec2 inTexCoord;" +
            "varying vec2 TexCoord;\n" +
            "void main() {" +
            "   gl_Position = vec4(inPos, 1.0f);" +
            "   TexCoord = inTexCoord;" +
            "}";
    final static String fragmentShaderSource = "#extension GL_OES_EGL_image_external : require\n" +
            "varying vec2 TexCoord;" +
            "uniform samplerExternalOES inTexture;\n" +
            "void main() {" +
            "   gl_FragColor = texture2D(inTexture, TexCoord);" +
            "}";
    final static float[] vertexWithTexture = {
            -1.0f, 1.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 0.0f
    };

    final static short[] drawOrder = {0, 1, 2, 0, 2, 3};
    final static Buffer drawOrderBuffer = ByteBuffer.allocateDirect(drawOrder.length << 1)
            .order(ByteOrder.nativeOrder()).asShortBuffer().put(drawOrder).position(0);

    static {
        System.loadLibrary("opengl");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileDescriptor = getFdFromAssets("ymsk.mp4");
        GLSurfaceView glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_LOG_GL_CALLS);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        glSurfaceView.setEGLContextFactory(new ContextFactory());
        glSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                int vertexShader = Util.createShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource);
                int fragmentShader = Util.createShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource);

                int programId = Util.createProgram(vertexShader, fragmentShader);

                //texture
                int textureId = createTexture();
                Util.activateTexture(textureId);

                mSurfaceTexture = new SurfaceTexture(textureId);
                mSurface = new Surface(mSurfaceTexture);
                mSurfaceTexture.updateTexImage();
                setDataSource(fileDescriptor, mSurface);

                //vbo
                int[] VBOs = new int[1];
                GLES30.glGenBuffers(1, VBOs, 0);

                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, VBOs[0]);
                FloatBuffer floatBuffer = Util.createFloatBuffer(vertexWithTexture);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexWithTexture.length << 2, floatBuffer, GL_STATIC_DRAW);


                int positionHandle = GLES30.glGetAttribLocation(programId, "inPos");
                GLES30.glEnableVertexAttribArray(positionHandle);
                GLES30.glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, (3 + 2) << 2, 0);
                int colorHandle = GLES30.glGetAttribLocation(programId, "inTexCoord");
                GLES30.glEnableVertexAttribArray(colorHandle);
                GLES30.glVertexAttribPointer(colorHandle, 2, GL_FLOAT, false, (3 + 2) << 2, VERTEX_Stride);
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                GLES30.glViewport(0, 0, width, height);
                GLES30.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES30.glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
                Optional.ofNullable(mSurfaceTexture).ifPresent(SurfaceTexture::updateTexImage);
                GLES30.glDrawElements(GL_TRIANGLE_STRIP, drawOrder.length, GL_UNSIGNED_SHORT, drawOrderBuffer);
            }
        });
        setContentView(glSurfaceView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (IOException ignored) {
        }
        Optional.ofNullable(mSurface).ifPresent(Surface::release);
        Optional.ofNullable(mSurfaceTexture).ifPresent(SurfaceTexture::release);
    }

    public static int createTexture() {
        int[] textureHandles = new int[1];
        GLES30.glGenTextures(1, textureHandles, 0);
        return textureHandles[0];
    }


    public AssetFileDescriptor getFdFromAssets(String fileName) {
        try {
            return getAssets().openFd(fileName);
        } catch (IOException ignored) {
        }
        return null;
    }


    public static void setDataSource(AssetFileDescriptor fd, Surface mSurface) {
        if (fd.getDeclaredLength() < 0) {
            init(fd.getFileDescriptor(), 0, 0x7ffffffffffffffL, mSurface);
        } else {
            init(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength(), mSurface);
        }
        decode();
    }

    public static native void init(FileDescriptor fileDescriptor, long offset, long length, Surface surface);

    public static native void decode();
}