//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [WAYN1990] Knapp Wayne. "Ray with Bicubic Patch Intersection Problem",  =
//=            Ray Tracing News, volume 3, number 3, july 13 1990.          =
//=            available at                                                 =
//=         http://jedi.ks.uiuc.edu/~johns/raytracer/rtn/rtnv3n3.html#art19 =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 27 2006 - Gina Chiquillo / David Camello: Original base version =
//= - April 28 2005 - Gina Chiquillo / Oscar Chavarro: quality check        =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.Geometry;

public class ParametricBiCubicPatch extends Surface {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    public Matrix4x4 Gx_MATRIX = new Matrix4x4();
    public Matrix4x4 Gy_MATRIX = new Matrix4x4();
    public Matrix4x4 Gz_MATRIX = new Matrix4x4();

    public static final int QUAD = 7;

    /// Note that the contourCurve must have 4 points with its respective
    /// control parameters.
    public ParametricCubicCurve contourCurve;
    public int type;

    // Number of steps for curve approximation
    private static final int INITIAL_APPROXIMATION_STEPS = 12;
    private int approximationSteps;

    public ParametricBiCubicPatch(int type) {
        contourCurve = new ParametricCubicCurve();
        approximationSteps = INITIAL_APPROXIMATION_STEPS;
        this.type = type;
    }

    public ParametricBiCubicPatch(int type, ParametricCubicCurve curve) {
        this.contourCurve = curve;
        approximationSteps = INITIAL_APPROXIMATION_STEPS;
        this.type = type;
    }

    public int getApproximationSteps() {
        return approximationSteps;
    }

    public void setApproximationSteps(int n) {
        approximationSteps = n;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        type = type;
    }

