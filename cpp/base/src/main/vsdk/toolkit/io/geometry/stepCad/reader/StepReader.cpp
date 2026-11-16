#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/geometry/stepCad/reader/StepReader.h"

#include "java/io/FileInputStream.h"
#include "java/lang/Exception.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/System.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepEntity.h"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepSolidBuilder.h"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepTokenizer.h"

PolyhedralBoundedSolid* StepReader::readSolid(const java::File* stepFile)
{
    if ( stepFile == nullptr ) {
        throw java::IllegalArgumentException("stepFile is null");
    }
    // Java's FileInputStream constructor throws FileNotFoundException
    if ( !stepFile->exists() || stepFile->isDirectory() ||
         !stepFile->canRead() ) {
        throw java::Exception(stepFile->getPath() +
            (stepFile->exists() ? " (Permission denied)"
                                : " (No such file or directory)"));
    }
    java::FileInputStream in(stepFile->getPath().c_str());
    PolyhedralBoundedSolid* solid = readSolid(&in);
    in.close();
    return solid;
}

PolyhedralBoundedSolid* StepReader::readSolid(java::InputStream* in)
{
    if ( in == nullptr ) {
        throw java::IllegalArgumentException("input stream is null");
    }

    _StepEntityMap entities;
    _StepTokenizer::parse(in, entities);

    PolyhedralBoundedSolid* solid = _StepSolidBuilder::build(entities);

    if ( !PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid) ) {
        java::System::err.println(
            "StepReader: reconstructed solid failed validateIntermediate. "
            "The source STEP file may not satisfy manifold assumptions.");
    }

    return solid;
}
