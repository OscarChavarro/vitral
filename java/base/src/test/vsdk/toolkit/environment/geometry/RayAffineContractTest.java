package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.environment.scene.csg.CsgTestOperand;
import vsdk.toolkit.environment.scene.csg.CsgPointClassification;
import vsdk.toolkit.environment.scene.csg.CsgOperation;
import vsdk.toolkit.environment.scene.csg.CsgTree;
import vsdk.toolkit.environment.scene.csg.CsgBodyOperand;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.numerics.FloatingInterval;
import vsdk.toolkit.environment.geometry.element.*;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.*;
import vsdk.toolkit.environment.scene.AffinePlacement;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SimpleBody;

import static vsdk.toolkit.GTestAssertions.*;

/**
Port of `cpp/base/src/test/ray_affine_contract_tests.cpp`; each test method
is named `<cpp suite>_<cpp test>`.
*/
class RayAffineContractTest
{

    @Test
    void FloatingIntervalContract_OutwardRoundingAndFailureCases()
    {
    expectTrue(FloatingInterval.isSupportedEnvironment());
    final FloatingInterval sum=FloatingInterval.exact(0.1).add(FloatingInterval.exact(0.2));
    expectTrue(sum.isValid());
    expectLe(sum.getLower(), 0.3);
    expectGe(sum.getUpper(), 0.3);
    final FloatingInterval quotient=FloatingInterval.exact(1).divide(FloatingInterval.exact(3));
    expectTrue(quotient.isValid());
    expectLt(quotient.getLower(), 1.0/3.0);
    expectGt(quotient.getUpper(), 1.0/3.0);
    expectFalse(FloatingInterval.exact(1).divide(FloatingInterval.exact(0)).isValid());
    expectFalse(FloatingInterval.exact(Double.MAX_VALUE).multiply(FloatingInterval.exact(2)).isValid());
    final FloatingInterval tiny=FloatingInterval.exact(Double.MIN_VALUE).multiply(FloatingInterval.exact(0.5));
    expectTrue(tiny.isValid());
    expectLe(tiny.getLower(), 0);
    final double minValue=Double.MIN_VALUE;
    expectGe(tiny.getUpper(), minValue);
    // Not ported: the cpp test switches the rounding mode with fesetround and
    // expects the certified queries to refuse it; Java arithmetic is always
    // round-to-nearest, so a directed rounding mode cannot be set.
}

    @Test
    void FloatingIntervalContract_BoundsContainIndependentExactRationalResults()
    {
    expectTrue(FloatingInterval.isSupportedEnvironment());
    // Selected floor and ceiling endpoints from
    // generate_interval_reference.py, using exact rational arithmetic.
    final double[][] cases = {
        {0.1,0.2,0,0.3,0.30000000000000004},
        {0.1,0.2,2,0.02,0.020000000000000004},
        {-0.1,0.2,1,-0.30000000000000004,-0.3},
        {1e-300,1e-20,0,1e-20,1.0000000000000001e-20},
        {1e-300,1e-20,2,1e-320,1.0005e-320},
        {1e-300,1e-20,3,1.0000000000000001e-280,1.0000000000000002e-280},
        {1e150,1e140,1,9.999999998999999e149,9.999999999e149},
        {1e150,1e140,2,9.999999999999999e289,1e290},
        {1.0000000000000002,0.9999999999999999,1,3.3306690738754696e-16,3.3306690738754696e-16},
        {1.0000000000000002,0.9999999999999999,3,1.0000000000000002,1.0000000000000004},
        {-1e-200,-1e-110,2,1e-310,1.00000000000005e-310},
        {1,3,3,0.3333333333333333,0.33333333333333337},
        {1e-200,1e100,3,9.999999999999999e-301,1e-300}
    };
    for (int i=0;i<13;++i) {
        final FloatingInterval a=FloatingInterval.exact(cases[i][0]);
        final FloatingInterval b=FloatingInterval.exact(cases[i][1]);
        FloatingInterval result = new FloatingInterval();
        switch ((int)(cases[i][2])) {
        case 0: result=a.add(b); break;
        case 1: result=a.subtract(b); break;
        case 2: result=a.multiply(b); break;
        default: result=a.divide(b); break;
        }
        expectTrue(result.isValid(), "" + i);
        expectLe(result.getLower(), cases[i][3], "" + i);
        expectGe(result.getUpper(), cases[i][4], "" + i);
    }
}

