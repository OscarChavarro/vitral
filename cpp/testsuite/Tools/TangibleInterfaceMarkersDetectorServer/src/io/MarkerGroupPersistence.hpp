#ifndef MARKER_GROUP_PERSISTENCE_HPP
#define MARKER_GROUP_PERSISTENCE_HPP

#include "java/lang/String.h"
#include "model/MarkerGroup.hpp"

class MarkerGroupPersistence {
public:
    bool readFromJsonFile(const java::String& filePath, MarkerGroup* outGroup) const;

private:
    Quaterniond eulerDegToQuaternion(double yawDeg, double pitchDeg, double rollDeg) const;
};

#endif
