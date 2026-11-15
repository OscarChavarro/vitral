package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RayInstanceStep;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SimpleBody;

public class CsgBody extends SimpleBody {
    private CsgBodyEvaluator evaluator;

    private double membershipTolerance;

    private final ArrayList<CsgTree> nodes = new ArrayList<CsgTree>();

    private final ArrayList<CsgBodyOperand> operands = new ArrayList<CsgBodyOperand>();

    private final ArrayList<SimpleBody> leaves = new ArrayList<SimpleBody>();

    private boolean findBoundary(Ray localRay, boolean firstVisible, double t, RayBoundary result) {
    if (isLocallyMissed(localRay)) return false;
    final RayCrossingQuery query = new RayCrossingQuery(0.0, Double.POSITIVE_INFINITY);
    long selected = -1;
    double bestDistance = Double.POSITIVE_INFINITY;
    if (evaluator == CsgBodyEvaluator.ROTH) {
        CsgRayClassification classification = new CsgRayClassification();
        if (CsgRothRayClassifier.classify(getExpression(), localRay, query, classification) !=
            RaySolidQueryStatus.Supported) return false;
        final ArrayList<RayBoundary> boundaries = classification.getSolid().boundaries;
        for (long i = 0; i < boundaries.size(); i++) {
            final double candidate = boundaries.get((int)(i)).t;
            if (firstVisible) { if (candidate > VSDK.EPSILON) { selected = i; break; } }
            else if (Math.abs(candidate - t) < bestDistance) { bestDistance = Math.abs(candidate - t); selected = i; }
        }
        if (selected < 0) return false;
        result.assign(boundaries.get((int)(selected)));
        return true;
    }
    CsgRaySurfaceEvents events = new CsgRaySurfaceEvents();
    if (CsgCrossingMembershipFilter.select(getExpression(), localRay, query, membershipTolerance, events) !=
        RaySolidQueryStatus.Supported) return false;
    for (long i = 0; i < events.size(); i++) {
        final double candidate = events.getT(i);
        if (firstVisible) { if (candidate > VSDK.EPSILON) { selected = i; break; } }
        else if (Math.abs(candidate - t) < bestDistance) { bestDistance = Math.abs(candidate - t); selected = i; }
    }
    if (selected < 0) return false;
    final CsgRayEventReference reference = events.get(selected);
    final CsgRayLeaf leaf = reference != null ? events.getLeaves().get(reference.leafIndex) : null;
    if (leaf == null || reference.boundaryIndex < 0 ||
        reference.boundaryIndex >= leaf.getClassification().boundaries.size()) return false;
    result.assign(leaf.getClassification().boundaries.get((int)(reference.boundaryIndex)));
    return true;
}

    private boolean toLocalRay(Ray ray, Ray[] result, Matrix4x4d[] forward, Matrix4x4d[] inverse) {
    if (queryAffinePlacement(forward, inverse) != AffinePlacementStatus.Valid) return false;
    final Vector3Dd origin = inverse[0].multiply(ray.getOrigin());
    final Vector3Dd direction = inverse[0].withoutTranslation().multiply(ray.getDirection());
    if (!Double.isFinite(origin.x()) || !Double.isFinite(origin.y()) ||
        !Double.isFinite(origin.z()) || !Double.isFinite(direction.x()) ||
        !Double.isFinite(direction.y()) || !Double.isFinite(direction.z()) ||
        (direction.x() == 0 && direction.y() == 0 && direction.z() == 0)) return false;
    result[0] = Ray.fromAffine(origin, direction, ray.getT());
    return true;
}

