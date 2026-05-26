//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package vsdk.toolkit.render;

// Java classes
import java.util.ArrayList;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.Surface;
import vsdk.toolkit.environment.geometry.volume.Solid;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Volume;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.Calligraphic2DBuffer;

public class WireframeRenderer extends RenderingElement
{
    private static int calculateCanonicalOutcode(Vector3Dd p, double fpd)
    {
        int bits = 0x0;

        if ( p.z() + p.x() - 1 > 0 ) bits |= Camera.OPCODE_RIGHT;
        if ( p.z() - p.x() - 1 > 0 ) bits |= Camera.OPCODE_LEFT;
        if ( p.z() + p.y() - 1 > 0 ) bits |= Camera.OPCODE_UP;
        if ( p.z() - p.y() - 1 > 0 ) bits |= Camera.OPCODE_DOWN;
        if ( p.z() > 0 ) bits |= Camera.OPCODE_NEAR;
        if ( p.z() < -fpd ) bits |= Camera.OPCODE_FAR;

        return bits;
    }

    private static Vector3Dd[] clipLineCanonicVolume(Vector3Dd point0, Vector3Dd point1, Camera c)
    {
        Matrix4x4d nt = c.getNormalizingTransformation();
        Vector3Dd clippedPoint0 = nt.multiply(point0);
        Vector3Dd clippedPoint1 = nt.multiply(point1);
        double fpd = (c.getFarPlaneDistance() - c.getNearPlaneDistance()) / c.getNearPlaneDistance();

        int outcode0 = calculateCanonicalOutcode(clippedPoint0, fpd);
        int outcode1 = calculateCanonicalOutcode(clippedPoint1, fpd);

        while ( true ) {
            if ( outcode0 == 0x0 && outcode1 == 0x0 ) {
                return new Vector3Dd[] { clippedPoint0, clippedPoint1 };
            }
            if ( (outcode0 & outcode1) != 0x0 ) {
                return null;
            }

            int outcodeout = (outcode0 != 0) ? outcode0 : outcode1;
            Vector3Dd dir = clippedPoint1.subtract(clippedPoint0);
            double l = dir.length();
            if ( l < VSDK.EPSILON ) {
                return null;
            }
            dir = dir.multiply(1.0 / l);

            double de = (outcodeout == outcode1) ? -VSDK.EPSILON : VSDK.EPSILON;
            Vector3Dd m = null;

            if ( (Camera.OPCODE_UP & outcodeout) != 0x0 ) {
                double t = (l * (1 - clippedPoint0.z() - clippedPoint0.y())) /
                    (clippedPoint1.z() - clippedPoint0.z() + clippedPoint1.y() - clippedPoint0.y());
                m = clippedPoint0.add(dir.multiply(t + de));
            }
            else if ( (Camera.OPCODE_DOWN & outcodeout) != 0x0 ) {
                double t = (l * (clippedPoint0.y() - clippedPoint0.z() + 1)) /
                    (clippedPoint1.z() - clippedPoint0.z() - clippedPoint1.y() + clippedPoint0.y());
                m = clippedPoint0.add(dir.multiply(t + de));
            }
            else if ( (Camera.OPCODE_LEFT & outcodeout) != 0x0 ) {
                double t = (l * (clippedPoint0.x() - clippedPoint0.z() + 1)) /
                    (clippedPoint1.z() - clippedPoint0.z() - clippedPoint1.x() + clippedPoint0.x());
                m = clippedPoint0.add(dir.multiply(t + de));
            }
            else if ( (Camera.OPCODE_RIGHT & outcodeout) != 0x0 ) {
                double t = (l * (1 - clippedPoint0.z() - clippedPoint0.x())) /
                    (clippedPoint1.z() - clippedPoint0.z() + clippedPoint1.x() - clippedPoint0.x());
                m = clippedPoint0.add(dir.multiply(t + de));
            }
            else if ( (Camera.OPCODE_NEAR & outcodeout) != 0x0 ) {
                double t = (-clippedPoint0.z() * l) / (clippedPoint1.z() - clippedPoint0.z());
                m = clippedPoint0.add(dir.multiply(t + de));
            }
            else if ( (Camera.OPCODE_FAR & outcodeout) != 0x0 ) {
                double t = ((-fpd - clippedPoint0.z()) * l) / (clippedPoint1.z() - clippedPoint0.z());
                m = clippedPoint0.add(dir.multiply(t + de));
            }

            if ( m == null ) {
                return null;
            }

            if ( outcodeout == outcode0 ) {
                clippedPoint0 = m;
                outcode0 = calculateCanonicalOutcode(clippedPoint0, fpd);
            }
            else {
                clippedPoint1 = m;
                outcode1 = calculateCanonicalOutcode(clippedPoint1, fpd);
            }
        }
    }

    /**
    Given a 3D line (with endpoints `cp0` and `cp1`), previously clipped
    against the current view volume, this method projects the line in to the
    projection plane by applying a projection transformation specified by 
    `Proj`, and adds the resulting 2D line to the Calligraphic2DBuffer
    `lineSet`.
    */
    private static void addLine(Calligraphic2DBuffer lineSet,
                                Vector3Dd cp0, Vector3Dd cp1, Matrix4x4d Proj,
                                Camera c) {
        //-----------------------------------------------------------------
        Vector4Dd hp0, hp1; // Clipped points in homogeneous space
        Vector4Dd pp0, pp1; // Projected points

        double f;
        f = 1;// f = (c.getFarPlaneDistance() - c.getNearPlaneDistance())/20;

        hp0 = new Vector4Dd(cp0);
        hp1 = new Vector4Dd(cp1);
        pp0 = Proj.multiply(hp0).dividedByW();
        pp1 = Proj.multiply(hp1).dividedByW();
        lineSet.add2DLine(pp0.x()*f, pp0.y()*f, pp1.x()*f, pp1.y()*f);
    }

