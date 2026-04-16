#!/usr/bin/env bash
gradle :testsuite:ApplicationCases:CommandServer:runMain -PrunMainClass=CommandServer -PrunJvmArgs='-Xms300m|-Xmx300m'
