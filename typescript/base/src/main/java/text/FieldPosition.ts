/** Mutable field range used by Java format APIs. */
export class FieldPosition {
    private beginIndexValue = 0;
    private endIndexValue = 0;
    public constructor(private readonly field = 0) {}
    public getField(): number {
        return this.field;
    }
    public getBeginIndex(): number {
        return this.beginIndexValue;
    }
    public getEndIndex(): number {
        return this.endIndexValue;
    }
    public setBeginIndex(value: number): void {
        this.beginIndexValue = value;
    }
    public setEndIndex(value: number): void {
        this.endIndexValue = value;
    }
}
