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
    private simpleBodiesArray: ArrayList<SimpleBody>;
    private lightsArray: ArrayList<Light>;
    private backgroundsArray: ArrayList<Background>;
    private camerasArray: ArrayList<Camera>;
    private activeCameraIndex = 0;
    private activeBackgroundIndex = 0;

    public constructor() {
        super();
        this.simpleBodiesArray = new ArrayList<SimpleBody>();
        this.lightsArray = new ArrayList<Light>();
        this.backgroundsArray = new ArrayList<Background>();
        this.camerasArray = new ArrayList<Camera>();
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
        this.simpleBodiesArray.add(b);
    }

    public addCamera(c: Camera): void {
        this.camerasArray.add(c);
    }

    public addBackground(b: Background): void {
        this.backgroundsArray.add(b);
    }

    public addLight(l: Light): void {
        l.setId(this.lightsArray.size());
        this.lightsArray.add(l);
    }

    public getSimpleBodies(): ArrayList<SimpleBody> {
        return this.simpleBodiesArray;
    }

    public getLights(): ArrayList<Light> {
        return this.lightsArray;
    }

    public getBackgrounds(): ArrayList<Background> {
        return this.backgroundsArray;
    }

    public getCameras(): ArrayList<Camera> {
        return this.camerasArray;
    }

    public setSimpleBodies(simpleBodies: ArrayList<SimpleBody>): void {
        this.simpleBodiesArray = simpleBodies;
    }

    public setLights(lights: ArrayList<Light>): void {
        this.lightsArray = lights;
        for (let i = 0; i < this.lightsArray.size(); i++) {
            this.lightsArray.get(i).setId(i);
        }
    }

    public setBackgrounds(backgrounds: ArrayList<Background>): void {
        this.backgroundsArray = backgrounds;
    }

    public getActiveBackground(): Background {
        return this.backgroundsArray.get(this.activeBackgroundIndex);
    }

    public getActiveCamera(): Camera {
        return this.camerasArray.get(this.activeCameraIndex);
    }

    public setCameras(cameras: ArrayList<Camera>): void {
        this.camerasArray = cameras;
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
        return new SimpleSceneSnapshot(this.simpleBodiesArray, this.lightsArray, b as Background, a);
    }
}
