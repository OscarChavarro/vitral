#!/usr/bin/env bash
gradle --quiet :testsuite:Tools:TangibleInterfaceGizmoCreator:runMain -PrunMainClass=TangibleInterfaceGizmoCreator \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
