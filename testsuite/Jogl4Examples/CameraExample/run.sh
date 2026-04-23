#!/usr/bin/env bash
gradle :testsuite:Jogl4Examples:CameraExample:runMain \
  -PrunMainClass=CameraExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
