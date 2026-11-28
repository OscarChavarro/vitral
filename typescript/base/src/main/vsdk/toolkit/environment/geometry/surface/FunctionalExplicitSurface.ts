import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { AlgebraicExpression } from "../../../common/symbolicAlgebra/AlgebraicExpression.js";
import { AlgebraicExpressionException } from "../../../common/symbolicAlgebra/AlgebraicExpressionException.js";
import { Logger } from "../../../common/logging/Logger.js";
import { VSDK } from "../../../common/VSDK.js";
import type { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { QuadMesh } from "./QuadMesh.js";
import { Surface } from "./Surface.js";
import { TriangleMesh } from "./TriangleMesh.js";

/** Explicit z=f(x,y) surface tessellated into an internal triangle mesh. */
export class FunctionalExplicitSurface extends Surface<Ray, RayHit> {
    private xyFunction!: AlgebraicExpression;
    private functionExpression!: string;
    private minXBound!: number;
    private minYBound!: number;
    private minZBound!: number;
    private maxXBound!: number;
    private maxYBound!: number;
    private maxZBound!: number;
    private tesselationHintX!: number;
    private tesselationHintY!: number;
    private internalTriangleMesh!: TriangleMesh;

    public constructor(fxy: string) {
        super();
        this.init(fxy);
    }

    private init(fxy: string): void {
        this.functionExpression = fxy;
        this.xyFunction = new AlgebraicExpression();
        try {
            this.xyFunction.setExpression(fxy);
        } catch (e) {
            if (!(e instanceof AlgebraicExpressionException)) throw e;
            Logger.reportMessage(
                this,
                VSDK.WARNING,
                "constructor",
                'Cannot create algebraic expression for "' + fxy + '":\n' + e,
            );
            try {
                this.xyFunction.setExpression("0");
            } catch (e2) {
                if (!(e2 instanceof AlgebraicExpressionException)) throw e2;
                Logger.reportMessage(
                    this,
                    VSDK.FATAL_ERROR,
                    "constructor",
                    "So bad. Something is wrong with algebraic expressions!:\n" + e2,
                );
            }
        }
        this.minXBound = this.minYBound = this.minZBound = -1.0;
        this.maxXBound = this.maxYBound = this.maxZBound = 1.0;
        this.tesselationHintX = 10;
        this.tesselationHintY = 10;
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

    public setTesselationHint(tesx: number, tesy: number): void {
        this.tesselationHintX = tesx;
        this.tesselationHintY = tesy;
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

    private updateInternalGeometry(): void {
        //-----------------------------------------------------------------
        // Size of each tile in x direction
        const dx = (this.maxXBound - this.minXBound) / this.tesselationHintX;
        // Size of each tile in y direction
        const dy = (this.maxYBound - this.minYBound) / this.tesselationHintY;
        // Temporary variable
        let x: number;
        let y: number;
        let ix: number;
        let iy: number;
        let index: number;

        this.internalTriangleMesh = new TriangleMesh();

        //-----------------------------------------------------------------
        this.internalTriangleMesh.initVertexPositionsArray((this.tesselationHintX + 1) * (this.tesselationHintY + 1));
        let z: number;
        const v = this.internalTriangleMesh.getVertexPositions()!;

        try {
            index = 0;
            for (iy = 0, y = this.minYBound; iy <= this.tesselationHintY; iy++, y += dy) {
                this.xyFunction.defineValue("y", y);
                for (ix = 0, x = this.minXBound; ix <= this.tesselationHintX; ix++, x += dx) {
                    this.xyFunction.defineValue("x", x);
                    z = this.xyFunction.eval();
                    if (z > this.maxZBound) {
                        z = this.maxZBound;
                    }
                    if (z < this.minZBound) {
                        z = this.minZBound;
                    }
                    v[3 * index] = x;
                    v[3 * index + 1] = y;
                    v[3 * index + 2] = z;
                    index++;
                }
            }
        } catch (e) {
            if (!(e instanceof AlgebraicExpressionException)) throw e;
            Logger.reportMessage(this, VSDK.WARNING, "constructor", "Cannot evaluate algebraic expression!" + e);
            return;
        }

        //-----------------------------------------------------------------
        this.internalTriangleMesh.initTriangleArrays(this.tesselationHintX * this.tesselationHintY * 2);
        index = 0;
        const t = this.internalTriangleMesh.getTriangleIndexes()!;
        const hx = this.tesselationHintX;
        const hy = this.tesselationHintY;
        for (iy = 0; iy < hy; iy++) {
            for (ix = 0; ix < hx; ix++) {
                t[3 * index] = this.coord(hx, hy, ix, iy);
                t[3 * index + 1] = this.coord(hx, hy, ix + 1, iy);
                t[3 * index + 2] = this.coord(hx, hy, ix + 1, iy + 1);
                index++;
                t[3 * index] = this.coord(hx, hy, ix, iy);
                t[3 * index + 1] = this.coord(hx, hy, ix + 1, iy + 1);
                t[3 * index + 2] = this.coord(hx, hy, ix, iy + 1);
                index++;
            }
        }

        //-----------------------------------------------------------------
        this.internalTriangleMesh.calculateNormals();
    }

    public getInternalTriangleMesh(): TriangleMesh {
        return this.internalTriangleMesh;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): Float64Array | number[] {
        return this.internalTriangleMesh.getMinMax();
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.
    \todo  Should not delegate work over tesselated geometry version. Should
    evaluate directly from algebraic function surface!
    */
    public doIntersectionFirstHit(inOutRay: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit): Ray | null | boolean {
        if (outHit === undefined) {
            const hit = new RayHit();
            if (this.internalTriangleMesh.doIntersectionFirstHit(inRay, hit)) {
                return hit.getRay();
            }
            return null;
        }
        return this.internalTriangleMesh.doIntersectionFirstHit(inRay, outHit);
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    */
    public override doExtraInformation(inRay: Ray, inT: number, outData: RayHit): void {
        const hit = new RayHit();
        if (this.internalTriangleMesh.doIntersectionFirstHit(inRay.withT(inT), hit)) {
            outData.clone(hit);
        }
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doContainmentTest.
    @return INSIDE, OUTSIDE or LIMIT constant value
    */
    public override doContainmentTest(p: Vector3Dd, distanceTolerance: number): number {
        return this.internalTriangleMesh.doContainmentTest(p, distanceTolerance);
    }

    /**
    Java private helper `coord`: index of the vertex (ix, iy) of the grid.
    */
    private coord(tesselationHintX: number, _tesselationHintY: number, ix: number, iy: number): number {
        return (tesselationHintX + 1) * iy + ix;
    }

    /**
    Exports the tessellation of the surface as a mesh of quadrilaterals: the
    same grid of `getTesselationHintX()` x `getTesselationHintY()` cells of
    the internal triangle mesh, without the diagonals that split each cell
    in two triangles, so the lines of the mesh are the lines of constant x
    and y of the surface.
    @return a new mesh, or null if the surface has no valid tessellation
    (i.e. its function could not be evaluated)
    */
    public exportToQuadMesh(): QuadMesh | null {
        if (
            this.internalTriangleMesh === null ||
            this.internalTriangleMesh.getVertexPositions() === null ||
            this.internalTriangleMesh.getTriangleIndexes() === null
        ) {
            return null;
        }
        const source = this.internalTriangleMesh.getVertexPositions()!;
        const n = (this.tesselationHintX + 1) * (this.tesselationHintY + 1);
        const mesh = new QuadMesh();
        let ix: number;
        let iy: number;
        let index: number;

        if (source.length < 3 * n) {
            return null;
        }
        mesh.initVertexPositionsArray(n);
        mesh.getVertexPositions()!.set(source.subarray(0, 3 * n), 0);
        mesh.initQuadArrays(this.tesselationHintX * this.tesselationHintY);
        index = 0;
        for (iy = 0; iy < this.tesselationHintY; iy++) {
            for (ix = 0; ix < this.tesselationHintX; ix++) {
                mesh.setQuadAt(
                    index,
                    this.coord(this.tesselationHintX, this.tesselationHintY, ix, iy),
                    this.coord(this.tesselationHintX, this.tesselationHintY, ix + 1, iy),
                    this.coord(this.tesselationHintX, this.tesselationHintY, ix + 1, iy + 1),
                    this.coord(this.tesselationHintX, this.tesselationHintY, ix, iy + 1),
                );
                index++;
            }
        }
        return mesh;
    }
}
