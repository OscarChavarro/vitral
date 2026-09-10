import type { ModelElement } from "./ModelElement.js";

/** Base class for serializable elements of Vitral's model. */
export class Entity implements ModelElement {
    public static readonly BYTE_SIZE_IN_BYTES = 1;
    public static readonly INT_SIZE_IN_BYTES = 4;
    public static readonly LONG_SIZE_IN_BYTES = 8;
    public static readonly FLOAT_SIZE_IN_BYTES = 4;
    public static readonly DOUBLE_SIZE_IN_BYTES = 8;
    public static readonly VECTOR3D_SIZE_IN_BYTES = 24;
    public static readonly COLORRGB_SIZE_IN_BYTES = 24;
    public static readonly POINTER_SIZE_IN_BYTES = 8;

    public getSizeInBytes(): number {
        return 0;
    }

    /** Pairs public getX/setX methods, as Java reflection does for this API. */
    public getEncapsulatedVariables(): string[] {
        const getters = new Map<string, () => unknown>();
        const setters = new Set<string>();
        let prototype: object | null = Object.getPrototypeOf(this);
        while (prototype !== null && prototype !== Entity.prototype) {
            for (const name of Object.getOwnPropertyNames(prototype)) {
                const descriptor = Object.getOwnPropertyDescriptor(prototype, name);
                if (typeof descriptor?.value !== "function") continue;
                if (name.startsWith("get") && name.length > 3 && descriptor.value.length === 0)
                    getters.set(name.slice(3), descriptor.value.bind(this) as () => unknown);
                if (name.startsWith("set") && name.length > 3 && descriptor.value.length === 1)
                    setters.add(name.slice(3));
            }
            prototype = Object.getPrototypeOf(prototype);
        }
        const variables: string[] = [];
        for (const [name, getter] of getters) {
            if (!setters.has(name)) continue;
            const type = Entity.supportedTypeName(getter());
            if (type !== undefined) variables.push(`${type}:${name.charAt(0).toLowerCase()}${name.slice(1)}`);
        }
        return variables;
    }

    private static supportedTypeName(value: unknown): string | undefined {
        switch (typeof value) {
            case "number":
                return "double";
            case "boolean":
                return "boolean";
            case "string":
                return "java.lang.String";
            case "object":
                return value === null ? undefined : (value as { constructor: { name: string } }).constructor.name;
            default:
                return undefined;
        }
    }

    /** Mirrors the current Java implementation, whose clone contract returns null. */
    public clone(): unknown {
        return null;
    }
}
