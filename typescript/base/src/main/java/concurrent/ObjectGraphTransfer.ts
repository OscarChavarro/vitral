import type { WorkerTransferValue } from "./WorkerProtocol.js";

/**
A class whose instances an `ObjectGraphTransfer` can carry.
*/
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type TransferableClass = abstract new (...args: any[]) => object;

type EncodedValue = WorkerTransferValue;

/**
Carries a graph of objects to another JavaScript realm (a Web Worker or a
Node worker), and rebuilds it there with the same classes.

It has no Java counterpart: Java threads share one heap, so a thread works
over the very objects another one built (i.e. the `SimpleSceneSnapshot` the
`ParallelRaytracer` hands its threads). Workers share nothing, and the
structured clone of the platform copies data but not classes: an object
arrives as a plain record, without its methods. So the graph is encoded as a
table of nodes, each one naming the class of its object by its position in a
list of classes both sides agree on (class names can not be used: a bundler
renames them), and rebuilt by creating each object from the prototype of its
class (without running its constructor) and restoring its own fields.

What is carried, and how:
  - Objects of the listed classes, arrays, plain objects, `Map`s and `Set`s,
    with their identity: an object reached from two places is rebuilt once,
    and cycles are kept.
  - Primitive values (and `undefined`, and `bigint`s); typed arrays, array
    buffers and data views, copied.
  - Fields named in `transientFields` are left out and rebuilt as `null`,
    as Java deserializes its `transient` fields (i.e. the
    listeners of an `Entity`, which point to objects of the GUI).
An object of a class that is not listed, or a function, can not be carried:
encoding fails naming it, so the list can be completed.
*/
export class ObjectGraphTransfer {
    private readonly classIndex: Map<Function, number>;

    /**
    @param classes classes whose objects can be carried; both sides must use
    the same list, in the same order
    @param transientFields names of the fields that are not carried
    */
    public constructor(
        private readonly classes: readonly TransferableClass[],
        private readonly transientFields: ReadonlySet<string> = new Set<string>(),
    ) {
        this.classIndex = new Map<Function, number>();
        classes.forEach((type: TransferableClass, index: number): void => {
            this.classIndex.set(type, index);
        });
    }

    /**
    @param root object to carry
    @return a structured-clone value that `decode` rebuilds
    */
    public encode(root: unknown): WorkerTransferValue {
        const nodes: EncodedValue[] = [];
        const ids: Map<object, number> = new Map<object, number>();
        const pending: object[] = [];

        const encodeValue = (value: unknown, path: string): EncodedValue => {
            switch (typeof value) {
                case "number":
                case "string":
                case "boolean":
                    return value;
                case "undefined":
                    return { u: 1 };
                case "bigint":
                    return { b: value.toString() };
                case "function":
                    throw new Error("ObjectGraphTransfer: a function can not be carried (at " + path + ")");
                case "symbol":
                    throw new Error("ObjectGraphTransfer: a symbol can not be carried (at " + path + ")");
                default:
                    break;
            }
            if (value === null) {
                return null;
            }
            const object: object = value as object;
            let id: number | undefined = ids.get(object);
            if (id === undefined) {
                id = nodes.length;
                ids.set(object, id);
                nodes.push(null);
                pending.push(object);
                nodePaths.set(object, path);
            }
            return { r: id };
        };
        const nodePaths: Map<object, string> = new Map<object, string>();

        const rootValue: EncodedValue = encodeValue(root, "root");
        while (pending.length > 0) {
            const object: object = pending.shift()!;
            const id: number = ids.get(object)!;
            const path: string = nodePaths.get(object)!;
            nodes[id] = this.encodeNode(object, path, encodeValue);
        }
        return { root: rootValue, nodes };
    }

