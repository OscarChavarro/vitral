import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import type { Ray } from "../../environment/geometry/element/Ray.js";
import { Box } from "../../environment/geometry/volume/Box.js";
import { Cone } from "../../environment/geometry/volume/Cone.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { KeyEvent } from "../KeyEvent.js";
import type { MouseEvent } from "../MouseEvent.js";
import type { ViewportElementScaler } from "../viewport/ViewportElementScaler.js";
import { Gizmo } from "./Gizmo.js";
import { InputGizmo } from "./InputGizmo.js";
import { InputGizmoValueChangeRules } from "./InputGizmoValueChangeRules.js";
import { ReferenceFrameGizmo } from "./ReferenceFrameGizmo.js";

/**
Java record `ScaleGizmo.ContourSegment`: a straight piece of the contour of the
flat handles, in world space, with the color it must be drawn with. The record
accessors keep their Java names.
*/
export class ContourSegment {
    public constructor(
        private readonly startPoint: Vector3Dd,
        private readonly endPoint: Vector3Dd,
        private readonly segmentColor: ColorRgb,
    ) {}

    /** @return first end of the piece */
    public start(): Vector3Dd {
        return this.startPoint;
    }

    /** @return second end of the piece */
    public end(): Vector3Dd {
        return this.endPoint;
    }

    /** @return color the piece must be drawn with */
    public color(): ColorRgb {
        return this.segmentColor;
    }
}

/**
Gizmo to specify the scale of an object along the X, Y and Z axes of its
frame (in the style of 3ds Max's scale gizmo, and following the structure of
`TranslateGizmo` and `RotateGizmo`).

- Geometric model: each axis is a straight cylinder (see `getElements`) from
  the origin of the frame out along it, capped by a small cube at its tip
  (both easy to point at with the cursor).
- Two-axis handles: for each pair of axes, a flat trapezoidal band (a quad,
  see `buildBandQuad`) contained in the plane those two axes span (XY, YZ or
  XZ): its two parallel sides join the axes at `BAND_INNER_REACH` and
  `BAND_OUTER_REACH` of their length, and its other two sides lie on the axes
  themselves. Picking one scales both of its axes (see `XY_GROUP`,
  `YZ_GROUP`, `XZ_GROUP`).
- Uniform (all-axis) handle: three triangles (see `buildUniformTriangles`),
  one in each of those same planes, all of them sharing the vertex at the
  origin of the frame and reaching the inner side of the bands, which they
  are flush with. Picking any of them scales the three axes together (see
  `UNIFORM_GROUP`).
- Size: like `TranslateGizmo` and `RotateGizmo`, the gizmo keeps an apparent
  size in pixels from its camera, scaled for the resolution of the screen
  (see `applyScale`).
- Selection: the handle under the cursor is the volatile selection and the
  one clicked is the persistent selection (see `pickElement`,
  `ScaleGizmoInteractionTechnique`). The axes the current selection scales
  (see `groupIncludesAxis`, `getCurrentSelection`) are drawn yellow instead
  of with their own color.
- Flat handles are not drawn as surfaces: only their contour is (see
  `buildContourSegments`), with each of its edges split in two halves, each
  one with the color of the axis it ends at. The handle that is the current
  selection draws its contour yellow, and only it fills its interior, with a
  translucent gray (see `HANDLE_FILL_COLOR`).
- Numeric input: the scale factors (see `getScale`) are shown and edited with
  a nested `InputGizmo` (see `getInputGizmo`), stepped by
  `InputGizmoValueChangeRules.forScale()`. It takes priority over the
  letter-driven scaling below while it consumes the key.
- Letter-driven scaling: `x`/`X`, `y`/`Y` and `z`/`Z` shrink/grow a single
  axis, and the arrow keys (when the input gizmo does not consume them, i.e.
  `Ctrl` or `Alt` is held) shrink/grow every axis uniformly.

The transformation matrix of the gizmo has the orientation and the position of
the frame its handles belong to; the scale factors themselves are kept apart
(see `getScale`), as they are not a rigid transformation of that frame.
*/
export class ScaleGizmo extends Gizmo {
    /// Number of axes, and of the values of the input gizmo
    public static readonly AXIS_COUNT = 3;

