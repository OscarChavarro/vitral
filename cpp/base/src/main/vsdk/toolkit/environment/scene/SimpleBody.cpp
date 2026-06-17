#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "java/lang/String.h"

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/SurfaceRayIntersection.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/media/Image.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "java/lang/String.h"

#include <cmath>
#include <cfloat>
#include "java/lang/String.h"

SimpleBody::SimpleBody()
    : geometry(0), geometryIsSphere(false), position(0, 0, 0), scale(1, 1, 1), rotation(), rotationInverse(),
      rotationQuaternion(rotation.exportToQuaternion()), rotationInverseQuaternion(rotationInverse.exportToQuaternion()),
      inverseScale(1, 1, 1), hasInvertibleScale(true),
      hasIdentityRotation(true), hasUnitScale(true), hasZeroTranslation(true),
      hasTranslationOnlyTransform(true), hasIdentityTransform(true),
      globalMaterial(0), globalTextureMap(0), globalNormalMap(0), globalNormalMapRgb(0),
      name(""), modificationVersion(0)
{
}

SimpleBody::~SimpleBody()
{
    if ( geometry != 0 ) {
        delete geometry;
        geometry = 0;
    }
    if ( globalMaterial != 0 ) {
        delete globalMaterial;
        globalMaterial = 0;
    }
    if ( globalTextureMap != 0 ) {
        delete globalTextureMap;
        globalTextureMap = 0;
    }
    if ( globalNormalMap != 0 ) {
        delete globalNormalMap;
        globalNormalMap = 0;
    }
}

void SimpleBody::markModified() { modificationVersion++; }

bool SimpleBody::isIdentityRotation(const Matrix4x4d& matrix)
{
    return
        std::abs(matrix.get(0, 0) - 1.0) <= VSDK::EPSILON &&
        std::abs(matrix.get(0, 1)) <= VSDK::EPSILON &&
        std::abs(matrix.get(0, 2)) <= VSDK::EPSILON &&
        std::abs(matrix.get(1, 0)) <= VSDK::EPSILON &&
        std::abs(matrix.get(1, 1) - 1.0) <= VSDK::EPSILON &&
        std::abs(matrix.get(1, 2)) <= VSDK::EPSILON &&
        std::abs(matrix.get(2, 0)) <= VSDK::EPSILON &&
        std::abs(matrix.get(2, 1)) <= VSDK::EPSILON &&
        std::abs(matrix.get(2, 2) - 1.0) <= VSDK::EPSILON;
}

void SimpleBody::updateTransformFlags()
{
    hasIdentityRotation = isIdentityRotation(rotation);
    hasUnitScale =
        std::abs(scale.x() - 1.0) <= VSDK::EPSILON &&
        std::abs(scale.y() - 1.0) <= VSDK::EPSILON &&
        std::abs(scale.z() - 1.0) <= VSDK::EPSILON;
    hasZeroTranslation =
        std::abs(position.x()) <= VSDK::EPSILON &&
        std::abs(position.y()) <= VSDK::EPSILON &&
        std::abs(position.z()) <= VSDK::EPSILON;
    hasTranslationOnlyTransform = hasIdentityRotation && hasUnitScale;
    hasIdentityTransform = hasTranslationOnlyTransform && hasZeroTranslation;
}

const java::String& SimpleBody::getName() const { return name; }
long long SimpleBody::getModificationVersion() const { return modificationVersion; }
void SimpleBody::setName(const java::String& n) { name = n; markModified(); }

Geometry* SimpleBody::getGeometry() const { return geometry; }
void SimpleBody::setGeometry(Geometry* g)
{
    if ( geometry != g && geometry != 0 ) {
        delete geometry;
    }
    geometry = g;
    geometryIsSphere = dynamic_cast<Sphere*>(geometry) != 0;
    markModified();
}

Matrix4x4d SimpleBody::getRotation() const { return rotation; }
void SimpleBody::setRotation(const Matrix4x4d& r)
{
    Matrix4x4d sanitized = r.withoutTranslation();
    rotationQuaternion = sanitized.exportToQuaternion().normalized();
    rotation = sanitized;
    rotationInverseQuaternion = rotationQuaternion.conjugated();
    rotationInverse = Matrix4x4d().importFromQuaternion(rotationInverseQuaternion);
    updateTransformFlags();
    markModified();
}

