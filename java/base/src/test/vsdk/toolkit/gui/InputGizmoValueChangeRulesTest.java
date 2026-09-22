package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.gui.gizmo.InputGizmoValueChangeRules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the step levels, grid restriction and circular range of
`InputGizmoValueChangeRules`.
 */
class InputGizmoValueChangeRulesTest
{
    private static final double EPS = 1.0e-9;

    @Test
    void given_translationRules_when_asked_then_hasTheExpectedLevelsAndNoRestrictions()
    {
        // Arrange
        InputGizmoValueChangeRules rules = InputGizmoValueChangeRules.forTranslation();

        // Act & Assert
        assertThat(rules.getLevelOneStep()).isCloseTo(0.1, offset(EPS));
        assertThat(rules.getLevelTwoStep()).isCloseTo(1.0, offset(EPS));
        assertThat(rules.getLevelThreeStep()).isCloseTo(5.0, offset(EPS));
        assertThat(rules.isGridRestrictionEnabled()).isFalse();
        assertThat(rules.isCircular()).isFalse();
    }

    @Test
    void given_rotationRules_when_asked_then_hasTheExpectedLevelsAndIsCircular()
    {
        // Arrange
        InputGizmoValueChangeRules rules = InputGizmoValueChangeRules.forRotation();

        // Act & Assert
        assertThat(rules.getLevelOneStep()).isCloseTo(1.0, offset(EPS));
        assertThat(rules.getLevelTwoStep()).isCloseTo(5.0, offset(EPS));
        assertThat(rules.getLevelThreeStep()).isCloseTo(15.0, offset(EPS));
        assertThat(rules.isCircular()).isTrue();
        assertThat(rules.getMinValue()).isCloseTo(0.0, offset(EPS));
        assertThat(rules.getMaxValue()).isCloseTo(360.0, offset(EPS));
    }

    @Test
    void given_noRestrictions_when_stepApplied_then_justAddsTheStep()
    {
        // Arrange
        InputGizmoValueChangeRules rules = new InputGizmoValueChangeRules(0.1, 1.0, 5.0);

        // Act & Assert
        assertThat(rules.applyStep(0.3333, 10.0)).isCloseTo(10.3333, offset(EPS));
    }

    @Test
    void given_gridRestriction_when_stepApplied_then_roundsToTheNearestGridMultiple()
    {
        // Arrange
        InputGizmoValueChangeRules rules = new InputGizmoValueChangeRules(0.1, 1.0, 5.0);

        rules.setGridRestrictionEnabled(true);
        rules.setGridSize(1.0);

        // Act & Assert: 0.3333 + 10 = 10.3333, rounded to the nearest integer
        assertThat(rules.applyStep(0.3333, 10.0)).isCloseTo(10.0, offset(EPS));
        // 0.6 + 10 = 10.6, rounds up to 11
        assertThat(rules.applyStep(0.6, 10.0)).isCloseTo(11.0, offset(EPS));
    }

    @Test
    void given_circularRange_when_stepGoesPastTheMax_then_wrapsAroundToTheMin()
    {
        // Arrange
        InputGizmoValueChangeRules rules = new InputGizmoValueChangeRules(1.0, 5.0, 15.0, 0.0, 360.0);

        // Act & Assert: 355 + 15 = 370, wrapped into [0, 360)
        assertThat(rules.applyStep(355.0, 15.0)).isCloseTo(10.0, offset(EPS));
    }

    @Test
    void given_circularRange_when_stepGoesBelowTheMin_then_wrapsAroundToTheMax()
    {
        // Arrange
        InputGizmoValueChangeRules rules = new InputGizmoValueChangeRules(1.0, 5.0, 15.0, 0.0, 360.0);

        // Act & Assert: 5 - 15 = -10, wrapped into [0, 360)
        assertThat(rules.applyStep(5.0, -15.0)).isCloseTo(350.0, offset(EPS));
    }

    @Test
    void given_nonPositiveGridSize_when_set_then_isIgnored()
    {
        // Arrange
        InputGizmoValueChangeRules rules = new InputGizmoValueChangeRules(0.1, 1.0, 5.0);

        rules.setGridRestrictionEnabled(true);

        // Act
        rules.setGridSize(-1.0);
        rules.setGridSize(0.0);

        // Assert: the default grid size (1.0) is kept
        assertThat(rules.applyStep(0.6, 10.0)).isCloseTo(11.0, offset(EPS));
    }
}
