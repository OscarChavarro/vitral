import {
    Camera,
    ColorRgb,
    HiddenLineRenderer,
    Matrix4x4d,
    RendererConfiguration,
    SimpleMaterial,
    VSDK,
    Vector3Dd,
    type InfinitePlane,
    type PolyhedralBoundedSolid,
    _PolyhedralBoundedSolidFaceValidator,
    type _PolyhedralBoundedSolidEdge,
    type _PolyhedralBoundedSolidFace,
    type _PolyhedralBoundedSolidHalfEdge,
    type _PolyhedralBoundedSolidLoop,
    type _PolyhedralBoundedSolidVertex,
} from "@vitral/base";
import { WebGLLineRenderer } from "../WebGLLineRenderer.js";
import { WebGLRendererConfigurationShaderSelector } from "../WebGLRendererConfigurationShaderSelector.js";
import { WebGLSimpleMaterialRenderer } from "../WebGLSimpleMaterialRenderer.js";
import { WebGLPolyhedralBoundedSolidRenderer } from "./WebGLPolyhedralBoundedSolidRenderer.js";

/** Java's private nested `DebugLines`. */
class DebugLines {
    public constructor(
        public readonly positions: Float32Array,
        public readonly colors: Float32Array,
        public readonly pointPositions: Float32Array = new Float32Array(0),
        public readonly pointColors: Float32Array = new Float32Array(0),
    ) {}
}

/**
Port of
`vsdk.toolkit.render.jogl.polyhedralBoundedSolid.Jogl4PolyhedralBoundedSolidDebugRenderer`.

Every overlay is the Java one, built from the same half-edge walks with the same
colours, widths and depth biases: the edge wires with each edge's debug colour,
the vertex point cloud, the per-vertex face normals, the bounding box and its
selection corners, the yellow outline of every degenerate face, the boundary of
the selected face (or of every face) drawn as curved arrows — counter-clockwise
for the outer loop, reversed for the inner ones — the selected face filled in
red, and the edge-visibility debug view, which colours each edge by how its two
faces face the camera and, for a single selected edge, adds the two face
normals and twenty quantitative-invisibility samples along it.

The runtime boundaries are the ones `WebGLPolyhedralBoundedSolidRenderer`
records: asynchronous drawing because a shader source arrives over `fetch`,
and the active material read for the context being drawn into. Java's
`List<Float>` accumulators, which become `float[]` before upload, are `number[]`
accumulators that become `Float32Array`s, which store the same single-precision
values.
*/
export class WebGLPolyhedralBoundedSolidDebugRenderer {
    private static readonly NORMAL_LINE_LENGTH = Math.fround(0.2);
    private static readonly WIREFRAME_DEPTH_BIAS = -1.0e-4;
    private static readonly POINTS_DEPTH_BIAS = -2.0e-4;
    private static readonly HIGHLIGHT_DEPTH_BIAS = -3.0e-4;
    private static readonly EDGE_ARROW_SIZE = 0.5;
    private static readonly EDGE_ARROW_CURVE_OFFSET = 0.1;
    private static readonly CURVED_ARROW_SEGMENTS = 10;

    private constructor() {}

