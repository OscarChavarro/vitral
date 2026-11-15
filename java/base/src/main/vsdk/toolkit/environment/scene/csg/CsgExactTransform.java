package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/** Homogeneous inverse numerator det(A) * [A^-1, -A^-1 t; 0, 1]. */
public class CsgExactTransform {
    /** `result` is a one-element output holder. */
    public static boolean exactSum(double[] a, double[] b, int count, double[] result) {
        if (a == null || b == null || count<=0 || count>4) return false;
        double candidate=0;
        for (int i=0;i<count;++i) candidate+=a[i]*b[i];
        if (!Double.isFinite(candidate)) return false;
        final double[] c={candidate,0,0,0}, d={1,0,0,0};
        if (!CsgExactArithmetic.equalSumProducts(a,b,c,d,count)) return false;
        result[0]=candidate;
        return true;
    }

    /** cpp `exactSum(a, b, count, row[k])`: the output is `row[k]`. */
    static boolean exactSum(double[] a, double[] b, int count, double[] row, int k) {
        double[] value = {row[k]};
        final boolean done = exactSum(a, b, count, value);
        row[k] = value[0];
        return done;
    }

    /** `result` is a caller-allocated 4 x 4 array. */
    public static boolean inverseNumerator(Matrix4x4d forward, double[][] result) {
        if (result == null || forward.get(3,0)!=0 || forward.get(3,1)!=0 || forward.get(3,2)!=0 || forward.get(3,3)!=1) return false;
        double[][] cofactor = new double[3][3];
        for (int i=0;i<3;++i) for (int j=0;j<3;++j) {
            final int r=(i+1)%3,s=(i+2)%3,c=(j+1)%3,d=(j+2)%3;
            final double[] a={forward.get(r,c),-forward.get(r,d)};
            final double[] b={forward.get(s,d),forward.get(s,c)};
            if (!exactSum(a,b,2,cofactor[i],j)) return false;
        }
        final double[] detTerms={forward.get(0,0),forward.get(0,1),forward.get(0,2)};
        double[] determinant={0};
        if (!exactSum(detTerms,cofactor[0],3,determinant) || determinant[0]==0) return false;
        double[][] candidate={{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0}};
        for (int i=0;i<3;++i) {
            for (int j=0;j<3;++j) candidate[i][j]=cofactor[j][i];
            final double[] translation={-forward.get(0,3),-forward.get(1,3),-forward.get(2,3)};
            if (!exactSum(candidate[i],translation,3,candidate[i],3)) return false;
        }
        candidate[3][3]=determinant[0];
        for (int i=0;i<4;++i) for (int j=0;j<4;++j) result[i][j]=candidate[i][j];
        return true;
    }
}
