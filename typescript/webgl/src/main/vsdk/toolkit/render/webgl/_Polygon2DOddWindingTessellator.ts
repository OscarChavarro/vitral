import { Polygon2D } from "@vitral/base";

interface TessellationEdge {
    x1: number;
    y1: number;
    x2: number;
    y2: number;
}

/**
Browser replacement for the GLU tessellator that
`vsdk.toolkit.render.jogl.Jogl4Polygon2DRenderer` drives.

Java fills a `Polygon2D` surface by handing every contour to
`GLU.gluNewTess()` and collecting the triangles the callbacks emit. GLU is a
companion library of desktop OpenGL; WebGL has neither it nor any entry point
that would let a page reach one, and the whole of it -- a sweep over an
arbitrary set of possibly self-intersecting contours, five winding rules, the
`GLU_TESS_COMBINE` vertex synthesis -- is far more than the one rule this
renderer ever asks for. `Jogl4Polygon2DRenderer` sets no winding rule, so the
tessellator runs under its default, `GLU_TESS_WINDING_ODD`, and it consumes
nothing but the triangles: no vertex identity survives the callback, because
the collector copies the three coordinates out and throws the reference away.

So what has to be reproduced is exactly this: the region covered an odd number
of times by the contours, expressed as triangles. That is what this class
computes, and it computes it exactly rather than approximately:

  1. every contour edge is collected, and every pair of edges that properly
     cross is split at the crossing, so that afterwards no two edges meet
     anywhere but at an endpoint;
  2. the distinct endpoint ordinates become the horizontal bands of a sweep;
  3. inside a band the edges crossing it keep a fixed left-to-right order,
     since they no longer cross, so sorting them once by their abscissa at the
     middle of the band orders them at both of its boundaries; and
  4. the odd rule is then the pairing of those crossings two by two -- first
     with second, third with fourth -- each pair spanning one trapezoid of
     covered area, emitted as two triangles.

The triangles are therefore not the triangles GLU would have produced -- a
sweep that cuts on every ordinate makes more, and thinner, ones -- but the
filled region is the same region, down to the arithmetic. Nothing downstream
can tell the two apart: this is the surface pass, drawn flat in a single
colour, and the wires and the points of the mode that shows the triangulation
come from `JoglTriangularRenderer`'s monotone decomposition instead, which is
ported as it stands.

The output is the layout Java's collector produces: three floats a vertex,
`(x, 0, y)`, the polygon lying on the XZ plane.
*/
export class _Polygon2DOddWindingTessellator {
    /**
    Two ordinates closer together than this are one band boundary, and an
    intersection parameter within this of an edge end is that end. It is the
    tolerance `_Polygon2DWA` already uses when it decides that two clipping
    vertices are the same point.
    */
    private static readonly EPSILON = 0.0001;

    private constructor() {}

    public static tessellatePolygonToTriangles(polygon: Polygon2D | null): number[] {
        const out: number[] = [];
        if (polygon === null || polygon.loops.length === 0) {
            return out;
        }

        const edges: TessellationEdge[] = _Polygon2DOddWindingTessellator.collectEdges(polygon);
        if (edges.length === 0) {
            return out;
        }

        const split: TessellationEdge[] = _Polygon2DOddWindingTessellator.splitAtCrossings(edges);
        const bands: number[] = _Polygon2DOddWindingTessellator.collectBandBoundaries(split);

        for (let i = 0; i + 1 < bands.length; i++) {
            _Polygon2DOddWindingTessellator.emitBand(out, split, bands[i]!, bands[i + 1]!);
        }
        return out;
    }

    /**
    Java skips a contour of fewer than three vertices before opening a GLU
    contour for it; the same contours are skipped here.
    */
    private static collectEdges(polygon: Polygon2D): TessellationEdge[] {
        const edges: TessellationEdge[] = [];

        for (const contour of polygon.loops) {
            const vertices = contour.vertices;
            if (vertices.length < 3) {
                continue;
            }
            for (let j = 0; j < vertices.length; j++) {
                const current = vertices[j]!;
                const next = vertices[(j + 1) % vertices.length]!;
                if (Math.abs(current.y - next.y) < _Polygon2DOddWindingTessellator.EPSILON) {
                    // A horizontal edge crosses no band interior, so it can
                    // take no part in the parity count.
                    continue;
                }
                edges.push({ x1: current.x, y1: current.y, x2: next.x, y2: next.y });
            }
        }
        return edges;
    }

