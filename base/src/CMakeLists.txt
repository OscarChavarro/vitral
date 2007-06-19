
# Packages
SET(VSDK_BASIC_PCKS
  toolkit/common
  toolkit/environment
  toolkit/environment/geometry
  toolkit/environment/geometry/polyhedralBoundedSolidNodes
  toolkit/environment/scene
  toolkit/gui
  toolkit/media
  toolkit/processing
  toolkit/render)
SET(VSDK_IO_PCKS
  toolkit/io
  toolkit/io/image
  toolkit/io/geometry
  toolkit/io/metadata)
SET(VSDK_AWT_PCKS
  toolkit/render/awt)

# Optional packages
IF(BUILD_JOGL)
  SET(VSDK_JOGL_PCKS
    toolkit/render/jogl)
ENDIF(BUILD_JOGL)

# Compilation
VSDK_COMPILE_JAVA_PACKAGE(
  vsdk
  ${VSDK_SOURCE_DIR}/src
  ${VSDK_BASIC_PCKS}
  ${VSDK_IO_PCKS}
  ${VSDK_AWT_PCKS}
  ${VSDK_JOGL_PCKS})

## eof - CMakeLists.txt
