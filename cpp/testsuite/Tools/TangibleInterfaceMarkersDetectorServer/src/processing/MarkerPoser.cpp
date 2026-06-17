#include <cmath>

#include "java/util/ArrayList.txx"
#include "processing/MarkerPoser.hpp"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
Matrix4x4d MarkerPoser::buildTransform(const Vector3Dd& position, const Quaterniond& rotation) const {
    Matrix4x4d r = Matrix4x4d().importFromQuaternion(rotation.normalized());
    return r.withTranslation(position);
}

Quaterniond MarkerPoser::weightedAverageQuaternion(const java::ArrayList<Quaterniond>& quats,
                                                   const java::ArrayList<double>& weights) const {
    if (quats.size() == 0) {
        return Quaterniond(Vector3Dd(0, 0, 0), 1.0);
    }

    const Quaterniond& ref = quats.get(0);
    double rw = ref.magnitude();
    double rx = ref.direction().x();
    double ry = ref.direction().y();
    double rz = ref.direction().z();

    double sumW = 0.0;
    double sumX = 0.0;
    double sumY = 0.0;
    double sumZ = 0.0;

    for (long i = 0; i < quats.size(); ++i) {
        const Quaterniond& q = quats.get(i);
        double w = q.magnitude();
        double x = q.direction().x();
        double y = q.direction().y();
        double z = q.direction().z();

        double dot = rw*w + rx*x + ry*y + rz*z;
        if (dot < 0.0) {
            w = -w; x = -x; y = -y; z = -z;
        }

        double weight = weights.get(i);
        sumW += weight * w;
        sumX += weight * x;
        sumY += weight * y;
        sumZ += weight * z;
    }

    double len = std::sqrt(sumW*sumW + sumX*sumX + sumY*sumY + sumZ*sumZ);
    if (len < 1e-12) {
        return Quaterniond(Vector3Dd(0, 0, 0), 1.0);
    }

    return Quaterniond(Vector3Dd(sumX / len, sumY / len, sumZ / len), sumW / len).normalized();
}

java::ArrayList<MarkerGroupPose> MarkerPoser::estimate(
    const java::ArrayList<MarkerPose>& detected,
    const java::ArrayList<MarkerGroup>& groups,
    double decisionMarginThreshold,
    double viewAngleCosThreshold) const {

    java::ArrayList<MarkerGroupPose> out;

    for (long gi = 0; gi < groups.size(); ++gi) {
        const MarkerGroup& group = groups.get(gi);

        java::ArrayList<Vector3Dd> candidatePositions;
        java::ArrayList<Quaterniond> candidateRotations;
        java::ArrayList<double> candidateWeights;

        for (long di = 0; di < detected.size(); ++di) {
            const MarkerPose& dm = detected.get(di);
            if (dm.decisionMargin < decisionMarginThreshold) continue;
            if (dm.viewDot < viewAngleCosThreshold) continue;

            Marker gm;
            if (!group.findMarkerById(dm.markerId, &gm)) continue;

            Quaterniond camMarkerQ(
                Vector3Dd(dm.rotation.direction().x(), dm.rotation.direction().y(), dm.rotation.direction().z()),
                dm.rotation.magnitude());
            Vector3Dd camMarkerP(dm.position.x(), dm.position.y(), dm.position.z());

            Matrix4x4d tCameraMarker = buildTransform(camMarkerP, camMarkerQ);
            Matrix4x4d tGroupMarker = buildTransform(gm.position, gm.rotation);
            Matrix4x4d tCameraGroup = tCameraMarker.multiply(tGroupMarker.inverse());

            candidatePositions.add(tCameraGroup.extractTranslation());
            candidateRotations.add(tCameraGroup.withoutTranslation().exportToQuaternion().normalized());
            candidateWeights.add(dm.decisionMargin > 0.0 ? dm.decisionMargin : 1.0);
        }

        if (candidatePositions.size() == 0) continue;

        double sumW = 0.0;
        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;
        for (long i = 0; i < candidatePositions.size(); ++i) {
            const Vector3Dd& p = candidatePositions.get(i);
            double w = candidateWeights.get(i);
            sumW += w;
            sumX += w * p.x();
            sumY += w * p.y();
            sumZ += w * p.z();
        }

        MarkerGroupPose gp;
        gp.label = group.label;
        gp.position = Vector3Dd(sumX / sumW, sumY / sumW, sumZ / sumW);
        gp.rotation = weightedAverageQuaternion(candidateRotations, candidateWeights);
        out.add(gp);
    }

    return out;
}
