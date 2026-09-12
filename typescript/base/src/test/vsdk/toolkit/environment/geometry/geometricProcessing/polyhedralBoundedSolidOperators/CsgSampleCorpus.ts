/**
Names the boolean fixture corpus used by regression and diagnostic tests.

<p>Traceability: [MANT1988] Ch. 15 boolean set-operation examples and
project-local fixtures derived from the book's CSG discussion.</p>
 */
export class CsgSampleCorpus {
    public static readonly HOLLOW_BRICK = new CsgSampleCorpus("HOLLOW_BRICK", 1);
    public static readonly MANT1986_2 = new CsgSampleCorpus("MANT1986_2", 2);
    public static readonly STACKED_BLOCKS = new CsgSampleCorpus("STACKED_BLOCKS", 3);
    public static readonly CROSS_PAIR = new CsgSampleCorpus("CROSS_PAIR", 4);
    public static readonly MOON_BLOCK = new CsgSampleCorpus("MOON_BLOCK", 5);
    public static readonly MANT1988_6_13 = new CsgSampleCorpus("MANT1988_6_13", 6);
    public static readonly MANT1988_3 = new CsgSampleCorpus("MANT1988_3", 7);
    public static readonly MANT1988_15_2_HOLED = new CsgSampleCorpus("MANT1988_15_2_HOLED", 8);
    public static readonly MANT1988_15_1 = new CsgSampleCorpus("MANT1988_15_1", 9);

    private constructor(
        private readonly nameValue: string,
        private readonly idValue: number,
    ) {}

    public id(): number {
        return this.idValue;
    }

    public name(): string {
        return this.nameValue;
    }

    public toString(): string {
        return this.nameValue;
    }

    public static values(): CsgSampleCorpus[] {
        return [
            CsgSampleCorpus.HOLLOW_BRICK,
            CsgSampleCorpus.MANT1986_2,
            CsgSampleCorpus.STACKED_BLOCKS,
            CsgSampleCorpus.CROSS_PAIR,
            CsgSampleCorpus.MOON_BLOCK,
            CsgSampleCorpus.MANT1988_6_13,
            CsgSampleCorpus.MANT1988_3,
            CsgSampleCorpus.MANT1988_15_2_HOLED,
            CsgSampleCorpus.MANT1988_15_1,
        ];
    }
}
