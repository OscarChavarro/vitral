import {
  TangibleInterfaceEvent2RayGizmoMapper,
  type TangibleInterfaceEvent,
  type TangibleInterfaceListener,
} from '@vitral/base';
import type { MeshModel } from '../model/mesh-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MeshExample/src/gui/TangibleInterfaceInteractionTechniques.java`.
 */
export class TangibleInterfaceInteractionTechniques implements TangibleInterfaceListener {
  private static readonly RAY_CUBE_TANGIBLE_ELEMENT_ID = 'rayCube1';

  private readonly mapper: TangibleInterfaceEvent2RayGizmoMapper;

  constructor(
    private readonly model: MeshModel,
    private readonly repaintCallback: () => void,
  ) {
    this.mapper = new TangibleInterfaceEvent2RayGizmoMapper(model.getCamera());
  }

  tangibleInterfaceEventReceived(event: TangibleInterfaceEvent): void {
    if (event.getId() !== TangibleInterfaceInteractionTechniques.RAY_CUBE_TANGIBLE_ELEMENT_ID) {
      return;
    }

    this.mapper.map(event, this.model.getRayGizmo());
    this.repaintCallback();
  }
}
