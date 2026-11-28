import { describe, expect, it } from "vitest";
import { createWorkerMessageHandler, type WorkerResponse } from "java/concurrent/WorkerProtocol.js";

describe("WorkerProtocol", () => {
    it("returns a synchronous render failure and handles the next request", async () => {
        const responses: WorkerResponse<number>[] = [];
        const handle = createWorkerMessageHandler(
            (input: number) => {
                if (input < 0) {
                    throw new RangeError("Invalid tile");
                }
                return input * 2;
            },
            (response) => responses.push(response),
        );

        handle({ data: { id: "1", kind: "run", payload: -1 } });
        handle({ data: { id: "2", kind: "run", payload: 4 } });
        await new Promise<void>((resolve) => setTimeout(resolve, 0));

        expect(responses).toContainEqual({
            id: "1",
            ok: false,
            error: expect.objectContaining({ name: "RangeError", message: "Invalid tile" }),
        });
        expect(responses).toContainEqual({ id: "2", ok: true, value: 8 });
    });
});
