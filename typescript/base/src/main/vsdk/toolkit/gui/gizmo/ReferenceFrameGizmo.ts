import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { ViewportElementScaler } from "../viewport/ViewportElementScaler.js";
import { Gizmo } from "./Gizmo.js";

/**
Gizmo that represents the orientation of the world reference frame (X, Y and
Z axes) as seen from a camera. It is designed to be drawn as a small
indicator over a corner of a viewport, as done by 3D modeling programs.

This class holds the model needed to draw the gizmo (size, axes, colors,
labels and label positions) and the operations to estimate the rotation the
axes must have to be shown from a given camera. It does not depend on any
rendering technology: renderers such as `Jogl4ReferenceFrameGizmoRenderer`
are responsible for the actual painting.

The gizmo is defined in a canonical space where the origin is at the center
of the square area of `getSizeInPixels()` pixels devoted to it, and the
visible axes have length `getAxisLength()` (the area covers the range
[-1, 1] in each direction).
*/
export class ReferenceFrameGizmo extends Gizmo {
    public static readonly AXIS_X = 0;
    public static readonly AXIS_Y = 1;
    public static readonly AXIS_Z = 2;
    public static readonly NUMBER_OF_AXES = 3;

    public static readonly DEFAULT_SIZE_IN_PIXELS = 64;
    public static readonly DEFAULT_AXIS_LENGTH = 1.0;
    public static readonly DEFAULT_LINE_WIDTH = 1.0;

    private static readonly DEFAULT_AXIS_COLORS: readonly ColorRgb[] = [
        new ColorRgb(0.78, 0, 0),
        new ColorRgb(0, 0.61, 0),
        new ColorRgb(0, 0, 0.76),
    ];
    private static readonly DEFAULT_AXIS_LABELS: readonly string[] = ["X", "Y", "Z"];

    private sizeInPixels: number;
    private axisLength: number;
    private lineWidth: number;
    private visible: boolean;
    private readonly axisColors: ColorRgb[];
    private readonly axisLabels: string[];

    public constructor() {
        super();
        this.sizeInPixels = ReferenceFrameGizmo.DEFAULT_SIZE_IN_PIXELS;
        this.axisLength = ReferenceFrameGizmo.DEFAULT_AXIS_LENGTH;
        this.lineWidth = ReferenceFrameGizmo.DEFAULT_LINE_WIDTH;
        this.visible = true;
        this.axisColors = ReferenceFrameGizmo.DEFAULT_AXIS_COLORS.slice();
        this.axisLabels = ReferenceFrameGizmo.DEFAULT_AXIS_LABELS.slice();
    }

    /**
    @return the width and height, in pixels, of the square area where the
    gizmo is drawn
    */
    public getSizeInPixels(): number {
        return this.sizeInPixels;
    }

    /**
    @param sizeInPixels width and height, in pixels, of the square area where
    the gizmo is drawn; not positive values are ignored
    */
    public setSizeInPixels(sizeInPixels: number): void {
        if (sizeInPixels > 0) {
            this.sizeInPixels = sizeInPixels;
        }
    }

    /**
    @return the length of the axes, in the canonical space of the gizmo
    */
    public getAxisLength(): number {
        return this.axisLength;
    }

    /**
    @param axisLength length of the axes, in the canonical space of the gizmo;
    not positive values are ignored
    */
    public setAxisLength(axisLength: number): void {
        if (axisLength > 0.0) {
            this.axisLength = axisLength;
        }
    }

    /**
    @return the width, in pixels, of the lines that draw the axes
    */
    public getLineWidth(): number {
        return this.lineWidth;
    }

    /**
    @param lineWidth width, in pixels, of the lines that draw the axes; not
    positive values are ignored
    */
    public setLineWidth(lineWidth: number): void {
        if (lineWidth > 0.0) {
            this.lineWidth = lineWidth;
        }
    }

    /**
    Sets the size of the area and the line width of the gizmo to the values
    that make it look proportional to the screen resolution: the default
    values (designed for legacy resolutions) multiplied by the scale of the
    given scaler.

    @param scaler scaler informed of the resolution of the screen
    */
    public applyScale(scaler: ViewportElementScaler | null): void {
        if (scaler === null) {
            return;
        }
        this.setSizeInPixels(scaler.scaleSize(ReferenceFrameGizmo.DEFAULT_SIZE_IN_PIXELS));
        this.setLineWidth(scaler.scaleLength(ReferenceFrameGizmo.DEFAULT_LINE_WIDTH));
    }

    public isVisible(): boolean {
        return this.visible;
    }

