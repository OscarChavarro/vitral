# Makefile of vitral_xaw (the Athena widget set of the Xt layer) for
# legacy toolchains without CMake (e.g. GNU make 3.75 and gcc 2.7.2 on
# Debian 1.1). It mirrors the VITRAL_XAW_SOURCES list of CMakeLists.txt:
# keep both in sync.
#
# The library is built as a static archive in build/libvitral_xaw.a, with
# the objects in buildmk/. The options are the ones of ../legacy.mk.

VITRAL_DIR = ..
include $(VITRAL_DIR)/legacy.mk

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    -Isrc/main -I$(VITRAL_DIR)/xt/src/main $(BASE_INCLUDES) $(X11_INCLUDES) \
    $(X11_DEFINES) $(DEFINES)

SOURCES = \
    src/main/vsdk/toolkit/gui/XawPanelWidgets.cpp \
    src/main/vsdk/toolkit/gui/XawWidgetSet.cpp \
    src/main/vsdk/toolkit/render/xaw/XawGuiRenderer.cpp

OBJECTS = $(patsubst %.cpp,buildmk/%.o,$(SOURCES))
LIBRARY = build/libvitral_xaw.a

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
