import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public class ReentrantGeometryIntersectionSmoke {
    private static final double EPS = 1e-6;

    public static void main(String[] args) throws Exception
    {
        testTriangleMesh();
        testTriangleMeshGroup();
        testTorus();
        testPolyhedralBoundedSolid();

        stressTriangleMeshReentrancy();
        stressTorusReentrancy();
        stressPolyhedralReentrancy();

        System.out.println("ALL_REENTRANT_GEOMETRY_TESTS_PASSED");
    }

    private static TriangleMesh buildTwoTriangleMesh()
    {
        TriangleMesh mesh = new TriangleMesh();
        Vertex[] vertices = new Vertex[] {
            new Vertex(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1), 0, 0),
            new Vertex(new Vector3D(1, 0, 0), new Vector3D(0, 0, 1), 1, 0),
            new Vertex(new Vector3D(0, 1, 0), new Vector3D(0, 0, 1), 0, 1),
            new Vertex(new Vector3D(1, 1, 0), new Vector3D(0, 0, 1), 1, 1)
        };
        Triangle[] triangles = new Triangle[] {
            new Triangle(0, 1, 2),
            new Triangle(1, 3, 2)
        };
        mesh.setVertexes(vertices, true, false, false, true);
        mesh.setTriangles(triangles);
        return mesh;
    }

    private static void testTriangleMesh()
    {
        TriangleMesh mesh = buildTwoTriangleMesh();
        Ray ray = new Ray(new Vector3D(0.2, 0.2, -1), new Vector3D(0, 0, 1));
        RayHit hit = new RayHit();

        assertTrue(mesh.doIntersection(ray, hit), "TriangleMesh must intersect");
        assertNear(hit.ray().t(), 1.0, 1e-5, "TriangleMesh t");

        RayHit extra = new RayHit();
        mesh.doExtraInformation(hit.ray(), hit.ray().t(), extra);
        assertNear(extra.p.z(), 0.0, 1e-5, "TriangleMesh hit z");
        assertNear(extra.u, 0.2, 1e-2, "TriangleMesh u");
        assertNear(extra.v, 0.2, 1e-2, "TriangleMesh v");
    }

    private static void testTriangleMeshGroup()
    {
        TriangleMesh mesh = buildTwoTriangleMesh();
        ArrayList<TriangleMesh> meshes = new ArrayList<TriangleMesh>();
        meshes.add(mesh);
        TriangleMeshGroup group = new TriangleMeshGroup(meshes);

        Ray ray = new Ray(new Vector3D(0.8, 0.8, -1), new Vector3D(0, 0, 1));
        RayHit hit = new RayHit();
        assertTrue(group.doIntersection(ray, hit), "TriangleMeshGroup must intersect");

        int[] info = group.doIntersectionInformation();
        assertTrue(info[0] == 0, "TriangleMeshGroup mesh index must be 0");
        assertTrue(info[1] == 1, "TriangleMeshGroup triangle index must be 1");
    }

    private static void testTorus()
    {
        Torus torus = new Torus(2.0, 0.5);
        Ray ray = new Ray(new Vector3D(2.3, 0, -2), new Vector3D(0, 0, 1));
        RayHit hit = new RayHit();

        assertTrue(torus.doIntersection(ray, hit), "Torus must intersect");
        RayHit extra = new RayHit();
        torus.doExtraInformation(hit.ray(), hit.ray().t(), extra);
        assertNear(extra.p.x(), 2.3, 1e-3, "Torus hit x");
        assertNear(extra.p.z(), -0.4, 3e-2, "Torus hit z");
        assertTrue(Math.abs(extra.n.length() - 1.0) < 1e-4, "Torus normal must be unit");
    }

    private static void testPolyhedralBoundedSolid()
    {
        PolyhedralBoundedSolid solid =
            new Box(2, 2, 2).exportToPolyhedralBoundedSolid();
        Ray ray = new Ray(new Vector3D(0, 0, -5), new Vector3D(0, 0, 1));
        RayHit hit = new RayHit();

        assertTrue(solid.doIntersection(ray, hit), "PolyhedralBoundedSolid must intersect");
        RayHit extra = new RayHit();
        solid.doExtraInformation(hit.ray(), hit.ray().t(), extra);
        assertTrue(extra.p.z() < 0, "PolyhedralBoundedSolid first hit must be lower face");
        assertNear(Math.abs(extra.p.z()), 1.0, 1e-2, "PolyhedralBoundedSolid |z|");
    }

    private static void stressTriangleMeshReentrancy() throws Exception
    {
        final TriangleMesh mesh = buildTwoTriangleMesh();
        Runnable a = () -> {
            for ( int i = 0; i < 1500; i++ ) {
                RayHit hit = new RayHit();
                Ray ray = new Ray(new Vector3D(0.2, 0.2, -1), new Vector3D(0, 0, 1));
                if ( !mesh.doIntersection(ray, hit) ) {
                    throw new IllegalStateException("TriangleMesh thread A miss");
                }
                RayHit extra = new RayHit();
                mesh.doExtraInformation(hit.ray(), hit.ray().t(), extra);
                if ( Math.abs(extra.u - 0.2) > 2e-2 || Math.abs(extra.v - 0.2) > 2e-2 ) {
                    throw new IllegalStateException(
                        "TriangleMesh thread A UV corruption u=" + extra.u +
                        " v=" + extra.v);
                }
            }
        };
        Runnable b = () -> {
            for ( int i = 0; i < 1500; i++ ) {
                RayHit hit = new RayHit();
                Ray ray = new Ray(new Vector3D(0.8, 0.8, -1), new Vector3D(0, 0, 1));
                if ( !mesh.doIntersection(ray, hit) ) {
                    throw new IllegalStateException("TriangleMesh thread B miss");
                }
                RayHit extra = new RayHit();
                mesh.doExtraInformation(hit.ray(), hit.ray().t(), extra);
                if ( Math.abs(extra.u - 0.8) > 2e-2 || Math.abs(extra.v - 0.8) > 2e-2 ) {
                    throw new IllegalStateException(
                        "TriangleMesh thread B UV corruption u=" + extra.u +
                        " v=" + extra.v);
                }
            }
        };
        runParallel(a, b);
    }

    private static void stressTorusReentrancy() throws Exception
    {
        final Torus torus = new Torus(2.0, 0.5);
        Runnable a = () -> {
            for ( int i = 0; i < 2000; i++ ) {
                Ray ray = new Ray(new Vector3D(2.3, 0, -2), new Vector3D(0, 0, 1));
                RayHit hit = new RayHit();
                if ( !torus.doIntersection(ray, hit) ) {
                    throw new IllegalStateException("Torus thread A miss");
                }
                RayHit extra = new RayHit();
                torus.doExtraInformation(hit.ray(), hit.ray().t(), extra);
                if ( extra.p.z() > 0 ) {
                    throw new IllegalStateException("Torus thread A point corruption");
                }
            }
        };
        Runnable b = () -> {
            for ( int i = 0; i < 2000; i++ ) {
                Ray ray = new Ray(new Vector3D(2.3, 0, 2), new Vector3D(0, 0, -1));
                RayHit hit = new RayHit();
                if ( !torus.doIntersection(ray, hit) ) {
                    throw new IllegalStateException("Torus thread B miss");
                }
                RayHit extra = new RayHit();
                torus.doExtraInformation(hit.ray(), hit.ray().t(), extra);
                if ( extra.p.z() < 0 ) {
                    throw new IllegalStateException("Torus thread B point corruption");
                }
            }
        };
        runParallel(a, b);
    }

    private static void stressPolyhedralReentrancy() throws Exception
    {
        final PolyhedralBoundedSolid solid =
            new Box(2, 2, 2).exportToPolyhedralBoundedSolid();
        Runnable a = () -> {
            for ( int i = 0; i < 1500; i++ ) {
                Ray ray = new Ray(new Vector3D(0, 0, -5), new Vector3D(0, 0, 1));
                RayHit hit = new RayHit();
                if ( !solid.doIntersection(ray, hit) ) {
                    throw new IllegalStateException("Polyhedral thread A miss");
                }
                RayHit extra = new RayHit();
                solid.doExtraInformation(hit.ray(), hit.ray().t(), extra);
                if ( extra.p.z() >= 0 ) {
                    throw new IllegalStateException("Polyhedral thread A point corruption");
                }
            }
        };
        Runnable b = () -> {
            for ( int i = 0; i < 1500; i++ ) {
                Ray ray = new Ray(new Vector3D(0, 0, 5), new Vector3D(0, 0, -1));
                RayHit hit = new RayHit();
                if ( !solid.doIntersection(ray, hit) ) {
                    throw new IllegalStateException("Polyhedral thread B miss");
                }
                RayHit extra = new RayHit();
                solid.doExtraInformation(hit.ray(), hit.ray().t(), extra);
                if ( extra.p.z() <= 0 ) {
                    throw new IllegalStateException("Polyhedral thread B point corruption");
                }
            }
        };
        runParallel(a, b);
    }

    private static void runParallel(Runnable a, Runnable b) throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> fa = executor.submit(a);
            Future<?> fb = executor.submit(b);
            fa.get();
            fb.get();
        }
        finally {
            executor.shutdownNow();
        }
    }

    private static void assertTrue(boolean condition, String message)
    {
        if ( !condition ) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertNear(double value, double expected, double tolerance, String label)
    {
        if ( Math.abs(value - expected) > tolerance ) {
            throw new IllegalStateException(
                label + " expected " + expected + " but was " + value);
        }
    }
}
