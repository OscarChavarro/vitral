package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SceneSolid;

/** Conservative, exact sufficient conditions for equality of toolkit solids. */
public class CsgSolidEquivalence {
private static boolean sameEllipsoidLinear(Matrix4x4d a, double radiusA,
    Matrix4x4d b, double radiusB)
{
    double[][] x=new double[3][3], y=new double[3][3];
    for (int i=0;i<3;++i) for (int j=0;j<3;++j) {
        x[i][j]=a.get(i,j)*radiusA; y[i][j]=b.get(i,j)*radiusB;
        if (!Double.isFinite(x[i][j]) || !Double.isFinite(y[i][j]) ||
            !CsgExactArithmetic.equalProducts(a.get(i,j),radiusA,x[i][j],1) ||
            !CsgExactArithmetic.equalProducts(b.get(i,j),radiusB,y[i][j],1))
            return CsgDyadicSolid.sameEllipsoids(radiusA,a,radiusB,b);
    }
    // Equal Gram matrices describe the same affine image of a sphere.
    for (int i=0;i<3;++i) for (int j=i;j<3;++j)
        if (!CsgExactArithmetic.equalSumProducts(x[i],x[j],y[i],y[j],3))
            return CsgDyadicSolid.sameEllipsoids(radiusA,a,radiusB,b);
    return true;
}

    private static boolean sameTranslation(Matrix4x4d a, Matrix4x4d b)
{ for (int i=0;i<3;++i) if (a.get(i,3)!=b.get(i,3)) return false; return true; }

    private static boolean signedPermutation(Matrix4x4d a, Matrix4x4d b,
    int[] axes, int[] signs)
{
    if (!sameTranslation(a,b)) return false;
    final boolean[] used ={false,false,false};
    for (int j=0;j<3;++j) {
        boolean found=false;
        for (int k=0;k<3 && !found;++k) if (!used[k]) {
            boolean same=true, reverse=true;
            for (int i=0;i<3;++i) {
                if (a.get(i,k)!=b.get(i,j)) same=false;
                if (a.get(i,k)!=-b.get(i,j)) reverse=false;
            }
            if (same || reverse) { axes[j]=k; signs[j]=same?1:-1; used[k]=true; found=true; }
        }
        if (!found) return false;
    }
    return true;
}

    private static boolean proportional(double[] a, double[] b, int count)
{
    int pivot=-1;
    for (int i=0;i<count;++i) {
        if (!Double.isFinite(a[i]) || !Double.isFinite(b[i])) return false;
        if (pivot<0 && (a[i]!=0 || b[i]!=0)) pivot=i;
    }
    if (pivot<0 || a[pivot]==0 || b[pivot]==0 || (a[pivot]<0)!=(b[pivot]<0)) return false;
    for (int i=0;i<count;++i)
        if (!CsgExactArithmetic.equalProducts(a[i],b[pivot],b[i],a[pivot])) return false;
    return true;
}

