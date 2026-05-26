package vsdk.toolkit.environment.scene;

// Java
import java.io.Serial;
import java.util.concurrent.atomic.AtomicLong;

// Vitral
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Quaterniond;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.geometricProcessing.SurfaceRayIntersection;
import vsdk.toolkit.environment.geometry.volume.Sphere;

/**
Represents a scene body composed of geometry plus object-to-world transform
state and visual attributes.

<p>The underlying geometry remains defined in object space around the origin.
`SimpleBody` is responsible for moving rays between world and object space.
*/
public class SimpleBody extends Entity {
    @Serial private static final long serialVersionUID = 20060502L;

    //- Model (1/6): body form ----------------------------------------
    private Geometry geometry;

    //- Model (2/6): body geometric transformations -------------------
    private Vector3Dd position;
    private Vector3Dd scale;
    private Matrix4x4d rotation;
    private Matrix4x4d rotationInverse;
    private Quaterniond rotationQuaternion;
    private Quaterniond rotationInverseQuaternion;
    private Vector3Dd inverseScale;
    private boolean hasInvertibleScale;
    private boolean hasIdentityRotation;
    private boolean hasUnitScale;
    private boolean hasZeroTranslation;
    private boolean hasIdentityTransform;
    private boolean hasTranslationOnlyTransform;

    //- Model (3/6): body visual data ---------------------------------
    private SimpleMaterial globalMaterial;
    private Image globalTextureMap;
    private NormalMap globalNormalMap;
    private RGBImageUncompressed globalNormalMapRgb;

    //- Model (4/6): body physical data -------------------------------

    //- Model (5/6): body structural relationships --------------------

    //- Model (6/6): body semantic data -------------------------------
    private String name;
    private final AtomicLong modificationVersion = new AtomicLong();

    public SimpleBody()
    {
        geometry = null;
        setPosition(new Vector3Dd(0, 0, 0));
        setRotation(new Matrix4x4d());
        setScale(new Vector3Dd(1, 1, 1));
        globalMaterial = new SimpleMaterial();
        globalTextureMap = null;
        globalNormalMap = null;
    }

    /**
    @return application-defined body name, or {@code null}
    */
    public String getName()
    {
        return name;
    }

    public long getModificationVersion()
    {
        return modificationVersion.get();
    }

    private void markModified()
    {
        modificationVersion.incrementAndGet();
    }

    /**
    @param n application-defined body name
    */
    public void setName(String n)
    {
        name = n;
        markModified();
    }

    /**
    @return the body geometry in object space
    */
    public Geometry getGeometry()
    {
        return geometry;
    }

    /**
    @param g geometry in object space
    */
    public void setGeometry(Geometry g)
    {
        geometry = g;
        markModified();
    }

    /**
    @return the cached object-to-world rotation matrix
    */
    public Matrix4x4d getRotation()
    {
        return rotation;
    }

    /**
    Sets the object-to-world rotation and refreshes the cached inverse
    rotation and quaternion forms used during intersection queries.

    @param rotation rigid-body rotation matrix without translation
    */
    public void setRotation(Matrix4x4d rotation)
    {
        Matrix4x4d sanitizedRotation = sanitizeRotationMatrix(rotation);
        Quaterniond cachedRotationQuaternion =
            sanitizedRotation.exportToQuaternion().normalized();

        this.rotation = sanitizedRotation;
        this.rotationQuaternion = cachedRotationQuaternion;
        this.rotationInverseQuaternion = cachedRotationQuaternion.conjugated();
        this.rotationInverse = new Matrix4x4d()
            .importFromQuaternion(rotationInverseQuaternion);
        updateTransformFlags();
        markModified();
    }

    /**
    @return the cached world-to-object rotation matrix
    */
    public Matrix4x4d getRotationInverse()
    {
        return rotationInverse;
    }

