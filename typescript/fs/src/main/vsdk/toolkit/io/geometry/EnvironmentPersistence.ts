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

/**
Format dispatcher for scene import, as in the Java original.

Only the `obj` branch is wired so far: of the eight readers the Java class
reaches (obj, 3ds, gts, ply, ase, wrl/gz, vtk and bin), `ReaderObj` is the
only one ported to TypeScript to date. The remaining extensions therefore
fall through and leave the scene untouched, which is exactly what Java does
for any extension it does not recognize. Each reader gets its branch back as
it is ported.
*/
export class EnvironmentPersistence extends PersistenceElement {
    public static importEnvironment(inSceneFileFd: File, inoutScene: SimpleScene): void {
        const type = EnvironmentPersistence.extractExtensionFromFile(inSceneFileFd).toLowerCase();

        if (type === "obj") {
            ReaderObj.importEnvironment(inSceneFileFd, inoutScene);
        }
    }
}
