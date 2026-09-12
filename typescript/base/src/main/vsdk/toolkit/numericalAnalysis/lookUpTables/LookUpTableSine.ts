/**
Port of `vsdk.toolkit.numericalAnalysis.lookUpTables.LookUpTableSine`.

A thousand-entry sine table indexed by the fractional part of a period. The
constructor that takes a decimal count rounds pi to that many decimals before
building the table, which is what makes a texture built from it reproducible
across machines regardless of the library's own pi.
*/
export class LookUpTableSine {
    private static readonly SIZE = 1000;

    private readonly table: Float64Array;

    /**
    Java has two constructors: the no-argument one uses `Math.PI`, and the
    other rounds pi to a number of decimals first. TypeScript spells that as
    one optional parameter, and `null` is the no-argument flavor.
    */
    public constructor(numberOfApproximationDecimals: number | null = null) {
        this.table = new Float64Array(LookUpTableSine.SIZE);
        let pi: number = Math.PI;
        if (numberOfApproximationDecimals !== null) {
            const scale: number = Math.pow(10.0, numberOfApproximationDecimals);
            pi = Math.round(Math.PI * scale) / scale;
        }
        for (let i = 0; i < LookUpTableSine.SIZE; i++) {
            this.table[i] = Math.sin((i / LookUpTableSine.SIZE) * (pi * 2.0));
        }
    }

    public eval(fraction: number): number {
        let index: number = Math.trunc(fraction * LookUpTableSine.SIZE);
        if (index < 0) {
            index = 0;
        } else if (index >= LookUpTableSine.SIZE) {
            index = LookUpTableSine.SIZE - 1;
        }
        return this.table[index]!;
    }
}
