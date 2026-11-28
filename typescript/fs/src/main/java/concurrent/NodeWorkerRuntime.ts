/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import { parentPort } from "node:worker_threads";

import {
    createWorkerMessageHandler,
    type WorkerRequest,
    type WorkerTransferValue,
} from "@vitral/base/java/concurrent/WorkerProtocol";

/**
Worker-side half of the Vitral protocol for `node:worker_threads`.

Node provides the message value directly. Wrap it in the event shape used
by the shared worker handler to keep progress, cancellation and errors
consistent across runtimes.
*/
export function installNodeWorkerRuntime<TInput extends WorkerTransferValue, TResult extends WorkerTransferValue>(
    run: (
        input: TInput,
        signal: AbortSignal,
        notify: (value: WorkerTransferValue) => void,
    ) => Promise<TResult> | TResult,
): void {
    const port = parentPort;
    if (port === null) {
        throw new Error("installNodeWorkerRuntime must be called from a worker thread");
    }
    const handle = createWorkerMessageHandler(run, (response) => port.postMessage(response));
    port.on("message", (data: WorkerRequest<TInput>) => handle({ data }));
}
