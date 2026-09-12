import { PresentationElement } from "../PresentationElement.js";

/**
The Gizmo abstract class provides an interface for *Gizmo
style classes. This serves two purposes:
  - To help in design level organization of renderers (this eases the
    study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all gizmos (but none of these as been detected yet)
*/
export abstract class Gizmo extends PresentationElement {}
