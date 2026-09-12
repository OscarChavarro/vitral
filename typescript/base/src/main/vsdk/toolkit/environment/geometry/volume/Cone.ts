//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { PolyhedralBoundedSolidEulerOperators } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { VSDK } from "../../../common/VSDK.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import type { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "./polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { PolyhedralBoundedSolidModeler } from "../geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { Solid } from "./Solid.js";

export class Cone extends Solid {
    private static readonly NO_HIT = Number.POSITIVE_INFINITY;

    private r1: number; // Radius at the base
    private r2: number; // Radius at the top
    private h: number; // Height

    private static readonly DEFAULT_CIRCUMFERENCE_DIVISIONS = 36;
    private static readonly DEFAULT_HEIGHT_DIVISIONS = 1;
    private static readonly MIN_CIRCUMFERENCE_DIVISIONS = 3;
    private static readonly MIN_HEIGHT_DIVISIONS = 1;

    public constructor(r1: number, r2: number, h: number) {
        super();
        this.r1 = r1;
        this.r2 = r2;
        this.h = h;
    }

    public getBaseRadius(): number {
        return this.r1;
    }

    public getTopRadius(): number {
        return this.r2;
    }

    public getHeight(): number {
        return this.h;
    }

    public setBaseRadius(val: number): void {
        this.r1 = val;
    }

    public setTopRadius(val: number): void {
        this.r2 = val;
    }

    public setHeight(val: number): void {
        this.h = val;
    }

    /**
    Java overloads `doIntersectionCylinder(Ray)` (always null) and
    `doIntersectionCylinder(Ray, double, double, RayHit)`.
    */
    private doIntersectionCylinder(inOutRay: Ray, inR?: number, inH?: number, outInfo?: RayHit | null): Ray | null {
        if (inR === undefined || inH === undefined || outInfo === undefined) {
            return null;
        }
        let A: number;
        let B: number;
        let C: number;
        let discriminant: number;
        let t0: number;
        const ox = inOutRay.getOrigin().x();
        const oy = inOutRay.getOrigin().y();
        const oz = inOutRay.getOrigin().z();
        const dx = inOutRay.getDirection().x();
        const dy = inOutRay.getDirection().y();
        const dz = inOutRay.getDirection().z();

        //- Calcula el termino A --------------------------------------------
        A = VSDK.square(dx) + VSDK.square(dy);
        if (Math.abs(A) <= VSDK.EPSILON) return null;

        //- Calcula el termino B --------------------------------------------
        B = 2 * (dx * ox + dy * oy);

        //- Calcula el termino C --------------------------------------------
        C = VSDK.square(ox) + VSDK.square(oy) - VSDK.square(inR);

        //- Calcula el discriminant. Si el discriminant no es positivo el -
        //- rayo no intersecta el cilindro. retorna t = 0
        discriminant = VSDK.square(B) - 4 * A * C;
        if (discriminant <= VSDK.EPSILON) return null;

        //- Resuelve la ecuacion cuadratica para las raices de la ecuacion. -
        //- (-B +/- sqrt(B^2 - 4*A*C)) / 2A.                                -
        discriminant = Math.sqrt(discriminant);
        t0 = (-B - discriminant) / (2 * A);

        //- Si t0 es > 0 listo. Si no debemos calcular la otra raiz t1. -----
        if (t0 > VSDK.EPSILON) {
            const pz = oz + dz * t0;
            if (pz > inH || pz < 0) {
                return null;
            }

            if (outInfo !== null) {
                const px = ox + dx * t0;
                const py = oy + dy * t0;
                outInfo.p = new Vector3Dd(px, py, pz);
                outInfo.n = new Vector3Dd(px, py, 0).normalized();
            }
            return inOutRay.withT(t0);
        }

        return null;
    }

