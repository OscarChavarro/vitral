import { ReaderPly as StreamReaderPly } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderPly";
import type { ReaderPlyResult } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderPlyResult";
import type {
    ReaderPlyMeshRequest,
    ReaderPlyMeshResult,
} from "@vitral/base/vsdk/toolkit/io/geometry/ReaderPlyMeshWorker";
import type { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { NodeWorkerExecutor } from "../../../../java/concurrent/NodeWorkerExecutor.js";
import type { File } from "../../../../java/io/File.js";
import { FileInputStream } from "../../../../java/io/FileInputStream.js";
import { BufferedInputStream } from "@vitral/base/java/io/BufferedInputStream";

export class ReaderPly {
    private static createWorker(): NodeWorkerExecutor<ReaderPlyMeshRequest, ReaderPlyMeshResult> {
        return new NodeWorkerExecutor(new URL("./ReaderPlyWorker.js", import.meta.url));
    }

    public static async importGeometry(file: File, signal?: AbortSignal): Promise<ReaderPlyResult> {
        const stream = new BufferedInputStream(new FileInputStream(file.getPath().toCString()));
        try {
            return await StreamReaderPly.importGeometry(stream, ReaderPly.createWorker, signal);
        } finally {
            stream.close();
        }
    }

    public static async importEnvironment(file: File, scene: SimpleScene, signal?: AbortSignal): Promise<void> {
        const stream = new BufferedInputStream(new FileInputStream(file.getPath().toCString()));
        try {
            await StreamReaderPly.importEnvironment(stream, scene, ReaderPly.createWorker, signal);
        } finally {
            stream.close();
        }
    }
}
