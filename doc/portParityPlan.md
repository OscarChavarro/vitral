# Port parity plan

State of the 1:1 parity of the `cpp`, `turboc` and `typescript` ports with
the Java reference implementation (review of 2026-11-22), and plan to close
the gaps with the equivalent technology of each port (OpenGL instead of
JOGL; Xt / Motif / Athena / GTK 4 / Xlib or HTML instead of AWT / Swing).

## 1. How the comparison was made

- Production code only (`src/main` and the applications); tests are not
  covered.
- **Classes**: a Java class is "present" in a port when a source file of
  the same name exists in the port. Missing ones were then searched as a
  `class` / `struct` / `interface` / `enum` declaration inside other files:
  those are listed as "merged" (the port has the type, in another file).
- **Methods**: for each class present in both, each public method name of
  the Java class was searched in the files of the port class. `toString`,
  `equals`, `hashCode`, `clone`, `compareTo` and `main` are skipped.
- **Adapters and applications**: names are compared after removing the
  technology prefix (`Jogl4`, `OpenGL4`, `OpenGL1`, `WebGL`, `Awt`, `Swing`,
  `Xt`, `Xm`, `Xaw`, `Gtk4`, `Html`, `Xlib`, ...); TypeScript application
  files (`kebab-case`) are converted to class names first.

The comparison is by name. A class merged under another name, or a method
that was renamed or made a free function, counts as missing; a method name
present in a comment counts as present. The lists are the input of the
work, not a proof: each item is confirmed when it is taken.

## 2. Summary

### Core (`base`), against the 558 classes of `java/base`

| | cpp | turboc | typescript |
|---|---|---|---|
| Java classes without a file of their own | 91 | 104 | 53 |
| ... of which the type exists in another file (merged) | 17 | 17 | 1 |
| ... absent | 74 | 87 | 52 |
| Public methods missing in the classes present | 124 of 3175 (3.9%) | 142 of 3160 (4.5%) | 39 of 3229 (1.2%) |
| Classes with at least one missing method | 59 | 66 | 22 |

Everything `cpp` lacks, `turboc` lacks too; `turboc` lacks 13 more classes.

### Rendering and GUI adapters

| Java reference | Equivalent | Missing |
|---|---|---|
| `jogl4` (62 classes) | `cpp/opengl4` | 4 |
| `jogl4` | `typescript/webgl` | 1 |
| `jogl4` | `cpp/opengl1` | 10 |
| `jogl4` | `turboc/opengl1` | 21 |
| `awt` (15 classes) | `cpp` `xt`, `xm`, `xaw`, `gtk4`, `glfw`, `glut`, `xlib` | 8 |
| `awt` | `turboc` `xt`, `xm`, `xaw`, `glut` | 8 |
| `awt` | `typescript/webgl` | 7 |
| `jogl2` (65 classes) | none: 34 of its classes have no equivalent in `jogl4` nor in any port | 34 |

### SceneEditorApplication (104 classes in Java)

| | cpp | turboc | typescript |
|---|---|---|---|
| Classes of the port | 117 | 95 | 81 |
| Java classes without an equivalent name | 19 | 23 | 28 |

### Testsuite

| Group | java | cpp | turboc | typescript |
|---|---|---|---|---|
| Offline examples | 8 | 6 | 6 | 5 |
| GPU examples (Jogl4 / OpenGL 4 / OpenGL 1 / WebGL) | 8 | 7 | 4 | 8 |
| Other applications (Android, J2ME, CommandServer, SciVis, 3D model search) | 5 | 0 | 0 | 0 |
| Jogl2 examples, techniques, benchmarks, VSDK examples, distributed examples | 12, 3, 2, 7, 1 | 0 | 0 | 0 |

## 3. Core gaps

### 3.1 Absent classes, by package

Columns show the port where the class is absent.