    private static boolean sameBox(Box a, Matrix4x4d pa, Box b, Matrix4x4d pb)
{
    final Vector3Dd[] ab=a.getRawBounds(); final Vector3Dd[] bb=b.getRawBounds();
    final double[] amin ={ab[0].x(),ab[0].y(),ab[0].z()}; final double[] amax ={ab[1].x(),ab[1].y(),ab[1].z()};
    final double[] bmin ={bb[0].x(),bb[0].y(),bb[0].z()}; final double[] bmax ={bb[1].x(),bb[1].y(),bb[1].z()};
    for (int i=0;i<3;++i)
        if (!Double.isFinite(amin[i]) || !Double.isFinite(amax[i]) ||
            !Double.isFinite(bmin[i]) || !Double.isFinite(bmax[i]) ||
            !(amin[i]<amax[i]) || !(bmin[i]<bmax[i])) return false;
    // Axis-aligned world boxes: compare exact dyadic affine endpoints, including
    // translations, scales, reflections and permutations without rounded sums.
    int[] ai=new int[3]; int[] bi=new int[3]; final boolean[] usedA ={false,false,false}; final boolean[] usedB ={false,false,false};
    boolean axisAligned=true;
    for (int i=0;i<3;++i) {
        ai[i]=-1; bi[i]=-1;
        for (int j=0;j<3;++j) {
            if (pa.get(i,j)!=0) { if (ai[i]>=0) axisAligned=false; ai[i]=j; }
            if (pb.get(i,j)!=0) { if (bi[i]>=0) axisAligned=false; bi[i]=j; }
        }
        if (ai[i]<0 || bi[i]<0) axisAligned=false;
        else { if (usedA[ai[i]] || usedB[bi[i]]) axisAligned=false; usedA[ai[i]]=true; usedB[bi[i]]=true; }
    }
    if (axisAligned) {
        for (int i=0;i<3;++i) {
            final double sa=pa.get(i,ai[i]), sb=pb.get(i,bi[i]);
            if (!CsgExactArithmetic.equalAffine(sa,sa>0?amin[ai[i]]:amax[ai[i]],pa.get(i,3),
                sb,sb>0?bmin[bi[i]]:bmax[bi[i]],pb.get(i,3)) ||
                !CsgExactArithmetic.equalAffine(sa,sa>0?amax[ai[i]]:amin[ai[i]],pa.get(i,3),
                sb,sb>0?bmax[bi[i]]:bmin[bi[i]],pb.get(i,3))) return false;
        }
        return true;
    }
    // For general affine boxes, match parallel edge directions and compare
    // their exact edge vectors and a corresponding world-space corner.
    final boolean[] used ={false,false,false};
    final boolean[] reversed ={false,false,false};
    for (int j=0;j<3;++j) {
        boolean found=false;
        for (int k=0;k<3 && !found;++k) if (!used[k]) {
            int pivot=-1;
            for (int i=0;i<3;++i) if (pa.get(i,k)!=0) { pivot=i; break; }
            if (pivot<0 || pb.get(pivot,j)==0) continue;
            boolean same=true;
            for (int i=0;i<3;++i)
                if (!CsgExactArithmetic.equalProducts(pa.get(i,k),pb.get(pivot,j),pb.get(i,j),pa.get(pivot,k))) same=false;
            final boolean reverse=(pa.get(pivot,k)<0)!=(pb.get(pivot,j)<0);
            for (int i=0;i<3 && same;++i) {
                final double[] ac ={pa.get(i,k),-pa.get(i,k)};
                final double[] av ={amax[k],amin[k]};
                final double[] bc ={pb.get(i,j),-pb.get(i,j)};
                final double[] bv ={reverse?bmin[j]:bmax[j],reverse?bmax[j]:bmin[j]};
                if (!CsgExactArithmetic.equalSumProducts(ac,av,bc,bv,2)) same=false;
            }
            if (same) { used[k]=true; reversed[j]=reverse; found=true; }
        }
        if (!found) return false;
    }
    for (int i=0;i<3;++i) {
        final double[] ac ={pa.get(i,0),pa.get(i,1),pa.get(i,2),pa.get(i,3)};
        final double[] av ={amin[0],amin[1],amin[2],1};
        final double[] bc ={pb.get(i,0),pb.get(i,1),pb.get(i,2),pb.get(i,3)};
        final double[] bv ={reversed[0]?bmax[0]:bmin[0],reversed[1]?bmax[1]:bmin[1],
            reversed[2]?bmax[2]:bmin[2],1};
        if (!CsgExactArithmetic.equalSumProducts(ac,av,bc,bv,4)) return false;
    }
    return true;
}

    private static boolean axisPlane(double[] coefficients, Matrix4x4d placement,
    int[] axis, double[] scale, double[] offset, double[] root, boolean[] positive)
{
    int local=-1;
    for (int i=0;i<4;++i) if (!Double.isFinite(coefficients[i])) return false;
    for (int i=0;i<3;++i) if (coefficients[i]!=0) { if (local>=0) return false; local=i; }
    if (local<0) return false;
    axis[0]=-1;
    for (int row=0;row<3;++row) {
        if (placement.get(row,local)==0) continue;
        boolean single=true;
        for (int column=0;column<3;++column)
            if (column!=local && placement.get(row,column)!=0) single=false;
        if (single) { if (axis[0]>=0) return false; axis[0]=row; }
    }
    if (axis[0]<0) return false;
    final double candidate=-coefficients[3]/coefficients[local];
    if (!Double.isFinite(candidate) ||
        !CsgExactArithmetic.equalProducts(candidate,coefficients[local],-coefficients[3],1)) return false;
    scale[0]=placement.get(axis[0],local); offset[0]=placement.get(axis[0],3); root[0]=candidate;
    positive[0]=(coefficients[local]<0)==(scale[0]<0);
    return true;
}

