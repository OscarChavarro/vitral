import { Double } from "../../../../../java/lang/Double.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { VSDK } from "../../../common/VSDK.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { GeometryBounds } from "../../geometry/element/GeometryBounds.js";
import { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import { Ray } from "../../geometry/element/Ray.js";
import { RayBoundary } from "../../geometry/element/RayBoundary.js";
import { RayCrossingQuery } from "../../geometry/element/RayCrossingQuery.js";
import { RayHit } from "../../geometry/element/RayHit.js";
import { RayInstanceStep } from "../../geometry/element/RayInstanceStep.js";
import type { RaySolidClassification } from "../../geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "../../geometry/element/RaySolidQueryStatus.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import { SimpleBody } from "../SimpleBody.js";
import { CsgBodyEvaluator } from "./CsgBodyEvaluator.js";
import { CsgBodyOperand } from "./CsgBodyOperand.js";
import { CsgCrossingMembershipFilter } from "./CsgCrossingMembershipFilter.js";
import type { CsgExpression } from "./CsgExpression.js";
import type { CsgOperation } from "./CsgOperation.js";
import { CsgPlacedExpression } from "./CsgPlacedExpression.js";
import { CsgRayClassification } from "./CsgRayClassification.js";
import { CsgRaySurfaceEvents } from "./CsgRaySurfaceEvents.js";
import { CsgRothRayClassifier } from "./CsgRothRayClassifier.js";
import { CsgSolidBounds } from "./CsgSolidBounds.js";
import { CsgTree } from "./CsgTree.js";

// Scene body whose solid is a CSG expression over leaf bodies it owns. Node 0
// is the root; nested nodes are identity-placed CsgTree operands, so every leaf
// keeps its own placement in the coordinates of this body, and the body's
// placement maps the whole solid to its parent. Rays are evaluated with the
// selected shared evaluator; the visible hit is the first boundary (Roth) or
// selected surface event (crossing membership) beyond VSDK.EPSILON. Surface
// detail comes from the owning leaf body, with the normal turned outward of the
// combined solid, and a leaf without material shows the material of this body.
// A failed or uncertain evaluation reports no hit. Leaves must not be edited
// after insertion; queries keep no shared scratch state, so concurrent renders
// are safe.
export class CsgBody extends SimpleBody {
    private readonly evaluator: CsgBodyEvaluator;
    private membershipTolerance: number;
    private readonly nodes: ArrayList<CsgTree>;
    private readonly operands: ArrayList<CsgBodyOperand>;
    private readonly leaves: ArrayList<SimpleBody>;

    public constructor(rootOperation: CsgOperation, value: CsgBodyEvaluator) {
        super();
        this.evaluator = value;
        this.membershipTolerance = 1.0e-6;
        this.nodes = new ArrayList<CsgTree>(4);
        this.operands = new ArrayList<CsgBodyOperand>(4);
        this.leaves = new ArrayList<SimpleBody>(4);
        this.nodes.add(new CsgTree(rootOperation));
    }

    public getEvaluator(): CsgBodyEvaluator {
        return this.evaluator;
    }

    // World tolerance of the crossing membership tests; ignored by Roth.
    public getMembershipTolerance(): number {
        return this.membershipTolerance;
    }

    public setMembershipTolerance(tolerance: number): boolean {
        if (!Double.isFinite(tolerance) || tolerance < 0) return false;
        this.membershipTolerance = tolerance;
        this.markModified();
        return true;
    }

    // Appends an operation node under an existing node and returns its index,
    // or -1 when the parent is unknown.
    public addNode(parentNode: number, operation: CsgOperation): number {
        if (parentNode < 0 || parentNode >= this.nodes.size()) return -1;
        const node = new CsgTree(operation);
        if (!this.nodes.add(node)) return -1;
        if (!this.nodes.get(parentNode).addOperand(node)) {
            this.nodes.remove(this.nodes.size() - 1);
            return -1;
        }
        this.markModified();
        return this.nodes.size() - 1;
    }

    // Takes ownership of the leaf, also on failure.
    public addLeaf(parentNode: number, leaf: SimpleBody | null): boolean {
        if (leaf === null) return false;
        if (parentNode < 0 || parentNode >= this.nodes.size() || !this.leaves.add(leaf)) return false;
        const operand = new CsgBodyOperand(leaf);
        if (!this.operands.add(operand)) return false;
        if (!this.nodes.get(parentNode).addOperand(operand)) return false;
        this.markModified();
        return true;
    }

    public getNodeCount(): number {
        return this.nodes.size();
    }

    public getLeafCount(): number {
        return this.leaves.size();
    }

    public getLeaf(index: number): SimpleBody | null {
        return index < 0 || index >= this.leaves.size() ? null : this.leaves.get(index);
    }

    public getExpression(): CsgExpression {
        return this.nodes.get(0);
    }

    private toLocalRay(ray: Ray, result: Ray[], forward: Matrix4x4d[], inverse: Matrix4x4d[]): boolean {
        if (this.queryAffinePlacement(forward, inverse) !== AffinePlacementStatus.Valid) return false;
        const origin = inverse[0]!.multiply(ray.getOrigin());
        const direction = inverse[0]!.withoutTranslation().multiply(ray.getDirection());
        if (
            !Double.isFinite(origin.x()) ||
            !Double.isFinite(origin.y()) ||
            !Double.isFinite(origin.z()) ||
            !Double.isFinite(direction.x()) ||
            !Double.isFinite(direction.y()) ||
            !Double.isFinite(direction.z()) ||
            (direction.x() === 0 && direction.y() === 0 && direction.z() === 0)
        )
            return false;
        result[0] = Ray.fromAffine(origin, direction, ray.getT());
        return true;
    }

    // Slab test against the conservative solid bounds; unknown bounds never cull.
    private isLocallyMissed(localRay: Ray): boolean {
        const bounds = new GeometryBounds();
        CsgSolidBounds.query(this.getExpression(), bounds);
        if (bounds.getKind() === GeometryBounds.Kind.Empty) return true;
        if (bounds.getKind() !== GeometryBounds.Kind.Finite) return false;
        let low = 0;
        let high = Double.POSITIVE_INFINITY;
        for (let axis = 0; axis < 3; axis++) {
            const o =
                axis === 0
                    ? localRay.getOrigin().x()
                    : axis === 1
                      ? localRay.getOrigin().y()
                      : localRay.getOrigin().z();
            const d =
                axis === 0
                    ? localRay.getDirection().x()
                    : axis === 1
                      ? localRay.getDirection().y()
                      : localRay.getDirection().z();
            const minimum =
                axis === 0 ? bounds.getMinimum().x() : axis === 1 ? bounds.getMinimum().y() : bounds.getMinimum().z();
            const maximum =
                axis === 0 ? bounds.getMaximum().x() : axis === 1 ? bounds.getMaximum().y() : bounds.getMaximum().z();
            if (d === 0) {
                if (o < minimum || o > maximum) return true;
                continue;
            }
            let a = (minimum - o) / d;
            let b = (maximum - o) / d;
            if (a > b) {
                const swap = a;
                a = b;
                b = swap;
            }
            if (a > low) low = a;
            if (b < high) high = b;
            if (low > high) return true;
        }
        return false;
    }

    // With firstVisible, the first boundary beyond VSDK.EPSILON; otherwise the
    // boundary nearest to t. Boundaries are copied with their orientation.
    private findBoundary(localRay: Ray, firstVisible: boolean, t: number, result: RayBoundary): boolean {
        if (this.isLocallyMissed(localRay)) return false;
        const query = new RayCrossingQuery(0.0, Double.POSITIVE_INFINITY);
        let selected = -1;
        let bestDistance = Double.POSITIVE_INFINITY;
        if (this.evaluator === CsgBodyEvaluator.ROTH) {
            const classification = new CsgRayClassification();
            if (
                CsgRothRayClassifier.classify(this.getExpression(), localRay, query, classification) !==
                RaySolidQueryStatus.Supported
            )
                return false;
            const boundaries = classification.getSolid().boundaries;
            for (let i = 0; i < boundaries.size(); i++) {
                const candidate = boundaries.get(i).t;
                if (firstVisible) {
                    if (candidate > VSDK.EPSILON) {
                        selected = i;
                        break;
                    }
                } else if (Math.abs(candidate - t) < bestDistance) {
                    bestDistance = Math.abs(candidate - t);
                    selected = i;
                }
            }
            if (selected < 0) return false;
            result.assign(boundaries.get(selected));
            return true;
        }
        const events = new CsgRaySurfaceEvents();
        if (
            CsgCrossingMembershipFilter.select(
                this.getExpression(),
                localRay,
                query,
                this.membershipTolerance,
                events,
            ) !== RaySolidQueryStatus.Supported
        )
            return false;
        for (let i = 0; i < events.size(); i++) {
            const candidate = events.getT(i);
            if (firstVisible) {
                if (candidate > VSDK.EPSILON) {
                    selected = i;
                    break;
                }
            } else if (Math.abs(candidate - t) < bestDistance) {
                bestDistance = Math.abs(candidate - t);
                selected = i;
            }
        }
        if (selected < 0) return false;
        const reference = events.get(selected);
        const leaf = reference !== null ? events.getLeaves().get(reference.leafIndex) : null;
        if (
            leaf === null ||
            reference!.boundaryIndex < 0 ||
            reference!.boundaryIndex >= leaf.getClassification().boundaries.size()
        )
            return false;
        result.assign(leaf.getClassification().boundaries.get(reference!.boundaryIndex));
        return true;
    }

    // The leaf body is the innermost instance of the boundary provenance.
    private findLeaf(boundary: RayBoundary): SimpleBody | null {
        const step = new RayInstanceStep();
        if (!boundary.getInstance(0, step)) return null;
        for (let i = 0; i < this.leaves.size(); i++) {
            if (this.leaves.get(i) === step.instance) return this.leaves.get(i);
        }
        return null;
    }

    public override doIntersectionFirstHit(inRay: Ray): Ray | null;
    public override doIntersectionFirstHit(inOutRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inOutRay: Ray, outHitArgument?: RayHit | null): boolean | Ray | null {
        if (arguments.length === 1) {
            const firstHit = new RayHit();
            if (!this.doIntersectionFirstHit(inOutRay, firstHit) || firstHit.getRay() === null) {
                return null;
            }
            return new Ray(firstHit.getRay()!);
        }
        const outHit: RayHit | null = outHitArgument ?? null;
        const localRay: Ray[] = [new Ray()];
        const forward: Matrix4x4d[] = [new Matrix4x4d()];
        const inverse: Matrix4x4d[] = [new Matrix4x4d()];
        const boundary = new RayBoundary();
        if (
            !this.toLocalRay(inOutRay, localRay, forward, inverse) ||
            !this.findBoundary(localRay[0]!, true, 0.0, boundary)
        )
            return false;
        const t = boundary.t;
        if (outHit !== null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                const worldRay = inOutRay.withT(t);
                outHit.setRay(worldRay);
                if (outHit.needsAnySurfaceData()) this.doExtraInformation(worldRay, t, outHit);
            } else outHit.setHitDistance(t);
        }
        return true;
    }

    public override doExtraInformation(inRay: Ray, inT: number, outData: RayHit | null): void {
        const localRay: Ray[] = [new Ray()];
        const forward: Matrix4x4d[] = [new Matrix4x4d()];
        const inverse: Matrix4x4d[] = [new Matrix4x4d()];
        const boundary = new RayBoundary();
        if (
            !this.toLocalRay(inRay, localRay, forward, inverse) ||
            !this.findBoundary(localRay[0]!, false, inT, boundary)
        )
            return;
        const leaf = this.findLeaf(boundary);
        if (leaf === null) return;
        // Leaf bodies read the ray stored in the hit, so it carries the local ray.
        const leafRay = localRay[0]!.withT(inT);
        outData!.setRay(leafRay);
        leaf.doExtraInformation(leafRay, inT, outData);
        if (outData!.needsNormal()) {
            let n = outData!.normal;
            if (boundary.normalReversed) n = new Vector3Dd(-n.x(), -n.y(), -n.z());
            const m = inverse[0]!;
            outData!.normal = new Vector3Dd(
                m.get(0, 0) * n.x() + m.get(1, 0) * n.y() + m.get(2, 0) * n.z(),
                m.get(0, 1) * n.x() + m.get(1, 1) * n.y() + m.get(2, 1) * n.z(),
                m.get(0, 2) * n.x() + m.get(1, 2) * n.y() + m.get(2, 2) * n.z(),
            ).normalized();
        }
        if (outData!.needsPoint()) outData!.point = forward[0]!.multiply(outData!.point);
        if (outData!.needsTangent())
            outData!.tangent = forward[0]!.withoutTranslation().multiply(outData!.tangent).normalized();
        if (outData!.material === null) outData!.material = this.getMaterial();
        outData!.setRay(inRay.withT(inT));
    }

    public override classifyRay(
        ray: Ray,
        query: RayCrossingQuery,
        result: RaySolidClassification,
    ): RaySolidQueryStatus {
        result.clear();
        const localRay: Ray[] = [new Ray()];
        const forward: Matrix4x4d[] = [new Matrix4x4d()];
        const inverse: Matrix4x4d[] = [new Matrix4x4d()];
        if (!this.toLocalRay(ray, localRay, forward, inverse)) return RaySolidQueryStatus.Indeterminate;
        const classification = new CsgRayClassification();
        const status = CsgRothRayClassifier.classify(this.getExpression(), localRay[0]!, query, classification);
        if (status !== RaySolidQueryStatus.Supported) return status;
        result.assign(classification.getSolid());
        const step = new RayInstanceStep(this, forward[0]!, inverse[0]!);
        for (let i = 0; i < result.boundaries.size(); i++) {
            if (!result.boundaries.get(i).appendInstance(step)) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
        }
        return status;
    }

    public override getSolidWorldBounds(result: GeometryBounds): void {
        const local = new GeometryBounds();
        CsgSolidBounds.query(this.getExpression(), local);
        const forward: Matrix4x4d[] = [new Matrix4x4d()];
        const inverse: Matrix4x4d[] = [new Matrix4x4d()];
        if (this.queryAffinePlacement(forward, inverse) !== AffinePlacementStatus.Valid) {
            result.setIndeterminate();
            return;
        }
        result.assign(local.transformed(forward[0]!));
    }

    public override getWorldBounds(result: GeometryBounds): void {
        this.getSolidWorldBounds(result);
    }

    public override classifyPointCertified(point: Vector3Dd, distanceTolerance: number): GeometryContainment {
        const forward: Matrix4x4d[] = [new Matrix4x4d()];
        const inverse: Matrix4x4d[] = [new Matrix4x4d()];
        const placement = this.queryAffinePlacement(forward, inverse);
        if (placement === AffinePlacementStatus.Invalid) return GeometryContainment.InvalidInput;
        if (placement === AffinePlacementStatus.Unsupported) return GeometryContainment.Unsupported;
        if (placement === AffinePlacementStatus.Indeterminate) return GeometryContainment.Indeterminate;
        const placed = new CsgPlacedExpression(this.getExpression());
        if (placed.setAffinePlacement(forward[0]!, inverse[0]!) !== AffinePlacementStatus.Valid)
            return GeometryContainment.Indeterminate;
        return CsgCrossingMembershipFilter.classifyOperand(placed, point, distanceTolerance);
    }
}
