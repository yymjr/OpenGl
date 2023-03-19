package com.yymjr.opengl;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_TEXTURE0;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Util {

    public static int createProgram(int vertexShader, int fragmentShader) {
        int programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vertexShader);
        GLES30.glAttachShader(programId, fragmentShader);
        GLES30.glLinkProgram(programId);
        GLES30.glUseProgram(programId);
        return programId;
    }

    public static FloatBuffer createFloatBuffer(final float[] data) {
        FloatBuffer dst = ByteBuffer.allocateDirect(data.length << 2)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        dst.put(data).position(0);
        return dst;
    }

    public static int createShader(int type, String source) {
        int shaderId = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shaderId, source);
        GLES30.glCompileShader(shaderId);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            GLES30.glDeleteShader(shaderId);
            GLES30.glGetShaderInfoLog(shaderId);
            shaderId = 0;
        }
        return shaderId;
    }

    public static void activateTexture(int textureId) {
        GLES30.glActiveTexture(GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
        //配置边缘过渡参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }
}
