#ifndef __CSG_RAY_PLACEMENT__
#define __CSG_RAY_PLACEMENT__
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class CsgOperandView;
// Borrowed identity and owned child-to-parent and inverse snapshots.
class CsgRayPlacement {
public:
    const CsgOperandView* operand;
    Matrix4x4d childToParent;
    Matrix4x4d parentToChild;
    CsgRayPlacement();
    CsgRayPlacement(const CsgOperandView* operand, const Matrix4x4d& forward, const Matrix4x4d& inverse);
};
#endif
