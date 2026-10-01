import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import type { Geometry } from "../../environment/geometry/Geometry.js";
import type { ParametricCurve } from "../../environment/geometry/curve/ParametricCurve.js";
import { Arrow } from "../../environment/geometry/volume/Arrow.js";
import { Box } from "../../environment/geometry/volume/Box.js";
import { Cone } from "../../environment/geometry/volume/Cone.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { CurveModeler } from "../../processing/CurveModeler.js";
import type { ViewportElementScaler } from "../viewport/ViewportElementScaler.js";
import { Gizmo } from "./Gizmo.js";
import { InputGizmo } from "./InputGizmo.js";
import { ReferenceFrameGizmo } from "./ReferenceFrameGizmo.js";
import { TranslateGizmoLineSegment } from "./TranslateGizmoLineSegment.js";

export class TranslateGizmo extends Gizmo {
    /// Internal transformation state
    private transformationMatrix: Matrix4x4d | null = null;
    private camera!: Camera;

    /// Geometric model based in primitive instancing: primitive concretions
    private readonly arrowModel: Arrow;
    private readonly cylinderModel: Cone;
    private readonly boxModel: Box;
    private readonly coneModel: Cone;

    /// Geometric model based in primitive instancing: primitive instances
    /// This list is always of size 12, and its elements follow the order
    /// indicated in the values of the *_ELEMENT constants of this class.
    private readonly elements: SimpleBody[];
    private readonly elementInstances3dsmax: SimpleBody[];

    /// Internal element selection state
    public static readonly X_AXIS_ELEMENT = 1;
    public static readonly Y_AXIS_ELEMENT = 2;
    public static readonly Z_AXIS_ELEMENT = 3;
    public static readonly XYY_SEGMENT_ELEMENT = 4;
    public static readonly XYX_SEGMENT_ELEMENT = 5;
    public static readonly YZZ_SEGMENT_ELEMENT = 6;
    public static readonly YZY_SEGMENT_ELEMENT = 7;
    public static readonly XZZ_SEGMENT_ELEMENT = 8;
    public static readonly XZX_SEGMENT_ELEMENT = 9;
    public static readonly XY_BOX_ELEMENT = 10;
    public static readonly YZ_BOX_ELEMENT = 11;
    public static readonly XZ_BOX_ELEMENT = 12;

    public static readonly NULL_GROUP = 0;
    public static readonly X_AXIS_GROUP = 1;
    public static readonly Y_AXIS_GROUP = 2;
    public static readonly Z_AXIS_GROUP = 3;
    public static readonly XY_PLANE_GROUP = 4;
    public static readonly YZ_PLANE_GROUP = 5;
    public static readonly XZ_PLANE_GROUP = 6;

    private static readonly SEGMENT_LENGTH = 0.32;
    private static readonly SEGMENT_WIDTH = 0.02;
    private static readonly BOX_SIDE = 0.3;
    private static readonly BOX_HEIGHT = 0.01;
    private static readonly ARROW_LENGTH = 1.0;

    /// Size and line width designed for legacy resolutions
    public static readonly DEFAULT_APPARENT_SIZE_IN_PIXELS = 100;
    public static readonly DEFAULT_LINE_WIDTH = 1.0;

    /// Apparent size (in legacy resolution pixels) the user has chosen
    private baseApparentSizeInPixels: number;
    /// Apparent size in pixels of the screen currently in use
    private apparentSizeInPixels: number;
    /// Width, in pixels, of the lines of the gizmo
    private lineWidth: number;

    /// Interaction state
    private readonly inputGizmo: InputGizmo;
    private persistentSelection: number;
    private volatileSelection: number;

    private selectedResizing: boolean;
    private currentScale: number;

