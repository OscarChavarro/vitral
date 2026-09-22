#===========================================================================
#= Root controller for the native (C / C++) packages used by the Vitral    =
#= Java toolkit through JNI.                                              =
#=                                                                        =
#= Build everything with:                                                 =
#=     cmake -S . -B build && cmake --build build -j                      =
#=                                                                        =
#= Each package leaves its shared library in its own ./build directory,   =
#= which is where vsdk.toolkit.common.NativeLibraryLoader looks for it,   =
#= so no installation nor -Djava.library.path is needed.                  =
#===========================================================================

cmake_minimum_required(VERSION 3.16)
project(vitral_pkgs LANGUAGES C CXX)

# On MacOSX, third party libraries (i.e. libpng) usually come from Homebrew
# or MacPorts, whose prefixes are not searched by default on Apple Silicon.
if(APPLE)
  foreach(_prefix "/opt/homebrew" "/opt/local" "/usr/local")
    if(IS_DIRECTORY "${_prefix}")
      list(APPEND CMAKE_PREFIX_PATH "${_prefix}")
    endif()
  endforeach()
endif()

option(WITH_NATIVE_IMAGE_READER "Build the libpng based image reader" ON)
option(WITH_LEMPEL_ZIV_WELCH "Build the LZW compression library" ON)
option(WITH_SPHARMONIC_KIT "Build the spherical harmonics library" ON)

if(WITH_NATIVE_IMAGE_READER)
  add_subdirectory(NativeImageReader)
endif()

if(WITH_LEMPEL_ZIV_WELCH)
  add_subdirectory(LempelZivWelch)
endif()

if(WITH_SPHARMONIC_KIT)
  add_subdirectory(SpharmonicKit27)
endif()
