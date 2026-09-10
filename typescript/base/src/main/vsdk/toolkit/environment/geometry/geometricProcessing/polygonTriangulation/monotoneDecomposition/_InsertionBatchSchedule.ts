export class _InsertionBatchSchedule {
    private constructor() {}
    public static mathLogStarN(n: number): number {
        let i = 0,
            value = n;
        for (; value >= 1; i++) value = Math.log2(value);
        return i - 1;
    }
    public static mathN(n: number, h: number): number {
        let value = n;
        for (let i = 0; i < h; i++) value = Math.log2(value);
        return Math.ceil(n / value);
    }
}
