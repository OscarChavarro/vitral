/**
 * Public compatibility surface completed during Phase 1 of the Java-to-
 * TypeScript port.  Additional Vitral domains are exported only after their
 * Java implementations and dependency closure have been ported.
 */
export { Object as JavaObject } from "./java/lang/Object.js";
export { Throwable } from "./java/lang/Throwable.js";
export { Exception } from "./java/lang/Exception.js";
export { RuntimeException } from "./java/lang/RuntimeException.js";
export { IllegalArgumentException } from "./java/lang/IllegalArgumentException.js";
export { IllegalStateException } from "./java/lang/IllegalStateException.js";
export { IndexOutOfBoundsException } from "./java/lang/IndexOutOfBoundsException.js";
export { ArithmeticException } from "./java/lang/ArithmeticException.js";
export { UnsupportedOperationException } from "./java/lang/UnsupportedOperationException.js";
export { CloneNotSupportedException } from "./java/lang/CloneNotSupportedException.js";
export { String as JavaString } from "./java/lang/String.js";
export { StringBuilder } from "./java/lang/StringBuilder.js";
export { Integer } from "./java/lang/Integer.js";
export { Long } from "./java/lang/Long.js";
export { Double } from "./java/lang/Double.js";
export { Float } from "./java/lang/Float.js";
export { Byte } from "./java/lang/Byte.js";
export { Short } from "./java/lang/Short.js";
export { Character } from "./java/lang/Character.js";
export { Math as JavaMath } from "./java/lang/Math.js";
export { Random } from "./java/util/Random.js";
export { ArrayList } from "./java/util/ArrayList.js";
export { type Iterator } from "./java/util/Iterator.js";
export { HashMap } from "./java/util/HashMap.js";
export { type MapEntry } from "./java/util/HashMap.js";
export { StringTokenizer } from "./java/util/StringTokenizer.js";
export { Date as JavaDate } from "./java/util/Date.js";
export { HashSet } from "./java/util/HashSet.js";
export { LinkedHashSet } from "./java/util/LinkedHashSet.js";
export { LinkedHashMap } from "./java/util/LinkedHashMap.js";
export { Stack } from "./java/util/Stack.js";
export { AtomicLong } from "./java/util/concurrent/atomic/AtomicLong.js";
export { LongAdder } from "./java/util/concurrent/atomic/LongAdder.js";
export { ConcurrentLinkedQueue } from "./java/util/concurrent/ConcurrentLinkedQueue.js";
export { CompletionStage } from "./java/util/concurrent/CompletionStage.js";
export { InputStream } from "./java/io/InputStream.js";
export { OutputStream } from "./java/io/OutputStream.js";
export { BufferedInputStream } from "./java/io/BufferedInputStream.js";
export { ByteArrayInputStream } from "./java/io/ByteArrayInputStream.js";
export { ByteArrayOutputStream } from "./java/io/ByteArrayOutputStream.js";
export { IOException } from "./java/io/IOException.js";
export { Reader } from "./java/io/Reader.js";
export { StringReader } from "./java/io/StringReader.js";
export { InputStreamReader } from "./java/io/InputStreamReader.js";
export { BufferedReader } from "./java/io/BufferedReader.js";
export { BufferedOutputStream } from "./java/io/BufferedOutputStream.js";
export { DataInputStream } from "./java/io/DataInputStream.js";
export { DataOutputStream } from "./java/io/DataOutputStream.js";
export { ByteBuffer, type ByteOrder } from "./java/nio/ByteBuffer.js";
export { DecimalFormat } from "./java/text/DecimalFormat.js";
export { FieldPosition } from "./java/text/FieldPosition.js";
export { SimpleDateFormat } from "./java/text/SimpleDateFormat.js";
export { ThreadLocal } from "./java/lang/ThreadLocal.js";
export { Thread } from "./java/lang/Thread.js";
export { Boolean as JavaBoolean } from "./java/lang/Boolean.js";
export { StringBuffer } from "./java/lang/StringBuffer.js";
export { StackTraceElement } from "./java/lang/StackTraceElement.js";
export { Class as JavaClass } from "./java/lang/Class.js";
export { ClassLoader } from "./java/lang/ClassLoader.js";
export { type Comparable } from "./java/lang/Comparable.js";
export { type CharSequence } from "./java/lang/CharSequence.js";
export { type Runnable } from "./java/lang/Runnable.js";
export { type Serializable } from "./java/lang/Serializable.js";
export { type Method } from "./java/lang/reflect/Method.js";
export { ObjectInputStream } from "./java/io/ObjectInputStream.js";
export { ObjectOutputStream } from "./java/io/ObjectOutputStream.js";
export { StreamTokenizer } from "./java/io/StreamTokenizer.js";
export { GZIPInputStream } from "./java/util/zip/GZIPInputStream.js";
export { Pattern } from "./java/util/regex/Pattern.js";
export { Matcher } from "./java/util/regex/Matcher.js";
export { HttpClient } from "./java/net/http/HttpClient.js";
export { WebSocket, type Listener as WebSocketListener } from "./java/net/http/WebSocket.js";
export type { Document } from "./org/w3c/dom/Document.js";
export type { Element } from "./org/w3c/dom/Element.js";
export type { Node } from "./org/w3c/dom/Node.js";
export type { NodeList } from "./org/w3c/dom/NodeList.js";
export type { NamedNodeMap } from "./org/w3c/dom/NamedNodeMap.js";
export { BrowserWorkerExecutor } from "./java/concurrent/BrowserWorkerExecutor.js";
export { CooperativeFiberScheduler, type CooperativeFiber } from "./java/concurrent/CooperativeFiberScheduler.js";
export { Entity } from "./vsdk/toolkit/common/Entity.js";
export { FundamentalEntity } from "./vsdk/toolkit/common/FundamentalEntity.js";
export type { ModelElement } from "./vsdk/toolkit/common/ModelElement.js";
export { VSDK } from "./vsdk/toolkit/common/VSDK.js";
export { VSDKException } from "./vsdk/toolkit/common/VSDKException.js";
export { VSDKFatalException } from "./vsdk/toolkit/common/VSDKFatalException.js";
export { PresentationElement } from "./vsdk/toolkit/gui/PresentationElement.js";
export { Complex } from "./vsdk/toolkit/common/linealAlgebra/Complex.js";
export { MatrixDimensionMismatchException } from "./vsdk/toolkit/common/linealAlgebra/exceptions/MatrixDimensionMismatchException.js";
export { MatrixIndexOutOfBoundsException } from "./vsdk/toolkit/common/linealAlgebra/exceptions/MatrixIndexOutOfBoundsException.js";
export { MatrixNotSquareException } from "./vsdk/toolkit/common/linealAlgebra/exceptions/MatrixNotSquareException.js";
export { MatrixSingularException } from "./vsdk/toolkit/common/linealAlgebra/exceptions/MatrixSingularException.js";
export { Vector2Dd } from "./vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";
export { Vector2Df } from "./vsdk/toolkit/common/linealAlgebra/Vector2Df.js";
export { Vector3Dd } from "./vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
export { Vector3Df } from "./vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
export { Vector4Dd } from "./vsdk/toolkit/common/linealAlgebra/Vector4Dd.js";
export { Vector4Df } from "./vsdk/toolkit/common/linealAlgebra/Vector4Df.js";
export { Quaterniond } from "./vsdk/toolkit/common/linealAlgebra/Quaterniond.js";
export { Quaternionf } from "./vsdk/toolkit/common/linealAlgebra/Quaternionf.js";
export { MatrixNxM } from "./vsdk/toolkit/common/linealAlgebra/MatrixNxM.js";
export { Matrix4x4d } from "./vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
export { Matrix4x4f } from "./vsdk/toolkit/common/linealAlgebra/Matrix4x4f.js";
export { AlgebraicExpression } from "./vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.js";
export { AlgebraicExpressionException } from "./vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpressionException.js";
export { _AlgebraicExpressionNode } from "./vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionNode.js";
export { _AlgebraicExpressionConstantNode } from "./vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionConstantNode.js";
export { _AlgebraicExpressionVariableNode } from "./vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionVariableNode.js";
export { _AlgebraicExpressionBinaryOperatorNode } from "./vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionBinaryOperatorNode.js";
export { _AlgebraicExpressionUnaryOperatorNode } from "./vsdk/toolkit/common/symbolicAlgebra/_AlgebraicExpressionUnaryOperatorNode.js";
export { ColorRgb } from "./vsdk/toolkit/common/color/ColorRgb.js";
export { ColorRgba } from "./vsdk/toolkit/common/color/ColorRgba.js";
export { Background } from "./vsdk/toolkit/environment/background/Background.js";
export { CubemapBackground } from "./vsdk/toolkit/environment/background/CubemapBackground.js";
export { FixedBackground } from "./vsdk/toolkit/environment/background/FixedBackground.js";
export { SimpleBackground } from "./vsdk/toolkit/environment/background/SimpleBackground.js";
export { Camera } from "./vsdk/toolkit/environment/camera/Camera.js";
export { CameraSnapshot } from "./vsdk/toolkit/environment/camera/CameraSnapshot.js";
export { Logger } from "./vsdk/toolkit/common/logging/Logger.js";
export { ArrayListOfBytes } from "./vsdk/toolkit/common/dataStructures/ArrayListOfBytes.js";
export { ArrayListOfDoubles } from "./vsdk/toolkit/common/dataStructures/ArrayListOfDoubles.js";
export { ArrayListOfInts } from "./vsdk/toolkit/common/dataStructures/ArrayListOfInts.js";
export { ArrayListOfLongs } from "./vsdk/toolkit/common/dataStructures/ArrayListOfLongs.js";
export { _CircularDoubleLinkedListNode } from "./vsdk/toolkit/common/dataStructures/_CircularDoubleLinkedListNode.js";
export { CircularDoubleLinkedList } from "./vsdk/toolkit/common/dataStructures/CircularDoubleLinkedList.js";
export { _NAryTreeNode } from "./vsdk/toolkit/common/dataStructures/_NAryTreeNode.js";
export { _NAryTreeIntermediateNode } from "./vsdk/toolkit/common/dataStructures/_NAryTreeIntermediateNode.js";
export { _NAryTreeLeafNode } from "./vsdk/toolkit/common/dataStructures/_NAryTreeLeafNode.js";
export { BinaryTreeNode } from "./vsdk/toolkit/common/dataStructures/BinaryTreeNode.js";
export { NAryTree } from "./vsdk/toolkit/common/dataStructures/NAryTree.js";
export { NAryTreeTraverser } from "./vsdk/toolkit/common/dataStructures/NAryTreeTraverser.js";
export { GeometryStatistics } from "./vsdk/toolkit/common/statistics/GeometryStatistics.js";
export { PolyhedralBoundedSolidStatistics } from "./vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.js";
export { RaytraceStatistics } from "./vsdk/toolkit/common/statistics/RaytraceStatistics.js";
export { RenderingStatistics } from "./vsdk/toolkit/common/statistics/RenderingStatistics.js";
export { SolidTextureStatistics } from "./vsdk/toolkit/common/statistics/SolidTextureStatistics.js";
export { ProgressMonitor } from "./vsdk/toolkit/gui/feedback/ProgressMonitor.js";
export { ProgressMonitorConsole } from "./vsdk/toolkit/gui/feedback/ProgressMonitorConsole.js";
export { ProgressMonitorConsoleLongFormat } from "./vsdk/toolkit/gui/feedback/ProgressMonitorConsoleLongFormat.js";
export { ProgressMonitorInRam } from "./vsdk/toolkit/gui/feedback/ProgressMonitorInRam.js";
export { TangibleInterfaceEvent } from "./vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.js";
export type { TangibleInterfaceListener } from "./vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceListener.js";
export {
    TangibleInterfaceNetworkClient,
    FrameListener as TangibleInterfaceNetworkClientFrameListener,
} from "./vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.js";
export { MediaEntity } from "./vsdk/toolkit/media/MediaEntity.js";
export { Calligraphic2DBuffer } from "./vsdk/toolkit/media/Calligraphic2DBuffer.js";
export { FourierShapeDescriptor } from "./vsdk/toolkit/media/FourierShapeDescriptor.js";
export { GeometryMetadata } from "./vsdk/toolkit/media/GeometryMetadata.js";
export { GrayScalePalette } from "./vsdk/toolkit/media/GrayScalePalette.js";
export { Image } from "./vsdk/toolkit/media/Image.js";
export { IndexedColorImageUncompressed } from "./vsdk/toolkit/media/IndexedColorImageUncompressed.js";
export { NormalMap } from "./vsdk/toolkit/media/NormalMap.js";
export { PrimitiveCountShapeDescriptor } from "./vsdk/toolkit/media/PrimitiveCountShapeDescriptor.js";
export { RGBAImageCompressed } from "./vsdk/toolkit/media/RGBAImageCompressed.js";
export { RGBAImageUncompressed } from "./vsdk/toolkit/media/RGBAImageUncompressed.js";
export { RGBAPixel } from "./vsdk/toolkit/media/RGBAPixel.js";
export { RGBColorPalette } from "./vsdk/toolkit/media/RGBColorPalette.js";
export { RGBImageUncompressed } from "./vsdk/toolkit/media/RGBImageUncompressed.js";
export { RGBPixel } from "./vsdk/toolkit/media/RGBPixel.js";
export { RGBProceduralColorPalette } from "./vsdk/toolkit/media/RGBProceduralColorPalette.js";
export { ShapeDescriptor } from "./vsdk/toolkit/media/ShapeDescriptor.js";
export { ZBuffer } from "./vsdk/toolkit/media/ZBuffer.js";
export { RGBAImageHDRUncompressed } from "./vsdk/toolkit/media/RGBAImageHDRUncompressed.js";
export { RGBAPixelHDR } from "./vsdk/toolkit/media/RGBAPixelHDR.js";
export { ProcessingElement } from "./vsdk/toolkit/processing/ProcessingElement.js";
export { Containment } from "./vsdk/toolkit/processing/Containment.js";
export { StopWatch } from "./vsdk/toolkit/processing/StopWatch.js";
export { SignalProcessing } from "./vsdk/toolkit/processing/SignalProcessing.js";
export { ComputationalGeometry, ClippedLine2DResult } from "./vsdk/toolkit/processing/ComputationalGeometry.js";
export { ImageProcessing } from "./vsdk/toolkit/processing/ImageProcessing.js";
export type { Material } from "./vsdk/toolkit/environment/material/Material.js";
export { SimpleMaterial } from "./vsdk/toolkit/environment/material/SimpleMaterial.js";
export { MicroFacetedMaterial, MicrofacetConfig } from "./vsdk/toolkit/environment/material/MicroFacetedMaterial.js";
export { RendererConfiguration } from "./vsdk/toolkit/environment/material/RendererConfiguration.js";
export { ShadingType } from "./vsdk/toolkit/environment/material/ShadingType.js";
export { Geometry, type GeometryRay, type GeometryRayHit } from "./vsdk/toolkit/environment/geometry/Geometry.js";
export { Curve } from "./vsdk/toolkit/environment/geometry/curve/Curve.js";
export { ParametricCurve } from "./vsdk/toolkit/environment/geometry/curve/ParametricCurve.js";
export { Intersection } from "./vsdk/toolkit/environment/geometry/element/Intersection.js";
export { Ray } from "./vsdk/toolkit/environment/geometry/element/Ray.js";
export { RayHit } from "./vsdk/toolkit/environment/geometry/element/RayHit.js";
export { Triangle } from "./vsdk/toolkit/environment/geometry/element/Triangle.js";
export { Vertex } from "./vsdk/toolkit/environment/geometry/element/Vertex.js";
export { Vertex2D } from "./vsdk/toolkit/environment/geometry/element/Vertex2D.js";
export { Surface } from "./vsdk/toolkit/environment/geometry/surface/Surface.js";
export { HalfSpace } from "./vsdk/toolkit/environment/geometry/surface/HalfSpace.js";
export { InfinitePlane } from "./vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
export { ParametricBiCubicPatch } from "./vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.js";
export { QuadMesh } from "./vsdk/toolkit/environment/geometry/surface/QuadMesh.js";
export { TriangleMesh } from "./vsdk/toolkit/environment/geometry/surface/TriangleMesh.js";
export { FunctionalExplicitSurface } from "./vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.js";
export { Md2Mesh } from "./vsdk/toolkit/environment/geometry/surface/Md2Mesh.js";
export { Volume } from "./vsdk/toolkit/environment/geometry/volume/Volume.js";
export { Solid } from "./vsdk/toolkit/environment/geometry/volume/Solid.js";
export { Sphere } from "./vsdk/toolkit/environment/geometry/volume/Sphere.js";
export { Box } from "./vsdk/toolkit/environment/geometry/volume/Box.js";
export { VoxelVolume } from "./vsdk/toolkit/environment/geometry/volume/VoxelVolume.js";
export { Cone } from "./vsdk/toolkit/environment/geometry/volume/Cone.js";
export { Torus } from "./vsdk/toolkit/environment/geometry/volume/Torus.js";
export { Arrow } from "./vsdk/toolkit/environment/geometry/volume/Arrow.js";
export {
    PolyhedralBoundedSolidNumericPolicy,
    ToleranceContext,
} from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
export type { _PolyhedralBoundedSolidValidationStrategy } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidValidationStrategy.js";
export { _PolyhedralBoundedSolidVertex } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
export { _PolyhedralBoundedSolidEdge } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
export { _PolyhedralBoundedSolidLoop } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
export { _PolyhedralBoundedSolidHalfEdge } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
export {
    _PolyhedralBoundedSolidFace,
    PointInsideResult,
} from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
export { PolyhedralBoundedSolid } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
export { PolyhedralBoundedSolidEulerOperators } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
export { PolyhedralBoundedSolidTopologyEditing } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
export { _TopologicalIntegrityStrategy } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_TopologicalIntegrityStrategy.js";
export { PolyhedralBoundedSolidGeometricValidator } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
export { _GeometricPlanarityStrategy } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricPlanarityStrategy.js";
export { _GeometricFaceOrientationStrategy } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricFaceOrientationStrategy.js";
export { _GeometricStrictLoopsStrategy } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricStrictLoopsStrategy.js";
export { _GeometricStrictFaceIntersectionsStrategy } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricStrictFaceIntersectionsStrategy.js";
export { _PolyhedralBoundedSolidTopologicalValidator } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidTopologicalValidator.js";
export { _PolyhedralBoundedSolidBooleanTopologyPredicates } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidBooleanTopologyPredicates.js";
export { PolyhedralBoundedSolidValidationEngine } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
export { PolyhedralBoundedSolidPredicates } from "./vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.js";
export { TriangleStripMesh } from "./vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.js";
export { _AnimationInfo } from "./vsdk/toolkit/environment/geometry/surface/_AnimationInfo.js";
export {
    TriangleMeshGroup,
    type TriangleMeshLike,
} from "./vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.js";
export { _Polygon2DContour } from "./vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.js";
export { Polygon2D } from "./vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.js";
import "./vsdk/toolkit/environment/material/RendererConfigurationBehavior.js";
export {
    workerError,
    createWorkerMessageHandler,
    type WorkerExecutionOptions,
    type WorkerExecutor,
    type WorkerFailure,
    type WorkerRequest,
    type WorkerResponse,
    type WorkerSuccess,
    type WorkerTransferValue,
} from "./java/concurrent/WorkerProtocol.js";
