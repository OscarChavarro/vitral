import { describe, expect, it } from "vitest";
import { Entity } from "vsdk/toolkit/common/Entity.js";
import { EntityEvent, EntityEventType } from "vsdk/toolkit/common/EntityEvent.js";
import type { EntityListener } from "vsdk/toolkit/common/EntityListener.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Arrow } from "vsdk/toolkit/environment/geometry/volume/Arrow.js";
import { BlobSolid } from "vsdk/toolkit/environment/geometry/volume/BlobSolid.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { PlanarHalfSpace } from "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.js";
import { QuadricSurface } from "vsdk/toolkit/environment/geometry/volume/QuadricSurface.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { Torus } from "vsdk/toolkit/environment/geometry/volume/Torus.js";

class RecordingListener implements EntityListener {
    public readonly events: EntityEvent[] = [];

    public notifyEntityEvent(event: EntityEvent): void {
        this.events.push(event);
    }
}

describe("Entity", () => {
    it("keeps control specifications declared by geometry constructors", () => {
        expect(new Sphere(1).getControlSpecifications()).toEqual(["double;radius;(0, INFINITE)"]);
        expect(new Cone(1, 0, 2).getControlSpecifications()).toEqual([
            "double;bottomRadius;(0, INFINITE)",
            "double;topRadius;[0, INFINITE)",
            "double;height;(0, INFINITE)",
        ]);
        expect(new Entity().getControlSpecifications()).toEqual([]);
    });

    it("exposes semantic accessors for sphere and cone attributes", () => {
        const sphere = new Sphere(1);
        sphere.setRadius(2.5);
        expect(sphere.getRadius()).toBe(2.5);
        expect(sphere.getRadiusSquared()).toBe(6.25);

        const cone = new Cone(1, 0.5, 2);
        cone.setBottomRadius(3);
        cone.setTopRadius(0);
        cone.setHeight(4);
        expect([cone.getBottomRadius(), cone.getTopRadius(), cone.getHeight()]).toEqual([3, 0, 4]);
    });

    it("notifies UPDATED and DELETED, then drops subscribers", () => {
        const sphere = new Sphere(1);
        const listener = new RecordingListener();
        sphere.addEntityListener(listener);
        sphere.addEntityListener(listener);

        sphere.update();
        sphere.dispose();
        sphere.update();

        expect(listener.events.map((event) => event.getType())).toEqual([
            EntityEventType.UPDATED,
            EntityEventType.DELETED,
        ]);
        expect(listener.events[0]?.getSource()).toBe(sphere);
    });

    it("stops notifying removed listeners", () => {
        const entity = new Entity();
        const listener = new RecordingListener();
        entity.addEntityListener(listener);
        entity.removeEntityListener(listener);
        entity.update();
        expect(listener.events).toEqual([]);
    });

    it("has getX/setX accessors for every declared control specification", () => {
        for (const entity of [
            new Sphere(1),
            new Cone(1, 0, 2),
            new Box(1, 2, 3),
            new Arrow(1, 0.5, 0.1, 0.2),
            new Torus(1, 0.25),
            new PlanarHalfSpace(0, 0, 1, 0),
            new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1),
            new BlobSolid(0.5, false),
        ]) {
            for (const specification of entity.getControlSpecifications()) {
                const name = specification.split(";")[1]!.trim();
                const label = name.charAt(0).toUpperCase() + name.slice(1);
                const accessors = entity as unknown as Record<string, unknown>;
                expect(typeof accessors[`get${label}`]).toBe("function");
                expect(typeof accessors[`set${label}`]).toBe("function");
            }
        }
    });

    it("does not report its own event methods as encapsulated variables", () => {
        expect(new Sphere(1).getEncapsulatedVariables()).toContain("double:radius");
        expect(new Sphere(1).getEncapsulatedVariables().some((v) => v.includes("controlSpecifications"))).toBe(false);
    });
});
