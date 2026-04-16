#!/usr/bin/env bash
gradle :testsuite:Benchmarks:QuadBenchmark:runMain -PrunMainClass=QuadBenchmark -PrunJvmArgs='-Xms800m|-Xmx800m'
