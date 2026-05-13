#!/usr/bin/env bash
gradle --quiet :testsuite:Benchmarks:RamBandwidthBenchmark:runMain -PrunMainClass=RamBandwidthBenchmark -PrunJvmArgs='-Xms800m|-Xmx800m'
