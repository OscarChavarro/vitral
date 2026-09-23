// Java basic classes
import { ArrayList } from "../../../../java/util/ArrayList.js";

// VSDK Classes
import { Entity } from "../../common/Entity.js";
import type { Background } from "../background/Background.js";
import type { Camera } from "../camera/Camera.js";
import type { CameraSnapshot } from "../camera/CameraSnapshot.js";
import type { Light } from "../light/Light.js";
import type { SimpleBody } from "./SimpleBody.js";
import { SimpleSceneSnapshot } from "./SimpleSceneSnapshot.js";

export class SimpleScene extends Entity {
    private simpleBodies: ArrayList<SimpleBody>;
    private lights: ArrayList<Light>;
    private backgrounds: ArrayList<Background>;
    private cameras: ArrayList<Camera>;
    private activeCameraIndex = 0;
    private activeBackgroundIndex = 0;

    public constructor() {
        super();
        this.simpleBodies = new ArrayList<SimpleBody>();
        this.lights = new ArrayList<Light>();
        this.backgrounds = new ArrayList<Background>();
        this.cameras = new ArrayList<Camera>();
    }

    public getActiveCameraIndex(): number {
        return this.activeCameraIndex;
    }

    public getActiveBackgroundIndex(): number {
        return this.activeBackgroundIndex;
    }

    public setActiveCameraIndex(i: number): void {
        this.activeCameraIndex = i;
    }

    public setActiveBackgroundIndex(i: number): void {
        this.activeBackgroundIndex = i;
    }

    public addBody(b: SimpleBody): void {
        this.simpleBodies.add(b);
    }

    public addCamera(c: Camera): void {
        this.cameras.add(c);
    }

    public addBackground(b: Background): void {
        this.backgrounds.add(b);
    }

    public addLight(l: Light): void {
        l.setId(this.lights.size());
        this.lights.add(l);
    }

    public getSimpleBodies(): ArrayList<SimpleBody> {
        return this.simpleBodies;
    }

    public getLights(): ArrayList<Light> {
        return this.lights;
    }

    public getBackgrounds(): ArrayList<Background> {
        return this.backgrounds;
    }

    public getCameras(): ArrayList<Camera> {
        return this.cameras;
    }

    public setSimpleBodies(simpleBodies: ArrayList<SimpleBody>): void {
        this.simpleBodies = simpleBodies;
    }

    public setLights(lights: ArrayList<Light>): void {
        this.lights = lights;
        for (let i = 0; i < this.lights.size(); i++) {
            this.lights.get(i).setId(i);
        }
    }

    public setBackgrounds(backgrounds: ArrayList<Background>): void {
        this.backgrounds = backgrounds;
    }

    public getActiveBackground(): Background {
        return this.backgrounds.get(this.activeBackgroundIndex);
    }

    public getActiveCamera(): Camera {
        return this.cameras.get(this.activeCameraIndex);
    }

    public setCameras(cameras: ArrayList<Camera>): void {
        this.cameras = cameras;
    }

    public exportToSimpleSceneSnapshot(): SimpleSceneSnapshot;
    public exportToSimpleSceneSnapshot(viewportXSize: number, viewportYSize: number): SimpleSceneSnapshot;
    public exportToSimpleSceneSnapshot(cameraSnapshot: CameraSnapshot, background: Background): SimpleSceneSnapshot;
    public exportToSimpleSceneSnapshot(a?: number | CameraSnapshot, b?: number | Background): SimpleSceneSnapshot {
        if (a === undefined) {
            return this.exportToSimpleSceneSnapshot(
                this.getActiveCamera().exportToCameraSnapshot(),
                this.getActiveBackground(),
            );
        }
        if (typeof a === "number") {
            return this.exportToSimpleSceneSnapshot(
                this.getActiveCamera().exportToCameraSnapshot(a, b as number),
                this.getActiveBackground(),
            );
        }
        return new SimpleSceneSnapshot(this.simpleBodies, this.lights, b as Background, a);
    }
}
