#!/usr/bin/env bash
gradle --quiet :testsuite:ApplicationCases:CommandServer:runMain -PrunMainClass=CommandServer -PrunJvmArgs='-Xms300m|-Xmx300m'
