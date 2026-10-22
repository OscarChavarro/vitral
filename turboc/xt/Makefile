# Makefile of vitral_xt (the Xt layer of the toolkit) for
# legacy toolchains without CMake (e.g. GNU make 3.75 and gcc 2.7.2 on
# Debian 1.1). It mirrors the VITRAL_XT_SOURCES list of CMakeLists.txt:
# keep both in sync.
#
# The library is built as a static archive in build/libvitral_xt.a, with
# the objects in buildmk/. The options are the ones of ../legacy.mk.

VITRAL_DIR = ..
include $(VITRAL_DIR)/legacy.mk

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    -Isrc/main $(BASE_INCLUDES) $(X11_INCLUDES) $(X11_DEFINES) $(DEFINES)

SOURCES = \
    src/main/vsdk/toolkit/gui/XtEventQueue.cpp \
    src/main/vsdk/toolkit/gui/XtSystem.cpp \
    src/main/vsdk/toolkit/gui/XtTextEncoding.cpp \
    src/main/vsdk/toolkit/gui/XtWidgetSupport.cpp \
    src/main/vsdk/toolkit/gui/editor/XtGenericEditor.cpp \
    src/main/vsdk/toolkit/render/xt/XtDialogHost.cpp \
    src/main/vsdk/toolkit/render/xt/XtTreeView.cpp \
    src/main/vsdk/toolkit/render/xlib/XlibRGBAImageUncompressedRenderer.cpp \
    src/main/vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.cpp

OBJECTS = $(patsubst %.cpp,buildmk/%.o,$(SOURCES))
LIBRARY = build/libvitral_xt.a

all: $(LIBRARY)

$(LIBRARY): $(OBJECTS)
	@mkdir -p build
	rm -f $@
	$(AR) rc $@ $(OBJECTS)
	$(RANLIB) $@

buildmk/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -c $< -o $@

clean:
	rm -rf buildmk $(LIBRARY)

.PHONY: all clean
