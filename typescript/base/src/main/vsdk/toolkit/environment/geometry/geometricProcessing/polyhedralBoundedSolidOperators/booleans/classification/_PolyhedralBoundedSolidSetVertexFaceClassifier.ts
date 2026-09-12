//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Boolean as JavaBoolean } from "../../../../../../../../java/lang/Boolean.js";
import { platformPrint } from "../../../../../../../../java/lang/_PlatformConsole.js";
import { ArrayList } from "../../../../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../../../../java/util/Collections.js";
import { Vector3Dd } from "../../../../../../common/linealAlgebra/Vector3Dd.js";
import type { InfinitePlane } from "../../../../surface/InfinitePlane.js";
import type { PolyhedralBoundedSolid } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import type { _PolyhedralBoundedSolidFace } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "../intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";
import { _PolyhedralBoundedSolidSetOperatorNullEdge } from "../topology/_PolyhedralBoundedSolidSetOperatorNullEdge.js";
import { _PolyhedralBoundedSolidSetClassifier } from "./_PolyhedralBoundedSolidSetClassifier.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace } from "./_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.js";
import { _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector } from "./_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.js";

type OnFace = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace;
const OnFace = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace;
const OnSector = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector;

/**
Classification stages for vertex/face coincidences during set operations,
starting from the splitting-classifier rules of [MANT1988].14.5 and adapted
to boundary classification by [MANT1988].15.6.1 and problem [MANT1988].15.4.
*/
export class _PolyhedralBoundedSolidSetVertexFaceClassifier extends _PolyhedralBoundedSolidOperator {
    private static readonly TRACE_COPLANAR_TANGENTIAL_PROPERTY = "vsdk.setop.traceCoplanarTangential";
    private static readonly DEBUG_01_STRUCTURE = 0x01;
    private static readonly DEBUG_03_VERTEX_FACE_CLASSIFIER = 0x04;
    private static readonly DEBUG_99_SHOW_OPERATIONS = 0x40;

    private static debugFlags = 0;
    private sonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
    private soneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;

    private static isCoplanarTangentialTraceEnabled(): boolean {
        return JavaBoolean.getBoolean(
            _PolyhedralBoundedSolidSetVertexFaceClassifier.TRACE_COPLANAR_TANGENTIAL_PROPERTY,
        );
    }

    private static traceCoplanarTangential(message: string): void {
        if (!_PolyhedralBoundedSolidSetVertexFaceClassifier.isCoplanarTangentialTraceEnabled()) {
            return;
        }
        console.log("[SetOpCoplanarTrace] " + message);
    }

    /**
    Vertex/Face classifier for the set operations algorithm (big phase 1).
    Answer to problem [MANT1988].15.4.
    */
    public classify(
        v: _PolyhedralBoundedSolidVertex,
        f: _PolyhedralBoundedSolidFace,
        op: number,
        BvsA: number,
        flags: number,
        inSonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>,
        inSoneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        _PolyhedralBoundedSolidSetVertexFaceClassifier.debugFlags = flags;
        this.sonea = inSonea;
        this.soneb = inSoneb;
        this.vertexFaceClassify(v, f, op, BvsA, inSolidA, inSolidB);
    }

    private static compareToZero(value: number): number {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor.compareToZero(value);
    }

