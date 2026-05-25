#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"

const double Arrow::NO_HIT = 1e308;

Arrow::Arrow(double bl, double hl, double br, double hr)
    : baseLength(bl), headLength(hl), baseRadius(br), headRadius(hr) {
    baseCylinder = new Cone(baseRadius, baseRadius, baseLength);
    headCone = new Cone(headRadius, 0, headLength);
    lastElement = baseCylinder;
}

Arrow::~Arrow() {
    delete baseCylinder;
    delete headCone;
}

double Arrow::getBaseLength() const { return baseLength; }
void Arrow::setBaseLength(double val) { baseLength = val; baseCylinder->setHeight(val); }
double Arrow::getHeadLength() const { return headLength; }
void Arrow::setHeadLength(double val) { headLength = val; headCone->setHeight(val); }
double Arrow::getBaseRadius() const { return baseRadius; }
void Arrow::setBaseRadius(double val) { baseRadius = val; baseCylinder->setBaseRadius(val); baseCylinder->setTopRadius(val); }
double Arrow::getHeadRadius() const { return headRadius; }
void Arrow::setHeadRadius(double val) { headRadius = val; headCone->setBaseRadius(val); }

Ray* Arrow::doIntersection(const Ray& inOutRay) {
    Vector3Dd tr(0,0,-baseLength);
    Ray headRay(inOutRay.origin().add(tr), inOutRay.direction());
    Ray baseRay(inOutRay);

    Ray* baseHit = baseCylinder->doIntersection(baseRay);
    Ray* headHit = headCone->doIntersection(headRay);

    Ray* result = nullptr;
    if ((baseHit != nullptr && headHit == nullptr) ||
        (baseHit != nullptr && headHit != nullptr && baseHit->t() < headHit->t())) {
        lastElement = baseCylinder;
        result = new Ray(inOutRay.withT(baseHit->t()));
    }
    else if ((baseHit == nullptr && headHit != nullptr) ||
             (baseHit != nullptr && headHit != nullptr && headHit->t() < baseHit->t())) {
        lastElement = headCone;
        result = new Ray(inOutRay.withT(headHit->t()));
    }

    if (baseHit) delete baseHit;
    if (headHit) delete headHit;
    return result;
}

bool Arrow::doIntersectionDistanceOnly(const Ray& inRay, RayHit* outHit) {
    Vector3Dd shiftedHeadOrigin(inRay.origin().x(), inRay.origin().y(), inRay.origin().z() - baseLength);
    Ray shiftedHeadRay(shiftedHeadOrigin, inRay.direction(), inRay.t());

    RayHit localHit(RayHit::DETAIL_NONE, false);
    RayHit* candidateHit = (outHit != nullptr) ? outHit : &localHit;
    bool shouldStoreRay = (outHit != nullptr) ? outHit->shouldStoreRay() : false;
    candidateHit->setStoreRay(false);

    double baseT = NO_HIT;
    candidateHit->resetForDistanceOnly();
    if (baseCylinder->doIntersection(inRay, candidateHit)) baseT = candidateHit->hitDistance();

    double headT = NO_HIT;
    candidateHit->resetForDistanceOnly();
    if (headCone->doIntersection(shiftedHeadRay, candidateHit)) headT = candidateHit->hitDistance();

    double winnerT = (baseT < headT) ? baseT : headT;
    if (winnerT == NO_HIT) return false;

    if (outHit != nullptr) {
        if (shouldStoreRay) outHit->setRay(inRay.withT(winnerT));
        else outHit->setHitDistance(winnerT);
        outHit->setStoreRay(shouldStoreRay);
    }
    return true;
}

bool Arrow::doIntersection(const Ray& inRay, RayHit* outHit) {
    if (outHit == nullptr || !outHit->needsAnySurfaceData()) {
        return doIntersectionDistanceOnly(inRay, outHit);
    }

    Vector3Dd tr(0,0,-baseLength);
    Ray shiftedHeadRay(inRay.origin().add(tr), inRay.direction(), inRay.t());

    RayHit baseHit(outHit->requiredDetailMask());
    RayHit headHit(outHit->requiredDetailMask());
    bool hasBase = baseCylinder->doIntersection(inRay, &baseHit);
    bool hasHead = headCone->doIntersection(shiftedHeadRay, &headHit);
    if (!hasBase && !hasHead) return false;

    double baseT = hasBase ? (baseHit.ray()!=nullptr ? baseHit.ray()->t() : baseHit.hitDistance()) : NO_HIT;
    double headT = hasHead ? (headHit.ray()!=nullptr ? headHit.ray()->t() : headHit.hitDistance()) : NO_HIT;

    if (hasBase && (!hasHead || baseT < headT)) {
        outHit->clone(baseHit);
        outHit->setRay(inRay.withT(baseT));
    }
    else {
        outHit->clone(headHit);
        outHit->setRay(inRay.withT(headT));
        if (outHit->needsPoint()) {
            outHit->p = Vector3Dd(outHit->p.x(), outHit->p.y(), outHit->p.z() + baseLength);
        }
    }
    return true;
}

void Arrow::doExtraInformation(const Ray& inRay, double inT, RayHit* outData) {
    if (outData == nullptr) return;
    RayHit hit;
    if (doIntersection(inRay.withT(inT), &hit)) outData->clone(hit);
}

double* Arrow::getMinMax() {
    double* m = new double[6];
    double r = baseRadius > headRadius ? baseRadius : headRadius;
    m[0]=-r; m[1]=-r; m[2]=0; m[3]=r; m[4]=r; m[5]=baseLength+headLength;
    return m;
}
