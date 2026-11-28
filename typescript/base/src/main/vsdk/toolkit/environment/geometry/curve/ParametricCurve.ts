//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =
//= [.SUN2006] Java 1.5.0 API, available at http://java.sun.com             =
//= [.FONT2006] The FontForge project home page, available at               =
//=            http://fontforge.sourceforge.net/bezier.html                 =

import { IndexOutOfBoundsException } from "../../../../../java/lang/IndexOutOfBoundsException.js";
import { NullPointerException } from "../../../../../java/lang/NullPointerException.js";
import { ArrayIndexOutOfBoundsException } from "../../../../../java/lang/ArrayIndexOutOfBoundsException.js";
import { Double } from "../../../../../java/lang/Double.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { VSDK } from "../../../common/VSDK.js";
import { Calligraphic3DBuffer } from "../../../media/Calligraphic3DBuffer.js";
import { Curve } from "./Curve.js";

/**
This class represents a set of curve segments, forming a poly-line.
The represented poly-line can be straight or curved, can be integral 
(continous) or broken in pieces (discontinous), and each piece can be 
open or closed. Usually, a poly-line is co-planar, all of its pieces are 
closed, and the first closed piece conforms the outer contour, while the 
remaining close pieces make holes inside the first piece.  In this scheme,
the order in which pieces and segments are defined is important.

Each segment is a parametric cubic curve with an infinite set of points in
the form <x(t), y(t), z(t)> for t in the interval [0.0, 1.0]. Only the
starting (t = 0) and ending (t = 1) points are explicity represented, 
and the remaining intermediate points can be calculated via interpolation 
using the `evaluate` method.

The behavior of each segment can vary depending of the type of its defining
control points. For each type of curve supported, there is a public constant
defined in this class that specify it, and a static matrix that represents the
interpolation equations used. 

Note that each control point is stored in the `points` ArrayList, and depending
of its type (which is stored in the `types` ArrayList), the number of 
Vector3Dd's that forms it varies, and its specific interpretation also varies.
Check the documentation of each identification type value constant to 
understand the exact interpretation of control points. The curve interpolation
scheme can be:
  - Reset interpolation sequence: the control point is not really a point,
    but a command that indicate to algorithms to break the poly-line in two
    pieces. Note that for this type of command, there is no Vector3Dd positions
    associated to array list, so the corresponding static vector is null or
    empty.
  - First order in respect to t: the curve segment is "linear", that is
    straight, the control points are "corners" between lines and each control
    point has just one Vector3Dd (the position of a line end)
  - Second order in respect to t: the curve segment is "quadratic"
  - Third order in respect to t: the curve segment is "cubic". There are
    multiple cubic curve interpretations based on the specific interpolation
    method used, and in specific control point interpretation.

From a design point of view, this class is similar to Sun Microsystems'
java.awt.geom.PathIterator class [.SUN2006], only that this one supports 
curves in 3D space, and not just in a 2D plane. The implementation follows the
parametric curve development derived in Chapter 11 of [FOLE1992].

\todo  Document in detail the interpretation of point data for each curve type.
\todo  Pending to implement some other curve types, like Catmull-Rom.
\todo  Include some curve examples and its corresponding data structure in
      this documentation.
\todo  Fix the definition of UNRBSPLINE_MATRIX, to have it divided by 6.
\todo  Add evaluateFirstDerivative and evaluateSecondDerivative functions
*/
export class ParametricCurve extends Curve {
    // Model's "basis matrices" for evaluating curve parametric equations like
    // described in [FOLE1992].11.2.

    // This equation was derived as the answer of exercise 11.3 in
    // [FOLE1992], from equation base 11.11
    public static LINEAR_MATRIX: Matrix4x4d = Matrix4x4d.copyOf([
        [0.0, 0.0, 0.0, 1.0],
        [0.0, 0.0, 1.0, 1.0],
        [-1.0, 1.0, 0.0, 0.0],
        [1.0, 0.0, 0.0, 0.0],
    ]);

    // Equation 11.19 in [FOLE1992]
    public static HERMITE_MATRIX: Matrix4x4d = Matrix4x4d.copyOf([
        [2.0, -2.0, 1.0, 1.0],
        [-3.0, 3.0, -2.0, -1.0],
        [0.0, 0.0, 1.0, 0.0],
        [1.0, 0.0, 0.0, 0.0],
    ]);