    private encodeNode(
        object: object,
        path: string,
        encodeValue: (value: unknown, path: string) => EncodedValue,
    ): EncodedValue {
        if (ArrayBuffer.isView(object) || object instanceof ArrayBuffer) {
            return { t: object as ArrayBufferView | ArrayBuffer };
        }
        if (Array.isArray(object)) {
            return { a: object.map((item: unknown, i: number): EncodedValue => encodeValue(item, path + "[" + i + "]")) };
        }
        if (object instanceof Map) {
            const entries: EncodedValue[] = [];
            for (const [key, item] of object) {
                entries.push([encodeValue(key, path + ".key"), encodeValue(item, path + ".value")]);
            }
            return { m: entries };
        }
        if (object instanceof Set) {
            const items: EncodedValue[] = [];
            for (const item of object) {
                items.push(encodeValue(item, path + ".item"));
            }
            return { s: items };
        }
        const prototype: object | null = Object.getPrototypeOf(object) as object | null;
        const fields: { [key: string]: EncodedValue } = {};
        for (const key of Object.keys(object)) {
            if (this.transientFields.has(key)) {
                // As Java deserializes a `transient` field: with its default
                fields[key] = null;
                continue;
            }
            fields[key] = encodeValue((object as Record<string, unknown>)[key], path + "." + key);
        }
        if (prototype === Object.prototype || prototype === null) {
            return { o: fields };
        }
        const type: Function = (prototype as { constructor: Function }).constructor;
        const index: number | undefined = this.classIndex.get(type);
        if (index === undefined) {
            throw new Error("ObjectGraphTransfer: objects of class " + type.name +
                " can not be carried (at " + path + "): add it to the list of classes");
        }
        return { c: index, f: fields };
    }

    /**
    @param encoded what `encode` gave
    @return the rebuilt graph
    */
    public decode(encoded: WorkerTransferValue): unknown {
        const record = encoded as unknown as { readonly root: EncodedValue; readonly nodes: readonly EncodedValue[] };
        const nodes: readonly EncodedValue[] = record.nodes;
        const objects: unknown[] = new Array<unknown>(nodes.length);

        // First pass: create every object, so references can be resolved
        nodes.forEach((node: EncodedValue, i: number): void => {
            const n = node as Record<string, unknown>;
            if ("t" in n) {
                objects[i] = n["t"];
            }
            else if ("a" in n) {
                objects[i] = [];
            }
            else if ("m" in n) {
                objects[i] = new Map<unknown, unknown>();
            }
            else if ("s" in n) {
                objects[i] = new Set<unknown>();
            }
            else if ("o" in n) {
                objects[i] = {};
            }
            else {
                const type: TransferableClass | undefined = this.classes[n["c"] as number];
                if (type === undefined) {
                    throw new Error("ObjectGraphTransfer: unknown class index " + String(n["c"]));
                }
                objects[i] = Object.create(type.prototype) as object;
            }
        });

        const decodeValue = (value: EncodedValue): unknown => {
            if (value === null || typeof value !== "object") {
                return value;
            }
            const v = value as Record<string, unknown>;
            if ("r" in v) {
                return objects[v["r"] as number];
            }
            if ("u" in v) {
                return undefined;
            }
            if ("b" in v) {
                return BigInt(v["b"] as string);
            }
            throw new Error("ObjectGraphTransfer: malformed value");
        };

        // Second pass: restore the contents
        nodes.forEach((node: EncodedValue, i: number): void => {
            const n = node as Record<string, unknown>;
            const target: unknown = objects[i];
            if ("t" in n) {
                return;
            }
            if ("a" in n) {
                const array = target as unknown[];
                for (const item of n["a"] as EncodedValue[]) {
                    array.push(decodeValue(item));
                }
                return;
            }
            if ("m" in n) {
                const map = target as Map<unknown, unknown>;
                for (const entry of n["m"] as EncodedValue[][]) {
                    map.set(decodeValue(entry[0]!), decodeValue(entry[1]!));
                }
                return;
            }
            if ("s" in n) {
                const set = target as Set<unknown>;
                for (const item of n["s"] as EncodedValue[]) {
                    set.add(decodeValue(item));
                }
                return;
            }
            const fields = ("o" in n ? n["o"] : n["f"]) as { [key: string]: EncodedValue };
            const object = target as Record<string, unknown>;
            for (const key of Object.keys(fields)) {
                object[key] = decodeValue(fields[key]!);
            }
        });

        return decodeValue(record.root);
    }
}
