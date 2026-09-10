// References: [WEATH1977] Kevin Weiler and Peter Atherton, "Hidden Surface
// Removal Using Polygon Area Sorting", Cornell University, 1977.
import { Polygon2D } from "../../surface/polygon/Polygon2D.js";
import { _CircularDoubleLinkedList } from "./_CircularDoubleLinkedList.js";
import { _DoubleLinkedListNode } from "./_DoubleLinkedListNode.js";
import { _Polygon2DContourWA } from "./_Polygon2DContourWA.js";
import { _Polygon2DWA } from "./_Polygon2DWA.js";
import { _VertexNode2D } from "./_VertexNode2D.js";
import { PolygonTopologicalMerger } from "./PolygonTopologicalMerger.js";

/** Weiler-Atherton clipping for convex/non-convex polygons and holes. */
export class WeilerAthertonPolygonClipper {
    private clipPolyWA: _Polygon2DWA | null = null;
    private subjectPolyWA: _Polygon2DWA | null = null;
    // Per-edge traversal state used while building the intersection graph.
    private firstIntersection = false;
    private previousOut = false;
    private readonly coincidentPoints = [false, false, false, false];
    private readonly topologicalMerger = new PolygonTopologicalMerger();

    public unionPolygons(a: Polygon2D | null, b: Polygon2D | null, out: Polygon2D | null): void {
        if (a === null || b === null || out === null) return;
        const aMinusB = new Polygon2D();
        const bMinusA = new Polygon2D();
        const intersectionAB = new Polygon2D();
        const temp = new Polygon2D();
        this.clipPolygons(b, a, intersectionAB, aMinusB);
        this.clipPolygons(a, b, temp, bMinusA);
        WeilerAthertonPolygonClipper.resetOutputPolygon(out);
        WeilerAthertonPolygonClipper.appendNonEmptyContours(aMinusB, out);
        WeilerAthertonPolygonClipper.appendNonEmptyContours(bMinusA, out);
        WeilerAthertonPolygonClipper.appendNonEmptyContours(intersectionAB, out);
        this.topologicalMerger.mergeInPlace(out);
    }

