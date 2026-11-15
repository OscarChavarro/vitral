#include <math.h>
#include "java/lang/Double.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayBoundary.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RayInstanceStep.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBody.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBodyOperand.h"
#include "vsdk/toolkit/environment/scene/csg/CsgCrossingMembershipFilter.h"
#include "vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRayClassification.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRaySurfaceEvents.h"
#include "vsdk/toolkit/environment/scene/csg/CsgRothRayClassifier.h"
#include "vsdk/toolkit/environment/scene/csg/CsgSolidBounds.h"
#include "vsdk/toolkit/environment/scene/csg/CsgTree.h"

CsgBody::CsgBody(CsgOperation::Value rootOperation, CsgBodyEvaluator::Value value)
    : evaluator(value), membershipTolerance(1.0e-6), nodes(4), operands(4), leaves(4)
{
    nodes.add(new CsgTree(rootOperation));
}

CsgBody::~CsgBody()
{ long int i;
    for (i = 0; i < nodes.size(); i++) delete nodes[i];
    for (i = 0; i < operands.size(); i++) delete operands[i];
    for (i = 0; i < leaves.size(); i++) delete leaves[i];
}

CsgBodyEvaluator::Value CsgBody::getEvaluator() const { return evaluator; }
double CsgBody::getMembershipTolerance() const { return membershipTolerance; }

BOOLEAN CsgBody::setMembershipTolerance(double tolerance)
{
    if (!Double::isFinite(tolerance) || tolerance < 0) return FALSE;
    membershipTolerance = tolerance;
    markModified();
    return TRUE;
}

long int CsgBody::addNode(long int parentNode, CsgOperation::Value operation)
{
    if (parentNode < 0 || parentNode >= nodes.size()) return -1;
    CsgTree* node = new CsgTree(operation);
    if (!nodes.add(node)) { delete node; return -1; }
    if (!nodes[parentNode]->addOperand(*node)) {
        nodes.remove(nodes.size() - 1);
        delete node;
        return -1;
    }
    markModified();
    return nodes.size() - 1;
}

BOOLEAN CsgBody::addLeaf(long int parentNode, SimpleBody* leaf)
{
    if (leaf == NULL) return FALSE;
    if (parentNode < 0 || parentNode >= nodes.size() || !leaves.add(leaf)) { delete leaf; return FALSE; }
    CsgBodyOperand* operand = new CsgBodyOperand(*leaf);
    if (!operands.add(operand)) { delete operand; return FALSE; }
    if (!nodes[parentNode]->addOperand(*operand)) return FALSE;
    markModified();
    return TRUE;
}

long int CsgBody::getNodeCount() const { return nodes.size(); }
long int CsgBody::getLeafCount() const { return leaves.size(); }
const SimpleBody* CsgBody::getLeaf(long int index) const
{ return index < 0 || index >= leaves.size() ? (const SimpleBody*)NULL : leaves[index]; }
const CsgExpression& CsgBody::getExpression() const { return *nodes[0]; }

BOOLEAN CsgBody::toLocalRay(const Ray& ray, Ray& result, Matrix4x4d& forward, Matrix4x4d& inverse) const
{
    if (queryAffinePlacement(forward, inverse) != AffinePlacementStatus::Valid) return FALSE;
    const Vector3Dd origin = inverse.multiply(ray.getOrigin());
    const Vector3Dd direction = inverse.withoutTranslation().multiply(ray.getDirection());
    if (!Double::isFinite(origin.x()) || !Double::isFinite(origin.y()) ||
        !Double::isFinite(origin.z()) || !Double::isFinite(direction.x()) ||
        !Double::isFinite(direction.y()) || !Double::isFinite(direction.z()) ||
        (direction.x() == 0 && direction.y() == 0 && direction.z() == 0)) return FALSE;
    result = Ray::fromAffine(origin, direction, ray.getT());
    return TRUE;
}

