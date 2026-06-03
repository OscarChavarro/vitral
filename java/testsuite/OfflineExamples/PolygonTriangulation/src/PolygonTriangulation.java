// Java classes
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Vitral classes
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;

// Application classes
import io.PolygonReader;
import model.PolygonModel;
import model.RenderTransform;
import options.CommandLineOptions;
import render.PolygonRasterizer;
import render.TriangleRasterizer;

public class PolygonTriangulation {
    private static CommandLineOptions
    parseCommandLineOptions(String[] args)
    {
        try {
            CommandLineOptions commandLineOptions = CommandLineOptions.parse(args);
            if ( commandLineOptions.shouldShowHelp() ) {
                CommandLineOptions.printUsage();
                return null;
            }
            return commandLineOptions;
        }
        catch ( IllegalArgumentException exception ) {
            System.err.println(exception.getMessage());
            CommandLineOptions.printUsage();
            return null;
        }
    }

    private static Polygon2D
    loadInputPolygon(CommandLineOptions commandLineOptions) throws Exception
    {
        PolygonReader polygonReader = new PolygonReader();
        return polygonReader.read(commandLineOptions.getInputFileName());
    }

    private static List<MonotoneDecompositionTriangulator.Triangle>
    triangulatePolygon(Polygon2D inputPolygon)
    {
        MonotoneDecompositionTriangulator triangulator =
            new MonotoneDecompositionTriangulator();
        List<MonotoneDecompositionTriangulator.Triangle> triangles =
            new ArrayList<>();
        triangulator.triangulate(inputPolygon, triangles);
        return triangles;
    }

    private static void
    printTriangles(List<MonotoneDecompositionTriangulator.Triangle> triangles)
    {
        for ( int triangleIndex = 0; triangleIndex < triangles.size(); triangleIndex++ ) {
            MonotoneDecompositionTriangulator.Triangle triangle =
                triangles.get(triangleIndex);
            System.out.printf("triangle #%d: %d %d %d%n", triangleIndex,
                triangle.a, triangle.b, triangle.c);
        }
    }

    private static RGBImageUncompressed
    createWorkingImage(PolygonModel model)
    {
        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(model.getImageWidth(), model.getImageHeight());
        image.createTestPattern();
        return image;
    }

    private static RGBPixel
    createBorderColor()
    {
        RGBPixel borderColor = new RGBPixel();
        borderColor.r = -1;
        borderColor.g = -1;
        borderColor.b = 0;
        return borderColor;
    }

    private static void
    renderPolygonPanel(RGBImageUncompressed image,
        PolygonModel model, RenderTransform renderTransform,
        RGBPixel borderColor)
    {
        PolygonRasterizer polygonRasterizer = new PolygonRasterizer();
        polygonRasterizer.renderSmoothFilledPolygon(image, model,
            renderTransform.getMinX(), renderTransform.getMinY(),
            renderTransform.getScale(), renderTransform.getOffsetX(),
            renderTransform.getOffsetY(), borderColor);
    }

    private static void
    renderTrianglePanel(
        RGBImageUncompressed image,
        PolygonModel model,
        List<MonotoneDecompositionTriangulator.Triangle> triangles,
        RenderTransform renderTransform, RGBPixel borderColor)
    {
        TriangleRasterizer triangleRasterizer = new TriangleRasterizer();
        triangleRasterizer.renderTriangulatedPolygon(image, model, triangles,
            renderTransform.getMinX(), renderTransform.getMinY(),
            renderTransform.getScale(),
            renderTransform.getOffsetX() + model.getZoneWidth(),
            renderTransform.getOffsetY(), borderColor);
    }

    private static void exportImage(
        RGBImageUncompressed image,
        String outputFileName) throws Exception
    {
        ImagePersistence.exportPNG(new File(outputFileName), image);
        System.out.println("Image written to: " + outputFileName);
    }

    public static void
    main(String[] args) {
        try {
            CommandLineOptions commandLineOptions = parseCommandLineOptions(args);
            if ( commandLineOptions == null ) {
                return;
            }

            Polygon2D inputPolygon = loadInputPolygon(commandLineOptions);
            PolygonModel model = commandLineOptions.toPolygonModel(inputPolygon);
            List<MonotoneDecompositionTriangulator.Triangle> triangles =
                    triangulatePolygon(inputPolygon);
            printTriangles(triangles);

            RGBImageUncompressed image = createWorkingImage(model);
            RenderTransform renderTransform =
                RenderTransform.compute(inputPolygon, model);
            RGBPixel borderColor = createBorderColor();

            renderPolygonPanel(image, model, renderTransform, borderColor);
            renderTrianglePanel(image, model, triangles, renderTransform, borderColor);
            exportImage(image, model.getOutputFileName());
        }
        catch ( Exception exception ) {
            System.err.println("PolygonTriangulation failed: " + exception.getMessage());
            exception.printStackTrace(System.err);
        }
    }
}
