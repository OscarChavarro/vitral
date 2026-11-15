package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/** Coefficient order: xx, yy, zz, xy, xz, yz, x, y, z, constant. */
public class CsgExactQuadric {
    public static boolean world(double[] local, Matrix4x4d forward, double[] result) {
        if (local == null || result == null) return false;
        for (int i=0;i<10;++i) if (!Double.isFinite(local[i])) return false;
        double[][] inverse = new double[4][4]; if (!CsgExactTransform.inverseNumerator(forward,inverse)) return false;
        double[][] q={{local[0],0,0,0},{0,local[1],0,0},{0,0,local[2],0},{0,0,0,local[9]}};
        final int[] row={0,0,1,0,1,2}, col={1,2,2,3,3,3};
        for (int k=0;k<6;++k) {
            final double half=local[3+k]*0.5;
            if (!CsgExactArithmetic.equalProducts(local[3+k],0.5,half,1)) return false;
            q[row[k]][col[k]]=q[col[k]][row[k]]=half;
        }
        double[][] intermediate = new double[4][4], world = new double[4][4];
        for (int i=0;i<4;++i) for (int j=0;j<4;++j) {
            final double[] column={inverse[0][j],inverse[1][j],inverse[2][j],inverse[3][j]};
            if (!CsgExactTransform.exactSum(q[i],column,4,intermediate[i],j)) return false;
        }
        for (int i=0;i<4;++i) for (int j=0;j<4;++j) {
            final double[] column={inverse[0][i],inverse[1][i],inverse[2][i],inverse[3][i]};
            final double[] values={intermediate[0][j],intermediate[1][j],intermediate[2][j],intermediate[3][j]};
            if (!CsgExactTransform.exactSum(column,values,4,world[i],j)) return false;
        }
        double[] candidate={world[0][0],world[1][1],world[2][2],0,0,0,0,0,0,world[3][3]};
        for (int k=0;k<6;++k) {
            candidate[3+k]=world[row[k]][col[k]]*2;
            if (!CsgExactArithmetic.equalProducts(world[row[k]][col[k]],2,candidate[3+k],1)) return false;
        }
        for (int i=0;i<10;++i) result[i]=candidate[i];
        return true;
    }
}