// Slab test against the conservative solid bounds; unknown bounds never cull.
BOOLEAN CsgBody::isLocallyMissed(const Ray& localRay) const
{
    GeometryBounds bounds;
    CsgSolidBounds::query(getExpression(), bounds);
    if (bounds.getKind() == GeometryBounds::Kind::Empty) return TRUE;
    if (bounds.getKind() != GeometryBounds::Kind::Finite) return FALSE;
    double low = 0, high = Double::POSITIVE_INFINITY;
    for (int axis = 0; axis < 3; axis++) {
        const double o = axis == 0 ? localRay.getOrigin().x() : axis == 1 ? localRay.getOrigin().y() : localRay.getOrigin().z();
        const double d = axis == 0 ? localRay.getDirection().x() : axis == 1 ? localRay.getDirection().y() : localRay.getDirection().z();
        const double minimum = axis == 0 ? bounds.getMinimum().x() : axis == 1 ? bounds.getMinimum().y() : bounds.getMinimum().z();
        const double maximum = axis == 0 ? bounds.getMaximum().x() : axis == 1 ? bounds.getMaximum().y() : bounds.getMaximum().z();
        if (d == 0) {
            if (o < minimum || o > maximum) return TRUE;
            continue;
        }
        double a = (minimum - o) / d, b = (maximum - o) / d;
        if (a > b) { const double swap = a; a = b; b = swap; }
        if (a > low) low = a;
        if (b < high) high = b;
        if (low > high) return TRUE;
    }
    return FALSE;
}

// With firstVisible, the first boundary beyond VSDK::EPSILON; otherwise the
// boundary nearest to t. Boundaries are copied with their orientation.
BOOLEAN CsgBody::findBoundary(const Ray& localRay, BOOLEAN firstVisible, double t, RayBoundary& result) const
{
    if (isLocallyMissed(localRay)) return FALSE;
    const RayCrossingQuery query(0.0, Double::POSITIVE_INFINITY);
    long int selected = -1;
    double bestDistance = Double::POSITIVE_INFINITY;
    if (evaluator == CsgBodyEvaluator::ROTH) {
        CsgRayClassification classification;
        if (CsgRothRayClassifier::classify(getExpression(), localRay, query, classification) !=
            RaySolidQueryStatus::Supported) return FALSE;
        const ArrayList<RayBoundary>& boundaries = classification.getSolid().boundaries;
        for (long int i = 0; i < boundaries.size(); i++) {
            const double candidate = boundaries[i].t;
            if (firstVisible) { if (candidate > VSDK::EPSILON) { selected = i; break; } }
            else if (fabs(candidate - t) < bestDistance) { bestDistance = fabs(candidate - t); selected = i; }
        }
        if (selected < 0) return FALSE;
        result = boundaries[selected];
        return TRUE;
    }
    CsgRaySurfaceEvents events;
    if (CsgCrossingMembershipFilter::select(getExpression(), localRay, query, membershipTolerance, events) !=
        RaySolidQueryStatus::Supported) return FALSE;
    for (long int i = 0; i < events.size(); i++) {
        const double candidate = events.getT(i);
        if (firstVisible) { if (candidate > VSDK::EPSILON) { selected = i; break; } }
        else if (fabs(candidate - t) < bestDistance) { bestDistance = fabs(candidate - t); selected = i; }
    }
    if (selected < 0) return FALSE;
    const CsgRayEventReference* reference = events.get(selected);
    const CsgRayLeaf* leaf = reference ? events.getLeaves().get(reference->leafIndex) : (const CsgRayLeaf*)NULL;
    if (!leaf || reference->boundaryIndex < 0 ||
        reference->boundaryIndex >= leaf->getClassification().boundaries.size()) return FALSE;
    result = leaf->getClassification().boundaries[reference->boundaryIndex];
    return TRUE;
}

// The leaf body is the innermost instance of the boundary provenance.
const SimpleBody* CsgBody::findLeaf(const RayBoundary& boundary) const
{
    RayInstanceStep step;
    if (!boundary.getInstance(0, step)) return NULL;
    for (long int i = 0; i < leaves.size(); i++) {
        if (((const Entity*)(leaves[i])) == step.instance) return leaves[i];
    }
    return NULL;
}

BOOLEAN CsgBody::doIntersectionFirstHit(const Ray& inOutRay, RayHit* outHit) const
{
    Ray localRay;
    Matrix4x4d forward, inverse;
    RayBoundary boundary;
    if (!toLocalRay(inOutRay, localRay, forward, inverse) ||
        !findBoundary(localRay, TRUE, 0.0, boundary)) return FALSE;
    const double t = boundary.t;
    if (outHit) {
        if (outHit->shouldStoreRay() || outHit->needsAnySurfaceData()) {
            const Ray worldRay = inOutRay.withT(t);
            outHit->setRay(worldRay);
            if (outHit->needsAnySurfaceData()) doExtraInformation(worldRay, t, outHit);
        }
        else outHit->setHitDistance(t);
    }
    return TRUE;
}

