import { Double } from "../../../../java/lang/Double.js";
import { Float } from "../../../../java/lang/Float.js";
import { Integer } from "../../../../java/lang/Integer.js";
import { Long } from "../../../../java/lang/Long.js";
import type { Entity } from "../../common/Entity.js";
import { type EntityEvent, EntityEventType } from "../../common/EntityEvent.js";
import type { EntityListener } from "../../common/EntityListener.js";
import { VSDK } from "../../common/VSDK.js";
import { Logger } from "../../common/logging/Logger.js";
import { ControlSpecification } from "./ControlSpecification.js";
import type { GenericEditorListener } from "./GenericEditorListener.js";

type Accessor = (...args: unknown[]) => unknown;

/**
Builds an editor for any `Entity` from its control specifications (see
`Entity.getControlSpecifications()` and `ControlSpecification`), without
code specific to the entity class.

This class holds all the logic independent of GUI technology: it parses the
specifications, reads and writes values through the entity accessors
(reflection in Java), validates the intervals and orchestrates the
construction. Each GUI technology (Awt, Qt, Web...) provides a subclass that
only implements the presentation hooks: `beginBuild`, `addControl`,
`endBuild`, `showValidationMessage`, `setControlValue` and `clearControls`.

The editor subscribes to the entity under edition: when the entity emits
`UPDATED` the controls are refreshed, and when it emits `DELETED` the
controls are removed and an "Entity deleted" message is shown.

Java finds the accessors with reflection (`Class.getMethod`) and checks the
return type of the getter against the declared type. A JavaScript object has
its methods as properties, and every Java numeric type is a `number`: the
getter is taken if it is a function of no arguments that returns a number,
and the value is presented as Java's `String.valueOf` would (`1.0` for a
`double`, `1` for an `int`).
*/
export abstract class GenericEditor implements EntityListener {
    protected entity: Entity | null;
    protected readonly specifications: ControlSpecification[];
    private listener: GenericEditorListener | null;
    private entityDeleted: boolean;

    protected constructor() {
        this.entity = null;
        this.specifications = [];
        this.listener = null;
        this.entityDeleted = false;
    }

    /**
    @param listener object notified after each accepted change, may be null
    */
    public setListener(listener: GenericEditorListener | null): void {
        this.listener = listener;
    }

    /**
    Builds the editor for the given entity: one control per supported control
    specification, or a message when there is nothing to edit.
    @param entity the entity to edit
    */
    public build(entity: Entity | null): void {
        this.detach();
        this.entity = entity;
        this.specifications.length = 0;

        if (entity === null) {
            Logger.reportMessage(this, VSDK.WARNING, "GenericEditor.build", "Null entity received, building an empty editor.");
            this.beginBuild("");
            this.showValidationMessage("No object to edit.");
            this.endBuild();
            return;
        }

        entity.addEntityListener(this);
        this.beginBuild(GenericEditor.simpleClassName(entity));

        for (const text of entity.getControlSpecifications()) {
            const specification: ControlSpecification | null = ControlSpecification.parse(text);
            if (specification === null) {
                continue;
            }
            if (!GenericEditor.typeIsSupported(specification.getType())) {
                Logger.reportMessage(
                    this,
                    VSDK.WARNING,
                    "GenericEditor.build",
                    'Unsupported type "' + specification.getType() + '" in control specification "' + text +
                        '" of class ' + GenericEditor.simpleClassName(entity) + ", control skipped.",
                );
                continue;
            }
            const value: string | null = this.readValue(specification);
            if (value === null) {
                continue;
            }
            this.specifications.push(specification);
            this.addControl(specification, value);
        }

        if (this.specifications.length === 0) {
            this.showValidationMessage("No editable attributes for " + GenericEditor.simpleClassName(entity));
        } else {
            this.showValidationMessage(null);
        }
        this.endBuild();
    }

    /**
    Stops listening to the entity under edition, if any. Call it when the
    editor is hidden or reused for another entity.
    */
    public detach(): void {
        if (this.entity !== null) {
            this.entity.removeEntityListener(this);
        }
        this.entity = null;
        this.entityDeleted = false;
    }

    /**
    @return true if the last entity under edition emitted `DELETED` and the
    editor is showing the "Entity deleted" message
    */
    public isEntityDeleted(): boolean {
        return this.entityDeleted;
    }

    /**
    Reacts to the events of the entity under edition.
    @param event the event emitted by the entity
    */
    public notifyEntityEvent(event: EntityEvent | null): void {
        if (event === null || this.entity === null || event.getSource() !== this.entity) {
            return;
        }
        if (event.getType() === EntityEventType.DELETED) {
            const className: string = GenericEditor.simpleClassName(this.entity);
            this.entity.removeEntityListener(this);
            this.entity = null;
            this.specifications.length = 0;
            this.entityDeleted = true;
            this.clearControls("Entity deleted (" + className + ").");
        } else if (event.getType() === EntityEventType.UPDATED) {
            for (const specification of this.specifications) {
                const value: string | null = this.readValue(specification);
                if (value !== null) {
                    this.setControlValue(specification, value);
                }
            }
        }
    }

    /**
    @return the entity under edition, or null
    */
    public getEntity(): Entity | null {
        return this.entity;
    }

    /**
    Reads the current value of an attribute through its getter.
    @param specification the attribute to read
    @return the value as text, or null if the entity has no usable getter
    */
    public readValue(specification: ControlSpecification): string | null {
        const getter: Accessor | null = this.findGetter(specification);
        if (getter === null || this.entity === null) {
            return null;
        }
        let value: unknown;
        try {
            value = getter.call(this.entity);
        } catch (e) {
            Logger.reportMessage(this, VSDK.WARNING, "GenericEditor.readValue", "Cannot call get" + specification.getLabel() + ": " + String(e));
            return null;
        }
        if (typeof value !== "number") {
            this.reportMissingAccessor(specification, "get" + specification.getLabel() + "() returning " + specification.getType());
            return null;
        }
        return GenericEditor.valueToString(specification.getType(), value);
    }