    private boolean isLocallyMissed(Ray localRay) {
    GeometryBounds bounds = new GeometryBounds();
    CsgSolidBounds.query(getExpression(), bounds);
    if (bounds.getKind() == GeometryBounds.Kind.Empty) return true;
    if (bounds.getKind() != GeometryBounds.Kind.Finite) return false;
    double low = 0, high = Double.POSITIVE_INFINITY;
    for (int axis = 0; axis < 3; axis++) {
        final double o = axis == 0 ? localRay.getOrigin().x() : axis == 1 ? localRay.getOrigin().y() : localRay.getOrigin().z();
        final double d = axis == 0 ? localRay.getDirection().x() : axis == 1 ? localRay.getDirection().y() : localRay.getDirection().z();
        final double minimum = axis == 0 ? bounds.getMinimum().x() : axis == 1 ? bounds.getMinimum().y() : bounds.getMinimum().z();
        final double maximum = axis == 0 ? bounds.getMaximum().x() : axis == 1 ? bounds.getMaximum().y() : bounds.getMaximum().z();
        if (d == 0) {
            if (o < minimum || o > maximum) return true;
            continue;
        }
        double a = (minimum - o) / d, b = (maximum - o) / d;
        if (a > b) { final double swap = a; a = b; b = swap; }
        if (a > low) low = a;
        if (b < high) high = b;
        if (low > high) return true;
    }
    return false;
}

    private SimpleBody findLeaf(RayBoundary boundary) {
    RayInstanceStep step = new RayInstanceStep();
    if (!boundary.getInstance(0, step)) return null;
    for (long i = 0; i < leaves.size(); i++) {
        if ((Entity)(leaves.get((int)(i))) == step.instance) return leaves.get((int)(i));
    }
    return null;
}

    public CsgBody(CsgOperation rootOperation, CsgBodyEvaluator value) {
        this.evaluator = value;
        this.membershipTolerance = 1.0e-6;
    nodes.add(new CsgTree(rootOperation));
}

    public CsgBodyEvaluator getEvaluator() { return evaluator; }

    // World tolerance of the crossing membership tests; ignored by Roth.
    public double getMembershipTolerance() { return membershipTolerance; }

