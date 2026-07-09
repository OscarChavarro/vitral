#ifndef __MARKER_GROUP_PERSISTENCE__
#define __MARKER_GROUP_PERSISTENCE__

#include "java/lang/String.h"
#include "model/MarkerGroup.hpp"

class MarkerGroupPersistence {
public:
    bool readFromJsonFile(const java::String& filePath, MarkerGroup* outGroup) const;

private:
    Quaterniond eulerDegToQuaternion(double yawDeg, double pitchDeg, double rollDeg) const;
};

#endif
