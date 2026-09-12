// VSDK classes
import { Double, JavaMath, Polygon2D, Vertex2D, _Polygon2DContour } from "@vitral/base";

// Application classes
import { PolygonModel } from "./PolygonModel.js";

class Bounds {
    public minX: number = Double.MAX_VALUE;
    public minY: number = Double.MAX_VALUE;
    public maxX: number = -Double.MAX_VALUE;
    public maxY: number = -Double.MAX_VALUE;

    public include(vertex: Vertex2D): void {
        this.minX = JavaMath.min(this.minX, vertex.x);
        this.minY = JavaMath.min(this.minY, vertex.y);
        this.maxX = JavaMath.max(this.maxX, vertex.x);
        this.maxY = JavaMath.max(this.maxY, vertex.y);
    }
}

export class RenderTransform {
    private static readonly EPSILON: number = 1e-9;
    private static readonly DEFAULT_IMAGE_MARGIN: number = 10;

    private readonly minX: number;
    private readonly minY: number;
    private readonly scale: number;
    private readonly offsetX: number;
    private readonly offsetY: number;

    public constructor(minX: number, minY: number, scale: number, offsetX: number, offsetY: number) {
        this.minX = minX;
        this.minY = minY;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public static compute(polygon: Polygon2D, model: PolygonModel): RenderTransform {
        const bounds: Bounds = RenderTransform.computeBounds(polygon);
        const minX: number = bounds.minX;
        const minY: number = bounds.minY;
        const polygonWidth: number = bounds.maxX - bounds.minX;
        const polygonHeight: number = bounds.maxY - bounds.minY;
        const usableWidth: number = model.getZoneWidth() - 1.0 - 2.0 * RenderTransform.DEFAULT_IMAGE_MARGIN;
        const usableHeight: number = model.getZoneHeight() - 1.0 - 2.0 * RenderTransform.DEFAULT_IMAGE_MARGIN;
        const scale: number =
            polygonWidth < RenderTransform.EPSILON || polygonHeight < RenderTransform.EPSILON
                ? 1.0
                : JavaMath.min(usableWidth / polygonWidth, usableHeight / polygonHeight);
        const scaledWidth: number = polygonWidth * scale;
        const scaledHeight: number = polygonHeight * scale;
        const offsetX: number = RenderTransform.DEFAULT_IMAGE_MARGIN + (usableWidth - scaledWidth) / 2.0;
        const offsetY: number = RenderTransform.DEFAULT_IMAGE_MARGIN + (usableHeight - scaledHeight) / 2.0;

        return new RenderTransform(minX, minY, scale, offsetX, offsetY);
    }

    private static computeBounds(polygon: Polygon2D): Bounds {
        const bounds: Bounds = new Bounds();
        let contour: _Polygon2DContour;
        for (contour of polygon.loops) {
            let vertex: Vertex2D;
            for (vertex of contour.vertices) {
                bounds.include(vertex);
            }
        }
        return bounds;
    }

    public getMinX(): number {
        return this.minX;
    }

    public getMinY(): number {
        return this.minY;
    }

    public getScale(): number {
        return this.scale;
    }

    public getOffsetX(): number {
        return this.offsetX;
    }

    public getOffsetY(): number {
        return this.offsetY;
    }
}
