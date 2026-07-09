#ifndef __NON_TANGIBLE_CUBE__
#define __NON_TANGIBLE_CUBE__

#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include "java/lang/String.h"
#include "webservice/Protocol.hpp"

class NonTangibleCube {
public:
    NonTangibleCube();

    void mapMarker(int markerId, int cubeId);
    bool parseMapping(const java::String& spec);

    java::ArrayList<CubePose> update(const java::ArrayList<MarkerPose>& markers,
                                 double decisionMarginThreshold,
                                 double viewAngleCosThreshold);

private:
    struct CubeMarkerGroup {
        int cubeId;
        java::ArrayList<MarkerPose> poses;
    };

    int cubeOf(int markerId) const;

    java::HashMap<int, int> markerToCube_;
    bool hasMapping_;
};
#endif
