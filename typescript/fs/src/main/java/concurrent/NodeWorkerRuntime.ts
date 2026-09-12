/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import { parentPort } from "node:worker_threads";

import {
    type WorkerFailure,
    type WorkerRequest,
    type WorkerResponse,
    type WorkerTransferValue,
    workerError,
} from "@vitral/base/java/concurrent/WorkerProtocol";

/**
Worker-side half of the Vitral protocol for `node:worker_threads`.

`createWorkerMessageHandler` in `@vitral/base` is written against the browser
`MessageEvent` shape; a Node worker receives the bare message value instead, so
this installs the same state machine over `parentPort`. Cancellation stays
cooperative through an `AbortSignal`, and a computation may emit progress
notices before it settles.
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
    const controllers = new Map<string, AbortController>();
    const post = (response: WorkerResponse<TResult>): void => {
        port.postMessage(response);
    };
    port.on("message", (request: WorkerRequest<TInput>) => {
        if (request.kind === "cancel") {
            controllers.get(request.id)?.abort();
            return;
        }
        if (request.payload === undefined) {
            const error: WorkerFailure["error"] = {
                name: "TypeError",
                message: "Run request has no payload",
            };
            post({ id: request.id, ok: false, error });
            return;
        }
        const controller = new AbortController();
        controllers.set(request.id, controller);
        const notify = (value: WorkerTransferValue): void => {
            if (!controller.signal.aborted) post({ id: request.id, ok: "notice", value });
        };
        void Promise.resolve(run(request.payload, controller.signal, notify))
            .then(
                (value) => {
                    if (!controller.signal.aborted) post({ id: request.id, ok: true, value });
                },
                (error: unknown) => {
                    if (!controller.signal.aborted) post({ id: request.id, ok: false, error: workerError(error) });
                },
            )
            .finally(() => controllers.delete(request.id));
    });
}
