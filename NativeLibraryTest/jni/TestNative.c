/*
 * TestNative.c
 *
 *  Created on: Jan 25, 2016
 *      Author: pugh
 */

#include <jni.h>
#include <stdio.h>
#include "TestNative.h"

JNIEXPORT jint JNICALL Java_TestNative_whatIsTheAnswer
(JNIEnv * env, jclass class)
{
   return 42;
}