    // Equation 11.28 in [FOLE1992]
    public static BEZIER_MATRIX: Matrix4x4d = Matrix4x4d.copyOf([
        [-1.0, 3.0, -3.0, 1.0],
        [3.0, -6.0, 3.0, 0.0],
        [-3.0, 3.0, 0.0, 0.0],
        [1.0, 0.0, 0.0, 0.0],
    ]);

    // Equation 11.34 in [FOLE1992], but NOT multiplied by 1/6. (why?)
    public static UNRBSPLINE_MATRIX: Matrix4x4d = Matrix4x4d.copyOf([
        [-1.0, 3.0, -3.0, 1.0],
        [3.0, -6.0, 3.0, 0.0],
        [-3.0, 0.0, 3.0, 0.0],
        [1.0, 4.0, 1.0, 0.0],
    ]);

    // Equation 11.47 in [FOLE1992], but NOT multiplied by T. (why?)
    public static CATMULL_ROM_MATRIX: Matrix4x4d = Matrix4x4d.copyOf([
        [-0.5, 1.5, -1.5, 0.5],
        [1.0, -2.5, 2.0, -0.5],
        [-0.5, 0.0, 0.5, 0.0],
        [0.0, 1.0, 0.0, 0.0],
    ]);

    // Constants used for identification type values for controlling points

    /// Identification type value for specifying a discontinuity in the curve.
    /// A control point of this type doesn't have any Vector3Dd, but uses a
    /// slot (possibly null) in the `points` attribute.
    public static readonly BREAK = 1;
    /// Identification type value for straight lines. A control point of this
    /// type has one Vector3Dd, representing and end of a straight line.
    public static readonly CORNER = 2;
    /// Identification type value for quadratic curves. A control point of this
    /// type has two Vector3Dd.
    public static readonly QUAD = 3;
    /// Identification type value for cubic curve with Hermite interpolation.
    /// A control point of this type has three Vector3Dd.
    public static readonly HERMITE = 4;
    /// Identification type value for cubic curve with Bezier interpolation
    /// A control point of this type has three Vector3Dd.
    public static readonly BEZIER = 5;
    /// Identification type value for cubic curve with Uniform Non Rational
    /// B Spline interpolation
    /// A control point of this type has three Vector3Dd.
    public static readonly UNRBSPLINE = 6;
    /// Identification type value for cubic curve with Non Uniform Non
    /// Rational B Spline interpolation
    /// A control point of this type has three Vector3Dd.
    public static readonly NUNRBSPLINE = 7;
    /// Identification type value for cubic curve with Catmull-Rom
    /// interpolation
    /// A control point of this type has three Vector3Dd.
    public static readonly CATMULLROM = 8;

    /** Java's `2/3` and `1/3` in `evaluateQuadratic`: int divisions. */
    private static readonly TWO_THIRDS_AS_JAVA_INT = Math.trunc(2 / 3);
    private static readonly ONE_THIRD_AS_JAVA_INT = Math.trunc(1 / 3);

    // Curve model: a set of points, each having a type of data interpretation
    /// Controlling points array (Java's `ArrayList<Vector3Dd[]>`; the methods
    /// of this class check its indexes as `ArrayList` does)
    public points: (Vector3Dd[] | null)[];
    /// Controlling points types array. This array always has the same
    /// dimension of the `points` array. The values always correspond to one
    /// of the identification type values defined in this class (i.e.
    /// CORNER, HERMITE, BEZIER, etc.).
    public types: number[];

    // Number of steps for curve approximation
    private static readonly INITIAL_APPROXIMATION_STEPS = 50;
    private approximationSteps: number;

    /**
    This constructor takes care for the initialization of the matrices needed
    for the definition of the different types of curve model supported. Note
    that the matrices values are formally derived in [FOLE1992].11.2 section.

    The resulting curve created is the empty curve.

    \todo  Leave the UNRBSPLINE_MATRIX and CATMULL_ROM_MATRIX matrices
    initialized to standard teoretical values, with their scalar constants
    applied. This implies updating the evaluation methods.
    */
    public constructor() {
        super();
        //- Empty curve model creation ------------------------------------
        this.points = [];
        this.types = [];
        this.approximationSteps = ParametricCurve.INITIAL_APPROXIMATION_STEPS;
    }

