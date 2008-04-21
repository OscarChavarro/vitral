===========================================================================
To build this J2ME application you need:
  - A J2SDK 1.5 or newer
  - Sun Microsystems Wireless Toolkit 2.5.2 or newer
  - Apache Ant 1.7.0 or newer
  - J2MEPolish 2.0.2 or newer

After installing that things, copy the following files from the core VitralSDK
source tree:

./vsdk/toolkit/media/Image.java
./vsdk/toolkit/media/RGBImage.java
./vsdk/toolkit/media/RGBPixel.java
./vsdk/toolkit/media/Calligraphic2DBuffer.java
./vsdk/toolkit/media/FourierShapeDescriptor.java
./vsdk/toolkit/media/GeometryMetadata.java
./vsdk/toolkit/media/GrayScalePalette.java
./vsdk/toolkit/media/IndexedColorImage.java
./vsdk/toolkit/media/MediaEntity.java
./vsdk/toolkit/media/NormalMap.java
./vsdk/toolkit/media/PrimitiveCountShapeDescriptor.java
./vsdk/toolkit/media/RGBAImage.java
./vsdk/toolkit/media/RGBAPixel.java
./vsdk/toolkit/media/RGBColorPalette.java
./vsdk/toolkit/media/RGBProceduralColorPalette.java
./vsdk/toolkit/media/ShapeDescriptor.java
./vsdk/toolkit/media/ZBuffer.java
./vsdk/toolkit/common/Entity.java
./vsdk/toolkit/common/VSDK.java
./vsdk/toolkit/common/AlgebraicExpression.java
./vsdk/toolkit/common/Vector3D.java
./vsdk/toolkit/common/ColorRgb.java
./vsdk/toolkit/common/FundamentalEntity.java
./vsdk/toolkit/common/_AlgebraicExpressionConstantNode.java
./vsdk/toolkit/common/VSDKJ2ME.java
./vsdk/toolkit/common/_AlgebraicExpressionBinaryOperatorNode.java
./vsdk/toolkit/common/AlgebraicExpressionException.java
./vsdk/toolkit/common/_AlgebraicExpressionNode.java
./vsdk/toolkit/common/_AlgebraicExpressionUnaryOperatorNode.java
./vsdk/toolkit/common/_AlgebraicExpressionVariableNode.java
./vsdk/toolkit/common/ArrayListOfDoubles.java
./vsdk/toolkit/common/CircularDoubleLinkedList.java
./vsdk/toolkit/common/Complex.java
./vsdk/toolkit/common/Matrix4x4.java
./vsdk/toolkit/common/MatrixNxM.java
./vsdk/toolkit/common/Quaternion.java
./vsdk/toolkit/common/Ray.java
./vsdk/toolkit/common/RendererConfiguration.java
./vsdk/toolkit/common/Triangle.java
./vsdk/toolkit/common/Vector4D.java
./vsdk/toolkit/common/Vertex.java
./vsdk/toolkit/common/VSDKException.java
./vsdk/toolkit/render/j2me/J2meRGBImageRenderer.java
./vsdk/toolkit/render/j2me/J2meCalligraphic2DBufferRenderer.java
./vsdk/toolkit/render/Raytracer.java
./vsdk/toolkit/render/RenderingElement.java
./vsdk/toolkit/render/WireframeRenderer.java
./vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/_PolyhedralBoundedSolidEdge.java
./vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/_PolyhedralBoundedSolidLoop.java
./vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/_PolyhedralBoundedSolidVertex.java
./vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/_PolyhedralBoundedSolidFace.java
./vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/_PolyhedralBoundedSolidHalfEdge.java
./vsdk/toolkit/environment/geometry/PolyhedralBoundedSolid.java
./vsdk/toolkit/environment/geometry/TriangleMeshGroup.java
./vsdk/toolkit/environment/geometry/TriangleStripMesh.java
./vsdk/toolkit/environment/geometry/Solid.java
./vsdk/toolkit/environment/geometry/TriangleMesh.java
./vsdk/toolkit/environment/geometry/Curve.java
./vsdk/toolkit/environment/geometry/Cone.java
./vsdk/toolkit/environment/geometry/VoxelVolume.java
./vsdk/toolkit/environment/geometry/InfinitePlane.java
./vsdk/toolkit/environment/geometry/Sphere.java
./vsdk/toolkit/environment/geometry/HalfSpace.java
./vsdk/toolkit/environment/geometry/GeometryIntersectionInformation.java
./vsdk/toolkit/environment/geometry/Box.java
./vsdk/toolkit/environment/geometry/ParametricBiCubicPatch.java
./vsdk/toolkit/environment/geometry/Surface.java
./vsdk/toolkit/environment/geometry/Arrow.java
./vsdk/toolkit/environment/geometry/Geometry.java
./vsdk/toolkit/environment/geometry/ParametricCurve.java
./vsdk/toolkit/environment/geometry/FunctionalExplicitSurface.java
./vsdk/toolkit/environment/scene/SimpleBodyGroup.java
./vsdk/toolkit/environment/scene/SimpleBody.java
./vsdk/toolkit/environment/scene/SimpleScene.java
./vsdk/toolkit/environment/FixedBackground.java
./vsdk/toolkit/environment/Background.java
./vsdk/toolkit/environment/Light.java
./vsdk/toolkit/environment/Camera.java
./vsdk/toolkit/environment/SimpleBackground.java
./vsdk/toolkit/environment/CubemapBackground.java
./vsdk/toolkit/environment/Material.java
./vsdk/toolkit/gui/PresentationElement.java
./vsdk/toolkit/gui/ProgressMonitor.java
./vsdk/toolkit/gui/CameraController.java
./vsdk/toolkit/gui/CameraControllerAquynza.java
./vsdk/toolkit/gui/J2meSystem.java
./vsdk/toolkit/gui/KeyEvent.java
./vsdk/toolkit/gui/Controller.java
./vsdk/toolkit/processing/ProcessingElement.java
./vsdk/toolkit/processing/ComputationalGeometry.java

