#!/bin/sh
uname -a | awk '{print $1}' | tr '[:upper:]' '[:lower:]'
