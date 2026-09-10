import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import type { ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidValidationStrategy } from "./_PolyhedralBoundedSolidValidationStrategy.js";
/** Checks the non-self-intersection requirement of [MANT1988].15.2, criterion 3. */ export class _GeometricStrictFaceIntersectionsStrategy implements _PolyhedralBoundedSolidValidationStrategy<PolyhedralBoundedSolid> {
    public validate(s: PolyhedralBoundedSolid, c: ToleranceContext, m: string[]): boolean {
        const faces = s.getPolygonsList();
        for (let i = 0; i < faces.length; i++)
            for (let j = i + 1; j < faces.length; j++) {
                const a = faces[i]!,
                    b = faces[j]!,
                    plane = b.getContainingPlane();
                if (plane === null) continue;
                for (const loop of a.boundariesList)
                    for (let k = 0; k < loop.halfEdgesList.size(); k++) {
                        const p = loop.halfEdgesList.get(k)!.startingVertex.position;
                        if (
                            Math.abs(plane.pointDistance(p)) <= c.bigEpsilon() &&
                            b.testPointInside(p, c.bigEpsilon(), plane) === 1
                        ) {
                            m.push(`Faces [${a.id}] and [${b.id}] intersect improperly.`);
                            return false;
                        }
                    }
            }
        return true;
    }
}
