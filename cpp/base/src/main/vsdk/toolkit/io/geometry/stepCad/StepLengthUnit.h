#ifndef __STEP_LENGTH_UNIT__
#define __STEP_LENGTH_UNIT__

/**
Length unit to use when writing or interpreting a STEP file.

Each constant carries the ISO 10303-21 SI prefix string and the
corresponding scale factor that converts from metres (the Vitral
internal unit) to the target unit.

Usage example:
<pre>
    StepWriter::exportSolid(solid, out, "part", MILLIMETERS);
</pre>

C++ port note: the Java enum fields `siPrefix` and `metreScale` are the
methods of the same names in `StepLengthUnitUtil` (the convention of the
C++ port for Java enums, see `ShadingTypeUtil`).
*/
enum StepLengthUnit {
    /** Nanometres  — SI_UNIT(.NANO.,.METRE.)  — scale = 1 × 10⁹  */
    NANOMETERS,
    /** Micrometres — SI_UNIT(.MICRO.,.METRE.) — scale = 1 × 10⁶  */
    MICROMETERS,
    /** Millimetres — SI_UNIT(.MILLI.,.METRE.) — scale = 1 × 10³  */
    MILLIMETERS,
    /** Centimetres — SI_UNIT(.CENTI.,.METRE.) — scale = 1 × 10²  */
    CENTIMETERS,
    /** Metres (default) — SI_UNIT($,.METRE.) — scale = 1           */
    METERS,
    /** Kilometres — SI_UNIT(.KILO.,.METRE.) — scale = 1 × 10⁻³  */
    KILOMETERS
};

class StepLengthUnitUtil {
public:
    /** SI prefix token used inside SI_UNIT(...) in the STEP file. */
    static const char* siPrefix(StepLengthUnit unit)
    {
        switch ( unit ) {
          case NANOMETERS: return ".NANO.";
          case MICROMETERS: return ".MICRO.";
          case MILLIMETERS: return ".MILLI.";
          case CENTIMETERS: return ".CENTI.";
          case KILOMETERS: return ".KILO.";
          case METERS: default: return "$";
        }
    }

    /**
    Factor to multiply internal metre coordinates by to obtain the
    value in this unit.  Conversely, divide coordinates read from a
    file in this unit by `metreScale` to obtain metres.
    */
    static double metreScale(StepLengthUnit unit)
    {
        switch ( unit ) {
          case NANOMETERS: return 1e9;
          case MICROMETERS: return 1e6;
          case MILLIMETERS: return 1e3;
          case CENTIMETERS: return 1e2;
          case KILOMETERS: return 1e-3;
          case METERS: default: return 1.0;
        }
    }

    /** @return the name of the Java enum constant */
    static const char* name(StepLengthUnit unit)
    {
        switch ( unit ) {
          case NANOMETERS: return "NANOMETERS";
          case MICROMETERS: return "MICROMETERS";
          case MILLIMETERS: return "MILLIMETERS";
          case CENTIMETERS: return "CENTIMETERS";
          case KILOMETERS: return "KILOMETERS";
          case METERS: default: return "METERS";
        }
    }

private:
    StepLengthUnitUtil() {}
};

#endif
