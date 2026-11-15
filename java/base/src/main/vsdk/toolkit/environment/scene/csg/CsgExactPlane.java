package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/**
Exact sufficient conversion of a local oriented plane to world coefficients.
Returns false when any binary64 intermediate is not exactly representable.
*/
public class CsgExactPlane {
    public static boolean world(double[] local, Matrix4x4d forward, double[] result) {
        if (local == null || result == null) return false;
        for (int i=0;i<4;++i) if (!Double.isFinite(local[i])) return false;
        if (local[0]==0 && local[1]==0 && local[2]==0) return false;
        double[][] inverse = new double[4][4];
        if (!CsgExactTransform.inverseNumerator(forward,inverse)) return false;
        double[] candidate = new double[4];
        for (int i=0;i<4;++i) {
            final double[] column={inverse[0][i],inverse[1][i],inverse[2][i],inverse[3][i]};
            if (!CsgExactTransform.exactSum(column,local,4,candidate,i)) return false;
        }
        final double sign=inverse[3][3]<0 ? -1 : 1;
        for (int i=0;i<4;++i) result[i]=sign*candidate[i];
        return true;
    }
}
