//
// Created by 杨小标 on 2023/3/16.
//

#include <unistd.h>
#include "inkPlayer.h"

inkPlayer::inkPlayer(int fd, int64_t offset, int64_t length, ANativeWindow *window) {
    extractor = AMediaExtractor_new();
    media_status_t err = AMediaExtractor_setDataSourceFd(extractor, fd, offset, length);
    if (err != AMEDIA_OK) {
        LOGE("setDataSource error: %d", err);
        return;
    }
    size_t numTracks = AMediaExtractor_getTrackCount(extractor);
    const char *mime;
    for (uint i = 0; i < numTracks; i++) {
        AMediaFormat *format = AMediaExtractor_getTrackFormat(extractor, i);

        if (!AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime)) {
            LOGE("no mime type");
            continue;
        }
        if (!strncmp(mime, "video/", 6)) {
            videoMediaCodec = AMediaCodec_createDecoderByType(mime);
            AMediaCodec_configure(videoMediaCodec, format, window, nullptr, 0);
            err = AMediaExtractor_selectTrack(extractor, i);
            if (err != AMEDIA_OK) {
                LOGE("AMediaExtractor_selectTrack error: %d", err);
                return;
            }
        } else if (!strncmp(mime, "audio/", 6)) {
            LOGE("not support audio");
        } else {
            LOGE("expected audio or video mime type, got %s", mime);
        }
        AMediaFormat_delete(format);
    }
}

void inkPlayer::onInputAvailable(inkPlayer *ptr) {
    LOGV("onInputAvailable id:%ld", syscall(__NR_gettid));
    while (!ptr->mSawInputEOS) {
        int t = AMediaExtractor_getSampleTrackIndex(ptr->extractor);
        LOGV("current track:%d", t);
        ssize_t index = AMediaCodec_dequeueInputBuffer(ptr->videoMediaCodec, 5000);
        if (index < 0) continue;
        size_t bufSize;
        uint8_t *buf = AMediaCodec_getInputBuffer(ptr->videoMediaCodec, index, &bufSize);
        int sampleSize = AMediaExtractor_readSampleData(ptr->extractor, buf, bufSize);
        if (sampleSize < 0) {
            sampleSize = 0;
            ptr->mSawInputEOS = true;
            LOGV("EOS");
        }
        int64_t presentationTimeUs = AMediaExtractor_getSampleTime(ptr->extractor);
        LOGV("read sampleSize:%d PTS:%lld", sampleSize, presentationTimeUs);
        media_status_t err = AMediaCodec_queueInputBuffer(ptr->videoMediaCodec, index, 0,
                                                          sampleSize,
                                                          presentationTimeUs,
                                                          ptr->mSawInputEOS
                                                          ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM
                                                          : 0);
        if (err != AMEDIA_OK) {
            LOGE("AMediaCodec_queueInputBuffer error: %d", err);
        }
        AMediaExtractor_advance(ptr->extractor);
    }
}

void inkPlayer::onOutputAvailable(inkPlayer *ptr) {
    LOGV("onOutputAvailable id:%ld", syscall(__NR_gettid));
    AMediaCodecBufferInfo info;
    while (!ptr->mSawOutputEOS) {
        ssize_t index = AMediaCodec_dequeueOutputBuffer(ptr->videoMediaCodec, &info, 5000);
        LOGV("dequeue Output index：%d", index);
        if (index < 0) continue;
        if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
            LOGV("EOS on track");
            ptr->mSawOutputEOS = true;
        }
        LOGV("got decoded buffer for size %d", info.size);
        AMediaCodec_releaseOutputBuffer(ptr->videoMediaCodec, index, true);
    }
}

void *OnInputAvailableCB(void *arg) {
    auto *ptr = (inkPlayer *) arg;
    ptr->onInputAvailable(ptr);
    return nullptr;
}

void *OnOutputAvailableCB(void *arg) {
    auto *ptr = (inkPlayer *) arg;
    ptr->onOutputAvailable(ptr);
    return nullptr;
}

void inkPlayer::start() {
    AMediaCodec_start(videoMediaCodec);

    pthread_t tid;
    int ref = pthread_create(&tid, nullptr, OnInputAvailableCB, this);
    ref = pthread_create(&tid, nullptr, OnOutputAvailableCB, this);
    if (ref != 0) {
        LOGV("create thread failed.");
    }
}

inkPlayer::~inkPlayer() {
    AMediaCodec_delete(videoMediaCodec);
    AMediaExtractor_delete(extractor);
}