    private static applyCoplanarRulesToVertexFaceNeighborhood(
        nbr: ArrayList<OnFace>,
        referenceFace: _PolyhedralBoundedSolidFace,
        referencePlane: InfinitePlane | null,
        BvsA: number,
        op: number,
        useMirrorFace: boolean,
    ): void {
        let current: OnFace;
        let he: _PolyhedralBoundedSolidHalfEdge | null;
        let localFace: _PolyhedralBoundedSolidFace;
        let c: Vector3Dd;
        let d: number;
        let i: number;

        const nnbr = nbr.size();
        const backup = new ArrayList<OnFace>();
        for (i = 0; i < nnbr; i++) {
            backup.add(new OnFace(nbr.get(i)));
        }

        for (i = 0; i < nnbr; i++) {
            current = nbr.get(i);
            he = current.sector;
            if (he === null || he.parentLoop === null || he.parentLoop.parentFace === null) {
                continue;
            }

            if (useMirrorFace) {
                he = he.mirrorHalfEdge();
                if (he === null || he.parentLoop === null || he.parentLoop.parentFace === null) {
                    continue;
                }
            }
            localFace = he.parentLoop.parentFace;
            if (localFace.getContainingPlane() === null || referencePlane === null) {
                continue;
            }

            c = localFace.getContainingPlane()!.getNormal().crossProduct(referencePlane.getNormal());
            d = c.dotProduct(c);
            if (_PolyhedralBoundedSolidSetVertexFaceClassifier.compareToZero(d) !== 0) {
                continue;
            }

            d = localFace.getContainingPlane()!.getNormal().dotProduct(referencePlane.getNormal());
            if (_PolyhedralBoundedSolidSetVertexFaceClassifier.compareToZero(d) === 1) {
                if (BvsA !== 0) {
                    nbr.get(i).cl = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.IN : OnSector.OUT;
                    nbr.get((i + 1) % nnbr).cl =
                        op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.IN : OnSector.OUT;
                } else {
                    nbr.get(i).cl = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.OUT : OnSector.IN;
                    nbr.get((i + 1) % nnbr).cl =
                        op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.OUT : OnSector.IN;
                }
            } else {
                if (BvsA !== 0) {
                    nbr.get(i).cl = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.IN : OnSector.OUT;
                    nbr.get((i + 1) % nnbr).cl =
                        op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.IN : OnSector.OUT;
                } else {
                    nbr.get(i).cl = op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.IN : OnSector.OUT;
                    nbr.get((i + 1) % nnbr).cl =
                        op === _PolyhedralBoundedSolidSetClassifier.UNION ? OnSector.IN : OnSector.OUT;
                }
            }

            _PolyhedralBoundedSolidSetVertexFaceClassifier.traceCoplanarTangential(
                "vf legacy coplanar rule op=" +
                    op +
                    " side=" +
                    BvsA +
                    " face=" +
                    referenceFace.id +
                    " localFace=" +
                    localFace.id +
                    " sectorIndex=" +
                    i +
                    " class=" +
                    nbr.get(i).cl,
            );
        }

        let ins = 0;
        let outs = 0;
        for (i = 0; i < nnbr; i++) {
            if (nbr.get(i).cl === OnSector.OUT) {
                outs++;
            } else {
                ins++;
            }
        }
        if (outs === nnbr || ins === nnbr) {
            for (i = 0; i < nnbr; i++) {
                nbr.get(i).cl = backup.get(i).cl;
            }
        }
    }