    private static void processBrep(SimpleBody body, Matrix4x4d P,
                        Calligraphic2DBuffer outLineSet,
                        Camera inCamera)
    {
        //-----------------------------------------------------------------
        PolyhedralBoundedSolid brep;

        if ( !(body.getGeometry() instanceof Volume) ) {
            return;
        }
        brep = ((Volume)body.getGeometry()).exportToPolyhedralBoundedSolid();
        if ( brep == null ) return;

        //-----------------------------------------------------------------
        int i;
        Vector3Dd mp0, mp1;         // Edge points
        Vector3Dd[] clippedSegment;  // Clipped points
        Matrix4x4d M;

        M = body.getTransformationMatrix();
        for ( i = 0; i < brep.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = brep.getEdgesList().get(i);
            int start, end;
            start = e.getStartingVertexId();
            end = e.getEndingVertexId();
            if ( start >= 0 && end >= 0 ) {
                mp0 = e.rightHalf.startingVertex.position;
                mp1 = e.leftHalf.startingVertex.position;
                if ( mp0 != null && mp1 != null ) {
                    mp0 = M.multiply(mp0);
                    mp1 = M.multiply(mp1);
                    clippedSegment = clipLineCanonicVolume(mp0, mp1, inCamera);
                    if ( clippedSegment != null ) {
                        addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                    }

                }
            }
        }
    }

    private static void processMesh(SimpleBody body, Matrix4x4d P,
                        Calligraphic2DBuffer outLineSet,
                        Camera inCamera)
    {
        int j;                     // subobject index
        int t;                     // triangle index
        TriangleMeshGroup mg;
        TriangleMesh mesh;
        int nv;
        int nt;
        double v[];
        int tr[];
        Matrix4x4d M;               // Modelview matrix
        int p0, p1, p2;
        Vector3Dd mp0, mp1;         // Mesh points
        Vector3Dd[] clippedSegment;  // Clipped points

        mg = body.getGeometry().exportToTriangleMeshGroup();
        if ( mg == null ) return;

        mp0 = new Vector3Dd();
        mp1 = new Vector3Dd();
        M = body.getTransformationMatrix();
        for ( j = 0; j < mg.getMeshes().size(); j++ ) {
            mesh = mg.getMeshes().get(j);
            nv = mesh.getNumVertices();
            nt = mesh.getNumTriangles();
            v = mesh.getVertexPositions();
            tr = mesh.getTriangleIndexes();
            for ( t = 0; t < nt; t++ ) {
                p0 = tr[3*t];
                p1 = tr[3*t+1];
                p2 = tr[3*t+2];

                mp0 = new Vector3Dd(v[3*p0], v[3*p0+1], v[3*p0+2]);
                mp1 = new Vector3Dd(v[3*p1], v[3*p1+1], v[3*p1+2]);
                mp0 = M.multiply(mp0);
                mp1 = M.multiply(mp1);
                clippedSegment = clipLineCanonicVolume(mp0, mp1, inCamera);
                if ( clippedSegment != null ) {
                    addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                }

                mp0 = new Vector3Dd(v[3*p1], v[3*p1+1], v[3*p1+2]);
                mp1 = new Vector3Dd(v[3*p2], v[3*p2+1], v[3*p2+2]);
                mp0 = M.multiply(mp0);
                mp1 = M.multiply(mp1);
                clippedSegment = clipLineCanonicVolume(mp0, mp1, inCamera);
                if ( clippedSegment != null ) {
                    addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                }

                mp0 = new Vector3Dd(v[3*p2], v[3*p2+1], v[3*p2+2]);
                mp1 = new Vector3Dd(v[3*p0], v[3*p0+1], v[3*p0+2]);
                mp0 = M.multiply(mp0);
                mp1 = M.multiply(mp1);
                clippedSegment = clipLineCanonicVolume(mp0, mp1, inCamera);
                if ( clippedSegment != null ) {
                    addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                }
            }
        }
    }

    public static void execute(Calligraphic2DBuffer outLineSet,
                        ArrayList <SimpleBody> inSimpleBodyArray,
                        Camera inCamera)
    {
        //- Calligraphic rendering of lines in to 2D line buffer ----------
        int i;                     // Index inside objects list
        Matrix4x4d P;               // Projection matrix
        Geometry g;

        P = new Matrix4x4d();
        P = P.canonicalPerspectiveProjection();

        for ( i = 0; i < inSimpleBodyArray.size(); i++ ) {
            g = inSimpleBodyArray.get(i).getGeometry();
            if ( g instanceof Surface ) {
                processMesh(inSimpleBodyArray.get(i), P, outLineSet, inCamera);
            }
            else if ( g instanceof Solid ) {
                processBrep(inSimpleBodyArray.get(i), P, outLineSet, inCamera);
            }
        }
    }
}
