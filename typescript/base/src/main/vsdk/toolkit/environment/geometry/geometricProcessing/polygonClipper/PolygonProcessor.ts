import { BinaryTreeNode } from "../../../../common/dataStructures/BinaryTreeNode.js";
import { ProcessingElement } from "../../../../processing/ProcessingElement.js";
import { Vertex2D } from "../../element/Vertex2D.js";
import { Polygon2D } from "../../surface/polygon/Polygon2D.js";
import { _Polygon2DContour } from "../../surface/polygon/_Polygon2DContour.js";

/** Non-recursive Ramer--Douglas--Peucker and polygon-contour utilities. */
export class PolygonProcessor extends ProcessingElement {
    private static readonly EPSILON = 1e-18;

    public static polygon2DSimplify(pol2DIn: Polygon2D, epsilon: number, copy: boolean): Polygon2D {
        const simplified = new Polygon2D();
        simplified.loops.length = 0;
        // Java's public method always passes true to its private helper. Keep
        // that historical behavior, even though its API documents `copy`.
        void copy;
        for (const contour of pol2DIn.loops)
            simplified.loops.push(this.polygon2DContourSimplify(contour, epsilon, true));
        return simplified;
    }

    public static classifyContourHoles(polygon: Polygon2D): void {
        for (const contour of polygon.loops) contour.fleetingFlag = false;
        const head = new BinaryTreeNode<_Polygon2DContour>(null as unknown as _Polygon2DContour);
        polygon.setHeadNode(head);
        for (let i = 0; i < polygon.loops.length; i++) {
            const contour = polygon.loops[i]!;
            const contained = [contour];
            for (let j = i + 1; j < polygon.loops.length; j++) {
                const candidate = polygon.loops[j]!;
                if (!candidate.fleetingFlag && this.contourInsidePolygon(contour.vertices, candidate.vertices))
                    contained.push(candidate);
            }
            contained.sort((a, b) => b.compareTo(a));
            this.insertListInBinaryTree(contained, head);
        }
    }

    /** Returns true when the contour lies inside the supplied polygon contour. */
    public static contourInsidePolygon(contour: readonly Vertex2D[], mainPolygon: readonly Vertex2D[]): boolean {
        for (const point of contour) {
            const result = this.isPointInsidePolygon2D(point, mainPolygon);
            if (result === 1) return true;
            if (result === -1) return false;
        }
        return false;
    }

    /** Returns 1 inside, -1 outside, and 0 on the boundary. */
    public static isPointInsidePolygon2D(point: Vertex2D, polygon: readonly Vertex2D[]): number {
        if (polygon.length < 3) return -1;
        let isInside = false;
        for (let i = 0; i < polygon.length; i++) {
            const current = polygon[i]!;
            const next = polygon[(i + 1) % polygon.length]!;
            let temp = current.y - next.y;
            if (Math.abs(temp) < this.EPSILON && Math.abs(point.y - (next.y + temp / 2)) < this.EPSILON) {
                if ((next.x - point.x) * (point.x - current.x) >= 0) return 0;
            }
            if (point.y < current.y !== point.y < next.y) {
                temp = ((next.x - current.x) * (point.y - current.y)) / (next.y - current.y) + current.x;
                if (Math.abs(point.x - temp) < this.EPSILON) return 0;
                if (point.x < temp) isInside = !isInside;
            }
        }
        return isInside ? 1 : -1;
    }