    public constructor(cam: Camera) {
        super();
        this.baseApparentSizeInPixels = TranslateGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS;
        this.apparentSizeInPixels = TranslateGizmo.DEFAULT_APPARENT_SIZE_IN_PIXELS;
        this.lineWidth = TranslateGizmo.DEFAULT_LINE_WIDTH;
        this.inputGizmo = new InputGizmo(3);

        const axisColors = new ReferenceFrameGizmo();

        for (let axis = 0; axis < 3; axis++) {
            this.inputGizmo.setFieldColor(axis, axisColors.getAxisColor(axis));
        }
        this.persistentSelection = TranslateGizmo.X_AXIS_GROUP;
        this.volatileSelection = TranslateGizmo.NULL_GROUP;

        // Total arrow length = 0.2 empty + 0.5 base + 0.3 head
        this.arrowModel = new Arrow(0.5 * TranslateGizmo.ARROW_LENGTH, 0.3 * TranslateGizmo.ARROW_LENGTH, 0.025, 0.05);
        this.cylinderModel = new Cone(TranslateGizmo.SEGMENT_WIDTH, TranslateGizmo.SEGMENT_WIDTH, TranslateGizmo.SEGMENT_LENGTH);
        this.boxModel = new Box(TranslateGizmo.BOX_SIDE, TranslateGizmo.BOX_SIDE, TranslateGizmo.BOX_HEIGHT);
        this.coneModel = new Cone(0.05, 0, 0.3 * TranslateGizmo.ARROW_LENGTH);

        this.elements = [];
        for (let i = 0; i < 12; i++) {
            const r = new SimpleBody();
            this.elements.push(r);
        }

        this.elementInstances3dsmax = [];
        for (let i = 0; i < 15; i++) {
            const r = new SimpleBody();
            this.elementInstances3dsmax.push(r);
        }

        this.setCamera(cam);
        this.selectedResizing = true;
        this.currentScale = 1.0;
    }

    public getApparentSizeInPixels(): number {
        return this.apparentSizeInPixels;
    }