    public static async drawDebugOverlays(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        _camera: Camera | null,
        quality: RendererConfiguration | null,
        modelViewProjection: Matrix4x4d | null,
    ): Promise<void> {
        if (gl === null || solid === null || quality === null || modelViewProjection === null) {
            return;
        }

        if (quality.isWiresSet()) {
            const lines: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildSolidEdgeLines(solid);
            if (lines.positions.length > 0) {
                await WebGLLineRenderer.drawLines(
                    gl,
                    modelViewProjection,
                    lines.positions,
                    lines.colors,
                    1.0,
                    WebGLPolyhedralBoundedSolidDebugRenderer.WIREFRAME_DEPTH_BIAS,
                );
            }
        }

        if (quality.isPointsSet()) {
            const points: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildSolidPointCloud(solid);
            if (points.pointPositions.length > 0) {
                await WebGLPolyhedralBoundedSolidRenderer.drawColoredPrimitives(
                    gl,
                    modelViewProjection,
                    points.pointPositions,
                    points.pointColors,
                    gl.POINTS,
                    6.0,
                    WebGLPolyhedralBoundedSolidDebugRenderer.POINTS_DEPTH_BIAS,
                );
            }
        }

        if (quality.isNormalsSet()) {
            const normals: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildVertexNormalLines(solid);
            if (normals.positions.length > 0) {
                await WebGLLineRenderer.drawLines(
                    gl,
                    modelViewProjection,
                    normals.positions,
                    normals.colors,
                    1.0,
                    WebGLPolyhedralBoundedSolidDebugRenderer.HIGHLIGHT_DEPTH_BIAS,
                );
            }
        }

        if (quality.isBoundingVolumeSet()) {
            const bounds: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildBoundingVolumeLines(
                solid,
                quality,
            );
            if (bounds.positions.length > 0) {
                await WebGLLineRenderer.drawLines(
                    gl,
                    modelViewProjection,
                    bounds.positions,
                    bounds.colors,
                    1.0,
                    WebGLPolyhedralBoundedSolidDebugRenderer.HIGHLIGHT_DEPTH_BIAS,
                );
            }
        }

        if (quality.isSelectionCornersSet()) {
            const corners: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildSelectionCornerLines(solid);
            if (corners.positions.length > 0) {
                await WebGLLineRenderer.drawLines(
                    gl,
                    modelViewProjection,
                    corners.positions,
                    corners.colors,
                    1.0,
                    WebGLPolyhedralBoundedSolidDebugRenderer.HIGHLIGHT_DEPTH_BIAS,
                );
            }
        }

        const nonPlanar: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildNonPlanarFaceHighlights(solid);
        if (nonPlanar.positions.length > 0) {
            await WebGLLineRenderer.drawLines(
                gl,
                modelViewProjection,
                nonPlanar.positions,
                nonPlanar.colors,
                4.0,
                WebGLPolyhedralBoundedSolidDebugRenderer.HIGHLIGHT_DEPTH_BIAS,
            );
        }
    }

    public static async drawDebugFaceBoundary(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        faceIndex: number,
        modelViewProjection: Matrix4x4d | null,
    ): Promise<void> {
        if (gl === null || solid === null || modelViewProjection === null) {
            return;
        }
        const lines: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildFaceBoundaryLines(solid, faceIndex);
        if (lines.positions.length === 0) {
            return;
        }
        await WebGLLineRenderer.drawLines(
            gl,
            modelViewProjection,
            lines.positions,
            lines.colors,
            2.0,
            WebGLPolyhedralBoundedSolidDebugRenderer.HIGHLIGHT_DEPTH_BIAS,
        );
    }