    /**
    Sets the world-to-object rotation and refreshes the cached forward
    rotation and quaternion forms used during intersection queries.

    @param rotationInverse rigid-body inverse rotation matrix without translation
    */
    public void setRotationInverse(Matrix4x4d rotationInverse)
    {
        Matrix4x4d sanitizedInverseRotation =
            sanitizeRotationMatrix(rotationInverse);
        Quaterniond cachedInverseRotationQuaternion =
            sanitizedInverseRotation.exportToQuaternion().normalized();

        this.rotationInverse = sanitizedInverseRotation;
        this.rotationInverseQuaternion = cachedInverseRotationQuaternion;
        this.rotationQuaternion = cachedInverseRotationQuaternion.conjugated();
        this.rotation = new Matrix4x4d().importFromQuaternion(rotationQuaternion);
        updateTransformFlags();
        markModified();
    }

    /**
    @return default material used by this body
    */
    public SimpleMaterial getMaterial()
    {
        return globalMaterial;
    }

    /**
    @param m default material used by this body
    */
    public void setMaterial(SimpleMaterial m)
    {
        globalMaterial = m;
        markModified();
    }

    /**
    @return body texture, or {@code null}
    */
    public Image getTexture()
    {
        return globalTextureMap;
    }

    /**
    @param in texture to associate with this body
    */
    public void setTexture(Image in)
    {
        globalTextureMap = in;
        markModified();
    }

    /**
    @return body normal map, or {@code null}
    */
    public NormalMap getNormalMap()
    {
        return globalNormalMap;
    }

    /**
    @return RGB preview image for the current normal map, or {@code null}
    */
    public RGBImageUncompressed getNormalMapRgb()
    {
        return globalNormalMapRgb;
    }

    /**
    @param in normal map to associate with this body
    */
    public void setNormalMap(NormalMap in)
    {
        globalNormalMap = in;
        if ( globalNormalMap != null ) {
            globalNormalMapRgb = globalNormalMap.exportToRgbImage();
        }
        markModified();
    }

    /**
    @return body position in world space
    */
    public Vector3Dd getPosition()
    {
        return position;
    }

    /**
    @param p body position in world space
    */
    public void setPosition(Vector3Dd p)
    {
        position = p;
        updateTransformFlags();
        markModified();
    }

    /**
    @return body scale relative to object space axes
    */
    public Vector3Dd getScale()
    {
        return scale;
    }

    /**
    @return object-to-world matrix with scale, rotation and translation
    */
    public Matrix4x4d getTransformationMatrix()
    {
        Matrix4x4d scaleMatrix = new Matrix4x4d();
        Matrix4x4d translateMatrix = new Matrix4x4d();
        Matrix4x4d multipliedMatrix;
        scaleMatrix = scaleMatrix.scale(scale);
        translateMatrix = translateMatrix.translation(position);
        multipliedMatrix = translateMatrix.multiply(rotation.multiply(scaleMatrix));
        return multipliedMatrix;
    }

    /**
    Sets the body scale and refreshes cached reciprocal values required by
    world-to-object ray conversion.

    <p>Ray queries require every scale component to be non-zero. If any
    component is near zero the body remains visually transformable, but
    intersection queries report no hits because the inverse transform is not
    defined.

    @param s body scale relative to object space axes
    */
    public void setScale(Vector3Dd s)
    {
        scale = s;
        hasInvertibleScale =
            Math.abs(scale.x()) > VSDK.EPSILON &&
            Math.abs(scale.y()) > VSDK.EPSILON &&
            Math.abs(scale.z()) > VSDK.EPSILON;

        if ( hasInvertibleScale ) {
            inverseScale = new Vector3Dd(
                1.0 / scale.x(),
                1.0 / scale.y(),
                1.0 / scale.z());
        }
        else {
            inverseScale = new Vector3Dd();
        }
        updateTransformFlags();
        markModified();
    }

    /**
    Intersects a world-space ray against this body.

    <p>The incoming ray is transformed into object space using the cached
    inverse scale and inverse rotation. The resulting hit information is then
    transformed back into world space.

    @param inRay ray to be tested for intersection
    @return a new ray containing the closest hit distance, or {@code null}
        when the ray misses the body
    */
    public final Ray doIntersection(Ray inRay)
    {
        RayHit hit = new RayHit();
        if ( doIntersection(inRay, hit) ) {
            return hit.ray();
        }
        return null;
    }