    public setVisible(visible: boolean): void {
        this.visible = visible;
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the end point of the axis, in the canonical space of the gizmo
    */
    public getAxisEnd(axis: number): Vector3Dd {
        ReferenceFrameGizmo.checkAxis(axis);
        return ReferenceFrameGizmo.unitVector(axis).multiply(this.axisLength);
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the color used to draw the axis and its label
    */
    public getAxisColor(axis: number): ColorRgb {
        ReferenceFrameGizmo.checkAxis(axis);
        return this.axisColors[axis]!;
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the text that names the axis
    */
    public getAxisLabel(axis: number): string {
        ReferenceFrameGizmo.checkAxis(axis);
        return this.axisLabels[axis]!;
    }

    /**
    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @return the position, in the canonical space of the gizmo, where the label
    of the axis must be anchored: the end of the axis
    */
    public getLabelPosition(axis: number): Vector3Dd {
        return this.getAxisEnd(axis);
    }

    /**
    Estimates the rotation to apply to the axes of the gizmo (defined in world
    space) so they are seen from a camera with the given orientation: the
    inverse of the camera rotation, composed with the change of convention
    between the camera frame (front, left, up) and the canonical space of the
    gizmo, where the screen plane is XY and Z points to the viewer.

    @param cameraRotation rotation of the camera, as given by
    `Camera.getRotation()`
    @return the rotation matrix to apply to the axes of the gizmo
    */
    public estimateOrientation(cameraRotation: Matrix4x4d): Matrix4x4d {
        const viewRotation: Matrix4x4d = new Matrix4x4d()
            .axisRotation(JavaMath.toRadians(90), -1, 0, 0)
            .multiply(new Matrix4x4d().axisRotation(JavaMath.toRadians(90), 0, 0, 1));

        return viewRotation.multiply(cameraRotation.invert());
    }

    /**
    Estimates the direction in which an axis of the gizmo points, once the
    orientation for the given camera is applied.

    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @param cameraRotation rotation of the camera, as given by
    `Camera.getRotation()`
    @return an unit vector with the estimated direction of the axis
    */
    public estimateAxisDirection(axis: number, cameraRotation: Matrix4x4d): Vector3Dd {
        ReferenceFrameGizmo.checkAxis(axis);
        return this.estimateOrientation(cameraRotation).multiply(ReferenceFrameGizmo.unitVector(axis)).normalized();
    }

    /**
    Builds the geometry to draw an axis as a line with thickness, seen from a
    camera: a triangle strip (a rectangle of 4 vertices, in strip order) in
    the canonical space of the gizmo, over the plane of the screen. The width
    of the rectangle is `getLineWidth()` pixels, and it is extended half of
    that width at both ends (square caps), so the joint of the axes at the
    origin has no gaps and an axis pointing to the viewer is seen as a dot.

    @param axis one of `AXIS_X`, `AXIS_Y` or `AXIS_Z`
    @param cameraRotation rotation of the camera, as given by
    `Camera.getRotation()`
    @return the 4 vertices of the triangle strip; the z coordinates keep the
    depth of the ends of the axis
    */
    public buildAxisStrip(axis: number, cameraRotation: Matrix4x4d): Vector3Dd[] {
        ReferenceFrameGizmo.checkAxis(axis);

        const orientation: Matrix4x4d = this.estimateOrientation(cameraRotation);
        const start: Vector3Dd = orientation.multiply(new Vector3Dd(0, 0, 0));
        const end: Vector3Dd = orientation.multiply(this.getAxisEnd(axis));

        let dx: number = end.x() - start.x();
        let dy: number = end.y() - start.y();
        const length: number = Math.sqrt(dx * dx + dy * dy);

        if (length < VSDK.EPSILON) {
            dx = 1;
            dy = 0;
        } else {
            dx /= length;
            dy /= length;
        }

        // One canonical unit is half the size of the area, in pixels
        const halfWidth: number = this.lineWidth / this.sizeInPixels;
        const px: number = -dy * halfWidth;
        const py: number = dx * halfWidth;
        const sx: number = start.x() - dx * halfWidth;
        const sy: number = start.y() - dy * halfWidth;
        const ex: number = end.x() + dx * halfWidth;
        const ey: number = end.y() + dy * halfWidth;

        return [
            new Vector3Dd(sx + px, sy + py, start.z()),
            new Vector3Dd(sx - px, sy - py, start.z()),
            new Vector3Dd(ex + px, ey + py, end.z()),
            new Vector3Dd(ex - px, ey - py, end.z()),
        ];
    }

    private static unitVector(axis: number): Vector3Dd {
        return new Vector3Dd(
            axis === ReferenceFrameGizmo.AXIS_X ? 1 : 0,
            axis === ReferenceFrameGizmo.AXIS_Y ? 1 : 0,
            axis === ReferenceFrameGizmo.AXIS_Z ? 1 : 0,
        );
    }

    private static checkAxis(axis: number): void {
        if (axis < 0 || axis >= ReferenceFrameGizmo.NUMBER_OF_AXES) {
            throw new IllegalArgumentException("axis must be AXIS_X, AXIS_Y or AXIS_Z");
        }
    }
}
