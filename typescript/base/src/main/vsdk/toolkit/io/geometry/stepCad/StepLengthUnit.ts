/**
Length unit to use when writing or interpreting a STEP file.

Each constant carries the ISO 10303-21 SI prefix string and the
corresponding scale factor that converts from metres (the Vitral
internal unit) to the target unit.

Usage example:
<pre>
    StepWriter.exportSolid(solid, out, "part", StepLengthUnit.MILLIMETERS);
</pre>

Port of `vsdk.toolkit.io.geometry.stepCad.StepLengthUnit`. A Java enum whose
constants carry fields is a class with one frozen instance per constant, in
declaration order, so that `values()`, `name()` and `ordinal()` answer what
the Java ones answer.
*/
export class StepLengthUnit {
    /** Nanometres  — SI_UNIT(.NANO.,.METRE.)  — scale = 1 × 10⁹  */
    public static readonly NANOMETERS = new StepLengthUnit("NANOMETERS", 0, ".NANO.", 1e9);

    /** Micrometres — SI_UNIT(.MICRO.,.METRE.) — scale = 1 × 10⁶  */
    public static readonly MICROMETERS = new StepLengthUnit("MICROMETERS", 1, ".MICRO.", 1e6);

    /** Millimetres — SI_UNIT(.MILLI.,.METRE.) — scale = 1 × 10³  */
    public static readonly MILLIMETERS = new StepLengthUnit("MILLIMETERS", 2, ".MILLI.", 1e3);

    /** Centimetres — SI_UNIT(.CENTI.,.METRE.) — scale = 1 × 10²  */
    public static readonly CENTIMETERS = new StepLengthUnit("CENTIMETERS", 3, ".CENTI.", 1e2);

    /** Metres (default) — SI_UNIT($,.METRE.) — scale = 1           */
    public static readonly METERS = new StepLengthUnit("METERS", 4, "$", 1.0);

    /** Kilometres — SI_UNIT(.KILO.,.METRE.) — scale = 1 × 10⁻³  */
    public static readonly KILOMETERS = new StepLengthUnit("KILOMETERS", 5, ".KILO.", 1e-3);

    private static readonly VALUES: readonly StepLengthUnit[] = [
        StepLengthUnit.NANOMETERS,
        StepLengthUnit.MICROMETERS,
        StepLengthUnit.MILLIMETERS,
        StepLengthUnit.CENTIMETERS,
        StepLengthUnit.METERS,
        StepLengthUnit.KILOMETERS,
    ];

    private constructor(
        private readonly constantName: string,
        private readonly constantOrdinal: number,
        /** SI prefix token used inside SI_UNIT(...) in the STEP file. */
        public readonly siPrefix: string,
        /**
        Factor to multiply internal metre coordinates by to obtain the
        value in this unit.  Conversely, divide coordinates read from a
        file in this unit by {@code metreScale} to obtain metres.
        */
        public readonly metreScale: number,
    ) {
        Object.freeze(this);
    }

    public static values(): StepLengthUnit[] {
        return [...StepLengthUnit.VALUES];
    }

    public name(): string {
        return this.constantName;
    }

    public ordinal(): number {
        return this.constantOrdinal;
    }

    public toString(): string {
        return this.constantName;
    }
}
