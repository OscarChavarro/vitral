//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { VSDK } from "../../../../common/VSDK.js";
import { Logger } from "../../../../common/logging/Logger.js";
import type { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidEdge } from "./nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "./nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "./nodes/_PolyhedralBoundedSolidLoop.js";

/**
Topological validation helpers for the half-edge representation described in
[MANT1988].10.2.1 and [MANT1988].10.2.2.
*/
export class _PolyhedralBoundedSolidTopologicalValidator {
    private constructor() {}

    /**
    Checks the fundamental half-edge consistency expected from the graph and
    identification structure described in [MANT1988].10.2.1 and
    [MANT1988].10.2.2.
    */
    public static validateTopologicalIntegrity(solid: PolyhedralBoundedSolid): boolean {
        let i: number;
        let j: number;
        let k: number;
        let e: _PolyhedralBoundedSolidEdge;
        let h1: _PolyhedralBoundedSolidHalfEdge | null;
        let h2: _PolyhedralBoundedSolidHalfEdge | null;
        let f: _PolyhedralBoundedSolidFace;
        let l: _PolyhedralBoundedSolidLoop;

        for (i = 0; i < solid.getEdgesList().size(); i++) {
            e = solid.getEdgesList().get(i)!;
            h1 = e.rightHalf;
            h2 = e.leftHalf;
            if (h1 === null || h2 === null) {
                Logger.reportMessage(solid, VSDK.WARNING, "validateTopologicalIntegrity", "Edge with null halfedge!");
                return false;
            }
            if (h1.parentLoop.parentFace.parentSolid !== h2.parentLoop.parentFace.parentSolid) {
                Logger.reportMessage(
                    solid,
                    VSDK.WARNING,
                    "validateTopologicalIntegrity",
                    "Edge belonging to two different solids!",
                );
                return false;
            }
        }

        const edgeCount = new Int32Array(solid.getEdgesList().size());
        for (i = 0; i < edgeCount.length; i++) {
            edgeCount[i] = 0;
        }

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            f = solid.getPolygonsList().get(i)!;
            for (j = 0; j < f.boundariesList.size(); j++) {
                let he: _PolyhedralBoundedSolidHalfEdge | null;
                l = f.boundariesList.get(j)!;

                he = l.boundaryStartHalfEdge;
                if (he === null) {
                    Logger.reportMessage(
                        solid,
                        VSDK.WARNING,
                        "validateTopologicalIntegrity",
                        "Loop without starting halfedge\n" + "Offending solid:\n" + solid.toString(),
                    );
                    return false;
                }
                const heStart = he;
                do {
                    he = he!.next();
                    if (he === null) {
                        Logger.reportMessage(solid, VSDK.WARNING, "validateTopologicalIntegrity", "Not closed loop!");
                        return false;
                    }

                    for (k = 0; k < edgeCount.length; k++) {
                        if (he.parentEdge === solid.getEdgesList().get(k)) {
                            edgeCount[k]!++;
                            break;
                        }
                    }
                } while (he !== heStart);
            }
        }

        for (i = 0; i < edgeCount.length; i++) {
            if (edgeCount[i] !== 2) {
                Logger.reportMessage(
                    solid,
                    VSDK.WARNING,
                    "validateTopologicalIntegrity",
                    "Edges with different halfedges than 2!",
                );
                return false;
            }
        }

        return true;
    }

    /**
    Rebuilds the vertex-to-emanating-halfedge links required by the vertex node
    definition of [MANT1988].10.2.1 and used throughout the Euler-operator
    programs of chapter [MANT1988].11.
    */
    public static remakeEmanatingHalfedgesReferences(solid: PolyhedralBoundedSolid): void {
        let i: number;
        let j: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            solid.getVerticesList().get(i)!.emanatingHalfEdge = null;
        }

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            for (j = 0; j < face.boundariesList.size(); j++) {
                let he: _PolyhedralBoundedSolidHalfEdge | null;

                const loop = face.boundariesList.get(j)!;
                he = loop.boundaryStartHalfEdge;
                if (he === null) {
                    continue;
                }
                const heStart = he;
                do {
                    he!.startingVertex.emanatingHalfEdge = he;
                    he = he!.next();
                    if (he === null) {
                        break;
                    }
                } while (he !== heStart);
            }
        }

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            if (solid.getVerticesList().get(i)!.emanatingHalfEdge === null) {
                solid.getVerticesList().locateWindowAtElem(solid.getVerticesList().get(i)!);
                solid.getVerticesList().removeElemAtWindow();
                i--;
            }
        }
    }
}
