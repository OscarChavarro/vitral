#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include <cmath>

Sphere::Sphere(double r) : radius_(r), radiusSquared_(r * r) {}

Ray* Sphere::doIntersection(const Ray& inoutRay) {
    double dx = -inoutRay.getOrigin().x();
    double dy = -inoutRay.getOrigin().y();
    double dz = -inoutRay.getOrigin().z();
    const Vector3Dd& direction = inoutRay.getDirection();
    double v = direction.x() * dx + direction.y() * dy + direction.z() * dz;

    double t = radiusSquared_ + v * v - dx * dx - dy * dy - dz * dz;
    if (t < 0) {
        return nullptr;
    }

    t = v - std::sqrt(t);
    if (t < 0) {
        return nullptr;
    }

    Ray r = inoutRay.withT(t);
    return new Ray(r);
}

bool Sphere::doIntersection(const Ray& inRay, RayHit* outHit) {
    double dx = -inRay.getOrigin().x();
    double dy = -inRay.getOrigin().y();
    double dz = -inRay.getOrigin().z();
    const Vector3Dd& direction = inRay.getDirection();
    double projection = direction.x() * dx + direction.y() * dy + direction.z() * dz;

    double discriminant = radiusSquared_ + projection * projection - dx * dx - dy * dy - dz * dz;
    if (discriminant < 0) {
        return false;
    }

    double t = projection - std::sqrt(discriminant);
    if (t < 0) {
        return false;
    }

    if (outHit != nullptr) {
        if (outHit->shouldStoreRay() || outHit->needsAnySurfaceData()) {
            Ray hitRay = inRay.withT(t);
            outHit->setRay(hitRay);
            if (outHit->needsAnySurfaceData()) {
                doExtraInformation(hitRay, t, outHit);
                outHit->setRay(hitRay);
            }
        }
        else {
            outHit->setHitDistance(t);
        }
    }
    return true;
}

void Sphere::doExtraInformation(const Ray& inRay, double inT, RayHit* outData) {
    if (outData == nullptr) {
        return;
    }

    RaytraceStatistics::recordGeometryDetailComputation();
    bool needsNormalVector = outData->needsNormal() || outData->needsTextureCoordinates() || outData->needsTangent();
    if (!outData->needsPoint() && !needsNormalVector) {
        return;
    }

    Vector3Dd point(
        inRay.getOrigin().x() + inT * inRay.getDirection().x(),
        inRay.getOrigin().y() + inT * inRay.getDirection().y(),
        inRay.getOrigin().z() + inT * inRay.getDirection().z());

    if (outData->needsPoint()) {
        outData->p = point;
    }

    Vector3Dd normal;
    if (needsNormalVector) {
        normal = point.normalized();
        if (outData->needsNormal()) {
            outData->n = normal;
        }
    }

    if (!outData->needsTextureCoordinates() && !outData->needsTangent()) {
        return;
    }

    double theta;
    double phi = std::acos(normal.z());

    if (normal.x() > VSDK::EPSILON) {
        theta = std::atan(normal.y() / normal.x()) + 3 * M_PI / 2;
    }
    else if (normal.x() < VSDK::EPSILON) {
        theta = std::atan(normal.y() / normal.x()) + 3 * M_PI / 2;
        theta += M_PI;
        if (theta > 2 * M_PI) {
            theta -= 2 * M_PI;
        }
    }
    else {
        theta = 0.0;
    }

    if (outData->needsTextureCoordinates()) {
        outData->u = ((theta + M_PI / 2) / (2 * M_PI));
        outData->v = 1 - (phi / M_PI);
    }
    if (outData->needsTangent()) {
        outData->t = Vector3Dd(
            std::sin(theta - M_PI / 2),
            -std::cos(theta - M_PI / 2),
            0);
    }
}

int Sphere::doContainmentTest(const Vector3Dd& p, double distanceTolerance) {
    double l = p.length();
    if (l < radius_ - distanceTolerance) {
        return INSIDE;
    }
    else if (l > radius_ + distanceTolerance) {
        return OUTSIDE;
    }
    return LIMIT;
}

double* Sphere::getMinMax() {
    double* minmax = new double[6];
    for (int i = 0; i < 3; i++) {
        minmax[i] = -radius_;
    }
    for (int i = 3; i < 6; i++) {
        minmax[i] = radius_;
    }
    return minmax;
}

double Sphere::getRadius() const { return radius_; }
double Sphere::getRadiusSquared() const { return radiusSquared_; }
void Sphere::setRadius(double r) { radius_ = r; radiusSquared_ = r * r; }

Vector3Dd Sphere::spherePosition(double theta, double t, double r) {
    double phi = (t - 0.5) * M_PI;
    return Vector3Dd(
        std::cos(phi) * std::cos(theta) * r,
        std::cos(phi) * std::sin(theta) * r,
        std::sin(phi) * r);
}

PolyhedralBoundedSolid* Sphere::exportToPolyhedralBoundedSolid() {
    return buildPolyhedralBoundedSolid(DEFAULT_MERIDIANS, DEFAULT_PARALLELS);
}

