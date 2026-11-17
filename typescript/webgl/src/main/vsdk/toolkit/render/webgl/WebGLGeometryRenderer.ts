import {
    Arrow,
    Box,
    Cone,
    FunctionalExplicitSurface,
    HeightFieldSolid,
    ImplicitSolid,
    InfinitePlane,
    Logger,
    ParametricBiCubicPatch,
    ParametricCurve,
    PlanarHalfSpace,
    PolyhedralBoundedSolid,
    QuadMesh,
    QuadricSurface,
    Sphere,
    SubdividedBezierPatch,
    Torus,
    TriangleMesh,
    TriangleMeshGroup,
    TriangleStripMesh,
    Volume,
    VSDK,
    type Camera,
    type Geometry,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLParametricCurveRenderer } from "./curve/WebGLParametricCurveRenderer.js";
import { WebGLFunctionalExplicitSurfaceRenderer } from "./surface/WebGLFunctionalExplicitSurfaceRenderer.js";
import { WebGLInfinitePlaneRenderer } from "./surface/WebGLInfinitePlaneRenderer.js";
import { WebGLParametricBiCubicPatchRenderer } from "./surface/WebGLParametricBiCubicPatchRenderer.js";
import { WebGLQuadMeshRenderer } from "./surface/WebGLQuadMeshRenderer.js";
import { WebGLSubdividedBezierPatchRenderer } from "./surface/WebGLSubdividedBezierPatchRenderer.js";
import { WebGLTriangleMeshGroupRenderer } from "./surface/WebGLTriangleMeshGroupRenderer.js";
import { WebGLTriangleMeshRenderer } from "./surface/WebGLTriangleMeshRenderer.js";
import { WebGLTriangleStripMeshRenderer } from "./surface/WebGLTriangleStripMeshRenderer.js";
import { WebGLMeshRenderer, type WebGLMesh } from "./WebGLMeshRenderer.js";
import { WebGLArrowRenderer } from "./volume/WebGLArrowRenderer.js";
import { WebGLBoxRenderer } from "./volume/WebGLBoxRenderer.js";
import { WebGLConeRenderer } from "./volume/WebGLConeRenderer.js";
import { WebGLHeightFieldSolidRenderer } from "./volume/WebGLHeightFieldSolidRenderer.js";
import { WebGLImplicitSolidRenderer } from "./volume/WebGLImplicitSolidRenderer.js";
import { WebGLPlanarHalfSpaceRenderer } from "./volume/WebGLPlanarHalfSpaceRenderer.js";
import { WebGLPolyhedralBoundedSolidRenderer } from "./volume/polyhedralBoundedSolid/WebGLPolyhedralBoundedSolidRenderer.js";
import { WebGLQuadricSurfaceRenderer } from "./volume/WebGLQuadricSurfaceRenderer.js";
import { WebGLSphereRenderer } from "./volume/WebGLSphereRenderer.js";
import { WebGLTorusRenderer } from "./volume/WebGLTorusRenderer.js";
import { WebGLVolumeRenderer } from "./volume/WebGLVolumeRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4GeometryRenderer`.

