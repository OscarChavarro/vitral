import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { TangibleInterfaceEvent } from "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.js";
import {
    FrameListener as TangibleInterfaceNetworkClientFrameListener,
    TangibleInterfaceNetworkClient,
} from "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.js";
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
        expect(TangibleInterfaceNetworkClient.FrameListener).toBe(TangibleInterfaceNetworkClientFrameListener);
        const c = new TangibleInterfaceNetworkClient("ws://example.invalid");
        expect(() => (c as unknown as { processMessage(m: string): void }).processMessage("not json")).not.toThrow();
    });
});