    private static boolean sameOrientedPlane(double[] a, Matrix4x4d pa,
    double[] b, Matrix4x4d pb)
{
    int[] axisA={-1},axisB={-1}; double[] scaleA={0},scaleB={0},offsetA={0},offsetB={0},rootA={0},rootB={0};
    boolean[] positiveA={false},positiveB={false};
    if (axisPlane(a,pa,axisA,scaleA,offsetA,rootA,positiveA) &&
        axisPlane(b,pb,axisB,scaleB,offsetB,rootB,positiveB))
        return axisA[0]==axisB[0] && positiveA[0]==positiveB[0] &&
            CsgExactArithmetic.equalAffine(scaleA[0],rootA[0],offsetA[0],scaleB[0],rootB[0],offsetB[0]);
    double[] worldA=new double[4]; double[] worldB=new double[4];
    if (CsgExactPlane.world(a,pa,worldA) && CsgExactPlane.world(b,pb,worldB))
        return proportional(worldA,worldB,4);
    int[] axes=new int[3]; int[] signs=new int[3];
    if (!signedPermutation(pa,pb,axes,signs)) return CsgDyadicSolid.samePlanes(a,pa,b,pb);
    final double[] transformed ={signs[0]*a[axes[0]],signs[1]*a[axes[1]],signs[2]*a[axes[2]],a[3]};
    return proportional(transformed,b,4);
}

    private static boolean sameHalfSpace(PlanarHalfSpace a, Matrix4x4d pa,
    PlanarHalfSpace b, Matrix4x4d pb)
{
    final InfinitePlane x=a.getBoundaryPlane(); final InfinitePlane y=b.getBoundaryPlane();
    final Vector3Dd nx=x.getCoefficients(), ny=y.getCoefficients();
    final double[] av ={nx.x(),nx.y(),nx.z(),x.getConstantCoefficient()};
    final double[] bv ={ny.x(),ny.y(),ny.z(),y.getConstantCoefficient()};
    if ((av[0]==0 && av[1]==0 && av[2]==0) || (bv[0]==0 && bv[1]==0 && bv[2]==0)) return false;
    return sameOrientedPlane(av,pa,bv,pb);
}

    private static boolean sphereQuadric(Sphere sphere, Matrix4x4d ps,
    QuadricSurface quadric, Matrix4x4d pq)
{
    final double sphereRadius=sphere.getRadius();
    if (!Double.isFinite(sphereRadius) || !(sphereRadius>0)) return false;
    final double squared=sphereRadius*sphereRadius;
    if (Double.isFinite(squared) && CsgExactArithmetic.equalProducts(sphereRadius,sphereRadius,squared,1)) {
        final double[] local ={1,1,1,0,0,0,0,0,0,-squared};
        double[] worldA=new double[10]; double[] worldB=new double[10];
        if (CsgExactQuadric.world(local,ps,worldA) && worldQuadric(quadric,pq,worldB) &&
            proportional(worldA,worldB,10)) return true;
    }
    final Vector3Dd sq=quadric.squareCoefficients(), mixed=quadric.mixedCoefficients(), lin=quadric.linearCoefficients();
    final double[] coefficients ={sq.x(),sq.y(),sq.z(),mixed.x(),mixed.y(),mixed.z(),
        lin.x(),lin.y(),lin.z(),quadric.constantCoefficient()};
    if (CsgDyadicSolid.sphereQuadric(sphereRadius,ps,coefficients,pq)) return true;
    int[] axes=new int[3]; int[] signs=new int[3];
    if (!sameTranslation(ps,pq) ||
        (!signedPermutation(ps,pq,axes,signs) && !sameEllipsoidLinear(ps,1,pq,1))) return false;
    final double q=sq.x(), radius=sphere.getRadius(), constant=quadric.constantCoefficient();
    if (!Double.isFinite(q) || !Double.isFinite(radius) || !Double.isFinite(constant) ||
        !(q>0) || !(radius>0) || sq.y()!=q || sq.z()!=q || mixed.x()!=0 || mixed.y()!=0 || mixed.z()!=0 ||
        lin.x()!=0 || lin.y()!=0 || lin.z()!=0) return false;
    // The intermediate must itself be exact; otherwise conservatively decline.
    final double intermediate=q*radius;
    if (!Double.isFinite(intermediate) || !CsgExactArithmetic.equalProducts(q,radius,intermediate,1)) return false;
    return CsgExactArithmetic.equalProducts(intermediate,radius,-constant,1);
}

