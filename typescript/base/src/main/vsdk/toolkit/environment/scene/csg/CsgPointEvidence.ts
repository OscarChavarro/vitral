import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import type { CsgOperandView } from "./CsgOperandView.js";
import { CsgPointWitness } from "./CsgPointWitness.js";

// A leaf fact and the coordinates/tolerance at which it was certified.
export class CsgPointEvidence {
    private readonly path: CsgPointWitness;
    private readonly equivalentPath: CsgPointWitness;
    private equivalence: boolean;
    private classification: GeometryContainment;
    private point: Vector3Dd;
    private tolerance: number;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgPointEvidence);
    public constructor(path: CsgPointWitness, classification: GeometryContainment, point: Vector3Dd, tolerance: number);
    // Records a proven identity between two sibling solid views; path is the
    // source of the certified classification, equivalentPath its recipient.
    public constructor(source: CsgPointWitness, equivalent: CsgPointWitness, point: Vector3Dd, tolerance: number);
    public constructor(
        p?: CsgPointEvidence | CsgPointWitness,
        c?: GeometryContainment | CsgPointWitness,
        q?: Vector3Dd,
        t?: number,
    ) {
        this.path = new CsgPointWitness();
        this.equivalentPath = new CsgPointWitness();
        this.equivalence = false;
        this.classification = GeometryContainment.Indeterminate;
        this.point = new Vector3Dd();
        this.tolerance = 0;
        if (p instanceof CsgPointEvidence) {
            this.assign(p);
        } else if (p !== undefined) {
            this.path.assign(p);
            if (c instanceof CsgPointWitness) {
                this.equivalentPath.assign(c);
                this.equivalence = true;
            } else this.classification = c!;
            this.point = q!;
            this.tolerance = t!;
        }
    }

    /** C++ copy assignment. */
    public assign(other: CsgPointEvidence): CsgPointEvidence {
        if (this !== other) {
            this.path.assign(other.path);
            this.equivalentPath.assign(other.equivalentPath);
            this.equivalence = other.equivalence;
            this.classification = other.classification;
            this.point = other.point;
            this.tolerance = other.tolerance;
        }
        return this;
    }

    public getPath(): CsgPointWitness {
        return this.path;
    }

    public getClassification(): GeometryContainment {
        return this.classification;
    }

    public getPoint(): Vector3Dd {
        return this.point;
    }

    public getTolerance(): number {
        return this.tolerance;
    }

    public isEquivalence(): boolean {
        return this.equivalence;
    }

    public getEquivalentPath(): CsgPointWitness {
        return this.equivalentPath;
    }

    public prepend(operand: CsgOperandView | null): boolean {
        return this.path.prepend(operand) && (!this.equivalence || this.equivalentPath.prepend(operand));
    }

    public reverseOrientation(): void {
        this.path.reverseOrientation();
        if (this.equivalence) this.equivalentPath.reverseOrientation();
    }
}
