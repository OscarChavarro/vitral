// VSDK classes
import { Polygon2D, RGBImageUncompressed, RGBPixel, Rasterizer2D, Vertex2D, _Polygon2DContour } from "@vitral/base";

// Application classes
import { PolygonModel } from "../model/PolygonModel.js";

export class PolygonRasterizer {
    public renderSmoothFilledPolygon(
        image: RGBImageUncompressed,
        model: PolygonModel,
        minX: number,
        minY: number,
        scale: number,
        offsetX: number,
        offsetY: number,
        borderColor: RGBPixel,
    ): void {
        const projectedPolygon: Polygon2D = this.projectPolygon(
            model.getPolygon2D(),
            minX,
            minY,
            scale,
            offsetX,
            offsetY,
        );

        try {
            Rasterizer2D.fillSmoothPolygon(image, projectedPolygon);
        } catch {
            // Some polygon shapes trigger the known quicksort issue in this helper.
        }

        Rasterizer2D.drawPolygon(image, projectedPolygon, borderColor);
    }

    public renderPolygonBorder(
        image: RGBImageUncompressed,
        model: PolygonModel,
        minX: number,
        minY: number,
        scale: number,
        offsetX: number,
        offsetY: number,
        borderColor: RGBPixel,
    ): void {
        const projectedPolygon: Polygon2D = this.projectPolygon(
            model.getPolygon2D(),
            minX,
            minY,
            scale,
            offsetX,
            offsetY,
        );
        Rasterizer2D.drawPolygon(image, projectedPolygon, borderColor);
    }

    public projectPolygon(
        sourcePolygon: Polygon2D,
        minX: number,
        minY: number,
        scale: number,
        offsetX: number,
        offsetY: number,
    ): Polygon2D {
        const projectedPolygon: Polygon2D = new Polygon2D();
        projectedPolygon.loops.length = 0;

        let contour: _Polygon2DContour;
        for (contour of sourcePolygon.loops) {
            projectedPolygon.nextLoop();
            let vertex: Vertex2D;
            for (vertex of contour.vertices) {
                const projectedX: number = offsetX + (vertex.x - minX) * scale;
                const projectedY: number = offsetY + (vertex.y - minY) * scale;
                projectedPolygon.addVertex(projectedX, projectedY, 0.2, 0.6, 1.0);
            }
        }

        return projectedPolygon;
    }
}
