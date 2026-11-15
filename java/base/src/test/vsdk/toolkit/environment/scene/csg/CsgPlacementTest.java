package vsdk.toolkit.environment.scene.csg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.AffineToleranceMap;
import vsdk.toolkit.environment.scene.SimpleBody;

import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLe;
import static vsdk.toolkit.GTestAssertions.expectNe;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/csg_placement_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class CsgPlacementTest
{

    @Test
    void CsgPlacedComposition_PreservesWorldToleranceUnderShearAndReflection()
    {
    SimpleBody sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    CsgBodyOperand leaf = new CsgBodyOperand(sphere);
    CsgTree child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(leaf));
    CsgPlacedExpression placed = new CsgPlacedExpression(child);
    final Matrix4x4d shear=Matrix4x4d.identityMatrix().withVal(0,1,0.5);
    final Matrix4x4d unshear=Matrix4x4d.identityMatrix().withVal(0,1,-0.5);
    final Matrix4x4d forward=new Matrix4x4d().translation(4,-1,2).multiply(shear).multiply(new Matrix4x4d().scale(-2,1,0.5));
    final Matrix4x4d inverse=new Matrix4x4d().scale(-0.5,1,2).multiply(unshear).multiply(new Matrix4x4d().translation(-4,1,-2));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward,inverse));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root,forward.multiply(new Vector3Dd()),0.05));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root,forward.multiply(new Vector3Dd()),0));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root,forward.multiply(new Vector3Dd(2,0,0)),0.05));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root,forward.multiply(new Vector3Dd(2,0,0)),0));
    expectEq(GeometryContainment.Indeterminate, CsgPointClassification.classify(root,forward.multiply(new Vector3Dd(1,0,0)),0));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(root,forward.multiply(new Vector3Dd(1.01,0,0)),0.1));
    expectEq(AffinePlacementStatus.Invalid, placed.setAffinePlacement(new Matrix4x4d().scale(0,1,1),new Matrix4x4d()));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root,forward.multiply(new Vector3Dd()),0.05));
    expectFalse(child.addOperand(placed));
}

    @Test
    void CsgPlacedComposition_NestedDifferenceUsesOrderedOrientation()
    {
    SimpleBody big = new SimpleBody();
    SimpleBody small = new SimpleBody();
    big.setGeometry(new Sphere(2)); small.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(big);
    CsgBodyOperand b = new CsgBodyOperand(small);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b));
    CsgPlacedExpression placed = new CsgPlacedExpression(difference);
    final Matrix4x4d forward=new Matrix4x4d().translation(10,0,0).multiply(new Matrix4x4d().scale(2,1,1));
    final Matrix4x4d inverse=new Matrix4x4d().scale(0.5,1,1).multiply(new Matrix4x4d().translation(-10,0,0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward,inverse));
    CsgTree root = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(root.addOperand(placed));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root,new Vector3Dd(10,0,0),0.05));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root,new Vector3Dd(13,0,0),0.05));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root,new Vector3Dd(15,0,0),0.05));
}

    @Test
    void AffineToleranceMap_EnclosesInverseImagesAndDoesNotMutateOnFailure()
    {
    final Matrix4x4d forward=new Matrix4x4d().translation(4,-1,2).multiply(
        Matrix4x4d.identityMatrix().withVal(0,1,0.5)).multiply(new Matrix4x4d().scale(-2,1,0.5));
    final Matrix4x4d inverse=new Matrix4x4d().scale(-0.5,1,2).multiply(
        Matrix4x4d.identityMatrix().withVal(0,1,-0.5)).multiply(new Matrix4x4d().translation(-4,1,-2));
    final Vector3Dd point = new Vector3Dd(4.3,-0.7,2.1);
    Vector3Dd[] center = {new Vector3Dd()};
    double[] bound = {0};
    double[] inner = {-1};
    expectEq(AffinePlacementStatus.Valid, AffineToleranceMap.map(forward,point,0.4,center,bound,inner));
    expectGt(inner[0], 0);
    for (int x=-1;x<=1;++x) for (int y=-1;y<=1;++y) for (int z=-1;z<=1;++z) {
        final double length=Math.sqrt((double)(x*x+y*y+z*z));
        if (length==0) continue;
        final Vector3Dd sample=point.add(new Vector3Dd(x,y,z).multiply(0.4/length));
        expectLe(inverse.multiply(sample).subtract(center[0]).length(), bound[0]);
        final Vector3Dd childSample=center[0].add(new Vector3Dd(x,y,z).multiply(inner[0]/length));
        expectLe(forward.multiply(childSample).subtract(point).length(), 0.4);
    }
    center[0]=new Vector3Dd(7,8,9); bound[0]=42;
    expectEq(AffinePlacementStatus.Indeterminate, AffineToleranceMap.map(new Matrix4x4d().scale(0,1,1),point,0.4,center,bound));
    expectDoubleEq(7, center[0].x()); expectDoubleEq(42, bound[0]);
    expectEq(AffinePlacementStatus.Invalid, AffineToleranceMap.map(forward,point,-1,center,bound));
    expectDoubleEq(7, center[0].x()); expectDoubleEq(42, bound[0]);
}

    @Test
    void CsgPointWitnessContract_RecordsDecisivePathAndDifferenceOrientation()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody inner = new SimpleBody();
    outer.setGeometry(new Sphere(2)); inner.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(inner);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b));
    CsgPlacedExpression placed = new CsgPlacedExpression(difference);
    final Matrix4x4d forward=new Matrix4x4d().translation(3,0,0);
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward,new Matrix4x4d().translation(-3,0,0)));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    CsgPointWitness witness = new CsgPointWitness();
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyWithWitness(root,new Vector3Dd(3,0,0),0.1,witness));
    expectEq(2, witness.getLength());
    expectEq(placed, witness.getOperand(0)); expectEq(b, witness.getOperand(1));
    expectTrue(witness.isReversed());
    expectEq(null, witness.getOperand(-1)); expectEq(null, witness.getOperand(2));
    // Inside a difference requires both operands, so a single-leaf proof is unavailable.
    expectEq(GeometryContainment.Inside, CsgPointClassification.classifyWithWitness(root,new Vector3Dd(4.5,0,0),0.05,witness));
    expectFalse(witness.isAvailable()); expectFalse(witness.isReversed());
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classifyWithWitness(root,new Vector3Dd(),-1,witness));
    expectFalse(witness.isAvailable());
}

    @Test
    void CsgPointWitnessContract_CertifiesOnlyExposedBoundaries()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody inner = new SimpleBody();
    SimpleBody distant = new SimpleBody();
    outer.setGeometry(new Sphere(2)); inner.setGeometry(new Sphere(1)); distant.setGeometry(new Sphere(1));
    distant.setPosition(new Vector3Dd(10,0,0));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(inner);
    CsgBodyOperand c = new CsgBodyOperand(distant);
    CsgTree united = new CsgTree(CsgOperation.UNION);
    CsgTree intersected = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(a)); expectTrue(united.addOperand(c));
    expectTrue(intersected.addOperand(a)); expectTrue(intersected.addOperand(b));
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b));
    CsgPointWitness witness = new CsgPointWitness();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithWitness(united,new Vector3Dd(2,0,0),0.01,witness));
    expectEq(1, witness.getLength()); expectEq(a, witness.getOperand(0)); expectFalse(witness.isReversed());
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithWitness(intersected,new Vector3Dd(1,0,0),0.01,witness));
    expectEq(1, witness.getLength()); expectEq(b, witness.getOperand(0)); expectFalse(witness.isReversed());
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithWitness(difference,new Vector3Dd(1,0,0),0.01,witness));
    expectEq(1, witness.getLength()); expectEq(b, witness.getOperand(0)); expectTrue(witness.isReversed());
    CsgBodyOperand repeatedInner = new CsgBodyOperand(inner);
    expectTrue(difference.addOperand(repeatedInner));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithWitness(difference,new Vector3Dd(1,0,0),0.01,witness));
    expectEq(1, witness.getLength()); expectEq(b, witness.getOperand(0)); expectTrue(witness.isReversed());
    CsgTree coincident = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(coincident.addOperand(b)); expectTrue(coincident.addOperand(b));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyWithWitness(coincident,new Vector3Dd(1,0,0),0.01,witness));
    expectFalse(witness.isAvailable());
}

    @Test
    void CsgPlacedComposition_TraversesMultiplePlacementsAndRetainsWitnessPath()
    {
    SimpleBody sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    CsgBodyOperand leaf = new CsgBodyOperand(sphere);
    CsgTree inner = new CsgTree(CsgOperation.UNION);
    expectTrue(inner.addOperand(leaf));
    CsgPlacedExpression first = new CsgPlacedExpression(inner);
    expectEq(AffinePlacementStatus.Valid, first.setAffinePlacement(
        new Matrix4x4d().translation(2,0,0),new Matrix4x4d().translation(-2,0,0)));
    CsgTree middle = new CsgTree(CsgOperation.UNION);
    expectTrue(middle.addOperand(first));
    CsgPlacedExpression second = new CsgPlacedExpression(middle);
    expectEq(AffinePlacementStatus.Valid, second.setAffinePlacement(
        new Matrix4x4d().scale(-2,1,1),new Matrix4x4d().scale(-0.5,1,1)));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(second));
    CsgPointWitness witness = new CsgPointWitness();
    expectEq(GeometryContainment.Inside, CsgPointClassification.classifyWithWitness(root,new Vector3Dd(-4,0,0),0.1,witness));
    expectEq(3, witness.getLength());
    expectEq(second, witness.getOperand(0)); expectEq(first, witness.getOperand(1)); expectEq(leaf, witness.getOperand(2));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root,new Vector3Dd(2,0,0),0.1));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(root,new Vector3Dd(-6.01,0,0),0.1));
}

    @Test
    void AffineToleranceMap_NearSingularMappingPreservesFailureOutputs()
    {
    // The exact determinant of these binary64 entries is zero, but the rounded
    // products cannot decide its sign.
    final Matrix4x4d nearlySingular=Matrix4x4d.identityMatrix().withVal(0,0,0.1).withVal(0,1,0.3)
        .withVal(1,0,0.2).withVal(1,1,0.6);
    Vector3Dd[] point = {new Vector3Dd(7,8,9)}; double[] tolerance={42};
    expectEq(AffinePlacementStatus.Indeterminate, AffineToleranceMap.map(nearlySingular,new Vector3Dd(),0.1,point,tolerance));
    expectDoubleEq(7, point[0].x()); expectDoubleEq(42, tolerance[0]);
    // An exactly representable determinant 2^-52 is decided, however small.
    final Matrix4x4d tiny=Matrix4x4d.identityMatrix().withVal(0,0,1).withVal(0,1,1)
        .withVal(1,0,1).withVal(1,1,Math.nextAfter(1,2));
    expectEq(AffinePlacementStatus.Valid, AffineToleranceMap.map(tiny,new Vector3Dd(),0.1,point,tolerance));
}

    @Test
    void AffineToleranceMap_EnclosesSamplesAcrossScalesAndShears()
    {
    final double[] scales = {0.01,0.5,2,100};
    for (int i=0;i<4;++i) for (int j=0;j<4;++j) {
        final double sx=scales[i], sy=scales[j], sz=(i%2!=0 ? -1.5 : 1.5);
        final Matrix4x4d forward=new Matrix4x4d().translation(5,-3,2).multiply(
            Matrix4x4d.identityMatrix().withVal(0,1,0.75).withVal(1,2,-0.25)).multiply(
            new Matrix4x4d().scale(sx,sy,sz));
        final Matrix4x4d inverse=new Matrix4x4d().scale(1/sx,1/sy,1/sz).multiply(
            Matrix4x4d.identityMatrix().withVal(0,1,-0.75).withVal(1,2,0.25).withVal(0,2,-0.1875)).multiply(
            new Matrix4x4d().translation(-5,3,-2));
        final Vector3Dd point = new Vector3Dd(5.25,-3.5,2.125);
        Vector3Dd[] center = {null}; double[] outer={0}, inner={-1};
        expectEq(AffinePlacementStatus.Valid, AffineToleranceMap.map(forward,point,0.25,center,outer,inner));
        expectGt(inner[0], 0);
        for (int x=-1;x<=1;++x) for (int y=-1;y<=1;++y) for (int z=-1;z<=1;++z) {
            final double length=Math.sqrt((double)(x*x+y*y+z*z));
            if (length==0) continue;
            final Vector3Dd direction=new Vector3Dd(x,y,z).multiply(1/length);
            final Vector3Dd parentSample=point.add(direction.multiply(0.25));
            expectLe(inverse.multiply(parentSample).subtract(center[0]).length(), outer[0]);
            final Vector3Dd childSample=center[0].add(direction.multiply(inner[0]));
            expectLe(forward.multiply(childSample).subtract(point).length(), 0.25);
        }
    }
}

    @Test
    void AffineToleranceMap_RejectsUnsupportedRoundingWithoutChangingOutputs()
    {
    // Java floating point always rounds to nearest and cannot change the
    // rounding mode, which is the cpp case where fesetround fails: skipped.
    assumeTrue(false, "Rounding mode cannot be changed");
    Vector3Dd[] center = {new Vector3Dd(7,8,9)}; double[] outer={42}, inner={17};
    final AffinePlacementStatus status=AffineToleranceMap.map(new Matrix4x4d().translation(1,2,3),
        new Vector3Dd(4,5,6),0.1,center,outer,inner);
    expectEq(AffinePlacementStatus.Unsupported, status);
    expectDoubleEq(7, center[0].x()); expectDoubleEq(42, outer[0]); expectDoubleEq(17, inner[0]);
}

    @Test
    void CsgPlacedComposition_RotatedBoundaryKeepsItsWitness()
    {
    SimpleBody sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    CsgBodyOperand leaf = new CsgBodyOperand(sphere);
    CsgTree child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(leaf));
    CsgPlacedExpression placed = new CsgPlacedExpression(child);
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d inverseRotation=rotation.transpose();
    final Matrix4x4d forward=new Matrix4x4d().translation(2,3,0).multiply(rotation).multiply(new Matrix4x4d().scale(2,1,1));
    final Matrix4x4d inverse=new Matrix4x4d().scale(0.5,1,1).multiply(inverseRotation).multiply(new Matrix4x4d().translation(-2,-3,0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward,inverse));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    CsgPointWitness witness = new CsgPointWitness();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithWitness(
        root,forward.multiply(new Vector3Dd(1,0,0)),0.1,witness));
    expectEq(2, witness.getLength());
    expectEq(placed, witness.getOperand(0)); expectEq(leaf, witness.getOperand(1));
    expectFalse(witness.isReversed());
}

    @Test
    void CsgPlacedComposition_InnerProbeDoesNotHideErrors()
    {
    CsgTestOperand invalid = new CsgTestOperand(GeometryContainment.Indeterminate,GeometryContainment.InvalidInput,0.08);
    CsgTestOperand unsupported = new CsgTestOperand(GeometryContainment.Indeterminate,GeometryContainment.Unsupported,0.08);
    CsgTree invalidChild = new CsgTree(CsgOperation.UNION);
    CsgTree unsupportedChild = new CsgTree(CsgOperation.UNION);
    expectTrue(invalidChild.addOperand(invalid)); expectTrue(unsupportedChild.addOperand(unsupported));
    CsgPlacedExpression placedInvalid = new CsgPlacedExpression(invalidChild);
    CsgPlacedExpression placedUnsupported = new CsgPlacedExpression(unsupportedChild);
    final Matrix4x4d forward=new Matrix4x4d().scale(2,1,1), inverse=new Matrix4x4d().scale(0.5,1,1);
    expectEq(AffinePlacementStatus.Valid, placedInvalid.setAffinePlacement(forward,inverse));
    expectEq(AffinePlacementStatus.Valid, placedUnsupported.setAffinePlacement(forward,inverse));
    CsgTree rootInvalid = new CsgTree(CsgOperation.UNION);
    CsgTree rootUnsupported = new CsgTree(CsgOperation.UNION);
    expectTrue(rootInvalid.addOperand(placedInvalid)); expectTrue(rootUnsupported.addOperand(placedUnsupported));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(rootInvalid,new Vector3Dd(),0.1));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(rootUnsupported,new Vector3Dd(),0.1));
}

    @Test
    void CsgPointProofContract_RecordsConjunctiveLeafFactsAndBoundaryOrientation()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody inner = new SimpleBody();
    SimpleBody distant = new SimpleBody();
    outer.setGeometry(new Sphere(2)); inner.setGeometry(new Sphere(1)); distant.setGeometry(new Sphere(1));
    distant.setPosition(new Vector3Dd(10,0,0));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(inner);
    CsgBodyOperand c = new CsgBodyOperand(distant);
    CsgTree united = new CsgTree(CsgOperation.UNION);
    CsgTree intersection = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(a)); expectTrue(united.addOperand(c));
    expectTrue(intersection.addOperand(a)); expectTrue(intersection.addOperand(b));
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b));
    CsgPointProof proof = new CsgPointProof();
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyWithProof(united,new Vector3Dd(5,0,0),0.1,proof));
    expectTrue(proof.isAvailable()); expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Outside, proof.getEvidence(0).getClassification());
    expectEq(GeometryContainment.Outside, proof.getEvidence(1).getClassification());
    expectEq(a, proof.getEvidence(0).getPath().getOperand(0));
    expectEq(c, proof.getEvidence(1).getPath().getOperand(0));
    expectDoubleEq(5, proof.getEvidence(0).getPoint().x());
    expectDoubleEq(0.1, proof.getEvidence(0).getTolerance());
    expectEq(GeometryContainment.Inside, CsgPointClassification.classifyWithProof(intersection,new Vector3Dd(),0.1,proof));
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Inside, proof.getEvidence(0).getClassification());
    expectEq(GeometryContainment.Inside, proof.getEvidence(1).getClassification());
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithProof(difference,new Vector3Dd(1,0,0),0.01,proof));
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Boundary, proof.getEvidence(1).getClassification());
    expectTrue(proof.getEvidence(1).getPath().isReversed());
    expectEq(null, proof.getEvidence(2));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classifyWithProof(difference,new Vector3Dd(),-1,proof));
    expectFalse(proof.isAvailable());
}
    @Test
    void CsgPointProofContract_SimplifiesEqualSpheresButNotMerelyNearbySurfaces()
    {
    SimpleBody solid = new SimpleBody();
    SimpleBody equalButDistinct = new SimpleBody();
    SimpleBody almostEqual = new SimpleBody();
    solid.setGeometry(new Sphere(1)); equalButDistinct.setGeometry(new Sphere(1));
    almostEqual.setGeometry(new Sphere(1)); almostEqual.setPosition(new Vector3Dd(0.0001,0,0));
    CsgBodyOperand a = new CsgBodyOperand(solid);
    CsgBodyOperand alias = new CsgBodyOperand(solid);
    CsgBodyOperand other = new CsgBodyOperand(equalButDistinct);
    CsgBodyOperand nearOther = new CsgBodyOperand(almostEqual);
    CsgTree united = new CsgTree(CsgOperation.UNION);
    CsgTree intersection = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    CsgTree unknown = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(a)); expectTrue(united.addOperand(alias));
    expectTrue(intersection.addOperand(a)); expectTrue(intersection.addOperand(alias));
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(alias));
    expectTrue(unknown.addOperand(a)); expectTrue(unknown.addOperand(nearOther));
    CsgPointProof proof = new CsgPointProof();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithProof(united,new Vector3Dd(1,0,0),0.01,proof));
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(intersection,new Vector3Dd(1,0,0),0.01));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyWithProof(difference,new Vector3Dd(1,0,0),0.01,proof));
    expectEq(2, proof.getCount()); expectTrue(proof.getEvidence(1).getPath().isReversed());
    CsgTree equalGeometry = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(equalGeometry.addOperand(a)); expectTrue(equalGeometry.addOperand(other));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyWithProof(equalGeometry,new Vector3Dd(1,0,0),0.01,proof));
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Indeterminate, CsgPointClassification.classifyWithProof(unknown,new Vector3Dd(1,0,0),0.01,proof));
    expectFalse(proof.isAvailable());
    CsgTestOperand uncertain = new CsgTestOperand(GeometryContainment.Indeterminate);
    CsgTree selfWithUnknown = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(selfWithUnknown.addOperand(a)); expectTrue(selfWithUnknown.addOperand(alias));
    expectTrue(selfWithUnknown.addOperand(uncertain));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyWithProof(selfWithUnknown,new Vector3Dd(1,0,0),0.01,proof));
    expectEq(2, proof.getCount());
}

    @Test
    void CsgRayLeafQueryContract_CollectsPlacedDifferenceAndPreservesOrientation()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody inner = new SimpleBody();
    outer.setGeometry(new Sphere(2)); inner.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(inner);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b));
    CsgPlacedExpression placed = new CsgPlacedExpression(difference);
    final Matrix4x4d forward=new Matrix4x4d().translation(3,0,0).multiply(new Matrix4x4d().scale(2,1,1));
    final Matrix4x4d inverse=new Matrix4x4d().scale(0.5,1,1).multiply(new Matrix4x4d().translation(-3,0,0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward,inverse));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    CsgRayLeafBatch batch = new CsgRayLeafBatch();
    expectEq(RaySolidQueryStatus.Supported, CsgRayLeafQuery.query(root,
        Ray.fromAffine(new Vector3Dd(-5,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,20),batch));
    expectEq(2, batch.size());
    final CsgRayLeaf first=batch.get(0); final CsgRayLeaf second=batch.get(1);
    expectEq(2, first.getPath().getLength()); expectEq(2, second.getPath().getLength());
    expectEq(placed, first.getPath().getOperand(0)); expectEq(a, first.getPath().getOperand(1));
    expectEq(b, second.getPath().getOperand(1));
    expectFalse(first.getPath().isReversed()); expectTrue(second.getPath().isReversed());
    final RaySolidClassification out =first.getClassification();
    final RaySolidClassification in =second.getClassification();
    expectEq(2, out.boundaries.size()); expectEq(2, in.boundaries.size());
    expectEq(3, out.insideSegments.size()); expectEq(3, in.insideSegments.size());
    expectDoubleEq(4, out.boundaries.get(0).t); expectDoubleEq(12, out.boundaries.get(1).t);
    expectDoubleEq(6, in.boundaries.get(0).t); expectDoubleEq(10, in.boundaries.get(1).t);
    expectFalse(out.insideSegments.get(0)); expectTrue(out.insideSegments.get(1)); expectFalse(out.insideSegments.get(2));
    expectFalse(in.insideSegments.get(0)); expectTrue(in.insideSegments.get(1)); expectFalse(in.insideSegments.get(2));
    expectFalse(out.boundaries.get(0).normalReversed); expectTrue(in.boundaries.get(0).normalReversed);
    expectEq(1, out.boundaries.get(0).getInstanceCount());
    expectEq(1, first.getPlacementCount());
    final CsgRayPlacement parent=first.getPlacement(0);
    expectNe(null, parent);
    expectEq(placed, parent.operand);
    expectDoubleEq(3, parent.childToParent.multiply(new Vector3Dd()).x());
}
    @Test
    void CsgRayLeafQueryContract_KeepsCoincidentLeavesAndClearsResultsOnFailure()
    {
    SimpleBody sphere = new SimpleBody(); sphere.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(sphere);
    CsgBodyOperand alias = new CsgBodyOperand(sphere);
    CsgTree equal = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(equal.addOperand(a)); expectTrue(equal.addOperand(alias));
    CsgRayLeafBatch batch = new CsgRayLeafBatch();
    final Ray ray=Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0));
    expectEq(RaySolidQueryStatus.Supported, CsgRayLeafQuery.query(equal,ray,new RayCrossingQuery(0,10),batch));
    expectEq(2, batch.size());
    expectDoubleEq(batch.get(0).getClassification().boundaries.get(0).t, batch.get(1).getClassification().boundaries.get(0).t);
    expectFalse(batch.get(0).getPath().isReversed()); expectTrue(batch.get(1).getPath().isReversed());
    CsgTestOperand unsupported = new CsgTestOperand(GeometryContainment.Indeterminate);
    CsgTree partial = new CsgTree(CsgOperation.UNION);
    expectTrue(partial.addOperand(a)); expectTrue(partial.addOperand(unsupported));
    expectEq(RaySolidQueryStatus.Unsupported, CsgRayLeafQuery.query(partial,ray,new RayCrossingQuery(0,10),batch));
    expectEq(0, batch.size());
    unsupported.setMalformedRay(true);
    expectEq(RaySolidQueryStatus.Indeterminate, CsgRayLeafQuery.query(partial,ray,new RayCrossingQuery(0,10),batch));
    expectEq(0, batch.size());
    expectEq(RaySolidQueryStatus.InvalidInput, CsgRayLeafQuery.query(equal,ray,new RayCrossingQuery(1,1),batch));
    expectEq(0, batch.size());
}

    @Test
    void CsgRayLeafQueryContract_NestedDifferencesPreserveParityAndLeafParameter()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody middle = new SimpleBody();
    SimpleBody inner = new SimpleBody();
    outer.setGeometry(new Sphere(3)); middle.setGeometry(new Sphere(2)); inner.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(middle);
    CsgBodyOperand c = new CsgBodyOperand(inner);
    CsgTree nested = new CsgTree(CsgOperation.DIFFERENCE);
    CsgTree root = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(nested.addOperand(b)); expectTrue(nested.addOperand(c));
    expectTrue(root.addOperand(a)); expectTrue(root.addOperand(nested));
    CsgRayLeafBatch batch = new CsgRayLeafBatch();
    expectEq(RaySolidQueryStatus.Supported, CsgRayLeafQuery.query(root,
        Ray.fromAffine(new Vector3Dd(-5,0,0),new Vector3Dd(2,0,0)),new RayCrossingQuery(0,10),batch));
    expectEq(3, batch.size());
    expectFalse(batch.get(0).getPath().isReversed());
    expectTrue(batch.get(1).getPath().isReversed());
    expectFalse(batch.get(2).getPath().isReversed());
    expectTrue(batch.get(1).getClassification().boundaries.get(0).normalReversed);
    expectFalse(batch.get(2).getClassification().boundaries.get(0).normalReversed);
    expectDoubleEq(1, batch.get(0).getClassification().boundaries.get(0).t);
    expectDoubleEq(1.5, batch.get(1).getClassification().boundaries.get(0).t);
    expectDoubleEq(2, batch.get(2).getClassification().boundaries.get(0).t);
}

    @Test
    void CsgRayLeafQueryContract_CollectsBasicSolidFamiliesAndEndpointEvents()
    {
    SimpleBody box = new SimpleBody();
    SimpleBody halfspace = new SimpleBody();
    SimpleBody quadric = new SimpleBody();
    box.setGeometry(new Box(2,2,2));
    halfspace.setGeometry(new PlanarHalfSpace(1,0,0,0));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1));
    CsgBodyOperand a = new CsgBodyOperand(box);
    CsgBodyOperand b = new CsgBodyOperand(halfspace);
    CsgBodyOperand c = new CsgBodyOperand(quadric);
    CsgTree root = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(root.addOperand(a)); expectTrue(root.addOperand(b)); expectTrue(root.addOperand(c));
    CsgRayLeafBatch batch = new CsgRayLeafBatch();
    expectEq(RaySolidQueryStatus.Supported, CsgRayLeafQuery.query(root,
        Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(2,4),batch));
    expectEq(3, batch.size());
    expectEq(2, batch.get(0).getClassification().boundaries.size());
    expectEq(1, batch.get(1).getClassification().boundaries.size());
    expectEq(2, batch.get(2).getClassification().boundaries.size());
    expectDoubleEq(2, batch.get(0).getClassification().boundaries.get(0).t);
    expectDoubleEq(4, batch.get(0).getClassification().boundaries.get(1).t);
    expectDoubleEq(3, batch.get(1).getClassification().boundaries.get(0).t);
    expectDoubleEq(2, batch.get(2).getClassification().boundaries.get(0).t);
    expectDoubleEq(4, batch.get(2).getClassification().boundaries.get(1).t);
}

    @Test
    void CsgRayEventQueryContract_GroupsExactTiesWithoutMergingNearbyEvents()
    {
    SimpleBody same = new SimpleBody();
    SimpleBody shifted = new SimpleBody();
    same.setGeometry(new Sphere(1)); shifted.setGeometry(new Sphere(1));
    shifted.setPosition(new Vector3Dd(0.000001,0,0));
    CsgBodyOperand a = new CsgBodyOperand(same);
    CsgBodyOperand alias = new CsgBodyOperand(same);
    CsgBodyOperand b = new CsgBodyOperand(shifted);
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(a)); expectTrue(root.addOperand(alias)); expectTrue(root.addOperand(b));
    CsgRayLeafBatch leaves = new CsgRayLeafBatch();
    expectEq(RaySolidQueryStatus.Supported, CsgRayLeafQuery.query(root,
        Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,10),leaves));
    CsgRayEventBatch groups = new CsgRayEventBatch();
    expectEq(RaySolidQueryStatus.Supported, CsgRayEventQuery.group(leaves,groups));
    expectEq(4, groups.size());
    expectDoubleEq(2, groups.get(0).getT()); expectDoubleEq(4, groups.get(2).getT());
    expectEq(2, groups.get(0).size()); expectEq(2, groups.get(2).size());
    expectEq(0, groups.get(0).get(0).leafIndex); expectEq(0, groups.get(0).get(0).boundaryIndex);
    expectEq(1, groups.get(0).get(1).leafIndex); expectEq(0, groups.get(0).get(1).boundaryIndex);
    expectEq(2, groups.get(1).get(0).leafIndex);
    expectGt(groups.get(1).getT(), groups.get(0).getT());
    expectGt(groups.get(3).getT(), groups.get(2).getT());
    leaves.get(2).getClassification().insideSegments.clear();
    expectEq(RaySolidQueryStatus.Indeterminate, CsgRayEventQuery.group(leaves,groups));
    expectEq(0, groups.size());
}
    @Test
    void CsgRayEventQueryContract_EmptyLeafBoundariesProduceEmptyEventBatch()
    {
    SimpleBody solid = new SimpleBody(); solid.setGeometry(new Sphere(1));
    CsgBodyOperand operand = new CsgBodyOperand(solid); CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(operand));
    CsgRayLeafBatch leaves = new CsgRayLeafBatch(); CsgRayEventBatch groups = new CsgRayEventBatch();
    expectEq(RaySolidQueryStatus.Supported, CsgRayLeafQuery.query(root,
        Ray.fromAffine(new Vector3Dd(0,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,0.5),leaves));
    expectEq(1, leaves.size()); expectTrue(leaves.get(0).getClassification().insideSegments.get(0));
    expectEq(RaySolidQueryStatus.Supported, CsgRayEventQuery.group(leaves,groups));
    expectEq(0, groups.size());
}

    @Test
    void CsgPointProofContract_CarriesAllLeafFactsThroughPlacedNestedExpression()
    {
    SimpleBody left = new SimpleBody();
    SimpleBody right = new SimpleBody();
    left.setGeometry(new Sphere(1)); right.setGeometry(new Sphere(1)); right.setPosition(new Vector3Dd(10,0,0));
    CsgBodyOperand a = new CsgBodyOperand(left);
    CsgBodyOperand b = new CsgBodyOperand(right);
    CsgTree child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(a)); expectTrue(child.addOperand(b));
    CsgPlacedExpression placed = new CsgPlacedExpression(child);
    final Matrix4x4d forward=new Matrix4x4d().translation(3,0,0).multiply(new Matrix4x4d().scale(2,1,1));
    final Matrix4x4d inverse=new Matrix4x4d().scale(0.5,1,1).multiply(new Matrix4x4d().translation(-3,0,0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward,inverse));
    CsgTree root = new CsgTree(CsgOperation.UNION); expectTrue(root.addOperand(placed));
    CsgPointProof proof = new CsgPointProof();
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyWithProof(root,new Vector3Dd(13,0,0),0.1,proof));
    expectEq(2, proof.getCount());
    expectEq(placed, proof.getEvidence(0).getPath().getOperand(0));
    expectEq(a, proof.getEvidence(0).getPath().getOperand(1));
    expectEq(placed, proof.getEvidence(1).getPath().getOperand(0));
    expectEq(b, proof.getEvidence(1).getPath().getOperand(1));
    expectNear(5, proof.getEvidence(0).getPoint().x(), 1e-12);
    expectGt(proof.getEvidence(0).getTolerance(), 0.05);
}

    @Test
    void CsgExactArithmeticContract_ComparesDyadicProductsAndAffineEndpointsExactly()
    {
    expectTrue(CsgExactArithmetic.equalProducts(0.5,6,3,1));
    expectFalse(CsgExactArithmetic.equalProducts(0.1,0.1,0.01,1));
    expectTrue(CsgExactArithmetic.equalProducts(Double.MIN_VALUE,2,Math.scalb(Double.MIN_VALUE,1),1));
    expectFalse(CsgExactArithmetic.equalProducts(Double.MIN_VALUE,0.5,0,1));
    expectTrue(CsgExactArithmetic.equalProducts(Double.MAX_VALUE,2,2,Double.MAX_VALUE));
    expectFalse(CsgExactArithmetic.equalProducts(Double.MAX_VALUE,2,Double.MAX_VALUE,1));
    expectTrue(CsgExactArithmetic.equalAffine(2,3,-5,1,1,0));
    expectFalse(CsgExactArithmetic.equalAffine(1,Double.MAX_VALUE,1,1,Double.MAX_VALUE,0));
    expectFalse(CsgExactArithmetic.equalProducts(Double.NaN,1,1,1));
}

    @Test
    void CsgSolidEquivalenceContract_SpheresSupportPermutationsReflectionsAndRadiusScale()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody nearby = new SimpleBody();
    a.setGeometry(new Sphere(2)); b.setGeometry(new Sphere(1)); nearby.setGeometry(new Sphere(1));
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1).withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d placement=new Matrix4x4d().translation(3,0,0).multiply(rotation).multiply(new Matrix4x4d().scale(2,2,2));
    final Matrix4x4d inverse=new Matrix4x4d().scale(0.5,0.5,0.5).multiply(rotation.transpose()).multiply(new Matrix4x4d().translation(-3,0,0));
    a.setPosition(new Vector3Dd(3,0,0));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(placement,inverse));
    nearby.setPosition(new Vector3Dd(3.00000001,0,0)); nearby.setScale(new Vector3Dd(2,2,2));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE); expectTrue(difference.addOperand(aa)); expectTrue(difference.addOperand(bb));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference,new Vector3Dd(5,0,0),0.1));
}

    @Test
    void CsgSolidEquivalenceContract_BoxesSupportTranslatedBoundsAndAffineEdgeReparameterization()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody c = new SimpleBody();
    SimpleBody shearA = new SimpleBody();
    SimpleBody shearB = new SimpleBody();
    a.setGeometry(new Box(new Vector3Dd(-1,-1,-1),new Vector3Dd(1,1,1)));
    b.setGeometry(new Box(new Vector3Dd(0,0,0),new Vector3Dd(4,4,4)));
    b.setPosition(new Vector3Dd(-1,-1,-1)); b.setScale(new Vector3Dd(0.5,0.5,0.5));
    c.setGeometry(new Box(new Vector3Dd(0,0,0),new Vector3Dd(4,4,4)));
    c.setPosition(new Vector3Dd(-1+1e-12,-1,-1)); c.setScale(new Vector3Dd(0.5,0.5,0.5));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand cc = new CsgBodyOperand(c);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
    shearA.setGeometry(new Box(new Vector3Dd(-1,-2,-3),new Vector3Dd(1,2,3)));
    shearB.setGeometry(new Box(new Vector3Dd(-0.5,-2,-3),new Vector3Dd(0.5,2,3)));
    final Matrix4x4d shear=Matrix4x4d.identityMatrix().withVal(0,1,0.5);
    final Matrix4x4d unshear=Matrix4x4d.identityMatrix().withVal(0,1,-0.5);
    expectEq(AffinePlacementStatus.Valid, shearA.setAffinePlacement(shear,unshear));
    expectEq(AffinePlacementStatus.Valid, shearB.setAffinePlacement(shear.multiply(new Matrix4x4d().scale(2,1,1)),
        new Matrix4x4d().scale(0.5,1,1).multiply(unshear)));
    CsgBodyOperand sa = new CsgBodyOperand(shearA); CsgBodyOperand sb = new CsgBodyOperand(shearB); expectTrue(sa.isSameSolid(sb)); expectTrue(sb.isSameSolid(sa));
}

    @Test
    void CsgSolidEquivalenceContract_HalfSpacesAndQuadricsPreserveSignAndAxisMappings()
    {
    SimpleBody ha = new SimpleBody();
    SimpleBody hb = new SimpleBody();
    SimpleBody hc = new SimpleBody();
    SimpleBody qa = new SimpleBody();
    SimpleBody qb = new SimpleBody();
    SimpleBody qc = new SimpleBody();
    ha.setGeometry(new PlanarHalfSpace(1,2,0,-3));
    hb.setGeometry(new PlanarHalfSpace(2,-1,0,-3));
    hc.setGeometry(new PlanarHalfSpace(-1,-2,0,3));
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1).withVal(1,0,1).withVal(1,1,0);
    expectEq(AffinePlacementStatus.Valid, hb.setAffinePlacement(rotation,rotation.transpose()));
    CsgBodyOperand a = new CsgBodyOperand(ha); CsgBodyOperand b = new CsgBodyOperand(hb); CsgBodyOperand c = new CsgBodyOperand(hc); expectTrue(a.isSameSolid(b)); expectTrue(b.isSameSolid(a)); expectFalse(a.isSameSolid(c));
    qa.setGeometry(new QuadricSurface(new Vector3Dd(1,2,3),new Vector3Dd(),new Vector3Dd(),-1));
    qb.setGeometry(new QuadricSurface(new Vector3Dd(6,3,9),new Vector3Dd(),new Vector3Dd(),-3));
    qc.setGeometry(new QuadricSurface(new Vector3Dd(-1,-2,-3),new Vector3Dd(),new Vector3Dd(),1));
    expectEq(AffinePlacementStatus.Valid, qb.setAffinePlacement(rotation,rotation.transpose()));
    CsgBodyOperand x = new CsgBodyOperand(qa); CsgBodyOperand y = new CsgBodyOperand(qb); CsgBodyOperand z = new CsgBodyOperand(qc); expectTrue(x.isSameSolid(y)); expectTrue(y.isSameSolid(x)); expectFalse(x.isSameSolid(z));
}

    @Test
    void CsgSolidEquivalenceContract_PlacedViewsOfSameExpressionMustHaveSamePlacement()
    {
    SimpleBody a = new SimpleBody(); a.setGeometry(new Sphere(1)); CsgBodyOperand leaf = new CsgBodyOperand(a);
    CsgTree child = new CsgTree(CsgOperation.UNION); expectTrue(child.addOperand(leaf));
    CsgPlacedExpression first = new CsgPlacedExpression(child);
    CsgPlacedExpression second = new CsgPlacedExpression(child);
    CsgPlacedExpression shifted = new CsgPlacedExpression(child);
    expectEq(AffinePlacementStatus.Valid, first.setAffinePlacement(new Matrix4x4d().translation(3,0,0),new Matrix4x4d().translation(-3,0,0)));
    expectEq(AffinePlacementStatus.Valid, second.setAffinePlacement(new Matrix4x4d().translation(3,0,0),new Matrix4x4d().translation(-3,0,0)));
    expectEq(AffinePlacementStatus.Valid, shifted.setAffinePlacement(new Matrix4x4d().translation(4,0,0),new Matrix4x4d().translation(-4,0,0)));
    expectTrue(first.isSameSolid(second)); expectTrue(second.isSameSolid(first)); expectFalse(first.isSameSolid(shifted));
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE); expectTrue(difference.addOperand(first)); expectTrue(difference.addOperand(second));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference,new Vector3Dd(4,0,0),0.1));
}

    @Test
    void CsgExactArithmeticContract_IndependentExactRationalFixtures()
    {
    // The statements of csg_exact_reference.inc, read at test time.
    final java.util.List<CsgReferenceInclude.ExactCase> cases = CsgReferenceInclude.exactCases();
    expectEq(1379, cases.size());
    for (CsgReferenceInclude.ExactCase c : cases) {
        if (c.function.equals("equalProducts"))
            expectEq(c.expected, CsgExactArithmetic.equalProducts(c.scalars[0],c.scalars[1],c.scalars[2],c.scalars[3]));
        else if (c.function.equals("equalAffine"))
            expectEq(c.expected, CsgExactArithmetic.equalAffine(c.scalars[0],c.scalars[1],c.scalars[2],c.scalars[3],c.scalars[4],c.scalars[5]));
        else
            expectEq(c.expected, CsgExactArithmetic.equalSumProducts(c.arrays[0],c.arrays[1],c.arrays[2],c.arrays[3],c.count));
    }
}

    @Test
    void CsgSolidEquivalenceContract_RecognizesEquivalentCrossFamilySolids()
    {
    SimpleBody sphere = new SimpleBody();
    SimpleBody sphericalQuadric = new SimpleBody();
    SimpleBody complement = new SimpleBody();
    SimpleBody halfspace = new SimpleBody();
    SimpleBody linearQuadric = new SimpleBody();
    SimpleBody wrongHalfspace = new SimpleBody();
    sphere.setGeometry(new Sphere(2));
    sphericalQuadric.setGeometry(new QuadricSurface(new Vector3Dd(3,3,3),new Vector3Dd(),new Vector3Dd(),-12));
    complement.setGeometry(new QuadricSurface(new Vector3Dd(-3,-3,-3),new Vector3Dd(),new Vector3Dd(),12));
    halfspace.setGeometry(new PlanarHalfSpace(1,2,0,-3));
    linearQuadric.setGeometry(new QuadricSurface(new Vector3Dd(),new Vector3Dd(),new Vector3Dd(2,4,0),-6));
    wrongHalfspace.setGeometry(new QuadricSurface(new Vector3Dd(),new Vector3Dd(),new Vector3Dd(-2,-4,0),6));
    CsgBodyOperand a = new CsgBodyOperand(sphere);
    CsgBodyOperand b = new CsgBodyOperand(sphericalQuadric);
    CsgBodyOperand c = new CsgBodyOperand(complement);
    CsgBodyOperand d = new CsgBodyOperand(halfspace);
    CsgBodyOperand e = new CsgBodyOperand(linearQuadric);
    CsgBodyOperand f = new CsgBodyOperand(wrongHalfspace);
    expectTrue(a.isSameSolid(b)); expectTrue(b.isSameSolid(a)); expectFalse(a.isSameSolid(c));
    expectTrue(d.isSameSolid(e)); expectTrue(e.isSameSolid(d)); expectFalse(d.isSameSolid(f));
}

    @Test
    void CsgSolidEquivalenceContract_DoesNotEquateRoundedCoincidentBoxEndpoints()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    a.setGeometry(new Box(new Vector3Dd(0,0,0),new Vector3Dd(1,1,1)));
    b.setGeometry(new Box(new Vector3Dd(0,0,0),new Vector3Dd(0.5,1,1)));
    final double translation=Math.scalb(1.0,53);
    a.setPosition(new Vector3Dd(translation,0,0)); b.setPosition(new Vector3Dd(translation,0,0));
    expectDoubleEq(translation+1, translation+0.5); // Binary64 endpoints round together.
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    expectFalse(aa.isSameSolid(bb)); expectFalse(bb.isSameSolid(aa));
}

    @Test
    void CsgSolidEquivalenceContract_AxisHalfSpacesAllowDifferentTranslationsAndScales()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody c = new SimpleBody();
    SimpleBody d = new SimpleBody();
    a.setGeometry(new PlanarHalfSpace(1,0,0,-2));
    b.setGeometry(new PlanarHalfSpace(2,0,0,-2));
    c.setGeometry(new PlanarHalfSpace(-2,0,0,2));
    d.setGeometry(new QuadricSurface(new Vector3Dd(),new Vector3Dd(),new Vector3Dd(2,0,0),-2));
    b.setPosition(new Vector3Dd(1,9,-4)); c.setPosition(new Vector3Dd(1,9,-4));
    d.setPosition(new Vector3Dd(1,-3,4));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand cc = new CsgBodyOperand(c);
    CsgBodyOperand dd = new CsgBodyOperand(d);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa));
    expectTrue(aa.isSameSolid(dd)); expectTrue(dd.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
    b.setPosition(new Vector3Dd(1+1e-12,9,-4)); expectFalse(aa.isSameSolid(bb));
}

    @Test
    void CsgSolidEquivalenceContract_QuadricMixedTermsAndLinearSignsTransformExactly()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody c = new SimpleBody();
    a.setGeometry(new QuadricSurface(new Vector3Dd(1,2,3),new Vector3Dd(4,5,6),new Vector3Dd(7,8,9),-10));
    b.setGeometry(new QuadricSurface(new Vector3Dd(6,3,9),new Vector3Dd(-12,18,-15),new Vector3Dd(24,-21,27),-30));
    c.setGeometry(new QuadricSurface(new Vector3Dd(6,3,9),new Vector3Dd(12,18,-15),new Vector3Dd(24,-21,27),-30));
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1).withVal(1,0,1).withVal(1,1,0);
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(rotation,rotation.transpose()));
    expectEq(AffinePlacementStatus.Valid, c.setAffinePlacement(rotation,rotation.transpose()));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand cc = new CsgBodyOperand(c);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
}

    @Test
    void CsgPointAttributionContract_ReportsAllCoincidentOwnersAndDifferenceOrientation()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody inner = new SimpleBody();
    SimpleBody equalInner = new SimpleBody();
    outer.setGeometry(new Sphere(2)); inner.setGeometry(new Sphere(1)); equalInner.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(inner);
    CsgBodyOperand c = new CsgBodyOperand(equalInner);
    CsgTree united = new CsgTree(CsgOperation.UNION);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(b)); expectTrue(united.addOperand(c));
    expectTrue(difference.addOperand(a)); expectTrue(difference.addOperand(b)); expectTrue(difference.addOperand(c));
    CsgPointAttribution owners = new CsgPointAttribution();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(united,new Vector3Dd(1,0,0),0.01,owners));
    expectTrue(owners.isComplete()); expectEq(2, owners.getCount());
    expectEq(b, owners.getOwner(0).getOperand(0)); expectEq(c, owners.getOwner(1).getOperand(0));
    expectFalse(owners.getOwner(0).isReversed()); expectFalse(owners.getOwner(1).isReversed());
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(difference,new Vector3Dd(1,0,0),0.01,owners));
    expectEq(2, owners.getCount());
    expectTrue(owners.getOwner(0).isReversed()); expectTrue(owners.getOwner(1).isReversed());
    expectEq(null, owners.getOwner(2));
}
    @Test
    void CsgPointAttributionContract_ExcludesHiddenCancellationAndRejectsAmbiguity()
    {
    SimpleBody hidden = new SimpleBody();
    SimpleBody visible = new SimpleBody();
    SimpleBody nearby = new SimpleBody();
    hidden.setGeometry(new Sphere(1)); visible.setGeometry(new Sphere(1)); nearby.setGeometry(new Sphere(1));
    nearby.setPosition(new Vector3Dd(0.0001,0,0));
    CsgBodyOperand a = new CsgBodyOperand(hidden);
    CsgBodyOperand b = new CsgBodyOperand(visible);
    CsgBodyOperand c = new CsgBodyOperand(nearby);
    CsgTree cancelled = new CsgTree(CsgOperation.DIFFERENCE); expectTrue(cancelled.addOperand(a)); expectTrue(cancelled.addOperand(a));
    CsgTree root = new CsgTree(CsgOperation.UNION); expectTrue(root.addOperand(cancelled)); expectTrue(root.addOperand(b));
    CsgPointAttribution owners = new CsgPointAttribution();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(root,new Vector3Dd(1,0,0),0.01,owners));
    expectEq(1, owners.getCount()); expectEq(b, owners.getOwner(0).getOperand(0));
    CsgTree ambiguous = new CsgTree(CsgOperation.UNION); expectTrue(ambiguous.addOperand(b)); expectTrue(ambiguous.addOperand(c));
    expectEq(GeometryContainment.Indeterminate, CsgPointClassification.classifyWithAttribution(ambiguous,new Vector3Dd(1,0,0),0.01,owners));
    expectFalse(owners.isComplete()); expectEq(0, owners.getCount());
}

    @Test
    void CsgSolidEquivalenceContract_EllipsoidsSupportExactNonPermutationReparameterization()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody quadric = new SimpleBody();
    SimpleBody nearby = new SimpleBody();
    a.setGeometry(new Sphere(1)); b.setGeometry(new Sphere(1)); nearby.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1));
    final Matrix4x4d forwardA=new Matrix4x4d().scale(5,5,5), inverseA=new Matrix4x4d().scale(0.2,0.2,0.2);
    final Matrix4x4d forwardB=Matrix4x4d.identityMatrix().withVal(0,0,3).withVal(0,1,-4)
        .withVal(1,0,4).withVal(1,1,3).withVal(2,2,5);
    final Matrix4x4d inverseB=Matrix4x4d.identityMatrix().withVal(0,0,0.12).withVal(0,1,0.16)
        .withVal(1,0,-0.16).withVal(1,1,0.12).withVal(2,2,0.2);
    expectEq(AffinePlacementStatus.Valid, a.setAffinePlacement(forwardA,inverseA));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forwardB,inverseB));
    expectEq(AffinePlacementStatus.Valid, quadric.setAffinePlacement(forwardB,inverseB));
    nearby.setScale(new Vector3Dd(5,5,4.999999));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand qq = new CsgBodyOperand(quadric);
    CsgBodyOperand cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa));
    expectTrue(aa.isSameSolid(qq)); expectTrue(qq.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
}

    @Test
    void CsgSolidEquivalenceContract_AffineBoxesSupportTranslatedLocalOrigins()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody nearby = new SimpleBody();
    a.setGeometry(new Box(new Vector3Dd(-1,-1,-1),new Vector3Dd(1,1,1)));
    b.setGeometry(new Box(new Vector3Dd(0,0,0),new Vector3Dd(2,2,2)));
    nearby.setGeometry(new Box(new Vector3Dd(0,0,0),new Vector3Dd(2,2,2)));
    final Matrix4x4d shear=Matrix4x4d.identityMatrix().withVal(0,1,0.5);
    final Matrix4x4d unshear=Matrix4x4d.identityMatrix().withVal(0,1,-0.5);
    final Matrix4x4d shifted=new Matrix4x4d().translation(-1.5,-1,-1).multiply(shear);
    final Matrix4x4d shiftedInverse=unshear.multiply(new Matrix4x4d().translation(1.5,1,1));
    expectEq(AffinePlacementStatus.Valid, a.setAffinePlacement(shear,unshear));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(shifted,shiftedInverse));
    expectEq(AffinePlacementStatus.Valid, nearby.setAffinePlacement(
        new Matrix4x4d().translation(-1.500001,-1,-1).multiply(shear),
        unshear.multiply(new Matrix4x4d().translation(1.500001,1,1))));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
}

    @Test
    void CsgSolidEquivalenceContract_ObliquePlanesSupportTranslatedReflectedAndShearedFrames()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody quadric = new SimpleBody();
    SimpleBody opposite = new SimpleBody();
    a.setGeometry(new PlanarHalfSpace(1,2,0,-3));
    b.setGeometry(new PlanarHalfSpace(-1,2.5,0,-2));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(),new Vector3Dd(),new Vector3Dd(-2,5,0),-4));
    opposite.setGeometry(new PlanarHalfSpace(1,-2.5,0,2));
    final Matrix4x4d linear=Matrix4x4d.identityMatrix().withVal(0,0,-1).withVal(0,1,0.5);
    final Matrix4x4d forward=new Matrix4x4d().translation(1,0,0).multiply(linear);
    final Matrix4x4d inverse=linear.multiply(new Matrix4x4d().translation(-1,0,0));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forward,inverse));
    expectEq(AffinePlacementStatus.Valid, quadric.setAffinePlacement(forward,inverse));
    expectEq(AffinePlacementStatus.Valid, opposite.setAffinePlacement(forward,inverse));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand qq = new CsgBodyOperand(quadric);
    CsgBodyOperand cc = new CsgBodyOperand(opposite);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa));
    expectTrue(aa.isSameSolid(qq)); expectTrue(qq.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
}

    @Test
    void CsgSolidEquivalenceContract_QuadricsAllowGeneralExactAffineReparameterization()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody c = new SimpleBody();
    SimpleBody sphere = new SimpleBody();
    SimpleBody shiftedQuadric = new SimpleBody();
    a.setGeometry(new QuadricSurface(new Vector3Dd(1,2,3),new Vector3Dd(4,5,6),new Vector3Dd(7,8,9),-10));
    // new F(-u_x + 0.5 u_y + 1,u_y,u_z) in the reflected/sheared frame.
    b.setGeometry(new QuadricSurface(new Vector3Dd(1,4.25,3),new Vector3Dd(-5,-5,8.5),new Vector3Dd(-9,16.5,14),-2));
    c.setGeometry(new QuadricSurface(new Vector3Dd(1,4.25,3),new Vector3Dd(-5,-5,8.500001),new Vector3Dd(-9,16.5,14),-2));
    final Matrix4x4d linear=Matrix4x4d.identityMatrix().withVal(0,0,-1).withVal(0,1,0.5);
    final Matrix4x4d forward=new Matrix4x4d().translation(1,0,0).multiply(linear);
    final Matrix4x4d inverse=linear.multiply(new Matrix4x4d().translation(-1,0,0));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forward,inverse));
    expectEq(AffinePlacementStatus.Valid, c.setAffinePlacement(forward,inverse));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand cc = new CsgBodyOperand(c);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
    sphere.setGeometry(new Sphere(1)); sphere.setPosition(new Vector3Dd(1,0,0));
    shiftedQuadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(-2,0,0),0));
    CsgBodyOperand ss = new CsgBodyOperand(sphere);
    CsgBodyOperand qq = new CsgBodyOperand(shiftedQuadric);
    expectTrue(ss.isSameSolid(qq)); expectTrue(qq.isSameSolid(ss));
}

    @Test
    void CsgExactQuadricContract_IndependentRationalHomogeneousTransforms()
    {
    // The statements of csg_quadric_reference.inc, read at test time.
    final java.util.List<CsgReferenceInclude.QuadricCase> cases = CsgReferenceInclude.quadricCases();
    expectEq(50, cases.size());
    for (CsgReferenceInclude.QuadricCase c : cases) {
        Matrix4x4d forward = Matrix4x4d.identityMatrix();
        for (int k=0;k<c.values.length;++k) forward = forward.withVal(c.rows[k],c.columns[k],c.values[k]);
        double[] result = new double[10];
        expectTrue(CsgExactQuadric.world(c.local,forward,result));
        for (int i=0;i<10;++i) expectDoubleEq(c.expected[i],result[i]);
    }
}
    @Test
    void CsgExactQuadricContract_RejectsUnrepresentableIntermediateAtomically()
    {
    final double[] local = {1,1,1,Double.MIN_VALUE,0,0,0,0,0,-1};
    double[] result = {11,12,13,14,15,16,17,18,19,20};
    expectFalse(CsgExactQuadric.world(local,Matrix4x4d.identityMatrix(),result));
    for (int i=0;i<10;++i) expectDoubleEq(11+i, result[i]);
}
    @Test
    void CsgPointAttributionContract_RetainsPlacedNestedDifferenceOwners()
    {
    SimpleBody outer = new SimpleBody();
    SimpleBody inner = new SimpleBody();
    SimpleBody duplicate = new SimpleBody();
    outer.setGeometry(new Sphere(2)); inner.setGeometry(new Sphere(1)); duplicate.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(inner);
    CsgBodyOperand c = new CsgBodyOperand(duplicate);
    CsgTree child = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(child.addOperand(a)); expectTrue(child.addOperand(b)); expectTrue(child.addOperand(c));
    CsgPlacedExpression placed = new CsgPlacedExpression(child);
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(
        new Matrix4x4d().translation(3,0,0).multiply(new Matrix4x4d().scale(2,1,1)),
        new Matrix4x4d().scale(0.5,1,1).multiply(new Matrix4x4d().translation(-3,0,0))));
    CsgTree root = new CsgTree(CsgOperation.UNION); expectTrue(root.addOperand(placed));
    CsgPointAttribution owners = new CsgPointAttribution();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(root,new Vector3Dd(5,0,0),0.1,owners));
    expectEq(2, owners.getCount());
    expectEq(placed, owners.getOwner(0).getOperand(0)); expectEq(b, owners.getOwner(0).getOperand(1));
    expectEq(placed, owners.getOwner(1).getOperand(0)); expectEq(c, owners.getOwner(1).getOperand(1));
    expectTrue(owners.getOwner(0).isReversed()); expectTrue(owners.getOwner(1).isReversed());
}

    @Test
    void CsgPointProofContract_EquivalentUnsupportedLeafUsesCertifiedSiblingInEitherOrder()
    {
    SimpleBody sphere = new SimpleBody(); UncertifiedTestBody quadric = new UncertifiedTestBody();
    sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1));
    CsgBodyOperand a = new CsgBodyOperand(sphere);
    CsgBodyOperand b = new CsgBodyOperand(quadric);
    expectTrue(a.isSameSolid(b)); expectEq(GeometryContainment.Unsupported, b.classifyPointCertified(new Vector3Dd(1,0,0),0.01));
    CsgTree first = new CsgTree(CsgOperation.UNION);
    CsgTree second = new CsgTree(CsgOperation.UNION);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(first.addOperand(a)); expectTrue(first.addOperand(b));
    expectTrue(second.addOperand(b)); expectTrue(second.addOperand(a));
    expectTrue(difference.addOperand(b)); expectTrue(difference.addOperand(a));
    CsgPointAttribution owners = new CsgPointAttribution();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(first,new Vector3Dd(1,0,0),0.01,owners));
    expectEq(2, owners.getCount()); expectEq(a, owners.getOwner(0).getOperand(0)); expectEq(b, owners.getOwner(1).getOperand(0));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(second,new Vector3Dd(1,0,0),0.01,owners));
    expectEq(2, owners.getCount()); expectEq(b, owners.getOwner(0).getOperand(0)); expectEq(a, owners.getOwner(1).getOperand(0));
    CsgPointProof proof = new CsgPointProof();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithProof(second,new Vector3Dd(1,0,0),0.01,proof));
    boolean found=false;
    for (long i=0;i<proof.getCount();++i) if (proof.getEvidence(i).isEquivalence()) {
        found=true;
        expectEq(a, proof.getEvidence(i).getPath().getOperand(0));
        expectEq(b, proof.getEvidence(i).getEquivalentPath().getOperand(0));
    }
    expectTrue(found);
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference,new Vector3Dd(1,0,0),0.01));
}
    @Test
    void CsgPointProofContract_DoesNotTransferAcrossDistinctOrUncertainShapes()
    {
    SimpleBody sphere = new SimpleBody(); UncertifiedTestBody quadric = new UncertifiedTestBody(); UncertifiedTestBody opposite = new UncertifiedTestBody();
    sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1.000001));
    opposite.setGeometry(new QuadricSurface(new Vector3Dd(-1,-1,-1),new Vector3Dd(),new Vector3Dd(),1));
    CsgBodyOperand a = new CsgBodyOperand(sphere);
    CsgBodyOperand b = new CsgBodyOperand(quadric);
    CsgBodyOperand c = new CsgBodyOperand(opposite);
    CsgTree first = new CsgTree(CsgOperation.UNION);
    CsgTree second = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(first.addOperand(a)); expectTrue(first.addOperand(b));
    expectTrue(second.addOperand(a)); expectTrue(second.addOperand(c));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(first,new Vector3Dd(1,0,0),0.01));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(second,new Vector3Dd(1,0,0),0.01));
    CsgTree equal = new CsgTree(CsgOperation.UNION);
    UncertifiedTestBody same = new UncertifiedTestBody(); same.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1));
    CsgBodyOperand sameOperand = new CsgBodyOperand(same); expectTrue(equal.addOperand(a)); expectTrue(equal.addOperand(sameOperand));
    expectEq(GeometryContainment.Indeterminate, CsgPointClassification.classify(equal,new Vector3Dd(1,0,0),0));
}

    @Test
    void CsgPointProofContract_EquivalentLeafRelationKeepsNestedPlacementAndDifferenceParity()
    {
    SimpleBody outer = new SimpleBody(); SimpleBody sphere = new SimpleBody(); UncertifiedTestBody quadric = new UncertifiedTestBody();
    outer.setGeometry(new Sphere(2)); sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1));
    CsgBodyOperand a = new CsgBodyOperand(outer);
    CsgBodyOperand b = new CsgBodyOperand(sphere);
    CsgBodyOperand c = new CsgBodyOperand(quadric);
    CsgTree child = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(child.addOperand(a)); expectTrue(child.addOperand(b)); expectTrue(child.addOperand(c));
    CsgPlacedExpression placed = new CsgPlacedExpression(child);
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(
        new Matrix4x4d().translation(3,0,0).multiply(new Matrix4x4d().scale(2,1,1)),
        new Matrix4x4d().scale(0.5,1,1).multiply(new Matrix4x4d().translation(-3,0,0))));
    CsgTree root = new CsgTree(CsgOperation.UNION); expectTrue(root.addOperand(placed));
    CsgPointProof proof = new CsgPointProof();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithProof(root,new Vector3Dd(5,0,0),0.1,proof));
    boolean found=false;
    for (long i=0;i<proof.getCount();++i) if (proof.getEvidence(i).isEquivalence()) {
        found=true;
        final CsgPointWitness source =proof.getEvidence(i).getPath();
        final CsgPointWitness recipient =proof.getEvidence(i).getEquivalentPath();
        expectEq(2, source.getLength()); expectEq(2, recipient.getLength());
        expectEq(placed, source.getOperand(0)); expectEq(b, source.getOperand(1));
        expectEq(placed, recipient.getOperand(0)); expectEq(c, recipient.getOperand(1));
        expectTrue(source.isReversed()); expectTrue(recipient.isReversed());
    }
    expectTrue(found);
    CsgPointAttribution owners = new CsgPointAttribution();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(root,new Vector3Dd(5,0,0),0.1,owners));
    expectEq(2, owners.getCount()); expectTrue(owners.getOwner(0).isReversed()); expectTrue(owners.getOwner(1).isReversed());
}

    @Test
    void CsgSolidEquivalenceContract_IndependentlyBuiltCompositionsPreserveBooleanSemantics()
    {
    SimpleBody first = new SimpleBody();
    SimpleBody second = new SimpleBody();
    SimpleBody third = new SimpleBody();
    first.setGeometry(new Sphere(1)); second.setGeometry(new Sphere(1)); third.setGeometry(new Sphere(1));
    second.setPosition(new Vector3Dd(4,0,0)); third.setPosition(new Vector3Dd(8,0,0));
    CsgBodyOperand a = new CsgBodyOperand(first);
    CsgBodyOperand b = new CsgBodyOperand(second);
    CsgBodyOperand c = new CsgBodyOperand(third);
    CsgTree left = new CsgTree(CsgOperation.UNION);
    CsgTree right = new CsgTree(CsgOperation.UNION);
    CsgTree wrong = new CsgTree(CsgOperation.UNION);
    expectTrue(left.addOperand(a)); expectTrue(left.addOperand(b));
    expectTrue(right.addOperand(b)); expectTrue(right.addOperand(a));
    expectTrue(wrong.addOperand(a)); expectTrue(wrong.addOperand(c));
    expectTrue(left.isSameSolid(right)); expectTrue(right.isSameSolid(left)); expectFalse(left.isSameSolid(wrong));
    CsgTree minusA = new CsgTree(CsgOperation.DIFFERENCE);
    CsgTree minusB = new CsgTree(CsgOperation.DIFFERENCE);
    CsgTree minusWrong = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(minusA.addOperand(a)); expectTrue(minusA.addOperand(b)); expectTrue(minusA.addOperand(c));
    expectTrue(minusB.addOperand(a)); expectTrue(minusB.addOperand(c)); expectTrue(minusB.addOperand(b));
    expectTrue(minusWrong.addOperand(b)); expectTrue(minusWrong.addOperand(a)); expectTrue(minusWrong.addOperand(c));
    expectTrue(minusA.isSameSolid(minusB)); expectFalse(minusA.isSameSolid(minusWrong));
    CsgPlacedExpression placedA = new CsgPlacedExpression(left);
    CsgPlacedExpression placedB = new CsgPlacedExpression(right);
    CsgPlacedExpression shifted = new CsgPlacedExpression(right);
    final Matrix4x4d p=new Matrix4x4d().translation(2,0,0), inv=new Matrix4x4d().translation(-2,0,0);
    expectEq(AffinePlacementStatus.Valid, placedA.setAffinePlacement(p,inv));
    expectEq(AffinePlacementStatus.Valid, placedB.setAffinePlacement(p,inv));
    expectEq(AffinePlacementStatus.Valid, shifted.setAffinePlacement(new Matrix4x4d().translation(3,0,0),new Matrix4x4d().translation(-3,0,0)));
    expectTrue(placedA.isSameSolid(placedB)); expectFalse(placedA.isSameSolid(shifted));
    CsgTree composed = new CsgTree(CsgOperation.UNION); expectTrue(composed.addOperand(left)); expectTrue(composed.addOperand(right));
    CsgPointAttribution owners = new CsgPointAttribution();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(composed,new Vector3Dd(1,0,0),0.01,owners));
    expectEq(2, owners.getCount());
    expectEq(left, owners.getOwner(0).getOperand(0)); expectEq(a, owners.getOwner(0).getOperand(1));
    expectEq(right, owners.getOwner(1).getOperand(0)); expectEq(a, owners.getOwner(1).getOperand(1));
}

    @Test
    void CsgSolidEquivalenceContract_DoesNotDeclareCyclicExpressionsEquivalent()
    {
    CsgCycleTestOperand a = new CsgCycleTestOperand();
    CsgCycleTestOperand b = new CsgCycleTestOperand();
    CsgPlacedExpression first = new CsgPlacedExpression(a);
    CsgPlacedExpression second = new CsgPlacedExpression(b);
    expectFalse(first.isSameSolid(second)); expectFalse(second.isSameSolid(first));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(first));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(root,new Vector3Dd(),0.1));
}

    @Test
    void CsgSolidEquivalenceContract_DoesNotIgnoreUnknownScenePolicy()
    {
    SimpleBody sphere = new SimpleBody(); sphere.setGeometry(new Sphere(1));
    CsgBodyOperand leaf = new CsgBodyOperand(sphere);
    CsgTree child = new CsgTree(CsgOperation.UNION); expectTrue(child.addOperand(leaf));
    CsgPolicyTestOperand normal = new CsgPolicyTestOperand(child,false);
    CsgPolicyTestOperand inverted = new CsgPolicyTestOperand(child,true);
    CsgTree first = new CsgTree(CsgOperation.UNION);
    CsgTree second = new CsgTree(CsgOperation.UNION);
    expectTrue(first.addOperand(normal)); expectTrue(second.addOperand(inverted));
    expectFalse(first.isSameSolid(second)); expectFalse(second.isSameSolid(first));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(first,new Vector3Dd(),0.1));
    CsgRayLeafBatch leaves = new CsgRayLeafBatch();
    expectEq(RaySolidQueryStatus.Unsupported, CsgRayLeafQuery.query(first,
        Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,10),leaves));
    expectEq(0, leaves.size());
}

    @Test
    void CsgSolidEquivalenceContract_SceneSubclassMustOptIntoStructuralTraversal()
    {
    SimpleBody sphere = new SimpleBody(); sphere.setGeometry(new Sphere(1)); CsgBodyOperand leaf = new CsgBodyOperand(sphere);
    CsgPolicyTestTree custom = new CsgPolicyTestTree(); expectTrue(custom.addOperand(leaf));
    CsgTree root = new CsgTree(CsgOperation.UNION); expectTrue(root.addOperand(custom));
    expectFalse(custom.isStructuralTraversalSafe());
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(root,new Vector3Dd(),0.1));
    CsgRayLeafBatch leaves = new CsgRayLeafBatch();
    expectEq(RaySolidQueryStatus.Unsupported, CsgRayLeafQuery.query(root,
        Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,10),leaves));
}

    @Test
    void CsgSolidEquivalenceContract_OpaqueExpressionMustOptIntoTraversal()
    {
    SimpleBody sphere = new SimpleBody(); sphere.setGeometry(new Sphere(1)); CsgBodyOperand leaf = new CsgBodyOperand(sphere);
    CsgOpaqueTestExpression opaque = new CsgOpaqueTestExpression(leaf); CsgPlacedExpression wrapped = new CsgPlacedExpression(opaque);
    CsgTree root = new CsgTree(CsgOperation.UNION); expectTrue(root.addOperand(wrapped));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(root,new Vector3Dd(),0.1));
    CsgRayLeafBatch leaves = new CsgRayLeafBatch();
    expectEq(RaySolidQueryStatus.Unsupported, CsgRayLeafQuery.query(root,
        Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,10),leaves));
    expectEq(0, leaves.size());
}

    @Test
    void CsgSolidEquivalenceContract_AssociatesAndDeduplicatesBooleanSolidSets()
    {
    SimpleBody one = new SimpleBody();
    SimpleBody two = new SimpleBody();
    SimpleBody three = new SimpleBody();
    one.setGeometry(new Sphere(1)); two.setGeometry(new Sphere(1)); three.setGeometry(new Sphere(1));
    two.setPosition(new Vector3Dd(4,0,0)); three.setPosition(new Vector3Dd(8,0,0));
    CsgBodyOperand a = new CsgBodyOperand(one);
    CsgBodyOperand b = new CsgBodyOperand(two);
    CsgBodyOperand c = new CsgBodyOperand(three);
    CsgTree inner = new CsgTree(CsgOperation.UNION);
    CsgTree flat = new CsgTree(CsgOperation.UNION);
    CsgTree nested = new CsgTree(CsgOperation.UNION);
    expectTrue(inner.addOperand(c)); expectTrue(inner.addOperand(b));
    expectTrue(flat.addOperand(a)); expectTrue(flat.addOperand(b)); expectTrue(flat.addOperand(c));
    expectTrue(nested.addOperand(inner)); expectTrue(nested.addOperand(a)); expectTrue(nested.addOperand(a));
    expectTrue(flat.isSameSolid(nested)); expectTrue(nested.isSameSolid(flat));
    CsgTree innerI = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree flatI = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree nestedI = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(innerI.addOperand(c)); expectTrue(innerI.addOperand(b));
    expectTrue(flatI.addOperand(a)); expectTrue(flatI.addOperand(b)); expectTrue(flatI.addOperand(c));
    expectTrue(nestedI.addOperand(innerI)); expectTrue(nestedI.addOperand(a)); expectTrue(nestedI.addOperand(a));
    expectTrue(flatI.isSameSolid(nestedI));
    CsgTree minusFlat = new CsgTree(CsgOperation.DIFFERENCE);
    CsgTree minusUnion = new CsgTree(CsgOperation.DIFFERENCE);
    CsgTree wrong = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(minusFlat.addOperand(a)); expectTrue(minusFlat.addOperand(b)); expectTrue(minusFlat.addOperand(c));
    expectTrue(minusUnion.addOperand(a)); expectTrue(minusUnion.addOperand(inner));
    expectTrue(wrong.addOperand(b)); expectTrue(wrong.addOperand(a)); expectTrue(wrong.addOperand(c));
    expectTrue(minusFlat.isSameSolid(minusUnion)); expectFalse(minusFlat.isSameSolid(wrong));
    CsgPlacedExpression translated = new CsgPlacedExpression(inner);
    expectEq(AffinePlacementStatus.Valid, translated.setAffinePlacement(new Matrix4x4d().translation(1,0,0),new Matrix4x4d().translation(-1,0,0)));
    CsgTree different = new CsgTree(CsgOperation.UNION); expectTrue(different.addOperand(a)); expectTrue(different.addOperand(translated));
    expectFalse(flat.isSameSolid(different));
}

    @Test
    void CsgPointProofContract_EquivalentCompositeCanReuseProofWithoutInventingOwners()
    {
    SimpleBody sphere = new SimpleBody(); UncertifiedTestBody quadric = new UncertifiedTestBody();
    sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1));
    CsgBodyOperand a = new CsgBodyOperand(sphere);
    CsgBodyOperand b = new CsgBodyOperand(quadric);
    CsgTree known = new CsgTree(CsgOperation.UNION);
    CsgTree unknown = new CsgTree(CsgOperation.UNION);
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(known.addOperand(a)); expectTrue(unknown.addOperand(b));
    expectTrue(root.addOperand(unknown)); expectTrue(root.addOperand(known));
    expectTrue(known.isSameSolid(unknown));
    CsgPointProof proof = new CsgPointProof();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithProof(root,new Vector3Dd(1,0,0),0.01,proof));
    expectTrue(proof.isAvailable());
    boolean relation=false;
    for (long i=0;i<proof.getCount();++i) if (proof.getEvidence(i).isEquivalence()) {
        final CsgPointEvidence fact=proof.getEvidence(i);
        if (fact.getPath().getOperand(0)==known && fact.getEquivalentPath().getOperand(0)==unknown) relation=true;
    }
    expectTrue(relation);
    CsgPointAttribution owners = new CsgPointAttribution();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithAttribution(root,new Vector3Dd(1,0,0),0.01,owners));
    expectFalse(owners.isComplete());
    CsgPointWitness witness = new CsgPointWitness();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithWitness(root,new Vector3Dd(1,0,0),0.01,witness));
    expectEq(2, witness.getLength()); expectEq(known, witness.getOperand(0)); expectEq(a, witness.getOperand(1));
}

    @Test
    void CsgDyadicContract_PreservesExtremeTermsCancellationAndNonRepresentableProducts()
    {
    CsgDyadic a = new CsgDyadic();
    CsgDyadic b = new CsgDyadic();
    CsgDyadic c = new CsgDyadic();
    CsgDyadic d = new CsgDyadic();
    CsgDyadic product = new CsgDyadic();
    CsgDyadic rounded = new CsgDyadic();
    CsgDyadic sum = new CsgDyadic();
    CsgDyadic recovered = new CsgDyadic();
    expectTrue(CsgDyadic.fromDouble(0.1,a)); expectTrue(CsgDyadic.fromDouble(0.1,b));
    expectTrue(CsgDyadic.multiply(a,b,product)); expectTrue(CsgDyadic.fromDouble(0.01,rounded));
    expectFalse(product.equals(rounded));
    expectTrue(CsgDyadic.fromDouble(Double.MAX_VALUE,a));
    expectTrue(CsgDyadic.fromDouble(Double.MIN_VALUE,b));
    expectTrue(CsgDyadic.add(a,b,sum)); expectTrue(CsgDyadic.subtract(sum,a,recovered)); expectTrue(recovered.equals(b));
    expectTrue(CsgDyadic.fromDouble(-0.125,c)); expectTrue(CsgDyadic.fromDouble(0.75,d));
    CsgDyadic ab = new CsgDyadic();
    CsgDyadic ac = new CsgDyadic();
    CsgDyadic ad = new CsgDyadic();
    CsgDyadic lhs = new CsgDyadic();
    CsgDyadic bc = new CsgDyadic();
    CsgDyadic rhs = new CsgDyadic();
    expectTrue(CsgDyadic.add(b,c,bc)); expectTrue(CsgDyadic.multiply(a,bc,lhs));
    expectTrue(CsgDyadic.multiply(a,b,ab)); expectTrue(CsgDyadic.multiply(a,c,ac));
    expectTrue(CsgDyadic.add(ab,ac,rhs)); expectTrue(lhs.equals(rhs));
    expectTrue(CsgDyadic.multiply(c,d,ad)); expectTrue(CsgDyadic.subtract(ad,ad,recovered)); expectEq(0, recovered.getSign());
}
    @Test
    void CsgSolidEquivalenceContract_DyadicCertificatesHandleUnrepresentableAffineIntermediates()
    {
    SimpleBody planeA = new SimpleBody();
    SimpleBody planeB = new SimpleBody();
    SimpleBody quadricA = new SimpleBody();
    SimpleBody quadricB = new SimpleBody();
    SimpleBody sphere = new SimpleBody();
    SimpleBody translatedQuadric = new SimpleBody();
    SimpleBody nearby = new SimpleBody();
    SimpleBody nearbyQuadric = new SimpleBody();
    planeA.setGeometry(new PlanarHalfSpace(0,1,0,-1));
    planeB.setGeometry(new PlanarHalfSpace(0,1,0.1,-1));
    quadricA.setGeometry(new QuadricSurface(new Vector3Dd(),new Vector3Dd(0,0,1),new Vector3Dd(),0));
    quadricB.setGeometry(new QuadricSurface(new Vector3Dd(0,0,0.1),new Vector3Dd(0,0,1),new Vector3Dd(),0));
    nearbyQuadric.setGeometry(new QuadricSurface(new Vector3Dd(0,0,Math.nextAfter(0.1,1)),new Vector3Dd(0,0,1),new Vector3Dd(),0));
    sphere.setGeometry(new Sphere(1));
    translatedQuadric.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(0,0,2),0));
    nearby.setGeometry(new PlanarHalfSpace(0,1,Math.nextAfter(0.1,1),-1));
    final Matrix4x4d linear=Matrix4x4d.identityMatrix().withVal(0,1,0.1).withVal(1,2,0.1);
    final Matrix4x4d inverse=Matrix4x4d.identityMatrix().withVal(0,1,-0.1).withVal(0,2,0.01).withVal(1,2,-0.1);
    expectEq(AffinePlacementStatus.Valid, planeB.setAffinePlacement(linear,inverse));
    expectEq(AffinePlacementStatus.Valid, quadricB.setAffinePlacement(linear,inverse));
    expectEq(AffinePlacementStatus.Valid, nearbyQuadric.setAffinePlacement(linear,inverse));
    expectEq(AffinePlacementStatus.Valid, nearby.setAffinePlacement(linear,inverse));
    expectEq(AffinePlacementStatus.Valid, sphere.setAffinePlacement(linear,inverse));
    final Matrix4x4d shifted=new Matrix4x4d().translation(0,0.1,1).multiply(linear);
    final Matrix4x4d shiftedInverse=inverse.multiply(new Matrix4x4d().translation(0,-0.1,-1));
    expectEq(AffinePlacementStatus.Valid, translatedQuadric.setAffinePlacement(shifted,shiftedInverse));
    CsgBodyOperand pa = new CsgBodyOperand(planeA);
    CsgBodyOperand pb = new CsgBodyOperand(planeB);
    CsgBodyOperand qa = new CsgBodyOperand(quadricA);
    CsgBodyOperand qb = new CsgBodyOperand(quadricB);
    CsgBodyOperand s = new CsgBodyOperand(sphere);
    CsgBodyOperand q = new CsgBodyOperand(translatedQuadric);
    CsgBodyOperand n = new CsgBodyOperand(nearby);
    CsgBodyOperand nq = new CsgBodyOperand(nearbyQuadric);
    expectTrue(pa.isSameSolid(pb)); expectTrue(pb.isSameSolid(pa)); expectFalse(pa.isSameSolid(n));
    expectTrue(qa.isSameSolid(qb)); expectTrue(qb.isSameSolid(qa)); expectFalse(qa.isSameSolid(nq));
    expectTrue(s.isSameSolid(q)); expectTrue(q.isSameSolid(s));
}

    @Test
    void CsgDyadicContract_IndependentRationalMultiProductFixtures()
    {
    // The tables of csg_dyadic_reference.inc are read at test time; the loop
    // below is the code of that file.
    final CsgReferenceInclude.DyadicTables tables = CsgReferenceInclude.dyadicTables();
    final double[][] samples = tables.samples;
    final boolean[] expected = tables.expected;
    expectEq(300, samples.length); expectEq(300, expected.length);
    for (int caseIndex=0;caseIndex<300;++caseIndex) {
        CsgDyadic[] x = CsgDyadic.newArray(8);
        CsgDyadic l1 = new CsgDyadic(), l2 = new CsgDyadic(), r1 = new CsgDyadic(), r2 = new CsgDyadic(), l = new CsgDyadic(), r = new CsgDyadic();
        for(int i=0;i<8;++i) expectTrue(CsgDyadic.fromDouble(samples[caseIndex][i],x[i]));
        expectTrue(CsgDyadic.multiply(x[0],x[1],l1)); expectTrue(CsgDyadic.multiply(x[2],x[3],l2));
        expectTrue(CsgDyadic.multiply(x[4],x[5],r1)); expectTrue(CsgDyadic.multiply(x[6],x[7],r2));
        expectTrue(CsgDyadic.add(l1,l2,l)); expectTrue(CsgDyadic.add(r1,r2,r));
        expectEq(expected[caseIndex],l.equals(r));
        CsgDyadic combinedLeft = new CsgDyadic(), combinedRight = new CsgDyadic(), p00 = new CsgDyadic(), p01 = new CsgDyadic(),
            p10 = new CsgDyadic(), p11 = new CsgDyadic(), t0 = new CsgDyadic(), t1 = new CsgDyadic();
        expectTrue(CsgDyadic.multiply(l,r,combinedLeft));
        expectTrue(CsgDyadic.multiply(l1,r1,p00)); expectTrue(CsgDyadic.multiply(l1,r2,p01));
        expectTrue(CsgDyadic.multiply(l2,r1,p10)); expectTrue(CsgDyadic.multiply(l2,r2,p11));
        expectTrue(CsgDyadic.add(p00,p01,t0)); expectTrue(CsgDyadic.add(p10,p11,t1));
        expectTrue(CsgDyadic.add(t0,t1,combinedRight)); expectTrue(combinedLeft.equals(combinedRight));
    }
}

    @Test
    void CsgDyadicContract_CapacityAndRoundingFailuresAreAtomic()
    {
    CsgDyadic high = new CsgDyadic();
    CsgDyadic one = new CsgDyadic();
    CsgDyadic sentinel = new CsgDyadic();
    CsgDyadic result = new CsgDyadic();
    expectTrue(CsgDyadic.fromDouble(2,high)); expectTrue(CsgDyadic.fromDouble(1,one));
    expectTrue(CsgDyadic.fromDouble(17,sentinel));
    for (int i=0;i<16;++i) expectTrue(CsgDyadic.multiply(high,high,high));
    result=sentinel;
    expectFalse(CsgDyadic.add(high,one,result)); expectTrue(result.equals(sentinel));
    CsgDyadic dense = new CsgDyadic();
    CsgDyadic square = new CsgDyadic();
    expectTrue(CsgDyadic.fromDouble(2,high));
    for (int i=0;i<14;++i) expectTrue(CsgDyadic.multiply(high,high,high));
    expectTrue(CsgDyadic.add(high,one,dense));
    expectTrue(CsgDyadic.multiply(dense,dense,square));
    expectFalse(CsgDyadic.multiply(square,square,result)); expectTrue(result.equals(sentinel));
    // Java floating point always rounds to nearest and cannot change the
    // rounding mode, which is the cpp case where fesetround fails: skipped.
    assumeTrue(false, "Rounding mode cannot be changed");
    final boolean success=CsgDyadic.fromDouble(1.25,result);
    expectFalse(success); expectTrue(result.equals(sentinel));
}

    @Test
    void CsgSolidEquivalenceContract_DyadicGramHandlesRoundedRadiusProducts()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody nearby = new SimpleBody();
    a.setGeometry(new Sphere(0.1)); b.setGeometry(new Sphere(0.1));
    nearby.setGeometry(new Sphere(Math.nextAfter(0.1,1)));
    final Matrix4x4d forwardA=new Matrix4x4d().scale(5,5,5), inverseA=new Matrix4x4d().scale(0.2,0.2,0.2);
    final Matrix4x4d forwardB=Matrix4x4d.identityMatrix().withVal(0,0,3).withVal(0,1,-4)
        .withVal(1,0,4).withVal(1,1,3).withVal(2,2,5);
    final Matrix4x4d inverseB=Matrix4x4d.identityMatrix().withVal(0,0,0.12).withVal(0,1,0.16)
        .withVal(1,0,-0.16).withVal(1,1,0.12).withVal(2,2,0.2);
    expectEq(AffinePlacementStatus.Valid, a.setAffinePlacement(forwardA,inverseA));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forwardB,inverseB));
    expectEq(AffinePlacementStatus.Valid, nearby.setAffinePlacement(forwardB,inverseB));
    CsgBodyOperand aa = new CsgBodyOperand(a);
    CsgBodyOperand bb = new CsgBodyOperand(b);
    CsgBodyOperand cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb)); expectTrue(bb.isSameSolid(aa)); expectFalse(aa.isSameSolid(cc));
}
}
