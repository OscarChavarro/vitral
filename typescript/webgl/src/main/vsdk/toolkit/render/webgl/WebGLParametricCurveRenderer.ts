import {
    ColorRgb,
    Matrix4x4d,
    ParametricCurve,
    type Camera,
    type RendererConfiguration,
    type Vector3Dd,
} from "@vitral/base";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";
import { WebGLMinMaxRenderer } from "./WebGLMinMaxRenderer.js";
import { WebGLSelectionCornersRenderer } from "./WebGLSelectionCornersRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4ParametricCurveRenderer`.

Renders a `ParametricCurve` with the WebGL pipeline. As in
`Jogl2ParametricCurveRenderer`, each segment of the curve (segments ending at
a `BREAK` point are skipped) is approximated by the polyline given by
`ParametricCurve.calculatePoints` and drawn, unlit, with the wire color of the
configuration, whatever its surface and wire bits are (a curve has no
surface). The points bit shows the vertices of the polylines, and the
bounding volume and selection corners bits are honored as for any geometry.

Java's two overloads (with and without a color) are one method here, with an
optional color.
*/
export class WebGLParametricCurveRenderer {
    private static readonly POINT_SIZE: number = 4.0;
    private static readonly POINT_RGB: readonly number[] = [1.0, 0.0, 0.0];

    private constructor() {}

    /**
    Draws the curve.

    @param gl WebGL context
    @param curve curve to draw
    @param camera camera that views the curve
    @param quality bits of rendering configuration
    @param localTransform transformation from curve space to world space; null
    for the identity
    @param color color of the curve; when not given, the wire color of the
    configuration
    */
    public static async draw(
        gl: WebGL2RenderingContext | null,
        curve: ParametricCurve | null,
        camera: Camera | null,
        quality: RendererConfiguration | null,
        localTransform: Matrix4x4d | null,
        color?: ColorRgb,
    ): Promise<void> {
        const c: ColorRgb = color !== undefined
            ? color
            : (quality !== null && quality.getWireColor() !== null
                ? quality.getWireColor()
                : new ColorRgb(1, 1, 1));

        if (gl === null || curve === null || camera === null || quality === null ||
            curve.types === null || curve.types.length < 2) {
            return;
        }
        const local: Matrix4x4d = localTransform !== null ? localTransform : Matrix4x4d.identityMatrix();
        const mvp: Matrix4x4d = camera.calculateProjectionMatrix().multiply(local);
        const vertices: Vector3Dd[] = [];
        const positions: Float32Array = WebGLParametricCurveRenderer.buildSegmentPositions(curve, vertices);

        gl.enable(gl.DEPTH_TEST);
        gl.depthFunc(gl.LEQUAL);
        if (positions.length > 0) {
            await WebGLLineRenderer.drawLines(gl, mvp, positions,
                WebGLParametricCurveRenderer.buildUniformColors(positions.length / 3, c.r(), c.g(), c.b()),
                1.0);
        }
        if (quality.isPointsSet() && vertices.length > 0) {
            await WebGLParametricCurveRenderer.drawPoints(gl, mvp, vertices);
        }
        if (quality.isBoundingVolumeSet()) {
            await WebGLMinMaxRenderer.draw(gl, curve, camera, local);
        }
        if (quality.isSelectionCornersSet()) {
            await WebGLSelectionCornersRenderer.draw(gl, curve, camera, local);
        }
        gl.depthFunc(gl.LESS);
    }

    /**
    @param curve curve to approximate
    @param vertices receives the vertices of the polylines
    @return x,y,z of the ends of the line segments of the polylines of the
    segments of the curve
    */
    private static buildSegmentPositions(curve: ParametricCurve, vertices: Vector3Dd[]): Float32Array {
        const ends: Vector3Dd[] = [];

        for (let i: number = 1; i < curve.types.length; i++) {
            if (curve.types[i] === ParametricCurve.BREAK) {
                i++;
                continue;
            }
            const polyline: Vector3Dd[] = curve.calculatePoints(i, false);

            for (let j: number = 0; j + 1 < polyline.length; j++) {
                ends.push(polyline[j]!);
                ends.push(polyline[j + 1]!);
            }
            vertices.push(...polyline);
        }

        const positions: Float32Array = new Float32Array(ends.length * 3);

        for (let k: number = 0; k < ends.length; k++) {
            positions[3 * k] = ends[k]!.x();
            positions[3 * k + 1] = ends[k]!.y();
            positions[3 * k + 2] = ends[k]!.z();
        }
        return positions;
    }

    /**
    Draws the vertices of the polylines as small crosses, as the points of
    `Jogl2ParametricCurveRenderer` are, sized in world units relative to the
    curve.
    */
    private static async drawPoints(gl: WebGL2RenderingContext, mvp: Matrix4x4d, vertices: Vector3Dd[]): Promise<void> {
        const minmax: number[] = WebGLParametricCurveRenderer.boundsOf(vertices);
        const size: number = Math.max(minmax[3]! - minmax[0]!,
            Math.max(minmax[4]! - minmax[1]!, minmax[5]! - minmax[2]!));
        const h: number = Math.fround(Math.max(size, 1e-6) * 0.01);
        const positions: Float32Array = new Float32Array(vertices.length * 6 * 3);
        let k: number = 0;

        for (const v of vertices) {
            const x: number = v.x();
            const y: number = v.y();
            const z: number = v.z();
            const axes: number[][] = [[h, 0, 0], [0, h, 0], [0, 0, h]];

            for (const a of axes) {
                positions[k++] = x - a[0]!;
                positions[k++] = y - a[1]!;
                positions[k++] = z - a[2]!;
                positions[k++] = x + a[0]!;
                positions[k++] = y + a[1]!;
                positions[k++] = z + a[2]!;
            }
        }
        const rgb: readonly number[] = WebGLParametricCurveRenderer.POINT_RGB;
        await WebGLLineRenderer.drawLines(gl, mvp, positions,
            WebGLParametricCurveRenderer.buildUniformColors(positions.length / 3, rgb[0]!, rgb[1]!, rgb[2]!),
            Math.max(1.0, WebGLParametricCurveRenderer.POINT_SIZE / 2));
    }

    private static boundsOf(vertices: Vector3Dd[]): number[] {
        const minmax: number[] = [
            Number.MAX_VALUE, Number.MAX_VALUE, Number.MAX_VALUE,
            -Number.MAX_VALUE, -Number.MAX_VALUE, -Number.MAX_VALUE,
        ];

        for (const v of vertices) {
            minmax[0] = Math.min(minmax[0]!, v.x());
            minmax[1] = Math.min(minmax[1]!, v.y());
            minmax[2] = Math.min(minmax[2]!, v.z());
            minmax[3] = Math.max(minmax[3]!, v.x());
            minmax[4] = Math.max(minmax[4]!, v.y());
            minmax[5] = Math.max(minmax[5]!, v.z());
        }
        return minmax;
    }

    private static buildUniformColors(vertexCount: number, r: number, g: number, b: number): Float32Array {
        const colors: Float32Array = new Float32Array(vertexCount * 3);

        for (let i: number = 0; i < vertexCount; i++) {
            colors[3 * i] = r;
            colors[3 * i + 1] = g;
            colors[3 * i + 2] = b;
        }
        return colors;
    }
}
