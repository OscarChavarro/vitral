import { BrowserWorkerExecutor, type WorkerTransferValue } from "@vitral/base";

type SumInput = { readonly values: Int32Array; readonly [key: string]: WorkerTransferValue };

export function createRandomValues(count: number): Int32Array {
    if (!Number.isInteger(count) || count < 0) throw new RangeError("count must be a non-negative integer");
    const values = new Int32Array(count);
    for (let i = 0; i < values.length; i++) values[i] = Math.floor(Math.random() * 100) + 1;
    return values;
}

export async function sumWithWorkers(
    values: Int32Array,
    workers = navigator.hardwareConcurrency || 1,
): Promise<number> {
    const count = Math.max(1, Math.min(workers, values.length || 1));
    const executors = Array.from(
        { length: count },
        () =>
            new BrowserWorkerExecutor<SumInput, number>(
                new Worker(new URL("./sum.worker.js", import.meta.url), { type: "module" }),
            ),
    );
    try {
        const jobs = executors.map((executor, workerIndex) => {
            const start = Math.floor((values.length * workerIndex) / count);
            const end = Math.floor((values.length * (workerIndex + 1)) / count);
            const chunk = values.slice(start, end);
            return executor.execute({ values: chunk }, { transfer: [chunk.buffer] });
        });
        return (await Promise.all(jobs)).reduce((total, partial) => total + partial, 0);
    } finally {
        for (const executor of executors) executor.terminate();
    }
}

const count = Number(new URLSearchParams(window.location.search).get("n") ?? "10000000");
const values = createRandomValues(count);
const startedAt = performance.now();
sumWithWorkers(values).then((sum) =>
    console.log({
        count,
        workers: navigator.hardwareConcurrency || 1,
        sum,
        elapsedMilliseconds: performance.now() - startedAt,
    }),
);
