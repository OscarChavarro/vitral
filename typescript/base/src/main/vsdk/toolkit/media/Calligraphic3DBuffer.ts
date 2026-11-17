import { ArrayListOfDoubles } from "../common/dataStructures/ArrayListOfDoubles.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { MediaEntity } from "./MediaEntity.js";

/**
The Calligraphic3DBuffer class represents a set of 3D line segments, the
input of a vector graphics device before projection, in the same way that
`Calligraphic2DBuffer` represents its already projected 2D output.

It is the generic representation to which the 1-dimensional geometries
(i.e. curves) and the line annotations of a scene (i.e. a reference grid) are
exported to be drawn without a 3D graphics API: the lines are projected by a
camera, and can have their hidden parts removed by the quantitative
invisibility algorithm of `HiddenLineRenderer`, where they are "free lines":
they are hidden by solids, but do not hide anything.

As a media entity it is not a Geometry: it has no interpretation as a
modelled shape (no containment, no intersection), only as lines to be
drawn. Coordinates are those of the space where it was built (i.e. object or
world coordinates).
*/
export class Calligraphic3DBuffer extends MediaEntity {
    /// Values stored per line: x0, y0, z0, x1, y1, z1, R, G, B, width
    private static readonly LINE_SIZE = 10;

    private lineData: ArrayListOfDoubles;

    public constructor() {
        super();
        this.lineData = new ArrayListOfDoubles(Calligraphic3DBuffer.LINE_SIZE * 1000);
    }

    /**
    Erases all of internal calligraphy contents
    */
    public init(): void {
        this.lineData.clean();
    }

    /**
    Adds a 3D line to the calligraphic buffer.
    Java overloads `add3DLine(Vector3Dd p0, Vector3Dd p1)` and
    `add3DLine(double x0, double y0, double z0, double x1, double y1, double z1)`.
    */
    public add3DLine(p0: Vector3Dd, p1: Vector3Dd): void;
    public add3DLine(x0: number, y0: number, z0: number, x1: number, y1: number, z1: number): void;
    public add3DLine(
        a: Vector3Dd | number,
        b: Vector3Dd | number,
        z0?: number,
        x1?: number,
        y1?: number,
        z1?: number,
    ): void {
        if (a instanceof Vector3Dd) {
            const p1 = b as Vector3Dd;
            this.add3DLine(a.x(), a.y(), a.z(), p1.x(), p1.y(), p1.z());
            return;
        }
        this.lineData.add(a);
        this.lineData.add(b as number);
        this.lineData.add(z0!);
        this.lineData.add(x1!);
        this.lineData.add(y1!);
        this.lineData.add(z1!);
        this.lineData.add(0); // R
        this.lineData.add(0); // G
        this.lineData.add(0); // B
        this.lineData.add(1.0); // Width
    }

    /**
    @param i index of the line, from 0 to `getNumLines() - 1`
    @return the two end points of the line
    */
    public get3DLine(i: number): Vector3Dd[] {
        const base = Calligraphic3DBuffer.LINE_SIZE * i;
        const outP0 = new Vector3Dd(this.lineData.get(base), this.lineData.get(base + 1), this.lineData.get(base + 2));
        const outP1 = new Vector3Dd(
            this.lineData.get(base + 3),
            this.lineData.get(base + 4),
            this.lineData.get(base + 5),
        );
        return [outP0, outP1];
    }

    public getNumLines(): number {
        return Math.trunc(this.lineData.size() / Calligraphic3DBuffer.LINE_SIZE);
    }
}
