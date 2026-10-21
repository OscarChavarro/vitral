#include <cctype>
#include <cstdio>

#include <java/lang/Math.h>
#include "java/lang/String.h"
#include <BRepAlgoAPI_Common.hxx>
#include <BRepAlgoAPI_Cut.hxx>
#include <BRepAlgoAPI_Fuse.hxx>
#include <BRepLib.hxx>
#include <BRep_Builder.hxx>
#include <BRep_Tool.hxx>
#include <GeomConvert_CurveToAnaCurve.hxx>
#include <Geom_Curve.hxx>
#include <Geom_Line.hxx>
#include <IFSelect_ReturnStatus.hxx>
#include <NCollection_IndexedMap.hxx>
#include <Precision.hxx>
#include <STEPControl_Reader.hxx>
#include <STEPControl_Writer.hxx>
#include <ShapeUpgrade_UnifySameDomain.hxx>
#include <Standard_Failure.hxx>
#include <TopAbs_ShapeEnum.hxx>
#include <TopExp.hxx>
#include <TopTools_ShapeMapHasher.hxx>
#include <TopoDS.hxx>
#include <TopoDS_Edge.hxx>
#include <TopoDS_Shape.hxx>
#include <exception>
enum class OpCode {
  Union,
  AMinusB,
  BMinusA,
  Intersection
};

java::String ToUpper(java::String value) {
  for (size_t i = 0; i < value.size(); i++) {
    value[i] = static_cast<char>(std::toupper((unsigned char)value[i]));
  }
  return value;
}

bool ParseOpCode(const java::String& opcodeRaw, OpCode& out) {
  const java::String opcode = ToUpper(opcodeRaw);
  if (opcode == "UNION") {
    out = OpCode::Union;
    return true;
  }
  if (opcode == "A_MINUS_B") {
    out = OpCode::AMinusB;
    return true;
  }
  if (opcode == "B_MINUS_A") {
    out = OpCode::BMinusA;
    return true;
  }
  if (opcode == "INTERSECTION") {
    out = OpCode::Intersection;
    return true;
  }
  return false;
}

TopoDS_Shape ReadStepShape(const java::String& path) {
  STEPControl_Reader reader;
  const IFSelect_ReturnStatus readStatus = reader.ReadFile(path.c_str());
  if (readStatus != IFSelect_RetDone) {
    throw Standard_Failure(("Failed to read STEP file: " + path).c_str());
  }

  const int transferCount = reader.TransferRoots();
  if (transferCount <= 0) {
    throw Standard_Failure(("Failed to transfer STEP entities: " + path).c_str());
  }

  TopoDS_Shape shape = reader.OneShape();
  if (shape.IsNull()) {
    throw Standard_Failure(("Imported geometry is null: " + path).c_str());
  }
  return shape;
}

TopoDS_Shape ExecuteBoolean(const TopoDS_Shape& shapeA, const TopoDS_Shape& shapeB, OpCode opcode) {
  switch (opcode) {
    case OpCode::Union: {
      BRepAlgoAPI_Fuse op(shapeA, shapeB);
      op.Build();
      if (!op.IsDone()) {
        throw Standard_Failure("UNION operation failed");
      }
      op.SimplifyResult(true, true, Precision::Angular());
      return op.Shape();
    }
    case OpCode::AMinusB: {
      BRepAlgoAPI_Cut op(shapeA, shapeB);
      op.Build();
      if (!op.IsDone()) {
        throw Standard_Failure("A_MINUS_B operation failed");
      }
      op.SimplifyResult(true, true, Precision::Angular());
      return op.Shape();
    }
    case OpCode::BMinusA: {
      BRepAlgoAPI_Cut op(shapeB, shapeA);
      op.Build();
      if (!op.IsDone()) {
        throw Standard_Failure("B_MINUS_A operation failed");
      }
      op.SimplifyResult(true, true, Precision::Angular());
      return op.Shape();
    }
    case OpCode::Intersection: {
      BRepAlgoAPI_Common op(shapeA, shapeB);
      op.Build();
      if (!op.IsDone()) {
        throw Standard_Failure("INTERSECTION operation failed");
      }
      op.SimplifyResult(true, true, Precision::Angular());
      return op.Shape();
    }
  }

  throw Standard_Failure("Unsupported opcode");
}

