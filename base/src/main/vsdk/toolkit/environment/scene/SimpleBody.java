package vsdk.toolkit.environment.scene;

// Java
import java.io.Serial;

// Vitral
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Quaternion;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.RayHit;

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
    private Vector3D position;
    private Vector3D scale;
    private Matrix4x4 rotation;
    private Matrix4x4 rotationInverse;
    private Quaternion rotationQuaternion;
    private Quaternion rotationInverseQuaternion;
    private Vector3D inverseScale;
    private boolean hasInvertibleScale;

    //- Model (3/6): body visual data ---------------------------------
    private Material globalMaterial;
    private Image globalTextureMap;
    private NormalMap globalNormalMap;
    private RGBImage globalNormalMapRgb;

    //- Model (4/6): body physical data -------------------------------

    //- Model (5/6): body structural relationships --------------------

    //- Model (6/6): body semantic data -------------------------------
    private String name;

    public SimpleBody()
    {
        geometry = null;
        position = new Vector3D(0, 0, 0);
        setRotation(new Matrix4x4());
        setScale(new Vector3D(1, 1, 1));
        globalMaterial = new Material();
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

    /**
    @param n application-defined body name
    */
    public void setName(String n)
    {
        name = n;
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
    }

    /**
    @return the cached object-to-world rotation matrix
    */
    public Matrix4x4 getRotation()
    {
        return rotation;
    }

    /**
    Sets the object-to-world rotation and refreshes the cached inverse
    rotation and quaternion forms used during intersection queries.

    @param rotation rigid-body rotation matrix without translation
    */
    public void setRotation(Matrix4x4 rotation)
    {
        Matrix4x4 sanitizedRotation = sanitizeRotationMatrix(rotation);
        Quaternion cachedRotationQuaternion =
            sanitizedRotation.exportToQuaternion().normalized();

        this.rotation = sanitizedRotation;
        this.rotationQuaternion = cachedRotationQuaternion;
        this.rotationInverseQuaternion = cachedRotationQuaternion.conjugated();
        this.rotationInverse = new Matrix4x4()
            .importFromQuaternion(rotationInverseQuaternion);
    }

    /**
    @return the cached world-to-object rotation matrix
    */
    public Matrix4x4 getRotationInverse()
    {
        return rotationInverse;
    }

    /**
    Sets the world-to-object rotation and refreshes the cached forward
    rotation and quaternion forms used during intersection queries.

    @param rotationInverse rigid-body inverse rotation matrix without translation
    */
    public void setRotationInverse(Matrix4x4 rotationInverse)
    {
        Matrix4x4 sanitizedInverseRotation =
            sanitizeRotationMatrix(rotationInverse);
        Quaternion cachedInverseRotationQuaternion =
            sanitizedInverseRotation.exportToQuaternion().normalized();

        this.rotationInverse = sanitizedInverseRotation;
        this.rotationInverseQuaternion = cachedInverseRotationQuaternion;
        this.rotationQuaternion = cachedInverseRotationQuaternion.conjugated();
        this.rotation = new Matrix4x4().importFromQuaternion(rotationQuaternion);
    }

    /**
    @return default material used by this body
    */
    public Material getMaterial()
    {
        return globalMaterial;
    }

    /**
    @param m default material used by this body
    */
    public void setMaterial(Material m)
    {
        globalMaterial = m;
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
    public RGBImage getNormalMapRgb()
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
    }

    /**
    @return body position in world space
    */
    public Vector3D getPosition()
    {
        return position;
    }

    /**
    @param p body position in world space
    */
    public void setPosition(Vector3D p)
    {
        position = p;
    }

    /**
    @return body scale relative to object space axes
    */
    public Vector3D getScale()
    {
        return scale;
    }

    /**
    @return object-to-world matrix with scale, rotation and translation
    */
    public Matrix4x4 getTransformationMatrix()
    {
        Matrix4x4 scaleMatrix = new Matrix4x4();
        Matrix4x4 translateMatrix = new Matrix4x4();
        Matrix4x4 multipliedMatrix;
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
    public void setScale(Vector3D s)
    {
        scale = s;
        hasInvertibleScale =
            Math.abs(scale.x()) > VSDK.EPSILON &&
            Math.abs(scale.y()) > VSDK.EPSILON &&
            Math.abs(scale.z()) > VSDK.EPSILON;

        if ( hasInvertibleScale ) {
            inverseScale = new Vector3D(
                1.0 / scale.x(),
                1.0 / scale.y(),
                1.0 / scale.z());
        }
        else {
            inverseScale = new Vector3D();
        }
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

        Vector3D localOrigin = worldPointToObjectSpace(inOutRay.origin());
        Vector3D localDirection = worldDirectionToObjectSpace(inOutRay.direction());
        double localDirectionLength = localDirection.length();
        if ( localDirectionLength <= VSDK.EPSILON ) {
            return false;
        }

        int requiredDetailMask =
            outHit != null ? outHit.requiredDetailMask() : RayHit.DETAIL_NONE;
        Ray localRay = new Ray(
            localOrigin,
            localDirection.multiply(1.0 / localDirectionLength),
            scaleRayParameterForObjectSpace(inOutRay.t(), localDirectionLength));

        RayHit hit = outHit;
        if ( hit == null ) {
            hit = new RayHit(RayHit.DETAIL_NONE);
        }
        else {
            hit.reset(requiredDetailMask);
        }

        // ... and compute doIntersection operation on object's coordinates
        if ( geometry.doIntersection(localRay, hit) ) {
            if ( hit.ray() == null ) {
                return false;
            }
            if ( outHit != null ) {
                double worldT = hit.ray().t() / localDirectionLength;
                outHit.setRay(inOutRay.withT(worldT));
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
    public int computeQuantitativeInvisibility(Vector3D origin, Vector3D p)
    {
        if ( geometry == null || !hasInvertibleScale ) {
            return 0;
        }

        Vector3D myOrigin = worldPointToObjectSpace(origin);
        Vector3D myP = worldPointToObjectSpace(p);

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

        Vector3D localOrigin = worldPointToObjectSpace(inRay.origin());
        Vector3D localDirection = worldDirectionToObjectSpace(inRay.direction());
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

    private static Matrix4x4 sanitizeRotationMatrix(Matrix4x4 rotationMatrix)
    {
        return rotationMatrix.withoutTranslation();
    }

    private static Vector3D scaleComponents(Vector3D value, Vector3D factors)
    {
        return new Vector3D(
            value.x() * factors.x(),
            value.y() * factors.y(),
            value.z() * factors.z());
    }

    private static Vector3D normalizeIfPossible(Vector3D vector)
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

    private Vector3D worldPointToObjectSpace(Vector3D point)
    {
        Vector3D translatedPoint = point.subtract(position);
        Vector3D rotatedPoint = rotationInverseQuaternion.rotate(translatedPoint);
        return scaleComponents(rotatedPoint, inverseScale);
    }

    private Vector3D worldDirectionToObjectSpace(Vector3D direction)
    {
        Vector3D rotatedDirection = rotationInverseQuaternion.rotate(direction);
        return scaleComponents(rotatedDirection, inverseScale);
    }

    private Vector3D objectPointToWorldSpace(Vector3D point)
    {
        Vector3D scaledPoint = scaleComponents(point, scale);
        return rotationQuaternion.rotate(scaledPoint).add(position);
    }

    private Vector3D objectNormalToWorldSpace(Vector3D normal)
    {
        Vector3D scaledNormal = scaleComponents(normal, inverseScale);
        return normalizeIfPossible(rotationQuaternion.rotate(scaledNormal));
    }

    private Vector3D objectTangentToWorldSpace(Vector3D tangent)
    {
        Vector3D scaledTangent = scaleComponents(tangent, scale);
        return normalizeIfPossible(rotationQuaternion.rotate(scaledTangent));
    }
}
