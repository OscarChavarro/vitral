import { describe, expect, it } from "vitest";
import { TangibleInterfaceNetworkClientFrameListener, TangibleInterfaceEvent, TangibleInterfaceNetworkClient, Vector3Dd, Quaterniond } from "../../../../index.js";

describe("Phase 13 tangible-interface contracts", () => {
  it("retains the event identifier, pose and readable representation", () => {
    const event = new TangibleInterfaceEvent("rayCube", new Vector3Dd(1, 2, 3), new Quaterniond(new Vector3Dd(0, 1, 0), 1));
    expect(event.getId()).toBe("rayCube"); expect(event.getPosition().epsilonEquals(new Vector3Dd(1, 2, 3))).toBe(true);
    expect(event.toString()).toContain("TangibleInterfaceEvent{id=rayCube");
  });

  it("parses pose batches and notifies a listener in message order", () => {
    const client = new TangibleInterfaceNetworkClient("ws://example.invalid/v1/values");
    const received: TangibleInterfaceEvent[] = [];
    const listener = { tangibleInterfaceEventReceived: (event: TangibleInterfaceEvent) => received.push(event) };
    client.addListener(listener);
    (client as unknown as { processMessage(message: string): void }).processMessage('[{"label":"one","position":[1,2,3],"quaternion":[1,0,0,0]},{"label":"two","position":[4,5,6],"quaternion":[0,0,1,0]}]');
    expect(received.map((event) => event.getId())).toEqual(["one", "two"]);
    expect(received[1]?.getRotation().direction().epsilonEquals(new Vector3Dd(0, 1, 0))).toBe(true);
    client.removeListener(listener);
    (client as unknown as { processMessage(message: string): void }).processMessage('{"label":"three","position":[0,0,0],"quaternion":[1,0,0,0]}');
    expect(received).toHaveLength(2);
  });

  it("ignores malformed frames and exposes the nested frame-listener contract", () => {
    const messages: string[] = [];
    const listener = new TangibleInterfaceNetworkClient.FrameListener((message) => messages.push(message), () => undefined);
    listener.onText(null as never, "payload"); listener.onError(null as never, new Event("error"));
    expect(messages).toEqual(["payload"]);
    expect(TangibleInterfaceNetworkClient.FrameListener).toBe(TangibleInterfaceNetworkClientFrameListener);
    const client = new TangibleInterfaceNetworkClient("ws://example.invalid");
    expect(() => (client as unknown as { processMessage(message: string): void }).processMessage("not json")).not.toThrow();
  });
});
