#!/usr/bin/env bash
gradle :testsuite:Jogl2Examples:MeshExample:runMain -PrunMainClass=MeshExample -PrunJvmArgs='-Xms300m|-Xmx300m'
