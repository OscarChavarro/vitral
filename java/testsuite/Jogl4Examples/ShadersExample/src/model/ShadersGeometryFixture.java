package model;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.surface.BezierPatchSubdivision;
import vsdk.toolkit.environment.geometry.surface.SubdividedBezierPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.BlobComponent;
import vsdk.toolkit.environment.geometry.volume.BlobSolid;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.HeightFieldSolid;
import vsdk.toolkit.environment.geometry.volume.PolynomialSurface;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.csg.CsgBody;
import vsdk.toolkit.environment.scene.csg.CsgBodyEvaluator;
import vsdk.toolkit.environment.scene.csg.CsgOperation;
import vsdk.toolkit.processing.BezierPatchTessellator;
import vsdk.toolkit.processing.SolidBoundaryTessellationReport;
import vsdk.toolkit.processing.SolidBoundaryTessellator;

/**
One non-sphere geometry of the example, built once and shared by both
renderers: the software path ray traces a body that references its
geometries, and the OpenGL path draws the drawable mesh that VITRAL
tessellation derives from that same body. The mesh is an approximation of
declared resolution, never a second definition of the geometry.
*/
public class ShadersGeometryFixture
{
    /** Cells along the longest side of the region of a tessellated solid */
    public static final int SOLID_RESOLUTION = 96;
    /** Parameter steps of the patch mesh along u and v */
    public static final int PATCH_STEPS = 48;

    private final ShadersGeometry kind;
    private final ArrayList<Geometry> geometries = new ArrayList<Geometry>();
    private GeometryBounds region = new GeometryBounds();
    private TriangleMesh drawableMesh;
    private final SolidBoundaryTessellationReport report = new SolidBoundaryTessellationReport();

    /** PRE: kind is not SPHERE */
    public ShadersGeometryFixture(ShadersGeometry inKind)
    {
        kind = inKind;
        drawableMesh = null;
        switch ( kind ) {
            case QUARTIC: buildQuartic(); break;
            case BLOB: buildBlob(); break;
            case HEIGHT_FIELD: buildHeightField(); break;
            case CSG: buildCsg(); break;
            case PATCH: buildPatch(); break;
            case SPHERE: break;
        }
    }

    public ShadersGeometry getKind() { return kind; }

    /** Resolution and accuracy of the solid meshes (empty for the patch) */
    public SolidBoundaryTessellationReport getTessellationReport()
    {
        return report;
    }

    /** Rounded cube x^4 + y^4 + z^4 - 0.4 < 0, a quartic PolynomialSurface */
    private void buildQuartic()
    {
        PolynomialSurface quartic = new PolynomialSurface(4, false);
        double[] c = quartic.getCoefficients();
        c[PolynomialSurface.getTermIndex(4, 4, 0, 0)] = 1.0;
        c[PolynomialSurface.getTermIndex(4, 0, 4, 0)] = 1.0;
        c[PolynomialSurface.getTermIndex(4, 0, 0, 4)] = 1.0;
        c[PolynomialSurface.getTermIndex(4, 0, 0, 0)] = -0.4;
        geometries.add(quartic);
        region.setFinite(new Vector3Dd(-0.85, -0.85, -0.85), new Vector3Dd(0.85, 0.85, 0.85));
    }

    /** Four merged blob components */
    private void buildBlob()
    {
        BlobSolid blob = new BlobSolid(0.5, false);
        blob.addComponent(new BlobComponent(new Vector3Dd(-0.45, 0, -0.3), 0.8, 1.0));
        blob.addComponent(new BlobComponent(new Vector3Dd(0.45, 0, -0.3), 0.8, 1.0));
        blob.addComponent(new BlobComponent(new Vector3Dd(0, 0, 0.45), 0.8, 1.0));
        blob.addComponent(new BlobComponent(new Vector3Dd(0, -0.35, 0), 0.6, 1.0));
        geometries.add(blob);
        GeometryBounds bounds = new GeometryBounds();
        blob.getBounds(bounds);
        region = bounds.inflated(0.05);
    }

    /**
    Hills on a 33 x 33 grid, laid with its height along world z and scaled into
    a 1.6 unit square by the body's placement.
    */
    private void buildHeightField()
    {
        final long size = 33;
        HeightFieldSolid field = new HeightFieldSolid(size, size, new Vector3Dd(1, 0, 1), new Vector3Dd(size - 2, 1, size - 2));
        for ( long x = 0; x < size; x++ ) {
            for ( long z = 0; z < size; z++ ) {
                final double u = (x - 16.0) / 16.0, v = (z - 16.0) / 16.0;
                final double h = 0.45 + 0.3 * Math.cos(3.0 * u) * Math.cos(2.5 * v) +
                    0.15 * Math.sin(5.0 * (u + v));
                field.setHeight(x, z, h);
            }
        }
        field.updateBlocks();
        geometries.add(field);
        region.setFinite(new Vector3Dd(-0.82, -0.82, -0.45), new Vector3Dd(0.82, 0.82, 0.35));
    }

