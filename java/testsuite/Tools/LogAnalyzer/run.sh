#!/usr/bin/env bash
gradle --quiet :testsuite:Tools:LogAnalyzer:runMain -PrunMainClass=LogAnalyzer