    @Test
    void EllipsoidDistanceBoundsContract_AxisAndShearEncloseKnownDistances()
    {
    double[] lower = {-1};
    double[] upper = {-1};
    GeometryContainment[] side = {GeometryContainment.Indeterminate};
    final Matrix4x4d identity=Matrix4x4d.identityMatrix();
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistanceBounds.query(
        new Vector3Dd(3,0,0),1,identity,new Vector3Dd(1,0,0),side,lower,upper));
    expectEq(GeometryContainment.Outside, side[0]);
    expectLe(lower[0], 2); expectGe(upper[0], 2);
    expectNear(2, lower[0], 1e-12); expectNear(2, upper[0], 1e-12);
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistanceBounds.query(
        new Vector3Dd(),1,identity,new Vector3Dd(0,1,0),side,lower,upper));
    expectEq(GeometryContainment.Inside, side[0]);
    expectGt(lower[0], 0); expectLe(lower[0], 1); expectGe(upper[0], 1); expectNear(1, upper[0], 1e-12);
    final Matrix4x4d shear=new Matrix4x4d().translation(3,-2,1).multiply(
        Matrix4x4d.identityMatrix().withVal(0,0,2).withVal(0,1,0.5));
    final Vector3Dd seed = new Vector3Dd(0.6,0.8,0);
    final Vector3Dd normal = new Vector3Dd(0.3,0.65,0);
    final Vector3Dd point=shear.multiply(seed).add(normal.multiply(0.2/normal.length()));
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistanceBounds.query(point,1,shear,seed,side,lower,upper));
    expectEq(GeometryContainment.Outside, side[0]);
    expectLe(lower[0], 0.2+1e-12); expectGe(upper[0], 0.2-1e-12);
    expectNear(0.2, upper[0], 1e-10);
}

    @Test
    void EllipsoidDistanceBoundsContract_InvalidUncertainAndRoundingModeKeepOutputs()
    {
    double[] lower = {17};
    double[] upper = {23};
    GeometryContainment[] side = {GeometryContainment.Boundary};
    expectEq(GeometryDistanceStatus.InvalidInput, AffineEllipsoidDistanceBounds.query(
        new Vector3Dd(),1,new Matrix4x4d().scale(0,1,1),new Vector3Dd(1,0,0),side,lower,upper));
    expectEq(GeometryDistanceStatus.InvalidInput, AffineEllipsoidDistanceBounds.query(
        new Vector3Dd(),1,new Matrix4x4d(),new Vector3Dd(),side,lower,upper));
    expectDoubleEq(17, lower[0]); expectDoubleEq(23, upper[0]);
    expectEq(GeometryContainment.Boundary, side[0]);
    final Matrix4x4d ill=Matrix4x4d.identityMatrix().withVal(0,1,1).withVal(1,1,1e-18);
    final GeometryDistanceStatus status=AffineEllipsoidDistanceBounds.query(
        new Vector3Dd(2,0,0),1,ill,new Vector3Dd(1,0,0),side,lower,upper);
    expectTrue(status==GeometryDistanceStatus.Indeterminate || status==GeometryDistanceStatus.Valid);
    if (status==GeometryDistanceStatus.Indeterminate) { expectDoubleEq(17, lower[0]); expectDoubleEq(23, upper[0]); }
    if (status==GeometryDistanceStatus.Valid) {
        final double distance=2-Math.sqrt(2.0);
        expectLe(lower[0], distance+1e-12); expectGe(upper[0], distance-1e-12);
    }
    lower[0]=17; upper[0]=23;
    side[0]=GeometryContainment.Boundary;
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    // Not ported: the cpp test switches the rounding mode with fesetround and
    // expects the certified queries to refuse it; Java arithmetic is always
    // round-to-nearest, so a directed rounding mode cannot be set.
}

    @Test
    void EllipsoidDistanceBoundsContract_SceneExposesCertifiedEnclosuresAndContainment()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3,2,1));
    GeometryContainment[] side = {GeometryContainment.Indeterminate};
    double[] lower = {0};
    double[] upper = {0};
    expectEq(GeometryDistanceStatus.Valid, body.encloseBoundaryDistance(
        new Vector3Dd(3.2,0,0),new Vector3Dd(1,0,0),side,lower,upper));
    expectEq(GeometryContainment.Outside, side[0]);
    expectLe(lower[0], 0.2+1e-14); expectGe(upper[0], 0.2-1e-14);
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(),0.1));
    // World distance is 0.2; the plain inverse-norm bound is below 0.14.
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(2.8,0,0),0.14));
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(3.2,0,0),0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(3.05,0,0),0.1));
    expectEq(GeometryContainment.Indeterminate, body.classifyPointCertified(new Vector3Dd(3.1,0,0),0.1));
    expectEq(GeometryContainment.InvalidInput, body.classifyPointCertified(new Vector3Dd(),-1));
    Box box = new Box(1,1,1);
    body.setGeometryReference(box);
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(),0.1));
    Cone cone = new Cone(1,1,1);
    body.setGeometryReference(cone);
    expectEq(GeometryContainment.Unsupported, body.classifyPointCertified(new Vector3Dd(),0.1));
}

    @Test
    void EllipsoidDistanceBoundsContract_IndependentReferencesStayInsideCertifiedBounds()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d inverseRotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,1)
        .withVal(1,0,-1).withVal(1,1,0);
    final Matrix4x4d shape=Matrix4x4d.identityMatrix().withVal(0,0,-2).withVal(0,1,0.5).withVal(1,2,-0.3);
    final Matrix4x4d inverseShape=Matrix4x4d.identityMatrix().withVal(0,0,-0.5)
        .withVal(0,1,0.25).withVal(0,2,0.075).withVal(1,2,0.3);
    final double[][] samples = {{0.12,0.27,0.31,0.484219147169112},
        {0.65,0.1,-0.2,0.437976391530554},{1.1,0.2,0.3,0.228896833955284},
        {0.1,-1.2,0.5,0.341598824284725},{-0.9,-0.4,-0.3,0.0336629761551959},
        {0,0,0,0.84234037196832}};
    final double[] factors = {0.01,1,100};
    int definite=0;
    for (int k=0;k<3;++k) {
        final double factor=factors[k];
        final Matrix4x4d forward=new Matrix4x4d().translation(4,-3,2).multiply(rotation)
            .multiply(new Matrix4x4d().scale(factor,factor,factor)).multiply(shape);
        final Matrix4x4d inverse=inverseShape.multiply(new Matrix4x4d().scale(1/factor,1/factor,1/factor))
            .multiply(inverseRotation).multiply(new Matrix4x4d().translation(-4,3,-2));
        expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
        for (int index=0;index<6;++index) {
            final Vector3Dd local = new Vector3Dd(samples[index][0],samples[index][1],samples[index][2]);
            final Vector3Dd point=forward.multiply(local);
            final Vector3Dd seed=local.dotProduct(local)>0 ? local : new Vector3Dd(1,0,0);
            GeometryContainment[] side = {GeometryContainment.Indeterminate};
            double[] lower = {0};
            double[] upper = {0};
            expectEq(GeometryDistanceStatus.Valid, body.encloseBoundaryDistance(point,seed,side,lower,upper), "" + k + ", " + index);
            final double reference=samples[index][3]*factor;
            expectLe(lower[0], reference+1e-9*factor, "" + k + ", " + index);
            expectGe(upper[0], reference-1e-9*factor, "" + k + ", " + index);
            final GeometryContainment expected=local.dotProduct(local)<1
                ? GeometryContainment.Inside : GeometryContainment.Outside;
            expectTrue(side[0]==expected || side[0]==GeometryContainment.Indeterminate);
            final double nearTolerance=reference+1e-4*factor;
            final GeometryContainment near=body.classifyPointCertified(point,nearTolerance);
            expectTrue(near==GeometryContainment.Boundary || near==GeometryContainment.Indeterminate);
            expectEq(near, body.classifyPoint(point,nearTolerance));
            final double farTolerance=reference*0.25;
            final GeometryContainment far=body.classifyPointCertified(point,farTolerance);
            expectTrue(far==expected || far==GeometryContainment.Indeterminate);
            expectEq(far, body.classifyPoint(point,farTolerance));
            if (near==GeometryContainment.Boundary) ++definite;
            if (far==expected) ++definite;
        }
    }
    expectEq(36, definite);
}

    @Test
    void EllipsoidDistanceBoundsContract_DiverseAffineFixturesEncloseIndependentDistances()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d[] matrices = {
        Matrix4x4d.identityMatrix().withVal(0,1,0.9).withVal(0,2,0.2).withVal(1,2,0.6),
        Matrix4x4d.identityMatrix().withVal(0,0,0.2).withVal(1,1,3).withVal(1,2,0.4),
        Matrix4x4d.identityMatrix().withVal(0,1,0.8).withVal(0,2,0.4).withVal(1,0,0.3)
            .withVal(1,2,-0.2).withVal(2,0,0.1).withVal(2,1,0.5).withVal(2,2,1.2)
    };
    final Vector3Dd[] points = {new Vector3Dd(),new Vector3Dd(0.1,0.3,-0.2),new Vector3Dd(0.6,-0.2,0.1),
        new Vector3Dd(0.3,0.6,0.2),new Vector3Dd(1.2,0.3,-0.1),new Vector3Dd(-0.4,-1.1,0.6),
        new Vector3Dd(0.2,0.1,1.4),new Vector3Dd(-0.9,0.3,-0.4)};
    // Generated independently by generate_certified_bounds_reference.py.
    final double[][] expected = {
        {0.578406438716408,0.436864249515077,0.21684579558424,0.342292911715945,
         0.222512940034161,0.318320672587438,0.453659506464624,0.0181298663812327},
        {0.2,0.166279867729029,0.0749031414139726,0.0947459615938375,
         0.0502305824969421,0.439324293232699,0.413291190037398,0.00675839616866524},
        {0.526534747238471,0.427659995509968,0.195778365340438,0.309070492537248,
         0.199911925624104,0.356476528572591,0.522022480951273,0.0172612127525319}
    };
    int verifiedSides=0,definite=0;
    for (int matrix=0;matrix<3;++matrix) {
      expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(matrices[matrix],matrices[matrix].inverse()));
      for (int index=0;index<8;++index) {
        final Vector3Dd point=matrices[matrix].multiply(points[index]);
        final Vector3Dd seed=points[index].dotProduct(points[index])>0 ? points[index] : new Vector3Dd(1,0,0);
        final GeometryContainment expectedSide=points[index].dotProduct(points[index])<1
            ? GeometryContainment.Inside : GeometryContainment.Outside;
        GeometryContainment[] side = {GeometryContainment.Indeterminate};
        double[] lower = {0};
        double[] upper = {0};
        expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistanceBounds.query(
            point,1,matrices[matrix],seed,side,lower,upper), "" + matrix + ", " + index);
        expectLe(lower[0], expected[matrix][index]+1e-8, "" + matrix + ", " + index);
        expectGe(upper[0], expected[matrix][index]-1e-8, "" + matrix + ", " + index);
        expectTrue(side[0]==expectedSide || side[0]==GeometryContainment.Indeterminate);
        if (side[0]==expectedSide) ++verifiedSides;
        final GeometryContainment near=body.classifyPointCertified(point,expected[matrix][index]+1e-4);
        expectTrue(near==GeometryContainment.Boundary || near==GeometryContainment.Indeterminate);
        if (near==GeometryContainment.Boundary) ++definite;
        final GeometryContainment far=body.classifyPointCertified(point,expected[matrix][index]*0.25);
        expectTrue(far==expectedSide || far==GeometryContainment.Indeterminate);
        if (far==expectedSide) ++definite;
        GeometryContainment[] minimumSide = {GeometryContainment.Indeterminate};
        double[] minimumLower = {0};
        double[] minimumUpper = {0};
        expectEq(GeometryDistanceStatus.Valid, body.encloseMinimumBoundaryDistance(
            point,1e-5,minimumSide,minimumLower,minimumUpper), "" + matrix + ", " + index);
        expectLe(minimumLower[0], expected[matrix][index]+1e-8, "" + matrix + ", " + index);
        expectGe(minimumUpper[0], expected[matrix][index]-1e-8, "" + matrix + ", " + index);
        expectLe(minimumUpper[0]-minimumLower[0], 1e-5);
      }
    }
    expectGe(verifiedSides, 20);
    expectEq(48, definite);
}

    @Test
    void EllipsoidDistanceBoundsContract_NonUnitRadiusAndShearUseWorldTolerance()
    {
    Sphere sphere = new Sphere(2);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward=new Matrix4x4d().translation(3,-2,1).multiply(
        Matrix4x4d.identityMatrix().withVal(0,0,2).withVal(0,1,0.5));
    final Matrix4x4d inverse=Matrix4x4d.identityMatrix().withVal(0,0,0.5).withVal(0,1,-0.25)
        .multiply(new Matrix4x4d().translation(-3,2,-1));
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    final Vector3Dd source = new Vector3Dd(0.6,0.8,0);
    final Vector3Dd normal = new Vector3Dd(0.3,0.65,0);
    final Vector3Dd point=forward.multiply(source.multiply(2)).add(normal.multiply(0.2/normal.length()));
    GeometryContainment[] side = {GeometryContainment.Indeterminate};
    double[] lower = {0};
    double[] upper = {0};
    expectEq(GeometryDistanceStatus.Valid, body.encloseBoundaryDistance(point,source,side,lower,upper));
    expectEq(GeometryContainment.Outside, side[0]);
    expectLe(lower[0], 0.2+1e-12); expectGe(upper[0], 0.2-1e-12);
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(point,0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(point,0.3));
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(3,-2,1),0.1));
}

    @Test
    void EllipsoidMinimumDistanceContract_SceneProducesCertifiedTightDistances()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3,2,1));
    final Vector3Dd[] points = {new Vector3Dd(3.2,0,0),new Vector3Dd(),new Vector3Dd(1,0,0),new Vector3Dd(2.8,0,0)};
    final double[] expected = {0.2,1,Math.sqrt(0.875),0.2};
    final GeometryContainment[] sides = {GeometryContainment.Outside,GeometryContainment.Inside,
        GeometryContainment.Inside,GeometryContainment.Inside};
    for (int i=0;i<4;++i) {
        GeometryContainment[] side = {GeometryContainment.Indeterminate};
        double[] lower = {0};
        double[] upper = {0};
        expectEq(GeometryDistanceStatus.Valid, body.encloseMinimumBoundaryDistance(points[i],1e-6,side,lower,upper), "" + i);
        expectEq(sides[i], side[0]);
        expectLe(lower[0], expected[i]+1e-12); expectGe(upper[0], expected[i]-1e-12);
        expectLe(upper[0]-lower[0], 1e-6);
    }
    // The closest point to (1,0,0) is off axis; the radial direction alone
    // cannot produce a witness inside this narrow tolerance band.
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(1,0,0),Math.sqrt(0.875)+1e-7));
    GeometryContainment[] side = {GeometryContainment.Boundary};
    double[] lower = {17};
    double[] upper = {23};
    expectEq(GeometryDistanceStatus.InvalidInput, body.encloseMinimumBoundaryDistance(new Vector3Dd(3.2,0,0),0,side,lower,upper));
    expectEq(GeometryDistanceStatus.Indeterminate, body.encloseMinimumBoundaryDistance(new Vector3Dd(3.2,0,0),1e-30,side,lower,upper));
    expectEq(GeometryContainment.Boundary, side[0]); expectDoubleEq(17, lower[0]); expectDoubleEq(23, upper[0]);
}

    @Test
    void EllipsoidMinimumDistanceContract_ShearedSceneMatchesIndependentReferences()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward=Matrix4x4d.identityMatrix().withVal(0,0,-2).withVal(0,1,0.5).withVal(1,2,-0.3);
    final Matrix4x4d inverse=Matrix4x4d.identityMatrix().withVal(0,0,-0.5).withVal(0,1,0.25)
        .withVal(0,2,0.075).withVal(1,2,0.3);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    final double[][] samples = {{0.12,0.27,0.31,0.484219147169112},
        {0.65,0.1,-0.2,0.437976391530554},{1.1,0.2,0.3,0.228896833955284},
        {0.1,-1.2,0.5,0.341598824284725},{0.01,0.01,0.01,0.828855732290258},
        {0,0,0,0.84234037196832}};
    for (int i=0;i<6;++i) {
        final Vector3Dd local = new Vector3Dd(samples[i][0],samples[i][1],samples[i][2]);
        GeometryContainment[] side = {GeometryContainment.Indeterminate};
        double[] lower = {0};
        double[] upper = {0};
        expectEq(GeometryDistanceStatus.Valid, body.encloseMinimumBoundaryDistance(
            forward.multiply(local),1e-5,side,lower,upper), "" + i);
        expectLe(lower[0], samples[i][3]+1e-9); expectGe(upper[0], samples[i][3]-1e-9);
        expectLe(upper[0]-lower[0], 1e-5);
    }
}

    @Test
    void EllipsoidMinimumDistanceContract_CertifiedBoundsSurvivePlacementAndRadius()
    {
    Sphere sphere = new Sphere(2);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d inverseRotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,1)
        .withVal(1,0,-1).withVal(1,1,0);
    final Matrix4x4d shape=Matrix4x4d.identityMatrix().withVal(0,0,-2).withVal(0,1,0.5).withVal(1,2,-0.3);
    final Matrix4x4d inverseShape=Matrix4x4d.identityMatrix().withVal(0,0,-0.5)
        .withVal(0,1,0.25).withVal(0,2,0.075).withVal(1,2,0.3);
    final double[][] samples = {{0.12,0.27,0.31,0.484219147169112},
        {0.1,-1.2,0.5,0.341598824284725},{0,0,0,0.84234037196832}};
    final double[] factors = {0.01,1,100};
    for (int k=0;k<3;++k) {
        final double factor=factors[k];
        final Matrix4x4d forward=new Matrix4x4d().translation(4,-3,2).multiply(rotation)
            .multiply(new Matrix4x4d().scale(factor,factor,factor)).multiply(shape);
        final Matrix4x4d inverse=inverseShape.multiply(new Matrix4x4d().scale(1/factor,1/factor,1/factor))
            .multiply(inverseRotation).multiply(new Matrix4x4d().translation(-4,3,-2));
        expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
        for (int i=0;i<3;++i) {
            final Vector3Dd local = new Vector3Dd(samples[i][0]*2,samples[i][1]*2,samples[i][2]*2);
            GeometryContainment[] side = {GeometryContainment.Indeterminate};
            double[] lower = {0};
            double[] upper = {0};
            expectEq(GeometryDistanceStatus.Valid, body.encloseMinimumBoundaryDistance(
                forward.multiply(local),1e-5*factor,side,lower,upper), "" + k + ", " + i);
            final double expected=2*factor*samples[i][3];
            expectLe(lower[0], expected+1e-9*factor); expectGe(upper[0], expected-1e-9*factor);
            expectLe(upper[0]-lower[0], 1e-5*factor);
        }
    }
}

    @Test
    void EllipsoidMinimumDistanceContract_FailureAndBoundaryDoNotInventResults()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3,2,1));
    GeometryContainment[] side = {GeometryContainment.Inside};
    double[] lower = {17};
    double[] upper = {23};
    expectEq(GeometryDistanceStatus.InvalidInput, body.encloseMinimumBoundaryDistance(
        new Vector3Dd(),Double.NaN,side,lower,upper));
    expectEq(GeometryContainment.Inside, side[0]); expectDoubleEq(17, lower[0]); expectDoubleEq(23, upper[0]);
    expectEq(GeometryDistanceStatus.Valid, body.encloseMinimumBoundaryDistance(
        new Vector3Dd(3,0,0),1e-8,side,lower,upper));
    expectLe(lower[0], 0); expectGe(upper[0], 0); expectLe(upper[0], 1e-8);
    expectEq(GeometryContainment.Indeterminate, side[0]);
    side[0]=GeometryContainment.Boundary; lower[0]=17; upper[0]=23;
    // Not ported: the cpp test switches the rounding mode with fesetround and
    // expects the certified queries to refuse it; Java arithmetic is always
    // round-to-nearest, so a directed rounding mode cannot be set.
    body.setScale(new Vector3Dd(0,1,1));
    expectEq(GeometryDistanceStatus.InvalidInput, body.encloseMinimumBoundaryDistance(
        new Vector3Dd(),1e-5,side,lower,upper));
    expectEq(GeometryContainment.Boundary, side[0]); expectDoubleEq(17, lower[0]); expectDoubleEq(23, upper[0]);
}

    @Test
    void EllipsoidMinimumDistanceContract_WrongInverseHintCannotFalsifyBounds()
    {
    GeometryContainment[] side = {GeometryContainment.Indeterminate};
    double[] lower = {-1};
    double[] upper = {-1};
    // The inverse hint is deliberately wrong. It may worsen the witness, but
    // the returned enclosure is calculated from the forward matrix itself.
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidMinimumDistance.query(
        new Vector3Dd(3,0,0),1,Matrix4x4d.identityMatrix(),new Matrix4x4d().scale(-1,1,1),
        10,side,lower,upper));
    expectEq(GeometryContainment.Outside, side[0]);
    expectLe(lower[0], 2); expectGe(upper[0], 2); expectLe(upper[0]-lower[0], 10);
    side[0]=GeometryContainment.Boundary; lower[0]=17; upper[0]=23;
    expectEq(GeometryDistanceStatus.Indeterminate, AffineEllipsoidMinimumDistance.query(
        new Vector3Dd(3,0,0),1,Matrix4x4d.identityMatrix(),new Matrix4x4d().scale(-1,1,1),
        1e-6,side,lower,upper));
    expectEq(GeometryContainment.Boundary, side[0]); expectDoubleEq(17, lower[0]); expectDoubleEq(23, upper[0]);
}

    @Test
    void FloatingIntervalContract_IntrinsicSphereTolerancePreservesUncertainty()
    {
    Sphere sphere = new Sphere(3);
    expectEq(GeometryContainment.Inside, sphere.classifyPoint(new Vector3Dd(2.8,0,0),0.1));
    expectEq(GeometryContainment.Boundary, sphere.classifyPoint(new Vector3Dd(3.05,0,0),0.1));
    expectEq(GeometryContainment.Outside, sphere.classifyPoint(new Vector3Dd(3.2,0,0),0.1));
    expectEq(GeometryContainment.Indeterminate, sphere.classifyPoint(new Vector3Dd(3.1,0,0),0.1));
    expectThrow(IllegalStateException.class, () -> { sphere.doContainmentTest(new Vector3Dd(3.1,0,0),0.1); });
    expectEq(GeometryContainment.Boundary, sphere.classifyPoint(new Vector3Dd(3,0,0),0));
}

    @Test
    void EllipsoidDistanceContract_OrthogonalAxesCoverExteriorInteriorAndHardCase()
    {
    final Matrix4x4d forward=new Matrix4x4d().translation(5,-2,3).multiply(new Matrix4x4d().scale(-3,2,1));
    double[] distance = {-1};
    double[] error = {-1};
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(new Vector3Dd(5,-2,3),1,forward,distance,error));
    expectNear(1, distance[0], 1e-10);
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(new Vector3Dd(1.8,-2,3),1,forward,distance,error));
    expectNear(0.2, distance[0], 1e-10);
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(new Vector3Dd(4,-2,3),1,forward,distance,error));
    expectNear(Math.sqrt(0.875), distance[0], 1e-10);
    expectGt(error[0], 0);
    final Matrix4x4d shear=Matrix4x4d.identityMatrix().withVal(0,1,0.5);
    expectEq(GeometryDistanceStatus.Unsupported, OrthogonalEllipsoidDistance.query(new Vector3Dd(),1,shear,distance,error));
    expectEq(GeometryDistanceStatus.InvalidInput, OrthogonalEllipsoidDistance.query(new Vector3Dd(),0,forward,distance,error));
    expectEq(GeometryDistanceStatus.Indeterminate, OrthogonalEllipsoidDistance.query(new Vector3Dd(),1,new Matrix4x4d().scale(1e-9,1,1),distance,error));
    // A nonzero column whose squared norm underflows is ill-conditioned,
    // not a proven singular input.
    distance[0]=17; error[0]=23;
    expectEq(GeometryDistanceStatus.Indeterminate, OrthogonalEllipsoidDistance.query(new Vector3Dd(),1,new Matrix4x4d().scale(1e-300,1,1),distance,error));
    expectDoubleEq(17, distance[0]); expectDoubleEq(23, error[0]);
}

    @Test
    void EllipsoidDistanceContract_ObliqueNormalOffsetsGiveWorldDistance()
    {
    final Matrix4x4d forward=new Matrix4x4d().scale(3,2,1);
    final Vector3Dd surface = new Vector3Dd(1.8,1.6,0);
    final Vector3Dd normal=new Vector3Dd(1,2,0).multiply(1/Math.sqrt(5.0));
    double[] distance = {0};
    double[] error = {0};
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(surface.add(normal.multiply(0.2)),1,forward,distance,error));
    expectNear(0.2, distance[0], 1e-9);
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(surface.subtract(normal.multiply(0.05)),1,forward,distance,error));
    expectNear(0.05, distance[0], 1e-9);
}

    @Test
    void EllipsoidDistanceContract_DistancesAgreeWithIndependentSurfaceSampling()
    {
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d forward=new Matrix4x4d().translation(4,-3,2).multiply(rotation)
        .multiply(new Matrix4x4d().scale(-3,2,1));
    final Vector3Dd[] localPoints = {new Vector3Dd(),new Vector3Dd(0.1,0.3,0.2),new Vector3Dd(-0.7,0.1,0),
        new Vector3Dd(0.6,0.7,0.1),new Vector3Dd(1.4,0.3,0.5),new Vector3Dd(-0.5,-1.2,0.3)};
    for (int index=0;index<6;++index) {
        final Vector3Dd point=forward.multiply(localPoints[index]);
        double sampled=Double.MAX_VALUE;
        for (int latitude=0;latitude<=180;++latitude) {
            final double theta=Math.PI*latitude/180;
            for (int longitude=0;longitude<360;++longitude) {
                final double phi=2*Math.PI*longitude/360;
                final Vector3Dd unit = new Vector3Dd(Math.sin(theta)*Math.cos(phi),
                    Math.sin(theta)*Math.sin(phi),Math.cos(theta));
                final double candidate=forward.multiply(unit).subtract(point).length();
                if (candidate<sampled) sampled=candidate;
            }
        }
        double[] distance = {0};
        double[] error = {0};
        expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(point,1,forward,distance,error));
        // The Frobenius norm is below 3.75; the two angular half-steps
        // sum to pi/180, so 0.07 bounds the surface sampling error[0].
        expectLe(distance[0], sampled+error[0]);
        expectGe(distance[0], sampled-0.07-error[0]);
    }
}

    @Test
    void EllipsoidDistanceContract_RepeatedAxesRotationRadiusAndFailureAreExplicit()
    {
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d forward=new Matrix4x4d().translation(4,2,-3).multiply(rotation)
        .multiply(new Matrix4x4d().scale(-2,2,1));
    double[] distance = {-1};
    double[] error = {-1};
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(new Vector3Dd(4,2,-3),2,forward,distance,error));
    expectNear(2, distance[0], 1e-10);
    final Vector3Dd endpoint=forward.multiply(new Vector3Dd(2,0,0));
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(endpoint,2,forward,distance,error));
    expectNear(0, distance[0], 1e-10);
    distance[0]=17; error[0]=23;
    expectEq(GeometryDistanceStatus.Unsupported, OrthogonalEllipsoidDistance.query(new Vector3Dd(),2,
            Matrix4x4d.identityMatrix().withVal(0,1,0.3),distance,error));
    expectDoubleEq(17, distance[0]);
    expectDoubleEq(23, error[0]);
    expectEq(GeometryDistanceStatus.InvalidInput, OrthogonalEllipsoidDistance.query(new Vector3Dd(Double.NaN,0,0),2,forward,distance,error));
    expectEq(GeometryDistanceStatus.InvalidInput, OrthogonalEllipsoidDistance.query(new Vector3Dd(),2,new Matrix4x4d().scale(0,1,1),distance,error));
    expectDoubleEq(17, distance[0]);
    expectDoubleEq(23, error[0]);
}

    @Test
    void EllipsoidDistanceContract_ShearSupportsExactNormalOffsetsAndCenter()
    {
    final Matrix4x4d forward=new Matrix4x4d().translation(3,-2,1).multiply(
        Matrix4x4d.identityMatrix().withVal(0,0,2).withVal(0,1,0.5));
    final Vector3Dd surface=forward.multiply(new Vector3Dd(0.6,0.8,0));
    final Vector3Dd n = new Vector3Dd(0.3,0.65,0);
    final Vector3Dd normal=n.multiply(1/n.length());
    double[] distance = {0};
    double[] error = {0};
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(surface.add(normal.multiply(0.2)),1,forward,distance,error));
    expectNear(0.2, distance[0], 1e-9);
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(surface.subtract(normal.multiply(0.05)),1,forward,distance,error));
    expectNear(0.05, distance[0], 1e-9);
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(new Vector3Dd(3,-2,1),1,forward,distance,error));
    expectNear(Math.sqrt((5.25-Math.sqrt(11.5625))/2), distance[0], 1e-9);
    Vector3Dd[] witness = {new Vector3Dd()};
    expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.queryWithWitness(
        surface.add(normal.multiply(0.2)),1,forward,distance,error,witness));
    expectNear(0, witness[0].subtract(surface).length(), 1e-9);
}

    @Test
    void EllipsoidDistanceContract_ShearedNearSingularityDoesNotInventDistance()
    {
    final Matrix4x4d forward=Matrix4x4d.identityMatrix().withVal(0,1,1).withVal(1,1,1e-8);
    double[] distance = {17};
    double[] error = {23};
    expectEq(GeometryDistanceStatus.Indeterminate, AffineEllipsoidDistance.query(new Vector3Dd(),1,forward,distance,error));
    expectDoubleEq(17, distance[0]);
    expectDoubleEq(23, error[0]);
}

    @Test
    void EllipsoidDistanceContract_SceneBodyExposesAffineDistanceAndFailures()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward=new Matrix4x4d().translation(3,-2,1).multiply(
        Matrix4x4d.identityMatrix().withVal(0,0,2).withVal(0,1,0.5));
    final Matrix4x4d inverse=Matrix4x4d.identityMatrix().withVal(0,0,0.5).withVal(0,1,-0.25)
        .multiply(new Matrix4x4d().translation(-3,2,-1));
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    final Vector3Dd n = new Vector3Dd(0.3,0.65,0);
    final Vector3Dd point=forward.multiply(new Vector3Dd(0.6,0.8,0)).add(n.multiply(0.2/n.length()));
    double[] distance = {0};
    double[] error = {0};
    expectEq(GeometryDistanceStatus.Valid, body.estimateBoundaryDistance(point,distance,error));
    expectNear(0.2, distance[0], 1e-9);
    expectGt(error[0], 0);
    Box box = new Box(1,1,1);
    body.setGeometryReference(box);
    distance[0]=17; error[0]=23;
    expectEq(GeometryDistanceStatus.Unsupported, body.estimateBoundaryDistance(point,distance,error));
    expectDoubleEq(17, distance[0]); expectDoubleEq(23, error[0]);
    expectEq(GeometryDistanceStatus.InvalidInput, body.estimateBoundaryDistance(new Vector3Dd(Double.NaN,0,0),distance,error));
    body.setGeometryReference(sphere);
    body.resetAffinePlacement();
    body.setScale(new Vector3Dd(0,1,1));
    expectEq(GeometryDistanceStatus.InvalidInput, body.estimateBoundaryDistance(point,distance,error));
    body.setScale(new Vector3Dd(1e-12,1,1));
    expectEq(GeometryDistanceStatus.Indeterminate, body.estimateBoundaryDistance(point,distance,error));
    expectDoubleEq(17, distance[0]); expectDoubleEq(23, error[0]);
}

    @Test
    void EllipsoidDistanceContract_ShearedDistanceAgreesWithIndependentSurfaceSampling()
    {
    final Matrix4x4d forward=new Matrix4x4d().translation(1,-3,2).multiply(
        Matrix4x4d.identityMatrix().withVal(0,0,-2).withVal(0,1,0.5).withVal(1,2,-0.3));
    final Vector3Dd[] samples = {new Vector3Dd(),new Vector3Dd(0.1,0.3,0.2),new Vector3Dd(-0.7,0.1,0),
        new Vector3Dd(0.6,0.7,0.1),new Vector3Dd(1.4,0.3,0.5),new Vector3Dd(-0.5,-1.2,0.3)};
    for (int index=0;index<6;++index) {
        final Vector3Dd point=forward.multiply(samples[index]);
        double sampled=Double.MAX_VALUE;
        for (int latitude=0;latitude<=180;++latitude) {
            final double theta=Math.PI*latitude/180;
            for (int longitude=0;longitude<360;++longitude) {
                final double phi=2*Math.PI*longitude/360;
                final Vector3Dd unit = new Vector3Dd(Math.sin(theta)*Math.cos(phi),
                    Math.sin(theta)*Math.sin(phi),Math.cos(theta));
                final double candidate=forward.multiply(unit).subtract(point).length();
                if (candidate<sampled) sampled=candidate;
            }
        }
        double[] distance = {0};
        double[] error = {0};
        expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(point,1,forward,distance,error));
        // ||A||_F < 2.6 and the sum of angular half-steps is pi/180.
        expectLe(distance[0], sampled+error[0]);
        expectGe(distance[0], sampled-0.05-error[0]);
    }
}

    @Test
    void EllipsoidDistanceContract_ShearedDistanceMatchesIndependentConstrainedMinimization()
    {
    final Matrix4x4d forward=Matrix4x4d.identityMatrix().withVal(0,0,-2)
        .withVal(0,1,0.5).withVal(1,2,-0.3);
    // Generated by generate_ellipsoid_distance_reference.py: independent
    // constrained SLSQP with eight surface seeds, keeping the smallest value.
    final double[][] samples = {
        {0.12,0.27,0.31,0.484219147169112},{0.65,0.1,-0.2,0.437976391530554},
        {0.21,-0.72,0.35,0.200937270429387},{0.35,0.55,0.15,0.293307568130137},
        {1.1,0.2,0.3,0.228896833955284},{-1.1,0.8,0.4,0.717026711154501},
        {0.1,-1.2,0.5,0.341598824284725},{0.3,0.2,1.5,0.529526975338513},
        {-0.9,-0.4,-0.3,0.0336629761551959},{0.01,0.01,0.01,0.828855732290258},
        {0,0,0,0.84234037196832},{1.5,-1.4,0.9,2.19701576003708}
    };
    for (int i=0;i<12;++i) {
        final Vector3Dd point=forward.multiply(new Vector3Dd(samples[i][0],samples[i][1],samples[i][2]));
        double[] distance = {0};
        double[] error = {0};
        expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(point,1,forward,distance,error), "" + i);
        expectNear(samples[i][3], distance[0], 5e-7, "" + i);
        expectLe(Math.abs(samples[i][3]-distance[0]), error[0]+5e-12, "" + i);
    }
}

    @Test
    void EllipsoidDistanceContract_NumericalReferenceSurvivesRotationTranslationAndScale()
    {
    final Matrix4x4d rotation=Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d shape=Matrix4x4d.identityMatrix().withVal(0,0,-2)
        .withVal(0,1,0.5).withVal(1,2,-0.3);
    final double[][] samples = {{0.12,0.27,0.31,0.484219147169112},
        {0.1,-1.2,0.5,0.341598824284725},{0.01,0.01,0.01,0.828855732290258},
        {-0.9,-0.4,-0.3,0.0336629761551959}};
    final double[] factors = {0.01,1,100};
    for (int k=0;k<3;++k) {
        final double factor=factors[k];
        final Matrix4x4d forward=new Matrix4x4d().translation(4,-3,2).multiply(rotation)
            .multiply(new Matrix4x4d().scale(factor,factor,factor)).multiply(shape);
        for (int i=0;i<4;++i) {
            final Vector3Dd point=forward.multiply(new Vector3Dd(samples[i][0],samples[i][1],samples[i][2]));
            double[] distance = {0};
            double[] error = {0};
            expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(point,1,forward,distance,error), "" + k + ", " + i);
            expectNear(samples[i][3]*factor, distance[0], 5e-7*factor, "" + k + ", " + i);
        }
    }
}

    @Test
    void RayAffineContract_CopiesPreserveUnchangedComponents()
    {
    Ray original = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0), 1);
    final Ray atExit = original.withT(2);
    expectDoubleEq(2, atExit.getDirection().x());
    expectDoubleEq(1, atExit.getOrigin().add(atExit.getDirection().multiply(atExit.getT())).x());
    expectDoubleEq(1, original.getT());
    final Ray shifted = original.withOrigin(new Vector3Dd(-5,0,0));
    expectDoubleEq(2, shifted.getDirection().x());
    expectDoubleEq(1, shifted.getT());
    final Ray redirected = original.withDirection(new Vector3Dd(4,1,0));
    expectDoubleEq(4, redirected.getDirection().x());
    expectDoubleEq(1, redirected.getDirection().y());
    expectDoubleEq(-3, redirected.getOrigin().x());
    expectDoubleEq(1, redirected.getT());
    final Ray copied = Ray.copyOf(original);
    expectTrue(copied.equals(original));
    expectEq(0, Double.compare(original.withT(-0.0).getT(), -0.0));
}

    @Test
    void RayAffineContract_ExplicitAffineConstructionPreservesLegacyConstructor()
    {
    Ray legacy = new Ray(new Vector3Dd(), new Vector3Dd(2,0,0));
    expectDoubleEq(1, legacy.getDirection().x());
    Ray affine = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(2,0,0));
    expectDoubleEq(2, affine.getDirection().x());
    expectDoubleEq(0, affine.getT());
}

    @Test
    void TriangleContract_SharedKernelPreservesAffineRayAndBarycentrics()
    {
    final Vector3Dd a = new Vector3Dd(0,0,0);
    final Vector3Dd b = new Vector3Dd(2,0,0);
    final Vector3Dd c = new Vector3Dd(0,2,0);
    final Ray ray = Ray.fromAffine(new Vector3Dd(0.5,0.5,4), new Vector3Dd(0,0,-2));
    double[] t = {0};
    double[] u = {0};
    double[] v = {0};
    expectTrue(Triangle.intersectRay(ray, a, b, c, t, u, v));
    expectDoubleEq(2, t[0]);
    expectDoubleEq(0.25, u[0]);
    expectDoubleEq(0.25, v[0]);
    Intersection hit= Triangle.doIntersectionWithTriangle(ray, a, b, c);
    expectNe(null, hit);
    expectDoubleEq(t[0], hit.getT());
    expectTrue(Triangle.intersectRay(ray, a, c, b, t, u, v));
    expectFalse(Triangle.intersectRay(Ray.fromAffine(new Vector3Dd(3,3,1), new Vector3Dd(0,0,-1)),
        a, b, c, t, u, v));
    expectFalse(Triangle.intersectRay(Ray.fromAffine(new Vector3Dd(0.5,0.5,1), new Vector3Dd(1,0,0)),
        a, b, c, t, u, v));
    expectFalse(Triangle.intersectRay(ray, a, a, c, t, u, v));
}

    @Test
    void TriangleContract_ProjectedContainmentAndWindingWorkOnEveryAxis()
    {
    final Vector3Dd first = new Vector3Dd(0,0,0);
    final Vector3Dd[] second = {new Vector3Dd(0,2,0), new Vector3Dd(2,0,0), new Vector3Dd(2,0,0)};
    final Vector3Dd[] third = {new Vector3Dd(0,0,2), new Vector3Dd(0,0,2), new Vector3Dd(0,2,0)};
    final Vector3Dd[] inside = {new Vector3Dd(0,0.5,0.5), new Vector3Dd(0.5,0,0.5), new Vector3Dd(0.5,0.5,0)};
    final Vector3Dd[] outside = {new Vector3Dd(0,2,2), new Vector3Dd(2,0,2), new Vector3Dd(2,2,0)};
    for (int i = 0; i < 3; ++i) {
        for (int reversed = 0; reversed < 2; ++reversed) {
            Vector3Dd a = first, b = reversed != 0 ? third[i] : second[i];
            final Vector3Dd c = reversed != 0 ? second[i] : third[i];
            Vector3Dd[] normal = {new Vector3Dd()};
            double[] distance = {0};
            expectTrue(Triangle.supportingPlane(a, b, c, 1e-9, normal, distance));
            final Triangle.ProjectionAxis axis = Triangle.dominantProjection(normal[0]);
            expectEq(i, axis.ordinal());
            if (Triangle.requiresProjectionSwap(a, b, c, axis)) {
                final Vector3Dd temporary = a; a = b; b = temporary;
            }
            expectTrue(Triangle.containsProjected(a, b, c, inside[i], axis));
            expectFalse(Triangle.containsProjected(a, b, c, outside[i], axis));
            expectTrue(Triangle.containsProjected(a, b, c, a, axis));
        }
    }
    Vector3Dd[] normal = {new Vector3Dd()};
    double[] distance = {0};
    expectFalse(Triangle.supportingPlane(first, first, first, 1e-9, normal, distance));
    expectFalse(Triangle.supportingPlane(first, first, first, 0, normal, distance));
}

    @Test
    void SphereFirstHitContract_FirstHitCrossingsAndDetailUseSameParameter()
    {
    Sphere sphere = new Sphere(1);
    Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
    RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(1, hit.getHitDistance());
    expectDoubleEq(-1, hit.point.x());
    expectDoubleEq(-1, hit.normal.x());
    expectNe(null, hit.getRay());
    expectDoubleEq(2, hit.getRay().getDirection().x());
    RayCrossingTestCollector crossings = new RayCrossingTestCollector();
    sphere.emitRayCrossings(ray, new RayCrossingQuery(0,10), crossings);
    expectEq(2L, crossings.roots.size());
    expectDoubleEq(crossings.roots.get(0), hit.getHitDistance());
    Ray pointerHit= sphere.doIntersectionFirstHit(ray);
    expectNe(null, pointerHit);
    expectDoubleEq(hit.getHitDistance(), pointerHit.getT());
    expectDoubleEq(2, pointerHit.getDirection().x());
}

    @Test
    void SphereFirstHitContract_InteriorExitAndClippedFarRoot()
    {
    Sphere sphere = new Sphere(1);
    Ray ray = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(2,0,0));
    RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(0.5, hit.getHitDistance());
    expectDoubleEq(1, hit.point.x());
    expectDoubleEq(1, hit.normal.x());
    ray = ray.withOrigin(new Vector3Dd(-3,0,0));
    expectTrue(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(1.25,2), hit));
    expectDoubleEq(2, hit.getHitDistance());
    expectDoubleEq(1, hit.point.x());
    expectFalse(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(1.25,1.75), hit));
    expectTrue(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(2,2), null));
    expectFalse(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(3,2), hit));
}

    @Test
    void SphereFirstHitContract_TangencyAndDistanceOnly()
    {
    Sphere sphere = new Sphere(1);
    Ray ray = Ray.fromAffine(new Vector3Dd(-2,1,0), new Vector3Dd(1,0,0));
    RayHit hit = new RayHit(RayHit.DETAIL_NONE, false);
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(2, hit.getHitDistance());
    expectEq(null, hit.getRay());
    ray = ray.withDirection(new Vector3Dd());
    expectFalse(sphere.doIntersectionFirstHit(ray, hit));
    Sphere oblique = new Sphere(Math.sqrt(5.0));
    ray = Ray.fromAffine(new Vector3Dd(-6,-3,0), new Vector3Dd(2,1,0));
    expectTrue(oblique.doIntersectionFirstHit(ray, hit));
    expectNear(2, hit.getHitDistance(), 1e-12);
}

    @Test
    void SphereFirstHitContract_SceneFastPathAndDetailAgreeForAffineRay()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10,0,0));
    Ray ray = Ray.fromAffine(new Vector3Dd(7,0,0), new Vector3Dd(2,0,0));
    RayHit distance = new RayHit(RayHit.DETAIL_NONE, false);
    RayHit detail = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(ray, distance));
    expectTrue(body.doIntersectionFirstHit(ray, detail));
    expectDoubleEq(1, distance.getHitDistance());
    expectDoubleEq(distance.getHitDistance(), detail.getHitDistance());
    expectDoubleEq(9, detail.point.x());
    expectDoubleEq(-1, detail.normal.x());
    expectNe(null, detail.getRay());
    expectDoubleEq(2, detail.getRay().getDirection().x());
    ray = ray.withOrigin(new Vector3Dd(10,0,0));
    expectTrue(body.doIntersectionFirstHit(ray, distance));
    expectTrue(body.doIntersectionFirstHit(ray, detail));
    expectDoubleEq(0.5, distance.getHitDistance());
    expectDoubleEq(distance.getHitDistance(), detail.getHitDistance());
    expectDoubleEq(11, detail.point.x());
}

    @Test
    void SphereFirstHitContract_NegativeNonuniformScalePreservesWorldParameter()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10,0,0));
    body.setScale(new Vector3Dd(-2,3,1));
    Ray ray = Ray.fromAffine(new Vector3Dd(4,0,0), new Vector3Dd(2,0,0));
    RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(2, hit.getHitDistance());
    expectDoubleEq(8, hit.point.x());
    expectDoubleEq(-1, hit.normal.x());
    expectNe(null, hit.getRay());
    expectDoubleEq(2, hit.getRay().getDirection().x());
    ray = ray.withOrigin(new Vector3Dd(10,0,0));
    expectTrue(body.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(1, hit.getHitDistance());
    expectDoubleEq(12, hit.point.x());
    expectDoubleEq(1, hit.normal.x());
}

    @Test
    void SphereFirstHitContract_InwardShadowBiasFindsTheAnalyticExit()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Vector3Dd surfacePoint = new Vector3Dd(1,0,0);
    final Vector3Dd lightDirection = new Vector3Dd(-0.1,1,0).normalized();
    final double bias = 1e-6;
    Ray shadowRay = Ray.fromAffine(surfacePoint.add(lightDirection.multiply(bias)),
        lightDirection);
    expectEq(Geometry.INSIDE, sphere.doContainmentTest(shadowRay.getOrigin(), 0));
    RayHit hit = new RayHit(RayHit.DETAIL_NONE, false);
    expectTrue(body.doIntersectionFirstHit(shadowRay, hit));
    // On the unit sphere, the other chord endpoint lies at -2*(p dot d).
    final double expectedExit = -2 * surfacePoint.dotProduct(lightDirection) - bias;
    expectNear(expectedExit, hit.getHitDistance(), 1e-12);
    expectGt(hit.getHitDistance(), bias);
}


    @Test
    void SphereDetailContract_RequestedAndAvailableAreDistinctAndResettable()
    {
    Sphere sphere = new Sphere(1);
    Ray ray = Ray.fromAffine(new Vector3Dd(0,0,2), new Vector3Dd(0,0,-1));
    RayHit hit = new RayHit(RayHit.DETAIL_ALL);
    expectFalse(hit.isDetailAvailabilityKnown());
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectTrue(hit.isDetailAvailabilityKnown());
    expectEq(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, hit.getAvailableDetailMask());
    expectEq((int)(RayHit.DETAIL_ALL), hit.getRequiredDetailMask());
    RayHit copied = new RayHit(hit);
    expectTrue(copied.isDetailAvailabilityKnown());
    expectEq(hit.getAvailableDetailMask(), copied.getAvailableDetailMask());
    hit.resetForDistanceOnly();
    expectFalse(hit.isDetailAvailabilityKnown());
    expectEq((int)(RayHit.DETAIL_NONE), hit.getAvailableDetailMask());
    expectTrue(copied.isDetailAvailabilityKnown());
}


    @Test
    void GeometryQueryContract_DistinguishesUnsupportedMissAndInvalidInput()
    {
    Sphere sphere = new Sphere(1);
    final Geometry geometry= sphere;
    Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    RayHit hit = workspace.hitResult();
    hit.reset(RayHit.DETAIL_POINT);
    RayBoundary firstBoundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, geometry.queryFirstHit(ray, new RayCrossingQuery(1.25,2), hit, firstBoundary));
    expectEq(sphere, firstBoundary.geometry);
    expectDoubleEq(2, firstBoundary.t);
    expectFalse(firstBoundary.normalReversed);
    expectDoubleEq(2, hit.getHitDistance());
    expectDoubleEq(1, hit.point.x());
    expectTrue(hit.isDetailAvailabilityKnown());
    expectEq((int)(RayHit.DETAIL_POINT), hit.getAvailableDetailMask());
    expectEq(RayFirstHitStatus.Miss, geometry.queryFirstHit(ray, new RayCrossingQuery(1.25,1.75), hit, firstBoundary));
    expectEq(null, firstBoundary.geometry);
    expectFalse(hit.hasHitDistance());
    expectEq(RayFirstHitStatus.InvalidInput, geometry.queryFirstHit(ray, new RayCrossingQuery(2,1), hit));
    ray = ray.withDirection(new Vector3Dd(0,0,0));
    expectEq(RayFirstHitStatus.InvalidInput, geometry.queryFirstHit(ray, new RayCrossingQuery(0,3), hit));
    ray = ray.withDirection(new Vector3Dd(1e300,0,0));
    expectEq(RayFirstHitStatus.Indeterminate, geometry.queryFirstHit(ray, new RayCrossingQuery(0,3), hit));
    Torus unsupported = new Torus(2,1);
    expectEq(RayFirstHitStatus.Unsupported, unsupported.queryFirstHit(ray, new RayCrossingQuery(0,3), hit));
}

    @Test
    void GeometryQueryContract_WorkspaceAndExtensibleProvenanceAreIndependent()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody instance = new SimpleBody();
    instance.setGeometryReference(sphere);
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
    expectEq(RaySolidQueryStatus.Supported, sphere.classifyRay(ray, new RayCrossingQuery(0,3), workspace.solidResult()));
    expectEq(2L, workspace.solidResult().boundaries.size());
    RayBoundary boundary = workspace.solidResult().boundaries.get(0);
    expectEq(sphere, boundary.geometry);
    expectEq(-1, boundary.getProvenance().getSurfaceIndex());
    expectTrue(boundary.getProvenance().setSurfaceIndex(7));
    expectFalse(boundary.getProvenance().setSurfaceIndex(-2));
    boundary.normalReversed = true;
    Matrix4x4d forward = new Matrix4x4d().translation(1,2,3);
    Matrix4x4d inverse = new Matrix4x4d().translation(-1,-2,-3);
    RayInstanceStep step = new RayInstanceStep(instance, forward, inverse);
    for (int i = 0; i < 32; i++) expectTrue(boundary.appendInstance(step));
    expectFalse(boundary.appendInstance(new RayInstanceStep()));
    expectEq(32L, boundary.getInstanceCount());
    RayBoundary copied = new RayBoundary(boundary);
    boundary.clearInstances();
    expectEq(7, boundary.getProvenance().getSurfaceIndex());
    expectEq(32L, copied.getInstanceCount());
    expectEq(7, copied.getProvenance().getSurfaceIndex());
    expectTrue(copied.normalReversed);
    RayInstanceStep recovered = new RayInstanceStep();
    expectTrue(copied.getInstance(31, recovered));
    expectEq(instance, recovered.instance);
    expectDoubleEq(1, recovered.childToParent.multiply(new Vector3Dd()).x());
    expectDoubleEq(0, recovered.parentToChild.multiply(recovered.childToParent.multiply(new Vector3Dd())).x());
    expectFalse(copied.getInstance(32, recovered));
    workspace.clear();
    expectTrue(workspace.solidResult().boundaries.isEmpty());
    expectEq(GeometryBounds.Kind.Unsupported, workspace.boundsResult().getKind());
    expectFalse(workspace.hitResult().hasHitDistance());
    expectEq(32L, copied.getInstanceCount());
}


    @Test
    void GeometryQueryContract_SceneInstancePreservesAffineParameterAndProvenance()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10,0,0));
    body.setScale(new Vector3Dd(-2,3,1));
    Ray ray = Ray.fromAffine(new Vector3Dd(4,0,0), new Vector3Dd(2,0,0));
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    expectEq(2L, workspace.solidResult().boundaries.size());
    expectDoubleEq(2, workspace.solidResult().boundaries.get(0).t);
    expectDoubleEq(4, workspace.solidResult().boundaries.get(1).t);
    expectEq(sphere, workspace.solidResult().boundaries.get(0).geometry);
    RayInstanceStep step = new RayInstanceStep();
    expectTrue(workspace.solidResult().boundaries.get(0).getInstance(0, step));
    expectEq(body, step.instance);
    final Vector3Dd worldPoint = ray.getOrigin().add(ray.getDirection().multiply(2));
    final Vector3Dd localPoint = step.parentToChild.multiply(worldPoint);
    expectDoubleEq(1, localPoint.x());
    expectDoubleEq(worldPoint.x(), step.childToParent.multiply(localPoint).x());
    body.setScale(new Vector3Dd(0,3,1));
    expectEq(RaySolidQueryStatus.InvalidInput, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    expectTrue(workspace.solidResult().boundaries.isEmpty());
}

    @Test
    void BoxContract_InstancePreservesAffineParameterAndProvenance()
    {
    Box box = new Box(2,2,2);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(box);
    body.setPosition(new Vector3Dd(10,0,0));
    body.setScale(new Vector3Dd(-2,3,1));
    final Ray ray = Ray.fromAffine(new Vector3Dd(4,0,0), new Vector3Dd(2,0,0));
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    expectEq(2, workspace.solidResult().boundaries.size());
    expectDoubleEq(2, workspace.solidResult().boundaries.get(0).t);
    expectDoubleEq(4, workspace.solidResult().boundaries.get(1).t);
    expectEq(box, workspace.solidResult().boundaries.get(0).geometry);
    RayInstanceStep step = new RayInstanceStep();
    expectTrue(workspace.solidResult().boundaries.get(0).getInstance(0, step));
    expectEq(body, step.instance);
    // Certified under the reflected nonuniform placement: world x in [8,12],
    // y in [-3,3], z in [-1,1].
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(10,0,0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(12.05,0,0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(10,2.95,0), 0.1));
    expectEq(GeometryContainment.Outside, body.classifyPoint(new Vector3Dd(7.8,0,0), 0.1));
}

    @Test
    void PlanarHalfSpaceContract_ReflectedInstanceKeepsBoundaryAndInterior()
    {
    PlanarHalfSpace half = new PlanarHalfSpace(1,0,0,-1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(half);
    body.setScale(new Vector3Dd(-2,3,1));
    final Ray ray = Ray.fromAffine(new Vector3Dd(-4,0,0), new Vector3Dd(1,0,0));
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    expectEq(1, workspace.solidResult().boundaries.size());
    expectDoubleEq(2, workspace.solidResult().boundaries.get(0).t);
    expectEq(half, workspace.solidResult().boundaries.get(0).geometry);
    expectEq(2, workspace.solidResult().insideSegments.size());
    expectFalse(workspace.solidResult().insideSegments.get(0));
    expectTrue(workspace.solidResult().insideSegments.get(1));
    RayInstanceStep step = new RayInstanceStep();
    expectTrue(workspace.solidResult().boundaries.get(0).getInstance(0, step));
    expectEq(body, step.instance);
}


    @Test
    void GeometryQueryContract_PointContainmentSeparatesBoundaryAndUnsupported()
    {
    Sphere sphere = new Sphere(1);
    final Geometry geometry= sphere;
    expectEq(GeometryContainment.Inside, geometry.classifyPoint(new Vector3Dd(0,0,0), 0));
    expectEq(GeometryContainment.Boundary, geometry.classifyPoint(new Vector3Dd(1,0,0), 0));
    expectEq(GeometryContainment.Boundary, geometry.classifyPoint(new Vector3Dd(1.01,0,0), 0.02));
    expectEq(GeometryContainment.Outside, geometry.classifyPoint(new Vector3Dd(2,0,0), 0));
    expectEq(GeometryContainment.InvalidInput, geometry.classifyPoint(new Vector3Dd(), -1));
    expectEq(GeometryContainment.InvalidInput, geometry.classifyPoint(new Vector3Dd(), Double.NaN));
    expectEq(GeometryContainment.Indeterminate, geometry.classifyPoint(new Vector3Dd(1e300,0,0), 0));
    Sphere tiny = new Sphere(1e-300);
    expectEq(GeometryContainment.Indeterminate, tiny.classifyPoint(new Vector3Dd(2e-300,0,0), 0));
    Torus unsupported = new Torus(2,1);
    expectEq(GeometryContainment.Unsupported, unsupported.classifyPoint(new Vector3Dd(), 0));
}

    @Test
    void GeometryQueryContract_CrossingFailuresAreNotReportedAsMisses()
    {
    Sphere sphere = new Sphere(1);
    RayCrossingTestCollector crossings = new RayCrossingTestCollector();
    Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(0,0,0));
    expectEq(RayCrossingSupport.InvalidInput, sphere.emitRayCrossings(ray, new RayCrossingQuery(0,3), crossings));
    ray = ray.withDirection(new Vector3Dd(1e300,0,0));
    expectEq(RayCrossingSupport.Indeterminate, sphere.emitRayCrossings(ray, new RayCrossingQuery(0,3), crossings));
    expectTrue(crossings.roots.isEmpty());
}

    @Test
    void GeometryQueryContract_ShearRoundTripPreservesTheRayParameter()
    {
    Sphere sphere = new Sphere(1);
    final Matrix4x4d shear = Matrix4x4d.identityMatrix().withVal(0,1,0.5);
    final Matrix4x4d inverse = Matrix4x4d.identityMatrix().withVal(0,1,-0.5);
    final Ray local = Ray.fromAffine(new Vector3Dd(-3,0.5,0), new Vector3Dd(2,0,0));
    final Ray world = Ray.fromAffine(shear.multiply(local.getOrigin()),
        shear.withoutTranslation().multiply(local.getDirection()));
    final Ray recovered = Ray.fromAffine(inverse.multiply(world.getOrigin()),
        inverse.withoutTranslation().multiply(world.getDirection()));
    RayHit localHit = new RayHit(RayHit.DETAIL_POINT, false);
    RayHit recoveredHit = new RayHit(RayHit.DETAIL_POINT, false);
    expectEq(RayFirstHitStatus.Hit, sphere.queryFirstHit(local, new RayCrossingQuery(0,4), localHit));
    expectEq(RayFirstHitStatus.Hit, sphere.queryFirstHit(recovered, new RayCrossingQuery(0,4), recoveredHit));
    expectDoubleEq(localHit.getHitDistance(), recoveredHit.getHitDistance());
    final Vector3Dd expected = shear.multiply(localHit.point);
    final Vector3Dd worldAtT = world.getOrigin().add(world.getDirection().multiply(recoveredHit.getHitDistance()));
    expectDoubleEq(expected.x(), worldAtT.x());
    expectDoubleEq(expected.y(), worldAtT.y());
}


    @Test
    void GeometryQueryContract_SurfaceEventsUseTheSameExtensibleProvenanceModel()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    RayCrossing event = new RayCrossing(1, sphere, RayCrossingKind.Entering);
    RayInstanceStep step = new RayInstanceStep(body, new Matrix4x4d(), new Matrix4x4d());
    expectTrue(event.getProvenance().setSurfaceIndex(3));
    expectTrue(event.getProvenance().appendInstance(step));
    event.normalReversed = true;
    RayCrossing copy = new RayCrossing(event);
    expectTrue(copy.normalReversed);
    event.getProvenance().clear();
    expectEq(-1, event.getProvenance().getSurfaceIndex());
    expectEq(3, copy.getProvenance().getSurfaceIndex());
    expectEq(1L, copy.getProvenance().getInstanceCount());
    RayInstanceStep restored = new RayInstanceStep();
    expectTrue(copy.getProvenance().getInstance(0, restored));
    expectEq(body, restored.instance);
}


    @Test
    void GeometryQueryContract_SeparateWorkerWorkspacesCanRunConcurrently() throws InterruptedException
    {
    Sphere sphere = new Sphere(1);
    GeometryWorkspaceTestWorker first = new GeometryWorkspaceTestWorker(sphere);
    GeometryWorkspaceTestWorker second = new GeometryWorkspaceTestWorker(sphere);
    Thread firstThread = new Thread(first);
    Thread secondThread = new Thread(second);
    firstThread.start(); secondThread.start();
    firstThread.join(); secondThread.join();
    expectTrue(first.passed());
    expectTrue(second.passed());
    expectNe(null, first.boundaryStorage());
    expectNe(null, second.boundaryStorage());
    expectTrue(first.boundaryStorage() != second.boundaryStorage());
}


    @Test
    void GeometryQueryContract_NestedWorkspaceScratchIsStableAndRequiresLifoRelease()
    {
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    ArrayList<RaySolidClassification> acquired = new ArrayList<RaySolidClassification>();
    for (int i = 0; i < 32; i++) {
        RaySolidClassification slot= workspace.acquireSolidScratch();
        expectNe(null, slot);
        slot.insideSegments.add(true);
        acquired.add(slot);
    }
    expectFalse(workspace.releaseSolidScratch(acquired.get(0)));
    expectTrue(acquired.get(0).insideSegments.get(0));
    for (int i = 31; i >= 0; i--) expectTrue(workspace.releaseSolidScratch(acquired.get(i)));
    RaySolidClassification reused= workspace.acquireSolidScratch();
    expectEq(acquired.get(0), reused);
    expectTrue(reused.insideSegments.isEmpty());
    workspace.clear();
    expectFalse(workspace.releaseSolidScratch(reused));
    expectEq(reused, workspace.acquireSolidScratch());
}


    @Test
    void GeometryQueryContract_SceneContainmentHasExplicitWorldTolerancePolicy()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10,0,0));
    body.setScale(new Vector3Dd(-2,3,1));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(10,0,0), 0));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(8,0,0), 0));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(8.05,0,0), 0.1));
    body.setScale(new Vector3Dd(-2,-2,-2));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(7.95,0,0), 0.1));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), -1));
    body.setScale(new Vector3Dd(0,2,2));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), 0));
    body.setScale(new Vector3Dd(1e-12,2,2));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(1,0,0));
    expectEq(RaySolidQueryStatus.Indeterminate, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    body.setScale(new Vector3Dd(Double.POSITIVE_INFINITY,2,2));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(RaySolidQueryStatus.InvalidInput, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    body.setScale(new Vector3Dd(1,1,1));
    body.setRotation(Matrix4x4d.identityMatrix().withVal(0,0,0));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(RaySolidQueryStatus.Indeterminate, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    body.setRotation(Matrix4x4d.identityMatrix().withVal(0,1,0.5));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(RaySolidQueryStatus.Indeterminate, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    body.setRotation(new Matrix4x4d());
    body.setPosition(new Vector3Dd(Double.POSITIVE_INFINITY,0,0));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(RaySolidQueryStatus.InvalidInput, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
    body.setPosition(new Vector3Dd(1e308,0,0));
    body.setScale(new Vector3Dd(1e-5,1,1));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(RaySolidQueryStatus.Indeterminate, body.classifyRay(ray, new RayCrossingQuery(0,5), workspace.solidResult()));
}

    @Test
    void GeometryQueryContract_EllipsoidUsesWorldSpaceToleranceOnMultipleAxes()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    ellipsoid.setPosition(new Vector3Dd(10, 0, 0));
    ellipsoid.setScale(new Vector3Dd(3, 2, 1));
    final double tolerance = 0.1;
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(12.95, 0, 0), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(12.8, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(13.05, 0, 0), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(13.2, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0.95), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0.8), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 1.05), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 1.2), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(12.95, 0, 0), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(12.8, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(13.05, 0, 0), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(13.2, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 0.95), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 0.8), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 1.05), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 1.2), tolerance));
    // Exactly on the tolerance threshold, floating point comparisons must not
    // silently force either boundary or outside when the certificate is unsure.
    expectEq(GeometryContainment.Indeterminate, ellipsoid.classifyPoint(new Vector3Dd(13.1, 0, 0), tolerance));
    ellipsoid.setScale(new Vector3Dd(-3, 2, 1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(13.05, 0, 0), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(13.2, 0, 0), tolerance));
    ellipsoid.setScale(new Vector3Dd(0, 2, 1));
    expectEq(GeometryContainment.InvalidInput, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0), tolerance));
    ellipsoid.setScale(new Vector3Dd(1e-12, 2, 1));
    expectEq(GeometryContainment.Indeterminate, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0), tolerance));
}

    @Test
    void GeometryQueryContract_ShearedEllipsoidUsesWorldSpaceNormalDistance()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    final Matrix4x4d forward = new Matrix4x4d().translation(3, 0, 0).multiply(
        Matrix4x4d.identityMatrix().withVal(0, 1, 0.5)).multiply(new Matrix4x4d().scale(2, 1, 1));
    final Matrix4x4d inverse = new Matrix4x4d().scale(0.5, 1, 1).multiply(
        Matrix4x4d.identityMatrix().withVal(0, 1, -0.5)).multiply(new Matrix4x4d().translation(-3, 0, 0));
    expectEq(AffinePlacementStatus.Valid, ellipsoid.setAffinePlacement(forward, inverse));
    // At the transformed local point (0, 1, 0), the world normal is (0, 1, 0).
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(3.5, 1.05, 0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(3.5, 1.2, 0), 0.1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(3.5, 0.95, 0), 0.1));
}

    @Test
    void GeometryQueryContract_EllipsoidObliqueNormalUsesAWorldSpaceWitness()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    ellipsoid.setScale(new Vector3Dd(3,2,1));
    // (1.8, 1.6, 0) lies on the surface; (1, 2, 0)/sqrt(5) is its
    // outward unit normal. Both offsets have an explicit surface witness.
    final double nx = 1.0 / Math.sqrt(5.0);
    final double ny = 2.0 * nx;
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(1.8 + 0.095*nx, 1.6 + 0.095*ny, 0), 0.1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(1.8 - 0.095*nx, 1.6 - 0.095*ny, 0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(1.8 + 0.101*nx, 1.6 + 0.101*ny, 0), 0.1));
}

    @Test
    void GeometryQueryContract_EllipsoidInteriorNearToleranceIsNotBoundary()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3,2,1));
    // For x > 3 - 1/3 the nearest surface point lies at (3,0,0),
    // so this interior point is 0.2 world units from the surface.
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(2.8,0,0), 0.19));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(2.8,0,0), 0.19));
}

    @Test
    void GeometryQueryContract_EllipsoidCenterCanReachBoundaryWithPositiveTolerance()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3,2,1));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(),0.9));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(),1.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(),1.1));
    expectEq(GeometryContainment.Indeterminate, body.classifyPointCertified(new Vector3Dd(),1.0));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(),1.0));
}

    @Test
    void GeometryQueryContract_ExtremeAffineEllipsoidCertifiesBoundaryWhenPossible()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward=new Matrix4x4d().scale(1e-12,1,1);
    final Matrix4x4d inverse=new Matrix4x4d().scale(1e12,1,1);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(),1e-15));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(1.05e-12,0,0),1e-13));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(1.05e-12,0,0),1e-13));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(),0));
}

    @Test
    void GeometryQueryContract_AffineEllipsoidAgreesWithIndependentSurfaceGrid()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward = Matrix4x4d.identityMatrix().withVal(0,0,2).withVal(0,1,0.5);
    final Matrix4x4d inverse = Matrix4x4d.identityMatrix().withVal(0,0,0.5).withVal(0,1,-0.25);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    final Vector3Dd[] points = {new Vector3Dd(0,0,0), new Vector3Dd(1.1,0.2,0.1),
        new Vector3Dd(2.4,0.3,0.2), new Vector3Dd(-1.8,0.9,0.4), new Vector3Dd(0.5,-0.3,1.4)};
    int definite=0;
    for (int index=0;index<5;++index) {
        double sampled=Double.MAX_VALUE;
        for (int latitude=0;latitude<=180;++latitude) {
            final double theta=Math.PI*latitude/180;
            for (int longitude=0;longitude<360;++longitude) {
                final double phi=2*Math.PI*longitude/360;
                final Vector3Dd u = new Vector3Dd(Math.sin(theta)*Math.cos(phi),
                    Math.sin(theta)*Math.sin(phi),Math.cos(theta));
                final double distance=forward.multiply(u).subtract(points[index]).length();
                if (distance<sampled) sampled=distance;
            }
        }
        // Every surface point is within 0.05 world units of a grid point:
        // ||A||_F < 2.6, and the sum of half angular steps is pi/180.
        final double errorBound=0.05;
        final GeometryContainment nearResult=body.classifyPoint(points[index],sampled+errorBound);
        expectTrue(nearResult==GeometryContainment.Boundary || nearResult==GeometryContainment.Indeterminate);
        if (nearResult==GeometryContainment.Boundary) ++definite;
        if (sampled>2*errorBound) {
            final GeometryContainment side=body.classifyPoint(points[index],0);
            final GeometryContainment farResult=body.classifyPoint(points[index],sampled-2*errorBound);
            expectTrue(farResult==side || farResult==GeometryContainment.Indeterminate);
            if (farResult==side) ++definite;
        }
    }
    expectGe(definite, 5);
}

    @Test
    void GeometryQueryContract_SurfaceGridChecksReflectionsShearAndScaleRanges()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d rotation = Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d inverseRotation = Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,1)
        .withVal(1,0,-1).withVal(1,1,0);
    final Matrix4x4d shear = Matrix4x4d.identityMatrix().withVal(0,1,0.5).withVal(1,2,-0.3);
    final Matrix4x4d inverseShear = Matrix4x4d.identityMatrix().withVal(0,1,-0.5)
        .withVal(1,2,0.3).withVal(0,2,-0.15);
    final Vector3Dd[] localPoints = {new Vector3Dd(), new Vector3Dd(0.6,0.2,0.1),
        new Vector3Dd(1.2,0.4,0.3), new Vector3Dd(-0.8,0.9,0.4), new Vector3Dd(0.2,-0.4,1.4)};
    final double[] scales = {0.01,1,100};
    int definite=0;
    for (int mode=0; mode<3; ++mode) {
        final double factor=scales[mode];
        final Matrix4x4d forward=new Matrix4x4d().translation(4,-3,2).multiply(rotation).multiply(shear)
            .multiply(new Matrix4x4d().scale(-2*factor,factor,0.7*factor));
        final Matrix4x4d inverse=new Matrix4x4d().scale(-0.5/factor,1/factor,1/(0.7*factor))
            .multiply(inverseShear).multiply(inverseRotation).multiply(new Matrix4x4d().translation(-4,3,-2));
        expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
        // ||A||_F < 3*factor; angular grid half-step sum is pi/180.
        final double errorBound=0.055*factor;
        for (int index=0; index<5; ++index) {
            final Vector3Dd point=forward.multiply(localPoints[index]);
            double sampled=Double.MAX_VALUE;
            for (int latitude=0; latitude<=180; ++latitude) {
                final double theta=Math.PI*latitude/180;
                for (int longitude=0; longitude<360; ++longitude) {
                    final double phi=2*Math.PI*longitude/360;
                    final Vector3Dd unit = new Vector3Dd(Math.sin(theta)*Math.cos(phi),
                        Math.sin(theta)*Math.sin(phi),Math.cos(theta));
                    final double distance=forward.multiply(unit).subtract(point).length();
                    if (distance<sampled) sampled=distance;
                }
            }
            final GeometryContainment nearResult=body.classifyPoint(point,sampled+errorBound);
            expectTrue(nearResult==GeometryContainment.Boundary || nearResult==GeometryContainment.Indeterminate);
            if (nearResult==GeometryContainment.Boundary) ++definite;
            if (sampled>2*errorBound) {
                final GeometryContainment side=localPoints[index].dotProduct(localPoints[index])<1
                    ? GeometryContainment.Inside : GeometryContainment.Outside;
                final GeometryContainment farResult=body.classifyPoint(point,sampled-2*errorBound);
                expectTrue(farResult==side || farResult==GeometryContainment.Indeterminate);
                if (farResult==side) ++definite;
            }
        }
    }
    expectGe(definite, 15);
}

    @Test
    void GeometryQueryContract_ShearedEllipsoidObliqueNormalHasWorldDistance()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward = Matrix4x4d.identityMatrix().withVal(0,0,2).withVal(0,1,0.5);
    final Matrix4x4d inverse = Matrix4x4d.identityMatrix().withVal(0,0,0.5).withVal(0,1,-0.25);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    // The transformed point of (0.6, 0.8, 0) has normal proportional to (0.3, 0.65, 0).
    final double length = Math.sqrt(0.3*0.3 + 0.65*0.65);
    final double nx = 0.3 / length, ny = 0.65 / length;
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(1.6+0.09*nx,0.8+0.09*ny,0),0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(1.6-0.09*nx,0.8-0.09*ny,0),0.1));
    expectEq(GeometryContainment.Outside, body.classifyPoint(new Vector3Dd(1.6+0.11*nx,0.8+0.11*ny,0),0.1));
}

    @Test
    void GeometryQueryContract_ExteriorNearBandUsesImprovedSupportingPlane()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward=Matrix4x4d.identityMatrix().withVal(0,0,3).withVal(0,1,0.8);
    final Matrix4x4d inverse=Matrix4x4d.identityMatrix().withVal(0,0,1.0/3).withVal(0,1,-0.8/3);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    // Transform the unit-sphere point (0.6, 0.8, 0). Convexity makes the
    // outward normal offset its exact distance to the transformed surface.
    final Vector3Dd surface=forward.multiply(new Vector3Dd(0.6,0.8,0));
    final Vector3Dd normal = new Vector3Dd(0.2,0.64,0);
    final Vector3Dd unit=normal.multiply(1.0/normal.length());
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(surface.add(unit.multiply(0.1001)),0.1));
    expectEq(GeometryContainment.Outside, body.classifyPoint(surface.add(unit.multiply(0.1001)),0.1));
}

    @Test
    void GeometryQueryContract_ShearedEllipsoidNormalOffsetsBracketTolerance()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(sphere);
    final Matrix4x4d forward=Matrix4x4d.identityMatrix().withVal(0,0,3).withVal(0,1,0.8);
    final Matrix4x4d inverse=Matrix4x4d.identityMatrix().withVal(0,0,1.0/3).withVal(0,1,-0.8/3);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    final Vector3Dd[] source = {new Vector3Dd(0.6,0.8,0),new Vector3Dd(-0.8,0.6,0),
        new Vector3Dd(0,0.6,0.8),new Vector3Dd(0.6,0,0.8),new Vector3Dd(-0.6,-0.8,0)};
    int definite=0;
    for (int i=0;i<5;++i) {
        final Vector3Dd p=forward.multiply(source[i]);
        final Vector3Dd n = new Vector3Dd(source[i].x()/3,source[i].y()-0.8*source[i].x()/3,source[i].z());
        final Vector3Dd u=n.multiply(1.0/n.length());
        // Every outward normal ray of a convex ellipsoid has an exact
        // distance equal to its offset from the chosen surface point.
        final GeometryContainment outside=body.classifyPoint(p.add(u.multiply(0.1001)),0.1);
        expectTrue(outside==GeometryContainment.Outside || outside==GeometryContainment.Indeterminate);
        if (outside==GeometryContainment.Outside) ++definite;
        final GeometryContainment near=body.classifyPoint(p.add(u.multiply(0.0999)),0.1);
        expectTrue(near==GeometryContainment.Boundary || near==GeometryContainment.Indeterminate);
        if (near==GeometryContainment.Boundary) ++definite;
    }
    expectGe(definite, 5);
}

    @Test
    void GeometryQueryContract_RotatedReflectedEllipsoidUsesWorldTolerance()
    {
    Sphere sphere = new Sphere(1);
    SimpleBody ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    final Matrix4x4d rotation = Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,-1)
        .withVal(1,0,1).withVal(1,1,0);
    final Matrix4x4d inverseRotation = Matrix4x4d.identityMatrix().withVal(0,0,0).withVal(0,1,1)
        .withVal(1,0,-1).withVal(1,1,0);
    final Matrix4x4d forward = rotation.multiply(new Matrix4x4d().scale(-3,2,1));
    final Matrix4x4d inverse = new Matrix4x4d().scale(-1.0/3,0.5,1).multiply(inverseRotation);
    expectEq(AffinePlacementStatus.Valid, ellipsoid.setAffinePlacement(forward, inverse));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(), 0.1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(0,3.05,0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(0,3.2,0), 0.1));
}


    @Test
    void GeometryQueryContract_OpenSurfaceDoesNotAcquireFictitiousSolidInterior()
    {
    InfinitePlane plane = new InfinitePlane(1,0,0,0);
    final Ray ray = Ray.fromAffine(new Vector3Dd(-2,0,0), new Vector3Dd(1,0,0));
    RayHit legacy = new RayHit(RayHit.DETAIL_NONE, false);
    expectTrue(plane.doIntersectionFirstHit(ray, legacy));
    GeometryQueryWorkspace workspace = new GeometryQueryWorkspace();
    expectEq(RayFirstHitStatus.Hit, plane.queryFirstHit(ray, new RayCrossingQuery(0,4), workspace.hitResult()));
    expectEq(RaySolidQueryStatus.Unsupported, plane.classifyRay(ray, new RayCrossingQuery(0,4), workspace.solidResult()));
    expectTrue(workspace.solidResult().insideSegments.isEmpty());
}


    @Test
    void GeometryQueryContract_SmallSphereNormalIsUnitAndZeroNormalUnavailable()
    {
    Sphere sphere = new Sphere(1e-4);
    Ray ray = Ray.fromAffine(new Vector3Dd(0,0,3e-4), new Vector3Dd(0,0,-1));
    RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectEq(RayFirstHitStatus.Hit, sphere.queryFirstHit(ray, new RayCrossingQuery(0,1), hit));
    expectNear(1, hit.normal.z(), 1e-12);
    expectEq((int)(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL), hit.getAvailableDetailMask());
    Sphere unit = new Sphere(1);
    Ray center = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(1,0,0));
    hit.reset(RayHit.DETAIL_NORMAL);
    unit.doExtraInformation(center, 0, hit);
    expectTrue(hit.isDetailAvailabilityKnown());
    expectEq(0, hit.getAvailableDetailMask() & RayHit.DETAIL_NORMAL);
}

    @Test
    void GeometryBoundsContract_TransformsAllCornersAndReportsInvalidPlacement()
    {
    GeometryBounds local = new GeometryBounds();
    expectTrue(local.setFinite(new Vector3Dd(1,2,3), new Vector3Dd(3,4,5)));
    Matrix4x4d placement = Matrix4x4d.identityMatrix().withVal(0,0,-1)
        .withVal(0,1,2).withVal(0,3,10);
    final GeometryBounds world = local.transformed(placement);
    expectEq(GeometryBounds.Kind.Finite, world.getKind());
    expectDoubleEq(11, world.getMinimum().x());
    expectDoubleEq(17, world.getMaximum().x());
    expectDoubleEq(2, world.getMinimum().y());
    expectDoubleEq(4, world.getMaximum().y());
    expectDoubleEq(3, world.getMinimum().z());
    expectDoubleEq(5, world.getMaximum().z());
    expectEq(GeometryBounds.Kind.Indeterminate, local.transformed(placement.withVal(3,0,1)).getKind());
    expectEq(GeometryBounds.Kind.Indeterminate, local.transformed(placement.withVal(0,0,Double.POSITIVE_INFINITY)).getKind());
    GeometryBounds empty = new GeometryBounds();
    empty.setEmpty();
    expectEq(GeometryBounds.Kind.Empty, empty.transformed(placement).getKind());
    GeometryBounds unbounded = new GeometryBounds();
    unbounded.setUnbounded();
    expectEq(GeometryBounds.Kind.Unbounded, unbounded.transformed(placement).getKind());
}

    @Test
    void GeometryBoundsContract_BodyUsesItsPlacementAndGeometry()
    {
    SimpleBody body = new SimpleBody();
    body.setGeometry(new Sphere(2));
    body.setPosition(new Vector3Dd(5,0,0));
    body.setScale(new Vector3Dd(-2,1,1));
    GeometryBounds bounds = new GeometryBounds();
    body.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectDoubleEq(1, bounds.getMinimum().x());
    expectDoubleEq(9, bounds.getMaximum().x());
    expectDoubleEq(-2, bounds.getMinimum().y());
    expectDoubleEq(2, bounds.getMaximum().y());
    SimpleBody empty = new SimpleBody();
    empty.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Empty, bounds.getKind());
}

    @Test
    void GeometryBoundsContract_EnclosingPreservesUnknownAndUnboundedStates()
    {
    GeometryBounds first = new GeometryBounds();
    GeometryBounds second = new GeometryBounds();
    GeometryBounds empty = new GeometryBounds();
    GeometryBounds unknown = new GeometryBounds();
    GeometryBounds indeterminate = new GeometryBounds();
    GeometryBounds unbounded = new GeometryBounds();
    expectTrue(first.setFinite(new Vector3Dd(-2,1,3), new Vector3Dd(1,4,5)));
    expectTrue(second.setFinite(new Vector3Dd(0,-3,4), new Vector3Dd(5,2,7)));
    empty.setEmpty();
    indeterminate.setIndeterminate();
    unbounded.setUnbounded();
    final GeometryBounds combined = first.enclosing(second);
    expectEq(GeometryBounds.Kind.Finite, combined.getKind());
    expectDoubleEq(-2, combined.getMinimum().x());
    expectDoubleEq(-3, combined.getMinimum().y());
    expectDoubleEq(7, combined.getMaximum().z());
    expectEq(GeometryBounds.Kind.Finite, empty.enclosing(first).getKind());
    expectEq(GeometryBounds.Kind.Unbounded, first.enclosing(unbounded).getKind());
    expectEq(GeometryBounds.Kind.Unsupported, unbounded.enclosing(unknown).getKind());
    expectEq(GeometryBounds.Kind.Indeterminate, unknown.enclosing(indeterminate).getKind());
}

    @Test
    void QuadricSurfaceContract_EllipsoidAndParaboloidUseAffineRoots()
    {
    QuadricSurface ellipsoid = new QuadricSurface(new Vector3Dd(1,4,9), new Vector3Dd(), new Vector3Dd(), -1);
    final Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
    RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    RayBoundary boundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, ellipsoid.queryFirstHit(ray, new RayCrossingQuery(0,10), hit, boundary));
    expectDoubleEq(1, boundary.t);
    expectEq(ellipsoid, boundary.geometry);
    expectDoubleEq(-1, hit.point.x());
    expectDoubleEq(-1, hit.normal.x());
    RayHit deferred = new RayHit(RayHit.DETAIL_NORMAL, false);
    boundary.normalReversed = true;
    expectEq(RayFirstHitStatus.Hit, ellipsoid.querySurfaceDetail(ray, boundary, deferred));
    expectDoubleEq(1, deferred.normal.x());
    RayCrossingTestCollector events = new RayCrossingTestCollector();
    expectEq(RayCrossingSupport.Supported, ellipsoid.emitRayCrossings(ray, new RayCrossingQuery(0,10), events));
    expectEq(2, events.roots.size());
    expectDoubleEq(1, events.roots.get(0));
    expectDoubleEq(2, events.roots.get(1));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(),0));
    QuadricSurface paraboloid = new QuadricSurface(new Vector3Dd(1,1,0), new Vector3Dd(), new Vector3Dd(0,0,-1), 0);
    final Ray vertical=Ray.fromAffine(new Vector3Dd(1,0,3), new Vector3Dd(0,0,-2));
    expectEq(RayFirstHitStatus.Hit, paraboloid.queryFirstHit(vertical,new RayCrossingQuery(0,10),hit,boundary));
    expectDoubleEq(1, boundary.t);
    RayCrossingTestCollector verticalEvents = new RayCrossingTestCollector();
    expectEq(RayCrossingSupport.Supported, paraboloid.emitRayCrossings(vertical, new RayCrossingQuery(0,10), verticalEvents));
    expectEq(1, verticalEvents.roots.size());
    expectEq(RayCrossingKind.Leaving, verticalEvents.kinds.get(0));
    QuadricSurface plane = new QuadricSurface(new Vector3Dd(), new Vector3Dd(), new Vector3Dd(0,0,1), 0);
    expectEq(RayFirstHitStatus.Indeterminate, plane.queryFirstHit(
        Ray.fromAffine(new Vector3Dd(),new Vector3Dd(1,0,0)), new RayCrossingQuery(0,10), hit, boundary));
}

    @Test
    void QuadricSurfaceContract_SolidIntervalsHandleEllipsoidsComplementsAndClipping()
    {
    QuadricSurface solid = new QuadricSurface(new Vector3Dd(1,1,1), new Vector3Dd(), new Vector3Dd(), -1);
    QuadricSurface complement = new QuadricSurface(new Vector3Dd(-1,-1,-1), new Vector3Dd(), new Vector3Dd(), 1);
    final Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0));
    RaySolidClassification result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, solid.classifyRay(ray,new RayCrossingQuery(0,10),result));
    expectEq(2, result.boundaries.size());
    expectEq(3, result.insideSegments.size());
    expectDoubleEq(2, result.boundaries.get(0).t);
    expectDoubleEq(4, result.boundaries.get(1).t);
    expectEq(solid, result.boundaries.get(0).geometry);
    expectFalse(result.insideSegments.get(0)); expectTrue(result.insideSegments.get(1)); expectFalse(result.insideSegments.get(2));
    expectEq(RaySolidQueryStatus.Supported, complement.classifyRay(ray,new RayCrossingQuery(0,10),result));
    expectEq(3, result.insideSegments.size());
    expectTrue(result.insideSegments.get(0)); expectFalse(result.insideSegments.get(1)); expectTrue(result.insideSegments.get(2));
    expectEq(RaySolidQueryStatus.Supported, solid.classifyRay(ray,new RayCrossingQuery(2.5,3.5),result));
    expectEq(0, result.boundaries.size()); expectEq(1, result.insideSegments.size()); expectTrue(result.insideSegments.get(0));
    expectEq(RaySolidQueryStatus.Supported, solid.classifyRay(ray,new RayCrossingQuery(2,4),result));
    expectEq(3, result.insideSegments.size());
    expectFalse(result.insideSegments.get(0)); expectTrue(result.insideSegments.get(1)); expectFalse(result.insideSegments.get(2));
}

    @Test
    void QuadricSurfaceContract_SolidIntervalsHandleLinearTangencyAndCoincidence()
    {
    QuadricSurface paraboloid = new QuadricSurface(new Vector3Dd(1,1,0), new Vector3Dd(), new Vector3Dd(0,0,-1), 0);
    RaySolidClassification result = new RaySolidClassification();
    final Ray vertical=Ray.fromAffine(new Vector3Dd(1,0,3),new Vector3Dd(0,0,-2));
    expectEq(RaySolidQueryStatus.Supported, paraboloid.classifyRay(vertical,new RayCrossingQuery(0,10),result));
    expectEq(1, result.boundaries.size()); expectEq(2, result.insideSegments.size());
    expectDoubleEq(1, result.boundaries.get(0).t);
    expectTrue(result.insideSegments.get(0)); expectFalse(result.insideSegments.get(1));
    QuadricSurface sphere = new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1);
    QuadricSurface inverse = new QuadricSurface(new Vector3Dd(-1,-1,-1),new Vector3Dd(),new Vector3Dd(),1);
    final Ray tangent=Ray.fromAffine(new Vector3Dd(-3,1,0),new Vector3Dd(1,0,0));
    expectEq(RaySolidQueryStatus.Supported, sphere.classifyRay(tangent,new RayCrossingQuery(0,10),result));
    expectEq(0, result.boundaries.size()); expectEq(1, result.insideSegments.size()); expectFalse(result.insideSegments.get(0));
    expectEq(RaySolidQueryStatus.Supported, inverse.classifyRay(tangent,new RayCrossingQuery(0,10),result));
    expectEq(1, result.insideSegments.size()); expectTrue(result.insideSegments.get(0));
    QuadricSurface plane = new QuadricSurface(new Vector3Dd(),new Vector3Dd(),new Vector3Dd(0,0,1),0);
    expectEq(RaySolidQueryStatus.Indeterminate, plane.classifyRay(
        Ray.fromAffine(new Vector3Dd(),new Vector3Dd(1,0,0)),new RayCrossingQuery(0,10),result));
    expectEq(0, result.insideSegments.size()); expectEq(0, result.boundaries.size());
}

    @Test
    void QuadricSurfaceContract_SolidIntervalsPreserveAffineInstanceProvenance()
    {
    SimpleBody body = new SimpleBody();
    QuadricSurface quadric= new QuadricSurface(new Vector3Dd(1,1,1),new Vector3Dd(),new Vector3Dd(),-1);
    body.setGeometry(quadric);
    body.setPosition(new Vector3Dd(3,0,0));
    body.setScale(new Vector3Dd(2,1,1));
    final Ray ray = Ray.fromAffine(new Vector3Dd(-1,0,0),new Vector3Dd(2,0,0));
    RaySolidClassification result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray,new RayCrossingQuery(0,10),result));
    expectEq(2, result.boundaries.size());
    expectDoubleEq(1, result.boundaries.get(0).t);
    expectDoubleEq(3, result.boundaries.get(1).t);
    expectEq(quadric, result.boundaries.get(0).geometry);
    expectEq(1, result.boundaries.get(0).getInstanceCount());
}

    @Test
    void QuadricSurfaceContract_FiniteBoundsEncloseShiftedMixedEllipsoid()
    {
    QuadricSurface quadric = new QuadricSurface(new Vector3Dd(2,2,4),new Vector3Dd(2,0,0),new Vector3Dd(0,6,-4),-2);
    GeometryBounds bounds = new GeometryBounds();
    quadric.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    final double extent=Math.sqrt(6.0);
    expectNear(1-extent, bounds.getMinimum().x(), 1e-12);
    expectNear(1+extent, bounds.getMaximum().x(), 1e-12);
    expectNear(-2-extent, bounds.getMinimum().y(), 1e-12);
    expectNear(-2+extent, bounds.getMaximum().y(), 1e-12);
    expectNear(-1, bounds.getMinimum().z(), 1e-12);
    expectNear(2, bounds.getMaximum().z(), 1e-12);
    double[] legacy=quadric.getMinMax();
    expectDoubleEq(bounds.getMinimum().x(), legacy[0]);
    expectDoubleEq(bounds.getMaximum().z(), legacy[5]);
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(quadric);
    body.setPosition(new Vector3Dd(4,0,0));
    body.setScale(new Vector3Dd(-2,1,1));
    body.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectNear(2-2*extent, bounds.getMinimum().x(), 1e-11);
    expectNear(2+2*extent, bounds.getMaximum().x(), 1e-11);
}

    @Test
    void QuadricSurfaceContract_BoundsAreConservativeForUnboundedAndInvalidInputs()
    {
    GeometryBounds bounds = new GeometryBounds();
    QuadricSurface inverse = new QuadricSurface(new Vector3Dd(-1,-1,-1),new Vector3Dd(),new Vector3Dd(),1);
    inverse.getBounds(bounds); expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
    QuadricSurface cylinder = new QuadricSurface(new Vector3Dd(1,1,0),new Vector3Dd(),new Vector3Dd(),-1);
    cylinder.getBounds(bounds); expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
    QuadricSurface tiny = new QuadricSurface(new Vector3Dd(1e-15,1,1),new Vector3Dd(),new Vector3Dd(),-1);
    tiny.getBounds(bounds); expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
    QuadricSurface invalid = new QuadricSurface(new Vector3Dd(Double.POSITIVE_INFINITY,1,1),new Vector3Dd(),new Vector3Dd(),-1);
    invalid.getBounds(bounds); expectEq(GeometryBounds.Kind.Indeterminate, bounds.getKind());
    QuadricSurface overflow = new QuadricSurface(new Vector3Dd(1e200,1e200,1e200),new Vector3Dd(),new Vector3Dd(),-1);
    overflow.getBounds(bounds); expectEq(GeometryBounds.Kind.Indeterminate, bounds.getKind());
}

    @Test
    void QuadricSurfaceContract_MixedTermSolidIntervalsAgreeWithAnalyticRoots()
    {
    QuadricSurface quadric = new QuadricSurface(new Vector3Dd(2,2,4),new Vector3Dd(2,0,0),new Vector3Dd(0,6,-4),-2);
    final Ray ray=Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0));
    RaySolidClassification result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, quadric.classifyRay(ray,new RayCrossingQuery(0,10),result));
    expectEq(2, result.boundaries.size()); expectEq(3, result.insideSegments.size());
    expectDoubleEq(2, result.boundaries.get(0).t); expectDoubleEq(4, result.boundaries.get(1).t);
    expectFalse(result.insideSegments.get(0)); expectTrue(result.insideSegments.get(1)); expectFalse(result.insideSegments.get(2));
}

    @Test
    void BoxContract_ArbitraryBoundsSupportSharedSolidQueriesAndDeferredDetail()
    {
    Box box = new Box(new Vector3Dd(-2,-1,-3),new Vector3Dd(4,3,1));
    final Ray ray=Ray.fromAffine(new Vector3Dd(-4,0,0),new Vector3Dd(1,0,0));
    RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    RayBoundary boundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, box.queryFirstHit(ray,new RayCrossingQuery(0,10),hit,boundary));
    expectDoubleEq(2, boundary.t); expectEq(box, boundary.geometry);
    expectDoubleEq(-2, hit.point.x()); expectDoubleEq(-1, hit.normal.x());
    boundary.normalReversed=true;
    RayHit deferred = new RayHit(RayHit.DETAIL_NORMAL,false);
    expectEq(RayFirstHitStatus.Hit, box.querySurfaceDetail(ray,boundary,deferred));
    expectDoubleEq(1, deferred.normal.x());
    RaySolidClassification result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, box.classifyRay(ray,new RayCrossingQuery(0,10),result));
    expectEq(2, result.boundaries.size()); expectEq(3, result.insideSegments.size());
    expectDoubleEq(2, result.boundaries.get(0).t); expectDoubleEq(8, result.boundaries.get(1).t);
    expectFalse(result.insideSegments.get(0)); expectTrue(result.insideSegments.get(1)); expectFalse(result.insideSegments.get(2));
    expectEq(GeometryContainment.Inside, box.classifyPoint(new Vector3Dd(0,0,0),0));
    expectTrue(box.doIntersectionFirstHit(ray,hit)); expectDoubleEq(-2, hit.point.x());
    Geometry clone=box.copyGeometry(); expectNe(null, clone);
    GeometryBounds copied = new GeometryBounds(); clone.getBounds(copied);
    expectEq(GeometryBounds.Kind.Finite, copied.getKind());
    expectDoubleEq(-2, copied.getMinimum().x()); expectDoubleEq(4, copied.getMaximum().x());
    box.getRawBounds()[0]=new Vector3Dd(-1,-1,-3);
    expectEq(RayFirstHitStatus.Hit, box.queryFirstHit(ray,new RayCrossingQuery(0,10),null,boundary));
    expectDoubleEq(3, boundary.t);
}

    @Test
    void PlanarHalfSpaceContract_DeferredDetailPreservesLeafAndOrientation()
    {
    PlanarHalfSpace solid = new PlanarHalfSpace(2,0,0,-4);
    final Ray ray=Ray.fromAffine(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0));
    RayBoundary boundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, solid.queryFirstHit(ray,new RayCrossingQuery(0,10),null,boundary));
    expectEq(solid, boundary.geometry); expectDoubleEq(5, boundary.t);
    boundary.normalReversed=true;
    RayHit detail = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL,false);
    expectEq(RayFirstHitStatus.Hit, solid.querySurfaceDetail(ray,boundary,detail));
    expectDoubleEq(2, detail.point.x()); expectDoubleEq(-1, detail.normal.x());
}

    @Test
    void CsgStructureContract_OrderedBorrowedOperandsAndNestedTreesPreserveIdentity()
    {
    SimpleBody first = new SimpleBody();
    SimpleBody second = new SimpleBody();
    first.setGeometry(new Sphere(1));
    second.setGeometry(new Sphere(2));
    second.setPosition(new Vector3Dd(4,0,0));
    CsgBodyOperand firstOperand = new CsgBodyOperand(first);
    CsgBodyOperand secondOperand = new CsgBodyOperand(second);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(firstOperand)); expectTrue(difference.addOperand(secondOperand));
    expectEq(CsgOperation.DIFFERENCE, difference.getOperation());
    expectEq(2, difference.getOperandCount());
    expectEq(first.getGeometry(), difference.getOperand(0).getGeometry());
    expectEq(second.getGeometry(), difference.getOperand(1).getGeometry());
    expectEq(null, difference.getOperand(0).getChildExpression());
    expectDoubleEq(4, difference.getOperand(1).getAffinePlacement().multiply(new Vector3Dd()).x());
    Matrix4x4d[] forward = {new Matrix4x4d()};
    Matrix4x4d[] inverse = {new Matrix4x4d()};
    expectEq(AffinePlacementStatus.Valid, difference.getOperand(1).queryAffinePlacement(forward,inverse));
    expectDoubleEq(-4, inverse[0].multiply(new Vector3Dd()).x());
    second.setPosition(new Vector3Dd(7,0,0));
    expectDoubleEq(7, difference.getOperand(1).getAffinePlacement().multiply(new Vector3Dd()).x());
    expectEq(null, difference.getOperand(-1)); expectEq(null, difference.getOperand(2));
    expectFalse(difference.addOperand(difference));
    CsgTree root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(difference));
    expectEq(difference, root.getOperand(0).getChildExpression());
    expectEq(null, root.getOperand(0).getGeometry());
    expectEq(CsgOperation.DIFFERENCE, root.getOperand(0).getChildExpression().getOperation());
    expectFalse(difference.addOperand(root));
    expectEq(2, difference.getOperandCount());
}

    @Test
    void AffinePlacementContract_ValidatesPairsAndRejectsInvalidOrInconsistentMatrices()
    {
    final Matrix4x4d forward=new Matrix4x4d().translation(3,-2,1).multiply(
        Matrix4x4d.identityMatrix().withVal(0,1,0.5));
    final Matrix4x4d inverse=Matrix4x4d.identityMatrix().withVal(0,1,-0.5).multiply(
        new Matrix4x4d().translation(-3,2,-1));
    expectEq(AffinePlacementStatus.Valid, AffinePlacement.validate(forward,inverse));
    expectEq(AffinePlacementStatus.Invalid, AffinePlacement.validate(
        new Matrix4x4d().scale(0,1,1),new Matrix4x4d()));
    expectEq(AffinePlacementStatus.Indeterminate, AffinePlacement.validate(forward,new Matrix4x4d()));
    expectEq(AffinePlacementStatus.Indeterminate, AffinePlacement.validate(
        new Matrix4x4d().scale(1e-200,1e-200,1),new Matrix4x4d().scale(1e200,1e200,1)));
    expectEq(AffinePlacementStatus.Invalid, AffinePlacement.validate(
        forward.withVal(3,0,1),inverse));
    expectEq(AffinePlacementStatus.Invalid, AffinePlacement.validate(
        forward.withVal(0,0,Double.POSITIVE_INFINITY),inverse));
}

    @Test
    void AffinePlacementContract_SceneCrossingsKeepParametersAndInstanceProvenance()
    {
    SimpleBody body = new SimpleBody();
    Sphere sphere=new Sphere(1);
    body.setGeometry(sphere);
    body.setPosition(new Vector3Dd(3,0,0));
    body.setScale(new Vector3Dd(-2,1,1));
    final Ray ray=Ray.fromAffine(new Vector3Dd(-1,0,0),new Vector3Dd(2,0,0));
    RayCrossingTestCollector result = new RayCrossingTestCollector();
    expectEq(RayCrossingSupport.Supported, body.emitRayCrossings(ray,new RayCrossingQuery(0,10),result));
    expectEq(2, result.roots.size());
    expectDoubleEq(1, result.roots.get(0)); expectDoubleEq(3, result.roots.get(1));
    expectEq(sphere, result.geometries.get(0)); expectEq(1, result.instanceCounts.get(0));
    expectEq(RayCrossingKind.Entering, result.kinds.get(0)); expectEq(RayCrossingKind.Leaving, result.kinds.get(1));
}

    @Test
    void AffinePlacementContract_ExplicitShearDrivesQueriesRenderingAndEditing()
    {
    SimpleBody body = new SimpleBody();
    Sphere sphere=new Sphere(1);
    body.setGeometry(sphere);
    final Matrix4x4d forward=new Matrix4x4d().translation(3,0,0).multiply(
        Matrix4x4d.identityMatrix().withVal(0,1,0.5)).multiply(new Matrix4x4d().scale(2,1,1));
    final Matrix4x4d inverse=new Matrix4x4d().scale(0.5,1,1).multiply(
        Matrix4x4d.identityMatrix().withVal(0,1,-0.5)).multiply(new Matrix4x4d().translation(-3,0,0));
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward,inverse));
    expectFalse(body.supportsDecomposedPlacement());
    final Ray ray=Ray.fromAffine(forward.multiply(new Vector3Dd(-3,0,0)),
        forward.withoutTranslation().multiply(new Vector3Dd(1,0,0)));
    RaySolidClassification classification = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray,new RayCrossingQuery(0,10),classification));
    expectEq(2, classification.boundaries.size());
    expectNear(2, classification.boundaries.get(0).t, 1e-12);
    RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, true);
    expectTrue(body.doIntersectionFirstHit(ray,hit));
    expectNe(null, hit.getRay()); expectNear(2, hit.getRay().getT(), 1e-12);
    expectNear(1, hit.point.x(), 1e-12);
    expectNear(-0.8944271909999159, hit.normal.x(), 1e-12);
    expectNear(0.4472135954999579, hit.normal.y(), 1e-12);
    expectEq(GeometryContainment.Inside, body.classifyPoint(forward.multiply(new Vector3Dd()),0));
    expectEq(GeometryContainment.Inside, body.classifyPoint(forward.multiply(new Vector3Dd()),0.1));
    final Matrix4x4d before=body.getAffinePlacement();
    expectEq(AffinePlacementStatus.Invalid, body.setAffinePlacement(new Matrix4x4d().scale(0,1,1),new Matrix4x4d()));
    expectTrue(body.getAffinePlacement().epsilonEquals(before,0));
    body.setPosition(new Vector3Dd(5,1,0));
    Matrix4x4d[] moved = {new Matrix4x4d()};
    Matrix4x4d[] movedInverse = {new Matrix4x4d()};
    expectEq(AffinePlacementStatus.Valid, body.queryAffinePlacement(moved,movedInverse));
    expectNear(5, moved[0].multiply(new Vector3Dd()).x(), 1e-12);
    expectNear(0, movedInverse[0].multiply(new Vector3Dd(5,1,0)).length(), 1e-12);
    body.resetAffinePlacement(); expectTrue(body.supportsDecomposedPlacement());
    expectTrue(body.getAffinePlacement().epsilonEquals(new Matrix4x4d(),0));
}

    @Test
    void FloatingIntervalContract_DeterministicStressContainsExactRationalBounds()
    {
    expectTrue(FloatingInterval.isSupportedEnvironment());
    // Generated by generate_interval_stress_reference.py using exact rationals.
    final double[][] cases = {
        {7.146546754040844e-69, 7.146546754040845e-69, 0, 1.4293093508081688e-68, 1.429309350808169e-68},
        {-3.5566057525170395e+21, -1.4583237856775876e+33, 1, 1.4583237856740308e+33, 1.458323785674031e+33},
        {-8.11391527557009e+84, -1.5651942759869695e-82, 2, 1269.985374516554, 1269.9853745165542},
        {-7.005861629145818e+116, 2.093032075091621e+26, 3, -3.347230896516071e+90, -3.3472308965160706e+90},
        {1.7111650208095105e-80, 3951736602112120.0, 0, 3951736602112120.0, 3951736602112120.5},
        {-3.899596746493458e-148, -1.4313098573698764e+53, 1, 1.4313098573698762e+53, 1.4313098573698764e+53},
        {-2.9147068561821865e+67, 3.2035585822490825e-138, 2, -9.337434163862687e-71, -9.337434163862685e-71},
        {-1.0036314697615076e+56, 8.595075937226869e-51, 3, -1.1676819112377978e+106, -1.1676819112377976e+106},
        {-1.690969726056025e-14, -5.55933477059183e+44, 0, -5.559334770591831e+44, -5.55933477059183e+44},
        {-4.3388294890186107e+42, -1.7015852866887574e+116, 1, 1.701585286688757e+116, 1.7015852866887574e+116},
        {-1.894326392189219e+53, 2.329431193695396e-17, 2, -4.412702989006026e+36, -4.412702989006025e+36},
        {-1.0208457775245834e+153, -1.0208457775245832e+153, 3, 1.0, 1.0000000000000002},
        {-4.562818881199697e+85, 2.7172586259056785e-72, 0, -4.562818881199697e+85, -4.562818881199696e+85},
        {2.960898220504463e-40, 6.649198969387046e-112, 1, 2.9608982205044627e-40, 2.960898220504463e-40},
        {-6646668405780803.0, 2.848094402563231e+110, 2, -1.893033908219818e+126, -1.8930339082198176e+126},
        {8.454772843731548e-50, 1.1089667819206423e+73, 3, 7.624009106105553e-123, 7.624009106105555e-123},
        {2.8086253420183986e+58, -1.0792115406317105e-124, 0, 2.808625342018398e+58, 2.8086253420183986e+58},
        {1.5882786408807579e-99, 2.964314447997346e-102, 1, 1.5853143264327605e-99, 1.5853143264327607e-99},
        {-2.680570190843948e+104, 6.248135193642002e+67, 2, -1.6748564948439732e+172, -1.674856494843973e+172},
        {-4.931861314370597e-21, -3.391881745352832e-46, 3, 1.4540192390632924e+25, 1.4540192390632926e+25},
        {1.3741384403734075e+70, -2.044099822357928e+149, 0, -2.044099822357928e+149, -2.0440998223579277e+149},
        {7.652799199332246e+21, 3.9278002242702226e-89, 1, 7.652799199332245e+21, 7.652799199332246e+21},
        {3.75199107563365e-22, 3.7519910756336506e-22, 2, 1.4077437031634557e-43, 1.4077437031634559e-43},
        {-9.094398980472972e-147, 1.3287552253160717e-133, 3, -6.844299692826934e-14, -6.844299692826933e-14},
        {1.0265886784318475e+108, -1.916315795976818e+61, 0, 1.0265886784318473e+108, 1.0265886784318475e+108},
        {1.1188068606861152e+53, 1.2393022779389087e+70, 1, -1.2393022779389087e+70, -1.2393022779389085e+70},
        {4.027621290101606e-63, 7.831154528608348e-129, 2, 3.154092470549859e-191, 3.1540924705498595e-191},
        {1.4998938791684562e+66, -1.0134956380621436e-78, 3, -1.4799213956523103e+144, -1.4799213956523101e+144},
        {-3.291038874024351e+20, -4.5813304442681997e-125, 0, -3.2910388740243515e+20, -3.291038874024351e+20},
        {6.628538733888976e-24, -154744931.69699302, 1, 154744931.69699302, 154744931.69699305},
        {-9.205481630175304e-152, -1.3587824594568078e+55, 2, 1.2508246969934063e-96, 1.2508246969934066e-96},
        {-7.960151267585567e-87, -7.998774084540255e-37, 3, 9.951714079499587e-51, 9.951714079499588e-51},
        {-4.630168634212488e+158, -4.834134258630453e-18, 0, -4.630168634212489e+158, -4.630168634212488e+158},
        {3.847223730397442e-117, 3.847223730397443e-117, 1, -7.044203657368268e-133, -7.044203657368268e-133},
        {-6.833021388605202e-77, -2.7067301603686296e+107, 2, 1.8495145078981634e+31, 1.8495145078981637e+31},
        {-2.715791966617085e-97, -1.816861271330557e-46, 3, 1.4947712351357492e-51, 1.4947712351357495e-51},
        {7.971032764418196e+55, -7.402207684804431e-112, 0, 7.971032764418195e+55, 7.971032764418196e+55},
        {-4.882773892133515e+24, 4.0718275219942044e-82, 1, -4.882773892133516e+24, -4.882773892133515e+24},
        {-2.4793688191233346e-64, 2.3867158809198215e-110, 2, -5.917548935259088e-174, -5.917548935259087e-174},
        {4.947951554127676e+144, 3.666361594251627e+40, 3, 1.3495536179206691e+104, 1.3495536179206693e+104},
        {-2.3530147634202964e-72, -5.951630108185943e-73, 0, -2.948177774238891e-72, -2.9481777742388903e-72},
        {1.5835292443330722e-25, 5.230194477104014e-79, 1, 1.583529244333072e-25, 1.5835292443330722e-25},
        {-8.439740587736398e-115, 9.917447007016836e-09, 2, -8.370068003184486e-123, -8.370068003184485e-123},
        {2.1679518598992123e-100, 8.955389978715759e-95, 3, 2.4208346761579058e-06, 2.420834676157906e-06},
        {-3.248475655729729e-55, -3.2484756557297285e-55, 0, -6.496951311459458e-55, -6.496951311459457e-55},
        {-2.5154338451103455e-142, 1.2809559014651951e-48, 1, -1.2809559014651952e-48, -1.2809559014651951e-48},
        {-370705540480549.56, -1.6058651255974986e+60, 2, 5.953030993234863e+74, 5.953030993234864e+74},
        {-8.912909900324393e-70, -2.392715657549679e+78, 3, 3.725018420889962e-148, 3.725018420889963e-148},
        {7.521179311428108e+19, 4.300536432303738e-10, 0, 7.521179311428108e+19, 7.52117931142811e+19},
        {-4.6044121992001574e-126, -1.321209905758332e+67, 1, 1.3212099057583318e+67, 1.321209905758332e+67},
        {-5.8223902096831565e+134, 1.466369558975731e-114, 2, -8.537775763957706e+20, -8.537775763957704e+20},
        {5.0903072198465135e+120, -1.4937149639888107e+84, 3, -3.407816981529982e+36, -3.407816981529981e+36},
        {8.526842899455951e-27, -1.999149353405858e-07, 0, -1.999149353405858e-07, -1.9991493534058578e-07},
        {-4.092137893802181e+140, 1.5403752585329928e+89, 1, -4.092137893802182e+140, -4.092137893802181e+140},
        {-1.156487437800255e-147, -1.5947101103409532e-31, 2, 1.8442622095423708e-178, 1.844262209542371e-178},
        {1.696053312351897e+104, 1.6960533123518972e+104, 3, 0.9999999999999998, 0.9999999999999999},
        {-2.5248682808900746e-08, -4.261666906767745e+16, 0, -4.261666906767746e+16, -4.261666906767745e+16},
        {5.2751611350313e+116, 0.001492527481737261, 1, 5.2751611350312994e+116, 5.2751611350313e+116},
        {-1.2280760209282201e+66, -5.6334765238469545e+20, 2, 6.918337433398509e+86, 6.91833743339851e+86},
        {6.357995500901358e-124, -1.317484762865612e-131, 3, -48258588.48699183, -48258588.48699182},
        {4.487976006258921e-134, -8.20423364170612e+158, 0, -8.20423364170612e+158, -8.204233641706119e+158},
        {1.4366870856264788e+68, 8.423740531088334e+155, 1, -8.423740531088334e+155, -8.423740531088333e+155},
        {-7.288522672983534e+32, -8.27933867935607e+71, 2, 6.034414768179625e+104, 6.034414768179627e+104},
        {-3.2203343329787468e+150, -4.3538290944758115e+85, 3, 7.396556601325357e+64, 7.396556601325358e+64},
    };
    for (int i=0;i<64;++i) {
        final FloatingInterval a=FloatingInterval.exact(cases[i][0]);
        final FloatingInterval b=FloatingInterval.exact(cases[i][1]);
        FloatingInterval result = new FloatingInterval();
        switch ((int)(cases[i][2])) {
        case 0: result=a.add(b); break;
        case 1: result=a.subtract(b); break;
        case 2: result=a.multiply(b); break;
        default: result=a.divide(b); break;
        }
        expectTrue(result.isValid(), "" + i);
        expectLe(result.getLower(), cases[i][3], "" + i);
        expectGe(result.getUpper(), cases[i][4], "" + i);
    }
}

    @Test
    void CsgPointClassificationContract_PlacedSpheresAndNestedBooleanOperations()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    a.setGeometry(new Sphere(2)); b.setGeometry(new Sphere(1));
    b.setPosition(new Vector3Dd(1,0,0));
    CsgBodyOperand aOperand = new CsgBodyOperand(a);
    CsgBodyOperand bOperand = new CsgBodyOperand(b);
    CsgTree united = new CsgTree(CsgOperation.UNION);
    CsgTree intersected = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(aOperand)); expectTrue(united.addOperand(bOperand));
    expectTrue(intersected.addOperand(aOperand)); expectTrue(intersected.addOperand(bOperand));
    expectTrue(difference.addOperand(aOperand)); expectTrue(difference.addOperand(bOperand));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(united,new Vector3Dd(-1,0,0),0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(united,new Vector3Dd(4,0,0),0.1));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(intersected,new Vector3Dd(1,0,0),0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(intersected,new Vector3Dd(-1,0,0),0.1));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(difference,new Vector3Dd(-1,0,0),0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference,new Vector3Dd(1,0,0),0.1));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(difference,new Vector3Dd(0,0,0),0.1));
    CsgTree nested = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(nested.addOperand(united)); expectTrue(nested.addOperand(aOperand));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(nested,new Vector3Dd(-1,0,0),0.1));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(nested,new Vector3Dd(),-1));
    CsgTree empty = new CsgTree(CsgOperation.UNION);
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(empty,new Vector3Dd(),0));
}

    @Test
    void CsgPointClassificationContract_PreservesUncertaintyAndProvesMaskedResults()
    {
    CsgTestOperand inside = new CsgTestOperand(GeometryContainment.Inside);
    CsgTestOperand outside = new CsgTestOperand(GeometryContainment.Outside);
    CsgTestOperand boundary = new CsgTestOperand(GeometryContainment.Boundary);
    CsgTestOperand unknown = new CsgTestOperand(GeometryContainment.Indeterminate);
    CsgTestOperand unsupported = new CsgTestOperand(GeometryContainment.Unsupported);
    CsgTestOperand invalid = new CsgTestOperand(GeometryContainment.InvalidInput);
    CsgTree hiddenBoundary = new CsgTree(CsgOperation.UNION);
    CsgTree exposedBoundary = new CsgTree(CsgOperation.UNION);
    expectTrue(hiddenBoundary.addOperand(inside)); expectTrue(hiddenBoundary.addOperand(boundary));
    expectTrue(exposedBoundary.addOperand(outside)); expectTrue(exposedBoundary.addOperand(boundary));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(hiddenBoundary,new Vector3Dd(),0.1));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(exposedBoundary,new Vector3Dd(),0.1));
    CsgTree unknownUnion = new CsgTree(CsgOperation.UNION);
    CsgTree unknownIntersection = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(unknownUnion.addOperand(outside)); expectTrue(unknownUnion.addOperand(unknown));
    expectTrue(unknownIntersection.addOperand(inside)); expectTrue(unknownIntersection.addOperand(unsupported));
    expectEq(GeometryContainment.Indeterminate, CsgPointClassification.classify(unknownUnion,new Vector3Dd(),0.1));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(unknownIntersection,new Vector3Dd(),0.1));
    CsgTree decisive = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree malformed = new CsgTree(CsgOperation.UNION);
    expectTrue(decisive.addOperand(outside)); expectTrue(decisive.addOperand(unknown));
    expectTrue(malformed.addOperand(inside)); expectTrue(malformed.addOperand(invalid));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(decisive,new Vector3Dd(),0.1));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(malformed,new Vector3Dd(),0.1));
}

    @Test
    void QuadricRayExpansionContract_ExpansionMatchesQuadricAlongRay()
    {
    final QuadricSurface quadric = new QuadricSurface(new Vector3Dd(1.5, -0.75, 2.0), new Vector3Dd(0.5, -1.25, 0.375),
        new Vector3Dd(-0.25, 1.0, 0.625), -3.0);
    // new Q(origin) < 0 and a > 0, so the ray crosses the surface twice.
    final Vector3Dd origin = new Vector3Dd(0.25, -0.125, 0.5);
    final Vector3Dd direction = new Vector3Dd(1.0, 0.25, 0.5);
    final double a = QuadricRayExpansion.squareTerm(quadric, true,
        direction.multiply(direction), QuadricRayExpansion.mixedProducts(direction, direction));
    final double b = QuadricRayExpansion.linearTerm(quadric, origin.multiply(direction), direction,
        QuadricRayExpansion.symmetricMixedProducts(origin, direction));
    final double c = QuadricRayExpansion.constantTerm(quadric,
        QuadricRayExpansion.constantTermWithoutMixed(quadric, origin.multiply(origin), origin),
        QuadricRayExpansion.mixedProducts(origin, origin));
    expectNear(c, quadric.evaluate(origin), 1e-12);
    for ( int i = -4; i <= 4; i++ ) {
        final double t = 0.75 * i;
        expectNear((a*t + b)*t + c, quadric.evaluate(origin.add(direction.multiply(t))), 1e-11);
    }
    expectGt(a, 0.0);
    expectLt(c, 0.0);
    double[] first = {0};
    double[] second = {0};
    expectTrue(QuadricSurface.solve(a, b, c, first, second));
    expectGt(first[0], 0.0);
    expectLt(second[0], 0.0);
    expectNear(quadric.evaluate(origin.add(direction.multiply(first[0]))), 0.0, 1e-9);
    expectNear(quadric.evaluate(origin.add(direction.multiply(second[0]))), 0.0, 1e-9);
}

    @Test
    void QuadricRayExpansionContract_QuadricWithoutSquareTermsIsLinearAlongRay()
    {
    final QuadricSurface plane = new QuadricSurface(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 2), -1.0);
    final Vector3Dd origin = new Vector3Dd(0.25, 0.5, -1.5);
    final Vector3Dd direction = new Vector3Dd(0, 0.5, 1);
    final double a = QuadricRayExpansion.squareTerm(plane, false,
        direction.multiply(direction), QuadricRayExpansion.mixedProducts(direction, direction));
    final double b = QuadricRayExpansion.linearTerm(plane, origin.multiply(direction), direction,
        QuadricRayExpansion.symmetricMixedProducts(origin, direction));
    final double c = QuadricRayExpansion.constantTerm(plane,
        QuadricRayExpansion.constantTermWithoutMixed(plane, origin.multiply(origin), origin),
        QuadricRayExpansion.mixedProducts(origin, origin));
    expectEq(a, 0.0);
    double[] first = {0};
    double[] second = {0};
    expectTrue(QuadricSurface.solve(a, b, c, first, second));
    expectDoubleEq(first[0], 2.0);
    expectDoubleEq(second[0], 2.0);
}

}
