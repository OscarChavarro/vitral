package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import vsdk.toolkit.common.linealAlgebra.*;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;

/**
Validates the robust {@link PolyhedralBoundedSolidPredicates#quantitativeInvisibility}
against an independent ray-march of the APPE1967 featured object (the union of 12
axis-aligned boxes). The march is a sampling oracle, so at a near-tangent line of
sight it cannot tell a measure-zero graze from a real thin occluder; those cases
are genuinely ambiguous (either visible or hidden is acceptable). The test
therefore measures the maximal INSIDE-RUN DEPTH along each line of sight and only
flags a disagreement as a real error when the depth is unambiguous: clearly
substantial (a real occluder, QI must be > 0) or clearly ~zero (a tangent, QI
must be 0). The ambiguous in-between band is skipped. The committed scan uses a 30deg / 8-sample grid for speed; the full 15deg / 12-sample scan was also verified to report 0 real errors during development.
 */
class PredicatesQiScanTest {
    static final double[][] BOX = {
        {0.5,0.1,0.1, 0.5,0.1,0.1},{0.5,0.9,0.1, 0.5,0.1,0.1},
        {0.1,0.5,0.1, 0.1,0.5,0.1},{0.9,0.5,0.1, 0.1,0.5,0.1},
        {0.1,0.5,0.1, 0.1,0.5,0.1},{0.1,0.5,0.9, 0.1,0.5,0.1},
        {0.1,0.1,0.5, 0.1,0.1,0.5},{0.1,0.9,0.5, 0.1,0.1,0.5},
        {0.3,0.5,0.5, 0.3,0.1,0.1},{0.5,0.5,0.5, 0.1,0.1,0.5},
        {0.7,0.5,0.9, 0.3,0.1,0.1},{0.9,0.5,0.9, 0.1,0.5,0.1}
    };
    static boolean bi(Vector3Dd p){ for(double[] b:BOX) if(Math.abs(p.x()-b[0])<=b[3]-1e-7&&Math.abs(p.y()-b[1])<=b[4]-1e-7&&Math.abs(p.z()-b[2])<=b[5]-1e-7) return true; return false; }
    static boolean coarseOccluded(Vector3Dd oe, Vector3Dd lp){ Vector3Dd d=lp.subtract(oe); for(int i=1;i<2000;i++){double s=(double)i/2000; if(s>=1-3e-4)break; if(bi(oe.add(d.multiply(s)))) return true;} return false; }
    // max inside-run depth (in local units) of the segment oeLocal -> lpLocal
    static double insideDepth(Vector3Dd oe, Vector3Dd lp){
        Vector3Dd d=lp.subtract(oe); double L=d.length(); int N=200000;
        boolean prev=false; double runStart=0,maxDepth=0;
        for(int i=0;i<=N;i++){ double s=(double)i/N; if(s>=1-3e-4) break; boolean in=bi(oe.add(d.multiply(s)));
            if(in&&!prev) runStart=s; if(!in&&prev){double dp=(s-runStart)*L; if(dp>maxDepth)maxDepth=dp;} prev=in; }
        if(prev){double dp=(1-runStart)*L; if(dp>maxDepth)maxDepth=dp;}
        return maxDepth;
    }
    @Test void quantitativeInvisibility_matchesGroundTruthOutsideAmbiguousBand(){
        PolyhedralBoundedSolid solid=SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        Vector3Dd eye=new Vector3Dd(2,-1,2);
        double CLEAR_OCCLUDER=5.0e-3, CLEAR_TANGENT=5.0e-5;
        int errors=0;
        for(int za=0; za<360; za+=30) for(int xa=0; xa<360; xa+=30){
            Matrix4x4d R=new Matrix4x4d().axisRotation(Math.toRadians(za),0,0,1).multiply(new Matrix4x4d().axisRotation(Math.toRadians(xa),1,0,0));
            Matrix4x4d Rinv=R.inverse(); Vector3Dd oe=Rinv.multiply(eye);
            for(int ei=0; ei<solid.getEdgesList().size(); ei++){
                var e=solid.getEdgesList().get(ei);
                Vector3Dd a=e.leftHalf.startingVertex.position, b=e.rightHalf.startingVertex.position;
                for(int k=1;k<=8;k++){ double t=k/9.0;
                    Vector3Dd lp=a.multiply(1-t).add(b.multiply(t));
                    int qi=PolyhedralBoundedSolidPredicates.quantitativeInvisibility(solid,oe,lp);
                    boolean qiOccluded=qi>0;
                    boolean coarse=coarseOccluded(oe,lp);
                    if(qiOccluded==coarse) continue;            // agree: not a candidate
                    double depth=insideDepth(oe,lp);            // resolve only the grazing candidates
                    if(depth>CLEAR_OCCLUDER && !qiOccluded){ errors++; if(errors<=25) System.out.printf("MISS rot(%d,%d) e=%d t=%.2f depth=%.5f qi=%d%n",za,xa,ei,t,depth,qi); }
                    else if(depth<CLEAR_TANGENT && qiOccluded){ errors++; if(errors<=25) System.out.printf("PHANTOM rot(%d,%d) e=%d t=%.2f depth=%.6f qi=%d%n",za,xa,ei,t,depth,qi); }
                }
            }
        }
        System.out.println("Robust QI real errors (outside ambiguous band) TOTAL="+errors);
        assertThat(errors).isZero();
    }
}
