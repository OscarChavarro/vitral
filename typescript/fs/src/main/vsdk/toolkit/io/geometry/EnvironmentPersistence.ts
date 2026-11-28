/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
// VSDK Classes
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { File } from "../../../../java/io/File.js";
import { PersistenceElement } from "../PersistenceElement.js";
import { ReaderObj } from "./ReaderObj.js";
import { ReaderPly } from "./ReaderPly.js";

/**
Format dispatcher for scene import, as in the Java original.

The asynchronous entry point also reads PLY and waits for its mesh workers.
The synchronous entry point serves callers that parse OBJ inline.
*/
export class EnvironmentPersistence extends PersistenceElement {
    public static importEnvironment(inSceneFileFd: File, inoutScene: SimpleScene): void {
        const type = EnvironmentPersistence.extractExtensionFromFile(inSceneFileFd).toLowerCase();

        if (type === "obj") {
            ReaderObj.importEnvironment(inSceneFileFd, inoutScene);
        }
    }

    public static async importEnvironmentAsync(file: File, scene: SimpleScene, signal?: AbortSignal): Promise<void> {
        const type = EnvironmentPersistence.extractExtensionFromFile(file).toLowerCase();
        if (type === "ply") {
            await ReaderPly.importEnvironment(file, scene, signal);
        } else {
            EnvironmentPersistence.importEnvironment(file, scene);
        }
    }
}