| Package (`vsdk/toolkit/...`) | Class | cpp | turboc | typescript |
|---|---|---|---|---|
| `(root)` | `Component` | absent | absent | absent |
| `animation` | `AnimationEvent` |  | absent |  |
| `animation` | `AnimationEventGenerator` |  | absent |  |
| `animation` | `AnimationListener` |  | absent |  |
| `animation` | `Md2AnimationListener` | absent | absent |  |
| `common` | `FundamentalEntity` | absent | absent |  |
| `common` | `ModelElement` | absent | absent |  |
| `common` | `NativeLibraryLoader` | absent | absent | absent |
| `common` | `VSDKJ2ME` | absent | absent | absent |
| `common/dataStructures` | `ArrayListOfBytes` | absent | absent |  |
| `common/dataStructures` | `ArrayListOfDoubles` | absent | absent |  |
| `common/dataStructures` | `ArrayListOfInts` | absent | absent |  |
| `common/dataStructures` | `ArrayListOfLongs` | absent | absent |  |
| `common/dataStructures` | `NAryTree` | absent | absent |  |
| `common/dataStructures` | `NAryTreeTraverser` | absent | absent |  |
| `common/dataStructures` | `_NAryTreeIntermediateNode` | absent | absent |  |
| `common/dataStructures` | `_NAryTreeLeafNode` | absent | absent |  |
| `common/dataStructures` | `_NAryTreeNode` | absent | absent |  |
| `common/statistics` | `RenderingStatistics` | absent | absent |  |
| `environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition` | `_ContourData` | absent | absent |  |
| `environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition` | `_IndexedVertex` | absent | absent |  |
| `environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/construction` | `_BoundaryRepresentationFromCurveBuildState` | absent | absent |  |
| `environment/geometry/surface` | `_ParametricBiCubicPatchIntersector` |  |  | absent |
| `gui` | `CameraControllerBlender` |  |  | absent |
| `gui` | `CameraControllerGoogleEarth` |  |  | absent |
| `gui` | `CameraControllerOrbiter` |  |  | absent |
| `gui` | `HudIcon` | absent | absent | absent |
| `gui` | `TextVisualConfiguration` | absent | absent | absent |
| `gui` | `ViewportWindow` | absent | absent | absent |
| `gui` | `ViewportWindowSetManager` | absent | absent | absent |
| `gui/dialog` | `InformationDialog` | absent | absent | absent |
| `gui/dialog/components` | `DialogFileFilter` |  |  | absent |
| `gui/dialog/components` | `DialogTreeNode` |  |  | absent |
| `gui/visualAnalytics` | `PercentageWheelWidget` | absent | absent | absent |
| `gui/visualAnalytics` | `PercentageWheelWidgetController` | absent | absent | absent |
| `gui/visualAnalytics` | `VisualDoubleVariable` | absent | absent | absent |
| `gui/visualAnalytics` | `VisualVariableSet` | absent | absent | absent |
| `gui/widget` | `ExceptionWidgetBadName` | absent | absent |  |
| `gui/widget` | `ExceptionWidgetParseError` | absent | absent |  |
| `gui/widget` | `WidgetCommandExecutor` | absent | absent |  |
| `gui/widget/variable` | `WidgetBooleanVariable` | absent | absent |  |
| `gui/widget/variable` | `WidgetColorRgbVariable` | absent | absent |  |
| `gui/widget/variable` | `WidgetDoubleVariable` | absent | absent |  |
| `gui/widget/variable` | `WidgetIntegerVariable` | absent | absent |  |
| `gui/widget/variable` | `WidgetStringVariable` | absent | absent |  |
| `gui/widget/variable` | `WidgetVector3DVariable` | absent | absent |  |
| `io` | `FileLocator` |  |  | absent |
| `io` | `XmlException` |  |  | absent |
| `io/geometry` | `Md2Persistence` | absent | absent |  |
| `io/geometry` | `ParametricBiCubicPatchPersistence` |  |  | absent |
| `io/geometry` | `ParametricCurvePersistence` |  |  | absent |
| `io/geometry` | `Reader3ds` | absent | absent | absent |
| `io/geometry` | `ReaderAse` | absent | absent | absent |
| `io/geometry` | `ReaderBinNeedForSpeed` | absent | absent | absent |
| `io/geometry` | `ReaderGts` | absent | absent | absent |
| `io/geometry` | `ReaderPly` |  |  | absent |
| `io/geometry` | `ReaderPlyResult` |  |  | absent |
| `io/geometry` | `ReaderVrml` | absent | absent | absent |
| `io/geometry` | `ReaderVtk` | absent | absent | absent |
| `io/geometry` | `ViewpointBinaryPersistence` | absent | absent | absent |
| `io/geometry` | `_CScanf` | absent | absent |  |
| `io/geometry/stepCad` | `StepLengthUnit` |  | absent |  |
| `io/geometry/stepCad/reader` | `StepReader` |  | absent |  |
| `io/geometry/stepCad/reader` | `_StepEntity` |  | absent |  |
| `io/geometry/stepCad/reader` | `_StepSolidBuilder` |  | absent |  |
| `io/geometry/stepCad/reader` | `_StepTokenizer` |  | absent |  |
| `io/geometry/stepCad/writer` | `StepWriter` | absent | absent | absent |
| `io/geometry/stepCad/writer` | `_StepEntityBuffer` | absent | absent | absent |
| `io/geometry/stepCad/writer` | `_StepGeometryEmitter` | absent | absent | absent |
| `io/geometry/stepCad/writer` | `_StepHeaderWriter` | absent | absent | absent |
| `io/geometry/stepCad/writer` | `_StepProductStructureEmitter` | absent | absent | absent |
| `io/geometry/stepCad/writer` | `_StepSolidValidator` | absent | absent | absent |
| `io/geometry/stepCad/writer` | `_StepTopologyEmitter` | absent | absent | absent |
| `io/geometry/stepCad/writer` | `_StepUnitContextEmitter` | absent | absent | absent |
| `io/geometry/stl` | `StlWriter` |  | absent |  |
| `io/geometry/stl` | `_StlFaceTriangulator` |  | absent |  |
| `io/geometry/stl` | `_StlFacetEmitter` |  | absent |  |
| `io/geometry/stl` | `_StlSolidValidator` |  | absent |  |
| `io/gui` | `GuiPersistence` | absent | absent |  |
| `io/image` | `ImageNotRecognizedException` | absent | absent |  |
| `io/image` | `ImagePersistenceHelper` | absent | absent |  |
| `io/image` | `ImagePersistenceSGI` | absent | absent |  |
| `io/image` | `ImagePersistenceTarga` | absent | absent | absent |
| `io/image` | `NativeImageReaderWrapper` | absent | absent | absent |
| `io/image` | `_NativeImageReaderWrapperHeaderInfo` | absent | absent | absent |
| `io/metadata` | `ShapeDescriptorPersistence` | absent | absent | absent |
| `io/xml` | `XmlImporter` | absent | absent | absent |
| `io/xml` | `XmlNode` | absent | absent | absent |
| `io/xml` | `XmlNodeParam` | absent | absent | absent |
| `processing` | `LzwWrapper` | absent | absent | absent |
| `processing` | `SignalProcessing` | absent | absent |  |
| `processing` | `SolverPolynomialQuarticBairstow` | absent | absent |  |
| `processing` | `SpharmonicKitWrapper` | absent | absent | absent |
| `processing/linealAlgebra` | `LinearAlgebraBackend` | absent | absent |  |
| `processing/linealAlgebra` | `MatrixAlgorithmsSupport` | absent | absent |  |
| `processing/linealAlgebra` | `StrategySelector` | absent | absent |  |
| `render` | `AutoStereogramGenerator` |  |  | absent |
| `render` | `PolyhedralBoundedSolidDebugger` |  |  | absent |
| `render` | `SelectionCorners` |  | absent |  |
| `shapeMatching` | `ShapeDescriptor2DGenerator` | absent | absent | absent |
| `shapeMatching/plugins` | `ShapeDescriptor2DGeneratorFourier` | absent | absent | absent |

