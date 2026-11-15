package vsdk.toolkit.environment.geometry.volume;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.numerics.FloatingInterval;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.numericalAnalysis.polynomial.PolynomialSolver;
import vsdk.toolkit.numericalAnalysis.polynomial.QuadraticSolver;
import vsdk.toolkit.numericalAnalysis.polynomial.QuarticSolver;

/**
Implicit solid F(x,y,z) < 0 of a polynomial of order 2 to 7 in x, y and z.
Coefficients are stored in the PovRay term order: decreasing powers of x,
then of y, then of z, so a quartic's 35 terms run from x^4 (index 0) down to
the constant (index 34). getTermIndex and getTermPowers convert between term
indices and powers. Ray roots come from the Sturm-sequence solver, or for
quartics from the closed-form solver unless Sturm sequences are requested.
The surface is generally unbounded; bound it by composition when needed.
*/
public class PolynomialSurface extends ImplicitSolid {
    public static final int MINIMUM_ORDER = 2;

    public static final int MAXIMUM_ORDER = 7;

    public static final int MAXIMUM_TERMS = 120;

    // Root solver tolerance of PovRay polynomial shapes.
    public static final double SOLVER_EPSILON = 1.0e-10;

    // PovRay drops expanded ray coefficients of magnitude below this value.
    public static final double POVRAY_EXPANSION_CUTOFF = 1.0e-4;

    private static final double COEFFICIENT_LIMIT = 1.0e-20;

    private static final int[] TERM_COUNTS = {1, 4, 10, 20, 35, 56, 84, 120};

    private static final int[][] BINOMIAL = {{0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 3, 3, 1, 0, 0, 0, 0, 0, 0, 0}, {0, 1, 4, 6, 4, 1, 0, 0, 0, 0, 0, 0},
        {0, 1, 5, 10, 10, 5, 1, 0, 0, 0, 0, 0},
        {0, 1, 6, 15, 20, 15, 6, 1, 0, 0, 0, 0},
        {0, 1, 7, 21, 35, 35, 21, 7, 1, 0, 0, 0},
        {0, 1, 8, 28, 56, 70, 56, 28, 8, 1, 0, 0},
        {0, 1, 9, 36, 84, 126, 126, 84, 36, 9, 1, 0},
        {0, 1, 10, 45, 120, 210, 252, 210, 120, 45, 10, 1}};

    private static final int[] FACTORIALS = {1, 1, 2, 6, 24, 120, 720, 5040};

    private int order;

    private boolean sturm;

    private double[] coefficients = new double[MAXIMUM_TERMS];

    private static int rollTerm(int order, int x, int y, int z) {
    final int xstart = BINOMIAL[order - x + 2][order - x];
    order = order - x;
    final int ystart = BINOMIAL[order - y + 1][order - y];
    order = order - y;
    final int zstart = BINOMIAL[order - z][order - z];
    return xstart + ystart + zstart;
}

