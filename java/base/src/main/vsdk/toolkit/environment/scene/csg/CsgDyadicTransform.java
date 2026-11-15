package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/** Exact homogeneous inverse numerator; no intermediate binary64 rounding. */
public class CsgDyadicTransform {
    public static boolean dot(CsgDyadic[] a, CsgDyadic[] b, int count, CsgDyadic result) {
        if (a == null || b == null || count<=0 || count>4) return false;
        CsgDyadic sum = new CsgDyadic();
        for (int i=0;i<count;++i) {
            CsgDyadic product = new CsgDyadic(), next = new CsgDyadic();
            if (!CsgDyadic.multiply(a[i],b[i],product) || !CsgDyadic.add(sum,product,next)) return false;
            sum.assign(next);
        }
        result.assign(sum); return true;
    }

    /** `result` is a caller-allocated 4 x 4 array of numbers. */
    public static boolean inverseNumerator(Matrix4x4d forward, CsgDyadic[][] result) {
        if (result == null || forward.get(3,0)!=0 || forward.get(3,1)!=0 || forward.get(3,2)!=0 || forward.get(3,3)!=1) return false;
        CsgDyadic[][] matrix = {CsgDyadic.newArray(3), CsgDyadic.newArray(3), CsgDyadic.newArray(3)};
        CsgDyadic[] translation = CsgDyadic.newArray(3);
        CsgDyadic[][] cofactors = {CsgDyadic.newArray(3), CsgDyadic.newArray(3), CsgDyadic.newArray(3)};
        CsgDyadic[][] candidate = {CsgDyadic.newArray(4), CsgDyadic.newArray(4), CsgDyadic.newArray(4), CsgDyadic.newArray(4)};
        for (int i=0;i<3;++i) {
            if (!CsgDyadic.fromDouble(-forward.get(i,3),translation[i])) return false;
            for (int j=0;j<3;++j) if (!CsgDyadic.fromDouble(forward.get(i,j),matrix[i][j])) return false;
        }
        for (int i=0;i<3;++i) for (int j=0;j<3;++j) {
            final int r=(i+1)%3,s=(i+2)%3,c=(j+1)%3,d=(j+2)%3;
            CsgDyadic first = new CsgDyadic(), second = new CsgDyadic();
            if (!CsgDyadic.multiply(matrix[r][c],matrix[s][d],first) ||
                !CsgDyadic.multiply(matrix[r][d],matrix[s][c],second) ||
                !CsgDyadic.subtract(first,second,cofactors[i][j])) return false;
        }
        CsgDyadic determinant = new CsgDyadic();
        if (!dot(matrix[0],cofactors[0],3,determinant) || determinant.getSign()==0) return false;
        for (int i=0;i<3;++i) {
            for (int j=0;j<3;++j) candidate[i][j].assign(cofactors[j][i]);
            if (!dot(candidate[i],translation,3,candidate[i][3])) return false;
        }
        candidate[3][3].assign(determinant);
        for (int i=0;i<4;++i) for (int j=0;j<4;++j) result[i][j].assign(candidate[i][j]);
        return true;
    }
}
