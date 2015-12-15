/*
 * edu_umd_cs_buildServer_util_jni_ProcessKiller.c
 *
 *  Created on: Dec 15, 2015
 *      Author: pugh
 */

#include <jni.h>

#include "edu_umd_cs_buildServer_util_jni_ProcessKiller.h"

#include <signal.h>

JNIEXPORT jint JNICALL Java_edu_umd_cs_buildServer_util_jni_ProcessKiller_kill
  (JNIEnv * env, jclass class, jint pid, jint sig) {

    return kill(pid, sig);
}
