package vsdk.toolkit.environment.scene;

// Java
import java.io.Serial;

// Vitral
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Quaterniond;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.NormalMapTexture;
import vsdk.toolkit.environment.material.Material;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.geometricProcessing.SurfaceRayIntersection;
import vsdk.toolkit.environment.geometry.volume.Sphere;

/**
The VITRAL scene body: a `SceneSolid` whose placement is edited either as
position, rotation and scale or as an arbitrary affine map, with VITRAL
appearance (material, texture, normal map) and first hit fast paths for
identity and translation-only placements.
*/
public class SimpleBody extends SceneSolid {
    @Serial private static final long serialVersionUID = 20060502L;

    private AffinePlacement affinePlacement;
    private boolean explicitAffinePlacement;
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
    private boolean hasTranslationOnlyTransform;
    private boolean hasIdentityTransform;

    private SimpleMaterial material;
    /// False when the material is shared (see `setMaterialReference`)
    private boolean ownsMaterial;
    private Image texture;
    /// False when the texture is shared (see `setTextureReference`)
    private boolean ownsTexture;
    private NormalMap normalMap;
    /// False when the normal map is shared (see `setNormalMapReference`)
    private boolean ownsNormalMap;
    private RGBImageUncompressed normalMapRgb;

    private enum QueryPlacementStatus { Valid, Invalid, Indeterminate }

    public SimpleBody()
    {
        super();
        affinePlacement = new AffinePlacement();
        explicitAffinePlacement = false;
        position = new Vector3Dd(0, 0, 0);
        scale = new Vector3Dd(1, 1, 1);
        rotation = new Matrix4x4d();
        rotationInverse = new Matrix4x4d();
        rotationQuaternion = rotation.exportToQuaternion();
        rotationInverseQuaternion = rotationInverse.exportToQuaternion();
        inverseScale = new Vector3Dd(1, 1, 1);
        hasInvertibleScale = true;
        hasIdentityRotation = true;
        hasUnitScale = true;
        hasZeroTranslation = true;
        hasTranslationOnlyTransform = true;
        hasIdentityTransform = true;
        // Java keeps its historic default material; cpp starts with none.
        material = new SimpleMaterial();
        ownsMaterial = true;
        texture = null;
        ownsTexture = true;
        normalMap = null;
        ownsNormalMap = true;
        normalMapRgb = null;
    }

    /// Only plain SimpleBody instances; subclasses opt in themselves
    @Override
    protected boolean denotesPlacedGeometry() { return getClass() == SimpleBody.class; }

    public boolean supportsDecomposedPlacement() { return !explicitAffinePlacement; }

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

    private void updateTransformFlags()
    {
        hasIdentityRotation = isIdentityRotation(rotation);
        hasUnitScale =
            Math.abs(scale.x() - 1.0) <= VSDK.EPSILON &&
            Math.abs(scale.y() - 1.0) <= VSDK.EPSILON &&
            Math.abs(scale.z() - 1.0) <= VSDK.EPSILON;
        hasZeroTranslation =
            Math.abs(position.x()) <= VSDK.EPSILON &&
            Math.abs(position.y()) <= VSDK.EPSILON &&
            Math.abs(position.z()) <= VSDK.EPSILON;
        hasTranslationOnlyTransform = hasIdentityRotation && hasUnitScale;
        hasIdentityTransform = hasTranslationOnlyTransform && hasZeroTranslation;
    }

    private void updateDecomposedPlacement()
    {
        final Matrix4x4d forward = new Matrix4x4d().translation(position)
            .multiply(rotation.multiply(new Matrix4x4d().scale(scale)));
        final Matrix4x4d inverse = new Matrix4x4d().scale(inverseScale)
            .multiply(rotationInverse).multiply(new Matrix4x4d().translation(position.multiply(-1)));
        affinePlacement.assign(forward, inverse);
    }