    private void buildGeometryMatricesXYZ_Bezier() {
        double[][] mx = new double[4][4];
        double[][] my = new double[4][4];
        double[][] mz = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Vector3D[] vp = contourCurve.getPoint(i * 4 + j);
                mx[i][j] = vp[0].x;
                my[i][j] = vp[0].y;
                mz[i][j] = vp[0].z;
            }
        }
        Gx_MATRIX.M = mx;
        Gy_MATRIX.M = my;
        Gz_MATRIX.M = mz;
    }

    private void buildGeometryMatricesXYZ_Hermite() {
        double[][] mx = new double[4][4];
        double[][] my = new double[4][4];
        double[][] mz = new double[4][4];

        /* The upper-left 2x2 matrix portion of Gh contains the x coordinates
           of the four corners of the patch.
           The upper-right and lower-left 2x2 matrix portion of Gh contains
           the x coordiates of the tangent vectors four corners of the patch.
        */
        int p = 0;
        int i = 0;
        for (int j = 0; j < 2; j++) {
            Vector3D[] vp = contourCurve.getPoint(p);

            mx[i][j] = vp[0].x;
            my[i][j] = vp[0].y;
            mz[i][j] = vp[0].z;

            mx[i][j + 2] = vp[2 - j].x;
            my[i][j + 2] = vp[2 - j].y;
            mz[i][j + 2] = vp[2 - j].z;

            mx[i + 2][j] = vp[1 + j].x;
            my[i + 2][j] = vp[1 + j].y;
            mz[i + 2][j] = vp[1 + j].z;

            Vector3D vn = new Vector3D(0, 0, 0);

            mx[i + 2][j + 2] = vn.x;
            my[i + 2][j + 2] = vn.y;
            mz[i + 2][j + 2] = vn.z;
            p++;
        }
        p = 2;
        i = 1;
        for (int j = 0; j < 2; j++) {
            Vector3D[] vp = contourCurve.getPoint(p);

            mx[i][j] = vp[0].x;
            my[i][j] = vp[0].y;
            mz[i][j] = vp[0].z;

            mx[i][j + 2] = vp[j + 1].x;
            my[i][j + 2] = vp[j + 1].y;
            mz[i][j + 2] = vp[j + 1].z;

            mx[i + 2][j] = vp[2 - j].x;
            my[i + 2][j] = vp[2 - j].y;
            mz[i + 2][j] = vp[2 - j].z;

            Vector3D vn = new Vector3D(0, 0, 0);

            mx[i + 2][j + 2] = vn.x;
            my[i + 2][j + 2] = vn.y;
            mz[i + 2][j + 2] = vn.z;
            p++;

        }

        Gx_MATRIX.M = mx;
        Gy_MATRIX.M = my;
        Gz_MATRIX.M = mz;
    }

    private void buildGeometryMatricesXYZ_Quad() {
        double[][] mx = new double[4][4];
        double[][] my = new double[4][4];
        double[][] mz = new double[4][4];

        int i = 0;
        int sum = 1;
        int p = 0;
        for (int j = 0; j < 4; j += 3) {
            Vector3D[] vp = contourCurve.getPoint(p);
            mx[i][j] = vp[0].x;
            my[i][j] = vp[0].y;
            mz[i][j] = vp[0].z;

            mx[i + 1][j] = vp[1 + (j / 3)].x;
            my[i + 1][j] = vp[1 + (j / 3)].y;
            mz[i + 1][j] = vp[1 + (j / 3)].z;

            mx[i][j + sum] = vp[2 - (j / 3)].x;
            my[i][j + sum] = vp[2 - (j / 3)].y;
            mz[i][j + sum] = vp[2 - (j / 3)].z;

            Vector3D vn = vp[1].substract(vp[0]).add(vp[2].substract(vp[0]));
            vn = vn.add(vp[0]);
            mx[i + 1][j + sum] = vn.x;
            my[i + 1][j + sum] = vn.y;
            mz[i + 1][j + sum] = vn.z;
            p++;
            sum *= -1;
        }

        i = 3;
        sum = -1;

        for (int j = 3; j > -1; j -= 3) {
            Vector3D[] vp = contourCurve.getPoint(p);
            mx[i][j] = vp[0].x;
            my[i][j] = vp[0].y;
            mz[i][j] = vp[0].z;

            mx[i - 1][j] = vp[2 - (j / 3)].x;
            my[i - 1][j] = vp[2 - (j / 3)].y;
            mz[i - 1][j] = vp[2 - (j / 3)].z;

            mx[i][j + sum] = vp[1 + (j / 3)].x;
            my[i][j + sum] = vp[1 + (j / 3)].y;
            mz[i][j + sum] = vp[1 + (j / 3)].z;

            Vector3D vn = vp[1].substract(vp[0]).add(vp[2].substract(vp[0]));
            vn = vn.add(vp[0]);
            mx[i - 1][j + sum] = vn.x;
            my[i - 1][j + sum] = vn.y;
            mz[i - 1][j + sum] = vn.z;

            p++;
            sum *= -1;
        }

        Gx_MATRIX.M = mx;
        Gy_MATRIX.M = my;
        Gz_MATRIX.M = mz;
    }

    /**
    This methods evaluates equation set 11.76 in [FOLE1992] for a given set
    of approximationSteps^2 points, and in the code, the variables names
    follows the following notation:
    <UL>
      <LI> S_MATRIX  Column vector for storing s parameter polinomial
      <LI> M_MATRIX  Patch's blending function
      <LI> Gx_MATRIX Geometry matrix for x
      <LI> Gy_MATRIX Geometry matrix for y
      <LI> Gz_MATRIX Geometry matrix for z
      <LI> Mt_MATRIX M's transpose
      <LI> Tt_MATRIX Row vector for storing t parameter polinomial
    </UL>
    */
    public double[][][] evaluateSurface() {
        //- Build matrices M, Gx, Gy, Gz and Mt ---------------------------
        Matrix4x4 M_MATRIX = new Matrix4x4();
        Matrix4x4 Mt_MATRIX = new Matrix4x4();

        if ( this.type == ParametricCubicCurve.BEZIER ) {
            buildGeometryMatricesXYZ_Bezier();
            M_MATRIX = ParametricCubicCurve.BEZIER_MATRIX;
        }
        else if ( this.type == ParametricCubicCurve.HERMITE ) {
            buildGeometryMatricesXYZ_Hermite();
            M_MATRIX = ParametricCubicCurve.HERMITE_MATRIX;
        }
        else if ( this.type == ParametricBiCubicPatch.QUAD ) {
            buildGeometryMatricesXYZ_Quad();
            M_MATRIX = ParametricCubicCurve.BEZIER_MATRIX;
        }
        Mt_MATRIX = new Matrix4x4(M_MATRIX);
        Mt_MATRIX.transpose();

        //-----------------------------------------------------------------
        // In the current dimension
        double[][][] gridPoints;

        gridPoints = new double[approximationSteps][approximationSteps][3];

        Matrix4x4 S_MATRIX = new Matrix4x4();
        Matrix4x4 Tt_MATRIX = new Matrix4x4();

        for ( int d = 0; d < 3; d++ ) { // for each equation...
            // Select the geometry matrix for current dimension
            Matrix4x4 M_G_Mt_MATRIX = new Matrix4x4();

            if ( d == 0 ) {
                // x(s, t)
                M_G_Mt_MATRIX =
                    M_MATRIX.multiply(Gx_MATRIX).multiply(Mt_MATRIX);
            }
            else if ( d == 1 ) {
                // y(s, t)
                M_G_Mt_MATRIX =
                    M_MATRIX.multiply(Gy_MATRIX).multiply(Mt_MATRIX);
            }
            else {
                // y(s, t)
                M_G_Mt_MATRIX = 
                    M_MATRIX.multiply(Gz_MATRIX).multiply(Mt_MATRIX);
            }

            // Evaluate current dimension in the generic equational form
            for ( int i = 0; i < approximationSteps; i++ ) {
                double s = (double) i / (approximationSteps - 1);

                S_MATRIX.M[0][0] = s * s * s;
                S_MATRIX.M[0][1] = s * s;
                S_MATRIX.M[0][2] = s;
                S_MATRIX.M[0][3] = 1;

                Matrix4x4 S_M_G_Mt_MATRIX = S_MATRIX.multiply(M_G_Mt_MATRIX);

                for ( int j = 0; j < approximationSteps; j++ ) {
                    double t = (double) j / (approximationSteps - 1);

                    Tt_MATRIX.M[0][0] = t * t * t;
                    Tt_MATRIX.M[1][0] = t * t;
                    Tt_MATRIX.M[2][0] = t;
                    Tt_MATRIX.M[3][0] = 1;

                    Matrix4x4 Q_MATRIX = S_M_G_Mt_MATRIX.multiply(Tt_MATRIX);
                    // The result is a 1x1 matrix.
                    gridPoints[i][j][d] = Q_MATRIX.M[0][0];
                }
            }
        }

        return gridPoints;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.

    @todo implement the method
    */
    public boolean doIntersection(Ray r) {
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.

    Check the discution in [WAYN1990] about solvin this problem. Two main
    strategies are known for solving this: a numeric root finding,
    trying different values for Ray.t until a given error tolerance is
    reached and converting the patch to a mesh and test the mesh.

    This method implements the numerical approach, while an explicit
    convertion to a Mesh could be managed by the user/programmer directly.
    WARNING: The numerical approach is really, really slow... 

    @todo implement the method
    */
    public void doExtraInformation(Ray inRay, double intT, 
                                   GeometryIntersectionInformation outData) {
        return;
    }

    /** 
    Returns an approximate bounding volume minmax for current patch, from
    the minmax of its contour curve.

    @bug current contour curve asumption is not valid
    */
    public double[] getMinMax() {
        return contourCurve.getMinMax();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