PolyhedralBoundedSolid* Sphere::exportToPolyhedralBoundedSolid(int meridians, int parallels) {
    int normalizedMeridians = meridians < MIN_MERIDIANS ? MIN_MERIDIANS : meridians;
    int normalizedParallels = parallels < MIN_PARALLELS ? MIN_PARALLELS : parallels;

    if (normalizedMeridians == DEFAULT_MERIDIANS && normalizedParallels == DEFAULT_PARALLELS) {
        return exportToPolyhedralBoundedSolid();
    }

    return buildPolyhedralBoundedSolid(normalizedMeridians, normalizedParallels);
}

PolyhedralBoundedSolid* Sphere::buildPolyhedralBoundedSolid(int nmeridians, int nparalels) {
    double theta;
    double phi;
    double dtheta = 2 * M_PI / ((double)nmeridians);
    double dphi = 1.0 / ((double)nparalels);
    int i, base2, base1;
    Vector3Dd pos;

    PolyhedralBoundedSolid* solid;

    solid = new PolyhedralBoundedSolid();
    pos = Vector3Dd(0, 0, -radius_);
    PolyhedralBoundedSolidEulerOperators::mvfs(solid, pos, 1, 1);

    pos = spherePosition(dtheta, dphi, radius_);
    PolyhedralBoundedSolidEulerOperators::smev(solid, 1, 1, 3, pos);
    pos = spherePosition(0, dphi, radius_);
    PolyhedralBoundedSolidEulerOperators::smev(solid, 1, 3, 2, pos);

    PolyhedralBoundedSolidEulerOperators::mef(solid, 1, 1, 3, 2, 3, 2);

    for (i = 2; i < nmeridians; i++) {
        theta = dtheta * ((double)i);
        pos = spherePosition(theta, dphi, radius_);
        PolyhedralBoundedSolidEulerOperators::smev(solid, 1, 1, (i + 1) + 1, pos);
        PolyhedralBoundedSolidEulerOperators::mef(solid, 1, 1, (i + 0) + 1, (1), (i + 1) + 1, (1), i + 1);
    }

    PolyhedralBoundedSolidEulerOperators::mef(solid, 1, 1, (i + 1), (1), (2), (3), i + 1);
    base2 = i + 2;
    base1 = 2;

    int p;
    int nextFaceId = nmeridians + 2;
    for (p = 0; p < nparalels - 2; p++) {
        phi = ((double)(p + 2)) / ((double)nparalels);
        for (i = 0; i < nmeridians; i++) {
            theta = dtheta * ((double)i);
            pos = spherePosition(theta, phi, radius_);
            PolyhedralBoundedSolidEulerOperators::smev(solid, 1, (i) + base1, (i) + base2, pos);
            if (i > 0) {
                int quadFaceId = nextFaceId++;
                int diagFaceId = nextFaceId++;
                PolyhedralBoundedSolidEulerOperators::mef(solid, 1, 1,
                    (i - 1) + base2,
                    (i - 1) + base1,
                    (i) + base2,
                    (i) + base1,
                    quadFaceId);
                PolyhedralBoundedSolidEulerOperators::smef(solid, quadFaceId,
                    (i - 1) + base2,
                    (i) + base1,
                    diagFaceId);
            }
        }
        {
            int quadFaceId = nextFaceId++;
            int diagFaceId = nextFaceId++;
            PolyhedralBoundedSolidEulerOperators::mef(solid, 1, 1,
                (i + base2 - 1),
                (base1 + i - 1),
                (base2),
                (base2 + 1),
                quadFaceId);
            PolyhedralBoundedSolidEulerOperators::smef(solid, quadFaceId,
                (i - 1) + base2,
                base1,
                diagFaceId);
        }
        base1 = base2;
        base2 += nmeridians;
    }

    pos = Vector3Dd(0, 0, radius_);
    PolyhedralBoundedSolidEulerOperators::smev(solid, 1, base1, base2, pos);

    for (i = 0; i < nmeridians - 2; i++) {
        PolyhedralBoundedSolidEulerOperators::mef(solid, 1, 1,
            base2,
            base1 + i,
            base1 + i + 1,
            base1 + i + 2,
            nextFaceId++);
    }

    PolyhedralBoundedSolidEulerOperators::mef(solid, 1, 1,
        base2,
        base1 + i,
        base1 + i + 1,
        base1,
        nextFaceId++);

    return solid;
}

Vector3Dd Sphere::spherePosition(double theta, double phi) {
    return Vector3Dd(
        std::cos(phi) * std::cos(theta) * radius_,
        -std::cos(phi) * std::sin(theta) * radius_,
        std::sin(phi) * radius_);
}

Vector3Dd Sphere::sphereNormal(double theta, double phi) {
    return Vector3Dd(
        std::cos(phi) * std::cos(theta),
        -std::cos(phi) * std::sin(theta),
        std::sin(phi));
}

Vector3Dd Sphere::sphereTangent(double theta, double) {
    return Vector3Dd(
        std::sin(theta),
        std::cos(theta),
        0);
}

Vector3Dd Sphere::sphereBinormal(double theta, double phi) {
    return Vector3Dd(
        -std::sin(phi) * std::cos(theta),
        std::sin(phi) * std::sin(theta),
        std::cos(phi) * std::cos(theta) * std::cos(theta) +
        std::cos(phi) * std::sin(theta) * std::sin(theta));
}
