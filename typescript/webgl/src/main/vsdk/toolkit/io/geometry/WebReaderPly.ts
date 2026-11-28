import {
    ByteArrayInputStream,
    ReaderPly,
    type ReaderPlyResult,
    type ReaderPlyWorkerFactory,
    type SimpleScene,
} from "@vitral/base";

/** Browser adapter. The application supplies the factory that launches its bundled worker entry. */
export class WebReaderPly {
    public static importGeometry(
        contents: Uint8Array,
        createWorker: ReaderPlyWorkerFactory,
        signal?: AbortSignal,
    ): Promise<ReaderPlyResult> {
        return ReaderPly.importGeometry(new ByteArrayInputStream(contents), createWorker, signal);
    }

    public static importEnvironment(
        contents: Uint8Array,
        scene: SimpleScene,
        createWorker: ReaderPlyWorkerFactory,
        signal?: AbortSignal,
    ): Promise<void> {
        return ReaderPly.importEnvironment(new ByteArrayInputStream(contents), scene, createWorker, signal);
    }
}