    public clipPolygons(clip: Polygon2D, subject: Polygon2D, inner: Polygon2D | null, outer: Polygon2D | null): void {
        if (inner === null || outer === null) return;
        const exits: _DoubleLinkedListNode<_VertexNode2D>[] = [];
        const entries: _DoubleLinkedListNode<_VertexNode2D>[] = [];
        let emptyInner = true;
        let emptyOuter = true;
        this.clipPolyWA = new _Polygon2DWA(clip, true);
        this.subjectPolyWA = new _Polygon2DWA(subject, true);

        // Find all intersections and populate the entry/exit lists.
        for (const clipContour of this.clipPolyWA.loops) {
            clipContour.isClipped = false;
            let clipNode = clipContour.vertices.getHead();
            if (clipContour.vertices.size() > 1 && clipNode !== null) {
                do {
                    const clipPrevious = clipNode.previous;
                    this.previousOut = false;
                    for (const subjectContour of this.subjectPolyWA.loops) {
                        let subjectNode = subjectContour.vertices.getHead();
                        if (subjectContour.vertices.size() > 1 && subjectNode !== null) {
                            this.firstIntersection = true;
                            do {
                                const subjectPrevious = subjectNode.previous;
                                const intersection = new _VertexNode2D();
                                if (
                                    this.intersecLineLine2D(
                                        clipPrevious.data,
                                        clipNode.data,
                                        subjectPrevious.data,
                                        subjectNode.data,
                                        intersection,
                                        this.coincidentPoints,
                                    )
                                ) {
                                    this.makeCut(
                                        clipContour,
                                        subjectContour,
                                        clipPrevious,
                                        clipNode,
                                        subjectPrevious,
                                        subjectNode,
                                        intersection,
                                        exits,
                                        entries,
                                    );
                                }
                                subjectNode = subjectNode.next;
                            } while (subjectNode !== subjectContour.vertices.getHead());
                        }
                    }
                    clipNode = clipNode.next;
                } while (clipNode !== clipContour.vertices.getHead());
            }
        }

        // Inner polygons.
        for (const start of exits) {
            if ((start.data.flags & 0x01) === 0) {
                if (emptyInner) emptyInner = false;
                else inner.nextLoop();
                let iterator = start;
                do {
                    iterator.data.flags |= 0x01;
                    this.addNode(inner, iterator);
                    iterator = iterator.next;
                    iterator.data.flags |= 0x01;
                    if (iterator.data.pairNode !== null) iterator = iterator.data.pairNode;
                } while (iterator !== start && iterator.data.pairNode !== start);
            }
        }
        // Outer polygons.
        for (const start of entries) {
            if ((start.data.flags & 0x02) === 0) {
                if (emptyOuter) emptyOuter = false;
                else outer.nextLoop();
                let iterator = start;
                let isSubject = true;
                do {
                    iterator.data.flags |= 0x02;
                    this.addNode(outer, iterator);
                    iterator = isSubject ? iterator.next : iterator.previous;
                    iterator.data.flags |= 0x02;
                    if (iterator.data.pairNode !== null) {
                        iterator = iterator.data.pairNode;
                        isSubject = !isSubject;
                    }
                } while (iterator !== start && iterator.data.pairNode !== start);
            }
        }

        this.classifyHolesAndContours(this.clipPolyWA);
        this.classifyHolesAndContours(this.subjectPolyWA);
        // Non-intersecting clip contours entirely inside the subject.
        for (const clipContour of this.clipPolyWA.loops) {
            if (clipContour.isClipped) continue;
            const head = clipContour.vertices.getHead();
            if (head === null) continue;
            let inside = false;
            for (const subjectContour of this.subjectPolyWA.loops) {
                let node: _DoubleLinkedListNode<_VertexNode2D> = head;
                let pointInPolygon = false;
                do {
                    if (this.isPointInPolygon2D(node.data, subjectContour.vertices) === 1) {
                        pointInPolygon = true;
                        break;
                    }
                    node = node.next;
                } while (node !== head);
                if (pointInPolygon) inside = !inside;
            }
            if (inside) {
                if (emptyInner) emptyInner = false;
                else inner.nextLoop();
                if (emptyOuter) emptyOuter = false;
                else outer.nextLoop();
                let node: _DoubleLinkedListNode<_VertexNode2D> = head;
                do {
                    this.addNode(inner, node);
                    node = node.next;
                } while (node !== head);
                node = head;
                do {
                    this.addNode(outer, node);
                    node = node.previous;
                } while (node !== head);
            }
        }
        // Non-intersecting subject contours.
        for (const subjectContour of this.subjectPolyWA.loops) {
            if (subjectContour.isClipped) continue;
            const head = subjectContour.vertices.getHead();
            if (head === null) continue;
            let inside = false;
            for (const clipContour of this.clipPolyWA.loops) {
                let node: _DoubleLinkedListNode<_VertexNode2D> = head;
                let pointInPolygon = false;
                do {
                    if (this.isPointInPolygon2D(node.data, clipContour.vertices) === 1) {
                        pointInPolygon = true;
                        break;
                    }
                    node = node.next;
                } while (node !== head);
                if (pointInPolygon) inside = !inside;
            }
            if (inside) {
                if (emptyInner) emptyInner = false;
                else inner.nextLoop();
            } else {
                if (emptyOuter) emptyOuter = false;
                else outer.nextLoop();
            }
            let node: _DoubleLinkedListNode<_VertexNode2D> = head;
            do {
                this.addNode(inside ? inner : outer, node);
                node = node.next;
            } while (node !== head);
        }
    }

