// Start or end, along a ray, of the influence sphere of one blob component.
export class BlobInfluenceInterval {
    private readonly starting: boolean;
    private readonly component: number;
    private readonly bound: number;

    public constructor(isStart: boolean = true, componentIndex: number = 0, distance: number = 0) {
        this.starting = isStart;
        this.component = componentIndex;
        this.bound = distance;
    }

    public isStart(): boolean {
        return this.starting;
    }

    public getComponent(): number {
        return this.component;
    }

    public getBound(): number {
        return this.bound;
    }
}
