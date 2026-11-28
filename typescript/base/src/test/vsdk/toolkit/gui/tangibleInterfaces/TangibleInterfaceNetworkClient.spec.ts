import { afterEach, describe, expect, it, vi } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { TangibleInterfaceEvent } from "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.js";
import {
    FrameListener as TangibleInterfaceNetworkClientFrameListener,
    TangibleInterfaceNetworkClient,
} from "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.js";

afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
});

describe("TangibleInterfaceNetworkClient", () => {
    it("parses and notifies listeners", () => {
        const c = new TangibleInterfaceNetworkClient("ws://example.invalid"),
            events: TangibleInterfaceEvent[] = [];
        c.addListener({ tangibleInterfaceEventReceived: (e) => events.push(e) });
        (c as unknown as { processMessage(m: string): void }).processMessage(
            '[{"label":"one","position":[1,2,3],"quaternion":[1,0,0,0]},{"label":"two","position":[4,5,6],"quaternion":[0,0,1,0]}]',
        );
        expect(events.map((e) => e.getId())).toEqual(["one", "two"]);
        expect(
            events[1]
                ?.getRotation()
                .direction()
                .epsilonEquals(new Vector3Dd(0, 1, 0)),
        ).toBe(true);
    });
    it("handles frame listener and malformed input", () => {
        const messages: string[] = [],
            l = new TangibleInterfaceNetworkClient.FrameListener(
                (m) => messages.push(m),
                () => undefined,
            );
        l.onText(null as never, "payload");
        expect(messages).toEqual(["payload"]);
        l.onText(null as never, "first-", false);
        expect(messages).toEqual(["payload"]);
        l.onText(null as never, "last", true);
        expect(messages).toEqual(["payload", "first-last"]);
        expect(TangibleInterfaceNetworkClient.FrameListener).toBe(TangibleInterfaceNetworkClientFrameListener);
        const c = new TangibleInterfaceNetworkClient("ws://example.invalid");
        expect(() => (c as unknown as { processMessage(m: string): void }).processMessage("not json")).not.toThrow();
    });

    it("reports the handshake once and ignores callbacks from a replaced connection", () => {
        const sockets: EventTarget[] = [];
        vi.stubGlobal(
            "WebSocket",
            class extends EventTarget {
                public constructor() {
                    super();
                    sockets.push(this);
                }
                public close(): void {}
            },
        );
        vi.spyOn(console, "log").mockImplementation(() => {});
        const client = new TangibleInterfaceNetworkClient("ws://example.invalid");
        const first = vi.fn();
        const second = vi.fn();
        client.run(first);
        sockets[0]!.dispatchEvent(new Event("open"));
        sockets[0]!.dispatchEvent(new Event("error"));
        client.run(second);
        sockets[0]!.dispatchEvent(new Event("error"));
        sockets[1]!.dispatchEvent(new Event("error"));
        sockets[1]!.dispatchEvent(new Event("error"));

        expect(first.mock.calls).toEqual([[true]]);
        expect(second.mock.calls).toEqual([[false]]);
    });

    it("scans pose records with Java's group and field rules", () => {
        const client = new TangibleInterfaceNetworkClient("ws://example.invalid");
        const received: string[] = [];
        client.addListener({ tangibleInterfaceEventReceived: (event) => received.push(event.getId()) });
        const parser = client as unknown as { processMessage(message: string): void };
        parser.processMessage(
            'prefix {"label":"valid","position":[1,2,3,],"quaternion":[1,0,0,0]}' +
                ' {"label":"bad","position":[1,2],"quaternion":[1,0,0,0]} suffix',
        );
        expect(received).toEqual(["valid"]);
    });

    it("ends the stream when a pose contains an invalid numeric field", () => {
        let socket: EventTarget | undefined;
        const close = vi.fn();
        vi.stubGlobal(
            "WebSocket",
            class extends EventTarget {
                public constructor() {
                    super();
                    socket = this;
                }
                public close = close;
            },
        );
        const report = vi.spyOn(console, "log").mockImplementation(() => {});
        const client = new TangibleInterfaceNetworkClient("ws://example.invalid");
        const receive = vi.fn();
        client.addListener({ tangibleInterfaceEventReceived: receive });
        client.run();
        socket!.dispatchEvent(new Event("open"));
        for (const x of ["0b10", "2"]) {
            const data = `{"label":"marker","position":[${x},0,0],"quaternion":[1,0,0,0]}`;
            socket!.dispatchEvent(new MessageEvent("message", { data }));
        }
        expect(report).toHaveBeenCalledOnce();
        expect(close).toHaveBeenCalledOnce();
        expect(receive).not.toHaveBeenCalled();
    });
});
