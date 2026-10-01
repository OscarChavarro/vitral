import { PresentationElement } from "../PresentationElement.js";
import type { Widget } from "./Widget.js";

export abstract class WidgetElement extends PresentationElement {
    protected context: Widget | null = null;
}