    private static polygon2DContourSimplify(
        contour: _Polygon2DContour,
        epsilon: number,
        copy: boolean,
    ): _Polygon2DContour {
        const simplified = new _Polygon2DContour();
        const count = contour.vertices.length;
        if (count < 3) {
            for (const point of contour.vertices) simplified.vertices.push(copy ? this.copyVertex(point) : point);
            return simplified;
        }
        const flags = new Array<number>(count).fill(0);
        const missingNodes = [count - 1, 0];
        while (missingNodes.length > 0) {
            const ind0 = missingNodes.pop()!;
            const ind1 = missingNodes.pop()!;
            flags[ind0] = 1;
            flags[ind1] = 1;
            if (ind1 - ind0 > 1) {
                const [indFar, distance] = this.getFarthestNodeToLine(contour, ind0, ind1);
                if (distance > epsilon) missingNodes.push(indFar, ind0, ind1, indFar);
            }
        }
        for (let i = 0; i < count; i++) {
            if (flags[i] === 1) {
                const point = contour.vertices[i]!;
                simplified.vertices.push(copy ? this.copyVertex(point) : point);
            }
        }
        return simplified;
    }

    private static getFarthestNodeToLine(contour: _Polygon2DContour, ind0: number, ind1: number): [number, number] {
        const first = contour.vertices[ind0]!;
        const last = contour.vertices[ind1]!;
        const deltaY = last.y - first.y;
        let nx: number;
        let ny: number;
        if (deltaY < 0.00001 && deltaY > -0.00001) {
            nx = 0;
            ny = 1;
        } else {
            const slope = -(last.x - first.x) / deltaY;
            const normalLength = Math.sqrt(1 + slope * slope);
            nx = 1 / normalLength;
            ny = slope / normalLength;
        }
        let maximumDistance = 0;
        let farthest = ind0 + 1;
        for (let i = ind0 + 1; i < ind1; i++) {
            const point = contour.vertices[i]!;
            const distance = Math.abs((point.x - first.x) * nx + (point.y - first.y) * ny);
            if (distance > maximumDistance) {
                maximumDistance = distance;
                farthest = i;
            }
        }
        return [farthest, maximumDistance];
    }

    private static insertListInBinaryTree(list: _Polygon2DContour[], head: BinaryTreeNode<_Polygon2DContour>): void {
        if (list.length === 0) return;
        const [containingNode, containingLevel] = this.findContainingContourNode(list[0]!, head);
        this.insertListInBinaryTreeNode(list, containingNode, containingLevel);
    }

    private static insertListInBinaryTreeNode(
        list: _Polygon2DContour[],
        containingNode: BinaryTreeNode<_Polygon2DContour>,
        containingLevel: number,
    ): void {
        let level = containingLevel + 1;
        if (level % 2 === 0) list[0]!.setExteriorContour(containingNode.getData());
        let lastChild = containingNode.getChild();
        if (lastChild === null) {
            lastChild = new BinaryTreeNode(list[0]!);
            containingNode.setChild(lastChild);
        } else {
            while (lastChild.getSibling() !== null) lastChild = lastChild.getSibling()!;
            const child = new BinaryTreeNode(list[0]!);
            lastChild.setSibling(child);
            lastChild = child;
        }
        list[0]!.fleetingFlag = true;
        for (let i = 1; i < list.length; i++) {
            level++;
            const contour = list[i]!;
            if (level % 2 === 0) contour.setExteriorContour(lastChild.getData());
            const child = new BinaryTreeNode(contour);
            lastChild.setChild(child);
            contour.fleetingFlag = true;
            lastChild = child;
        }
    }

    private static findContainingContourNode(
        contour: _Polygon2DContour,
        head: BinaryTreeNode<_Polygon2DContour>,
    ): [BinaryTreeNode<_Polygon2DContour>, number] {
        let current = head;
        let level = 0;
        while (true) {
            let child = current.getChild();
            let found = false;
            while (child !== null) {
                if (this.contourInsidePolygon(contour.vertices, child.getData().vertices)) {
                    current = child;
                    level++;
                    found = true;
                    break;
                }
                child = child.getSibling();
            }
            if (!found) return [current, level];
        }
    }

    private static copyVertex(point: Vertex2D): Vertex2D {
        return new Vertex2D(point.x, point.y, point.color.r(), point.color.g(), point.color.b());
    }
}