    private static boolean halfSpaceQuadric(PlanarHalfSpace plane, Matrix4x4d pp,
    QuadricSurface quadric, Matrix4x4d pq)
{
    final Vector3Dd sq=quadric.squareCoefficients(), mixed=quadric.mixedCoefficients(), lin=quadric.linearCoefficients();
    if (sq.x()!=0 || sq.y()!=0 || sq.z()!=0 || mixed.x()!=0 || mixed.y()!=0 || mixed.z()!=0) return false;
    final InfinitePlane boundary=plane.getBoundaryPlane();
    final Vector3Dd n=boundary.getCoefficients();
    if ((n.x()==0 && n.y()==0 && n.z()==0) || (lin.x()==0 && lin.y()==0 && lin.z()==0)) return false;
    final double[] a ={n.x(),n.y(),n.z(),boundary.getConstantCoefficient()};
    final double[] b ={lin.x(),lin.y(),lin.z(),quadric.constantCoefficient()};
    return sameOrientedPlane(a,pp,b,pq);
}

    private static boolean worldQuadric(QuadricSurface quadric, Matrix4x4d placement, double[] result)
{
    final Vector3Dd sq=quadric.squareCoefficients(), mixed=quadric.mixedCoefficients(), lin=quadric.linearCoefficients();
    final double[] local ={sq.x(),sq.y(),sq.z(),mixed.x(),mixed.y(),mixed.z(),lin.x(),lin.y(),lin.z(),quadric.constantCoefficient()};
    return CsgExactQuadric.world(local,placement,result);
}

    private static boolean sameQuadric(QuadricSurface a, Matrix4x4d pa,
    QuadricSurface b, Matrix4x4d pb)
{
    double[] worldA=new double[10]; double[] worldB=new double[10];
    if (worldQuadric(a,pa,worldA) && worldQuadric(b,pb,worldB)) return proportional(worldA,worldB,10);
    final Vector3Dd as=a.squareCoefficients(), am=a.mixedCoefficients(), al=a.linearCoefficients();
    final Vector3Dd bs=b.squareCoefficients(), bm=b.mixedCoefficients(), bl=b.linearCoefficients();
    final double[] localA ={as.x(),as.y(),as.z(),am.x(),am.y(),am.z(),al.x(),al.y(),al.z(),a.constantCoefficient()};
    final double[] localB ={bs.x(),bs.y(),bs.z(),bm.x(),bm.y(),bm.z(),bl.x(),bl.y(),bl.z(),b.constantCoefficient()};
    if (CsgDyadicSolid.sameQuadrics(localA,pa,localB,pb)) return true;
    int[] axes=new int[3]; int[] signs=new int[3]; if (!signedPermutation(pa,pb,axes,signs)) return false;
    final double[] sq ={as.x(),as.y(),as.z()}; final double[] lin ={al.x(),al.y(),al.z()};
    final double[][] mixed={{0,am.x(),am.y()},{am.x(),0,am.z()},{am.y(),am.z(),0}};
    final double[] av ={sq[axes[0]],sq[axes[1]],sq[axes[2]],
        signs[0]*signs[1]*mixed[axes[0]][axes[1]],signs[0]*signs[2]*mixed[axes[0]][axes[2]],
        signs[1]*signs[2]*mixed[axes[1]][axes[2]],signs[0]*lin[axes[0]],signs[1]*lin[axes[1]],
        signs[2]*lin[axes[2]],a.constantCoefficient()};
    final double[] bv ={bs.x(),bs.y(),bs.z(),bm.x(),bm.y(),bm.z(),bl.x(),bl.y(),bl.z(),b.constantCoefficient()};
    return proportional(av,bv,10);
}

