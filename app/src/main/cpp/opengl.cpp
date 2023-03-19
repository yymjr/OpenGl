#include <jni.h>
#include <cstring>
#include <cstdio>

#include <android/native_window_jni.h>
#include "inkPlayer.h"

inkPlayer *player;

extern "C"
JNIEXPORT void JNICALL
Java_com_yymjr_opengl_MainActivity_decode(JNIEnv *env, jclass clazz) {
    if (player != nullptr) {
        player->start();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yymjr_opengl_MainActivity_init(JNIEnv *env, jclass clazz, jobject fileDescriptor,
                                        jlong offset, jlong length, jobject surface) {
    if (fileDescriptor != nullptr) {
        ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
        jclass fileDescriptorClazz = env->GetObjectClass(fileDescriptor);
        jfieldID descriptorId = env->GetFieldID(fileDescriptorClazz, "descriptor", "I");
        jint fd = env->GetIntField(fileDescriptor, descriptorId);
        player = new inkPlayer(fd, offset, length, window);
    }
}