    private static nextVertexId(current: PolyhedralBoundedSolid, other: PolyhedralBoundedSolid): number {
        const a = current.getMaxVertexId();
        const b = other.getMaxVertexId();
        let m = a;
        if (b > a) {
            m = b;
        }

        return m + 1;
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    This is the inward-oriented variant of the bisector from problem
    [MANT1988].14.1 used by the vertex/face classifier.
    */
    private static inside(he: _PolyhedralBoundedSolidHalfEdge): Vector3Dd {
        let middle: Vector3Dd;
        let a: Vector3Dd;
        // Java names this local `b`; it is computed but unused there too.
        let _b: Vector3Dd;

        a = he.next()!.startingVertex.position.subtract(he.startingVertex.position);
        _b = he.previous()!.startingVertex.position.subtract(he.startingVertex.position);
        a = a.normalized();
        _b = _b.normalized();

        const n = he.parentLoop.parentFace.getContainingPlane()!.getNormal();

        middle = n.crossProduct(a);
        middle = middle.normalized();

        return middle;
    }

    /**
    Current method is the first step for the initial vertex/face
    classification of sectors (vertex neighborhood) for `vtx`, as indicated on
    section [MANT1988].14.5.2 and program [MANT1988].14.4, but biased towards
    the set operator classifier as proposed on section [MANT1988].15.6.1 and
    problem [MANT1988].15.4.
    */
    private static vertexFaceGetNeighborhood(
        vtx: _PolyhedralBoundedSolidVertex,
        referencePlane: InfinitePlane,
        _BvsA: number,
    ): ArrayList<OnFace> {
        let he: _PolyhedralBoundedSolidHalfEdge;
        let bisect: Vector3Dd;
        let d: number;
        let c: OnFace;

        const neighborSectorsInfo = new ArrayList<OnFace>();

        he = vtx.emanatingHalfEdge!;
        do {
            c = new OnFace();
            c.sector = he;
            d = referencePlane.pointDistance(he.next()!.startingVertex.position);
            c.cl = _PolyhedralBoundedSolidSetVertexFaceClassifier.compareToZero(d);
            c.isWide = false;
            c.position = new Vector3Dd(he.next()!.startingVertex.position);
            c.situation = OnFace.UNDEFINED;
            c.referencePlane = referencePlane;
            neighborSectorsInfo.add(c);
            if (_PolyhedralBoundedSolidOperator.checkWideness(he)) {
                bisect = _PolyhedralBoundedSolidSetVertexFaceClassifier.inside(he).add(vtx.position);
                c.situation = OnFace.CROSSING_EDGE;

                c = new OnFace();
                c.sector = he;
                d = referencePlane.pointDistance(bisect);
                c.cl = _PolyhedralBoundedSolidSetVertexFaceClassifier.compareToZero(d);
                c.isWide = true;
                c.position = new Vector3Dd(bisect);
                c.situation = OnFace.CROSSING_EDGE;
                c.referencePlane = referencePlane;
                neighborSectorsInfo.add(c);
            }
            he = he.mirrorHalfEdge()!.next()!;
        } while (he !== vtx.emanatingHalfEdge);

        let i: number;

        for (i = 0; i < neighborSectorsInfo.size(); i++) {
            c = neighborSectorsInfo.get(i);
            if (c.cl === OnFace.ON && c.situation === OnFace.UNDEFINED) {
                c.situation = OnFace.INPLANE_EDGE;
            }
        }

        return neighborSectorsInfo;
    }

    /**
    Reclassifies the vertex/face neighborhood per [MANT1988].14.5
    (sections 14.5.1 and 14.5.2), biased towards the set-operator classifier
    as proposed in section 15.6.1 and problem 15.4. The `useMirrorFace`
    flag of the underlying processor is always `false` here: the
    wMANT2008 mirror variant was the now-removed "borrowed" branch (§5.1
    cleanup, plan-csg-boolean-fix-stage2).
    */
    private static vertexFaceReclassifyOnSectors(
        nbr: ArrayList<OnFace>,
        referenceFace: _PolyhedralBoundedSolidFace,
        referencePlane: InfinitePlane | null,
        BvsA: number,
        op: number,
    ): void {
        _PolyhedralBoundedSolidSetVertexFaceClassifier.applyCoplanarRulesToVertexFaceNeighborhood(
            nbr,
            referenceFace,
            referencePlane,
            BvsA,
            op,
            false,
        );
    }

    private static printNbr(neighborSectorsInfo: ArrayList<OnFace>): void {
        let i: number;

        for (i = 0; i < neighborSectorsInfo.size(); i++) {
            console.log("    . " + neighborSectorsInfo.get(i));
        }
    }

    private static inplaneEdgesOn(nbr: ArrayList<OnFace>): boolean {
        let i: number;

        for (i = 0; i < nbr.size(); i++) {
            if (nbr.get(i).situation === OnFace.INPLANE_EDGE) {
                return true;
            }
        }
        return false;
    }

    /**
    Current method implements the set of changes from table [MANT1988].15.3
    for the edge reclassification rules.
    */
    private static vertexFaceReclassifyOnEdges(nbr: ArrayList<OnFace>, op: number): void {
        let l: OnFace;
        let i: number;

        for (i = 0; i < nbr.size(); i++) {
            l = nbr.get(i);
            l.applyRules(op);
        }
    }

    /**
    This method implements the third stage of the vertex/face classifier:
    given the previously reclassified list of vertex neighbors, insert a new
    vertex in the direction of the last "in" before an "out" sector of the
    sequence. This follows section [MANT1988].14.6.2 and program
    [MANT1988].14.7, biased for set operations as indicated by
    [MANT1988].15.6.1.
    */
    private vertexFaceInsertNullEdges(
        nbr: ArrayList<OnFace>,
        f: _PolyhedralBoundedSolidFace,
        v: _PolyhedralBoundedSolidVertex,
        BvsA: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        let start: number;
        let i: number;
        let head: _PolyhedralBoundedSolidHalfEdge;
        let tail: _PolyhedralBoundedSolidHalfEdge;
        const nnbr = nbr.size();
        let sone: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
        const debugFlags = _PolyhedralBoundedSolidSetVertexFaceClassifier.debugFlags;

        const solida = v.emanatingHalfEdge!.parentLoop.parentFace.parentSolid;

        if (nnbr <= 0) {
            return;
        }

        i = 0;
        while (!(
            (nbr.get(i).cl === OnFace.AinB || nbr.get(i).cl === OnFace.BinA) &&
            (nbr.get((i + 1) % nnbr).cl === OnFace.AoutB || nbr.get((i + 1) % nnbr).cl === OnFace.BoutA)
        )) {
            i++;
            if (i >= nnbr) {
                return;
            }
        }
        start = i;
        head = nbr.get(i).sector!;

        while (true) {
            while (!(
                (nbr.get(i).cl === OnFace.AoutB || nbr.get(i).cl === OnFace.BoutA) &&
                (nbr.get((i + 1) % nnbr).cl === OnFace.AinB || nbr.get((i + 1) % nnbr).cl === OnFace.BinA)
            )) {
                i = (i + 1) % nnbr;
            }
            tail = nbr.get(i).sector!;

            if (
                (debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_03_VERTEX_FACE_CLASSIFIER) !==
                    0x00 &&
                (debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_99_SHOW_OPERATIONS) !== 0x00
            ) {
                console.log("       -> LMEV (Vertex/face split):");
                console.log("          . (" + start + ") H1: " + head);
                console.log("          . (" + i + ") H2: " + tail);
            }

            PolyhedralBoundedSolidEulerOperators.lmev(
                solida,
                head,
                tail,
                _PolyhedralBoundedSolidSetVertexFaceClassifier.nextVertexId(inSolidA, inSolidB),
                head.startingVertex.position,
            );

            if (
                (debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_03_VERTEX_FACE_CLASSIFIER) !==
                    0x00 &&
                (debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_99_SHOW_OPERATIONS) !== 0x00
            ) {
                console.log("          . New vertex: " + head.startingVertex.id);
            }

            if (BvsA !== 0) {
                sone = this.soneb;
            } else {
                sone = this.sonea;
            }
            sone!.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(head.previous()!.parentEdge!));

            this.makeRing(f, v, BvsA, inSolidA, inSolidB);

            while (!(
                (nbr.get(i).cl === OnFace.AinB || nbr.get(i).cl === OnFace.BinA) &&
                (nbr.get((i + 1) % nnbr).cl === OnFace.AoutB || nbr.get((i + 1) % nnbr).cl === OnFace.BoutA)
            )) {
                i = (i + 1) % nnbr;
                if (i === start) {
                    return;
                }
            }
        }
    }

