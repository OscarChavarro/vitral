//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { platformPrint } from "../../../../../../../../java/lang/_PlatformConsole.js";
import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import { VSDK } from "../../../../../../common/VSDK.js";
import { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _SetOperationTrace } from "../_SetOperationTrace.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "../intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";
import { _PolyhedralBoundedSolidSetClassifier } from "./_PolyhedralBoundedSolidSetClassifier.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector } from "./_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex } from "./_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex.js";

const ON = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON;
const IN = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
const OUT = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;

class VertexVertexClassificationData {
    public readonly nba: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>;
    public readonly nbb: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>;
    public readonly sectors: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector>;

    public constructor(
        inNba: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>,
        inNbb: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>,
        inSectors: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector>,
    ) {
        this.nba = inNba;
        this.nbb = inNbb;
        this.sectors = inSectors;
    }
}

/**
Geometric preprocessing and sector reclassification for vertex/vertex matches,
following section [MANT1988].15.6.2 and programs [MANT1988].15.7 through
[MANT1988].15.10.
*/
export class _PolyhedralBoundedSolidSetVertexVertexClassifier extends _PolyhedralBoundedSolidOperator {
    private static readonly DEBUG_01_STRUCTURE = 0x01;
    private static readonly DEBUG_04_VERTEX_VERTEX_CLASSIFIER = 0x08;

    private static debugFlags = 0;
    private nba: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> | null = null;
    private nbb: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> | null = null;
    private sectors: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector> | null = null;

    private coplanarSameOrientationForSectorPair(secta: number, sectb: number): boolean | null {
        const nba = this.nba!;
        const nbb = this.nbb!;

        if (secta < 0 || sectb < 0 || secta >= nba.size() || sectb >= nbb.size()) {
            return null;
        }

        const ha = nba.get(secta).he;
        const hb = nbb.get(sectb).he;
        if (
            ha === null ||
            hb === null ||
            ha.parentLoop === null ||
            hb.parentLoop === null ||
            ha.parentLoop.parentFace === null ||
            hb.parentLoop.parentFace === null ||
            ha.parentLoop.parentFace.getContainingPlane() === null ||
            hb.parentLoop.parentFace.getContainingPlane() === null
        ) {
            return null;
        }

        const n1 = ha.parentLoop.parentFace.getContainingPlane()!.getNormal();
        const n2 = hb.parentLoop.parentFace.getContainingPlane()!.getNormal();
        if (!_PolyhedralBoundedSolidSetGeometricPredicateProcessor.colinearVectors(n1, n2)) {
            return null;
        }

        return n1.dotProduct(n2) >= 0.0;
    }

