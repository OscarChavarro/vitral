#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLEBODY_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLEBODY_H__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/lang/String.h"

#include "java/lang/String.h"
#include "java/lang/String.h"

class Geometry;
class Ray;
class RayHit;
class Image;
class RGBImageUncompressed;
class NormalMap;
class SimpleMaterial;

class SimpleBody {
private:
    Geometry* geometry;
    bool geometryIsSphere;
    Vector3Dd position;
    Vector3Dd scale;
    Matrix4x4d rotation;
    Matrix4x4d rotationInverse;
    Quaterniond rotationQuaternion;
    Quaterniond rotationInverseQuaternion;
    Vector3Dd inverseScale;
    bool hasInvertibleScale;
    bool hasIdentityRotation;
    bool hasUnitScale;
    bool hasZeroTranslation;
    bool hasTranslationOnlyTransform;
    bool hasIdentityTransform;

    SimpleMaterial* globalMaterial;
    Image* globalTextureMap;
    NormalMap* globalNormalMap;
    RGBImageUncompressed* globalNormalMapRgb;

    java::String name;
    long long modificationVersion;

    void markModified();
    void updateTransformFlags();
    static bool isIdentityRotation(const Matrix4x4d& matrix);
    bool doIntersectionWithTranslationOnlySphereFastPath(const Ray& inOutRay, RayHit* outHit) const;

public:
    SimpleBody();
    virtual ~SimpleBody();

    const java::String& getName() const;
    long long getModificationVersion() const;
    void setName(const java::String& n);

    Geometry* getGeometry() const;
    void setGeometry(Geometry* g);

    Matrix4x4d getRotation() const;
    void setRotation(const Matrix4x4d& rotation);
    Matrix4x4d getRotationInverse() const;
    void setRotationInverse(const Matrix4x4d& rotationInverse);

    SimpleMaterial* getMaterial() const;
    void setMaterial(SimpleMaterial* m);

    Image* getTexture() const;
    void setTexture(Image* in);

    NormalMap* getNormalMap() const;
    RGBImageUncompressed* getNormalMapRgb() const;
    void setNormalMap(NormalMap* in);

    Vector3Dd getPosition() const;
    void setPosition(const Vector3Dd& p);

    Vector3Dd getScale() const;
    Matrix4x4d getTransformationMatrix() const;
    void setScale(const Vector3Dd& s);

    Ray* doIntersection(const Ray& inRay) const;
    bool doIntersection(const Ray& inOutRay, RayHit* outHit) const;
    int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p) const;
    void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) const;
};

#endif
