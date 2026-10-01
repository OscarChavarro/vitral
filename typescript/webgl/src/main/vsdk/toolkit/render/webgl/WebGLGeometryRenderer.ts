import {
    Arrow,
    Box,
    Cone,
    FunctionalExplicitSurface,
    identityHashCode,
    InfinitePlane,
    Logger,
    ParametricBiCubicPatch,
    ParametricCurve,
    RendererConfiguration,
    Sphere,
    Torus,
    TriangleMesh,
    Vector3Dd,
    VSDK,
    type Camera,
    type Geometry,
    type Light,
    type Matrix4x4d,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLFunctionalExplicitSurfaceRenderer } from "./WebGLFunctionalExplicitSurfaceRenderer.js";
import { WebGLInfinitePlaneRenderer } from "./WebGLInfinitePlaneRenderer.js";
import { WebGLMeshBuilder } from "./WebGLMeshBuilder.js";
import { WebGLMeshRenderer, type WebGLMesh } from "./WebGLMeshRenderer.js";
import { WebGLMinMaxRenderer } from "./WebGLMinMaxRenderer.js";
import { WebGLParametricBiCubicPatchRenderer } from "./WebGLParametricBiCubicPatchRenderer.js";
import { WebGLParametricCurveRenderer } from "./WebGLParametricCurveRenderer.js";
import { WebGLSelectionCornersRenderer } from "./WebGLSelectionCornersRenderer.js";
import { WebGLSphereRenderer } from "./WebGLSphereRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4GeometryRenderer`.

Renders the geometries of the toolkit with the WebGL pipeline: each one is
tessellated once into a mesh (cached by its parameters) that is drawn by
`WebGLMeshRenderer`, so every geometry honors every bit of the
`RendererConfiguration` in the same way (surfaces, wires, points, normals,
bounding volume, texture, bump map and shading).

