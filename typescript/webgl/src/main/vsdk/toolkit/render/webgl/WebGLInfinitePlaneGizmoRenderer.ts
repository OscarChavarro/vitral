import { Camera, ColorRgb, InfinitePlaneGizmo, Matrix4x4d, VSDK, Vector3Dd } from "@vitral/base";
import { WebGLCameraRenderer } from "./WebGLCameraRenderer.js";
import { WebGLLineRenderer } from "./WebGLLineRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4InfinitePlaneGizmoRenderer`.

An infinite plane is drawn as the outline of a square patch of it, sized so it
always covers the same fraction of the viewport: the tangent frame, the
area-fraction sizing with its minimum projected cosine, the orthogonal and
perspective branches of the visible-height calculation, and the four edges are
the Java ones.

The boundaries crossed are this package's recurring ones: the GLSL source of
the line renderer arrives over `fetch`, so drawing is asynchronous, and Java's
static `Jogl4LineRenderer` state is held per `WebGL2RenderingContext`. Java's
`LINE_WIDTH` of 2.0 is issued as it is; `WebGLLineRenderer` answers a width
above 1.0 with its host-side quad expansion, since no browser engine supports a
wide `gl.lineWidth`.
*/
export class WebGLInfinitePlaneGizmoRenderer {
    private static readonly VIEWPORT_AREA_FRACTION = 0.125;
    private static readonly MIN_PROJECTED_COSINE = 0.1;
    private static readonly LINE_WIDTH = 2.0;

    private constructor() {}

    public static async draw(
        gl: WebGL2RenderingContext,
        gizmo: InfinitePlaneGizmo | null,
        camera: Camera | null,
    ): Promise<void> {
        if (gizmo === null || camera === null || !gizmo.isVisible()) {
            return;
        }

        const center: Vector3Dd = gizmo.getPoint();
        const normal: Vector3Dd = gizmo.getNormal();
        if (normal.length() < VSDK.EPSILON) {
            return;
        }

        camera.updateVectors();
        const n: Vector3Dd = normal.normalized();
        const u: Vector3Dd = WebGLInfinitePlaneGizmoRenderer.buildTangent(n, camera);
        const v: Vector3Dd = n.crossProduct(u).normalized();
        const halfSide: number = WebGLInfinitePlaneGizmoRenderer.calculateHalfSide(camera, center, n);

        const p0: Vector3Dd = center.add(u.multiply(-halfSide)).add(v.multiply(-halfSide));
        const p1: Vector3Dd = center.add(u.multiply(halfSide)).add(v.multiply(-halfSide));
        const p2: Vector3Dd = center.add(u.multiply(halfSide)).add(v.multiply(halfSide));
        const p3: Vector3Dd = center.add(u.multiply(-halfSide)).add(v.multiply(halfSide));

        const color: ColorRgb = gizmo.getFrameColor();

        const positions = new Float32Array(24);
        const colors = new Float32Array(24);
        WebGLInfinitePlaneGizmoRenderer.addLine(positions, colors, 0, p0, p1, color);
        WebGLInfinitePlaneGizmoRenderer.addLine(positions, colors, 6, p1, p2, color);
        WebGLInfinitePlaneGizmoRenderer.addLine(positions, colors, 12, p2, p3, color);
        WebGLInfinitePlaneGizmoRenderer.addLine(positions, colors, 18, p3, p0, color);

        const projection: Matrix4x4d = WebGLCameraRenderer.activate(gl, camera);
        await WebGLLineRenderer.drawLines(
            gl,
            projection,
            positions,
            colors,
            WebGLInfinitePlaneGizmoRenderer.LINE_WIDTH,
        );
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        WebGLLineRenderer.release(gl);
    }

    private static buildTangent(normal: Vector3Dd, camera: Camera): Vector3Dd {
        let tangent: Vector3Dd = normal.crossProduct(camera.getFront());
        if (tangent.length() < VSDK.EPSILON) {
            tangent = normal.crossProduct(camera.getUp());
        }
        if (tangent.length() < VSDK.EPSILON) {
            tangent = normal.crossProduct(camera.getLeft());
        }
        if (tangent.length() < VSDK.EPSILON) {
            tangent = new Vector3Dd(1, 0, 0);
        }
        return tangent.normalized();
    }

    private static calculateHalfSide(camera: Camera, center: Vector3Dd, normal: Vector3Dd): number {
        const aspect: number = Math.max(1.0e-6, camera.getViewportXSize() / camera.getViewportYSize());
        let visibleHeight: number;

        if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            visibleHeight = 2.0 / Math.max(camera.getOrthogonalZoom(), 1.0e-6);
        } else {
            let depth: number = center.subtract(camera.getPosition()).dotProduct(camera.getFront());
            depth = Math.max(camera.getNearPlaneDistance(), Math.abs(depth));
            visibleHeight = 2.0 * depth * Math.tan((camera.getFov() * Math.PI) / 180.0 / 2.0);
        }
        const visibleWidth: number = visibleHeight * aspect;

        let projectedCosine: number = Math.abs(normal.normalized().dotProduct(camera.getFront().normalized()));
        projectedCosine = Math.max(WebGLInfinitePlaneGizmoRenderer.MIN_PROJECTED_COSINE, projectedCosine);
        const side: number = Math.sqrt(
            (visibleWidth * visibleHeight * WebGLInfinitePlaneGizmoRenderer.VIEWPORT_AREA_FRACTION) / projectedCosine,
        );
        return side * 0.5;
    }

    private static addLine(
        positions: Float32Array,
        colors: Float32Array,
        offset: number,
        a: Vector3Dd,
        b: Vector3Dd,
        color: ColorRgb,
    ): void {
        WebGLInfinitePlaneGizmoRenderer.addVertex(positions, colors, offset, a, color);
        WebGLInfinitePlaneGizmoRenderer.addVertex(positions, colors, offset + 3, b, color);
    }

    private static addVertex(
        positions: Float32Array,
        colors: Float32Array,
        offset: number,
        p: Vector3Dd,
        color: ColorRgb,
    ): void {
        positions[offset] = p.x();
        positions[offset + 1] = p.y();
        positions[offset + 2] = p.z();
        colors[offset] = color.r();
        colors[offset + 1] = color.g();
        colors[offset + 2] = color.b();
    }
}
