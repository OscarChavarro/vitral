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
    private minXBound = -1;
    private minYBound = -1;
    private minZBound = -1;
    private maxXBound = 1;
    private maxYBound = 1;
    private maxZBound = 1;
    private tesselationHintX = 10;
    private tesselationHintY = 10;
    private internalTriangleMesh = new TriangleMesh();
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
    public setBounds(
        minXBound: number,
        minYBound: number,
        minZBound: number,
        maxXBound: number,
        maxYBound: number,
        maxZBound: number,
    ): void {
        this.minXBound = minXBound;
        this.minYBound = minYBound;
        this.minZBound = minZBound;
        this.maxXBound = maxXBound;
        this.maxYBound = maxYBound;
        this.maxZBound = maxZBound;
        this.updateInternalGeometry();
    }
    public setTesselationHint(x: number, y: number): void {
        if (!Number.isInteger(x) || !Number.isInteger(y) || x <= 0 || y <= 0)
            throw new RangeError("Tessellation hints must be positive integers");
        this.tesselationHintX = x;
        this.tesselationHintY = y;
        this.updateInternalGeometry();
    }
    public getTesselationHintX(): number {
        return this.tesselationHintX;
    }
    public getTesselationHintY(): number {
        return this.tesselationHintY;
    }
    public getMinXBound(): number {
        return this.minXBound;
    }
    public getMinYBound(): number {
        return this.minYBound;
    }
    public getMinZBound(): number {
        return this.minZBound;
    }
    public getMaxXBound(): number {
        return this.maxXBound;
    }
    public getMaxYBound(): number {
        return this.maxYBound;
    }
    public getMaxZBound(): number {
        return this.maxZBound;
    }
    public getInternalTriangleMesh(): TriangleMesh {
        return this.internalTriangleMesh;
    }
    public getMinMax(): Float64Array {
        return this.internalTriangleMesh.getMinMax();
    }
    public doIntersectionFirstHit(ray: Ray, out: RayHit): boolean {
        return this.internalTriangleMesh.doIntersectionFirstHit(ray, out);
    }
    public override doExtraInformation(ray: Ray, t: number, out: RayHit): void {
        this.internalTriangleMesh.doExtraInformation(ray, t, out);
    }
    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        return this.internalTriangleMesh.doContainmentTest(point, tolerance);
    }
    private updateInternalGeometry(): void {
        const dx = (this.maxXBound - this.minXBound) / this.tesselationHintX,
            dy = (this.maxYBound - this.minYBound) / this.tesselationHintY;
        const mesh = new TriangleMesh();
        mesh.initVertexPositionsArray((this.tesselationHintX + 1) * (this.tesselationHintY + 1));
        const positions = mesh.getVertexPositions()!;
        let index = 0;
        for (let iy = 0, y = this.minYBound; iy <= this.tesselationHintY; iy++, y += dy) {
            this.xyFunction.defineValue("y", y);
            for (let ix = 0, x = this.minXBound; ix <= this.tesselationHintX; ix++, x += dx) {
                this.xyFunction.defineValue("x", x);
                let z = this.xyFunction.eval();
                z = Math.min(this.maxZBound, Math.max(this.minZBound, z));
                positions[index * 3] = x;
                positions[index * 3 + 1] = y;
                positions[index * 3 + 2] = z;
                index++;
            }
        }
        mesh.initTriangleArrays(this.tesselationHintX * this.tesselationHintY * 2);
        const triangles = mesh.getTriangleIndexes()!;
        index = 0;
        for (let iy = 0; iy < this.tesselationHintY; iy++)
            for (let ix = 0; ix < this.tesselationHintX; ix++) {
                const a = (this.tesselationHintX + 1) * iy + ix,
                    b = a + 1,
                    c = b + this.tesselationHintX + 1,
                    d = a + this.tesselationHintX + 1;
                triangles.set([a, b, c], index * 3);
                index++;
                triangles.set([a, c, d], index * 3);
                index++;
            }
        mesh.calculateNormals();
        this.internalTriangleMesh = mesh;
    }
}
