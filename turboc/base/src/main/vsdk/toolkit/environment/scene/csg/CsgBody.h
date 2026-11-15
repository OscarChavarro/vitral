#ifndef __CSG_BODY__
#define __CSG_BODY__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Class.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBodyEvaluator.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperation.h"
class CsgBodyOperand;
class CsgExpression;
class CsgTree;
class RayBoundary;

// Scene body whose solid is a CSG expression over leaf bodies it owns. Node 0
// is the root; nested nodes are identity-placed CsgTree operands, so every leaf
// keeps its own placement in the coordinates of this body, and the body's
// placement maps the whole solid to its parent. Rays are evaluated with the
// selected shared evaluator; the visible hit is the first boundary (Roth) or
// selected surface event (crossing membership) beyond VSDK::EPSILON. Surface
// detail comes from the owning leaf body, with the normal turned outward of the
// combined solid, and a leaf without material shows the material of this body.
// A failed or uncertain evaluation reports no hit. Leaves must not be edited
// after insertion; queries keep no shared scratch state, so concurrent renders
// are safe.
class CsgBody : public SimpleBody {
private:
    CsgBodyEvaluator::Value evaluator;
    double membershipTolerance;
    ArrayList<CsgTree*> nodes;
    ArrayList<CsgBodyOperand*> operands;
    ArrayList<SimpleBody*> leaves;
    BOOLEAN findBoundary(const Ray& localRay, BOOLEAN firstVisible, double t, RayBoundary& result) const;
    BOOLEAN toLocalRay(const Ray& ray, Ray& result, Matrix4x4d& forward, Matrix4x4d& inverse) const;
    BOOLEAN isLocallyMissed(const Ray& localRay) const;
    const SimpleBody* findLeaf(const RayBoundary& boundary) const;
    CsgBody(const CsgBody&);
    CsgBody& operator=(const CsgBody&);
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    CsgBody(CsgOperation::Value rootOperation, CsgBodyEvaluator::Value evaluator);
    virtual ~CsgBody();
    CsgBodyEvaluator::Value getEvaluator() const;
    // World tolerance of the crossing membership tests; ignored by Roth.
    double getMembershipTolerance() const;
    BOOLEAN setMembershipTolerance(double tolerance);
    // Appends an operation node under an existing node and returns its index,
    // or -1 when the parent is unknown.
    long int addNode(long int parentNode, CsgOperation::Value operation);
    // Takes ownership of the leaf, also on failure.
    BOOLEAN addLeaf(long int parentNode, SimpleBody* leaf);
    long int getNodeCount() const;
    long int getLeafCount() const;
    const SimpleBody* getLeaf(long int index) const;
    const CsgExpression& getExpression() const;

    virtual BOOLEAN doIntersectionFirstHit(const Ray& inOutRay, RayHit* outHit) const;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) const;
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;
    virtual void getWorldBounds(GeometryBounds& result) const;
    virtual void getSolidWorldBounds(GeometryBounds& result) const;
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& point, double distanceTolerance) const;
};
#endif
