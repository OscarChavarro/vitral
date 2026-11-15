package vsdk.toolkit.environment.scene.csg;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SimpleBody;

import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGe;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLe;
import static vsdk.toolkit.GTestAssertions.expectNe;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/csg_evaluator_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class CsgEvaluatorTest
{

    private static final Ray alongX = Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0));

    @Test
    void CsgRothRayClassifierContract_CombinesTableThreeOperatorsWithOwners()
    {
    SimpleBody left = new SimpleBody();
    SimpleBody right = new SimpleBody();
    left.setGeometry(new Sphere(1)); right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(1.5,0,0));
    CsgBodyOperand a = new CsgBodyOperand(left);
    CsgBodyOperand b = new CsgBodyOperand(right);
    CsgTree unionTree = new CsgTree(CsgOperation.UNION), intersection = new CsgTree(CsgOperation.INTERSECTION),
        difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(unionTree.addOperand(a)); expectTrue(unionTree.addOperand(b));
    expectTrue(intersection.addOperand(a)); expectTrue(intersection.addOperand(b));
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b));
    CsgRayClassification result = new CsgRayClassification();

    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(unionTree,alongX,new RayCrossingQuery(0,20),result));
    final RaySolidClassification u =result.getSolid();
    expectEq(2, u.boundaries.size()); expectEq(3, u.insideSegments.size());
    expectDoubleEq(2, u.boundaries.get(0).t); expectDoubleEq(5.5, u.boundaries.get(1).t);
    expectFalse(u.insideSegments.get(0)); expectTrue(u.insideSegments.get(1)); expectFalse(u.insideSegments.get(2));
    expectEq(2, result.getOwnerGroupCount());
    expectEq(1, result.getOwnerCount(0)); expectEq(0, result.getOwner(0,0).leafIndex);
    expectEq(1, result.getOwnerCount(1)); expectEq(1, result.getOwner(1,0).leafIndex);
    expectEq(a, result.getLeaves().get(0).getPath().getOperand(0));

    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(intersection,alongX,new RayCrossingQuery(0,20),result));
    expectEq(2, result.getSolid().boundaries.size());
    expectDoubleEq(3.5, result.getSolid().boundaries.get(0).t);
    expectDoubleEq(4, result.getSolid().boundaries.get(1).t);
    expectEq(1, result.getOwner(0,0).leafIndex);
    expectEq(0, result.getOwner(1,0).leafIndex);

    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(difference,alongX,new RayCrossingQuery(0,20),result));
    final RaySolidClassification d =result.getSolid();
    expectEq(2, d.boundaries.size());
    expectDoubleEq(2, d.boundaries.get(0).t); expectDoubleEq(3.5, d.boundaries.get(1).t);
    expectFalse(d.boundaries.get(0).normalReversed);
    expectTrue(d.boundaries.get(1).normalReversed);
    expectEq(1, result.getOwner(1,0).leafIndex);
    expectTrue(result.getLeaves().get(1).getPath().isReversed());
}

    @Test
    void CsgRothRayClassifierContract_CoincidentEventsGiveRegularizedResult()
    {
    SimpleBody first = new SimpleBody();
    SimpleBody second = new SimpleBody();
    SimpleBody same = new SimpleBody();
    first.setGeometry(new Box(2,2,2)); second.setGeometry(new Box(2,2,2)); same.setGeometry(new Box(2,2,2));
    second.setPosition(new Vector3Dd(2,0,0));
    CsgBodyOperand a = new CsgBodyOperand(first);
    CsgBodyOperand b = new CsgBodyOperand(second);
    CsgBodyOperand c = new CsgBodyOperand(same);
    CsgRayClassification result = new CsgRayClassification();

    CsgTree touching = new CsgTree(CsgOperation.UNION);
    expectTrue(touching.addOperand(a)); expectTrue(touching.addOperand(b));
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(touching,alongX,new RayCrossingQuery(0,20),result));
    expectEq(4.0, result.getLeaves().get(0).getClassification().boundaries.get(1).t);
    expectEq(4.0, result.getLeaves().get(1).getClassification().boundaries.get(0).t);
    expectEq(2, result.getSolid().boundaries.size());
    expectDoubleEq(2, result.getSolid().boundaries.get(0).t);
    expectDoubleEq(6, result.getSolid().boundaries.get(1).t);

    CsgTree faceContact = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(faceContact.addOperand(a)); expectTrue(faceContact.addOperand(b));
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(faceContact,alongX,new RayCrossingQuery(0,20),result));
    expectEq(0, result.getSolid().boundaries.size());
    expectEq(1, result.getSolid().insideSegments.size());
    expectFalse(result.getSolid().insideSegments.get(0));

    CsgTree identical = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(identical.addOperand(a)); expectTrue(identical.addOperand(c));
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(identical,alongX,new RayCrossingQuery(0,20),result));
    expectEq(2, result.getSolid().boundaries.size());
    // No single face changes the intersection; both are joint owners.
    expectEq(2, result.getOwnerCount(0));
    expectEq(2, result.getOwnerCount(1));

    CsgTree emptyDifference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(emptyDifference.addOperand(a)); expectTrue(emptyDifference.addOperand(c));
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(emptyDifference,alongX,new RayCrossingQuery(0,20),result));
    expectEq(0, result.getSolid().boundaries.size());
    expectFalse(result.getSolid().insideSegments.get(0));
}

    @Test
    void CsgRothRayClassifierContract_InteriorStartUnboundedOperandAndFailures()
    {
    SimpleBody ball = new SimpleBody();
    SimpleBody half = new SimpleBody();
    ball.setGeometry(new Sphere(1)); half.setGeometry(new PlanarHalfSpace(1,0,0,0));
    CsgBodyOperand a = new CsgBodyOperand(ball);
    CsgBodyOperand h = new CsgBodyOperand(half);
    CsgRayClassification result = new CsgRayClassification();
    CsgTree inside = new CsgTree(CsgOperation.UNION);
    expectTrue(inside.addOperand(a));
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(inside,
        Ray.fromAffine(new Vector3Dd(0,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,5),result));
    expectEq(1, result.getSolid().boundaries.size());
    expectTrue(result.getSolid().insideSegments.get(0));
    expectDoubleEq(1, result.getSolid().boundaries.get(0).t);

    CsgTree cut = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(cut.addOperand(h)); expectTrue(cut.addOperand(a));
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(cut,alongX,new RayCrossingQuery(0,20),result));
    final boolean halfStartsInside=result.getLeaves().get(0).getClassification().insideSegments.get(0);
    expectEq(halfStartsInside, result.getSolid().insideSegments.get(0));
    for (long i=0;i+1<result.getSolid().insideSegments.size();++i)
        expectNe(result.getSolid().insideSegments.get((int)(i)), result.getSolid().insideSegments.get((int)(i+1)));

    CsgTestOperand uncertain = new CsgTestOperand(GeometryContainment.Indeterminate);
    CsgTree partial = new CsgTree(CsgOperation.UNION);
    expectTrue(partial.addOperand(a)); expectTrue(partial.addOperand(uncertain));
    expectEq(RaySolidQueryStatus.Unsupported, CsgRothRayClassifier.classify(partial,alongX,new RayCrossingQuery(0,20),result));
    expectEq(0, result.getSolid().insideSegments.size());
    expectEq(0, result.getLeaves().size());
    expectEq(RaySolidQueryStatus.InvalidInput, CsgRothRayClassifier.classify(inside,alongX,new RayCrossingQuery(2,2),result));
}

    @Test
    void CsgRothRayClassifierContract_PlacedNestedDifferenceKeepsLeafProvenance()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody hole = new SimpleBody();
    outer.setGeometry(new Sphere(2)); hole.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(hole);
    CsgTree shell = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(shell.addOperand(a)); expectTrue(shell.addOperand(b));
    CsgPlacedExpression placed = new CsgPlacedExpression(shell);
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(
        new Matrix4x4d().translation(3,0,0).multiply(new Matrix4x4d().scale(2,1,1)),
        new Matrix4x4d().scale(0.5,1,1).multiply(new Matrix4x4d().translation(-3,0,0))));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    CsgRayClassification result = new CsgRayClassification();
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(root,
        Ray.fromAffine(new Vector3Dd(-5,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,20),result));
    final RaySolidClassification s =result.getSolid();
    expectEq(4, s.boundaries.size());
    final double[] expected = {4,6,10,12};
    for (int i=0;i<4;++i) expectDoubleEq(expected[i], s.boundaries.get((int)(i)).t);
    expectFalse(s.boundaries.get(0).normalReversed); expectTrue(s.boundaries.get(1).normalReversed);
    expectTrue(s.boundaries.get(2).normalReversed); expectFalse(s.boundaries.get(3).normalReversed);
    final CsgRayLeaf holeLeaf=result.getLeaves().get(result.getOwner(1,0).leafIndex);
    expectEq(2, holeLeaf.getPath().getLength());
    expectEq(placed, holeLeaf.getPath().getOperand(0));
    expectEq(b, holeLeaf.getPath().getOperand(1));
    expectEq(1, holeLeaf.getPlacementCount());
}

