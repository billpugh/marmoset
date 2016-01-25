all: made

UNAME := $(shell uname)

# Define a variable for classpath
CLASS_PATH = ../build/classes

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

# $* matches the target filename without the extension
edu_umd_cs_buildServer_util_jni_ProcessKiller.h : edu/umd/cs/buildServer/util/jni/ProcessKiller.class
	javah -classpath $(CLASS_PATH) edu.umd.cs.buildServer.util.jni.ProcessKiller

made: edu_umd_cs_buildServer_util_jni_ProcessKiller.c edu_umd_cs_buildServer_util_jni_ProcessKiller.h
	mkdir -p ../lib/${UNAME}/64
	mkdir -p ../lib/${UNAME}/32
	gcc -m64 -shared -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I ${JAVA_HOME}/include/darwin -Wall -Werror -fpic  -o ../lib/${UNAME}/64/libProcessKiller.so edu_umd_cs_buildServer_util_jni_ProcessKiller.c
	gcc -m32 -shared -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I ${JAVA_HOME}/include/darwin -Wall -Werror -fpic  -o ../lib/${UNAME}/32/libProcessKiller.so edu_umd_cs_buildServer_util_jni_ProcessKiller.c
	touch made

clean:
	rm -rf edu_umd_cs_buildServer_util_jni_ProcessKiller.h *.o made ../lib/${UNAME}
