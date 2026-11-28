import { describe, expect, it } from "vitest";
import { ViewportSet } from "vsdk/toolkit/gui/viewport/ViewportSet.js";

/**
Exercises the translation of pixel coordinates between a `ViewportSet` and
its viewports.
 */
describe("ViewportSetCoordinatesTest", () => {
    /** AssertJ `isEqualTo`: Java `equals` when the value defines it. */
    function isEqualTo(actual: unknown, expected: unknown): void {
        const a = actual as { equals?: (other: unknown) => boolean } | null;
        if (a !== null && typeof a === "object" && typeof a.equals === "function") {
            expect(a.equals(expected)).toBe(true);
        } else {
            expect(actual).toEqual(expected);
        }
    }

    it("given_everyViewportOfTheStandardSet_when_convertingBackAndForth_then_coordinatesAreKept", () => {
        // Arrange
        let set = ViewportSet.createStandardSet("test");

        set.resize(1600, 900);

        for (const viewport of set.getViewports()) {
            for (const x of [-20, 0, 37, 400]) {
                for (const y of [-20, 0, 55, 300]) {
                    // Act
                    let setX = set.toSetX(viewport, x);
                    let setY = set.toSetY(viewport, y);

                    // Assert
                    isEqualTo(set.toViewportX(viewport, setX), x);
                    isEqualTo(set.toViewportY(viewport, setY), y);
                }
            }
        }
    });

    it("given_aViewport_when_convertingItsUpperLeftCorner_then_itIsInsideTheViewportArea", () => {
        // Arrange
        let set = ViewportSet.createStandardSet("test");
        set.resize(1600, 900);
        let viewport = set.getViewport(1);

        // Act
        let found = set.findViewportAt(set.toSetX(viewport, 1), set.toSetY(viewport, 1));

        // Assert
        expect(found).toBe(viewport);
    });
});
