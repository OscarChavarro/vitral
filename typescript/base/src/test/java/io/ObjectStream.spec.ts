import { describe, expect, it } from "vitest";
import { ByteArrayInputStream } from "java/io/ByteArrayInputStream.js";
import { ByteArrayOutputStream } from "java/io/ByteArrayOutputStream.js";
import { IOException } from "java/io/IOException.js";
import { ObjectInputStream } from "java/io/ObjectInputStream.js";
import { ObjectOutputStream } from "java/io/ObjectOutputStream.js";

describe("portable object streams", () => {
    it("reads objects in the same order they were written", () => {
        const bytes = new ByteArrayOutputStream();
        const writer = new ObjectOutputStream(bytes);
        writer.writeObject({ id: 0x1000_0000_0000_0000n, name: "first\nitem" });
        writer.writeObject(["second"]);

        const reader = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        expect(reader.readObject()).toEqual({ id: 0x1000_0000_0000_0000n, name: "first\nitem" });
        expect(reader.readObject()).toEqual(["second"]);
        expect(() => reader.readObject()).toThrow(IOException);
    });

    it("reads a single record written without the delimiting newline", () => {
        const legacy = new TextEncoder().encode('{"id":{"$vitralBigInt":"42"}}');
        expect(new ObjectInputStream(new ByteArrayInputStream(legacy)).readObject()).toEqual({ id: 42n });
    });
});
