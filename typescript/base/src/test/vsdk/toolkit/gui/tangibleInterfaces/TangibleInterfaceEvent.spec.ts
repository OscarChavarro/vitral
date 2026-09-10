import { describe, expect, it } from "vitest";
import { Quaterniond } from "vsdk/toolkit/common/linealAlgebra/Quaterniond.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { TangibleInterfaceEvent } from "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.js";
describe("TangibleInterfaceEvent", () =>
    it("retains pose and id", () => {
        const e = new TangibleInterfaceEvent(
            "rayCube",
            new Vector3Dd(1, 2, 3),
            new Quaterniond(new Vector3Dd(0, 1, 0), 1),
        );
        expect(e.getId()).toBe("rayCube");
        expect(e.getPosition().epsilonEquals(new Vector3Dd(1, 2, 3))).toBe(true);
        expect(e.toString()).toContain("TangibleInterfaceEvent{id=rayCube");
    }));
