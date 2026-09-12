import { PolyhedralBoundedSolidEulerOperators } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { VSDK } from "../../../common/VSDK.js";
import { Ray } from "../element/Ray.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { RayHit } from "../element/RayHit.js";
import type { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "../geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { Cone } from "./Cone.js";
import { Solid } from "./Solid.js";

export class Arrow extends Solid {
    private static readonly NO_HIT = Number.POSITIVE_INFINITY;

    private baseLength: number;
    private headLength: number;
    private baseRadius: number;
    private headRadius: number;

    private baseCylinder: Cone;
    private headCone: Cone;
    private lastElement: Cone;

    private static readonly DEFAULT_CIRCUMFERENCE_DIVISIONS = Math.trunc(36 / 4);
    private static readonly DEFAULT_HEIGHT_DIVISIONS = 1;
    private static readonly MIN_CIRCUMFERENCE_DIVISIONS = 3;
    private static readonly MIN_HEIGHT_DIVISIONS = 1;

    public constructor(baseLength: number, headLength: number, baseRadius: number, headRadius: number) {
        super();
        this.baseLength = baseLength;
        this.headLength = headLength;
        this.baseRadius = baseRadius;
        this.headRadius = headRadius;
        this.baseCylinder = new Cone(baseRadius, baseRadius, baseLength);
        this.headCone = new Cone(headRadius, 0, headLength);
        this.lastElement = this.baseCylinder;
    }

    public getBaseLength(): number {
        return this.baseLength;
    }

    public setBaseLength(val: number): void {
        this.baseLength = val;
        this.baseCylinder.setHeight(val);
    }

    public getHeadLength(): number {
        return this.headLength;
    }

    public setHeadLength(val: number): void {
        this.headLength = val;
        this.headCone.setHeight(val);
    }

    public getBaseRadius(): number {
        return this.baseRadius;
    }

    public setBaseRadius(val: number): void {
        this.baseRadius = val;
        this.baseCylinder.setBaseRadius(val);
        this.baseCylinder.setTopRadius(val);
    }

    public getHeadRadius(): number {
        return this.headRadius;
    }

    public setHeadRadius(val: number): void {
        this.headRadius = val;
        this.headCone.setBaseRadius(val);
    }

    private doIntersectionDistanceOnly(inRay: Ray, outHit: RayHit | null): boolean {
        const shiftedHeadOrigin = new Vector3Dd(
            inRay.getOrigin().x(),
            inRay.getOrigin().y(),
            inRay.getOrigin().z() - this.baseLength,
        );
        const shiftedHeadRay = new Ray(shiftedHeadOrigin, inRay.getDirection(), inRay.getT());

        let candidateHit: RayHit;
        let shouldStoreRay = false;
        if (outHit !== null) {
            candidateHit = outHit;
            shouldStoreRay = outHit.shouldStoreRay();
        } else {
            candidateHit = new RayHit(RayHit.DETAIL_NONE, false);
        }
        candidateHit.setStoreRay(false);

        let baseT = Arrow.NO_HIT;
        candidateHit.resetForDistanceOnly();
        if (this.baseCylinder.doIntersectionFirstHit(inRay, candidateHit)) {
            baseT = candidateHit.hitDistance();
        }

        let headT = Arrow.NO_HIT;
        candidateHit.resetForDistanceOnly();
        if (this.headCone.doIntersectionFirstHit(shiftedHeadRay, candidateHit)) {
            headT = candidateHit.hitDistance();
        }

        const winnerT = baseT < headT ? baseT : headT;
        if (winnerT === Arrow.NO_HIT) {
            return false;
        }

        if (outHit !== null) {
            if (shouldStoreRay) {
                outHit.setRay(inRay.withT(winnerT));
            } else {
                outHit.setHitDistance(winnerT);
            }
            outHit.setStoreRay(shouldStoreRay);
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    Java overload `doIntersectionFirstHit(Ray inOutRay)`.
    @param inOutRay
    @return true if given ray intersects current Arrow
    */
    public doIntersectionFirstHit(inOutRay: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            const inOutRay = inRay;
            const tr = new Vector3Dd(0, 0, -this.baseLength);

            const headRay = new Ray(inOutRay.getOrigin().add(tr), inOutRay.getDirection());
            const baseRay = new Ray(inOutRay);

            const baseHit = this.baseCylinder.doIntersectionFirstHit(baseRay);
            const headHit = this.headCone.doIntersectionFirstHit(headRay);

            if (
                (baseHit !== null && headHit === null) ||
                (baseHit !== null && headHit !== null && baseHit.getT() < headHit.getT())
            ) {
                this.lastElement = this.baseCylinder;
                return inOutRay.withT(baseHit.getT());
            } else if (
                (baseHit === null && headHit !== null) ||
                (baseHit !== null && headHit !== null && headHit.getT() < baseHit.getT())
            ) {
                this.lastElement = this.headCone;
                return inOutRay.withT(headHit.getT());
            }

            return null;
        }

        if (outHit === null || !outHit.needsAnySurfaceData()) {
            return this.doIntersectionDistanceOnly(inRay, outHit);
        }

        const tr = new Vector3Dd(0, 0, -this.baseLength);
        const shiftedHeadRay = new Ray(inRay.getOrigin().add(tr), inRay.getDirection(), inRay.getT());

        const baseHit = new RayHit(outHit.requiredDetailMask());
        const headHit = new RayHit(outHit.requiredDetailMask());
        const hasBase = this.baseCylinder.doIntersectionFirstHit(inRay, baseHit);
        const hasHead = this.headCone.doIntersectionFirstHit(shiftedHeadRay, headHit);

        if (!hasBase && !hasHead) {
            return false;
        }

        const baseT = hasBase ? (baseHit.ray() !== null ? baseHit.ray()!.getT() : baseHit.hitDistance()) : Arrow.NO_HIT;
        const headT = hasHead ? (headHit.ray() !== null ? headHit.ray()!.getT() : headHit.hitDistance()) : Arrow.NO_HIT;

        if (hasBase && (!hasHead || baseT < headT)) {
            outHit.clone(baseHit);
            outHit.setRay(inRay.withT(baseT));
        } else {
            outHit.clone(headHit);
            outHit.setRay(inRay.withT(headT));
            if (outHit.p !== null) {
                outHit.p = new Vector3Dd(outHit.p.x(), outHit.p.y(), outHit.p.z() + this.baseLength);
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param inT
    @param outData
    */
    public override doExtraInformation(inRay: Ray, inT: number, outData: RayHit): void {
        const hit = new RayHit();
        if (this.doIntersectionFirstHit(inRay.withT(inT), hit)) {
            outData.clone(hit);
        }
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        const minmax = new Float64Array(6);
        const r = Math.max(this.baseRadius, this.headRadius);

        minmax[0] = -r;
        minmax[1] = -r;
        minmax[2] = 0;
        minmax[3] = r;
        minmax[4] = r;
        minmax[5] = this.baseLength + this.headLength;

        return minmax;
    }

    /**
    Java overloads `exportToPolyhedralBoundedSolid()` and
    `exportToPolyhedralBoundedSolid(int circumferenceDivisions, int heightDivisions)`.
    */
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(
        circumferenceDivisions: number,
        heightDivisions: number,
    ): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(
        circumferenceDivisions?: number,
        heightDivisions?: number,
    ): PolyhedralBoundedSolid {
        if (circumferenceDivisions === undefined || heightDivisions === undefined) {
            return this.buildPolyhedralBoundedSolid(
                Arrow.DEFAULT_CIRCUMFERENCE_DIVISIONS,
                Arrow.DEFAULT_HEIGHT_DIVISIONS,
            );
        }
        const normalizedCircumferenceDivisions = Math.max(Arrow.MIN_CIRCUMFERENCE_DIVISIONS, circumferenceDivisions);
        const normalizedHeightDivisions = Math.max(Arrow.MIN_HEIGHT_DIVISIONS, heightDivisions);

        if (
            normalizedCircumferenceDivisions === Arrow.DEFAULT_CIRCUMFERENCE_DIVISIONS &&
            normalizedHeightDivisions === Arrow.DEFAULT_HEIGHT_DIVISIONS
        ) {
            return this.exportToPolyhedralBoundedSolid();
        }

        return this.buildPolyhedralBoundedSolid(normalizedCircumferenceDivisions, normalizedHeightDivisions);
    }

    /**
    Current implementation of the cylinder follows the idea suggested on
    section [MANT1988].12.3.1 and program [MANT1988].12.4, where the
    cylinder is built upon a circular lamina base and an extrusion
    (translational sweep) operation. The cone case is done manually,
    */
    private static closeTopFaceToApex(solid: PolyhedralBoundedSolid, nsides: number, apexZ: number): void {
        const apex = new Vector3Dd(0, 0, apexZ);
        let i: number;
        const base1 = 2 * nsides + 1;
        const base2 = 3 * nsides + 1;

        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, base1, base2, apex);

        for (i = 0; i < nsides - 2; i++) {
            PolyhedralBoundedSolidEulerOperators.mef(
                solid,
                1,
                1,
                base2,
                base1 + i,
                base1 + i + 1,
                base1 + i + 2,
                base2 + i + 1,
            );
        }

        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, base2, base1 + i, base1 + i + 1, base1, base2 + i + 1);
    }

    private buildPolyhedralBoundedSolid(nsides: number, heightDivisions: number): PolyhedralBoundedSolid {
        let T: Matrix4x4d;
        let S: Matrix4x4d;
        let M: Matrix4x4d;

        const solid = PolyhedralBoundedSolidModeler.createCircularLamina(0.0, 0.0, this.baseRadius, 0.0, nsides);

        // Cylinder case
        const cylinderZStep = this.baseLength / heightDivisions;
        let i: number;
        for (i = 0; i < heightDivisions; i++) {
            T = new Matrix4x4d();
            T = T.translation(0.0, 0.0, cylinderZStep);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1), T);
        }

        T = new Matrix4x4d();
        T = T.translation(0.0, 0.0, 0);
        const f = this.headRadius / this.baseRadius;
        S = new Matrix4x4d();
        S = S.scale(f, f, 1);
        M = T.multiply(S);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1), M);

        // Cone case
        let prevRadius = this.headRadius;
        const coneZStep = this.headLength / heightDivisions;
        for (i = 1; i < heightDivisions; i++) {
            const nextRadius = this.headRadius * (1.0 - i / heightDivisions);
            const coneScale = prevRadius > VSDK.EPSILON ? nextRadius / prevRadius : 0.0;
            T = new Matrix4x4d();
            T = T.translation(0.0, 0.0, coneZStep);
            S = new Matrix4x4d();
            S = S.scale(coneScale, coneScale, 1.0);
            M = T.multiply(S);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1), M);
            prevRadius = nextRadius;
        }

        Arrow.closeTopFaceToApex(solid, nsides, this.baseLength + this.headLength);

        return solid;
    }
}
