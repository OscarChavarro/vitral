import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { AlgebraicExpression } from "../../../common/symbolicAlgebra/AlgebraicExpression.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Surface } from "./Surface.js";
import { TriangleMesh } from "./TriangleMesh.js";

/** Explicit z=f(x,y) surface tessellated into an internal triangle mesh. */
export class FunctionalExplicitSurface extends Surface<Ray, RayHit> {
    private xyFunction = new AlgebraicExpression();
    private functionExpression: string;
    private minx = -1;
    private miny = -1;
    private minz = -1;
    private maxx = 1;
    private maxy = 1;
    private maxz = 1;
    private nx = 10;
    private ny = 10;
    private internalGeometry = new TriangleMesh();
    public constructor(expression: string) {
        super();
        this.functionExpression = expression;
        try {
            this.xyFunction.setExpression(expression);
        } catch {
            this.functionExpression = "0";
            this.xyFunction.setExpression("0");
        }
        this.updateInternalGeometry();
    }
    public getFunctionExpression(): string {
        return this.functionExpression;
    }
    public setBounds(minx: number, miny: number, minz: number, maxx: number, maxy: number, maxz: number): void {
        this.minx = minx;
        this.miny = miny;
        this.minz = minz;
        this.maxx = maxx;
        this.maxy = maxy;
        this.maxz = maxz;
        this.updateInternalGeometry();
    }
    public setTesselationHint(x: number, y: number): void {
        if (!Number.isInteger(x) || !Number.isInteger(y) || x <= 0 || y <= 0)
            throw new RangeError("Tessellation hints must be positive integers");
        this.nx = x;
        this.ny = y;
        this.updateInternalGeometry();
    }
    public getTesselationHintX(): number {
        return this.nx;
    }
    public getTesselationHintY(): number {
        return this.ny;
    }
    public getMinXBound(): number {
        return this.minx;
    }
    public getMinYBound(): number {
        return this.miny;
    }
    public getMinZBound(): number {
        return this.minz;
    }
    public getMaxXBound(): number {
        return this.maxx;
    }
    public getMaxYBound(): number {
        return this.maxy;
    }
    public getMaxZBound(): number {
        return this.maxz;
    }
    public getInternalTriangleMesh(): TriangleMesh {
        return this.internalGeometry;
    }
    public getMinMax(): Float64Array {
        return this.internalGeometry.getMinMax();
    }
    public doIntersectionFirstHit(ray: Ray, out: RayHit): boolean {
        return this.internalGeometry.doIntersectionFirstHit(ray, out);
    }
    public override doExtraInformation(ray: Ray, t: number, out: RayHit): void {
        this.internalGeometry.doExtraInformation(ray, t, out);
    }
    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        return this.internalGeometry.doContainmentTest(point, tolerance);
    }
    private updateInternalGeometry(): void {
        const dx = (this.maxx - this.minx) / this.nx,
            dy = (this.maxy - this.miny) / this.ny;
        const mesh = new TriangleMesh();
        mesh.initVertexPositionsArray((this.nx + 1) * (this.ny + 1));
        const positions = mesh.getVertexPositions()!;
        let index = 0;
        for (let iy = 0, y = this.miny; iy <= this.ny; iy++, y += dy) {
            this.xyFunction.defineValue("y", y);
            for (let ix = 0, x = this.minx; ix <= this.nx; ix++, x += dx) {
                this.xyFunction.defineValue("x", x);
                let z = this.xyFunction.eval();
                z = Math.min(this.maxz, Math.max(this.minz, z));
                positions[index * 3] = x;
                positions[index * 3 + 1] = y;
                positions[index * 3 + 2] = z;
                index++;
            }
        }
        mesh.initTriangleArrays(this.nx * this.ny * 2);
        const triangles = mesh.getTriangleIndexes()!;
        index = 0;
        for (let iy = 0; iy < this.ny; iy++)
            for (let ix = 0; ix < this.nx; ix++) {
                const a = (this.nx + 1) * iy + ix,
                    b = a + 1,
                    c = b + this.nx + 1,
                    d = a + this.nx + 1;
                triangles.set([a, b, c], index * 3);
                index++;
                triangles.set([a, c, d], index * 3);
                index++;
            }
        mesh.calculateNormals();
        this.internalGeometry = mesh;
    }
}
