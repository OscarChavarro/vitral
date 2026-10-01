import {
    Matrix4x4d,
    SpherePolyhedralCache,
    type SpherePolyhedralCacheEntry,
    type Camera,
    type Light,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
    type Sphere,
} from "@vitral/base";
import { WebGLMesh, WebGLMeshRenderer } from "./WebGLMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4SphereRenderer`.

Renders a `Sphere` as a tessellated mesh (see `WebGLMeshRenderer`). The
tessellation comes from `SpherePolyhedralCache`: the sphere is converted, in
code, into a `PolyhedralBoundedSolid` (with `slices` meridians and
`stacks - 1` bands), whose triangles carry the normals, tangents, binormals and
texture coordinates of the parametric surface. The meshes are cached by radius
and resolution, so many spheres can be drawn per frame.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `light` argument (see
`WebGLMeshRenderer.draw`): a null light, as in Java, lights the sphere from
the camera. Drawing is asynchronous because a GLSL source arrives over
`fetch`.
*/
export class WebGLSphereRenderer {
    private static readonly DEFAULT_SLICES = 32;
    private static readonly DEFAULT_STACKS = 16;

    private static readonly MESHES = new Map<string, WebGLMesh>();

    private constructor() {}

    public static async draw(
        gl: WebGL2RenderingContext,
        sphere: Sphere | null,
        camera: Camera | null,
        light: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null = null,
        normalMap: RGBImageUncompressed | null = null,
        modelViewLocal: Matrix4x4d = Matrix4x4d.identityMatrix(),
        slices: number = WebGLSphereRenderer.DEFAULT_SLICES,
        stacks: number = WebGLSphereRenderer.DEFAULT_STACKS,
    ): Promise<void> {
        if (sphere === null || camera === null || material === null || quality === null) {
            return;
        }

        await WebGLMeshRenderer.draw(
            gl,
            WebGLSphereRenderer.obtainMesh(sphere, slices, stacks),
            sphere,
            camera,
            light,
            material,
            quality,
            textureMap,
            normalMap,
            modelViewLocal,
        );
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        for (const mesh of WebGLSphereRenderer.MESHES.values()) {
            WebGLMeshRenderer.release(gl, mesh);
        }
        WebGLSphereRenderer.MESHES.clear();
        WebGLMeshRenderer.dispose(gl);
    }

    private static obtainMesh(sphere: Sphere, requestedSlices: number, requestedStacks: number): WebGLMesh {
        const slices: number = Math.max(12, requestedSlices);
        const stacks: number = Math.max(8, requestedStacks);
        const key: string = sphere.getRadius() + "/" + slices + "/" + stacks;
        let mesh: WebGLMesh | undefined = WebGLSphereRenderer.MESHES.get(key);

        if (mesh === undefined) {
            const entry: SpherePolyhedralCacheEntry =
                SpherePolyhedralCache.obtain(sphere, slices, stacks - 1);

            mesh = new WebGLMesh(entry.getPositions(), entry.getNormals(), entry.getUvs(),
                entry.getTangents(), entry.getBiNormals(), sphere.getRadius());
            WebGLSphereRenderer.MESHES.set(key, mesh);
        }
        return mesh;
    }
}