    public Matrix4x4d getRotation()
    {
        if (explicitAffinePlacement) throw new IllegalStateException("Rotation is not defined for an arbitrary affine placement");
        return rotation;
    }

    public void setRotation(Matrix4x4d r)
    {
        if (explicitAffinePlacement) throw new IllegalStateException("Cannot decompose an arbitrary affine placement");
        Matrix4x4d sanitized = r.withoutTranslation();
        rotationQuaternion = sanitized.exportToQuaternion().normalized();
        rotation = sanitized;
        rotationInverseQuaternion = rotationQuaternion.conjugated();
        rotationInverse = new Matrix4x4d().importFromQuaternion(rotationInverseQuaternion);
        updateTransformFlags();
        updateDecomposedPlacement();
        markModified();
    }

    public Matrix4x4d getRotationInverse()
    {
        if (explicitAffinePlacement) throw new IllegalStateException("Rotation is not defined for an arbitrary affine placement");
        return rotationInverse;
    }

    public void setRotationInverse(Matrix4x4d ri)
    {
        if (explicitAffinePlacement) throw new IllegalStateException("Cannot decompose an arbitrary affine placement");
        Matrix4x4d sanitized = ri.withoutTranslation();
        rotationInverseQuaternion = sanitized.exportToQuaternion().normalized();
        rotationInverse = sanitized;
        rotationQuaternion = rotationInverseQuaternion.conjugated();
        rotation = new Matrix4x4d().importFromQuaternion(rotationQuaternion);
        updateTransformFlags();
        updateDecomposedPlacement();
        markModified();
    }

    public SimpleMaterial getMaterial() { return material; }

    @Override
    public Material getSurfaceMaterial() { return material; }

    /**
    Sets the material, taking its ownership (it is deleted with this body or
    when replaced).
    @param m new material, or null
    */
    public void setMaterial(SimpleMaterial m)
    {
        material = m;
        ownsMaterial = true;
        markModified();
    }

    /**
    Sets a material shared with other owners: this body does not delete it.
    @param m shared material, or null
    */
    public void setMaterialReference(SimpleMaterial m)
    {
        material = m;
        ownsMaterial = false;
        markModified();
    }

    public Image getTexture() { return texture; }

    /**
    Sets the texture, taking its ownership (it is deleted with this body or
    when replaced).
    @param in new texture, or null
    */
    public void setTexture(Image in)
    {
        texture = in;
        ownsTexture = true;
        markModified();
    }

    /**
    Sets a texture shared with other owners, such as a scene resource
    (`SimpleScene.addImage`): this body does not delete it.
    @param in shared texture, or null
    */
    public void setTextureReference(Image in)
    {
        texture = in;
        ownsTexture = false;
        markModified();
    }

    public NormalMap getNormalMap() { return normalMap; }

    public RGBImageUncompressed getNormalMapRgb() { return normalMapRgb; }

    /**
    Sets the normal map, taking its ownership.
    @param in new normal map, or null
    */
    public void setNormalMap(NormalMap in)
    {
        normalMap = in;
        ownsNormalMap = true;
        normalMapRgb = in != null ? new NormalMapTexture(in) : null;
        markModified();
    }

    /**
    Sets a normal map shared with other owners, such as a scene resource
    (`SimpleScene.addNormalMap`): this body does not delete it. The body
    keeps its own RGB encoding of the map.
    @param in shared normal map, or null
    */
    public void setNormalMapReference(NormalMap in)
    {
        normalMap = in;
        ownsNormalMap = false;
        normalMapRgb = in != null ? new NormalMapTexture(in) : null;
        markModified();
    }

    public Vector3Dd getPosition()
    {
        return explicitAffinePlacement ? affinePlacement.getForward().extractTranslation() : position;
    }