Notes:

- `VSDKJ2ME`, `NativeLibraryLoader`, `LzwWrapper`, `SpharmonicKitWrapper`,
  `NativeImageReaderWrapper` and `_NativeImageReaderWrapperHeaderInfo` are
  Java platform glue (J2ME, JNI): they have no equivalent to port. The C++
  ports link libpng / libjpeg directly instead.
- `ArrayListOfBytes`, `ArrayListOfDoubles`, `ArrayListOfInts` and
  `ArrayListOfLongs` are primitive collections of Java; the C++ ports use
  the templates of their Java-like layer, so they are not needed there.
- `ReaderMitScene` of TypeScript lives in `@vitral/fs`, not in
  `@vitral/base` (it is listed in 3.2).

### 3.2 Classes merged in another file (type present)

- **cpp** (17): `BinnedSahHierarchyNode`, `BinnedSahInstanceVisitor`, `BinnedSahLeafVisitor`, `DeterminantStrategy`, `GaussCpuStrategy`, `InverseStrategy`, `LuCpuStrategy`, `MatrixDimensionMismatchException`, `MatrixIndexOutOfBoundsException`, `MatrixNotSquareException`, `MatrixSingularException`, `NaiveCofactorCpuStrategy`, `TriangleMeshAccelerationMethodUtil`, `_BinnedSahBox`, `_BinnedSahBuildTask`, `_BinnedSahInstanceAdapter`, `_TriangleMeshGroupClosestHit`
- **turboc** (17): `BinnedSahHierarchyNode`, `BinnedSahInstanceVisitor`, `BinnedSahLeafVisitor`, `DeterminantStrategy`, `GaussCpuStrategy`, `InverseStrategy`, `LuCpuStrategy`, `MatrixDimensionMismatchException`, `MatrixIndexOutOfBoundsException`, `MatrixNotSquareException`, `MatrixSingularException`, `NaiveCofactorCpuStrategy`, `TriangleMeshAccelerationMethodUtil`, `_BinnedSahBox`, `_BinnedSahBuildTask`, `_BinnedSahInstanceAdapter`, `_TriangleMeshGroupClosestHit`
- **typescript** (1): `ReaderMitScene`

These need no work, unless the project wants one file per class in every
port (then they are mechanical moves).

### 3.3 Missing public methods in classes that exist

#### cpp

| Class | Missing / public | Methods |
|---|---|---|
| `Camera` | 12 / 52 | `boundingConvexPolyhedraIsVisible`, `calculateFarPlane`, `calculateNearPlane`, `calculateUPlane`, `calculateUPlaneAtPixel`, `calculateVPlane`, `calculateVPlaneAtPixel`, `clipLineCohenSutherlandCanonicVolume`, `clipLineCohenSutherlandPlanes`, `convertViewportPointToUnitSquare`, `getBoundingPlanes`, `viewport2UnitSquareTransform` |
| `VSDK` | 8 / 9 | `formatByteAsHex`, `formatIntAsHex`, `formatNumberWithinZeroes`, `setWithFatalExceptions`, `setWithSystemExit`, `signedByte2unsignedInteger`, `square`, `unsigned8BitInteger2signedByte` |
| `ImagePersistence` | 7 / 12 | `exportBMP`, `exportGIF`, `exportJPG`, `exportPNG_24bitRgb`, `exportPNM`, `initHelpers`, `registerHelper` |
| `Intersection` | 6 / 6 | `getNormal`, `getPoint`, `getT`, `setNormal`, `setPoint`, `setT` |
| `RayGizmo` | 6 / 36 | `RollIndicatorMesh`, `buildRollIndicatorMesh`, `calculateMissingRayLength`, `calculateViewThicknessScale`, `calculateVisibleNormalLength`, `getRollIndicatorModelMatrix` |
| `CameraControllerGoogleEarth` | 5 / 18 | `getxOld`, `getyOld`, `processMouseEvent`, `setxOld`, `setyOld` |
| `ComputationalGeometry` | 5 / 8 | `accepted`, `clipped0`, `clipped1`, `cohenSutherlandLineClipping2D`, `cohenSutherlandLineClipping2DResult` |
| `RGBAImageCompressed` | 4 / 18 | `dettach`, `getPixel`, `getPixelRgba`, `putPixel` |
| `RGBAImageUncompressed` | 4 / 16 | `dettach`, `exportToRgbImage`, `getRawImageDirectBuffer`, `getRawImageSizeInBytes` |
| `WidgetDialog` | 4 / 32 | `getAction`, `getBinding`, `getKind`, `getLabelKey` |
| `LinearAlgebraEngine` | 3 / 8 | `backend`, `determinantStrategy`, `inverseStrategy` |
| `ZBuffer` | 3 / 9 | `getZ`, `setZ`, `setZBuffer` |
| `ControlledRGBAImageHDRUncompressed` | 2 / 14 | `getInterpolationTypeEnum`, `getMapTypeEnum` |
| `CsgDyadic` | 2 / 8 | `assign`, `newArray` |
| `ImageToSolidTextureInterpolationTypes` | 2 / 2 | `fromInt`, `value` |
| `ImageToSolidTextureProjectionMethods` | 2 / 2 | `fromInt`, `value` |
| `IndexedColorImageUncompressed` | 2 / 14 | `exportToRgbImage`, `getPixel8bitGrayScale` |
| `PolyhedralBoundedSolidDebugger` | 2 / 2 | `createOfflineRenderer`, `execute` |
| `RGBImageUncompressed` | 2 / 16 | `dettach`, `getRawImageSizeInBytes` |
| `TangibleInterfaceNetworkClient` | 2 / 5 | `onError`, `onText` |
| `TriangleMesh` | 2 / 66 | `isDoubleSided`, `setDoubleSided` |
| `Vector2Dd` | 2 / 11 | `distance`, `set` |
| `BezierPatchSubdivision` | 1 / 1 | `value` |
| `ColorRgb` | 1 / 10 | `exportToFloatArrayVector` |
| `ColorRgba` | 1 / 9 | `set` |
| `Containment` | 1 / 1 | `value` |
| `CsgOperation` | 1 / 1 | `value` |
| `CsgPointAttribution` | 1 / 11 | `assign` |
| `CsgPointCandidate` | 1 / 1 | `assign` |
| `CsgPointEvidence` | 1 / 9 | `assign` |
| `CsgPointProof` | 1 / 11 | `assign` |
| `CsgPointWitness` | 1 / 8 | `assign` |
| `CsgRayClassification` | 1 / 10 | `assign` |
| `CsgRayEventBatch` | 1 / 5 | `assign` |
| `CsgRayEventGroup` | 1 / 5 | `assign` |
| `CsgRayLeaf` | 1 / 9 | `assign` |
| `CsgRayLeafBatch` | 1 / 6 | `assign` |
| `CsgRaySurfaceEvents` | 1 / 8 | `assign` |
| `CsgVersionStamp` | 1 / 6 | `assign` |
| `Entity` | 1 / 8 | `getEncapsulatedVariables` |
| `GeometryBounds` | 1 / 14 | `assign` |
| `HeightFieldSolid` | 1 / 28 | `assign` |
| `InfinitePlaneGizmo` | 1 / 16 | `buildFrameCorners` |
| `Light` | 1 / 13 | `LightDirection` |
| `Md2Mesh` | 1 / 11 | `getAnimationName` |
| `NormalMap` | 1 / 11 | `exportToRgbaImage` |
| `QuadMesh` | 1 / 25 | `doContainmentTest` |
| `RGBAColorPalette` | 1 / 11 | `colorsView` |
| `Rasterizer2D` | 1 / 7 | `compare` |
| `RayBoundary` | 1 / 6 | `assign` |
| `RayCrossing` | 1 / 1 | `assign` |
| `RayInstancePath` | 1 / 8 | `assign` |
| `RayInstanceStep` | 1 / 1 | `assign` |
| `RaySolidClassification` | 1 / 2 | `assign` |
| `ShadingType` | 1 / 3 | `getCode` |
| `SimpleBody` | 1 / 30 | `transformPointToWorld` |
| `SubdividedBezierPatch` | 1 / 20 | `assign` |
| `TriangleMeshIntersectionIndex` | 1 / 3 | `assign` |
| `_Construct` | 1 / 17 | `setSegmentInserted` |

