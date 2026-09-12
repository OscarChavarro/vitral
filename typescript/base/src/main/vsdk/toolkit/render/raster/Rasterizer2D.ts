//= References:                                                             =
//= [BRES1965] Bresenham, J.E. "Algorithm for computer control of a digital =
//=            plotter" IBM Syst. J. 4, 1 (1965), 25-30.                    =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =

import { Collections } from "../../../../java/util/Collections.js";
import type { Comparator } from "../../../../java/util/Comparator.js";
import { Double } from "../../../../java/lang/Double.js";
import { Integer } from "../../../../java/lang/Integer.js";
import { VSDK } from "../../common/VSDK.js";
import { Vertex2D } from "../../environment/geometry/element/Vertex2D.js";
import { RGBPixel } from "../../media/RGBPixel.js";
import { Image } from "../../media/Image.js";
import { Polygon2D } from "../../environment/geometry/surface/polygon/Polygon2D.js";
import { _Polygon2DContour } from "../../environment/geometry/surface/polygon/_Polygon2DContour.js";
import { RenderingElement } from "../RenderingElement.js";

class FillEdge {
    public yMin = 0;
    public yMaxExclusive = 0;
    public xAtCurrentY = 0;
    public inverseSlope = 0;
    public sortOrder = 0;
}

interface SpanShader {
    shade(img: Image, polygon: Polygon2D, y: number, xStart: number, xEndExclusive: number): void;
}

export class Rasterizer2D extends RenderingElement {
    private static readonly ACTIVE_EDGE_COMPARATOR: Comparator<FillEdge> = {
        compare(a: FillEdge, b: FillEdge): number {
            let cmp = Double.compare(a.xAtCurrentY, b.xAtCurrentY);
            if (cmp !== 0) {
                return cmp;
            }

            cmp = Double.compare(a.inverseSlope, b.inverseSlope);
            if (cmp !== 0) {
                return cmp;
            }

            return Integer.compare(a.sortOrder, b.sortOrder);
        },
    };

