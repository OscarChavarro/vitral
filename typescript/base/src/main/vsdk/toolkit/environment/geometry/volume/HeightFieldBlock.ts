// Height range of one square block of height field cells, used to skip
// blocks a ray passes above or below.
export class HeightFieldBlock {
    private minimumHeight: number;
    private maximumHeight: number;

    public constructor();
    /** cpp copy constructor. */
    public constructor(other: HeightFieldBlock);
    public constructor(other?: HeightFieldBlock) {
        if (other !== undefined) {
            this.minimumHeight = other.minimumHeight;
            this.maximumHeight = other.maximumHeight;
            return;
        }
        this.minimumHeight = 65536.0;
        this.maximumHeight = 0.0;
    }

    public getMinimumHeight(): number {
        return this.minimumHeight;
    }

    public getMaximumHeight(): number {
        return this.maximumHeight;
    }

    public include(height: number): void {
        if (height < this.minimumHeight) this.minimumHeight = height;
        if (height > this.maximumHeight) this.maximumHeight = height;
    }
}
