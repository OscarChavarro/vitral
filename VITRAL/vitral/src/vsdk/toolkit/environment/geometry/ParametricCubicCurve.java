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
This class can represent a set of curve segments, forming a poly-curve. Each
segment is a parametric cubic curve with a set of points in the form
<x(t), y(t), z(t)> for t in the interval [0.0, 1.0]. The behavior of each
segment can vary depending of the type of its defining control points. Note
that each control point is stored in the `points` ArrayList, and depending
of its type, the number of Vector3d's that forms it varies.
*/

public class ParametricCubicCurve extends Geometry {
    // Model matrices for evaluating curve parametric equations like
    // described in [FOLE1992].11.2.
    public static Matrix4x4 HERMITE_MATRIX = null;
    public static Matrix4x4 BEZIER_MATRIX = null;
    public static Matrix4x4 UNRBSPLINE_MATRIX = null;
    public static Matrix4x4 CATMULL_ROM_MATRIX = null;

    // Constants used for identification of knots
    public static final int CORNER = 1;
    public static final int HERMITE = 2;
    public static final int BEZIER = 3;
    public static final int UNRBSPLINE = 4;
    public static final int NUNRBSPLINE = 5;
    public static final int CATMULLROM = 6;

    // Curve model: a set of points, each having a type of knot interpretation
    public ArrayList<Vector3D[]> points;
    public ArrayList<Integer> types;

    // Number of steps for curve approximation
    private static final int INITIAL_APPROXIMATION_STEPS = 50;
    private int approximationSteps;


    /**
    This constructor takes care for the initialization of the matrices needed
    for the definition of the different types of curve model supported. Note
    that the matrices values are formally derived in [FOLE1992].11.2 section.
    */
    public ParametricCubicCurve() {
        //- Matrix initialization -----------------------------------------
        if ( HERMITE_MATRIX == null ) {
            // All the matrixes are computed only for the first time this
            // class is instantiated. Note that this makes the Matrix4x4
            // creation efficient, but also makes this class non-re-entrant,
            // and not thread-safe.
            double[][] m;

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

        //-----------------------------------------------------------------
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


    public Vector3D evaluate(int n_seg,  float t) {
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

    /**
    TODO!
    */
    private Vector3D evaluateLinear(int nseg, float t) {
        return null;
    }

    private Vector3D evaluateHermite(int nseg, float t) {

        Vector3D[] pointB = points.get(nseg - 1);
        Vector3D[] pointA = points.get(nseg);
        //p1
        Vector3D result = pointB[0];
        double vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += HERMITE_MATRIX.M[i][0] * (Math.pow(t, 3 - i));
        }
        result = result.multiply(vt);

        //p4
        vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += HERMITE_MATRIX.M[i][1] * (Math.pow(t, 3 - i));
        }
        Vector3D p = pointA[0];
        result = result.add(p.multiply(vt));

        //r1
        vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += HERMITE_MATRIX.M[i][2] * (Math.pow(t, 3 - i));
        }
        p = pointB[2];
        result = result.add(p.multiply(vt));

        //r4
        vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += HERMITE_MATRIX.M[i][3] * (Math.pow(t, 3 - i));
        }
        p = pointA[1];
        result = result.add(p.multiply(vt));

        return result;
    }

    private Vector3D evaluateBezier(int nseg, float t) {
        Vector3D[] pointB = points.get(nseg - 1);
        //p1
        Vector3D result = pointB[0];
        double vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += BEZIER_MATRIX.M[i][0] * (Math.pow(t, 3 - i));
        }
        result = result.multiply(vt);

        //p2
        vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += BEZIER_MATRIX.M[i][1] * (Math.pow(t, 3 - i));
        }
        Vector3D p = pointB[2];
        result = result.add(p.multiply(vt));
        //p3
        vt = 0;

        Vector3D[] pointA = points.get(nseg);
        for (int i = 0; i < 4; i++) {
            vt += BEZIER_MATRIX.M[i][2] * (Math.pow(t, 3 - i));
        }
        p = pointA[1];
        result = result.add(p.multiply(vt));
        //p4
        vt = 0;
        for (int i = 0; i < 4; i++) {
            vt += BEZIER_MATRIX.M[i][3] * (Math.pow(t, 3 - i));
        }
        p = pointA[0];
        result = result.add(p.multiply(vt));

        //  System.out.print(result.toString());
        return result;
    }

    private Vector3D evaluateBspline(int nseg, float t) {
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
    in the point `endingPointForSegment`.
    */
    public ArrayList<Vector3D> calculatePoints(int endingPointForSegment) {
        ArrayList<Vector3D> pol = new ArrayList<Vector3D> ();

        if ( endingPointForSegment <= 2 &&
             types.get(endingPointForSegment) == UNRBSPLINE ) {
            // The Uniform Non Rational B Splines require a least 3 control
            // points, which do not form a curve segment.
            return pol;
    }

        if ( types.get(endingPointForSegment) == CORNER ||
             endingPointForSegment == 0 ||
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
                q = evaluate(endingPointForSegment,  i / (float) approximationSteps);
                if (q != null) {
                    pol.add(q);
                }
            }

        }
        return pol;
    }

    public boolean doIntersection(Ray r)
    {
        return false;
    }

    public void doExtraInformation(Ray inRay, double intT, 
                                      GeometryIntersectionInformation outData)
    {
        return;
    }

    /** TODO: This method must check min max values based on control point
    min max */
    public double[] getMinMax()
    {
        double minmax[] = new double[6];
        for ( int i = 0; i < 6; i++ ) {
        minmax[i] = 0.0;
    }
        return minmax;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
