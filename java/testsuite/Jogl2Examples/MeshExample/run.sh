#!/usr/bin/env bash
gradle --quiet :testsuite:Jogl2Examples:MeshExample:runMain \
  -PrunMainClass=MeshExample \
  -PrunJvmArgs='-Xms300m|-Xmx300m|--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