    /**
    Vertex/Vertex classifier for the set operations algorithm (big phase 2).
    Following program [MANT1988].15.6. Similar in structure to program
    [MANT1988].14.3.
    */
    public classify(
        va: _PolyhedralBoundedSolidVertex,
        vb: _PolyhedralBoundedSolidVertex,
        op: number,
        flags: number,
    ): VertexVertexClassificationData {
        _PolyhedralBoundedSolidSetVertexVertexClassifier.debugFlags = flags;
        const debugFlags = _PolyhedralBoundedSolidSetVertexVertexClassifier.debugFlags;

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_01_STRUCTURE) !== 0x00) {
            if (
                (debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) ===
                0x00
            ) {
                platformPrint("  - ");
            } else {
                platformPrint("  * ");
            }
            platformPrint("Vertex of {A} / Vertex of {B} pair: A[" + va.id + "] / B[" + vb.id + "]");
            if (
                (debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) ===
                0x00
            ) {
                console.log(".");
            } else {
                console.log(" ->");
            }
        }

        this.vertexVertexGetNeighborhood(va, vb);

        if (
            (debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
            0x00
        ) {
            console.log("   - Initial sector/sector intersection candidates:");
            for (let i = 0; i < this.sectors!.size(); i++) {
                console.log("    . " + this.sectors!.get(i));
            }
        }

        this.vertexVertexReclassifyOnSectors(op);

        if (
            (debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
            0x00
        ) {
            console.log("   - On sector reclassified:");
            for (let i = 0; i < this.sectors!.size(); i++) {
                console.log("    . " + this.sectors!.get(i));
            }
        }

        this.vertexVertexReclassifyOnEdges(op);

        if (
            (debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
            0x00
        ) {
            console.log("   - On edges reclassified:");
            for (let i = 0; i < this.sectors!.size(); i++) {
                console.log("    . " + this.sectors!.get(i));
            }
        }

        return new VertexVertexClassificationData(this.nba!, this.nbb!, this.sectors!);
    }

    /**
    Following program [MANT1988].15.8.
    */
    private static nbrpreproc(
        v: _PolyhedralBoundedSolidVertex,
    ): ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> {
        let n: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex;
        let bisec: Vector3Dd;
        let he: _PolyhedralBoundedSolidHalfEdge;

        const nb = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>();

        he = v.emanatingHalfEdge!;
        let oldref2: Vector3Dd;

        do {
            n = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
            n.he = he;
            n.wide = false;

            n.ref1 = he.previous()!.startingVertex.position.subtract(he.startingVertex.position);
            n.ref2 = he.next()!.startingVertex.position.subtract(he.startingVertex.position);
            n.ref12 = n.ref1.crossProduct(n.ref2);

            if (
                PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                    n.ref1,
                    n.ref2,
                    _PolyhedralBoundedSolidOperator.numericContext,
                ) ||
                n.ref12.dotProduct(he.parentLoop.parentFace.getContainingPlane()!.getNormal()) > 0.0
            ) {
                if (
                    PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                        n.ref1,
                        n.ref2,
                        _PolyhedralBoundedSolidOperator.numericContext,
                    )
                ) {
                    bisec = _PolyhedralBoundedSolidSetClassifier.inside(he);
                } else {
                    bisec = n.ref1.add(n.ref2);
                    bisec = bisec.multiply(-1);
                }
                oldref2 = n.ref2;
                n.ref2 = bisec;
                n.ref12 = n.ref1.crossProduct(n.ref2);
                nb.add(n);

                n = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
                n.he = he;
                n.ref2 = oldref2;
                n.ref1 = bisec;
                n.ref12 = n.ref1.crossProduct(n.ref2);
                n.wide = true;
            }

            nb.add(n);

            he = he.mirrorHalfEdge()!.next()!;
        } while (he !== v.emanatingHalfEdge);

        return nb;
    }

    /**
    Checks if two coplanar sectors overlap, by doing the coplanar
    sector-within test required by section [MANT1988].15.6.2.
    */
    private static sectoroverlap(
        na: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
        nb: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex,
    ): boolean {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlap(
            na,
            nb,
            (_PolyhedralBoundedSolidSetVertexVertexClassifier.debugFlags &
                _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                0,
        );
    }

    /**
    Following program [MANT1988].15.9. According to the sector intersection
    test from section [MANT1988].15.6.2, the variables are interpreted as in
    figure [MANT1988].15.8 and equation [MANT1988].15.5.
    */
    private static sctrwitthin(dir: Vector3Dd, ref1: Vector3Dd, ref2: Vector3Dd, ref12: Vector3Dd): boolean {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.sctrwitthin(dir, ref1, ref2, ref12);
    }

    private static compareToZero(value: number): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(value);
    }

    private static resolveCoplanarVertexVertexClass(op: number, sameOrientation: boolean, sideA: boolean): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.resolveCoplanarVertexVertexClass(
            op,
            sameOrientation,
            sideA,
        );
    }

    /**
    Sector intersection test.
    Following program [MANT1988].15.9 and section [MANT1988].15.6.2.
    */
    private vertexVertexSectorIntersectionTest(i: number, j: number): boolean {
        let c1: boolean;
        let c2: boolean;
        const debugFlags = _PolyhedralBoundedSolidSetVertexVertexClassifier.debugFlags;

        const na = this.nba!.get(i);
        const nb = this.nbb!.get(j);
        const h1 = na.he!;
        const h2 = nb.he!;

        let intrs: Vector3Dd;

        const n1 = h1.parentLoop.parentFace.getContainingPlane()!.getNormal();
        const n2 = h2.parentLoop.parentFace.getContainingPlane()!.getNormal();
        intrs = n1.crossProduct(n2);

        if (
            PolyhedralBoundedSolidNumericPolicy.unitVectorsParallel(
                n1,
                n2,
                _PolyhedralBoundedSolidOperator.numericContext,
            )
        ) {
            if (
                (debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                0
            ) {
                platformPrint(" <coplanar>");
            }
            return _PolyhedralBoundedSolidSetVertexVertexClassifier.sectoroverlap(na, nb);
        }

        c1 = _PolyhedralBoundedSolidSetVertexVertexClassifier.sctrwitthin(intrs, na.ref1!, na.ref2!, na.ref12!);
        c2 = _PolyhedralBoundedSolidSetVertexVertexClassifier.sctrwitthin(intrs, nb.ref1!, nb.ref2!, nb.ref12!);
        if (c1 && c2) {
            if (
                (debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                0
            ) {
                platformPrint(" <TRUE>");
            }
            return true;
        } else {
            intrs = intrs.multiply(-1);
            c1 = _PolyhedralBoundedSolidSetVertexVertexClassifier.sctrwitthin(intrs, na.ref1!, na.ref2!, na.ref12!);
            c2 = _PolyhedralBoundedSolidSetVertexVertexClassifier.sctrwitthin(intrs, nb.ref1!, nb.ref2!, nb.ref12!);
            if (c1 && c2) {
                if (
                    (debugFlags &
                        _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                    0
                ) {
                    platformPrint(" <TRUE>");
                }
                return true;
            }
        }

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0) {
            platformPrint(" <FALSE>");
        }

        return false;
    }

    /**
    Given a pair of coincident vertices `va` and `vb`, this method creates the
    lists `nba`, `nbb`, and `sectors` as explained in section
    [MANT1988].15.6.2 and program [MANT1988].15.7.
    */
    private vertexVertexGetNeighborhood(va: _PolyhedralBoundedSolidVertex, vb: _PolyhedralBoundedSolidVertex): void {
        let i: number;
        const debugFlags = _PolyhedralBoundedSolidSetVertexVertexClassifier.debugFlags;

        this.nba = _PolyhedralBoundedSolidSetVertexVertexClassifier.nbrpreproc(va);
        this.nbb = _PolyhedralBoundedSolidSetVertexVertexClassifier.nbrpreproc(vb);
        this.sectors = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector>();
        const nba = this.nba;
        const nbb = this.nbb;
        const sectors = this.sectors;

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0) {
            console.log("   - NBA list of neighbor sectors for vertex on {A}:");
            for (i = 0; i < nba.size(); i++) {
                console.log("    . A[" + (i + 1) + "]: " + nba.get(i));
            }
            console.log("   - NBB list of neighbor sectors for vertex on {B}:");
            for (i = 0; i < nbb.size(); i++) {
                console.log("    . B[" + (i + 1) + "]: " + nbb.get(i));
            }
        }

        let d1: number;
        let d2: number;
        let d3: number;
        let d4: number;
        let j: number;
        let s: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector;
        let na: Vector3Dd;
        let nb: Vector3Dd;
        let xa: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex;
        let xb: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex;

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !== 0) {
            console.log(
                "   - Initial intersection tests between sectors (false intersections are sectors touching on a single point):",
            );
        }

        for (i = 0; i < nba.size(); i++) {
            for (j = 0; j < nbb.size(); j++) {
                if (
                    (debugFlags &
                        _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                    0
                ) {
                    platformPrint("    . A[" + (i + 1) + "] / B[" + (j + 1) + "]:");
                }

                if (this.vertexVertexSectorIntersectionTest(i, j)) {
                    s = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector();
                    s.secta = i;
                    s.sectb = j;
                    xa = nba.get(i);
                    xb = nbb.get(j);
                    s.hea = xa.he;
                    s.heb = xb.he;
                    s.wa = xa.wide;
                    s.wb = xb.wide;

                    na = xa.he!.parentLoop.parentFace.getContainingPlane()!.getNormal();
                    nb = xb.he!.parentLoop.parentFace.getContainingPlane()!.getNormal();
                    d1 = nb.dotProduct(xa.ref1!);
                    d2 = nb.dotProduct(xa.ref2!);
                    d3 = na.dotProduct(xb.ref1!);
                    d4 = na.dotProduct(xb.ref2!);
                    s.s1a = _PolyhedralBoundedSolidSetVertexVertexClassifier.compareToZero(d1);
                    s.s2a = _PolyhedralBoundedSolidSetVertexVertexClassifier.compareToZero(d2);
                    s.s1b = _PolyhedralBoundedSolidSetVertexVertexClassifier.compareToZero(d3);
                    s.s2b = _PolyhedralBoundedSolidSetVertexVertexClassifier.compareToZero(d4);
                    s.intersect = true;
                    sectors.add(s);
                }

                if (
                    (debugFlags &
                        _PolyhedralBoundedSolidSetVertexVertexClassifier.DEBUG_04_VERTEX_VERTEX_CLASSIFIER) !==
                    0
                ) {
                    platformPrint("\n");
                }
            }
        }
    }

    /**
    Following section [MANT1988].15.6.2 and program [MANT1988].15.10.
    */
    private vertexVertexReclassifyOnSectors(op: number): void {
        let i: number;
        let j: number;
        let newsa: number;
        let newsb: number;
        let secta: number;
        let prevsecta: number;
        let nextsecta: number;
        let sectb: number;
        let prevsectb: number;
        let nextsectb: number;
        let si: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector;
        let sj: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector;
        const nba = this.nba!;
        const nbb = this.nbb!;
        const sectors = this.sectors!;

        for (i = 0; i < sectors.size(); i++) {
            if (
                sectors.get(i).s1a === ON &&
                sectors.get(i).s2a === ON &&
                sectors.get(i).s1b === ON &&
                sectors.get(i).s2b === ON
            ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = secta === 0 ? nba.size() - 1 : secta - 1;
                prevsectb = sectb === 0 ? nbb.size() - 1 : sectb - 1;
                nextsecta = secta === nba.size() - 1 ? 0 : secta + 1;
                nextsectb = sectb === nbb.size() - 1 ? 0 : sectb + 1;
                const ha = nba.get(secta).he!;
                const hb = nbb.get(sectb).he!;
                const n1 = ha.parentLoop.parentFace.getContainingPlane()!.getNormal();
                const n2 = hb.parentLoop.parentFace.getContainingPlane()!.getNormal();
                const d = Vector3Dd.distance(n1, n2);
                const nonopposite = d < VSDK.EPSILON;
                if (nonopposite) {
                    newsa = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OUT : IN;
                    newsb = op === _PolyhedralBoundedSolidSetClassifier.UNION ? IN : OUT;
                } else {
                    newsa = op === _PolyhedralBoundedSolidSetClassifier.UNION ? IN : OUT;
                    newsb = op === _PolyhedralBoundedSolidSetClassifier.UNION ? IN : OUT;
                }
                si = sectors.get(i);

                for (j = 0; j < sectors.size(); j++) {
                    sj = sectors.get(j);
                    if (sj.secta === prevsecta && sj.sectb === sectb) {
                        if (sj.s1a !== ON) {
                            sj.s2a = newsa;
                        }
                    }
                    if (sj.secta === nextsecta && sj.sectb === sectb) {
                        if (sj.s2a !== ON) {
                            sj.s1a = newsa;
                        }
                    }
                    if (sj.secta === secta && sj.sectb === prevsectb) {
                        if (sj.s1b !== ON) {
                            sj.s2b = newsb;
                        }
                    }
                    if (sj.secta === secta && sj.sectb === nextsectb) {
                        if (sj.s2b !== ON) {
                            sj.s1b = newsb;
                        }
                    }
                    if (sj.s1a === sj.s2a && (sj.s1a === IN || sj.s1a === OUT)) {
                        sj.intersect = false;
                    }
                    if (sj.s1b === sj.s2b && (sj.s1b === IN || sj.s1b === OUT)) {
                        sj.intersect = false;
                    }
                }

                si.s1a = si.s2a = newsa;
                si.s1b = si.s2b = newsb;
                si.intersect = false;
                _SetOperationTrace.traceCoplanarTangential(
                    "  deactivated vv sector pair sectA=" + secta + " sectB=" + sectb,
                );
            }
        }
    }

    /**
    Resolves "on" edge classes from the local geometry of both vertex
    neighborhoods before the operation-biased rules of figures
    [MANT1988].15.10 to 15.12 are applied.

    <p>A sector edge is classified "on" when it lies in the plane of the
    other sector. The biased rules assume that the surface crosses the other
    solid along that edge. When the surface on both sides of the edge has
    the same class with respect to the other solid (for instance a concave
    edge of one solid lying on a convex edge of the other, with the faces
    interleaved), there is no crossing and the edge takes that class. When
    the two sides differ, the curve runs along the edge and it stays "on"
    for the biased rules.</p>
    */
    private resolveOnEdgesByLocalNeighborhood(): void {
        const ON = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON;

        for (const s of this.sectors!) {
            if (!s.intersect) {
                continue;
            }
            if (s.s1a === ON) {
                s.s1a = this.classifyRayWithoutCrossing(this.nba!, s.secta, true, this.nbb!);
            }
            if (s.s2a === ON) {
                s.s2a = this.classifyRayWithoutCrossing(this.nba!, s.secta, false, this.nbb!);
            }
            if (s.s1b === ON) {
                s.s1b = this.classifyRayWithoutCrossing(this.nbb!, s.sectb, true, this.nba!);
            }
            if (s.s2b === ON) {
                s.s2b = this.classifyRayWithoutCrossing(this.nbb!, s.sectb, false, this.nba!);
            }
            if ((s.s1a === s.s2a && s.s1a !== ON) || (s.s1b === s.s2b && s.s1b !== ON)) {
                s.intersect = false;
            }
        }
    }

    /**
    Class of the surface next to a boundary ray of sector `index` in `own`,
    measured against the `other` neighborhood on both sides of the ray (in
    this sector and in the adjacent one that shares the ray). ON when the
    sides differ or either side lies on the other solid's surface.
    */
    private classifyRayWithoutCrossing(
        own: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>,
        index: number,
        firstRay: boolean,
        other: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>,
    ): number {
        const ON = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON;
        let adjacent: _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex;
        let ray: Vector3Dd;
        let adjacentRay: Vector3Dd;
        let adjacentOther: Vector3Dd;
        let here: number;

        const n = own.get(index);
        if (firstRay) {
            adjacent = own.get(index === 0 ? own.size() - 1 : index - 1);
            ray = n.ref1!;
            here = this.classifyDirectionInNeighborhood(
                _PolyhedralBoundedSolidSetVertexVertexClassifier.directionNextToRay(n.ref1!, n.ref2!),
                other,
            );
            adjacentRay = adjacent.ref2!;
            adjacentOther = adjacent.ref1!;
        } else {
            adjacent = own.get(index === own.size() - 1 ? 0 : index + 1);
            ray = n.ref2!;
            here = this.classifyDirectionInNeighborhood(
                _PolyhedralBoundedSolidSetVertexVertexClassifier.directionNextToRay(n.ref2!, n.ref1!),
                other,
            );
            adjacentRay = adjacent.ref1!;
            adjacentOther = adjacent.ref2!;
        }
        if (
            here === ON ||
            new Vector3Dd(ray).normalized().dotProduct(new Vector3Dd(adjacentRay).normalized()) < 1.0 - VSDK.EPSILON
        ) {
            return ON;
        }
        const there = this.classifyDirectionInNeighborhood(
            _PolyhedralBoundedSolidSetVertexVertexClassifier.directionNextToRay(adjacentRay, adjacentOther),
            other,
        );
        return there === here ? here : ON;
    }

    /**
    A direction inside a (minor arc) sector, next to its boundary ray.
    */
    private static directionNextToRay(ray: Vector3Dd, otherRay: Vector3Dd): Vector3Dd {
        const r = new Vector3Dd(ray).normalized();
        const o = new Vector3Dd(otherRay).normalized();
        return r.add(o.multiply(1.0e-3)).normalized();
    }

    private static readonly PROBE_RAYS: Vector3Dd[] = [
        new Vector3Dd(0.5773, 0.3341, 0.7452).normalized(),
        new Vector3Dd(-0.2311, 0.8817, 0.4113).normalized(),
        new Vector3Dd(0.7061, -0.6129, 0.3549).normalized(),
    ];

    /**
    Classifies a direction leaving the common vertex against the cone that
    the given neighborhood sectors bound: IN, OUT, or ON when the direction
    lies on one of those sectors or no probe ray gives an unambiguous count.
    Ray parity over the sectors, each a planar minor arc through the vertex.
    */
    private classifyDirectionInNeighborhood(
        d: Vector3Dd,
        nb: ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>,
    ): number {
        const ON = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON;
        const planeTolerance = 1.0e-9;

        for (const n of nb) {
            const normal = n.he!.parentLoop.parentFace.getContainingPlane()!.getNormal();
            if (
                Math.abs(d.dotProduct(normal)) < planeTolerance &&
                _PolyhedralBoundedSolidSetVertexVertexClassifier.sctrwitthin(d, n.ref1!, n.ref2!, n.ref12!)
            ) {
                return ON;
            }
        }

        for (const w of _PolyhedralBoundedSolidSetVertexVertexClassifier.PROBE_RAYS) {
            let crossings = 0;
            let ambiguous = false;

            for (const n of nb) {
                const normal = n.he!.parentLoop.parentFace.getContainingPlane()!.getNormal();
                const denominator = w.dotProduct(normal);
                let t: number;
                let q: Vector3Dd;

                if (Math.abs(denominator) < 1.0e-9) {
                    continue;
                }
                t = -d.dotProduct(normal) / denominator;
                if (t <= 0.0) {
                    continue;
                }
                q = d.add(w.multiply(t));
                if (
                    q.length() < 1.0e-9 ||
                    PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                        q,
                        n.ref1!,
                        _PolyhedralBoundedSolidOperator.numericContext,
                    ) ||
                    PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                        q,
                        n.ref2!,
                        _PolyhedralBoundedSolidOperator.numericContext,
                    )
                ) {
                    ambiguous = true;
                    break;
                }
                if (_PolyhedralBoundedSolidSetVertexVertexClassifier.sctrwitthin(q, n.ref1!, n.ref2!, n.ref12!)) {
                    crossings++;
                }
            }
            if (!ambiguous) {
                return crossings % 2 === 1
                    ? _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN
                    : _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
            }
        }
        return ON;
    }

    /**
    Reclassification procedure for "on"-edges on the vertex/vertex classifier,
    as expected from the high-level description of section [MANT1988].15.6.2
    and the case structures of figures [MANT1988].15.10, [MANT1988].15.11,
    and [MANT1988].15.12.
    */
    private vertexVertexReclassifyOnEdges(op: number): void {
        let i: number;
        let j: number;
        let newsa: number;
        let newsb: number;
        let secta: number;
        let prevsecta: number;
        let sectb: number;
        let prevsectb: number;
        const nba = this.nba!;
        const nbb = this.nbb!;
        const sectors = this.sectors!;

        this.resolveOnEdgesByLocalNeighborhood();

        for (i = 0; i < sectors.size(); i++) {
            if (sectors.get(i).intersect && sectors.get(i).s1a === ON && sectors.get(i).s1b === ON) {
                newsa = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OUT : IN;
                newsb = op === _PolyhedralBoundedSolidSetClassifier.UNION ? IN : OUT;

                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = secta === 0 ? nba.size() - 1 : secta - 1;
                prevsectb = sectb === 0 ? nbb.size() - 1 : sectb - 1;

                for (j = 0; j < sectors.size(); j++) {
                    if (sectors.get(j).intersect) {
                        if (sectors.get(j).secta === secta && sectors.get(j).sectb === sectb) {
                            sectors.get(j).s1a = newsa;
                            sectors.get(j).s1b = newsb;
                        }

                        if (sectors.get(j).secta === prevsecta && sectors.get(j).sectb === sectb) {
                            sectors.get(j).s2a = newsa;
                            sectors.get(j).s1b = newsb;
                        }

                        if (sectors.get(j).secta === secta && sectors.get(j).sectb === prevsectb) {
                            sectors.get(j).s1a = newsa;
                            sectors.get(j).s2b = newsb;
                        }

                        if (sectors.get(j).secta === prevsecta && sectors.get(j).sectb === prevsectb) {
                            sectors.get(j).s2a = newsa;
                            sectors.get(j).s2b = newsb;
                        }

                        if (
                            sectors.get(j).s1a === sectors.get(j).s2a &&
                            (sectors.get(j).s1a === IN || sectors.get(j).s1a === OUT)
                        ) {
                            sectors.get(j).intersect = false;
                        }
                        if (
                            sectors.get(j).s1b === sectors.get(j).s2b &&
                            (sectors.get(j).s1b === IN || sectors.get(j).s1b === OUT)
                        ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
        }

        for (i = 0; i < sectors.size(); i++) {
            if (sectors.get(i).intersect && sectors.get(i).s1a === ON) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = secta === 0 ? nba.size() - 1 : secta - 1;
                prevsectb = sectb === 0 ? nbb.size() - 1 : sectb - 1;
                newsa = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OUT : IN;

                for (j = 0; j < sectors.size(); j++) {
                    if (sectors.get(j).intersect) {
                        if (sectors.get(j).secta === secta && sectors.get(j).sectb === sectb) {
                            sectors.get(j).s1a = newsa;
                        }

                        if (sectors.get(j).secta === prevsecta && sectors.get(j).sectb === sectb) {
                            sectors.get(j).s2a = newsa;
                        }

                        if (
                            sectors.get(j).s1a === sectors.get(j).s2a &&
                            (sectors.get(j).s1a === IN || sectors.get(j).s1a === OUT)
                        ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            } else if (sectors.get(i).intersect && sectors.get(i).s1b === ON) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = secta === 0 ? nba.size() - 1 : secta - 1;
                prevsectb = sectb === 0 ? nbb.size() - 1 : sectb - 1;
                newsb = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OUT : IN;

                for (j = 0; j < sectors.size(); j++) {
                    if (sectors.get(j).intersect) {
                        if (sectors.get(j).secta === secta && sectors.get(j).sectb === sectb) {
                            sectors.get(j).s1b = newsb;
                        }

                        if (sectors.get(j).secta === secta && sectors.get(j).sectb === prevsectb) {
                            sectors.get(j).s2b = newsb;
                        }

                        if (
                            sectors.get(j).s1b === sectors.get(j).s2b &&
                            (sectors.get(j).s1b === IN || sectors.get(j).s1b === OUT)
                        ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
        }
    }
}

type _VertexVertexClassificationData = VertexVertexClassificationData;

export namespace _PolyhedralBoundedSolidSetVertexVertexClassifier {
    export type VertexVertexClassificationData = _VertexVertexClassificationData;
}
