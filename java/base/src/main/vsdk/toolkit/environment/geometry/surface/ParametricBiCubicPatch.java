//= References:                                                             =
//= [WAYN1990] Knapp Wayne. "Ray with Bicubic Patch Intersection Problem",  =
//=            Ray Tracing News, volume 3, number 3, july 13 1990.          =
//=            available at                                                 =
//=         http://jedi.ks.uiuc.edu/~johns/raytracer/rtn/rtnv3n3.html#art19 =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =

package vsdk.toolkit.environment.geometry.surface;
import java.io.Serial;


import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.element.RayHit;

public class ParametricBiCubicPatch extends Surface {
    @Serial private static final long serialVersionUID = 20060502L;

    public Matrix4x4d geometryMatrixX = new Matrix4x4d();
    public Matrix4x4d geometryMatrixY = new Matrix4x4d();
    public Matrix4x4d geometryMatrixZ = new Matrix4x4d();

    private Matrix4x4d sParameterMatrix;
    private Matrix4x4d tParameterMatrix;
    private Matrix4x4d sDerivativeParameterMatrix;
    private Matrix4x4d tDerivativeParameterMatrix;
    private Matrix4x4d basisMatrix;
    private Matrix4x4d transposedBasisMatrix;
    private Matrix4x4d coefficientMatrixX;
    private Matrix4x4d coefficientMatrixY;
    private Matrix4x4d coefficientMatrixZ;

    public static final int FERGUSON = 7;

    /// Note that the contourCurve must have 4 points with its respective
    /// control parameters.
    public ParametricCurve contourCurve;
    private Vector3Dd controlMeshPoints[][];
    public int type;

    // Number of steps for curve approximation
    private static final int INITIAL_APPROXIMATION_STEPS = 12;
    private int approximationSteps;

    public ParametricBiCubicPatch() {
        approximationSteps = INITIAL_APPROXIMATION_STEPS;
        this.type = ParametricCurve.HERMITE;
        contourCurve = null;
        sParameterMatrix = null;
        tParameterMatrix = null;
        sDerivativeParameterMatrix = null;
        tDerivativeParameterMatrix = null;
        basisMatrix = null;
        transposedBasisMatrix = null;
        coefficientMatrixX = null;
        coefficientMatrixY = null;
        coefficientMatrixZ = null;
    }

    /**
    Given a contour curve, this constructor builds a corresponding
    FERGUSON type patch.

    PRE: Contour curve must has at least 4 control points. The first
    4 control points (the only one used) must be of type HERMITE.

    Note that a Ferguson patch is an Hermite patch with zero valued
    twist vectors.
    @param curve
    */
    public void buildFergusonPatch(ParametricCurve curve) {
        this.contourCurve = curve;
        approximationSteps = INITIAL_APPROXIMATION_STEPS;
        this.type = FERGUSON;
        calculateMatrices();
    }

    public void buildBezierPatch(Vector3Dd controlMeshPoints[][])
    {
        this.controlMeshPoints = controlMeshPoints;
        approximationSteps = INITIAL_APPROXIMATION_STEPS;
        this.type = ParametricCurve.BEZIER;
        calculateMatrices();
    }

    /**
    PRE: Some of the "build*Patch" methods should be called before calling
    this method.
    */
    private void calculateMatrices()
    {
        //- Build matrices M, Gx, Gy, Gz and Mt ---------------------------
        if ( this.type == ParametricCurve.BEZIER ) {
            buildGeometryMatricesXYZ_Bezier();
            basisMatrix = ParametricCurve.BEZIER_MATRIX;
        }
        else if ( this.type == ParametricCurve.HERMITE ) {
            buildGeometryMatricesXYZ_Hermite();
            basisMatrix = ParametricCurve.HERMITE_MATRIX;
        }
        else if ( this.type == ParametricBiCubicPatch.FERGUSON ) {
            buildGeometryMatricesXYZ_Ferguson();
            basisMatrix = ParametricCurve.HERMITE_MATRIX;
        }
        transposedBasisMatrix = new Matrix4x4d(basisMatrix);
        transposedBasisMatrix = transposedBasisMatrix.transpose();
        coefficientMatrixX = basisMatrix.multiply(geometryMatrixX).multiply(transposedBasisMatrix);
        coefficientMatrixY = basisMatrix.multiply(geometryMatrixY).multiply(transposedBasisMatrix);
        coefficientMatrixZ = basisMatrix.multiply(geometryMatrixZ).multiply(transposedBasisMatrix);
        sParameterMatrix = new Matrix4x4d();
        tParameterMatrix = new Matrix4x4d();
        sDerivativeParameterMatrix = new Matrix4x4d();
        tDerivativeParameterMatrix = new Matrix4x4d();
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
        this.type = type;
    }

