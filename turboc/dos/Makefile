# Makefile of the DOS-specific services module for legacy Unix build hosts.
# It mirrors CMakeLists.txt and builds the host validation implementation;
# Turbo C++ uses the same sources after scripts/map_to_83.py converts them.

VITRAL_DIR = ..
include $(VITRAL_DIR)/legacy.mk

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    -Isrc/main $(BASE_INCLUDES) $(DEFINES)

SOURCES = \
    src/main/vsdk/toolkit/media/XmsMemory.cpp \
    src/main/vsdk/toolkit/media/XmsRgbImage.cpp

OBJECTS = $(patsubst %.cpp,buildmk/%.o,$(SOURCES))
LIBRARY = build/libvitral_dos.a

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
