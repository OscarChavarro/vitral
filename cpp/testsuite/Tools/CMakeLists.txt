# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

# Prefer the locally built OpenCV distribution when it is available.  Its
# package configuration lives below lib/cmake/opencv4, a standard suffix
# searched from this prefix by find_package(OpenCV).
set(_VITRAL_LOCAL_OPENCV_PREFIX "/usr/local/src/opencv/opencv-4.x_live")
if(EXISTS "${_VITRAL_LOCAL_OPENCV_PREFIX}/lib/cmake/opencv4/OpenCVConfig.cmake")
  list(PREPEND CMAKE_PREFIX_PATH "${_VITRAL_LOCAL_OPENCV_PREFIX}")
endif()

add_subdirectory(TangibleInterfaceLabelsCreator)
add_subdirectory(TangibleInterfaceMarkersCreator)
add_subdirectory(TangibleInterfaceMarkersDetectorServer)
