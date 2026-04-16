#!/usr/bin/env bash
gradle :testsuite:_APITests:_JNIExample:javaProgram:runMain -PrunMainClass=program -PrunJvmArgs='-Djava.library.path=../myLibraryNative/lib'
