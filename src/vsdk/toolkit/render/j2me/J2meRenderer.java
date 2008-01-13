//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 31 2007 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.j2me;

import vsdk.toolkit.render.RenderingElement;

/**
The J2meRenderer abstract class provides an interface for J2me*Renderer
style classes. This serves two purposes:
  - To help in design level organization of Awt renderers (this eases the
    study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all J2me renderers classes and J2me renderers' private utility/supporting
    classes (but none of these as been detected yet)
*/

public abstract class J2meRenderer extends RenderingElement {
    ;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
