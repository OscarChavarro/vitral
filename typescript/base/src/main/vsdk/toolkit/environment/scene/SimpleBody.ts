import { NormalMapTexture } from "../../media/NormalMapTexture.js";
import { Double } from "../../../../java/lang/Double.js";
import { IllegalStateException } from "../../../../java/lang/IllegalStateException.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { VSDK } from "../../common/VSDK.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import type { Quaterniond } from "../../common/linealAlgebra/Quaterniond.js";
import { Ray } from "../geometry/element/Ray.js";
import type { Image } from "../../media/Image.js";
import type { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import type { NormalMap } from "../../media/NormalMap.js";
import { SimpleMaterial } from "../material/SimpleMaterial.js";
import { RayCrossingQuery } from "../geometry/element/RayCrossingQuery.js";
import { RayHit } from "../geometry/element/RayHit.js";
import { SurfaceRayIntersection } from "../geometry/geometricProcessing/SurfaceRayIntersection.js";
import { Sphere } from "../geometry/volume/Sphere.js";
import { AffinePlacement } from "./AffinePlacement.js";
import { AffinePlacementStatus } from "./AffinePlacementStatus.js";
import { SceneSolid } from "./SceneSolid.js";

enum QueryPlacementStatus {
    Valid,
    Invalid,
    Indeterminate,
}

/**
The VITRAL scene body: a `SceneSolid` whose placement is edited either as
position, rotation and scale or as an arbitrary affine map, with VITRAL
appearance (material, texture, normal map) and first hit fast paths for
identity and translation-only placements.
*/
export class SimpleBody extends SceneSolid {
    private affinePlacement: AffinePlacement;
    private explicitAffinePlacement: boolean;
    private position: Vector3Dd;
    private scale: Vector3Dd;
    private rotation: Matrix4x4d;
    private rotationInverse: Matrix4x4d;
    private rotationQuaternion: Quaterniond;
    private rotationInverseQuaternion: Quaterniond;
    private inverseScale: Vector3Dd;
    private hasInvertibleScale: boolean;
    private hasIdentityRotation: boolean;
    private hasUnitScale: boolean;
    private hasZeroTranslation: boolean;
    private hasTranslationOnlyTransform: boolean;
    private hasIdentityTransform: boolean;

    private material: SimpleMaterial | null;
    /// False when the material is shared (see `setMaterialReference`)
    private ownsMaterial: boolean;
    private texture: Image | null;
    /// False when the texture is shared (see `setTextureReference`)
    private ownsTexture: boolean;
    private normalMap: NormalMap | null;
    /// False when the normal map is shared (see `setNormalMapReference`)
    private ownsNormalMap: boolean;
    private normalMapRgb: RGBImageUncompressed | null;

    public constructor() {
        super();
        this.affinePlacement = new AffinePlacement();
        this.explicitAffinePlacement = false;
        this.position = new Vector3Dd(0, 0, 0);
        this.scale = new Vector3Dd(1, 1, 1);
        this.rotation = new Matrix4x4d();
        this.rotationInverse = new Matrix4x4d();
        this.rotationQuaternion = this.rotation.exportToQuaternion();
        this.rotationInverseQuaternion = this.rotationInverse.exportToQuaternion();
        this.inverseScale = new Vector3Dd(1, 1, 1);
        this.hasInvertibleScale = true;
        this.hasIdentityRotation = true;
        this.hasUnitScale = true;
        this.hasZeroTranslation = true;
        this.hasTranslationOnlyTransform = true;
        this.hasIdentityTransform = true;
        // The TypeScript port keeps its historical default material; C++ starts with none.
        this.material = new SimpleMaterial();
        this.ownsMaterial = true;
        this.texture = null;
        this.ownsTexture = true;
        this.normalMap = null;
        this.ownsNormalMap = true;
        this.normalMapRgb = null;
    }

    public supportsDecomposedPlacement(): boolean {
        return !this.explicitAffinePlacement;
    }

    /// Only plain SimpleBody instances; subclasses opt in themselves
    protected override denotesPlacedGeometry(): boolean {
        return this.constructor === SimpleBody;
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

    private updateTransformFlags(): void {
        this.hasIdentityRotation = SimpleBody.isIdentityRotation(this.rotation);
        this.hasUnitScale =
            Math.abs(this.scale.x() - 1.0) <= VSDK.EPSILON &&
            Math.abs(this.scale.y() - 1.0) <= VSDK.EPSILON &&
            Math.abs(this.scale.z() - 1.0) <= VSDK.EPSILON;
        this.hasZeroTranslation =
            Math.abs(this.position.x()) <= VSDK.EPSILON &&
            Math.abs(this.position.y()) <= VSDK.EPSILON &&
            Math.abs(this.position.z()) <= VSDK.EPSILON;
        this.hasTranslationOnlyTransform = this.hasIdentityRotation && this.hasUnitScale;
        this.hasIdentityTransform = this.hasTranslationOnlyTransform && this.hasZeroTranslation;
    }

    private updateDecomposedPlacement(): void {
        const forward = new Matrix4x4d()
            .translation(this.position)
            .multiply(this.rotation.multiply(new Matrix4x4d().scale(this.scale)));
        const inverse = new Matrix4x4d()
            .scale(this.inverseScale)
            .multiply(this.rotationInverse)
            .multiply(new Matrix4x4d().translation(this.position.multiply(-1)));
        this.affinePlacement.assign(forward, inverse);
    }

    public getRotation(): Matrix4x4d {
        if (this.explicitAffinePlacement)
            throw new IllegalStateException("Rotation is not defined for an arbitrary affine placement");
        return this.rotation;
    }

    public setRotation(r: Matrix4x4d): void {
        if (this.explicitAffinePlacement)
            throw new IllegalStateException("Cannot decompose an arbitrary affine placement");
        const sanitized = r.withoutTranslation();
        this.rotationQuaternion = sanitized.exportToQuaternion().normalized();
        this.rotation = sanitized;
        this.rotationInverseQuaternion = this.rotationQuaternion.conjugated();
        this.rotationInverse = new Matrix4x4d().importFromQuaternion(this.rotationInverseQuaternion);
        this.updateTransformFlags();
        this.updateDecomposedPlacement();
        this.markModified();
    }

    public getRotationInverse(): Matrix4x4d {
        if (this.explicitAffinePlacement)
            throw new IllegalStateException("Rotation is not defined for an arbitrary affine placement");
        return this.rotationInverse;
    }

    public setRotationInverse(ri: Matrix4x4d): void {
        if (this.explicitAffinePlacement)
            throw new IllegalStateException("Cannot decompose an arbitrary affine placement");
        const sanitized = ri.withoutTranslation();
        this.rotationInverseQuaternion = sanitized.exportToQuaternion().normalized();
        this.rotationInverse = sanitized;
        this.rotationQuaternion = this.rotationInverseQuaternion.conjugated();
        this.rotation = new Matrix4x4d().importFromQuaternion(this.rotationQuaternion);
        this.updateTransformFlags();
        this.updateDecomposedPlacement();
        this.markModified();
    }

    public getMaterial(): SimpleMaterial | null {
        return this.material;
    }

    public override getSurfaceMaterial(): SimpleMaterial | null {
        return this.material;
    }

    /**
    Sets the material, taking its ownership (it is deleted with this body or
    when replaced).
    @param m new material, or null
    */
    public setMaterial(m: SimpleMaterial | null): void {
        this.material = m;
        this.ownsMaterial = true;
        this.markModified();
    }

    /**
    Sets a material shared with other owners: this body does not delete it.
    @param m shared material, or null
    */
    public setMaterialReference(m: SimpleMaterial | null): void {
        this.material = m;
        this.ownsMaterial = false;
        this.markModified();
    }

    public getTexture(): Image | null {
        return this.texture;
    }

    /**
    Sets the texture, taking its ownership (it is deleted with this body or
    when replaced).
    @param inTexture new texture, or null
    */
    public setTexture(inTexture: Image | null): void {
        this.texture = inTexture;
        this.ownsTexture = true;
        this.markModified();
    }

    /**
    Sets a texture shared with other owners, such as a scene resource
    (`SimpleScene.addImage`): this body does not delete it.
    @param inTexture shared texture, or null
    */
    public setTextureReference(inTexture: Image | null): void {
        this.texture = inTexture;
        this.ownsTexture = false;
        this.markModified();
    }

    public getNormalMap(): NormalMap | null {
        return this.normalMap;
    }

    public getNormalMapRgb(): RGBImageUncompressed | null {
        return this.normalMapRgb;
    }

    /**
    Sets the normal map, taking its ownership.
    @param inNormalMap new normal map, or null
    */
    public setNormalMap(inNormalMap: NormalMap | null): void {
        this.normalMap = inNormalMap;
        this.ownsNormalMap = true;
        this.normalMapRgb = inNormalMap ? new NormalMapTexture(inNormalMap) : null;
        this.markModified();
    }

    /**
    Sets a normal map shared with other owners, such as a scene resource
    (`SimpleScene.addNormalMap`): this body does not delete it. The body
    keeps its own RGB encoding of the map.
    @param inNormalMap shared normal map, or null
    */
    public setNormalMapReference(inNormalMap: NormalMap | null): void {
        this.normalMap = inNormalMap;
        this.ownsNormalMap = false;
        this.normalMapRgb = inNormalMap ? new NormalMapTexture(inNormalMap) : null;
        this.markModified();
    }

    public getPosition(): Vector3Dd {
        return this.explicitAffinePlacement ? this.affinePlacement.getForward().extractTranslation() : this.position;
    }

    public setPosition(p: Vector3Dd): void {
        if (this.explicitAffinePlacement) {
            const forward = this.affinePlacement.getForward().withTranslation(p);
            const inverseTranslation = this.affinePlacement.getInverse().withoutTranslation().multiply(p).multiply(-1);
            const inverse = this.affinePlacement.getInverse().withTranslation(inverseTranslation);
            const candidate = new AffinePlacement();
            if (candidate.assign(forward, inverse) !== AffinePlacementStatus.Valid)
                throw new IllegalStateException("Invalid affine translation");
            this.affinePlacement = candidate;
            this.markModified();
            return;
        }
        this.position = p;
        this.updateTransformFlags();
        this.updateDecomposedPlacement();
        this.markModified();
    }

    public getScale(): Vector3Dd {
        if (this.explicitAffinePlacement)
            throw new IllegalStateException("Scale is not defined for an arbitrary affine placement");
        return this.scale;
    }

    public getTransformationMatrix(): Matrix4x4d {
        return this.affinePlacement.getForward();
    }

    public override getAffinePlacement(): Matrix4x4d {
        return this.affinePlacement.getForward();
    }

    public override setAffinePlacement(forward: Matrix4x4d, inverse: Matrix4x4d): AffinePlacementStatus {
        const candidate = new AffinePlacement();
        const status = candidate.assign(forward, inverse);
        if (status !== AffinePlacementStatus.Valid) return status;
        this.affinePlacement = candidate;
        this.explicitAffinePlacement = true;
        this.markModified();
        return status;
    }

    // Reset to identity and re-enable decomposed editing.
    public override resetAffinePlacement(): void {
        this.explicitAffinePlacement = false;
        this.position = new Vector3Dd();
        this.scale = new Vector3Dd(1, 1, 1);
        this.inverseScale = new Vector3Dd(1, 1, 1);
        this.rotation = Matrix4x4d.identityMatrix();
        this.rotationInverse = this.rotation;
        this.rotationQuaternion = this.rotation.exportToQuaternion();
        this.rotationInverseQuaternion = this.rotationQuaternion;
        this.hasInvertibleScale = true;
        this.updateTransformFlags();
        this.updateDecomposedPlacement();
        this.markModified();
    }

    public setScale(s: Vector3Dd): void {
        if (this.explicitAffinePlacement)
            throw new IllegalStateException("Cannot decompose an arbitrary affine placement");
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
        this.updateDecomposedPlacement();
        this.markModified();
    }

    /**
    Transforms an object-space surface normal into world space (TypeScript
    helper kept for HiddenLineRenderer): the inverse transpose of the placement,
    renormalized.
    */
    public transformNormalToWorld(normal: Vector3Dd): Vector3Dd {
        const inverse = this.affinePlacement.getInverse();
        const n = normal;
        const world = new Vector3Dd(
            inverse.get(0, 0) * n.x() + inverse.get(1, 0) * n.y() + inverse.get(2, 0) * n.z(),
            inverse.get(0, 1) * n.x() + inverse.get(1, 1) * n.y() + inverse.get(2, 1) * n.z(),
            inverse.get(0, 2) * n.x() + inverse.get(1, 2) * n.y() + inverse.get(2, 2) * n.z(),
        );
        return world.length() <= VSDK.EPSILON ? world : world.normalized();
    }

    /** Transforms an object-space point into world space (TypeScript helper). */
    public transformPointToWorld(point: Vector3Dd): Vector3Dd {
        return this.affinePlacement.getForward().multiply(point);
    }

    public doIntersectionFirstHit(inRay: Ray): Ray | null;
    public doIntersectionFirstHit(inOutRay: Ray, outHit: RayHit | null): boolean;
    public doIntersectionFirstHit(inOutRay: Ray, outHitArgument?: RayHit | null): boolean | Ray | null {
        if (arguments.length === 1) {
            const firstHit = new RayHit();
            if (!this.doIntersectionFirstHit(inOutRay, firstHit) || firstHit.getRay() === null) {
                return null;
            }
            return new Ray(firstHit.getRay()!);
        }
        const outHit: RayHit | null = outHitArgument ?? null;
        if (this.geometry === null || (!this.explicitAffinePlacement && !this.hasInvertibleScale)) {
            return false;
        }
        if (this.explicitAffinePlacement) return this.doIntersectionWithAffinePlacement(inOutRay, outHit);

        const requestedDetailMask = outHit !== null ? outHit.getRequiredDetailMask() : RayHit.DETAIL_NONE;

        if (
            this.hasTranslationOnlyTransform &&
            requestedDetailMask === RayHit.DETAIL_NONE &&
            this.hasSphereGeometry()
        ) {
            return this.doIntersectionWithTranslationOnlySphereFastPath(inOutRay, outHit);
        }

        if (this.hasIdentityTransform) {
            return SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, inOutRay, outHit as RayHit);
        }

        if (this.hasTranslationOnlyTransform) {
            return this.doIntersectionWithTranslationOnly(inOutRay, outHit, requestedDetailMask);
        }

        const translatedOrigin = inOutRay.getOrigin().subtract(this.position);
        const rotatedOrigin = this.rotationInverseQuaternion.rotate(translatedOrigin);
        const localOrigin = new Vector3Dd(
            rotatedOrigin.x() * this.inverseScale.x(),
            rotatedOrigin.y() * this.inverseScale.y(),
            rotatedOrigin.z() * this.inverseScale.z(),
        );

        const rotatedDirection = this.rotationInverseQuaternion.rotate(inOutRay.getDirection());
        let localDirection = new Vector3Dd(
            rotatedDirection.x() * this.inverseScale.x(),
            rotatedDirection.y() * this.inverseScale.y(),
            rotatedDirection.z() * this.inverseScale.z(),
        );
        const localDirectionLength = localDirection.length();
        if (localDirectionLength <= VSDK.EPSILON) {
            return false;
        }
        localDirection = localDirection.multiply(1.0 / localDirectionLength);

        let localRayT = inOutRay.getT();
        if (localRayT >= Double.MAX_VALUE / localDirectionLength) {
            localRayT = Double.MAX_VALUE;
        } else {
            localRayT *= localDirectionLength;
        }

        const localRay = new Ray(localOrigin, localDirection, localRayT);
        const requestedStoreRay = outHit !== null ? outHit.shouldStoreRay() : false;

        if (outHit !== null) {
            outHit.setStoreRay(false);
            outHit.resetForDistanceOnly();
            if (!SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, localRay, outHit)) {
                outHit.setStoreRay(requestedStoreRay);
                outHit.setRequiredDetailMask(requestedDetailMask);
                return false;
            }
        } else {
            const localHitStorage = new RayHit();
            localHitStorage.setStoreRay(false);
            localHitStorage.resetForDistanceOnly();
            if (!SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, localRay, localHitStorage)) {
                return false;
            }
        }

        if (outHit !== null) {
            let localHitT: number;
            if (outHit.getRay() !== null) {
                localHitT = outHit.getRay()!.getT();
            } else if (outHit.hasHitDistance()) {
                localHitT = outHit.getHitDistance();
            } else {
                return false;
            }

            const worldT = localHitT / localDirectionLength;

            outHit.setStoreRay(requestedStoreRay);
            outHit.setRequiredDetailMask(requestedDetailMask);
            if (requestedStoreRay || requestedDetailMask !== RayHit.DETAIL_NONE) {
                const worldRay = inOutRay.withT(worldT);
                outHit.setRay(worldRay);
                if (requestedDetailMask !== RayHit.DETAIL_NONE) {
                    this.doExtraInformation(worldRay, worldT, outHit);
                }
            } else {
                outHit.setHitDistance(worldT);
            }
        }
        return true;
    }

    private doIntersectionWithTranslationOnlySphereFastPath(inOutRay: Ray, outHit: RayHit | null): boolean {
        const sphere = this.geometry as Sphere;
        const t = [0],
            farT = [0];
        if (
            Sphere.intersectionRoots(
                inOutRay.getOrigin().subtract(this.position),
                inOutRay.getDirection(),
                sphere.getRadius(),
                new RayCrossingQuery(0, Double.POSITIVE_INFINITY),
                t,
                farT,
            ) === 0
        ) {
            return false;
        }

        if (outHit !== null) {
            if (outHit.shouldStoreRay()) {
                outHit.setRay(inOutRay.withT(t[0]!));
            } else {
                outHit.setHitDistance(t[0]!);
            }
        }
        return true;
    }

    private doIntersectionWithAffinePlacement(ray: Ray, outHit: RayHit | null): boolean {
        const inverse = this.affinePlacement.getInverse();
        const origin = inverse.multiply(ray.getOrigin());
        const direction = inverse.withoutTranslation().multiply(ray.getDirection());
        const length = direction.length();
        if (!Double.isFinite(length) || length <= VSDK.EPSILON) return false;
        let localT = ray.getT();
        if (localT >= Double.MAX_VALUE / length) localT = Double.MAX_VALUE;
        else localT *= length;
        const localRay = new Ray(origin, direction, localT);
        const localHit = new RayHit(RayHit.DETAIL_NONE, false);
        if (!SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, localRay, localHit)) return false;
        const distance = localHit.getRay() ? localHit.getRay()!.getT() : localHit.getHitDistance();
        if (!localHit.getRay() && !localHit.hasHitDistance()) return false;
        const worldT = distance / length;
        if (!Double.isFinite(worldT)) return false;
        if (outHit) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                const worldRay = ray.withT(worldT);
                outHit.setRay(worldRay);
                if (outHit.needsAnySurfaceData()) this.doExtraInformationWithAffinePlacement(worldRay, worldT, outHit);
            } else outHit.setHitDistance(worldT);
        }
        return true;
    }

    private doExtraInformationWithAffinePlacement(ray: Ray, t: number, hit: RayHit): void {
        const forward = this.affinePlacement.getForward();
        const inverse = this.affinePlacement.getInverse();
        const origin = inverse.multiply(ray.getOrigin());
        const rawDirection = inverse.withoutTranslation().multiply(ray.getDirection());
        const length = rawDirection.length();
        if (!Double.isFinite(length) || length <= VSDK.EPSILON) return;
        const localRay = new Ray(origin, rawDirection, t * length);
        this.geometry!.doExtraInformation(localRay, t * length, hit);
        if (hit.needsPoint()) hit.point = forward.multiply(hit.point);
        if (hit.needsNormal()) {
            const n = hit.normal;
            hit.normal = new Vector3Dd(
                inverse.get(0, 0) * n.x() + inverse.get(1, 0) * n.y() + inverse.get(2, 0) * n.z(),
                inverse.get(0, 1) * n.x() + inverse.get(1, 1) * n.y() + inverse.get(2, 1) * n.z(),
                inverse.get(0, 2) * n.x() + inverse.get(1, 2) * n.y() + inverse.get(2, 2) * n.z(),
            ).normalized();
        }
        if (hit.needsTangent()) hit.tangent = forward.withoutTranslation().multiply(hit.tangent).normalized();
        hit.material = this.material;
        hit.texture = this.texture;
        hit.normalMap = this.normalMap;
        hit.setRay(ray.withT(t));
    }

    private doIntersectionWithTranslationOnly(
        inOutRay: Ray,
        outHit: RayHit | null,
        requiredDetailMask: number,
    ): boolean {
        const localRay = inOutRay.withOrigin(inOutRay.getOrigin().subtract(this.position));

        const localHitStorage = outHit !== null ? null : new RayHit();
        const hit = outHit !== null ? outHit : localHitStorage!;

        if (requiredDetailMask === RayHit.DETAIL_NONE) {
            hit.resetForDistanceOnly();
        } else {
            hit.reset(requiredDetailMask);
        }

        if (!SurfaceRayIntersection.doIntersectionFirstHit(this.geometry, localRay, hit)) {
            return false;
        }

        let localHitT: number;
        if (hit.getRay() !== null) {
            localHitT = hit.getRay()!.getT();
        } else if (hit.hasHitDistance()) {
            localHitT = hit.getHitDistance();
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
                outHit.point = hit.point.add(this.position);
            }
            if (outHit.needsNormal()) {
                outHit.normal = hit.normal;
            }
            if (outHit.needsTextureCoordinates()) {
                outHit.u = hit.u;
                outHit.v = hit.v;
            }
            if (outHit.needsTangent()) {
                outHit.tangent = hit.tangent;
            }
            outHit.material = hit.material;
            outHit.texture = hit.texture;
            outHit.normalMap = hit.normalMap;
        }

        return true;
    }

    public computeQuantitativeInvisibility(origin: Vector3Dd, p: Vector3Dd): number {
        if (this.geometry === null || (!this.explicitAffinePlacement && !this.hasInvertibleScale)) {
            return 0;
        }
        if (this.explicitAffinePlacement)
            return this.geometry.computeQuantitativeInvisibility(
                this.affinePlacement.getInverse().multiply(origin),
                this.affinePlacement.getInverse().multiply(p),
            );

        const translatedOrigin = origin.subtract(this.position);
        const rotatedOrigin = this.rotationInverseQuaternion.rotate(translatedOrigin);
        const myOrigin = new Vector3Dd(
            rotatedOrigin.x() * this.inverseScale.x(),
            rotatedOrigin.y() * this.inverseScale.y(),
            rotatedOrigin.z() * this.inverseScale.z(),
        );

        const translatedPoint = p.subtract(this.position);
        const rotatedPoint = this.rotationInverseQuaternion.rotate(translatedPoint);
        const myPoint = new Vector3Dd(
            rotatedPoint.x() * this.inverseScale.x(),
            rotatedPoint.y() * this.inverseScale.y(),
            rotatedPoint.z() * this.inverseScale.z(),
        );

        return this.geometry.computeQuantitativeInvisibility(myOrigin, myPoint);
    }

    public override doExtraInformation(_inRay: Ray, _inT: number, outData: RayHit | null): void {
        if (outData === null || this.geometry === null || (!this.explicitAffinePlacement && !this.hasInvertibleScale))
            return;
        if (this.explicitAffinePlacement) {
            if (outData.getRay())
                this.doExtraInformationWithAffinePlacement(outData.getRay()!, outData.getRay()!.getT(), outData);
            return;
        }

        const worldRayPtr = outData.getRay();
        if (worldRayPtr === null) {
            outData.material = this.material;
            outData.texture = this.texture;
            outData.normalMap = this.normalMap;
            return;
        }

        const worldRay = worldRayPtr;
        const worldT = worldRay.getT();

        const translatedOrigin = worldRay.getOrigin().subtract(this.position);
        const rotatedOrigin = this.rotationInverseQuaternion.rotate(translatedOrigin);
        const localOrigin = new Vector3Dd(
            rotatedOrigin.x() * this.inverseScale.x(),
            rotatedOrigin.y() * this.inverseScale.y(),
            rotatedOrigin.z() * this.inverseScale.z(),
        );

        const rotatedDirection = this.rotationInverseQuaternion.rotate(worldRay.getDirection());
        let localDirection = new Vector3Dd(
            rotatedDirection.x() * this.inverseScale.x(),
            rotatedDirection.y() * this.inverseScale.y(),
            rotatedDirection.z() * this.inverseScale.z(),
        );
        const localDirLength = localDirection.length();
        if (localDirLength <= VSDK.EPSILON) {
            outData.material = this.material;
            outData.texture = this.texture;
            outData.normalMap = this.normalMap;
            return;
        }
        localDirection = localDirection.multiply(1.0 / localDirLength);
        const localT = worldT * localDirLength;
        const localRay = new Ray(localOrigin, localDirection, localT);

        this.geometry.doExtraInformation(localRay, localT, outData);

        if (outData.needsPoint()) {
            const worldPoint = this.rotation
                .multiply(
                    new Vector3Dd(
                        outData.point.x() * this.scale.x(),
                        outData.point.y() * this.scale.y(),
                        outData.point.z() * this.scale.z(),
                    ),
                )
                .add(this.position);
            outData.point = worldPoint;
        }

        if (outData.needsNormal()) {
            const worldNormal = this.rotation
                .multiply(
                    new Vector3Dd(
                        outData.normal.x() * this.inverseScale.x(),
                        outData.normal.y() * this.inverseScale.y(),
                        outData.normal.z() * this.inverseScale.z(),
                    ),
                )
                .normalized();
            outData.normal = worldNormal;
        }

        if (outData.needsTangent()) {
            const worldTangent = this.rotation
                .multiply(
                    new Vector3Dd(
                        outData.tangent.x() * this.scale.x(),
                        outData.tangent.y() * this.scale.y(),
                        outData.tangent.z() * this.scale.z(),
                    ),
                )
                .normalized();
            outData.tangent = worldTangent;
        }

        outData.material = this.material;
        outData.texture = this.texture;
        outData.normalMap = this.normalMap;
        outData.setRay(worldRay.withT(worldT));
    }

    private queryPlacementStatus(): QueryPlacementStatus {
        const position = this.position,
            scale = this.scale;
        if (
            !Double.isFinite(position.x()) ||
            !Double.isFinite(position.y()) ||
            !Double.isFinite(position.z()) ||
            !Double.isFinite(scale.x()) ||
            !Double.isFinite(scale.y()) ||
            !Double.isFinite(scale.z()) ||
            scale.x() === 0 ||
            scale.y() === 0 ||
            scale.z() === 0
        )
            return QueryPlacementStatus.Invalid;
        if (!this.hasInvertibleScale) return QueryPlacementStatus.Indeterminate;
        // This scene type promises a decomposed rotation, not a general affine matrix.
        // Reject non-orthogonal or inconsistent matrices rather than silently using
        // the quaternion-derived inverse as if it were the inverse of a shear.
        for (let row = 0; row < 3; row++) {
            for (let column = 0; column < 3; column++) {
                let product = 0,
                    orthogonal = 0;
                for (let k = 0; k < 3; k++) {
                    const a = this.rotation.get(row, k),
                        inv = this.rotationInverse.get(k, column);
                    const x = this.rotation.get(k, row),
                        y = this.rotation.get(k, column);
                    if (!Double.isFinite(a) || !Double.isFinite(inv) || !Double.isFinite(x) || !Double.isFinite(y))
                        return QueryPlacementStatus.Invalid;
                    product += a * inv;
                    orthogonal += x * y;
                }
                const expected = row === column ? 1.0 : 0.0;
                if (
                    !Double.isFinite(product) ||
                    !Double.isFinite(orthogonal) ||
                    JavaMath.abs(product - expected) > 1e-8 ||
                    JavaMath.abs(orthogonal - expected) > 1e-8
                )
                    return QueryPlacementStatus.Indeterminate;
            }
        }
        return QueryPlacementStatus.Valid;
    }

    public override queryAffinePlacement(forward: Matrix4x4d[], inverse: Matrix4x4d[]): AffinePlacementStatus {
        if (this.explicitAffinePlacement) {
            forward[0] = this.affinePlacement.getForward();
            inverse[0] = this.affinePlacement.getInverse();
            return this.affinePlacement.getStatus();
        }
        const status = this.queryPlacementStatus();
        if (status === QueryPlacementStatus.Invalid) return AffinePlacementStatus.Invalid;
        if (status === QueryPlacementStatus.Indeterminate) return AffinePlacementStatus.Indeterminate;
        forward[0] = this.affinePlacement.getForward();
        inverse[0] = this.affinePlacement.getInverse();
        return this.affinePlacement.getStatus();
    }
}