    private addNode(out: Polygon2D, node: _DoubleLinkedListNode<_VertexNode2D>): void {
        const v = node.data;
        out.addVertex(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
    }

    private makeCut(
        clipContour: _Polygon2DContourWA,
        subjectContour: _Polygon2DContourWA,
        clipPrevious: _DoubleLinkedListNode<_VertexNode2D>,
        clipNode: _DoubleLinkedListNode<_VertexNode2D>,
        subjectPrevious: _DoubleLinkedListNode<_VertexNode2D>,
        subjectNode: _DoubleLinkedListNode<_VertexNode2D>,
        intersection: _VertexNode2D,
        exits: _DoubleLinkedListNode<_VertexNode2D>[],
        entries: _DoubleLinkedListNode<_VertexNode2D>[],
    ): void {
        let firstCutOut = false;
        if (!this.coincidentPoints[0] && !this.coincidentPoints[2]) {
            if (this.firstIntersection) {
                firstCutOut =
                    this.crossProductSegments(
                        subjectPrevious.data,
                        subjectNode.data,
                        clipPrevious.data,
                        clipNode.data,
                    ) < 0;
            }
            this.updatePolygonsAndListsWithCuts(
                clipContour,
                subjectContour,
                clipPrevious,
                clipNode,
                subjectPrevious,
                subjectNode,
                intersection,
                firstCutOut,
                exits,
                entries,
                true,
            );
            return;
        }
        const dot = this.dotProductNorm2D(clipPrevious.data, clipNode.data, subjectPrevious.data, subjectNode.data);
        if (dot >= 0.9999 && dot <= 1.0001) {
            const parallel = new _VertexNode2D(
                clipNode.data.x - clipPrevious.data.x,
                clipNode.data.y - clipPrevious.data.y,
            );
            const negative = new _VertexNode2D(-parallel.x, -parallel.y);
            const away1C = this.coincidentPoints[0]
                ? new _VertexNode2D(
                      clipPrevious.previous.data.x - clipPrevious.data.x,
                      clipPrevious.previous.data.y - clipPrevious.data.y,
                  )
                : new _VertexNode2D(negative.x, negative.y);
            const away1S = this.coincidentPoints[2]
                ? new _VertexNode2D(
                      subjectPrevious.previous.data.x - subjectPrevious.data.x,
                      subjectPrevious.previous.data.y - subjectPrevious.data.y,
                  )
                : new _VertexNode2D(negative.x, negative.y);
            const away2C = this.coincidentPoints[1]
                ? new _VertexNode2D(clipNode.next.data.x - clipNode.data.x, clipNode.next.data.y - clipNode.data.y)
                : new _VertexNode2D(parallel.x, parallel.y);
            const away2S = this.coincidentPoints[3]
                ? new _VertexNode2D(
                      subjectNode.next.data.x - subjectNode.data.x,
                      subjectNode.next.data.y - subjectNode.data.y,
                  )
                : new _VertexNode2D(parallel.x, parallel.y);
            if (this.are3VectorsOrderedCounterclockwise2D(parallel, away1C, away1S) === 1) {
                this.updatePolygonsAndListsWithCuts(
                    clipContour,
                    subjectContour,
                    clipPrevious,
                    clipNode,
                    subjectPrevious,
                    subjectNode,
                    intersection,
                    true,
                    exits,
                    entries,
                    true,
                );
            }
            if (this.are3VectorsOrderedCounterclockwise2D(negative, away2S, away2C) === 1) {
                this.updatePolygonsAndListsWithCuts(
                    clipContour,
                    subjectContour,
                    clipPrevious,
                    clipNode,
                    subjectPrevious,
                    subjectNode,
                    intersection,
                    false,
                    exits,
                    entries,
                    false,
                );
            }
        } else if (Math.abs(dot) < 0.9999) {
            const ac = new _VertexNode2D(clipNode.data.x - clipPrevious.data.x, clipNode.data.y - clipPrevious.data.y);
            const bc = this.coincidentPoints[0]
                ? new _VertexNode2D(
                      clipPrevious.previous.data.x - clipPrevious.data.x,
                      clipPrevious.previous.data.y - clipPrevious.data.y,
                  )
                : new _VertexNode2D(-ac.x, -ac.y);
            const as = new _VertexNode2D(
                subjectNode.data.x - subjectPrevious.data.x,
                subjectNode.data.y - subjectPrevious.data.y,
            );
            const bs = this.coincidentPoints[2]
                ? new _VertexNode2D(
                      subjectPrevious.previous.data.x - subjectPrevious.data.x,
                      subjectPrevious.previous.data.y - subjectPrevious.data.y,
                  )
                : new _VertexNode2D(-as.x, -as.y);
            const orderAC = this.are3VectorsOrderedCounterclockwise2D(as, ac, bs);
            const orderBC = this.are3VectorsOrderedCounterclockwise2D(as, bc, bs);
            if (orderAC !== 0 && orderBC !== 0 && (orderAC === 1) !== (orderBC === 1)) {
                firstCutOut = orderAC === -1;
                this.updatePolygonsAndListsWithCuts(
                    clipContour,
                    subjectContour,
                    clipPrevious,
                    clipNode,
                    subjectPrevious,
                    subjectNode,
                    intersection,
                    firstCutOut,
                    exits,
                    entries,
                    true,
                );
            }
        }
    }

    private updatePolygonsAndListsWithCuts(
        clipContour: _Polygon2DContourWA,
        subjectContour: _Polygon2DContourWA,
        clipPrevious: _DoubleLinkedListNode<_VertexNode2D>,
        clipNode: _DoubleLinkedListNode<_VertexNode2D>,
        subjectPrevious: _DoubleLinkedListNode<_VertexNode2D>,
        subjectNode: _DoubleLinkedListNode<_VertexNode2D>,
        intersectionS: _VertexNode2D,
        firstCutOut: boolean,
        exits: _DoubleLinkedListNode<_VertexNode2D>[],
        entries: _DoubleLinkedListNode<_VertexNode2D>[],
        firstPoints: boolean,
    ): void {
        let cutS: _DoubleLinkedListNode<_VertexNode2D> | null = null;
        if (!this.coincidentPoints[0] && !this.coincidentPoints[2]) {
            const intersectionC = new _VertexNode2D(intersectionS);
            intersectionS.pairNode = this.insertOrderedNodeBetweenTwoNodes(
                clipContour.vertices,
                clipPrevious,
                clipNode,
                intersectionC,
            );
            intersectionC.pairNode = subjectContour.vertices.insertBefore(intersectionS, subjectNode);
            cutS = intersectionC.pairNode;
        } else if (firstPoints) {
            if (this.coincidentPoints[0]) {
                if (this.coincidentPoints[2]) {
                    clipPrevious.data.pairNode = subjectPrevious;
                    subjectPrevious.data.pairNode = clipPrevious;
                    cutS = subjectPrevious;
                } else {
                    const intersection = new _VertexNode2D(clipPrevious.data);
                    clipPrevious.data.pairNode = subjectContour.vertices.insertBefore(intersection, subjectNode);
                    intersection.pairNode = clipPrevious;
                    cutS = clipPrevious.data.pairNode;
                }
            } else if (this.coincidentPoints[2]) {
                const intersection = new _VertexNode2D(subjectPrevious.data);
                subjectPrevious.data.pairNode = this.insertOrderedNodeBetweenTwoNodes(
                    clipContour.vertices,
                    clipPrevious,
                    clipNode,
                    intersection,
                );
                intersection.pairNode = subjectPrevious;
                cutS = subjectPrevious;
            }
        } else if (this.coincidentPoints[1]) {
            if (this.coincidentPoints[3]) {
                clipNode.data.pairNode = subjectNode;
                subjectNode.data.pairNode = clipNode;
                cutS = subjectNode;
            } else {
                const intersection = new _VertexNode2D(clipNode.data);
                clipNode.data.pairNode = subjectContour.vertices.insertBefore(intersection, subjectNode);
                intersection.pairNode = clipNode;
                cutS = clipNode.data.pairNode;
            }
        } else if (this.coincidentPoints[3]) {
            const intersection = new _VertexNode2D(subjectNode.data);
            subjectNode.data.pairNode = this.insertOrderedNodeBetweenTwoNodes(
                clipContour.vertices,
                clipPrevious,
                clipNode,
                intersection,
            );
            intersection.pairNode = subjectNode;
            cutS = subjectNode;
        }
        if (cutS === null) {
            // The Java decision tree reaches this point only after establishing
            // one of the endpoint configurations above; a null is the same
            // invalid internal state that would produce a NullPointerException.
            throw new TypeError("Cut classification did not establish a subject traversal node");
        }
        if (this.firstIntersection) {
            subjectContour.isClipped = true;
            clipContour.isClipped = true;
            if (firstCutOut) {
                exits.push(cutS);
                this.previousOut = true;
            } else {
                entries.push(cutS);
                this.previousOut = false;
            }
            this.firstIntersection = false;
        } else {
            if (this.previousOut) entries.push(cutS);
            else exits.push(cutS);
            this.previousOut = !this.previousOut;
        }
    }

    private classifyHolesAndContours(polygon: _Polygon2DWA): void {
        for (const test of polygon.loops) {
            for (const contour of polygon.loops) {
                if (contour === test) continue;
                const head = test.vertices.getHead();
                if (head === null) continue;
                let node = head;
                let inside = false;
                do {
                    if (this.isPointInPolygon2D(node.data, contour.vertices) === 1) {
                        inside = true;
                        break;
                    }
                    node = node.next;
                } while (node !== head);
                if (inside) test.isHole = !test.isHole;
            }
        }
    }

    private are3VectorsOrderedCounterclockwise2D(a: _VertexNode2D, b: _VertexNode2D, c: _VertexNode2D): number {
        let length = Math.hypot(a.x, a.y);
        a.x /= length;
        a.y /= length;
        length = Math.hypot(b.x, b.y);
        b.x /= length;
        b.y /= length;
        length = Math.hypot(c.x, c.y);
        c.x /= length;
        c.y /= length;
        let value = this.dotProduct2D(a, b);
        if (value > 0.9999 && value < 1.0001) return 0;
        value = this.dotProduct2D(b, c);
        if (value > 0.9999 && value < 1.0001) return 0;
        value = this.dotProduct2D(a, c);
        if (value > 0.9999 && value < 1.0001) return 0;
        if (this.crossProduct2D(a, c) < 0) {
            if (this.crossProduct2D(b, c) >= 0) return 1;
            if (this.crossProduct2D(a, b) > 0) return 1;
            return -1;
        }
        if (this.crossProduct2D(b, c) <= 0) return -1;
        if (this.crossProduct2D(b, a) > 0) return -1;
        return 1;
    }

    // Franklin point-in-polygon test with the Java boundary extension.
    private isPointInPolygon2D(point: _VertexNode2D, polygon: _CircularDoubleLinkedList<_VertexNode2D>): number {
        let node = polygon.getHead();
        if (node === null) return -1;
        let inside = false;
        do {
            let value = node.data.y - node.next.data.y;
            if (
                Math.abs(value) < 0.0001 &&
                Math.abs(point.y - (node.next.data.y + value / 2)) < 0.0001 &&
                (node.next.data.x - point.x) * (point.x - node.data.x) >= 0
            )
                return 0;
            if (point.y < node.data.y !== point.y < node.next.data.y) {
                value =
                    ((node.next.data.x - node.data.x) * (point.y - node.data.y)) / (node.next.data.y - node.data.y) +
                    node.data.x;
                if (Math.abs(point.x - value) < 0.0001) return 0;
                if (point.x < value) inside = !inside;
            }
            node = node.next;
        } while (node !== polygon.getHead());
        return inside ? 1 : -1;
    }

    private crossProductSegments(a1: _VertexNode2D, a2: _VertexNode2D, b1: _VertexNode2D, b2: _VertexNode2D): number {
        return (a2.x - a1.x) * (b2.y - b1.y) - (a2.y - a1.y) * (b2.x - b1.x);
    }
    private crossProduct2D(a: _VertexNode2D, b: _VertexNode2D): number {
        return a.x * b.y - a.y * b.x;
    }
    private dotProduct2D(a: _VertexNode2D, b: _VertexNode2D): number {
        return a.x * b.x + a.y * b.y;
    }
    private dotProductNorm2D(a1: _VertexNode2D, a2: _VertexNode2D, b1: _VertexNode2D, b2: _VertexNode2D): number {
        let x1 = a2.x - a1.x;
        let y1 = a2.y - a1.y;
        let x2 = b2.x - b1.x;
        let y2 = b2.y - b1.y;
        let length = Math.hypot(x1, y1);
        x1 /= length;
        y1 /= length;
        length = Math.hypot(x2, y2);
        x2 /= length;
        y2 /= length;
        return x1 * x2 + y1 * y2;
    }

    private insertOrderedNodeBetweenTwoNodes(
        list: _CircularDoubleLinkedList<_VertexNode2D>,
        node: _DoubleLinkedListNode<_VertexNode2D>,
        next: _DoubleLinkedListNode<_VertexNode2D>,
        intersection: _VertexNode2D,
    ): _DoubleLinkedListNode<_VertexNode2D> | null {
        let iterator = node;
        if (Math.abs(node.data.x - next.data.x) > Math.abs(node.data.y - next.data.y)) {
            const sign = (next.data.x - node.data.x) / Math.abs(next.data.x - node.data.x);
            while (iterator.data.x * sign < intersection.x * sign && iterator !== next) iterator = iterator.next;
        } else {
            const sign = (next.data.y - node.data.y) / Math.abs(next.data.y - node.data.y);
            while (iterator.data.y * sign < intersection.y * sign && iterator !== next) iterator = iterator.next;
        }
        return list.insertBefore(intersection, iterator);
    }

    /** Exact translation of Java's segment-intersection decision tree. */
    public intersecLineLine2D(
        p0: _VertexNode2D,
        p1: _VertexNode2D,
        p2: _VertexNode2D,
        p3: _VertexNode2D,
        out: _VertexNode2D,
        coincident: boolean[],
    ): boolean {
        coincident[0] = false;
        coincident[1] = false;
        coincident[2] = false;
        coincident[3] = false;
        let anyEndpoint = false;
        let p0p2 = false;
        let p0p3 = false;
        let p2p1 = false;
        if (Math.abs(p0.x - p2.x) < 0.0001 && Math.abs(p0.y - p2.y) < 0.0001) {
            coincident[0] = true;
            coincident[2] = true;
            p0p2 = true;
            anyEndpoint = true;
        }
        if (Math.abs(p0.x - p3.x) < 0.0001 && Math.abs(p0.y - p3.y) < 0.0001) {
            coincident[0] = true;
            coincident[3] = true;
            p0p3 = true;
            anyEndpoint = true;
        }
        if (Math.abs(p1.x - p2.x) < 0.0001 && Math.abs(p1.y - p2.y) < 0.0001) {
            coincident[1] = true;
            coincident[2] = true;
            p2p1 = true;
            anyEndpoint = true;
        }
        if (Math.abs(p1.x - p3.x) < 0.0001 && Math.abs(p1.y - p3.y) < 0.0001) {
            coincident[1] = true;
            coincident[3] = true;
            anyEndpoint = true;
        }
        const dx1 = p1.x - p0.x;
        const dx2 = p3.x - p2.x;
        const vertical1 = dx1 > -0.00001 && dx1 < 0.00001;
        const vertical2 = dx2 > -0.00001 && dx2 < 0.00001;
        if (vertical1 && vertical2) {
            if (Math.abs(p0.x - p2.x) < 0.0001) {
                if ((p3.y - p0.y) * (p0.y - p2.y) > 0) coincident[0] = true;
                if ((p1.y - p2.y) * (p2.y - p0.y) > 0) coincident[2] = true;
                if ((p3.y - p1.y) * (p1.y - p2.y) > 0) coincident[1] = true;
                if ((p1.y - p3.y) * (p3.y - p0.y) > 0) coincident[3] = true;
                if (anyEndpoint) return p0p2 || (coincident[0]! && !p0p3) || (coincident[2]! && !p2p1);
                return coincident[0]! || coincident[2]!;
            }
            return p0p2;
        }
        let m1 = 0,
            b1 = 0,
            m2 = 0,
            b2 = 0;
        if (!vertical1) {
            m1 = (p1.y - p0.y) / (p1.x - p0.x);
            b1 = p0.y - m1 * p0.x;
        }
        if (!vertical2) {
            m2 = (p3.y - p2.y) / (p3.x - p2.x);
            b2 = p2.y - m2 * p2.x;
        }
        if (vertical1) {
            if ((p1.y - p3.y) * (p3.y - p0.y) > 0 && Math.abs(p0.x - p3.x) < 0.0001) coincident[3] = true;
            if ((p1.y - p2.y) * (p2.y - p0.y) > 0 && Math.abs(p0.x - p2.x) < 0.0001) {
                coincident[2] = true;
                return anyEndpoint ? p0p2 : true;
            }
            out.x = p0.x;
            out.y = m2 * out.x + b2;
            if ((p3.x - out.x) * (out.x - p2.x) > 0) {
                if (Math.abs(p1.y - out.y) < 0.0001) coincident[1] = true;
                if (Math.abs(p0.y - out.y) < 0.0001) {
                    coincident[0] = true;
                    return anyEndpoint ? p0p2 : true;
                }
                if ((p1.y - out.y) * (out.y - p0.y) > 0) return anyEndpoint ? p0p2 : true;
            }
            return p0p2;
        }
        if (vertical2) {
            if ((p3.y - p1.y) * (p1.y - p2.y) > 0 && Math.abs(p2.x - p1.x) < 0.0001) coincident[1] = true;
            if ((p3.y - p0.y) * (p0.y - p2.y) > 0 && Math.abs(p2.x - p0.x) < 0.0001) {
                coincident[0] = true;
                return anyEndpoint ? p0p2 : true;
            }
            out.x = p2.x;
            out.y = m1 * out.x + b1;
            if ((p1.x - out.x) * (out.x - p0.x) > 0) {
                if (Math.abs(p3.y - out.y) < 0.0001) coincident[3] = true;
                if (Math.abs(p2.y - out.y) < 0.0001) {
                    coincident[2] = true;
                    return anyEndpoint ? p0p2 : true;
                }
                if ((p3.y - out.y) * (out.y - p2.y) > 0) return anyEndpoint ? p0p2 : true;
            }
            return p0p2;
        }
        if (Math.abs(m2 - m1) < 0.00001) {
            if (Math.abs(b1 - b2) < 0.0001) {
                if ((p3.x - p0.x) * (p0.x - p2.x) >= 0) coincident[0] = true;
                if ((p3.x - p1.x) * (p1.x - p2.x) >= 0) coincident[1] = true;
                if ((p1.x - p2.x) * (p2.x - p0.x) >= 0) coincident[2] = true;
                if ((p1.x - p3.x) * (p3.x - p0.x) >= 0) coincident[3] = true;
                if (anyEndpoint) return p0p2 || (coincident[0]! && !p0p3) || (coincident[2]! && !p2p1);
                return coincident[0]! || coincident[2]!;
            }
            return p0p2;
        }
        let intersects1 = false;
        let intersects2 = false;
        if (m1 < 1) {
            out.x = (b1 - b2) / (m2 - m1);
            out.y = m1 * out.x + b1;
            if ((p1.x - out.x) * (out.x - p0.x) >= 0) intersects1 = true;
        } else {
            out.y = (b1 * m2 - b2 * m1) / (m2 - m1);
            out.x = (out.y - b1) / m1;
            if ((p1.y - out.y) * (out.y - p0.y) >= 0) intersects1 = true;
        }
        if (m2 < 1) {
            if ((p3.x - out.x) * (out.x - p2.x) >= 0) intersects2 = true;
        } else if ((p3.y - out.y) * (out.y - p2.y) >= 0) intersects2 = true;
        if (intersects1 && intersects2) {
            if (Math.abs(p0.x - out.x) < 0.0001 && Math.abs(p0.y - out.y) < 0.0001) {
                coincident[0] = true;
                return anyEndpoint ? p0p2 : true;
            }
            if (Math.abs(p1.x - out.x) < 0.0001 && Math.abs(p1.y - out.y) < 0.0001) {
                coincident[1] = true;
                return p0p2;
            }
            if (Math.abs(p2.x - out.x) < 0.0001 && Math.abs(p2.y - out.y) < 0.0001) {
                coincident[2] = true;
                return anyEndpoint ? p0p2 : true;
            }
            if (Math.abs(p3.x - out.x) < 0.0001 && Math.abs(p3.y - out.y) < 0.0001) {
                coincident[3] = true;
                return p0p2;
            }
            return anyEndpoint ? p0p2 : true;
        }
        return p0p2;
    }

    public getClipPolyWA(): _Polygon2DWA | null {
        return this.clipPolyWA;
    }
    public getSubjectPolyWA(): _Polygon2DWA | null {
        return this.subjectPolyWA;
    }

    private static resetOutputPolygon(polygon: Polygon2D): void {
        polygon.loops.length = 0;
        polygon.nextLoop();
    }
    private static appendNonEmptyContours(source: Polygon2D, target: Polygon2D): void {
        let hasOutput = WeilerAthertonPolygonClipper.hasAnyVertex(target);
        for (const contour of source.loops) {
            if (contour.vertices.length === 0) continue;
            if (hasOutput) target.nextLoop();
            for (const v of contour.vertices) target.addVertex(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
            hasOutput = true;
        }
    }
    private static hasAnyVertex(polygon: Polygon2D): boolean {
        return polygon.loops.some((contour) => contour.vertices.length !== 0);
    }
}
