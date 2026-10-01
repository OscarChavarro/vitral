import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { Ray } from "../../environment/geometry/element/Ray.js";
import { Torus } from "../../environment/geometry/volume/Torus.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { ViewportElementScaler } from "../viewport/ViewportElementScaler.js";
import { Gizmo } from "./Gizmo.js";
import { InputGizmo } from "./InputGizmo.js";
import { InputGizmoValueChangeRules } from "./InputGizmoValueChangeRules.js";
import { ReferenceFrameGizmo } from "./ReferenceFrameGizmo.js";

/**
Gizmo to specify the orientation of an object, made of three rings, one around
each axis of the frame of the gizmo (in the style of the rotation gizmo of
3D Studio Max, and following the structure of `TranslateGizmo`), plus a fourth
ring around the front vector of the camera that views the gizmo.

- Geometric model: each ring has an internal model based on a `Torus` (a
  `SimpleBody` of the ring), used to interact with the mouse: the tube of the
  torus is a little thicker than the drawn ring, so it is easy to point at.
- Size: like `TranslateGizmo`, the gizmo keeps an apparent size in pixels
  from its camera, and its base size and line widths are designed for legacy
  resolutions and scaled for the one of the screen (see `applyScale`).
- Drawing: rings are drawn as strips of quads facing the camera (see
  `buildRingStrips`), so each ring can have its own width in pixels.
- Visibility: the three axis rings only show (and can only be picked on) the
  half closer to the camera, relative to a sphere that contains them (see
  `isPointOnVisibleHemisphere`), unless the ring is seen almost face on (its
  axis nearly aligned with the view direction), when it is shown whole, like
  the camera ring always is (see `buildCameraRingStrips`).
- Camera ring: a fourth ring, around the front vector of the camera that
  views the gizmo, drawn gray and slightly larger than the other three (see
  `getCameraRingRadius`). Dragging it rotates the object around the current
  view direction. It never hides a half, as it always faces the camera.
- Selection: the ring under the cursor is the volatile selection and the one
  clicked is the persistent selection (see `RotateGizmoInteractionTechnique`).
  Rings are drawn with the color of their axis (or gray for the camera ring),
  unless they are the current selection (see `getCurrentSelection`): then
  they are yellow.
- Rotation arc: while a ring is being dragged, the gizmo keeps the arc the
  rotation has swept (see `setArc`), to be drawn as a translucent sector with
  the color of its ring (never yellow), and labeled with its angle.
- Numeric input: the angles (in degrees, with two decimals) of the orientation
  of the gizmo are shown and edited with a nested `InputGizmo`. The first
  three are the angles of the rotations around the X, Y and Z axes, applied in
  that order, that give the orientation (see `extractAnglesInDegrees`). The
  fourth (gray) field is not part of that orientation: it always shows 0 and
  lets the user type a relative angle that, on ENTER, rotates the object that
  much around the current camera axis and goes back to 0.

The transformation matrix of the gizmo has the orientation and the position
of the frame the rings belong to.
*/
export class RotateGizmo extends Gizmo {
    public static readonly NULL_GROUP = 0;
    public static readonly X_RING_GROUP = 1;
    public static readonly Y_RING_GROUP = 2;
    public static readonly Z_RING_GROUP = 3;
    public static readonly CAMERA_RING_GROUP = 4;

    /// Number of colored axis rings (X, Y, Z), and of their input gizmo fields
    public static readonly RING_COUNT = 3;
    /// Index of the camera ring where a ring index is expected (i.e. in
    /// `setArc`, or the `ring` argument of `RotateGizmoInteractionTechnique`)
    public static readonly CAMERA_RING_INDEX = RotateGizmo.RING_COUNT;
    /// Number of input gizmo fields, the axis ones plus the camera one
    public static readonly INPUT_FIELD_COUNT = RotateGizmo.RING_COUNT + 1;
    /// Index of the (always 0, relative) field of the camera ring
    public static readonly CAMERA_INPUT_FIELD_INDEX = RotateGizmo.RING_COUNT;

    /// Size and line width designed for legacy resolutions; the width
    /// matches TranslateGizmo#DEFAULT_LINE_WIDTH so both gizmos look
    /// visually consistent (same ribbon thickness) in the 3D UI
    public static readonly DEFAULT_APPARENT_SIZE_IN_PIXELS = 100;
    public static readonly DEFAULT_LINE_WIDTH = 1.0;

    /// Decimals of the angles of the input gizmo, and maximum digits of the
    /// integer part (-180 to 180)
    public static readonly ANGLE_DECIMALS = 2;
    public static readonly ANGLE_INTEGER_DIGITS = 3;

    /// Number of straight pieces of the strip of a ring
    public static readonly RING_SEGMENTS = 96;

    /// Maximum angle of each triangle of the sector that shows the rotation arc
    public static readonly ARC_STEP_IN_DEGREES = 3.0;
    /// Distance of the label of the rotation arc from the center of the gizmo,
    /// relative to the radius of the rings
    private static readonly ARC_LABEL_DISTANCE = 1.2;

    /// Radius of the rings, and tolerance to point at them with the cursor,
    /// relative to the apparent size of the gizmo (1.0 is the size in pixels)
    private static readonly RING_RADIUS = 0.8;
    private static readonly PICK_TOLERANCE = 0.06;
    private static readonly MAX_TUBE_TO_RING_RADIUS_RATIO = 0.5;