#### turboc

| Class | Missing / public | Methods |
|---|---|---|
| `Camera` | 12 / 52 | `boundingConvexPolyhedraIsVisible`, `calculateFarPlane`, `calculateNearPlane`, `calculateUPlane`, `calculateUPlaneAtPixel`, `calculateVPlane`, `calculateVPlaneAtPixel`, `clipLineCohenSutherlandCanonicVolume`, `clipLineCohenSutherlandPlanes`, `convertViewportPointToUnitSquare`, `getBoundingPlanes`, `viewport2UnitSquareTransform` |
| `VSDK` | 8 / 9 | `formatByteAsHex`, `formatIntAsHex`, `formatNumberWithinZeroes`, `setWithFatalExceptions`, `setWithSystemExit`, `signedByte2unsignedInteger`, `square`, `unsigned8BitInteger2signedByte` |
| `ImagePersistence` | 7 / 12 | `exportBMP`, `exportGIF`, `exportJPG`, `exportPNG_24bitRgb`, `exportPNM`, `initHelpers`, `registerHelper` |
| `Intersection` | 6 / 6 | `getNormal`, `getPoint`, `getT`, `setNormal`, `setPoint`, `setT` |
| `RayGizmo` | 6 / 36 | `RollIndicatorMesh`, `buildRollIndicatorMesh`, `calculateMissingRayLength`, `calculateViewThicknessScale`, `calculateVisibleNormalLength`, `getRollIndicatorModelMatrix` |
| `CameraControllerGoogleEarth` | 5 / 18 | `getxOld`, `getyOld`, `processMouseEvent`, `setxOld`, `setyOld` |
| `ComputationalGeometry` | 5 / 8 | `accepted`, `clipped0`, `clipped1`, `cohenSutherlandLineClipping2D`, `cohenSutherlandLineClipping2DResult` |
| `RGBAImageCompressed` | 4 / 18 | `dettach`, `getPixel`, `getPixelRgba`, `putPixel` |
| `RGBAImageUncompressed` | 4 / 16 | `dettach`, `exportToRgbImage`, `getRawImageDirectBuffer`, `getRawImageSizeInBytes` |
| `WidgetDialog` | 4 / 32 | `getAction`, `getBinding`, `getKind`, `getLabelKey` |
| `LinearAlgebraEngine` | 3 / 8 | `backend`, `determinantStrategy`, `inverseStrategy` |
| `ZBuffer` | 3 / 9 | `getZ`, `setZ`, `setZBuffer` |
| `_PolyhedralBoundedSolidEdge` | 3 / 5 | `getListIndex`, `getListOwner`, `setListSlot` |
| `_PolyhedralBoundedSolidFace` | 3 / 12 | `getListIndex`, `getListOwner`, `setListSlot` |
| `_PolyhedralBoundedSolidHalfEdge` | 3 / 7 | `getListIndex`, `getListOwner`, `setListSlot` |
| `_PolyhedralBoundedSolidLoop` | 3 / 8 | `getListIndex`, `getListOwner`, `setListSlot` |
| `_PolyhedralBoundedSolidVertex` | 3 / 3 | `getListIndex`, `getListOwner`, `setListSlot` |
| `ControlledRGBAImageHDRUncompressed` | 2 / 14 | `getInterpolationTypeEnum`, `getMapTypeEnum` |
| `CsgDyadic` | 2 / 8 | `assign`, `newArray` |
| `HiddenLineRenderer` | 2 / 6 | `edgeTypeName`, `executeAppelAlgorithmWithDiagnostics` |
| `ImageToSolidTextureInterpolationTypes` | 2 / 2 | `fromInt`, `value` |
| `ImageToSolidTextureProjectionMethods` | 2 / 2 | `fromInt`, `value` |
| `IndexedColorImageUncompressed` | 2 / 14 | `exportToRgbImage`, `getPixel8bitGrayScale` |
| `PolyhedralBoundedSolidDebugger` | 2 / 2 | `createOfflineRenderer`, `execute` |
| `RGBImageUncompressed` | 2 / 16 | `dettach`, `getRawImageSizeInBytes` |
| `TangibleInterfaceNetworkClient` | 2 / 5 | `onError`, `onText` |
| `TriangleMesh` | 2 / 66 | `isDoubleSided`, `setDoubleSided` |
| `Vector2Dd` | 2 / 11 | `distance`, `set` |
| `BezierPatchSubdivision` | 1 / 1 | `value` |
| `Calligraphic2DBuffer` | 1 / 5 | `get2DLine` |
| `ColorRgb` | 1 / 10 | `exportToFloatArrayVector` |
| `ColorRgba` | 1 / 9 | `set` |
| `CsgOperation` | 1 / 1 | `value` |
| `CsgPointAttribution` | 1 / 11 | `assign` |
| `CsgPointCandidate` | 1 / 1 | `assign` |
| `CsgPointEvidence` | 1 / 9 | `assign` |
| `CsgPointProof` | 1 / 11 | `assign` |
| `CsgPointWitness` | 1 / 8 | `assign` |
| `CsgRayClassification` | 1 / 10 | `assign` |
| `CsgRayEventBatch` | 1 / 5 | `assign` |
| `CsgRayEventGroup` | 1 / 5 | `assign` |
| `CsgRayLeaf` | 1 / 9 | `assign` |
| `CsgRayLeafBatch` | 1 / 6 | `assign` |
| `CsgRaySurfaceEvents` | 1 / 8 | `assign` |
| `CsgVersionStamp` | 1 / 6 | `assign` |
| `Entity` | 1 / 8 | `getEncapsulatedVariables` |
| `GeometryBounds` | 1 / 14 | `assign` |
| `HeightFieldSolid` | 1 / 28 | `assign` |
| `InfinitePlaneGizmo` | 1 / 16 | `buildFrameCorners` |
| `Light` | 1 / 13 | `LightDirection` |
| `Md2Mesh` | 1 / 11 | `getAnimationName` |
| `NormalMap` | 1 / 11 | `exportToRgbaImage` |
| `QuadMesh` | 1 / 25 | `doContainmentTest` |
| `RGBAColorPalette` | 1 / 11 | `colorsView` |
| `Rasterizer2D` | 1 / 7 | `compare` |
| `RayBoundary` | 1 / 6 | `assign` |
| `RayCrossing` | 1 / 1 | `assign` |
| `RayInstancePath` | 1 / 8 | `assign` |
| `RayInstanceStep` | 1 / 1 | `assign` |
| `RaySolidClassification` | 1 / 2 | `assign` |
| `ShadingType` | 1 / 3 | `getCode` |
| `SimpleBody` | 1 / 30 | `transformPointToWorld` |
| `SubdividedBezierPatch` | 1 / 20 | `assign` |
| `TriangleMeshIntersectionIndex` | 1 / 3 | `assign` |
| `Vector3Dd` | 1 / 22 | `distance` |
| `_Construct` | 1 / 17 | `setSegmentInserted` |

