all: made

UNAME := $(shell uname)


# Define a variable for classpath
CLASS_PATH = ../bin

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

# $* matches the target filename without the extension
TestNative.h : TestNative.class
	javah -classpath $(CLASS_PATH) $*

made: TestNative.c TestNative.h
	mkdir -p ${UNAME}/64
	mkdir -p ${UNAME}/32
	gcc -m64 -shared -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I ${JAVA_HOME}/include/darwin -Wall -Werror -fpic  -o ${UNAME}/64/libProcessKiller.so TestNative.c
	gcc -m32 -shared -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I ${JAVA_HOME}/include/darwin -Wall -Werror -fpic  -o ${UNAME}/32/libProcessKiller.so TestNative.c
	touch made

clean:
	rm -rf TestNative.h *.o made ${UNAME}