    private static void transform(int order, double[] coeffs, Matrix4x4d[] q, double cutoff) {
    int termIndex;
    int partialIndex;
    int[] ip = {0};
    int i;
    int[] i0 = {0};
    int[] i1 = {0};
    int[] i2 = {0};
    int[] i3 = {0};
    int[] jp = {0};
    int j;
    int[] j0 = {0};
    int[] j1 = {0};
    int[] j2 = {0};
    int[] j3 = {0};
    int[] kp = {0};
    int k;
    int[] k0 = {0};
    int[] k1 = {0};
    int[] k2 = {0};
    int[] k3 = {0};
    int[] wp = {0};
    double[] b = new double[MAXIMUM_TERMS];
    double term;
    double tempx;
    double tempy;
    double tempz;

    for (i = 0; i < 4; i++) {
        for (j = 0; j < 4; j++) {
            if (q[0].get(i, j) > -COEFFICIENT_LIMIT && q[0].get(i, j) < COEFFICIENT_LIMIT) {
                q[0] = q[0].withVal(i, j, 0.0);
            }
        }
    }

    for (i = 0; i < TERM_COUNTS[order]; i++) {
        b[i] = 0.0;
    }
    for (termIndex = 0; termIndex < TERM_COUNTS[order]; termIndex++) {
        if (coeffs[termIndex] != 0.0) {
            getTermPowers(order, termIndex, ip, jp, kp, wp);
            // Step through terms in: (q[0][0]*x+q[0][1]*y+q[0][2]*z+q[0][3])^i
            for (i = 0; i < TERM_COUNTS[ip[0]]; i++) {
                getTermPowers(ip[0], i, i0, i1, i2, i3);
                tempx =
                    partialTerm(q[0], 0, ip[0], i0[0], i1[0], i2[0], i3[0]);
                if (tempx != 0.0) {

                    // Step through terms in:
                    // (q[1][0]*x+q[1][1]*y+q[1][2]*z+q[1][3])^j
                    for (j = 0; j < TERM_COUNTS[jp[0]]; j++) {
                        getTermPowers(jp[0], j, j0, j1, j2, j3);
                        tempy = partialTerm(q[0], 1, jp[0], j0[0], j1[0], j2[0], j3[0]);
                        if (tempy != 0.0) {

                            // Step through terms in:
                            // (q[2][0]*x+q[2][1]*y+q[2][2]*z+q[2][3])^k
                            for (k = 0; k < TERM_COUNTS[kp[0]]; k++) {
                                getTermPowers(kp[0], k, k0, k1, k2, k3);
                                tempz = partialTerm(q[0], 2, kp[0], k0[0], k1[0], k2[0], k3[0]);
                                if (tempz != 0.0) {
                                    // Figure out it's index, and add into
                                    // result
                                    partialIndex = rollTerm(order,
                                        i0[0] + j0[0] + k0[0], i1[0] + j1[0] + k1[0],
                                        i2[0] + j2[0] + k2[0]);
                                    term = coeffs[termIndex] * tempx *
                                                  tempy * tempz;
                                    b[partialIndex] += term;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    for (i = 0; i < TERM_COUNTS[order]; i++) {
        if (b[i] > -cutoff && b[i] < cutoff) {
            coeffs[i] = 0.0;
        } else {
            coeffs[i] = b[i];
        }
    }
}

    private static double partialTerm(Matrix4x4d q, int row, int pwr, int i, int j, int k, int l) {
    double result = (double)(FACTORIALS[pwr] / (FACTORIALS[i] * FACTORIALS[j] *
        FACTORIALS[k] * FACTORIALS[l]));
    for (int n = 0; n < i; n++) result *= q.get(0, row);
    for (int n = 0; n < j; n++) result *= q.get(1, row);
    for (int n = 0; n < k; n++) result *= q.get(2, row);
    for (int n = 0; n < l; n++) result *= q.get(3, row);
    return result;
}

    private int intersectGeneral(Vector3Dd origin, Vector3Dd direction, double minimumDistance, double[] depths, double cutoff) {
    double[] t = new double[MAXIMUM_ORDER + 1];
    int i;
    int j;
    getRayPolynomial(origin, direction, t, cutoff);
    for (i = 0; i <= order; i++) {
        if (t[i] > -COEFFICIENT_LIMIT && t[i] < COEFFICIENT_LIMIT) {
            t[i] = 0.0;
        }
    }
    for (i = 0, j = order; i <= order; i++) {
        if (t[i] != 0.0) {
            break;
        }
        j -= 1;
    }
    if (j > 2) {
        PolynomialSolver polynomialSolver = new PolynomialSolver(j, t, i);
        return polynomialSolver.solve(depths, minimumDistance, SOLVER_EPSILON);
    }
    // The three lowest coefficients; leading entries are zero when j < 2.
    if (j > 0) {
        return QuadraticSolver.solve(t, order - 2, depths);
    }
    return 0;
}

    private int intersectQuartic(Vector3Dd origin, Vector3Dd direction, double minimumDistance, double[] depths) {
    double[] t = new double[5];
    getQuarticRayPolynomial(origin, direction, t);
    if (sturm) {
        if (t[0] == 0.0) {
            if (t[1] == 0.0) {
                return QuadraticSolver.solve(t, 2, depths);
            }
            PolynomialSolver cubicSolver = new PolynomialSolver(3, t, 1);
            return cubicSolver.solve(depths, minimumDistance, SOLVER_EPSILON);
        }
        PolynomialSolver quarticSolver = new PolynomialSolver(4, t);
        return quarticSolver.solve(depths, minimumDistance, SOLVER_EPSILON);
    }
    return QuarticSolver.solve(t, depths, minimumDistance, SOLVER_EPSILON);
}

    private Vector3Dd quarticGradient(Vector3Dd point) {
    final double[] a = coefficients;
    final double x = point.x();
    final double y = point.y();
    final double z = point.z();
    final double x2 = x * x;
    final double y2 = y * y;
    final double z2 = z * z;
    final double x3 = x * x2;
    final double y3 = y * y2;
    final double z3 = z * z2;
    final double nx =
        4 * a[0] * x3 + 3 * x2 * (a[1] * y + a[2] * z + a[3]) +
        2 * x *
            (a[4] * y2 + y * (a[5] * z + a[6]) + a[7] * z2 + a[8] * z + a[9]) +
        a[10] * y3 + y2 * (a[11] * z + a[12]) +
        y * (a[13] * z2 + a[14] * z + a[15]) + a[16] * z3 + a[17] * z2 +
        a[18] * z + a[19];

    final double ny = a[1] * x3 + x2 * (2 * a[4] * y + a[5] * z + a[6]) +
                x * (3 * a[10] * y2 + 2 * y * (a[11] * z + a[12]) + a[13] * z2 +
                        a[14] * z + a[15]) +
                4 * a[20] * y3 + 3 * y2 * (a[21] * z + a[22]) +
                2 * y * (a[23] * z2 + a[24] * z + a[25]) + a[26] * z3 +
                a[27] * z2 + a[28] * z + a[29];

    final double nz = a[2] * x3 + x2 * (a[5] * y + 2 * a[7] * z + a[8]) +
                x * (a[11] * y2 + y * (2 * a[13] * z + a[14]) + 3 * a[16] * z2 +
                        2 * a[17] * z + a[18]) +
                a[21] * y3 + y2 * (2 * a[23] * z + a[24]) +
                y * (3 * a[26] * z2 + 2 * a[27] * z + a[28]) + 4 * a[30] * z3 +
                3 * a[31] * z2 + 2 * a[32] * z + a[33];
    return new Vector3Dd(nx, ny, nz);
}

    private static FloatingInterval power(FloatingInterval value, int exponent) {
    if (exponent == 0) return FloatingInterval.exact(1.0);
    if (exponent == 1) return value;
    if (exponent % 2 == 0) return power(value.square(), exponent / 2);
    return value.multiply(power(value, exponent - 1));
}

    // All coefficients start at zero. Throws java.IllegalArgumentException
    // for an order outside [MINIMUM_ORDER, MAXIMUM_ORDER].
    public PolynomialSurface(int initialOrder, boolean useSturmSequences) {
        this.order = initialOrder;
        this.sturm = useSturmSequences;
        this.coefficients = new double[MAXIMUM_TERMS];
    if (!isValidOrder(initialOrder))
        throw new IllegalArgumentException("PolynomialSurface order must be in [2, 7]");
}

    /** cpp copy constructor. */
    public PolynomialSurface(PolynomialSurface other) {
        this.order = other.order;
        this.sturm = other.sturm;
        this.coefficients = other.coefficients.clone();
    }

    public static boolean isValidOrder(int value) {
    return value >= MINIMUM_ORDER && value <= MAXIMUM_ORDER;
}

    public static int getTermCount(int value) {
    return value >= 0 && value <= MAXIMUM_ORDER ? TERM_COUNTS[value] : 0;
}

    // Term index of x^xPower y^yPower z^zPower, or -1 when the powers are
    // negative or add up to more than order.
    public static int getTermIndex(int value, int x, int y, int z) {
    if (value < 0 || value > MAXIMUM_ORDER || x < 0 || y < 0 || z < 0 || x + y + z > value)
        return -1;
    return rollTerm(value, x, y, z);
}

    // Powers of term index; constantPower is order minus the other three.
    public static void getTermPowers(int order, int index, int[] x, int[] y, int[] z, int[] w) {
    int i;
    int torder;
    if (index == 0) {
        x[0] = order; y[0] = 0; z[0] = 0; w[0] = 0;
        return;
    }
    if (order == 1) {
        if (index == 1) {
            x[0] = 0; y[0] = 1; z[0] = 0; w[0] = 0;
            return;
        }
        x[0] = 0; y[0] = 0; z[0] = 3 - index;
        w[0] = order - (x[0] + y[0] + z[0]);
        return;
    }
    for (i = 0; BINOMIAL[3 + i][i + 1] <= index; i++) {
    }
    torder = order;
    x[0] = torder - i;
    index -= BINOMIAL[2 + i][i];
    torder = i;
    if (index == 0) {
        y[0] = torder; z[0] = 0; w[0] = 0;
        return;
    }
    if (torder == 1) {
        y[0] = 0; z[0] = 2 - index;
        w[0] = order - (x[0] + y[0] + z[0]);
        return;
    }
    for (i = 0; BINOMIAL[2 + i][i + 1] <= index; i++) {
    }
    y[0] = torder - i;
    index -= BINOMIAL[1 + i][i];
    torder = i;
    z[0] = torder - index;
    w[0] = order - (x[0] + y[0] + z[0]);
}

    public int getOrder() { return order; }

    public boolean usesSturmSequences() { return sturm; }

    public double[] getCoefficients() { return coefficients; }

    // PovRay ray roots: distances d >= minimumDistance along the unit direction
    // where the surface is crossed, in solver order; returns their count (at
    // most order). Orders other than 4 drop expanded coefficients below
    // POVRAY_EXPANSION_CUTOFF; the typed queries keep them.
    public int intersect(Vector3Dd origin, Vector3Dd unitDirection, double minimumDistance, double[] depths) {
    if (order == 4) return intersectQuartic(origin, unitDirection, minimumDistance, depths);
    return intersectGeneral(origin, unitDirection, minimumDistance, depths, POVRAY_EXPANSION_CUTOFF);
}

    // Coefficients of t^order down to t^0 of F(origin + t*direction), by
    // general term expansion, zeroing those below cutoff in magnitude; the
    // quartic solver path uses its own expansion.
    public void getRayPolynomial(Vector3Dd origin, Vector3Dd direction, double[] result, double cutoff) {
    double[] a = new double[MAXIMUM_TERMS];
    for (int i = 0; i < TERM_COUNTS[order]; i++) a[i] = coefficients[i];
    Matrix4x4d q = Matrix4x4d.identityMatrix().multiply(0.0);
    q = q.withVal(0, 0, direction.x());
    q = q.withVal(3, 0, origin.x());
    q = q.withVal(0, 1, direction.y());
    q = q.withVal(3, 1, origin.y());
    q = q.withVal(0, 2, direction.z());
    q = q.withVal(3, 2, origin.z());
    Matrix4x4d[] qH = {q};
    transform(order, a, qH, cutoff);
    // The polynomial is now in x alone: x^(order-i) holds the t^(order-i) term.
    for (int i = 0; i <= order; i++) result[i] = a[BINOMIAL[3 + i][4] - 1];
}

    public void getQuarticRayPolynomial(Vector3Dd origin, Vector3Dd direction, double[] result) {
    final double[] a = coefficients;
    final double x = origin.x();
    final double y = origin.y();
    final double z = origin.z();
    final double xx = direction.x();
    final double yy = direction.y();
    final double zz = direction.z();
    final double x2 = x * x;
    final double y2 = y * y;
    final double z2 = z * z;
    final double x3 = x * x2;
    final double y3 = y * y2;
    final double z3 = z * z2;
    final double x4 = x * x3;
    final double y4 = y * y3;
    final double z4 = z * z3;
    final double xx2 = xx * xx;
    final double yy2 = yy * yy;
    final double zz2 = zz * zz;
    final double xx3 = xx * xx2;
    final double yy3 = yy * yy2;
    final double zz3 = zz * zz2;
    final double xx4 = xx * xx3;
    final double yy4 = yy * yy3;
    final double zz4 = zz * zz3;
    final double xZ = x * z;
    final double xZz = x * zz;
    final double xxZ = xx * z;
    final double xxZz = xx * zz;
    final double xY = x * y;
    final double xYy = x * yy;
    final double xxY = xx * y;
    final double xxYy = xx * yy;
    final double yZ = y * z;
    final double yZz = y * zz;
    final double yyZ = yy * z;
    final double yyZz = yy * zz;

    /**
    Determine the coefficients of t^n, where the line is represented
    as (x,y,z) + (xx,yy,zz)*t.
    */
    double temp = a[0] * xx4;
    temp += a[1] * xx3 * yy;
    temp += a[2] * xx3 * zz;
    temp += a[4] * xx2 * yy2;
    temp += a[5] * xx2 * yyZz;
    temp += a[7] * xx2 * zz2;
    temp += a[10] * xx * yy3;
    temp += a[11] * xxZz * yy2;
    temp += a[13] * xxYy * zz2;
    temp += a[16] * xx * zz3;
    temp += a[20] * yy4;
    temp += a[21] * yy3 * zz;
    temp += a[23] * yy2 * zz2;
    temp += a[26] * yy * zz3;
    temp += a[30] * zz4;

    result[0] = temp;

    temp = 4 * a[0] * x * xx3;
    temp += a[1] * (3 * xx2 * xYy + xx3 * y);
    temp += a[2] * (3 * xx2 * xZz + xx3 * z);
    temp += a[3] * xx3;
    temp += a[4] * (2 * x * xx * yy2 + 2 * xx2 * y * yy);
    temp += a[5] * (xx2 * (yZz + yyZ) + 2 * xYy * xxZz);
    temp += a[6] * xx2 * yy;
    temp += a[7] * (2 * x * xx * zz2 + 2 * xx2 * z * zz);
    temp += a[8] * xx2 * zz;
    temp += a[10] * (x * yy3 + 3 * xxY * yy2);
    temp += a[11] * (xx * (2 * y * yyZz + yy2 * z) + xZz * yy2);
    temp += a[12] * xx * yy2;
    temp += a[13] * (xx * (y * zz2 + 2 * yyZ * zz) + xYy * zz2);
    temp += a[14] * xxYy * zz;
    temp += a[16] * (x * zz3 + 3 * xxZ * zz2);
    temp += a[17] * xx * zz2;
    temp += 4 * a[20] * y * yy3;
    temp += a[21] * (3 * yy2 * yZz + yy3 * z);
    temp += a[22] * yy3;
    temp += zz * (2 * a[23] * yy * (yZz + yyZ) + a[24] * yy2 +
                     zz * (a[26] * (yZz + 3 * yyZ) + a[27] * yy +
                              zz * (4 * a[30] * z + a[31])));
    result[1] = temp;

    temp = 6 * a[0] * x2 * xx2;
    temp += 3 * a[1] * x * xx * (xYy + xxY);
    temp += 3 * a[2] * x * xx * (xZz + xxZ);
    temp += 3 * a[3] * x * xx2;
    temp += a[4] * (x2 * yy2 + 4 * xYy * xxY + xx2 * y2);
    temp += a[5] * (x2 * yyZz + 2 * x * xx * (yZz + yyZ) + xx2 * yZ);
    temp += a[6] * (2 * x * xxYy + xx2 * y);
    temp += a[7] * (x2 * zz2 + 4 * xZz * xxZ + xx2 * z2);
    temp += a[8] * (2 * x * xxZz + xx2 * z);
    temp += a[9] * xx2;
    temp += a[10] * (3 * xY * yy2 + 3 * xxYy * y2);
    temp += a[11] * (xYy * (2 * yZz + yyZ) + xx * (y2 * zz + 2 * y * yyZ));
    temp += a[12] * (x * yy2 + 2 * xxY * yy);
    temp += a[13] * (xZz * (yZz + 2 * yyZ) + xx * (2 * yZ * zz + yy * z2));
    temp += a[14] * (xYy * zz + xx * (yZz + yyZ));
    temp += a[15] * xxYy;
    temp += a[16] * (3 * xZ * zz2 + 3 * xxZz * z2);
    temp += a[17] * (x * zz2 + 2 * xxZ * zz);
    temp += a[18] * xxZz;
    temp += 6 * a[20] * y2 * yy2;
    temp += 3 * a[21] * y * yy * (yZz + yyZ);
    temp += 3 * a[22] * y * yy2;
    temp += a[23] * (y2 * zz2 + 4 * yZz * yyZ + yy2 * z2);
    temp += a[24] * (2 * y * yyZz + yy2 * z);
    temp += a[25] * yy2;
    temp += zz * (3 * a[26] * z * (yZz + yyZ) + a[27] * (yZz + 2 * yyZ) +
                     a[28] * yy + 6 * a[30] * z2 * zz + 3 * a[31] * z * zz +
                     a[32] * zz);
    result[2] = temp;

    temp = 4 * a[0] * x3 * xx;
    temp += a[1] * x2 * (xYy + 3 * xxY);
    temp += a[2] * x2 * (xZz + 3 * xxZ);
    temp += 3 * a[3] * x2 * xx;
    temp += 2 * a[4] * xY * (xYy + xxY);
    temp += a[5] * x * (x * (yZz + yyZ) + 2 * xxY * z);
    temp += a[6] * x * (xYy + 2 * xxY);
    temp += 2 * a[7] * xZ * (xZz + xxZ);
    temp += a[8] * x * (xZz + 2 * xxZ);
    temp += 2 * a[9] * x * xx;
    temp += a[10] * (3 * xYy * y2 + xx * y3);
    temp += a[11] * (xY * (yZz + 2 * yyZ) + xxZ * y2);
    temp += a[12] * (2 * xY * yy + xx * y2);
    temp += a[13] * (xZ * (2 * yZz + yyZ) + xxY * z2);
    temp += a[14] * (x * (yZz + yyZ) + xxY * z);
    temp += a[15] * (xYy + xxY);
    temp += a[16] * (3 * xZz * z2 + xx * z3);
    temp += a[17] * (2 * xZ * zz + xx * z2);
    temp += a[18] * (xZz + xxZ);
    temp += a[19] * xx;
    temp += 4 * a[20] * y3 * yy;
    temp += a[21] * y2 * (yZz + 3 * yyZ);
    temp += 3 * a[22] * y2 * yy;
    temp += 2 * a[23] * yZ * (yZz + yyZ);
    temp += a[24] * y * (yZz + 2 * yyZ);
    temp += 2 * a[25] * y * yy;
    temp += a[26] * (3 * yZz * z2 + yy * z3);
    temp += a[27] * (2 * yZ * zz + yy * z2);
    temp += a[28] * (yZz + yyZ);
    temp += a[29] * yy;
    temp += zz * (4 * a[30] * z3 + 3 * a[31] * z2 + 2 * a[32] * z + a[33]);
    result[3] = temp;

    temp = a[0] * x4;
    temp += a[1] * x3 * y;
    temp += a[2] * x3 * z;
    temp += a[3] * x3;
    temp += a[4] * x2 * y2;
    temp += a[5] * x2 * yZ;
    temp += a[6] * x2 * y;
    temp += a[7] * x2 * z2;
    temp += a[8] * x2 * z;
    temp += a[9] * x2;
    temp += a[10] * x * y3;
    temp += a[11] * x * y2 * z;
    temp += a[12] * x * y2;
    temp += a[13] * xY * z2;
    temp += a[14] * xY * z;
    temp += a[15] * xY;
    temp += a[16] * x * z3;
    temp += a[17] * x * z2;
    temp += a[18] * xZ;
    temp += a[19] * x;
    temp += a[20] * y4;
    temp += a[21] * y3 * z;
    temp += a[22] * y3;
    temp += a[23] * y2 * z2;
    temp += a[24] * y2 * z;
    temp += a[25] * y2;
    temp += a[26] * y * z3;
    temp += a[27] * y * z2;
    temp += a[28] * yZ;
    temp += a[29] * y;
    temp += a[30] * z4;
    temp += a[31] * z3;
    temp += a[32] * z2;
    temp += a[33] * z;
    temp += a[34];
    result[4] = temp;
}

    public double evaluate(Vector3Dd point) {
    double[] x = new double[MAXIMUM_ORDER + 1];
    double[] y = new double[MAXIMUM_ORDER + 1];
    double[] z = new double[MAXIMUM_ORDER + 1];
    int[] k0 = {0}, k1 = {0}, k2 = {0}, k3 = {0};
    x[0] = 1.0;
    y[0] = 1.0;
    z[0] = 1.0;
    x[1] = point.x();
    y[1] = point.y();
    z[1] = point.z();
    for (int i = 2; i <= MAXIMUM_ORDER; i++) {
        x[i] = x[1] * x[i - 1];
        y[i] = y[1] * y[i - 1];
        z[i] = z[1] * z[i - 1];
    }
    double result = 0.0;
    for (int i = 0; i < TERM_COUNTS[order]; i++) {
        getTermPowers(order, i, k0, k1, k2, k3);
        result += coefficients[i] * x[k0[0]] * y[k1[0]] * z[k2[0]];
    }
    return result;
}

    public Vector3Dd gradient(Vector3Dd point) {
    if (order == 4) return quarticGradient(point);
    int[] xp = {0}, yp = {0}, zp = {0}, wp = {0};
    double[] x = new double[MAXIMUM_ORDER + 1];
    double[] y = new double[MAXIMUM_ORDER + 1];
    double[] z = new double[MAXIMUM_ORDER + 1];
    x[0] = 1.0;
    y[0] = 1.0;
    z[0] = 1.0;
    x[1] = point.x();
    y[1] = point.y();
    z[1] = point.z();
    for (int i = 2; i <= order; i++) {
        x[i] = point.x() * x[i - 1];
        y[i] = point.y() * y[i - 1];
        z[i] = point.z() * z[i - 1];
    }
    final double[] a = coefficients;
    double rx = 0.0;
    double ry = 0.0;
    double rz = 0.0;
    for (int i = 0; i < TERM_COUNTS[order]; i++) {
        getTermPowers(order, i, xp, yp, zp, wp);
        if (xp[0] >= 1) {
            rx += xp[0] * a[i] * x[xp[0] - 1] * y[yp[0]] * z[zp[0]];
        }
        if (yp[0] >= 1) {
            ry += yp[0] * a[i] * x[xp[0]] * y[yp[0] - 1] * z[zp[0]];
        }
        if (zp[0] >= 1) {
            rz += zp[0] * a[i] * x[xp[0]] * y[yp[0]] * z[zp[0] - 1];
        }
    }
    return new Vector3Dd(rx, ry, rz);
}

    @Override
    public Geometry copyGeometry() { return new PolynomialSurface(this); }

    @Override
    public double evaluateField(Vector3Dd point) { return evaluate(point); }

    @Override
    public Vector3Dd evaluateFieldGradient(Vector3Dd point) { return gradient(point); }

    @Override
    public FloatingInterval encloseField(FloatingInterval x, FloatingInterval y, FloatingInterval z) {
    if (!x.isValid() || !y.isValid() || !z.isValid()) return new FloatingInterval();
    FloatingInterval result = FloatingInterval.exact(0.0);
    int[] xp = {0}, yp = {0}, zp = {0}, wp = {0};
    for (int i = 0; i < TERM_COUNTS[order]; i++) {
        if (coefficients[i] == 0.0) continue;
        if (!Double.isFinite(coefficients[i])) return new FloatingInterval();
        getTermPowers(order, i, xp, yp, zp, wp);
        result = result.add(FloatingInterval.exact(coefficients[i]).multiply(
            power(x, xp[0]).multiply(power(y, yp[0])).multiply(power(z, zp[0]))));
    }
    return result;
}

    @Override
    public boolean encloseFieldGradient(FloatingInterval x, FloatingInterval y, FloatingInterval z, FloatingInterval[] gx, FloatingInterval[] gy, FloatingInterval[] gz) {
    if (!x.isValid() || !y.isValid() || !z.isValid()) return false;
    gx[0] = gy[0] = gz[0] = FloatingInterval.exact(0.0);
    int[] xp = {0}, yp = {0}, zp = {0}, wp = {0};
    for (int i = 0; i < TERM_COUNTS[order]; i++) {
        if (coefficients[i] == 0.0) continue;
        if (!Double.isFinite(coefficients[i])) return false;
        getTermPowers(order, i, xp, yp, zp, wp);
        final FloatingInterval c = FloatingInterval.exact(coefficients[i]);
        if (xp[0] > 0) gx[0] = gx[0].add(c.multiply(FloatingInterval.exact(xp[0])).multiply(
            power(x, xp[0] - 1).multiply(power(y, yp[0])).multiply(power(z, zp[0]))));
        if (yp[0] > 0) gy[0] = gy[0].add(c.multiply(FloatingInterval.exact(yp[0])).multiply(
            power(x, xp[0]).multiply(power(y, yp[0] - 1)).multiply(power(z, zp[0]))));
        if (zp[0] > 0) gz[0] = gz[0].add(c.multiply(FloatingInterval.exact(zp[0])).multiply(
            power(x, xp[0]).multiply(power(y, yp[0])).multiply(power(z, zp[0] - 1))));
    }
    return gx[0].isValid() && gy[0].isValid() && gz[0].isValid();
}

    @Override
    public boolean findRayRoots(Vector3Dd origin, Vector3Dd unitDirection, double maximumDistance, ArrayList<Double> distances) {
    for (int i = 0; i < TERM_COUNTS[order]; i++)
        if (!Double.isFinite(coefficients[i])) return false;
    double[] depths = new double[MAXIMUM_ORDER];
    // The typed queries keep every expanded coefficient.
    final int count = order == 4 ? intersectQuartic(origin, unitDirection, 0.0, depths) :
        intersectGeneral(origin, unitDirection, 0.0, depths, 0.0);
    for (int i = 0; i < count; i++) distances.add(depths[i]);
    return true;
}

    @Override
    public void getBounds(GeometryBounds result) {
    for (int i = 0; i < TERM_COUNTS[order]; i++)
        if (!Double.isFinite(coefficients[i])) { result.setIndeterminate(); return; }
    result.setUnbounded();
}
}