#### typescript

| Class | Missing / public | Methods |
|---|---|---|
| `ImagePersistence` | 6 / 12 | `exportBMP`, `exportGIF`, `exportJPG`, `exportPNM`, `importRGB`, `importRGBA` |
| `VoxelVolume` | 4 / 21 | `exportToPolyhedralBoundedSolid`, `getThreshold`, `isFilled`, `setThreshold` |
| `WidgetDialog` | 4 / 32 | `getAction`, `getBinding`, `getKind`, `getLabelKey` |
| `ShadingType` | 3 / 3 | `fromCode`, `getCode`, `next` |
| `ImageToSolidTextureInterpolationTypes` | 2 / 2 | `fromInt`, `value` |
| `ParametricBiCubicPatch` | 2 / 16 | `doExtraInformation`, `getBezierControlPoint` |
| `PersistenceElement` | 2 / 43 | `checkDirectory`, `verifyLibrary` |
| `Vector2Dd` | 2 / 11 | `x`, `y` |
| `BezierPatchSubdivision` | 1 / 1 | `value` |
| `Box` | 1 / 26 | `doContainmentTest` |
| `Cone` | 1 / 12 | `doContainmentTest` |
| `Containment` | 1 / 1 | `value` |
| `CsgDyadic` | 1 / 8 | `newArray` |
| `CsgOperation` | 1 / 1 | `value` |
| `ImagePersistenceHelper` | 1 / 15 | `exportGIF` |
| `ImageToSolidTextureProjectionMethods` | 1 / 2 | `fromInt` |
| `InfinitePlaneGizmo` | 1 / 16 | `buildFrameCorners` |
| `Light` | 1 / 13 | `LightDirection` |
| `NormalMapTexture` | 1 / 1 | `getFloatSamples` |
| `RGBAImageUncompressed` | 1 / 16 | `getRawImageSizeInBytes` |
| `RGBImageUncompressed` | 1 / 16 | `getRawImageSizeInBytes` |
| `Shader` | 1 / 2 | `LocalShadingResult` |

