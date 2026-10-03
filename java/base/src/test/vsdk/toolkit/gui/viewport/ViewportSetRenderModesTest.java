package vsdk.toolkit.gui.viewport;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.gui.KeyEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
Exercises the render modes available in a `ViewportSet`, which depend on the
technology presenting it (i.e. no GPU mode over a 2D canvas).
 */
class ViewportSetRenderModesTest
{
    private static final int[] CPU_ONLY_MODES = {
        Viewport.RENDER_MODE_HIDDEN_LINES, Viewport.RENDER_MODE_RAYTRACING
    };

    @Test
    void given_aNewSet_when_askingForModes_then_gpuAndRaytracingAreAvailable()
    {
        // Arrange
        ViewportSet set = ViewportSet.createStandardSet("test");

        // Act / Assert
        assertThat(set.isRenderModeAvailable(Viewport.RENDER_MODE_Z_BUFFER)).isTrue();
        assertThat(set.isRenderModeAvailable(Viewport.RENDER_MODE_RAYTRACING)).isTrue();
        assertThat(set.isRenderModeAvailable(Viewport.RENDER_MODE_HIDDEN_LINES)).isFalse();
    }

    @Test
    void given_gpuViewports_when_onlyCpuModesAreAvailable_then_theyChangeToTheDefaultOne()
    {
        // Arrange
        ViewportSet set = ViewportSet.createStandardSet("test");
        set.getViewport(2).setRenderMode(Viewport.RENDER_MODE_RAYTRACING);

        // Act
        set.setAvailableRenderModes(CPU_ONLY_MODES);

        // Assert
        assertThat(set.getViewport(0).getRenderMode()).isEqualTo(Viewport.RENDER_MODE_HIDDEN_LINES);
        assertThat(set.getViewport(2).getRenderMode()).isEqualTo(Viewport.RENDER_MODE_RAYTRACING);
    }

    @Test
    void given_onlyCpuModes_when_addingAViewport_then_itDoesNotStartInGpuMode()
    {
        // Arrange
        ViewportSet set = ViewportSet.createStandardSet("test");
        set.setAvailableRenderModes(CPU_ONLY_MODES);

        // Act
        set.addViewport(new Viewport());

        // Assert
        assertThat(set.getViewport(4).getRenderMode()).isEqualTo(Viewport.RENDER_MODE_HIDDEN_LINES);
    }

    @Test
    void given_onlyCpuModes_when_cyclingWithTheDotKey_then_gpuModeIsNeverSelected()
    {
        // Arrange
        ViewportSet set = ViewportSet.createStandardSet("test");
        set.setAvailableRenderModes(CPU_ONLY_MODES);
        ViewportSetInteractionTechniques techniques = new ViewportSetInteractionTechniques(set);
        KeyEvent dot = new KeyEvent();
        dot.unicodeId = '.';
        Viewport selected = set.getSelectedViewport();

        // Act
        techniques.processKeyPressedEvent(dot);
        int afterFirst = selected.getRenderMode();
        techniques.processKeyPressedEvent(dot);
        int afterSecond = selected.getRenderMode();

        // Assert
        assertThat(afterFirst).isEqualTo(Viewport.RENDER_MODE_RAYTRACING);
        assertThat(afterSecond).isEqualTo(Viewport.RENDER_MODE_HIDDEN_LINES);
    }

    @Test
    void given_onlyCpuModes_when_processingTheGpuCommand_then_itIsRejected()
    {
        // Arrange
        ViewportSet set = ViewportSet.createStandardSet("test");
        set.setAvailableRenderModes(CPU_ONLY_MODES);
        ViewportSetInteractionTechniques techniques = new ViewportSetInteractionTechniques(set);

        // Act
        boolean processed = techniques.processCommand(ViewportSetCommands.IDV_RENDER_MODE_GPU);

        // Assert
        assertThat(processed).isFalse();
        assertThat(set.getSelectedViewport().getRenderMode()).isEqualTo(Viewport.RENDER_MODE_HIDDEN_LINES);
    }

    @Test
    void given_everyRenderMode_when_convertingToCommandAndBack_then_modeIsKept()
    {
        for ( int mode : new int[] {Viewport.RENDER_MODE_Z_BUFFER,
                                    Viewport.RENDER_MODE_RAYTRACING,
                                    Viewport.RENDER_MODE_HIDDEN_LINES} ) {
            // Act
            String command = Viewport.commandForRenderMode(mode);

            // Assert
            assertThat(Viewport.renderModeForCommand(command)).isEqualTo(mode);
        }
    }
}
