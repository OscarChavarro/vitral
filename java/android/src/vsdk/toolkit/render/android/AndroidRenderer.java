package vsdk.toolkit.render.android;

import vsdk.toolkit.render.RenderingElement;

/**
The AndroidRenderer abstract class provides an interface for Android*Renderer
style classes. This serves two purposes:
  - To help in design level organization of Android renderers (this eases the
    study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all Android renderers classes and Android renderers' private 
    utility/supporting classes (but none of these as been detected yet)
*/

public abstract class AndroidRenderer extends RenderingElement {
    ;
}
