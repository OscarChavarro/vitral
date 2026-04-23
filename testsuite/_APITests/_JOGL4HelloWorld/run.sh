#!/usr/bin/env bash
gradle :testsuite:_APITests:_JOGL4HelloWorld:runMain \
  -PrunMainClass=HelloWorldJOGL4 \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
