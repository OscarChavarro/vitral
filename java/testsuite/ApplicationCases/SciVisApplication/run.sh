#!/usr/bin/env bash
gradle --quiet :testsuite:ApplicationCases:SciVisApplication:runMain -PrunMainClass=SciVisApplication -PrunJvmArgs='-Xms700m|-Xmx700m'
