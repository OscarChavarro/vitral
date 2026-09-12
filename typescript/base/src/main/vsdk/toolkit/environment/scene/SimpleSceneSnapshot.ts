import { ArrayList } from "../../../../java/util/ArrayList.js";
import { Collections } from "../../../../java/util/Collections.js";
import type { List } from "../../../../java/util/List.js";
import { Objects } from "../../../../java/util/Objects.js";

import { Entity } from "../../common/Entity.js";
import type { Background } from "../background/Background.js";
import type { CameraSnapshot } from "../camera/CameraSnapshot.js";
import type { Light } from "../light/Light.js";
import type { SimpleBody } from "./SimpleBody.js";

/**
Immutable scene container used by the raytracer for a consistent render pass.
*/
export class SimpleSceneSnapshot extends Entity {
    private readonly simpleBodies: List<SimpleBody>;
    private readonly lights: List<Light>;
    private readonly background: Background;
    private readonly cameraSnapshot: CameraSnapshot;

    public constructor(
        simpleBodies: List<SimpleBody>,
        lights: List<Light>,
        background: Background,
        cameraSnapshot: CameraSnapshot,
    ) {
        super();
        this.simpleBodies = Collections.unmodifiableList(
            new ArrayList<SimpleBody>(Objects.requireNonNull(simpleBodies, "simpleBodies cannot be null")),
        );
        this.lights = Collections.unmodifiableList(
            new ArrayList<Light>(Objects.requireNonNull(lights, "lights cannot be null")),
        );
        this.background = Objects.requireNonNull(background, "background cannot be null");
        this.cameraSnapshot = Objects.requireNonNull(cameraSnapshot, "cameraSnapshot cannot be null");
    }

    public getSimpleBodies(): List<SimpleBody> {
        return this.simpleBodies;
    }

    public getLights(): List<Light> {
        return this.lights;
    }

    public getBackground(): Background {
        return this.background;
    }

    public getCameraSnapshot(): CameraSnapshot {
        return this.cameraSnapshot;
    }
}
