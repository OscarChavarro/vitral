import { ColorRgba } from "../common/color/ColorRgba.js";
import { MediaEntity } from "./MediaEntity.js";

/**
Port of `vsdk.toolkit.media.RGBAColorPalette`.

A list of colors, optionally with a parallel list of positions, evaluated
either by nearest entry or by linear interpolation. Java's `ColorRgba` copy
constructor has no TypeScript counterpart, so every place Java writes
`new ColorRgba(c)` spells the four channels out; the defensive copying on the
way in and on the way out is the Java one.
*/
export class RGBAColorPalette extends MediaEntity {
    private readonly colors: ColorRgba[] = [];
    private readonly positions: number[] = [];

    public init(size: number): void {
        this.colors.length = 0;
        this.positions.length = 0;
        for (let i = 0; i < size; i++) {
            this.colors.push(new ColorRgba());
        }
    }

    public size(): number {
        return this.colors.length;
    }

    public hasPositions(): boolean {
        return this.positions.length !== 0;
    }

    public getColorAt(i: number): ColorRgba | null {
        if (i < 0 || i >= this.colors.length) {
            return null;
        }
        return RGBAColorPalette.copyOf(this.colors[i]!);
    }

    public getPositionAt(i: number): number {
        if (i < 0 || i >= this.positions.length) {
            return 0.0;
        }
        return this.positions[i]!;
    }

    public setColorAt(i: number, r: ColorRgba | number, g?: number, b?: number, a?: number): void {
        if (i < 0 || i >= this.colors.length) {
            return;
        }
        this.colors[i] = r instanceof ColorRgba ? RGBAColorPalette.copyOf(r) : new ColorRgba(r, g ?? 0, b ?? 0, a ?? 0);
    }

    public addColor(r: ColorRgba | number, g?: number, b?: number, a?: number): void {
        this.colors.push(
            r instanceof ColorRgba ? RGBAColorPalette.copyOf(r) : new ColorRgba(r, g ?? 0, b ?? 0, a ?? 0),
        );
    }

    public addColorAt(position: number, c: ColorRgba): void {
        this.colors.push(RGBAColorPalette.copyOf(c));
        this.positions.push(position);
    }

    public evalNearest(t: number): ColorRgba {
        t = RGBAColorPalette.clamp01(t);
        const n: number = this.colors.length;
        if (n === 0) {
            return new ColorRgba();
        }
        let i: number = Math.trunc(t * n);
        if (i < 0) {
            i = 0;
        }
        if (i >= n) {
            i = n - 1;
        }
        return RGBAColorPalette.copyOf(this.colors[i]!);
    }

    public evalLinear(t: number): ColorRgba {
        t = RGBAColorPalette.clamp01(t);
        if (this.colors.length === 0) {
            return new ColorRgba();
        }
        if (this.colors.length === 1) {
            return RGBAColorPalette.copyOf(this.colors[0]!);
        }
        if (this.positions.length > 0 && this.positions.length === this.colors.length) {
            return this.evalPositioned(t);
        }
        const n: number = this.colors.length - 1;
        let inf: number = Math.trunc(t * n);
        let sup: number = inf + 1;
        const delta: number = 1.0 / n;
        const p: number = (t - inf * delta) / delta;

        if (inf < 0) inf = 0;
        if (inf > n) inf = n;
        if (sup < 0) sup = 0;
        if (sup > n) sup = n;

        return RGBAColorPalette.interpolate(this.colors[inf]!, this.colors[sup]!, p);
    }

    /** Java's `colorsView` answers a `List.copyOf`; this is that frozen copy. */
    public colorsView(): readonly ColorRgba[] {
        return Object.freeze(this.colors.map((c) => RGBAColorPalette.copyOf(c)));
    }

    private evalPositioned(t: number): ColorRgba {
        const n: number = this.colors.length - 1;
        for (let i = 0; i < n; i++) {
            const a: number = this.positions[i]!;
            const b: number = this.positions[i + 1]!;
            if (a === b) {
                continue;
            }
            if (t >= a && t <= b) {
                return RGBAColorPalette.interpolate(this.colors[i]!, this.colors[i + 1]!, (t - a) / (b - a));
            }
        }
        return new ColorRgba();
    }

    private static copyOf(c: ColorRgba): ColorRgba {
        return new ColorRgba(c.getR(), c.getG(), c.getB(), c.getA());
    }

    private static interpolate(a: ColorRgba, b: ColorRgba, p: number): ColorRgba {
        return new ColorRgba(
            a.getR() + (b.getR() - a.getR()) * p,
            a.getG() + (b.getG() - a.getG()) * p,
            a.getB() + (b.getB() - a.getB()) * p,
            a.getA() + (b.getA() - a.getA()) * p,
        );
    }

    private static clamp01(t: number): number {
        if (t < 0.0) return 0.0;
        if (t > 1.0) return 1.0;
        return t;
    }
}
