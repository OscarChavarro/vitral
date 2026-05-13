#!/usr/bin/env bash
gradle --quiet :testsuite:ApplicationCases:SearchEngineFor3DModels:runMain -PrunMainClass=BatchConsole -PrunJvmArgs='-Djava.library.path=../../../lib|-Xms100m|-Xmx1000m'
