import { InputStream } from "../../../../java/io/InputStream.js";
import { IOException } from "../../../../java/io/IOException.js";
import { OutputStream } from "../../../../java/io/OutputStream.js";
import { NullPointerException } from "../../../../java/lang/NullPointerException.js";
import { FourierShapeDescriptor } from "../../media/FourierShapeDescriptor.js";
import { GeometryMetadata } from "../../media/GeometryMetadata.js";
import { PrimitiveCountShapeDescriptor } from "../../media/PrimitiveCountShapeDescriptor.js";
import { ShapeDescriptor } from "../../media/ShapeDescriptor.js";
import { PersistenceElement } from "../PersistenceElement.js";

/** Reads and writes the legacy stream format for geometry shape descriptors. */
export class ShapeDescriptorPersistence extends PersistenceElement {
    private static readonly TYPE_ENDING = 0x01;
    private static readonly TYPE_STRING = 0x02;
    private static readonly TYPE_FOURIER_DESCRIPTOR = 0x03;
    private static readonly TYPE_PRIMITIVECOUNT_DESCRIPTOR = 0x04;

    public static exportGeometryMetadata(writer: OutputStream, metadata: GeometryMetadata): void {
        // Vitral's PersistenceElement writes four bytes for this legacy ID.
        ShapeDescriptorPersistence.writeLongBE(writer, metadata.getId());
        writer.write(ShapeDescriptorPersistence.TYPE_STRING);
        const filename = metadata.getFilename();
        if (filename === null) {
            throw new NullPointerException();
        }
        ShapeDescriptorPersistence.writeAsciiString(writer, filename);
        ShapeDescriptorPersistence.exportDescriptorMetadata(writer, metadata.getDescriptors());
        writer.write(ShapeDescriptorPersistence.TYPE_ENDING);
    }

    public static importGeometryMetadata(reader: InputStream): GeometryMetadata | null {
        const metadataId = ShapeDescriptorPersistence.readLongBE(reader);
        const chunkId = ShapeDescriptorPersistence.importByte(reader);
        if (chunkId !== ShapeDescriptorPersistence.TYPE_STRING) {
            console.error("ERROR importing database (wrong format " + chunkId + ")!");
            return null;
        }

        const metadata = new GeometryMetadata();
        metadata.setId(metadataId);
        metadata.setFilename(ShapeDescriptorPersistence.readMetadataAsciiString(reader));

        let subChunkId: number;
        do {
            subChunkId = ShapeDescriptorPersistence.importByte(reader);
            switch (subChunkId) {
                case ShapeDescriptorPersistence.TYPE_ENDING:
                    break;
                case ShapeDescriptorPersistence.TYPE_FOURIER_DESCRIPTOR:
                case ShapeDescriptorPersistence.TYPE_PRIMITIVECOUNT_DESCRIPTOR: {
                    const label = ShapeDescriptorPersistence.readMetadataAsciiString(reader);
                    const bytesToSkip = ShapeDescriptorPersistence.readSignedShortBE(reader);
                    const vector = new Float64Array(Math.trunc(bytesToSkip / 4));
                    for (let i = 0; i < vector.length; i++) {
                        vector[i] = ShapeDescriptorPersistence.readFloatBE(reader);
                    }

                    const descriptor =
                        subChunkId === ShapeDescriptorPersistence.TYPE_FOURIER_DESCRIPTOR
                            ? new FourierShapeDescriptor(label)
                            : new PrimitiveCountShapeDescriptor(label);
                    descriptor.setFeatureVector(vector);
                    metadata.getDescriptors().push(descriptor);
                    reader.skip(bytesToSkip - vector.length * 4);
                    break;
                }
                default: {
                    const bytesToSkip = ShapeDescriptorPersistence.readSignedShortBE(reader);
                    console.log("Skipping bytes: " + bytesToSkip);
                    reader.skip(bytesToSkip);
                    break;
                }
            }
        } while (subChunkId !== ShapeDescriptorPersistence.TYPE_ENDING);
        return metadata;
    }

    private static importByte(reader: InputStream): number {
        const value = reader.read();
        if (value < 0) {
            // Java's chunk loop makes no progress at EOF. Report truncated
            // input so callers can recover when reading a partial record.
            throw new IOException("Unexpected end of shape descriptor metadata");
        }
        return value < 128 ? value : value - 256;
    }

    private static readMetadataAsciiString(reader: InputStream): string {
        let result = "";
        for (let value = ShapeDescriptorPersistence.importByte(reader); value !== 0;) {
            // Java's ASCII reader casts the signed byte directly to char.
            result += String.fromCharCode(value);
            value = ShapeDescriptorPersistence.importByte(reader);
        }
        return result;
    }

    private static exportDescriptorMetadata(writer: OutputStream, descriptors: readonly ShapeDescriptor[]): void {
        for (const descriptor of descriptors) {
            if (descriptor instanceof FourierShapeDescriptor) {
                writer.write(ShapeDescriptorPersistence.TYPE_FOURIER_DESCRIPTOR);
            } else if (descriptor instanceof PrimitiveCountShapeDescriptor) {
                writer.write(ShapeDescriptorPersistence.TYPE_PRIMITIVECOUNT_DESCRIPTOR);
            } else {
                console.log("Non registered class. Dumping skipped.");
                return;
            }

            ShapeDescriptorPersistence.writeAsciiString(writer, descriptor.getLabel());
            const vector = descriptor.getFeatureVector();
            ShapeDescriptorPersistence.writeSignedShortBE(writer, vector.length * 4);
            for (const value of vector) {
                ShapeDescriptorPersistence.writeFloatBE(writer, value);
            }
        }
    }
}
