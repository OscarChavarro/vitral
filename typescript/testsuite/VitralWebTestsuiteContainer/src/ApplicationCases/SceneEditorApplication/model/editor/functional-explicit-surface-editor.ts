import { Double, FunctionalExplicitSurface, Integer, StringTokenizer, type SimpleBody } from '@vitral/base';

/**
 * Editable parameters of the surface, in the order used by the predefined
 * configurations (Java's enum `FunctionalExplicitSurfaceEditor.Parameter`; its
 * label is `parameterLabel`).
 */
export enum FunctionalExplicitSurfaceParameter {
  FUNCTION = 0,
  MIN_X = 1,
  MIN_Y = 2,
  MIN_Z = 3,
  MAX_X = 4,
  MAX_Y = 5,
  MAX_Z = 6,
  NX = 7,
  NY = 8,
}

const PARAMETER_LABELS: readonly string[] = [
  'z = f(x,y) =',
  'MinX: ',
  'MinY: ',
  'MinZ: ',
  'MaxX: ',
  'MaxY: ',
  'MaxZ: ',
  'Nx: ',
  'Ny: ',
];

/**
 * Port of `model.editor.FunctionalExplicitSurfaceEditor`.
 *
 * Edits the parameters of a body whose geometry is a `FunctionalExplicitSurface`
 * (z = f(x, y)): its function, its bounds and its tessellation. Each change
 * replaces the geometry of the body with a new surface. It also offers a set of
 * predefined configurations. It does not depend on any GUI technology: GUI
 * editors present the parameters as texts and pass back what the user typed.
 */
export class FunctionalExplicitSurfaceEditor {
  /** Title of the editor */
  static readonly TITLE = 'FUNCTIONAL EXPLICIT SURFACE EDITOR';
  /** Label of the list of predefined configurations */
  static readonly PRESETS_LABEL = 'Predefined configurations:';
  /** First entry of the list of predefined configurations: selects none */
  static readonly NO_PRESET = '<None selected>';

  /** Java's `Parameter.values()`, in declaration order */
  static readonly PARAMETERS: readonly FunctionalExplicitSurfaceParameter[] = [
    FunctionalExplicitSurfaceParameter.FUNCTION,
    FunctionalExplicitSurfaceParameter.MIN_X,
    FunctionalExplicitSurfaceParameter.MIN_Y,
    FunctionalExplicitSurfaceParameter.MIN_Z,
    FunctionalExplicitSurfaceParameter.MAX_X,
    FunctionalExplicitSurfaceParameter.MAX_Y,
    FunctionalExplicitSurfaceParameter.MAX_Z,
    FunctionalExplicitSurfaceParameter.NX,
    FunctionalExplicitSurfaceParameter.NY,
  ];

  /**
   * Predefined configurations, with the values of the parameters in the order
   * of `Parameter`, separated by `;`.
   */
  private static readonly PRESETS: readonly string[] = Object.freeze([
    FunctionalExplicitSurfaceEditor.NO_PRESET,
    'cos((PI*x)/2);-10;-10;-10;10;10;10;100;100',
    '0.03*((1.5-x/2)*((2.25-y^2)^4)*(sin(PI*x/2))^2);-2;-1.5;-10;2;1.5;3;100;100',
    'x*y*cos(x*y);-1.5;-1.5;-10;1.5;1.5;3;100;100',
    'x^2+y^2;-2;-2;-10;2;2;2;100;100',
    'x*y*((x^2 - y^2)/(x^2+y^2));-1.5;-1.5;-10;1.5;1.5;3;100;100',
    '(1-sqrt(abs(x*y)));-1.5;-1.5;-10;1.5;1.5;10;100;100',
    '(x^3*y^2)/2;-1.5;-1.5;-10;1.5;1.5;10;100;100',
    '2*y^2*sin(2*x);-1.5;-1.5;-10;1.5;1.5;10;100;100',
    'cos(7.17307*(x+3/2)+(2*y)+(-3))*(exp(2*y+(-3)));-1.5;-1.5;-10;1.5;1.5;3;100;100',
  ]);

  private readonly target: SimpleBody;

