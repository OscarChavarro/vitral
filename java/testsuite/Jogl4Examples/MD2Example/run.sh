#!/usr/bin/env bash
gradle --quiet :testsuite:Jogl4Examples:MD2Example:runMain \
  -PrunMainClass=Md2MeshExample \
  -PrunJvmArgs='-Xms300m|-Xmx300m|--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
