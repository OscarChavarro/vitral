//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 20 2006 - Gina Chiquillo / David Camello: Original base version =
//= - April 28 2005 - Gina Chiquillo / Oscar Chavarro: quality check        =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.Geometry;

/**
This class can represents a set of curve segments, forming a poly-curve. Each
segment is a parametric cubic curve with a set of points in the form
<x(t), y(t), z(t)> for t in the interval [0.0, 1.0]. The behavior of each
segment can vary depending of the type of its defining control points. Note
that each control point is stored in the `points` ArrayList, and depending
of its type, the number of Vector3d's that forms it varies, and its
specific interpretation varies.

@todo Document in detail the interpretation of point data for each curve type.
@todo Pending to implement some other curve types, like Catmull-Rom.
*/

public class ParametricCubicCurve extends Curve {
    // Model matrices for evaluating curve parametric equations like
    // described in [FOLE1992].11.2.
    public static Matrix4x4 LINEAR_MATRIX = null;
    public static Matrix4x4 HERMITE_MATRIX = null;
    public static Matrix4x4 BEZIER_MATRIX = null;
    public static Matrix4x4 UNRBSPLINE_MATRIX = null;
    public static Matrix4x4 CATMULL_ROM_MATRIX = null;

    // Constants used for identification of defining points
    public static final int CORNER = 1; // Usual name for linear polycurves
    public static final int HERMITE = 2;
    public static final int BEZIER = 3;
    public static final int UNRBSPLINE = 4;
    public static final int NUNRBSPLINE = 5;
    public static final int CATMULLROM = 6;

    // Curve model: a set of points, each having a type of data interpretation
    public ArrayList<Vector3D[]> points;
    public ArrayList<Integer> types;

    // Number of steps for curve approximation
    private static final int INITIAL_APPROXIMATION_STEPS = 50;
    private int approximationSteps;

    /**
    This constructor takes care for the initialization of the matrices needed
    for the definition of the different types of curve model supported. Note
    that the matrices values are formally derived in [FOLE1992].11.2 section.
    <P>

    The resulting curve created is the empty curve.

    @todo Leave the UNRBSPLINE_MATRIX and CATMULL_ROM_MATRIX matrices 
    initialized to standard teoretical values, with their scalar constants 
    applied. This implies updating the evaluation methods.
    */
    public ParametricCubicCurve() {
        //- Matrix initialization -----------------------------------------
        // All the matrixes are computed only for the first time this
        // class is instantiated. Note that this makes the Matrix4x4
        // creation efficient, but also makes this class non-re-entrant,
        // and not thread-safe.
        if ( HERMITE_MATRIX == null ) {
            double[][] m;

            // This equation was derived as the answer of exercise 11.3 in
            // [FOLE1992], from equation base 11.11
            m = new double[][] { { 0.0,  0.0,  0.0,  1.0},
                                 { 0.0,  0.0,  1.0,  1.0},
                                 {-1.0,  1.0,  0.0,  0.0},
                                 { 1.0,  0.0,  0.0,  0.0} };
            LINEAR_MATRIX = new Matrix4x4();
            LINEAR_MATRIX.M = m;

            // Equation 11.19 in [FOLE1992]
            m = new double[][] { { 2.0, -2.0,  1.0,  1.0},
                                 {-3.0,  3.0, -2.0, -1.0},
                                 { 0.0,  0.0,  1.0,  0.0},
                                 { 1.0,  0.0,  0.0,  0.0} };
            HERMITE_MATRIX = new Matrix4x4();
            HERMITE_MATRIX.M = m;

            // Equation 11.28 in [FOLE1992]
            m = new double[][] { {-1.0,  3.0, -3.0,  1.0},
                                 { 3.0, -6.0,  3.0,  0.0},
                                 {-3.0,  3.0,  0.0,  0.0},
                                 { 1.0,  0.0,  0.0,  0.0} };
            BEZIER_MATRIX = new Matrix4x4();
            BEZIER_MATRIX.M = m;

            // Equation 11.34 in [FOLE1992], but NOT multiplied by 1/6. (why?)
            m = new double[][] { {-1.0,  3.0, -3.0,  1.0},
                                 { 3.0, -6.0,  3.0,  0.0},
                                 {-3.0,  0.0,  3.0,  0.0},
                                 { 1.0,  4.0,  1.0,  0.0} };
            UNRBSPLINE_MATRIX = new Matrix4x4();
            UNRBSPLINE_MATRIX.M = m;

            // Equation 11.47 in [FOLE1992], but NOT multiplied by T. (why?)
            m = new double[][] { {-0.5,  1.5, -1.5,  0.5},
                                 { 1.0, -2.5,  2.0, -0.5},
                                 {-0.5,  0.0,  0.5,  0.0},
                                 { 0.0,  1.0,  0.0,  0.0} };
            CATMULL_ROM_MATRIX = new Matrix4x4();
            CATMULL_ROM_MATRIX.M = m;
        }

        //- Empty curve model creation ------------------------------------
        points = new ArrayList<Vector3D[]> ();
        types = new ArrayList<Integer> ();
        approximationSteps = INITIAL_APPROXIMATION_STEPS;
    }

