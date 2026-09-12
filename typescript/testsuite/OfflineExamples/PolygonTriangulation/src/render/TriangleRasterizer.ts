// VSDK classes
import {
    Integer,
    JavaMath,
    MonotoneDecompositionTriangulator,
    Polygon2D,
    RGBImageUncompressed,
    RGBPixel,
    Rasterizer2D,
    VSDK,
    Vertex2D,
    _Polygon2DContour,
} from "@vitral/base";

// Application classes
import { PolygonModel } from "../model/PolygonModel.js";
import { PolygonRasterizer } from "./PolygonRasterizer.js";

export class TriangleRasterizer {
    private static readonly TRIANGLE_FILL_PALETTE: number[][] = [
        [
            VSDK.unsigned8BitInteger2signedByte(210),
            VSDK.unsigned8BitInteger2signedByte(90),
            VSDK.unsigned8BitInteger2signedByte(90),
        ],
        [
            VSDK.unsigned8BitInteger2signedByte(90),
            VSDK.unsigned8BitInteger2signedByte(200),
            VSDK.unsigned8BitInteger2signedByte(90),
        ],
        [
            VSDK.unsigned8BitInteger2signedByte(90),
            VSDK.unsigned8BitInteger2signedByte(90),
            VSDK.unsigned8BitInteger2signedByte(220),
        ],
        [
            VSDK.unsigned8BitInteger2signedByte(210),
            VSDK.unsigned8BitInteger2signedByte(200),
            VSDK.unsigned8BitInteger2signedByte(80),
        ],
        [
            VSDK.unsigned8BitInteger2signedByte(80),
            VSDK.unsigned8BitInteger2signedByte(200),
            VSDK.unsigned8BitInteger2signedByte(200),
        ],
        [
            VSDK.unsigned8BitInteger2signedByte(200),
            VSDK.unsigned8BitInteger2signedByte(80),
            VSDK.unsigned8BitInteger2signedByte(200),
        ],
        [
            VSDK.unsigned8BitInteger2signedByte(200),
            VSDK.unsigned8BitInteger2signedByte(140),
            VSDK.unsigned8BitInteger2signedByte(60),
        ],
        [
            VSDK.unsigned8BitInteger2signedByte(140),
            VSDK.unsigned8BitInteger2signedByte(80),
            VSDK.unsigned8BitInteger2signedByte(200),
        ],
    ];

    public renderTriangulatedPolygon(
        image: RGBImageUncompressed,
        model: PolygonModel,
        triangles: MonotoneDecompositionTriangulator.Triangle[],
        minX: number,
        minY: number,
        scale: number,
        offsetX: number,
        offsetY: number,
        borderColor: RGBPixel,
    ): void {
        const polygonVertices: Vertex2D[] = TriangleRasterizer.flattenVertices(model.getPolygon2D());
        const edgeColor: RGBPixel = new RGBPixel();
        edgeColor.r = -1;
        edgeColor.g = -1;
        edgeColor.b = -1;

        const fillColor: RGBPixel = new RGBPixel();

        let i: number;
        for (i = 0; i < triangles.length; i++) {
            const triangle: MonotoneDecompositionTriangulator.Triangle = triangles[i]!;
            if (!TriangleRasterizer.isValidTriangleIndex(triangle, polygonVertices.length)) {
                continue;
            }

            const vertexA: Vertex2D = polygonVertices[triangle.a]!;
            const vertexB: Vertex2D = polygonVertices[triangle.b]!;
            const vertexC: Vertex2D = polygonVertices[triangle.c]!;

            const ax: number = TriangleRasterizer.projectX(vertexA.x, minX, scale, offsetX);
            const ay: number = TriangleRasterizer.projectY(vertexA.y, minY, scale, offsetY);
            const bx: number = TriangleRasterizer.projectX(vertexB.x, minX, scale, offsetX);
            const by: number = TriangleRasterizer.projectY(vertexB.y, minY, scale, offsetY);
            const cx: number = TriangleRasterizer.projectX(vertexC.x, minX, scale, offsetX);
            const cy: number = TriangleRasterizer.projectY(vertexC.y, minY, scale, offsetY);

            const paletteEntry: number[] =
                TriangleRasterizer.TRIANGLE_FILL_PALETTE[i % TriangleRasterizer.TRIANGLE_FILL_PALETTE.length]!;
            fillColor.r = paletteEntry[0]!;
            fillColor.g = paletteEntry[1]!;
            fillColor.b = paletteEntry[2]!;

            TriangleRasterizer.fillTriangle(image, ax, ay, bx, by, cx, cy, fillColor);

            Rasterizer2D.drawLine(image, ax, ay, bx, by, edgeColor);
            Rasterizer2D.drawLine(image, bx, by, cx, cy, edgeColor);
            Rasterizer2D.drawLine(image, cx, cy, ax, ay, edgeColor);
        }

        const polygonRasterizer: PolygonRasterizer = new PolygonRasterizer();
        polygonRasterizer.renderPolygonBorder(image, model, minX, minY, scale, offsetX, offsetY, borderColor);
    }

