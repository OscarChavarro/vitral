package matrixexample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import matrixexample.config.CommandLineOptions;
import vsdk.toolkit.common.linealAlgebra.MatrixNxM;
import vsdk.toolkit.processing.linealAlgebra.LinearAlgebraEngine;

public class MatrixExample
{
    public static void main(String[] args) throws IOException
    {
        CommandLineOptions options = CommandLineOptions.parse(args);
        LinearAlgebraEngine engine = LinearAlgebraEngine.fromStrategy(options.strategy());

        MatrixNxM matrix = createDiagonallyDominantMatrix(options.size(), options.seed());

        StringBuilder out = new StringBuilder();
        out.append("strategy=").append(options.strategy()).append('\n');
        out.append("operation=").append(options.operation()).append('\n');
        out.append("size=").append(options.size()).append('\n');
        out.append("seed=").append(options.seed()).append('\n');

        if ( "det".equals(options.operation()) || "both".equals(options.operation()) ) {
            long start = System.nanoTime();
            double determinant = engine.determinant(matrix);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            out.append("determinant=").append(determinant).append('\n');
            out.append("determinantElapsedMs=").append(elapsedMs).append('\n');
        }

        if ( "inv".equals(options.operation()) || "both".equals(options.operation()) ) {
            long start = System.nanoTime();
            MatrixNxM inverse = engine.inverse(matrix);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            out.append("inverseElapsedMs=").append(elapsedMs).append('\n');
            out.append("inverseFrobeniusLikeTrace=").append(approxTrace(inverse)).append('\n');
        }

        Path outputDir = Paths.get("output");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve(options.outputFile());
        Files.writeString(outputFile, out.toString(), StandardCharsets.UTF_8);

        System.out.println(out);
        System.out.println("Wrote results to " + outputFile.toAbsolutePath());
    }

    public static MatrixNxM createDiagonallyDominantMatrix(int n, long seed)
    {
        MatrixNxM matrix = new MatrixNxM(n, n);
        Random random = new Random(seed);
        int i;
        int j;

        for ( i = 0; i < n; i++ ) {
            double rowAbsSum = 0.0;
            for ( j = 0; j < n; j++ ) {
                if ( i == j ) {
                    continue;
                }
                double v = (random.nextDouble() * 2.0) - 1.0;
                matrix = matrix.withVal(i, j, v);
                rowAbsSum += Math.abs(v);
            }
            matrix = matrix.withVal(i, i, rowAbsSum + 1.0 + random.nextDouble());
        }

        return matrix;
    }

    private static double approxTrace(MatrixNxM matrix)
    {
        int n = Math.min(matrix.getNumRows(), matrix.getNumColumns());
        double trace = 0.0;
        int i;
        for ( i = 0; i < n; i++ ) {
            trace += matrix.getVal(i, i);
        }
        return trace;
    }
}
