import { describe, expect, it } from "vitest";
import { ByteArrayInputStream } from "java/io/ByteArrayInputStream.js";
import { ByteArrayOutputStream } from "java/io/ByteArrayOutputStream.js";
import { IOException } from "java/io/IOException.js";
import { ShapeDescriptorPersistence } from "vsdk/toolkit/io/metadata/ShapeDescriptorPersistence.js";
import { GeometryMetadata } from "vsdk/toolkit/media/GeometryMetadata.js";
import { PrimitiveCountShapeDescriptor } from "vsdk/toolkit/media/PrimitiveCountShapeDescriptor.js";

describe("ShapeDescriptorPersistence", () => {
    it("reads and writes the Java record including its four-byte ID", () => {
        const metadata = new GeometryMetadata();
        metadata.setId(0xf2345678);
        metadata.setFilename("mesh.obj");
        const counts = new PrimitiveCountShapeDescriptor("counts");
        counts.setFeature(0, 5);
        metadata.getDescriptors().push(counts);

        const writer = new ByteArrayOutputStream();
        ShapeDescriptorPersistence.exportGeometryMetadata(writer, metadata);
        const bytes = writer.toByteArray();
        const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, "0")).join("");
        expect(hex).toBe("f2345678026d6573682e6f626a0004636f756e7473000018" + "40a00000" + "00000000".repeat(5) + "01");

        const result = ShapeDescriptorPersistence.importGeometryMetadata(new ByteArrayInputStream(bytes));
        expect(result?.getId()).toBe(0xf2345678);
        expect(result?.getFilename()).toBe("mesh.obj");
        expect(result?.getDescriptors()[0]).toBeInstanceOf(PrimitiveCountShapeDescriptor);
        expect(result?.getDescriptorByName("counts")?.getFeatureVector()).toEqual(counts.getFeatureVector());
    });

    it("reports a truncated record without waiting for a terminator", () => {
        const reader = new ByteArrayInputStream(Uint8Array.of(0, 0, 0, 1, 2, 0));

        expect(() => ShapeDescriptorPersistence.importGeometryMetadata(reader)).toThrow(IOException);
    });
});
