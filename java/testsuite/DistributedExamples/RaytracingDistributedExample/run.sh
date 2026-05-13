#!/usr/bin/env bash
gradle --quiet :testsuite:DistributedExamples:RaytracingDistributedExample:runMain -PrunMainClass=RaytracerDistributed -PrunJvmArgs='-Xms300m|-Xmx300m'