    public int getApproximationSteps()
    {
        return approximationSteps;
    }

    public void setApproximationSteps(int n)
    {
        approximationSteps = n;
    }

    public void addPoint(Vector3D[] point, int type) {
        points.add(point);
        types.add(new Integer(type));
    }

    public void addPointAt(Vector3D[] point, int type, int position) {
        points.add(position, point);
        types.add(position, new Integer(type));
    }

    public Vector3D[] getPoint(int pos) {
        return points.get(pos);
    }

    public int getPointSize() {
       return this.points.size();
    }

    public void removePoint(int pos) {
        points.remove(pos);
        types.remove(pos);
    }

    public void setPointAt(Vector3D[] p, int pos) {
       points.set(pos, p);
    }

    /**
    Given current ParametricCubicCurve, this method takes the scalar 
    parameter t, which must be inside the interval [0.0, 1.0], and 
    returns the Vector3D resulting from the evaluation of the n_seg
    segment of the polycurve.
    */
    public Vector3D evaluate(int n_seg,  double t) {
        if (types.get(n_seg).intValue() == CORNER ) {
            return evaluateLinear(n_seg, t);
        }
        else if (types.get(n_seg).intValue() == HERMITE ) {
            return evaluateHermite(n_seg, t);
        }
        else if (types.get(n_seg).intValue() == BEZIER ) {
            return evaluateBezier(n_seg, t);
        }
        else if (types.get(n_seg).intValue() == UNRBSPLINE ) {
            return evaluateBspline(n_seg, t);
        }
        return null;
    }

    private Vector3D evaluateLinear(int nseg, double t) {
        Vector3D[] startingSegmentPoint = points.get(nseg - 1);
        Vector3D[] endingSegmentPoint = points.get(nseg);
        Vector3D p;

        // p1
        Vector3D result = startingSegmentPoint[0];
        double vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += LINEAR_MATRIX.M[i][0] * (Math.pow(t, 3 - i));
        }
        result = result.multiply(vt);

