all: libProcessKiller.so

edu_umd_cs_buildServer_util_jni_ProcessKiller.o: edu_umd_cs_buildServer_util_jni_ProcessKiller.c
	gcc  -m32 -I ../BuildServer/jniInclude/ -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -c -Wall -Werror -fpic edu_umd_cs_buildServer_util_jni_ProcessKiller.c

libProcessKiller.so: edu_umd_cs_buildServer_util_jni_ProcessKiller.o
	gcc -m32 -shared -o libProcessKiller.so edu_umd_cs_buildServer_util_jni_ProcessKiller.o

clean:
	rm -f *.o *.so