    public static async drawDebugFace(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        faceIndex: number,
        modelMatrix: Matrix4x4d | null,
        modelViewProjection: Matrix4x4d | null,
        camera: Camera | null,
    ): Promise<void> {
        if (gl === null || solid === null || faceIndex < 0 || modelMatrix === null || modelViewProjection === null) {
            return;
        }

        await WebGLPolyhedralBoundedSolidRenderer.ensureInitialized(gl);

        const modelViewITLocal: Matrix4x4d = modelMatrix.invert().transpose();
        const material: SimpleMaterial = WebGLSimpleMaterialRenderer.getActiveMaterial(gl)
            .withDiffuse(new ColorRgb(1.0, 0.0, 0.0))
            .withAmbient(new ColorRgb(1.0, 0.0, 0.0))
            .withSpecular(new ColorRgb(0.0, 0.0, 0.0));

        const mesh: WebGLPolyhedralBoundedSolidRenderer.MeshData = WebGLPolyhedralBoundedSolidRenderer.buildFaceMesh(
            solid,
            faceIndex,
        );
        if (mesh.vertexCount === 0) {
            return;
        }

        const quality = new RendererConfiguration();
        quality.setTexture(false);
        quality.setUseVertexColors(false);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

        const programId: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
            gl,
            quality,
            false,
            false,
        );
        WebGLPolyhedralBoundedSolidRenderer.configureSurfaceProgram(
            gl,
            programId,
            modelViewProjection,
            modelMatrix,
            modelViewITLocal,
            material,
            null,
            quality,
            camera !== null ? camera.getPosition() : new Vector3Dd(0, 0, 0),
        );
        await WebGLPolyhedralBoundedSolidRenderer.renderMesh(gl, mesh, gl.TRIANGLES);
        WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
    }

    public static async drawDebugEdges(
        gl: WebGL2RenderingContext | null,
        solid: PolyhedralBoundedSolid | null,
        camera: Camera | null,
        edgeIndex: number,
        modelViewProjection: Matrix4x4d | null,
    ): Promise<void> {
        if (gl === null || solid === null || modelViewProjection === null) {
            return;
        }

        const lines: DebugLines = WebGLPolyhedralBoundedSolidDebugRenderer.buildDebugEdgeLines(
            solid,
            camera,
            edgeIndex,
        );
        if (lines.positions.length > 0) {
            await WebGLLineRenderer.drawLines(
                gl,
                modelViewProjection,
                lines.positions,
                lines.colors,
                2.0,
                WebGLPolyhedralBoundedSolidDebugRenderer.HIGHLIGHT_DEPTH_BIAS,
            );
        }
        if (lines.pointPositions.length > 0) {
            await WebGLPolyhedralBoundedSolidRenderer.drawColoredPrimitives(
                gl,
                modelViewProjection,
                lines.pointPositions,
                lines.pointColors,
                gl.POINTS,
                4.0,
                WebGLPolyhedralBoundedSolidDebugRenderer.HIGHLIGHT_DEPTH_BIAS,
            );
        }
    }

    private static buildSolidEdgeLines(solid: PolyhedralBoundedSolid): DebugLines {
        const positions: number[] = [];
        const colors: number[] = [];

        for (let i = 0; i < solid.getEdgesList().size(); i++) {
            const edge: _PolyhedralBoundedSolidEdge = solid.getEdgesList().get(i)!;
            if (edge.rightHalf === null || edge.leftHalf === null) {
                continue;
            }
            const start: Vector3Dd = edge.rightHalf.startingVertex.position;
            const end: Vector3Dd = edge.leftHalf.startingVertex.position;
            WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, start, end, edge.debugColor);
        }
        return new DebugLines(new Float32Array(positions), new Float32Array(colors));
    }

    private static buildSolidPointCloud(solid: PolyhedralBoundedSolid): DebugLines {
        const positions: number[] = [];
        const colors: number[] = [];
        for (let i = 0; i < solid.getVerticesList().size(); i++) {
            const vertex: _PolyhedralBoundedSolidVertex = solid.getVerticesList().get(i)!;
            WebGLPolyhedralBoundedSolidDebugRenderer.appendPoint(positions, colors, vertex.position, vertex.debugColor);
        }
        return new DebugLines(
            new Float32Array(0),
            new Float32Array(0),
            new Float32Array(positions),
            new Float32Array(colors),
        );
    }

    private static buildVertexNormalLines(solid: PolyhedralBoundedSolid): DebugLines {
        const positions: number[] = [];
        const colors: number[] = [];
        const yellow = new ColorRgb(1, 1, 0);

        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(i)!;
            const plane: InfinitePlane | null = face.getContainingPlane();
            if (plane === null) {
                continue;
            }
            const normal: Vector3Dd = plane.getNormal().normalized();

            for (let j = 0; j < face.boundariesList.size(); j++) {
                const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(j);
                if (loop === null || loop.boundaryStartHalfEdge === null) {
                    continue;
                }
                let he: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
                const start: _PolyhedralBoundedSolidHalfEdge = he;
                do {
                    he = he!.next();
                    if (he === null) {
                        break;
                    }
                    const p: Vector3Dd = he.startingVertex.position;
                    WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(
                        positions,
                        colors,
                        p.add(normal.multiply(WebGLPolyhedralBoundedSolidDebugRenderer.NORMAL_LINE_LENGTH / 100.0)),
                        p.add(normal.multiply(WebGLPolyhedralBoundedSolidDebugRenderer.NORMAL_LINE_LENGTH)),
                        yellow,
                    );
                } while (he !== start);
            }
        }
        return new DebugLines(new Float32Array(positions), new Float32Array(colors));
    }

    private static buildBoundingVolumeLines(solid: PolyhedralBoundedSolid, quality: RendererConfiguration): DebugLines {
        const minmax: Float64Array = solid.getMinMax();
        const c: ColorRgb = quality.getBoundingVolumeColor();
        return new DebugLines(
            WebGLPolyhedralBoundedSolidDebugRenderer.buildBoxLinePositions(minmax),
            WebGLPolyhedralBoundedSolidDebugRenderer.buildUniformColors(c, 24),
        );
    }

    private static buildSelectionCornerLines(solid: PolyhedralBoundedSolid): DebugLines {
        const minmax: Float64Array = solid.getMinMax();
        let min = new Vector3Dd(minmax[0]!, minmax[1]!, minmax[2]!);
        let max = new Vector3Dd(minmax[3]!, minmax[4]!, minmax[5]!);
        let delta: Vector3Dd = max.subtract(min);
        min = min.subtract(delta.multiply(0.01));
        max = max.add(delta.multiply(0.01));
        delta = delta.multiply(0.25);

        const positions: number[] = [];
        const colors: number[] = [];
        const white = new ColorRgb(1, 1, 1);
        const corner = WebGLPolyhedralBoundedSolidDebugRenderer.appendCorner;

        corner(
            positions,
            colors,
            min,
            new Vector3Dd(delta.x(), 0, 0),
            new Vector3Dd(0, delta.y(), 0),
            new Vector3Dd(0, 0, delta.z()),
            white,
        );
        corner(
            positions,
            colors,
            new Vector3Dd(max.x(), min.y(), min.z()),
            new Vector3Dd(-delta.x(), 0, 0),
            new Vector3Dd(0, delta.y(), 0),
            new Vector3Dd(0, 0, delta.z()),
            white,
        );
        corner(
            positions,
            colors,
            new Vector3Dd(min.x(), max.y(), min.z()),
            new Vector3Dd(delta.x(), 0, 0),
            new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, delta.z()),
            white,
        );
        corner(
            positions,
            colors,
            new Vector3Dd(min.x(), min.y(), max.z()),
            new Vector3Dd(delta.x(), 0, 0),
            new Vector3Dd(0, delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()),
            white,
        );
        corner(
            positions,
            colors,
            new Vector3Dd(max.x(), max.y(), min.z()),
            new Vector3Dd(-delta.x(), 0, 0),
            new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, delta.z()),
            white,
        );
        corner(
            positions,
            colors,
            new Vector3Dd(max.x(), min.y(), max.z()),
            new Vector3Dd(-delta.x(), 0, 0),
            new Vector3Dd(0, delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()),
            white,
        );
        corner(
            positions,
            colors,
            new Vector3Dd(min.x(), max.y(), max.z()),
            new Vector3Dd(delta.x(), 0, 0),
            new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()),
            white,
        );
        corner(
            positions,
            colors,
            max,
            new Vector3Dd(-delta.x(), 0, 0),
            new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()),
            white,
        );

        return new DebugLines(new Float32Array(positions), new Float32Array(colors));
    }

    private static buildNonPlanarFaceHighlights(solid: PolyhedralBoundedSolid): DebugLines {
        const positions: number[] = [];
        const colors: number[] = [];
        const yellow = new ColorRgb(1, 1, 0);

        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(i)!;
            if (_PolyhedralBoundedSolidFaceValidator.isSurfaceDegenerate(face)) {
                WebGLPolyhedralBoundedSolidDebugRenderer.appendFaceBoundaryLines(face, positions, colors, yellow);
            }
        }
        return new DebugLines(new Float32Array(positions), new Float32Array(colors));
    }

    private static buildFaceBoundaryLines(solid: PolyhedralBoundedSolid | null, faceIndex: number): DebugLines {
        const positions: number[] = [];
        const colors: number[] = [];

        if (solid === null || solid.getPolygonsList() === null || faceIndex < -1) {
            return new DebugLines(new Float32Array(positions), new Float32Array(colors));
        }

        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            if (faceIndex > -1 && i !== faceIndex) {
                continue;
            }
            WebGLPolyhedralBoundedSolidDebugRenderer.appendFaceBoundaryLines(
                solid.getPolygonsList().get(i),
                positions,
                colors,
                WebGLPolyhedralBoundedSolidDebugRenderer.colorForIndex(i),
            );
        }
        return new DebugLines(new Float32Array(positions), new Float32Array(colors));
    }

    private static buildDebugEdgeLines(
        solid: PolyhedralBoundedSolid,
        camera: Camera | null,
        edgeIndex: number,
    ): DebugLines {
        const positions: number[] = [];
        const colors: number[] = [];
        const pointPositions: number[] = [];
        const pointColors: number[] = [];

        for (let i = 0; edgeIndex >= -1 && i < solid.getEdgesList().size(); i++) {
            if (i !== edgeIndex && edgeIndex > -1) {
                continue;
            }

            const edge: _PolyhedralBoundedSolidEdge = solid.getEdgesList().get(i)!;
            if (edge.leftHalf === null || edge.rightHalf === null) {
                continue;
            }
            const start: Vector3Dd = edge.rightHalf.startingVertex.position;
            const end: Vector3Dd = edge.leftHalf.startingVertex.position;
            const face1: _PolyhedralBoundedSolidFace = edge.leftHalf.parentLoop.parentFace;
            const face2: _PolyhedralBoundedSolidFace = edge.rightHalf.parentLoop.parentFace;

            let color = new ColorRgb(0.8, 0, 0);
            if (camera !== null) {
                const f1: boolean = HiddenLineRenderer.isFaceVisibleFromCamera(face1, camera) >= 0;
                const f2: boolean = HiddenLineRenderer.isFaceVisibleFromCamera(face2, camera) >= 0;
                if (!f1 && !f2) {
                    color = new ColorRgb(0, 0, 0);
                } else if (f1 !== f2) {
                    color = new ColorRgb(1, 0, 0);
                }
            }
            WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, start, end, color);

            if (edgeIndex > -1 && camera !== null) {
                const middle: Vector3Dd = start.add(end).multiply(0.5);
                const n1: Vector3Dd = face1.getContainingPlane()!.getNormal();
                const n2: Vector3Dd = face2.getContainingPlane()!.getNormal();
                WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(
                    positions,
                    colors,
                    middle,
                    middle.add(n1.multiply(0.1)),
                    new ColorRgb(1, 1, 0),
                );
                WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(
                    positions,
                    colors,
                    middle,
                    middle.add(n2.multiply(0.1)),
                    new ColorRgb(0, 1, 1),
                );

                let d: Vector3Dd = end.subtract(start);
                const length: number = d.length();
                if (length > VSDK.EPSILON) {
                    d = d.normalized();
                    for (let t = VSDK.EPSILON; t < length; t += length / 20.0) {
                        const p: Vector3Dd = start.add(d.multiply(t));
                        const qi: number = solid.computeQuantitativeInvisibility(camera.getPosition(), p);
                        WebGLPolyhedralBoundedSolidDebugRenderer.appendPoint(
                            pointPositions,
                            pointColors,
                            p,
                            qi === 0 ? new ColorRgb(0, 1, 0) : new ColorRgb(0, 0, 1),
                        );
                    }
                }
            }
        }

        return new DebugLines(
            new Float32Array(positions),
            new Float32Array(colors),
            new Float32Array(pointPositions),
            new Float32Array(pointColors),
        );
    }

    private static appendFaceBoundaryLines(
        face: _PolyhedralBoundedSolidFace | null,
        positions: number[],
        colors: number[],
        color: ColorRgb,
    ): void {
        if (face === null) {
            return;
        }
        for (let j = 0; j < face.boundariesList.size(); j++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(j);
            if (loop === null || loop.boundaryStartHalfEdge === null) {
                continue;
            }
            let he: _PolyhedralBoundedSolidHalfEdge = loop.boundaryStartHalfEdge;
            const start: _PolyhedralBoundedSolidHalfEdge = he;
            do {
                const next: _PolyhedralBoundedSolidHalfEdge | null = he.next();
                if (next === null) {
                    break;
                }
                WebGLPolyhedralBoundedSolidDebugRenderer.appendFaceBoundaryArrow(
                    positions,
                    colors,
                    he,
                    next,
                    j === 0 ? 1.0 : -1.0,
                    color,
                );
                he = next;
            } while (he !== start);
        }
    }

    private static appendFaceBoundaryArrow(
        positions: number[],
        colors: number[],
        he: _PolyhedralBoundedSolidHalfEdge | null,
        next: _PolyhedralBoundedSolidHalfEdge | null,
        invert: number,
        color: ColorRgb,
    ): void {
        if (he === null || next === null || he.startingVertex === null || next.startingVertex === null) {
            return;
        }

        const nextNext: _PolyhedralBoundedSolidHalfEdge | null = next.next();
        if (nextNext === null || nextNext.startingVertex === null) {
            WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(
                positions,
                colors,
                he.startingVertex.position,
                next.startingVertex.position,
                color,
            );
            return;
        }

        const startPoint: Vector3Dd = he.startingVertex.position;
        const endPoint: Vector3Dd = next.startingVertex.position;
        const thirdPoint: Vector3Dd = nextNext.startingVertex.position;
        const a: Vector3Dd = endPoint.subtract(startPoint);
        const b: Vector3Dd = thirdPoint.subtract(startPoint);
        if (a.length() <= VSDK.EPSILON || b.length() <= VSDK.EPSILON) {
            WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, startPoint, endPoint, color);
            return;
        }

        let planeNormal: Vector3Dd = a.normalized().crossProduct(b.normalized());
        if (planeNormal.length() <= VSDK.EPSILON) {
            const containingPlane: InfinitePlane | null =
                he.parentLoop !== null && he.parentLoop.parentFace !== null
                    ? he.parentLoop.parentFace.getContainingPlane()
                    : null;
            if (containingPlane === null || containingPlane.getNormal().length() <= VSDK.EPSILON) {
                WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, startPoint, endPoint, color);
                return;
            }
            planeNormal = containingPlane.getNormal();
        }

        WebGLPolyhedralBoundedSolidDebugRenderer.appendCurvedArrowOverPlane(
            positions,
            colors,
            startPoint,
            endPoint,
            planeNormal.normalized(),
            invert,
            WebGLPolyhedralBoundedSolidDebugRenderer.EDGE_ARROW_SIZE,
            WebGLPolyhedralBoundedSolidDebugRenderer.EDGE_ARROW_CURVE_OFFSET,
            color,
        );
    }

    private static appendCurvedArrowOverPlane(
        positions: number[],
        colors: number[],
        startPoint: Vector3Dd,
        endPoint: Vector3Dd,
        planeNormal: Vector3Dd,
        invert: number,
        sizePercent: number,
        curveOffsetPercent: number,
        color: ColorRgb,
    ): void {
        const axis: Vector3Dd = endPoint.subtract(startPoint);
        const fullLength: number = axis.length();
        if (fullLength <= 1e-12) {
            return;
        }

        sizePercent = Math.min(1.0, Math.max(VSDK.EPSILON, sizePercent));
        curveOffsetPercent = Math.min(1.0, Math.max(0.0, curveOffsetPercent));

        const factor: number = fullLength * sizePercent;
        const delta: number = factor / WebGLPolyhedralBoundedSolidDebugRenderer.CURVED_ARROW_SEGMENTS;
        const tangentAxis: Vector3Dd = axis.normalized();
        let curveAxis: Vector3Dd = tangentAxis.crossProduct(planeNormal);
        if (curveAxis.length() <= 1e-12) {
            WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, startPoint, endPoint, color);
            return;
        }
        curveAxis = curveAxis.normalized();

        for (let i = 0; i < WebGLPolyhedralBoundedSolidDebugRenderer.CURVED_ARROW_SEGMENTS; i++) {
            const t0: number = i * delta;
            const t1: number = t0 + delta;
            const p0: Vector3Dd = WebGLPolyhedralBoundedSolidDebugRenderer.curvedArrowPoint(
                startPoint,
                tangentAxis,
                curveAxis,
                invert,
                t0,
                fullLength,
                curveOffsetPercent,
            );
            const p1: Vector3Dd = WebGLPolyhedralBoundedSolidDebugRenderer.curvedArrowPoint(
                startPoint,
                tangentAxis,
                curveAxis,
                invert,
                t1,
                fullLength,
                curveOffsetPercent,
            );
            WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, p0, p1, color);
        }

        const tip: Vector3Dd = WebGLPolyhedralBoundedSolidDebugRenderer.curvedArrowPoint(
            startPoint,
            tangentAxis,
            curveAxis,
            invert,
            factor,
            fullLength,
            curveOffsetPercent,
        );
        let tangent: Vector3Dd = tangentAxis.add(
            curveAxis.multiply(
                invert * WebGLPolyhedralBoundedSolidDebugRenderer.curveSlope(factor, fullLength, curveOffsetPercent),
            ),
        );
        if (tangent.length() <= 1e-12) {
            tangent = tangentAxis;
        }
        tangent = tangent.normalized();

        let headSide: Vector3Dd = tangent.crossProduct(planeNormal);
        if (headSide.length() <= 1e-12) {
            headSide = curveAxis;
        }
        headSide = headSide.normalized();

        const headLength: number = factor * 0.1;
        const headHalfWidth: number = fullLength * curveOffsetPercent * 0.5;
        const headBase: Vector3Dd = tip.subtract(tangent.multiply(headLength));

        WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(
            positions,
            colors,
            tip,
            headBase.add(headSide.multiply(headHalfWidth)),
            color,
        );
        WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(
            positions,
            colors,
            tip,
            headBase.add(headSide.multiply(-headHalfWidth)),
            color,
        );
    }

    private static curvedArrowPoint(
        startPoint: Vector3Dd,
        tangentAxis: Vector3Dd,
        curveAxis: Vector3Dd,
        invert: number,
        axisDistance: number,
        fullLength: number,
        curveOffsetPercent: number,
    ): Vector3Dd {
        return startPoint.add(
            tangentAxis
                .multiply(axisDistance)
                .add(
                    curveAxis.multiply(
                        invert *
                            WebGLPolyhedralBoundedSolidDebugRenderer.curveFactor(
                                axisDistance,
                                fullLength,
                                curveOffsetPercent,
                            ),
                    ),
                ),
        );
    }

    private static curveFactor(axisDistance: number, fullLength: number, curveOffsetPercent: number): number {
        const percent: number = axisDistance / fullLength;
        return curveOffsetPercent * fullLength * Math.sin(percent * Math.PI);
    }

    private static curveSlope(axisDistance: number, fullLength: number, curveOffsetPercent: number): number {
        const percent: number = axisDistance / fullLength;
        return curveOffsetPercent * Math.PI * Math.cos(percent * Math.PI);
    }

    private static appendLine(
        positions: number[],
        colors: number[],
        a: Vector3Dd,
        b: Vector3Dd,
        color: ColorRgb,
    ): void {
        positions.push(a.x());
        positions.push(a.y());
        positions.push(a.z());
        positions.push(b.x());
        positions.push(b.y());
        positions.push(b.z());
        WebGLPolyhedralBoundedSolidDebugRenderer.appendColor(colors, color);
        WebGLPolyhedralBoundedSolidDebugRenderer.appendColor(colors, color);
    }

    private static appendPoint(positions: number[], colors: number[], p: Vector3Dd, color: ColorRgb): void {
        positions.push(p.x());
        positions.push(p.y());
        positions.push(p.z());
        WebGLPolyhedralBoundedSolidDebugRenderer.appendColor(colors, color);
    }

    private static appendColor(colors: number[], color: ColorRgb): void {
        colors.push(color.r());
        colors.push(color.g());
        colors.push(color.b());
    }

    private static buildBoxLinePositions(minmax: Float64Array): Float32Array {
        const x0: number = minmax[0]!;
        const y0: number = minmax[1]!;
        const z0: number = minmax[2]!;
        const x1: number = minmax[3]!;
        const y1: number = minmax[4]!;
        const z1: number = minmax[5]!;
        return new Float32Array([
            x0,
            y0,
            z0,
            x1,
            y0,
            z0,
            x1,
            y0,
            z0,
            x1,
            y1,
            z0,
            x1,
            y1,
            z0,
            x0,
            y1,
            z0,
            x0,
            y1,
            z0,
            x0,
            y0,
            z0,

            x0,
            y0,
            z1,
            x1,
            y0,
            z1,
            x1,
            y0,
            z1,
            x1,
            y1,
            z1,
            x1,
            y1,
            z1,
            x0,
            y1,
            z1,
            x0,
            y1,
            z1,
            x0,
            y0,
            z1,

            x0,
            y0,
            z0,
            x0,
            y0,
            z1,
            x1,
            y0,
            z0,
            x1,
            y0,
            z1,
            x1,
            y1,
            z0,
            x1,
            y1,
            z1,
            x0,
            y1,
            z0,
            x0,
            y1,
            z1,
        ]);
    }

    private static appendCorner(
        positions: number[],
        colors: number[],
        origin: Vector3Dd,
        dx: Vector3Dd,
        dy: Vector3Dd,
        dz: Vector3Dd,
        color: ColorRgb,
    ): void {
        WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, origin, origin.add(dx), color);
        WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, origin, origin.add(dy), color);
        WebGLPolyhedralBoundedSolidDebugRenderer.appendLine(positions, colors, origin, origin.add(dz), color);
    }

    private static buildUniformColors(color: ColorRgb, vertexCount: number): Float32Array {
        const colors = new Float32Array(vertexCount * 3);
        for (let i = 0; i < vertexCount; i++) {
            const base: number = i * 3;
            colors[base] = color.r();
            colors[base + 1] = color.g();
            colors[base + 2] = color.b();
        }
        return colors;
    }

    private static colorForIndex(index: number): ColorRgb {
        switch (index % 8) {
            case 0:
                return new ColorRgb(1, 0, 0);
            case 1:
                return new ColorRgb(0, 1, 0);
            case 2:
                return new ColorRgb(0, 0, 1);
            case 3:
                return new ColorRgb(0, 1, 1);
            case 4:
                return new ColorRgb(1, 0, 1);
            case 5:
                return new ColorRgb(0.5, 0, 0);
            case 6:
                return new ColorRgb(0, 0.5, 0);
            default:
                return new ColorRgb(0.6, 0.5, 0.4);
        }
    }
}
