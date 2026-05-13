#!/usr/bin/env bash
gradle --quiet :testsuite:_APITests:_WiiRemoteJHelloWorld:runMain -PrunMainClass=WiiRemoteSampleApplication