Known false positives of the name search: accessors of Java records and
enums that the ports write as fields or free functions (`value`, `fromInt`,
`getCode`, `accepted`, `clipped0`, `x`, `y`), and nested types reported as
methods (`LightDirection`, `LocalShadingResult`, `RollIndicatorMesh`).

## 4. Adapter gaps

### 4.1 `jogl4` against the OpenGL and WebGL adapters

| Java class | `cpp/opengl4` | `cpp/opengl1` | `turboc/opengl1` | `typescript/webgl` |
|---|---|---|---|---|
| `Jogl4InfinitePlaneGizmoRenderer` | missing | missing | missing | |
| `Jogl4ReferenceFrameGizmoRenderer` | missing | missing | missing | |
| `Jogl4Md2MeshRenderer` | missing | missing | missing | |
| `Jogl4ShaderLoader` | missing | n/a (no shaders) | n/a | |
| `Jogl4ShaderProgramUtil`, `Jogl4RendererConfigurationShaderSelector` | | n/a (no shaders) | n/a | |
| `Jogl4PolyhedralBoundedSolidDebugRenderer`, `Jogl4PolyhedralBoundedSolidDebugHUDRenderer` | | missing | missing | |
| `Jogl4SimpleMaterialRenderer`, `Jogl4SolidTextureRenderer` | | missing | missing | |
| `Jogl4VoxelVolumeRenderer` | | | missing | missing |
| `Jogl4BoxRenderer`, `Jogl4ConeRenderer`, `Jogl4TorusRenderer`, `Jogl4InfinitePlaneRenderer` | | | missing | |
| `Jogl4PolyhedralBoundedSolidRenderer`, `Jogl4QuadMeshRenderer`, `Jogl4TriangleMeshGroupRenderer` | | | missing | |
| `Jogl4FunctionalExplicitSurfaceRenderer`, `Jogl4ParametricBiCubicPatchRenderer`, `Jogl4ParametricCurveRenderer` | | | missing | |

`cpp/opengl1` is a mechanical port of `cpp/opengl4` without shaders, so the
three shader classes do not apply to the OpenGL 1.2 adapters.

### 4.2 `awt` against the GUI and 2D drawing adapters

| Java class | Role | cpp | turboc | typescript |
|---|---|---|---|---|
| `AwtCalligraphic2DBufferRenderer` | draws a buffer of 2D lines | missing | missing | missing |
| `AwtIndexedColorImageUncompressedRenderer` | draws an indexed image | missing | missing | missing |
| `AwtRGBColorPaletteRenderer` | draws a palette | missing | missing | missing |
| `ImagePersistenceAwt` | image formats through the platform | missing | missing | missing |
| `SwingEventListener` | event bridge | missing | missing | missing |
| `AwtFontReader` | font outlines | missing (FreeType reader in `base`) | missing (same) | |
| `AwtRenderer` | common 2D drawing entry | missing | missing | |
| `CollapsablePanel` | collapsible section widget | missing | missing | |
| `AwtGizmo2DDrawListRenderer` | draws the 2D parts of gizmos | | | missing |
| `SwingDialogHost` | host of the dialogs of `gui/dialog` | | | missing |

### 4.3 `jogl2` classes without an equivalent anywhere

`jogl2` is the older JOGL adapter; 34 of its 65 classes were never carried
to `jogl4`, so no port has them either:

- Stereo: `Jogl2StereoStrategyRenderer` and its eight strategies (anaglyph,
  autostereogram, cyclopean z-buffer, interlace, multiview, pbuffer, quad
  buffer, wiggle).
- Backgrounds: `Jogl2CubemapBackgroundRenderer`,
  `Jogl2FixedBackgroundRenderer`, `Jogl2SimpleBackgroundRenderer`.
- Widgets and HUD: `Jogl2HudIconRenderer`,
  `Jogl2PercentageWheelWidgetRenderer`, `Jogl2ProgressMonitorRenderer`,
  `Jogl2CurvedArrowOverPlaneRenderer`.
- Shape matching: `ShapeDescriptor3DGenerator`,
  `ShapeDescriptor3DGeneratorSphericalHarmonics`,
  `Jogl2ShapeMatchingOfflineRenderer`,
  `JoglShapeMatchingOfflineRenderable`.
- Others: `Jogl2ZBufferRenderer`, `Jogl2ProjectedViewRenderer`,
  `Jogl2Calligraphic2DBufferRenderer`, the face and vertex renderers of
  the boundary representation, `Jogl2SimpleBodyRenderer` and
  `Jogl2SimpleBodyGroupRenderer`, `ImagePersistenceJogl`,
  `JoglRepainterAnimationListener`, the tesselator routines and the image
  renderer associations.

Before porting any of them, decide whether each one is moved to `jogl4`
(then it becomes part of the reference) or retired with `jogl2`.

## 5. SceneEditorApplication gaps

Java classes without an equivalent name in each port. In `cpp` and
`turboc` most of them are AWT pieces whose work is done by the Xt / Xlib
code with another structure; those are marked "by structure" and need a
reading, not a port.