    private void buildGeometryMatricesXYZ_Bezier() {
        double[][] mx = new double[4][4];
        double[][] my = new double[4][4];
        double[][] mz = new double[4][4];
        int i;
        int j;

        for ( i = 0; i < 4; i++ ) {
            for ( j = 0; j < 4; j++ ) {
                Vector3Dd vp = controlMeshPoints[i][j];
                mx[i][j] = vp.x();
                my[i][j] = vp.y();
                mz[i][j] = vp.z();
            }
        }
        geometryMatrixX = Matrix4x4d.copyOf(mx);
        geometryMatrixY = Matrix4x4d.copyOf(my);
        geometryMatrixZ = Matrix4x4d.copyOf(mz);
        //printGeometryMatrices();
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
            Vector3Dd[] vp = contourCurve.getPoint(p);

            mx[i][j] = vp[0].x();
            my[i][j] = vp[0].y();
            mz[i][j] = vp[0].z();

            mx[i][j + 2] = vp[2 - j].x();
            my[i][j + 2] = vp[2 - j].y();
            mz[i][j + 2] = vp[2 - j].z();

            mx[i + 2][j] = vp[1 + j].x();
            my[i + 2][j] = vp[1 + j].y();
            mz[i + 2][j] = vp[1 + j].z();

            Vector3Dd vn = new Vector3Dd(0, 0, 0);

            mx[i + 2][j + 2] = vn.x();
            my[i + 2][j + 2] = vn.y();
            mz[i + 2][j + 2] = vn.z();
            p++;
        }
        p = 2;
        i = 1;
        for (int j = 0; j < 2; j++) {
            Vector3Dd[] vp = contourCurve.getPoint(p);

            mx[i][j] = vp[0].x();
            my[i][j] = vp[0].y();
            mz[i][j] = vp[0].z();

            mx[i][j + 2] = vp[j + 1].x();
            my[i][j + 2] = vp[j + 1].y();
            mz[i][j + 2] = vp[j + 1].z();

            mx[i + 2][j] = vp[2 - j].x();
            my[i + 2][j] = vp[2 - j].y();
            mz[i + 2][j] = vp[2 - j].z();

            Vector3Dd vn = new Vector3Dd(0, 0, 0);

            mx[i + 2][j + 2] = vn.x();
            my[i + 2][j + 2] = vn.y();
            mz[i + 2][j + 2] = vn.z();
            p++;

        }