Matrix4x4d SimpleBody::getRotationInverse() const { return rotationInverse; }
void SimpleBody::setRotationInverse(const Matrix4x4d& ri)
{
    Matrix4x4d sanitized = ri.withoutTranslation();
    rotationInverseQuaternion = sanitized.exportToQuaternion().normalized();
    rotationInverse = sanitized;
    rotationQuaternion = rotationInverseQuaternion.conjugated();
    rotation = Matrix4x4d().importFromQuaternion(rotationQuaternion);
    updateTransformFlags();
    markModified();
}

SimpleMaterial* SimpleBody::getMaterial() const { return globalMaterial; }
void SimpleBody::setMaterial(SimpleMaterial* m)
{
    if ( globalMaterial != m && globalMaterial != 0 ) {
        delete globalMaterial;
    }
    globalMaterial = m;
    markModified();
}
Image* SimpleBody::getTexture() const { return globalTextureMap; }
void SimpleBody::setTexture(Image* in)
{
    if ( globalTextureMap != in && globalTextureMap != 0 ) {
        delete globalTextureMap;
    }
    globalTextureMap = in;
    markModified();
}
NormalMap* SimpleBody::getNormalMap() const { return globalNormalMap; }
RGBImageUncompressed* SimpleBody::getNormalMapRgb() const { return globalNormalMapRgb; }

void SimpleBody::setNormalMap(NormalMap* in)
{
    if ( globalNormalMap != in && globalNormalMap != 0 ) {
        delete globalNormalMap;
    }
    globalNormalMap = in;
    globalNormalMapRgb = 0;
    markModified();
}

Vector3Dd SimpleBody::getPosition() const { return position; }
void SimpleBody::setPosition(const Vector3Dd& p) { position = p; updateTransformFlags(); markModified(); }
Vector3Dd SimpleBody::getScale() const { return scale; }

Matrix4x4d SimpleBody::getTransformationMatrix() const
{
    Matrix4x4d scaleMatrix = Matrix4x4d().scale(scale);
    Matrix4x4d translateMatrix = Matrix4x4d().translation(position);
    return translateMatrix.multiply(rotation.multiply(scaleMatrix));
}

void SimpleBody::setScale(const Vector3Dd& s)
{
    scale = s;
    hasInvertibleScale =
        std::abs(scale.x()) > VSDK::EPSILON &&
        std::abs(scale.y()) > VSDK::EPSILON &&
        std::abs(scale.z()) > VSDK::EPSILON;

    if ( hasInvertibleScale ) {
        inverseScale = Vector3Dd(1.0 / scale.x(), 1.0 / scale.y(), 1.0 / scale.z());
    }
    else {
        inverseScale = Vector3Dd();
    }
    updateTransformFlags();
    markModified();
}

Ray* SimpleBody::doIntersection(const Ray& inRay) const
{
    RayHit hit;
    if ( !doIntersection(inRay, &hit) || hit.ray() == 0 ) {
        return 0;
    }
    return new Ray(*hit.ray());
}

bool SimpleBody::doIntersectionWithTranslationOnlySphereFastPath(const Ray& inOutRay, RayHit* outHit) const
{
    const Sphere* sphere = static_cast<const Sphere*>(geometry);
    const Vector3Dd& origin = inOutRay.getOrigin();
    const Vector3Dd& direction = inOutRay.getDirection();
    const double dx = position.x() - origin.x();
    const double dy = position.y() - origin.y();
    const double dz = position.z() - origin.z();
    const double projection = direction.x() * dx + direction.y() * dy + direction.z() * dz;
    const double discriminant =
        sphere->getRadiusSquared() + projection * projection - dx * dx - dy * dy - dz * dz;

    if ( discriminant < 0 ) {
        return false;
    }

    const double t = projection - std::sqrt(discriminant);
    if ( t < 0 ) {
        return false;
    }

    if ( outHit != 0 ) {
        if ( outHit->shouldStoreRay() ) {
            outHit->setRay(inOutRay.withT(t));
        }
        else {
            outHit->setHitDistance(t);
        }
    }
    return true;
}