    public void setPosition(Vector3Dd p)
    {
        if (explicitAffinePlacement) {
            final Matrix4x4d forward = affinePlacement.getForward().withTranslation(p);
            final Vector3Dd inverseTranslation = affinePlacement.getInverse().withoutTranslation().multiply(p).multiply(-1);
            final Matrix4x4d inverse = affinePlacement.getInverse().withTranslation(inverseTranslation);
            AffinePlacement candidate = new AffinePlacement();
            if (candidate.assign(forward,inverse) != AffinePlacementStatus.Valid)
                throw new IllegalStateException("Invalid affine translation");
            affinePlacement = candidate;
            markModified();
            return;
        }
        position = p; updateTransformFlags(); updateDecomposedPlacement(); markModified();
    }

    public Vector3Dd getScale()
    {
        if (explicitAffinePlacement) throw new IllegalStateException("Scale is not defined for an arbitrary affine placement");
        return scale;
    }

    public Matrix4x4d getTransformationMatrix() { return affinePlacement.getForward(); }

    @Override
    public Matrix4x4d getAffinePlacement() { return affinePlacement.getForward(); }

    @Override
    public AffinePlacementStatus setAffinePlacement(Matrix4x4d forward, Matrix4x4d inverse)
    {
        AffinePlacement candidate = new AffinePlacement();
        final AffinePlacementStatus status = candidate.assign(forward,inverse);
        if (status != AffinePlacementStatus.Valid) return status;
        affinePlacement = candidate;
        explicitAffinePlacement = true;
        markModified();
        return status;
    }

    // Reset to identity and re-enable decomposed editing.
    @Override
    public void resetAffinePlacement()
    {
        explicitAffinePlacement = false;
        position = new Vector3Dd(); scale = new Vector3Dd(1,1,1); inverseScale = new Vector3Dd(1,1,1);
        rotation = Matrix4x4d.identityMatrix(); rotationInverse = rotation;
        rotationQuaternion = rotation.exportToQuaternion(); rotationInverseQuaternion = rotationQuaternion;
        hasInvertibleScale = true;
        updateTransformFlags(); updateDecomposedPlacement(); markModified();
    }

    public void setScale(Vector3Dd s)
    {
        if (explicitAffinePlacement) throw new IllegalStateException("Cannot decompose an arbitrary affine placement");
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
        updateDecomposedPlacement();
        markModified();
    }

    /**
    Java-only helper (kept from the previous Java port, used by the hidden
    line renderer): object-space normal to unit world-space normal.
    */
    public Vector3Dd transformNormalToWorld(Vector3Dd normal)
    {
        if (explicitAffinePlacement) {
            final Matrix4x4d inverse = affinePlacement.getInverse();
            return normalizeIfPossible(new Vector3Dd(
                inverse.get(0,0)*normal.x()+inverse.get(1,0)*normal.y()+inverse.get(2,0)*normal.z(),
                inverse.get(0,1)*normal.x()+inverse.get(1,1)*normal.y()+inverse.get(2,1)*normal.z(),
                inverse.get(0,2)*normal.x()+inverse.get(1,2)*normal.y()+inverse.get(2,2)*normal.z()));
        }
        Vector3Dd scaledNormal = scaleComponents(normal, inverseScale);
        return normalizeIfPossible(rotationQuaternion.rotate(scaledNormal));
    }

    /**
    Java-only helper (kept from the previous Java port): object-space point
    to world space.
    */
    public Vector3Dd transformPointToWorld(Vector3Dd point)
    {
        if (explicitAffinePlacement) return affinePlacement.getForward().multiply(point);
        Vector3Dd scaledPoint = scaleComponents(point, scale);
        return rotationQuaternion.rotate(scaledPoint).add(position);
    }

    public Ray doIntersectionFirstHit(Ray inRay)
    {
        RayHit hit = new RayHit();
        if ( !doIntersectionFirstHit(inRay, hit) || hit.getRay() == null ) {
            return null;
        }
        return hit.getRay();
    }

