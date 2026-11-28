// VSDK classes
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import type { Ray } from "../../environment/geometry/element/Ray.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import { InfinitePlane } from "../../environment/geometry/surface/InfinitePlane.js";
import { Controller } from "../Controller.js";
import type { MouseEvent } from "../MouseEvent.js";
import type { PercentageWheelWidget } from "./PercentageWheelWidget.js";

export class PercentageWheelWidgetController extends Controller {
    private readonly controlledWidget: PercentageWheelWidget;

    public constructor(controlledWidget: PercentageWheelWidget) {
        super();
        this.controlledWidget = controlledWidget;
    }

    public processMousePressedEvent(e: MouseEvent, c: Camera): boolean {
        let i: number;

        i = this.selectSector(e.getX(), e.getY(), c);

        if (i === -1) {
            return false;
        }

        this.controlledWidget.setSelectedSector(i);

        return true;
    }

    public processMouseReleasedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public processMouseClickedEvent(_e: MouseEvent): boolean {
        return false;
    }

    public processMouseMovedEvent(e: MouseEvent, c: Camera): boolean {
        let i: number;

        i = this.selectSector(e.getX(), e.getY(), c);

        if (i === -1) {
            return false;
        }

        this.controlledWidget.setHighligtedSector(i);

        return true;
    }

    private selectSector(x: number, y: number, c: Camera): number {
        let i: number;
        const p: Vector3Dd = this.controlledWidget.getPosition();
        let ray: Ray;
        c.updateVectors();
        ray = c.generateRay(x, y);
        let plane: InfinitePlane;
        plane = new InfinitePlane(new Vector3Dd(0, 0, 1), p);
        const hit: Ray | null = plane.doIntersectionFirstHit(ray);
        if (hit === null) {
            return -1;
        }

        let inPlane: Vector3Dd;
        let r: number;
        let angle: number;

        inPlane = hit.getOrigin().add(hit.getDirection().multiply(hit.getT())).subtract(p);
        r = inPlane.length();
        angle = JavaMath.toDegrees(inPlane.obtainSphericalThetaAngle());

        r = r / this.controlledWidget.getScale();

        if (r < this.controlledWidget.getInnerRadius()) {
            // User has click inside label center, nothing happens
            return -1;
        } else if (r > this.controlledWidget.getOuterRadius() + this.controlledWidget.getBorderWidth()) {
            // User has click outside wheel, nothing happens
            return -1;
        }

        let N: number;
        N = this.controlledWidget.getDataset().getDoubles().size();

        // Java's `(int)Math.floor(...)` narrows a double to int
        i = Math.trunc(Math.floor(angle * (N / 360.0)));
        return i;
    }

    /**
     * @return the controlledWidget
     */
    public getControlledWidget(): PercentageWheelWidget {
        return this.controlledWidget;
    }
}