    /**
    The classic CSG sample: the intersection of a cube and a sphere, minus three
    cylinders along the axes, evaluated with Roth's classification.
    */
    private void buildCsg()
    {
        geometries.add(new Box(1.3, 1.3, 1.3));
        geometries.add(new Sphere(0.85));
        final double r2 = 0.33 * 0.33;
        geometries.add(new QuadricSurface(new Vector3Dd(1, 1, 0), new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 0), -r2));
        geometries.add(new QuadricSurface(new Vector3Dd(0, 1, 1), new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 0), -r2));
        geometries.add(new QuadricSurface(new Vector3Dd(1, 0, 1), new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 0), -r2));
        region.setFinite(new Vector3Dd(-0.72, -0.72, -0.72), new Vector3Dd(0.72, 0.72, 0.72));
    }

    /** A wavy open Bezier patch facing the camera */
    private void buildPatch()
    {
        Vector3Dd[][] net = new Vector3Dd[4][4];
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                final double x = -0.9 + 0.6 * i, z = -0.9 + 0.6 * j;
                final double y = ((i + j) % 2 == 0 ? -0.35 : 0.35) * ((i == 0 || i == 3) ? 0.5 : 1.0);
                net[i][j] = new Vector3Dd(x, y, z);
            }
        }
        geometries.add(new SubdividedBezierPatch(BezierPatchSubdivision.UniformGrid, 16, 16, 0.0, net));
    }

    /**
    A new body for one frame, referencing the fixture's geometries, owning
    the given material (which may be null) and turned by the rotation.
    */
    public SimpleBody createBody(SimpleMaterial material, Matrix4x4d rotation)
    {
        SimpleBody body = null;
        // cpp setGeometryReference (borrowed); the same under garbage collection.
        if ( kind == ShadersGeometry.CSG ) {
            CsgBody csg = new CsgBody(CsgOperation.DIFFERENCE, CsgBodyEvaluator.ROTH);
            final long core = csg.addNode(0, CsgOperation.INTERSECTION);
            for ( int i = 0; i < geometries.size(); i++ ) {
                SimpleBody leaf = new SimpleBody();
                // cpp leaves start without a material, so the CSG body's one
                // shades them; the Java SimpleBody starts with a default one.
                leaf.setMaterial(null);
                leaf.setGeometry(geometries.get(i));
                csg.addLeaf(i < 2 ? core : 0, leaf);
            }
            body = csg;
        }
        else {
            body = new SimpleBody();
            body.setGeometry(geometries.get(0));
        }
        if ( material != null ) body.setMaterial(material);
        Matrix4x4d forward = Matrix4x4d.identityMatrix();
        if ( kind == ShadersGeometry.HEIGHT_FIELD ) {
            // Grid x to world x, grid z to world -y, height y to world z.
            final double s = 1.6 / 30.0, hz = 0.7;
            final double[][] m = {
                { s, 0, 0, -16.0 * s },
                { 0, 0, -s, 16.0 * s },
                { 0, hz, 0, -0.4 },
                { 0, 0, 0, 1 } };
            forward = new Matrix4x4d(m);
        }
        final Matrix4x4d placed = rotation.multiply(forward);
        body.setAffinePlacement(placed, placed.inverse());
        return body;
    }

    /** Mesh drawn by the OpenGL path, built on first use; owned by the fixture */
    public TriangleMesh getDrawableMesh()
    {
        if ( drawableMesh != null ) return drawableMesh;
        if ( kind == ShadersGeometry.PATCH ) {
            drawableMesh = BezierPatchTessellator.tessellate(
                (SubdividedBezierPatch)geometries.get(0), PATCH_STEPS, PATCH_STEPS);
            return drawableMesh;
        }
        SimpleBody body = createBody(null, Matrix4x4d.identityMatrix());
        drawableMesh = SolidBoundaryTessellator.tessellate(body, region, SOLID_RESOLUTION, report);
        return drawableMesh;
    }

    public static String nameOf(ShadersGeometry geometry)
    {
        switch ( geometry ) {
            case SPHERE: return "sphere";
            case QUARTIC: return "quartic";
            case BLOB: return "blob";
            case HEIGHT_FIELD: return "heightfield";
            case CSG: return "csg";
            case PATCH: return "patch";
        }
        return "sphere";
    }

    /** @return null for an unknown name (cpp: false, with the result unchanged) */
    public static ShadersGeometry parse(String name)
    {
        for ( ShadersGeometry g = ShadersGeometry.SPHERE; ; g = next(g) ) {
            if ( name.equals(nameOf(g)) ) {
                return g;
            }
            if ( next(g) == ShadersGeometry.SPHERE ) return null;
        }
    }

    public static ShadersGeometry next(ShadersGeometry geometry)
    {
        switch ( geometry ) {
            case SPHERE: return ShadersGeometry.QUARTIC;
            case QUARTIC: return ShadersGeometry.BLOB;
            case BLOB: return ShadersGeometry.HEIGHT_FIELD;
            case HEIGHT_FIELD: return ShadersGeometry.CSG;
            case CSG: return ShadersGeometry.PATCH;
            case PATCH: return ShadersGeometry.SPHERE;
        }
        return ShadersGeometry.SPHERE;
    }
}
