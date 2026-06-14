#include "model/NonTangibleCube.hpp"
#include <cstdio>
#include <cmath>
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"

NonTangibleCube::NonTangibleCube() : hasMapping_(false) {}

void NonTangibleCube::mapMarker(int markerId, int cubeId) {
    markerToCube_.put(markerId, cubeId);
    hasMapping_ = true;
}

bool NonTangibleCube::parseMapping(const java::String& spec) {
    markerToCube_.clear();
    hasMapping_ = false;

    int pos = 0;
    while (pos < spec.length()) {
        int colon = spec.find(':', pos);
        if (colon == java::String::npos) break;

        java::String cubeIdStr = spec.substr(pos, colon - pos);
        int cubeId = std::atoi(cubeIdStr.c_str());
        int semicolon = spec.find(';', colon);
        if (semicolon == java::String::npos) semicolon = spec.length();

        java::String markerList = spec.substr(colon + 1, semicolon - colon - 1);
        int mpos = 0;
        while (mpos < markerList.length()) {
            int comma = markerList.find(',', mpos);
            if (comma == java::String::npos) comma = markerList.length();

            java::String markerIdStr = markerList.substr(mpos, comma - mpos);
            int markerId = std::atoi(markerIdStr.c_str());
            mapMarker(markerId, cubeId);
            mpos = comma + 1;
        }

        pos = semicolon + 1;
    }

    return hasMapping_;
}

int NonTangibleCube::cubeOf(int markerId) const {
    return markerToCube_.getOrDefault(markerId, -1);
}

java::ArrayList<CubePose> NonTangibleCube::update(const java::ArrayList<MarkerPose>& markers,
                                          double decisionMarginThreshold,
                                          double viewAngleCosThreshold) {
    java::ArrayList<CubeMarkerGroup> cubeMarkers;

    for (long i = 0; i < markers.size(); ++i) {
        const MarkerPose& m = markers.get(i);
        if (m.decisionMargin < decisionMarginThreshold) continue;
        if (m.viewDot < viewAngleCosThreshold) continue;

        int cubeId = -1;
        if (hasMapping_) {
            cubeId = cubeOf(m.markerId);
            if (cubeId < 0) continue;
        } else {
            cubeId = m.markerId;
        }

        long groupIdx = -1;
        for (long j = 0; j < cubeMarkers.size(); ++j) {
            if (cubeMarkers.get(j).cubeId == cubeId) {
                groupIdx = j;
                break;
            }
        }

        if (groupIdx < 0) {
            CubeMarkerGroup group;
            group.cubeId = cubeId;
            cubeMarkers.add(group);
            groupIdx = cubeMarkers.size() - 1;
        }

        cubeMarkers[groupIdx].poses.add(m);
    }

    java::ArrayList<CubePose> result;
    for (long i = 0; i < cubeMarkers.size(); ++i) {
        const CubeMarkerGroup& group = cubeMarkers.get(i);
        int cubeId = group.cubeId;
        const java::ArrayList<MarkerPose>& poses = group.poses;

        if (poses.size() == 0) continue;

        float sumX = 0, sumY = 0, sumZ = 0;
        for (long i = 0; i < poses.size(); ++i) {
            const MarkerPose& p = poses.get(i);
            sumX += p.position.x();
            sumY += p.position.y();
            sumZ += p.position.z();
        }
        float avgX = sumX / poses.size();
        float avgY = sumY / poses.size();
        float avgZ = sumZ / poses.size();

        float sumQa = 0, sumQb = 0, sumQc = 0, sumQd = 0;
        for (long i = 0; i < poses.size(); ++i) {
            const MarkerPose& p = poses.get(i);
            Vector3Df dir = p.rotation.direction();
            float mag = p.rotation.magnitude();
            sumQa += mag;
            sumQb += dir.x();
            sumQc += dir.y();
            sumQd += dir.z();
        }

        float qlen = std::sqrt(sumQa*sumQa + sumQb*sumQb + sumQc*sumQc + sumQd*sumQd);
        float qa = 0, qb = 0, qc = 0, qd = 0;
        if (qlen > 1e-6f) {
            qa = sumQa / qlen;
            qb = sumQb / qlen;
            qc = sumQc / qlen;
            qd = sumQd / qlen;
        }

        CubePose cube;
        cube.id = cubeId;
        cube.x = avgX;
        cube.y = avgY;
        cube.z = avgZ;
        cube.a = qa;
        cube.b = qb;
        cube.c = qc;
        cube.d = qd;

        result.add(cube);
    }

    return result;
}
