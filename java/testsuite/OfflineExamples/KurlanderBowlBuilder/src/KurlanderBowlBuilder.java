import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.CsgKurlanderBowlFixture;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.io.geometry.stepCad.StepLengthUnit;
import vsdk.toolkit.io.geometry.stepCad.reader.StepReader;
import vsdk.toolkit.io.geometry.stepCad.writer.StepWriter;

public class KurlanderBowlBuilder
{
    private static final int MOTIF_COUNT = 40;
    private static final File OUTPUT_STEP = new File("output.step");

    public static void main(String[] args) throws Exception
    {
        long startedAt = System.nanoTime();
        int motifIndex;

        for ( motifIndex = 0; motifIndex < MOTIF_COUNT; motifIndex++ ) {
            long stepStartedAt = System.nanoTime();
            PolyhedralBoundedSolid operandA = loadOperandA();
            PolyhedralBoundedSolid operandB =
                CsgKurlanderBowlFixture.createSingleMotif(motifIndex);
            PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
                operandA, operandB, PolyhedralBoundedSolidModeler.SUBTRACT,
                false, false);

            writeStep(result, OUTPUT_STEP, "kurlander_bowl_step_" + motifIndex);

            double stepSeconds = secondsSince(stepStartedAt);
            double accumulatedSeconds = secondsSince(startedAt);
            System.out.printf(
                "motif %02d/40: step=%.3fs total=%.3fs%n",
                motifIndex + 1, stepSeconds, accumulatedSeconds);
        }
    }

    private static PolyhedralBoundedSolid loadOperandA() throws Exception
    {
        if ( OUTPUT_STEP.isFile() ) {
            return StepReader.readSolid(OUTPUT_STEP);
        }
        return CsgKurlanderBowlFixture.createBowl();
    }

    private static void writeStep(
        PolyhedralBoundedSolid solid, File stepFile, String productName)
        throws Exception
    {
        try ( OutputStream out = new FileOutputStream(stepFile) ) {
            StepWriter.exportSolid(solid, out, productName,
                StepLengthUnit.METERS);
        }
    }

    private static double secondsSince(long startedAt)
    {
        return (System.nanoTime() - startedAt) / 1000000000.0;
    }
}