    public static boolean same(SceneSolid a, SceneSolid b)
{
    if (a==b) return true;
    // Each scene specialization explicitly exposes its unmodified core solid.
    final Geometry ga=a.getSolidEquivalenceGeometry();
    final Geometry gb=b.getSolidEquivalenceGeometry();
    if (ga == null || gb == null) return false;
    Matrix4x4d[] pa={new Matrix4x4d()},ia={new Matrix4x4d()},pb={new Matrix4x4d()},ib={new Matrix4x4d()};
    if (a.queryAffinePlacement(pa,ia)!=AffinePlacementStatus.Valid ||
        b.queryAffinePlacement(pb,ib)!=AffinePlacementStatus.Valid) return false;
    final Sphere sa=ga instanceof Sphere g ? g : null; final Sphere sb=gb instanceof Sphere g ? g : null;
    if (sa != null && sb != null && sa.getClass()==Sphere.class && sb.getClass()==Sphere.class) {
        final double ra=sa.getRadius(), rb=sb.getRadius();
        if (!Double.isFinite(ra) || !Double.isFinite(rb) || ra<=0 || rb<=0 || !sameTranslation(pa[0],pb[0])) return false;
        final boolean[] used ={false,false,false};
        for (int j=0;j<3;++j) {
            boolean found=false;
            for (int k=0;k<3 && !found;++k) if (!used[k]) {
                boolean same=true, reverse=true;
                for (int i=0;i<3;++i) {
                    if (!CsgExactArithmetic.equalProducts(pa[0].get(i,k),ra,pb[0].get(i,j),rb)) same=false;
                    if (!CsgExactArithmetic.equalProducts(pa[0].get(i,k),ra,-pb[0].get(i,j),rb)) reverse=false;
                }
                if (same || reverse) { used[k]=true; found=true; }
            }
            if (!found) return sameEllipsoidLinear(pa[0],ra,pb[0],rb);
        }
        return true;
    }
    final Box ba=ga instanceof Box g ? g : null; final Box bb=gb instanceof Box g ? g : null;
    if (ba != null && bb != null && ba.getClass()==Box.class && bb.getClass()==Box.class) return sameBox(ba,pa[0],bb,pb[0]);
    final PlanarHalfSpace ha=ga instanceof PlanarHalfSpace g ? g : null; final PlanarHalfSpace hb=gb instanceof PlanarHalfSpace g ? g : null;
    if (ha != null && hb != null && ha.getClass()==PlanarHalfSpace.class && hb.getClass()==PlanarHalfSpace.class) return sameHalfSpace(ha,pa[0],hb,pb[0]);
    final QuadricSurface qa=ga instanceof QuadricSurface g ? g : null; final QuadricSurface qb=gb instanceof QuadricSurface g ? g : null;
    if (qa != null && qb != null && qa.getClass()==QuadricSurface.class && qb.getClass()==QuadricSurface.class) return sameQuadric(qa,pa[0],qb,pb[0]);
    if (sa != null && qb != null && sa.getClass()==Sphere.class && qb.getClass()==QuadricSurface.class) return sphereQuadric(sa,pa[0],qb,pb[0]);
    if (sb != null && qa != null && sb.getClass()==Sphere.class && qa.getClass()==QuadricSurface.class) return sphereQuadric(sb,pb[0],qa,pa[0]);
    if (ha != null && qb != null && ha.getClass()==PlanarHalfSpace.class && qb.getClass()==QuadricSurface.class) return halfSpaceQuadric(ha,pa[0],qb,pb[0]);
    if (hb != null && qa != null && hb.getClass()==PlanarHalfSpace.class && qa.getClass()==QuadricSurface.class) return halfSpaceQuadric(hb,pb[0],qa,pa[0]);
    return false;
}
}
