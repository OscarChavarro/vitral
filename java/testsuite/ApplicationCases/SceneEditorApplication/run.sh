#!/usr/bin/env bash
gradle --quiet :testsuite:ApplicationCases:SceneEditorApplication:runMain -PrunMainClass=application.SceneEditorApplication -PrunJvmArgs='-Djava.library.path=../../../lib|-Xms300m|-Xmx300m'
