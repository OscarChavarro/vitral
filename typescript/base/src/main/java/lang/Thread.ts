import { type WorkerExecutor, type WorkerTransferValue } from "../concurrent/WorkerProtocol.js";
/** Worker-backed thread handle. Computation is supplied by a Web Worker executor, never a Node-only thread. */
export class Thread<TInput extends WorkerTransferValue, TResult extends WorkerTransferValue> {
    private result: Promise<TResult> | undefined;
    public constructor(
        private readonly executor: WorkerExecutor<TInput, TResult>,
        private readonly input: TInput,
    ) {}
    public start(): void {
        if (this.result !== undefined) throw new Error("Thread already started");
        this.result = this.executor.execute(this.input);
    }
    public join(): Promise<TResult> {
        if (this.result === undefined) throw new Error("Thread has not been started");
        return this.result;
    }
    public interrupt(): void {
        this.executor.terminate();
    }
}
