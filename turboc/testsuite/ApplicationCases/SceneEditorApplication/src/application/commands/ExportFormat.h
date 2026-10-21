#ifndef __EXPORT_FORMAT__
#define __EXPORT_FORMAT__

/**
Formats to export the objects of the scene.
*/
/// C++98 replacement of a scoped enumeration: keeps the
/// `ExportFormat::VALUE` syntax
class ExportFormat {
  public:
    enum Value {
        OBJ,
        GTS,
        VTK
    };
    ExportFormat() : value(OBJ) {}
    ExportFormat(Value v) : value(v) {}
    operator Value() const { return value; }
  private:
    Value value;
};

#endif