TopoDS_Shape PlanarizeForStep(const TopoDS_Shape& input) {
  if (input.IsNull()) {
    throw Standard_Failure("Cannot planarize null shape");
  }

  // Force same-domain unification as a mandatory STEP post-process.
  ShapeUpgrade_UnifySameDomain unifier(input, true, true, false);
  unifier.SetSafeInputMode(true);
  unifier.SetLinearTolerance(Precision::Confusion());
  unifier.SetAngularTolerance(Precision::Angular());
  unifier.Build();

  const TopoDS_Shape& planarized = unifier.Shape();
  if (planarized.IsNull()) {
    throw Standard_Failure("Planarization/unification failed");
  }

  BRep_Builder builder;
  NCollection_IndexedMap<TopoDS_Shape, TopTools_ShapeMapHasher> edges;
  TopExp::MapShapes(planarized, TopAbs_EDGE, edges);

  int convertedEdges = 0;
  int nonLinearEdges = 0;

  for (int i = 1; i <= edges.Extent(); ++i) {
    const TopoDS_Edge edge = TopoDS::Edge(edges(i));
    if (edge.IsNull()) {
      continue;
    }

    double first = 0.0;
    double last = 0.0;
    Handle(Geom_Curve) curve = BRep_Tool::Curve(edge, first, last);
    if (curve.IsNull()) {
      continue;
    }

    if (!curve->IsKind(STANDARD_TYPE(Geom_Line))) {
      const double tol = java::Math::max(BRep_Tool::Tolerance(edge), Precision::Confusion());
      double cf = first;
      double cl = last;
      double deviation = 0.0;
      Handle(Geom_Line) line = GeomConvert_CurveToAnaCurve::ComputeLine(curve, tol, first, last, cf, cl, deviation);
      if (line.IsNull()) {
        ++nonLinearEdges;
        continue;
      }

      builder.UpdateEdge(edge, line, tol);
      builder.Range(edge, cf, cl, true);
      builder.SameRange(edge, true);
      builder.SameParameter(edge, false);
      ++convertedEdges;
    }
  }

  if (nonLinearEdges > 0) {
    throw Standard_Failure("Found non-linear edges that cannot be converted to LINE");
  }

  // Rebuild p-curves to keep consistency after replacing 3D edge curves.
  BRepLib::SameParameter(planarized, Precision::Confusion(), true);
  return planarized;
}

void WriteStepShape(const TopoDS_Shape& shape, const java::String& path) {
  if (shape.IsNull()) {
    throw Standard_Failure("Boolean operation result is null");
  }

  STEPControl_Writer writer;
  const IFSelect_ReturnStatus transferStatus = writer.Transfer(shape, STEPControl_AsIs);
  if (transferStatus != IFSelect_RetDone) {
    throw Standard_Failure(("Failed to transfer shape to STEP: " + path).c_str());
  }

  const IFSelect_ReturnStatus writeStatus = writer.Write(path.c_str());
  if (writeStatus != IFSelect_RetDone) {
    throw Standard_Failure(("Failed to write STEP file: " + path).c_str());
  }
}

void PrintUsage(const char* argv0) {
  std::fprintf(stderr,
               "Usage: %s inputA.step inputB.step <opcode> output.step\n"
               "Opcodes: UNION | A_MINUS_B | B_MINUS_A | INTERSECTION\n",
               argv0);
}

int main(int argc, char** argv) {
  if (argc != 5) {
    PrintUsage(argv[0]);
    return 1;
  }

  const java::String inputA = argv[1];
  const java::String inputB = argv[2];
  const java::String opcodeArg = argv[3];
  const java::String output = argv[4];

  OpCode opcode;
  if (!ParseOpCode(opcodeArg, opcode)) {
    std::fprintf(stderr, "Invalid opcode: %s\n", opcodeArg.c_str());
    PrintUsage(argv[0]);
    return 2;
  }

  try {
    const TopoDS_Shape shapeA = ReadStepShape(inputA);
    const TopoDS_Shape shapeB = ReadStepShape(inputB);
    const TopoDS_Shape result = ExecuteBoolean(shapeA, shapeB, opcode);
    const TopoDS_Shape planarized = PlanarizeForStep(result);
    WriteStepShape(planarized, output);

    std::printf("Result written to: %s\n", output.c_str());
    return 0;
  } catch (const Standard_Failure&) {
    std::fprintf(stderr, "Operation failed with Standard_Failure\n");
    return 3;
  } catch (const std::exception& e) {
    std::fprintf(stderr, "Error: %s\n", e.what());
    return 4;
  }
}
