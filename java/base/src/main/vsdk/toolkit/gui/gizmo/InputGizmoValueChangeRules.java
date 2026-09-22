package vsdk.toolkit.gui.gizmo;

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
public class InputGizmoValueChangeRules {
    /// Default values of the grid restriction, used while it is disabled
    private static final double DEFAULT_GRID_SIZE = 1.0;

    /// Increment requested by RIGHT / LEFT
    private final double levelOneStep;
    /// Increment requested by UP / DOWN
    private final double levelTwoStep;
    /// Increment requested by PAGEUP / PAGEDOWN
    private final double levelThreeStep;

    /// True if stepped values are rounded to the nearest multiple of
    /// `gridSize`; meant to be toggled at runtime
    private boolean gridRestrictionEnabled;
    /// Size of the grid the stepped values are rounded to, while the grid
    /// restriction is enabled; always positive
    private double gridSize;

    /// True if stepped values are wrapped into [minValue, maxValue)
    private final boolean circular;
    /// Lower bound (included) of the circular range
    private final double minValue;
    /// Upper bound (excluded) of the circular range
    private final double maxValue;

    /**
    Creates rules with no grid restriction and no circular range: stepping
    only adds the step of the requested level.
    @param levelOneStep increment requested by RIGHT / LEFT
    @param levelTwoStep increment requested by UP / DOWN
    @param levelThreeStep increment requested by PAGEUP / PAGEDOWN
    */
    public InputGizmoValueChangeRules(double levelOneStep, double levelTwoStep, double levelThreeStep)
    {
        this(levelOneStep, levelTwoStep, levelThreeStep, 0.0, 0.0);
    }

    /**
    Creates rules with a circular range and no grid restriction (see
    `setGridRestrictionEnabled` to turn it on later).
    @param levelOneStep increment requested by RIGHT / LEFT
    @param levelTwoStep increment requested by UP / DOWN
    @param levelThreeStep increment requested by PAGEUP / PAGEDOWN
    @param minValue lower bound (included) of the circular range
    @param maxValue upper bound (excluded) of the circular range, greater than
    `minValue`
    */
    public InputGizmoValueChangeRules(double levelOneStep, double levelTwoStep, double levelThreeStep,
                                      double minValue, double maxValue)
    {
        this.levelOneStep = levelOneStep;
        this.levelTwoStep = levelTwoStep;
        this.levelThreeStep = levelThreeStep;
        this.gridRestrictionEnabled = false;
        this.gridSize = DEFAULT_GRID_SIZE;
        this.circular = maxValue > minValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
    @return the rules `TranslateGizmo` uses: world units stepped by 0.1, 1 and
    5, with no grid restriction and no circular range
    */
    public static InputGizmoValueChangeRules forTranslation()
    {
        return new InputGizmoValueChangeRules(0.1, 1.0, 5.0);
    }

    /**
    @return the rules `RotateGizmo` (and `ScaleGizmo`'s rotation-free but
    angle-like uses, if any) use: degrees stepped by 1, 5 and 15, wrapped
    around a full turn, [0, 360)
    */
    public static InputGizmoValueChangeRules forRotation()
    {
        return new InputGizmoValueChangeRules(1.0, 5.0, 15.0, 0.0, 360.0);
    }

    /**
    @return the rules `ScaleGizmo` uses: scale factors stepped by 0.01, 0.1
    and 1, with no grid restriction and no circular range
    */
    public static InputGizmoValueChangeRules forScale()
    {
        return new InputGizmoValueChangeRules(0.01, 0.1, 1.0);
    }

    public double getLevelOneStep()
    {
        return levelOneStep;
    }

    public double getLevelTwoStep()
    {
        return levelTwoStep;
    }

    public double getLevelThreeStep()
    {
        return levelThreeStep;
    }

    /**
    @return true if stepped values are rounded to the nearest multiple of
    `getGridSize()`
    */
    public boolean isGridRestrictionEnabled()
    {
        return gridRestrictionEnabled;
    }

    /**
    @param enabled true to round every stepped value to the nearest multiple
    of `getGridSize()`
    */
    public void setGridRestrictionEnabled(boolean enabled)
    {
        gridRestrictionEnabled = enabled;
    }

    /**
    @return the size of the grid stepped values are rounded to, while the grid
    restriction is enabled
    */
    public double getGridSize()
    {
        return gridSize;
    }

    /**
    @param gridSize size of the grid stepped values are rounded to; not
    positive values are ignored
    */
    public void setGridSize(double gridSize)
    {
        if ( gridSize > 0.0 ) {
            this.gridSize = gridSize;
        }
    }

    /**
    @return true if stepped values are wrapped into [getMinValue(),
    getMaxValue())
    */
    public boolean isCircular()
    {
        return circular;
    }

    /**
    @return the lower bound (included) of the circular range
    */
    public double getMinValue()
    {
        return minValue;
    }

    /**
    @return the upper bound (excluded) of the circular range
    */
    public double getMaxValue()
    {
        return maxValue;
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
    public double applyStep(double currentValue, double step)
    {
        double result = currentValue + step;

        if ( gridRestrictionEnabled ) {
            result = Math.round(result / gridSize) * gridSize;
        }
        if ( circular ) {
            result = wrap(result);
        }
        return result;
    }

    /**
    @param value value to wrap
    @return the value wrapped into [minValue, maxValue)
    */
    private double wrap(double value)
    {
        double range = maxValue - minValue;
        double offset = (value - minValue) % range;

        if ( offset < 0.0 ) {
            offset += range;
        }
        return minValue + offset;
    }
}
