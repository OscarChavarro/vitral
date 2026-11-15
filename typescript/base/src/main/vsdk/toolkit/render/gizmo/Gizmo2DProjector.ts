import { VSDK } from "../../common/VSDK.js";
import type { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "../../common/linealAlgebra/Vector4Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import type { Gizmo2DDrawList } from "./Gizmo2DDrawList.js";

/**
Port of `vsdk.toolkit.render.gizmo.Gizmo2DProjector`.

Geometric stage of the 2D gizmo pipeline [FOLE1992].6: takes the world space
primitives the gizmos build for the OpenGL renderers (triangle strips, fans,
segments, see `GizmoSolidTessellator`), transforms them with the projection
matrix of a camera, clips them in homogeneous coordinates against the six
planes `-w <= x, y, z <= w` (Sutherland-Hodgman for polygons) and maps the
result to the pixels of the viewport of the camera, adding them to a
`Gizmo2DDrawList`.

Pixels have the origin at the upper left corner of the viewport and grow
downwards; the viewport size is the one of the camera (see
`Camera.updateViewportResize`).
*/
export class Gizmo2DProjector {
    private static readonly CLIP_PLANES: readonly (readonly number[])[] = [
        [1.0, 0.0, 0.0, 1.0],
        [-1.0, 0.0, 0.0, 1.0],
        [0.0, 1.0, 0.0, 1.0],
        [0.0, -1.0, 0.0, 1.0],
        [0.0, 0.0, 1.0, 1.0],
        [0.0, 0.0, -1.0, 1.0],
    ];

    private readonly projection: Matrix4x4d;
    private readonly width: number;
    private readonly height: number;
    private readonly gizmo2DDrawList: Gizmo2DDrawList;

    /**
    @param camera camera viewing the gizmo, with its viewport size updated
    @param gizmo2DDrawList list receiving the projected primitives
    */
    public constructor(camera: Camera, gizmo2DDrawList: Gizmo2DDrawList) {
        this.projection = camera.calculateProjectionMatrix();
        this.width = camera.getViewportXSize();
        this.height = camera.getViewportYSize();
        this.gizmo2DDrawList = gizmo2DDrawList;
    }

    public getGizmo2DDrawList(): Gizmo2DDrawList {
        return this.gizmo2DDrawList;
    }

    //= Polygons ==========================================================

    /**
    Adds a simple polygon (convex or not), given by its vertices in order.
    */
    public addPolygon(vertices: Vector3Dd[], color: ColorRgb, opacity: number): void {
        let polygon: Vector4Dd[] = [];

        for (const v of vertices) {
            polygon.push(this.projection.multiply(new Vector4Dd(v)));
        }
        for (const plane of Gizmo2DProjector.CLIP_PLANES) {
            polygon = Gizmo2DProjector.clip(polygon, plane);
            if (polygon.length < 3) {
                return;
            }
        }

        const n = polygon.length;
        const x: number[] = new Array<number>(n);
        const y: number[] = new Array<number>(n);
        let depth = 0;

        for (let i = 0; i < n; i++) {
            const ndc = polygon[i]!.dividedByW();
            x[i] = this.toPixelX(ndc.x());
            y[i] = this.toPixelY(ndc.y());
            depth += ndc.z();
        }
        this.gizmo2DDrawList.addPolygon(x, y, depth / n, color, opacity);
    }

    /**
    Adds a triangle strip, as one quad for each pair of consecutive edges (a
    strip of 4 vertices is one quad), which are drawn with fewer seams than
    its triangles.
    */
    public addStrip(strip: Vector3Dd[], color: ColorRgb, opacity: number): void {
        for (let i = 0; i + 3 < strip.length; i += 2) {
            this.addPolygon([strip[i]!, strip[i + 1]!, strip[i + 3]!,
                strip[i + 2]!], color, opacity);
        }
        if (strip.length % 2 === 1 && strip.length >= 3) {
            const last = strip.length - 1;
            this.addPolygon([strip[last - 2]!, strip[last - 1]!,
                strip[last]!], color, opacity);
        }
    }

    /**
    Adds a triangle fan as its triangles (the first vertex is the center or
    apex of the fan).
    */
    public addFanTriangles(fan: Vector3Dd[], color: ColorRgb, opacity: number): void {
        for (let i = 1; i + 1 < fan.length; i++) {
            this.addPolygon([fan[0]!, fan[i]!, fan[i + 1]!], color, opacity);
        }
    }

    /**
    Adds a planar triangle fan as one polygon (its center followed by its
    perimeter), so a translucent fan is not darker along its inner edges.
    */
    public addFanAsPolygon(fan: Vector3Dd[], color: ColorRgb, opacity: number): void {
        if (fan.length >= 3) {
            this.addPolygon(fan, color, opacity);
        }
    }

    /**
    Adds a list of independent triangles (3 vertices each).
    */
    public addTriangles(triangles: Vector3Dd[], color: ColorRgb, opacity: number): void {
        for (let i = 0; i + 2 < triangles.length; i += 3) {
            this.addPolygon([triangles[i]!, triangles[i + 1]!,
                triangles[i + 2]!], color, opacity);
        }
    }

    //= Lines and points ==================================================

    /**
    Adds a segment.
    @param depthBias added to its depth (negative is towards the viewer)
    */
    public addSegment(p0: Vector3Dd, p1: Vector3Dd, color: ColorRgb,
                      lineWidth: number, depthBias: number): void {
        let start = this.projection.multiply(new Vector4Dd(p0));
        let end = this.projection.multiply(new Vector4Dd(p1));

        for (const plane of Gizmo2DProjector.CLIP_PLANES) {
            const d0 = Gizmo2DProjector.evaluate(plane, start);
            const d1 = Gizmo2DProjector.evaluate(plane, end);

            if (d0 < 0.0 && d1 < 0.0) {
                return;
            }
            if (d0 < 0.0 || d1 < 0.0) {
                const denominator = d0 - d1;
                if (Math.abs(denominator) < VSDK.EPSILON) {
                    return;
                }
                const intersection = Gizmo2DProjector.interpolate(start, end, d0 / denominator);
                if (d0 < 0.0) {
                    start = intersection;
                }
                else {
                    end = intersection;
                }
            }
        }
        const a = start.dividedByW();
        const b = end.dividedByW();
        this.gizmo2DDrawList.addLine(this.toPixelX(a.x()), this.toPixelY(a.y()), this.toPixelX(b.x()),
            this.toPixelY(b.y()), (a.z() + b.z()) / 2 + depthBias, color, lineWidth);
    }

    /**
    @param p point in world space
    @return its pixel [x, y] in the viewport, or null if it is behind the
    camera or outside of the depth range of the view volume (it may be
    outside the sides of the viewport)
    */
    public projectPoint(p: Vector3Dd): number[] | null {
        const clip = this.projection.multiply(new Vector4Dd(p));

        if (clip.w() <= VSDK.EPSILON || clip.z() < -clip.w() || clip.z() > clip.w()) {
            return null;
        }
        const ndc = clip.dividedByW();
        return [this.toPixelX(ndc.x()), this.toPixelY(ndc.y())];
    }

    //=====================================================================

    private toPixelX(ndcX: number): number {
        return (ndcX + 1.0) / 2.0 * this.width;
    }

    private toPixelY(ndcY: number): number {
        return (1.0 - ndcY) / 2.0 * this.height;
    }

    /**
    Sutherland-Hodgman clipping of a polygon against one plane of the
    homogeneous view volume.
    */
    private static clip(polygon: Vector4Dd[], plane: readonly number[]): Vector4Dd[] {
        const result: Vector4Dd[] = [];
        const n = polygon.length;

        for (let i = 0; i < n; i++) {
            const current = polygon[i]!;
            const next = polygon[(i + 1) % n]!;
            const dc = Gizmo2DProjector.evaluate(plane, current);
            const dn = Gizmo2DProjector.evaluate(plane, next);

            if (dc >= 0.0) {
                result.push(current);
            }
            if ((dc >= 0.0) !== (dn >= 0.0)) {
                result.push(Gizmo2DProjector.interpolate(current, next, dc / (dc - dn)));
            }
        }
        return result;
    }

    private static interpolate(a: Vector4Dd, b: Vector4Dd, t: number): Vector4Dd {
        return a.multiply(1.0 - t).add(b.multiply(t));
    }

    private static evaluate(plane: readonly number[], point: Vector4Dd): number {
        return plane[0]! * point.x() + plane[1]! * point.y() +
            plane[2]! * point.z() + plane[3]! * point.w();
    }
}