    /**
    This algorithm implements the Bresenham line algoritm with NO CLIPPING!
    See [BRES1965].
    Note that this is a currently naive implementation that makes use
    of double floating point arithmetic, while original Bresenham algorithm
    make use of most efficient integer line arithmetic.
    */
    public static drawLine(img: Image, x0: number, y0: number, x1: number, y1: number, p: RGBPixel): void {
        let dx: number, dy: number;
        let dxdy: number;
        let dydx: number;
        let x: number, y: number;
        let xx: number, yy: number;

        dx = x1 - x0;
        dy = y1 - y0;

        if (Math.abs(dx) > VSDK.EPSILON && Math.abs(dy / dx) <= 1 && x1 > x0) {
            // Pendiente entre -1 y 1
            dydx = dy / dx;
            for (x = x0, yy = y0; x <= x1; x++) {
                y = Math.trunc(yy);
                if (x >= 0 && x < img.getXSize() && y >= 0 && y < img.getYSize()) {
                    img.putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
        } else if (Math.abs(dx) > VSDK.EPSILON && Math.abs(dy / dx) <= 1 && x1 < x0) {
            // Pendiente entre -1 y 1
            dydx = dy / dx;
            for (x = x1, yy = y1; x <= x0; x++) {
                y = Math.trunc(yy);
                if (x >= 0 && x < img.getXSize() && y >= 0 && y < img.getYSize()) {
                    img.putPixelRgb(x, y, p);
                }
                yy += dydx;
            }
        } else if (Math.abs(dy) > VSDK.EPSILON && y1 > y0) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx / dy;
            for (y = y0, xx = x0; y <= y1; y++) {
                x = Math.trunc(xx);
                if (x >= 0 && x < img.getXSize() && y >= 0 && y < img.getYSize()) {
                    img.putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
        } else if (Math.abs(dy) > VSDK.EPSILON && y1 < y0) {
            // Pendiente mayor a 1 o menor a -1
            dxdy = dx / dy;
            for (y = y1, xx = x1; y <= y0; y++) {
                x = Math.trunc(xx);
                if (x >= 0 && x < img.getXSize() && y >= 0 && y < img.getYSize()) {
                    img.putPixelRgb(x, y, p);
                }
                xx += dxdy;
            }
        }
    }

    /**
    Draws a lines outline from a given polygon.
    */
    public static drawPolygon(img: Image, p: Polygon2D, color: RGBPixel): void {
        let va: Vertex2D;
        let vb: Vertex2D | null = null;
        let i: number;
        let j: number;

        for (i = 0; i < p.loops.length; i++) {
            const contour: _Polygon2DContour = p.loops[i]!;
            if (contour.vertices.length < 2) {
                continue;
            }
            for (j = 0; j < contour.vertices.length - 1; j++) {
                va = contour.vertices[j]!;
                vb = contour.vertices[j + 1]!;
                Rasterizer2D.drawLine(
                    img,
                    Math.trunc(va.x),
                    Math.trunc(va.y),
                    Math.trunc(vb.x),
                    Math.trunc(vb.y),
                    color,
                );
            }
            va = vb!;
            vb = contour.vertices[0]!;
            Rasterizer2D.drawLine(img, Math.trunc(va.x), Math.trunc(va.y), Math.trunc(vb.x), Math.trunc(vb.y), color);
        }
    }

    private static clamp(value: number, minValue: number, maxValue: number): number {
        if (value < minValue) {
            return minValue;
        }
        if (value > maxValue) {
            return maxValue;
        }
        return value;
    }

    private static addFillEdge(
        buckets: FillEdge[][],
        a: Vertex2D,
        b: Vertex2D,
        imageHeight: number,
        yRange: number[],
        sortOrder: number,
    ): void {
        let dx = b.x - a.x;
        let dy = b.y - a.y;

        if (Math.abs(dx) < VSDK.EPSILON && Math.abs(dy) < VSDK.EPSILON) {
            return;
        }
        if (Math.abs(dy) < VSDK.EPSILON) {
            return;
        }

        let top = a;
        let bottom = b;
        if (a.y > b.y) {
            top = b;
            bottom = a;
            dx = -dx;
            dy = -dy;
        }

        const inverseSlope = dx / dy;
        const yMin = Math.trunc(Math.ceil(top.y));
        const yMaxExclusive = Math.trunc(Math.ceil(bottom.y));

        if (yMin >= yMaxExclusive) {
            return;
        }

        const clippedYMin = Rasterizer2D.clamp(yMin, 0, imageHeight);
        const clippedYMaxExclusive = Rasterizer2D.clamp(yMaxExclusive, 0, imageHeight);

        if (clippedYMin >= clippedYMaxExclusive) {
            return;
        }

        const edge = new FillEdge();
        edge.yMin = clippedYMin;
        edge.yMaxExclusive = clippedYMaxExclusive;
        edge.inverseSlope = inverseSlope;
        edge.xAtCurrentY = top.x + (clippedYMin - top.y) * inverseSlope;
        edge.sortOrder = sortOrder;

        buckets[clippedYMin]!.push(edge);
        yRange[0] = Math.min(yRange[0]!, clippedYMin);
        yRange[1] = Math.max(yRange[1]!, clippedYMaxExclusive);
    }

    private static rasterizePolygonSpans(img: Image, polygon: Polygon2D, shader: SpanShader): void {
        const imageWidth = img.getXSize();
        const imageHeight = img.getYSize();

        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        const buckets: FillEdge[][] = [];
        let y: number;
        for (y = 0; y < imageHeight; y++) {
            buckets.push([]);
        }

        const yRange: number[] = [imageHeight, 0];
        let sortOrder = 0;
        let i: number;
        let j: number;

        for (i = 0; i < polygon.loops.length; i++) {
            const contour: _Polygon2DContour = polygon.loops[i]!;
            const vertexCount = contour.vertices.length;
            if (vertexCount < 2) {
                continue;
            }

            for (j = 0; j < vertexCount; j++) {
                const a: Vertex2D = contour.vertices[j]!;
                const b: Vertex2D = contour.vertices[(j + 1) % vertexCount]!;
                Rasterizer2D.addFillEdge(buckets, a, b, imageHeight, yRange, sortOrder);
                sortOrder++;
            }
        }

        if (yRange[0]! >= yRange[1]!) {
            return;
        }

        const activeEdges: FillEdge[] = [];

        for (y = yRange[0]!; y < yRange[1]!; y++) {
            const bucket: FillEdge[] = buckets[y]!;
            if (bucket.length !== 0) {
                activeEdges.push(...bucket);
            }

            for (i = activeEdges.length - 1; i >= 0; i--) {
                if (y >= activeEdges[i]!.yMaxExclusive) {
                    activeEdges.splice(i, 1);
                }
            }

            if (activeEdges.length < 2) {
                for (i = 0; i < activeEdges.length; i++) {
                    activeEdges[i]!.xAtCurrentY += activeEdges[i]!.inverseSlope;
                }
                continue;
            }

            Collections.sort(activeEdges, Rasterizer2D.ACTIVE_EDGE_COMPARATOR);

            for (i = 0; i + 1 < activeEdges.length; i += 2) {
                let xLeft = activeEdges[i]!.xAtCurrentY;
                let xRight = activeEdges[i + 1]!.xAtCurrentY;

                if (xLeft > xRight) {
                    const tmp = xLeft;
                    xLeft = xRight;
                    xRight = tmp;
                }

                let xStart = Math.trunc(Math.ceil(xLeft));
                let xEndExclusive = Math.trunc(Math.ceil(xRight));

                if (xEndExclusive <= 0 || xStart >= imageWidth) {
                    continue;
                }

                xStart = Rasterizer2D.clamp(xStart, 0, imageWidth);
                xEndExclusive = Rasterizer2D.clamp(xEndExclusive, 0, imageWidth);

                if (xStart < xEndExclusive) {
                    shader.shade(img, polygon, y, xStart, xEndExclusive);
                }
            }

            for (i = 0; i < activeEdges.length; i++) {
                activeEdges[i]!.xAtCurrentY += activeEdges[i]!.inverseSlope;
            }
        }
    }

    /**
    Current polygon filling rasterizer (scan-line) algorithm is a NAIVE
    implementation of the general macro-algorithm outlined at [FOLE1992].3.6.

    This implementation is working but inefficient, due to the fact that
    neither scanline coherence nor edge coherence is taken into account. This
    means that for each scanline, all polygon edges are intersected
    analitically. This is the "brute-force technique" recommended to be
    avoided in [FOLE1992].3.6.3, but it is provided as reference to
    compare efficiency with other (clever) aproaches that makes use of
    mid-point algoritms for intersection finding, or "active edge tables"
    (AETs) for efficient edge-coherence based traversals.
    */
    public static fillPolygon(img: Image, p: Polygon2D, color: RGBPixel): void {
        Rasterizer2D.rasterizePolygonSpans(img, p, {
            shade(image: Image, polygon: Polygon2D, y: number, xStart: number, xEndExclusive: number): void {
                let x: number;
                for (x = xStart; x < xEndExclusive; x++) {
                    image.putPixelRgb(x, y, color);
                }
            },
        });
    }

    /**
    Given the current polygon `p` and the coordinate of a pixel (x, y),
    this method gives the interpolated color `outColor`.
    */
    public static fillSmoothPolygonCalculateColor(p: Polygon2D, x: number, y: number, outPixel: RGBPixel): void {
        let va: Vertex2D;
        let i: number;
        let j: number;
        let distance: number;
        let totaldistance = 0.0;

        let outR = 0.0;
        let outG = 0.0;
        let outB = 0.0;

        for (i = 0; i < p.loops.length; i++) {
            for (j = 0; j < p.loops[i]!.vertices.length; j++) {
                va = p.loops[i]!.vertices[j]!;
                distance = 1.0 / (1.0 + Math.sqrt((va.x - x) * (va.x - x) + (va.y - y) * (va.y - y)));
                totaldistance += distance;
                outR += va.color.r() * distance;
                outG += va.color.g() * distance;
                outB += va.color.b() * distance;
            }
        }

        const normalizedR = outR / totaldistance;
        const normalizedG = outG / totaldistance;
        const normalizedB = outB / totaldistance;
        const clippedR = Math.max(0.0, Math.min(1.0, normalizedR));
        const clippedG = Math.max(0.0, Math.min(1.0, normalizedG));
        const clippedB = Math.max(0.0, Math.min(1.0, normalizedB));
        const rr = Math.trunc(clippedR * 255.0);
        const gg = Math.trunc(clippedG * 255.0);
        const bb = Math.trunc(clippedB * 255.0);

        outPixel.r = VSDK.unsigned8BitInteger2signedByte(rr);
        outPixel.g = VSDK.unsigned8BitInteger2signedByte(gg);
        outPixel.b = VSDK.unsigned8BitInteger2signedByte(bb);
    }

    /**
    Current polygon filling rasterizer (scan-line) algorithm is a NAIVE
    implementation of the general macro-algorithm outlined at [FOLE1992].3.6.

    This implementation is working but inefficient, due to the fact that
    neither scanline coherence nor edge coherence is taken into account. This
    means that for each scanline, all polygon edges are intersected
    analitically. This is the "brute-force technique" recommended to be
    avoided in [FOLE1992].3.6.3, but it is provided as reference to
    compare efficiency with other (clever) aproaches that makes use of
    mid-point algoritms for intersection finding, or "active edge tables"
    (AETs) for efficient edge-coherence based traversals.
    */
    public static fillSmoothPolygon(img: Image, p: Polygon2D): void {
        Rasterizer2D.rasterizePolygonSpans(img, p, {
            shade(image: Image, polygon: Polygon2D, y: number, xStart: number, xEndExclusive: number): void {
                const color = new RGBPixel();
                let x: number;
                for (x = xStart; x < xEndExclusive; x++) {
                    Rasterizer2D.fillSmoothPolygonCalculateColor(polygon, x, y, color);
                    image.putPixelRgb(x, y, color);
                }
            },
        });
    }
}
