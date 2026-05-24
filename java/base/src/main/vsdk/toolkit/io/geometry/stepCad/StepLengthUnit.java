package vsdk.toolkit.io.geometry.stepCad;

/**
Length unit to use when writing or interpreting a STEP file.

Each constant carries the ISO 10303-21 SI prefix string and the
corresponding scale factor that converts from metres (the Vitral
internal unit) to the target unit.

Usage example:
<pre>
    StepWriter.exportSolid(solid, out, "part", StepLengthUnit.MILLIMETERS);
</pre>
*/
public enum StepLengthUnit {

    /** Nanometres  — SI_UNIT(.NANO.,.METRE.)  — scale = 1 × 10⁹  */
    NANOMETERS(".NANO.", 1e9),

    /** Micrometres — SI_UNIT(.MICRO.,.METRE.) — scale = 1 × 10⁶  */
    MICROMETERS(".MICRO.", 1e6),

    /** Millimetres — SI_UNIT(.MILLI.,.METRE.) — scale = 1 × 10³  */
    MILLIMETERS(".MILLI.", 1e3),

    /** Centimetres — SI_UNIT(.CENTI.,.METRE.) — scale = 1 × 10²  */
    CENTIMETERS(".CENTI.", 1e2),

    /** Metres (default) — SI_UNIT($,.METRE.) — scale = 1           */
    METERS("$", 1.0),

    /** Kilometres — SI_UNIT(.KILO.,.METRE.) — scale = 1 × 10⁻³  */
    KILOMETERS(".KILO.", 1e-3);

    /** SI prefix token used inside SI_UNIT(...) in the STEP file. */
    public final String siPrefix;

    /**
    Factor to multiply internal metre coordinates by to obtain the
    value in this unit.  Conversely, divide coordinates read from a
    file in this unit by {@code metreScale} to obtain metres.
    */
    public final double metreScale;

    StepLengthUnit(String siPrefix, double metreScale)
    {
        this.siPrefix = siPrefix;
        this.metreScale = metreScale;
    }
}