    /// Selection groups
    public static readonly NULL_GROUP = 0;
    public static readonly X_AXIS_GROUP = 1;
    public static readonly Y_AXIS_GROUP = 2;
    public static readonly Z_AXIS_GROUP = 3;
    /// Two-axis handle: the trapezoidal band in the XY plane
    public static readonly XY_GROUP = 4;
    /// Two-axis handle: the trapezoidal band in the YZ plane
    public static readonly YZ_GROUP = 5;
    /// Two-axis handle: the trapezoidal band in the XZ plane
    public static readonly XZ_GROUP = 6;
    /// All-axis handle: the three triangles that share the origin of the
    /// frame, one in each of the XY, YZ and XZ planes
    public static readonly UNIFORM_GROUP = 7;

    /// The two-axis groups, in the order `buildBandQuad` and the renderers
    /// walk them
    public static readonly BAND_GROUPS: readonly number[] = [ScaleGizmo.XY_GROUP, ScaleGizmo.YZ_GROUP, ScaleGizmo.XZ_GROUP];

    /// Indexes of `elementInstances`, in the order `getElements` gives them
    private static readonly X_SHAFT_ELEMENT = 1;
    private static readonly X_TIP_ELEMENT = 2;
    private static readonly Y_SHAFT_ELEMENT = 3;
    private static readonly Y_TIP_ELEMENT = 4;
    private static readonly Z_SHAFT_ELEMENT = 5;
    private static readonly Z_TIP_ELEMENT = 6;

    /// Size and contour width designed for legacy resolutions; the width
    /// matches TranslateGizmo#DEFAULT_LINE_WIDTH so both gizmos look
    /// visually consistent (same ribbon thickness) in the 3D UI
    public static readonly DEFAULT_APPARENT_SIZE_IN_PIXELS = 100;
    public static readonly DEFAULT_LINE_WIDTH = 1.0;

    /// Color the interior of the handle currently selected is filled with
    public static readonly HANDLE_FILL_COLOR = new ColorRgb(0.88, 0.88, 0.88);

    /// Total length of an axis (shaft and tip together), and shape of its
    /// parts, relative to the apparent size of the gizmo. The radius of the
    /// shaft is not here: it follows `lineWidth`, like the contour of the
    /// flat handles, so both look consistent (see `getLineWidthInWorldUnits`)
    private static readonly AXIS_LENGTH = 0.8;
    private static readonly TIP_SIZE = 0.06;
    /// Fractions of the length of an axis where the parallel sides of a
    /// two-axis band meet it. The inner one is also the boundary the uniform
    /// handle reaches, so band and uniform handle are flush
    private static readonly BAND_INNER_REACH = 0.52;
    private static readonly BAND_OUTER_REACH = 0.73;

    private transformationMatrix: Matrix4x4d | null;
    private camera: Camera | null = null;

    /// Scale factors shown (and edited) by the gizmo; kept apart from `transformationMatrix`,
    /// which only carries the orientation and position of its frame
    private scale: Vector3Dd;

    /// Apparent size (in legacy resolution pixels) the user has chosen
    private baseApparentSizeInPixels: number;
    /// Apparent size in pixels of the screen currently in use
    private apparentSizeInPixels: number;
    /// Width, in pixels, of the contour of the flat handles
    private lineWidth: number;
    private currentScale: number;

    /// Geometric model based in primitive instancing: primitive concretions.
    /// The two-axis bands and the uniform handle are flat polygons instead
    /// (see `buildBandQuad`, `buildUniformTriangles`)
    private readonly shaftModel: Cone;
    private readonly tipModel: Box;

    /// Geometric model based in primitive instancing: primitive instances,
    /// always of size 6, in the order given by the `*_ELEMENT` constants
    private readonly elements: SimpleBody[];

    private readonly axisColors: ColorRgb[];

    /// Interaction state
    private readonly inputGizmo: InputGizmo;
    private persistentSelection: number;
    private volatileSelection: number;

