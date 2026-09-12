import {
  TangibleInterfaceEvent2InfinitePlaneGizmoMapper,
  TangibleInterfaceEvent2RayGizmoMapper,
  type TangibleInterfaceEvent,
  type TangibleInterfaceListener,
} from '@vitral/base';
import type { SolidTextureModel } from '../model/solid-texture-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/gui/TangibleInterfaceInteractionTechniques.java`.
 *
 * Two physical markers reach this program where `MeshExample` had one: the
 * `rayCube1` cube drives the ray gizmo, and the `cuttingPlane1` cube drives the
 * infinite-plane gizmo that clips the solid-textured geometry.
 */
export class TangibleInterfaceInteractionTechniques implements TangibleInterfaceListener {
  private static readonly RAY_CUBE_TANGIBLE_ELEMENT_ID = 'rayCube1';
  private static readonly CUTTING_PLANE_CUBE_TANGIBLE_ELEMENT_ID = 'cuttingPlane1';

  private readonly toRayGizmoMapper: TangibleInterfaceEvent2RayGizmoMapper;
  private readonly toInfinitePlaneGizmoMapper: TangibleInterfaceEvent2InfinitePlaneGizmoMapper;

  constructor(
    private readonly model: SolidTextureModel,
    private readonly repaintCallback: () => void,
  ) {
    this.toRayGizmoMapper = new TangibleInterfaceEvent2RayGizmoMapper(model.getCamera());
    this.toInfinitePlaneGizmoMapper = new TangibleInterfaceEvent2InfinitePlaneGizmoMapper(
      model.getCamera(),
    );
  }

  tangibleInterfaceEventReceived(event: TangibleInterfaceEvent): void {
    if (event.getId() === TangibleInterfaceInteractionTechniques.RAY_CUBE_TANGIBLE_ELEMENT_ID) {
      this.toRayGizmoMapper.map(event, this.model.getRayGizmo());
    } else if (
      event.getId() ===
      TangibleInterfaceInteractionTechniques.CUTTING_PLANE_CUBE_TANGIBLE_ELEMENT_ID
    ) {
      this.toInfinitePlaneGizmoMapper.map(event, this.model.getInfinitePlaneGizmo());
    }

    this.repaintCallback();
  }
}
