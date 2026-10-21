#ifndef __LIGHT_GIZMO_STYLE__
#define __LIGHT_GIZMO_STYLE__

/**
C++98 replacement for a scoped enumeration: values are accessed as
`LightGizmoStyle::VALUE` and objects convert implicitly to the plain `Value` enum,
so they can be compared and used in `switch` statements.
*/
class LightGizmoStyle {
  public:
    enum Value {
        CROSS,
        OMNI_BILLBOARD
    };

    LightGizmoStyle() : value(CROSS) {}
    LightGizmoStyle(Value inValue) : value(inValue) {}
    explicit LightGizmoStyle(int inValue) : value((Value)inValue) {}
    operator Value() const { return value; }

  private:
    Value value;
};

#endif
