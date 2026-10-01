import {
    Vector3Dd,
    type Camera,
    type InfinitePlane,
    type Light,
    type Matrix4x4d,
    type RendererConfiguration,
    type RGBImageUncompressed,
    type SimpleMaterial,
} from "@vitral/base";
import { WebGLGeometryRenderer } from "./WebGLGeometryRenderer.js";
import { WebGLMeshBuilder } from "./WebGLMeshBuilder.js";
import type { WebGLMesh } from "./WebGLMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4InfinitePlaneRenderer`.

Renders an `InfinitePlane` with the WebGL pipeline. As in
`Jogl2InfinitePlaneRenderer`, a finite part of the plane is shown: a square
of `SIZE` units centered at the point of the plane closest to the origin,
divided into `TILES` x `TILES` cells. The square is visible from both sides.

The bounding volume of an infinite plane is infinite, so the bounding volume
and the selection corners are drawn around the square shown.
*/
export class WebGLInfinitePlaneRenderer {
    /// Side of the square of the plane that is shown
    public static readonly SIZE: number = 20.0;
    /// Number of cells along each side of the square
    public static readonly TILES: number = 10;

    private constructor() {}

    /**
    Draws the plane, with the passes selected by the configuration (see
    `WebGLMeshRenderer`).

    @param gl WebGL context
    @param plane plane to draw
    @param camera camera that views the plane
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the plane
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from plane space to world space
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        plane: InfinitePlane,
        camera: Camera,
        lights: Light | Iterable<Light | null> | null,
        material: SimpleMaterial,
        quality: RendererConfiguration,
        textureMap: RGBImageUncompressed | null,
        normalMap: RGBImageUncompressed | null,
        localTransform: Matrix4x4d | null,
    ): Promise<void> {
        await WebGLGeometryRenderer.draw(gl, plane, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param plane a plane
    @return a string that identifies the square shown for the plane
    */
    public static meshKey(plane: InfinitePlane): string {
        return "infiniteplane/" + plane.getA() + "/" + plane.getB() + "/" +
            plane.getC() + "/" + plane.getD();
    }

    /**
    @param plane a plane
    @return the bounding box of the square shown for the plane, as given by
    `Geometry.getMinMax()`
    */
    public static calculateShownMinMax(plane: InfinitePlane): number[] {
        const frame: Vector3Dd[] = WebGLInfinitePlaneRenderer.calculateFrame(plane);
        const minmax: number[] = [
            Number.MAX_VALUE, Number.MAX_VALUE, Number.MAX_VALUE,
            -Number.MAX_VALUE, -Number.MAX_VALUE, -Number.MAX_VALUE,
        ];
        const h: number = WebGLInfinitePlaneRenderer.SIZE / 2;

        for (let i: number = -1; i <= 1; i += 2) {
            for (let j: number = -1; j <= 1; j += 2) {
                const p: Vector3Dd = frame[0]!.add(frame[1]!.multiply(i * h)).add(frame[2]!.multiply(j * h));
                const c: number[] = [p.x(), p.y(), p.z()];

                for (let k: number = 0; k < 3; k++) {
                    minmax[k] = Math.min(minmax[k]!, c[k]!);
                    minmax[k + 3] = Math.max(minmax[k + 3]!, c[k]!);
                }
            }
        }
        return minmax;
    }

    /**
    @param plane plane to tessellate
    @return the mesh of the square shown for the plane
    */
    public static buildMesh(plane: InfinitePlane): WebGLMesh {
        const frame: Vector3Dd[] = WebGLInfinitePlaneRenderer.calculateFrame(plane);
        const center: Vector3Dd = frame[0]!;
        const u: Vector3Dd = frame[1]!;
        const v: Vector3Dd = frame[2]!;
        const n: Vector3Dd = frame[3]!;
        const tiles: number = WebGLInfinitePlaneRenderer.TILES;
        const h: number = WebGLInfinitePlaneRenderer.SIZE / 2;
        const d: number = WebGLInfinitePlaneRenderer.SIZE / tiles;
        const builder: WebGLMeshBuilder = new WebGLMeshBuilder(WebGLInfinitePlaneRenderer.SIZE);

        builder.setDoubleSided(true);
        for (let i: number = 0; i < tiles; i++) {
            const a0: number = -h + i * d;
            const a1: number = a0 + d;

            for (let j: number = 0; j < tiles; j++) {
                const b0: number = -h + j * d;
                const b1: number = b0 + d;

                // (u, v, n) is right handed: counterclockwise seen from n
                builder.addQuad(
                    center.add(u.multiply(a0)).add(v.multiply(b0)), n,
                    i / tiles, j / tiles,
                    center.add(u.multiply(a1)).add(v.multiply(b0)), n,
                    (i + 1) / tiles, j / tiles,
                    center.add(u.multiply(a1)).add(v.multiply(b1)), n,
                    (i + 1) / tiles, (j + 1) / tiles,
                    center.add(u.multiply(a0)).add(v.multiply(b1)), n,
                    i / tiles, (j + 1) / tiles);
            }
        }
        return builder.build();
    }

    /**
    @return the point of the plane closest to the origin, two orthonormal
    directions (u, v) on it and its normal n, with (u, v, n) right handed
    */
    private static calculateFrame(plane: InfinitePlane): Vector3Dd[] {
        const n: Vector3Dd = plane.getNormal();
        const length: number = Math.sqrt(plane.getA() * plane.getA() +
            plane.getB() * plane.getB() + plane.getC() * plane.getC());
        // The plane is a x + b y + c z + d = 0; (a, b, c) may not be unitary
        const center: Vector3Dd = n.multiply(length > 0 ? -plane.getD() / length : 0);
        const reference: Vector3Dd = Math.abs(n.z()) < 0.9 ? new Vector3Dd(0, 0, 1) : new Vector3Dd(1, 0, 0);
        const u: Vector3Dd = reference.crossProduct(n).normalized();
        const v: Vector3Dd = n.crossProduct(u).normalized();

        return [center, u, v, n];
    }
}
