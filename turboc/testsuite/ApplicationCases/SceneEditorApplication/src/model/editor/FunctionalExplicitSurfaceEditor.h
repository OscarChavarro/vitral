#ifndef __FUNCTIONAL_EXPLICIT_SURFACE_EDITOR__
#define __FUNCTIONAL_EXPLICIT_SURFACE_EDITOR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class FunctionalExplicitSurface;
class SimpleBody;

/**
Edits the parameters of a body whose geometry is a
`FunctionalExplicitSurface` (z = f(x, y)): its function, its bounds and its
tessellation. Each change replaces the geometry of the body with a new
surface. It also offers a set of predefined configurations. It does not
depend on any GUI technology: GUI editors present the parameters as texts and
pass back what the user typed.
*/
class FunctionalExplicitSurfaceEditor {
public:
    /** Title of the editor */
    static const char* const TITLE;
    /** Label of the list of predefined configurations */
    static const char* const PRESETS_LABEL;
    /** First entry of the list of predefined configurations: selects none */
    static const char* const NO_PRESET;

    /**
    Editable parameters of the surface, in the order used by the predefined
    configurations.
    */
    enum Parameter {
        FUNCTION,
        MIN_X,
        MIN_Y,
        MIN_Z,
        MAX_X,
        MAX_Y,
        MAX_Z,
        NX,
        NY
    };
    enum { PARAMETER_COUNT = 9 };

    /**
    @return text presented next to the value of the parameter
    */
    static String getLabel(Parameter parameter);

private:
    SimpleBody* target;

    FunctionalExplicitSurface* surface() const;
    ArrayList<String> currentValues() const;
    void rebuild(const ArrayList<String>& values);

public:
    /**
    @param target body to edit; its geometry must be a
    `FunctionalExplicitSurface`
    */
    explicit FunctionalExplicitSurfaceEditor(SimpleBody* target);

    /**
    @return the predefined configurations, the first one being `NO_PRESET`
    */
    static ArrayList<String> getPresets();

    /**
    @param parameter parameter to read
    @return the current value of the parameter, as text
    */
    String getValue(Parameter parameter) const;

    /**
    Changes one parameter, replacing the surface of the target.
    @param parameter parameter to change
    @param text new value, as typed by the user
    @throws java::NumberFormatException if a numeric parameter is not a
    number
    */
    void setValue(Parameter parameter, const String& text);

    /**
    Changes the parameters to the ones of a predefined configuration,
    replacing the surface of the target. Parameters missing in the
    configuration keep their value.
    @param preset one of `getPresets`
    @return false if nothing changed (`NO_PRESET`)
    @throws java::NumberFormatException if the configuration has a bad
    number
    */
    BOOLEAN applyPreset(const String& preset);
};

#endif