    /**
    Intersects a world-space ray against this body and reports the hit in
    world coordinates.

    @param inOutRay world-space ray to test
    @param outHit output structure populated on hit; may be {@code null}
    @return {@code true} when the ray intersects the body
    */
    public final boolean doIntersection(Ray inOutRay, RayHit outHit)
    {
        if ( geometry == null || !hasInvertibleScale ) {
            return false;
        }

        int requiredDetailMask =
            outHit != null ? outHit.requiredDetailMask() : RayHit.DETAIL_NONE;

        if ( hasTranslationOnlyTransform &&
             requiredDetailMask == RayHit.DETAIL_NONE &&
             geometry instanceof Sphere sphere ) {
            return doIntersectionWithTranslationOnlySphereFastPath(
                inOutRay, outHit, sphere);
        }

        if ( hasIdentityTransform ) {
            return SurfaceRayIntersection.doIntersection(geometry, inOutRay, outHit);
        }

        if ( hasTranslationOnlyTransform ) {
            return doIntersectionWithTranslationOnly(
                inOutRay,
                outHit,
                requiredDetailMask);
        }

        Vector3Dd localOrigin = worldPointToObjectSpace(inOutRay.origin());
        Vector3Dd localDirection = worldDirectionToObjectSpace(inOutRay.direction());
        double localDirectionLength = localDirection.length();
        if ( localDirectionLength <= VSDK.EPSILON ) {
            return false;
        }

        Ray localRay = new Ray(
            localOrigin,
            localDirection.multiply(1.0 / localDirectionLength),
            scaleRayParameterForObjectSpace(inOutRay.t(), localDirectionLength));

        RayHit hit = outHit;
        if ( hit == null ) {
            hit = new RayHit(RayHit.DETAIL_NONE);
        }
        else {
            if ( requiredDetailMask == RayHit.DETAIL_NONE ) {
                hit.resetForDistanceOnly();
            }
            else {
                hit.reset(requiredDetailMask);
            }
        }

        // ... and compute doIntersection operation on object's coordinates
        if ( SurfaceRayIntersection.doIntersection(geometry, localRay, hit) ) {
            double localHitT;
            if ( hit.ray() != null ) {
                localHitT = hit.ray().t();
            }
            else if ( hit.hasHitDistance() ) {
                localHitT = hit.hitDistance();
            }
            else {
                return false;
            }
            if ( outHit != null ) {
                double worldT = localHitT / localDirectionLength;
                if ( outHit.shouldStoreRay() || outHit.needsAnySurfaceData() ) {
                    outHit.setRay(inOutRay.withT(worldT));
                }
                else {
                    outHit.setHitDistance(worldT);
                }
                if ( outHit.needsPoint() ) {
                    outHit.p = objectPointToWorldSpace(hit.p);
                }
                if ( outHit.needsNormal() ) {
                    outHit.n = objectNormalToWorldSpace(hit.n);
                }
                if ( outHit.needsTextureCoordinates() ) {
                    outHit.u = hit.u;
                    outHit.v = hit.v;
                }
                if ( outHit.needsTangent() ) {
                    outHit.t = objectTangentToWorldSpace(hit.t);
                }
                outHit.material = hit.material;
                outHit.texture = hit.texture;
                outHit.normalMap = hit.normalMap;
            }
            return true;
        }
        return false;
    }

    /**
    Computes quantitative invisibility in object space by transforming the
    input points from world coordinates into the body's local coordinates.

    @param origin world-space observer position
    @param p world-space point being tested
    @return quantitative invisibility value reported by the underlying geometry
    */
    public int computeQuantitativeInvisibility(Vector3Dd origin, Vector3Dd p)
    {
        if ( geometry == null || !hasInvertibleScale ) {
            return 0;
        }

        Vector3Dd myOrigin = worldPointToObjectSpace(origin);
        Vector3Dd myP = worldPointToObjectSpace(p);

        return geometry.computeQuantitativeInvisibility(myOrigin, myP);
    }

