package model.editor;

// Java basic classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

// VSDK classes
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.scene.SimpleBody;

/**
Edits the parameters of a body whose geometry is a `FunctionalExplicitSurface`
(z = f(x, y)): its function, its bounds and its tessellation. Each change
replaces the geometry of the body with a new surface. It also offers a set of
predefined configurations. It does not depend on any GUI technology: GUI
editors present the parameters as texts and pass back what the user typed.
*/
public class FunctionalExplicitSurfaceEditor
{
    /** Title of the editor */
    public static final String TITLE = "FUNCTIONAL EXPLICIT SURFACE EDITOR";
    /** Label of the list of predefined configurations */
    public static final String PRESETS_LABEL = "Predefined configurations:";
    /** First entry of the list of predefined configurations: selects none */
    public static final String NO_PRESET = "<None selected>";

    /**
    Predefined configurations, with the values of the parameters in the order
    of `Parameter`, separated by `;`.
    */
    private static final List<String> PRESETS = Collections.unmodifiableList(List.of(
        NO_PRESET,
        "cos((PI*x)/2);-10;-10;-10;10;10;10;100;100",
        "0.03*((1.5-x/2)*((2.25-y^2)^4)*(sin(PI*x/2))^2);-2;-1.5;-10;2;1.5;3;100;100",
        "x*y*cos(x*y);-1.5;-1.5;-10;1.5;1.5;3;100;100",
        "x^2+y^2;-2;-2;-10;2;2;2;100;100",
        "x*y*((x^2 - y^2)/(x^2+y^2));-1.5;-1.5;-10;1.5;1.5;3;100;100",
        "(1-sqrt(abs(x*y)));-1.5;-1.5;-10;1.5;1.5;10;100;100",
        "(x^3*y^2)/2;-1.5;-1.5;-10;1.5;1.5;10;100;100",
        "2*y^2*sin(2*x);-1.5;-1.5;-10;1.5;1.5;10;100;100",
        "cos(7.17307*(x+3/2)+(2*y)+(-3))*(exp(2*y+(-3)));-1.5;-1.5;-10;1.5;1.5;3;100;100"));

    /**
    Editable parameters of the surface, in the order used by the predefined
    configurations.
    */
    public enum Parameter
    {
        FUNCTION("z = f(x,y) ="),
        MIN_X("MinX: "),
        MIN_Y("MinY: "),
        MIN_Z("MinZ: "),
        MAX_X("MaxX: "),
        MAX_Y("MaxY: "),
        MAX_Z("MaxZ: "),
        NX("Nx: "),
        NY("Ny: ");

        private final String label;

        Parameter(String label)
        {
            this.label = label;
        }

        /**
        @return text presented next to the value of the parameter
        */
        public String getLabel()
        {
            return label;
        }
    }

    private final SimpleBody target;

    /**
    @param target body to edit; its geometry must be a
    `FunctionalExplicitSurface`
    */
    public FunctionalExplicitSurfaceEditor(SimpleBody target)
    {
        this.target = target;
    }

    /**
    @return the predefined configurations, the first one being `NO_PRESET`
    */
    public static List<String> getPresets()
    {
        return PRESETS;
    }

    private FunctionalExplicitSurface surface()
    {
        return (FunctionalExplicitSurface)target.getGeometry();
    }

    /**
    @param parameter parameter to read
    @return the current value of the parameter, as text
    */
    public String getValue(Parameter parameter)
    {
        FunctionalExplicitSurface surface = surface();

        switch ( parameter ) {
          case FUNCTION: return surface.getFunctionExpression();
          case MIN_X: return "" + surface.getMinXBound();
          case MIN_Y: return "" + surface.getMinYBound();
          case MIN_Z: return "" + surface.getMinZBound();
          case MAX_X: return "" + surface.getMaxXBound();
          case MAX_Y: return "" + surface.getMaxYBound();
          case MAX_Z: return "" + surface.getMaxZBound();
          case NX: return "" + surface.getTesselationHintX();
          default: return "" + surface.getTesselationHintY();
        }
    }

    private List<String> currentValues()
    {
        List<String> values = new ArrayList<>();

        for ( Parameter parameter : Parameter.values() ) {
            values.add(getValue(parameter));
        }
        return values;
    }

    /**
    Changes one parameter, replacing the surface of the target.
    @param parameter parameter to change
    @param text new value, as typed by the user
    @throws NumberFormatException if a numeric parameter is not a number
    */
    public void setValue(Parameter parameter, String text)
    {
        List<String> values = currentValues();

        values.set(parameter.ordinal(), text);
        rebuild(values);
    }

    /**
    Changes the parameters to the ones of a predefined configuration,
    replacing the surface of the target. Parameters missing in the
    configuration keep their value.
    @param preset one of `getPresets`
    @return false if nothing changed (`NO_PRESET`)
    @throws NumberFormatException if the configuration has a bad number
    */
    public boolean applyPreset(String preset)
    {
        List<String> values = currentValues();
        StringTokenizer parser = new StringTokenizer(preset, ";");
        int i;

        for ( i = 0; parser.hasMoreTokens() && i < values.size(); i++ ) {
            String token = parser.nextToken();
            if ( i == 0 && token.equals(NO_PRESET) ) {
                return false;
            }
            values.set(i, token);
        }
        rebuild(values);
        return true;
    }

    private void rebuild(List<String> values)
    {
        FunctionalExplicitSurface surface;

        surface = new FunctionalExplicitSurface(values.get(Parameter.FUNCTION.ordinal()));
        surface.setBounds(
            Double.parseDouble(values.get(Parameter.MIN_X.ordinal())),
            Double.parseDouble(values.get(Parameter.MIN_Y.ordinal())),
            Double.parseDouble(values.get(Parameter.MIN_Z.ordinal())),
            Double.parseDouble(values.get(Parameter.MAX_X.ordinal())),
            Double.parseDouble(values.get(Parameter.MAX_Y.ordinal())),
            Double.parseDouble(values.get(Parameter.MAX_Z.ordinal())));
        surface.setTesselationHint(
            Integer.parseInt(values.get(Parameter.NX.ordinal())),
            Integer.parseInt(values.get(Parameter.NY.ordinal())));
        target.setGeometry(surface);
    }
}