    /** `ArrayList.get`, `set` and `remove(int)`: `Objects.checkIndex`. */
    private static checkIndex(index: number, length: number): void {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
    }

    /** `ArrayList.add(int, E)`: `rangeCheckForAdd`. */
    private static checkIndexForAdd(index: number, size: number): void {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private getListed<T>(list: T[], index: number): T {
        ParametricCurve.checkIndex(index, list.length);
        return list[index]!;
    }

    /**
    `c[k]` with Java's checks: a null control point array raises
    NullPointerException and an index outside it
    ArrayIndexOutOfBoundsException. A null element is returned, as Java reads
    it; it fails only where it is used (see `nonNull`).
    */
    private static elementOrNull(c: Vector3Dd[] | null, k: number): Vector3Dd | null {
        if (c === null) {
            throw new NullPointerException();
        }
        if (k < 0 || k >= c.length) {
            throw new ArrayIndexOutOfBoundsException("Index " + k + " out of bounds for length " + c.length);
        }
        return c[k] ?? null;
    }

    /** The NullPointerException of Java's first use of a null point. */
    private static nonNull(v: Vector3Dd | null): Vector3Dd {
        if (v === null) {
            throw new NullPointerException();
        }
        return v;
    }

    /** `c[k]` read and used at once. */
    private static element(c: Vector3Dd[] | null, k: number): Vector3Dd {
        return ParametricCurve.nonNull(ParametricCurve.elementOrNull(c, k));
    }

    public getApproximationSteps(): number {
        return this.approximationSteps;
    }

    public setApproximationSteps(n: number): void {
        this.approximationSteps = n;
    }

    public addPoint(point: Vector3Dd[] | null, type: number): void {
        if (type === ParametricCurve.BREAK && this.points.length < 1) {
            // It has no sense to insert a break command as the first segment
            // in the curve. A `BREAK` is only inserted after a first curve
            // segment is available in the curve model.
            return;
        }
        if (this.shouldSkipConsecutiveEndpoint(point, type)) {
            return;
        }
        this.points.push(point);
        this.types.push(type);
    }

    /**
    Calculates and return the number of pieces in current curve.
    @return number of pieces in current curve.
    */
    public getNumPieces(): number {
        let sum = 1,
            i: number;

        for (i = 1; i < this.types.length; i++) {
            if (this.types[i] === ParametricCurve.BREAK) {
                sum++;
            }
        }

        return sum;
    }

    public addPointAt(point: Vector3Dd[] | null, type: number, position: number): void {
        if (type === ParametricCurve.BREAK && position < 1) {
            // It has no sense to insert a break command as the first segment
            // in the curve. A `BREAK` is only inserted after a first curve
            // segment is available in the curve model.
            return;
        }
        ParametricCurve.checkIndexForAdd(position, this.points.length);
        this.points.splice(position, 0, point);
        ParametricCurve.checkIndexForAdd(position, this.types.length);
        this.types.splice(position, 0, type);
    }

    private shouldSkipConsecutiveEndpoint(point: Vector3Dd[] | null, type: number): boolean {
        if (
            type === ParametricCurve.BREAK ||
            point === null ||
            point.length < 1 ||
            (point[0] ?? null) === null ||
            this.points.length === 0
        ) {
            return false;
        }

        const previousIndex: number = this.points.length - 1;
        if (this.getListed(this.types, previousIndex) === ParametricCurve.BREAK) {
            return false;
        }

        const previousPoint: Vector3Dd[] | null = this.points[previousIndex]!;
        if (previousPoint === null || previousPoint.length < 1 || (previousPoint[0] ?? null) === null) {
            return false;
        }

        return Vector3Dd.distance(previousPoint[0]!, point[0]!) < VSDK.EPSILON;
    }

    public getPoint(pos: number): Vector3Dd[] | null {
        return this.getListed(this.points, pos);
    }

    public getPointSize(): number {
        return this.points.length;
    }

    /**
    \todo  should check after removal of element 0 that if next command is a
    BREAK, that command should be also be removed.
    @param pos
    */
    public removePoint(pos: number): void {
        ParametricCurve.checkIndex(pos, this.points.length);
        this.points.splice(pos, 1);
        ParametricCurve.checkIndex(pos, this.types.length);
        this.types.splice(pos, 1);
    }

    public setPointAt(p: Vector3Dd[] | null, pos: number): void {
        ParametricCurve.checkIndex(pos, this.points.length);
        this.points[pos] = p;
    }

    /**
    Given current ParametricCurve, this method takes the scalar
    parameter t, which must be inside the interval [0.0, 1.0], and
    returns the Vector3Dd resulting from the evaluation of the
    segment of the polycurve ending in the segment `endingSegment`.
    The ending segment type determines the interpretation of
    interpolants.

    Returns null if current endingSegment is not a segment but a BREAK
    command.
    @param endingSegment
    @param t
    @return a new Vector3Dd containing a point over the curve or null if not
    possible to compute
    */
    public evaluate(endingSegment: number, t: number): Vector3Dd | null {
        if (this.getListed(this.types, endingSegment) === ParametricCurve.CORNER) {
            return this.evaluateLinear(endingSegment, t);
        } else if (this.getListed(this.types, endingSegment) === ParametricCurve.QUAD) {
            return this.evaluateQuadratic(endingSegment, t);
        } else if (this.getListed(this.types, endingSegment) === ParametricCurve.HERMITE) {
            return this.evaluateHermite(endingSegment, t);
        } else if (this.getListed(this.types, endingSegment) === ParametricCurve.BEZIER) {
            return this.evaluateBezier(endingSegment, t);
        } else if (this.getListed(this.types, endingSegment) === ParametricCurve.UNRBSPLINE) {
            return this.evaluateBspline(endingSegment, t);
        }
        return null;
    }

    private evaluateLinear(nseg: number, t: number): Vector3Dd {
        const startingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg - 1);
        const endingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg);
        let p: Vector3Dd;

