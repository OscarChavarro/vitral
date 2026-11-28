import { describe, expect, it } from "vitest";
import { InputGizmoValueChangeRules } from "vsdk/toolkit/gui/gizmo/InputGizmoValueChangeRules.js";

/**
Exercises the step levels, grid restriction and circular range of
`InputGizmoValueChangeRules`.
 */
describe("InputGizmoValueChangeRulesTest", () => {
    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    const EPS = 1.0e-9;

    it("given_translationRules_when_asked_then_hasTheExpectedLevelsAndNoRestrictions", () => {
        // Arrange
        let rules = InputGizmoValueChangeRules.forTranslation();

        // Act & Assert
        closeTo(rules.getLevelOneStep(), 0.1, EPS);
        closeTo(rules.getLevelTwoStep(), 1.0, EPS);
        closeTo(rules.getLevelThreeStep(), 5.0, EPS);
        expect(rules.isGridRestrictionEnabled()).toBe(false);
        expect(rules.isCircular()).toBe(false);
    });

    it("given_rotationRules_when_asked_then_hasTheExpectedLevelsAndIsCircular", () => {
        // Arrange
        let rules = InputGizmoValueChangeRules.forRotation();

        // Act & Assert
        closeTo(rules.getLevelOneStep(), 1.0, EPS);
        closeTo(rules.getLevelTwoStep(), 5.0, EPS);
        closeTo(rules.getLevelThreeStep(), 15.0, EPS);
        expect(rules.isCircular()).toBe(true);
        closeTo(rules.getMinValue(), 0.0, EPS);
        closeTo(rules.getMaxValue(), 360.0, EPS);
    });

    it("given_noRestrictions_when_stepApplied_then_justAddsTheStep", () => {
        // Arrange
        let rules = new InputGizmoValueChangeRules(0.1, 1.0, 5.0);

        // Act & Assert
        closeTo(rules.applyStep(0.3333, 10.0), 10.3333, EPS);
    });

    it("given_gridRestriction_when_stepApplied_then_roundsToTheNearestGridMultiple", () => {
        // Arrange
        let rules = new InputGizmoValueChangeRules(0.1, 1.0, 5.0);

        rules.setGridRestrictionEnabled(true);
        rules.setGridSize(1.0);

        // Act & Assert: 0.3333 + 10 = 10.3333, rounded to the nearest integer
        closeTo(rules.applyStep(0.3333, 10.0), 10.0, EPS);
        // 0.6 + 10 = 10.6, rounds up to 11
        closeTo(rules.applyStep(0.6, 10.0), 11.0, EPS);
    });

    it("given_circularRange_when_stepGoesPastTheMax_then_wrapsAroundToTheMin", () => {
        // Arrange
        let rules = new InputGizmoValueChangeRules(1.0, 5.0, 15.0, 0.0, 360.0);

        // Act & Assert: 355 + 15 = 370, wrapped into [0, 360)
        closeTo(rules.applyStep(355.0, 15.0), 10.0, EPS);
    });

    it("given_circularRange_when_stepGoesBelowTheMin_then_wrapsAroundToTheMax", () => {
        // Arrange
        let rules = new InputGizmoValueChangeRules(1.0, 5.0, 15.0, 0.0, 360.0);

        // Act & Assert: 5 - 15 = -10, wrapped into [0, 360)
        closeTo(rules.applyStep(5.0, -15.0), 350.0, EPS);
    });

    it("given_nonPositiveGridSize_when_set_then_isIgnored", () => {
        // Arrange
        let rules = new InputGizmoValueChangeRules(0.1, 1.0, 5.0);

        rules.setGridRestrictionEnabled(true);

        // Act
        rules.setGridSize(-1.0);
        rules.setGridSize(0.0);

        // Assert: the default grid size (1.0) is kept
        closeTo(rules.applyStep(0.6, 10.0), 11.0, EPS);
    });
});
