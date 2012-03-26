#!/bin/sh

pushd /home/vbraun/Eclipse/workspace/Quill

javah -jni -d jni -classpath bin/classes org.libharu.Document
javah -jni -d jni -classpath bin/classes org.libharu.Page
javah -jni -d jni -classpath bin/classes org.libharu.Font


/home/vbraun/opt/android-ndk-r7/ndk-build

popd