    /**
    Rebuilds detailed hit information for a world-space ray/parameter pair.

    @param inRay world-space ray that previously hit the body
    @param inT hit distance measured along the world-space ray
    @param outInfo structure populated with world-space hit data
    */
    public void doExtraInformation(Ray inRay, double inT,
                                   RayHit outInfo)
    {
        if ( outInfo == null || geometry == null || !hasInvertibleScale ) {
            return;
        }

        if ( hasIdentityTransform ) {
            outInfo.setRay(inRay);
            geometry.doExtraInformation(inRay, inT, outInfo);
            outInfo.setRay(inRay);
            return;
        }

        if ( hasTranslationOnlyTransform ) {
            Ray localRay = inRay.withOrigin(inRay.origin().subtract(position));
            outInfo.setRay(inRay);
            geometry.doExtraInformation(localRay, inT, outInfo);
            outInfo.setRay(inRay);
            if ( outInfo.needsPoint() ) {
                outInfo.p = outInfo.p.add(position);
            }
            return;
        }

        Vector3Dd localOrigin = worldPointToObjectSpace(inRay.origin());
        Vector3Dd localDirection = worldDirectionToObjectSpace(inRay.direction());
        double localDirectionLength = localDirection.length();
        if ( localDirectionLength <= VSDK.EPSILON ) {
            return;
        }

        double localT = scaleRayParameterForObjectSpace(inT, localDirectionLength);
        Ray localRay = new Ray(
            localOrigin,
            localDirection.multiply(1.0 / localDirectionLength),
            localT);

        outInfo.setRay(inRay);
        geometry.doExtraInformation(localRay, localT, outInfo);
        outInfo.setRay(inRay);
        if ( outInfo.needsPoint() ) {
            outInfo.p = objectPointToWorldSpace(outInfo.p);
        }
        if ( outInfo.needsNormal() ) {
            outInfo.n = objectNormalToWorldSpace(outInfo.n);
        }
        if ( outInfo.needsTangent() ) {
            outInfo.t = objectTangentToWorldSpace(outInfo.t);
        }
    }

    private static Matrix4x4d sanitizeRotationMatrix(Matrix4x4d rotationMatrix)
    {
        return rotationMatrix.withoutTranslation();
    }

    private void updateTransformFlags()
    {
        hasIdentityRotation =
            rotation != null && isIdentityRotation(rotation);
        hasUnitScale =
            scale != null &&
            Math.abs(scale.x() - 1.0) <= VSDK.EPSILON &&
            Math.abs(scale.y() - 1.0) <= VSDK.EPSILON &&
            Math.abs(scale.z() - 1.0) <= VSDK.EPSILON;
        hasZeroTranslation =
            position != null &&
            Math.abs(position.x()) <= VSDK.EPSILON &&
            Math.abs(position.y()) <= VSDK.EPSILON &&
            Math.abs(position.z()) <= VSDK.EPSILON;
        hasTranslationOnlyTransform = hasIdentityRotation && hasUnitScale;
        hasIdentityTransform = hasTranslationOnlyTransform && hasZeroTranslation;
    }

    private static boolean isIdentityRotation(Matrix4x4d matrix)
    {
        return
            Math.abs(matrix.get(0, 0) - 1.0) <= VSDK.EPSILON &&
            Math.abs(matrix.get(0, 1)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(0, 2)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(1, 0)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(1, 1) - 1.0) <= VSDK.EPSILON &&
            Math.abs(matrix.get(1, 2)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(2, 0)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(2, 1)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(2, 2) - 1.0) <= VSDK.EPSILON;
    }

