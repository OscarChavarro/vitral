package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/** Exact sufficient certificates; failure never establishes non-equivalence. */
public class CsgDyadicSolid {
    private static CsgDyadic[][] square(int n) {
        CsgDyadic[][] result = new CsgDyadic[n][];
        for (int i = 0; i < n; i++) result[i] = CsgDyadic.newArray(n);
        return result;
    }

    private static boolean proportional(CsgDyadic[] a, CsgDyadic[] b, int count) {
        int pivot=-1;
        for (int i=0;i<count;++i) if (a[i].getSign() != 0 || b[i].getSign() != 0) { pivot=i; break; }
        if (pivot<0 || a[pivot].getSign() == 0 || a[pivot].getSign()!=b[pivot].getSign()) return false;
        for (int i=0;i<count;++i) {
            CsgDyadic x = new CsgDyadic(), y = new CsgDyadic();
            if (!CsgDyadic.multiply(a[i],b[pivot],x) || !CsgDyadic.multiply(b[i],a[pivot],y) || !x.equals(y)) return false;
        }
        return true;
    }

    private static boolean planeWorld(double[] local, Matrix4x4d forward, CsgDyadic[] result) {
        if (local == null || result == null || (local[0]==0 && local[1]==0 && local[2]==0)) return false;
        CsgDyadic[] input = CsgDyadic.newArray(4), candidate = CsgDyadic.newArray(4);
        CsgDyadic[][] inverse = square(4);
        for (int i=0;i<4;++i) if (!CsgDyadic.fromDouble(local[i],input[i])) return false;
        if (!CsgDyadicTransform.inverseNumerator(forward,inverse)) return false;
        for (int i=0;i<4;++i) {
            CsgDyadic[] column = new CsgDyadic[4]; for (int j=0;j<4;++j) column[j]=new CsgDyadic(inverse[j][i]);
            if (!CsgDyadicTransform.dot(column,input,4,candidate[i])) return false;
            if (inverse[3][3].getSign()<0) candidate[i].negate();
        }
        for (int i=0;i<4;++i) result[i].assign(candidate[i]);
        return true;
    }

    private static boolean quadricLocal(double[] local, CsgDyadic[] result) {
        if (local == null || result == null) return false;
        CsgDyadic[] candidate = CsgDyadic.newArray(10);
        for (int i=0;i<10;++i) if (!CsgDyadic.fromDouble(local[i],candidate[i])) return false;
        for (int i=0;i<10;++i) result[i].assign(candidate[i]);
        return true;
    }

    private static boolean quadricWorld(CsgDyadic[] local, Matrix4x4d forward, CsgDyadic[] result) {
        if (local == null || result == null) return false;
        CsgDyadic[][] inverse = square(4); if (!CsgDyadicTransform.inverseNumerator(forward,inverse)) return false;
        // Use twice the homogeneous coefficient matrix, avoiding any division.
        CsgDyadic[][] q = square(4), intermediate = square(4), world = square(4);
        for (int i=0;i<3;++i) if (!CsgDyadic.add(local[i],local[i],q[i][i])) return false;
        if (!CsgDyadic.add(local[9],local[9],q[3][3])) return false;
        final int[] row={0,0,1,0,1,2}, col={1,2,2,3,3,3};
        for (int i=0;i<6;++i) { q[col[i]][row[i]].assign(local[3+i]); q[row[i]][col[i]].assign(q[col[i]][row[i]]); }
        for (int i=0;i<4;++i) for (int j=0;j<4;++j) {
            CsgDyadic[] column = new CsgDyadic[4]; for (int k=0;k<4;++k) column[k]=new CsgDyadic(inverse[k][j]);
            if (!CsgDyadicTransform.dot(q[i],column,4,intermediate[i][j])) return false;
        }
        for (int i=0;i<4;++i) for (int j=0;j<4;++j) {
            CsgDyadic[] column = new CsgDyadic[4], values = new CsgDyadic[4];
            for (int k=0;k<4;++k) { column[k]=new CsgDyadic(inverse[k][i]); values[k]=new CsgDyadic(intermediate[k][j]); }
            if (!CsgDyadicTransform.dot(column,values,4,world[i][j])) return false;
        }
        result[0].assign(world[0][0]); result[1].assign(world[1][1]); result[2].assign(world[2][2]);
        for (int i=0;i<6;++i) result[3+i].assign(world[row[i]][col[i]]);
        result[9].assign(world[3][3]);
        return true;
    }

    public static boolean samePlanes(double[] a, Matrix4x4d pa, double[] b, Matrix4x4d pb) {
        CsgDyadic[] x = CsgDyadic.newArray(4), y = CsgDyadic.newArray(4);
        return planeWorld(a,pa,x) && planeWorld(b,pb,y) && proportional(x,y,4);
    }

    public static boolean sameQuadrics(double[] a, Matrix4x4d pa, double[] b, Matrix4x4d pb) {
        CsgDyadic[] qa = CsgDyadic.newArray(10), qb = CsgDyadic.newArray(10),
            x = CsgDyadic.newArray(10), y = CsgDyadic.newArray(10);
        return quadricLocal(a,qa) && quadricLocal(b,qb) && quadricWorld(qa,pa,x) && quadricWorld(qb,pb,y) && proportional(x,y,10);
    }

    public static boolean sphereQuadric(double radius, Matrix4x4d ps, double[] q, Matrix4x4d pq) {
        if (!Double.isFinite(radius) || radius<=0) return false;
        CsgDyadic[] local = CsgDyadic.newArray(10), quadric = CsgDyadic.newArray(10),
            x = CsgDyadic.newArray(10), y = CsgDyadic.newArray(10);
        CsgDyadic r = new CsgDyadic();
        if (!CsgDyadic.fromDouble(1,local[0]) || !CsgDyadic.fromDouble(radius,r)) return false;
        local[2].assign(local[0]); local[1].assign(local[2]);
        if (!CsgDyadic.multiply(r,r,local[9])) return false;
        local[9].negate();
        return quadricLocal(q,quadric) && quadricWorld(local,ps,x) && quadricWorld(quadric,pq,y) && proportional(x,y,10);
    }

    public static boolean sameEllipsoids(double ra, Matrix4x4d pa, double rb, Matrix4x4d pb) {
        if (!Double.isFinite(ra) || !Double.isFinite(rb) || ra<=0 || rb<=0) return false;
        for (int i=0;i<3;++i) if (pa.get(i,3)!=pb.get(i,3)) return false;
        CsgDyadic rA = new CsgDyadic(), rB = new CsgDyadic();
        CsgDyadic[][] x = square(3), y = square(3);
        if (!CsgDyadic.fromDouble(ra,rA) || !CsgDyadic.fromDouble(rb,rB)) return false;
        for (int i=0;i<3;++i) for (int j=0;j<3;++j) {
            CsgDyadic a = new CsgDyadic(), b = new CsgDyadic();
            if (!CsgDyadic.fromDouble(pa.get(i,j),a) || !CsgDyadic.fromDouble(pb.get(i,j),b) ||
                !CsgDyadic.multiply(a,rA,x[i][j]) || !CsgDyadic.multiply(b,rB,y[i][j])) return false;
        }
        for (int i=0;i<3;++i) for (int j=i;j<3;++j) {
            CsgDyadic a = new CsgDyadic(), b = new CsgDyadic();
            if (!CsgDyadicTransform.dot(x[i],x[j],3,a) || !CsgDyadicTransform.dot(y[i],y[j],3,b) || !a.equals(b)) return false;
        }
        return true;
    }
}
