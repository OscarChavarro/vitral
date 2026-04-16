#!/usr/bin/env bash
gradle :testsuite:DistributedExamples:RaytracingDistributedExample:runMain -PrunMainClass=RaytracerDistributed -PrunJvmArgs='-Xms300m|-Xmx300m'
