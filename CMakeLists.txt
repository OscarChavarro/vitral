PROJECT(VSDK Java)

# --------------------------------------------------------------------------
# VSDK version number
SET(VSDK_MAJOR_VERSION 1)
SET(VSDK_MINOR_VERSION 0)
SET(VSDK_BUILD_VERSION 0)
SET(VSDK_VERSION
  "${VSDK_MAJOR_VERSION}.${VSDK_MINOR_VERSION}.${VSDK_BUILD_VERSION}")

# --------------------------------------------------------------------------
# Compilation options
OPTION(BUILD_JAVA_DEBUG "Build java code with debug information." OFF)
OPTION(BUILD_JAI "Build JAI based code." OFF)
OPTION(BUILD_JOGL "Build JOGL based code." OFF)
OPTION(BUILD_TESTSUITE "Build the test suite." OFF)

# --------------------------------------------------------------------------
# Additional dependencies
INCLUDE("${VSDK_SOURCE_DIR}/VSDKCompileJava.cmake")

# --------------------------------------------------------------------------
# Real compilation starts here
SUBDIRS(src)
IF(BUILD_TESTSUITE)
  SUBDIRS(testsuite)
ENDIF(BUILD_TESTSUITE)

## eof - CMakeLists.txt
