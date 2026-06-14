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

if(WITH_APRILTAG)
  find_package(apriltag QUIET)
  if(NOT apriltag_FOUND)
    message(FATAL_ERROR
      "apriltag library not found. Install it with:\n"
      "  macOS (Homebrew):  brew install apriltag\n"
      "  Linux (apt):       sudo apt-get install libapriltag-dev\n"
      "  From source:       git clone https://github.com/AprilRobotics/apriltag.git\n"
      "                     cd apriltag && mkdir build && cd build && cmake .. && make install")
  endif()
  if(TARGET apriltag::apriltag)
    set(APRILTAG_TARGET apriltag::apriltag)
  else()
    set(APRILTAG_TARGET apriltag)
  endif()
endif()

if(WITH_OPENCV)
  find_package(OpenCV QUIET COMPONENTS core imgproc videoio calib3d highgui)
  if(NOT OpenCV_FOUND)
    message(FATAL_ERROR
      "OpenCV library not found. Install it with:\n"
      "  macOS (Homebrew):  brew install opencv\n"
      "  Linux (apt):       sudo apt-get install libopencv-dev\n"
      "  From source:       https://opencv.org/releases/")
  endif()
endif()

set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
if(NOT TARGET vitral_base)
  add_subdirectory(../../../base base)
endif()

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

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
