#!/usr/bin/env bash
gradle --quiet :testsuite:Benchmarks:QuadBenchmark:runMain -PrunMainClass=QuadBenchmark -PrunJvmArgs='-Xms800m|-Xmx800m'
