import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossing, RayCrossingKind, RayCrossingQuery, RayCrossingSupport, Sphere } from "../../../../../main/index.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";

describe("ray crossings", () => {
    it("emits ordered sphere crossings and distinguishes unsupported geometry", () => {
        const sphere = new Sphere(1), query = new RayCrossingQuery(0, 10);
        expect(Sphere.intersectionRoots(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0), 1, query)).toEqual([1, 2]);
        const events: RayCrossing[] = [];
        const sink = { crossing: (event: RayCrossing) => { events.push(event); } };
        const ray = new Ray(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
        expect(sphere.emitRayCrossings(ray, query, sink)).toBe(RayCrossingSupport.Supported);
        expect(events.map(event => event.kind)).toEqual([RayCrossingKind.Entering, RayCrossingKind.Leaving]);
        expect(events[0]?.geometry).toBe(sphere);
        events.length = 0;
        sphere.emitRayCrossings(new Ray(new Vector3Dd(-2, 1, 0), new Vector3Dd(1, 0, 0)), query, sink);
        expect(events.map(event => event.kind)).toEqual([RayCrossingKind.Tangent]);
        events.length = 0;
        expect(new Box(1, 1, 1).emitRayCrossings(ray, query, sink)).toBe(RayCrossingSupport.Unsupported);
        expect(events).toHaveLength(0);
        expect(Sphere.intersectionRoots(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0), 1, new RayCrossingQuery(2.5, 3.5))).toEqual([]);
    });
});
