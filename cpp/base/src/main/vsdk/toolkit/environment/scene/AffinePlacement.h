#ifndef __AFFINE_PLACEMENT__
#define __AFFINE_PLACEMENT__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/scene/AffinePlacementStatus.h"

// Pair in the column-vector convention. A valid pair maps geometry to its
// parent and back without changing affine ray parameters.
class AffinePlacement {
private:
    Matrix4x4d forward;
    Matrix4x4d inverse;
    AffinePlacementStatus status;
public:
    AffinePlacement();
    AffinePlacementStatus assign(const Matrix4x4d& forward, const Matrix4x4d& inverse);
    const Matrix4x4d& getForward() const { return forward; }
    const Matrix4x4d& getInverse() const { return inverse; }
    AffinePlacementStatus getStatus() const { return status; }
    static AffinePlacementStatus validate(const Matrix4x4d& forward, const Matrix4x4d& inverse);
};
#endif
