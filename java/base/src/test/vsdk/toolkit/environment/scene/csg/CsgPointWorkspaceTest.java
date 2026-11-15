package vsdk.toolkit.environment.scene.csg;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SimpleBody;

import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/csg_point_workspace_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class CsgPointWorkspaceTest
{

// Random trees with repeated operands, a sheared placed subtree, an uncertain
// operand and points on and near boundaries: the containment-only query must
// give the result of the full classification for every point and tolerance.
    @Test
    void CsgPointWorkspaceContract_ContainmentMatchesFullClassificationOnRandomTrees()
    {
    int seed=97531;
    final int bodyCount=5;
    SimpleBody[] bodies = new SimpleBody[bodyCount];
    for (int i=0;i<bodyCount;++i) bodies[i] = new SimpleBody();
    for (int i=0;i<bodyCount;++i) {
        if (i%2==0) bodies[i].setGeometry(new Sphere(0.6+0.1*i));
        else bodies[i].setGeometry(new Box(1.0+0.2*i,1.4,0.9+0.1*i));
    }
    UncertifiedTestBody opaque = new UncertifiedTestBody();
    opaque.setGeometry(new Sphere(0.7));
    final double[] tolerances = {0,1e-6,0.05};
    final double[][] shear = {{1.3,0.2,0,0.1},{0,0.8,0.3,-0.2},{0.1,0,1.1,0.05},{0,0,0,1}};
    final Matrix4x4d forward = new Matrix4x4d(shear);
    CsgPointWorkspace workspace = new CsgPointWorkspace();
    long compared=0, definite=0;
    for (int trial=0;trial<120;++trial) {
        for (int i=0;i<bodyCount;++i) {
            seed=seed*1103515245+12345; final double x=((seed>>>8)%2000)/1000.0-1.0;
            seed=seed*1103515245+12345; final double y=((seed>>>8)%2000)/2000.0-0.5;
            bodies[i].setPosition(new Vector3Dd(x,y,0.1*i-0.2));
        }
        opaque.setPosition(new Vector3Dd(0.3*(trial%5)-0.6,0,0));
        CsgBodyOperand o0 = new CsgBodyOperand(bodies[0]);
        CsgBodyOperand o1 = new CsgBodyOperand(bodies[1]);
        CsgBodyOperand o2 = new CsgBodyOperand(bodies[2]);
        CsgBodyOperand o3 = new CsgBodyOperand(bodies[3]);
        CsgBodyOperand o4 = new CsgBodyOperand(bodies[4]);
        CsgBodyOperand again = new CsgBodyOperand(bodies[1]);
        CsgBodyOperand u = new CsgBodyOperand(opaque);
        final CsgOperation[] ops = {CsgOperation.UNION,CsgOperation.INTERSECTION,CsgOperation.DIFFERENCE};
        CsgTree inner = new CsgTree(ops[trial%3]);
        CsgTree middle = new CsgTree(ops[(trial/3)%3]);
        CsgTree root = new CsgTree(ops[(trial/9)%3]);
        expectTrue(inner.addOperand(o0)); expectTrue(inner.addOperand(o1)); expectTrue(inner.addOperand(again));
        if (trial%4==0) expectTrue(inner.addOperand(u));
        CsgPlacedExpression placed = new CsgPlacedExpression(inner);
        expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(trial%2!=0 ? forward : Matrix4x4d.identityMatrix(),
                trial%2!=0 ? forward.inverse() : Matrix4x4d.identityMatrix()));
        expectTrue(middle.addOperand(o3)); expectTrue(middle.addOperand(placed));
        expectTrue(root.addOperand(middle)); expectTrue(root.addOperand(o4)); expectTrue(root.addOperand(o2));
        if (trial%6==0) expectTrue(root.addOperand(o2));
        for (int k=0;k<40;++k) {
            Vector3Dd p = new Vector3Dd();
            if (k<20) {
                seed=seed*1103515245+12345; final double x=((seed>>>8)%3000)/1000.0-1.5;
                seed=seed*1103515245+12345; final double y=((seed>>>8)%2000)/1000.0-1.0;
                p=new Vector3Dd(x,y,0.05);
            }
            else {
                // Points on the spheres and box faces.
                final SimpleBody b =bodies[k%bodyCount];
                final double r=k%2==0 ? 0.6+0.1*(k%bodyCount) : 0.5*(1.0+0.2*(k%bodyCount));
                p=b.getPosition().add(new Vector3Dd(r,0,0));
            }
            for (int t=0;t<3;++t) {
                final GeometryContainment full=CsgPointClassification.classify(root,p,tolerances[t]);
                final GeometryContainment lean=
                    CsgPointClassification.classifyContainment(root,p,tolerances[t],workspace);
                expectEq(full, lean, "" + "trial " + trial + " point " + k + " tolerance " + t);
                ++compared;
                if (full==GeometryContainment.Inside || full==GeometryContainment.Outside ||
                    full==GeometryContainment.Boundary) ++definite;
                expectEq(0, workspace.results.size());
                expectEq(0, workspace.path.size());
            }
        }
    }
    expectGt(definite, compared/2);
    expectLt(definite, compared);
}

    @Test
    void CsgPointWorkspaceContract_OperandContainmentAndInvalidInput()
    {
    SimpleBody ball = new SimpleBody();
    ball.setGeometry(new Sphere(1));
    CsgBodyOperand a = new CsgBodyOperand(ball);
    CsgPointWorkspace workspace = new CsgPointWorkspace();
    expectEq(GeometryContainment.Inside, CsgPointClassification.classifyOperandContainment(a,new Vector3Dd(0,0,0),0,workspace));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyOperandContainment(a,new Vector3Dd(1,0,0),1e-9,workspace));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classifyOperandContainment(a,new Vector3Dd(3,0,0),0,workspace));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classifyOperandContainment(a,new Vector3Dd(0,0,0),-1,workspace));
    CsgTree tree = new CsgTree(CsgOperation.UNION);
    expectTrue(tree.addOperand(a));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classifyContainment(tree,new Vector3Dd(0,Double.NaN,0),0,workspace));
}