bool SimpleBody::doIntersection(const Ray& inOutRay, RayHit* outHit) const
{
    if ( geometry == 0 || !hasInvertibleScale ) {
        return false;
    }

    const int requestedDetailMask = outHit != 0 ? outHit->requiredDetailMask() : RayHit::DETAIL_NONE;

    if ( hasTranslationOnlyTransform && requestedDetailMask == RayHit::DETAIL_NONE && geometryIsSphere ) {
        return doIntersectionWithTranslationOnlySphereFastPath(inOutRay, outHit);
    }

    if ( hasIdentityTransform ) {
        return SurfaceRayIntersection::doIntersection(geometry, inOutRay, outHit);
    }

    if ( hasTranslationOnlyTransform ) {
        return doIntersectionWithTranslationOnly(inOutRay, outHit, requestedDetailMask);
    }

    Vector3Dd translatedOrigin = inOutRay.getOrigin().subtract(position);
    Vector3Dd rotatedOrigin = rotationInverseQuaternion.rotate(translatedOrigin);
    Vector3Dd localOrigin(
        rotatedOrigin.x() * inverseScale.x(),
        rotatedOrigin.y() * inverseScale.y(),
        rotatedOrigin.z() * inverseScale.z());

    Vector3Dd rotatedDirection = rotationInverseQuaternion.rotate(inOutRay.getDirection());
    Vector3Dd localDirection(
        rotatedDirection.x() * inverseScale.x(),
        rotatedDirection.y() * inverseScale.y(),
        rotatedDirection.z() * inverseScale.z());
    const double localDirectionLength = localDirection.length();
    if ( localDirectionLength <= VSDK::EPSILON ) {
        return false;
    }
    localDirection = localDirection.multiply(1.0 / localDirectionLength);

    double localRayT = inOutRay.getT();
    if ( localRayT >= DBL_MAX / localDirectionLength ) {
        localRayT = DBL_MAX;
    }
    else {
        localRayT *= localDirectionLength;
    }

    Ray localRay(localOrigin, localDirection, localRayT);
    const bool requestedStoreRay = outHit != 0 ? outHit->shouldStoreRay() : false;

    if ( outHit != 0 ) {
        outHit->setStoreRay(false);
        outHit->resetForDistanceOnly();
        if ( !SurfaceRayIntersection::doIntersection(geometry, localRay, outHit) ) {
            outHit->setStoreRay(requestedStoreRay);
            outHit->setRequiredDetailMask(requestedDetailMask);
            return false;
        }
    }
    else {
        RayHit localHitStorage;
        localHitStorage.setStoreRay(false);
        localHitStorage.resetForDistanceOnly();
        if ( !SurfaceRayIntersection::doIntersection(geometry, localRay, &localHitStorage) ) {
            return false;
        }
    }

    if ( outHit != 0 ) {
        double localHitT;
        if ( outHit->ray() != 0 ) {
            localHitT = outHit->ray()->getT();
        }
        else if ( outHit->hasHitDistance() ) {
            localHitT = outHit->hitDistance();
        }
        else {
            return false;
        }

        double worldT = localHitT / localDirectionLength;

        outHit->setStoreRay(requestedStoreRay);
        outHit->setRequiredDetailMask(requestedDetailMask);
        if ( requestedStoreRay || requestedDetailMask != RayHit::DETAIL_NONE ) {
            Ray worldRay(inOutRay.getOrigin(), inOutRay.getDirection(), worldT);
            outHit->setRay(worldRay);
            if ( requestedDetailMask != RayHit::DETAIL_NONE ) {
                doExtraInformation(worldRay, worldT, outHit);
            }
        }
        else {
            outHit->setHitDistance(worldT);
        }
    }
    return true;
}

bool SimpleBody::doIntersectionWithTranslationOnly(
    const Ray& inOutRay,
    RayHit* outHit,
    int requiredDetailMask) const
{
    Ray localRay(inOutRay.getOrigin().subtract(position), inOutRay.getDirection(), inOutRay.getT());

    RayHit localHitStorage;
    RayHit* hit = outHit != 0 ? outHit : &localHitStorage;

    if ( requiredDetailMask == RayHit::DETAIL_NONE ) {
        hit->resetForDistanceOnly();
    }
    else {
        hit->reset(requiredDetailMask);
    }

    if ( !SurfaceRayIntersection::doIntersection(geometry, localRay, hit) ) {
        return false;
    }

    double localHitT;
    if ( hit->ray() != 0 ) {
        localHitT = hit->ray()->getT();
    }
    else if ( hit->hasHitDistance() ) {
        localHitT = hit->hitDistance();
    }
    else {
        return false;
    }

    if ( outHit != 0 ) {
        if ( outHit->shouldStoreRay() || outHit->needsAnySurfaceData() ) {
            outHit->setRay(inOutRay.withT(localHitT));
        }
        else {
            outHit->setHitDistance(localHitT);
        }
        if ( outHit->needsPoint() ) {
            outHit->p = hit->p.add(position);
        }
        if ( outHit->needsNormal() ) {
            outHit->n = hit->n;
        }
        if ( outHit->needsTextureCoordinates() ) {
            outHit->u = hit->u;
            outHit->v = hit->v;
        }
        if ( outHit->needsTangent() ) {
            outHit->t = hit->t;
        }
        outHit->material = hit->material;
        outHit->texture = hit->texture;
        outHit->normalMap = hit->normalMap;
    }

    return true;
}

