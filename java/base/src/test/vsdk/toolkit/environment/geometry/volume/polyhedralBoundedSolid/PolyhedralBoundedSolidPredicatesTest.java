package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;
import org.junit.jupiter.api.Test;
import vsdk.toolkit.common.linealAlgebra.*;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;
import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidPredicatesTest {
    static final double[][] BOX = {
        {0.5,0.1,0.1, 0.5,0.1,0.1},{0.5,0.9,0.1, 0.5,0.1,0.1},
        {0.1,0.5,0.1, 0.1,0.5,0.1},{0.9,0.5,0.1, 0.1,0.5,0.1},
        {0.1,0.5,0.1, 0.1,0.5,0.1},{0.1,0.5,0.9, 0.1,0.5,0.1},
        {0.1,0.1,0.5, 0.1,0.1,0.5},{0.1,0.9,0.5, 0.1,0.1,0.5},
        {0.3,0.5,0.5, 0.3,0.1,0.1},{0.5,0.5,0.5, 0.1,0.1,0.5},
        {0.7,0.5,0.9, 0.3,0.1,0.1},{0.9,0.5,0.9, 0.1,0.5,0.1}
    };
    static boolean boxInside(Vector3Dd p){
        for(double[] b:BOX) if(Math.abs(p.x()-b[0])<=b[3]-1e-7&&Math.abs(p.y()-b[1])<=b[4]-1e-7&&Math.abs(p.z()-b[2])<=b[5]-1e-7) return true;
        return false; }
    static boolean march(Matrix4x4d mInv, Vector3Dd eye, Vector3Dd t){
        Vector3Dd d=t.subtract(eye); for(int i=1;i<3000;i++){double s=(double)i/3000; if(s>=1-3e-4)break;
            if(boxInside(mInv.multiply(eye.add(d.multiply(s))))) return true;} return false; }

    @Test void isPointInside_matches12BoxGroundTruth(){
        PolyhedralBoundedSolid solid=SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        int bad=0, tested=0;
        for(int ix=0;ix<=20;ix++)for(int iy=0;iy<=20;iy++)for(int iz=0;iz<=20;iz++){
            double x=ix/20.0,y=iy/20.0,z=iz/20.0;
            Vector3Dd p=new Vector3Dd(x,y,z);
            // skip points near any box boundary (genuinely ambiguous surface)
            boolean nearBoundary=false;
            for(double[] b:BOX){ for(int a=0;a<3;a++){ double c=(a==0?p.x():a==1?p.y():p.z()); double cc=b[a],h=b[3+a];
                if(Math.abs(Math.abs(c-cc)-h)<0.01) nearBoundary=true; } }
            if(nearBoundary) continue;
            tested++;
            boolean gt=boxInside(p);
            boolean got=PolyhedralBoundedSolidPredicates.isPointInside(solid,p);
            if(gt!=got){ bad++; if(bad<=10) System.out.printf("  inside mismatch p=(%.2f,%.2f,%.2f) gt=%b got=%b%n",x,y,z,gt,got); }
        }
        System.out.println("isPointInside tested="+tested+" bad="+bad);
        assertThat(bad).isZero();
    }


}