// The heuristic gives the same events with a reused workspace as with a fresh
// one, query after query.
    @Test
    void CsgPointWorkspaceContract_HeuristicSelectionReusesTheWorkspace()
    {
    SimpleBody a = new SimpleBody();
    SimpleBody b = new SimpleBody();
    SimpleBody c = new SimpleBody();
    a.setGeometry(new Sphere(2)); b.setGeometry(new Sphere(1)); c.setGeometry(new Sphere(1));
    c.setPosition(new Vector3Dd(1.5,0,0));
    CsgBodyOperand oa = new CsgBodyOperand(a);
    CsgBodyOperand ob = new CsgBodyOperand(b);
    CsgBodyOperand oc = new CsgBodyOperand(c);
    CsgTree inner = new CsgTree(CsgOperation.UNION);
    CsgTree root = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(inner.addOperand(ob)); expectTrue(inner.addOperand(oc));
    expectTrue(root.addOperand(oa)); expectTrue(root.addOperand(inner));
    CsgPointWorkspace workspace = new CsgPointWorkspace();
    CsgRaySurfaceEvents fresh = new CsgRaySurfaceEvents();
    CsgRaySurfaceEvents reused = new CsgRaySurfaceEvents();
    for (int i=0;i<20;++i) {
        final Ray ray=Ray.fromAffine(new Vector3Dd(-5,0.1*(i%7)-0.3,0.05*(i%3)),new Vector3Dd(1,0,0));
        expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(root,ray,new RayCrossingQuery(0,20),1e-9,fresh));
        expectEq(RaySolidQueryStatus.Supported, CsgCrossingMembershipFilter.select(root,ray,new RayCrossingQuery(0,20),1e-9,workspace,reused));
        expectEq(fresh.size(), reused.size());
        for (long k=0;k<fresh.size();++k) {
            expectEq(fresh.get(k).leafIndex, reused.get(k).leafIndex);
            expectEq(fresh.get(k).boundaryIndex, reused.get(k).boundaryIndex);
            expectDoubleEq(fresh.getT(k), reused.getT(k));
        }
        // Difference: both outer crossings plus the hole crossings inside the ball.
        expectEq(4, reused.size());
    }
}
}
