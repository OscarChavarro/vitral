#ifndef __SIMPLE_BODY__
#define __SIMPLE_BODY__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/Entity.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
class Geometry;

#include "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingSink.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingSupport.h"
class Ray;
class RayHit;
class Image;
class RGBImageUncompressed;
class NormalMap;
class SimpleMaterial;
class Material;

class SimpleBody : public Entity {
protected:
    Geometry* geometry;
private:
    /// False when the geometry is shared (see `setGeometryReference`)
    BOOLEAN ownsGeometry;
    BOOLEAN geometryIsSphere;
    Vector3Dd position;
    Vector3Dd scale;
    Matrix4x4d rotation;
    Matrix4x4d rotationInverse;
    Quaterniond rotationQuaternion;
    Quaterniond rotationInverseQuaternion;
    Vector3Dd inverseScale;
    BOOLEAN hasInvertibleScale;
    BOOLEAN hasIdentityRotation;
    BOOLEAN hasUnitScale;
    BOOLEAN hasZeroTranslation;
    BOOLEAN hasTranslationOnlyTransform;
    BOOLEAN hasIdentityTransform;

    SimpleMaterial* material;
    /// False when the material is shared (see `setMaterialReference`)
    BOOLEAN ownsMaterial;
    Image* texture;
    NormalMap* normalMap;
    RGBImageUncompressed* normalMapRgb;

    String name;
    long long modificationVersion;

protected:
    void markModified();
private:
    void updateTransformFlags();
    static BOOLEAN isIdentityRotation(const Matrix4x4d& matrix);
    BOOLEAN doIntersectionWithTranslationOnly(const Ray& inOutRay, RayHit* outHit, int requiredDetailMask) const;
    BOOLEAN doIntersectionWithTranslationOnlySphereFastPath(const Ray& inOutRay, RayHit* outHit) const;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual RayCrossingSupport::Value emitRayCrossings(const Ray&, const RayCrossingQuery&, RayCrossingSink&) const { return RayCrossingSupport::Unsupported; }
    virtual BOOLEAN supportsDecomposedPlacement() const { return TRUE; }
    SimpleBody();
    virtual ~SimpleBody();

    const String& getName() const;
    long long getModificationVersion() const;
    void setName(const String& n);

    virtual Geometry* getGeometry() const;

    /**
    Sets the geometry, taking its ownership (it is deleted with this body or
    when replaced).
    @param g new geometry, or null
    */
    virtual void setGeometry(Geometry* g);

    /**
    Sets a geometry shared with other owners (i.e. a primitive instanced by
    several bodies of a gizmo, as done by Java code with garbage collected
    references): this body does not delete it.
    @param g shared geometry, or null
    */
    virtual void setGeometryReference(Geometry* g);

    virtual Matrix4x4d getRotation() const;
    virtual void setRotation(const Matrix4x4d& rotation);
    virtual Matrix4x4d getRotationInverse() const;
    virtual void setRotationInverse(const Matrix4x4d& rotationInverse);

    virtual SimpleMaterial* getMaterial() const;
    virtual Material* getSurfaceMaterial() const;

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

    virtual Vector3Dd getPosition() const;
    virtual void setPosition(const Vector3Dd& p);

    virtual Vector3Dd getScale() const;
    virtual Matrix4x4d getTransformationMatrix() const;
    virtual void setScale(const Vector3Dd& s);

    Ray* doIntersectionFirstHit(const Ray& inRay) const;
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inOutRay, RayHit* outHit) const;
    int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p) const;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) const;
};

#endif