    private boolean doIntersectionWithTranslationOnlySphereFastPath(Ray inOutRay, RayHit outHit)
    {
        final Sphere sphere = (Sphere)geometry;
        double[] t = {0}, farT = {0};
        if (Sphere.intersectionRoots(inOutRay.getOrigin().subtract(position),
                inOutRay.getDirection(), sphere.getRadius(),
                new RayCrossingQuery(0, Double.POSITIVE_INFINITY), t, farT) == 0) {
            return false;
        }

        if ( outHit != null ) {
            if ( outHit.shouldStoreRay() ) {
                outHit.setRay(inOutRay.withT(t[0]));
            }
            else {
                outHit.setHitDistance(t[0]);
            }
        }
        return true;
    }

    @Override
    public boolean doIntersectionFirstHit(Ray inOutRay, RayHit outHit)
    {
        if ( geometry == null || (!explicitAffinePlacement && !hasInvertibleScale) ) {
            return false;
        }
        if (explicitAffinePlacement) return doIntersectionWithAffinePlacement(inOutRay, outHit);

        final int requestedDetailMask = outHit != null ? outHit.getRequiredDetailMask() : RayHit.DETAIL_NONE;

        if ( hasTranslationOnlyTransform && requestedDetailMask == RayHit.DETAIL_NONE && hasSphereGeometry() ) {
            return doIntersectionWithTranslationOnlySphereFastPath(inOutRay, outHit);
        }

        if ( hasIdentityTransform ) {
            return SurfaceRayIntersection.doIntersectionFirstHit(geometry, inOutRay, outHit);
        }

        if ( hasTranslationOnlyTransform ) {
            return doIntersectionWithTranslationOnly(inOutRay, outHit, requestedDetailMask);
        }

        Vector3Dd translatedOrigin = inOutRay.getOrigin().subtract(position);
        Vector3Dd rotatedOrigin = rotationInverseQuaternion.rotate(translatedOrigin);
        Vector3Dd localOrigin = new Vector3Dd(
            rotatedOrigin.x() * inverseScale.x(),
            rotatedOrigin.y() * inverseScale.y(),
            rotatedOrigin.z() * inverseScale.z());

        Vector3Dd rotatedDirection = rotationInverseQuaternion.rotate(inOutRay.getDirection());
        Vector3Dd localDirection = new Vector3Dd(
            rotatedDirection.x() * inverseScale.x(),
            rotatedDirection.y() * inverseScale.y(),
            rotatedDirection.z() * inverseScale.z());
        final double localDirectionLength = localDirection.length();
        if ( localDirectionLength <= VSDK.EPSILON ) {
            return false;
        }
        localDirection = localDirection.multiply(1.0 / localDirectionLength);

        double localRayT = inOutRay.getT();
        if ( localRayT >= Double.MAX_VALUE / localDirectionLength ) {
            localRayT = Double.MAX_VALUE;
        }
        else {
            localRayT *= localDirectionLength;
        }

        Ray localRay = new Ray(localOrigin, localDirection, localRayT);
        final boolean requestedStoreRay = outHit != null ? outHit.shouldStoreRay() : false;

        if ( outHit != null ) {
            outHit.setStoreRay(false);
            outHit.resetForDistanceOnly();
            if ( !SurfaceRayIntersection.doIntersectionFirstHit(geometry, localRay, outHit) ) {
                outHit.setStoreRay(requestedStoreRay);
                outHit.setRequiredDetailMask(requestedDetailMask);
                return false;
            }
        }
        else {
            RayHit localHitStorage = new RayHit();
            localHitStorage.setStoreRay(false);
            localHitStorage.resetForDistanceOnly();
            if ( !SurfaceRayIntersection.doIntersectionFirstHit(geometry, localRay, localHitStorage) ) {
                return false;
            }
        }

        if ( outHit != null ) {
            double localHitT;
            if ( outHit.getRay() != null ) {
                localHitT = outHit.getRay().getT();
            }
            else if ( outHit.hasHitDistance() ) {
                localHitT = outHit.getHitDistance();
            }
            else {
                return false;
            }

            double worldT = localHitT / localDirectionLength;

            outHit.setStoreRay(requestedStoreRay);
            outHit.setRequiredDetailMask(requestedDetailMask);
            if ( requestedStoreRay || requestedDetailMask != RayHit.DETAIL_NONE ) {
                Ray worldRay = inOutRay.withT(worldT);
                outHit.setRay(worldRay);
                if ( requestedDetailMask != RayHit.DETAIL_NONE ) {
                    doExtraInformation(worldRay, worldT, outHit);
                }
            }
            else {
                outHit.setHitDistance(worldT);
            }
        }
        return true;
    }

