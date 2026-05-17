cmake_minimum_required(VERSION 3.16)
project(vitral_base_cpp LANGUAGES CXX)
cmake_policy(SET CMP0135 NEW)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

add_library(vitral_base SHARED
    src/VSDK.cpp
    src/main/vsdk/toolkit/common/logging/Logger.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Complex.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/MatrixNxM.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4d.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4f.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/AlgebraPrimitives.cpp
    src/main/vsdk/toolkit/processing/linealAlgebra/LinearAlgebraEngine.cpp
    src/main/vsdk/toolkit/java/lang/String.cpp
    src/main/vsdk/toolkit/java/util/Formatter.cpp
    src/main/vsdk/toolkit/gui/KeyEvent.cpp
    src/main/vsdk/toolkit/environment/camera/Camera.cpp
    src/main/vsdk/toolkit/gui/CameraControllerAquynza.cpp
)

target_include_directories(vitral_base PUBLIC src src/main)

option(VITRAL_BUILD_TESTS "Build tests" ON)
if(VITRAL_BUILD_TESTS)
  include(CTest)
  enable_testing()

  include(FetchContent)
  FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/archive/refs/tags/v1.14.0.zip
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
  )
  set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
  FetchContent_MakeAvailable(googletest)

  add_executable(vitral_base_tests
    src/test/vector_tests.cpp
    src/test/quaternion_tests.cpp
    src/test/matrix_nxm_tests.cpp
    src/test/matrix4x4_tests.cpp
  )
  target_link_libraries(vitral_base_tests PRIVATE vitral_base GTest::gtest_main)
  include(GoogleTest)
  gtest_discover_tests(vitral_base_tests)
endif()
