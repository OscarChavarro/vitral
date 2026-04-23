#!/usr/bin/env bash
gradle :testsuite:Jogl2Examples:MD2Example:runMain -PrunMainClass=Md2MeshExample -PrunJvmArgs='-Xms300m|-Xmx300m'
