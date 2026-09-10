import { Quaterniond } from "../../common/linealAlgebra/Quaterniond.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { PresentationElement } from "../PresentationElement.js";

/** A 6-DoF pose update emitted by a tangible-marker tracking service. */
export class TangibleInterfaceEvent extends PresentationElement {
    public constructor(
        private readonly id: string,
        private readonly position: Vector3Dd,
        private readonly rotation: Quaterniond,
    ) {
        super();
    }
    public getId(): string {
        return this.id;
    }
    public getPosition(): Vector3Dd {
        return this.position;
    }
    public getRotation(): Quaterniond {
        return this.rotation;
    }
    public override toString(): string {
        return `TangibleInterfaceEvent{id=${this.id}, position=${this.position}, rotation=${this.rotation}}`;
    }
}