    /// The camera ring is a little bigger than the other three, so it does
    /// not overlap them (see the reference image of 3D Studio Max)
    private static readonly CAMERA_RING_RADIUS_FACTOR = 1.15;
    private static readonly CAMERA_RING_COLOR = new ColorRgb(0.6, 0.6, 0.6);

    /// An axis ring is only clipped to its near half when it is seen at some
    /// angle: when its axis is almost aligned with the view direction (it is
    /// seen face on, like the camera ring), it is drawn and picked whole,
    /// because every one of its points is then equally close to the camera
    private static readonly AXIS_VIEW_ALIGNMENT_THRESHOLD = 0.98;

    /// Rotation of the local frame of each torus (whose axis is Z) so its
    /// axis is the axis of its ring
    private static readonly RING_TILT: readonly (readonly number[])[] = [
        [JavaMath.toRadians(90.0), 0, 1, 0],
        [JavaMath.toRadians(90.0), -1, 0, 0],
        [0, 0, 0, 1],
    ];

    private transformationMatrix: Matrix4x4d;
    private camera!: Camera;

    /// Geometric model based in primitive instancing: one torus for each ring
    private readonly ringModels: Torus[];
    private readonly ringInstances: SimpleBody[];
    private readonly axisColors: ColorRgb[];
    private readonly cameraRingModel: Torus;
    private readonly cameraRingInstance: SimpleBody;

    /// Apparent size (in legacy resolution pixels) the user has chosen
    private baseApparentSizeInPixels: number;
    /// Apparent size in pixels of the screen currently in use
    private apparentSizeInPixels: number;
    private readonly baseRingLineWidths: number[];
    private readonly ringLineWidths: number[];
    private baseCameraRingLineWidth: number;
    private cameraRingLineWidth: number;
    private currentScale: number;

    /// Rotation arc: ring dragged (-1 if none, `CAMERA_RING_INDEX` for the
    /// camera ring), the axes of its plane (fixed while dragging) and the
    /// angles, in radians, where the arc starts and how much it goes on
    /// (negative for the other way)
    private arcRing: number;
    private arcU: Vector3Dd = new Vector3Dd();
    private arcV: Vector3Dd = new Vector3Dd();
    private arcStartAngle = 0;
    private arcSweep = 0;

    /// Interaction state
    private readonly inputGizmo: InputGizmo;
    private persistentSelection: number;
    private volatileSelection: number;

    /**
    @param cam camera the gizmo is seen from
    */
    public constructor(cam: Camera) {
        super();
        this.transformationMatrix = new Matrix4x4d();
        this.baseApparentSizeInPixels = RotateGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS;
        this.apparentSizeInPixels = RotateGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS;
        this.currentScale = 1.0;
        this.persistentSelection = RotateGizmo.NULL_GROUP;
        this.volatileSelection = RotateGizmo.NULL_GROUP;
        this.arcRing = -1;

        const axisColorSource = new ReferenceFrameGizmo();

        this.axisColors = [];
        this.ringModels = [];
        this.ringInstances = [];
        this.baseRingLineWidths = [];
        this.ringLineWidths = [];
        this.inputGizmo = new InputGizmo(
            RotateGizmo.INPUT_FIELD_COUNT,
            RotateGizmo.ANGLE_DECIMALS,
            RotateGizmo.ANGLE_INTEGER_DIGITS,
            InputGizmoValueChangeRules.forRotation(),
        );

        for (let ring = 0; ring < RotateGizmo.RING_COUNT; ring++) {
            this.axisColors[ring] = axisColorSource.getAxisColor(ring);
            this.inputGizmo.setFieldColor(ring, this.axisColors[ring]!);
            this.ringModels[ring] = new Torus(RotateGizmo.RING_RADIUS, RotateGizmo.PICK_TOLERANCE);
            this.ringInstances[ring] = new SimpleBody();
            this.ringInstances[ring]!.setGeometry(this.ringModels[ring]!);
            this.baseRingLineWidths[ring] = RotateGizmo.DEFAULT_LINE_WIDTH;
            this.ringLineWidths[ring] = RotateGizmo.DEFAULT_LINE_WIDTH;
        }
        this.inputGizmo.setFieldColor(RotateGizmo.CAMERA_INPUT_FIELD_INDEX, RotateGizmo.CAMERA_RING_COLOR);
        this.baseCameraRingLineWidth = RotateGizmo.DEFAULT_LINE_WIDTH;
        this.cameraRingLineWidth = RotateGizmo.DEFAULT_LINE_WIDTH;
        this.cameraRingModel = new Torus(
            RotateGizmo.RING_RADIUS * RotateGizmo.CAMERA_RING_RADIUS_FACTOR,
            RotateGizmo.PICK_TOLERANCE,
        );
        this.cameraRingInstance = new SimpleBody();
        this.cameraRingInstance.setGeometry(this.cameraRingModel);
        this.setCamera(cam);
    }

    //= Camera, size and line widths ======================================

    public setCamera(cam: Camera): void {
        this.camera = cam;
    }

