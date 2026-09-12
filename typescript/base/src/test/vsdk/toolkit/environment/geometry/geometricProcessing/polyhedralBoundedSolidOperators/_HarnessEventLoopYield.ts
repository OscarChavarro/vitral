/**
Harness accommodation, not part of the Java sources.

Vitest's worker talks to the main process over a birpc channel with a fixed
60 s timeout, and the reply can only be processed when the worker's event loop
is free. The Kurlander/bowl fixtures run long synchronous boolean sweeps, so a
single test can block the loop past that limit and the run then reports
`[vitest-worker]: Timeout calling "onTaskUpdate"` even though every assertion
passed. Awaiting a macrotask between the iterations of those sweeps hands the
loop back without changing the operations or their order.
*/
export function yieldToEventLoop(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, 0));
}
