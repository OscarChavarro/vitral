#!/usr/bin/env bash
gradle --quiet :testsuite:Jogl2Examples:MeshExample:runMain -PrunMainClass=MeshExample -PrunJvmArgs='-Xms300m|-Xmx300m'
