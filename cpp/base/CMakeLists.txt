cmake_minimum_required(VERSION 3.16)
project(vitral_base_cpp LANGUAGES CXX)
cmake_policy(SET CMP0135 NEW)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

# Optional image format support
option(WITH_JPEG "Enable JPEG image format support" OFF)
option(WITH_PNG "Enable PNG image format support" OFF)
option(WITH_POSIX_THREADS "Enable POSIX thread based java.util.concurrent emulation" ON)

# Try to find JPEG library if requested
if(WITH_JPEG)
    find_package(JPEG)
    if(NOT JPEG_FOUND)
        message(WARNING "JPEG library not found. JPEG support will be disabled.")
        set(WITH_JPEG OFF)
    endif()
endif()

# Try to find PNG library if requested
if(WITH_PNG)
    find_package(PNG)
    if(NOT PNG_FOUND)
        message(WARNING "PNG library not found. PNG support will be disabled.")
        set(WITH_PNG OFF)
    endif()
endif()

# Print configuration summary
message(STATUS "Vitral C++ Base Configuration:")
message(STATUS "  JPEG Support: ${WITH_JPEG}")
message(STATUS "  PNG Support:  ${WITH_PNG}")
message(STATUS "  POSIX Threads: ${WITH_POSIX_THREADS}")

