import type { PolyhedralBoundedSolid } from "../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { RenderingElement } from "./RenderingElement.js";

/**
This class follows the Singleton design pattern. Its sole function is to
create an offline renderer for debugging PolyhedralBoundedSolid objects.

As current implementation of the offline renderer is implemented on
JOGL, it is possible this causes trouble on some platforms. In the event
of a compiling error due to unavailable JOGL libraries, this class could
be reprogrammed to return null.

Java finds the renderer by loading a class by name through the class loader,
and answers null when the class is not on the class path. A JavaScript runtime
has no class path, so a package that provides a renderer registers it here
under the Java class name with `registerPlugin`; until one does,
`createOfflineRenderer` answers null, as Java does without JOGL.
*/
export abstract class PolyhedralBoundedSolidDebugger extends RenderingElement {
    private static readonly plugins = new Map<string, () => unknown>();

    /**
    @param className the Java class name the plugin stands for
    @param factory Java's `getDeclaredConstructor().newInstance()` for it
    */
    public static registerPlugin(className: string, factory: () => unknown): void {
        PolyhedralBoundedSolidDebugger.plugins.set(className, factory);
    }

    public static createOfflineRenderer(): PolyhedralBoundedSolidDebugger | null {
        return PolyhedralBoundedSolidDebugger.loadPluginHelper(
            "vsdk.toolkit.render.jogl.Jogl2PolyhedralBoundedSolidDebugger",
        );
    }

    private static loadPluginHelper(className: string): PolyhedralBoundedSolidDebugger | null {
        try {
            const handle: (() => unknown) | undefined = PolyhedralBoundedSolidDebugger.plugins.get(className);
            if (handle === undefined) {
                throw new Error("Class not found: " + className);
            }
            const o: unknown = handle();
            if (o instanceof PolyhedralBoundedSolidDebugger) {
                return o;
            }
        } catch {
            // Class is not available... (not a problem)
        }
        return null;
    }

    public abstract execute(solid: PolyhedralBoundedSolid, filename: string): void;
}
