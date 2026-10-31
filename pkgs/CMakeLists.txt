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

# Every package is a JNI binding, so all of them need the JNI headers (jni.h,
# jni_md.h), which come only with a JDK (not with a JRE). Without them the
# packages are skipped, with a warning, instead of failing the configuration:
# the Java toolkit then silently falls back to its pure Java equivalents (see
# vsdk.toolkit.common.NativeLibraryLoader). Each package still fails on its
# own if configured stand alone without a JDK.
if(WITH_NATIVE_IMAGE_READER OR WITH_LEMPEL_ZIV_WELCH OR WITH_SPHARMONIC_KIT)
  find_package(JNI)
  if(NOT JAVA_INCLUDE_PATH OR NOT JAVA_INCLUDE_PATH2)
    message(WARNING "jni.h not found (install a JDK, e.g. openjdk-17-jdk-headless, "
                    "or set JAVA_HOME): skipping the native JNI packages, the "
                    "pure Java fallbacks will be used")
    set(WITH_NATIVE_IMAGE_READER OFF)
    set(WITH_LEMPEL_ZIV_WELCH OFF)
    set(WITH_SPHARMONIC_KIT OFF)
  endif()
endif()

if(WITH_NATIVE_IMAGE_READER)
  add_subdirectory(NativeImageReader)
endif()

if(WITH_LEMPEL_ZIV_WELCH)
  add_subdirectory(LempelZivWelch)
endif()

if(WITH_SPHARMONIC_KIT)
  add_subdirectory(SpharmonicKit27)
endif()
