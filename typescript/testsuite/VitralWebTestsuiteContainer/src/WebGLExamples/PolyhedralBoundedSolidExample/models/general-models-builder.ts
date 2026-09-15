//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import {
  Arrow,
  Box,
  ByteArrayInputStream,
  Cone,
  CsgKurlanderBowlFixture,
  InfinitePlane,
  JavaMath,
  Logger,
  Matrix4x4d,
  PolyhedralBoundedSolid,
  PolyhedralBoundedSolidEulerOperators,
  PolyhedralBoundedSolidModeler,
  PolyhedralBoundedSolidStatistics,
  PolyhedralBoundedSolidTopologyEditing,
  PolyhedralBoundedSolidValidationEngine,
  SimpleTestGeometryLibrary,
  Sphere,
  StepReader,
  VSDK,
  Vector3Dd,
  type ParametricCurve,
  type _PolyhedralBoundedSolidFace,
  type _PolyhedralBoundedSolidHalfEdge,
} from '@vitral/base';
import type { WebFontReader } from '@vitral/webgl';
import { csgOperationLabel, type CsgOperationNames } from './csg-operation-names';
import { csgSampleLabel, type CsgSampleNames } from './csg-sample-names';
import type { DebuggerModel } from './debugger-model';

/**
 * The files `GeneralModelsBuilder` reads while it builds, fetched ahead of
 * time: the bytes of every STEP file it names, keyed by the name it uses, and
 * a font reader that has already loaded every font it names.
 */