        // p2
        vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += LINEAR_MATRIX.M[i][1] * (Math.pow(t, 3 - i));
        }
        p = endingSegmentPoint[0];
        result = result.add(p.multiply(vt));

        // 0
        vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += LINEAR_MATRIX.M[i][2] * (Math.pow(t, 3 - i));
        }
        p = new Vector3D(0, 0, 0);
        result = result.add(p.multiply(vt));

        // 0
        vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += LINEAR_MATRIX.M[i][3] * (Math.pow(t, 3 - i));
        }
        p = new Vector3D(0, 0, 0);
        result = result.add(p.multiply(vt));

        return result;
    }

    private Vector3D evaluateHermite(int nseg, double t) {
        Vector3D[] startingSegmentPoint = points.get(nseg - 1);
        Vector3D[] endingSegmentPoint = points.get(nseg);

        // p1
        Vector3D result = startingSegmentPoint[0];
        double vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += HERMITE_MATRIX.M[i][0] * (Math.pow(t, 3 - i));
        }
        result = result.multiply(vt);

        // p4
        vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += HERMITE_MATRIX.M[i][1] * (Math.pow(t, 3 - i));
        }
        Vector3D p = endingSegmentPoint[0];
        result = result.add(p.multiply(vt));

        // r1
        vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += HERMITE_MATRIX.M[i][2] * (Math.pow(t, 3 - i));
        }
        p = startingSegmentPoint[2];
        result = result.add(p.multiply(vt));

        // r4
        vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += HERMITE_MATRIX.M[i][3] * (Math.pow(t, 3 - i));
        }
        p = endingSegmentPoint[1];
        result = result.add(p.multiply(vt));

        return result;
    }

    private Vector3D evaluateBezier(int nseg, double t) {
        Vector3D[] startingSegmentPoint = points.get(nseg - 1);
        Vector3D[] endingSegmentPoint = points.get(nseg);
        Vector3D p;

        // p1
        Vector3D result = startingSegmentPoint[0];
        double vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += BEZIER_MATRIX.M[i][0] * (Math.pow(t, 3 - i));
        }
        result = result.multiply(vt);

        // p2
        vt = 0;
        for ( int i = 0; i < 4; i++ ) {
            vt += BEZIER_MATRIX.M[i][1] * (Math.pow(t, 3 - i));
        }
        p = startingSegmentPoint[2];
        result = result.add(p.multiply(vt));

        // p3
        vt = 0;

        for (int i = 0; i < 4; i++) {
            vt += BEZIER_MATRIX.M[i][2] * (Math.pow(t, 3 - i));
        }
        p = endingSegmentPoint[1];
        result = result.add(p.multiply(vt));

        // p4
        vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += BEZIER_MATRIX.M[i][3] * (Math.pow(t, 3 - i));
        }
        p = endingSegmentPoint[0];
        result = result.add(p.multiply(vt));

        return result;
    }

    private Vector3D evaluateBspline(int nseg, double t) {
        if (points.size() < 4) {
            return null;
        }
        Vector3D result = new Vector3D(0, 0, 0);
        for (int np = 0; np < 4; np++) {
            double vt = 0;
            for (int i = 0; i < 4; i++) {
                vt += UNRBSPLINE_MATRIX.M[i][np] * (Math.pow(t, 3 - i));
            }
            Vector3D p = points.get(nseg - np)[0];
            // Note the 1/6 multiplication!
            result = result.add(p.multiply(vt / 6));
        }
        return result;
    }

    /**
    This method calculates the interpolation points for the segment that ends
    in the point `endingPointForSegment`.<P>

    Note that as evaluation is able to manage the linear/corner case under
    the same numerical schema that is used for other curve types, the polyline
    can be interpreted as multiple segments for applications requiring.
    However, in cases as simple drawing, this leads to unnecessary line
    primitives. Due to that situation, the user can specify if breaking
    rects or not.
    */
    public ArrayList<Vector3D> calculatePoints(int endingPointForSegment,
                                               boolean withBrokenRects) {
        ArrayList<Vector3D> pol = new ArrayList<Vector3D> ();

        if ( endingPointForSegment <= 2 &&
             types.get(endingPointForSegment) == UNRBSPLINE ) {
            // The Uniform Non Rational B Splines require a least 3 control
            // points, which do not form a curve segment.
            return pol;
        }

        if ( (types.get(endingPointForSegment) == CORNER && !withBrokenRects) 
             || endingPointForSegment == 0 ||
             (types.get(endingPointForSegment) == UNRBSPLINE &&
              endingPointForSegment < 3)) {
            pol.add(points.get(endingPointForSegment)[0]);
            pol.add(points.get(endingPointForSegment - 1)[0]);
        }
        else {
            Vector3D q = evaluate(endingPointForSegment,  0);
            if (q == null) {
                return pol;
            }
            pol.add(q);
            for ( int i = 1; i <= approximationSteps; i++ ) {
                q = evaluate(endingPointForSegment, 
                             (double)i / (double)approximationSteps);
                if (q != null) {
                    pol.add(q);
                }
            }

        }
        return pol;
    }

    /**
    This method returns the minmax of current curve, for those curves whose
    bounding volume is greater than the minmax of its control points.

    @todo if any curve gets outside its convex hull, this method fails in
    giving the exact bounding volume
    */
    public double[] getMinMax()
    {
        double minmax[] = new double[6];
        int i;

        for ( i = 0; i < 3; i++ ) minmax[i] = Double.MAX_VALUE;
        for ( ; i < 6; i++ ) minmax[i] = Double.MIN_VALUE;

        Vector3D[] p;

    for ( i = 0; i < points.size(); i++ ) {
        p = points.get(i);
            if ( p[0].x < minmax[0] ) minmax[0] = p[0].x;
            if ( p[0].y < minmax[1] ) minmax[1] = p[0].y;
            if ( p[0].z < minmax[2] ) minmax[2] = p[0].z;
            if ( p[0].x > minmax[3] ) minmax[3] = p[0].x;
            if ( p[0].y > minmax[4] ) minmax[4] = p[0].y;
            if ( p[0].z > minmax[5] ) minmax[5] = p[0].z;
    }
        return minmax;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
