#!/usr/bin/env bash
gradle :testsuite:ApplicationCases:SciVisApplication:runMain -PrunMainClass=SciVisApplication -PrunJvmArgs='-Xms700m|-Xmx700m'
