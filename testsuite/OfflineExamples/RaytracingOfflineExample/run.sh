#!/usr/bin/env bash
gradle :testsuite:OfflineExamples:RaytracingOfflineExample:runMain -PrunMainClass=RaytracerSimple -PrunJvmArgs='-Xms300m|-Xmx300m'
