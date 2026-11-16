# Makefile of the vitral base library for legacy toolchains without CMake
# (e.g. GNU make 3.75 and gcc 2.7.2 on Debian 1.1). It mirrors the
# VITRAL_BASE_SOURCES list of CMakeLists.txt: keep both in sync.
#
# The library is built as the static archives build/libvitral_base.a and
# build/libvitral_base_geometry.a (see below), with the objects in buildmk/. The options are the ones of ../legacy.mk, i.e.
#   make WITH_JPEG=1 WITH_PNG=1 WITH_FREETYPE=1 WITH_POSIX_THREADS=1

VITRAL_DIR = ..
include $(VITRAL_DIR)/legacy.mk

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    -Isrc -Isrc/main $(DEFINES)

SOURCES = \
    src/main/vsdk/toolkit/common/VSDK.cpp \
    src/main/vsdk/toolkit/common/Cpp98Support.cpp \
    src/main/vsdk/toolkit/common/Entity.cpp \
    src/main/vsdk/toolkit/common/EntityControlAccessor.cpp \
    src/main/vsdk/toolkit/gui/editor/ControlSpecification.cpp \
    src/main/vsdk/toolkit/gui/editor/GenericEditor.cpp \
    src/main/vsdk/toolkit/common/logging/Logger.cpp \
    src/main/vsdk/toolkit/common/statistics/RaytraceStatistics.cpp \
    src/main/vsdk/toolkit/common/statistics/SolidTextureStatistics.cpp \
    src/main/vsdk/toolkit/common/statistics/GeometryStatistics.cpp \
    src/main/vsdk/toolkit/common/color/ColorRgb.cpp \
    src/main/vsdk/toolkit/common/linealAlgebra/Complex.cpp \
    src/main/vsdk/toolkit/common/linealAlgebra/MatrixNxM.cpp \
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4d.cpp \
    src/main/vsdk/toolkit/common/linealAlgebra/Matrix4x4f.cpp \
    src/main/vsdk/toolkit/common/linealAlgebra/AlgebraPrimitives.cpp \
    src/main/vsdk/toolkit/common/linealAlgebra/processing/LinearAlgebraEngine.cpp \
    src/main/vsdk/toolkit/processing/ComputationalGeometry.cpp \
    src/main/vsdk/toolkit/numericalAnalysis/lookUpTables/LookUpTableChecksum16.cpp \
    src/main/vsdk/toolkit/numericalAnalysis/lookUpTables/LookUpTableSine.cpp \
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/PolynomialSolver.cpp \
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/CubicSolver.cpp \
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/QuadraticSolver.cpp \
    src/main/vsdk/toolkit/numericalAnalysis/polynomial/QuarticSolver.cpp \
    src/main/jackson/databind/JsonNode.cpp \
    src/main/jackson/databind/ObjectMapper.cpp \
    src/main/java/lang/Object.cpp \
    src/main/java/lang/Math.cpp \
    src/main/java/lang/IntegralConstants.cpp \
    src/main/java/lang/String.cpp \
    src/main/java/lang/System.cpp \
    src/main/java/lang/Character.cpp \
    src/main/java/lang/Float.cpp \
    src/main/java/lang/Double.cpp \
    src/main/java/lang/StringBuilder.cpp \
    src/main/java/lang/ProcessBuilder.cpp \
    src/main/java/lang/Thread.cpp \
    src/main/java/io/InputStream.cpp \
    src/main/java/io/File.cpp \
    src/main/java/io/FileInputStream.cpp \
    src/main/java/io/FileOutputStream.cpp \
    src/main/java/io/BufferedInputStream.cpp \
    src/main/java/io/BufferedOutputStream.cpp \
    src/main/java/io/PrintStream.cpp \
    src/main/java/io/BufferedReader.cpp \
    src/main/java/io/PrintWriter.cpp \
    src/main/java/io/Reader.cpp \
    src/main/java/io/InputStreamReader.cpp \
    src/main/java/io/FileReader.cpp \
    src/main/java/io/StreamTokenizer.cpp \
    src/main/java/nio/file/Files.cpp \
    src/main/java/net/ServerSocket.cpp \
    src/main/java/net/SocketInputStream.cpp \
    src/main/java/net/SocketOutputStream.cpp \
    src/main/java/net/Socket.cpp \
    src/main/java/net/WebSocket.cpp \
    src/main/java/util/Formatter.cpp \
    src/main/java/util/StringTokenizer.cpp \
    src/main/java/util/regex/Pattern.cpp \
    src/main/java/util/concurrent/Executors.cpp \
    src/main/vsdk/toolkit/gui/KeyEvent.cpp \
    src/main/vsdk/toolkit/gui/MouseEvent.cpp \
    src/main/vsdk/toolkit/gui/framebuffer/Framebuffer.cpp \
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferTheme.cpp \
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferPainter.cpp \
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferSnapshot.cpp \
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferMenuBar.cpp \
    src/main/vsdk/toolkit/gui/framebuffer/FramebufferMenuReader.cpp \
    src/main/vsdk/toolkit/gui/gizmo/LightGizmoOmniBillboard.cpp \
    src/main/vsdk/toolkit/gui/gizmo/InfinitePlaneGizmo.cpp \
    src/main/vsdk/toolkit/gui/gizmo/RayGizmo.cpp \
    src/main/vsdk/toolkit/gui/gizmo/RayGizmoInteractionTechniques.cpp \
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.cpp \
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2InfinitePlaneGizmoMapper.cpp \
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2RayGizmoMapper.cpp \
    src/main/vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.cpp \
    src/main/vsdk/toolkit/gui/RendererConfigurationController.cpp \
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorConsole.cpp \
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.cpp \
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitorInRam.cpp \
    src/main/vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorProducer.cpp \
    src/main/vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorConsumer.cpp \
    src/main/vsdk/toolkit/environment/camera/Camera.cpp \
    src/main/vsdk/toolkit/environment/background/SimpleBackground.cpp \
    src/main/vsdk/toolkit/environment/light/Light.cpp \
    src/main/vsdk/toolkit/environment/light/AmbientLight.cpp \
    src/main/vsdk/toolkit/environment/light/DirectionalLight.cpp \
    src/main/vsdk/toolkit/environment/light/PointLight.cpp \
    src/main/vsdk/toolkit/environment/light/SpotLight.cpp \
    src/main/vsdk/toolkit/environment/scene/SimpleScene.cpp \
    src/main/vsdk/toolkit/environment/scene/SimpleSceneSnapshot.cpp \
    src/main/vsdk/toolkit/environment/scene/SimpleBodyGroup.cpp \
    src/main/vsdk/toolkit/environment/scene/SimpleBody.cpp \
    src/main/vsdk/toolkit/processing/StopWatch.cpp \
    src/main/vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/Ray.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RayHit.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/Triangle.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/Vertex.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/Vertex2D.cpp \
    src/main/vsdk/toolkit/environment/geometry/curve/Curve.cpp \
    src/main/vsdk/toolkit/environment/geometry/curve/ParametricCurve.cpp \
    src/main/vsdk/toolkit/environment/geometry/Geometry.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/Surface.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/HalfSpace.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/InfinitePlane.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/Md2Mesh.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/_ParametricBiCubicPatchIntersector.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleMesh.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/QuadMesh.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/Voxelization.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/TriangleMeshVoxelization.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/TriangleMeshGroupVoxelization.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/FunctionalExplicitSurfaceVoxelization.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/SurfaceRayIntersection.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_ContourAwarePolygonTriangulator.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_Construct.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_IncrementalSegmentInserter.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_InsertionBatchSchedule.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_Monotone.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_RandomSegmentOrder.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_SegmentTableBuilder.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationTrapezoid.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationTrapezoidQueryNode.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/_Polygon2DWA.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonTopologicalMerger.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/WeilerAthertonPolygonClipper.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/Volume.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/Solid.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/Sphere.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/Box.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/Cone.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/Arrow.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/Torus.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/VoxelVolume.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidRepair.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidTopologicalValidator.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidBooleanTopologyPredicates.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidValidationStrategies.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidNodesToString.cpp \
    src/main/vsdk/toolkit/gui/CameraControllerAquynza.cpp \
    src/main/vsdk/toolkit/gui/CameraControllerOrbiter.cpp \
    src/main/vsdk/toolkit/gui/CameraControllerBlender.cpp \
    src/main/vsdk/toolkit/gui/CameraControllerGoogleEarth.cpp \
    src/main/vsdk/toolkit/render/TraceWorkspace.cpp \
    src/main/vsdk/toolkit/render/SpherePolyhedralCache.cpp \
    src/main/vsdk/toolkit/render/framebuffer/FramebufferGizmo2DDrawListRenderer.cpp \
    src/main/vsdk/toolkit/render/gizmo/Gizmo2DDrawList.cpp \
    src/main/vsdk/toolkit/render/gizmo/Gizmo2DProjector.cpp \
    src/main/vsdk/toolkit/render/gizmo/InputGizmo2DPainter.cpp \
    src/main/vsdk/toolkit/render/gizmo/RotateGizmo2DPainter.cpp \
    src/main/vsdk/toolkit/render/gizmo/ScaleGizmo2DPainter.cpp \
    src/main/vsdk/toolkit/render/gizmo/TranslateGizmo2DPainter.cpp \
    src/main/vsdk/toolkit/render/raytracing/DepthBufferEncoder.cpp \
    src/main/vsdk/toolkit/render/raytracing/ParallelRaytracer.cpp \
    src/main/vsdk/toolkit/render/raytracing/RasterTileArea.cpp \
    src/main/vsdk/toolkit/render/raytracing/RasterTileGenerator.cpp \
    src/main/vsdk/toolkit/render/raytracing/SimpleRaytracer.cpp \
    src/main/vsdk/toolkit/render/AutoStereogramGenerator.cpp \
    src/main/vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.cpp \
    src/main/vsdk/toolkit/render/dithering/Dithering.cpp \
    src/main/vsdk/toolkit/render/raster/Rasterizer2D.cpp \
    src/main/vsdk/toolkit/render/hiddenLine/WireframeRenderer.cpp \
    src/main/vsdk/toolkit/render/PolyhedralBoundedSolidDebugger.cpp \
    src/main/vsdk/toolkit/render/shaders/CpuTextureSamplingConfig.cpp \
    src/main/vsdk/toolkit/render/shaders/LightingShader.cpp \
    src/main/vsdk/toolkit/render/shaders/ConstantShader.cpp \
    src/main/vsdk/toolkit/render/shaders/ConstantTextureShader.cpp \
    src/main/vsdk/toolkit/render/shaders/CookTorranceShader.cpp \
    src/main/vsdk/toolkit/render/shaders/ShaderSelector.cpp \
    src/main/vsdk/toolkit/io/PersistenceElement.cpp \
    src/main/vsdk/toolkit/io/FileLocator.cpp \
    src/main/vsdk/toolkit/io/gui/DialogPersistence.cpp \
    src/main/vsdk/toolkit/io/geometry/EnvironmentPersistence.cpp \
    src/main/vsdk/toolkit/io/geometry/WriterObj.cpp \
    src/main/vsdk/toolkit/io/geometry/WriterGts.cpp \
    src/main/vsdk/toolkit/io/geometry/WriterVtk.cpp \
    src/main/vsdk/toolkit/io/geometry/ReaderObj.cpp \
    src/main/vsdk/toolkit/io/geometry/ReaderPly.cpp \
    src/main/vsdk/toolkit/io/geometry/ReaderPlyResult.cpp \
    src/main/vsdk/toolkit/io/geometry/ReaderMitScene.cpp \
    src/main/vsdk/toolkit/io/image/ImagePersistence.cpp \
    src/main/vsdk/toolkit/media/solidTexture/TextureUtils.cpp \
    src/main/vsdk/toolkit/media/solidTexture/from2d/ControlledRGBAImageHDRUncompressed.cpp \
    src/main/vsdk/toolkit/media/solidTexture/from2d/ImageTexture.cpp \
    src/main/vsdk/toolkit/media/solidTexture/from2d/SolidTextureCoordinateMapper.cpp \
    src/main/vsdk/toolkit/media/solidTexture/procedural/BumpTextureFixture.cpp \
    src/main/vsdk/toolkit/media/solidTexture/procedural/ColorTextureFixture.cpp \
    src/main/vsdk/toolkit/media/solidTexture/procedural/ProceduralNoise.cpp \
    src/main/vsdk/toolkit/media/RGBPixel.cpp \
    src/main/vsdk/toolkit/media/RGBAPixel.cpp \
    src/main/vsdk/toolkit/media/ShapeDescriptor.cpp \
    src/main/vsdk/toolkit/media/RGBColorPalette.cpp \
    src/main/vsdk/toolkit/media/RGBAColorPalette.cpp \
    src/main/vsdk/toolkit/media/RGBProceduralColorPalette.cpp \
    src/main/vsdk/toolkit/media/GrayScalePalette.cpp \
    src/main/vsdk/toolkit/media/Image.cpp \
    src/main/vsdk/toolkit/media/RGBImageUncompressed.cpp \
    src/main/vsdk/toolkit/media/RGBAImageUncompressed.cpp \
    src/main/vsdk/toolkit/media/RGBAImageHDRUncompressed.cpp \
    src/main/vsdk/toolkit/media/IndexedColorImageUncompressed.cpp \
    src/main/vsdk/toolkit/media/IndexedColorImageHDRUncompressed.cpp \
    src/main/vsdk/toolkit/media/RGBAImageCompressed.cpp \
    src/main/vsdk/toolkit/media/ZBuffer.cpp \
    src/main/vsdk/toolkit/media/NormalMap.cpp \
    src/main/vsdk/toolkit/media/Calligraphic2DBuffer.cpp \
    src/main/vsdk/toolkit/media/PrimitiveCountShapeDescriptor.cpp \
    src/main/vsdk/toolkit/media/FourierShapeDescriptor.cpp \
    src/main/vsdk/toolkit/media/GeometryMetadata.cpp \
    src/main/vsdk/toolkit/environment/material/SimpleMaterial.cpp \
    src/main/vsdk/toolkit/environment/material/MicroFacetedMaterial.cpp \
    src/main/vsdk/toolkit/environment/material/ShadingType.cpp \
    src/main/vsdk/toolkit/environment/material/RendererConfiguration.cpp \
    src/main/vsdk/toolkit/processing/CurveModeler.cpp \
    src/main/vsdk/toolkit/processing/ImageProcessing.cpp \
    src/main/vsdk/toolkit/environment/background/CubemapBackground.cpp \
    src/main/vsdk/toolkit/environment/background/FixedBackground.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidIdNamespace.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidVertexNeighborhood.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperandAligner.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidInteriorOverlap.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/slicing/_PolyhedralBoundedSolidSplitter.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_SetOperationTrace.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetOperatorNullEdge.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersector.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetOperatorSectorClassification.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetVertexFaceClassifier.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetVertexVertexClassifier.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetClassifier.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.cpp \
    src/main/vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.cpp \
    src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/validation/_PolyhedralBoundedSolidFaceValidator.cpp \
    src/main/vsdk/toolkit/io/geometry/ParametricCurvePersistence.cpp \
    src/main/vsdk/toolkit/io/geometry/ParametricBiCubicPatchPersistence.cpp \
    src/main/vsdk/toolkit/io/image/RGBColorPalettePersistence.cpp \
    src/main/org/w3c/dom/Node.cpp \
    src/main/javax/xml/parsers/DocumentBuilder.cpp \
    src/main/javax/xml/transform/Transformer.cpp \
    src/main/vsdk/toolkit/gui/widget/Widget.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetButtonGroup.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetCommand.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetDialog.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetDialogElement.cpp \
    src/main/vsdk/toolkit/gui/dialog/components/DialogFileFilter.cpp \
    src/main/vsdk/toolkit/gui/dialog/components/DialogTreeNode.cpp \
    src/main/vsdk/toolkit/gui/dialog/runtime/DialogSession.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetMenu.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetMenuElement.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetMenuItem.cpp \
    src/main/vsdk/toolkit/gui/viewport/Viewport.cpp \
    src/main/vsdk/toolkit/gui/viewport/ViewportElementScaler.cpp \
    src/main/vsdk/toolkit/gui/viewport/ViewportSet.cpp \
    src/main/vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.cpp \
    src/main/vsdk/toolkit/gui/viewport/ViewportInteractionTechniques.cpp \
    src/main/vsdk/toolkit/gui/gizmo/GizmoSolidTessellator.cpp \
    src/main/vsdk/toolkit/gui/gizmo/GizmoVertexArrayBuilder.cpp \
    src/main/vsdk/toolkit/gui/gizmo/InputGizmo.cpp \
    src/main/vsdk/toolkit/gui/gizmo/InputGizmoValueChangeRules.cpp \
    src/main/vsdk/toolkit/gui/gizmo/ReferenceFrameGizmo.cpp \
    src/main/vsdk/toolkit/gui/gizmo/TranslateGizmo.cpp \
    src/main/vsdk/toolkit/gui/gizmo/TranslateGizmoInteractionTechnique.cpp \
    src/main/vsdk/toolkit/gui/gizmo/RotateGizmo.cpp \
    src/main/vsdk/toolkit/gui/gizmo/RotateGizmoInteractionTechnique.cpp \
    src/main/vsdk/toolkit/gui/gizmo/ScaleGizmo.cpp \
    src/main/vsdk/toolkit/gui/gizmo/ScaleGizmoInteractionTechnique.cpp \
    src/main/vsdk/toolkit/gui/PresentationElement.cpp \
    src/main/vsdk/toolkit/environment/background/Background.cpp \
    src/main/vsdk/toolkit/media/MediaEntity.cpp \
    src/main/vsdk/toolkit/gui/CameraController.cpp \
    src/main/vsdk/toolkit/gui/CommandListener.cpp \
    src/main/vsdk/toolkit/gui/Controller.cpp \
    src/main/vsdk/toolkit/gui/gizmo/Gizmo.cpp \
    src/main/vsdk/toolkit/gui/feedback/ProgressMonitor.cpp \
    src/main/vsdk/toolkit/gui/widget/WidgetElement.cpp \
    src/main/vsdk/toolkit/gui/widget/variable/WidgetVariable.cpp \
    src/main/vsdk/toolkit/common/numerics/FloatingInterval.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RayCrossing.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RayCrossingQuery.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RayCrossingSink.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RayBoundary.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RaySolidBoundary.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RaySolidClassification.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RayInstancePath.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/RayInstanceStep.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/GeometryBounds.cpp \
    src/main/vsdk/toolkit/environment/geometry/element/GeometryQueryWorkspace.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/TriangleMeshIntersectionIndex.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDistance.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDistanceBounds.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDualBound.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/AffineEllipsoidMinimumDistance.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/OrthogonalEllipsoidDistance.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/QuadraticInteriorBound.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerConstructions.cpp \
    src/main/vsdk/toolkit/environment/scene/AffinePlacement.cpp \
    src/main/vsdk/toolkit/environment/scene/AffineToleranceMap.cpp \
    src/main/vsdk/toolkit/environment/scene/InstanceRayCrossingBuffer.cpp \
    src/main/vsdk/toolkit/environment/scene/SceneSolid.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/AabbBinHierarchy.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/TriangleAccelerationStructure.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahHierarchy.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahTriangleHierarchy.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahInstanceHierarchy.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/AabbBoundingVolume.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/AxisAlignedBoundingBox.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BoundingVolumeHierarchy.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/PreRayHitElement.cpp \
    src/main/vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/RaySlabReciprocals.cpp \
    src/main/vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/BlobComponent.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/BlobSolid.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/ImplicitSolid.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/PolynomialSurface.cpp \
    src/main/vsdk/toolkit/environment/geometry/volume/QuadricSurface.cpp \
    src/main/vsdk/toolkit/io/geometry/ReaderAltimetry.cpp \
    src/main/vsdk/toolkit/io/geometry/ReaderMitQuadGroup.cpp \
    src/main/vsdk/toolkit/io/geometry/ReaderMitTriangleGroup.cpp \
    src/main/vsdk/toolkit/processing/BezierPatchTessellator.cpp \
    src/main/vsdk/toolkit/processing/SolidBoundaryTessellationReport.cpp \
    src/main/vsdk/toolkit/processing/SolidBoundaryTessellator.cpp \
    src/main/vsdk/toolkit/render/raytracing/SnellRefraction.cpp \
    src/main/vsdk/toolkit/render/shaders/ShadowTransmittance.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgBody.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgBodyOperand.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgCrossingMembershipFilter.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgDyadic.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgDyadicSolid.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgDyadicTransform.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgExactArithmetic.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgExactPlane.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgExactQuadric.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgExactTransform.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgExpression.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgExpressionEquivalence.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgOperandView.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPointAttribution.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPointCandidate.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPointClassification.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPointEvidence.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPointProof.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPointWitness.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgPointWorkspace.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayClassification.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayEventBatch.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayEventCursor.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayEventGroup.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayEventQuery.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayEventReference.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayLeaf.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayLeafBatch.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayLeafQuery.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayPlacement.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRaySurfaceEvents.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRayWorkspace.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgRothRayClassifier.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgSolidBounds.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgSolidEquivalence.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgTree.cpp \
    src/main/vsdk/toolkit/environment/scene/csg/CsgVersionStamp.cpp