    private boolean doIntersectionWithTranslationOnly(
        Ray inOutRay,
        RayHit outHit,
        int requiredDetailMask)
    {
        Ray localRay = inOutRay.withOrigin(inOutRay.origin().subtract(position));

        RayHit hit = outHit;
        if ( hit == null ) {
            hit = new RayHit(RayHit.DETAIL_NONE);
        }
        else {
            if ( requiredDetailMask == RayHit.DETAIL_NONE ) {
                hit.resetForDistanceOnly();
            }
            else {
                hit.reset(requiredDetailMask);
            }
        }

        if ( !SurfaceRayIntersection.doIntersection(geometry, localRay, hit) ) {
            return false;
        }
        double localHitT;
        if ( hit.ray() != null ) {
            localHitT = hit.ray().t();
        }
        else if ( hit.hasHitDistance() ) {
            localHitT = hit.hitDistance();
        }
        else {
            return false;
        }
        if ( outHit != null ) {
            if ( outHit.shouldStoreRay() || outHit.needsAnySurfaceData() ) {
                outHit.setRay(inOutRay.withT(localHitT));
            }
            else {
                outHit.setHitDistance(localHitT);
            }
            if ( outHit.needsPoint() ) {
                outHit.p = hit.p.add(position);
            }
            if ( outHit.needsNormal() ) {
                outHit.n = hit.n;
            }
            if ( outHit.needsTextureCoordinates() ) {
                outHit.u = hit.u;
                outHit.v = hit.v;
            }
            if ( outHit.needsTangent() ) {
                outHit.t = hit.t;
            }
            outHit.material = hit.material;
            outHit.texture = hit.texture;
            outHit.normalMap = hit.normalMap;
        }
        return true;
    }

    private boolean doIntersectionWithTranslationOnlySphereFastPath(
        Ray inOutRay,
        RayHit outHit,
        Sphere sphere)
    {
        double dx = position.x() - inOutRay.origin().x();
        double dy = position.y() - inOutRay.origin().y();
        double dz = position.z() - inOutRay.origin().z();
        Vector3Dd direction = inOutRay.direction();
        double projection =
            direction.x()*dx + direction.y()*dy + direction.z()*dz;
        double discriminant =
            sphere.getRadiusSquared() + projection*projection
            - dx*dx - dy*dy - dz*dz;

        if ( discriminant < 0 ) {
            return false;
        }

        double t = projection - Math.sqrt(discriminant);
        if ( t < 0 ) {
            return false;
        }

        if ( outHit != null ) {
            if ( outHit.shouldStoreRay() ) {
                outHit.setRay(inOutRay.withT(t));
            }
            else {
                outHit.setHitDistance(t);
            }
        }
        return true;
    }

    private static Vector3Dd scaleComponents(Vector3Dd value, Vector3Dd factors)
    {
        return new Vector3Dd(
            value.x() * factors.x(),
            value.y() * factors.y(),
            value.z() * factors.z());
    }

    private static Vector3Dd normalizeIfPossible(Vector3Dd vector)
    {
        if ( vector.length() <= VSDK.EPSILON ) {
            return vector;
        }
        return vector.normalized();
    }

    private static double scaleRayParameterForObjectSpace(
        double worldSpaceRayParameter, double localDirectionLength)
    {
        if ( worldSpaceRayParameter >= Double.MAX_VALUE / localDirectionLength ) {
            return Double.MAX_VALUE;
        }
        return worldSpaceRayParameter * localDirectionLength;
    }

    private Vector3Dd worldPointToObjectSpace(Vector3Dd point)
    {
        Vector3Dd translatedPoint = point.subtract(position);
        Vector3Dd rotatedPoint = rotationInverseQuaternion.rotate(translatedPoint);
        return scaleComponents(rotatedPoint, inverseScale);
    }

    private Vector3Dd worldDirectionToObjectSpace(Vector3Dd direction)
    {
        Vector3Dd rotatedDirection = rotationInverseQuaternion.rotate(direction);
        return scaleComponents(rotatedDirection, inverseScale);
    }

    private Vector3Dd objectPointToWorldSpace(Vector3Dd point)
    {
        Vector3Dd scaledPoint = scaleComponents(point, scale);
        return rotationQuaternion.rotate(scaledPoint).add(position);
    }

    private Vector3Dd objectNormalToWorldSpace(Vector3Dd normal)
    {
        Vector3Dd scaledNormal = scaleComponents(normal, inverseScale);
        return normalizeIfPossible(rotationQuaternion.rotate(scaledNormal));
    }

    private Vector3Dd objectTangentToWorldSpace(Vector3Dd tangent)
    {
        Vector3Dd scaledTangent = scaleComponents(tangent, scale);
        return normalizeIfPossible(rotationQuaternion.rotate(scaledTangent));
    }
}
