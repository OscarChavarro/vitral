import { MediaEntity } from "./MediaEntity.js";
export abstract class ShapeDescriptor extends MediaEntity {
    protected label: string;
    public constructor(label: string) {
        super();
        this.label = label;
    }
    public getLabel(): string {
        return this.label;
    }
    public setLabel(label: string): void {
        this.label = label;
    }
    public abstract getFeatureVector(): Float64Array;
    public abstract setFeatureVector(vector: Float64Array | number[]): void;
}