    private static splitAtCrossings(edges: TessellationEdge[]): TessellationEdge[] {
        const parameters: number[][] = edges.map(() => []);

        for (let i = 0; i < edges.length; i++) {
            for (let j = i + 1; j < edges.length; j++) {
                const crossing = _Polygon2DOddWindingTessellator.properCrossing(edges[i]!, edges[j]!);
                if (crossing === null) {
                    continue;
                }
                parameters[i]!.push(crossing[0]!);
                parameters[j]!.push(crossing[1]!);
            }
        }

        const split: TessellationEdge[] = [];
        for (let i = 0; i < edges.length; i++) {
            const edge = edges[i]!;
            const cuts: number[] = parameters[i]!;
            if (cuts.length === 0) {
                split.push(edge);
                continue;
            }
            cuts.sort((a, b) => a - b);
            let previous = 0.0;
            for (const cut of cuts) {
                _Polygon2DOddWindingTessellator.pushPiece(split, edge, previous, cut);
                previous = cut;
            }
            _Polygon2DOddWindingTessellator.pushPiece(split, edge, previous, 1.0);
        }
        return split;
    }

    private static pushPiece(split: TessellationEdge[], edge: TessellationEdge, from: number, to: number): void {
        if (to - from <= 0.0) {
            return;
        }
        const x1: number = edge.x1 + (edge.x2 - edge.x1) * from;
        const y1: number = edge.y1 + (edge.y2 - edge.y1) * from;
        const x2: number = edge.x1 + (edge.x2 - edge.x1) * to;
        const y2: number = edge.y1 + (edge.y2 - edge.y1) * to;
        if (Math.abs(y2 - y1) < _Polygon2DOddWindingTessellator.EPSILON) {
            return;
        }
        split.push({ x1, y1, x2, y2 });
    }

    /**
    The crossing parameters of two segments, or null when they do not cross
    strictly inside both. A crossing at an endpoint needs no split: the
    ordinate of that endpoint is already a band boundary.
    */
    private static properCrossing(a: TessellationEdge, b: TessellationEdge): [number, number] | null {
        const ax: number = a.x2 - a.x1;
        const ay: number = a.y2 - a.y1;
        const bx: number = b.x2 - b.x1;
        const by: number = b.y2 - b.y1;

        const denominator: number = ax * by - ay * bx;
        if (Math.abs(denominator) < 1e-12) {
            return null;
        }

        const dx: number = b.x1 - a.x1;
        const dy: number = b.y1 - a.y1;
        const t: number = (dx * by - dy * bx) / denominator;
        const s: number = (dx * ay - dy * ax) / denominator;

        const epsilon = _Polygon2DOddWindingTessellator.EPSILON;
        if (t <= epsilon || t >= 1.0 - epsilon || s <= epsilon || s >= 1.0 - epsilon) {
            return null;
        }
        return [t, s];
    }

    private static collectBandBoundaries(edges: TessellationEdge[]): number[] {
        const ordinates: number[] = [];
        for (const edge of edges) {
            ordinates.push(edge.y1);
            ordinates.push(edge.y2);
        }
        ordinates.sort((a, b) => a - b);

        const bands: number[] = [];
        for (const ordinate of ordinates) {
            if (bands.length === 0 || ordinate - bands[bands.length - 1]! > _Polygon2DOddWindingTessellator.EPSILON) {
                bands.push(ordinate);
            }
        }
        return bands;
    }

    private static emitBand(out: number[], edges: TessellationEdge[], low: number, high: number): void {
        const middle: number = (low + high) / 2.0;
        const crossings: { middle: number; low: number; high: number }[] = [];

        for (const edge of edges) {
            const minimum: number = Math.min(edge.y1, edge.y2);
            const maximum: number = Math.max(edge.y1, edge.y2);
            if (minimum > low + _Polygon2DOddWindingTessellator.EPSILON) {
                continue;
            }
            if (maximum < high - _Polygon2DOddWindingTessellator.EPSILON) {
                continue;
            }
            crossings.push({
                middle: _Polygon2DOddWindingTessellator.abscissaAt(edge, middle),
                low: _Polygon2DOddWindingTessellator.abscissaAt(edge, low),
                high: _Polygon2DOddWindingTessellator.abscissaAt(edge, high),
            });
        }

        if (crossings.length < 2) {
            return;
        }
        crossings.sort((a, b) => a.middle - b.middle);

        for (let i = 0; i + 1 < crossings.length; i += 2) {
            const left = crossings[i]!;
            const right = crossings[i + 1]!;
            _Polygon2DOddWindingTessellator.emitTriangle(out, left.low, low, right.low, low, right.high, high);
            _Polygon2DOddWindingTessellator.emitTriangle(out, left.low, low, right.high, high, left.high, high);
        }
    }

    private static abscissaAt(edge: TessellationEdge, y: number): number {
        const t: number = (y - edge.y1) / (edge.y2 - edge.y1);
        return edge.x1 + (edge.x2 - edge.x1) * t;
    }

    private static emitTriangle(
        out: number[],
        ax: number,
        ay: number,
        bx: number,
        by: number,
        cx: number,
        cy: number,
    ): void {
        out.push(ax, 0.0, ay);
        out.push(bx, 0.0, by);
        out.push(cx, 0.0, cy);
    }
}
