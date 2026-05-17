cmake_minimum_required(VERSION 3.16)
project(vitral_cpp LANGUAGES CXX)

include(CTest)
enable_testing()

# Root controller for all C++ subprojects under cpp/
add_subdirectory(base)
