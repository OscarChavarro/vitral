#ifndef __SIMPLE_BODY__
#define __SIMPLE_BODY__

#include "vsdk/toolkit/common/VSDK.h"

#include "vsdk/toolkit/environment/scene/SceneSolid.h"
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
#include "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingSink.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingSupport.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryContainment.h"
#include "vsdk/toolkit/environment/scene/AffinePlacement.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryDistanceStatus.h"
class RaySolidClassification;
class GeometryBounds;

/**
The VITRAL scene body: a `SceneSolid` whose placement is edited either as
position, rotation and scale or as an arbitrary affine map, with VITRAL
appearance (material, texture, normal map) and first hit fast paths for
identity and translation-only placements.
*/
class SimpleBody : public SceneSolid {
private:
    AffinePlacement affinePlacement;
    BOOLEAN explicitAffinePlacement;
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
    /// False when the texture is shared (see `setTextureReference`)
    BOOLEAN ownsTexture;
    NormalMap* normalMap;
    /// False when the normal map is shared (see `setNormalMapReference`)
    BOOLEAN ownsNormalMap;
    RGBImageUncompressed* normalMapRgb;


protected:
    /// Only plain SimpleBody instances; subclasses opt in themselves
    virtual BOOLEAN denotesPlacedGeometry() const;
private:
    class QueryPlacementStatus { public: enum Value { Valid, Invalid, Indeterminate }; };
    QueryPlacementStatus::Value queryPlacementStatus() const;
    void updateTransformFlags();
    void updateDecomposedPlacement();
    BOOLEAN doIntersectionWithAffinePlacement(const Ray& ray, RayHit* hit) const;
    void doExtraInformationWithAffinePlacement(const Ray& ray, double t, RayHit* hit) const;
    static BOOLEAN isIdentityRotation(const Matrix4x4d& matrix);
    BOOLEAN doIntersectionWithTranslationOnly(const Ray& inOutRay, RayHit* outHit, int requiredDetailMask) const;
    BOOLEAN doIntersectionWithTranslationOnlySphereFastPath(const Ray& inOutRay, RayHit* outHit) const;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual BOOLEAN supportsDecomposedPlacement() const { return !explicitAffinePlacement; }
    SimpleBody();
    virtual ~SimpleBody();

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
    /**
    Sets the texture, taking its ownership (it is deleted with this body or
    when replaced).
    @param in new texture, or null
    */
    void setTexture(Image* in);
    /**
    Sets a texture shared with other owners, such as a scene resource
    (`SimpleScene::addImage`): this body does not delete it.
    @param in shared texture, or null
    */
    void setTextureReference(Image* in);

    NormalMap* getNormalMap() const;
    RGBImageUncompressed* getNormalMapRgb() const;
    /**
    Sets the normal map, taking its ownership.
    @param in new normal map, or null
    */
    void setNormalMap(NormalMap* in);
    /**
    Sets a normal map shared with other owners, such as a scene resource
    (`SimpleScene::addNormalMap`): this body does not delete it. The body
    keeps its own RGB encoding of the map.
    @param in shared normal map, or null
    */
    void setNormalMapReference(NormalMap* in);

    virtual Vector3Dd getPosition() const;
    virtual void setPosition(const Vector3Dd& p);

    virtual Vector3Dd getScale() const;
    virtual Matrix4x4d getTransformationMatrix() const;
    virtual Matrix4x4d getAffinePlacement() const;
    virtual AffinePlacementStatus::Value setAffinePlacement(const Matrix4x4d& forward, const Matrix4x4d& inverse);
    // Reset to identity and re-enable decomposed editing.
    virtual void resetAffinePlacement();
    virtual AffinePlacementStatus::Value queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const;
    virtual void setScale(const Vector3Dd& s);

    Ray* doIntersectionFirstHit(const Ray& inRay) const;
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inOutRay, RayHit* outHit) const;
    int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p) const;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) const;
};

#endif
