// Per-ray stepping state of the height field first-hit traversal: direction
// signs, the dominant axis and the slopes between ray coordinates.
export class HeightFieldTraversal {
    private readonly isdx: number;
    private readonly isdz: number;
    private readonly xDom: boolean;
    private readonly gdx: number;
    private readonly gdy: number;
    private readonly gdz: number;
    private readonly myx: number;
    private readonly mxz: number;
    private readonly mzx: number;
    private readonly myz: number;
    private depth: number;

    public constructor(
        isdxValue: number,
        isdzValue: number,
        xDomValue: boolean,
        gdxValue: number,
        gdyValue: number,
        gdzValue: number,
        myxValue: number,
        mxzValue: number,
        mzxValue: number,
        myzValue: number,
    ) {
        this.isdx = isdxValue;
        this.isdz = isdzValue;
        this.xDom = xDomValue;
        this.gdx = gdxValue;
        this.gdy = gdyValue;
        this.gdz = gdzValue;
        this.myx = myxValue;
        this.mxz = mxzValue;
        this.mzx = mzxValue;
        this.myz = myzValue;
        this.depth = 0;
    }

    public getIsdx(): number {
        return this.isdx;
    }
    public getIsdz(): number {
        return this.isdz;
    }
    public getXDom(): boolean {
        return this.xDom;
    }
    public getGdx(): number {
        return this.gdx;
    }
    public getGdy(): number {
        return this.gdy;
    }
    public getGdz(): number {
        return this.gdz;
    }
    public getMyx(): number {
        return this.myx;
    }
    public getMxz(): number {
        return this.mxz;
    }
    public getMzx(): number {
        return this.mzx;
    }
    public getMyz(): number {
        return this.myz;
    }
    // Ray parameter of the hit found by the traversal.
    public getDepth(): number {
        return this.depth;
    }
    public setDepth(value: number): void {
        this.depth = value;
    }
}
