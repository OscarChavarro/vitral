#ifndef MARKER_POSER_HPP
#define MARKER_POSER_HPP

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "webservice/Protocol.hpp"
#include "model/MarkerGroup.hpp"

class MarkerPoser {
public:
    java::ArrayList<MarkerGroupPose> estimate(
        const java::ArrayList<MarkerPose>& detected,
        const java::ArrayList<MarkerGroup>& groups,
        double decisionMarginThreshold,
        double viewAngleCosThreshold) const;

private:
    Matrix4x4d buildTransform(const Vector3Dd& position, const Quaterniond& rotation) const;
    Quaterniond weightedAverageQuaternion(
        const java::ArrayList<Quaterniond>& quats,
        const java::ArrayList<double>& weights) const;
};

#endif