    private static flattenVertices(polygon: Polygon2D): Vertex2D[] {
        const vertices: Vertex2D[] = [];
        let contour: _Polygon2DContour;
        for (contour of polygon.loops) {
            vertices.push(...contour.vertices);
        }
        return vertices;
    }

    private static isValidTriangleIndex(
        triangle: MonotoneDecompositionTriangulator.Triangle,
        vertexCount: number,
    ): boolean {
        return (
            triangle.a >= 0 &&
            triangle.b >= 0 &&
            triangle.c >= 0 &&
            triangle.a < vertexCount &&
            triangle.b < vertexCount &&
            triangle.c < vertexCount
        );
    }

    private static projectX(x: number, minX: number, scale: number, offsetX: number): number {
        return Math.trunc(offsetX + (x - minX) * scale);
    }

    private static projectY(y: number, minY: number, scale: number, offsetY: number): number {
        return Math.trunc(offsetY + (y - minY) * scale);
    }

    private static fillTriangle(
        image: RGBImageUncompressed,
        x0: number,
        y0: number,
        x1: number,
        y1: number,
        x2: number,
        y2: number,
        color: RGBPixel,
    ): void {
        const imageWidth: number = image.getXSize();
        const imageHeight: number = image.getYSize();
        const yMin: number = JavaMath.max(0, JavaMath.min(y0, JavaMath.min(y1, y2)));
        const yMax: number = JavaMath.min(imageHeight - 1, JavaMath.max(y0, JavaMath.max(y1, y2)));

        const vertices: number[][] = [
            [x0, y0],
            [x1, y1],
            [x2, y2],
        ];

        let y: number;
        for (y = yMin; y <= yMax; y++) {
            let xMin: number = Integer.MAX_VALUE;
            let xMax: number = Integer.MIN_VALUE;

            let edgeIndex: number;
            for (edgeIndex = 0; edgeIndex < 3; edgeIndex++) {
                const ax: number = vertices[edgeIndex]![0]!;
                const ay: number = vertices[edgeIndex]![1]!;
                const bx: number = vertices[(edgeIndex + 1) % 3]![0]!;
                const by: number = vertices[(edgeIndex + 1) % 3]![1]!;

                if (ay === by) {
                    if (ay === y) {
                        xMin = JavaMath.min(xMin, JavaMath.min(ax, bx));
                        xMax = JavaMath.max(xMax, JavaMath.max(ax, bx));
                    }
                } else if ((ay <= y && y <= by) || (by <= y && y <= ay)) {
                    const interpolationFactor: number = (y - ay) / (by - ay);
                    const xIntersection: number = Math.trunc(ax + interpolationFactor * (bx - ax));
                    xMin = JavaMath.min(xMin, xIntersection);
                    xMax = JavaMath.max(xMax, xIntersection);
                }
            }

            if (xMin > xMax) {
                continue;
            }

            xMin = JavaMath.max(0, xMin);
            xMax = JavaMath.min(imageWidth - 1, xMax);

            let x: number;
            for (x = xMin; x <= xMax; x++) {
                image.putPixelRgb(x, y, color);
            }
        }
    }
}