  /**
   * @param target body to edit; its geometry must be a
   * `FunctionalExplicitSurface`
   */
  constructor(target: SimpleBody) {
    this.target = target;
  }

  /**
   * @param parameter a parameter
   * @return text presented next to the value of the parameter (Java's
   * `Parameter.getLabel()`)
   */
  static parameterLabel(parameter: FunctionalExplicitSurfaceParameter): string {
    return PARAMETER_LABELS[parameter];
  }

  /**
   * @return the predefined configurations, the first one being `NO_PRESET`
   */
  static getPresets(): readonly string[] {
    return FunctionalExplicitSurfaceEditor.PRESETS;
  }

  private surface(): FunctionalExplicitSurface {
    return this.target.getGeometry() as FunctionalExplicitSurface;
  }

  /**
   * @param parameter parameter to read
   * @return the current value of the parameter, as text
   */
  getValue(parameter: FunctionalExplicitSurfaceParameter): string {
    const surface: FunctionalExplicitSurface = this.surface();

    switch (parameter) {
      case FunctionalExplicitSurfaceParameter.FUNCTION:
        return surface.getFunctionExpression();
      case FunctionalExplicitSurfaceParameter.MIN_X:
        return Double.toString(surface.getMinXBound());
      case FunctionalExplicitSurfaceParameter.MIN_Y:
        return Double.toString(surface.getMinYBound());
      case FunctionalExplicitSurfaceParameter.MIN_Z:
        return Double.toString(surface.getMinZBound());
      case FunctionalExplicitSurfaceParameter.MAX_X:
        return Double.toString(surface.getMaxXBound());
      case FunctionalExplicitSurfaceParameter.MAX_Y:
        return Double.toString(surface.getMaxYBound());
      case FunctionalExplicitSurfaceParameter.MAX_Z:
        return Double.toString(surface.getMaxZBound());
      case FunctionalExplicitSurfaceParameter.NX:
        return Integer.toString(surface.getTesselationHintX());
      default:
        return Integer.toString(surface.getTesselationHintY());
    }
  }

  private currentValues(): string[] {
    return FunctionalExplicitSurfaceEditor.PARAMETERS.map((p) => this.getValue(p));
  }

  /**
   * Changes one parameter, replacing the surface of the target.
   * @param parameter parameter to change
   * @param text new value, as typed by the user
   * @throws NumberFormatException if a numeric parameter is not a number
   */
  setValue(parameter: FunctionalExplicitSurfaceParameter, text: string): void {
    const values: string[] = this.currentValues();

    values[parameter] = text;
    this.rebuild(values);
  }

  /**
   * Changes the parameters to the ones of a predefined configuration,
   * replacing the surface of the target. Parameters missing in the
   * configuration keep their value.
   * @param preset one of `getPresets`
   * @return false if nothing changed (`NO_PRESET`)
   * @throws NumberFormatException if the configuration has a bad number
   */
  applyPreset(preset: string): boolean {
    const values: string[] = this.currentValues();
    const parser: StringTokenizer = new StringTokenizer(preset, ';');

    for (let i = 0; parser.hasMoreTokens() && i < values.length; i++) {
      const token: string = parser.nextToken().toString();
      if (i === 0 && token === FunctionalExplicitSurfaceEditor.NO_PRESET) {
        return false;
      }
      values[i] = token;
    }
    this.rebuild(values);
    return true;
  }

  private rebuild(values: string[]): void {
    const P = FunctionalExplicitSurfaceParameter;
    const surface: FunctionalExplicitSurface = new FunctionalExplicitSurface(values[P.FUNCTION]);
    surface.setBounds(
      Double.parseDouble(values[P.MIN_X]),
      Double.parseDouble(values[P.MIN_Y]),
      Double.parseDouble(values[P.MIN_Z]),
      Double.parseDouble(values[P.MAX_X]),
      Double.parseDouble(values[P.MAX_Y]),
      Double.parseDouble(values[P.MAX_Z]),
    );
    surface.setTesselationHint(Integer.parseInt(values[P.NX]), Integer.parseInt(values[P.NY]));
    this.target.setGeometry(surface);
  }
}
