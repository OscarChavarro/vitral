#include <stddef.h>
#include <math.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerConstructions.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
const double Arrow::NO_HIT = 1e308;

Arrow::Arrow(double bl, double hl, double br, double hr)
    : baseLength(bl), headLength(hl), baseRadius(br), headRadius(hr) {
    baseCylinder = new Cone(baseRadius, baseRadius, baseLength);
    headCone = new Cone(headRadius, 0, headLength);
    lastElement = baseCylinder;
    // Turbo C++ 3.0 crashes with the new of a template class as an argument
    MemberEntityControlAccessor<Arrow, double> *accessor;
    accessor = new MemberEntityControlAccessor<Arrow, double>(
        &Arrow::getBaseLength, &Arrow::setBaseLength);
    addControlSpecification("double;baseLength;(0, INFINITE)", accessor);
    accessor = new MemberEntityControlAccessor<Arrow, double>(
        &Arrow::getHeadLength, &Arrow::setHeadLength);
    addControlSpecification("double;headLength;(0, INFINITE)", accessor);
    accessor = new MemberEntityControlAccessor<Arrow, double>(
        &Arrow::getBaseRadius, &Arrow::setBaseRadius);
    addControlSpecification("double;baseRadius;(0, INFINITE)", accessor);
    accessor = new MemberEntityControlAccessor<Arrow, double>(
        &Arrow::getHeadRadius, &Arrow::setHeadRadius);
    addControlSpecification("double;headRadius;(0, INFINITE)", accessor);
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
void Arrow::setBaseRadius(double val) { baseRadius = val; baseCylinder->setBottomRadius(val); baseCylinder->setTopRadius(val); }
double Arrow::getHeadRadius() const { return headRadius; }
void Arrow::setHeadRadius(double val) { headRadius = val; headCone->setBottomRadius(val); }

Ray* Arrow::doIntersectionFirstHit(const Ray& inOutRay) {
    Vector3Dd tr(0,0,-baseLength);
    Ray headRay(inOutRay.getOrigin().add(tr), inOutRay.getDirection());
    Ray baseRay(inOutRay);

    Ray* baseHit = baseCylinder->doIntersectionFirstHit(baseRay);
    Ray* headHit = headCone->doIntersectionFirstHit(headRay);

    Ray* result = NULL;
    if ((baseHit != NULL && headHit == NULL) ||
        (baseHit != NULL && headHit != NULL && baseHit->getT() < headHit->getT())) {
        lastElement = baseCylinder;
        result = new Ray(inOutRay.withT(baseHit->getT()));
    }
    else if ((baseHit == NULL && headHit != NULL) ||
             (baseHit != NULL && headHit != NULL && headHit->getT() < baseHit->getT())) {
        lastElement = headCone;
        result = new Ray(inOutRay.withT(headHit->getT()));
    }

    if (baseHit) delete baseHit;
    if (headHit) delete headHit;
    return result;
}

BOOLEAN Arrow::doIntersectionDistanceOnly(const Ray& inRay, RayHit* outHit) {
    Vector3Dd shiftedHeadOrigin(inRay.getOrigin().x(), inRay.getOrigin().y(), inRay.getOrigin().z() - baseLength);
    Ray shiftedHeadRay(shiftedHeadOrigin, inRay.getDirection(), inRay.getT());

    RayHit localHit(RayHit::DETAIL_NONE, FALSE);
    RayHit* candidateHit = (outHit != NULL) ? outHit : &localHit;
    BOOLEAN shouldStoreRay = (outHit != NULL) ? outHit->shouldStoreRay() : FALSE;
    candidateHit->setStoreRay(FALSE);

    double baseT = NO_HIT;
    candidateHit->resetForDistanceOnly();
    if (baseCylinder->doIntersectionFirstHit(inRay, candidateHit)) baseT = candidateHit->getHitDistance();

    double headT = NO_HIT;
    candidateHit->resetForDistanceOnly();
    if (headCone->doIntersectionFirstHit(shiftedHeadRay, candidateHit)) headT = candidateHit->getHitDistance();

    double winnerT = (baseT < headT) ? baseT : headT;
    if (winnerT == NO_HIT) return FALSE;

    if (outHit != NULL) {
        if (shouldStoreRay) outHit->setRay(inRay.withT(winnerT));
        else outHit->setHitDistance(winnerT);
        outHit->setStoreRay(shouldStoreRay);
    }
    return TRUE;
}

BOOLEAN Arrow::doIntersectionFirstHit(const Ray& inRay, RayHit* outHit) {
    if (outHit == NULL || !outHit->needsAnySurfaceData()) {
        return doIntersectionDistanceOnly(inRay, outHit);
    }

    Vector3Dd tr(0,0,-baseLength);
    Ray shiftedHeadRay(inRay.getOrigin().add(tr), inRay.getDirection(), inRay.getT());

    RayHit baseHit(outHit->getRequiredDetailMask());
    RayHit headHit(outHit->getRequiredDetailMask());
    BOOLEAN hasBase = baseCylinder->doIntersectionFirstHit(inRay, &baseHit);
    BOOLEAN hasHead = headCone->doIntersectionFirstHit(shiftedHeadRay, &headHit);
    if (!hasBase && !hasHead) return FALSE;

    double baseT = hasBase ? (baseHit.getRay()!=NULL ? baseHit.getRay()->getT() : baseHit.getHitDistance()) : NO_HIT;
    double headT = hasHead ? (headHit.getRay()!=NULL ? headHit.getRay()->getT() : headHit.getHitDistance()) : NO_HIT;

    if (hasBase && (!hasHead || baseT < headT)) {
        outHit->clone(baseHit);
        outHit->setRay(inRay.withT(baseT));
    }
    else {
        outHit->clone(headHit);
        outHit->setRay(inRay.withT(headT));
        if (outHit->needsPoint()) {
            outHit->point = Vector3Dd(outHit->point.x(), outHit->point.y(), outHit->point.z() + baseLength);
        }
    }
    return TRUE;
}

void Arrow::doExtraInformation(const Ray& inRay, double inT, RayHit* outData) {
    if (outData == NULL) return;
    RayHit hit;
    if (doIntersectionFirstHit(inRay.withT(inT), &hit)) outData->clone(hit);
}

double* Arrow::getMinMax() {
    double* m = new double[6];
    double r = baseRadius > headRadius ? baseRadius : headRadius;
    m[0]=-r; m[1]=-r; m[2]=0; m[3]=r; m[4]=r; m[5]=baseLength+headLength;
    return m;
}

PolyhedralBoundedSolid* Arrow::exportToPolyhedralBoundedSolid()
{
    return buildPolyhedralBoundedSolid(
        DEFAULT_CIRCUMFERENCE_DIVISIONS, DEFAULT_HEIGHT_DIVISIONS);
}

PolyhedralBoundedSolid* Arrow::exportToPolyhedralBoundedSolid(
    int circumferenceDivisions, int heightDivisions)
{
    int normalizedCircumferenceDivisions =
        circumferenceDivisions < MIN_CIRCUMFERENCE_DIVISIONS ?
        MIN_CIRCUMFERENCE_DIVISIONS : circumferenceDivisions;
    int normalizedHeightDivisions =
        heightDivisions < MIN_HEIGHT_DIVISIONS ?
        MIN_HEIGHT_DIVISIONS : heightDivisions;

    if ( normalizedCircumferenceDivisions ==
             DEFAULT_CIRCUMFERENCE_DIVISIONS &&
         normalizedHeightDivisions == DEFAULT_HEIGHT_DIVISIONS ) {
        return exportToPolyhedralBoundedSolid();
    }

    return buildPolyhedralBoundedSolid(
        normalizedCircumferenceDivisions, normalizedHeightDivisions);
}

void Arrow::closeTopFaceToApex(
    PolyhedralBoundedSolid* solid, int nsides, double apexZ)
{
    int base1 = 2 * nsides + 1;
    int base2 = 3 * nsides + 1;
    Vector3Dd apex(0, 0, apexZ);
    PolyhedralBoundedSolidEulerOperators::smev(solid, 1, base1, base2, apex);

    int i = 0;
    for ( i = 0; i < nsides - 2; ++i ) {
        PolyhedralBoundedSolidEulerOperators::mef(
            solid, 1, 1, base2, base1 + i, base1 + i + 1, base1 + i + 2, base2 + i + 1);
    }

    PolyhedralBoundedSolidEulerOperators::mef(
        solid, 1, 1, base2, base1 + i, base1 + i + 1, base1, base2 + i + 1);
}

PolyhedralBoundedSolid* Arrow::buildPolyhedralBoundedSolid(
    int nsides, int heightDivisions)
{
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidEulerConstructions::createCircularLamina(0.0, 0.0, baseRadius, 0.0, nsides);

    Matrix4x4d transform;
    double cylinderZStep = baseLength / ((double)(heightDivisions));
    int i;
    for ( i = 0; i < heightDivisions; ++i ) {
        transform = transform.translation(0.0, 0.0, cylinderZStep);
        PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(solid, solid->findFace(1), transform);
    }

    double scaleFactor = headRadius / baseRadius;
    Matrix4x4d scaleTransform;
    scaleTransform = scaleTransform.scale(scaleFactor, scaleFactor, 1.0);
    PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(solid, solid->findFace(1), scaleTransform);

    double prevRadius = headRadius;
    double coneZStep = headLength / ((double)(heightDivisions));
    for ( i = 1; i < heightDivisions; ++i ) {
        double nextRadius = headRadius *
            (1.0 - (((double)(i)) / ((double)(heightDivisions))));
        double coneScale = Cpp98Support::abs(prevRadius) > VSDK::EPSILON ?
            nextRadius / prevRadius : 0.0;
        transform = transform.translation(0.0, 0.0, coneZStep);
        scaleTransform = scaleTransform.scale(coneScale, coneScale, 1.0);
        PolyhedralBoundedSolidEulerConstructions::translationalSweepExtrudeFacePlanar(
            solid, solid->findFace(1), transform.multiply(scaleTransform));
        prevRadius = nextRadius;
    }

    closeTopFaceToApex(solid, nsides, baseLength + headLength);

    return solid;
}

const Class Arrow::CLASS = { "Arrow", &Solid::CLASS };

const Class *
Arrow::getClass() const
{
    return &CLASS;
}
