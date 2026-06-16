cmake_minimum_required(VERSION 3.16)
project(tangibleInterfaceServer LANGUAGES C CXX)

if(POLICY CMP0135)
  cmake_policy(SET CMP0135 NEW)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()

option(WITH_APRILTAG "Use AprilTag library for marker detection" ON)
option(WITH_OPENCV "Use OpenCV library for camera/video" ON)

set(BUILD_TANGIBLE_INTERFACE_SERVER TRUE)

if(WITH_APRILTAG)
  if(NOT TARGET apriltag::apriltag AND NOT TARGET apriltag)
    find_package(apriltag QUIET)
  endif()
  if(NOT TARGET apriltag::apriltag AND NOT TARGET apriltag AND NOT apriltag_FOUND)
    message(WARNING
      "apriltag library not found. TangibleInterfaceMarkersDetectorServer will be skipped.\n"
      "  To enable, install with:\n"
      "  macOS (Homebrew):  brew install apriltag\n"
      "  Linux (apt):       sudo apt-get install libapriltag-dev\n"
      "  From source:       git clone https://github.com/AprilRobotics/apriltag.git\n"
      "                     cd apriltag && mkdir build && cd build && cmake .. && make install")
    set(WITH_APRILTAG OFF)
    set(BUILD_TANGIBLE_INTERFACE_SERVER FALSE)
  else()
    if(TARGET apriltag::apriltag)
      set(APRILTAG_TARGET apriltag::apriltag)
    elseif(TARGET apriltag)
      set(APRILTAG_TARGET apriltag)
    else()
      message(WARNING "apriltag package found but targets not defined properly. Trying fallback...")
      find_library(APRILTAG_LIB apriltag)
      if(APRILTAG_LIB)
        set(APRILTAG_TARGET ${APRILTAG_LIB})
      else()
        message(WARNING "Could not find apriltag library. TangibleInterfaceMarkersDetectorServer will be skipped.")
        set(BUILD_TANGIBLE_INTERFACE_SERVER FALSE)
      endif()
    endif()
  endif()
endif()

if(WITH_OPENCV)
  find_package(OpenCV QUIET COMPONENTS core imgproc videoio calib3d highgui)
  if(NOT OpenCV_FOUND)
    message(WARNING
      "OpenCV library not found. TangibleInterfaceMarkersDetectorServer will be skipped.\n"
      "  To enable, install with:\n"
      "  macOS (Homebrew):  brew install opencv\n"
      "  Linux (apt):       sudo apt-get install libopencv-dev\n"
      "  From source:       https://opencv.org/releases/")
    set(WITH_OPENCV OFF)
    set(BUILD_TANGIBLE_INTERFACE_SERVER FALSE)
  endif()
endif()

set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
if(NOT TARGET vitral_base)
  add_subdirectory(../../../base base)
endif()

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

if(BUILD_TANGIBLE_INTERFACE_SERVER)
  add_executable(tangibleInterfaceServer
  src/main.cpp
  src/vision/MarkerTracker.cpp
  src/model/CleanerConsumer.cpp
  src/options/CommandLineOptions.cpp
  src/model/MarkersModel.cpp
  src/processing/MarkerPoser.cpp
  src/io/MarkerGroupPersistence.cpp
  src/gui/KeyboardInteractionTechniques.cpp
  src/render/OpenCVMarkersRenderer.cpp
  src/render/OpenCVHudRenderer.cpp
  src/render/OpenCVMarkerGroupRenderer.cpp
  src/InteractiveMarkers.cpp
  src/config/Configuration.cpp
  src/webservice/WebSocketProtocol.cpp
  src/webservice/WebServiceServer.cpp
  src/webservice/WebServiceClient.cpp
  ../../../base/src/main/java/net/ServerSocket.cpp
  ../../../base/src/main/java/net/Socket.cpp
  ../../../base/src/main/java/net/SocketInputStream.cpp
  ../../../base/src/main/java/net/SocketOutputStream.cpp
  ../../../base/src/main/jackson/databind/JsonNode.cpp
  ../../../base/src/main/jackson/databind/ObjectMapper.cpp
)

target_include_directories(tangibleInterfaceServer PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}/src
  ${OpenCV_INCLUDE_DIRS}
  ${apriltag_SOURCE_DIR}
)

target_link_libraries(tangibleInterfaceServer PRIVATE
  ${APRILTAG_TARGET}
  ${OpenCV_LIBS}
  vitral_base
  Threads::Threads
)

set_target_properties(tangibleInterfaceServer PROPERTIES
  RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build"
)
else()
  message(STATUS "TangibleInterfaceMarkersDetectorServer skipped due to missing dependencies")
endif()
