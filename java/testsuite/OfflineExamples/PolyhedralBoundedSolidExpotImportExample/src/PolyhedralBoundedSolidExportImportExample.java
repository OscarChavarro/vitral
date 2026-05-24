import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.io.geometry.stepCad.reader.StepReader;
import vsdk.toolkit.io.geometry.stepCad.writer.StepWriter;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

/**
CLI example: builds two PolyhedralBoundedSolid cylinders (inspired by the
moon motif of CsgKurlanderBowlFixture) and exports each to an AP242 STEP
file under output/.

Solid A: a tall upright cylinder (r=0.15, h=0.55).
Solid B: a shorter, slightly offset cylinder (r=0.15, h=0.50) that, when
used together with A in a boolean subtraction, produces a crescent moon
silhouette -- the same construction used by CsgKurlanderBowlFixture.

The program must be run with CWD set to the project directory:
  java/testsuite/OfflineExamples/PolyhedralBoundedSolidExpotImportExample
The output/ folder (created on the fly) is listed in .gitignore.
*/
public class PolyhedralBoundedSolidExportImportExample {

    private static final int CYLINDER_SIDES = 30;

    public static void main(String[] args) throws Exception
    {
        File outputDir = new File("output");
        if ( !outputDir.exists() && !outputDir.mkdirs() ) {
            throw new RuntimeException(
                "Cannot create output directory: " + outputDir.getAbsolutePath());
        }

        System.out.println("Building solid A (upright cylinder r=0.15, h=0.55)...");
        PolyhedralBoundedSolid solidA = createCylinder(
            0.15, 0.55, new Vector3Dd(0.0, 0.0, 0.0));
        System.out.println("  Vertices: " + solidA.getVerticesList().size()
            + "  Edges: " + solidA.getEdgesList().size()
            + "  Faces: "  + solidA.getPolygonsList().size());

        System.out.println("Building solid B (offset cylinder r=0.15, h=0.50)...");
        PolyhedralBoundedSolid solidB = createCylinder(
            0.15, 0.50, new Vector3Dd(0.11, 0.0, 0.06));
        System.out.println("  Vertices: " + solidB.getVerticesList().size()
            + "  Edges: " + solidB.getEdgesList().size()
            + "  Faces: "  + solidB.getPolygonsList().size());

        exportStep(solidA, "output/solidA.step", "solidA");
        exportStep(solidB, "output/solidB.step", "solidB");

        System.out.println("Reading back output/solidA.step ...");
        PolyhedralBoundedSolid solidABack = importStep("output/solidA.step");
        System.out.println("  Vertices: " + solidABack.getVerticesList().size()
            + "  Edges: " + solidABack.getEdgesList().size()
            + "  Faces: "  + solidABack.getPolygonsList().size());

        System.out.println("Reading back output/solidB.step ...");
        PolyhedralBoundedSolid solidBBack = importStep("output/solidB.step");
        System.out.println("  Vertices: " + solidBBack.getVerticesList().size()
            + "  Edges: " + solidBBack.getEdgesList().size()
            + "  Faces: "  + solidBBack.getPolygonsList().size());

        System.out.println("Done. Round-trip complete.");
    }

    private static PolyhedralBoundedSolid createCylinder(double radius,
                                                         double height,
                                                         Vector3Dd translation)
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidModeler.createCircularLamina(
                0.0, 0.0, radius, 0.0, CYLINDER_SIDES);

        Matrix4x4d sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);

        Matrix4x4d move = new Matrix4x4d();
        move = move.translation(translation);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static PolyhedralBoundedSolid importStep(String path) throws Exception
    {
        File file = new File(path);
        PolyhedralBoundedSolid solid = StepReader.readSolid(file);
        System.out.println("  Validation: " + (solid.isValid() ? "OK" : "FAILED"));
        return solid;
    }

    private static void exportStep(PolyhedralBoundedSolid solid,
                                   String path,
                                   String name) throws Exception
    {
        System.out.println("Exporting " + name + " -> " + path + " ...");
        File file = new File(path);
        try ( OutputStream out = new FileOutputStream(file) ) {
            StepWriter.exportSolid(solid, out, name);
        }
        System.out.println("  Written " + file.length() + " bytes.");
    }
}
