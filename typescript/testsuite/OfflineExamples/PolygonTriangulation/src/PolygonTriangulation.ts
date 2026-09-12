// Basic JDK classes
import { File, ImagePersistence } from "@vitral/fs";

// Vitral classes
import {
    IllegalArgumentException,
    MonotoneDecompositionTriangulator,
    Polygon2D,
    RGBImageUncompressed,
    RGBPixel,
} from "@vitral/base";

// Application classes
import { PolygonReader } from "./io/PolygonReader.js";
import { PolygonModel } from "./model/PolygonModel.js";
import { RenderTransform } from "./model/RenderTransform.js";
import { CommandLineOptions } from "./options/CommandLineOptions.js";
import { PolygonRasterizer } from "./render/PolygonRasterizer.js";
import { TriangleRasterizer } from "./render/TriangleRasterizer.js";

export class PolygonTriangulation {
    private static parseCommandLineOptions(args: string[]): CommandLineOptions | null {
        try {
            const commandLineOptions: CommandLineOptions = CommandLineOptions.parse(args);
            if (commandLineOptions.shouldShowHelp()) {
                CommandLineOptions.printUsage();
                return null;
            }
            return commandLineOptions;
        } catch (exception) {
            if (!(exception instanceof IllegalArgumentException)) {
                throw exception;
            }
            console.error(exception.message);
            CommandLineOptions.printUsage();
            return null;
        }
    }

    private static loadInputPolygon(commandLineOptions: CommandLineOptions): Polygon2D {
        const polygonReader: PolygonReader = new PolygonReader();
        return polygonReader.read(commandLineOptions.getInputFileName());
    }

    private static triangulatePolygon(inputPolygon: Polygon2D): MonotoneDecompositionTriangulator.Triangle[] {
        const triangulator: MonotoneDecompositionTriangulator = new MonotoneDecompositionTriangulator();
        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        triangulator.triangulate(inputPolygon, triangles);
        return triangles;
    }

    private static printTriangles(triangles: MonotoneDecompositionTriangulator.Triangle[]): void {
        let triangleIndex: number;
        for (triangleIndex = 0; triangleIndex < triangles.length; triangleIndex++) {
            const triangle: MonotoneDecompositionTriangulator.Triangle = triangles[triangleIndex]!;
            console.log("triangle #" + triangleIndex + ": " + triangle.a + " " + triangle.b + " " + triangle.c);
        }
    }

    private static createWorkingImage(model: PolygonModel): RGBImageUncompressed {
        const image: RGBImageUncompressed = new RGBImageUncompressed();
        image.init(model.getImageWidth(), model.getImageHeight());
        image.createTestPattern();
        return image;
    }

    private static createBorderColor(): RGBPixel {
        const borderColor: RGBPixel = new RGBPixel();
        borderColor.r = -1;
        borderColor.g = -1;
        borderColor.b = 0;
        return borderColor;
    }

    private static renderPolygonPanel(
        image: RGBImageUncompressed,
        model: PolygonModel,
        renderTransform: RenderTransform,
        borderColor: RGBPixel,
    ): void {
        const polygonRasterizer: PolygonRasterizer = new PolygonRasterizer();
        polygonRasterizer.renderSmoothFilledPolygon(
            image,
            model,
            renderTransform.getMinX(),
            renderTransform.getMinY(),
            renderTransform.getScale(),
            renderTransform.getOffsetX(),
            renderTransform.getOffsetY(),
            borderColor,
        );
    }

    private static renderTrianglePanel(
        image: RGBImageUncompressed,
        model: PolygonModel,
        triangles: MonotoneDecompositionTriangulator.Triangle[],
        renderTransform: RenderTransform,
        borderColor: RGBPixel,
    ): void {
        const triangleRasterizer: TriangleRasterizer = new TriangleRasterizer();
        triangleRasterizer.renderTriangulatedPolygon(
            image,
            model,
            triangles,
            renderTransform.getMinX(),
            renderTransform.getMinY(),
            renderTransform.getScale(),
            renderTransform.getOffsetX() + model.getZoneWidth(),
            renderTransform.getOffsetY(),
            borderColor,
        );
    }

    private static exportImage(image: RGBImageUncompressed, outputFileName: string): void {
        ImagePersistence.exportPNG(new File(outputFileName), image);
        console.log("Image written to: " + outputFileName);
    }

    public static main(args: string[]): void {
        try {
            const commandLineOptions: CommandLineOptions | null = PolygonTriangulation.parseCommandLineOptions(args);
            if (commandLineOptions === null) {
                return;
            }

            const inputPolygon: Polygon2D = PolygonTriangulation.loadInputPolygon(commandLineOptions);
            const model: PolygonModel = commandLineOptions.toPolygonModel(inputPolygon);
            const triangles: MonotoneDecompositionTriangulator.Triangle[] =
                PolygonTriangulation.triangulatePolygon(inputPolygon);
            PolygonTriangulation.printTriangles(triangles);

            const image: RGBImageUncompressed = PolygonTriangulation.createWorkingImage(model);
            const renderTransform: RenderTransform = RenderTransform.compute(inputPolygon, model);
            const borderColor: RGBPixel = PolygonTriangulation.createBorderColor();

            PolygonTriangulation.renderPolygonPanel(image, model, renderTransform, borderColor);
            PolygonTriangulation.renderTrianglePanel(image, model, triangles, renderTransform, borderColor);
            PolygonTriangulation.exportImage(image, model.getOutputFileName());
        } catch (exception) {
            const error: Error = exception as Error;
            console.error("PolygonTriangulation failed: " + error.message);
            console.error(error.stack);
        }
    }
}

PolygonTriangulation.main(process.argv.slice(2));