export interface GeneralModelsResources {
  readonly stepFiles: ReadonlyMap<string, Uint8Array>;
  readonly fontReader: WebFontReader;
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/models/GeneralModelsBuilder.java`,
 * for every model of the Java program.
 *
 * `buildSolid` is Java's switch, and every builder it reaches is Java's,
 * statement by statement, over the ported kernel: the
 * `mvfs`/`smev` wire, the box, the holed box of [MANT1988].9.3 built with
 * `smev`/`kemr`/`mef`/`kfmrh`, the arc and the circular lamina, the two
 * translational sweeps, the sphere, cone and cylinder with the current
 * subdivisions, the arrow, the two laminas with an inner loop, the extruded
 * glyph, the glued cylinders of [MANT1988].12.4, the Euler operator wire, the
 * torus of the rotational sweep of [MANT1988].12.5, the featured object of
 * [APPE1967], and the imported STEP solid; and the models of the boolean set
 * operations and the plane split: `createHollowBox` of [MANT1988].15.1,
 * `createCsgLampShell`, `splitTest` over `PolyhedralBoundedSolidModeler.split`,
 * and the CSG block — `buildCsgMoonBlock`, `buildCsgTest2`, `buildCsgTest4`,
 * `buildCsgTest5`, `csgTest`, `createCsgOperands`,
 * `createCsgHudPreviewOperands` and their progress messages — over
 * `PolyhedralBoundedSolidModeler.setOp`.
 *
 * Java's overloads with a trailing motif index (`csgTest`,
 * `createCsgOperands`, `createCsgHudPreviewOperands`) are one method whose
 * index defaults to the 0 the shorter overload passes. `System.out` and
 * `System.err` are `console.log` and `console.error`.
 *
 * Three runtime boundaries:
 *
 *   - `importFromStepFile` opens `../../../../etc/solids/kurlanderBowl.step`
 *     with `StepReader.readSolid(File)`. The bytes are fetched before any
 *     build, from the URL chosen in the module's dialog, and the stream
 *     overload is handed a `ByteArrayInputStream` over them; a file that could
 *     not be fetched fails inside the same `try` a missing file fails in, and
 *     falls back to the holed box as Java's does.
 *   - `createFontBlock` names `../../../../etc/fonts/cyrvetic.ttf` and
 *     `AwtFontReader`; the reader here is `WebFontReader`, loaded beforehand,
 *     which answers the outline AWT answers.
 *   - `IMPORT_OR_FEATURED_OBJECT` imports `/tmp/solid.bin` through
 *     `ObjectInputStream` when that file exists and otherwise builds the
 *     featured object. A page has no `/tmp`, so the file never exists and the
 *     featured object is always what is built; `importFromFile`, which only
 *     that branch reaches, has nothing to read and is not ported.
 */
export class GeneralModelsBuilder {
  static readonly KURLANDER_BOWL_STEP_FILE = '../../../../etc/solids/kurlanderBowl.step';
  static readonly FONT_BLOCK_FONT_FILE = '../../../../etc/fonts/cyrvetic.ttf';

  private constructor() {}

  static buildSolid(
    model: DebuggerModel,
    resources: GeneralModelsResources,
  ): PolyhedralBoundedSolid | null {
    let mySolid: PolyhedralBoundedSolid | null;
    let csgPreviewOperands: PolyhedralBoundedSolid[];
    let translationMatrix: Matrix4x4d;
    let rotationMatrix: Matrix4x4d;
    let scaleMatrix: Matrix4x4d;
    let transformationMatrix: Matrix4x4d;
    model.clampSubdivisions();
    model.setCsgPreviewOperandA(null);
    model.setCsgPreviewOperandB(null);

    switch (model.getSolidModelName()) {
      case 'MVFS_SMEV_SAMPLE':
        mySolid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(mySolid, new Vector3Dd(0.1, 0.1, 0.1), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(mySolid, 1, 1, 4, new Vector3Dd(0.1, 1, 0.1));
        PolyhedralBoundedSolidEulerOperators.smev(mySolid, 1, 4, 3, new Vector3Dd(1, 1, 0.1));
        break;
      case 'BOX':
        mySolid = GeneralModelsBuilder.createBox(new Vector3Dd(0.9, 0.9, 0.9));
        break;
      case 'ARC_SAMPLE':
        mySolid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(mySolid, new Vector3Dd(1, 0.5, 0.1), 1, 1);
        PolyhedralBoundedSolidModeler.addArcToExistingFace(
          mySolid,
          1,
          1,
          0.5,
          0.5,
          0.5,
          0.1,
          0,
          270,
          9,
        );
        break;
      case 'CIRCULAR_LAMINA':
        mySolid = PolyhedralBoundedSolidModeler.createCircularLamina(0.5, 0.5, 0.5, 0.1, 12);
        break;
      case 'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC':
        mySolid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(mySolid, new Vector3Dd(1, 0.5, 0.1), 1, 1);
        PolyhedralBoundedSolidModeler.addArcToExistingFace(
          mySolid,
          1,
          1,
          0.5,
          0.5,
          0.5,
          0.1,
          0,
          270,
          18,
        );

        translationMatrix = new Matrix4x4d();
        translationMatrix = translationMatrix.translation(0.0, 0.0, 0.5);
        rotationMatrix = new Matrix4x4d();
        rotationMatrix = rotationMatrix.axisRotation(JavaMath.toRadians(5), 0, 1, 0);
        scaleMatrix = new Matrix4x4d();
        scaleMatrix = scaleMatrix.scale(0.5, 0.5, 0.5);
        transformationMatrix = translationMatrix.multiply(rotationMatrix.multiply(scaleMatrix));

        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
          mySolid,
          mySolid.findFace(1),
          transformationMatrix,
        );

        break;
      case 'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR':
        mySolid = PolyhedralBoundedSolidModeler.createCircularLamina(0.5, 0.5, 0.5, 0.1, 24);

        translationMatrix = new Matrix4x4d();
        translationMatrix = translationMatrix.translation(0.0, 0.0, 0.5);
        rotationMatrix = new Matrix4x4d();
        rotationMatrix = rotationMatrix.axisRotation(JavaMath.toRadians(5), 0, 1, 0);
        scaleMatrix = new Matrix4x4d();
        scaleMatrix = scaleMatrix.scale(0.5, 0.5, 0.5);
        transformationMatrix = translationMatrix.multiply(rotationMatrix.multiply(scaleMatrix));
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
          mySolid,
          mySolid.findFace(1),
          transformationMatrix,
        );

        /*
            T = new Matrix4x4d();
            T = T.translation(0.1, 0.1, 1.0);
            R = new Matrix4x4d();
            //R = R.axisRotation(Math.toRadians(15), 0, 1, 0);
            S = new Matrix4x4d();
            S = S.scale(0.2, 0.2, 0.2);
            M = T.multiply(R.multiply(S));
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFace(
                solid, solid.findFace(1), M);
*/
        break;

      case 'SPHERE':
        mySolid = GeneralModelsBuilder.createSphere(
          0.5,
          model.getSubdivisionCircumference(),
          model.getSubdivisionHeight(),
        );
        break;
      case 'CONE':
        mySolid = GeneralModelsBuilder.createCone(
          0.5,
          0.0,
          1.0,
          model.getSubdivisionCircumference(),
          model.getSubdivisionHeight(),
        );
        break;
      case 'CYLINDER':
        mySolid = GeneralModelsBuilder.createCylinder(
          0.5,
          1.0,
          model.getSubdivisionCircumference(),
          model.getSubdivisionHeight(),
        );
        break;
      case 'CSG_MOON_BLOCK':
        csgPreviewOperands = GeneralModelsBuilder.createCsgHudPreviewOperands('MOON_BLOCK');
        model.setCsgPreviewOperandA(csgPreviewOperands[0]!);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]!);
        mySolid = GeneralModelsBuilder.csgTest(
          1,
          'DIFFERENCE_A_MINUS_B',
          'MOON_BLOCK',
          model.isDebugCsg(),
        );
        model.setDebugCsg(false);
        break;
      case 'CSG_LAMP_SHELL':
        mySolid = GeneralModelsBuilder.createCsgLampShell(
          model.getSubdivisionCircumference(),
          model.getSubdivisionHeight(),
        );
        break;
      case 'ARROW':
        mySolid = GeneralModelsBuilder.createArrow(0.7, 0.3, 0.05, 0.1);
        break;
      case 'LAMINA_WITH_TWO_SHELLS':
        mySolid = GeneralModelsBuilder.createLaminaWithTwoShells();
        break;
      case 'LAMINA_WITH_HOLE':
        mySolid = GeneralModelsBuilder.createLaminaWithHole();
        break;
      case 'FONT_BLOCK':
        mySolid = GeneralModelsBuilder.createFontBlock(
          resources.fontReader,
          GeneralModelsBuilder.FONT_BLOCK_FONT_FILE,
          'A',
        );

        translationMatrix = new Matrix4x4d();
        translationMatrix = translationMatrix.translation(0.0, 0.0, 0.1);

        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
          mySolid,
          mySolid.findFace(1),
          translationMatrix,
        );

        break;
      case 'GLUED_CYLINDERS':
        mySolid = GeneralModelsBuilder.createGluedCilinders();
        break;
      case 'EULER_OPERATORS_TEST':
        mySolid = GeneralModelsBuilder.eulerOperatorsTest();
        break;
      case 'ROTATIONAL_SWEEP':
        mySolid = GeneralModelsBuilder.rotationalSweepTest();
        break;
      case 'SPLIT_TEST_PART_1':
        mySolid = GeneralModelsBuilder.splitTest(1);
        break;
      case 'SPLIT_TEST_PART_2':
        mySolid = GeneralModelsBuilder.splitTest(2);
        break;
      case 'SPLIT_TEST_PART_3':
        mySolid = GeneralModelsBuilder.splitTest(3);
        break;
      case 'CSG_DIRECT':
        GeneralModelsBuilder.printKurlanderAllMotifsProgressHint(model.getCsgSample(), 1);
        csgPreviewOperands = GeneralModelsBuilder.createCsgHudPreviewOperands(
          model.getCsgSample(),
          model.getKurlanderBowlSingleMotifIndex(),
        );
        model.setCsgPreviewOperandA(csgPreviewOperands[0]!);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]!);
        mySolid = GeneralModelsBuilder.csgTest(
          1,
          model.getCsgOperation(),
          model.getCsgSample(),
          model.isDebugCsg(),
          model.getKurlanderBowlSingleMotifIndex(),
        );
        model.setDebugCsg(false);
        break;
      case 'CSG_OPERAND1_PARTIAL':
        GeneralModelsBuilder.printKurlanderAllMotifsProgressHint(model.getCsgSample(), 2);
        csgPreviewOperands = GeneralModelsBuilder.createCsgHudPreviewOperands(
          model.getCsgSample(),
          model.getKurlanderBowlSingleMotifIndex(),
        );
        model.setCsgPreviewOperandA(csgPreviewOperands[0]!);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]!);
        mySolid = GeneralModelsBuilder.csgTest(
          2,
          model.getCsgOperation(),
          model.getCsgSample(),
          model.isDebugCsg(),
          model.getKurlanderBowlSingleMotifIndex(),
        );
        model.setDebugCsg(false);
        break;
      case 'CSG_OPERAND2_PARTIAL':
        GeneralModelsBuilder.printKurlanderAllMotifsProgressHint(model.getCsgSample(), 3);
        csgPreviewOperands = GeneralModelsBuilder.createCsgHudPreviewOperands(
          model.getCsgSample(),
          model.getKurlanderBowlSingleMotifIndex(),
        );
        model.setCsgPreviewOperandA(csgPreviewOperands[0]!);
        model.setCsgPreviewOperandB(csgPreviewOperands[1]!);
        mySolid = GeneralModelsBuilder.csgTest(
          3,
          model.getCsgOperation(),
          model.getCsgSample(),
          model.isDebugCsg(),
          model.getKurlanderBowlSingleMotifIndex(),
        );
        model.setDebugCsg(false);
        break;
      case 'FEATURED_OBJECT':
        mySolid = GeneralModelsBuilder.featuredObject();
        break;
      case 'IMPORT_OR_FEATURED_OBJECT':
        // Java: `new File("/tmp/solid.bin").exists() ? importFromFile(...) :
        // featuredObject()`. A page has no /tmp; see the class comment.
        mySolid = GeneralModelsBuilder.featuredObject();
        break;
      case 'STEP_IMPORT':
        mySolid = GeneralModelsBuilder.importFromStepFile(
          resources,
          GeneralModelsBuilder.KURLANDER_BOWL_STEP_FILE,
        );
        break;
      case 'HOLLOW_BOX':
        mySolid = GeneralModelsBuilder.createHollowBox();
        break;
      case 'HOLED_BOX':
      default:
        mySolid = GeneralModelsBuilder.createHoledBox();
        break;
    }

    return mySolid;
  }

  private static printKurlanderAllMotifsProgressHint(sample: CsgSampleNames, part: number): void {
    if (part !== 1) {
      GeneralModelsBuilder.printProgressMessage(
        'KURLANDER_BOWL_ALL_MOTIFS selected as operand preview; ' +
          'full motif processing only runs in CSG_DIRECT.',
      );
      return;
    }
    GeneralModelsBuilder.printProgressMessage(
      'KURLANDER_BOWL_ALL_MOTIFS selected; full motif processing will start.',
    );
  }

  private static printProgressMessage(message: string): void {
    console.log(message);
    console.error(message);
  }

  private static importFromStepFile(
    resources: GeneralModelsResources,
    filename: string,
  ): PolyhedralBoundedSolid {
    try {
      const bytes: Uint8Array | undefined = resources.stepFiles.get(filename);
      if (bytes === undefined) {
        throw new Error(filename + ' (No such file or directory)');
      }
      const solid: PolyhedralBoundedSolid | null = StepReader.readSolid(
        new ByteArrayInputStream(bytes),
      );
      if (solid === null) {
        Logger.reportMessage(
          GeneralModelsBuilder,
          VSDK.WARNING,
          'importFromStepFile',
          'StepReader returned null for ' + filename,
        );
        return GeneralModelsBuilder.createHoledBox();
      }
      return solid;
    } catch (e) {
      Logger.reportMessageWithException(
        GeneralModelsBuilder,
        VSDK.WARNING,
        'importFromStepFile',
        'Could not read STEP file ' + filename + ' — falling back to holed box.',
        e instanceof Error ? e : new Error(String(e)),
      );
      return GeneralModelsBuilder.createHoledBox();
    }
  }

  static createBox(boxSize: Vector3Dd): PolyhedralBoundedSolid {
    let R = new Matrix4x4d();
    R = R.translation(0.55, 0.55, 0.55);

    const b = new Box(boxSize);
    const solid: PolyhedralBoundedSolid = b.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    return solid;
  }

  /** Java's `createSphere(double)` and `createSphere(double, int, int)`. */
  static createSphere(
    r: number,
    subdivisionCircunference = 16,
    subdivisionHeight = 8,
  ): PolyhedralBoundedSolid {
    let R = new Matrix4x4d();
    R = R.translation(0.55, 0.55, 0.55);

    const s = new Sphere(r);
    const solid: PolyhedralBoundedSolid = s.exportToPolyhedralBoundedSolid(
      subdivisionCircunference,
      subdivisionHeight,
    );
    PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    return solid;
  }

  /** Java's `createCone(double, double, double)` and its subdivided overload. */
  static createCone(
    r1: number,
    r2: number,
    h: number,
    subdivisionCircunference = 36,
    subdivisionHeight = 1,
  ): PolyhedralBoundedSolid {
    let R = new Matrix4x4d();
    R = R.translation(0.55, 0.55, 0.05);

    const c = new Cone(r1, r2, h);
    const solid: PolyhedralBoundedSolid = c.exportToPolyhedralBoundedSolid(
      subdivisionCircunference,
      subdivisionHeight,
    );
    PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    return solid;
  }

  /** Java's `createCylinder(double, double)` and its subdivided overload. */
  static createCylinder(
    r: number,
    h: number,
    subdivisionCircunference = 36,
    subdivisionHeight = 1,
  ): PolyhedralBoundedSolid {
    let R = new Matrix4x4d();
    R = R.translation(0.55, 0.55, 0.05);

    const c = new Cone(r, r, h);
    const solid: PolyhedralBoundedSolid = c.exportToPolyhedralBoundedSolid(
      subdivisionCircunference,
      subdivisionHeight,
    );
    PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    return solid;
  }

  private static buildCsgMoonBlock(): PolyhedralBoundedSolid[] {
    const cylinderA: PolyhedralBoundedSolid = GeneralModelsBuilder.createCylinder(0.5, 1.0);
    const cylinderB: PolyhedralBoundedSolid = GeneralModelsBuilder.createCylinder(0.5, 2);

    let T = new Matrix4x4d();
    T = T.translation(0.275, 0.0, -0.5);
    PolyhedralBoundedSolidModeler.applyTransformation(cylinderB, T);

    PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderA);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderB);

    const operands: PolyhedralBoundedSolid[] = new Array<PolyhedralBoundedSolid>(2);
    operands[0] = cylinderA;
    operands[1] = cylinderB;
    return operands;
  }

  static createCsgLampShell(
    subdivisionCircunference: number,
    subdivisionHeight: number,
  ): PolyhedralBoundedSolid {
    const outerRadius = 0.5;
    const innerRadius = 0.45;

    const outerSphere: PolyhedralBoundedSolid = GeneralModelsBuilder.createSphere(
      outerRadius,
      subdivisionCircunference,
      subdivisionHeight,
    );
    const innerSphere: PolyhedralBoundedSolid = GeneralModelsBuilder.createSphere(
      innerRadius,
      subdivisionCircunference,
      subdivisionHeight,
    );

    PolyhedralBoundedSolidStatistics.reset();
    const sphericalShell: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.setOp(
      outerSphere,
      innerSphere,
      PolyhedralBoundedSolidModeler.SUBTRACT,
      false,
    );

    // Cube fully contains shell in X/Y, starts below it in Z and stops at
    // ~80% of shell height to mimic the unperforated lamp bowl profile.
    const clipCubeGeometry = new Box(new Vector3Dd(1.4, 1.4, 1.05));
    const clipCube: PolyhedralBoundedSolid = clipCubeGeometry.exportToPolyhedralBoundedSolid();
    let cubeMove = new Matrix4x4d();
    cubeMove = cubeMove.translation(0.55, 0.55, 0.325);
    PolyhedralBoundedSolidModeler.applyTransformation(clipCube, cubeMove);

    PolyhedralBoundedSolidStatistics.reset();
    const result: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.setOp(
      sphericalShell,
      clipCube,
      PolyhedralBoundedSolidModeler.INTERSECTION,
      false,
    );
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
    return result;
  }

  static createArrow(p1: number, p2: number, p3: number, p4: number): PolyhedralBoundedSolid {
    let R = new Matrix4x4d();
    R = R.translation(0.55, 0.55, 0.05);

    const a = new Arrow(p1, p2, p3, p4);
    const solid: PolyhedralBoundedSolid = a.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    return solid;
  }

  /**
   * PRE:
   * Works on the output of `createBox` method, for a box from <0.1, 0.1, 0.1>
   * to <1, 1, 1>
   */
  static extrudeBox(solid: PolyhedralBoundedSolid): void {
    //- Cube modification to holed box --------------------------------
    PolyhedralBoundedSolidEulerOperators.smev(solid, 6, 5, 9, new Vector3Dd(0.3, 0.3, 1));
    PolyhedralBoundedSolidEulerOperators.kemr(solid, 6, 6, 5, 9, 9, 5);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 6, 9, 10, new Vector3Dd(0.8, 0.3, 1));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 6, 10, 11, new Vector3Dd(0.8, 0.8, 1));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 6, 11, 12, new Vector3Dd(0.3, 0.8, 1));
    PolyhedralBoundedSolidEulerOperators.mef(solid, 6, 6, 9, 10, 12, 11, 7);

    //- Box extrusion -------------------------------------------------
    PolyhedralBoundedSolidEulerOperators.smev(solid, 7, 9, 13, new Vector3Dd(0.3, 0.3, 0.1));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 7, 10, 14, new Vector3Dd(0.8, 0.3, 0.1));
    PolyhedralBoundedSolidEulerOperators.mef(solid, 7, 7, 13, 9, 14, 10, 8);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 7, 11, 15, new Vector3Dd(0.8, 0.8, 0.1));
    PolyhedralBoundedSolidEulerOperators.mef(solid, 7, 7, 14, 10, 15, 11, 9);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 7, 12, 16, new Vector3Dd(0.3, 0.8, 0.1));
    PolyhedralBoundedSolidEulerOperators.mef(solid, 7, 7, 15, 11, 16, 12, 10);
    PolyhedralBoundedSolidEulerOperators.mef(solid, 7, 7, 13, 14, 16, 12, 11);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
  }

  /**
   * This method implements the example presented in section [MANT1988].9.3,
   * and figure [MANT1988].9.11.
   */
  static createHoledBox(): PolyhedralBoundedSolid {
    const solid: PolyhedralBoundedSolid = GeneralModelsBuilder.createBox(
      new Vector3Dd(0.9, 0.9, 0.9),
    );
    GeneralModelsBuilder.extrudeBox(solid);
    PolyhedralBoundedSolidEulerOperators.kfmrh(solid, 2, 11);
    //R = R.translation(-0.55, -0.55, -0.55);
    //PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

    return solid;
  }

  /**
   * This method is a test for the solution to problem [MANT1988].15.1 on case of one solid
   * fully inside another.
   */
  static createHollowBox(): PolyhedralBoundedSolid {
    // Outer box.
    const solidA: PolyhedralBoundedSolid = GeneralModelsBuilder.createBox(
      new Vector3Dd(0.9, 0.9, 0.9),
    );

    // Inner box at 80% size, centered at the same position as solidA.
    const solidB: PolyhedralBoundedSolid = GeneralModelsBuilder.createBox(
      new Vector3Dd(0.72, 0.72, 0.72),
    );

    // Hollow box = outer box minus inner box.
    const result: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.setOp(
      solidA,
      solidB,
      PolyhedralBoundedSolidModeler.SUBTRACT,
    );

    return result;
  }

  static createLaminaWithTwoShells(): PolyhedralBoundedSolid {
    //- Basic lamina --------------------------------------------------
    let R = new Matrix4x4d();

    R = R.translation(0.55, 0.55, 0.55);
    const solid = new PolyhedralBoundedSolid();
    PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(-0.5, -0.5, 0), 1, 1);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 4, new Vector3Dd(-0.5, 0.0, 0));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 4, 3, new Vector3Dd(0.5, 0.0, 0));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 3, 2, new Vector3Dd(0.5, -0.5, 0));
    PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 1, 4, 2, 3, 2);

    //- Hole ----------------------------------------------------------
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 5, new Vector3Dd(-0.3, 0.1, 0));
    PolyhedralBoundedSolidEulerOperators.kemr(solid, 1, 1, 1, 5, 5, 1);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 5, 6, new Vector3Dd(0.0, 0.4, 0));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 6, 7, new Vector3Dd(0.3, 0.1, 0));
    PolyhedralBoundedSolidEulerOperators.mef(
      solid,
      1 /* face1 */,
      1 /* face2 */,
      5 /* v1 */,
      6 /* v2 */,
      7 /* v3 */,
      6 /* v4 */,
      3 /* newfaceid */,
    );

    PolyhedralBoundedSolidEulerOperators.kfmrh(solid, 2, 3);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    return solid;
  }

  static createLaminaWithHole(): PolyhedralBoundedSolid {
    //- Basic lamina --------------------------------------------------
    let R = new Matrix4x4d();

    R = R.translation(0.55, 0.55, 0.55);
    const solid = new PolyhedralBoundedSolid();
    PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(-0.5, -0.5, 0), 1, 1);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 4, new Vector3Dd(-0.5, 0.5, 0));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 4, 3, new Vector3Dd(0.5, 0.5, 0));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 3, 2, new Vector3Dd(0.5, -0.5, 0));
    PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 1, 4, 2, 3, 2);

    //- Hole ----------------------------------------------------------
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 5, new Vector3Dd(-0.3, -0.3, 0));
    PolyhedralBoundedSolidEulerOperators.kemr(solid, 1, 1, 1, 5, 5, 1);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 5, 6, new Vector3Dd(0.0, 0.3, 0));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 6, 7, new Vector3Dd(0.3, -0.3, 0));
    PolyhedralBoundedSolidEulerOperators.mef(
      solid,
      1 /* face1 */,
      1 /* face2 */,
      5 /* v1 */,
      6 /* v2 */,
      7 /* v3 */,
      6 /* v4 */,
      3 /* newfaceid */,
    );

    PolyhedralBoundedSolidEulerOperators.kfmrh(solid, 2, 3);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidModeler.applyTransformation(solid, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    return solid;
  }

  /**
   * Java constructs its `AwtFontReader` here; the browser reader is handed in,
   * already holding the font file, for the reason recorded on the class.
   */
  static createFontBlock(
    fontReader: WebFontReader,
    fontFile: string,
    msg: string | null,
  ): PolyhedralBoundedSolid {
    if (msg === null || msg.trim().length === 0) {
      throw new Error('Font block text cannot be blank');
    }

    const codePoint: number = msg.codePointAt(0)!;
    const character: string = String.fromCodePoint(codePoint);
    const curve: ParametricCurve | null = fontReader.extractGlyph(fontFile, character);
    if (curve === null) {
      throw new Error("Unable to extract glyph '" + character + "' from font " + fontFile);
    }
    if (curve.types === null || curve.types.length < 2) {
      throw new Error(
        "Glyph '" +
          character +
          "' from font " +
          fontFile +
          ' did not produce a usable parametric curve',
      );
    }
    curve.setApproximationSteps(8);

    return PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);
  }

  /**
   * After algorithm decribed on section [MANT1988].12.4, and program
   * [MANT1988].12.7.
   */
  private static glue(
    solid1: PolyhedralBoundedSolid,
    solid2: PolyhedralBoundedSolid,
    faceid1: number,
    faceid2: number,
  ): void {
    solid1.merge(solid2);
    PolyhedralBoundedSolidEulerOperators.kfmrh(solid1, faceid1, faceid2);
    PolyhedralBoundedSolidTopologyEditing.loopGlue(solid1, faceid1);
  }

  /**
   * This method builds a test solid for evaluating the gluing algorithm in
   * a controlled way, as proposed on the example from section [MANT1988].12.4.
   * It is similar to the solid shown on figure [MANT1988].12.2.
   */
  static createGluedCilinders(): PolyhedralBoundedSolid {
    //- Create cilynder 1 ---------------------------------------------
    let T = new Matrix4x4d();
    T = T.translation(0, 0, 0.4);

    const solid1: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.createCircularLamina(
      0.0,
      0.0,
      0.5,
      0.0,
      6,
    );
    PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
      solid1,
      solid1.findFace(1),
      T,
    );
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid1);

    //-----------------------------------------------------------------
    const ang: number = (2 * Math.PI) / 6;
    let R = new Matrix4x4d();
    const a = new Vector3Dd(0.5, 0, 0);
    const b = new Vector3Dd(0.5 * Math.cos(ang), 0.5 * Math.sin(ang), 0);
    let c: Vector3Dd = a.add(b);
    c = c.multiply(0.5);
    R = R.translation(-c.x(), c.y(), c.z());
    PolyhedralBoundedSolidModeler.applyTransformation(solid1, R);

    //- Create cilynder 2 ---------------------------------------------
    const solid2: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.createCircularLamina(
      0.0,
      0.0,
      0.5,
      0.0,
      6,
    );
    PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
      solid2,
      solid2.findFace(1),
      T,
    );
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid2);

    //-----------------------------------------------------------------
    R = R.translation(c.x(), -c.y(), c.z());
    PolyhedralBoundedSolidModeler.applyTransformation(solid2, R);

    //-----------------------------------------------------------------
    GeneralModelsBuilder.glue(solid1, solid2, 8, 13);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid1);
    PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solid1);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid1);

    return solid1;
  }

  static eulerOperatorsTest(): PolyhedralBoundedSolid {
    /*
        solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0.1, 0.1, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(1.0, 0.2, 0));

        face = solid.findFace(1);
        //PolyhedralBoundedSolidEulerOperators.lkev(solid, face.findHalfEdge(2), face.findHalfEdge(1));

        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3, new Vector3Dd(0.5, 1, 0));

        //-----------------------------------------------------------------

        h1 = face.findHalfEdge(3);
        h2 = face.findHalfEdge(1);

        PolyhedralBoundedSolidEulerOperators.lmef(solid, h1, h2, 2);

        //-----------------------------------------------------------------

        h1 = face.findHalfEdge(1);
        PolyhedralBoundedSolidEulerOperators.lmev(solid, h1, h1, solid.getMaxVertexId()+1, new Vector3Dd(0.1, 0.1, 0.4));
*/

    /*
        solid = createBox(new Vector3Dd(1, 1, 1));

        //-----------------------------------------------------------------
        face = solid.findFace(3);
        h1 = face.findHalfEdge(1);
        face = solid.findFace(2);
        h2 = face.findHalfEdge(1);

        PolyhedralBoundedSolidEulerOperators.lmev(solid, h1, h2, solid.getMaxVertexId()+1, new Vector3Dd(0.55, 0.05, 0.05));

        PolyhedralBoundedSolidEulerOperators.lkev(solid, h1, h1.mirrorHalfEdge());
*/

    //-----------------------------------------------------------------
    const solid = new PolyhedralBoundedSolid();
    PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0.0, 0.0, 0.0), 1, 1);
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(1.0, 0.0, 0.0));
    PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3, new Vector3Dd(1.0, 1.0, 0.0));

    //-----------------------------------------------------------------
    //PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    console.log(solid.toString());
    return solid;
  }

  /**
   * Current method implements a simple and restricted rotational sweep (lathe)
   * algorithm for wires (solids with one face, and one open loop) in the z=0
   * plane, to be rotated about the x axis, as described in section
   * [MANT1988].12.3.2, and presented in program [MANT1988].12.5.
   * This version of the rotational sweep has some limitations and
   * characteristics:
   *   - It is the simpler form of rotational sweep, and serves as the base to
   *     develop complex/generalized versions of the algorithm.
   *   - The rotation axis is fixed to be the x-axis
   *   - The profile path must be open (a "wire" solid with just one face with
   *     one loop, which is open, with a single, connected and nonforking string
   *     of edges)
   *   - All edges must lie on the half plane [y>0,z=0], and must not touch the
   *     x axis.
   *
   * Private and unreferenced in Java too, where the only call to it is
   * commented out in `rotationalSweepTest`.
   */
  private static rotationalSweepVersion1(solid: PolyhedralBoundedSolid, nfaces: number): void {
    let first: _PolyhedralBoundedSolidHalfEdge;
    let cfirst: _PolyhedralBoundedSolidHalfEdge;
    let last: _PolyhedralBoundedSolidHalfEdge;
    let scan: _PolyhedralBoundedSolidHalfEdge | null = null;
    let v: Vector3Dd;
    let M: Matrix4x4d;

    first = solid.getPolygonsList().get(0)!.boundariesList.get(0)!.boundaryStartHalfEdge!;
    while (first.parentEdge !== first.next()!.parentEdge) {
      first = first.next()!;
    }
    last = first.next()!;
    while (last.parentEdge !== last.next()!.parentEdge) {
      last = last.next()!;
    }
    cfirst = first;
    M = new Matrix4x4d();
    M = M.axisRotation((2 * Math.PI) / nfaces, 1, 0, 0);

    let i: number;
    for (i = 0; i < nfaces - 1; i++) {
      v = M.multiply(cfirst.next()!.startingVertex.position);
      PolyhedralBoundedSolidEulerOperators.lmev(
        solid,
        cfirst.next(),
        cfirst.next(),
        solid.getMaxVertexId() + 1,
        v,
      );
      scan = cfirst.next()!;
      while (scan !== last.next()) {
        v = M.multiply(scan.previous()!.startingVertex.position);
        PolyhedralBoundedSolidEulerOperators.lmev(
          solid,
          scan.previous(),
          scan.previous(),
          solid.getMaxVertexId() + 1,
          v,
        );
        PolyhedralBoundedSolidEulerOperators.lmef(
          solid,
          scan.previous()!.previous(),
          scan.next(),
          solid.getMaxFaceId() + 1,
        );
        scan = scan.next()!.next()!.mirrorHalfEdge()!;
      }
      last = scan;
      cfirst = cfirst.next()!.next()!.mirrorHalfEdge()!;
    }
    PolyhedralBoundedSolidEulerOperators.lmef(
      solid,
      cfirst.next(),
      first.mirrorHalfEdge(),
      solid.getMaxFaceId() + 1,
    );
    while (cfirst !== scan) {
      PolyhedralBoundedSolidEulerOperators.lmef(
        solid,
        cfirst,
        cfirst.next()!.next()!.next(),
        solid.getMaxFaceId() + 1,
      );
      cfirst = cfirst.previous()!.mirrorHalfEdge()!.previous()!;
    }
  }

  /**
   * Current method implements a variant of simple and restricted rotational
   * sweep (lathe) algorithm for wires and laminas in the z=0 plane, to be
   * rotated about the x axis, as described in section [MANT1988].12.5, and
   * presented in programs [MANT1988].12.5 and [MANT1988].12.11.
   */
  private static rotationalSweepVersion2(solid: PolyhedralBoundedSolid, nfaces: number): void {
    //-----------------------------------------------------------------
    let first: _PolyhedralBoundedSolidHalfEdge;
    let cfirst: _PolyhedralBoundedSolidHalfEdge;
    let last: _PolyhedralBoundedSolidHalfEdge;
    let scan: _PolyhedralBoundedSolidHalfEdge | null = null;
    let h: _PolyhedralBoundedSolidHalfEdge;
    let tailf: _PolyhedralBoundedSolidFace | null = null;
    let headf: _PolyhedralBoundedSolidFace | null = null;
    let closedFigure = false;
    let v: Vector3Dd;
    let M: Matrix4x4d;

    //-----------------------------------------------------------------
    if (solid.getPolygonsList().size() > 1) {
      // Assume it's a lamina
      closedFigure = true;
      h = solid.getPolygonsList().get(0)!.boundariesList.get(0)!.boundaryStartHalfEdge!;
      PolyhedralBoundedSolidEulerOperators.lmev(
        solid,
        h,
        h.mirrorHalfEdge()!.next(),
        solid.getMaxVertexId() + 1,
        h.startingVertex.position,
      );

      PolyhedralBoundedSolidEulerOperators.lkef(
        solid,
        h.previous()!,
        h.previous()!.mirrorHalfEdge()!,
      );
      headf = solid.getPolygonsList().get(0);
    }

    //-----------------------------------------------------------------
    first = solid.getPolygonsList().get(0)!.boundariesList.get(0)!.boundaryStartHalfEdge!;
    while (first.parentEdge !== first.next()!.parentEdge) {
      first = first.next()!;
    }
    last = first.next()!;
    while (last.parentEdge !== last.next()!.parentEdge) {
      last = last.next()!;
    }
    cfirst = first;
    M = new Matrix4x4d();
    M = M.axisRotation((2 * Math.PI) / nfaces, 1, 0, 0);

    let i: number;
    for (i = 0; i < nfaces - 1; i++) {
      v = M.multiply(cfirst.next()!.startingVertex.position);
      PolyhedralBoundedSolidEulerOperators.lmev(
        solid,
        cfirst.next(),
        cfirst.next(),
        solid.getMaxVertexId() + 1,
        v,
      );
      scan = cfirst.next()!;
      while (scan !== last.next()) {
        v = M.multiply(scan.previous()!.startingVertex.position);
        PolyhedralBoundedSolidEulerOperators.lmev(
          solid,
          scan.previous(),
          scan.previous(),
          solid.getMaxVertexId() + 1,
          v,
        );
        PolyhedralBoundedSolidEulerOperators.lmef(
          solid,
          scan.previous()!.previous(),
          scan.next(),
          solid.getMaxFaceId() + 1,
        );
        scan = scan.next()!.next()!.mirrorHalfEdge()!;
      }
      last = scan;
      cfirst = cfirst.next()!.next()!.mirrorHalfEdge()!;
    }
    tailf = PolyhedralBoundedSolidEulerOperators.lmef(
      solid,
      cfirst.next(),
      first.mirrorHalfEdge(),
      solid.getMaxFaceId() + 1,
    );
    while (cfirst !== scan) {
      PolyhedralBoundedSolidEulerOperators.lmef(
        solid,
        cfirst,
        cfirst.next()!.next()!.next(),
        solid.getMaxFaceId() + 1,
      );
      cfirst = cfirst.previous()!.mirrorHalfEdge()!.previous()!;
    }

    //-----------------------------------------------------------------
    if (closedFigure) {
      PolyhedralBoundedSolidEulerOperators.lkfmrh(solid, headf!, tailf!);
      PolyhedralBoundedSolidTopologyEditing.loopGlue(solid, headf!.id);
    }

    //-----------------------------------------------------------------
  }

  /**
   * This method builds a test solid for evaluating the second version of the
   * rotational sweep algorithm in a controlled way, as proposed on the example
   * from section [MANT1988].12.5.
   * The created solid is is similar to the solid shown on figure
   * [MANT1988].12.5. (in particular when seting 4 sides);
   */
  private static createTestTorus(nsides: number, nrad: number): PolyhedralBoundedSolid {
    const center = new Vector3Dd(0.5, 0.5, 0);

    const solid: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.createCircularLamina(
      center.x(),
      center.y(),
      0.2,
      0.0,
      nsides,
    );

    // For seting 4 sided case to be equal to figure [MANT1988].12.5.
    // an aditional rotation must be applied to the lamina prior to the
    // rotational sweep.
    let T1 = new Matrix4x4d();
    let T2 = new Matrix4x4d();
    let R = new Matrix4x4d();
    T1 = T1.translation(center.multiply(-1));
    T2 = T2.translation(center);
    R = R.axisRotation(Math.PI / nsides, 0, 0, 1);
    const M: Matrix4x4d = T2.multiply(R.multiply(T1));
    PolyhedralBoundedSolidModeler.applyTransformation(solid, M);

    GeneralModelsBuilder.rotationalSweepVersion2(solid, nrad);
    return solid;
  }

  static rotationalSweepTest(): PolyhedralBoundedSolid {
    //-----------------------------------------------------------------
    /*
        solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0.75, 0.25, 0), 1, 1);
        PolyhedralBoundedSolidModeler.addArc(solid, 1, 1, 0.5, 0.25, 0.25, 0.0, 0.0, 90.0, 10);
        rotationalSweepVersion1(solid, 20);
*/
    const solid: PolyhedralBoundedSolid = GeneralModelsBuilder.createTestTorus(4, 16);
    //-----------------------------------------------------------------
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

    return solid;
  }

  static splitTest(part: number): PolyhedralBoundedSolid {
    //- Basic lamina --------------------------------------------------
    //PolyhedralBoundedSolid solid = createHoledBox();
    //PolyhedralBoundedSolid solid = createBox(new Vector3Dd(0.9, 0.9, 0.9));

    let solid: PolyhedralBoundedSolid = SimpleTestGeometryLibrary.createTestObjectMANT1986_1();

    /*
        Matrix4x4d R = new Matrix4x4d();
        PolyhedralBoundedSolid solid;
        R = R.translation(0.55, 0.55, 0.55);
        solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0.00+0.05, 0.00+0.05, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(0.94+0.05, 0.00+0.05, 0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3, new Vector3Dd(0.94+0.05, 0.46+0.05, 0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 3, 4, new Vector3Dd(0.00+0.05, 0.30+0.05, 0));
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 4, 3, 1, 2, 2);
        Matrix4x4d T = new Matrix4x4d();
        T = T.translation(0, 0, 0.4);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), T);
*/

    //-----------------------------------------------------------------
    // Java's `ArrayList`s: the ported splitter takes plain arrays.
    const solidsAbove: PolyhedralBoundedSolid[] = [];
    const solidsBelow: PolyhedralBoundedSolid[] = [];

    const sp = new InfinitePlane(new Vector3Dd(0, 0, 1) /*n*/, new Vector3Dd(0, 0, 0.3) /*p*/);

    //        sp = new InfinitePlane(new Vector3Dd(0, 0, 1) /*n*/,
    //                               new Vector3Dd(0, 0, 0.5) /*p*/);

    //-----------------------------------------------------------------
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

    if (part === 1) {
      return solid;
    }

    PolyhedralBoundedSolidModeler.split(solid, sp, solidsAbove, solidsBelow);

    //-----------------------------------------------------------------
    if (part === 3) {
      solid = GeneralModelsBuilder.listGet(solidsBelow, 0);
    } else {
      solid = GeneralModelsBuilder.listGet(solidsAbove, 0);
    }

    PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solid);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

    return solid;
  }

  /** Java's `ArrayList.get(int)`, which throws on an index out of range. */
  private static listGet(list: PolyhedralBoundedSolid[], index: number): PolyhedralBoundedSolid {
    if (index < 0 || index >= list.length) {
      throw new RangeError('Index ' + index + ' out of bounds for length ' + list.length);
    }
    return list[index]!;
  }

  /**
   * This method builds a test sample pair of solids for evaluating
   * the set operations algorithm in a controlled way.
   * This set correspond to a simple cases for CSG operations test: two
   * blocks without intersecting vertex pairs (only edge/face
   * intersections are present). The resulting gluing face can be a variation
   * of the method `createTestObjectsPairMANT1986_2`, if blocks are translated
   * so their parallel faces don't touch; or can be one simple test case for
   * the complex sector intersection.
   */
  private static buildCsgTest2(): PolyhedralBoundedSolid[] {
    const operands: PolyhedralBoundedSolid[] = new Array<PolyhedralBoundedSolid>(2);

    //-----------------------------------------------------------------
    let R = new Matrix4x4d();
    R = R.translation(0.5, 0.5, 0.15);

    let box = new Box(new Vector3Dd(1, 0.5, 0.3));
    const a: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(a, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

    //-----------------------------------------------------------------
    R = new Matrix4x4d();
    R = R.translation(0.5, 0.5, 0.15 + 0.3);

    box = new Box(new Vector3Dd(0.5, 1, 0.3));
    const b: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(b, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

    //-----------------------------------------------------------------
    operands[0] = a;
    operands[1] = b;

    return operands;
  }

  /**
   * Makes a hollowed brick from two L-shaped boxes. Note that on UNION
   * operation this object leads to an interesting topological problem for
   * PolyhedralBoundedSolid.maximizeFaces operation.
   */
  private static buildCsgTest4(): PolyhedralBoundedSolid[] {
    const operands: PolyhedralBoundedSolid[] = new Array<PolyhedralBoundedSolid>(2);

    let T: Matrix4x4d;
    let box: Box;

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.5, 0.1, 0.1);
    box = new Box(new Vector3Dd(1, 0.2, 0.2));
    const a: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(a, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.5, 0.9, 0.1);
    box = new Box(new Vector3Dd(1, 0.2, 0.2));
    const b: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(b, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.1, 0.5, 0.1);
    box = new Box(new Vector3Dd(0.2, 1, 0.2));
    const c: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(c, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(c);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.9, 0.5, 0.1);
    box = new Box(new Vector3Dd(0.2, 1, 0.2));
    const d: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(d, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(d);

    //-----------------------------------------------------------------
    const x: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.setOp(
      b,
      c,
      PolyhedralBoundedSolidModeler.UNION,
    );
    const y: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.setOp(
      a,
      d,
      PolyhedralBoundedSolidModeler.UNION,
    );

    operands[0] = x;
    operands[1] = y;
    return operands;
  }

  private static buildCsgTest5(): PolyhedralBoundedSolid[] {
    const operands: PolyhedralBoundedSolid[] = new Array<PolyhedralBoundedSolid>(2);

    let T: Matrix4x4d;
    let box: Box;

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.5, 0.1, 0.1);
    box = new Box(new Vector3Dd(1, 0.2, 0.2));
    const a: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(a, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.5, 0.9, 0.1);
    box = new Box(new Vector3Dd(1, 0.2, 0.2));
    const b: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(b, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.1, 0.5, 0.1);
    box = new Box(new Vector3Dd(0.2, 1, 0.2));
    const c: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(c, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(c);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.9, 0.5, 0.1);
    box = new Box(new Vector3Dd(0.2, 1, 0.2));
    const d: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(d, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(d);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.1, 0.5, 0.1);
    box = new Box(new Vector3Dd(0.2, 1, 0.2));
    const e: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(e, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(e);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.1, 0.5, 0.9);
    box = new Box(new Vector3Dd(0.2, 1, 0.2));
    const f: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(f, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(f);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.1, 0.1, 0.5);
    box = new Box(new Vector3Dd(0.2, 0.2, 1));
    const g: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(g, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(g);

    //-----------------------------------------------------------------
    T = new Matrix4x4d();
    T = T.translation(0.1, 0.9, 0.5);
    box = new Box(new Vector3Dd(0.2, 0.2, 1));
    const h: PolyhedralBoundedSolid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler.applyTransformation(h, T);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(h);

    //-----------------------------------------------------------------
    /*
        ac = PolyhedralBoundedSolidModeler.setOp(a, c, PolyhedralBoundedSolidModeler.UNION);
        bd = PolyhedralBoundedSolidModeler.setOp(b, d, PolyhedralBoundedSolidModeler.UNION);
        abcd = PolyhedralBoundedSolidModeler.setOp(bd, ac, PolyhedralBoundedSolidModeler.UNION);
        eg = PolyhedralBoundedSolidModeler.setOp(e, g, PolyhedralBoundedSolidModeler.UNION);
        fh = PolyhedralBoundedSolidModeler.setOp(f, h, PolyhedralBoundedSolidModeler.UNION);
        efgh = PolyhedralBoundedSolidModeler.setOp(eg, fh, PolyhedralBoundedSolidModeler.UNION);
        total = PolyhedralBoundedSolidModeler.setOp(abcd, efgh, PolyhedralBoundedSolidModeler.UNION);
*/
    const ac: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.setOp(
      a,
      c,
      PolyhedralBoundedSolidModeler.UNION,
    );

    operands[0] = ac;
    operands[1] = g;
    return operands;
  }

  /** Java's `csgTest(int, CsgOperationNames, CsgSampleNames, boolean[, int])`. */
  static csgTest(
    part: number,
    op: CsgOperationNames,
    sample: CsgSampleNames,
    withDebug: boolean,
    kurlanderSingleMotifIndex = 0,
  ): PolyhedralBoundedSolid {
    let res: PolyhedralBoundedSolid | null = null;

    console.log(
      'Creating C.S.G. test object with parts ' +
        part +
        ', ' +
        'operation ' +
        csgOperationLabel(op) +
        ', and sample pair ' +
        csgSampleLabel(sample),
    );

    const operands: PolyhedralBoundedSolid[] = GeneralModelsBuilder.createCsgOperands(
      sample,
      kurlanderSingleMotifIndex,
    );
    PolyhedralBoundedSolidStatistics.reset();

    //-----------------------------------------------------------------
    if (op === 'UNION') {
      res = PolyhedralBoundedSolidModeler.setOp(
        operands[0]!,
        operands[1]!,
        PolyhedralBoundedSolidModeler.UNION,
        withDebug,
      );
    } else if (op === 'INTERSECTION') {
      res = PolyhedralBoundedSolidModeler.setOp(
        operands[0]!,
        operands[1]!,
        PolyhedralBoundedSolidModeler.INTERSECTION,
        withDebug,
      );
    } else if (op === 'DIFFERENCE_A_MINUS_B') {
      res = PolyhedralBoundedSolidModeler.setOp(
        operands[0]!,
        operands[1]!,
        PolyhedralBoundedSolidModeler.SUBTRACT,
        withDebug,
      );
    } else {
      res = PolyhedralBoundedSolidModeler.setOp(
        operands[1]!,
        operands[0]!,
        PolyhedralBoundedSolidModeler.SUBTRACT,
        withDebug,
      );
    }

    //-----------------------------------------------------------------
    //PolyhedralBoundedSolidValidationEngine.validateIntermediate(operands[0]);
    //PolyhedralBoundedSolidValidationEngine.validateIntermediate(operands[1]);
    //PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);

    if (part === 2) {
      return operands[0]!;
    }
    if (part === 3) {
      return operands[1]!;
    }
    return res;
  }

  /** Java's `createCsgOperands(CsgSampleNames[, int])`. */
  private static createCsgOperands(
    sample: CsgSampleNames,
    kurlanderSingleMotifIndex = 0,
  ): PolyhedralBoundedSolid[] {
    let operands: PolyhedralBoundedSolid[];

    switch (sample) {
      case 'MANT1986_2':
        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();
        break;
      case 'STACKED_BLOCKS':
        operands = GeneralModelsBuilder.buildCsgTest2();
        break;
      case 'MANT1988_3':
        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_3();
        break;
      case 'HOLLOW_BRICK':
        operands = GeneralModelsBuilder.buildCsgTest4();
        break;
      case 'CROSS_PAIR':
        operands = GeneralModelsBuilder.buildCsgTest5();
        break;
      case 'MOON_BLOCK':
        operands = GeneralModelsBuilder.buildCsgMoonBlock();
        break;
      case 'MANT1988_15_2_HOLED':
        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(-1);
        break;
      case 'MANT1988_15_2_LIMIT_DIFFERENCE':
        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(0);
        break;
      case 'MANT1988_15_2_OPEN_DIFFERENCE':
        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(1);
        break;
      case 'MANT1988_6_13':
        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_6_13();
        break;
      case 'MANT1988_15_1':
        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();
        break;
      case 'KURLANDER_BOWL_SINGLE_MOTIF':
      default:
        operands =
          CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(kurlanderSingleMotifIndex);
        break;
    }

    return operands;
  }

  /** Java's `createCsgHudPreviewOperands(CsgSampleNames[, int])`. */
  private static createCsgHudPreviewOperands(
    sample: CsgSampleNames,
    kurlanderSingleMotifIndex = 0,
  ): PolyhedralBoundedSolid[] {
    switch (sample) {
      case 'MANT1986_2':
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();
      case 'MANT1988_3':
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_3();
      case 'MANT1988_15_2_HOLED':
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(-1);
      case 'MANT1988_15_2_LIMIT_DIFFERENCE':
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(0);
      case 'MANT1988_15_2_OPEN_DIFFERENCE':
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(1);
      case 'MANT1988_6_13':
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_6_13();
      case 'MANT1988_15_1':
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();
      case 'KURLANDER_BOWL_SINGLE_MOTIF':
        return CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(kurlanderSingleMotifIndex);
      default:
        return GeneralModelsBuilder.createCsgOperands(sample, kurlanderSingleMotifIndex);
    }
  }

  static featuredObject(): PolyhedralBoundedSolid {
    return SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
    /*
        PolyhedralBoundedSolid ops[];
        ops = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2();
        return PolyhedralBoundedSolidModeler.setOp(ops[0], ops[1],
                                      PolyhedralBoundedSolidModeler.DIFFERENCE);
*/
  }
}
