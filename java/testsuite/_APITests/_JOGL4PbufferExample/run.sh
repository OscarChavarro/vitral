#!/usr/bin/env bash
gradle --quiet :testsuite:_APITests:_JOGL4PbufferExample:runMain -PrunMainClass=PbufferExample