| Package | Java classes | cpp | turboc | typescript |
|---|---|---|---|---|
| `application` | `AwtJogl4MainThread`, `AwtJogl4GuiController` | by structure | by structure | `AwtJogl4MainThread` by structure |
| `application/commands` | `CommandResult`, `ExportFormat`, `GeometryCreationCommandsExecutor`, `Presenter` | | | missing |
| `application/mcp` | `MCPSceneSerializer` | | missing | missing |
| `application/mcp` | `MCPJson`, `MCPSceneTools`, `MCPViewportTools`, `AwtJogl4MCPApplicationTools` | | | missing (one protocol class holds them) |
| `gui/awt` | buttons panel, command executor, cursor warper, drawing area controller and feedback, frame scheduler, key event mapper, look and feel tuner, projection popup, file filter, element scaler | by structure | by structure | 4 missing: collapsible section, bevel border, frame scheduler, modify panel of the functional explicit surface |
| `io` | `XmlManager` | | | missing |
| `model` | `RenderTechnology` | | missing | missing |
| `model/dialog`, `model/history` | `SceneHierarchyPresenter`, `SceneSelectionOperation` | | | missing |
| `render` | `ClipSpaceLineProjector`, `ProjectedLineLayer`, `ProjectedLinesViewBuilder` | | missing by name | missing |
| `render` | `BrepExportCache`, `ProjectedPolygon`, `ProjectedSurfacesViewBuilder` | | | missing |
| `render/awt` | `AwtProjectedLineLayerRenderer`, `AwtProjectedPolygonRenderer`, `AwtViewportSetCanvas`, `AwtViewportSetRenderer` | by structure (Xlib and frame buffer renderers) | by structure (Xlib renderer) | missing |

The functional consequence in the web editor: it has no CPU drawing of
projected lines and polygons, so it offers neither the hidden lines nor the
hidden surfaces render modes, although both renderers are in
`@vitral/base`.

## 6. Testsuite gaps

- Offline examples missing: `KurlanderBowlBuilder` and
  `PolyhedralBoundedSolidExpotImportExample` (cpp, turboc, typescript);
  `ImageOfflineExample` (typescript).
- GPU examples missing: `MD2Example` (cpp); `PolyhedralBoundedSolidExample`,
  `ShadersExample`, `SolidTextureExample` and `MD2Example` (turboc; the two
  with shaders do not apply to OpenGL 1.2).
- Tools: Java has `LogAnalyzer`, `SpriteFontGenerator` and
  `TangibleInterfaceGizmoCreator`; cpp has the tangible interface label and
  marker creators and the marker detector server. Each side has tools the
  other lacks.
- Java only: `AndroidVitralSceneEditor`, `J2MEVitralSceneEditor`,
  `CommandServer`, `SciVisApplication`, `SearchEngineFor3DModels`, the 12
  `Jogl2Examples`, `Techniques`, `Benchmarks`, `VSDKExamples`,
  `DistributedExamples`.

## 7. Plan

Rules: the Java class is ported literally (same names, same order of
methods), then verified against the Java output where an offline example
or a test exists; `turboc` takes the `cpp` file as its source, in the
legacy subset. One commit per stage and port, made by the maintainer. A
gap that is decided to stay is written in the feature table of the README
as "Java only", so it stops being a gap.

### Stage 1. Decisions that remove work

1. `jogl2`: for each of the 34 classes of section 4.3, move to `jogl4` or
   retire. Until then they are out of the parity target.
2. Java-only applications and examples (section 6, last item): mark as
   Java only, except the ones chosen as worth having everywhere.
3. Platform glue (JNI wrappers, `VSDKJ2ME`): Java only by nature.
4. XML scene importer (`XmlImporter`, `XmlNode`, `XmlNodeParam`,
   `XmlManager`): it rests on `org.w3c.dom`. Either write a small parser
   over the Java-like IO layer of the C++ ports and a DOM based one for
   TypeScript, or declare it Java only. Decide before stage 4.

### Stage 2. Small core gaps (cpp and turboc)

1. `Camera`: the 12 missing methods (`calculateNearPlane`,
   `calculateFarPlane`, `calculateUPlane`, `calculateVPlane` and their
   `AtPixel` variants, `boundingConvexPolyhedraIsVisible`, the
   Cohen-Sutherland line clipping). They only need `InfinitePlane`, which
   the ports have.
2. `ComputationalGeometry.cohenSutherlandLineClipping2D` and its result
   type.
3. Image accessors: `ZBuffer.getZ` / `setZ`, the pixel accessors of
   `RGBAImageCompressed`, `exportToRgbImage`, `getRawImageSizeInBytes`.
4. `TriangleMesh.isDoubleSided` / `setDoubleSided`, `Vector2Dd.distance`,
   the `VSDK` formatting helpers, `Intersection`.
5. `NAryTree` and its nodes, `SolverPolynomialQuarticBairstow`,
   `SignalProcessing`, `RenderingStatistics`.
6. turboc only: `SelectionCorners`, the animation event classes.

TypeScript: `VoxelVolume.exportToPolyhedralBoundedSolid` with its
threshold accessors, `ParametricBiCubicPatch.doExtraInformation` and
`_ParametricBiCubicPatchIntersector`, `Box.doContainmentTest` and
`Cone.doContainmentTest`, `AutoStereogramGenerator`,
`PolyhedralBoundedSolidDebugger`, the three camera controllers (Blender,
Google Earth, orbiter).

### Stage 3. Readers and writers

No external library is needed for any of these.

| Format | cpp | turboc | typescript |
|---|---|---|---|
| PLY (`ReaderPly`, `ReaderPlyResult`) | present | present | port |
| STL (`StlWriter` and helpers) | present | port | present |
| STEP reader | present | port | present |
| STEP writer (`StepWriter` and its emitters) | port | port | port |
| Targa, SGI (`ImagePersistenceTarga`, `ImagePersistenceSGI`) | port | port | port Targa (SGI present) |
| Image helpers and BMP / PNM export (`ImagePersistenceHelper`, `exportBMP`, `exportPNM`) | port | port | helper present; port the export methods |
| GTS, VTK readers | port | port | port |
| Curves and patches persistence | present | present | port |
| MD2 (`Md2Persistence`, `Md2AnimationListener`) | port | port | present |
| 3DS, ASE, VRML, NFS readers; `ViewpointBinaryPersistence` | decide in stage 1 | decide | decide |

