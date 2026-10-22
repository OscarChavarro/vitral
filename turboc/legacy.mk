# Settings shared by the Makefiles of the turboc port for legacy toolchains
# without CMake (i.e. GNU make 3.75 and gcc 2.7.2 on Debian 1.1). Each
# Makefile sets VITRAL_DIR (this folder, relative to it) before including it.
#
# Options (on the make command line):
#   WITH_POSIX_THREADS=1 WITH_JPEG=1 WITH_PNG=1 WITH_FREETYPE=1
# and, also from the environment:
#   X11_DIR=<prefix>  X11 prefix (default /usr/X11R6)
#   GL_DIR=<prefix>   prefix of an OpenGL implementation outside the system
#                     folders, i.e. GL_DIR=/usr/local/src/Mesa-3.0 (the
#                     default of scripts/findLegacyGlDir.sh, used by
#                     scripts/compileWithMake.sh)

CXX = g++
AR = ar
RANLIB = ranlib
OPTIMIZATION = -O2
EXTRA_CXXFLAGS =
EXTRA_LIBS =

# gcc 2.x needs exception handling to be enabled explicitly, and gcc 2.7.2
# only catches an exception by a base class (i.e. `catch (const Exception&)`)
# with RTTI, which needs <typeinfo> in every file (Gcc2TypeInfo.h; the code
# itself does not use RTTI). gcc 2.7.2 also crashes when inlining functions
# with exception cleanups, and with optimizations its exception tables refer
# to deleted labels (undefined .Lnnn references when linking), so it builds
# without optimizations
GXX_VERSION := $(shell $(CXX) -dumpversion)
ifeq ($(firstword $(subst ., ,$(GXX_VERSION))),2)
OPTIMIZATION = -O0
LEGACY_CXXFLAGS = -fhandle-exceptions -frtti -fno-inline \
    -include $(VITRAL_DIR)/base/src/main/vsdk/toolkit/common/Gcc2TypeInfo.h
else
LEGACY_CXXFLAGS =
endif

# Optional features of the base library, with the libraries they need
DEFINES =
SYSTEM_LIBS =
ifdef WITH_JPEG
DEFINES += -DVITRAL_WITH_JPEG
SYSTEM_LIBS += -ljpeg
endif
ifdef WITH_PNG
DEFINES += -DVITRAL_WITH_PNG
SYSTEM_LIBS += -lpng -lz
endif
ifdef WITH_FREETYPE
DEFINES += -DVITRAL_WITH_FREETYPE -I/usr/include/freetype2
SYSTEM_LIBS += -lfreetype
endif
# The same thread configuration for the libraries and the programs, as the
# layout of the classes that hold mutexes depends on it
ifdef WITH_POSIX_THREADS
DEFINES += -DVITRAL_WITH_POSIX_THREADS
SYSTEM_LIBS += -lpthread
else
# Single threaded replacement of pthreads (see java/lang/PosixThreads.h)
DEFINES += -DVITRAL_WITHOUT_PTHREADS
endif
SYSTEM_LIBS += -lm

BASE_INCLUDES = -I$(VITRAL_DIR)/base/src -I$(VITRAL_DIR)/base/src/main
BASE_LIBRARY = $(VITRAL_DIR)/base/build/libvitral_base.a

# X11 (the Xt layer, its Athena widget set and the scene editor). The `String`
# type of Xt is renamed `XtString` around its headers (see XtIntrinsics.h)
ifndef X11_DIR
X11_DIR = /usr/X11R6
endif
X11_INCLUDES = -I$(X11_DIR)/include
X11_DEFINES = -DXTSTRINGDEFINES -DXMSTRINGDEFINES
X11_LIBS = -L$(X11_DIR)/lib -lXt -lXext -lX11
# Xlib with its own locale implementation (X_LOCALE, i.e. XFree86 3.2 over
# libc 5): the GUI selects its locale with _Xsetlocale (see XtTextEncoding)
ifneq ($(shell grep -l _Xsetlocale $(X11_DIR)/lib/libX11.so 2>/dev/null),)
X11_DEFINES += -DVITRAL_X_LOCALE
endif

# OpenGL and GLU: Mesa until 2.x names its libraries MesaGL and MesaGLU
ifneq ($(GL_DIR),)
GL_INCLUDES = -I$(GL_DIR)/include
GL_LDFLAGS = -L$(GL_DIR)/lib
else
GL_INCLUDES =
GL_LDFLAGS =
endif
ifneq ($(wildcard $(GL_DIR)/lib/libMesaGL.so /usr/lib/libMesaGL.so \
    /usr/local/lib/libMesaGL.so $(X11_DIR)/lib/libMesaGL.so),)
GL_LIBS = $(GL_LDFLAGS) -lMesaGLU -lMesaGL
else
GL_LIBS = $(GL_LDFLAGS) -lGLU -lGL
endif

# GLUT (OpenGL1Examples and _APITests); the GLUT of Mesa 3.0 does not name
# the X libraries it needs
GLUT_LIBS = $(GL_LDFLAGS) -lglut -L$(X11_DIR)/lib -lXmu -lXi -lXext -lX11

# Mesa offscreen rendering (OSMesa, for _OpenGL1PbufferExample): part of
# libMesaGL up to Mesa 3.0, libOSMesa in later versions. OSMESA_LIBS is
# empty when it is missing.
OSMESA_LIBS =
ifneq ($(wildcard $(GL_DIR)/include/GL/osmesa.h /usr/include/GL/osmesa.h \
    /usr/local/include/GL/osmesa.h),)
ifneq ($(shell grep -l OSMesaCreateContext $(GL_DIR)/lib/libMesaGL.so \
    /usr/local/lib/libMesaGL.so /usr/lib/libMesaGL.so 2>/dev/null),)
OSMESA_LIBS = $(GL_LIBS) -L$(X11_DIR)/lib -lXext -lX11
endif
ifneq ($(wildcard $(GL_DIR)/lib/libOSMesa.so /usr/lib/libOSMesa.so \
    /usr/local/lib/libOSMesa.so /usr/lib/*/libOSMesa.so),)
OSMESA_LIBS = $(GL_LDFLAGS) -lOSMesa
endif
endif
