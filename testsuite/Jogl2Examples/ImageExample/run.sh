#!/usr/bin/env bash
gradle :testsuite:Jogl2Examples:ImageExample:runMain \
  -PrunMainClass=ImageExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
