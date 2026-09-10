import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import type { ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidValidationStrategy } from "./_PolyhedralBoundedSolidValidationStrategy.js";
/** Applies the half-edge incidence and circular-cycle checks of [MANT1988].10.2.1–10.2.2. */
export class _TopologicalIntegrityStrategy implements _PolyhedralBoundedSolidValidationStrategy<PolyhedralBoundedSolid> {
    public validate(solid: PolyhedralBoundedSolid, _context: ToleranceContext, message: string[]): boolean {
        for (const face of solid.getPolygonsList())
            for (const loop of face.boundariesList) {
                if (loop.halfEdgesList.size() === 0 || loop.boundaryStartHalfEdge === null) {
                    message.push("Topological integrity test failed: empty loop.");
                    return false;
                }
                for (let i = 0; i < loop.halfEdgesList.size(); i++) {
                    const edge = loop.halfEdgesList.get(i)!;
                    if (edge.parentLoop !== loop || edge.next() === null || edge.previous() === null) {
                        message.push("Topological integrity test failed: broken half-edge cycle.");
                        return false;
                    }
                    if (
                        edge.parentEdge !== null &&
                        edge.parentEdge.leftHalf !== edge &&
                        edge.parentEdge.rightHalf !== edge
                    ) {
                        message.push("Topological integrity test failed: edge incidence mismatch.");
                        return false;
                    }
                }
            }
        return true;
    }
}