    private doIntersectionCone(inOutRay: Ray, inR: number, inH: number, outInfo: RayHit | null): Ray | null {
        let A: number;
        let B: number;
        let C: number;
        let discriminant: number;
        let t0: number;
        const ox = inOutRay.getOrigin().x();
        const oy = inOutRay.getOrigin().y();
        const oz = inOutRay.getOrigin().z();
        const dx = inOutRay.getDirection().x();
        const dy = inOutRay.getDirection().y();
        const dz = inOutRay.getDirection().z();
        if (inH <= VSDK.EPSILON) {
            return null;
        }
        const shiftedOz = oz - inH;
        const ratio = inR / inH;
        const ratioSquared = VSDK.square(ratio);

        //- Calcula el termino A --------------------------------------------
        A = VSDK.square(dx) + VSDK.square(dy) - VSDK.square(dz * ratio);
        if (Math.abs(A) <= VSDK.EPSILON) return null;

        //- Calcula el termino B --------------------------------------------
        B = 2 * (dx * ox + dy * oy - dz * shiftedOz * ratioSquared);

        //- Calcula el termino C --------------------------------------------
        C = VSDK.square(ox) + VSDK.square(oy) - VSDK.square(shiftedOz * ratio);

        //- Calcula el discriminant. Si el discriminant no es positivo el -
        //- rayo no intersecta la esfera. retorna t = 0
        discriminant = VSDK.square(B) - 4 * A * C;
        if (discriminant <= VSDK.EPSILON) return null;

        //- Resuelve la ecuacion cuadratica para las raices de la ecuacion. -
        //- (-B +/- sqrt(B^2 - 4*A*C)) / 2A.                                -
        discriminant = Math.sqrt(discriminant);
        t0 = (-B - discriminant) / (2 * A);

        //- Si t0 es > 0 listo. Si no debemos calcular la otra raiz t1. -----
        if (t0 > VSDK.EPSILON) {
            const shiftedPz = shiftedOz + dz * t0;
            if (shiftedPz > 0 || shiftedPz < -inH) {
                return null;
            }

            if (outInfo !== null) {
                const px = ox + dx * t0;
                const py = oy + dy * t0;
                outInfo.p = new Vector3Dd(px, py, shiftedPz + inH);
                outInfo.n = new Vector3Dd(px, py, -shiftedPz * ratioSquared).normalized();
            }
            return inOutRay.withT(t0);
        }

        return null;
    }

    private doIntersectionTap(inOutRay: Ray, inR: number, inH: number, outInfo: RayHit | null): Ray | null {
        const dx = inOutRay.getDirection().x();
        const dy = inOutRay.getDirection().y();
        const dz = inOutRay.getDirection().z();
        const ox = inOutRay.getOrigin().x();
        const oy = inOutRay.getOrigin().y();
        const oz = inOutRay.getOrigin().z();

        if (Math.abs(dz) > VSDK.EPSILON) {
            const t = (inH - oz) / dz;
            if (t > VSDK.EPSILON) {
                const px = ox + dx * t;
                const py = oy + dy * t;
                if (VSDK.square(px) + VSDK.square(py) < VSDK.square(inR)) {
                    if (outInfo !== null) {
                        outInfo.n = new Vector3Dd(0, 0, 1);
                        outInfo.p = new Vector3Dd(px, py, inH);
                    }
                    return inOutRay.withT(t);
                }
            }
        }
        return null;
    }

    private static hasHit(t: number): boolean {
        return t !== Cone.NO_HIT;
    }

    private doIntersectionCylinderDistance(inOutRay: Ray, inR: number, inH: number): number {
        const dx = inOutRay.getDirection().x();
        const dy = inOutRay.getDirection().y();
        const dz = inOutRay.getDirection().z();
        const ox = inOutRay.getOrigin().x();
        const oy = inOutRay.getOrigin().y();
        const oz = inOutRay.getOrigin().z();
        const A = VSDK.square(dx) + VSDK.square(dy);
        if (Math.abs(A) <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }

        const B = 2 * (dx * ox + dy * oy);
        const C = VSDK.square(ox) + VSDK.square(oy) - VSDK.square(inR);
        const discriminant = VSDK.square(B) - 4 * A * C;
        if (discriminant <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }

        const t0 = (-B - Math.sqrt(discriminant)) / (2 * A);
        if (t0 <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }
        const pz = oz + dz * t0;
        if (pz > inH || pz < 0) {
            return Cone.NO_HIT;
        }
        return t0;
    }

