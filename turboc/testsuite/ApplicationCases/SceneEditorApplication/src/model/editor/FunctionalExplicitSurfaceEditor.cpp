#include "java/lang/Double.h"
#include "java/lang/Integer.h"
#include "java/util/ArrayList.txx"
#include "java/util/StringTokenizer.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "model/editor/FunctionalExplicitSurfaceEditor.h"

const char* const FunctionalExplicitSurfaceEditor::TITLE =
    "FUNCTIONAL EXPLICIT SURFACE EDITOR";
const char* const FunctionalExplicitSurfaceEditor::PRESETS_LABEL =
    "Predefined configurations:";
const char* const FunctionalExplicitSurfaceEditor::NO_PRESET =
    "<None selected>";

/// Predefined configurations, with the values of the parameters in the
/// order of `Parameter`, separated by `;`.
static const char* const PRESETS[] = {
    FunctionalExplicitSurfaceEditor::NO_PRESET,
    "cos((PI*x)/2);-10;-10;-10;10;10;10;100;100",
    "0.03*((1.5-x/2)*((2.25-y^2)^4)*(sin(PI*x/2))^2);-2;-1.5;-10;2;1.5;3;100;100",
    "x*y*cos(x*y);-1.5;-1.5;-10;1.5;1.5;3;100;100",
    "x^2+y^2;-2;-2;-10;2;2;2;100;100",
    "x*y*((x^2 - y^2)/(x^2+y^2));-1.5;-1.5;-10;1.5;1.5;3;100;100",
    "(1-sqrt(abs(x*y)));-1.5;-1.5;-10;1.5;1.5;10;100;100",
    "(x^3*y^2)/2;-1.5;-1.5;-10;1.5;1.5;10;100;100",
    "2*y^2*sin(2*x);-1.5;-1.5;-10;1.5;1.5;10;100;100",
    "cos(7.17307*(x+3/2)+(2*y)+(-3))*(exp(2*y+(-3)));-1.5;-1.5;-10;1.5;1.5;3;100;100"
};
static const int PRESETS_COUNT = 10;

static const char* const LABELS[] = {
    "z = f(x,y) =",
    "MinX: ",
    "MinY: ",
    "MinZ: ",
    "MaxX: ",
    "MaxY: ",
    "MaxZ: ",
    "Nx: ",
    "Ny: "
};

String FunctionalExplicitSurfaceEditor::getLabel(Parameter parameter)
{
    return LABELS[parameter];
}

FunctionalExplicitSurfaceEditor::FunctionalExplicitSurfaceEditor(
    SimpleBody* target)
    : target(target)
{
}

ArrayList<String> FunctionalExplicitSurfaceEditor::getPresets()
{
    ArrayList<String> presets;
    int i;
    for ( i = 0; i < PRESETS_COUNT; i++ ) {
        presets.add(PRESETS[i]);
    }
    return presets;
}

FunctionalExplicitSurface* FunctionalExplicitSurfaceEditor::surface() const
{
    return (target->getGeometry() != NULL && target->getGeometry()->instanceOf(FunctionalExplicitSurface::CLASS) ? (FunctionalExplicitSurface*)target->getGeometry() : NULL);
}

String FunctionalExplicitSurfaceEditor::getValue(
    Parameter parameter) const
{
    FunctionalExplicitSurface* surface = this->surface();

    if ( surface == NULL ) {
        return String("");
    }
    switch ( parameter ) {
      case FUNCTION: return surface->getFunctionExpression();
      case MIN_X: return Double::toString(surface->getMinXBound());
      case MIN_Y: return Double::toString(surface->getMinYBound());
      case MIN_Z: return Double::toString(surface->getMinZBound());
      case MAX_X: return Double::toString(surface->getMaxXBound());
      case MAX_Y: return Double::toString(surface->getMaxYBound());
      case MAX_Z: return Double::toString(surface->getMaxZBound());
      case NX: return String::valueOf(surface->getTesselationHintX());
      default: return String::valueOf(surface->getTesselationHintY());
    }
}

ArrayList<String>
FunctionalExplicitSurfaceEditor::currentValues() const
{
    ArrayList<String> values;
    int parameter;

    for ( parameter = 0; parameter < PARAMETER_COUNT; parameter++ ) {
        values.add(getValue((Parameter)parameter));
    }
    return values;
}

void FunctionalExplicitSurfaceEditor::setValue(Parameter parameter,
                                               const String& text)
{
    ArrayList<String> values = currentValues();

    values.set(parameter, text);
    rebuild(values);
}

bool FunctionalExplicitSurfaceEditor::applyPreset(const String& preset)
{
    ArrayList<String> values = currentValues();
    StringTokenizer parser(preset, ";");
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

void FunctionalExplicitSurfaceEditor::rebuild(
    const ArrayList<String>& values)
{
    // Numbers are parsed before creating the surface, so a bad number
    // leaves the target untouched
    double minX = Double::parseDouble(values.get(MIN_X));
    double minY = Double::parseDouble(values.get(MIN_Y));
    double minZ = Double::parseDouble(values.get(MIN_Z));
    double maxX = Double::parseDouble(values.get(MAX_X));
    double maxY = Double::parseDouble(values.get(MAX_Y));
    double maxZ = Double::parseDouble(values.get(MAX_Z));
    int nx = Integer::parseInt(values.get(NX));
    int ny = Integer::parseInt(values.get(NY));

    FunctionalExplicitSurface* surface =
        new FunctionalExplicitSurface(values.get(FUNCTION));
    surface->setBounds(minX, minY, minZ, maxX, maxY, maxZ);
    surface->setTesselationHint(nx, ny);
    target->setGeometry(surface);
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int FunctionalExplicitSurfaceEditor::PARAMETER_COUNT;
