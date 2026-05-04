#!/usr/bin/env bash
gradle --quiet :testsuite:_APITests:_JOGL2HelloWorld:runMain \
  -PrunMainClass=HelloWorldJOGL \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
