#!/usr/bin/env bash
gradle :testsuite:VSDKExamples:PolygonClippingExample:runMain \
  -PrunMainClass=PolygonClippingExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
