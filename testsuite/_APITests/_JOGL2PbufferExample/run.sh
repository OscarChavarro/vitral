#!/usr/bin/env bash
gradle --quiet :testsuite:_APITests:_JOGL2PbufferExample:runMain -PrunMainClass=PbufferExample
