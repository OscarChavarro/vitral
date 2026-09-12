import { Double } from "../../../../java/lang/Double.js";
import { Entity } from "../../common/Entity.js";
import { VSDK } from "../../common/VSDK.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Quaterniond } from "../../common/linealAlgebra/Quaterniond.js";
import { Ray } from "../geometry/element/Ray.js";
import { Image } from "../../media/Image.js";
import { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import { NormalMap } from "../../media/NormalMap.js";
import { SimpleMaterial } from "../material/SimpleMaterial.js";
import { Geometry } from "../geometry/Geometry.js";
import { RayHit } from "../geometry/element/RayHit.js";
import { SurfaceRayIntersection } from "../geometry/geometricProcessing/SurfaceRayIntersection.js";
import { Sphere } from "../geometry/volume/Sphere.js";

/**
Represents a scene body composed of geometry plus object-to-world transform
state and visual attributes.

<p>The underlying geometry remains defined in object space around the origin.
`SimpleBody` is responsible for moving rays between world and object space.
*/
export class SimpleBody extends Entity {
    //- Model (1/6): body form ----------------------------------------
    private geometry: Geometry | null;

    //- Model (2/6): body geometric transformations -------------------
    private position!: Vector3Dd;
    private scale!: Vector3Dd;
    private rotation!: Matrix4x4d;
    private rotationInverse!: Matrix4x4d;
    private rotationQuaternion!: Quaterniond;
    private rotationInverseQuaternion!: Quaterniond;
    private inverseScale!: Vector3Dd;
    private hasInvertibleScale = false;
    private hasIdentityRotation = false;
    private hasUnitScale = false;
    private hasZeroTranslation = false;
    private hasIdentityTransform = false;
    private hasTranslationOnlyTransform = false;

    //- Model (3/6): body visual data ---------------------------------
    private globalMaterial: SimpleMaterial | null;
    private globalTextureMap: Image | null;
    private globalNormalMap: NormalMap | null;
    private globalNormalMapRgb: RGBImageUncompressed | null = null;

    //- Model (4/6): body physical data -------------------------------

    //- Model (5/6): body structural relationships --------------------

    //- Model (6/6): body semantic data -------------------------------
    private name: string | null = null;
    private modificationVersion = 0;

    public constructor() {
        super();
        this.geometry = null;
        this.setPosition(new Vector3Dd(0, 0, 0));
        this.setRotation(new Matrix4x4d());
        this.setScale(new Vector3Dd(1, 1, 1));
        this.globalMaterial = new SimpleMaterial();
        this.globalTextureMap = null;
        this.globalNormalMap = null;
    }

    /**
    @returns application-defined body name, or `null`
    */
    public getName(): string | null {
        return this.name;
    }

    public getModificationVersion(): number {
        return this.modificationVersion;
    }

    private markModified(): void {
        this.modificationVersion++;
    }

    /**
    @param n application-defined body name
    */
    public setName(n: string | null): void {
        this.name = n;
        this.markModified();
    }

    /**
    @returns the body geometry in object space
    */
    public getGeometry(): Geometry | null {
        return this.geometry;
    }

    /**
    @param g geometry in object space
    */
    public setGeometry(g: Geometry | null): void {
        this.geometry = g;
        this.markModified();
    }

    /**
    @returns the cached object-to-world rotation matrix
    */
    public getRotation(): Matrix4x4d {
        return this.rotation;
    }

    /**
    Sets the object-to-world rotation and refreshes the cached inverse
    rotation and quaternion forms used during intersection queries.

    @param rotation rigid-body rotation matrix without translation
    */
    public setRotation(rotation: Matrix4x4d): void {
        const sanitizedRotation = SimpleBody.sanitizeRotationMatrix(rotation);
        const cachedRotationQuaternion = sanitizedRotation.exportToQuaternion().normalized();

        this.rotation = sanitizedRotation;
        this.rotationQuaternion = cachedRotationQuaternion;
        this.rotationInverseQuaternion = cachedRotationQuaternion.conjugated();
        this.rotationInverse = new Matrix4x4d().importFromQuaternion(this.rotationInverseQuaternion);
        this.updateTransformFlags();
        this.markModified();
    }

    /**
    @returns the cached world-to-object rotation matrix
    */
    public getRotationInverse(): Matrix4x4d {
        return this.rotationInverse;
    }

    /**
    Sets the world-to-object rotation and refreshes the cached forward
    rotation and quaternion forms used during intersection queries.

    @param rotationInverse rigid-body inverse rotation matrix without translation
    */
    public setRotationInverse(rotationInverse: Matrix4x4d): void {
        const sanitizedInverseRotation = SimpleBody.sanitizeRotationMatrix(rotationInverse);
        const cachedInverseRotationQuaternion = sanitizedInverseRotation.exportToQuaternion().normalized();

        this.rotationInverse = sanitizedInverseRotation;
        this.rotationInverseQuaternion = cachedInverseRotationQuaternion;
        this.rotationQuaternion = cachedInverseRotationQuaternion.conjugated();
        this.rotation = new Matrix4x4d().importFromQuaternion(this.rotationQuaternion);
        this.updateTransformFlags();
        this.markModified();
    }

    /**
    @returns default material used by this body
    */
    public getMaterial(): SimpleMaterial | null {
        return this.globalMaterial;
    }

    /**
    @param m default material used by this body
    */
    public setMaterial(m: SimpleMaterial | null): void {
        this.globalMaterial = m;
        this.markModified();
    }

    /**
    @returns body texture, or `null`
    */
    public getTexture(): Image | null {
        return this.globalTextureMap;
    }

    /**
    @param inTexture texture to associate with this body
    */
    public setTexture(inTexture: Image | null): void {
        this.globalTextureMap = inTexture;
        this.markModified();
    }

    /**
    @returns body normal map, or `null`
    */
    public getNormalMap(): NormalMap | null {
        return this.globalNormalMap;
    }

    /**
    @returns RGB preview image for the current normal map, or `null`
    */
    public getNormalMapRgb(): RGBImageUncompressed | null {
        return this.globalNormalMapRgb;
    }

    /**
    @param inNormalMap normal map to associate with this body
    */
    public setNormalMap(inNormalMap: NormalMap | null): void {
        this.globalNormalMap = inNormalMap;
        if (this.globalNormalMap !== null) {
            this.globalNormalMapRgb = this.globalNormalMap.exportToRgbImage();
        }
        this.markModified();
    }

    /**
    @returns body position in world space
    */
    public getPosition(): Vector3Dd {
        return this.position;
    }

    /**
    @param p body position in world space
    */
    public setPosition(p: Vector3Dd): void {
        this.position = p;
        this.updateTransformFlags();
        this.markModified();
    }

    /**
    @returns body scale relative to object space axes
    */
    public getScale(): Vector3Dd {
        return this.scale;
    }

    /**
    Transforms an object-space surface normal into world space, applying the
    inverse-scale then the body rotation (the correct normal transform for an
    affine body transform), and renormalizing. Orientation is preserved, so an
    outward object-space normal maps to an outward world-space normal.

    @param normal object-space normal
    @returns the corresponding unit world-space normal
    */
    public transformNormalToWorld(normal: Vector3Dd): Vector3Dd {
        return this.objectNormalToWorldSpace(normal);
    }

    /**
    Transforms an object-space point into world space (scale, rotation,
    translation).

    @param point object-space point
    @returns the corresponding world-space point
    */
    public transformPointToWorld(point: Vector3Dd): Vector3Dd {
        return this.objectPointToWorldSpace(point);
    }

    /**
    @returns object-to-world matrix with scale, rotation and translation
    */
    public getTransformationMatrix(): Matrix4x4d {
        let scaleMatrix = new Matrix4x4d();
        let translateMatrix = new Matrix4x4d();
        let multipliedMatrix: Matrix4x4d;
        scaleMatrix = scaleMatrix.scale(this.scale);
        translateMatrix = translateMatrix.translation(this.position);
        multipliedMatrix = translateMatrix.multiply(this.rotation.multiply(scaleMatrix));
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
    public setScale(s: Vector3Dd): void {
        this.scale = s;
        this.hasInvertibleScale =
            Math.abs(this.scale.x()) > VSDK.EPSILON &&
            Math.abs(this.scale.y()) > VSDK.EPSILON &&
            Math.abs(this.scale.z()) > VSDK.EPSILON;

        if (this.hasInvertibleScale) {
            this.inverseScale = new Vector3Dd(1.0 / this.scale.x(), 1.0 / this.scale.y(), 1.0 / this.scale.z());
        } else {
            this.inverseScale = new Vector3Dd();
        }
        this.updateTransformFlags();
        this.markModified();
    }

    /**
    Intersects a world-space ray against this body.

    <p>The incoming ray is transformed into object space using the cached
    inverse scale and inverse rotation. The resulting hit information is then
    transformed back into world space.

    @param inRay ray to be tested for intersection
    @returns a new ray containing the closest hit distance, or `null`
        when the ray misses the body
    */
    public doIntersectionFirstHit(inRay: Ray): Ray | null;
    /**
    Intersects a world-space ray against this body and reports the hit in
    world coordinates.

    @param inOutRay world-space ray to test
    @param outHit output structure populated on hit; may be `null`
    @returns `true` when the ray intersects the body
    */
    public doIntersectionFirstHit(inOutRay: Ray, outHit: RayHit | null): boolean;
    public doIntersectionFirstHit(inOutRay: Ray, outHitArgument?: RayHit | null): boolean | Ray | null {
        if (arguments.length === 1) {
            const firstHit = new RayHit();
            if (this.doIntersectionFirstHit(inOutRay, firstHit)) {
                return firstHit.ray();
            }
            return null;
        }
        const outHit: RayHit | null = outHitArgument ?? null;

        if (this.geometry === null || !this.hasInvertibleScale) {
            return false;
        }

        const requiredDetailMask = outHit !== null ? outHit.requiredDetailMask() : RayHit.DETAIL_NONE;

        if (
            this.hasTranslationOnlyTransform &&
            requiredDetailMask === RayHit.DETAIL_NONE &&
            this.geometry instanceof Sphere
        ) {
            return this.doIntersectionWithTranslationOnlySphereFastPath(inOutRay, outHit, this.geometry);
        }

        if (this.hasIdentityTransform) {
            return SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, inOutRay, outHit as RayHit);
        }

        if (this.hasTranslationOnlyTransform) {
            return this.doIntersectionWithTranslationOnly(inOutRay, outHit, requiredDetailMask);
        }

        const localOrigin = this.worldPointToObjectSpace(inOutRay.getOrigin());
        const localDirection = this.worldDirectionToObjectSpace(inOutRay.getDirection());
        const localDirectionLength = localDirection.length();
        if (localDirectionLength <= VSDK.EPSILON) {
            return false;
        }

        const localRay = new Ray(
            localOrigin,
            localDirection.multiply(1.0 / localDirectionLength),
            SimpleBody.scaleRayParameterForObjectSpace(inOutRay.getT(), localDirectionLength),
        );

        let hit = outHit;
        if (hit === null) {
            hit = new RayHit(RayHit.DETAIL_NONE);
        } else {
            if (requiredDetailMask === RayHit.DETAIL_NONE) {
                hit.resetForDistanceOnly();
            } else {
                hit.reset(requiredDetailMask);
            }
        }

        // ... and compute doIntersectionFirstHit operation on object's coordinates
        if (SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, localRay, hit)) {
            let localHitT: number;
            if (hit.ray() !== null) {
                localHitT = (hit.ray() as Ray).getT();
            } else if (hit.hasHitDistance()) {
                localHitT = hit.hitDistance();
            } else {
                return false;
            }
            if (outHit !== null) {
                const worldT = localHitT / localDirectionLength;
                if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                    outHit.setRay(inOutRay.withT(worldT));
                } else {
                    outHit.setHitDistance(worldT);
                }
                if (outHit.needsPoint()) {
                    outHit.p = this.objectPointToWorldSpace(hit.p);
                }
                if (outHit.needsNormal()) {
                    outHit.n = this.objectNormalToWorldSpace(hit.n);
                }
                if (outHit.needsTextureCoordinates()) {
                    outHit.u = hit.u;
                    outHit.v = hit.v;
                }
                if (outHit.needsTangent()) {
                    outHit.t = this.objectTangentToWorldSpace(hit.t);
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
    @returns quantitative invisibility value reported by the underlying geometry
    */
    public computeQuantitativeInvisibility(origin: Vector3Dd, p: Vector3Dd): number {
        if (this.geometry === null || !this.hasInvertibleScale) {
            return 0;
        }

        const myOrigin = this.worldPointToObjectSpace(origin);
        const myP = this.worldPointToObjectSpace(p);

        return this.geometry.computeQuantitativeInvisibility(myOrigin, myP);
    }

    /**
    Rebuilds detailed hit information for a world-space ray/parameter pair.

    @param inRay world-space ray that previously hit the body
    @param inT hit distance measured along the world-space ray
    @param outInfo structure populated with world-space hit data
    */
    public doExtraInformation(inRay: Ray, inT: number, outInfo: RayHit | null): void {
        if (outInfo === null || this.geometry === null || !this.hasInvertibleScale) {
            return;
        }

        if (this.hasIdentityTransform) {
            outInfo.setRay(inRay);
            this.geometry.doExtraInformation(inRay, inT, outInfo);
            outInfo.setRay(inRay);
            return;
        }

        if (this.hasTranslationOnlyTransform) {
            const translationOnlyLocalRay = inRay.withOrigin(inRay.getOrigin().subtract(this.position));
            outInfo.setRay(inRay);
            this.geometry.doExtraInformation(translationOnlyLocalRay, inT, outInfo);
            outInfo.setRay(inRay);
            if (outInfo.needsPoint()) {
                outInfo.p = outInfo.p.add(this.position);
            }
            return;
        }

        const localOrigin = this.worldPointToObjectSpace(inRay.getOrigin());
        const localDirection = this.worldDirectionToObjectSpace(inRay.getDirection());
        const localDirectionLength = localDirection.length();
        if (localDirectionLength <= VSDK.EPSILON) {
            return;
        }

        const localT = SimpleBody.scaleRayParameterForObjectSpace(inT, localDirectionLength);
        const localRay = new Ray(localOrigin, localDirection.multiply(1.0 / localDirectionLength), localT);

        outInfo.setRay(inRay);
        this.geometry.doExtraInformation(localRay, localT, outInfo);
        outInfo.setRay(inRay);
        if (outInfo.needsPoint()) {
            outInfo.p = this.objectPointToWorldSpace(outInfo.p);
        }
        if (outInfo.needsNormal()) {
            outInfo.n = this.objectNormalToWorldSpace(outInfo.n);
        }
        if (outInfo.needsTangent()) {
            outInfo.t = this.objectTangentToWorldSpace(outInfo.t);
        }
    }

    private static sanitizeRotationMatrix(rotationMatrix: Matrix4x4d): Matrix4x4d {
        return rotationMatrix.withoutTranslation();
    }

    private updateTransformFlags(): void {
        this.hasIdentityRotation =
            this.rotation !== undefined && this.rotation !== null && SimpleBody.isIdentityRotation(this.rotation);
        this.hasUnitScale =
            this.scale !== undefined &&
            this.scale !== null &&
            Math.abs(this.scale.x() - 1.0) <= VSDK.EPSILON &&
            Math.abs(this.scale.y() - 1.0) <= VSDK.EPSILON &&
            Math.abs(this.scale.z() - 1.0) <= VSDK.EPSILON;
        this.hasZeroTranslation =
            this.position !== undefined &&
            this.position !== null &&
            Math.abs(this.position.x()) <= VSDK.EPSILON &&
            Math.abs(this.position.y()) <= VSDK.EPSILON &&
            Math.abs(this.position.z()) <= VSDK.EPSILON;
        this.hasTranslationOnlyTransform = this.hasIdentityRotation && this.hasUnitScale;
        this.hasIdentityTransform = this.hasTranslationOnlyTransform && this.hasZeroTranslation;
    }

    private static isIdentityRotation(matrix: Matrix4x4d): boolean {
        return (
            Math.abs(matrix.get(0, 0) - 1.0) <= VSDK.EPSILON &&
            Math.abs(matrix.get(0, 1)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(0, 2)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(1, 0)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(1, 1) - 1.0) <= VSDK.EPSILON &&
            Math.abs(matrix.get(1, 2)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(2, 0)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(2, 1)) <= VSDK.EPSILON &&
            Math.abs(matrix.get(2, 2) - 1.0) <= VSDK.EPSILON
        );
    }

    private doIntersectionWithTranslationOnly(
        inOutRay: Ray,
        outHit: RayHit | null,
        requiredDetailMask: number,
    ): boolean {
        const localRay = inOutRay.withOrigin(inOutRay.getOrigin().subtract(this.position));

        let hit = outHit;
        if (hit === null) {
            hit = new RayHit(RayHit.DETAIL_NONE);
        } else {
            if (requiredDetailMask === RayHit.DETAIL_NONE) {
                hit.resetForDistanceOnly();
            } else {
                hit.reset(requiredDetailMask);
            }
        }

        if (!SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, localRay, hit)) {
            return false;
        }
        let localHitT: number;
        if (hit.ray() !== null) {
            localHitT = (hit.ray() as Ray).getT();
        } else if (hit.hasHitDistance()) {
            localHitT = hit.hitDistance();
        } else {
            return false;
        }
        if (outHit !== null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                outHit.setRay(inOutRay.withT(localHitT));
            } else {
                outHit.setHitDistance(localHitT);
            }
            if (outHit.needsPoint()) {
                outHit.p = hit.p.add(this.position);
            }
            if (outHit.needsNormal()) {
                outHit.n = hit.n;
            }
            if (outHit.needsTextureCoordinates()) {
                outHit.u = hit.u;
                outHit.v = hit.v;
            }
            if (outHit.needsTangent()) {
                outHit.t = hit.t;
            }
            outHit.material = hit.material;
            outHit.texture = hit.texture;
            outHit.normalMap = hit.normalMap;
        }
        return true;
    }

    private doIntersectionWithTranslationOnlySphereFastPath(
        inOutRay: Ray,
        outHit: RayHit | null,
        sphere: Sphere,
    ): boolean {
        const dx = this.position.x() - inOutRay.getOrigin().x();
        const dy = this.position.y() - inOutRay.getOrigin().y();
        const dz = this.position.z() - inOutRay.getOrigin().z();
        const direction = inOutRay.getDirection();
        const projection = direction.x() * dx + direction.y() * dy + direction.z() * dz;
        const discriminant = sphere.getRadiusSquared() + projection * projection - dx * dx - dy * dy - dz * dz;

        if (discriminant < 0) {
            return false;
        }

        const t = projection - Math.sqrt(discriminant);
        if (t < 0) {
            return false;
        }

        if (outHit !== null) {
            if (outHit.shouldStoreRay()) {
                outHit.setRay(inOutRay.withT(t));
            } else {
                outHit.setHitDistance(t);
            }
        }
        return true;
    }

    private static scaleComponents(value: Vector3Dd, factors: Vector3Dd): Vector3Dd {
        return new Vector3Dd(value.x() * factors.x(), value.y() * factors.y(), value.z() * factors.z());
    }

    private static normalizeIfPossible(vector: Vector3Dd): Vector3Dd {
        if (vector.length() <= VSDK.EPSILON) {
            return vector;
        }
        return vector.normalized();
    }

    private static scaleRayParameterForObjectSpace(
        worldSpaceRayParameter: number,
        localDirectionLength: number,
    ): number {
        if (worldSpaceRayParameter >= Double.MAX_VALUE / localDirectionLength) {
            return Double.MAX_VALUE;
        }
        return worldSpaceRayParameter * localDirectionLength;
    }

    private worldPointToObjectSpace(point: Vector3Dd): Vector3Dd {
        const translatedPoint = point.subtract(this.position);
        const rotatedPoint = this.rotationInverseQuaternion.rotate(translatedPoint);
        return SimpleBody.scaleComponents(rotatedPoint, this.inverseScale);
    }

    private worldDirectionToObjectSpace(direction: Vector3Dd): Vector3Dd {
        const rotatedDirection = this.rotationInverseQuaternion.rotate(direction);
        return SimpleBody.scaleComponents(rotatedDirection, this.inverseScale);
    }

    private objectPointToWorldSpace(point: Vector3Dd): Vector3Dd {
        const scaledPoint = SimpleBody.scaleComponents(point, this.scale);
        return this.rotationQuaternion.rotate(scaledPoint).add(this.position);
    }

    private objectNormalToWorldSpace(normal: Vector3Dd): Vector3Dd {
        const scaledNormal = SimpleBody.scaleComponents(normal, this.inverseScale);
        return SimpleBody.normalizeIfPossible(this.rotationQuaternion.rotate(scaledNormal));
    }

    private objectTangentToWorldSpace(tangent: Vector3Dd): Vector3Dd {
        const scaledTangent = SimpleBody.scaleComponents(tangent, this.scale);
        return SimpleBody.normalizeIfPossible(this.rotationQuaternion.rotate(scaledTangent));
    }
}
