//
// Created by 杨小标 on 2023/3/16.
//

#ifndef OPENGL_INKPLAYER_H
#define OPENGL_INKPLAYER_H

#include <cstring>
#include <pthread.h>
#include <mutex>
#include <queue>
#include <iostream>
#include <sys/syscall.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaCrypto.h>
#include <media/NdkMediaFormat.h>
#include <media/NdkMediaMuxer.h>

#include <android/log.h>
#include <android/native_window_jni.h>

#define TAG "InkPlayer"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

class inkPlayer {
public:
    typedef std::function<void()> IOTask;
    std::queue<IOTask> mIOQueue;

    inkPlayer(int fd, int64_t offset, int64_t length, ANativeWindow *window);

    static void onInputAvailable(inkPlayer *ptr);

    static void onOutputAvailable(inkPlayer *ptr);

    void start();

    ~inkPlayer();

private:
    AMediaExtractor *extractor;
    AMediaCodec *videoMediaCodec;
    bool mSawInputEOS = false;
    bool mSawOutputEOS = false;
};

// Async API's callback
void *OnInputAvailableCB(void *arg);

void *OnOutputAvailableCB(void *arg);

#endif //OPENGL_INKPLAYER_H
