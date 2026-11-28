import { describe, expect, it } from "vitest";
import { RayGizmo } from "vsdk/toolkit/gui/gizmo/RayGizmo.js";

describe("RayGizmoRollIndicatorTest", () => {
    it("rollIndicator_isDisabledByDefault", () => {
        expect(new RayGizmo(null, 0).isRollIndicatorEnabled()).toBe(false);
    });
});
