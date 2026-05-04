#!/usr/bin/env bash
gradle --quiet :testsuite:_APITests:_JNIExample:javaProgram:runMain -PrunMainClass=program -PrunJvmArgs='-Djava.library.path=../myLibraryNative/lib'
