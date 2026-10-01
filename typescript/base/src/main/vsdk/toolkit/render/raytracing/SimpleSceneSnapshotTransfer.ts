import { ObjectGraphTransfer, type TransferableClass } from "../../../../java/concurrent/ObjectGraphTransfer.js";
import type { WorkerTransferValue } from "../../../../java/concurrent/WorkerProtocol.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { Collections } from "../../../../java/util/Collections.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { CircularDoubleLinkedList } from "../../common/dataStructures/CircularDoubleLinkedList.js";
import { _CircularDoubleLinkedListNode } from "../../common/dataStructures/_CircularDoubleLinkedListNode.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { CubemapBackground } from "../../environment/background/CubemapBackground.js";
import { FixedBackground } from "../../environment/background/FixedBackground.js";
import { SimpleBackground } from "../../environment/background/SimpleBackground.js";
import { Camera } from "../../environment/camera/Camera.js";
import { CameraSnapshot } from "../../environment/camera/CameraSnapshot.js";
import { ParametricCurve } from "../../environment/geometry/curve/ParametricCurve.js";
import { Triangle } from "../../environment/geometry/element/Triangle.js";
import { Vertex } from "../../environment/geometry/element/Vertex.js";
import { FunctionalExplicitSurface } from "../../environment/geometry/surface/FunctionalExplicitSurface.js";
import { InfinitePlane } from "../../environment/geometry/surface/InfinitePlane.js";
import { ParametricBiCubicPatch } from "../../environment/geometry/surface/ParametricBiCubicPatch.js";
import { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";
import { TriangleMeshGroup } from "../../environment/geometry/surface/TriangleMeshGroup.js";
import { Arrow } from "../../environment/geometry/volume/Arrow.js";
import { Box } from "../../environment/geometry/volume/Box.js";
import { Cone } from "../../environment/geometry/volume/Cone.js";
import { Sphere } from "../../environment/geometry/volume/Sphere.js";
import { Torus } from "../../environment/geometry/volume/Torus.js";
import { VoxelVolume } from "../../environment/geometry/volume/VoxelVolume.js";
import { PolyhedralBoundedSolid } from "../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { AmbientLight } from "../../environment/light/AmbientLight.js";
import { DirectionalLight } from "../../environment/light/DirectionalLight.js";
import { PointLight } from "../../environment/light/PointLight.js";
import { SpotLight } from "../../environment/light/SpotLight.js";
import { MicroFacetedMaterial } from "../../environment/material/MicroFacetedMaterial.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { SimpleSceneSnapshot } from "../../environment/scene/SimpleSceneSnapshot.js";
import { IndexedColorImageUncompressed } from "../../media/IndexedColorImageUncompressed.js";
import { NormalMap } from "../../media/NormalMap.js";
import { RGBAImageCompressed } from "../../media/RGBAImageCompressed.js";
import { RGBAImageUncompressed } from "../../media/RGBAImageUncompressed.js";
import { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import { _AlgebraicExpressionVariableNode } from "../../common/symbolicAlgebra/_AlgebraicExpressionVariableNode.js";
import { _AlgebraicExpressionConstantNode } from "../../common/symbolicAlgebra/_AlgebraicExpressionConstantNode.js";
import { RGBColorPalette } from "../../media/RGBColorPalette.js";
import { _AlgebraicExpressionBinaryOperatorNode } from "../../common/symbolicAlgebra/_AlgebraicExpressionBinaryOperatorNode.js";
import { _AlgebraicExpressionUnaryOperatorNode } from "../../common/symbolicAlgebra/_AlgebraicExpressionUnaryOperatorNode.js";
import { AlgebraicExpression } from "../../common/symbolicAlgebra/AlgebraicExpression.js";
import { Quaterniond } from "../../common/linealAlgebra/Quaterniond.js";

/**
Carries a `SimpleSceneSnapshot` to the workers of a `ParallelRaytracer`, which
rebuild it with `decodeSimpleSceneSnapshot` (see `ObjectGraphTransfer`, and
`ParallelRaytracer` about why a worker can not use the owner's snapshot).

The list below names the classes of everything a scene of the toolkit can
hold: bodies with their geometries (and the data structures inside them),
materials, images, lights, backgrounds and the camera snapshot. Both sides
build the same list from this module, so the indexes agree. The listeners of
an `Entity` (a Java `transient` field, pointing to objects of the GUI) are not
carried.
*/
let transfer: ObjectGraphTransfer | null = null;

function sceneTransfer(): ObjectGraphTransfer {
    if (transfer === null) {
        const unmodifiableList: TransferableClass =
            (Collections.unmodifiableList(new ArrayList<unknown>()) as object).constructor as TransferableClass;
        const classes: TransferableClass[] = [
            // java.util
            ArrayList,
            unmodifiableList,
            // Scene
            SimpleSceneSnapshot,
            SimpleBody,
            CameraSnapshot,
            Camera,
            // Linear algebra and colors
            Vector3Dd,
            Matrix4x4d,
            ColorRgb,
            // Materials and media
            SimpleMaterial,
            MicroFacetedMaterial,
            RGBImageUncompressed,
            RGBAImageUncompressed,
            RGBAImageCompressed,
            IndexedColorImageUncompressed,
            NormalMap,
            // Lights and backgrounds
            AmbientLight,
            DirectionalLight,
            PointLight,
            SpotLight,
            SimpleBackground,
            FixedBackground,
            CubemapBackground,
            // Geometries
            Sphere,
            Cone,
            Arrow,
            Box,
            Torus,
            VoxelVolume,
            InfinitePlane,
            FunctionalExplicitSurface,
            ParametricBiCubicPatch,
            ParametricCurve,
            TriangleMesh,
            TriangleMeshGroup,
            Triangle,
            Vertex,
            PolyhedralBoundedSolid,
            _PolyhedralBoundedSolidFace,
            _PolyhedralBoundedSolidLoop,
            _PolyhedralBoundedSolidHalfEdge,
            _PolyhedralBoundedSolidEdge,
            _PolyhedralBoundedSolidVertex,
            CircularDoubleLinkedList,
            _CircularDoubleLinkedListNode,
            _AlgebraicExpressionVariableNode,
            _AlgebraicExpressionConstantNode,
            RGBColorPalette,
            _AlgebraicExpressionBinaryOperatorNode,
            _AlgebraicExpressionUnaryOperatorNode,
            AlgebraicExpression,
            Quaterniond,
        ];
        transfer = new ObjectGraphTransfer(classes, new Set<string>(["entityListeners"]));
    }
    return transfer;
}

/**
@param snapshot scene to carry to a worker
@return the scene descriptor of a `ParallelRaytracer.execute`
*/
export function encodeSimpleSceneSnapshot(snapshot: SimpleSceneSnapshot): WorkerTransferValue {
    return sceneTransfer().encode(snapshot);
}

/**
@param descriptor what `encodeSimpleSceneSnapshot` gave
@return the rebuilt scene
*/
export function decodeSimpleSceneSnapshot(descriptor: WorkerTransferValue): SimpleSceneSnapshot {
    const snapshot: unknown = sceneTransfer().decode(descriptor);
    if (!(snapshot instanceof SimpleSceneSnapshot)) {
        throw new Error("The scene descriptor does not hold a SimpleSceneSnapshot");
    }
    return snapshot;
}