Supported geometries: `Sphere`, `Cone` (also cylinders and truncated cones),
`Arrow`, `Box`, `Torus`, `TriangleMesh`, and the open surfaces
`FunctionalExplicitSurface`, `ParametricBiCubicPatch` and `InfinitePlane`
(tessellated by their `WebGL*Renderer`, visible from both sides). A
`ParametricCurve` has no surface: it is drawn as lines by
`WebGLParametricCurveRenderer`.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`). `System.identityHashCode` comes from the
`identityHashCode` emulation of the base module.
*/
export class WebGLGeometryRenderer {
    private static readonly SLICES: number = 32;
    private static readonly TORUS_MAJOR_SEGMENTS: number = 48;
    private static readonly TORUS_MINOR_SEGMENTS: number = 24;
    private static readonly MAX_CACHED_MESHES: number = 256;

    private static readonly MESHES: Map<string, WebGLMesh> = new Map<string, WebGLMesh>();
    private static readonly REPORTED_UNSUPPORTED: Set<string> = new Set<string>();

    private constructor() {}

    /**
    @param geometry a geometry
    @return true if this renderer can draw the geometry
    */
    public static isSupported(geometry: Geometry | null): boolean {
        return geometry instanceof Sphere || geometry instanceof Cone ||
            geometry instanceof Arrow || geometry instanceof Box ||
            geometry instanceof Torus || geometry instanceof TriangleMesh ||
            geometry instanceof FunctionalExplicitSurface ||
            geometry instanceof ParametricBiCubicPatch ||
            geometry instanceof InfinitePlane ||
            geometry instanceof ParametricCurve;
    }

    /**
    Draws a geometry.

    @param gl WebGL context
    @param geometry geometry to draw
    @param camera camera that views the geometry
    @param lights light or lights of the scene, or null or empty to use a
    light at the camera
    @param material material of the geometry
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from geometry space to world space
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        geometry: Geometry | null,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        if (geometry === null) {
            return;
        }
        if (geometry instanceof Sphere) {
            await WebGLSphereRenderer.draw(gl, geometry, camera, lights, material, quality,
                textureMap, normalMap, localTransform ?? undefined,
                WebGLGeometryRenderer.SLICES, WebGLGeometryRenderer.SLICES / 2);
            return;
        }
        if (geometry instanceof ParametricCurve) {
            await WebGLParametricCurveRenderer.draw(gl, geometry, camera, quality, localTransform);
            return;
        }
        const mesh: WebGLMesh | null = WebGLGeometryRenderer.obtainMesh(geometry);
        if (mesh === null) {
            return;
        }
        if (geometry instanceof InfinitePlane && quality !== null) {
            await WebGLGeometryRenderer.drawInfinitePlaneMesh(gl, mesh, geometry, camera, lights, material,
                quality, textureMap, normalMap, localTransform);
            return;
        }
        await WebGLMeshRenderer.draw(gl, mesh, geometry, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    Draws the mesh of the square shown for an infinite plane. Its bounding
    volume is infinite, so the bounding volume and the selection corners are
    drawn around the square instead.
    */
    private static async drawInfinitePlaneMesh(
        gl: WebGL2RenderingContext,
        mesh: WebGLMesh,
        plane: InfinitePlane,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        const meshQuality: RendererConfiguration = new RendererConfiguration();

        meshQuality.clone(quality);
        meshQuality.setBoundingVolume(false);
        meshQuality.setSelectionCorners(false);
        await WebGLMeshRenderer.draw(gl, mesh, plane, camera, lights, material, meshQuality,
            textureMap, normalMap, localTransform);

        const shown: number[] = WebGLInfinitePlaneRenderer.calculateShownMinMax(plane);
        if (quality.isBoundingVolumeSet()) {
            await WebGLMinMaxRenderer.drawMinMax(gl, shown, camera, localTransform ?? undefined);
        }
        if (quality.isSelectionCornersSet()) {
            await WebGLSelectionCornersRenderer.drawMinMax(gl, shown, camera, localTransform);
        }
    }

    /**
    Releases the WebGL resources of the meshes. PRE: the context that created
    them is the given one.
    @param gl WebGL context
    */
    public static dispose(gl: WebGL2RenderingContext): void {
        WebGLGeometryRenderer.releaseMeshes(gl);
        WebGLSphereRenderer.dispose(gl);
    }

    private static releaseMeshes(gl: WebGL2RenderingContext): void {
        for (const mesh of WebGLGeometryRenderer.MESHES.values()) {
            WebGLMeshRenderer.release(gl, mesh);
        }
        WebGLGeometryRenderer.MESHES.clear();
    }

    private static obtainMesh(geometry: Geometry): WebGLMesh | null {
        const key: string | null = WebGLGeometryRenderer.keyOf(geometry);

        if (key === null) {
            const name: string = geometry.constructor.name;
            if (!WebGLGeometryRenderer.REPORTED_UNSUPPORTED.has(name)) {
                WebGLGeometryRenderer.REPORTED_UNSUPPORTED.add(name);
                Logger.reportMessage(null, VSDK.WARNING, "WebGLGeometryRenderer",
                    "Geometry not supported by the WebGL pipeline yet: " + name);
            }
            return null;
        }
        let mesh: WebGLMesh | null = WebGLGeometryRenderer.MESHES.get(key) ?? null;

        if (mesh === null) {
            mesh = WebGLGeometryRenderer.buildMesh(geometry);
            if (mesh === null) {
                return null;
            }
            if (WebGLGeometryRenderer.MESHES.size >= WebGLGeometryRenderer.MAX_CACHED_MESHES) {
                // Stale meshes (i.e. from geometries edited many times) are
                // recreated on demand; their GPU objects are freed with the
                // context
                WebGLGeometryRenderer.MESHES.clear();
            }
            WebGLGeometryRenderer.MESHES.set(key, mesh);
        }
        return mesh;
    }

    /**
    @return a string that identifies the mesh of the geometry, or null if the
    geometry is not supported. Parametric geometries are identified by their
    parameters; triangle meshes by identity, as they are not edited while
    shown.
    */
    private static keyOf(geometry: Geometry): string | null {
        if (geometry instanceof Cone) {
            return "cone/" + geometry.getBottomRadius() + "/" + geometry.getTopRadius() + "/" + geometry.getHeight();
        }
        if (geometry instanceof Arrow) {
            return "arrow/" + geometry.getBaseLength() + "/" + geometry.getHeadLength() + "/" +
                geometry.getBaseRadius() + "/" + geometry.getHeadRadius();
        }
        if (geometry instanceof Box) {
            const s: Vector3Dd = geometry.getSize();
            return "box/" + s.x() + "/" + s.y() + "/" + s.z();
        }
        if (geometry instanceof Torus) {
            return "torus/" + geometry.getMajorRadius() + "/" + geometry.getMinorRadius();
        }
        if (geometry instanceof TriangleMesh) {
            return "trianglemesh/" + identityHashCode(geometry);
        }
        if (geometry instanceof FunctionalExplicitSurface) {
            return WebGLFunctionalExplicitSurfaceRenderer.meshKey(geometry);
        }
        if (geometry instanceof ParametricBiCubicPatch) {
            return WebGLParametricBiCubicPatchRenderer.meshKey(geometry);
        }
        if (geometry instanceof InfinitePlane) {
            return WebGLInfinitePlaneRenderer.meshKey(geometry);
        }
        return null;
    }

    private static buildMesh(geometry: Geometry): WebGLMesh | null {
        if (geometry instanceof Cone) {
            return WebGLGeometryRenderer.buildCone(geometry);
        }
        if (geometry instanceof Arrow) {
            return WebGLGeometryRenderer.buildArrow(geometry);
        }
        if (geometry instanceof Box) {
            return WebGLGeometryRenderer.buildBox(geometry);
        }
        if (geometry instanceof Torus) {
            return WebGLGeometryRenderer.buildTorus(geometry);
        }
        if (geometry instanceof TriangleMesh) {
            return WebGLGeometryRenderer.buildTriangleMesh(geometry);
        }
        if (geometry instanceof FunctionalExplicitSurface) {
            return WebGLFunctionalExplicitSurfaceRenderer.buildMesh(geometry);
        }
        if (geometry instanceof ParametricBiCubicPatch) {
            return WebGLParametricBiCubicPatchRenderer.buildMesh(geometry);
        }
        if (geometry instanceof InfinitePlane) {
            return WebGLInfinitePlaneRenderer.buildMesh(geometry);
        }
        return null;
    }

    private static buildCone(cone: Cone): WebGLMesh {
        const r1: number = cone.getBottomRadius();
        const r2: number = cone.getTopRadius();
        const h: number = cone.getHeight();
        const slices: number = WebGLGeometryRenderer.SLICES;
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(Math.max(Math.max(r1, r2), h));

        builder.addFrustum(0, r1, h, r2, slices);
        builder.addDisk(0, 0, r1, false, slices);
        if (r2 > 0.0) {
            builder.addDisk(h, 0, r2, true, slices);
        }
        return builder.build();
    }

    private static buildArrow(arrow: Arrow): WebGLMesh {
        const h1: number = arrow.getBaseLength();
        const h2: number = arrow.getHeadLength();
        const r1: number = arrow.getBaseRadius();
        const r2: number = arrow.getHeadRadius();
        const slices: number = WebGLGeometryRenderer.SLICES;
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(h1 + h2);

        builder.addFrustum(0, r1, h1, r1, slices);
        builder.addDisk(0, 0, r1, false, slices);
        builder.addFrustum(h1, r2, h1 + h2, 0, slices);
        builder.addDisk(h1, r1, r2, false, slices);
        return builder.build();
    }

    private static buildBox(box: Box): WebGLMesh {
        const size: Vector3Dd = box.getSize();
        const hx: number = size.x() / 2;
        const hy: number = size.y() / 2;
        const hz: number = size.z() / 2;
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(Math.max(hx, Math.max(hy, hz)) * 2);

        // Each face is given counterclockwise seen from outside
        WebGLGeometryRenderer.addBoxFace(builder, new Vector3Dd(0, 0, -1),
            new Vector3Dd(-hx, -hy, -hz), new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(hx, hy, -hz), new Vector3Dd(hx, -hy, -hz));
        WebGLGeometryRenderer.addBoxFace(builder, new Vector3Dd(0, 0, 1),
            new Vector3Dd(-hx, -hy, hz), new Vector3Dd(hx, -hy, hz),
            new Vector3Dd(hx, hy, hz), new Vector3Dd(-hx, hy, hz));
        WebGLGeometryRenderer.addBoxFace(builder, new Vector3Dd(0, -1, 0),
            new Vector3Dd(-hx, -hy, hz), new Vector3Dd(-hx, -hy, -hz),
            new Vector3Dd(hx, -hy, -hz), new Vector3Dd(hx, -hy, hz));
        WebGLGeometryRenderer.addBoxFace(builder, new Vector3Dd(-1, 0, 0),
            new Vector3Dd(-hx, hy, hz), new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(-hx, -hy, -hz), new Vector3Dd(-hx, -hy, hz));
        WebGLGeometryRenderer.addBoxFace(builder, new Vector3Dd(0, 1, 0),
            new Vector3Dd(hx, hy, hz), new Vector3Dd(hx, hy, -hz),
            new Vector3Dd(-hx, hy, -hz), new Vector3Dd(-hx, hy, hz));
        WebGLGeometryRenderer.addBoxFace(builder, new Vector3Dd(1, 0, 0),
            new Vector3Dd(hx, -hy, hz), new Vector3Dd(hx, -hy, -hz),
            new Vector3Dd(hx, hy, -hz), new Vector3Dd(hx, hy, hz));
        return builder.build();
    }

    /**
    Adds a face whose vertices are given in the order upper left, lower left,
    lower right, upper right as seen from outside (the Jogl2 texture layout),
    and which is wound so its front side looks outside.
    */
    private static addBoxFace(
        builder: WebGLMeshBuilder,
        n: Vector3Dd,
        a: Vector3Dd,
        b: Vector3Dd,
        c: Vector3Dd,
        d: Vector3Dd,
    ): void {
        const winding: Vector3Dd = b.subtract(a).crossProduct(c.subtract(b));

        if (winding.dotProduct(n) >= 0) {
            builder.addQuad(a, n, 0, 1, b, n, 0, 0, c, n, 1, 0, d, n, 1, 1);
        }
        else {
            builder.addQuad(d, n, 1, 1, c, n, 1, 0, b, n, 0, 0, a, n, 0, 1);
        }
    }

    private static buildTorus(torus: Torus): WebGLMesh {
        const bigR: number = torus.getMajorRadius();
        const smallR: number = torus.getMinorRadius();
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(bigR + smallR);
        const nw: number = WebGLGeometryRenderer.TORUS_MAJOR_SEGMENTS;
        const nv: number = WebGLGeometryRenderer.TORUS_MINOR_SEGMENTS;

        for (let i: number = 0; i < nw; i++) {
            const w0: number = 2 * Math.PI * i / nw;
            const w1: number = 2 * Math.PI * (i + 1) / nw;

            for (let j: number = 0; j < nv; j++) {
                const v0: number = 2 * Math.PI * j / nv;
                const v1: number = 2 * Math.PI * (j + 1) / nv;
                const p00: Vector3Dd = WebGLGeometryRenderer.torusPoint(bigR, smallR, w0, v0);
                const p01: Vector3Dd = WebGLGeometryRenderer.torusPoint(bigR, smallR, w0, v1);
                const p11: Vector3Dd = WebGLGeometryRenderer.torusPoint(bigR, smallR, w1, v1);
                const p10: Vector3Dd = WebGLGeometryRenderer.torusPoint(bigR, smallR, w1, v0);
                const n00: Vector3Dd = WebGLGeometryRenderer.torusNormal(w0, v0);
                const n01: Vector3Dd = WebGLGeometryRenderer.torusNormal(w0, v1);
                const n11: Vector3Dd = WebGLGeometryRenderer.torusNormal(w1, v1);
                const n10: Vector3Dd = WebGLGeometryRenderer.torusNormal(w1, v0);

                builder.addQuad(
                    p00, n00, i / nw, j / nv,
                    p10, n10, (i + 1) / nw, j / nv,
                    p11, n11, (i + 1) / nw, (j + 1) / nv,
                    p01, n01, i / nw, (j + 1) / nv);
            }
        }
        return builder.build();
    }

    private static torusPoint(bigR: number, smallR: number, w: number, v: number): Vector3Dd {
        return new Vector3Dd(
            (bigR + smallR * Math.cos(v)) * Math.cos(w),
            (bigR + smallR * Math.cos(v)) * Math.sin(w),
            smallR * Math.sin(v));
    }

    private static torusNormal(w: number, v: number): Vector3Dd {
        return new Vector3Dd(Math.cos(v) * Math.cos(w), Math.cos(v) * Math.sin(w), Math.sin(v));
    }

    private static buildTriangleMesh(mesh: TriangleMesh): WebGLMesh | null {
        const p: Float64Array | null = mesh.getVertexPositions();
        const n: Float64Array | null = mesh.getVertexNormals();
        const uv: Float64Array | null = mesh.getVertexUvs();
        const idx: Int32Array | null = mesh.getTriangleIndexes();

        if (p === null || idx === null) {
            return null;
        }
        const minmax: ArrayLike<number> | null = mesh.getMinMax();
        let size: number = 1.0;
        if (minmax !== null && minmax.length >= 6) {
            size = Math.max(Math.abs(minmax[3]! - minmax[0]!),
                Math.max(Math.abs(minmax[4]! - minmax[1]!), Math.abs(minmax[5]! - minmax[2]!)));
        }
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(size);

        for (let t: number = 0; t + 2 < idx.length; t += 3) {
            const pos: Vector3Dd[] = [];
            const nor: Vector3Dd[] = [];
            const u: number[] = [0, 0, 0];
            const v: number[] = [0, 0, 0];

            for (let k: number = 0; k < 3; k++) {
                const i: number = idx[t + k]!;
                pos.push(new Vector3Dd(p[3 * i]!, p[3 * i + 1]!, p[3 * i + 2]!));
                nor.push((n !== null && n.length >= 3 * i + 3)
                    ? new Vector3Dd(n[3 * i]!, n[3 * i + 1]!, n[3 * i + 2]!)
                    : new Vector3Dd(0, 0, 1));
                if (uv !== null && uv.length >= 2 * i + 2) {
                    u[k] = uv[2 * i]!;
                    v[k] = uv[2 * i + 1]!;
                }
            }
            builder.addTriangle(pos[0]!, nor[0]!, u[0]!, v[0]!, pos[1]!, nor[1]!, u[1]!, v[1]!,
                pos[2]!, nor[2]!, u[2]!, v[2]!);
        }
        return builder.build();
    }
}
