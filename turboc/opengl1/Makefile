# Makefile of vitral_opengl1 (the OpenGL 1.x renderers of the toolkit) for
# legacy toolchains without CMake (e.g. GNU make 3.75 and gcc 2.7.2 on
# Debian 1.1). It mirrors the VITRAL_OPENGL1_SOURCES list of CMakeLists.txt:
# keep both in sync.
#
# The library is built as a static archive in build/libvitral_opengl1.a, with
# the objects in buildmk/. The options are the ones of ../legacy.mk.

VITRAL_DIR = ..
include $(VITRAL_DIR)/legacy.mk

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    -Isrc/main $(BASE_INCLUDES) $(GL_INCLUDES) $(X11_INCLUDES) $(DEFINES)

SOURCES = \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Loader.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MatrixState.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Renderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1BackgroundRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1FrameBufferReader.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MinMaxRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1SelectionCornersRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ViewportWindow.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ViewportSetRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1CameraRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MatrixRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ColorDepthImageRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RGBImageUncompressedRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RGBAImageUncompressedRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RGBAImageCompressedRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1LightRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ArrowRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1InputGizmoRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1RayGizmoRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1TranslateGizmoRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1RotateGizmoRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1ScaleGizmoRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Polygon2DRenderer.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1GluTessellator.cpp \
    src/main/vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.cpp \
    src/main/vsdk/toolkit/render/opengl1/OpenGL1SphereRenderer.cpp

OBJECTS = $(patsubst %.cpp,buildmk/%.o,$(SOURCES))
LIBRARY = build/libvitral_opengl1.a

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
