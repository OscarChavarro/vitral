export class ColorRgba {
    public constructor(
        private r = 0,
        private g = 0,
        private b = 0,
        private a = 0,
    ) {}
    public getR(): number {
        return this.r;
    }
    public getG(): number {
        return this.g;
    }
    public getB(): number {
        return this.b;
    }
    public getA(): number {
        return this.a;
    }
    public setR(v: number): void {
        this.r = v;
    }
    public setG(v: number): void {
        this.g = v;
    }
    public setB(v: number): void {
        this.b = v;
    }
    public setA(v: number): void {
        this.a = v;
    }
    public set(o: ColorRgba): void {
        this.r = o.r;
        this.g = o.g;
        this.b = o.b;
        this.a = o.a;
    }
}
