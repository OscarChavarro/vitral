#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/common/VSDK.h"
#include <cmath>

Torus::Torus(double inMajorRadius, double inMinorRadius)
    : majorRadius(inMajorRadius), minorRadius(inMinorRadius) {}

double Torus::getMajorRadius() const { return majorRadius; }
void Torus::setMajorRadius(double rMajor) { majorRadius = rMajor; }
double Torus::getMinorRadius() const { return minorRadius; }
void Torus::setMinorRadius(double rMinor) { minorRadius = rMinor; }

double Torus::implicitValue(const Vector3Dd& p) const {
    double x = p.x(), y = p.y(), z = p.z();
    double sum = x*x + y*y + z*z + majorRadius*majorRadius - minorRadius*minorRadius;
    return sum*sum - 4.0*majorRadius*majorRadius*(x*x + y*y);
}

Ray* Torus::doIntersection(const Ray& inOutRay) {
    Ray normalized(inOutRay.origin(), inOutRay.direction().normalized(), inOutRay.t());

    const double tMin = 0.0;
    const double tMax = 1e4;
    const int samples = 2048;
    double prevT = tMin;
    double prevF = implicitValue(normalized.origin().add(normalized.direction().multiply(prevT)));

    bool found = false;
    double rootT = 0.0;

    for (int i = 1; i <= samples; ++i) {
        double t = tMin + (tMax - tMin) * ((double)i / (double)samples);
        double f = implicitValue(normalized.origin().add(normalized.direction().multiply(t)));

        if ((prevF <= 0 && f >= 0) || (prevF >= 0 && f <= 0)) {
            double a = prevT;
            double b = t;
            double fa = prevF;
            for (int it = 0; it < 60; ++it) {
                double m = 0.5 * (a + b);
                double fm = implicitValue(normalized.origin().add(normalized.direction().multiply(m)));
                if ((fa <= 0 && fm >= 0) || (fa >= 0 && fm <= 0)) {
                    b = m;
                }
                else {
                    a = m;
                    fa = fm;
                }
            }
            rootT = 0.5 * (a + b);
            found = true;
            break;
        }
        prevT = t;
        prevF = f;
    }

    if (!found || rootT <= VSDK::EPSILON) {
        return nullptr;
    }

    Ray out = normalized.withT(rootT);
    return new Ray(out);
}

bool Torus::doIntersection(const Ray& inRay, RayHit* outHit) {
    Ray* hit = doIntersection(inRay);
    if (hit == nullptr) return false;

    if (outHit != nullptr) {
        outHit->setRay(*hit);
        doExtraInformation(*hit, hit->t(), outHit);
        outHit->setRay(*hit);
    }
    delete hit;
    return true;
}

void Torus::doExtraInformation(const Ray& inRay, double inT, RayHit* outHit) {
    if (outHit == nullptr) return;

    Vector3Dd p = inRay.origin().add(inRay.direction().multiply(inT));
    if (outHit->needsPoint()) {
        outHit->p = p;
    }

    if (outHit->needsNormal() || outHit->needsTextureCoordinates() || outHit->needsTangent()) {
        double x = p.x(), y = p.y(), z = p.z();
        double sum = x*x + y*y + z*z + majorRadius*majorRadius - minorRadius*minorRadius;
        Vector3Dd grad(
            4*x*sum - 8*majorRadius*majorRadius*x,
            4*y*sum - 8*majorRadius*majorRadius*y,
            4*z*sum);
        Vector3Dd n = grad.normalized();
        if (outHit->needsNormal()) outHit->n = n;

        if (outHit->needsTextureCoordinates()) {
            double theta = std::atan2(y, x);
            double ring = std::sqrt(x*x + y*y);
            double phi = std::atan2(z, ring - majorRadius);
            outHit->u = (theta + M_PI) / (2.0 * M_PI);
            outHit->v = (phi + M_PI) / (2.0 * M_PI);
        }

        if (outHit->needsTangent()) {
            double theta = std::atan2(y, x);
            outHit->t = Vector3Dd(-std::sin(theta), std::cos(theta), 0);
        }
    }
}

double* Torus::getMinMax() {
    double* m = new double[6];
    double ext = majorRadius + minorRadius;
    m[0] = -ext; m[1] = -ext; m[2] = -minorRadius;
    m[3] =  ext; m[4] =  ext; m[5] =  minorRadius;
    return m;
}