GIF and JPG export go through the platform in Java
(`ImagePersistenceAwt`); the C++ ports use libjpeg where it is built, and
TypeScript the canvas of the browser or a Node library in `@vitral/fs`.

### Stage 4. Toolkit widgets of the core

`gui/widget/variable` (the six `Widget...Variable` classes),
`WidgetCommandExecutor`, the widget exceptions, `GuiPersistence`,
`InformationDialog`, `HudIcon`, `TextVisualConfiguration`,
`ViewportWindow`, `ViewportWindowSetManager`, the visual analytics
classes, and the four missing accessors of `WidgetDialog`. They are model
classes without window system code; their drawing belongs to stage 5.

TypeScript has the widget variables; it lacks `HudIcon`,
`TextVisualConfiguration`, `InformationDialog`, `ViewportWindow`,
`ViewportWindowSetManager`, the visual analytics classes, the dialog
components (`DialogFileFilter`, `DialogTreeNode`), `FileLocator` (a
candidate for `@vitral/fs`) and the `WidgetDialog` accessors.

### Stage 5. 2D drawing adapters (the AWT equivalents)

The same three drawings in each 2D technology:

| Java (`awt`) | cpp / turboc equivalent | typescript equivalent |
|---|---|---|
| `AwtCalligraphic2DBufferRenderer` | `XlibCalligraphic2DBufferRenderer` in `xlib/` (`XDrawSegments`); the frame buffer renderer of `base` for the kernel and SVGALIB | `HtmlCalligraphic2DBufferRenderer` over a canvas 2D context |
| `AwtIndexedColorImageUncompressedRenderer` | Xlib `XPutImage` with the palette mapped to the visual | canvas `putImageData` |
| `AwtRGBColorPaletteRenderer` | Xlib rectangles | canvas rectangles |
| `AwtGizmo2DDrawListRenderer` | present | port to canvas 2D |
| `CollapsablePanel` | an Xt composite in `xt/`, with Motif and Athena variants; GTK 4 has `GtkExpander` | present by structure |
| `SwingDialogHost` | present | an HTML `dialog` host |

In `cpp` and `turboc` the scene editor already draws projected lines and
polygons inside `XlibDrawingAreaRenderer`; extracting that code into the
classes above gives the adapters without new drawing code.

### Stage 6. OpenGL adapters

1. `cpp/opengl4`: `OpenGL4InfinitePlaneGizmoRenderer`,
   `OpenGL4ReferenceFrameGizmoRenderer`, `OpenGL4Md2MeshRenderer` (after
   MD2 of stage 3), `OpenGL4ShaderLoader`; then the `MD2Example`. The
   WebGL adapter already has the first three and can be read next to the
   Java ones.
2. `cpp/opengl1`: the same three renderers, the two debug renderers of the
   boundary representation, `OpenGL1SimpleMaterialRenderer` (fixed
   function `glMaterial`) and `OpenGL1SolidTextureRenderer` (3D textures
   need OpenGL 1.2, which is the target).
3. `turboc/opengl1`: copy from `cpp/opengl1` the primitive renderers it
   lacks (box, cone, torus, infinite plane, boundary representation solid,
   quad mesh, triangle mesh group, functional explicit surface, parametric
   patch and curve, voxels), then the items of step 2, then the
   `PolyhedralBoundedSolidExample`.
4. `typescript/webgl`: `WebGLVoxelVolumeRenderer`.

### Stage 7. Scene editor

1. typescript: `BrepExportCache`, `ClipSpaceLineProjector`,
   `ProjectedLineLayer`, `ProjectedLinesViewBuilder`, `ProjectedPolygon`,
   `ProjectedSurfacesViewBuilder`, and the canvas 2D renderers of stage 5;
   `RenderTechnology` with a canvas 2D technology next to WebGL; the
   hidden lines and hidden surfaces render modes in the popup and in the
   MCP protocol (`hidden_lines`, `hidden_surfaces`, `withShadows`).
2. typescript: split the MCP protocol class into `MCPJson`,
   `MCPSceneTools`, `MCPViewportTools` and `MCPSceneSerializer`, as in
   Java; port `GeometryCreationCommandsExecutor` and the classes of
   `application/commands`, `SceneHierarchyPresenter`,
   `SceneSelectionOperation`.
3. turboc: `MCPSceneSerializer`, `RenderTechnology`, and the line view
   builder classes extracted from its Xlib renderer.
4. cpp and turboc: read the "by structure" rows of section 5 and either
   confirm the equivalence in a comment of the Xt class, or rename and
   split to match the Java structure.

### Stage 8. Offline examples

`PolyhedralBoundedSolidExpotImportExample` and `KurlanderBowlBuilder` in
the three ports (after the STEP writer of stage 3), `ImageOfflineExample`
in TypeScript. Their outputs join the reference comparisons.

### Stage 9. Keep it closed

Turn the comparison of section 1 into a script
(`scripts/checkPortParity.py`) with an allow list of the decided "Java
only" items, run with the other host checks. It is the parity matrix of
stage I of [cleanupPlan.md](cleanupPlan.md), and it replaces the
hand-written feature table of the README.

## 8. Order

Stage 1 first: it shrinks every other stage. Stages 2 and 3 are
independent and can run in parallel per port. Stage 5 needs nothing and
unblocks stage 7 for TypeScript. Stage 6 step 1 needs MD2 of stage 3.
Stage 4 can wait for a consumer (the widgets are used by Java
applications that the ports do not have). Stage 9 as soon as stage 1 has
produced the allow list.

## 9. Tracking

| Stage | cpp | turboc | typescript | Notes |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |
| 6 | | | | |
| 7 | | | | |
| 8 | | | | |
| 9 | | | | |
