#!/usr/bin/env bash
gradle :testsuite:Benchmarks:RamBandwidthBenchmark:runMain -PrunMainClass=RamBandwidthBenchmark -PrunJvmArgs='-Xms800m|-Xmx800m'
