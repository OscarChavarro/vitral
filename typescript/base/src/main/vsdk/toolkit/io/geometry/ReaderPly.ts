import type { WorkerExecutionOptions } from "../../../../java/concurrent/WorkerProtocol.js";
import type { InputStream } from "../../../../java/io/InputStream.js";
import { IOException } from "../../../../java/io/IOException.js";
import { Double } from "../../../../java/lang/Double.js";
import { Float } from "../../../../java/lang/Float.js";
import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { Integer } from "../../../../java/lang/Integer.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { StringTokenizer } from "../../../../java/util/StringTokenizer.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Logger } from "../../common/logging/Logger.js";
import type { Geometry } from "../../environment/geometry/Geometry.js";
import { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { PersistenceElement } from "../PersistenceElement.js";
import type { ReaderPlyWorkerFactory } from "./ReaderPlyMeshWorker.js";
import { ReaderPlyResult } from "./ReaderPlyResult.js";

interface PlyProperty {
    readonly name: string;
    readonly type: string;
}

interface PlyListProperty {
    readonly name: string;
    readonly countType: string;
    readonly valueType: string;
}

interface PlyElement {
    readonly name: string;
    readonly count: number;
    readonly properties: (PlyProperty | PlyListProperty)[];
}

interface PlyHeader {
    format: string;
    readonly elements: PlyElement[];
}

type TriangleIndices = readonly [number, number, number];

/** Reads ASCII and binary PLY streams and assembles triangle meshes in two workers. */
export class ReaderPly extends PersistenceElement {
    private static readonly FORMAT_ASCII = "ascii";
    private static readonly FORMAT_BINARY_LITTLE_ENDIAN = "binary_little_endian";
    private static readonly FORMAT_BINARY_BIG_ENDIAN = "binary_big_endian";

    private static defaultMaterial(): SimpleMaterial {
        return new SimpleMaterial()
            .withAmbient(new ColorRgb(0.18, 0.18, 0.18))
            .withDiffuse(new ColorRgb(0.72, 0.76, 0.7))
            .withSpecular(new ColorRgb(0.2, 0.2, 0.2))
            .withDoubleSided(false);
    }

    private static addThing(geometry: Geometry, bodies: ArrayList<SimpleBody>): void {
        const thing = new SimpleBody();
        thing.setGeometry(geometry);
        thing.setPosition(new Vector3Dd());
        thing.setRotation(new Matrix4x4d());
        thing.setRotationInverse(new Matrix4x4d());
        thing.setMaterial(ReaderPly.defaultMaterial());
        bodies.add(thing);
    }

    private static readPlyAsciiLine(input: InputStream): string | null {
        let line = "";
        let value = input.read();
        if (value < 0) return null;
        while (value >= 0 && value !== 10) {
            if (value !== 13) line += String.fromCharCode(value);
            value = input.read();
        }
        return line;
    }

    private static splitWhitespace(line: string): string[] {
        const tokens: string[] = [];
        const tokenizer = new StringTokenizer(line);
        while (tokenizer.hasMoreTokens()) {
            tokens.push(tokenizer.nextToken().toCString());
        }
        return tokens;
    }

    private static readHeader(input: InputStream): PlyHeader {
        const header: PlyHeader = { format: ReaderPly.FORMAT_ASCII, elements: [] };
        const signature = ReaderPly.readPlyAsciiLine(input);
        if (signature?.trim() !== "ply") {
            throw new IllegalArgumentException("input is not a PLY stream");
        }

        let currentElement: PlyElement | null = null;
        for (;;) {
            const line = ReaderPly.readPlyAsciiLine(input);
            if (line === null) throw new IOException("Unexpected end of PLY header");
            const tokens = ReaderPly.splitWhitespace(line);
            if (tokens.length === 0) continue;

            const tag = tokens[0]!.toLowerCase();
            if (tag === "end_header") return header;
            if (tag === "comment" || tag === "obj_info") continue;
            if (tag === "format" && tokens.length >= 2) {
                const format = tokens[1]!.toLowerCase();
                if (
                    format !== ReaderPly.FORMAT_ASCII &&
                    format !== ReaderPly.FORMAT_BINARY_LITTLE_ENDIAN &&
                    format !== ReaderPly.FORMAT_BINARY_BIG_ENDIAN
                ) {
                    throw new IllegalArgumentException("unsupported PLY format: " + format);
                }
                header.format = format;
                continue;
            }
            if (tag === "element" && tokens.length >= 3) {
                currentElement = { name: tokens[1]!, count: Integer.parseInt(tokens[2]!), properties: [] };
                header.elements.push(currentElement);
                continue;
            }
            if (tag === "property" && currentElement !== null) {
                if (tokens.length >= 5 && tokens[1] === "list") {
                    currentElement.properties.push({ countType: tokens[2]!, valueType: tokens[3]!, name: tokens[4]! });
                } else if (tokens.length >= 3) {
                    currentElement.properties.push({ type: tokens[1]!, name: tokens[2]! });
                } else {
                    throw new IllegalArgumentException("Incomplete PLY property");
                }
            }
        }
    }

    private static plyTypeSize(type: string): number {
        switch (type) {
            case "char":
            case "int8":
            case "uchar":
            case "uint8":
                return 1;
            case "short":
            case "int16":
            case "ushort":
            case "uint16":
                return 2;
            case "int":
            case "int32":
            case "uint":
            case "uint32":
            case "float":
            case "float32":
                return 4;
            case "double":
            case "float64":
                return 8;
            default:
                return 0;
        }
    }

    private static isSignedIntegerType(type: string): boolean {
        return (
            type === "char" ||
            type === "int8" ||
            type === "short" ||
            type === "int16" ||
            type === "int" ||
            type === "int32"
        );
    }

    private static isFloatType(type: string): boolean {
        return type === "float" || type === "float32" || type === "double" || type === "float64";
    }

    private static readUnsignedIntegerBytes(input: InputStream, bytes: number, bigEndian: boolean): bigint {
        const buffer = new Uint8Array(bytes);
        ReaderPly.readBytes(input, buffer);

        let value = 0n;
        if (bigEndian) {
            for (let i = 0; i < bytes; i++) {
                value = (value << 8n) | BigInt(buffer[i]!);
            }
        } else {
            for (let i = bytes - 1; i >= 0; i--) {
                value = (value << 8n) | BigInt(buffer[i]!);
            }
        }
        return value;
    }

    private static readBinaryScalarAsDouble(input: InputStream, type: string, bigEndian: boolean): number {
        const bytes = ReaderPly.plyTypeSize(type);
        if (bytes <= 0) return 0;
        const raw = ReaderPly.readUnsignedIntegerBytes(input, bytes, bigEndian);
        if (ReaderPly.isFloatType(type)) {
            if (bytes === 4) return Float.intBitsToFloat(Number(raw) | 0);
            return Double.longBitsToDouble(raw);
        }
        if (ReaderPly.isSignedIntegerType(type)) {
            return Number(BigInt.asIntN(bytes * 8, raw));
        }
        return Number(raw);
    }

    private static readBinaryScalarAsInt(input: InputStream, type: string, bigEndian: boolean): number {
        const value = ReaderPly.readBinaryScalarAsDouble(input, type, bigEndian);
        if (Number.isNaN(value)) return 0;
        return Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.trunc(value)));
    }

    private static skipBinaryScalar(input: InputStream, type: string): void {
        const bytes = ReaderPly.plyTypeSize(type);
        if (bytes > 0) {
            ReaderPly.readBytes(input, new Uint8Array(bytes));
        }
    }

    private static isFaceIndexProperty(name: string): boolean {
        return name === "vertex_indices" || name === "vertex_index";
    }

    private static addFaceTriangles(indices: readonly number[], triangles: TriangleIndices[]): void {
        for (let i = 2; i < indices.length; i++) {
            triangles.push([indices[0]!, indices[i - 1]!, indices[i]!]);
        }
    }

    private static readVertexAscii(element: PlyElement, line: string, vertices: ArrayList<Vector3Dd>): void {
        const tokens = ReaderPly.splitWhitespace(line);
        let tokenIndex = 0;
        let x = 0;
        let y = 0;
        let z = 0;

        for (const property of element.properties) {
            if (tokenIndex >= tokens.length) break;
            if ("countType" in property) {
                const count = Integer.parseInt(tokens[tokenIndex++]!);
                tokenIndex += count;
                continue;
            }
            const value = Double.parseDouble(tokens[tokenIndex++]!);
            if (property.name === "x") x = value;
            else if (property.name === "y") y = value;
            else if (property.name === "z") z = value;
        }
        vertices.add(new Vector3Dd(x, y, z));
    }

    private static readFaceAscii(element: PlyElement, line: string, triangles: TriangleIndices[]): void {
        const tokens = ReaderPly.splitWhitespace(line);
        let tokenIndex = 0;
        for (const property of element.properties) {
            if (tokenIndex >= tokens.length) break;
            if ("countType" in property) {
                const count = Integer.parseInt(tokens[tokenIndex++]!);
                const indices: number[] = [];
                for (let j = 0; j < count && tokenIndex < tokens.length; j++) {
                    const index = Integer.parseInt(tokens[tokenIndex++]!);
                    if (ReaderPly.isFaceIndexProperty(property.name)) indices.push(index);
                }
                if (ReaderPly.isFaceIndexProperty(property.name)) ReaderPly.addFaceTriangles(indices, triangles);
            } else {
                tokenIndex++;
            }
        }
    }

    private static readVertexBinary(
        input: InputStream,
        element: PlyElement,
        bigEndian: boolean,
        vertices: ArrayList<Vector3Dd>,
    ): void {
        let x = 0;
        let y = 0;
        let z = 0;
        for (const property of element.properties) {
            if ("countType" in property) {
                const count = ReaderPly.readBinaryScalarAsInt(input, property.countType, bigEndian);
                for (let j = 0; j < count; j++) ReaderPly.skipBinaryScalar(input, property.valueType);
                continue;
            }
            const value = ReaderPly.readBinaryScalarAsDouble(input, property.type, bigEndian);
            if (property.name === "x") x = value;
            else if (property.name === "y") y = value;
            else if (property.name === "z") z = value;
        }
        vertices.add(new Vector3Dd(x, y, z));
    }

    private static readFaceBinary(
        input: InputStream,
        element: PlyElement,
        bigEndian: boolean,
        triangles: TriangleIndices[],
    ): void {
        for (const property of element.properties) {
            if ("countType" in property) {
                const count = ReaderPly.readBinaryScalarAsInt(input, property.countType, bigEndian);
                const indices: number[] = [];
                for (let j = 0; j < count; j++) {
                    const index = ReaderPly.readBinaryScalarAsInt(input, property.valueType, bigEndian);
                    if (ReaderPly.isFaceIndexProperty(property.name)) indices.push(index);
                }
                if (ReaderPly.isFaceIndexProperty(property.name)) ReaderPly.addFaceTriangles(indices, triangles);
            } else {
                ReaderPly.skipBinaryScalar(input, property.type);
            }
        }
    }

    private static skipBinaryElementRecord(input: InputStream, element: PlyElement, bigEndian: boolean): void {
        for (const property of element.properties) {
            if ("countType" in property) {
                const count = ReaderPly.readBinaryScalarAsInt(input, property.countType, bigEndian);
                for (let j = 0; j < count; j++) ReaderPly.skipBinaryScalar(input, property.valueType);
            } else {
                ReaderPly.skipBinaryScalar(input, property.type);
            }
        }
    }

    private static async buildTriangleMesh(
        vertices: ArrayList<Vector3Dd>,
        triangles: readonly TriangleIndices[],
        createWorker: ReaderPlyWorkerFactory,
        options?: WorkerExecutionOptions,
    ): Promise<TriangleMesh | null> {
        if (vertices.isEmpty() || triangles.length === 0) return null;

        const coordinates = vertices.toArray().map((v) => [v.x(), v.y(), v.z()]);
        const indices = new Int32Array(triangles.length * 3);
        for (let i = 0; i < triangles.length; i++) indices.set(triangles[i]!, i * 3);

        const vertexWorker = createWorker();
        try {
            const triangleWorker = createWorker();
            try {
                const [vertexPart, trianglePart] = await Promise.all([
                    vertexWorker.execute({ kind: "vertices", coordinates }, options),
                    triangleWorker.execute({ kind: "triangles", indices, vertexCount: vertices.size() }, options),
                ]);
                if (vertexPart.kind !== "vertices" || trianglePart.kind !== "triangles") {
                    throw new IOException("Unexpected PLY mesh worker response");
                }
                if (trianglePart.indices.length === 0) return null;

                const mesh = new TriangleMesh();
                mesh.setName("PLY mesh");
                mesh.initVertexPositionsArray(vertices.size());
                mesh.getVertexPositions()!.set(vertexPart.positions);
                mesh.initTriangleArrays(trianglePart.indices.length / 3);
                mesh.getTriangleIndexes()!.set(trianglePart.indices);
                mesh.calculateNormals();
                mesh.setMaterials([ReaderPly.defaultMaterial()]);
                mesh.setMaterialRanges([[0, 0]]);
                return mesh;
            } finally {
                triangleWorker.terminate();
            }
        } finally {
            vertexWorker.terminate();
        }
    }

    private static rotateLikeObj(vertices: ArrayList<Vector3Dd>): void {
        const rotation = new Matrix4x4d().axisRotation(Math.PI / 2.0, new Vector3Dd(1, 0, 0));
        for (let i = 0; i < vertices.size(); i++) {
            vertices.set(i, rotation.multiply(vertices.get(i)));
        }
    }

    public static async importGeometry(
        input: InputStream,
        createWorker: ReaderPlyWorkerFactory,
        signal?: AbortSignal,
    ): Promise<ReaderPlyResult> {
        signal?.throwIfAborted();
        const header = ReaderPly.readHeader(input);
        const vertices = new ArrayList<Vector3Dd>();
        const triangles: TriangleIndices[] = [];
        const binary = header.format !== ReaderPly.FORMAT_ASCII;
        const bigEndian = header.format === ReaderPly.FORMAT_BINARY_BIG_ENDIAN;

        for (const element of header.elements) {
            for (let i = 0; i < element.count; i++) {
                if (binary) {
                    if (element.name === "vertex") ReaderPly.readVertexBinary(input, element, bigEndian, vertices);
                    else if (element.name === "face") ReaderPly.readFaceBinary(input, element, bigEndian, triangles);
                    else ReaderPly.skipBinaryElementRecord(input, element, bigEndian);
                } else {
                    const line = ReaderPly.readPlyAsciiLine(input) ?? "";
                    if (element.name === "vertex") ReaderPly.readVertexAscii(element, line, vertices);
                    else if (element.name === "face") ReaderPly.readFaceAscii(element, line, triangles);
                }
            }
        }

        ReaderPly.rotateLikeObj(vertices);
        const result = new ReaderPlyResult();
        result.pointCloud = vertices;
        result.triangleMesh = await ReaderPly.buildTriangleMesh(
            vertices,
            triangles,
            createWorker,
            signal === undefined ? undefined : { signal },
        );
        return result;
    }

    public static async importEnvironment(
        input: InputStream,
        scene: SimpleScene,
        createWorker: ReaderPlyWorkerFactory,
        signal?: AbortSignal,
    ): Promise<void> {
        const result = await ReaderPly.importGeometry(input, createWorker, signal);
        if (result.triangleMesh === null) {
            Logger.reportMessage(
                null,
                Logger.WARNING,
                "ReaderPly.importEnvironment",
                "PLY file contains no triangle mesh",
            );
            return;
        }
        ReaderPly.addThing(result.triangleMesh, scene.getSimpleBodies());
    }
}
