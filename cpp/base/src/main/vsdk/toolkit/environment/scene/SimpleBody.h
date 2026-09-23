#ifndef __SIMPLE_BODY__
#define __SIMPLE_BODY__

#include "vsdk/toolkit/common/Entity.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
class Geometry;
class Ray;
class RayHit;
class Image;
class RGBImageUncompressed;
class NormalMap;
class SimpleMaterial;

class SimpleBody : public Entity {
private:
    Geometry* geometry;
    /// False when the geometry is shared (see `setGeometryReference`)
    bool ownsGeometry;
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

    SimpleMaterial* material;
    /// False when the material is shared (see `setMaterialReference`)
    bool ownsMaterial;
    Image* texture;
    NormalMap* normalMap;
    RGBImageUncompressed* normalMapRgb;

    java::String name;
    long long modificationVersion;

    void markModified();
    void updateTransformFlags();
    static bool isIdentityRotation(const Matrix4x4d& matrix);
    bool doIntersectionWithTranslationOnly(const Ray& inOutRay, RayHit* outHit, int requiredDetailMask) const;
    bool doIntersectionWithTranslationOnlySphereFastPath(const Ray& inOutRay, RayHit* outHit) const;

public:
    SimpleBody();
    virtual ~SimpleBody();

    const java::String& getName() const;
    long long getModificationVersion() const;
    void setName(const java::String& n);

    Geometry* getGeometry() const;

    /**
    Sets the geometry, taking its ownership (it is deleted with this body or
    when replaced).
    @param g new geometry, or null
    */
    void setGeometry(Geometry* g);

    /**
    Sets a geometry shared with other owners (i.e. a primitive instanced by
    several bodies of a gizmo, as done by Java code with garbage collected
    references): this body does not delete it.
    @param g shared geometry, or null
    */
    void setGeometryReference(Geometry* g);

    Matrix4x4d getRotation() const;
    void setRotation(const Matrix4x4d& rotation);
    Matrix4x4d getRotationInverse() const;
    void setRotationInverse(const Matrix4x4d& rotationInverse);

    SimpleMaterial* getMaterial() const;

    /**
    Sets the material, taking its ownership (it is deleted with this body or
    when replaced).
    @param m new material, or null
    */
    void setMaterial(SimpleMaterial* m);

    /**
    Sets a material shared with other owners: this body does not delete it.
    @param m shared material, or null
    */
    void setMaterialReference(SimpleMaterial* m);

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

    Ray* doIntersectionFirstHit(const Ray& inRay) const;
    bool doIntersectionFirstHit(const Ray& inOutRay, RayHit* outHit) const;
    int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p) const;
    void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) const;
};

#endif
