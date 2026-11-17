import {
    BlobSolid,
    PolynomialSurface,
    identityHashCode,
    type BlobComponent,
    type ImplicitSolid,
    type Vector3Dd,
    type Camera,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "../WebGLGeometryRenderer.js";
import type { WebGLMesh } from "../WebGLMeshRenderer.js";
import { _WebGLSolidBoundaryMeshBuilder } from "./_WebGLSolidBoundaryMeshBuilder.js";

/**
Port of `vsdk.toolkit.render.jogl.volume.Jogl4ImplicitSolidRenderer`.

Renders an `ImplicitSolid` (i.e. a `BlobSolid` or a `PolynomialSurface`)
with the WebGL pipeline, from the `SolidBoundaryTessellator` mesh of its
bounds (see `_WebGLSolidBoundaryMeshBuilder`), cached by
`WebGLGeometryRenderer`. Blobs and polynomial surfaces are identified by
their parameters (so editing them produces a new mesh), other implicit solids
by identity.

Java's overloads for a single light and for a list of lights are one method
here, taken by the kind of the `lights` argument (see
`WebGLMeshRenderer.draw`); drawing is asynchronous because a GLSL source
arrives over `fetch`. The keys are made of JavaScript number texts instead of Java's
`Double.toString` ones; they are only compared with each other.
`Arrays.toString(double[])` is the bracketed, comma separated list it
prints.
*/
export class WebGLImplicitSolidRenderer {
    private constructor() {}

    /**
    Draws the solid, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        solid: ImplicitSolid,
        camera: Camera | null,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration | null,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLGeometryRenderer.draw(
            gl,
            solid,
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
    @param solid an implicit solid
    @return a string that identifies the mesh of the solid
    */
    public static meshKey(solid: ImplicitSolid): string {
        if (solid instanceof BlobSolid) {
            let key: string = "blob/";
            key += solid.getThreshold();
            for (let i: number = 0; i < solid.getComponentCount(); i++) {
                const c: BlobComponent = solid.getComponent(i);
                key +=
                    "/" + WebGLImplicitSolidRenderer.keyOf(c.getCenter()) + "/" + c.getRadius() + "/" + c.getStrength();
            }
            return key + "/" + _WebGLSolidBoundaryMeshBuilder.RESOLUTION;
        }
        if (solid instanceof PolynomialSurface) {
            return (
                "polynomial/" +
                solid.getOrder() +
                "/" +
                "[" +
                solid.getCoefficients().join(", ") +
                "]" +
                "/" +
                _WebGLSolidBoundaryMeshBuilder.RESOLUTION
            );
        }
        return "tessellatedsolid/" + identityHashCode(solid) + "/" + _WebGLSolidBoundaryMeshBuilder.RESOLUTION;
    }

    /**
    @param solid solid to tessellate
    @return the mesh of its boundary, or null if it can not be tessellated
    */
    public static buildMesh(solid: ImplicitSolid): WebGLMesh | null {
        return _WebGLSolidBoundaryMeshBuilder.buildMesh(solid);
    }

    /** Exact text of the coordinates, as `Vector3Dd.toString` rounds them */
    private static keyOf(v: Vector3Dd): string {
        return v.x() + "," + v.y() + "," + v.z();
    }
}
