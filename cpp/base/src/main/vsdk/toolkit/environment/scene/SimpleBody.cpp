#include "SimpleBody.h"

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

SimpleBody::SimpleBody()
    : geometry(0), position(0, 0, 0), scale(1, 1, 1), rotation(), rotationInverse(),
      rotationQuaternion(rotation.exportToQuaternion()), rotationInverseQuaternion(rotationInverse.exportToQuaternion()),
      inverseScale(1, 1, 1), hasInvertibleScale(true),
      globalMaterial(0), globalTextureMap(0), globalNormalMap(0), globalNormalMapRgb(0),
      name(""), modificationVersion(0)
{
}

void SimpleBody::markModified() { modificationVersion++; }
const std::string& SimpleBody::getName() const { return name; }
long long SimpleBody::getModificationVersion() const { return modificationVersion; }
void SimpleBody::setName(const std::string& n) { name = n; markModified(); }

Geometry* SimpleBody::getGeometry() const { return geometry; }
void SimpleBody::setGeometry(Geometry* g) { geometry = g; markModified(); }

Matrix4x4d SimpleBody::getRotation() const { return rotation; }
void SimpleBody::setRotation(const Matrix4x4d& r)
{
    Matrix4x4d sanitized = r.withoutTranslation();
    rotationQuaternion = sanitized.exportToQuaternion().normalized();
    rotation = sanitized;
    rotationInverseQuaternion = rotationQuaternion.conjugated();
    rotationInverse = Matrix4x4d().importFromQuaternion(rotationInverseQuaternion);
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
    markModified();
}

SimpleMaterial* SimpleBody::getMaterial() const { return globalMaterial; }
void SimpleBody::setMaterial(SimpleMaterial* m) { globalMaterial = m; markModified(); }
Image* SimpleBody::getTexture() const { return globalTextureMap; }
void SimpleBody::setTexture(Image* in) { globalTextureMap = in; markModified(); }
NormalMap* SimpleBody::getNormalMap() const { return globalNormalMap; }
RGBImageUncompressed* SimpleBody::getNormalMapRgb() const { return globalNormalMapRgb; }

void SimpleBody::setNormalMap(NormalMap* in)
{
    globalNormalMap = in;
    globalNormalMapRgb = 0;
    markModified();
}

Vector3Dd SimpleBody::getPosition() const { return position; }
void SimpleBody::setPosition(const Vector3Dd& p) { position = p; markModified(); }
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

bool SimpleBody::doIntersection(const Ray& inOutRay, RayHit* outHit) const
{
    if ( geometry == 0 || !hasInvertibleScale ) {
        return false;
    }

    Vector3Dd localOrigin = inOutRay.origin().subtract(position);
    localOrigin = Vector3Dd(localOrigin.x() * inverseScale.x(), localOrigin.y() * inverseScale.y(), localOrigin.z() * inverseScale.z());
    localOrigin = rotationInverse.multiply(localOrigin);

    Vector3Dd localDirection = Vector3Dd(
        inOutRay.direction().x() * inverseScale.x(),
        inOutRay.direction().y() * inverseScale.y(),
        inOutRay.direction().z() * inverseScale.z());
    localDirection = rotationInverse.multiply(localDirection).normalized();

    Ray localRay(localOrigin, localDirection, inOutRay.t());
    RayHit localHit = outHit != 0 ? RayHit(*outHit) : RayHit();

    if ( !geometry->doIntersection(localRay, &localHit) ) {
        return false;
    }

    if ( outHit != 0 && localHit.ray() != 0 ) {
        Ray localHitRay = *localHit.ray();
        Vector3Dd hitPoint = localHitRay.origin().add(localHitRay.direction().multiply(localHitRay.t()));
        Vector3Dd worldPoint = rotation.multiply(Vector3Dd(hitPoint.x() * scale.x(), hitPoint.y() * scale.y(), hitPoint.z() * scale.z())).add(position);

        double worldT = worldPoint.subtract(inOutRay.origin()).length();
        Ray worldRay(inOutRay.origin(), inOutRay.direction(), worldT);
        outHit->setRay(worldRay);

        if ( outHit->needsAnySurfaceData() ) {
            doExtraInformation(worldRay, worldT, outHit);
        }
    }

    return true;
}

int SimpleBody::computeQuantitativeInvisibility(const Vector3Dd&, const Vector3Dd&) const
{
    return 0;
}

void SimpleBody::doExtraInformation(const Ray&, double, RayHit* outData) const
{
    if ( outData == 0 ) return;
    outData->material = globalMaterial;
    outData->texture = globalTextureMap;
    outData->normalMap = globalNormalMap;
}