    /**
    Java's two constructors: with a camera, or with none yet (see
    `setCamera`), in which case its apparent size can not be computed until
    one is set.
    @param cam camera the gizmo is seen from, or null if it is not known yet
    */
    public constructor(cam: Camera | null = null) {
        super();
        this.transformationMatrix = new Matrix4x4d();
        this.scale = new Vector3Dd(1, 1, 1);
        this.baseApparentSizeInPixels = ScaleGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS;
        this.apparentSizeInPixels = ScaleGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS;
        this.lineWidth = ScaleGizmo.DEFAULT_LINE_WIDTH;
        this.currentScale = 1.0;
        this.persistentSelection = ScaleGizmo.NULL_GROUP;
        this.volatileSelection = ScaleGizmo.NULL_GROUP;

        const axisColorSource = new ReferenceFrameGizmo();

        this.axisColors = [];
        this.inputGizmo = new InputGizmo(ScaleGizmo.AXIS_COUNT, InputGizmo.DECIMALS, 1, InputGizmoValueChangeRules.forScale());
        for (let axis = 0; axis < ScaleGizmo.AXIS_COUNT; axis++) {
            this.axisColors[axis] = axisColorSource.getAxisColor(axis);
            this.inputGizmo.setFieldColor(axis, this.axisColors[axis]!);
        }

        const initialShaftRadius: number = this.getLineWidthInWorldUnits() / 2;
        this.shaftModel = new Cone(initialShaftRadius, initialShaftRadius, ScaleGizmo.AXIS_LENGTH - ScaleGizmo.TIP_SIZE);
        this.tipModel = new Box(ScaleGizmo.TIP_SIZE, ScaleGizmo.TIP_SIZE, ScaleGizmo.TIP_SIZE);

        this.elements = [];
        for (let i = 0; i < 6; i++) {
            this.elements.push(new SimpleBody());
        }

        this.setCamera(cam);
    }

    //= Camera and size =====================================================

    public setCamera(cam: Camera | null): void {
        this.camera = cam;
    }

    public getCamera(): Camera | null {
        return this.camera;
    }

    public getApparentSizeInPixels(): number {
        return this.apparentSizeInPixels;
    }

    public setApparentSizeInPixels(size: number): void {
        this.apparentSizeInPixels = size;
    }

    /**
    @return the apparent size the gizmo is designed to have in legacy
    resolutions, in pixels; it is the size chosen by the user, before scaling
    it for the resolution of the screen
    */
    public getBaseApparentSizeInPixels(): number {
        return this.baseApparentSizeInPixels;
    }

    /**
    @param size apparent size of the gizmo in legacy resolutions, in pixels;
    not positive values are ignored. It is used the next time `applyScale`
    is called
    */
    public setBaseApparentSizeInPixels(size: number): void {
        if (size > 0) {
            this.baseApparentSizeInPixels = size;
        }
    }

    /**
    @return the width, in pixels, of the contour of the flat handles
    */
    public getLineWidth(): number {
        return this.lineWidth;
    }

    /**
    @param lineWidth width, in pixels, of the contour of the flat handles;
    not positive values are ignored
    */
    public setLineWidth(lineWidth: number): void {
        if (lineWidth > 0.0) {
            this.lineWidth = lineWidth;
        }
    }

    /**
    Sets the apparent size and the contour width of the gizmo to the values
    that make it look proportional to the screen resolution: the base values
    (designed for legacy resolutions) multiplied by the scale of the given
    scaler. The new size is used the next time the geometry state is updated
    (see `setTransformationMatrix`, `updateGeometryState`).

    @param scaler scaler informed of the resolution of the screen
    */
    public applyScale(scaler: ViewportElementScaler | null): void {
        if (scaler === null) {
            return;
        }
        this.setApparentSizeInPixels(scaler.scaleSize(this.baseApparentSizeInPixels));
        this.setLineWidth(scaler.scaleLength(ScaleGizmo.DEFAULT_LINE_WIDTH));
    }

    /**
    @return the factor applied to the gizmo geometry so it keeps its apparent
    size in pixels, as seen from its camera
    */
    public getCurrentScale(): number {
        return this.currentScale;
    }

    /**
    @return the width of the shaft of the axes and of the contour of the flat
    handles, converted from pixels to world units, as seen from its camera at
    its current apparent size
    */
    public getLineWidthInWorldUnits(): number {
        return (this.lineWidth * this.currentScale) / this.apparentSizeInPixels;
    }

    //= Transformation and scale factors ===================================

    public getPosition(): Vector3Dd {
        return this.requireTransformationMatrix().extractTranslation();
    }

    public setPosition(p: Vector3Dd): void {
        this.setTransformationMatrix(this.requireTransformationMatrix().withTranslation(p));
    }

    /**
    Sets the frame (orientation and position) of the gizmo and recalculates
    its geometry.
    @param transformationMatrix orientation and position of the frame of the gizmo
    */
    public setTransformationMatrix(transformationMatrix: Matrix4x4d | null): void {
        this.transformationMatrix = transformationMatrix;
        this.updateGeometryState();
    }

    public getTransformationMatrix(): Matrix4x4d | null {
        return this.transformationMatrix;
    }

    private requireTransformationMatrix(): Matrix4x4d {
        if (this.transformationMatrix === null) {
            throw new TypeError("The transformation matrix of the ScaleGizmo has not been set");
        }
        return this.transformationMatrix;
    }

