#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExactTransform.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExactPlane.h"
BOOLEAN CsgExactPlane::world(const double* local, const Matrix4x4d& forward, double* result)
{ int i;
    if (!local || !result) return FALSE;
    for (i =0;i<4;++i) if (!Double::isFinite(local[i])) return FALSE;
    if (local[0]==0 && local[1]==0 && local[2]==0) return FALSE;
    double inverse[4][4];
    if (!CsgExactTransform::inverseNumerator(forward,inverse)) return FALSE;
    double candidate[4];
    for (i =0;i<4;++i) {
        const double column[4]={inverse[0][i],inverse[1][i],inverse[2][i],inverse[3][i]};
        if (!CsgExactTransform::exactSum(column,local,4,candidate[i])) return FALSE;
    }
    const double sign=inverse[3][3]<0 ? -1 : 1;
    for (i =0;i<4;++i) result[i]=sign*candidate[i];
    return TRUE;
}
