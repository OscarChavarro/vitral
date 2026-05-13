#!/usr/bin/env bash
gradle --quiet :testsuite:Jogl4Examples:PolyhedralBoundedSolidExample:runMain -PrunMainClass=PolyhedralBoundedSolidExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