    /**
    Vertex/Face classifier for the set operations algorithm (big phase 1).
    Answer to problem [MANT1988].15.4.
    */
    private vertexFaceClassify(
        v: _PolyhedralBoundedSolidVertex,
        f: _PolyhedralBoundedSolidFace,
        op: number,
        BvsA: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        const debugFlags = _PolyhedralBoundedSolidSetVertexFaceClassifier.debugFlags;

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_01_STRUCTURE) !== 0x00) {
            if (
                (debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_03_VERTEX_FACE_CLASSIFIER) !==
                0x00
            ) {
                platformPrint("  * ");
            } else {
                platformPrint("  - ");
            }
            console.log("Vertex/face pair V[" + v.id + "] / f[" + f.id + "]");
        }

        const nbr = _PolyhedralBoundedSolidSetVertexFaceClassifier.vertexFaceGetNeighborhood(
            v,
            f.getContainingPlane()!,
            BvsA,
        );
        if (_PolyhedralBoundedSolidSetVertexFaceClassifier.inplaneEdgesOn(nbr)) {
            Collections.reverse(nbr);
        }

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_03_VERTEX_FACE_CLASSIFIER) !== 0x00) {
            console.log("   - Initial sector neigborhood by near end vertices:");
            _PolyhedralBoundedSolidSetVertexFaceClassifier.printNbr(nbr);
        }

        _PolyhedralBoundedSolidSetVertexFaceClassifier.vertexFaceReclassifyOnSectors(
            nbr,
            f,
            f.getContainingPlane(),
            BvsA,
            op,
        );

        let i: number;
        for (i = 0; i < nbr.size(); i++) {
            nbr.get(i).updateLabel(BvsA);
        }

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_03_VERTEX_FACE_CLASSIFIER) !== 0x00) {
            console.log("   - Sector neigborhood reclassified on sectors (8-way boundary classification):");
            _PolyhedralBoundedSolidSetVertexFaceClassifier.printNbr(nbr);
        }

        _PolyhedralBoundedSolidSetVertexFaceClassifier.vertexFaceReclassifyOnEdges(nbr, op);

        if ((debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_03_VERTEX_FACE_CLASSIFIER) !== 0x00) {
            console.log("   - Sector neigborhood reclassified on edges:");
            _PolyhedralBoundedSolidSetVertexFaceClassifier.printNbr(nbr);
        }

        this.vertexFaceInsertNullEdges(nbr, f, v, BvsA, inSolidA, inSolidB);
    }

    private makeRing(
        f: _PolyhedralBoundedSolidFace,
        v: _PolyhedralBoundedSolidVertex,
        type: number,
        inSolidA: PolyhedralBoundedSolid,
        inSolidB: PolyhedralBoundedSolid,
    ): void {
        let solida: PolyhedralBoundedSolid;
        let solidb: PolyhedralBoundedSolid;
        let he: _PolyhedralBoundedSolidHalfEdge;
        const debugFlags = _PolyhedralBoundedSolidSetVertexFaceClassifier.debugFlags;

        solida = inSolidA;
        solidb = inSolidB;
        if (type === 1) {
            solida = inSolidB;
            solidb = inSolidA;
        }

        he = f.boundariesList.get(0)!.boundaryStartHalfEdge!;
        const ringSolid = he.parentLoop.parentFace.parentSolid;

        const vn1 = _PolyhedralBoundedSolidSetVertexFaceClassifier.nextVertexId(solida, solidb);
        PolyhedralBoundedSolidEulerOperators.lmev(ringSolid, he, he, vn1, v.position);
        he = he.previous()!;
        PolyhedralBoundedSolidEulerOperators.lkemr(ringSolid, he.mirrorHalfEdge()!, he);

        const vn2 = _PolyhedralBoundedSolidSetVertexFaceClassifier.nextVertexId(solida, solidb);
        PolyhedralBoundedSolidEulerOperators.lmev(ringSolid, he, he, vn2, v.position);

        if (
            (debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_03_VERTEX_FACE_CLASSIFIER) !== 0x00 &&
            (debugFlags & _PolyhedralBoundedSolidSetVertexFaceClassifier.DEBUG_99_SHOW_OPERATIONS) !== 0x00
        ) {
            console.log("       -> MAKE_RING (Vertex/face pierce):");
            console.log("          . New vertexes: " + vn1 + "/" + vn2 + ".");
        }

        let sone: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
        if (type === 1) {
            sone = this.sonea;
        } else {
            sone = this.soneb;
        }
        sone!.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(he.parentEdge!));
    }
}
