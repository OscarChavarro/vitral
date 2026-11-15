#ifndef __TI_EVNT_2_RY_GZM_MPPR__
#define __TI_EVNT_2_RY_GZM_MPPR__

class Camera;
class RayGizmo;
class TangibleInterfaceEvent;

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class TangibleInterfaceEvent2RayGizmoMapper {
private:
    static const Vector3Dd MARKER_INTO_SCENE;
    static const double DISTANCE_FACTOR;
    static const double MAX_GIZMO_DEPTH_FACTOR;

    Camera* camera;

    static double computeRollAngle(
        const Quaterniond& rotation,
        const Vector3Dd& worldDirection,
        const Vector3Dd& camRight,
        const Vector3Dd& camUp,
        const Vector3Dd& camFront);

public:
    explicit TangibleInterfaceEvent2RayGizmoMapper(Camera* camera);
    void map(const TangibleInterfaceEvent& event, RayGizmo* gizmo);
};

#endif
