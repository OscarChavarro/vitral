import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators as Euler } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
describe("PolyhedralBoundedSolidEulerOperatorsTest", () => {
    it("creates and clears a skeletal solid", () => {
        const s = new PolyhedralBoundedSolid();
        Euler.mvfs(s, new Vector3Dd(), 1, 1);
        const h = s.findFace(1)!.boundariesList[0]!.boundaryStartHalfEdge!;
        Euler.lmev(s, h, h, 2, new Vector3Dd(1, 0, 0));
        expect(s.getVerticesList()).toHaveLength(2);
        expect(s.getEdgesList()).toHaveLength(1);
    });

    it("restores a skeletal solid through lkev and kvfs", () => {
        const solid = new PolyhedralBoundedSolid();
        Euler.mvfs(solid, new Vector3Dd(), 1, 1);
        expect(Euler.smev(solid, 1, 1, 2, new Vector3Dd(1, 0, 0))).toBe(true);
        const edge = solid.getEdgesList()[0]!;
        Euler.lkev(solid, edge.rightHalf!, edge.leftHalf!);
        expect(solid.getEdgesList()).toHaveLength(0);
        expect(solid.getVerticesList()).toHaveLength(1);
        Euler.kvfs(solid);
        expect(solid.getPolygonsList()).toHaveLength(0);
        expect(solid.getVerticesList()).toHaveLength(0);
    });

    it("splits a wire face with lmef and restores it with lkef", () => {
        const solid = new PolyhedralBoundedSolid();
        Euler.mvfs(solid, new Vector3Dd(), 1, 1);
        expect(Euler.smev(solid, 1, 1, 2, new Vector3Dd(1, 0, 0))).toBe(true);
        expect(Euler.smev(solid, 1, 2, 3, new Vector3Dd(1, 1, 0))).toBe(true);
        expect(Euler.smev(solid, 1, 3, 4, new Vector3Dd(0, 1, 0))).toBe(true);
        const face = solid.findFace(1)!;
        const splitFace = Euler.lmef(solid, face.findHalfEdge(4)!, face.findHalfEdge(1)!, 2);
        expect(solid.getPolygonsList()).toHaveLength(2);
        const bridge = splitFace.boundariesList[0]!.boundaryStartHalfEdge!.parentEdge!;
        Euler.lkef(solid, bridge.rightHalf!, bridge.leftHalf!);
        expect(solid.getPolygonsList()).toHaveLength(1);
        const message: string[] = [];
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid, message), message.join(" ")).toBe(
            true,
        );
    });
    it("merges adjacent faces", () => {
        const s = new Box(2, 2, 2).exportToPolyhedralBoundedSolid(),
            e = s.getEdgesList()[0]!;
        Euler.lkef(s, e.leftHalf!, e.rightHalf!);
        expect(s.getPolygonsList()).toHaveLength(5);
        expect(s.getEdgesList()).toHaveLength(11);
    });
    it("bridges two rings and can split the bridge again", () => {
        const s = new PolyhedralBoundedSolid(),
            f = new _PolyhedralBoundedSolidFace(s, 1),
            a = new _PolyhedralBoundedSolidLoop(f),
            b = new _PolyhedralBoundedSolidLoop(f);
        const va = new _PolyhedralBoundedSolidVertex(s, new Vector3Dd(), 1),
            vb = new _PolyhedralBoundedSolidVertex(s, new Vector3Dd(1, 0, 0), 2),
            ha = new _PolyhedralBoundedSolidHalfEdge(va, a),
            hb = new _PolyhedralBoundedSolidHalfEdge(vb, b);
        a.halfEdgesList.add(ha);
        b.halfEdgesList.add(hb);
        a.boundaryStartHalfEdge = ha;
        b.boundaryStartHalfEdge = hb;
        Euler.lmekr(s, ha, hb);
        expect(f.boundariesList).toHaveLength(1);
        const edge = s.getEdgesList()[0]!;
        Euler.lkemr(s, edge.leftHalf!, edge.rightHalf!);
        expect(f.boundariesList).toHaveLength(2);
    });
    it("moves a ring between faces", () => {
        const s = new PolyhedralBoundedSolid(),
            source = new _PolyhedralBoundedSolidFace(s, 1),
            target = new _PolyhedralBoundedSolidFace(s, 2),
            outer = new _PolyhedralBoundedSolidLoop(source),
            ring = new _PolyhedralBoundedSolidLoop(source),
            targetOuter = new _PolyhedralBoundedSolidLoop(target),
            v = new _PolyhedralBoundedSolidVertex(s, new Vector3Dd(), 1);
        for (const l of [outer, ring, targetOuter]) {
            const h = new _PolyhedralBoundedSolidHalfEdge(v, l);
            l.halfEdgesList.add(h);
            l.boundaryStartHalfEdge = h;
        }
        expect(Euler.lringmv(s, ring, target, false)).toBe(true);
        expect(ring.parentFace).toBe(target);
    });

    it("moves a face loop into and out of a hole through kimrh/mikrh aliases", () => {
        const solid = new Box(2, 2, 2).exportToPolyhedralBoundedSolid(),
            destination = solid.getPolygonsList()[0]!,
            source = solid.getPolygonsList()[1]!,
            loop = source.boundariesList[0]!,
            faceCount = solid.getPolygonsList().length;
        Euler.lkimrh(solid, destination, source);
        expect(solid.getPolygonsList()).toHaveLength(faceCount - 1);
        expect(loop.parentFace).toBe(destination);
        const restored = Euler.lmikrh(solid, loop, 99);
        expect(restored.id).toBe(99);
        expect(restored.boundariesList).toEqual([loop]);
    });
    it("preserves a closed vertex neighbourhood through lmev and lkev", () => {
        const s = new Box(2, 2, 2).exportToPolyhedralBoundedSolid(),
            v = s.getVerticesList()[0]!;
        const hs = s
            .getPolygonsList()
            .flatMap((f) =>
                f.boundariesList.flatMap((l) =>
                    Array.from({ length: l.halfEdgesList.size() }, (_, i) => l.halfEdgesList.get(i)!),
                ),
            )
            .filter((h) => h.startingVertex === v);
        const first = hs[0]!,
            second = hs[1]!;
        Euler.lmev(s, first, second, 99, new Vector3Dd(2, 2, 2));
        const edge = s.getEdgesList().at(-1)!;
        Euler.lkev(s, edge.leftHalf!, edge.rightHalf!);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(s)).toBe(true);
    });
});