# Source files for the base library
set(VITRAL_BASE_SOURCES
    src/main/vsdk/toolkit/common/VSDK.cpp
    src/main/vsdk/toolkit/common/logging/Logger.cpp
    src/main/vsdk/toolkit/common/color/ColorRgb.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Complex.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/MatrixNxM.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4d.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4f.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/AlgebraPrimitives.cpp
    src/main/vsdk/toolkit/processing/linealAlgebra/LinearAlgebraEngine.cpp
    src/main/vsdk/toolkit/processing/ComputationalGeometry.cpp
    src/main/vsdk/toolkit/java/lang/Object.cpp
    src/main/vsdk/toolkit/java/lang/String.cpp
    src/main/vsdk/toolkit/java/lang/System.cpp
    src/main/vsdk/toolkit/java/lang/Character.cpp
    src/main/vsdk/toolkit/java/lang/Float.cpp
    src/main/vsdk/toolkit/java/lang/StringBuilder.cpp
    src/main/vsdk/toolkit/java/lang/ProcessBuilder.cpp
    src/main/vsdk/toolkit/java/io/InputStream.cpp
    src/main/vsdk/toolkit/java/io/OutputStream.cpp
    src/main/vsdk/toolkit/java/io/File.cpp
    src/main/vsdk/toolkit/java/io/FileInputStream.cpp
    src/main/vsdk/toolkit/java/io/FileOutputStream.cpp
    src/main/vsdk/toolkit/java/io/BufferedInputStream.cpp
    src/main/vsdk/toolkit/java/io/BufferedOutputStream.cpp
    src/main/vsdk/toolkit/java/io/PrintStream.cpp
    src/main/vsdk/toolkit/java/util/Formatter.cpp
    src/main/vsdk/toolkit/java/util/StringTokenizer.cpp
    src/main/vsdk/toolkit/java/util/concurrent/Executors.cpp
    src/main/vsdk/toolkit/gui/KeyEvent.cpp
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorConsole.cpp
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.cpp
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorInRam.cpp
    src/main/vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorProducer.cpp
    src/main/vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorConsumer.cpp
    src/main/vsdk/toolkit/environment/camera/Camera.cpp
    src/main/vsdk/toolkit/environment/light/Light.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleScene.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleSceneSnapshot.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleBodyGroup.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleBody.cpp
    src/main/vsdk/toolkit/common/statistics/RaytraceStatistics.cpp
    src/main/vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.cpp
    src/main/vsdk/toolkit/environment/geometry/elements/Ray.cpp
    src/main/vsdk/toolkit/environment/geometry/elements/RayHit.cpp
    src/main/vsdk/toolkit/environment/geometry/elements/Triangle.cpp
    src/main/vsdk/toolkit/environment/geometry/elements/Vertex.cpp
    src/main/vsdk/toolkit/environment/geometry/elements/Vertex2D.cpp
    src/main/vsdk/toolkit/environment/geometry/curve/Curve.cpp
    src/main/vsdk/toolkit/environment/geometry/curve/ParametricCurve.cpp
    src/main/vsdk/toolkit/environment/geometry/Geometry.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/Surface.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/HalfSpace.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/InfinitePlane.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/Md2Mesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleMesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/QuadMesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/Solid.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/Sphere.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/Box.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/Cone.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/Arrow.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/Torus.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/VoxelVolume.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.cpp
    src/main/vsdk/toolkit/gui/CameraControllerAquynza.cpp
    src/main/vsdk/toolkit/io/PersistenceElement.cpp
    src/main/vsdk/toolkit/io/image/ImagePersistence.cpp
    src/main/vsdk/toolkit/java/io/BufferedOutputStream.cpp
    src/main/vsdk/toolkit/media/MediaEntity.h
    src/main/vsdk/toolkit/media/RGBPixel.cpp
    src/main/vsdk/toolkit/media/RGBAPixel.cpp
    src/main/vsdk/toolkit/media/ShapeDescriptor.cpp
    src/main/vsdk/toolkit/media/RGBColorPalette.cpp
    src/main/vsdk/toolkit/media/RGBProceduralColorPalette.cpp
    src/main/vsdk/toolkit/media/GrayScalePalette.cpp
    src/main/vsdk/toolkit/media/Image.cpp
    src/main/vsdk/toolkit/media/RGBImageUncompressed.cpp
    src/main/vsdk/toolkit/media/RGBAImageUncompressed.cpp
    src/main/vsdk/toolkit/media/IndexedColorImageUncompressed.cpp
    src/main/vsdk/toolkit/media/RGBAImageCompressed.cpp
    src/main/vsdk/toolkit/media/ZBuffer.cpp
    src/main/vsdk/toolkit/media/NormalMap.cpp
    src/main/vsdk/toolkit/media/Calligraphic2DBuffer.cpp
    src/main/vsdk/toolkit/media/PrimitiveCountShapeDescriptor.cpp
    src/main/vsdk/toolkit/media/FourierShapeDescriptor.cpp
    src/main/vsdk/toolkit/media/GeometryMetadata.cpp
    src/main/vsdk/toolkit/environment/material/SimpleMaterial.cpp
    src/main/vsdk/toolkit/environment/material/ShadingType.cpp
    src/main/vsdk/toolkit/environment/material/RendererConfiguration.cpp
)

add_library(vitral_base SHARED ${VITRAL_BASE_SOURCES})

target_include_directories(vitral_base PUBLIC src src/main)

# Link optional libraries and set compile definitions
if(WITH_JPEG)
    target_include_directories(vitral_base PRIVATE ${JPEG_INCLUDE_DIR})
    target_link_libraries(vitral_base PRIVATE ${JPEG_LIBRARIES})
    target_compile_definitions(vitral_base PRIVATE VITRAL_WITH_JPEG)
    message(STATUS "JPEG support enabled: ${JPEG_LIBRARIES}")
endif()

if(WITH_PNG)
    target_include_directories(vitral_base PRIVATE ${PNG_INCLUDE_DIR})
    target_link_libraries(vitral_base PRIVATE ${PNG_LIBRARIES})
    target_compile_definitions(vitral_base PRIVATE VITRAL_WITH_PNG)
    message(STATUS "PNG support enabled: ${PNG_LIBRARIES}")
endif()

if(WITH_POSIX_THREADS)
    target_compile_definitions(vitral_base PRIVATE VITRAL_WITH_POSIX_THREADS)
    target_link_libraries(vitral_base PRIVATE pthread)
endif()

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