        geometryMatrixX = Matrix4x4d.copyOf(mx);
        geometryMatrixY = Matrix4x4d.copyOf(my);
        geometryMatrixZ = Matrix4x4d.copyOf(mz);
    }

    public void printGeometryMatrices()
    {
        double[][] mx = geometryMatrixX.toArrayCopy();
        double[][] my = geometryMatrixY.toArrayCopy();
        double[][] mz = geometryMatrixZ.toArrayCopy();

        System.out.println(
            "[ <"   + VSDK.formatDouble(mx[0][0]) + 
            ", "    + VSDK.formatDouble(my[0][0]) +
            ", "    + VSDK.formatDouble(mz[0][0]) +
            "> | <" + VSDK.formatDouble(mx[0][1]) +
            ", "    + VSDK.formatDouble(my[0][1]) +
            ", "    + VSDK.formatDouble(mz[0][1]) +
            "> | <" + VSDK.formatDouble(mx[0][2]) +
            ", "    + VSDK.formatDouble(my[0][2]) + 
            ", "    + VSDK.formatDouble(mz[0][2]) +
            "> | <" + VSDK.formatDouble(mx[0][3]) + 
            ", "    + VSDK.formatDouble(my[0][3]) + 
            ", "    + VSDK.formatDouble(mz[0][3]) +
            "> ]");

        System.out.println(
            "[ <"   + VSDK.formatDouble(mx[1][0]) + 
            ", "    + VSDK.formatDouble(my[1][0]) +
            ", "    + VSDK.formatDouble(mz[1][0]) +
            "> | <" + VSDK.formatDouble(mx[1][1]) +
            ", "    + VSDK.formatDouble(my[1][1]) +
            ", "    + VSDK.formatDouble(mz[1][1]) +
            "> | <" + VSDK.formatDouble(mx[1][2]) +
            ", "    + VSDK.formatDouble(my[1][2]) + 
            ", "    + VSDK.formatDouble(mz[1][2]) +
            "> | <" + VSDK.formatDouble(mx[1][3]) + 
            ", "    + VSDK.formatDouble(my[1][3]) + 
            ", "    + VSDK.formatDouble(mz[1][3]) +
            "> ]");
        
        System.out.println(
            "[ <"   + VSDK.formatDouble(mx[2][0]) + 
            ", "    + VSDK.formatDouble(my[2][0]) +
            ", "    + VSDK.formatDouble(mz[2][0]) +
            "> | <" + VSDK.formatDouble(mx[2][1]) +
            ", "    + VSDK.formatDouble(my[2][1]) +
            ", "    + VSDK.formatDouble(mz[2][1]) +
            "> | <" + VSDK.formatDouble(mx[2][2]) +
            ", "    + VSDK.formatDouble(my[2][2]) + 
            ", "    + VSDK.formatDouble(mz[2][2]) +
            "> | <" + VSDK.formatDouble(mx[2][3]) + 
            ", "    + VSDK.formatDouble(my[2][3]) + 
            ", "    + VSDK.formatDouble(mz[2][3]) +
            "> ]");

        System.out.println(
            "[ <"   + VSDK.formatDouble(mx[3][0]) + 
            ", "    + VSDK.formatDouble(my[3][0]) +
            ", "    + VSDK.formatDouble(mz[3][0]) +
            "> | <" + VSDK.formatDouble(mx[3][1]) +
            ", "    + VSDK.formatDouble(my[3][1]) +
            ", "    + VSDK.formatDouble(mz[3][1]) +
            "> | <" + VSDK.formatDouble(mx[3][2]) +
            ", "    + VSDK.formatDouble(my[3][2]) + 
            ", "    + VSDK.formatDouble(mz[3][2]) +
            "> | <" + VSDK.formatDouble(mx[3][3]) + 
            ", "    + VSDK.formatDouble(my[3][3]) + 
            ", "    + VSDK.formatDouble(mz[3][3]) +
            "> ]");

    }

    /**
    \todo  verify that current contour curve have at least 4 control points
    and that first 4 control points are of type HERMITE.
    */
    private void buildGeometryMatricesXYZ_Ferguson() {
        double[][] mx = new double[4][4];
        double[][] my = new double[4][4];
        double[][] mz = new double[4][4];
        Vector3Dd[] vp00 = contourCurve.getPoint(0);
        Vector3Dd[] vp10 = contourCurve.getPoint(1);
        Vector3Dd[] vp11 = contourCurve.getPoint(2);
        Vector3Dd[] vp01 = contourCurve.getPoint(3);

        // Positions with respect to contour curve
        mx[0][0] = vp00[0].x();
        my[0][0] = vp00[0].y();
        mz[0][0] = vp00[0].z();
        mx[0][1] = vp01[0].x();
        my[0][1] = vp01[0].y();
        mz[0][1] = vp01[0].z();
        mx[1][0] = vp10[0].x();
        my[1][0] = vp10[0].y();
        mz[1][0] = vp10[0].z();
        mx[1][1] = vp11[0].x();
        my[1][1] = vp11[0].y();
        mz[1][1] = vp11[0].z();

        // Partial derivatives with respect to S direction
        // For Hermite contour
        mx[2][0] = (vp00[2].x());
        my[2][0] = (vp00[2].y());
        mz[2][0] = (vp00[2].z());
        mx[2][1] = -(vp01[1].x());
        my[2][1] = -(vp01[1].y());
        mz[2][1] = -(vp01[1].z());
        mx[3][0] = (vp10[1].x());
        my[3][0] = (vp10[1].y());
        mz[3][0] = (vp10[1].z());
        mx[3][1] = -(vp11[2].x());
        my[3][1] = -(vp11[2].y());
        mz[3][1] = -(vp11[2].z());

        // Partial derivatives with respect to T direction
        // For Hermite contour
        mx[0][2] = -(vp00[1].x());
        my[0][2] = -(vp00[1].y());
        mz[0][2] = -(vp00[1].z());
        mx[0][3] = -(vp01[2].x());
        my[0][3] = -(vp01[2].y());
        mz[0][3] = -(vp01[2].z());
        mx[1][2] = (vp10[2].x());
        my[1][2] = (vp10[2].y());
        mz[1][2] = (vp10[2].z());
        mx[1][3] = (vp11[1].x());
        my[1][3] = (vp11[1].y());
        mz[1][3] = (vp11[1].z());

        // Ferguson patch: twist vectors (second order partial derivatives)
        // are all 0, as noted on [FOLE1992].11.3.1, equation [FOLE1992].11.84
        mx[2][2] = 0;
        my[2][2] = 0;
        mz[2][2] = 0;
        mx[2][3] = 0;
        my[2][3] = 0;
        mz[2][3] = 0;
        mx[3][2] = 0;
        my[3][2] = 0;
        mz[3][2] = 0;
        mx[3][3] = 0;
        my[3][3] = 0;
        mz[3][3] = 0;

        // Final result
        geometryMatrixX = Matrix4x4d.copyOf(mx);
        geometryMatrixY = Matrix4x4d.copyOf(my);
        geometryMatrixZ = Matrix4x4d.copyOf(mz);
        //printGeometryMatrices();
    }

    /**
    This method evaluates current patch position in the parameter space 
    position (s, t), computing the equation set 11.76 in [FOLE1992].

    The following class attributes are used:
    <UL>
      <LI> sParameterMatrix  Column vector for storing s parameter polynomial as
      explain in section [FOLE1992].11.3
      <LI> tParameterMatrix Row vector for storing t parameter polynomial
      <LI> basisMatrix  Patch's blending function
      <LI> transposedBasisMatrix M's transpose
      <LI> geometryMatrixX Geometry matrix for x
      <LI> geometryMatrixY Geometry matrix for y
      <LI> geometryMatrixZ Geometry matrix for z
    </UL>

    PRE: calculateMAtrices() should be called before calling this method.
    @param s parameter in the first direction, in [0, 1]
    @param t parameter in the second direction, in [0, 1]
    @return the point of the patch at (s, t)
    */
    public Vector3Dd evaluate(double s, double t)
    {
        sParameterMatrix = sParameterMatrix
            .withVal(0, 0, s * s * s)
            .withVal(0, 1, s * s)
            .withVal(0, 2, s)
            .withVal(0, 3, 1);

        tParameterMatrix = tParameterMatrix
            .withVal(0, 0, t * t * t)
            .withVal(1, 0, t * t)
            .withVal(2, 0, t)
            .withVal(3, 0, 1);

        Matrix4x4d S_M_Gx_Mt_MATRIX = sParameterMatrix.multiply(coefficientMatrixX);
        Matrix4x4d S_M_Gy_Mt_MATRIX = sParameterMatrix.multiply(coefficientMatrixY);
        Matrix4x4d S_M_Gz_Mt_MATRIX = sParameterMatrix.multiply(coefficientMatrixZ);
        Matrix4x4d Qx_MATRIX = S_M_Gx_Mt_MATRIX.multiply(tParameterMatrix);
        Matrix4x4d Qy_MATRIX = S_M_Gy_Mt_MATRIX.multiply(tParameterMatrix);
        Matrix4x4d Qz_MATRIX = S_M_Gz_Mt_MATRIX.multiply(tParameterMatrix);

        // The result is a 1x1 matrix.
        return new Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0));
    }

    /**
    Former output parameter form of `evaluate(double, double)`. `Vector3Dd` is
    immutable, so the given vector can not receive the point: use the returned
    one (as the TypeScript port does).
    @param p ignored
    @param s parameter in the first direction, in [0, 1]
    @param t parameter in the second direction, in [0, 1]
    @return the point of the patch at (s, t)
    @deprecated use `evaluate(double, double)`
    */
    @Deprecated
    public Vector3Dd evaluate(Vector3Dd p, double s, double t)
    {
        return evaluate(s, t);
    }

    public Vector3Dd evaluateTangent(double s, double t)
    {
        sDerivativeParameterMatrix = sDerivativeParameterMatrix
            .withVal(0, 0, 3 * s * s)
            .withVal(0, 1, 2 * s)
            .withVal(0, 2, 1)
            .withVal(0, 3, 0);
        tParameterMatrix = tParameterMatrix
            .withVal(0, 0, t * t * t)
            .withVal(1, 0, t * t)
            .withVal(2, 0, t)
            .withVal(3, 0, 1);

        Matrix4x4d S_M_Gx_Mt_MATRIX = sDerivativeParameterMatrix.multiply(coefficientMatrixX);
        Matrix4x4d S_M_Gy_Mt_MATRIX = sDerivativeParameterMatrix.multiply(coefficientMatrixY);
        Matrix4x4d S_M_Gz_Mt_MATRIX = sDerivativeParameterMatrix.multiply(coefficientMatrixZ);
        Matrix4x4d Qx_MATRIX = S_M_Gx_Mt_MATRIX.multiply(tParameterMatrix);
        Matrix4x4d Qy_MATRIX = S_M_Gy_Mt_MATRIX.multiply(tParameterMatrix);
        Matrix4x4d Qz_MATRIX = S_M_Gz_Mt_MATRIX.multiply(tParameterMatrix);

        // The result is a 1x1 matrix.
        Vector3Dd result = new Vector3Dd();

        result = new Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0));
        result = result.normalized();

        return result;
    }

    public Vector3Dd evaluateBinormal(double s, double t)
    {
        sParameterMatrix = sParameterMatrix
            .withVal(0, 0, s * s * s)
            .withVal(0, 1, s * s)
            .withVal(0, 2, s)
            .withVal(0, 3, 1);

        tDerivativeParameterMatrix = tDerivativeParameterMatrix
            .withVal(0, 0, 3 * t * t)
            .withVal(1, 0, 2 * t)
            .withVal(2, 0, 1)
            .withVal(3, 0, 0);

        Matrix4x4d S_M_Gx_Mt_MATRIX = sParameterMatrix.multiply(coefficientMatrixX);
        Matrix4x4d S_M_Gy_Mt_MATRIX = sParameterMatrix.multiply(coefficientMatrixY);
        Matrix4x4d S_M_Gz_Mt_MATRIX = sParameterMatrix.multiply(coefficientMatrixZ);
        Matrix4x4d Qx_MATRIX = S_M_Gx_Mt_MATRIX.multiply(tDerivativeParameterMatrix);
        Matrix4x4d Qy_MATRIX = S_M_Gy_Mt_MATRIX.multiply(tDerivativeParameterMatrix);
        Matrix4x4d Qz_MATRIX = S_M_Gz_Mt_MATRIX.multiply(tDerivativeParameterMatrix);

        // The results are 1x1 matrices.
        Vector3Dd result = new Vector3Dd();
        result = new Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0));
        result = result.normalized();

        return result;
    }

    /**
    This method evaluates current patch gradient in the parameter space 
    position (s, t), computing the patch normal as explain in section 
    [FOLE1992].11.3.4.

    The following class attributes are used:
    <UL>
      <LI> sParameterMatrix  Column vector for storing s parameter polynomial as
      explain in section [FOLE1992].11.3
      <LI> tParameterMatrix Row vector for storing t parameter polynomial
      <LI> basisMatrix  Patch's blending function
      <LI> transposedBasisMatrix M's transpose
      <LI> geometryMatrixX Geometry matrix for x
      <LI> geometryMatrixY Geometry matrix for y
      <LI> geometryMatrixZ Geometry matrix for z
    </UL>

    PRE: calculateMAtrices() should be called before calling this method.
    @param n
    @param s
    @param t
    */
    public Vector3Dd evaluateNormal(double s, double t)
    {
        Vector3Dd dQds;
        Vector3Dd dQdt;

        dQds = evaluateTangent(s, t);
        dQdt = evaluateBinormal(s, t);

        Vector3Dd nn = dQds.crossProduct(dQdt);
        nn = nn.normalized();

        return nn;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    \todo  implement the method
    @param r
    @return true if given ray intersects current ParametricBicubicPatch
    */
    public Ray doIntersectionFirstHit(Ray r) {
        return null;
    }

    @Override
    public boolean doIntersectionFirstHit(Ray inRay, RayHit outHit)
    {
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.

    Check the discusion in [WAYN1990] about solving this problem. Two main
    strategies are known for solving this: a numeric root finding
    (trying different values for Ray.t() until a given error tolerance is
    reached) and converting the patch to a mesh and test the mesh.

    This method implements the numerical approach, while an explicit
    convertion to a Mesh could be managed by the user/programmer directly.
    WARNING: The numerical approach is really, really slow... 

    TO DO implement the method
    */
    public void
    doExtraInformation(Ray inRay, double intT, 
                                   RayHit outData) {
        
    }

    /** 
    Returns an approximate bounding volume minmax for current patch, from
    the minmax of its contour curve.

    BUG: current contour curve asumption is not valid
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax() {
        if ( contourCurve != null ) {
            return contourCurve.getMinMax();
        }
        else {
            // This gives convex hull's minmax
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            double minMax[] = new double[6];
            int i, j;

            for ( i = 0; i < 4; i++ ) {
                for ( j = 0; j < 4; j++ ) {
                    Vector3Dd p = controlMeshPoints[i][j];

                    if ( p.x() < minX ) minX = p.x();
                    if ( p.y() < minY ) minY = p.y();
                    if ( p.z() < minZ ) minZ = p.z();
                    if ( p.x() > maxX ) maxX = p.x();
                    if ( p.y() > maxY ) maxY = p.y();
                    if ( p.z() > maxZ ) maxZ = p.z();
                }
            }
            minMax[0] = minX;
            minMax[1] = minY;
            minMax[2] = minZ;
            minMax[3] = maxX;
            minMax[4] = maxY;
            minMax[5] = maxZ;
            return minMax;
        }
    }

}