void CsgBody::doExtraInformation(const Ray& inRay, double inT, RayHit* outData) const
{
    Ray localRay;
    Matrix4x4d forward, inverse;
    RayBoundary boundary;
    if (!toLocalRay(inRay, localRay, forward, inverse) ||
        !findBoundary(localRay, FALSE, inT, boundary)) return;
    const SimpleBody* leaf = findLeaf(boundary);
    if (!leaf) return;
    // Leaf bodies read the ray stored in the hit, so it carries the local ray.
    const Ray leafRay = localRay.withT(inT);
    outData->setRay(leafRay);
    leaf->doExtraInformation(leafRay, inT, outData);
    if (outData->needsNormal()) {
        Vector3Dd n = outData->normal;
        if (boundary.normalReversed) n = Vector3Dd(-n.x(), -n.y(), -n.z());
        outData->normal = Vector3Dd(
            inverse.get(0,0)*n.x()+inverse.get(1,0)*n.y()+inverse.get(2,0)*n.z(),
            inverse.get(0,1)*n.x()+inverse.get(1,1)*n.y()+inverse.get(2,1)*n.z(),
            inverse.get(0,2)*n.x()+inverse.get(1,2)*n.y()+inverse.get(2,2)*n.z()).normalized();
    }
    if (outData->needsPoint()) outData->point = forward.multiply(outData->point);
    if (outData->needsTangent()) outData->tangent = forward.withoutTranslation().multiply(outData->tangent).normalized();
    if (outData->material == NULL) outData->material = getMaterial();
    outData->setRay(inRay.withT(inT));
}

RaySolidQueryStatus::Value CsgBody::classifyRay(const Ray& ray, const RayCrossingQuery& query,
    RaySolidClassification& result) const
{
    result.clear();
    Ray localRay;
    Matrix4x4d forward, inverse;
    if (!toLocalRay(ray, localRay, forward, inverse)) return RaySolidQueryStatus::Indeterminate;
    CsgRayClassification classification;
    const RaySolidQueryStatus::Value status =
        CsgRothRayClassifier::classify(getExpression(), localRay, query, classification);
    if (status != RaySolidQueryStatus::Supported) return status;
    result = classification.getSolid();
    const RayInstanceStep step(this, forward, inverse);
    for (long int i = 0; i < result.boundaries.size(); i++) {
        if (!result.boundaries[i].appendInstance(step)) {
            result.clear();
            return RaySolidQueryStatus::Indeterminate;
        }
    }
    return status;
}

void CsgBody::getSolidWorldBounds(GeometryBounds& result) const
{
    GeometryBounds local;
    CsgSolidBounds::query(getExpression(), local);
    Matrix4x4d forward, inverse;
    if (queryAffinePlacement(forward, inverse) != AffinePlacementStatus::Valid) { result.setIndeterminate(); return; }
    result = local.transformed(forward);
}

void CsgBody::getWorldBounds(GeometryBounds& result) const { getSolidWorldBounds(result); }

GeometryContainment::Value CsgBody::classifyPointCertified(const Vector3Dd& point, double distanceTolerance) const
{
    Matrix4x4d forward, inverse;
    const AffinePlacementStatus::Value placement = queryAffinePlacement(forward, inverse);
    if (placement == AffinePlacementStatus::Invalid) return GeometryContainment::InvalidInput;
    if (placement == AffinePlacementStatus::Unsupported) return GeometryContainment::Unsupported;
    if (placement == AffinePlacementStatus::Indeterminate) return GeometryContainment::Indeterminate;
    CsgPlacedExpression placed(getExpression());
    if (placed.setAffinePlacement(forward, inverse) != AffinePlacementStatus::Valid)
        return GeometryContainment::Indeterminate;
    return CsgCrossingMembershipFilter::classifyOperand(placed, point, distanceTolerance);
}

const Class CsgBody::CLASS = { "CsgBody", &SimpleBody::CLASS };

const Class *
CsgBody::getClass() const
{
    return &CLASS;
}
