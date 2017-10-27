/**
 * Created by jianxi on 2017/6/4..
 * https://github.com/mabeijianxi
 * mabeijianxi@gmail.com
 */

#include "ffmpeg.h"
#include <jni.h>
#include "android_log.h"

JNIEXPORT jint JNICALL
Java_com_daixun_gifbox_util_FFmpegCmd_cmdRun(JNIEnv *env, jclass type, jobjectArray commands) {

    int argc = (*env)->GetArrayLength(env,commands);
    char *argv[argc];
    int i;
    for (i = 0; i < argc; i++) {
        jstring js = (jstring) (*env)->GetObjectArrayElement(env,commands, i);
        argv[i] = (char *) (*env)->GetStringUTFChars(env,js, 0);
    }
    return jxRun(argc,argv);

}