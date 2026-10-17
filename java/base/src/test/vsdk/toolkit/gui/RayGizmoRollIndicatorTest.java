package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.gui.gizmo.RayGizmo;

import static org.assertj.core.api.Assertions.assertThat;

class RayGizmoRollIndicatorTest
{
    @Test
    void rollIndicator_isDisabledByDefault()
    {
        assertThat(new RayGizmo(null, 0).isRollIndicatorEnabled()).isFalse();
    }
}