    /**
    @return the scale factors shown (and edited) by the gizmo, along the X, Y
    and Z axes of its frame
    */
    public getScale(): Vector3Dd {
        return this.scale;
    }

    /**
    @param scale scale factors, along the X, Y and Z axes of the frame of the
    gizmo; null is ignored
    */
    public setScale(scale: Vector3Dd | null): void {
        if (scale !== null) {
            this.scale = scale;
        }
    }

    /**
    Recalculates the apparent size and the geometry of the gizmo from its
    current transformation, apparent size, selection and camera. The size is
    kept as it was if it can not be measured, or there is no camera yet.
    PRE: the transformation matrix of the gizmo has been set.
    */
    public updateGeometryState(): void {
        this.updateScale();
        this.calculateGeometryState();
    }

    private updateScale(): void {
        if (this.camera === null || this.transformationMatrix === null) {
            return;
        }
        this.camera.updateVectors();

        const p: Vector3Dd = this.getPosition();
        const right: Vector3Dd = this.camera.getLeft().multiply(-1).normalized();
        const a: Vector3Dd | null = this.camera.projectPointUsingRayMethodResult(p);
        const b: Vector3Dd | null = this.camera.projectPointUsingRayMethodResult(p.add(right));

        if (a !== null && b !== null) {
            const factor: number = Vector3Dd.distance(a, b);

            if (factor > VSDK.EPSILON) {
                this.currentScale = this.apparentSizeInPixels / factor;
            }
        }
    }

    //= Selection ============================================================

    /**
    @return the selected group (one of the `*_GROUP` constants) chosen by the
    user with a click, or `NULL_GROUP`
    */
    public getPersistentSelection(): number {
        return this.persistentSelection;
    }

    /**
    @param selection group (one of the `*_GROUP` constants) chosen by the user
    with a click
    */
    public setPersistentSelection(selection: number): void {
        this.persistentSelection = selection;
    }

    /**
    @return the group (one of the `*_GROUP` constants) currently under the
    cursor, or `NULL_GROUP`
    */
    public getVolatileSelection(): number {
        return this.volatileSelection;
    }

    /**
    @param selection group (one of the `*_GROUP` constants) currently under
    the cursor
    */
    public setVolatileSelection(selection: number): void {
        this.volatileSelection = selection;
    }

    /**
    @return the group (one of the `*_GROUP` constants) that is highlighted and
    manipulated: the one under the cursor, or the chosen one if the cursor is
    not over any group
    */
    public getCurrentSelection(): number {
        if (this.volatileSelection === ScaleGizmo.NULL_GROUP) {
            return this.persistentSelection;
        }
        return this.volatileSelection;
    }