// Independent check: each segment's membership must match the certified point
// classification of the expression at the segment midpoint.
    @Test
    void CsgRothRayClassifierContract_SegmentsMatchPointClassificationOnRandomTrees()
    {
    int seed=12345;
    final int bodyCount=6;
    SimpleBody[] bodies = new SimpleBody[bodyCount];
    for (int i=0;i<bodyCount;++i) bodies[i] = new SimpleBody();
    for (int i=0;i<bodyCount;++i) {
        if (i%2==0) bodies[i].setGeometry(new Sphere(0.6+0.1*i));
        else bodies[i].setGeometry(new Box(1.0+0.2*i,1.4,0.9+0.1*i));
    }
    long checked=0;
    for (int trial=0;trial<200;++trial) {
        for (int i=0;i<bodyCount;++i) {
            seed=seed*1103515245+12345; final double x=((seed>>>8)%2000)/1000.0-1.0;
            seed=seed*1103515245+12345; final double y=((seed>>>8)%2000)/2000.0-0.5;
            bodies[i].setPosition(new Vector3Dd(x,y,0.1*i-0.25));
        }
        CsgBodyOperand o0 = new CsgBodyOperand(bodies[0]);
        CsgBodyOperand o1 = new CsgBodyOperand(bodies[1]);
        CsgBodyOperand o2 = new CsgBodyOperand(bodies[2]);
        CsgBodyOperand o3 = new CsgBodyOperand(bodies[3]);
        CsgBodyOperand o4 = new CsgBodyOperand(bodies[4]);
        CsgBodyOperand o5 = new CsgBodyOperand(bodies[5]);
        final CsgOperation[] ops = {CsgOperation.UNION,CsgOperation.INTERSECTION,CsgOperation.DIFFERENCE};
        CsgTree inner = new CsgTree(ops[trial%3]);
        CsgTree middle = new CsgTree(ops[(trial/3)%3]);
        CsgTree root = new CsgTree(ops[(trial/9)%3]);
        expectTrue(inner.addOperand(o0)); expectTrue(inner.addOperand(o1)); expectTrue(inner.addOperand(o2));
        expectTrue(middle.addOperand(o3)); expectTrue(middle.addOperand(inner));
        expectTrue(root.addOperand(middle)); expectTrue(root.addOperand(o4)); expectTrue(root.addOperand(o5));
        seed=seed*1103515245+12345; final double oy=((seed>>>8)%1000)/1000.0-0.5;
        final Ray ray=Ray.fromAffine(new Vector3Dd(-4,oy,0.05),new Vector3Dd(1,0.03,-0.02));
        final double minT=0, maxT=8;
        CsgRayClassification result = new CsgRayClassification();
        expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(root,ray,new RayCrossingQuery(minT,maxT),result));
        final RaySolidClassification s =result.getSolid();
        expectEq(s.boundaries.size()+1, s.insideSegments.size());
        for (long i=0;i<s.insideSegments.size();++i) {
            final double a=i==0 ? minT : s.boundaries.get((int)(i-1)).t;
            final double b=i==s.boundaries.size() ? maxT : s.boundaries.get((int)(i)).t;
            if (b-a<1e-6) continue;
            final double t=0.5*(a+b);
            final Vector3Dd p=ray.getOrigin().add(ray.getDirection().multiply(t));
            final GeometryContainment c=CsgPointClassification.classify(root,p,0);
            if (c!=GeometryContainment.Inside && c!=GeometryContainment.Outside) continue;
            expectEq(c==GeometryContainment.Inside, s.insideSegments.get((int)(i)), "" + "trial " + trial + " segment " + i);
            ++checked;
        }
        for (long i=0;i<s.boundaries.size();++i) {
            expectGt(result.getOwnerCount(i), 0);
        }
    }
    expectGt(checked, 400);
}

    @Test
    void CsgCrossingMembershipFilterContract_PovHeuristicKeepsUnionInternalSurfaces()
    {
    SimpleBody left = new SimpleBody();
    SimpleBody right = new SimpleBody();
    left.setGeometry(new Sphere(1)); right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(1.5,0,0));
    CsgBodyOperand a = new CsgBodyOperand(left);
    CsgBodyOperand b = new CsgBodyOperand(right);
    CsgTree unionTree = new CsgTree(CsgOperation.UNION), intersection = new CsgTree(CsgOperation.INTERSECTION),
        difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(unionTree.addOperand(a)); expectTrue(unionTree.addOperand(b));
    expectTrue(intersection.addOperand(a)); expectTrue(intersection.addOperand(b));
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b));
    CsgRaySurfaceEvents events = new CsgRaySurfaceEvents();

    // Union keeps every operand event; Roth reports only 2 and 5.5.
    expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(
        unionTree,alongX,new RayCrossingQuery(0,20),1e-9,events));
    expectEq(4, events.size());
    final double[] unionT = {2,3.5,4,5.5};
    for (int i=0;i<4;++i) expectDoubleEq(unionT[i], events.getT(i));

    expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(
        intersection,alongX,new RayCrossingQuery(0,20),1e-9,events));
    expectEq(2, events.size());
    expectDoubleEq(3.5, events.getT(0)); expectDoubleEq(4, events.getT(1));
    expectEq(1, events.get(0).leafIndex); expectEq(0, events.get(1).leafIndex);

    expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(
        difference,alongX,new RayCrossingQuery(0,20),1e-9,events));
    expectEq(2, events.size());
    expectDoubleEq(2, events.getT(0)); expectDoubleEq(3.5, events.getT(1));
    final CsgRayEventReference hole=events.get(1);
    expectTrue(events.getLeaves().get(hole.leafIndex).getClassification().boundaries.get((int)(hole.boundaryIndex)).normalReversed);
}

    @Test
    void CsgCrossingMembershipFilterContract_BoundaryMembershipKeepsCoincidentFacesAndUncertaintyFails()
    {
    SimpleBody first = new SimpleBody();
    SimpleBody same = new SimpleBody();
    first.setGeometry(new Box(2,2,2)); same.setGeometry(new Box(2,2,2));
    CsgBodyOperand a = new CsgBodyOperand(first);
    CsgBodyOperand c = new CsgBodyOperand(same);
    CsgTree identical = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(identical.addOperand(a)); expectTrue(identical.addOperand(c));
    CsgRaySurfaceEvents events = new CsgRaySurfaceEvents();
    // Both coincident faces satisfy the boundary-inclusive membership; Roth
    // reports a single boundary for each pair.
    expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(
        identical,alongX,new RayCrossingQuery(0,20),1e-9,events));
    expectEq(4, events.size());
    expectDoubleEq(2, events.getT(0)); expectDoubleEq(2, events.getT(1));
    expectEq(0, events.get(0).leafIndex); expectEq(1, events.get(1).leafIndex);

    UncertifiedTestBody opaque = new UncertifiedTestBody(); opaque.setGeometry(new Sphere(1));
    opaque.setPosition(new Vector3Dd(0.5,0,0));
    CsgBodyOperand u = new CsgBodyOperand(opaque);
    CsgTree uncertain = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(uncertain.addOperand(a)); expectTrue(uncertain.addOperand(u));
    expectEq(RaySolidQueryStatus.Unsupported, CsgCrossingMembershipFilter.select(
        uncertain,alongX,new RayCrossingQuery(0,20),1e-9,events));
    expectEq(0, events.size());

    // A proven exclusion by another sibling makes the uncertain one irrelevant.
    SimpleBody far = new SimpleBody(); far.setGeometry(new Sphere(0.5)); far.setPosition(new Vector3Dd(0,5,0));
    CsgBodyOperand f = new CsgBodyOperand(far);
    CsgTree excluded = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(excluded.addOperand(a)); expectTrue(excluded.addOperand(u)); expectTrue(excluded.addOperand(f));
    CsgTree probe = new CsgTree(CsgOperation.UNION);
    expectTrue(probe.addOperand(a));
    expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(
        excluded,alongX,new RayCrossingQuery(0,20),1e-9,events));
    expectEq(0, events.size());
    expectEq(RaySolidQueryStatus.InvalidInput, CsgCrossingMembershipFilter.select(
        probe,alongX,new RayCrossingQuery(0,20),-1,events));
}

    @Test
    void CsgVersionStampContract_ResultsDetectEditsOfBorrowedOperands()
    {
    SimpleBody left = new SimpleBody();
    SimpleBody right = new SimpleBody();
    left.setGeometry(new Sphere(1)); right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(1.5,0,0));
    CsgBodyOperand a = new CsgBodyOperand(left);
    CsgBodyOperand b = new CsgBodyOperand(right);
    CsgTree inner = new CsgTree(CsgOperation.UNION);
    expectTrue(inner.addOperand(a)); expectTrue(inner.addOperand(b));
    CsgPlacedExpression placed = new CsgPlacedExpression(inner);
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    CsgRayClassification result = new CsgRayClassification();
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(root,alongX,new RayCrossingQuery(0,20),result));
    expectTrue(result.getVersions().isComplete());
    expectTrue(result.getVersions().isCurrent());
    right.setPosition(new Vector3Dd(1.4,0,0));
    expectFalse(result.getVersions().isCurrent());
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(root,alongX,new RayCrossingQuery(0,20),result));
    expectTrue(result.getVersions().isCurrent());
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(
        new Matrix4x4d().translation(1,0,0),new Matrix4x4d().translation(-1,0,0)));
    expectFalse(result.getVersions().isCurrent());
    expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(root,alongX,new RayCrossingQuery(0,20),result));
    SimpleBody extra = new SimpleBody(); extra.setGeometry(new Sphere(0.5));
    CsgBodyOperand c = new CsgBodyOperand(extra);
    expectTrue(inner.addOperand(c));
    expectFalse(result.getVersions().isCurrent());

    CsgPointProof proof = new CsgPointProof();
    expectEq(GeometryContainment.Inside, CsgPointClassification.classifyWithProof(root,new Vector3Dd(1,0,0),0.01,proof));
    expectTrue(proof.isAvailable());
    expectTrue(proof.getVersions().isCurrent());
    left.setPosition(new Vector3Dd(0,0.1,0));
    expectFalse(proof.getVersions().isCurrent());

    CsgRaySurfaceEvents events = new CsgRaySurfaceEvents();
    expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(
        root,alongX,new RayCrossingQuery(0,20),1e-9,events));
    expectTrue(events.getVersions().isCurrent());
    extra.setPosition(new Vector3Dd(0,0.2,0));
    expectFalse(events.getVersions().isCurrent());

    // A view that does not track its edits can never be proven current.
    CsgTestOperand untracked = new CsgTestOperand(GeometryContainment.Inside);
    CsgTree opaque = new CsgTree(CsgOperation.UNION);
    expectTrue(opaque.addOperand(a)); expectTrue(opaque.addOperand(untracked));
    CsgVersionStamp stamp = new CsgVersionStamp();
    expectTrue(stamp.capture(opaque));
    expectTrue(stamp.isCaptured());
    expectFalse(stamp.isComplete());
    expectFalse(stamp.isCurrent());
}

    @Test
    void CsgRayWorkspaceContract_ReusedStorageGivesFreshResults()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody hole = new SimpleBody();
    SimpleBody side = new SimpleBody();
    outer.setGeometry(new Sphere(2)); hole.setGeometry(new Sphere(1)); side.setGeometry(new Box(1,1,1));
    side.setPosition(new Vector3Dd(1.5,0,0));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(hole);
    CsgBodyOperand c = new CsgBodyOperand(side);
    CsgTree shell = new CsgTree(CsgOperation.DIFFERENCE);
    CsgTree small = new CsgTree(CsgOperation.UNION);
    expectTrue(shell.addOperand(a)); expectTrue(shell.addOperand(b)); expectTrue(shell.addOperand(c));
    expectTrue(small.addOperand(b));
    CsgRayWorkspace workspace = new CsgRayWorkspace();
    CsgRayClassification reused = new CsgRayClassification();
    final CsgTree[] order = {shell,small,shell,small,shell};
    for (int i=0;i<5;++i) {
        CsgRayClassification fresh = new CsgRayClassification();
        final Ray ray=Ray.fromAffine(new Vector3Dd(-5,0.1*i,0),new Vector3Dd(1,0,0));
        expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(order[i],ray,new RayCrossingQuery(0,20),fresh));
        expectEq(RaySolidQueryStatus.Supported, CsgRothRayClassifier.classify(order[i],ray,new RayCrossingQuery(0,20),workspace,reused));
        expectEq(fresh.getSolid().boundaries.size(), reused.getSolid().boundaries.size());
        expectEq(fresh.getSolid().insideSegments.size(), reused.getSolid().insideSegments.size());
        expectEq(fresh.getLeaves().size(), reused.getLeaves().size());
        for (long k=0;k<fresh.getSolid().boundaries.size();++k) {
            expectEq(fresh.getSolid().boundaries.get((int)(k)).t, reused.getSolid().boundaries.get((int)(k)).t);
            expectEq(fresh.getSolid().boundaries.get((int)(k)).normalReversed, reused.getSolid().boundaries.get((int)(k)).normalReversed);
            expectEq(fresh.getSolid().boundaries.get((int)(k)).getInstanceCount(), reused.getSolid().boundaries.get((int)(k)).getInstanceCount());
            expectEq(fresh.getOwnerCount(k), reused.getOwnerCount(k));
            expectEq(fresh.getOwner(k,0).leafIndex, reused.getOwner(k,0).leafIndex);
        }
        for (long k=0;k<fresh.getLeaves().size();++k)
            expectEq(fresh.getLeaves().get(k).getPath().getLength(), reused.getLeaves().get(k).getPath().getLength());
    }
    CsgTestOperand uncertain = new CsgTestOperand(GeometryContainment.Indeterminate);
    CsgTree failing = new CsgTree(CsgOperation.UNION);
    expectTrue(failing.addOperand(a)); expectTrue(failing.addOperand(uncertain));
    expectEq(RaySolidQueryStatus.Unsupported, CsgRothRayClassifier.classify(failing,alongX,new RayCrossingQuery(0,20),workspace,reused));
    expectEq(0, reused.getLeaves().size());
    expectEq(0, reused.getSolid().insideSegments.size());
    expectFalse(reused.getVersions().isCaptured());
}
    @Test
    void CsgBodyContract_DifferenceHitsCarryLeafNormalsMaterialsAndPlacement()
    {
    CsgBody body = new CsgBody(CsgOperation.DIFFERENCE,CsgBodyEvaluator.ROTH);
    SimpleBody outer=new SimpleBody(); outer.setGeometry(new Sphere(2)); outer.setMaterial(new SimpleMaterial());
    SimpleBody hole=new SimpleBody(); hole.setGeometry(new Sphere(1)); hole.setMaterial(new SimpleMaterial());
    hole.setPosition(new Vector3Dd(1.5,0,0));
    expectTrue(body.addLeaf(0,outer)); expectTrue(body.addLeaf(0,hole));
    expectFalse(body.addLeaf(3,new SimpleBody()));
    expectEq(-1, body.addNode(5,CsgOperation.UNION));
    expectEq(2, body.getLeafCount());
    RayHit hit = new RayHit(RayHit.DETAIL_POINT|RayHit.DETAIL_NORMAL,false);
    expectTrue(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-5,0,0),new Vector3Dd(1,0,0)),hit));
    expectNear(3, hit.getHitDistance(), 1e-12);
    expectNear(-1, hit.normal.x(), 1e-12); expectNear(-2, hit.point.x(), 1e-12);
    expectEq(outer.getMaterial(), hit.material);
    hit.reset(RayHit.DETAIL_POINT|RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(5,0,0),new Vector3Dd(-1,0,0)),hit));
    expectNear(4.5, hit.getHitDistance(), 1e-12);
    expectNear(1, hit.normal.x(), 1e-12);
    expectEq(hole.getMaterial(), hit.material);
    body.setPosition(new Vector3Dd(0,0,10));
    expectFalse(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-5,0,0),new Vector3Dd(1,0,0)),hit));
    hit.reset(RayHit.DETAIL_POINT|RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-5,0,10),new Vector3Dd(1,0,0)),hit));
    expectNear(3, hit.getHitDistance(), 1e-12); expectNear(10, hit.point.z(), 1e-12);
    GeometryBounds bounds = new GeometryBounds(); body.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectLe(bounds.getMinimum().z(), 8); expectGe(bounds.getMaximum().z(), 12);
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(-1,0,10),1e-9));
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(1.5,0,10),1e-9));
    RaySolidClassification solid = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(Ray.fromAffine(new Vector3Dd(-5,0,10),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,20),solid));
    expectEq(2, solid.boundaries.size());
    expectNear(3, solid.boundaries.get(0).t, 1e-12); expectNear(5.5, solid.boundaries.get(1).t, 1e-12);
}

    @Test
    void CsgBodyContract_EvaluatorsDifferOnNestedUnionInternalSurfaces()
    {
    CsgBody[] bodies = {new CsgBody(CsgOperation.DIFFERENCE,CsgBodyEvaluator.ROTH),
        new CsgBody(CsgOperation.DIFFERENCE,CsgBodyEvaluator.CROSSING_MEMBERSHIP)};
    for (int i=0;i<2;++i) {
        SimpleBody outer=new SimpleBody(); outer.setGeometry(new Sphere(5));
        SimpleBody left=new SimpleBody(); left.setGeometry(new Sphere(2)); left.setPosition(new Vector3Dd(-1,0,0));
        SimpleBody right=new SimpleBody(); right.setGeometry(new Sphere(2)); right.setPosition(new Vector3Dd(1,0,0));
        expectTrue(bodies[i].addLeaf(0,outer));
        final long holes=bodies[i].addNode(0,CsgOperation.UNION);
        expectEq(1, holes);
        expectTrue(bodies[i].addLeaf(holes,left)); expectTrue(bodies[i].addLeaf(holes,right));
        RayHit outside = new RayHit(RayHit.DETAIL_NONE,false);
        expectTrue(bodies[i].doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-10,0,0),new Vector3Dd(1,0,0)),outside));
        expectNear(5, outside.getHitDistance(), 1e-12);
    }
    RayHit roth = new RayHit(RayHit.DETAIL_POINT|RayHit.DETAIL_NORMAL,false);
    RayHit pov = new RayHit(RayHit.DETAIL_POINT|RayHit.DETAIL_NORMAL,false);
    final Ray fromHole=Ray.fromAffine(new Vector3Dd(0,0,0),new Vector3Dd(1,0,0));
    expectTrue(bodies[0].doIntersectionFirstHit(fromHole,roth));
    expectTrue(bodies[1].doIntersectionFirstHit(fromHole,pov));
    expectNear(3, roth.getHitDistance(), 1e-12);
    expectNear(1, pov.getHitDistance(), 1e-12);
    expectNear(-1, roth.normal.x(), 1e-12);
    expectNear(-1, pov.normal.x(), 1e-12);
}

    @Test
    void CsgMembershipRuleContract_FoldStopsOnlyWhenLaterOperandsCannotChangeIt()
    {
    final CsgOperation[] operations = { CsgOperation.UNION, CsgOperation.INTERSECTION,
        CsgOperation.DIFFERENCE };
    for ( int k = 0; k < 3; k++ ) {
        for ( int accumulated = 0; accumulated < 2; accumulated++ ) {
            final boolean value = accumulated == 1;
            final boolean unchanged = CsgMembershipRule.combine(operations[k], value, false) == value &&
                CsgMembershipRule.combine(operations[k], value, true) == value;
            expectEq(CsgMembershipRule.isDecided(operations[k], value), unchanged);
        }
    }
}

    @Test
    void CsgSegmentClassificationContract_MergesLeafSegmentsByTableThree()
    {
    // Left solid [1,3], right solid [2,4]; payloads name the events.
    CsgSegmentClassification<Integer> left = new CsgSegmentClassification<Integer>(false, 2);
    CsgSegmentClassification<Integer> right = new CsgSegmentClassification<Integer>(false, 2);
    left.append(1, 10); left.append(3, 11);
    right.append(2, 20); right.append(4, 21);
    expectTrue(left.getEvents().get(0).isEntering());
    expectFalse(left.getEvents().get(1).isEntering());
    final CsgSegmentClassification<Integer> u =
        CsgSegmentClassification.merge(left, right, CsgOperation.UNION);
    expectEq(2, u.getEvents().size());
    expectEq(10, u.getEvents().get(0).getPayload()); expectEq(21, u.getEvents().get(1).getPayload());
    final CsgSegmentClassification<Integer> i =
        CsgSegmentClassification.merge(left, right, CsgOperation.INTERSECTION);
    expectEq(2, i.getEvents().size());
    expectEq(20, i.getEvents().get(0).getPayload()); expectEq(11, i.getEvents().get(1).getPayload());
    final CsgSegmentClassification<Integer> d =
        CsgSegmentClassification.merge(left, right, CsgOperation.DIFFERENCE);
    expectEq(2, d.getEvents().size());
    expectEq(10, d.getEvents().get(0).getPayload()); expectTrue(d.getEvents().get(0).isEntering());
    expectEq(20, d.getEvents().get(1).getPayload()); expectFalse(d.getEvents().get(1).isEntering());
    expectFalse(d.isInitialInside());
}

    @Test
    void CsgSegmentClassificationContract_InitialStateAndTiesTakeTheLeftEvent()
    {
    // A ray starting inside the left solid, leaving it where the right one ends.
    CsgSegmentClassification<Integer> left = new CsgSegmentClassification<Integer>(true, 1);
    CsgSegmentClassification<Integer> right = new CsgSegmentClassification<Integer>(true, 1);
    left.append(2, 1); right.append(2, 2);
    final CsgSegmentClassification<Integer> u =
        CsgSegmentClassification.merge(left, right, CsgOperation.UNION);
    expectTrue(u.isInitialInside());
    expectEq(1, u.getEvents().size());
    expectEq(2, u.getEvents().get(0).getPayload());
    final CsgSegmentClassification<Integer> i =
        CsgSegmentClassification.merge(left, right, CsgOperation.INTERSECTION);
    expectEq(1, i.getEvents().size());
    expectEq(1, i.getEvents().get(0).getPayload());
}

    @Test
    void CsgTreeContract_OperationEditsAreStructureEdits()
    {
    CsgTree tree = new CsgTree(CsgOperation.UNION);
    long[] before = {0}, after = {0};
    expectTrue(tree.queryStructureVersion(before));
    tree.setOperation(CsgOperation.INTERSECTION);
    expectTrue(tree.queryStructureVersion(after));
    expectEq(CsgOperation.INTERSECTION, tree.getOperation());
    expectNe(before[0], after[0]);
}

    @Test
    void CsgBodyOperandContract_StructuralTraversalFollowsThePlacedGeometryOptIn()
    {
    SimpleBody plain = new SimpleBody(); plain.setGeometry(new Sphere(1));
    CsgBody composed = new CsgBody(CsgOperation.UNION, CsgBodyEvaluator.ROTH);
    CsgBodyOperand plainView = new CsgBodyOperand(plain);
    CsgBodyOperand composedView = new CsgBodyOperand(composed);
    expectTrue(plain.isStructuralTraversalSafe());
    expectTrue(plainView.isStructuralTraversalSafe());
    expectFalse(composedView.isStructuralTraversalSafe());
}
}
