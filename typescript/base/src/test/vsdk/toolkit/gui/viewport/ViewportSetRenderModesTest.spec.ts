// Port of java/base/src/test/vsdk/toolkit/gui/viewport/ViewportSetRenderModesTest.java
import { describe, expect, it } from "vitest";

import { KeyEvent } from "vsdk/toolkit/gui/KeyEvent.js";
import { Viewport } from "vsdk/toolkit/gui/viewport/Viewport.js";
import { ViewportSet } from "vsdk/toolkit/gui/viewport/ViewportSet.js";
import { ViewportSetCommands } from "vsdk/toolkit/gui/viewport/ViewportSetCommands.js";
import { ViewportSetInteractionTechniques } from "vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.js";

/**
Exercises the render modes available in a `ViewportSet`, which depend on the
technology presenting it (i.e. no GPU mode over a 2D canvas).
 */
describe("ViewportSetRenderModesTest", () => {
    const CPU_ONLY_MODES: readonly number[] = [Viewport.RENDER_MODE_HIDDEN_LINES, Viewport.RENDER_MODE_RAYTRACING];

    it("given_aNewSet_when_askingForModes_then_gpuAndRaytracingAreAvailable", () => {
        // Arrange
        const set: ViewportSet = ViewportSet.createStandardSet("test");

        // Act / Assert
        expect(set.isRenderModeAvailable(Viewport.RENDER_MODE_Z_BUFFER)).toBe(true);
        expect(set.isRenderModeAvailable(Viewport.RENDER_MODE_RAYTRACING)).toBe(true);
        expect(set.isRenderModeAvailable(Viewport.RENDER_MODE_HIDDEN_LINES)).toBe(false);
    });

    it("given_gpuViewports_when_onlyCpuModesAreAvailable_then_theyChangeToTheDefaultOne", () => {
        // Arrange
        const set: ViewportSet = ViewportSet.createStandardSet("test");
        set.getViewport(2).setRenderMode(Viewport.RENDER_MODE_RAYTRACING);

        // Act
        set.setAvailableRenderModes(CPU_ONLY_MODES);

        // Assert
        expect(set.getViewport(0).getRenderMode()).toBe(Viewport.RENDER_MODE_HIDDEN_LINES);
        expect(set.getViewport(2).getRenderMode()).toBe(Viewport.RENDER_MODE_RAYTRACING);
    });

    it("given_onlyCpuModes_when_addingAViewport_then_itDoesNotStartInGpuMode", () => {
        // Arrange
        const set: ViewportSet = ViewportSet.createStandardSet("test");
        set.setAvailableRenderModes(CPU_ONLY_MODES);

        // Act
        set.addViewport(new Viewport());

        // Assert
        expect(set.getViewport(4).getRenderMode()).toBe(Viewport.RENDER_MODE_HIDDEN_LINES);
    });

    it("given_onlyCpuModes_when_cyclingWithTheDotKey_then_gpuModeIsNeverSelected", () => {
        // Arrange
        const set: ViewportSet = ViewportSet.createStandardSet("test");
        set.setAvailableRenderModes(CPU_ONLY_MODES);
        const techniques = new ViewportSetInteractionTechniques(set);
        const dot = new KeyEvent();
        dot.unicodeId = KeyEvent.charCode(".");
        const selected: Viewport = set.getSelectedViewport()!;

        // Act
        techniques.processKeyPressedEvent(dot);
        const afterFirst: number = selected.getRenderMode();
        techniques.processKeyPressedEvent(dot);
        const afterSecond: number = selected.getRenderMode();

        // Assert
        expect(afterFirst).toBe(Viewport.RENDER_MODE_RAYTRACING);
        expect(afterSecond).toBe(Viewport.RENDER_MODE_HIDDEN_LINES);
    });

    it("given_onlyCpuModes_when_processingTheGpuCommand_then_itIsRejected", () => {
        // Arrange
        const set: ViewportSet = ViewportSet.createStandardSet("test");
        set.setAvailableRenderModes(CPU_ONLY_MODES);
        const techniques = new ViewportSetInteractionTechniques(set);

        // Act
        const processed: boolean = techniques.processCommand(ViewportSetCommands.IDV_RENDER_MODE_GPU);

        // Assert
        expect(processed).toBe(false);
        expect(set.getSelectedViewport()!.getRenderMode()).toBe(Viewport.RENDER_MODE_HIDDEN_LINES);
    });

    it("given_everyRenderMode_when_convertingToCommandAndBack_then_modeIsKept", () => {
        for (const mode of [
            Viewport.RENDER_MODE_Z_BUFFER,
            Viewport.RENDER_MODE_RAYTRACING,
            Viewport.RENDER_MODE_HIDDEN_LINES,
            Viewport.RENDER_MODE_HIDDEN_SURFACES,
        ]) {
            // Act
            const command: string = Viewport.commandForRenderMode(mode);

            // Assert
            expect(Viewport.renderModeForCommand(command)).toBe(mode);
        }
    });
});
