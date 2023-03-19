package com.yymjr.opengl;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class ContextFactory implements GLSurfaceView.EGLContextFactory {

    @Override
    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL10.EGL_NONE};
        EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        if (context == null) {
            // returns null if 3.0 is not supported;
        }
        return context;
    }

    @Override
    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
        egl.eglDestroyContext(display, context);
    }
}
