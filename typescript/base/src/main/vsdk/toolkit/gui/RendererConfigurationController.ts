import { RendererConfiguration } from "../environment/material/RendererConfiguration.js";
import { Controller } from "./Controller.js";
import { KeyEvent } from "./KeyEvent.js";

export class RendererConfigurationController extends Controller {
    private qualitySelection: RendererConfiguration | null;

    public constructor(qualitySelection: RendererConfiguration | null = null) {
        super();
        this.qualitySelection = qualitySelection;
    }

    public setRendererConfiguration(q: RendererConfiguration | null): void {
        this.qualitySelection = q;
    }

    public processKeyPressedEvent(keyEvent: KeyEvent): boolean {
        let updated = false;
        let st: number;
        const qualitySelection = this.qualitySelection;

        if (qualitySelection === null) {
            // Java dereferences the missing configuration only for the keys
            // it handles; the same keys fail here
            switch (keyEvent.keycode) {
                case KeyEvent.KEY_F1: case KeyEvent.KEY_F2: case KeyEvent.KEY_F3:
                case KeyEvent.KEY_F4: case KeyEvent.KEY_F5: case KeyEvent.KEY_F6:
                case KeyEvent.KEY_F7: case KeyEvent.KEY_F8: case KeyEvent.KEY_F9:
                    throw new TypeError("RendererConfigurationController has no RendererConfiguration");
                default:
                    return false;
            }
        }

        switch (keyEvent.keycode) {
            case KeyEvent.KEY_F1:
                qualitySelection.changePoints();
                updated = true;
                break;
            case KeyEvent.KEY_F2:
                qualitySelection.changeWires();
                updated = true;
                break;
            case KeyEvent.KEY_F3:
                qualitySelection.changeSurfaces();
                updated = true;
                break;
            case KeyEvent.KEY_F4:
                qualitySelection.changeBoundingVolume();
                updated = true;
                break;
            case KeyEvent.KEY_F5:
                qualitySelection.changeNormals();
                updated = true;
                break;
            case KeyEvent.KEY_F6:
                qualitySelection.changeTrianglesNormals();
                updated = true;
                break;
            case KeyEvent.KEY_F7:
                st = qualitySelection.getShadingType();
                if (st === RendererConfiguration.SHADING_TYPE_NOLIGHT) {
                    st = RendererConfiguration.SHADING_TYPE_FLAT;
                } else if (st === RendererConfiguration.SHADING_TYPE_FLAT) {
                    st = RendererConfiguration.SHADING_TYPE_GOURAUD;
                } else if (st === RendererConfiguration.SHADING_TYPE_GOURAUD) {
                    st = RendererConfiguration.SHADING_TYPE_PHONG;
                } else if (st === RendererConfiguration.SHADING_TYPE_PHONG) {
                    st = RendererConfiguration.SHADING_TYPE_COOK_TERRANCE;
                } else {
                    st = RendererConfiguration.SHADING_TYPE_NOLIGHT;
                }
                qualitySelection.setShadingType(st);
                updated = true;
                break;
            case KeyEvent.KEY_F8:
                qualitySelection.changeTexture();
                updated = true;
                break;
            case KeyEvent.KEY_F9:
                qualitySelection.changeBumpMap();
                updated = true;
                break;
        }
        return updated;
    }

    public processKeyReleasedEvent(_keyEvent: KeyEvent): boolean {
        return false;
    }
}
