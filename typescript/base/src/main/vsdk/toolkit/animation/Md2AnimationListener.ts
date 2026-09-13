import type { Md2Mesh } from "../environment/geometry/surface/Md2Mesh.js";
import { AnimationListener } from "./AnimationListener.js";
import type { AnimationEvent } from "./AnimationEvent.js";

/**
Port of `vsdk.toolkit.animation.Md2AnimationListener`.

The listener that turns wall-clock time into MD2 animation time: every tick it
copies the event's elapsed seconds into the mesh, and the renderer turns that
into a frame index and an interpolation parameter. Java narrows the event's
`double` to the mesh's `float`; TypeScript has one number type, so the value
arrives unnarrowed, which is the only difference.
*/
export class Md2AnimationListener extends AnimationListener {
    public constructor(private readonly md2Mesh: Md2Mesh | null) {
        super();
    }

    public override tick(e: AnimationEvent): void {
        // Vector3Dd p = new Vector3Dd(e.getT()*10.0, 0.0, 1.0);

        if (this.md2Mesh !== null) {
            //model.setAnimationTestCursorPosition(p);
            this.md2Mesh.setElapsedTimeSeg(e.getT());
        }
    }
}