    /**
    @param group one of the `*_GROUP` constants
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return true if scaling with `group` changes `axis`
    */
    public static groupIncludesAxis(group: number, axis: number): boolean {
        ScaleGizmo.checkAxis(axis);
        switch (group) {
            case ScaleGizmo.X_AXIS_GROUP:
                return axis === 0;
            case ScaleGizmo.Y_AXIS_GROUP:
                return axis === 1;
            case ScaleGizmo.Z_AXIS_GROUP:
                return axis === 2;
            case ScaleGizmo.XY_GROUP:
                return axis === 0 || axis === 1;
            case ScaleGizmo.YZ_GROUP:
                return axis === 1 || axis === 2;
            case ScaleGizmo.XZ_GROUP:
                return axis === 0 || axis === 2;
            case ScaleGizmo.UNIFORM_GROUP:
                return true;
            default:
                return false;
        }
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return true if the axis is highlighted (drawn yellow) because the group
    currently selected scales it
    */
    public isAxisHighlighted(axis: number): boolean {
        return ScaleGizmo.groupIncludesAxis(this.getCurrentSelection(), axis);
    }

    /**
    @param group `XY_GROUP`, `YZ_GROUP` or `XZ_GROUP`
    @return true if the band is the current selection, so it is drawn yellow
    */
    public isBandHighlighted(group: number): boolean {
        return this.getCurrentSelection() === group;
    }

    /**
    @return true if the uniform (all-axis) handle is the current selection, so
    it is drawn yellow
    */
    public isUniformHighlighted(): boolean {
        return this.getCurrentSelection() === ScaleGizmo.UNIFORM_GROUP;
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return the color the cylinder and the cube of the axis must be drawn
    with: the one of the axis, or yellow if the current selection scales it
    */
    public getAxisDisplayColor(axis: number): ColorRgb {
        ScaleGizmo.checkAxis(axis);
        return this.isAxisHighlighted(axis) ? InputGizmo.HIGHLIGHT_COLOR : this.axisColors[axis]!;
    }

    /**
    Gives the contour of the flat handles, which are drawn as outlines and not
    as surfaces: the inner and the outer edge of each two-axis band (the inner
    one is also the boundary the uniform handle reaches). Each edge is split at
    its middle point, so each half takes the color of the axis it ends at,
    unless the handle it belongs to is the current selection: then the whole
    edge is yellow.
    PRE: the transformation matrix of the gizmo has been set.

    @return the pieces of the contour, in world space
    */
    public buildContourSegments(): ContourSegment[] {
        const segments: ContourSegment[] = [];

        for (const group of ScaleGizmo.BAND_GROUPS) {
            const axes: number[] = ScaleGizmo.bandAxes(group);
            const quad: Vector3Dd[] = this.buildBandQuad(group);

            // The inner edge is shared with the uniform handle, so either of
            // the two being selected highlights it
            this.addContourEdge(segments, quad[0]!, quad[2]!, axes, this.isBandHighlighted(group) || this.isUniformHighlighted());
            this.addContourEdge(segments, quad[1]!, quad[3]!, axes, this.isBandHighlighted(group));
        }
        return segments;
    }

    /**
    Adds the two halves of an edge that goes from the axis `axes[0]` to the
    axis `axes[1]`, each one with the color of the axis it ends at (or yellow
    if the handle is highlighted).
    */
    private addContourEdge(segments: ContourSegment[], a: Vector3Dd, b: Vector3Dd, axes: number[], highlighted: boolean): void {
        const middle: Vector3Dd = a.add(b).multiply(0.5);
        const colorA: ColorRgb = highlighted ? InputGizmo.HIGHLIGHT_COLOR : this.axisColors[axes[0]!]!;
        const colorB: ColorRgb = highlighted ? InputGizmo.HIGHLIGHT_COLOR : this.axisColors[axes[1]!]!;

        segments.push(new ContourSegment(a, middle, colorA));
        segments.push(new ContourSegment(middle, b, colorB));
    }

    /**
    Finds the handle a ray points at, using the geometric model of the gizmo:
    the axis cylinders and cubes, the two-axis bands and the triangles of the
    uniform handle.
    PRE: the transformation matrix of the gizmo has been set.

    @param ray ray, in world space, i.e. the one the cursor defines
    @return the `*_GROUP` constant of the nearest handle hit by the ray, or
    `NULL_GROUP` if it hits none
    */
    public pickElement(ray: Ray): number {
        let nearestDistance: number = Number.MAX_VALUE;
        let nearestGroup: number = ScaleGizmo.NULL_GROUP;
        let index = 1;

        for (const element of this.elements) {
            const hit: Ray | null = element.getGeometry() !== null ? element.doIntersectionFirstHit(ray.withT(Number.MAX_VALUE)) : null;

            if (hit !== null && hit.getT() < nearestDistance) {
                nearestDistance = hit.getT();
                nearestGroup = ScaleGizmo.groupOfElement(index);
            }
            index++;
        }

        for (const group of ScaleGizmo.BAND_GROUPS) {
            const quad: Vector3Dd[] = this.buildBandQuad(group);
            // The quad, as the two triangles of its strip
            const t: number | null = ScaleGizmo.nearestOfTwoTriangles(ray, quad[0]!, quad[1]!, quad[2]!, quad[1]!, quad[3]!, quad[2]!);

            if (t !== null && t < nearestDistance) {
                nearestDistance = t;
                nearestGroup = group;
            }
        }

        const triangles: Vector3Dd[] = this.buildUniformTriangles();

        for (let i = 0; i + 2 < triangles.length; i += 3) {
            const t: number | null = ScaleGizmo.rayTriangleT(ray.getOrigin(), ray.getDirection(), triangles[i]!, triangles[i + 1]!, triangles[i + 2]!);

            if (t !== null && t < nearestDistance) {
                nearestDistance = t;
                nearestGroup = ScaleGizmo.UNIFORM_GROUP;
            }
        }

        return nearestGroup;
    }

    /**
    @param element one of the `*_ELEMENT` constants
    @return the group the element belongs to
    */
    private static groupOfElement(element: number): number {
        switch (element) {
            case ScaleGizmo.X_SHAFT_ELEMENT:
            case ScaleGizmo.X_TIP_ELEMENT:
                return ScaleGizmo.X_AXIS_GROUP;
            case ScaleGizmo.Y_SHAFT_ELEMENT:
            case ScaleGizmo.Y_TIP_ELEMENT:
                return ScaleGizmo.Y_AXIS_GROUP;
            case ScaleGizmo.Z_SHAFT_ELEMENT:
            case ScaleGizmo.Z_TIP_ELEMENT:
                return ScaleGizmo.Z_AXIS_GROUP;
            default:
                return ScaleGizmo.NULL_GROUP;
        }
    }

    //= Geometry ==============================================================

    public getElements(): SimpleBody[] {
        return this.elements;
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return the direction, in world space, of the axis
    */
    public getAxisDirection(axis: number): Vector3Dd {
        ScaleGizmo.checkAxis(axis);

        const rotation: Matrix4x4d = new Matrix4x4d(this.requireTransformationMatrix()).withoutTranslation();

        return rotation.multiply(ScaleGizmo.unitAxis(axis)).normalized();
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return the position, in world space, of the tip (the center of its cube)
    of the axis
    PRE: the transformation matrix of the gizmo has been set.
    */
    public getTipPosition(axis: number): Vector3Dd {
        const tipSize: number = this.currentScale * ScaleGizmo.TIP_SIZE;

        return this.axisPoint(axis, 1.0).subtract(this.getAxisDirection(axis).multiply(tipSize / 2));
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @param reach fraction of the length of the axis
    @return the point of the axis at that fraction of its length, in world
    space
    PRE: the transformation matrix of the gizmo has been set.
    */
    public axisPoint(axis: number, reach: number): Vector3Dd {
        ScaleGizmo.checkAxis(axis);

        return this.getPosition().add(this.getAxisDirection(axis).multiply(reach * this.currentScale * ScaleGizmo.AXIS_LENGTH));
    }

    /**
    @param group `XY_GROUP`, `YZ_GROUP` or `XZ_GROUP`
    @return the two axes (0, 1 or 2) whose plane contains the handles of the
    group
    */
    public static bandAxes(group: number): number[] {
        switch (group) {
            case ScaleGizmo.XY_GROUP:
                return [0, 1];
            case ScaleGizmo.YZ_GROUP:
                return [1, 2];
            case ScaleGizmo.XZ_GROUP:
                return [0, 2];
            default:
                throw new IllegalArgumentException(
                    "Invalid two-axis group: " + group + ". It must be XY_GROUP, YZ_GROUP or XZ_GROUP.",
                );
        }
    }

    /**
    Gives the trapezoidal band of a two-axis handle: a quad contained in the
    plane of the two axes of the group, whose parallel sides join them at
    `BAND_INNER_REACH` and `BAND_OUTER_REACH` of their length.
    PRE: the transformation matrix of the gizmo has been set.

    @param group `XY_GROUP`, `YZ_GROUP` or `XZ_GROUP`
    @return the 4 vertices of the quad, in world space, in the order of a
    triangle strip (inner and outer point of the first axis, then the ones of
    the second one)
    */
    public buildBandQuad(group: number): Vector3Dd[] {
        const axes: number[] = ScaleGizmo.bandAxes(group);

        return [
            this.axisPoint(axes[0]!, ScaleGizmo.BAND_INNER_REACH),
            this.axisPoint(axes[0]!, ScaleGizmo.BAND_OUTER_REACH),
            this.axisPoint(axes[1]!, ScaleGizmo.BAND_INNER_REACH),
            this.axisPoint(axes[1]!, ScaleGizmo.BAND_OUTER_REACH),
        ];
    }

    /**
    Gives the three triangles of the uniform (all-axis) handle: one in each of
    the XY, YZ and XZ planes, all of them with a vertex at the origin of the
    frame of the gizmo and the other two on the axes of their plane, at
    `BAND_INNER_REACH` of their length, so each triangle is flush with the
    inner side of the band of its plane.
    PRE: the transformation matrix of the gizmo has been set.

    @return the 9 vertices (3 consecutive per triangle), in world space
    */
    public buildUniformTriangles(): Vector3Dd[] {
        const origin: Vector3Dd = this.getPosition();
        const vertices: Vector3Dd[] = [];

        for (const group of ScaleGizmo.BAND_GROUPS) {
            const axes: number[] = ScaleGizmo.bandAxes(group);

            vertices.push(origin);
            vertices.push(this.axisPoint(axes[0]!, ScaleGizmo.BAND_INNER_REACH));
            vertices.push(this.axisPoint(axes[1]!, ScaleGizmo.BAND_INNER_REACH));
        }
        return vertices;
    }

    /**
    Recalculates the geometry of the elements of the gizmo (the cylinder and
    the cube of each axis) from its current transformation, apparent size and
    selection. The two-axis bands and the uniform handle are flat polygons,
    built on demand (see `buildBandQuad`, `buildUniformTriangles`).
    PRE: the transformation matrix of the gizmo has been set.
    */
    private calculateGeometryState(): void {
        if (this.transformationMatrix === null) {
            return;
        }

        const R: Matrix4x4d = new Matrix4x4d(this.transformationMatrix).withoutTranslation();
        const origin: Vector3Dd = this.getPosition();
        const totalLength: number = this.currentScale * ScaleGizmo.AXIS_LENGTH;
        const tipSize: number = this.currentScale * ScaleGizmo.TIP_SIZE;
        const shaftLength: number = Math.max(0.0, totalLength - tipSize);

        const shaftRadius: number = this.getLineWidthInWorldUnits() / 2;
        this.shaftModel.setBottomRadius(shaftRadius);
        this.shaftModel.setTopRadius(shaftRadius);
        this.shaftModel.setHeight(shaftLength);
        this.tipModel.setSize(tipSize, tipSize, tipSize);

        for (let axis = 0; axis < ScaleGizmo.AXIS_COUNT; axis++) {
            const material: SimpleMaterial = ScaleGizmo.createMaterial(this.getAxisDisplayColor(axis));
            const eleR: Matrix4x4d = R.multiply(ScaleGizmo.axisTilt(axis));
            const eleRi: Matrix4x4d = new Matrix4x4d(eleR).invert();

            const shaft: SimpleBody = this.elements[2 * axis]!;

            shaft.setGeometry(this.shaftModel);
            shaft.setMaterial(material);
            shaft.setRotation(eleR);
            shaft.setRotationInverse(eleRi);
            shaft.setPosition(origin);

            const tip: SimpleBody = this.elements[2 * axis + 1]!;

            tip.setGeometry(this.tipModel);
            tip.setMaterial(material);
            tip.setRotation(eleR);
            tip.setRotationInverse(eleRi);
            tip.setPosition(this.getTipPosition(axis));
        }
    }

    private static createMaterial(c: ColorRgb): SimpleMaterial {
        let m = new SimpleMaterial();

        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(c);
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    /**
    @param axis 0, 1 or 2
    @return the local rotation that takes the Z axis (the axis `shaftModel`
    and `tipModel` grow along) to the X, Y or Z axis of the frame
    */
    private static axisTilt(axis: number): Matrix4x4d {
        switch (axis) {
            case 0:
                return new Matrix4x4d().axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
            case 1:
                return new Matrix4x4d().axisRotation(JavaMath.toRadians(90.0), -1, 0, 0);
            default:
                return new Matrix4x4d();
        }
    }

    /**
    @return the nearest of the two hits of the ray with two triangles, or null
    if it hits neither
    */
    private static nearestOfTwoTriangles(
        ray: Ray,
        a1: Vector3Dd,
        b1: Vector3Dd,
        c1: Vector3Dd,
        a2: Vector3Dd,
        b2: Vector3Dd,
        c2: Vector3Dd,
    ): number | null {
        const t1: number | null = ScaleGizmo.rayTriangleT(ray.getOrigin(), ray.getDirection(), a1, b1, c1);
        const t2: number | null = ScaleGizmo.rayTriangleT(ray.getOrigin(), ray.getDirection(), a2, b2, c2);

        if (t1 === null) {
            return t2;
        }
        if (t2 === null) {
            return t1;
        }
        return Math.min(t1, t2);
    }

    /**
    Ray-triangle intersection (Moller-Trumbore), used to pick the flat
    handles, which are not backed by a `SimpleBody`.
    @return the distance along `dir` to the hit, or null if there is none
    */
    private static rayTriangleT(origin: Vector3Dd, dir: Vector3Dd, a: Vector3Dd, b: Vector3Dd, c: Vector3Dd): number | null {
        const edge1: Vector3Dd = b.subtract(a);
        const edge2: Vector3Dd = c.subtract(a);
        const h: Vector3Dd = dir.crossProduct(edge2);
        const det: number = edge1.dotProduct(h);

        if (Math.abs(det) < VSDK.EPSILON) {
            return null;
        }

        const invDet: number = 1.0 / det;
        const s: Vector3Dd = origin.subtract(a);
        const u: number = invDet * s.dotProduct(h);

        if (u < 0.0 || u > 1.0) {
            return null;
        }

        const q: Vector3Dd = s.crossProduct(edge1);
        const v: number = invDet * dir.dotProduct(q);

        if (v < 0.0 || u + v > 1.0) {
            return null;
        }

        const t: number = invDet * edge2.dotProduct(q);

        return t > VSDK.EPSILON ? t : null;
    }

    private static unitAxis(axis: number): Vector3Dd {
        return new Vector3Dd(axis === 0 ? 1 : 0, axis === 1 ? 1 : 0, axis === 2 ? 1 : 0);
    }

    private static checkAxis(axis: number): void {
        if (axis < 0 || axis >= ScaleGizmo.AXIS_COUNT) {
            throw new IllegalArgumentException("Invalid axis: " + axis + ". It must be 0, 1 or 2 for the X, Y or Z axis.");
        }
    }

    //= Numeric input =========================================================

    /**
    @return the input gizmo that shows (and lets the user type) the scale
    factors of this gizmo, updated with its current values
    */
    public getInputGizmo(): InputGizmo {
        this.inputGizmo.setValue(0, this.scale.x());
        this.inputGizmo.setValue(1, this.scale.y());
        this.inputGizmo.setValue(2, this.scale.z());
        for (let axis = 0; axis < ScaleGizmo.AXIS_COUNT; axis++) {
            this.inputGizmo.setFieldHighlighted(axis, this.isAxisHighlighted(axis));
        }
        return this.inputGizmo;
    }

    //= Events ================================================================

    public processMouseEvent(_mouseEvent: MouseEvent): boolean {
        return false;
    }

    /**
    Lets the numeric input gizmo use the key first (digits, `-`, decimal
    point, TAB, BACKSPACE, arrows, and ENTER / ESC while editing); if it does
    not consume the key, `x`/`X`, `y`/`Y` and `z`/`Z` shrink/grow a single
    axis, and the arrow keys shrink/grow every axis uniformly.
    @param keyEvent key press
    @return true if the scale factors changed
    */
    public processKeyPressedEvent(keyEvent: KeyEvent): boolean {
        if (this.inputGizmo.consumesKey(keyEvent)) {
            // Boxes not being edited must start from the current scale
            // factors, not from whatever they showed the last time they were
            // synced (see getInputGizmo)
            this.getInputGizmo();
            this.inputGizmo.processKeyPressedEvent(keyEvent);
            if (this.inputGizmo.consumeCommit()) {
                const values: number[] = this.inputGizmo.getValuesWithEdits();

                this.inputGizmo.cancelEditing();
                this.scale = new Vector3Dd(values[0]!, values[1]!, values[2]!);
                return true;
            }
            return false;
        }

        const unicodeId: number = keyEvent.unicodeId;
        const keycode: number = keyEvent.keycode;
        const deltaMov = 1.1;
        let updateNeeded = false;
        let s: Vector3Dd = this.scale;

        if (unicodeId !== KeyEvent.KEY_NONE) {
            switch (unicodeId) {
                case KeyEvent.charCode("x"):
                    s = s.withX(s.x() / deltaMov);
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("X"):
                    s = s.withX(s.x() * deltaMov);
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("y"):
                    s = s.withY(s.y() / deltaMov);
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("Y"):
                    s = s.withY(s.y() * deltaMov);
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("z"):
                    s = s.withZ(s.z() / deltaMov);
                    updateNeeded = true;
                    break;
                case KeyEvent.charCode("Z"):
                    s = s.withZ(s.z() * deltaMov);
                    updateNeeded = true;
                    break;
                default:
                    break;
            }
        } else {
            switch (keycode) {
                case KeyEvent.KEY_UP:
                case KeyEvent.KEY_RIGHT:
                    s = new Vector3Dd(s.x() * deltaMov, s.y() * deltaMov, s.z() * deltaMov);
                    updateNeeded = true;
                    break;
                case KeyEvent.KEY_LEFT:
                case KeyEvent.KEY_DOWN:
                    s = new Vector3Dd(s.x() / deltaMov, s.y() / deltaMov, s.z() / deltaMov);
                    updateNeeded = true;
                    break;
                default:
                    break;
            }
        }

        this.scale = s;
        return updateNeeded;
    }

    public processKeyReleasedEvent(_mouseEvent: KeyEvent): boolean {
        return false;
    }

    public processMousePressedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public processMouseReleasedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public processMouseClickedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public processMouseMovedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public processMouseDraggedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public processMouseWheelEvent(_e: MouseEvent): boolean {
        return false;
    }
}
