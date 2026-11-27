import { describe, expect, it } from "vitest";
import { KDTree } from "../../../../../main/vsdk/toolkit/common/dataStructures/KDTree.js";
import { Vector3Dd } from "../../../../../main/vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

class Point {
    public constructor(public readonly position: Vector3Dd) {}
}
const adapter = {
    getKDPosition: (value: Point): Vector3Dd => value.position,
    copy: (value: Point): Point => new Point(new Vector3Dd(value.position)),
};

describe("KDTree", () => {
    it("finds nearest points and filters flags before and after balancing", () => {
        const tree = new KDTree(adapter);
        tree.addPoint(new Point(new Vector3Dd(0, 0, 0)), 0x10);
        tree.addPoint(new Point(new Vector3Dd(1, 0, 0)), 0x20);
        tree.addPoint(new Point(new Vector3Dd(3, 0, 0)), 0);
        for (let i = 0; i < 2; ++i) {
            const results: Point[] = [];
            const distances: number[] = [];
            expect(tree.query(new Vector3Dd(), 2, results, distances, 100, 0x10)).toBe(2);
            expect(distances).toEqual([9, 1]);
            tree.balance();
        }
    });
    it("matches brute force with balanced and newly inserted values", () => {
        const tree = new KDTree(adapter);
        const points: Point[] = [];
        for (let i = 0; i < 80; ++i) {
            const point = new Point(new Vector3Dd((i * 17) % 101, (i * 31) % 97, (i * 23) % 89));
            points.push(point);
            tree.addPoint(point, i % 3 === 0 ? 0x10 : 0);
            if (i === 49) tree.balance();
        }
        for (let i = 0; i < 50; ++i) {
            const query = new Vector3Dd(i, (i * 5) % 79, 7.5);
            const excluded = i % 2 === 0 ? 0x10 : 0;
            const expected = points
                .filter((_, index) => !excluded || index % 3 !== 0)
                .map(({ position }) => {
                    const dx = position.x() - query.x(),
                        dy = position.y() - query.y(),
                        dz = position.z() - query.z();
                    return dx * dx + dy * dy + dz * dz;
                })
                .sort((a, b) => a - b)
                .slice(0, 5);
            const results: Point[] = [];
            const distances: number[] = [];
            expect(tree.query(query, 5, results, distances, 1e10, excluded)).toBe(5);
            expect(distances.sort((a, b) => a - b)).toEqual(expected);
        }
    });
});