ifdef WITH_FREETYPE
SOURCES += src/main/vsdk/toolkit/io/geometry/Freetype2FontReader.cpp
endif

OBJECTS = $(patsubst %.cpp,buildmk/%.o,$(SOURCES))

# The library is split in two archives, the geometry and the rest, linked as
# a group (BASE_LINK of ../legacy.mk): with 64 MB of memory, the ranlib of
# binutils 2.7 on Debian 1.1 runs out of memory indexing all the objects of
# gcc 2.7.2 in a single archive
GEOMETRY_OBJECTS = $(filter buildmk/src/main/vsdk/toolkit/environment/geometry/%,$(OBJECTS))
OTHER_OBJECTS = $(filter-out $(GEOMETRY_OBJECTS),$(OBJECTS))
LIBRARY = build/libvitral_base.a
GEOMETRY_LIBRARY = build/libvitral_base_geometry.a

all: $(LIBRARY) $(GEOMETRY_LIBRARY)

$(LIBRARY): $(OTHER_OBJECTS)
	@mkdir -p build
	rm -f $@
	$(AR) rc $@ $(OTHER_OBJECTS)
	$(RANLIB) $@

$(GEOMETRY_LIBRARY): $(GEOMETRY_OBJECTS)
	@mkdir -p build
	rm -f $@
	$(AR) rc $@ $(GEOMETRY_OBJECTS)
	$(RANLIB) $@

buildmk/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -c $< -o $@

clean:
	rm -rf buildmk $(LIBRARY) $(GEOMETRY_LIBRARY)

.PHONY: all clean