Renders the geometries of the toolkit with the WebGL pipeline, dispatching
each one to the renderer of its class (in the `curve`, `surface` and `volume`
subpackages). Most of them are tessellated once into a mesh by the
`buildMesh` of their renderer, cached here by the `meshKey` the renderer
gives, and drawn by `WebGLMeshRenderer`, so every geometry honors every bit
of the `RendererConfiguration` in the same way (surfaces, wires, points,
normals, bounding volume, texture, bump map and shading). A `Sphere` keeps
its own mesh cache, a `ParametricCurve` is drawn as lines, a
`TriangleMeshGroup` member by member, a `PlanarHalfSpace` by its boundary
plane, and a `PolyhedralBoundedSolid` by its own renderer, which is also the
last resort for the other volumes (see `WebGLVolumeRenderer`).

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`). Drawing is asynchronous because a GLSL source
arrives over `fetch`. A `PolyhedralBoundedSolid` and the volumes drawn by
their boundary representation need a camera, which Java dereferences without
a check; it is narrowed the same way here.

Java also dispatches `VoxelVolume` to `Jogl4VoxelVolumeRenderer`; that
renderer, and the `VoxelVolume` interface it relies on (Java commit
402415f4), have not been ported, so a voxel volume reaches the boundary
representation fallback instead.
*/
export class WebGLGeometryRenderer {
    private static readonly MAX_CACHED_MESHES: number = 256;

    private static readonly MESHES: Map<string, WebGLMesh> = new Map<string, WebGLMesh>();
    private static readonly REPORTED_UNSUPPORTED: Set<string> = new Set<string>();

    private constructor() {}

    /**
    @param geometry a geometry
    @return true if this renderer can draw the geometry
    */
    public static isSupported(geometry: Geometry | null): boolean {
        return (
            geometry instanceof Sphere ||
            geometry instanceof ParametricCurve ||
            geometry instanceof TriangleMeshGroup ||
            geometry instanceof PlanarHalfSpace ||
            geometry instanceof PolyhedralBoundedSolid ||
            (geometry !== null && WebGLGeometryRenderer.keyOf(geometry) !== null) ||
            (geometry instanceof Volume && WebGLVolumeRenderer.exportedSolid(geometry) !== null)
        );
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
            await WebGLSphereRenderer.draw(
                gl,
                geometry,
                camera,
                lights,
                material,
                quality,
                textureMap,
                normalMap,
                localTransform ?? undefined,
                WebGLSphereRenderer.SLICES,
                WebGLSphereRenderer.SLICES / 2,
            );
            return;
        }
        if (geometry instanceof ParametricCurve) {
            await WebGLParametricCurveRenderer.draw(gl, geometry, camera, quality, localTransform);
            return;
        }
        if (geometry instanceof TriangleMeshGroup) {
            await WebGLTriangleMeshGroupRenderer.draw(
                gl,
                geometry,
                camera,
                lights,
                material,
                quality,
                textureMap,
                normalMap,
                localTransform,
            );
            return;
        }
        if (geometry instanceof PlanarHalfSpace) {
            await WebGLPlanarHalfSpaceRenderer.draw(
                gl,
                geometry,
                camera,
                lights,
                material,
                quality,
                textureMap,
                normalMap,
                localTransform,
            );
            return;
        }
        if (geometry instanceof PolyhedralBoundedSolid) {
            await WebGLPolyhedralBoundedSolidRenderer.draw(
                gl,
                geometry,
                camera as Camera,
                lights,
                material,
                quality,
                localTransform,
            );
            return;
        }
        if (
            WebGLGeometryRenderer.keyOf(geometry) === null &&
            geometry instanceof Volume &&
            (await WebGLVolumeRenderer.draw(gl, geometry, camera as Camera, lights, material, quality, localTransform))
        ) {
            return;
        }
        const mesh: WebGLMesh | null = WebGLGeometryRenderer.obtainMesh(gl, geometry);
        if (mesh === null) {
            return;
        }
        if (geometry instanceof InfinitePlane && quality !== null) {
            await WebGLInfinitePlaneRenderer.drawMesh(
                gl,
                mesh,
                geometry,
                camera,
                lights,
                material,
                quality,
                textureMap,
                normalMap,
                localTransform,
            );
            return;
        }
        await WebGLMeshRenderer.draw(
            gl,
            mesh,
            geometry,
            camera,
            lights,
            material,
            quality,
            textureMap,
            normalMap,
            localTransform,
        );
    }

    /**
    Releases the WebGL resources of the meshes. PRE: the context that created
    them is the given one.
    @param gl WebGL context
    */
    public static dispose(gl: WebGL2RenderingContext): void {
        WebGLGeometryRenderer.releaseMeshes(gl);
        WebGLVolumeRenderer.dispose();
        WebGLSphereRenderer.dispose(gl);
    }

    private static releaseMeshes(gl: WebGL2RenderingContext): void {
        for (const mesh of WebGLGeometryRenderer.MESHES.values()) {
            WebGLMeshRenderer.release(gl, mesh);
        }
        WebGLGeometryRenderer.MESHES.clear();
    }

    private static obtainMesh(gl: WebGL2RenderingContext, geometry: Geometry): WebGLMesh | null {
        const key: string | null = WebGLGeometryRenderer.keyOf(geometry);

        if (key === null) {
            const name: string = geometry.constructor.name;
            if (!WebGLGeometryRenderer.REPORTED_UNSUPPORTED.has(name)) {
                WebGLGeometryRenderer.REPORTED_UNSUPPORTED.add(name);
                Logger.reportMessage(
                    null,
                    VSDK.WARNING,
                    "WebGLGeometryRenderer",
                    "Geometry not supported by the WebGL pipeline yet: " + name,
                );
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
                // released, and recreated on demand
                WebGLGeometryRenderer.releaseMeshes(gl);
            }
            WebGLGeometryRenderer.MESHES.set(key, mesh);
        }
        return mesh;
    }

    /**
    @return a string that identifies the mesh of the geometry (given by the
    `meshKey` of its renderer), or null if the geometry is not tessellated to
    a mesh
    */
    private static keyOf(geometry: Geometry): string | null {
        if (geometry instanceof Cone) {
            return WebGLConeRenderer.meshKey(geometry);
        }
        if (geometry instanceof Arrow) {
            return WebGLArrowRenderer.meshKey(geometry);
        }
        if (geometry instanceof Box) {
            return WebGLBoxRenderer.meshKey(geometry);
        }
        if (geometry instanceof Torus) {
            return WebGLTorusRenderer.meshKey(geometry);
        }
        if (geometry instanceof TriangleMesh) {
            return WebGLTriangleMeshRenderer.meshKey(geometry);
        }
        if (geometry instanceof QuadMesh) {
            return WebGLQuadMeshRenderer.meshKey(geometry);
        }
        if (geometry instanceof TriangleStripMesh) {
            return WebGLTriangleStripMeshRenderer.meshKey(geometry);
        }
        if (geometry instanceof FunctionalExplicitSurface) {
            return WebGLFunctionalExplicitSurfaceRenderer.meshKey(geometry);
        }
        if (geometry instanceof ParametricBiCubicPatch) {
            return WebGLParametricBiCubicPatchRenderer.meshKey(geometry);
        }
        if (geometry instanceof SubdividedBezierPatch) {
            return WebGLSubdividedBezierPatchRenderer.meshKey(geometry);
        }
        if (geometry instanceof InfinitePlane) {
            return WebGLInfinitePlaneRenderer.meshKey(geometry);
        }
        if (geometry instanceof QuadricSurface) {
            return WebGLQuadricSurfaceRenderer.meshKey(geometry);
        }
        if (geometry instanceof ImplicitSolid) {
            return WebGLImplicitSolidRenderer.meshKey(geometry);
        }
        if (geometry instanceof HeightFieldSolid) {
            return WebGLHeightFieldSolidRenderer.meshKey(geometry);
        }
        return null;
    }

    /**
    @return the mesh of the geometry, built by its renderer, or null if it
    can not be tessellated
    */
    private static buildMesh(geometry: Geometry): WebGLMesh | null {
        if (geometry instanceof Cone) {
            return WebGLConeRenderer.buildMesh(geometry);
        }
        if (geometry instanceof Arrow) {
            return WebGLArrowRenderer.buildMesh(geometry);
        }
        if (geometry instanceof Box) {
            return WebGLBoxRenderer.buildMesh(geometry);
        }
        if (geometry instanceof Torus) {
            return WebGLTorusRenderer.buildMesh(geometry);
        }
        if (geometry instanceof TriangleMesh) {
            return WebGLTriangleMeshRenderer.buildMesh(geometry);
        }
        if (geometry instanceof QuadMesh) {
            return WebGLQuadMeshRenderer.buildMesh(geometry);
        }
        if (geometry instanceof TriangleStripMesh) {
            return WebGLTriangleStripMeshRenderer.buildMesh(geometry);
        }
        if (geometry instanceof FunctionalExplicitSurface) {
            return WebGLFunctionalExplicitSurfaceRenderer.buildMesh(geometry);
        }
        if (geometry instanceof ParametricBiCubicPatch) {
            return WebGLParametricBiCubicPatchRenderer.buildMesh(geometry);
        }
        if (geometry instanceof SubdividedBezierPatch) {
            return WebGLSubdividedBezierPatchRenderer.buildMesh(geometry);
        }
        if (geometry instanceof InfinitePlane) {
            return WebGLInfinitePlaneRenderer.buildMesh(geometry);
        }
        if (geometry instanceof QuadricSurface) {
            return WebGLQuadricSurfaceRenderer.buildMesh(geometry);
        }
        if (geometry instanceof ImplicitSolid) {
            return WebGLImplicitSolidRenderer.buildMesh(geometry);
        }
        if (geometry instanceof HeightFieldSolid) {
            return WebGLHeightFieldSolidRenderer.buildMesh(geometry);
        }
        return null;
    }
}
