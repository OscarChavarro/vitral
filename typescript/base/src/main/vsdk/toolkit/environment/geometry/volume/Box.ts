//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { PolyhedralBoundedSolidEulerOperators } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { VSDK } from "../../../common/VSDK.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { Solid } from "./Solid.js";

export class Box extends Solid {
    private static readonly NORMAL_POS_Z = new Vector3Dd(0, 0, 1);
    private static readonly NORMAL_NEG_Z = new Vector3Dd(0, 0, -1);
    private static readonly NORMAL_POS_Y = new Vector3Dd(0, 1, 0);
    private static readonly NORMAL_NEG_Y = new Vector3Dd(0, -1, 0);
    private static readonly NORMAL_POS_X = new Vector3Dd(1, 0, 0);
    private static readonly NORMAL_NEG_X = new Vector3Dd(-1, 0, 0);
    private static readonly TANGENT_POS_Y = new Vector3Dd(0, 1, 0);
    private static readonly TANGENT_NEG_Y = new Vector3Dd(0, -1, 0);
    private static readonly TANGENT_NEG_X = new Vector3Dd(-1, 0, 0);
    private static readonly TANGENT_POS_X = new Vector3Dd(1, 0, 0);
    private static readonly ZERO_VECTOR = new Vector3Dd();

    private size: Vector3Dd;

