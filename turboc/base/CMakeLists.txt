cmake_minimum_required(VERSION 2.4)
project(vitral_base_turboc CXX)

# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++98 -Wall")

set(LIBRARY_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")

# Optional image format support
option(WITH_JPEG "Enable JPEG image format support" ON)
option(WITH_PNG "Enable PNG image format support" ON)
option(WITH_FREETYPE "Enable TrueType font reading through FreeType 2" OFF)
option(WITH_POSIX_THREADS "Enable POSIX thread based java.util.concurrent emulation" ON)

if(WITH_JPEG)
    find_package(JPEG)
    if(NOT JPEG_FOUND)
        message("JPEG library not found. JPEG support will be disabled.")
        set(WITH_JPEG OFF)
    endif(NOT JPEG_FOUND)
endif(WITH_JPEG)

if(WITH_PNG)
    find_package(PNG)
    if(NOT PNG_FOUND)
        message("PNG library not found. PNG support will be disabled.")
        set(WITH_PNG OFF)
    endif(NOT PNG_FOUND)
endif(WITH_PNG)

if(WITH_FREETYPE)
    find_path(FREETYPE_INCLUDE_DIR ft2build.h /usr/include/freetype2 /usr/local/include/freetype2)
    find_library(FREETYPE_LIBRARY freetype)
    if(FREETYPE_INCLUDE_DIR AND FREETYPE_LIBRARY)
        set(FREETYPE_FOUND ON)
    else(FREETYPE_INCLUDE_DIR AND FREETYPE_LIBRARY)
        message("FreeType 2 library not found. Font reading will be disabled.")
        set(WITH_FREETYPE OFF)
    endif(FREETYPE_INCLUDE_DIR AND FREETYPE_LIBRARY)
endif(WITH_FREETYPE)

message(STATUS "Vitral C++98 Base Configuration:")
message(STATUS "  JPEG Support: ${WITH_JPEG}")
message(STATUS "  PNG Support:  ${WITH_PNG}")
message(STATUS "  FreeType Support: ${WITH_FREETYPE}")
message(STATUS "  POSIX Threads: ${WITH_POSIX_THREADS}")

set(VITRAL_BASE_SOURCES
    src/main/vsdk/toolkit/common/VSDK.cpp
    src/main/vsdk/toolkit/common/Cpp98Support.cpp
    src/main/vsdk/toolkit/common/Entity.cpp
    src/main/vsdk/toolkit/common/EntityControlAccessor.cpp
    src/main/vsdk/toolkit/gui/editor/ControlSpecification.cpp
    src/main/vsdk/toolkit/gui/editor/GenericEditor.cpp
    src/main/vsdk/toolkit/common/logging/Logger.cpp
    src/main/vsdk/toolkit/common/statistics/RaytraceStatistics.cpp
    src/main/vsdk/toolkit/common/statistics/SolidTextureStatistics.cpp
    src/main/vsdk/toolkit/common/statistics/GeometryStatistics.cpp
    src/main/vsdk/toolkit/common/color/ColorRgb.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Complex.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/MatrixNxM.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4d.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4f.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/AlgebraPrimitives.cpp
    src/main/vsdk/toolkit/common/linealAlgebra/processing/LinearAlgebraEngine.cpp
    src/main/vsdk/toolkit/processing/ComputationalGeometry.cpp
    src/main/vsdk/toolkit/numericalAnalysis/lookUpTables/LookUpTableChecksum16.cpp
    src/main/vsdk/toolkit/numericalAnalysis/lookUpTables/LookUpTableSine.cpp
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/PolynomialSolver.cpp
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/CubicSolver.cpp
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/QuadraticSolver.cpp
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/QuarticSolver.cpp
    src/main/jackson/databind/JsonNode.cpp
    src/main/jackson/databind/ObjectMapper.cpp
    src/main/java/lang/Object.cpp
    src/main/java/lang/Math.cpp
    src/main/java/lang/IntegralConstants.cpp
    src/main/java/lang/String.cpp
    src/main/java/lang/System.cpp
    src/main/java/lang/Character.cpp
    src/main/java/lang/Float.cpp
    src/main/java/lang/Double.cpp
    src/main/java/lang/StringBuilder.cpp
    src/main/java/lang/ProcessBuilder.cpp
    src/main/java/lang/Thread.cpp
    src/main/java/io/InputStream.cpp
    src/main/java/io/File.cpp
    src/main/java/io/FileInputStream.cpp
    src/main/java/io/FileOutputStream.cpp
    src/main/java/io/BufferedInputStream.cpp
    src/main/java/io/BufferedOutputStream.cpp
    src/main/java/io/PrintStream.cpp
    src/main/java/io/BufferedReader.cpp
    src/main/java/io/PrintWriter.cpp
    src/main/java/io/Reader.cpp
    src/main/java/io/InputStreamReader.cpp
    src/main/java/io/FileReader.cpp
    src/main/java/io/StreamTokenizer.cpp
    src/main/java/nio/file/Files.cpp
    src/main/java/net/ServerSocket.cpp
    src/main/java/net/SocketInputStream.cpp
    src/main/java/net/SocketOutputStream.cpp
    src/main/java/net/Socket.cpp
    src/main/java/net/WebSocket.cpp
    src/main/java/util/Formatter.cpp
    src/main/java/util/StringTokenizer.cpp
    src/main/java/util/regex/Pattern.cpp
    src/main/java/util/concurrent/Executors.cpp
    src/main/vsdk/toolkit/gui/KeyEvent.cpp
    src/main/vsdk/toolkit/gui/MouseEvent.cpp
    src/main/vsdk/toolkit/gui/framebuffer/Framebuffer.cpp
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferTheme.cpp
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferPainter.cpp
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferSnapshot.cpp
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferMenuBar.cpp
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferMenuReader.cpp
    src/main/vsdk/toolkit/gui/gizmo/LightGizmoOmniBillboard.cpp
    src/main/vsdk/toolkit/gui/gizmo/InfinitePlaneGizmo.cpp
    src/main/vsdk/toolkit/gui/gizmo/RayGizmo.cpp
    src/main/vsdk/toolkit/gui/gizmo/RayGizmoInteractionTechniques.cpp
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.cpp
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2InfinitePlaneGizmoMapper.cpp
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2RayGizmoMapper.cpp
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.cpp
    src/main/vsdk/toolkit/gui/RendererConfigurationController.cpp
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorConsole.cpp
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.cpp
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorInRam.cpp
    src/main/vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorProducer.cpp
    src/main/vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorConsumer.cpp
    src/main/vsdk/toolkit/environment/camera/Camera.cpp
    src/main/vsdk/toolkit/environment/background/SimpleBackground.cpp
    src/main/vsdk/toolkit/environment/light/Light.cpp
    src/main/vsdk/toolkit/environment/light/AmbientLight.cpp
    src/main/vsdk/toolkit/environment/light/DirectionalLight.cpp
    src/main/vsdk/toolkit/environment/light/PointLight.cpp
    src/main/vsdk/toolkit/environment/light/SpotLight.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleScene.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleSceneSnapshot.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleBodyGroup.cpp
    src/main/vsdk/toolkit/environment/scene/SimpleBody.cpp
    src/main/vsdk/toolkit/processing/StopWatch.cpp
    src/main/vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.cpp
    src/main/vsdk/toolkit/environment/geometry/element/Ray.cpp
    src/main/vsdk/toolkit/environment/geometry/element/RayHit.cpp
    src/main/vsdk/toolkit/environment/geometry/element/Triangle.cpp
    src/main/vsdk/toolkit/environment/geometry/element/Vertex.cpp
    src/main/vsdk/toolkit/environment/geometry/element/Vertex2D.cpp
    src/main/vsdk/toolkit/environment/geometry/curve/Curve.cpp
    src/main/vsdk/toolkit/environment/geometry/curve/ParametricCurve.cpp
    src/main/vsdk/toolkit/environment/geometry/Geometry.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/Surface.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/HalfSpace.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/InfinitePlane.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/Md2Mesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/_ParametricBiCubicPatchIntersector.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleMesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/QuadMesh.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/Voxelization.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/TriangleMeshVoxelization.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/TriangleMeshGroupVoxelization.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/FunctionalExplicitSurfaceVoxelization.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/SurfaceRayIntersection.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_ContourAwarePolygonTriangulator.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_Construct.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_IncrementalSegmentInserter.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_InsertionBatchSchedule.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_Monotone.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_RandomSegmentOrder.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_SegmentTableBuilder.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationTrapezoid.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationTrapezoidQueryNode.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_Polygon2DWA.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonProcessor.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonTopologicalMerger.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/WeilerAthertonPolygonClipper.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.cpp
    src/main/vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/Volume.cpp
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
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidTopologicalValidator.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidBooleanTopologyPredicates.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidValidationStrategies.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.cpp
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidNodesToString.cpp
    src/main/vsdk/toolkit/gui/CameraControllerAquynza.cpp
    src/main/vsdk/toolkit/gui/CameraControllerOrbiter.cpp
    src/main/vsdk/toolkit/gui/CameraControllerBlender.cpp
    src/main/vsdk/toolkit/gui/CameraControllerGoogleEarth.cpp
    src/main/vsdk/toolkit/render/TraceWorkspace.cpp
    src/main/vsdk/toolkit/render/SpherePolyhedralCache.cpp
    src/main/vsdk/toolkit/render/raytracing/DepthBufferEncoder.cpp
    src/main/vsdk/toolkit/render/raytracing/ParallelRaytracer.cpp
    src/main/vsdk/toolkit/render/raytracing/RasterTileArea.cpp
    src/main/vsdk/toolkit/render/raytracing/RasterTileGenerator.cpp
    src/main/vsdk/toolkit/render/raytracing/SimpleRaytracer.cpp
    src/main/vsdk/toolkit/render/AutoStereogramGenerator.cpp
    src/main/vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.cpp
    src/main/vsdk/toolkit/render/raster/Rasterizer2D.cpp
    src/main/vsdk/toolkit/render/hiddenLine/WireframeRenderer.cpp
    src/main/vsdk/toolkit/render/PolyhedralBoundedSolidDebugger.cpp
    src/main/vsdk/toolkit/render/shaders/CpuTextureSamplingConfig.cpp
    src/main/vsdk/toolkit/render/shaders/LightingShader.cpp
    src/main/vsdk/toolkit/render/shaders/ConstantShader.cpp
    src/main/vsdk/toolkit/render/shaders/ConstantTextureShader.cpp
    src/main/vsdk/toolkit/render/shaders/CookTorranceShader.cpp
    src/main/vsdk/toolkit/render/shaders/ShaderSelector.cpp
    src/main/vsdk/toolkit/io/PersistenceElement.cpp
    src/main/vsdk/toolkit/io/FileLocator.cpp
    src/main/vsdk/toolkit/io/gui/DialogPersistence.cpp
    src/main/vsdk/toolkit/io/geometry/EnvironmentPersistence.cpp
    src/main/vsdk/toolkit/io/geometry/WriterObj.cpp
    src/main/vsdk/toolkit/io/geometry/WriterGts.cpp
    src/main/vsdk/toolkit/io/geometry/WriterVtk.cpp
    src/main/vsdk/toolkit/io/geometry/ReaderObj.cpp
    src/main/vsdk/toolkit/io/geometry/ReaderPly.cpp
    src/main/vsdk/toolkit/io/geometry/ReaderPlyResult.cpp
    src/main/vsdk/toolkit/io/geometry/ReaderMitScene.cpp
    src/main/vsdk/toolkit/io/image/ImagePersistence.cpp
    src/main/vsdk/toolkit/media/solidTexture/TextureUtils.cpp
    src/main/vsdk/toolkit/media/solidTexture/from2d/ControlledRGBAImageHDRUncompressed.cpp
    src/main/vsdk/toolkit/media/solidTexture/from2d/ImageTexture.cpp
    src/main/vsdk/toolkit/media/solidTexture/from2d/SolidTextureCoordinateMapper.cpp
    src/main/vsdk/toolkit/media/solidTexture/procedural/BumpTextureFixture.cpp
    src/main/vsdk/toolkit/media/solidTexture/procedural/ColorTextureFixture.cpp
    src/main/vsdk/toolkit/media/solidTexture/procedural/ProceduralNoise.cpp
    src/main/vsdk/toolkit/media/RGBPixel.cpp
    src/main/vsdk/toolkit/media/RGBAPixel.cpp
    src/main/vsdk/toolkit/media/ShapeDescriptor.cpp
    src/main/vsdk/toolkit/media/RGBColorPalette.cpp
    src/main/vsdk/toolkit/media/RGBAColorPalette.cpp
    src/main/vsdk/toolkit/media/RGBProceduralColorPalette.cpp
    src/main/vsdk/toolkit/media/GrayScalePalette.cpp
    src/main/vsdk/toolkit/media/Image.cpp
    src/main/vsdk/toolkit/media/RGBImageUncompressed.cpp
    src/main/vsdk/toolkit/media/RGBAImageUncompressed.cpp
    src/main/vsdk/toolkit/media/RGBAImageHDRUncompressed.cpp
    src/main/vsdk/toolkit/media/IndexedColorImageUncompressed.cpp
    src/main/vsdk/toolkit/media/IndexedColorImageHDRUncompressed.cpp
    src/main/vsdk/toolkit/media/RGBAImageCompressed.cpp
    src/main/vsdk/toolkit/media/ZBuffer.cpp
    src/main/vsdk/toolkit/media/NormalMap.cpp
    src/main/vsdk/toolkit/media/Calligraphic2DBuffer.cpp
    src/main/vsdk/toolkit/media/PrimitiveCountShapeDescriptor.cpp
    src/main/vsdk/toolkit/media/FourierShapeDescriptor.cpp
    src/main/vsdk/toolkit/media/GeometryMetadata.cpp
    src/main/vsdk/toolkit/environment/material/SimpleMaterial.cpp
    src/main/vsdk/toolkit/environment/material/MicroFacetedMaterial.cpp
    src/main/vsdk/toolkit/environment/material/ShadingType.cpp
    src/main/vsdk/toolkit/environment/material/RendererConfiguration.cpp
    src/main/vsdk/toolkit/processing/CurveModeler.cpp
    src/main/vsdk/toolkit/processing/ImageProcessing.cpp
    src/main/vsdk/toolkit/environment/background/CubemapBackground.cpp
    src/main/vsdk/toolkit/environment/background/FixedBackground.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidIdNamespace.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidFallbackGeometry.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidAxisAlignedCellFallback.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidOffsetCylinderFallback.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/construction/_PolyhedralBoundedSolidProfileDifferenceFallback.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/construction/_PolyhedralBoundedSolidOrthogonalProfileFallback.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/slicing/_PolyhedralBoundedSolidSplitter.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_SetOperationTrace.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetOperatorNullEdge.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersector.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetOperatorSectorClassification.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetVertexFaceClassifier.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetVertexVertexClassifier.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetClassifier.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.cpp
    src/main/vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.cpp
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/validation/_PolyhedralBoundedSolidFaceValidator.cpp
    src/main/vsdk/toolkit/io/geometry/ParametricCurvePersistence.cpp
    src/main/vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.cpp
    src/main/vsdk/toolkit/io/image/RGBColorPalettePersistence.cpp
    src/main/org/w3c/dom/Node.cpp
    src/main/javax/xml/parsers/DocumentBuilder.cpp
    src/main/javax/xml/transform/Transformer.cpp
    src/main/vsdk/toolkit/gui/widget/Widget.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetButtonGroup.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetCommand.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetDialog.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetDialogElement.cpp
    src/main/vsdk/toolkit/gui/dialog/components/DialogFileFilter.cpp
    src/main/vsdk/toolkit/gui/dialog/components/DialogTreeNode.cpp
    src/main/vsdk/toolkit/gui/dialog/runtime/DialogSession.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetMenu.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetMenuElement.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetMenuItem.cpp
    src/main/vsdk/toolkit/gui/viewport/Viewport.cpp
    src/main/vsdk/toolkit/gui/viewport/ViewportElementScaler.cpp
    src/main/vsdk/toolkit/gui/viewport/ViewportSet.cpp
    src/main/vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.cpp
    src/main/vsdk/toolkit/gui/viewport/ViewportInteractionTechniques.cpp
    src/main/vsdk/toolkit/gui/gizmo/GizmoSolidTessellator.cpp
    src/main/vsdk/toolkit/gui/gizmo/GizmoVertexArrayBuilder.cpp
    src/main/vsdk/toolkit/gui/gizmo/InputGizmo.cpp
    src/main/vsdk/toolkit/gui/gizmo/InputGizmoValueChangeRules.cpp
    src/main/vsdk/toolkit/gui/gizmo/ReferenceFrameGizmo.cpp
    src/main/vsdk/toolkit/gui/gizmo/TranslateGizmo.cpp
    src/main/vsdk/toolkit/gui/gizmo/TranslateGizmoInteractionTechnique.cpp
    src/main/vsdk/toolkit/gui/gizmo/RotateGizmo.cpp
    src/main/vsdk/toolkit/gui/gizmo/RotateGizmoInteractionTechnique.cpp
    src/main/vsdk/toolkit/gui/gizmo/ScaleGizmo.cpp
    src/main/vsdk/toolkit/gui/gizmo/ScaleGizmoInteractionTechnique.cpp
    src/main/vsdk/toolkit/gui/PresentationElement.cpp
    src/main/vsdk/toolkit/environment/background/Background.cpp
    src/main/vsdk/toolkit/media/MediaEntity.cpp
    src/main/vsdk/toolkit/gui/CameraController.cpp
    src/main/vsdk/toolkit/gui/CommandListener.cpp
    src/main/vsdk/toolkit/gui/Controller.cpp
    src/main/vsdk/toolkit/gui/gizmo/Gizmo.cpp
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitor.cpp
    src/main/vsdk/toolkit/gui/widget/WidgetElement.cpp
    src/main/vsdk/toolkit/gui/widget/variable/WidgetVariable.cpp
)

if(WITH_FREETYPE)
    set(VITRAL_BASE_SOURCES ${VITRAL_BASE_SOURCES}
        src/main/vsdk/toolkit/io/geometry/Freetype2FontReader.cpp)
endif(WITH_FREETYPE)

include_directories(src src/main)

if(WITH_JPEG)
    include_directories(${JPEG_INCLUDE_DIR})
    add_definitions(-DVITRAL_WITH_JPEG)
endif(WITH_JPEG)

if(WITH_PNG)
    include_directories(${PNG_INCLUDE_DIR})
    add_definitions(-DVITRAL_WITH_PNG)
endif(WITH_PNG)

if(WITH_FREETYPE)
    include_directories(${FREETYPE_INCLUDE_DIR})
    add_definitions(-DVITRAL_WITH_FREETYPE)
endif(WITH_FREETYPE)

if(WITH_POSIX_THREADS)
    add_definitions(-DVITRAL_WITH_POSIX_THREADS)
endif(WITH_POSIX_THREADS)

add_library(vitral_base SHARED ${VITRAL_BASE_SOURCES})

target_link_libraries(vitral_base pthread)

if(WITH_JPEG)
    target_link_libraries(vitral_base ${JPEG_LIBRARIES})
endif(WITH_JPEG)

if(WITH_PNG)
    target_link_libraries(vitral_base ${PNG_LIBRARIES})
endif(WITH_PNG)

if(WITH_FREETYPE)
    target_link_libraries(vitral_base ${FREETYPE_LIBRARY})
endif(WITH_FREETYPE)