    private doIntersectionConeDistance(inOutRay: Ray, inR: number, inH: number): number {
        if (inH <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }

        const dx = inOutRay.getDirection().x();
        const dy = inOutRay.getDirection().y();
        const dz = inOutRay.getDirection().z();
        const ox = inOutRay.getOrigin().x();
        const oy = inOutRay.getOrigin().y();
        const shiftedOz = inOutRay.getOrigin().z() - inH;
        const ratio = inR / inH;
        const ratioSquared = VSDK.square(ratio);
        const A = VSDK.square(dx) + VSDK.square(dy) - VSDK.square(dz * ratio);
        if (Math.abs(A) <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }

        const B = 2 * (dx * ox + dy * oy - dz * shiftedOz * ratioSquared);
        const C = VSDK.square(ox) + VSDK.square(oy) - VSDK.square(shiftedOz * ratio);
        const discriminant = VSDK.square(B) - 4 * A * C;
        if (discriminant <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }

        const t0 = (-B - Math.sqrt(discriminant)) / (2 * A);
        if (t0 <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }
        const shiftedPz = shiftedOz + dz * t0;
        if (shiftedPz > 0 || shiftedPz < -inH) {
            return Cone.NO_HIT;
        }
        return t0;
    }

    private doIntersectionTapDistance(inOutRay: Ray, inR: number, inH: number): number {
        const dz = inOutRay.getDirection().z();
        if (Math.abs(dz) <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }

        const t = (inH - inOutRay.getOrigin().z()) / dz;
        if (t <= VSDK.EPSILON) {
            return Cone.NO_HIT;
        }

        const px = inOutRay.getOrigin().x() + inOutRay.getDirection().x() * t;
        const py = inOutRay.getOrigin().y() + inOutRay.getDirection().y() * t;
        if (VSDK.square(px) + VSDK.square(py) >= VSDK.square(inR)) {
            return Cone.NO_HIT;
        }
        return t;
    }

