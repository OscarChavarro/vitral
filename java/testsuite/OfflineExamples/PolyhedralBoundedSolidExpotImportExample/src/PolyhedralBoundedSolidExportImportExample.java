import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.io.geometry.stepCad.StepLengthUnit;
import vsdk.toolkit.io.geometry.stepCad.reader.StepReader;
import vsdk.toolkit.io.geometry.stepCad.writer.StepWriter;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.CsgKurlanderBowlFixture;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

/**
CLI example: builds two PolyhedralBoundedSolid objects for the selected model
and exports each to an AP242 STEP file under output/ using millimetre units,
then reads them back to verify the round-trip.

Usage:
  java PolyhedralBoundedSolidExportImportExample [model]

Models:
  moonMotif  (default) - two cylinders inspired by the CsgKurlanderBowl moon
             motif: a tall upright cylinder (A) and a shorter, slightly offset
             cylinder (B).  When subtracted, they produce a crescent silhouette.

  bowl       - the CsgKurlanderBowl family:
               solidA: the bowl base (outer sphere minus inner sphere clipped
                       by a guide cylinder).
               solidB: the union of all 40 motifs (20 moons + 20 stars) as a
                       multi-shell solid.

The program must be run with CWD set to the project directory:
  java/testsuite/OfflineExamples/PolyhedralBoundedSolidExpotImportExample
The output/ folder (created on the fly) is listed in .gitignore.
*/
public class PolyhedralBoundedSolidExportImportExample {

    private static final int CYLINDER_SIDES = 30;

    public static void main(String[] args) throws Exception
    {
        String model = System.getProperty("model", "");
        if ( model.isEmpty() ) {
            printUsage();
            return;
        }
        if ( model.equals("bowl") ) {
            runBowl();
        } else if ( model.equals("moonMotif") ) {
            runMoonMotif();
        } else {
            System.err.println("Unknown model: " + model);
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage()
    {
        System.out.println(
            "Usage: ./run.sh <model>\n" +
            "\n" +
            "Available models:\n" +
            "  moonMotif   Two cylinders forming a crescent moon motif.\n" +
            "              Exports output/solidA.step and output/solidB.step.\n" +
            "\n" +
            "  bowl        CsgKurlanderBowl family:\n" +
            "                solidA - bowl base (sphere shell clipped by cylinder).\n" +
            "                solidB - union of all 40 motifs (20 moons + 20 stars).\n" +
            "              Exports output/bowlBase.step and output/bowlMotifs.step.\n" +
            "\n" +
            "Each model exports the solids in millimetres (AP242 STEP) and reads\n" +
            "them back to verify the round-trip."
        );
    }

    // ── moonMotif ────────────────────────────────────────────────────────────

    private static void runMoonMotif() throws Exception
    {
        File outputDir = ensureOutputDir();

        System.out.println("=== moonMotif ===");
        System.out.println("Building solid A (upright cylinder r=0.15 m, h=0.55 m)...");
        PolyhedralBoundedSolid solidA = createCylinder(
            0.15, 0.55, new Vector3Dd(0.0, 0.0, 0.0));
        printStats("solidA", solidA);

        System.out.println("Building solid B (offset cylinder r=0.15 m, h=0.50 m)...");
        PolyhedralBoundedSolid solidB = createCylinder(
            0.15, 0.50, new Vector3Dd(0.11, 0.0, 0.06));
        printStats("solidB", solidB);

        exportStep(solidA, "output/solidA.step", "solidA");
        exportStep(solidB, "output/solidB.step", "solidB");

        System.out.println("Reading back output/solidA.step ...");
        PolyhedralBoundedSolid solidABack = importStep("output/solidA.step");
        printStats("solidA (round-trip)", solidABack);

        System.out.println("Reading back output/solidB.step ...");
        PolyhedralBoundedSolid solidBBack = importStep("output/solidB.step");
        printStats("solidB (round-trip)", solidBBack);

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

    // ── bowl ─────────────────────────────────────────────────────────────────

    private static void runBowl() throws Exception
    {
        File outputDir = ensureOutputDir();

        System.out.println("=== bowl ===");
        System.out.println(
            "Building solidA: bowl base (sphere shell clipped by cylinder)...");
        PolyhedralBoundedSolid solidA = CsgKurlanderBowlFixture.createBowl();
        printStats("solidA (bowl)", solidA);

        System.out.println(
            "Building solidB: union of all 40 motifs (20 moons + 20 stars)...");
        PolyhedralBoundedSolid solidB =
            CsgKurlanderBowlFixture.createAllMotifsUnion();
        printStats("solidB (motifs union)", solidB);

        exportStep(solidA, "output/bowlBase.step", "bowlBase");
        exportStep(solidB, "output/bowlMotifs.step", "bowlMotifs");

        System.out.println("Reading back output/bowlBase.step ...");
        PolyhedralBoundedSolid solidABack = importStep("output/bowlBase.step");
        printStats("solidA (round-trip)", solidABack);

        System.out.println("Reading back output/bowlMotifs.step ...");
        PolyhedralBoundedSolid solidBBack = importStep("output/bowlMotifs.step");
        printStats("solidB (round-trip)", solidBBack);

        System.out.println("Done. Round-trip complete.");
    }

    // ── shared helpers ────────────────────────────────────────────────────────

    private static File ensureOutputDir() throws Exception
    {
        File outputDir = new File("output");
        if ( !outputDir.exists() && !outputDir.mkdirs() ) {
            throw new RuntimeException(
                "Cannot create output directory: " + outputDir.getAbsolutePath());
        }
        return outputDir;
    }

    private static void printStats(String label, PolyhedralBoundedSolid solid)
    {
        System.out.println("  " + label
            + "  Vertices: " + solid.getVerticesList().size()
            + "  Edges: "    + solid.getEdgesList().size()
            + "  Faces: "    + solid.getPolygonsList().size());
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
        System.out.println("Exporting " + name + " -> " + path
            + " (millimeters) ...");
        File file = new File(path);
        try ( OutputStream out = new FileOutputStream(file) ) {
            StepWriter.exportSolid(solid, out, name, StepLengthUnit.MILLIMETERS);
        }
        System.out.println("  Written " + file.length() + " bytes.");
    }
}