int SimpleBody::computeQuantitativeInvisibility(
    const Vector3Dd& origin,
    const Vector3Dd& p) const
{
    if ( geometry == 0 || !hasInvertibleScale ) {
        return 0;
    }

    Vector3Dd translatedOrigin = origin.subtract(position);
    Vector3Dd rotatedOrigin = rotationInverseQuaternion.rotate(translatedOrigin);
    Vector3Dd myOrigin(
        rotatedOrigin.x() * inverseScale.x(),
        rotatedOrigin.y() * inverseScale.y(),
        rotatedOrigin.z() * inverseScale.z());

    Vector3Dd translatedPoint = p.subtract(position);
    Vector3Dd rotatedPoint = rotationInverseQuaternion.rotate(translatedPoint);
    Vector3Dd myPoint(
        rotatedPoint.x() * inverseScale.x(),
        rotatedPoint.y() * inverseScale.y(),
        rotatedPoint.z() * inverseScale.z());

    return geometry->computeQuantitativeInvisibility(myOrigin, myPoint);
}

void SimpleBody::doExtraInformation(const Ray&, double, RayHit* outData) const
{
    if ( outData == 0 || geometry == 0 || !hasInvertibleScale ) return;

    const Ray* worldRayPtr = outData->ray();
    if ( worldRayPtr == 0 ) {
        outData->material = globalMaterial;
        outData->texture = globalTextureMap;
        outData->normalMap = globalNormalMap;
        return;
    }

    const Ray worldRay = *worldRayPtr;
    const double worldT = worldRay.getT();

    Vector3Dd translatedOrigin = worldRay.getOrigin().subtract(position);
    Vector3Dd rotatedOrigin = rotationInverseQuaternion.rotate(translatedOrigin);
    Vector3Dd localOrigin(
        rotatedOrigin.x() * inverseScale.x(),
        rotatedOrigin.y() * inverseScale.y(),
        rotatedOrigin.z() * inverseScale.z());

    Vector3Dd rotatedDirection = rotationInverseQuaternion.rotate(worldRay.getDirection());
    Vector3Dd localDirection(
        rotatedDirection.x() * inverseScale.x(),
        rotatedDirection.y() * inverseScale.y(),
        rotatedDirection.z() * inverseScale.z());
    double localDirLength = localDirection.length();
    if ( localDirLength <= VSDK::EPSILON ) {
        outData->material = globalMaterial;
        outData->texture = globalTextureMap;
        outData->normalMap = globalNormalMap;
        return;
    }
    localDirection = localDirection.multiply(1.0 / localDirLength);
    double localT = worldT * localDirLength;
    Ray localRay(localOrigin, localDirection, localT);

    geometry->doExtraInformation(localRay, localT, outData);

    if ( outData->needsPoint() ) {
        Vector3Dd worldPoint = rotation.multiply(Vector3Dd(
            outData->p.x() * scale.x(),
            outData->p.y() * scale.y(),
            outData->p.z() * scale.z())).add(position);
        outData->p = worldPoint;
    }

    if ( outData->needsNormal() ) {
        Vector3Dd worldNormal = rotation.multiply(Vector3Dd(
            outData->n.x() * inverseScale.x(),
            outData->n.y() * inverseScale.y(),
            outData->n.z() * inverseScale.z())).normalized();
        outData->n = worldNormal;
    }

    if ( outData->needsTangent() ) {
        Vector3Dd worldTangent = rotation.multiply(Vector3Dd(
            outData->t.x() * scale.x(),
            outData->t.y() * scale.y(),
            outData->t.z() * scale.z())).normalized();
        outData->t = worldTangent;
    }

    outData->material = globalMaterial;
    outData->texture = globalTextureMap;
    outData->normalMap = globalNormalMap;
    outData->setRay(worldRay.withT(worldT));
}
