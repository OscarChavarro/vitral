#!/usr/bin/env bash
gradle :testsuite:VSDKExamples:MeshExample:runMain -PrunMainClass=MeshExample -PrunJvmArgs='-Xms300m|-Xmx300m'