    public getCamera(): Camera {
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
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the width, in pixels, of the strip that draws the ring
    */
    public getRingLineWidth(ring: number): number {
        RotateGizmo.checkRing(ring);
        return this.ringLineWidths[ring]!;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @param lineWidth width, in pixels, of the strip that draws the ring; not
    positive values are ignored
    */
    public setRingLineWidth(ring: number, lineWidth: number): void {
        RotateGizmo.checkRing(ring);
        if (lineWidth > 0.0) {
            this.ringLineWidths[ring] = lineWidth;
        }
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the width the ring is designed to have in legacy resolutions, in
    pixels, before scaling it for the resolution of the screen
    */
    public getBaseRingLineWidth(ring: number): number {
        RotateGizmo.checkRing(ring);
        return this.baseRingLineWidths[ring]!;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @param lineWidth width of the ring in legacy resolutions, in pixels; not
    positive values are ignored. It is used the next time `applyScale` is called
    */
    public setBaseRingLineWidth(ring: number, lineWidth: number): void {
        RotateGizmo.checkRing(ring);
        if (lineWidth > 0.0) {
            this.baseRingLineWidths[ring] = lineWidth;
        }
    }

    /**
    @return the width, in pixels, of the strip that draws the camera ring
    */
    public getCameraRingLineWidth(): number {
        return this.cameraRingLineWidth;
    }

    /**
    @param lineWidth width, in pixels, of the strip that draws the camera
    ring; not positive values are ignored
    */
    public setCameraRingLineWidth(lineWidth: number): void {
        if (lineWidth > 0.0) {
            this.cameraRingLineWidth = lineWidth;
        }
    }

    /**
    @return the width the camera ring is designed to have in legacy
    resolutions, in pixels, before scaling it for the resolution of the screen
    */
    public getBaseCameraRingLineWidth(): number {
        return this.baseCameraRingLineWidth;
    }

    /**
    @param lineWidth width of the camera ring in legacy resolutions, in
    pixels; not positive values are ignored. It is used the next time
    `applyScale` is called
    */
    public setBaseCameraRingLineWidth(lineWidth: number): void {
        if (lineWidth > 0.0) {
            this.baseCameraRingLineWidth = lineWidth;
        }
    }

    /**
    Sets the apparent size and the line widths of the gizmo to the values that
    make it look proportional to the screen resolution: the base values
    (designed for legacy resolutions) multiplied by the scale of the given
    scaler. The new size is used by the gizmo the next time its
    transformation is set.

    @param scaler scaler informed of the resolution of the screen
    */
    public applyScale(scaler: ViewportElementScaler | null): void {
        if (scaler === null) {
            return;
        }
        this.setApparentSizeInPixels(scaler.scaleSize(this.baseApparentSizeInPixels));
        for (let ring = 0; ring < RotateGizmo.RING_COUNT; ring++) {
            this.ringLineWidths[ring] = scaler.scaleLength(this.baseRingLineWidths[ring]!);
        }
        this.cameraRingLineWidth = scaler.scaleLength(this.baseCameraRingLineWidth);
    }

    /**
    @return the factor applied to the gizmo geometry so it keeps its apparent
    size in pixels, as seen from its camera
    */
    public getCurrentScale(): number {
        return this.currentScale;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the width of the strip of the ring, converted from pixels to world
    units, as seen from its camera at its current apparent size
    */
    public getRingLineWidthInWorldUnits(ring: number): number {
        RotateGizmo.checkRing(ring);
        return (this.ringLineWidths[ring]! * this.currentScale) / this.apparentSizeInPixels;
    }

    /**
    @return the width of the strip of the camera ring, converted from pixels
    to world units, as seen from its camera at its current apparent size
    */
    public getCameraRingLineWidthInWorldUnits(): number {
        return (this.cameraRingLineWidth * this.currentScale) / this.apparentSizeInPixels;
    }

    /**
    @return the radius of the rings, in world units, as seen from its camera
    at its current apparent size
    */
    public getRingRadius(): number {
        return RotateGizmo.RING_RADIUS * this.currentScale;
    }

    /**
    @return the radius of the camera ring, in world units, as seen from its
    camera at its current apparent size
    */
    public getCameraRingRadius(): number {
        return RotateGizmo.RING_RADIUS * RotateGizmo.CAMERA_RING_RADIUS_FACTOR * this.currentScale;
    }

    //= Transformation ====================================================

    public getPosition(): Vector3Dd {
        return this.transformationMatrix.extractTranslation();
    }

    public setPosition(p: Vector3Dd): void {
        this.setTransformationMatrix(this.transformationMatrix.withTranslation(p));
    }

    /**
    Sets the frame of the gizmo and recalculates the geometry of its rings.
    @param transformationMatrix orientation and position of the frame the rings belong to
    */
    public setTransformationMatrix(transformationMatrix: Matrix4x4d): void {
        this.transformationMatrix = transformationMatrix;
        this.updateGeometryState();
    }

    public getTransformationMatrix(): Matrix4x4d {
        return this.transformationMatrix;
    }

    /**
    Recalculates the geometry of the rings of the gizmo from its current
    transformation, apparent size and camera.
    */
    public updateGeometryState(): void {
        this.updateScale();

        const ringRadius: number = this.getRingRadius();
        const rotation: Matrix4x4d = new Matrix4x4d(this.transformationMatrix).withoutTranslation();
        const position: Vector3Dd = this.getPosition();

        for (let ring = 0; ring < RotateGizmo.RING_COUNT; ring++) {
            // The tube covers the width of the drawn ring, at least
            const tubeRadius: number = Math.max(
                RotateGizmo.PICK_TOLERANCE * this.currentScale,
                this.getRingLineWidthInWorldUnits(ring) / 2,
            );

            this.ringModels[ring]!.setMajorRadius(ringRadius);
            this.ringModels[ring]!.setMinorRadius(
                Math.min(tubeRadius, ringRadius * RotateGizmo.MAX_TUBE_TO_RING_RADIUS_RATIO),
            );

            const tilt: readonly number[] = RotateGizmo.RING_TILT[ring]!;
            const ringRotation: Matrix4x4d = rotation.multiply(
                new Matrix4x4d().axisRotation(tilt[0]!, tilt[1]!, tilt[2]!, tilt[3]!),
            );

            this.ringInstances[ring]!.setRotation(ringRotation);
            this.ringInstances[ring]!.setPosition(position);
        }

        // The camera ring: its axis is the front vector of the camera, not
        // one of the axes of the frame of the gizmo
        const cameraRingRadius: number = this.getCameraRingRadius();
        const cameraTubeRadius: number = Math.max(
            RotateGizmo.PICK_TOLERANCE * this.currentScale,
            this.getCameraRingLineWidthInWorldUnits() / 2,
        );
        const right: Vector3Dd = this.getCameraPlaneRightDirection();
        const up: Vector3Dd = this.getCameraPlaneUpDirection();
        const front: Vector3Dd = this.getCameraAxisDirection();

        this.cameraRingModel.setMajorRadius(cameraRingRadius);
        this.cameraRingModel.setMinorRadius(
            Math.min(cameraTubeRadius, cameraRingRadius * RotateGizmo.MAX_TUBE_TO_RING_RADIUS_RATIO),
        );
        this.cameraRingInstance.setRotation(RotateGizmo.rotationFromBasis(right, up, front));
        this.cameraRingInstance.setPosition(position);
    }

    /**
    Calculates the scale that makes the gizmo look as big as its apparent
    size, from its camera. It is kept as it was if the size in pixels can not
    be measured.
    */
    private updateScale(): void {
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

    //= Geometry of the rings =============================================

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the internal model of the ring, whose axis is the local Z axis of
    its instance (see `getRingInstance`)
    */
    public getRingModel(ring: number): Torus {
        RotateGizmo.checkRing(ring);
        return this.ringModels[ring]!;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the instance of the model of the ring, with its position and
    orientation in world space; it is used to point at the ring with the mouse
    */
    public getRingInstance(ring: number): SimpleBody {
        RotateGizmo.checkRing(ring);
        return this.ringInstances[ring]!;
    }

    /**
    @return the internal model of the camera ring, whose axis is the local Z
    axis of its instance (see `getCameraRingInstance`)
    */
    public getCameraRingModel(): Torus {
        return this.cameraRingModel;
    }

    /**
    @return the instance of the model of the camera ring, with its position
    and orientation in world space; it is used to point at it with the mouse
    */
    public getCameraRingInstance(): SimpleBody {
        return this.cameraRingInstance;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the direction, in world space, of the axis of the ring
    */
    public getAxisDirection(ring: number): Vector3Dd {
        RotateGizmo.checkRing(ring);

        const rotation: Matrix4x4d = new Matrix4x4d(this.transformationMatrix).withoutTranslation();

        return rotation.multiply(RotateGizmo.unitAxis(ring)).normalized();
    }

    /**
    @return the direction, in world space, of the axis of the camera ring:
    the front vector of the camera that views the gizmo
    PRE: `camera.updateVectors()` has been called (i.e. via `updateGeometryState`)
    */
    public getCameraAxisDirection(): Vector3Dd {
        return this.camera.getFront().normalized();
    }

    /**
    @return the direction, in world space, that goes across the screen to the
    right, as seen from the camera of the gizmo; one of the two axes of the
    plane of the camera ring. Built (with `getCameraPlaneUpDirection`) as a
    proper (determinant +1) orthonormal basis with the camera axis, instead of
    taken directly from the camera, because `SimpleBody.setRotation` only
    keeps the rotation part of a matrix (it round-trips it through a
    quaternion): a basis with determinant -1 (i.e. a reflection, which is what
    the camera's own left/up/front can be, depending on its convention) would
    silently become a different, wrong rotation
    PRE: `camera.updateVectors()` has been called
    */
    public getCameraPlaneRightDirection(): Vector3Dd {
        const front: Vector3Dd = this.getCameraAxisDirection();
        const approximateUp: Vector3Dd = this.camera.getUp().normalized();

        return approximateUp.crossProduct(front).normalized();
    }

    /**
    @return the direction, in world space, that goes up the screen, as seen
    from the camera of the gizmo; the other axis of the plane of the camera
    ring (see `getCameraPlaneRightDirection`)
    PRE: `camera.updateVectors()` has been called
    */
    public getCameraPlaneUpDirection(): Vector3Dd {
        return this.getCameraAxisDirection().crossProduct(this.getCameraPlaneRightDirection()).normalized();
    }

    /**
    @param point a point in world space, i.e. of a ring of this gizmo
    @return true if the point is on the half of the sphere (centered at the
    position of the gizmo) that faces the camera, so an axis ring is only
    drawn and picked where this is true
    */
    public isPointOnVisibleHemisphere(point: Vector3Dd): boolean {
        return this.visibilityMetric(point, this.getPosition()) < VSDK.EPSILON;
    }

    /**
    @return true if the ring around the given axis is seen at some angle, so
    its far half must be clipped; false if it is seen almost face on (its
    axis almost aligned with the view direction), when it is drawn whole
    */
    private needsHemisphereClipping(axis: Vector3Dd, center: Vector3Dd): boolean {
        const viewDirection: Vector3Dd =
            this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL
                ? this.camera.getFront()
                : center.subtract(this.camera.getPosition()).normalized();

        return Math.abs(axis.normalized().dotProduct(viewDirection)) < RotateGizmo.AXIS_VIEW_ALIGNMENT_THRESHOLD;
    }

    /**
    @param point a point in world space
    @param center center of the sphere the point is assumed to belong to
    @return a negative value if the point is on the half of the sphere that
    faces the camera (see `isPointOnVisibleHemisphere`), zero if it is exactly
    at its silhouette
    */
    private visibilityMetric(point: Vector3Dd, center: Vector3Dd): number {
        let normal: Vector3Dd = point.subtract(center);
        const length: number = normal.length();

        if (length < VSDK.EPSILON) {
            return -1.0;
        }
        normal = normal.multiply(1.0 / length);

        const viewDirection: Vector3Dd =
            this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL
                ? this.camera.getFront()
                : point.subtract(this.camera.getPosition()).normalized();

        return normal.dotProduct(viewDirection);
    }

    /**
    Builds the geometry to draw a ring as one or more strips of quads facing
    the camera of the gizmo, following `buildRingArcs`. Only the half closer
    to the camera is returned, unless the ring is seen almost face on, when it
    is returned whole (see `needsHemisphereClipping`).
    PRE: the transformation matrix of the gizmo has been set.

    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the vertices of one or more triangle strips (an alternating pair,
    one at each side of the ring, for each position along it)
    */
    public buildRingStrips(ring: number): Vector3Dd[][] {
        RotateGizmo.checkRing(ring);

        const center: Vector3Dd = this.getPosition();
        const axis: Vector3Dd = this.getAxisDirection(ring);
        const rotation: Matrix4x4d = new Matrix4x4d(this.transformationMatrix).withoutTranslation();
        // The plane of the ring is spanned by the next two axes (X: YZ,
        // Y: ZX, Z: XY), so the ring goes around its axis counterclockwise
        const u: Vector3Dd = rotation.multiply(RotateGizmo.unitAxis((ring + 1) % RotateGizmo.RING_COUNT)).normalized();
        const v: Vector3Dd = rotation.multiply(RotateGizmo.unitAxis((ring + 2) % RotateGizmo.RING_COUNT)).normalized();
        const radius: number = this.getRingRadius();
        const halfWidth: number = this.getRingLineWidthInWorldUnits(ring) / 2;

        if (!this.needsHemisphereClipping(axis, center)) {
            const whole: Vector3Dd[][] = [];

            whole.push(this.buildFullRingStrip(center, axis, u, v, radius, halfWidth));
            return whole;
        }
        return this.buildVisibleRingArcs(center, axis, u, v, radius, halfWidth);
    }

    /**
    Builds the geometry to draw the camera ring as a strip of quads facing the
    camera: a closed triangle strip in world space, that follows the ring with
    its width in pixels (see `getCameraRingLineWidth`) at every vertex. Unlike
    `buildRingStrips`, it is always whole, as it always faces the camera.
    PRE: the transformation matrix of the gizmo has been set.

    @return the vertices of the closed triangle strip: an alternating pair
    (one at each side of the ring) for each of the `RING_SEGMENTS + 1` positions
    */
    public buildCameraRingStrip(): Vector3Dd[] {
        const center: Vector3Dd = this.getPosition();
        const axis: Vector3Dd = this.getCameraAxisDirection();
        const u: Vector3Dd = this.getCameraPlaneRightDirection();
        const v: Vector3Dd = this.getCameraPlaneUpDirection();
        const halfWidth: number = this.getCameraRingLineWidthInWorldUnits() / 2;

        return this.buildFullRingStrip(center, axis, u, v, this.getCameraRingRadius(), halfWidth);
    }

    /**
    Builds a closed ribbon strip around a full circle (see `buildRingStrips`
    and `buildCameraRingStrip`), without any clipping.
    */
    private buildFullRingStrip(
        center: Vector3Dd,
        axis: Vector3Dd,
        u: Vector3Dd,
        v: Vector3Dd,
        radius: number,
        halfWidth: number,
    ): Vector3Dd[] {
        const strip: Vector3Dd[] = new Array<Vector3Dd>(2 * (RotateGizmo.RING_SEGMENTS + 1));

        for (let i = 0; i <= RotateGizmo.RING_SEGMENTS; i++) {
            const angle: number = (2 * Math.PI * (i % RotateGizmo.RING_SEGMENTS)) / RotateGizmo.RING_SEGMENTS;
            const cos: number = Math.cos(angle);
            const sin: number = Math.sin(angle);
            const point: Vector3Dd = center.add(u.multiply(radius * cos)).add(v.multiply(radius * sin));
            const tangent: Vector3Dd = v.multiply(cos).subtract(u.multiply(sin));
            const side: Vector3Dd = this.sideAcross(point, tangent, axis, halfWidth);

            strip[2 * i] = point.add(side);
            strip[2 * i + 1] = point.subtract(side);
        }
        return strip;
    }

    /**
    Builds one or more ribbon strips that follow only the arcs of a circle
    that are on the half of its sphere (centered at `center`) that faces the
    camera (see `isPointOnVisibleHemisphere`), with the far arcs left out.
    */
    private buildVisibleRingArcs(
        center: Vector3Dd,
        axis: Vector3Dd,
        u: Vector3Dd,
        v: Vector3Dd,
        radius: number,
        halfWidth: number,
    ): Vector3Dd[][] {
        const n: number = RotateGizmo.RING_SEGMENTS;
        const points: Vector3Dd[] = new Array<Vector3Dd>(n + 1);
        const metrics: number[] = new Array<number>(n + 1);

        for (let i = 0; i <= n; i++) {
            const angle: number = (2 * Math.PI * (i % n)) / n;
            const point: Vector3Dd = center.add(u.multiply(radius * Math.cos(angle))).add(v.multiply(radius * Math.sin(angle)));

            points[i] = point;
            metrics[i] = this.visibilityMetric(point, center);
        }

        const arcs: Vector3Dd[][] = [];
        let current: Vector3Dd[] = [];

        for (let i = 0; i < n; i++) {
            const visibleA: boolean = metrics[i]! < 0;
            const visibleB: boolean = metrics[i + 1]! < 0;

            if (visibleA && current.length === 0) {
                current.push(points[i]!);
            }
            if (visibleA !== visibleB) {
                const boundary: Vector3Dd = RotateGizmo.interpolateOnCircle(
                    center,
                    points[i]!,
                    points[i + 1]!,
                    metrics[i]!,
                    metrics[i + 1]!,
                    radius,
                );

                current.push(boundary);
                if (visibleA) {
                    arcs.push(this.toRibbonStrip(current, axis, halfWidth));
                    current = [];
                } else {
                    current = [];
                    current.push(boundary);
                }
            } else if (visibleA) {
                current.push(points[i + 1]!);
            }
        }
        if (current.length > 1) {
            arcs.push(this.toRibbonStrip(current, axis, halfWidth));
        }
        return arcs;
    }

    /**
    @return the point where the segment between two consecutive samples of a
    circle (around `center`, at `radius`) crosses the silhouette of the
    sphere, estimated by linearly interpolating between them (by how far the
    visibility metric of each is from zero) and projecting back onto the circle
    */
    private static interpolateOnCircle(
        center: Vector3Dd,
        a: Vector3Dd,
        b: Vector3Dd,
        metricA: number,
        metricB: number,
        radius: number,
    ): Vector3Dd {
        const denominator: number = metricA - metricB;
        let t: number = Math.abs(denominator) < VSDK.EPSILON ? 0.5 : metricA / denominator;

        t = Math.max(0.0, Math.min(1.0, t));

        const blend: Vector3Dd = a.add(b.subtract(a).multiply(t));
        const fromCenter: Vector3Dd = blend.subtract(center);
        const length: number = fromCenter.length();

        if (length < VSDK.EPSILON) {
            return blend;
        }
        return center.add(fromCenter.multiply(radius / length));
    }

    /**
    Builds a ribbon strip (an alternating pair of vertices, one at each side)
    that follows a polyline of points already known to be on the ring, with
    the tangent at each one estimated from its neighbors.
    */
    private toRibbonStrip(polyline: Vector3Dd[], axis: Vector3Dd, halfWidth: number): Vector3Dd[] {
        const count: number = polyline.length;
        const strip: Vector3Dd[] = new Array<Vector3Dd>(2 * count);

        for (let i = 0; i < count; i++) {
            const point: Vector3Dd = polyline[i]!;
            const previous: Vector3Dd = polyline[Math.max(0, i - 1)]!;
            const next: Vector3Dd = polyline[Math.min(count - 1, i + 1)]!;
            const tangent: Vector3Dd = next.subtract(previous);
            const side: Vector3Dd = this.sideAcross(point, tangent, axis, halfWidth);

            strip[2 * i] = point.add(side);
            strip[2 * i + 1] = point.subtract(side);
        }
        return strip;
    }

    /**
    @return the offset, of length `halfWidth`, across the ring at `point`
    (perpendicular to `tangent` and facing the camera), used at both sides of
    a ribbon strip
    */
    private sideAcross(point: Vector3Dd, tangent: Vector3Dd, axis: Vector3Dd, halfWidth: number): Vector3Dd {
        let view: Vector3Dd;

        if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            view = this.camera.getFront();
        } else {
            view = point.subtract(this.camera.getPosition());
        }

        let side: Vector3Dd = tangent.crossProduct(view);

        if (side.length() < VSDK.EPSILON) {
            // Ring pointing to the viewer, or degenerate tangent: any
            // direction across the ring is valid
            side = axis;
        }
        return side.normalized().multiply(halfWidth);
    }

    private static unitAxis(axis: number): Vector3Dd {
        return new Vector3Dd(axis === 0 ? 1 : 0, axis === 1 ? 1 : 0, axis === 2 ? 1 : 0);
    }

    /**
    @return the pure rotation matrix whose local X, Y and Z axes are the given
    (orthonormal) world directions
    */
    private static rotationFromBasis(xAxis: Vector3Dd, yAxis: Vector3Dd, zAxis: Vector3Dd): Matrix4x4d {
        const values: number[][] = [
            [xAxis.x(), yAxis.x(), zAxis.x(), 0],
            [xAxis.y(), yAxis.y(), zAxis.y(), 0],
            [xAxis.z(), yAxis.z(), zAxis.z(), 0],
            [0, 0, 0, 1],
        ];

        return new Matrix4x4d(values);
    }

    //= Selection =========================================================

    /**
    Finds the ring (or the camera ring) a ray points at, using the torus
    models of the rings, and ignoring hits on the far half of an axis ring
    that needs clipping (see `needsHemisphereClipping`).
    PRE: the transformation matrix of the gizmo has been set.

    @param ray ray, in world space, i.e. the one the cursor defines
    @return the `*_RING_GROUP` constant of the nearest ring hit by the ray, or
    `NULL_GROUP` if it hits none
    */
    public pickRing(ray: Ray): number {
        let nearestDistance: number = Number.MAX_VALUE;
        let nearestRing: number = RotateGizmo.NULL_GROUP;
        const center: Vector3Dd = this.getPosition();

        for (let ring = 0; ring < RotateGizmo.RING_COUNT; ring++) {
            const hit: Ray | null = this.ringInstances[ring]!.doIntersectionFirstHit(ray.withT(Number.MAX_VALUE));

            if (hit === null || hit.getT() >= nearestDistance) {
                continue;
            }

            const hitPoint: Vector3Dd = ray.getOrigin().add(ray.getDirection().multiply(hit.getT()));

            if (this.needsHemisphereClipping(this.getAxisDirection(ring), center) && !this.isPointOnVisibleHemisphere(hitPoint)) {
                continue;
            }
            nearestDistance = hit.getT();
            nearestRing = RotateGizmo.groupOfRing(ring);
        }

        const cameraHit: Ray | null = this.cameraRingInstance.doIntersectionFirstHit(ray.withT(Number.MAX_VALUE));

        if (cameraHit !== null && cameraHit.getT() < nearestDistance) {
            nearestRing = RotateGizmo.CAMERA_RING_GROUP;
        }
        return nearestRing;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the `*_RING_GROUP` constant of the ring
    */
    public static groupOfRing(ring: number): number {
        RotateGizmo.checkRing(ring);
        return ring + 1;
    }

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
    @return the group (one of the `*_GROUP` constants) that is highlighted: the
    one under the cursor, or the chosen one if the cursor is not over any group
    */
    public getCurrentSelection(): number {
        if (this.volatileSelection === RotateGizmo.NULL_GROUP) {
            return this.persistentSelection;
        }
        return this.volatileSelection;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return true if the ring is the current selection, so it is drawn yellow
    */
    public isRingHighlighted(ring: number): boolean {
        RotateGizmo.checkRing(ring);
        return this.getCurrentSelection() === RotateGizmo.groupOfRing(ring);
    }

    /**
    @return true if the camera ring is the current selection, so it is drawn
    yellow instead of gray
    */
    public isCameraRingHighlighted(): boolean {
        return this.getCurrentSelection() === RotateGizmo.CAMERA_RING_GROUP;
    }

    /**
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis
    @return the color the ring must be drawn with: the one of its axis, or
    yellow if it is highlighted
    */
    public getRingColor(ring: number): ColorRgb {
        RotateGizmo.checkRing(ring);
        return this.isRingHighlighted(ring) ? InputGizmo.HIGHLIGHT_COLOR : this.axisColors[ring]!;
    }

    /**
    @return the color the camera ring must be drawn with: gray, or yellow if
    it is highlighted
    */
    public getCameraRingColor(): ColorRgb {
        return this.isCameraRingHighlighted() ? InputGizmo.HIGHLIGHT_COLOR : RotateGizmo.CAMERA_RING_COLOR;
    }

    //= Rotation arc ======================================================

    /**
    Sets the arc of the rotation being done around a ring, so it is shown.
    @param ring 0, 1 or 2 for the ring around the X, Y or Z axis, or
    `CAMERA_RING_INDEX` for the camera ring
    @param u direction, in world space, where the angles are measured from
    @param v direction, in world space, perpendicular to `u` and to the
    axis of the ring, so angles grow from `u` towards it
    @param startAngle angle, in radians, where the arc starts
    @param sweep angle, in radians, the arc goes on from its start (negative
    for the other way)
    */
    public setArc(ring: number, u: Vector3Dd, v: Vector3Dd, startAngle: number, sweep: number): void {
        RotateGizmo.checkRingOrCamera(ring);
        this.arcRing = ring;
        this.arcU = u;
        this.arcV = v;
        this.arcStartAngle = startAngle;
        this.arcSweep = sweep;
    }

    /**
    Stops showing the rotation arc.
    */
    public clearArc(): void {
        this.arcRing = -1;
    }

    /**
    @return true if there is a rotation arc to show
    */
    public isArcVisible(): boolean {
        return this.arcRing >= 0;
    }

    /**
    @return the ring the arc belongs to (0, 1 or 2, or `CAMERA_RING_INDEX`),
    or -1 if there is no arc
    */
    public getArcRing(): number {
        return this.arcRing;
    }

    /**
    @return the angle the arc goes on, in radians (negative for the other way)
    */
    public getArcSweep(): number {
        return this.arcSweep;
    }

    /**
    @return the angle the arc goes on, in degrees (negative for the other way)
    */
    public getArcSweepInDegrees(): number {
        return JavaMath.toDegrees(this.arcSweep);
    }

    /**
    @return the color to draw the arc with: the one of its axis (or gray for
    the camera ring), whether or not the ring is highlighted
    */
    public getArcColor(): ColorRgb {
        if (this.arcRing === RotateGizmo.CAMERA_RING_INDEX) {
            return RotateGizmo.CAMERA_RING_COLOR;
        }
        return this.axisColors[Math.max(0, this.arcRing)]!;
    }

    /**
    Builds the geometry to draw the arc as a translucent sector: a triangle
    fan in world space, with the center of the gizmo as its first vertex and
    then the points of the arc at the radius of its ring, from its start.
    An arc of more than a turn is shown as a whole disc.

    @return the vertices of the triangle fan, or an empty array if there is
    no arc to show, or it has no angle yet
    */
    public buildArcFan(): Vector3Dd[] {
        if (!this.isArcVisible() || Math.abs(this.arcSweep) < VSDK.EPSILON) {
            return [];
        }

        const sweep: number = Math.max(-2 * Math.PI, Math.min(2 * Math.PI, this.arcSweep));
        const segments: number = Math.max(
            1,
            Math.trunc(Math.ceil(Math.abs(JavaMath.toDegrees(sweep)) / RotateGizmo.ARC_STEP_IN_DEGREES)),
        );
        const fan: Vector3Dd[] = new Array<Vector3Dd>(segments + 2);

        fan[0] = this.getPosition();
        for (let i = 0; i <= segments; i++) {
            fan[i + 1] = this.pointOfArc(this.arcStartAngle + (sweep * i) / segments, 1.0);
        }
        return fan;
    }

    /**
    @return the point, in world space, where the label with the angle of the
    arc must be anchored: just outside the ring, at the middle of the arc; or
    null if there is no arc to show
    */
    public getArcLabelPosition(): Vector3Dd | null {
        if (!this.isArcVisible()) {
            return null;
        }
        return this.pointOfArc(this.arcStartAngle + this.arcSweep / 2, RotateGizmo.ARC_LABEL_DISTANCE);
    }

    private arcRingRadius(): number {
        return this.arcRing === RotateGizmo.CAMERA_RING_INDEX ? this.getCameraRingRadius() : this.getRingRadius();
    }

    private pointOfArc(angle: number, radiusFactor: number): Vector3Dd {
        const radius: number = this.arcRingRadius() * radiusFactor;

        return this.getPosition()
            .add(this.arcU.multiply(Math.cos(angle) * radius))
            .add(this.arcV.multiply(Math.sin(angle) * radius));
    }

    //= Numeric input =====================================================

    /**
    @return the input gizmo that shows (and lets the user type) the angles, in
    degrees, of this gizmo, updated with its current orientation and
    highlighted rings. The first three fields are the orientation of the
    gizmo (see `extractAnglesInDegrees`); the fourth is always 0, and is used
    to type a relative angle to rotate around the camera axis
    */
    public getInputGizmo(): InputGizmo {
        const angles: number[] = RotateGizmo.extractAnglesInDegrees(this.transformationMatrix);

        for (let ring = 0; ring < RotateGizmo.RING_COUNT; ring++) {
            this.inputGizmo.setValue(ring, angles[ring]!);
            this.inputGizmo.setFieldHighlighted(ring, this.isRingHighlighted(ring));
        }
        this.inputGizmo.setValue(RotateGizmo.CAMERA_INPUT_FIELD_INDEX, 0.0);
        this.inputGizmo.setFieldHighlighted(RotateGizmo.CAMERA_INPUT_FIELD_INDEX, this.isCameraRingHighlighted());
        return this.inputGizmo;
    }

    /**
    Builds the rotation given by three angles: a rotation around the X axis,
    then one around the Y axis and then one around the Z axis, all of them
    around the axes of the world.

    @param xDegrees rotation around the X axis, in degrees
    @param yDegrees rotation around the Y axis, in degrees
    @param zDegrees rotation around the Z axis, in degrees
    @return the rotation matrix
    */
    public static createRotationFromAnglesInDegrees(xDegrees: number, yDegrees: number, zDegrees: number): Matrix4x4d {
        const rx: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(xDegrees), 1, 0, 0);
        const ry: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(yDegrees), 0, 1, 0);
        const rz: Matrix4x4d = new Matrix4x4d().axisRotation(JavaMath.toRadians(zDegrees), 0, 0, 1);

        return rz.multiply(ry.multiply(rx));
    }

    /**
    Inverse of `createRotationFromAnglesInDegrees`: the angles of the rotation
    around the X, Y and Z axes that give an orientation. The angle around Y is
    in [-90, 90] and the other ones are in (-180, 180], so an orientation may
    be given with angles different from the ones that were used to create it.
    When the Y rotation is +-90 degrees there are infinite solutions (gimbal
    lock) and the rotation around Z is taken as 0.

    @param rotation rotation matrix (its translation is ignored)
    @return the angles, in degrees, around the X, Y and Z axes
    */
    public static extractAnglesInDegrees(rotation: Matrix4x4d): number[] {
        const sinY: number = Math.max(-1.0, Math.min(1.0, -rotation.get(2, 0)));
        const y: number = Math.asin(sinY);
        let x: number;
        let z: number;

        if (Math.abs(sinY) < 1.0 - 1.0e-9) {
            x = Math.atan2(rotation.get(2, 1), rotation.get(2, 2));
            z = Math.atan2(rotation.get(1, 0), rotation.get(0, 0));
        } else {
            x = Math.atan2(-rotation.get(1, 2), rotation.get(1, 1));
            z = 0;
        }
        return [JavaMath.toDegrees(x), JavaMath.toDegrees(y), JavaMath.toDegrees(z)];
    }

    private static checkRing(ring: number): void {
        if (ring < 0 || ring >= RotateGizmo.RING_COUNT) {
            throw new IllegalArgumentException("Invalid ring: " + ring + ". It must be 0, 1 or 2 for the X, Y or Z axis.");
        }
    }

    private static checkRingOrCamera(ring: number): void {
        if (ring < 0 || ring > RotateGizmo.RING_COUNT) {
            throw new IllegalArgumentException(
                "Invalid ring: " + ring + ". It must be 0, 1 or 2 for the X, Y or Z axis, or " +
                    RotateGizmo.CAMERA_RING_INDEX + " for the camera ring.",
            );
        }
    }
}