        // p1
        let result: Vector3Dd = ParametricCurve.element(startingSegmentControl, 0);
        let vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.LINEAR_MATRIX.get(i, 0) * Math.pow(t, 3 - i);
        }
        result = result.multiply(vt);

        // p2
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.LINEAR_MATRIX.get(i, 1) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(endingSegmentControl, 0);
        result = result.add(p.multiply(vt));

        // 0
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.LINEAR_MATRIX.get(i, 2) * Math.pow(t, 3 - i);
        }
        p = new Vector3Dd(0, 0, 0);
        result = result.add(p.multiply(vt));

        // 0
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.LINEAR_MATRIX.get(i, 3) * Math.pow(t, 3 - i);
        }
        p = new Vector3Dd(0, 0, 0);
        result = result.add(p.multiply(vt));

        return result;
    }

    /**
    Note that current implemented quadratic spline interpolation strategy
    is to first express the second order quadric as a third order Bezier
    spline, and later evaluate the Bezier spline, as noted on [.FONT2006].

    Java writes the two weights as `2/3` and `1/3`, which are int divisions
    and evaluate to 0; `TWO_THIRDS_AS_JAVA_INT` and `ONE_THIRD_AS_JAVA_INT`
    keep those values, so the two inner Bezier points are `qp0` and `qp1`.

    \todo  Express this interpolation as a QUAD_MATRIX and similar scheme to
    other points in this class.
    */
    private evaluateQuadratic(nseg: number, t: number): Vector3Dd {
        const startingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg - 1);
        const endingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg);
        let p: Vector3Dd;

        const qp0Read: Vector3Dd | null = ParametricCurve.elementOrNull(startingSegmentControl, 0);
        const qp1Read: Vector3Dd | null = ParametricCurve.elementOrNull(endingSegmentControl, 1);
        const qp2Read: Vector3Dd | null = ParametricCurve.elementOrNull(endingSegmentControl, 0);
        // Java uses all three below; a null one fails at its first use, and
        // nothing observable happens in between.
        const qp0: Vector3Dd = ParametricCurve.nonNull(qp0Read);
        const qp1: Vector3Dd = ParametricCurve.nonNull(qp1Read);
        const qp2: Vector3Dd = ParametricCurve.nonNull(qp2Read);

        // p0
        let result: Vector3Dd = qp0;
        let vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 0) * Math.pow(t, 3 - i);
        }
        result = result.multiply(vt);

        // p1
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 1) * Math.pow(t, 3 - i);
        }
        p = qp0.add(qp1.subtract(qp0).multiply(ParametricCurve.TWO_THIRDS_AS_JAVA_INT));
        result = result.add(p.multiply(vt));

        // p2
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 2) * Math.pow(t, 3 - i);
        }
        p = qp1.add(qp2.subtract(qp0).multiply(ParametricCurve.ONE_THIRD_AS_JAVA_INT));
        result = result.add(p.multiply(vt));

        // p3
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 3) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(endingSegmentControl, 0);
        result = result.add(p.multiply(vt));

        return result;
    }

    private evaluateHermite(nseg: number, t: number): Vector3Dd {
        const startingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg - 1);
        const endingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg);
        let p: Vector3Dd;

        // p1
        let result: Vector3Dd = ParametricCurve.element(startingSegmentControl, 0);
        let vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.HERMITE_MATRIX.get(i, 0) * Math.pow(t, 3 - i);
        }
        result = result.multiply(vt);

        // p4
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.HERMITE_MATRIX.get(i, 1) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(endingSegmentControl, 0);
        result = result.add(p.multiply(vt));

        // r1
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.HERMITE_MATRIX.get(i, 2) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(startingSegmentControl, 2);
        result = result.add(p.multiply(vt));

        // r4
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.HERMITE_MATRIX.get(i, 3) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(endingSegmentControl, 1);
        result = result.add(p.multiply(vt));

        return result;
    }

    private evaluateBezier(nseg: number, t: number): Vector3Dd {
        const startingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg - 1);
        const endingSegmentControl: Vector3Dd[] | null = this.getListed(this.points, nseg);
        let p: Vector3Dd;

        // p1
        let result: Vector3Dd = ParametricCurve.element(startingSegmentControl, 0);
        let vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 0) * Math.pow(t, 3 - i);
        }
        result = result.multiply(vt);

        // p2
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 1) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(startingSegmentControl, 2);
        result = result.add(p.multiply(vt));

        // p3
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 2) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(endingSegmentControl, 1);
        result = result.add(p.multiply(vt));

        // p4
        vt = 0;
        for (let i = 0; i < 4; i++) {
            vt += ParametricCurve.BEZIER_MATRIX.get(i, 3) * Math.pow(t, 3 - i);
        }
        p = ParametricCurve.element(endingSegmentControl, 0);
        result = result.add(p.multiply(vt));

        return result;
    }

    private evaluateBspline(nseg: number, t: number): Vector3Dd | null {
        if (this.points.length < 4) {
            return null;
        }
        let result: Vector3Dd = new Vector3Dd(0, 0, 0);
        for (let np = 0; np < 4; np++) {
            let vt = 0;
            for (let i = 0; i < 4; i++) {
                vt += ParametricCurve.UNRBSPLINE_MATRIX.get(i, np) * Math.pow(t, 3 - i);
            }
            const p: Vector3Dd = ParametricCurve.element(this.getListed(this.points, nseg - np), 0);
            // Note the 1/6 multiplication!
            result = result.add(p.multiply(vt / 6));
        }
        return result;
    }

    private calculatePointPosition(pin: number): number {
        let pout: number, i: number;

        pout = 0;
        for (i = 0; i < this.types.length && i < pin; i++) {
            if (this.types[i] === ParametricCurve.BREAK) {
                pout = -1;
            } else {
                pout++;
            }
        }
        return pout;
    }

    /**
    This method calculates the interpolation points for the segment that ends
    in the point `endingPointForSegment`.

    Note that as evaluation is able to manage the linear/corner case under
    the same numerical schema that is used for other curve types, the polyline
    can be interpreted as multiple segments for applications requiring.
    However, in cases as simple drawing, this leads to unnecessary line
    primitives. Due to that situation, the user can specify if breaking
    rects or not.
    @param endingPointForSegment
    @param withBrokenRects
    @return a set of points with a sampling over current curve
    */
    public calculatePoints(endingPointForSegment: number, withBrokenRects: boolean): Vector3Dd[] {
        const pol: Vector3Dd[] = [];

        // `relativePoint` is used to estimate current starting point
        // for curve. Starting points are -1 for break points (i.e. no curve),
        // and some curves needs previous N relative points, so this is
        // used to prevent computation of undefined curve segments.
        const relativePoint: number = this.calculatePointPosition(endingPointForSegment);

        if (
            (relativePoint <= 2 && this.getListed(this.types, endingPointForSegment) === ParametricCurve.UNRBSPLINE) ||
            relativePoint < 0
        ) {
            // The Uniform Non Rational B Splines require a least 3 control
            // points, which do not form a curve segment.
            // A break control point doesn't define any points
            return pol;
        }

        if (
            (this.getListed(this.types, endingPointForSegment) === ParametricCurve.CORNER && !withBrokenRects) ||
            relativePoint <= 0 ||
            (this.getListed(this.types, endingPointForSegment) === ParametricCurve.UNRBSPLINE && relativePoint < 3)
        ) {
            // Java's list takes a null point as it is; the callers fail on it.
            pol.push(
                ParametricCurve.elementOrNull(this.getListed(this.points, endingPointForSegment - 1), 0) as Vector3Dd,
            );
            pol.push(ParametricCurve.elementOrNull(this.getListed(this.points, endingPointForSegment), 0) as Vector3Dd);
        } else {
            let q: Vector3Dd | null = this.evaluate(endingPointForSegment, 0);
            if (q === null) {
                return pol;
            }
            pol.push(q);
            for (let i = 1; i <= this.approximationSteps; i++) {
                q = this.evaluate(endingPointForSegment, i / this.approximationSteps);
                if (q !== null) {
                    pol.push(q);
                }
            }
        }
        return pol;
    }

    /**
    This method returns an aproximate minmax of current curve, based on a
    sampling (evaluation) of points.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        const minmax: Float64Array = new Float64Array(6);
        let i: number, j: number;

        for (i = 0; i < 3; i++) minmax[i] = Double.MAX_VALUE;
        for (; i < 6; i++) minmax[i] = Double.MIN_VALUE;

        let p: Vector3Dd;

        for (i = 1; i < this.types.length; i++) {
            const polyline: Vector3Dd[] = this.calculatePoints(i, false);
            for (j = 0; j < polyline.length; j++) {
                p = ParametricCurve.nonNull(polyline[j] ?? null);
                if (p.x() < minmax[0]!) minmax[0] = p.x();
                if (p.y() < minmax[1]!) minmax[1] = p.y();
                if (p.z() < minmax[2]!) minmax[2] = p.z();
                if (p.x() > minmax[3]!) minmax[3] = p.x();
                if (p.y() > minmax[4]!) minmax[4] = p.y();
                if (p.z() > minmax[5]!) minmax[5] = p.z();
            }
        }
        return minmax;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doContainmentTest.
    \todo  Check efficiency for this implementation. Note that for the
    special application of volume rendering generation, it is better
    to provide another method, to add voxels after a path following
    over the line.
    @return INSIDE, OUTSIDE or LIMIT constant value
    */
    public override doContainmentTest(p: Vector3Dd, distanceTolerance: number): number {
        let i: number, j: number;
        let vec: Vector3Dd;

        for (i = 1; i < this.types.length; i++) {
            if (this.types[i] === ParametricCurve.BREAK) {
                i++;
                continue;
            }

            // Build a polyline for approximating the [i] curve segment
            const polyline: Vector3Dd[] = this.calculatePoints(i, false);

            // Solve problem for the polyline
            for (j = 0; j < polyline.length; j++) {
                vec = ParametricCurve.nonNull(polyline[j] ?? null);
                if (Vector3Dd.distance(vec, p) < distanceTolerance) {
                    return ParametricCurve.LIMIT;
                }
            }
        }

        return ParametricCurve.OUTSIDE;
    }

    /**
    Check the general interface contract in superclass method
    Curve.exportToCalligraphic3DBuffer. Each curve segment is approximated by
    the polyline of `calculatePoints` (with `getApproximationSteps()` steps),
    as when it is drawn with a 3D graphics API; break points start a new
    piece of the curve.
    @return a new buffer with the segments approximating the curve
    */
    public override exportToCalligraphic3DBuffer(): Calligraphic3DBuffer {
        const lines: Calligraphic3DBuffer = new Calligraphic3DBuffer();
        let i: number, j: number;

        for (i = 1; i < this.types.length; i++) {
            if (this.types[i] === ParametricCurve.BREAK) {
                i++;
                continue;
            }
            const polyline: Vector3Dd[] = this.calculatePoints(i, false);
            for (j = 0; j + 1 < polyline.length; j++) {
                lines.add3DLine(
                    ParametricCurve.nonNull(polyline[j] ?? null),
                    ParametricCurve.nonNull(polyline[j + 1] ?? null),
                );
            }
        }
        return lines;
    }
}
