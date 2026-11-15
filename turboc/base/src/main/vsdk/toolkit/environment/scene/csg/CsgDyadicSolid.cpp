#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/scene/csg/CsgDyadicTransform.h"
#include "vsdk/toolkit/environment/scene/csg/CsgDyadicSolid.h"
BOOLEAN CsgDyadicSolid::proportional(const CsgDyadic* a, const CsgDyadic* b, int count)
{ int i;
    int pivot=-1;
    for (i =0;i<count;++i) if (a[i].getSign() || b[i].getSign()) { pivot=i; break; }
    if (pivot<0 || !a[pivot].getSign() || a[pivot].getSign()!=b[pivot].getSign()) return FALSE;
    for (i =0;i<count;++i) {
        CsgDyadic x,y;
        if (!CsgDyadic::multiply(a[i],b[pivot],x) || !CsgDyadic::multiply(b[i],a[pivot],y) || !x.equals(y)) return FALSE;
    }
    return TRUE;
}
BOOLEAN CsgDyadicSolid::planeWorld(const double* local, const Matrix4x4d& forward, CsgDyadic* result)
{ int i;
    if (!local || !result || (local[0]==0 && local[1]==0 && local[2]==0)) return FALSE;
    CsgDyadic input[4], inverse[4][4], candidate[4];
    for (i =0;i<4;++i) if (!CsgDyadic::fromDouble(local[i],input[i])) return FALSE;
    if (!CsgDyadicTransform::inverseNumerator(forward,inverse)) return FALSE;
    for (i =0;i<4;++i) {
        CsgDyadic column[4]; for (int j=0;j<4;++j) column[j]=inverse[j][i];
        if (!CsgDyadicTransform::dot(column,input,4,candidate[i])) return FALSE;
        if (inverse[3][3].getSign()<0) candidate[i].negate();
    }
    for (i =0;i<4;++i) result[i]=candidate[i];
    return TRUE;
}
BOOLEAN CsgDyadicSolid::quadricLocal(const double* local, CsgDyadic* result)
{ int i;
    if (!local || !result) return FALSE;
    CsgDyadic candidate[10];
    for (i =0;i<10;++i) if (!CsgDyadic::fromDouble(local[i],candidate[i])) return FALSE;
    for (i =0;i<10;++i) result[i]=candidate[i];
    return TRUE;
}
BOOLEAN CsgDyadicSolid::quadricWorld(const CsgDyadic* local, const Matrix4x4d& forward, CsgDyadic* result)
{ int i; int j;
    if (!local || !result) return FALSE;
    CsgDyadic inverse[4][4]; if (!CsgDyadicTransform::inverseNumerator(forward,inverse)) return FALSE;
    // Use twice the homogeneous coefficient matrix, avoiding any division.
    CsgDyadic q[4][4], intermediate[4][4], world[4][4];
    for (i =0;i<3;++i) if (!CsgDyadic::add(local[i],local[i],q[i][i])) return FALSE;
    if (!CsgDyadic::add(local[9],local[9],q[3][3])) return FALSE;
    const int row[6]={0,0,1,0,1,2}, col[6]={1,2,2,3,3,3};
    for (i =0;i<6;++i) q[row[i]][col[i]]=q[col[i]][row[i]]=local[3+i];
    for (i =0;i<4;++i) for (j =0;j<4;++j) {
        CsgDyadic column[4]; for (int k=0;k<4;++k) column[k]=inverse[k][j];
        if (!CsgDyadicTransform::dot(q[i],column,4,intermediate[i][j])) return FALSE;
    }
    for (i =0;i<4;++i) for (j =0;j<4;++j) {
        CsgDyadic column[4], values[4];
        for (int k=0;k<4;++k) { column[k]=inverse[k][i]; values[k]=intermediate[k][j]; }
        if (!CsgDyadicTransform::dot(column,values,4,world[i][j])) return FALSE;
    }
    result[0]=world[0][0]; result[1]=world[1][1]; result[2]=world[2][2];
    for (i =0;i<6;++i) result[3+i]=world[row[i]][col[i]];
    result[9]=world[3][3];
    return TRUE;
}
BOOLEAN CsgDyadicSolid::samePlanes(const double* a, const Matrix4x4d& pa, const double* b, const Matrix4x4d& pb)
{
    CsgDyadic x[4],y[4];
    return planeWorld(a,pa,x) && planeWorld(b,pb,y) && proportional(x,y,4);
}
BOOLEAN CsgDyadicSolid::sameQuadrics(const double* a, const Matrix4x4d& pa, const double* b, const Matrix4x4d& pb)
{
    CsgDyadic qa[10],qb[10],x[10],y[10];
    return quadricLocal(a,qa) && quadricLocal(b,qb) && quadricWorld(qa,pa,x) && quadricWorld(qb,pb,y) && proportional(x,y,10);
}
BOOLEAN CsgDyadicSolid::sphereQuadric(double radius, const Matrix4x4d& ps, const double* q, const Matrix4x4d& pq)
{
    if (!Double::isFinite(radius) || radius<=0) return FALSE;
    CsgDyadic local[10], quadric[10], x[10],y[10],r;
    if (!CsgDyadic::fromDouble(1,local[0]) || !CsgDyadic::fromDouble(radius,r)) return FALSE;
    local[1]=local[2]=local[0];
    if (!CsgDyadic::multiply(r,r,local[9])) return FALSE;
    local[9].negate();
    return quadricLocal(q,quadric) && quadricWorld(local,ps,x) && quadricWorld(quadric,pq,y) && proportional(x,y,10);
}
BOOLEAN CsgDyadicSolid::sameEllipsoids(double ra, const Matrix4x4d& pa, double rb, const Matrix4x4d& pb)
{ int i; int j;
    if (!Double::isFinite(ra) || !Double::isFinite(rb) || ra<=0 || rb<=0) return FALSE;
    for (i =0;i<3;++i) if (pa.get(i,3)!=pb.get(i,3)) return FALSE;
    CsgDyadic rA,rB,x[3][3],y[3][3];
    if (!CsgDyadic::fromDouble(ra,rA) || !CsgDyadic::fromDouble(rb,rB)) return FALSE;
    for (i =0;i<3;++i) for (j =0;j<3;++j) {
        CsgDyadic a,b;
        if (!CsgDyadic::fromDouble(pa.get(i,j),a) || !CsgDyadic::fromDouble(pb.get(i,j),b) ||
            !CsgDyadic::multiply(a,rA,x[i][j]) || !CsgDyadic::multiply(b,rB,y[i][j])) return FALSE;
    }
    for (i =0;i<3;++i) for (j =i;j<3;++j) {
        CsgDyadic a,b;
        if (!CsgDyadicTransform::dot(x[i],x[j],3,a) || !CsgDyadicTransform::dot(y[i],y[j],3,b) || !a.equals(b)) return FALSE;
    }
    return TRUE;
}