    public constructor(dx: number, dy: number, dz: number);
    public constructor(s: Vector3Dd);
    public constructor(a: number | Vector3Dd, dy?: number, dz?: number) {
        super();
        if (a instanceof Vector3Dd) {
            this.size = new Vector3Dd(a);
        } else {
            this.size = new Vector3Dd(a, dy!, dz!);
        }
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    Java overload `doIntersectionFirstHit(Ray inOutRay)`.
    @param inOutRay
    @return true if given ray intersects current Box
    */
    public doIntersectionFirstHit(inOutRay: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            const hit = new RayHit();
            if (this.doIntersectionFirstHit(inRay, hit)) {
                return hit.ray();
            }
            return null;
        }

        let minT = Number.MAX_VALUE;
        let hitPlane = 0;
        const x2 = this.size.x() / 2;
        const y2 = this.size.y() / 2;
        const z2 = this.size.z() / 2;
        const ox = inRay.getOrigin().x();
        const oy = inRay.getOrigin().y();
        const oz = inRay.getOrigin().z();
        const dx = inRay.getDirection().x();
        const dy = inRay.getDirection().y();
        const dz = inRay.getDirection().z();

        if (Math.abs(dz) > VSDK.EPSILON) {
            const t = (z2 - oz) / dz;
            if (t > -VSDK.EPSILON) {
                const cx = ox + dx * t;
                const cy = oy + dy * t;
                if (cx >= -x2 && cx <= x2 && cy >= -y2 && cy <= y2) {
                    minT = t;
                    hitPlane = 1;
                }
            }
        }
        if (Math.abs(dz) > VSDK.EPSILON) {
            const t = (-z2 - oz) / dz;
            if (t > -VSDK.EPSILON && t < minT) {
                const cx = ox + dx * t;
                const cy = oy + dy * t;
                if (cx >= -x2 && cx <= x2 && cy >= -y2 && cy <= y2) {
                    minT = t;
                    hitPlane = 2;
                }
            }
        }
        if (Math.abs(dy) > VSDK.EPSILON) {
            const t = (y2 - oy) / dy;
            if (t > -VSDK.EPSILON && t < minT) {
                const cx = ox + dx * t;
                const cz = oz + dz * t;
                if (cx >= -x2 && cx <= x2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 3;
                }
            }
        }
        if (Math.abs(dy) > VSDK.EPSILON) {
            const t = (-y2 - oy) / dy;
            if (t > -VSDK.EPSILON && t < minT) {
                const cx = ox + dx * t;
                const cz = oz + dz * t;
                if (cx >= -x2 && cx <= x2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 4;
                }
            }
        }
        if (Math.abs(dx) > VSDK.EPSILON) {
            const t = (x2 - ox) / dx;
            if (t > -VSDK.EPSILON && t < minT) {
                const cy = oy + dy * t;
                const cz = oz + dz * t;
                if (cy >= -y2 && cy <= y2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 5;
                }
            }
        }
        if (Math.abs(dx) > VSDK.EPSILON) {
            const t = (-x2 - ox) / dx;
            if (t > -VSDK.EPSILON && t < minT) {
                const cy = oy + dy * t;
                const cz = oz + dz * t;
                if (cy >= -y2 && cy <= y2 && cz >= -z2 && cz <= z2) {
                    minT = t;
                    hitPlane = 6;
                }
            }
        }

        if (minT === Number.MAX_VALUE) {
            return false;
        }

        if (outHit !== null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                outHit.setRay(inRay.withT(minT));
            } else {
                outHit.setHitDistance(minT);
            }
            if (outHit.needsAnySurfaceData()) {
                const hitX = ox + dx * minT;
                const hitY = oy + dy * minT;
                const hitZ = oz + dz * minT;
                if (outHit.needsPoint()) {
                    outHit.p = new Vector3Dd(hitX, hitY, hitZ);
                }
                if (outHit.needsTextureCoordinates()) {
                    outHit.u = 0;
                    outHit.v = 0;
                    switch (hitPlane) {
                        case 1:
                            outHit.u = hitY / this.size.y() - 0.5;
                            outHit.v = 1 - (hitX / this.size.x() - 0.5);
                            break;
                        case 2:
                            outHit.u = hitY / this.size.y() - 0.5;
                            outHit.v = hitX / this.size.x() - 0.5;
                            break;
                        case 3:
                            outHit.u = 1 - (hitX / this.size.x() - 0.5);
                            outHit.v = hitZ / this.size.z() - 0.5;
                            break;
                        case 4:
                            outHit.u = hitX / this.size.x() - 0.5;
                            outHit.v = hitZ / this.size.z() - 0.5;
                            break;
                        case 5:
                            outHit.u = hitY / this.size.y() - 0.5;
                            outHit.v = hitZ / this.size.z() - 0.5;
                            break;
                        case 6:
                            outHit.u = 1 - (hitY / this.size.y() - 0.5);
                            outHit.v = hitZ / this.size.z() - 0.5;
                            break;
                        default:
                            break;
                    }
                }
                if (outHit.needsNormal()) {
                    outHit.n = Box.planeNormal(hitPlane);
                }
                if (outHit.needsTangent()) {
                    outHit.t = Box.planeTangent(hitPlane);
                }
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
        const hitX = inRay.getOrigin().x() + inRay.getDirection().x() * inT;
        const hitY = inRay.getOrigin().y() + inRay.getDirection().y() * inT;
        const hitZ = inRay.getOrigin().z() + inRay.getDirection().z() * inT;
        const hitPlane = this.classifyHitPlane(hitX, hitY, hitZ);

        if (outData.needsPoint()) {
            outData.p = new Vector3Dd(hitX, hitY, hitZ);
        }
        if (outData.needsNormal()) {
            outData.n = Box.planeNormal(hitPlane);
        }
        if (outData.needsTangent()) {
            outData.t = Box.planeTangent(hitPlane);
        }
        if (outData.needsTextureCoordinates()) {
            switch (hitPlane) {
                case 1:
                    outData.u = hitY / this.size.y() - 0.5;
                    outData.v = 1 - (hitX / this.size.x() - 0.5);
                    break;
                case 2:
                    outData.u = hitY / this.size.y() - 0.5;
                    outData.v = hitX / this.size.x() - 0.5;
                    break;
                case 3:
                    outData.u = 1 - (hitX / this.size.x() - 0.5);
                    outData.v = hitZ / this.size.z() - 0.5;
                    break;
                case 4:
                    outData.u = hitX / this.size.x() - 0.5;
                    outData.v = hitZ / this.size.z() - 0.5;
                    break;
                case 5:
                    outData.u = hitY / this.size.y() - 0.5;
                    outData.v = hitZ / this.size.z() - 0.5;
                    break;
                case 6:
                    outData.u = 1 - (hitY / this.size.y() - 0.5);
                    outData.v = hitZ / this.size.z() - 0.5;
                    break;
                default:
                    outData.u = 0;
                    outData.v = 0;
                    break;
            }
        }
    }

    private static planeNormal(hitPlane: number): Vector3Dd {
        switch (hitPlane) {
            case 1:
                return Box.NORMAL_POS_Z;
            case 2:
                return Box.NORMAL_NEG_Z;
            case 3:
                return Box.NORMAL_POS_Y;
            case 4:
                return Box.NORMAL_NEG_Y;
            case 5:
                return Box.NORMAL_POS_X;
            case 6:
                return Box.NORMAL_NEG_X;
            default:
                return Box.ZERO_VECTOR;
        }
    }

    private static planeTangent(hitPlane: number): Vector3Dd {
        switch (hitPlane) {
            case 1:
            case 2:
            case 5:
                return Box.TANGENT_POS_Y;
            case 3:
                return Box.TANGENT_NEG_X;
            case 4:
                return Box.TANGENT_POS_X;
            case 6:
                return Box.TANGENT_NEG_Y;
            default:
                return Box.ZERO_VECTOR;
        }
    }

    private classifyHitPlane(x: number, y: number, z: number): number {
        const x2 = this.size.x() / 2;
        const y2 = this.size.y() / 2;
        const z2 = this.size.z() / 2;
        const dxPlus = Math.abs(x - x2);
        const dxMinus = Math.abs(x + x2);
        const dyPlus = Math.abs(y - y2);
        const dyMinus = Math.abs(y + y2);
        const dzPlus = Math.abs(z - z2);
        const dzMinus = Math.abs(z + z2);

        let min = dzPlus;
        let plane = 1;
        if (dzMinus < min) {
            min = dzMinus;
            plane = 2;
        }
        if (dyPlus < min) {
            min = dyPlus;
            plane = 3;
        }
        if (dyMinus < min) {
            min = dyMinus;
            plane = 4;
        }
        if (dxPlus < min) {
            min = dxPlus;
            plane = 5;
        }
        if (dxMinus < min) {
            plane = 6;
        }
        return plane;
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        const minmax = new Float64Array(6);

        minmax[0] = -this.size.x() / 2;
        minmax[1] = -this.size.y() / 2;
        minmax[2] = -this.size.z() / 2;
        minmax[3] = this.size.x() / 2;
        minmax[4] = this.size.y() / 2;
        minmax[5] = this.size.z() / 2;

        return minmax;
    }

    public getSize(): Vector3Dd {
        return this.size;
    }

    public setSize(dx: number, dy: number, dz: number): void;
    public setSize(s: Vector3Dd): void;
    public setSize(a: number | Vector3Dd, dy?: number, dz?: number): void {
        if (!(a instanceof Vector3Dd)) {
            this.setSize(new Vector3Dd(a, dy!, dz!));
            return;
        }
        this.size = new Vector3Dd(a);
    }

    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid {
        return this.buildPolyhedralBoundedSolid();
    }

    /**
    Current method creates a polyhedral boundary representation for
    current box, following the strategy for Euler operators presented
    at sections [MANT1988].9.3., [MANT1988].12.3.1., as depicted in
    figure [MANT1988].9.11. and following the structure of the program
    [MANT1988].12.4.
    */
    private buildPolyhedralBoundedSolid(): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(
            solid,
            new Vector3Dd(-this.size.x() / 2, -this.size.y() / 2, -this.size.z() / 2),
            1,
            1,
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            1,
            4,
            new Vector3Dd(-this.size.x() / 2, this.size.y() / 2, -this.size.z() / 2),
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            4,
            3,
            new Vector3Dd(this.size.x() / 2, this.size.y() / 2, -this.size.z() / 2),
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            3,
            2,
            new Vector3Dd(this.size.x() / 2, -this.size.y() / 2, -this.size.z() / 2),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 1, 4, 2, 3, 2);

        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            1,
            5,
            new Vector3Dd(-this.size.x() / 2, -this.size.y() / 2, this.size.z() / 2),
        );
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            2,
            6,
            new Vector3Dd(this.size.x() / 2, -this.size.y() / 2, this.size.z() / 2),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 5, 1, 6, 2, 3);
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            3,
            7,
            new Vector3Dd(this.size.x() / 2, this.size.y() / 2, this.size.z() / 2),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 6, 2, 7, 3, 4);
        PolyhedralBoundedSolidEulerOperators.smev(
            solid,
            1,
            4,
            8,
            new Vector3Dd(-this.size.x() / 2, this.size.y() / 2, this.size.z() / 2),
        );
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 7, 3, 8, 4, 5);
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 5, 6, 8, 4, 6);
        return solid;
    }
}