    public setApparentSizeInPixels(du: number): void {
        this.apparentSizeInPixels = du;
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
    @return the width, in pixels, of the lines that draw the gizmo
    */
    public getLineWidth(): number {
        return this.lineWidth;
    }

    /**
    @param lineWidth width, in pixels, of the lines that draw the gizmo; not
    positive values are ignored
    */
    public setLineWidth(lineWidth: number): void {
        if (lineWidth > 0.0) {
            this.lineWidth = lineWidth;
        }
    }

    /**
    Sets the apparent size and the line width of the gizmo to the values
    that make it look proportional to the screen resolution: the base values
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
        this.setLineWidth(scaler.scaleLength(TranslateGizmo.DEFAULT_LINE_WIDTH));
    }

    /**
    @return the width of the lines of the gizmo, converted from pixels to
    world units, as seen from its camera at its current apparent size
    */
    public getLineWidthInWorldUnits(): number {
        return (this.lineWidth * this.currentScale) / this.apparentSizeInPixels;
    }

    /**
    Gives the straight lines drawn by the gizmo: the shaft of each axis arrow
    (from the gap around the origin up to the base of its head) and the
    segments that delimit the plane handles. Lines hidden because they point
    to the viewer of an orthogonal camera are not included, and the color of
    the lines of the selected group is yellow.
    PRE: the transformation matrix of the gizmo has been set.

    @return the lines of the gizmo, in world space
    */
    public getLineSegments(): TranslateGizmoLineSegment[] {
        const segments: TranslateGizmoLineSegment[] = [];
        const zAxis = new Vector3Dd(0, 0, 1);

        for (const element of this.elements) {
            const g: Geometry | null = element.getGeometry();
            let length: number;

            if (g === this.arrowModel) {
                length = this.currentScale * 0.5 * TranslateGizmo.ARROW_LENGTH;
            } else if (g === this.cylinderModel) {
                length = this.currentScale * TranslateGizmo.SEGMENT_LENGTH;
            } else {
                continue;
            }

            const start: Vector3Dd = element.getPosition();
            const end: Vector3Dd = start.add(element.getRotation().multiply(zAxis).multiply(length));

            segments.push(new TranslateGizmoLineSegment(start, end, element.getMaterial()!.getDiffuse()));
        }
        return segments;
    }

    /**
    Builds the geometry to draw a line of the gizmo as a line with thickness
    seen from the camera of the gizmo: a triangle strip (a rectangle of 4
    vertices, in strip order) in world space, facing the camera. Its width is
    `getLineWidth()` pixels, and it is extended half of that width at both
    ends (square caps), so lines that meet at a corner have no gaps.

    @param segment line to draw, as given by `getLineSegments()`
    @return the 4 vertices of the triangle strip, or null if the line has no
    length
    */
    public buildLineStrip(segment: TranslateGizmoLineSegment): Vector3Dd[] | null {
        let direction: Vector3Dd = segment.end().subtract(segment.start());
        const length: number = direction.length();

        if (length < VSDK.EPSILON) {
            return null;
        }
        direction = direction.multiply(1 / length);

        // Direction from the eye to the line, to face the camera
        let view: Vector3Dd;

        if (this.camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            view = this.camera.getFront();
        } else {
            view = segment.start().add(segment.end()).multiply(0.5).subtract(this.camera.getPosition());
        }

        let side: Vector3Dd = direction.crossProduct(view);

        if (side.length() < VSDK.EPSILON) {
            // Line pointing to the viewer: any direction in screen is valid
            side = this.camera.getUp();
        }

        const halfWidth: number = this.getLineWidthInWorldUnits() / 2;
        side = side.normalized().multiply(halfWidth);

        const start: Vector3Dd = segment.start().subtract(direction.multiply(halfWidth));
        const end: Vector3Dd = segment.end().add(direction.multiply(halfWidth));

        return [start.add(side), start.subtract(side), end.add(side), end.subtract(side)];
    }

    /**
    @return the factor applied to the gizmo geometry so it keeps its apparent
    size in pixels, as seen from its camera
    */
    public getCurrentScale(): number {
        return this.currentScale;
    }

    public setCamera(cam: Camera): void {
        this.camera = cam;
    }

    public getCamera(): Camera {
        return this.camera;
    }

    public getElements(): SimpleBody[] {
        return this.elements;
    }

    public getElements3dsmax(): SimpleBody[] {
        let i: number;
        let r: SimpleBody;
        let o: SimpleBody;
        let r2: SimpleBody;
        let g: Geometry | null;
        const red: SimpleMaterial = this.createMaterial(0.78, 0, 0);
        const green: SimpleMaterial = this.createMaterial(0, 0.61, 0);
        const blue: SimpleMaterial = this.createMaterial(0, 0, 0.76);

        const R: Matrix4x4d = new Matrix4x4d(this.requireTransformationMatrix()).withoutTranslation();
        let subR = new Matrix4x4d();
        let eleR: Matrix4x4d;
        let eleRi: Matrix4x4d;
        let subP: Vector3Dd;
        let eleP: Vector3Dd;

        this.coneModel.setBottomRadius(this.currentScale * 0.05);
        this.coneModel.setHeight(this.currentScale * 0.3 * TranslateGizmo.ARROW_LENGTH);
        this.boxModel.setSize(
            this.currentScale * (TranslateGizmo.BOX_SIDE + 0.025),
            this.currentScale * (TranslateGizmo.BOX_SIDE + 0.025),
            this.currentScale * TranslateGizmo.BOX_HEIGHT,
        );

        //-----------------------------------------------------------------
        const lineModel: ParametricCurve = CurveModeler.createLine(0, 0, 0, 0, 0, this.currentScale * 0.7);

        const segmentModel: ParametricCurve = CurveModeler.createLine(
            0, 0, 0, 0, 0, this.currentScale * TranslateGizmo.SEGMENT_LENGTH);

        //-----------------------------------------------------------------
        for (i = 0; i < this.elements.length; i++) {
            r = this.elementInstances3dsmax[i]!;
            o = this.elements[i]!;

            r.setMaterial(o.getMaterial());
            r.setRotation(o.getRotation());
            r.setRotationInverse(o.getRotationInverse());

            g = o.getGeometry();
            if (g !== null && g instanceof Arrow) {
                r.setGeometry(this.coneModel);
                switch (i) {
                    case 0:
                        // Rotation
                        subR = subR.axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
                        eleR = R.multiply(subR);
                        r.setRotation(eleR);
                        eleRi = new Matrix4x4d(eleR);
                        eleRi = eleRi.invert();
                        r.setRotationInverse(eleRi);
                        // Translation
                        subP = new Vector3Dd(0, 0, this.currentScale * 0.7 * TranslateGizmo.ARROW_LENGTH);
                        eleP = eleR.multiply(subP).add(this.getPosition());
                        r.setPosition(eleP);
                        r.setMaterial(red);
                        break;
                    case 1:
                        // Rotation
                        subR = subR.axisRotation(JavaMath.toRadians(90.0), -1, 0, 0);
                        eleR = R.multiply(subR);
                        r.setRotation(eleR);
                        eleRi = new Matrix4x4d(eleR);
                        eleRi = eleRi.invert();
                        r.setRotationInverse(eleRi);
                        // Translation
                        subP = new Vector3Dd(0, 0, this.currentScale * 0.7 * TranslateGizmo.ARROW_LENGTH);
                        eleP = eleR.multiply(subP).add(this.getPosition());
                        r.setPosition(eleP);
                        r.setMaterial(green);
                        break;
                    case 2:
                        // Rotation
                        subR = new Matrix4x4d();
                        eleR = R.multiply(subR);
                        r.setRotation(eleR);
                        eleRi = new Matrix4x4d(eleR);
                        eleRi = eleRi.invert();
                        r.setRotationInverse(eleRi);
                        // Translation
                        subP = new Vector3Dd(0, 0, this.currentScale * 0.7 * TranslateGizmo.ARROW_LENGTH);
                        eleP = eleR.multiply(subP).add(this.getPosition());
                        r.setPosition(eleP);
                        r.setMaterial(blue);
                        break;
                }

                r2 = this.elementInstances3dsmax[i + 12]!;
                r2.setMaterial(o.getMaterial());
                r2.setRotation(o.getRotation());
                r2.setRotationInverse(o.getRotationInverse());
                r2.setPosition(o.getPosition());
                r2.setGeometry(lineModel);
            } else if (g !== null && g instanceof Cone) {
                r.setPosition(o.getPosition());
                r.setGeometry(segmentModel);
            } else {
                r.setPosition(o.getPosition());
                r.setGeometry(g);
            }
        }

        return this.elementInstances3dsmax;
    }

    private createMaterial(r: number, g: number, b: number): SimpleMaterial {
        let m = new SimpleMaterial();

        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(new ColorRgb(r, g, b));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    /**
    This method updates the data structure contained in the `elementInstances`
    array starting from the given parameters.
    - translation is the position of the center of the gizmo
    - rotation is the rotation matrix containing the orientation of the gizmo
    - if autosize is false, initialdu, initialdv and camera parameters are not
      used, and the gizmo doesn't change its current size. If autosize is true,
      the gizmo size is changed such as from the current camera, the gizmo
      projection fit a 2D area of initialdu * initialdv pixels.
    - modelType must be one of the following values: MODEL_FOR_GRAVITY or
      MODEL_FOR_DISPLAY. Depending on this value the size of current
      geometric elements could change.
    */
    public calculateGeometryState(
        _translation: Vector3Dd,
        _rotation: Matrix4x4d,
        autosize: boolean,
        initialdu: number,
        camera: Camera,
    ): void {
        //-----------------------------------------------------------------
        let i: number;
        const red: SimpleMaterial = this.createMaterial(0.78, 0, 0);
        const green: SimpleMaterial = this.createMaterial(0, 0.61, 0);
        const blue: SimpleMaterial = this.createMaterial(0, 0, 0.76);
        const yellow: SimpleMaterial = this.createMaterial(1, 1, 0);
        let yellowTransparent: SimpleMaterial = this.createMaterial(1, 1, 0);

        yellowTransparent = yellowTransparent.withOpacity(0.2);

        let currentSelection: number;
        if (this.volatileSelection === TranslateGizmo.NULL_GROUP) {
            currentSelection = this.persistentSelection;
        } else {
            currentSelection = this.volatileSelection;
        }

        const R: Matrix4x4d = new Matrix4x4d(this.requireTransformationMatrix()).withoutTranslation();
        let subR = new Matrix4x4d();
        let eleR: Matrix4x4d;
        let eleRi: Matrix4x4d;
        let subP: Vector3Dd;
        let eleP: Vector3Dd;

        camera.updateVectors();

        // Java reads the field here, not the `autosize` parameter (which the
        // callers always pass with the same value)
        void autosize;
        if (this.selectedResizing) {
            const p: Vector3Dd = this.getPosition();
            let right: Vector3Dd = camera.getLeft().multiply(-1);

            right = right.normalized();
            const a: Vector3Dd | null = camera.projectPointUsingRayMethodResult(p);
            const b: Vector3Dd | null = camera.projectPointUsingRayMethodResult(p.add(right));

            // Keeps the last scale if the size in pixels can not be measured
            if (a !== null && b !== null) {
                const factor: number = Vector3Dd.distance(a, b);

                if (factor > VSDK.EPSILON) {
                    this.currentScale = initialdu / factor;
                }
            }
        }
        const scale: number = this.currentScale;

        this.arrowModel.setBaseLength(scale * 0.5 * TranslateGizmo.ARROW_LENGTH);
        this.arrowModel.setHeadLength(scale * 0.3 * TranslateGizmo.ARROW_LENGTH);
        this.arrowModel.setBaseRadius(scale * 0.025);
        this.arrowModel.setHeadRadius(scale * 0.05);
        this.cylinderModel.setBottomRadius(scale * TranslateGizmo.SEGMENT_WIDTH);
        this.cylinderModel.setTopRadius(scale * TranslateGizmo.SEGMENT_WIDTH);
        this.cylinderModel.setHeight(scale * TranslateGizmo.SEGMENT_LENGTH);
        this.boxModel.setSize(scale * TranslateGizmo.BOX_SIDE, scale * TranslateGizmo.BOX_SIDE, scale * TranslateGizmo.BOX_HEIGHT);

        //-----------------------------------------------------------------
        const front: Vector3Dd = camera.getFront();
        const axisI = new Vector3Dd(1, 0, 0);
        const axisJ = new Vector3Dd(0, 1, 0);
        const axisK = new Vector3Dd(0, 0, 1);

        const orthogonalCamera: boolean = camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL;
        const iPar: boolean = Math.abs(front.dotProduct(axisI)) > 1.0 - VSDK.EPSILON;
        const jPar: boolean = Math.abs(front.dotProduct(axisJ)) > 1.0 - VSDK.EPSILON;
        const kPar: boolean = Math.abs(front.dotProduct(axisK)) > 1.0 - VSDK.EPSILON;

        let index: number;
        for (i = 0, index = 1; index <= 12 && i < this.elements.length; index++, i++) {
            const r: SimpleBody = this.elements[i]!;
            r.setGeometry(null);
            switch (index) {
              case TranslateGizmo.X_AXIS_ELEMENT:
                if (!(orthogonalCamera && iPar)) {
                    // Basic model
                    r.setGeometry(this.arrowModel);
                    if (currentSelection === TranslateGizmo.X_AXIS_GROUP ||
                         currentSelection === TranslateGizmo.XY_PLANE_GROUP ||
                         currentSelection === TranslateGizmo.XZ_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(red);
                    }
                    // Rotation
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale*0.2* TranslateGizmo.ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.Y_AXIS_ELEMENT:
                  if (!(orthogonalCamera && jPar)) {
                    // Basic model
                    r.setGeometry(this.arrowModel);
                    if (currentSelection === TranslateGizmo.Y_AXIS_GROUP ||
                         currentSelection === TranslateGizmo.XY_PLANE_GROUP ||
                         currentSelection === TranslateGizmo.YZ_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(green);
                    }
                    // Rotation
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), -1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale*0.2* TranslateGizmo.ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.Z_AXIS_ELEMENT:
                if (!(orthogonalCamera && kPar)) {
                    // Basic model
                    r.setGeometry(this.arrowModel);
                    if (currentSelection === TranslateGizmo.Z_AXIS_GROUP ||
                         currentSelection === TranslateGizmo.YZ_PLANE_GROUP ||
                         currentSelection === TranslateGizmo.XZ_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(blue);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale*0.2* TranslateGizmo.ARROW_LENGTH);
                    eleP = eleR.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.XYY_SEGMENT_ELEMENT:
                if (!(orthogonalCamera && (iPar || jPar))) {
                    // Basic model
                    r.setGeometry(this.cylinderModel);
                    if (currentSelection === TranslateGizmo.XY_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(green);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, scale* TranslateGizmo.SEGMENT_LENGTH, 0);
                    eleP = eleR.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.XYX_SEGMENT_ELEMENT:
                if (!(orthogonalCamera && (iPar || jPar))) {
                    // Basic model
                    r.setGeometry(this.cylinderModel);
                    if (currentSelection === TranslateGizmo.XY_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(red);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), -1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale* TranslateGizmo.SEGMENT_LENGTH, 0, 0);
                    eleP = eleR.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.YZZ_SEGMENT_ELEMENT:
                // Basic model
                if (!(orthogonalCamera && (jPar || kPar))) {
                    r.setGeometry(this.cylinderModel);
                    if (currentSelection === TranslateGizmo.YZ_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(blue);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), -1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale* TranslateGizmo.SEGMENT_LENGTH);
                    eleP = R.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.YZY_SEGMENT_ELEMENT:
                if (!(orthogonalCamera && (jPar || kPar))) {
                    // Basic model
                    r.setGeometry(this.cylinderModel);
                    if (currentSelection === TranslateGizmo.YZ_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(green);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, scale* TranslateGizmo.SEGMENT_LENGTH, 0);
                    eleP = R.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.XZZ_SEGMENT_ELEMENT:
                if (!(orthogonalCamera && (iPar || kPar))) {
                    // Basic model
                    r.setGeometry(this.cylinderModel);
                    if (currentSelection === TranslateGizmo.XZ_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(blue);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, 0, scale* TranslateGizmo.SEGMENT_LENGTH);
                    eleP = R.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.XZX_SEGMENT_ELEMENT:
                if (!(orthogonalCamera && (iPar || kPar))) {
                    // Basic model
                    r.setGeometry(this.cylinderModel);
                    if (currentSelection === TranslateGizmo.XZ_PLANE_GROUP) {
                        r.setMaterial(yellow); 
                    }
                    else {
                        r.setMaterial(red);
                    }
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale* TranslateGizmo.SEGMENT_LENGTH, 0, 0);
                    eleP = R.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.XY_BOX_ELEMENT:
                if (!(orthogonalCamera && (iPar || jPar))) {
                    // Basic model
                    r.setGeometry(null);
                    if (currentSelection !== TranslateGizmo.XY_PLANE_GROUP) {
                        break;
                    }
                    r.setGeometry(this.boxModel);
                    r.setMaterial(yellowTransparent); 
                    // Rotation
                    subR = new Matrix4x4d();
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale*TranslateGizmo.BOX_SIDE/2, scale*TranslateGizmo.BOX_SIDE/2, 0);
                    eleP = R.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.YZ_BOX_ELEMENT:
                if (!(orthogonalCamera && (jPar || kPar))) {
                    // Basic model
                    r.setGeometry(null);
                    if (currentSelection !== TranslateGizmo.YZ_PLANE_GROUP) {
                        break;
                    }
                    r.setGeometry(this.boxModel);
                    r.setMaterial(yellowTransparent); 
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(0, scale*TranslateGizmo.BOX_SIDE/2, scale*TranslateGizmo.BOX_SIDE/2);
                    eleP = R.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
              case TranslateGizmo.XZ_BOX_ELEMENT:
                if (!(orthogonalCamera && (iPar || kPar))) {
                    // Basic model
                    r.setGeometry(null);
                    if (currentSelection !== TranslateGizmo.XZ_PLANE_GROUP) {
                        break;
                    }
                    r.setGeometry(this.boxModel);
                    r.setMaterial(yellowTransparent); 
                    // Rotation
                    subR = new Matrix4x4d();
                    subR = subR.axisRotation(JavaMath.toRadians(90.0), 1, 0, 0);
                    eleR = R.multiply(subR);
                    r.setRotation(eleR);
                    eleRi = new Matrix4x4d(eleR);
                    eleRi = eleRi.invert();
                    r.setRotationInverse(eleRi);
                    // Translation
                    subP = new Vector3Dd(scale*TranslateGizmo.BOX_SIDE/2, 0, scale*TranslateGizmo.BOX_SIDE/2);
                    eleP = R.multiply(subP).add(this.getPosition());
                    r.setPosition(eleP);
                }
                break;
            }
        }
    }

    public getPosition(): Vector3Dd {
        return this.requireTransformationMatrix().extractTranslation();
    }

    public setPosition(p: Vector3Dd): void {
        this.transformationMatrix = this.requireTransformationMatrix().withTranslation(p);
    }

    public setTransformationMatrix(transformationMatrix: Matrix4x4d): void {
        this.transformationMatrix = transformationMatrix;

        const R: Matrix4x4d = new Matrix4x4d(transformationMatrix).withoutTranslation();
        this.calculateGeometryState(this.getPosition(), R, this.selectedResizing, this.apparentSizeInPixels, this.camera);
    }

    public getTransformationMatrix(): Matrix4x4d | null {
        return this.transformationMatrix;
    }

    /**
    Java dereferences the matrix in these methods, failing with a
    `NullPointerException` while it has not been set; the same failure is
    raised here.
    */
    private requireTransformationMatrix(): Matrix4x4d {
        if (this.transformationMatrix === null) {
            throw new TypeError("The transformation matrix of the TranslateGizmo has not been set");
        }
        return this.transformationMatrix;
    }

    /**
    Recalculates the geometry of the elements of the gizmo from its current
    transformation, apparent size, resizing state, selection and camera.
    PRE: the transformation matrix of the gizmo has been set.
    */
    public updateGeometryState(): void {
        const R: Matrix4x4d = new Matrix4x4d(this.requireTransformationMatrix()).withoutTranslation();

        this.calculateGeometryState(this.getPosition(), R, this.selectedResizing, this.apparentSizeInPixels, this.camera);
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
    @return the group (one of the `*_GROUP` constants) that is highlighted and
    manipulated: the one under the cursor, or the chosen one if the cursor is
    not over any group
    */
    public getCurrentSelection(): number {
        if (this.volatileSelection === TranslateGizmo.NULL_GROUP) {
            return this.persistentSelection;
        }
        return this.volatileSelection;
    }

    /**
    @return the input gizmo that shows (and lets the user type) the
    coordinates of this gizmo, updated with its current position and highlighted
    axes
    */
    public getInputGizmo(): InputGizmo {
        if (this.transformationMatrix !== null) {
            const position: Vector3Dd = this.getPosition();

            this.inputGizmo.setValue(0, position.x());
            this.inputGizmo.setValue(1, position.y());
            this.inputGizmo.setValue(2, position.z());
        }
        for (let axis = 0; axis < 3; axis++) {
            this.inputGizmo.setFieldHighlighted(axis, this.isAxisHighlighted(axis));
        }
        return this.inputGizmo;
    }

    /**
    @param axis 0, 1 or 2 for the X, Y or Z axis
    @return true if the axis is highlighted (drawn yellow) because the group
    currently selected moves along it: the axis itself, or a plane that
    contains it
    */
    public isAxisHighlighted(axis: number): boolean {
        const currentSelection: number = this.getCurrentSelection();

        switch (axis) {
            case 0:
                return (
                    currentSelection === TranslateGizmo.X_AXIS_GROUP ||
                    currentSelection === TranslateGizmo.XY_PLANE_GROUP ||
                    currentSelection === TranslateGizmo.XZ_PLANE_GROUP
                );
            case 1:
                return (
                    currentSelection === TranslateGizmo.Y_AXIS_GROUP ||
                    currentSelection === TranslateGizmo.XY_PLANE_GROUP ||
                    currentSelection === TranslateGizmo.YZ_PLANE_GROUP
                );
            case 2:
                return (
                    currentSelection === TranslateGizmo.Z_AXIS_GROUP ||
                    currentSelection === TranslateGizmo.YZ_PLANE_GROUP ||
                    currentSelection === TranslateGizmo.XZ_PLANE_GROUP
                );
            default:
                return false;
        }
    }

    /**
    @return true if the size of the gizmo is recalculated to keep its apparent
    size in pixels, false if it is kept while it is being dragged
    */
    public isSelectedResizing(): boolean {
        return this.selectedResizing;
    }

    /**
    @param selectedResizing true if the size of the gizmo must be recalculated
    to keep its apparent size in pixels
    */
    public setSelectedResizing(selectedResizing: boolean): void {
        this.selectedResizing = selectedResizing;
    }
}