make the changes described in the following section of this document
and compile

===========================================================================
Comments relating to main basic distribution of Vitral SDK for supporting
J2ME CLDC 1.1 / MIDP 2.0 profiles using J2MEPolish (2.0.2):

To compile VitralSDK on mobile devices, some minor changes should be made.
Following each of the change, and the reasons why they had to be made:
  - Swing, Awt and JOGL are not supported on J2ME. Do not include any
    classes from vitral that uses those APIs.
  - Serializable not supported. Edit class Entity and make not to inherit
    from Serializable.
    . Entity, in theory one can change java.io.Serializable for
      de.enough.polish.io.Serializable, but that is failing reporting
      a "stack too large" in preverification step
  - DecimalFormat and FieldPosition not supported, remove from classes
    . VSDK
    . PolyhedralBoundedSolid
  - System.out / System.err not available, remove from classes
    . VSDK
    . ParametricBicubicPatch
  - Floating point math is supported, but some functions are not available
    on "Math" class. Instead, used corresponding methods from the
    "VSDKJ2ME" class. Check for methods Math.PI, Math.asin, Math.acos,
    Math.atan, Math.sinh, Math.cosh, Math.tanh, Math.atan2, Math.hypot,
    Math.log, Math.log10, Math.exp, Math.cbrt and Math.pow.
    Replace on:
    . Vector3D
    . Matrix4x4
    . _AlgebraicExpressionUnaryOperatorNode
    . GeometryMetadata
    . Sphere
  - Change java.lang.ArrayList for de.enough.polish.util.ArrayList:
    . <Use of a replacing macro recommended>
  - Typed java.lang.HashMap not supported, de.enough.polish.util.HashMap?
    . AlgebraicExpression
  - StreamTokenizer, StringReader not supported:
    . AlgebraicExpression
  - Can not call super() ... 
    . VSDKException
  - J2ME / J2MEPolish does not support the use of destructors.  So, all
    "finalize" methods should be commented out and they only should called a
    "destroy" method, so user can call destructors explicity when not supported
    by platform/profile
    . Calligraphic2DBuffer
    . FourierShapeDescriptor
    . GeometryMetadata
    . IndexedColorImage
    . PrimitiveCountShapeDescriptor
    . RGBAImage
    . RGBImage
    . SimpleBodyGroup
    . SimpleBody
  - Collections class not supported. Quick sort not available!
    . HiddenLineRenderer

After this changes, application should work :)

===========================================================================
Comments relating to main basic distribution of Vitral SDK for supporting
J2ME CLDC 1.0 / MIDP 2.0 profiles:

Bad things that make Vitral SDK incompatible with J2ME
  - Comparisons between double not supported:
    . Image.drawLine
	. RGBColorPalette.evalNearest
	. RGBColorPalette.evalLinear -> IndexedColorImage.getPixelRgb
	. RGBColorPalette.selectNearestIndexToRgb -> IndexedColorImage.putPixelRgb
  - Converting double to String kills program!

Things that can be managed, cutting little functionalities
  - Entity.java: Serializable not supported.
  - Math.floor not supoorted:
    . Image.getColorRgbNearest
	. Image.getColorRgbBiLinear
  
OTHER PROBLEMS MAY PERSIST, BUT THERE WAS NOT TESTED
  
BUILD FAILED
C:\build.xml:64: Unable to convert floating point classes: 
java.lang.UnsupportedOperationException: The opcodes DCMPG, DCMPL, FCMPG and FCMPL are not supported.

===========================================================================
