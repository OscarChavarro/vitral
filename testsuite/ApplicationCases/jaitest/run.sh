#!/usr/bin/env bash
gradle :testsuite:ApplicationCases:jaitest:runMain -PrunMainClass=jaitest -PrunJvmArgs='-Xms300m|-Xmx300m'
