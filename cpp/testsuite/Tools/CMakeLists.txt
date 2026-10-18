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