    private doIntersectionDistanceOnly(inOutRay: Ray, outHit: RayHit | null): boolean {
        let winnerT = Cone.NO_HIT;

        if (this.r2 < VSDK.EPSILON && this.r1 > VSDK.EPSILON) {
            const bodyT = this.doIntersectionConeDistance(inOutRay, this.r1, this.h);
            const tap1T = this.doIntersectionTapDistance(inOutRay, this.r1, 0);
            if (Cone.hasHit(tap1T) && (!Cone.hasHit(bodyT) || tap1T < bodyT)) {
                winnerT = tap1T;
            } else if (Cone.hasHit(bodyT)) {
                winnerT = bodyT;
            }
        } else if (VSDK.equals(this.r1, this.r2)) {
            const bodyT = this.doIntersectionCylinderDistance(inOutRay, this.r1, this.h);
            const tap1T = this.doIntersectionTapDistance(inOutRay, this.r1, 0);
            const tap2T = this.doIntersectionTapDistance(inOutRay, this.r1, this.h);

            if (
                Cone.hasHit(bodyT) &&
                ((Cone.hasHit(tap1T) && bodyT < tap1T) || !Cone.hasHit(tap1T)) &&
                ((Cone.hasHit(tap2T) && bodyT < tap2T) || !Cone.hasHit(tap2T))
            ) {
                winnerT = bodyT;
            } else if (
                Cone.hasHit(tap1T) &&
                ((Cone.hasHit(bodyT) && tap1T < bodyT) || !Cone.hasHit(bodyT)) &&
                ((Cone.hasHit(tap2T) && tap1T < tap2T) || !Cone.hasHit(tap2T))
            ) {
                winnerT = tap1T;
            } else if (Cone.hasHit(tap2T)) {
                winnerT = tap2T;
            }
        }

        if (!Cone.hasHit(winnerT)) {
            return false;
        }
        if (outHit !== null) {
            if (outHit.shouldStoreRay()) {
                outHit.setRay(inOutRay.withT(winnerT));
            } else {
                outHit.setHitDistance(winnerT);
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    Java overload `doIntersectionFirstHit(Ray inOutRay)`.
    @param inOutRay
    @return true if given ray intersects current Cone
    */
    public doIntersectionFirstHit(inOutRay: Ray): Ray | null;
    public override doIntersectionFirstHit(inOutRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inOutRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inOutRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            const hit = new RayHit();
            if (this.doIntersectionFirstHit(inOutRay, hit)) {
                return hit.ray();
            }
            return null;
        }

        if (outHit === null || !outHit.needsAnySurfaceData()) {
            return this.doIntersectionDistanceOnly(inOutRay, outHit);
        }

        const infoTap1 = new RayHit();
        const infoTap2 = new RayHit();
        const infoBody = new RayHit();
        let bodyHit: Ray | null;
        let tap1Hit: Ray | null;
        let tap2Hit: Ray | null;
        let winner: Ray | null = null;
        let winnerInfo: RayHit | null = null;

        if (this.r2 < VSDK.EPSILON && this.r1 > VSDK.EPSILON) {
            bodyHit = this.doIntersectionCone(inOutRay, this.r1, this.h, infoBody);
            tap1Hit = this.doIntersectionTap(inOutRay, this.r1, 0, infoTap1);
            if (
                (tap1Hit !== null && bodyHit === null) ||
                (tap1Hit !== null && bodyHit !== null && tap1Hit.getT() < bodyHit.getT())
            ) {
                infoTap1.n = infoTap1.n.multiply(-1);
                winner = inOutRay.withT(tap1Hit.getT());
                winnerInfo = infoTap1;
            } else if (bodyHit !== null) {
                winner = inOutRay.withT(bodyHit.getT());
                winnerInfo = infoBody;
            }
        } else if (VSDK.equals(this.r1, this.r2)) {
            let nearest = -1;
            bodyHit = this.doIntersectionCylinder(inOutRay, this.r1, this.h, infoBody);
            tap1Hit = this.doIntersectionTap(inOutRay, this.r1, 0, infoTap1);
            tap2Hit = this.doIntersectionTap(inOutRay, this.r1, this.h, infoTap2);

            if (
                bodyHit !== null &&
                ((tap1Hit !== null && bodyHit.getT() < tap1Hit.getT()) || tap1Hit === null) &&
                ((tap2Hit !== null && bodyHit.getT() < tap2Hit.getT()) || tap2Hit === null)
            ) {
                nearest = 1;
            } else if (
                tap1Hit !== null &&
                ((bodyHit !== null && tap1Hit.getT() < bodyHit.getT()) || bodyHit === null) &&
                ((tap2Hit !== null && tap1Hit.getT() < tap2Hit.getT()) || tap2Hit === null)
            ) {
                nearest = 3;
            } else if (tap2Hit !== null) {
                nearest = 2;
            }

            if (nearest === 1) {
                winner = inOutRay.withT(bodyHit!.getT());
                winnerInfo = infoBody;
            } else if (nearest === 2) {
                winner = inOutRay.withT(tap2Hit!.getT());
                winnerInfo = infoTap2;
            } else if (nearest === 3) {
                winner = inOutRay.withT(tap1Hit!.getT());
                winnerInfo = infoTap1;
            }
        }

        if (winner === null) {
            return false;
        }
        if (outHit !== null) {
            outHit.setRay(winner);
            outHit.p = new Vector3Dd(winnerInfo!.p);
            outHit.n = new Vector3Dd(winnerInfo!.n).normalized();
            outHit.t = new Vector3Dd(winnerInfo!.t);
            outHit.u = winnerInfo!.u;
            outHit.v = winnerInfo!.v;
            outHit.material = winnerInfo!.material;
            outHit.texture = winnerInfo!.texture;
            outHit.normalMap = winnerInfo!.normalMap;
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
    public override doExtraInformation(inRay: Ray, _inT: number, outData: RayHit): void {
        const hit = new RayHit();
        if (this.doIntersectionFirstHit(inRay.withT(Number.MAX_VALUE), hit)) {
            outData.clone(hit);
        }
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        // TODO!
        const minmax = new Float64Array(6);

        const r = Math.max(this.r1, this.r2);

        minmax[0] = -r;
        minmax[1] = -r;
        minmax[2] = 0;
        minmax[3] = r;
        minmax[4] = r;
        minmax[5] = this.h;

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
                Cone.DEFAULT_CIRCUMFERENCE_DIVISIONS,
                Cone.DEFAULT_HEIGHT_DIVISIONS,
            );
        }
        const normalizedCircumferenceDivisions = Math.max(Cone.MIN_CIRCUMFERENCE_DIVISIONS, circumferenceDivisions);
        const normalizedHeightDivisions = Math.max(Cone.MIN_HEIGHT_DIVISIONS, heightDivisions);

        if (
            normalizedCircumferenceDivisions === Cone.DEFAULT_CIRCUMFERENCE_DIVISIONS &&
            normalizedHeightDivisions === Cone.DEFAULT_HEIGHT_DIVISIONS
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
    private static closeTopFaceToApex(solid: PolyhedralBoundedSolid, apexZ: number): void {
        const topFace = solid.findFace(1);
        if (topFace === null || topFace.boundariesList.size() <= 0) {
            return;
        }

        const loop = topFace.boundariesList.get(0)!;
        const start = loop.boundaryStartHalfEdge;
        if (start === null) {
            return;
        }

        const ringVertexIds: number[] = [];
        let he: _PolyhedralBoundedSolidHalfEdge = start;
        do {
            ringVertexIds.push(he.startingVertex.id);
            he = he.next()!;
        } while (he !== start);

        if (ringVertexIds.length < 3) {
            return;
        }

        const apexVertexId = solid.getMaxVertexId() + 1;
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            ringVertexIds[0]!,
            apexVertexId,
            new Vector3Dd(0.0, 0.0, apexZ),
        );

        let i: number;
        for (i = 0; i < ringVertexIds.length - 2; i++) {
            PolyhedralBoundedSolidEulerOperators.mef(
                solid,
                1,
                1,
                apexVertexId,
                ringVertexIds[i]!,
                ringVertexIds[i + 1]!,
                ringVertexIds[i + 2]!,
                solid.getMaxFaceId() + 1,
            );
        }

        PolyhedralBoundedSolidEulerOperators.mef(
            solid,
            1,
            1,
            apexVertexId,
            ringVertexIds[ringVertexIds.length - 2]!,
            ringVertexIds[ringVertexIds.length - 1]!,
            ringVertexIds[0]!,
            solid.getMaxFaceId() + 1,
        );
    }

    private buildPolyhedralBoundedSolid(nsides: number, heightDivisions: number): PolyhedralBoundedSolid {
        let T: Matrix4x4d;
        let S: Matrix4x4d;
        let M: Matrix4x4d;

        const solid = PolyhedralBoundedSolidModeler.createCircularLamina(0.0, 0.0, this.r1, 0.0, nsides);

        if (this.r2 > VSDK.EPSILON && this.r1 > VSDK.EPSILON) {
            let prevRadius = this.r1;
            const zStep = this.h / heightDivisions;
            let i: number;
            for (i = 1; i <= heightDivisions; i++) {
                const nextRadius = this.r1 + (this.r2 - this.r1) * (i / heightDivisions);
                const f = nextRadius / prevRadius;
                T = new Matrix4x4d();
                T = T.translation(0.0, 0.0, zStep);
                S = new Matrix4x4d();
                S = S.scale(f, f, 1.0);
                M = T.multiply(S);
                PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1), M);
                prevRadius = nextRadius;
            }
        } else if (this.r2 <= VSDK.EPSILON && this.r1 > VSDK.EPSILON) {
            // Cone case, with optional vertical subdivisions.
            let prevRadius = this.r1;
            const zStep = this.h / heightDivisions;
            let i: number;
            for (i = 1; i < heightDivisions; i++) {
                const nextRadius = this.r1 * (1.0 - i / heightDivisions);
                const f = nextRadius / prevRadius;
                T = new Matrix4x4d();
                T = T.translation(0.0, 0.0, zStep);
                S = new Matrix4x4d();
                S = S.scale(f, f, 1.0);
                M = T.multiply(S);
                PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1), M);
                prevRadius = nextRadius;
            }
            Cone.closeTopFaceToApex(solid, this.h);
        }
        return solid;
    }
}