    private boolean doIntersectionWithAffinePlacement(Ray ray, RayHit outHit)
    {
        final Matrix4x4d inverse = affinePlacement.getInverse();
        final Vector3Dd origin = inverse.multiply(ray.getOrigin());
        final Vector3Dd direction = inverse.withoutTranslation().multiply(ray.getDirection());
        final double length = direction.length();
        if (!Double.isFinite(length) || length <= VSDK.EPSILON) return false;
        double localT = ray.getT();
        if (localT >= Double.MAX_VALUE / length) localT = Double.MAX_VALUE; else localT *= length;
        final Ray localRay = new Ray(origin, direction, localT);
        RayHit localHit = new RayHit(RayHit.DETAIL_NONE, false);
        if (!SurfaceRayIntersection.doIntersectionFirstHit(geometry, localRay, localHit)) return false;
        final double distance = localHit.getRay() != null ? localHit.getRay().getT() : localHit.getHitDistance();
        if (localHit.getRay() == null && !localHit.hasHitDistance()) return false;
        final double worldT = distance / length;
        if (!Double.isFinite(worldT)) return false;
        if (outHit != null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                final Ray worldRay = ray.withT(worldT);
                outHit.setRay(worldRay);
                if (outHit.needsAnySurfaceData()) doExtraInformationWithAffinePlacement(worldRay, worldT, outHit);
            } else outHit.setHitDistance(worldT);
        }
        return true;
    }

    private void doExtraInformationWithAffinePlacement(Ray ray, double t, RayHit hit)
    {
        final Matrix4x4d forward = affinePlacement.getForward();
        final Matrix4x4d inverse = affinePlacement.getInverse();
        final Vector3Dd origin = inverse.multiply(ray.getOrigin());
        final Vector3Dd rawDirection = inverse.withoutTranslation().multiply(ray.getDirection());
        final double length = rawDirection.length();
        if (!Double.isFinite(length) || length <= VSDK.EPSILON) return;
        final Ray localRay = new Ray(origin, rawDirection, t*length);
        geometry.doExtraInformation(localRay, t*length, hit);
        if (hit.needsPoint()) hit.point = forward.multiply(hit.point);
        if (hit.needsNormal()) {
            final Vector3Dd n = hit.normal;
            hit.normal = new Vector3Dd(
                inverse.get(0,0)*n.x()+inverse.get(1,0)*n.y()+inverse.get(2,0)*n.z(),
                inverse.get(0,1)*n.x()+inverse.get(1,1)*n.y()+inverse.get(2,1)*n.z(),
                inverse.get(0,2)*n.x()+inverse.get(1,2)*n.y()+inverse.get(2,2)*n.z()).normalized();
        }
        if (hit.needsTangent()) hit.tangent = forward.withoutTranslation().multiply(hit.tangent).normalized();
        hit.material = material; hit.texture = texture; hit.normalMap = normalMap;
        hit.setRay(ray.withT(t));
    }

    private boolean doIntersectionWithTranslationOnly(
        Ray inOutRay,
        RayHit outHit,
        int requiredDetailMask)
    {
        Ray localRay = inOutRay.withOrigin(inOutRay.getOrigin().subtract(position));

        RayHit localHitStorage = new RayHit();
        RayHit hit = outHit != null ? outHit : localHitStorage;

        if ( requiredDetailMask == RayHit.DETAIL_NONE ) {
            hit.resetForDistanceOnly();
        }
        else {
            hit.reset(requiredDetailMask);
        }

        if ( !SurfaceRayIntersection.doIntersectionFirstHit(geometry, localRay, hit) ) {
            return false;
        }

        double localHitT;
        if ( hit.getRay() != null ) {
            localHitT = hit.getRay().getT();
        }
        else if ( hit.hasHitDistance() ) {
            localHitT = hit.getHitDistance();
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
                outHit.point = hit.point.add(position);
            }
            if ( outHit.needsNormal() ) {
                outHit.normal = hit.normal;
            }
            if ( outHit.needsTextureCoordinates() ) {
                outHit.u = hit.u;
                outHit.v = hit.v;
            }
            if ( outHit.needsTangent() ) {
                outHit.tangent = hit.tangent;
            }
            outHit.material = hit.material;
            outHit.texture = hit.texture;
            outHit.normalMap = hit.normalMap;
        }

        return true;
    }

    public int computeQuantitativeInvisibility(Vector3Dd origin, Vector3Dd p)
    {
        if ( geometry == null || (!explicitAffinePlacement && !hasInvertibleScale) ) {
            return 0;
        }
        if (explicitAffinePlacement) return geometry.computeQuantitativeInvisibility(
            affinePlacement.getInverse().multiply(origin), affinePlacement.getInverse().multiply(p));

        Vector3Dd translatedOrigin = origin.subtract(position);
        Vector3Dd rotatedOrigin = rotationInverseQuaternion.rotate(translatedOrigin);
        Vector3Dd myOrigin = new Vector3Dd(
            rotatedOrigin.x() * inverseScale.x(),
            rotatedOrigin.y() * inverseScale.y(),
            rotatedOrigin.z() * inverseScale.z());

        Vector3Dd translatedPoint = p.subtract(position);
        Vector3Dd rotatedPoint = rotationInverseQuaternion.rotate(translatedPoint);
        Vector3Dd myPoint = new Vector3Dd(
            rotatedPoint.x() * inverseScale.x(),
            rotatedPoint.y() * inverseScale.y(),
            rotatedPoint.z() * inverseScale.z());

        return geometry.computeQuantitativeInvisibility(myOrigin, myPoint);
    }

    @Override
    public void doExtraInformation(Ray inRay, double inT, RayHit outData)
    {
        if ( outData == null || geometry == null || (!explicitAffinePlacement && !hasInvertibleScale) ) return;
        if (explicitAffinePlacement) {
            if (outData.getRay() != null) doExtraInformationWithAffinePlacement(outData.getRay(), outData.getRay().getT(), outData);
            return;
        }

        final Ray worldRayPtr = outData.getRay();
        if ( worldRayPtr == null ) {
            outData.material = material;
            outData.texture = texture;
            outData.normalMap = normalMap;
            return;
        }

        final Ray worldRay = worldRayPtr;
        final double worldT = worldRay.getT();

        Vector3Dd translatedOrigin = worldRay.getOrigin().subtract(position);
        Vector3Dd rotatedOrigin = rotationInverseQuaternion.rotate(translatedOrigin);
        Vector3Dd localOrigin = new Vector3Dd(
            rotatedOrigin.x() * inverseScale.x(),
            rotatedOrigin.y() * inverseScale.y(),
            rotatedOrigin.z() * inverseScale.z());

        Vector3Dd rotatedDirection = rotationInverseQuaternion.rotate(worldRay.getDirection());
        Vector3Dd localDirection = new Vector3Dd(
            rotatedDirection.x() * inverseScale.x(),
            rotatedDirection.y() * inverseScale.y(),
            rotatedDirection.z() * inverseScale.z());
        double localDirLength = localDirection.length();
        if ( localDirLength <= VSDK.EPSILON ) {
            outData.material = material;
            outData.texture = texture;
            outData.normalMap = normalMap;
            return;
        }
        localDirection = localDirection.multiply(1.0 / localDirLength);
        double localT = worldT * localDirLength;
        Ray localRay = new Ray(localOrigin, localDirection, localT);

        geometry.doExtraInformation(localRay, localT, outData);

        if ( outData.needsPoint() ) {
            Vector3Dd worldPoint = rotation.multiply(new Vector3Dd(
                outData.point.x() * scale.x(),
                outData.point.y() * scale.y(),
                outData.point.z() * scale.z())).add(position);
            outData.point = worldPoint;
        }

        if ( outData.needsNormal() ) {
            Vector3Dd worldNormal = rotation.multiply(new Vector3Dd(
                outData.normal.x() * inverseScale.x(),
                outData.normal.y() * inverseScale.y(),
                outData.normal.z() * inverseScale.z())).normalized();
            outData.normal = worldNormal;
        }

        if ( outData.needsTangent() ) {
            Vector3Dd worldTangent = rotation.multiply(new Vector3Dd(
                outData.tangent.x() * scale.x(),
                outData.tangent.y() * scale.y(),
                outData.tangent.z() * scale.z())).normalized();
            outData.tangent = worldTangent;
        }

        outData.material = material;
        outData.texture = texture;
        outData.normalMap = normalMap;
        outData.setRay(worldRay.withT(worldT));
    }

    private QueryPlacementStatus queryPlacementStatus()
    {
        if (!Double.isFinite(position.x()) || !Double.isFinite(position.y()) ||
            !Double.isFinite(position.z()) || !Double.isFinite(scale.x()) ||
            !Double.isFinite(scale.y()) || !Double.isFinite(scale.z()) ||
            scale.x() == 0 || scale.y() == 0 || scale.z() == 0) return QueryPlacementStatus.Invalid;
        if (!hasInvertibleScale) return QueryPlacementStatus.Indeterminate;
        // This scene type promises a decomposed rotation, not a general affine matrix.
        // Reject non-orthogonal or inconsistent matrices rather than silently using
        // the quaternion-derived inverse as if it were the inverse of a shear.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                double product = 0, orthogonal = 0;
                for (int k = 0; k < 3; k++) {
                    final double a = rotation.get(row,k), inv = rotationInverse.get(k,column);
                    final double x = rotation.get(k,row), y = rotation.get(k,column);
                    if (!Double.isFinite(a) || !Double.isFinite(inv) ||
                        !Double.isFinite(x) || !Double.isFinite(y)) return QueryPlacementStatus.Invalid;
                    product += a * inv;
                    orthogonal += x * y;
                }
                final double expected = row == column ? 1.0 : 0.0;
                if (!Double.isFinite(product) || !Double.isFinite(orthogonal) ||
                    Math.abs(product - expected) > 1e-8 ||
                    Math.abs(orthogonal - expected) > 1e-8) return QueryPlacementStatus.Indeterminate;
            }
        }
        return QueryPlacementStatus.Valid;
    }

    @Override
    public AffinePlacementStatus queryAffinePlacement(Matrix4x4d[] forward, Matrix4x4d[] inverse)
    {
        if (explicitAffinePlacement) {
            forward[0] = affinePlacement.getForward(); inverse[0] = affinePlacement.getInverse();
            return affinePlacement.getStatus();
        }
        final QueryPlacementStatus status = queryPlacementStatus();
        if (status == QueryPlacementStatus.Invalid) return AffinePlacementStatus.Invalid;
        if (status == QueryPlacementStatus.Indeterminate) return AffinePlacementStatus.Indeterminate;
        forward[0] = affinePlacement.getForward(); inverse[0] = affinePlacement.getInverse();
        return affinePlacement.getStatus();
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
}
