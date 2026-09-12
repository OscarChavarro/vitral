//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { PolyhedralBoundedSolidEulerOperators } from "./polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { RaytraceStatistics } from "../../../common/statistics/RaytraceStatistics.js";
import { VSDK } from "../../../common/VSDK.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../element/Ray.js";
import type { RayHit } from "../element/RayHit.js";
import { PolyhedralBoundedSolid } from "./polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { Solid } from "./Solid.js";

export class Sphere extends Solid {
    private _radius: number;
    private _radius_squared: number;

    private static readonly DEFAULT_PARALLELS = 8;
    private static readonly DEFAULT_MERIDIANS = 16;
    private static readonly MIN_PARALLELS = 2;
    private static readonly MIN_MERIDIANS = 3;

    public constructor(r: number) {
        super();
        this._radius = r;
        this._radius_squared = this._radius * this._radius;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    Java overload `doIntersectionFirstHit(Ray inout_rayo)`.
    @param inout_rayo
    @return true if given ray intersects current Sphere
    */
    public doIntersectionFirstHit(inout_rayo: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            const inout_rayo = inRay;
            const dx = -inout_rayo.getOrigin().x();
            const dy = -inout_rayo.getOrigin().y();
            const dz = -inout_rayo.getOrigin().z();
            const direction = inout_rayo.getDirection();
            const v = direction.x() * dx + direction.y() * dy + direction.z() * dz;

            // Test if the inout_rayo actually intersects the sphere
            let t = this._radius_squared + v * v - dx * dx - dy * dy - dz * dz;
            if (t < 0) {
                return null;
            }

            // Test if the intersection is in the positive
            // inout_rayo direction
            t = v - Math.sqrt(t);
            if (t < 0) {
                return null;
            }

            return inout_rayo.withT(t);
        }

        const dx = -inRay.getOrigin().x();
        const dy = -inRay.getOrigin().y();
        const dz = -inRay.getOrigin().z();
        const direction = inRay.getDirection();
        const projection = direction.x() * dx + direction.y() * dy + direction.z() * dz;

        const discriminant = this._radius_squared + projection * projection - dx * dx - dy * dy - dz * dz;
        if (discriminant < 0) {
            return false;
        }

        const t = projection - Math.sqrt(discriminant);
        if (t < 0) {
            return false;
        }

        if (outHit !== null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                const hitRay = inRay.withT(t);
                outHit.setRay(hitRay);
                if (outHit.needsAnySurfaceData()) {
                    this.doExtraInformation(hitRay, t, outHit);
                    outHit.setRay(hitRay);
                }
            } else {
                outHit.setHitDistance(t);
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inT
    */
    public override doExtraInformation(inRay: Ray, inT: number, outData: RayHit): void {
        RaytraceStatistics.recordGeometryDetailComputation();
        const needsNormalVector = outData.needsNormal() || outData.needsTextureCoordinates() || outData.needsTangent();
        if (!outData.needsPoint() && !needsNormalVector) {
            return;
        }

        const point = new Vector3Dd(
            inRay.getOrigin().x() + inT * inRay.getDirection().x(),
            inRay.getOrigin().y() + inT * inRay.getDirection().y(),
            inRay.getOrigin().z() + inT * inRay.getDirection().z(),
        );
        if (outData.needsPoint()) {
            outData.p = point;
        }

        let normal: Vector3Dd | null = null;
        if (needsNormalVector) {
            normal = new Vector3Dd(point).normalized();
            if (outData.needsNormal()) {
                outData.n = normal;
            }
        }

        if (!outData.needsTextureCoordinates() && !outData.needsTangent()) {
            return;
        }

        let theta: number;
        const phi = Math.acos(normal!.z());

        if (normal!.x() > VSDK.EPSILON) {
            theta = Math.atan(normal!.y() / normal!.x()) + (3 * Math.PI) / 2;
        } else if (normal!.x() < VSDK.EPSILON) {
            theta = Math.atan(normal!.y() / normal!.x()) + (3 * Math.PI) / 2;
            theta += Math.PI;
            if (theta > 2 * Math.PI) {
                theta -= 2 * Math.PI;
            }
        } else {
            theta = 0.0;
        }

        if (outData.needsTextureCoordinates()) {
            outData.u = (theta + Math.PI / 2) / (2 * Math.PI);
            outData.v = 1 - phi / Math.PI;
        }
        if (outData.needsTangent()) {
            outData.t = new Vector3Dd(Math.sin(theta - Math.PI / 2), -Math.cos(theta - Math.PI / 2), 0);
        }
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doContainmentTest.
    @return INSIDE, OUTSIDE or LIMIT constant value
    */
    public override doContainmentTest(p: Vector3Dd, distanceTolerance: number): number {
        const l = p.length();
        if (l < this._radius - distanceTolerance) {
            return Sphere.INSIDE;
        } else if (l > this._radius + distanceTolerance) {
            return Sphere.OUTSIDE;
        }
        return Sphere.LIMIT;
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array {
        const minmax = new Float64Array(6);
        for (let i = 0; i < 3; i++) {
            minmax[i] = -this._radius;
        }
        for (let i = 3; i < 6; i++) {
            minmax[i] = this._radius;
        }
        return minmax;
    }

    public getRadius(): number {
        return this._radius;
    }

    public getRadiusSquared(): number {
        return this._radius_squared;
    }

    public setRadius(r: number): void {
        this._radius = r;
        this._radius_squared = r * r;
    }

    private static spherePosition(theta: number, t: number, r: number): Vector3Dd {
        const phi = (t - 0.5) * Math.PI;
        return new Vector3Dd(
            Math.cos(phi) * Math.cos(theta) * r,
            Math.cos(phi) * Math.sin(theta) * r,
            Math.sin(phi) * r,
        );
    }

    /**
    Java overloads `exportToPolyhedralBoundedSolid()` and
    `exportToPolyhedralBoundedSolid(int meridians, int parallels)`.
    */
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(meridians: number, parallels: number): PolyhedralBoundedSolid;
    public override exportToPolyhedralBoundedSolid(meridians?: number, parallels?: number): PolyhedralBoundedSolid {
        if (meridians === undefined || parallels === undefined) {
            return this.buildPolyhedralBoundedSolid(Sphere.DEFAULT_MERIDIANS, Sphere.DEFAULT_PARALLELS);
        }
        const normalizedMeridians = Math.max(Sphere.MIN_MERIDIANS, meridians);
        const normalizedParallels = Math.max(Sphere.MIN_PARALLELS, parallels);

        if (normalizedMeridians === Sphere.DEFAULT_MERIDIANS && normalizedParallels === Sphere.DEFAULT_PARALLELS) {
            return this.exportToPolyhedralBoundedSolid();
        }

        return this.buildPolyhedralBoundedSolid(normalizedMeridians, normalizedParallels);
    }

    /**
    Given current sphere, this method generates a "polyhedral ball"
    approximation.
    Note that this method follows a similar strategy to the one proposed on
    function "ball", from program [MANT1988].12.6, but it is expressed entirely
    on "low level" operators, and doesn't rely on the previous availability of
    generalized rotational sweep operations.

    <p><b>Planarity guarantee for the default tessellation</b> (meridians=16,
    parallels=8): each latitude band produces quadrilaterals whose four
    vertices lie on the surface of the sphere at a constant latitude
    {@code phi} and two adjacent longitudes {@code lambda} and
    {@code lambda + 2*PI/meridians}.  All four vertices share the same
    {@code z = r*sin(phi)} coordinate, so the quad is planar in the
    horizontal plane at height {@code z}.  This was verified analytically
    in stage-1 analysis (2026-05-12) and holds for any (meridians, parallels)
    combination because the vertex positions are computed with
    {@code cos}/{@code sin} only in the latitude direction.</p>
    */
    private buildPolyhedralBoundedSolid(nmeridians: number, nparalels: number): PolyhedralBoundedSolid {
        let theta: number;
        let phi: number;
        const dtheta = (2 * Math.PI) / nmeridians;
        const dphi = 1.0 / nparalels;
        let i: number;
        let base2: number;
        let base1: number;
        let pos: Vector3Dd;

        //- Build triangles for lower cap ---------------------------------
        const solid = new PolyhedralBoundedSolid();
        pos = new Vector3Dd(0, 0, -this._radius);
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, pos, 1, 1);

        pos = new Vector3Dd();
        pos = Sphere.spherePosition(dtheta, dphi, this._radius);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 3, pos);
        pos = new Vector3Dd();
        pos = Sphere.spherePosition(0, dphi, this._radius);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 3, 2, pos);

        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 1, 3, 2, 3, 2);

        for (i = 2; i < nmeridians; i++) {
            theta = dtheta * i;
            pos = new Vector3Dd();
            pos = Sphere.spherePosition(theta, dphi, this._radius);
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, i + 1 + 1, pos);
            // Next face is <(1), (i+1), (i+0)>
            PolyhedralBoundedSolidEulerOperators.mef(
                solid,
                1 /* seed face, always face 1 */,
                1 /* seed face, always face 1 */,
                i + 0 + 1 /* start of half edge 1 */,
                1 /* end of half edge 1 */,
                i + 1 + 1 /* start of half edge 2 */,
                1 /* end of half edge 2 */,
                i + 1 /* new face id */,
            );
        }
        // Next face is <(1), (2), (i+1)>
        PolyhedralBoundedSolidEulerOperators.mef(
            solid,
            1 /* seed face, always face 1 */,
            1 /* seed face, always face 1 */,
            i + 1 /* start of half edge 1 */,
            1 /* end of half edge 1 */,
            2 /* start of half edge 2 */,
            3 /* end of half edge 2 */,
            i + 1 /* new face id */,
        );
        base2 = i + 2;
        base1 = 2;