    /**
    Validates a value typed by the user and, if valid, writes it to the
    entity through its setter and notifies the listener. On failure, the
    reason is presented with `showValidationMessage`.
    @param specification the attribute to change
    @param text the new value as typed by the user
    @return true if the value was accepted and written
    */
    public updateValue(specification: ControlSpecification | null, text: string | null): boolean {
        if (this.entity === null || specification === null || text === null) {
            return false;
        }

        let value: number;
        try {
            value = GenericEditor.parseValue(specification.getType(), text.trim());
        } catch {
            this.showValidationMessage(specification.getLabel() + ': "' + text + '" is not a valid ' + specification.getType() + ".");
            return false;
        }

        const numeric: number = value;
        if (!specification.contains(numeric)) {
            this.showValidationMessage(specification.getLabel() + " must be in " + specification.getIntervalText() + ".");
            return false;
        }

        const setter: Accessor | null = this.findSetter(specification);
        if (setter === null) {
            this.showValidationMessage(specification.getLabel() + " cannot be changed.");
            return false;
        }
        try {
            setter.call(this.entity, value);
        } catch (e) {
            Logger.reportMessage(this, VSDK.WARNING, "GenericEditor.updateValue", "Cannot call set" + specification.getLabel() + ": " + String(e));
            this.showValidationMessage(specification.getLabel() + " could not be changed.");
            return false;
        }

        this.showValidationMessage(null);
        const changed: Entity = this.entity;
        changed.update();
        if (this.listener !== null) {
            this.listener.notifyEntityChanged(changed);
        }
        return true;
    }

    /**
    Starts the presentation of a new editor, discarding any previous one.
    @param title name of the edited entity class
    */
    protected abstract beginBuild(title: string): void;

    /**
    Adds the control for one attribute. When the user confirms a new value,
    the subclass must call `updateValue(specification, text)`.
    @param specification the attribute presented by the control
    @param value current value as text
    */
    protected abstract addControl(specification: ControlSpecification, value: string): void;

    /**
    Finishes the presentation of the editor.
    */
    protected abstract endBuild(): void;

    /**
    Presents a message to the user about the last edition.
    @param message message to show, or null to clear it
    */
    protected abstract showValidationMessage(message: string | null): void;

    /**
    Shows a new value in the control of one attribute, after the entity
    emitted `UPDATED`.
    @param specification the attribute whose control must change
    @param value current value as text
    */
    protected abstract setControlValue(specification: ControlSpecification, value: string): void;

    /**
    Removes all the controls and presents only a message, used when the
    entity under edition is deleted.
    @param message message to show in place of the controls
    */
    protected abstract clearControls(message: string): void;

    private static typeIsSupported(type: string): boolean {
        return type === "double" || type === "float" || type === "int" || type === "long";
    }

    private static parseValue(type: string, text: string): number {
        switch (type) {
            case "double":
                return Double.parseDouble(text);
            case "float":
                return Float.parseFloat(text);
            case "int":
                return Integer.parseInt(text);
            case "long":
                return Number(Long.parseLong(text));
            default:
                throw new RangeError("Unsupported type " + type);
        }
    }

    /**
    `String.valueOf` of the boxed value a Java getter of the given type
    returns.
    */
    private static valueToString(type: string, value: number): string {
        switch (type) {
            case "int":
            case "long":
                return String(Math.trunc(value));
            default:
                return Double.toString(value);
        }
    }

    private findGetter(specification: ControlSpecification): Accessor | null {
        const methodName: string = "get" + specification.getLabel();
        const getter: unknown = this.entity === null ? undefined : (this.entity as unknown as Record<string, unknown>)[methodName];

        if (typeof getter !== "function" || (getter as Accessor).length !== 0) {
            this.reportMissingAccessor(specification, methodName + "()");
            return null;
        }
        return getter as Accessor;
    }

    private findSetter(specification: ControlSpecification): Accessor | null {
        const methodName: string = "set" + specification.getLabel();
        const setter: unknown = this.entity === null ? undefined : (this.entity as unknown as Record<string, unknown>)[methodName];

        if (typeof setter !== "function") {
            this.reportMissingAccessor(specification, methodName + "(" + specification.getType() + ")");
            return null;
        }
        return setter as Accessor;
    }

    private reportMissingAccessor(specification: ControlSpecification, accessor: string): void {
        Logger.reportMessage(
            this,
            VSDK.WARNING,
            "GenericEditor",
            "Class " + (this.entity === null ? "null" : GenericEditor.simpleClassName(this.entity)) +
                ' declares control "' + specification.getName() + '" but has no public ' + accessor +
                "; add it or fix the control specification.",
        );
    }

    /**
    Java's `entity.getClass().getSimpleName()`, which names the editor. A
    production bundle renames the JavaScript classes, so an application whose
    editor titles must keep the Java names installs a resolver that knows them;
    by default, the name of the JavaScript class is used.
    @param resolver gives the simple class name of an entity, or null to use the
    name of its JavaScript class
    */
    public static setSimpleClassNameResolver(resolver: ((entity: Entity) => string) | null): void {
        GenericEditor.simpleClassNameResolver = resolver;
    }

    private static simpleClassNameResolver: ((entity: Entity) => string) | null = null;

    private static simpleClassName(entity: Entity): string {
        if (GenericEditor.simpleClassNameResolver !== null) {
            return GenericEditor.simpleClassNameResolver(entity);
        }
        return (entity as unknown as { constructor: { name: string } }).constructor.name;
    }
}
