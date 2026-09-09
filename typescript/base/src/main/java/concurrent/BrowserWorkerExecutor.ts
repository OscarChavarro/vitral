import {
  type WorkerExecutionOptions,
  type WorkerExecutor,
  type WorkerRequest,
  type WorkerResponse,
  type WorkerTransferValue
} from "./WorkerProtocol.js";

/**
 * A one-request-at-a-time executor over a browser Web Worker.  It deliberately
 * communicates only structured-clone values so the same request protocol can
 * be implemented by a standalone runtime without exposing browser state to
 * computation code.
 */
export class BrowserWorkerExecutor<TInput extends WorkerTransferValue, TResult extends WorkerTransferValue>
  implements WorkerExecutor<TInput, TResult> {
  private nextRequestId = 1;
  private activeRequest: {
    readonly id: string;
    readonly resolve: (value: TResult) => void;
    readonly reject: (reason: unknown) => void;
    readonly abortListener?: () => void;
    readonly abortSignal?: AbortSignal;
  } | undefined;
  private terminated = false;

  public constructor(private readonly worker: Worker) {
    worker.addEventListener("message", this.onMessage);
    worker.addEventListener("error", this.onError);
    worker.addEventListener("messageerror", this.onMessageError);
  }

  public execute(input: TInput, options?: WorkerExecutionOptions): Promise<TResult> {
    if (this.terminated) return Promise.reject(new Error("This worker executor has been terminated"));
    if (this.activeRequest !== undefined) {
      return Promise.reject(new Error("This worker executor already has an active request"));
    }
    if (options?.signal?.aborted === true) {
      return Promise.reject(new DOMException("The operation was aborted", "AbortError"));
    }

    const id = globalThis.String(this.nextRequestId++);
    return new Promise<TResult>((resolve, reject) => {
      const abortListener = (): void => {
        this.post({ id, kind: "cancel" });
        this.finish(id, reject, new DOMException("The operation was aborted", "AbortError"));
      };
      this.activeRequest = {
        id,
        resolve,
        reject,
        ...(options?.signal === undefined ? {} : { abortListener, abortSignal: options.signal })
      };
      options?.signal?.addEventListener("abort", abortListener, { once: true });
      this.post({ id, kind: "run", payload: input }, options?.transfer);
    });
  }

  public terminate(): void {
    if (this.terminated) return;
    this.terminated = true;
    const active = this.activeRequest;
    if (active !== undefined) {
      this.finish(active.id, active.reject, new Error("Worker terminated"));
    }
    this.worker.removeEventListener("message", this.onMessage);
    this.worker.removeEventListener("error", this.onError);
    this.worker.removeEventListener("messageerror", this.onMessageError);
    this.worker.terminate();
  }

  private readonly onMessage = (event: MessageEvent<WorkerResponse<TResult>>): void => {
    const response = event.data;
    const active = this.activeRequest;
    if (active === undefined || response === null || response.id !== active.id) {
      return;
    }
    if (response.ok) {
      this.finish(active.id, active.resolve, response.value);
      return;
    }
    const error = new Error(response.error.message);
    error.name = response.error.name;
    if (response.error.stack !== undefined) {
      error.stack = response.error.stack;
    }
    this.finish(active.id, active.reject, error);
  };

  private readonly onError = (event: ErrorEvent): void => {
    const active = this.activeRequest;
    if (active !== undefined) {
      this.finish(active.id, active.reject, event.error ?? new Error(event.message));
    }
  };

  private readonly onMessageError = (): void => {
    const active = this.activeRequest;
    if (active !== undefined) {
      this.finish(active.id, active.reject, new Error("Worker returned a value that cannot be structured-cloned"));
    }
  };

  private post(request: WorkerRequest<TInput>, transfer?: readonly Transferable[]): void {
    if (transfer === undefined) {
      this.worker.postMessage(request);
      return;
    }
    this.worker.postMessage(request, [...transfer]);
  }

  private finish<T>(id: string, complete: (value: T) => void, value: T): void {
    const active = this.activeRequest;
    if (active === undefined || active.id !== id) {
      return;
    }
    this.activeRequest = undefined;
    if (active.abortListener !== undefined && active.abortSignal !== undefined) {
      active.abortSignal.removeEventListener("abort", active.abortListener);
    }
    complete(value);
  }
}