        //- Build triangulated side strips for sphere body ----------------
        // Each spherical quad is split into two triangles immediately after
        // creation via smef, adding a diagonal from TL corner to BR corner.
        // Face IDs are tracked with a running counter to avoid conflicts with
        // the doubled face count.
        let p: number;
        let nextFaceId = nmeridians + 2;
        for (p = 0; p < nparalels - 2; p++) {
            phi = (p + 2) / nparalels;
            for (i = 0; i < nmeridians; i++) {
                theta = dtheta * i;
                pos = new Vector3Dd();
                pos = Sphere.spherePosition(theta, phi, this._radius);
                PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i + base1, i + base2, pos);
                if (i > 0) {
                    const quadFaceId = nextFaceId++;
                    const diagFaceId = nextFaceId++;
                    PolyhedralBoundedSolidEulerOperators.mef(
                        solid,
                        1 /* seed face, always face 1 */,
                        1 /* seed face, always face 1 */,
                        i - 1 + base2 /* start of half edge 1: TL */,
                        i - 1 + base1 /* end of half edge 1: BL */,
                        i + base2 /* start of half edge 2: TR */,
                        i + base1 /* end of half edge 2: BR */,
                        quadFaceId,
                    );
                    // Split quad {TR,TL,BL,BR} into two triangles via TL→BR diagonal
                    PolyhedralBoundedSolidEulerOperators.smef(
                        solid,
                        quadFaceId,
                        i - 1 + base2 /* TL vertex */,
                        i + base1 /* BR vertex */,
                        diagFaceId,
                    );
                }
            }
            {
                const quadFaceId = nextFaceId++;
                const diagFaceId = nextFaceId++;
                // Wrap-around quad: closes the ring from the last meridian back to the first
                PolyhedralBoundedSolidEulerOperators.mef(
                    solid,
                    1 /* seed face, always face 1 */,
                    1 /* seed face, always face 1 */,
                    i + base2 - 1 /* start of half edge 1: TL-last */,
                    base1 + i - 1 /* end of half edge 1: BL-last */,
                    base2 /* start of half edge 2 */,
                    base2 + 1 /* end of half edge 2 */,
                    quadFaceId,
                );
                // Split wrap-around quad via TL-last → BL-0 diagonal
                PolyhedralBoundedSolidEulerOperators.smef(
                    solid,
                    quadFaceId,
                    i - 1 + base2 /* TL-last vertex */,
                    base1 /* BL-0 vertex (first old-parallel vertex) */,
                    diagFaceId,
                );
            }
            base1 = base2;
            base2 += nmeridians;
        }

        //- Build triangles for upper cap --------------------------------
        pos = new Vector3Dd(0, 0, this._radius);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, base1, base2, pos);

        for (i = 0; i < nmeridians - 2; i++) {
            PolyhedralBoundedSolidEulerOperators.mef(
                solid,
                1 /* seed face, always face 1 */,
                1 /* seed face, always face 1 */,
                base2 /* start of half edge 1 */,
                base1 + i /* end of half edge 1 */,
                base1 + i + 1 /* start of half edge 2 */,
                base1 + i + 2 /* end of half edge 2 */,
                nextFaceId++,
            );
        }

        PolyhedralBoundedSolidEulerOperators.mef(
            solid,
            1 /* seed face, always face 1 */,
            1 /* seed face, always face 1 */,
            base2 /* start of half edge 1 */,
            base1 + i /* end of half edge 1 */,
            base1 + i + 1 /* start of half edge 2 */,
            base1 /* end of half edge 2 */,
            nextFaceId++,
        );

        //-----------------------------------------------------------------
        return solid;
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `p` Vector3Dd the (x, y, z) coordinates
    of the corresponding point on Sphere's surface.
    \todo  check this method for efficiency improvement
    @param p
    @param theta
    @param phi
    */
    public spherePosition(theta: number, phi: number): Vector3Dd {
        return new Vector3Dd(
            Math.cos(phi) * Math.cos(theta) * this._radius,
            -Math.cos(phi) * Math.sin(theta) * this._radius,
            Math.sin(phi) * this._radius,
        );
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `n` Vector3Dd the (nx, ny, nz) coordinates
    of the surface normal at corresponding point on Sphere's surface.
    \todo  check this method for efficiency improvement
    @param n
    @param theta
    @param phi
    */
    public sphereNormal(theta: number, phi: number): Vector3Dd {
        return new Vector3Dd(Math.cos(phi) * Math.cos(theta), -Math.cos(phi) * Math.sin(theta), Math.sin(phi));
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `n` Vector3Dd the (tx, ty, tz) coordinates
    of the surface tangent at corresponding point on Sphere's surface. Tangents
    are aligned with respect to Sphere's equator.
    \todo  check this method for efficiency improvement
    \todo  check this method for efficiency improvement
    @param t
    @param theta
    @param phi
    */
    public sphereTangent(theta: number, _phi: number): Vector3Dd {
        return new Vector3Dd(Math.sin(theta), Math.cos(theta), 0);
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `n` Vector3Dd the (bx, by, bz) coordinates
    of the surface tangent binormal at corresponding point on Sphere's surface.
    Tangents binormals are perpendicular to both normal and tangent.
    \todo  check this method for efficiency improvement
    @param b
    @param theta
    @param phi
    */
    public sphereBinormal(theta: number, phi: number): Vector3Dd {
        return new Vector3Dd(
            -Math.sin(phi) * Math.cos(theta),
            Math.sin(phi) * Math.sin(theta),
            Math.cos(phi) * Math.cos(theta) * Math.cos(theta) + Math.cos(phi) * Math.sin(theta) * Math.sin(theta),
        );
    }
}
