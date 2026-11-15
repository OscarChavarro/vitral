#ifndef __GMTRY_WRKSPC_TST_WRKR__
#define __GMTRY_WRKSPC_TST_WRKR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Runnable.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryQueryWorkspace.h"
class Sphere;
class GeometryWorkspaceTestWorker : public Runnable {
private:
    const Sphere* sphere;
    GeometryQueryWorkspace workspace;
    BOOLEAN success;
public:
    explicit GeometryWorkspaceTestWorker(const Sphere* sphere);
    virtual void run();
    BOOLEAN passed() const;
    const RaySolidBoundary* boundaryStorage();
};
#endif
