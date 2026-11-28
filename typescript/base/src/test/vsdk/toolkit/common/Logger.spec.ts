import { afterEach, describe, expect, it, vi } from "vitest";
import { Exception } from "java/lang/Exception.js";
import { Logger } from "vsdk/toolkit/common/logging/Logger.js";
import { VSDKFatalException } from "vsdk/toolkit/common/VSDKFatalException.js";

afterEach(() => {
    Logger.setWithSystemExit(true);
    Logger.setWithFatalExceptions(true);
    vi.restoreAllMocks();
});

describe("Logger", () => {
    it("writes Java's exception separator and null diagnostic", () => {
        const report = vi.spyOn(console, "error").mockImplementation(() => {});

        Logger.reportMessageWithException(null, Logger.WARNING, "load", "text", null);

        const separator = "---------------------------------------------------------------------------";
        expect(report.mock.calls.slice(1)).toEqual([
            [separator],
            ["Given exception is null! not reporting details!"],
            [separator],
        ]);
    });

    it("retains the cause and throws the Java fatal message when process exit is disabled", () => {
        vi.spyOn(console, "error").mockImplementation(() => {});
        const cause = new Exception("read failed");
        Logger.setWithSystemExit(false);

        try {
            Logger.reportMessageWithException(null, Logger.FATAL_ERROR, "load", "failed", cause);
            expect.unreachable("the fatal error must be propagated");
        } catch (error) {
            expect(error).toBeInstanceOf(VSDKFatalException);
            expect((error as VSDKFatalException).getCause()).toBe(cause);
            expect((error as VSDKFatalException).getMessage()).toBe("VSDK fatal error at load: failed");
        }
        Logger.setWithFatalExceptions(false);
        expect(() => Logger.reportMessage(null, Logger.FATAL_ERROR, null, null)).not.toThrow();
    });
});
