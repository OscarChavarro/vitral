import { describe, expect, it } from "vitest";
import { Entity } from "vsdk/toolkit/common/Entity.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { ControlSpecification } from "vsdk/toolkit/gui/editor/ControlSpecification.js";
import { GenericEditor } from "vsdk/toolkit/gui/editor/GenericEditor.js";

/** Records the presentation hooks instead of creating widgets. */
class RecordingEditor extends GenericEditor {
    public readonly controls: string[] = [];
    public message: string | null = null;
    public clearedMessage: string | null = null;

    public constructor() {
        super();
    }

    protected beginBuild(_title: string): void {
        this.controls.length = 0;
    }

    protected addControl(specification: ControlSpecification, value: string): void {
        this.controls.push(specification.getLabel() + "=" + value);
    }

    protected endBuild(): void {}

    protected showValidationMessage(message: string | null): void {
        this.message = message;
    }

    protected setControlValue(specification: ControlSpecification, value: string): void {
        for (let i = 0; i < this.controls.length; i++) {
            if (this.controls[i]!.startsWith(specification.getLabel() + "=")) {
                this.controls[i] = specification.getLabel() + "=" + value;
            }
        }
    }

    protected clearControls(message: string): void {
        this.controls.length = 0;
        this.clearedMessage = message;
    }

    public first(): ControlSpecification {
        return this.specifications[0]!;
    }

    /** Java reads the package-private `specifications` list directly. */
    public specificationAt(index: number): ControlSpecification {
        return this.specifications[index]!;
    }
}

describe("GenericEditorTest", () => {
    it("parsesOpenAndClosedIntervals", () => {
        const open = ControlSpecification.parse("double;radius;(0, INFINITE)");
        expect(open).not.toBeNull();
        expect(open!.getType()).toBe("double");
        expect(open!.getName()).toBe("radius");
        expect(open!.getLabel()).toBe("Radius");
        expect(open!.contains(0.0)).toBe(false);
        expect(open!.contains(-1.0)).toBe(false);
        expect(open!.contains(1e-9)).toBe(true);
        expect(open!.contains(1e300)).toBe(true);

        const closed = ControlSpecification.parse("int;count;[1, 10]");
        expect(closed).not.toBeNull();
        expect(closed!.contains(1)).toBe(true);
        expect(closed!.contains(10)).toBe(true);
        expect(closed!.contains(0)).toBe(false);
        expect(closed!.contains(11)).toBe(false);

        const unbounded = ControlSpecification.parse("double;offset");
        expect(unbounded).not.toBeNull();
        expect(unbounded!.contains(-1e300)).toBe(true);
    });

    it("rejectsMalformedSpecifications", () => {
        expect(ControlSpecification.parse("double")).toBeNull();
        expect(ControlSpecification.parse("double;radius;0, 1")).toBeNull();
        expect(ControlSpecification.parse("double;radius;(1, 0)")).toBeNull();
        expect(ControlSpecification.parse("double;radius;(a, 1)")).toBeNull();
    });

    it("editsSphereRadiusThroughAccessors", () => {
        const sphere = new Sphere(1.0);
        const editor = new RecordingEditor();
        const changed: Entity[] = [];
        editor.setListener({ notifyEntityChanged: (entity: Entity) => changed.push(entity) });

        editor.build(sphere);
        expect(editor.controls).toEqual(["Radius=1.0"]);

        expect(editor.updateValue(editor.first(), "2.5")).toBe(true);
        expect(sphere.getRadius()).toBe(2.5);
        expect(sphere.getRadiusSquared()).toBe(6.25);
        expect(changed.length).toBe(1);
        expect(editor.message).toBeNull();

        expect(editor.updateValue(editor.first(), "0")).toBe(false);
        expect(editor.updateValue(editor.first(), "-3")).toBe(false);
        expect(editor.updateValue(editor.first(), "abc")).toBe(false);
        expect(sphere.getRadius()).toBe(2.5);
        expect(changed.length).toBe(1);
        expect(editor.message).not.toBeNull();
    });

    it("editsConeAttributesThroughAccessors", () => {
        const cone = new Cone(1.0, 0.0, 2.0);
        const editor = new RecordingEditor();

        editor.build(cone);
        expect(editor.controls).toEqual(["BottomRadius=1.0", "TopRadius=0.0", "Height=2.0"]);

        const bottom = editor.specificationAt(0);
        const top = editor.specificationAt(1);
        const height = editor.specificationAt(2);
        expect(editor.updateValue(bottom, "0")).toBe(false);
        expect(editor.updateValue(top, "0")).toBe(true);
        expect(editor.updateValue(top, "0.5")).toBe(true);
        expect(editor.updateValue(height, "0")).toBe(false);
        expect(editor.updateValue(height, "3")).toBe(true);
        expect(cone.getBottomRadius()).toBe(1.0);
        expect(cone.getTopRadius()).toBe(0.5);
        expect(cone.getHeight()).toBe(3.0);
    });

    it("refreshesControlsWhenEntityIsUpdated", () => {
        const sphere = new Sphere(1.0);
        const editor = new RecordingEditor();
        editor.build(sphere);

        // A change made by another tool, announced with update()
        sphere.setRadius(4.0);
        sphere.update();
        expect(editor.controls).toEqual(["Radius=4.0"]);
    });

    it("clearsControlsWhenEntityIsDisposed", () => {
        const sphere = new Sphere(1.0);
        const editor = new RecordingEditor();
        editor.build(sphere);

        sphere.dispose();
        expect(editor.isEntityDeleted()).toBe(true);
        expect(editor.getEntity()).toBeNull();
        expect(editor.controls.length).toBe(0);
        expect(editor.clearedMessage).toBe("Entity deleted (Sphere).");
        expect(editor.updateValue(ControlSpecification.parse("double;radius;(0, INFINITE)"), "2")).toBe(false);
    });

    it("stopsListeningToPreviousEntity", () => {
        const first = new Sphere(1.0);
        const second = new Sphere(2.0);
        const editor = new RecordingEditor();
        editor.build(first);
        editor.build(second);

        first.dispose();
        expect(editor.isEntityDeleted()).toBe(false);
        expect(editor.controls).toEqual(["Radius=2.0"]);
    });

    it("reportsEntitiesWithoutSpecifications", () => {
        const editor = new RecordingEditor();
        editor.build(new Entity());
        expect(editor.controls.length).toBe(0);
        expect(editor.message).not.toBeNull();
    });
});
