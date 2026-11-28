import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { SelectionCorners } from "vsdk/toolkit/render/SelectionCorners.js";

/**
Translation of `java/base/src/test/vsdk/toolkit/render/SelectionCornersTest.java`.
Exercises the geometry of the selection corners, independently of any
rendering technology.
*/
const EPS = 1.0e-9;
const BOX: number[] = [-1, -2, -3, 3, 2, 1];

/** AssertJ's `isCloseTo(expected, offset(EPS))`. */
function expectCloseTo(actual: number, expected: number): void {
    expect(Math.abs(actual - expected)).toBeLessThanOrEqual(EPS);
}

describe("SelectionCornersTest", () => {
    it("hasThreeLinesOnEachOfTheEightCorners", () => {
        const segments: Vector3Dd[] = SelectionCorners.buildSegments(BOX);

        expect(segments).toHaveLength(SelectionCorners.NUMBER_OF_CORNERS * SelectionCorners.LINES_PER_CORNER * 2);
    });

    it("missingOrIncompleteBoxGivesNoSegments", () => {
        expect(SelectionCorners.buildSegments(null)).toHaveLength(0);
        expect(SelectionCorners.buildSegments([0, 0, 0])).toHaveLength(0);
    });

    it("firstCornerIsAtTheEnlargedMinimumAndGoesInside", () => {
        const border: number = SelectionCorners.DEFAULT_BORDER_FRACTION;
        const line: number = SelectionCorners.DEFAULT_LINE_FRACTION;
        const segments: Vector3Dd[] = SelectionCorners.buildSegments(BOX);
        // Size of the box: 4 x 4 x 4
        const corner = new Vector3Dd(-1 - 4 * border, -2 - 4 * border, -3 - 4 * border);

        expectCloseTo(segments[0]!.x(), corner.x());
        expectCloseTo(segments[0]!.y(), corner.y());
        expectCloseTo(segments[0]!.z(), corner.z());
        expectCloseTo(segments[1]!.x(), corner.x() + 4 * line);
        expectCloseTo(segments[3]!.y(), corner.y() + 4 * line);
        expectCloseTo(segments[5]!.z(), corner.z() + 4 * line);
    });

    it("lastCornerIsAtTheEnlargedMaximumAndGoesInside", () => {
        const border: number = SelectionCorners.DEFAULT_BORDER_FRACTION;
        const line: number = SelectionCorners.DEFAULT_LINE_FRACTION;
        const segments: Vector3Dd[] = SelectionCorners.buildSegments(BOX);
        const last: number = segments.length - 6;

        expectCloseTo(segments[last]!.x(), 3 + 4 * border);
        expectCloseTo(segments[last]!.y(), 2 + 4 * border);
        expectCloseTo(segments[last]!.z(), 1 + 4 * border);
        expectCloseTo(segments[last + 1]!.x(), 3 + 4 * border - 4 * line);
        expectCloseTo(segments[last + 3]!.y(), 2 + 4 * border - 4 * line);
        expectCloseTo(segments[last + 5]!.z(), 1 + 4 * border - 4 * line);
    });

    it("everySegmentIsAxisAlignedAndHasTheLineLength", () => {
        const line: number = SelectionCorners.DEFAULT_LINE_FRACTION;
        const segments: Vector3Dd[] = SelectionCorners.buildSegments(BOX);

        for (let i = 0; i < segments.length; i += 2) {
            const d: Vector3Dd = segments[i + 1]!.subtract(segments[i]!);
            let nonZero = 0;

            nonZero += Math.abs(d.x()) > EPS ? 1 : 0;
            nonZero += Math.abs(d.y()) > EPS ? 1 : 0;
            nonZero += Math.abs(d.z()) > EPS ? 1 : 0;
            expect(nonZero).toBe(1);
            expectCloseTo(d.length(), 4 * line);
        }
    });

    it("defaultColorIsWhiteAndIsACopy", () => {
        expect(SelectionCorners.getDefaultColor().r()).toBe(1.0);
        expect(SelectionCorners.getDefaultColor()).not.toBe(SelectionCorners.getDefaultColor());
    });
});
