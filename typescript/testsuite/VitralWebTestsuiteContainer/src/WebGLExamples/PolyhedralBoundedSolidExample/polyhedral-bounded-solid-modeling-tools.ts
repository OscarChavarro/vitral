//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import type { PolyhedralBoundedSolid, Vector3Dd } from '@vitral/base';
import type { WebFontReader } from '@vitral/webgl';
import type { CsgOperationNames } from './models/csg-operation-names';
import type { CsgSampleNames } from './models/csg-sample-names';
import type { DebuggerModel } from './models/debugger-model';
import { GeneralModelsBuilder, type GeneralModelsResources } from './models/general-models-builder';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/PolyhedralBoundedSolidModelingTools.java`:
 * the program's façade over `GeneralModelsBuilder`, one delegating method per
 * builder.
 *
 * `buildSolid` and `createFontBlock` also carry the fetched resources the
 * browser builder needs, for the reason `GeneralModelsBuilder` records.
 */
export class PolyhedralBoundedSolidModelingTools {
  private constructor() {}

  static buildSolid(
    model: DebuggerModel,
    resources: GeneralModelsResources,
  ): PolyhedralBoundedSolid | null {
    return GeneralModelsBuilder.buildSolid(model, resources);
  }

  static createBox(boxSize: Vector3Dd): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createBox(boxSize);
  }

  static createSphere(r: number): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createSphere(r);
  }

  static createCone(r1: number, r2: number, h: number): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createCone(r1, r2, h);
  }

  static createCylinder(r: number, h: number): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createCylinder(r, h);
  }

  static createCsgLampShell(
    subdivisionCircunference: number,
    subdivisionHeight: number,
  ): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createCsgLampShell(subdivisionCircunference, subdivisionHeight);
  }

  static createArrow(p1: number, p2: number, p3: number, p4: number): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createArrow(p1, p2, p3, p4);
  }

  static extrudeBox(solid: PolyhedralBoundedSolid): void {
    GeneralModelsBuilder.extrudeBox(solid);
  }

  static createHoledBox(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createHoledBox();
  }

  static createHollowBox(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createHollowBox();
  }

  static createLaminaWithTwoShells(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createLaminaWithTwoShells();
  }

  static createLaminaWithHole(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createLaminaWithHole();
  }

  static createFontBlock(
    fontReader: WebFontReader,
    fontFile: string,
    msg: string,
  ): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createFontBlock(fontReader, fontFile, msg);
  }

  static createGluedCilinders(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.createGluedCilinders();
  }

  static eulerOperatorsTest(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.eulerOperatorsTest();
  }

  static rotationalSweepTest(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.rotationalSweepTest();
  }

  static splitTest(part: number): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.splitTest(part);
  }

  static csgTest(
    part: number,
    op: CsgOperationNames,
    sample: CsgSampleNames,
    withDebug: boolean,
  ): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.csgTest(part, op, sample, withDebug);
  }

  static featuredObject(): PolyhedralBoundedSolid {
    return GeneralModelsBuilder.featuredObject();
  }
}