    public boolean setMembershipTolerance(double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0) return false;
    membershipTolerance = tolerance;
    markModified();
    return true;
}

    // Appends an operation node under an existing node and returns its index,
    // or -1 when the parent is unknown.
    public long addNode(long parentNode, CsgOperation operation) {
    if (parentNode < 0 || parentNode >= nodes.size()) return -1;
    CsgTree node = new CsgTree(operation);
    if (!nodes.add(node)) {  return -1; }
    if (!nodes.get((int)(parentNode)).addOperand(node)) {
        nodes.remove(nodes.size() - 1);
        
        return -1;
    }
    markModified();
    return nodes.size() - 1;
}

    // Takes ownership of the leaf, also on failure.
    public boolean addLeaf(long parentNode, SimpleBody leaf) {
    if (leaf == null) return false;
    if (parentNode < 0 || parentNode >= nodes.size() || !leaves.add(leaf)) {  return false; }
    CsgBodyOperand operand = new CsgBodyOperand(leaf);
    if (!operands.add(operand)) {  return false; }
    if (!nodes.get((int)(parentNode)).addOperand(operand)) return false;
    markModified();
    return true;
}

    public long getNodeCount() { return nodes.size(); }

    public long getLeafCount() { return leaves.size(); }

    public SimpleBody getLeaf(long index) { return index < 0 || index >= leaves.size() ? null : leaves.get((int)(index)); }

    public CsgExpression getExpression() { return nodes.get((int)(0)); }

    @Override
    public boolean doIntersectionFirstHit(Ray inOutRay, RayHit outHit) {
    Ray[] localRay = {new Ray()};
    Matrix4x4d[] forward = {null};
    Matrix4x4d[] inverse = {null};
    RayBoundary boundary = new RayBoundary();
    if (!toLocalRay(inOutRay, localRay, forward, inverse) ||
        !findBoundary(localRay[0], true, 0.0, boundary)) return false;
    final double t = boundary.t;
    if (outHit != null) {
        if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
            final Ray worldRay = inOutRay.withT(t);
            outHit.setRay(worldRay);
            if (outHit.needsAnySurfaceData()) doExtraInformation(worldRay, t, outHit);
        }
        else outHit.setHitDistance(t);
    }
    return true;
}

    @Override
    public void doExtraInformation(Ray inRay, double inT, RayHit outData) {
    Ray[] localRay = {new Ray()};
    Matrix4x4d[] forward = {null};
    Matrix4x4d[] inverse = {null};
    RayBoundary boundary = new RayBoundary();
    if (!toLocalRay(inRay, localRay, forward, inverse) ||
        !findBoundary(localRay[0], false, inT, boundary)) return;
    final SimpleBody leaf = findLeaf(boundary);
    if (leaf == null) return;
    // Leaf bodies read the ray stored in the hit, so it carries the local ray.
    final Ray leafRay = localRay[0].withT(inT);
    outData.setRay(leafRay);
    leaf.doExtraInformation(leafRay, inT, outData);
    if (outData.needsNormal()) {
        Vector3Dd n = outData.normal;
        if (boundary.normalReversed) n = new Vector3Dd(-n.x(), -n.y(), -n.z());
        outData.normal = new Vector3Dd(
            inverse[0].get(0,0)*n.x()+inverse[0].get(1,0)*n.y()+inverse[0].get(2,0)*n.z(),
            inverse[0].get(0,1)*n.x()+inverse[0].get(1,1)*n.y()+inverse[0].get(2,1)*n.z(),
            inverse[0].get(0,2)*n.x()+inverse[0].get(1,2)*n.y()+inverse[0].get(2,2)*n.z()).normalized();
    }
    if (outData.needsPoint()) outData.point = forward[0].multiply(outData.point);
    if (outData.needsTangent()) outData.tangent = forward[0].withoutTranslation().multiply(outData.tangent).normalized();
    if (outData.material == null) outData.material = getMaterial();
    outData.setRay(inRay.withT(inT));
}

    @Override
    public RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result) {
    result.clear();
    Ray[] localRay = {new Ray()};
    Matrix4x4d[] forward = {null};
    Matrix4x4d[] inverse = {null};
    if (!toLocalRay(ray, localRay, forward, inverse)) return RaySolidQueryStatus.Indeterminate;
    CsgRayClassification classification = new CsgRayClassification();
    final RaySolidQueryStatus status =
        CsgRothRayClassifier.classify(getExpression(), localRay[0], query, classification);
    if (status != RaySolidQueryStatus.Supported) return status;
    result.assign(classification.getSolid());
    final RayInstanceStep step = new RayInstanceStep(this, forward[0], inverse[0]);
    for (long i = 0; i < result.boundaries.size(); i++) {
        if (!result.boundaries.get((int)(i)).appendInstance(step)) {
            result.clear();
            return RaySolidQueryStatus.Indeterminate;
        }
    }
    return status;
}

    @Override
    public void getWorldBounds(GeometryBounds result) { getSolidWorldBounds(result); }

    @Override
    public void getSolidWorldBounds(GeometryBounds result) {
    GeometryBounds local = new GeometryBounds();
    CsgSolidBounds.query(getExpression(), local);
    Matrix4x4d[] forward = {null};
    Matrix4x4d[] inverse = {null};
    if (queryAffinePlacement(forward, inverse) != AffinePlacementStatus.Valid) { result.setIndeterminate(); return; }
    result.assign(local.transformed(forward[0]));
}

    @Override
    public GeometryContainment classifyPointCertified(Vector3Dd point, double distanceTolerance) {
    Matrix4x4d[] forward = {null};
    Matrix4x4d[] inverse = {null};
    final AffinePlacementStatus placement = queryAffinePlacement(forward, inverse);
    if (placement == AffinePlacementStatus.Invalid) return GeometryContainment.InvalidInput;
    if (placement == AffinePlacementStatus.Unsupported) return GeometryContainment.Unsupported;
    if (placement == AffinePlacementStatus.Indeterminate) return GeometryContainment.Indeterminate;
    CsgPlacedExpression placed = new CsgPlacedExpression(getExpression());
    if (placed.setAffinePlacement(forward[0], inverse[0]) != AffinePlacementStatus.Valid)
        return GeometryContainment.Indeterminate;
    return CsgCrossingMembershipFilter.classifyOperand(placed, point, distanceTolerance);
}
}

