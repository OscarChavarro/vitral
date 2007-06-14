//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
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

public class ParametricBiCubicPatch extends Geometry {
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

  public void makeGeomMatrixXYZ_Bezier() {
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

  public void makeGeomMatrixXYZ_Hermite() {
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

  public void makeGeomMatrixXYZ_Quad() {
    double[][] mx = new double[4][4];
    double[][] my = new double[4][4];
    double[][] mz = new double[4][4];

    int i = 0;
    int sum = 1;
    int p = 0;
    for (int j = 0; j < 4; j += 3) {
      //  System.out.println("pos: (" + i + "," + j + "), (" + (i + 1) + "," + j +"), (" + i + "," + (j + sum) + ")");
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
      //   System.out.println("pos: (" + i + "," + j + "), (" + (i + 1) + "," + j +"), (" + i + "," + (j + sum) + ")");
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
      //Vector3D vn = new Vector3D(0, 0, 0);
      mx[i - 1][j + sum] = vn.x;
      my[i - 1][j + sum] = vn.y;
      mz[i - 1][j + sum] = vn.z;

      p++;
      sum *= -1;
    }

    Gx_MATRIX.M = mx;
    // System.out.println("mx: " + Gx_MT.toString());
    Gy_MATRIX.M = my;
    //System.out.println("my: " + Gy_MT.toString());
    Gz_MATRIX.M = mz;
    //System.out.println("mz: " + Gz_MT.toString());

  }

  public double[][][] evaluateSurface() {
    double[][][] gridPoints = new double[approximationSteps][approximationSteps][
        3];
    Matrix4x4 modelMatrix = new Matrix4x4();
    Matrix4x4 modelMatrixTras = new Matrix4x4();
    if (this.type == ParametricCubicCurve.BEZIER) {
      this.makeGeomMatrixXYZ_Bezier();
      modelMatrix = ParametricCubicCurve.BEZIER_MATRIX;
      modelMatrixTras = ParametricCubicCurve.BEZIER_MATRIX;
    }
    else if (this.type == ParametricCubicCurve.HERMITE) {
      this.makeGeomMatrixXYZ_Hermite();
      modelMatrix = ParametricCubicCurve.HERMITE_MATRIX;
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          modelMatrixTras.M[i][j] =
              ParametricCubicCurve.HERMITE_MATRIX.M[j][i];
        }
      }
    } else if (this.type == ParametricBiCubicPatch.QUAD) {
      this.makeGeomMatrixXYZ_Quad();
      modelMatrix = ParametricCubicCurve.BEZIER_MATRIX;
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          modelMatrixTras.M[i][j] =
              ParametricCubicCurve.BEZIER_MATRIX.M[j][i];
        }
      }

    }


    Matrix4x4 pre = new Matrix4x4();
    Matrix4x4 post = new Matrix4x4();

    // In the current dimension
    for (int d = 0; d < 3; d++) { // for each dimention...
      // Here, we precompute the patch's matrix
      Matrix4x4 matrixM = new Matrix4x4();

      if (d == 0) {
        matrixM =
            modelMatrix.multiply(Gx_MATRIX).multiply(modelMatrixTras);
      }
      else if (d == 1) {
        matrixM =
            modelMatrix.multiply(Gy_MATRIX).multiply(modelMatrixTras);
      }
      else {
        matrixM = modelMatrix.multiply(Gz_MATRIX).multiply(modelMatrixTras);
      }

      for (int i = 0; i < approximationSteps; i++) {
        double u = (double) i / (approximationSteps - 1);

        pre.M[0][0] = u * u * u;
        pre.M[0][1] = u * u;
        pre.M[0][2] = u;
        pre.M[0][3] = 1;

        Matrix4x4 tmp = pre.multiply(matrixM);

        for (int j = 0; j < approximationSteps; j++) {
          double v = (double) j / (approximationSteps - 1);

          post.M[0][0] = v * v * v;
          post.M[1][0] = v * v;
          post.M[2][0] = v;
          post.M[3][0] = 1;

          Matrix4x4 tmp2 = tmp.multiply(post);
          // The result is a 1x1 matrix.
          gridPoints[i][j][d] = tmp2.M[0][0];
        }
      }
    }

    return gridPoints;
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
