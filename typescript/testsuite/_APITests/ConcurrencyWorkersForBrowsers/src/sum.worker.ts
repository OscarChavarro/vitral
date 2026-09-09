import { workerError, type WorkerRequest, type WorkerResponse, type WorkerTransferValue } from "@vitral/base";

type SumInput = { readonly values: Int32Array; readonly [key: string]: WorkerTransferValue };
const cancelled = new Set<string>();

self.addEventListener("message", (event: MessageEvent<WorkerRequest<SumInput>>) => {
  const request = event.data;
  if (request.kind === "cancel") { cancelled.add(request.id); return; }
  try {
    const values = request.payload?.values;
    if (values === undefined) throw new Error("Missing values payload");
    let total = 0;
    for (let i = 0; i < values.length; i++) {
      if ((i & 0x3fff) === 0 && cancelled.delete(request.id)) throw new DOMException("The operation was aborted", "AbortError");
      total += values[i] ?? 0;
    }
    const response: WorkerResponse<number> = { id: request.id, ok: true, value: total };
    self.postMessage(response);
  }
  catch (error) {
    const response: WorkerResponse<number> = { id: request.id, ok: false, error: workerError(error) };
    self.postMessage(response);
  }
});
