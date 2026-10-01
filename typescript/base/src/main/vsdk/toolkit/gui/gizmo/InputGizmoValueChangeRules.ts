import { Math as JavaMath } from "../../../../java/lang/Math.js";

/**
Rules that define how the value of a box of an `InputGizmo` changes when it is
stepped with the keyboard (see `InputGizmo.processKeyPressedEvent`).

Different owners of a nested `InputGizmo` need different units and behavior:
i.e. `TranslateGizmo` steps world units, while `RotateGizmo` steps degrees,
that make sense to wrap around a full turn. This class collects that behavior
so each owner can pass its own rules to its `InputGizmo` (see `forTranslation`
and `forRotation`), instead of `InputGizmo` hard-coding a single one.

- Step levels: the increments of level 1, 2 and 3 (requested with RIGHT/LEFT,
  UP/DOWN and PAGEUP/PAGEDOWN respectively). i.e. 0.1, 1 and 5 world units for
  translation, or 1, 5 and 15 degrees for rotation.
- Grid restriction: when enabled (see `setGridRestrictionEnabled`), every
  stepped value is rounded to the nearest multiple of `getGridSize()`, i.e.
  stepping 0.3333 by 10 gives 10.3333, but with a grid size of 1 it gives 10
  (or 11, whichever is nearest). This flag is meant to be toggled at runtime
  (i.e. while a modifier key is held), so it is mutable.
- Circular range: when enabled, every stepped value is wrapped into
  `[getMinValue(), getMaxValue())` instead of growing without bound, i.e.
  stepping 355 degrees by 15 gives 10 (not 370) when the range is [0, 360).
*/
export class InputGizmoValueChangeRules {
    /// Default values of the grid restriction, used while it is disabled
    private static readonly DEFAULT_GRID_SIZE = 1.0;

    /// Increment requested by RIGHT / LEFT
    private readonly levelOneStep: number;
    /// Increment requested by UP / DOWN
    private readonly levelTwoStep: number;
    /// Increment requested by PAGEUP / PAGEDOWN
    private readonly levelThreeStep: number;

    /// True if stepped values are rounded to the nearest multiple of
    /// `gridSize`; meant to be toggled at runtime
    private gridRestrictionEnabled: boolean;
    /// Size of the grid the stepped values are rounded to, while the grid
    /// restriction is enabled; always positive
    private gridSize: number;

    /// True if stepped values are wrapped into [minValue, maxValue)
    private readonly circular: boolean;
    /// Lower bound (included) of the circular range
    private readonly minValue: number;
    /// Upper bound (excluded) of the circular range
    private readonly maxValue: number;

    /**
    Creates rules with a circular range (when `maxValue > minValue`) and no
    grid restriction (see `setGridRestrictionEnabled` to turn it on later).
    Java's three-argument constructor, with no circular range, is this one
    with the range left at [0, 0).
    @param levelOneStep increment requested by RIGHT / LEFT
    @param levelTwoStep increment requested by UP / DOWN
    @param levelThreeStep increment requested by PAGEUP / PAGEDOWN
    @param minValue lower bound (included) of the circular range
    @param maxValue upper bound (excluded) of the circular range, greater than
    `minValue`
    */
    public constructor(
        levelOneStep: number,
        levelTwoStep: number,
        levelThreeStep: number,
        minValue: number = 0.0,
        maxValue: number = 0.0,
    ) {
        this.levelOneStep = levelOneStep;
        this.levelTwoStep = levelTwoStep;
        this.levelThreeStep = levelThreeStep;
        this.gridRestrictionEnabled = false;
        this.gridSize = InputGizmoValueChangeRules.DEFAULT_GRID_SIZE;
        this.circular = maxValue > minValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
    @return the rules `TranslateGizmo` uses: world units stepped by 0.1, 1 and
    5, with no grid restriction and no circular range
    */
    public static forTranslation(): InputGizmoValueChangeRules {
        return new InputGizmoValueChangeRules(0.1, 1.0, 5.0);
    }

    /**
    @return the rules `RotateGizmo` (and `ScaleGizmo`'s rotation-free but
    angle-like uses, if any) use: degrees stepped by 1, 5 and 15, wrapped
    around a full turn, [0, 360)
    */
    public static forRotation(): InputGizmoValueChangeRules {
        return new InputGizmoValueChangeRules(1.0, 5.0, 15.0, 0.0, 360.0);
    }

    /**
    @return the rules `ScaleGizmo` uses: scale factors stepped by 0.01, 0.1
    and 1, with no grid restriction and no circular range
    */
    public static forScale(): InputGizmoValueChangeRules {
        return new InputGizmoValueChangeRules(0.01, 0.1, 1.0);
    }

    public getLevelOneStep(): number {
        return this.levelOneStep;
    }

    public getLevelTwoStep(): number {
        return this.levelTwoStep;
    }

    public getLevelThreeStep(): number {
        return this.levelThreeStep;
    }

    /**
    @return true if stepped values are rounded to the nearest multiple of
    `getGridSize()`
    */
    public isGridRestrictionEnabled(): boolean {
        return this.gridRestrictionEnabled;
    }

    /**
    @param enabled true to round every stepped value to the nearest multiple
    of `getGridSize()`
    */
    public setGridRestrictionEnabled(enabled: boolean): void {
        this.gridRestrictionEnabled = enabled;
    }

    /**
    @return the size of the grid stepped values are rounded to, while the grid
    restriction is enabled
    */
    public getGridSize(): number {
        return this.gridSize;
    }

    /**
    @param gridSize size of the grid stepped values are rounded to; not
    positive values are ignored
    */
    public setGridSize(gridSize: number): void {
        if (gridSize > 0.0) {
            this.gridSize = gridSize;
        }
    }

    /**
    @return true if stepped values are wrapped into [getMinValue(),
    getMaxValue())
    */
    public isCircular(): boolean {
        return this.circular;
    }

    /**
    @return the lower bound (included) of the circular range
    */
    public getMinValue(): number {
        return this.minValue;
    }

    /**
    @return the upper bound (excluded) of the circular range
    */
    public getMaxValue(): number {
        return this.maxValue;
    }

    /**
    Applies a step to a value: adds it, then rounds the result to the grid
    (if the grid restriction is enabled) and wraps it into the circular range
    (if this instance is circular).
    @param currentValue value before the step
    @param step signed increment to apply, i.e. one of `getLevelOneStep()`,
    `getLevelTwoStep()` or `getLevelThreeStep()`, negated for the decreasing
    direction of its level
    @return the value after the step, restricted as configured
    */
    public applyStep(currentValue: number, step: number): number {
        let result: number = currentValue + step;

        if (this.gridRestrictionEnabled) {
            result = JavaMath.round(result / this.gridSize) * this.gridSize;
        }
        if (this.circular) {
            result = this.wrap(result);
        }
        return result;
    }

    /**
    @param value value to wrap
    @return the value wrapped into [minValue, maxValue)
    */
    private wrap(value: number): number {
        const range: number = this.maxValue - this.minValue;
        let offset: number = (value - this.minValue) % range;

        if (offset < 0.0) {
            offset += range;
        }
        return this.minValue + offset;
    }
}
