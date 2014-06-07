//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - July 12 2007 - Oscar Chavarro: Original base version                  =
//===========================================================================

package vsdk.framework;

/**
A `Component` in VitralSDK is a software container for VSDK FRAMEWORK as a
"component" in the sence of object oriented programming (programming structured
over software components). Subclasses of class `Component` usually correspond
to interfaces and concrete classes, and to design patterns structures showed
in a "UML component diagram" inside a "Component". Each component exposes
its "services" via "interfaces" of such classes, and its correspondences
denotes the "required" services from other components.

Note that all Vitral architecture components depends upon the Vitral SDK
toolkit software platfotm contained in the vsdk.toolkit package.

The Component abstract class provides an interface for classes conforming
to Vitral software architecture for computer graphics development.
It serves two purposes:
  - To help in design level organization of Vitral architecture componets
    (this eases the study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all components (but none of these as been detected yet)
Note that this is a very high-level organizing class inside the Vitral SDK,
and it is supposed to be here to support high level design aspects
(software architecture).
*/
public abstract class Component
{
    ;
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
