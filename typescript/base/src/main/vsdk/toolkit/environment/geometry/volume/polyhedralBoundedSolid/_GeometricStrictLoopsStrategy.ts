import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import type { ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidValidationStrategy } from "./_PolyhedralBoundedSolidValidationStrategy.js";
/** Enforces non-degenerate, planar loop boundaries required by [MANT1988].10.2.1 and chapter 13 predicates. */ export class _GeometricStrictLoopsStrategy implements _PolyhedralBoundedSolidValidationStrategy<PolyhedralBoundedSolid> {
    public validate(s: PolyhedralBoundedSolid, c: ToleranceContext, m: string[]): boolean {
        for (const face of s.getPolygonsList()) {
            if (face.getContainingPlane() === null) {
                m.push(`Face [${face.id}] has no containing plane.`);
                return false;
            }
            for (const loop of face.boundariesList) {
                if (loop.halfEdgesList.size() < 3) {
                    m.push(`Face [${face.id}] has a degenerate loop.`);
                    return false;
                }
                for (let i = 0; i < loop.halfEdgesList.size(); i++)
                    if (
                        loop.halfEdgesList
                            .get(i)!
                            .startingVertex.position.subtract(
                                loop.halfEdgesList.get((i + 1) % loop.halfEdgesList.size())!.startingVertex.position,
                            )
                            .length() <= c.bigEpsilon()
                    ) {
                        m.push(`Face [${face.id}] has a zero-length loop edge.`);
                        return false;
                    }
            }
        }
        return true;
    }
}
