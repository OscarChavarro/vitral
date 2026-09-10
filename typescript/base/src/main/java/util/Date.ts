/** Mutable instant compatible with the relevant java.util.Date contract. */
export class Date {
    private timeValue: number;

    public constructor(time = globalThis.Date.now()) {
        if (!Number.isFinite(time)) throw new RangeError("Invalid epoch milliseconds");
        this.timeValue = globalThis.Math.trunc(time);
    }

    public getTime(): number {
        return this.timeValue;
    }
    public setTime(time: number): void {
        if (!Number.isFinite(time)) throw new RangeError("Invalid epoch milliseconds");
        this.timeValue = globalThis.Math.trunc(time);
    }
    public before(other: Date): boolean {
        return this.timeValue < other.timeValue;
    }
    public after(other: Date): boolean {
        return this.timeValue > other.timeValue;
    }
    public compareTo(other: Date): number {
        return this.timeValue < other.timeValue ? -1 : this.timeValue > other.timeValue ? 1 : 0;
    }
    public equals(other: unknown): boolean {
        return other instanceof Date && this.timeValue === other.timeValue;
    }
    public hashCode(): number {
        return (this.timeValue ^ (this.timeValue / 0x1_0000_0000)) | 0;
    }
    public clone(): Date {
        return new Date(this.timeValue);
    }
}
