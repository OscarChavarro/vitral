import { MediaEntity } from "./MediaEntity.js";
import { ShapeDescriptor } from "./ShapeDescriptor.js";
export class GeometryMetadata extends MediaEntity {
    private static lastId = 0;
    private id: number = ++GeometryMetadata.lastId;
    private filename: string | null = null;
    private readonly descriptors: ShapeDescriptor[] = [];
    public setId(id: number): void {
        this.id = id;
        if (GeometryMetadata.lastId < id) GeometryMetadata.lastId = id;
    }
    public getId(): number {
        return this.id;
    }
    public setFilename(f: string | null): void {
        this.filename = f !== null && f.length > 0 ? f : null;
    }
    public getFilename(): string | null {
        return this.filename;
    }
    public getDescriptors(): ShapeDescriptor[] {
        return this.descriptors;
    }
    public getDescriptorByName(name: string): ShapeDescriptor | null {
        return this.descriptors.find((x) => x.getLabel() === name) ?? null;
    }
    public doMinskowskiDistance(other: GeometryMetadata, s: number, subGroup: string): number {
        const a = this.getDescriptorByName(subGroup),
            b = other.getDescriptorByName(subGroup);
        if (a === null || b === null) return Number.MAX_VALUE;
        const av = a.getFeatureVector(),
            bv = b.getFeatureVector();
        if (av.length !== bv.length) return Number.MAX_VALUE;
        let sum = 0;
        for (let i = 0; i < av.length; i++) sum += Math.pow(Math.abs(av[i]! - bv[i]!), s);
        return Math.pow(sum, 1 / s);
    }
    public override toString(): string {
        return `${this.filename}\n    . ${this.descriptors.length} shape descriptors\n${this.descriptors.map((x) => `        . ${x.constructor.name}`).join("")}`;
    }
}
