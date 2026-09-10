export class _RandomSegmentOrder {
    private static nextPermutationIndex = 1;
    private static permutation: number[] = [];
    private static seed = 1;
    private constructor() {}
    public static generateRandomOrdering(n: number): number {
        this.nextPermutationIndex = 1;
        this.permutation = Array.from({ length: n + 1 }, () => 0);
        const local = Array.from({ length: n + 1 }, (_, index) => index);
        for (let i = 1, base = 0; i <= n; i++, base++) {
            const m = (Math.floor(this.randomUnit() * 32000) % (n + 1 - i)) + 1;
            this.permutation[i] = local[base + m]!;
            if (m !== 1) local[base + m] = local[base + 1]!;
        }
        return 0;
    }
    public static chooseSegment(): number {
        return this.permutation[this.nextPermutationIndex++]!;
    }
    private static randomUnit(): number {
        this.seed = (16807 * this.seed) % 2147483647;
        return this.seed / 2147483647;
    }
}
