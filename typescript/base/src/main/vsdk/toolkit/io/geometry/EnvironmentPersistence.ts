import type { OutputStream } from "../../../../java/io/OutputStream.js";
import type { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { PersistenceElement } from "../PersistenceElement.js";
import { WriterGts } from "./WriterGts.js";
import { WriterObj } from "./WriterObj.js";
import { WriterVtk } from "./WriterVtk.js";

/**
The stream half of `vsdk.toolkit.io.geometry.EnvironmentPersistence`: the
writers of a scene, which work over an `OutputStream` on every platform.

Java's `importEnvironment(File, SimpleScene)` names a file of the file system,
so its counterpart lives with each platform: `WebEnvironmentPersistence` of
`@vitral/webgl` imports a resource named by a URL (or given as bytes).
*/
export class EnvironmentPersistence extends PersistenceElement {
    public static exportEnvironmentObj(inOutputStream: OutputStream, inScene: SimpleScene): void {
        WriterObj.exportEnvironment(inOutputStream, inScene);
    }

    public static exportEnvironmentGts(inOutputStream: OutputStream, inScene: SimpleScene): void {
        WriterGts.exportEnvironment(inOutputStream, inScene);
    }

    public static exportEnvironmentVtk(inOutputStream: OutputStream, inScene: SimpleScene): void {
        WriterVtk.exportEnvironment(inOutputStream, inScene);
    }
}
