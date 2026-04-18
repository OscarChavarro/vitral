package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidKimrhMikrhTest
{
    @Test
    void given_boxSolid_when_lkimrh_then_faceIsRemovedAndItsLoopBecomesInnerLoop()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidFace face1 = solid.polygonsList.get(0);
        _PolyhedralBoundedSolidFace face2 = solid.polygonsList.get(1);
        _PolyhedralBoundedSolidLoop migratedLoop = face2.boundariesList.get(0);
        _PolyhedralBoundedSolidHalfEdge migratedHalfEdge =
            migratedLoop.boundaryStartHalfEdge;
        int faceCountBefore = solid.polygonsList.size();
        int face1LoopsBefore = face1.boundariesList.size();
        int face2LoopsBefore = face2.boundariesList.size();
        int removedFaceId = face2.id;

        // Action
        solid.lkimrh(face1, face2);

        // Assert
        assertThat(solid.polygonsList.size()).isEqualTo(faceCountBefore - 1);
        assertThat(solid.findFace(removedFaceId)).isNull();
        assertThat(face1.boundariesList.size())
            .isEqualTo(face1LoopsBefore + face2LoopsBefore);
        assertThat(migratedHalfEdge.parentLoop.parentFace).isSameAs(face1);
        assertThat(migratedHalfEdge.parentLoop).isNotSameAs(migratedLoop);
        assertThat(faceContainsLoop(face1, migratedHalfEdge.parentLoop)).isTrue();
    }

    @Test
    void given_faceWithInnerLoop_when_lmikrh_then_innerLoopBecomesNewFaceOuterLoop()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidFace face1 = solid.polygonsList.get(0);
        _PolyhedralBoundedSolidFace face2 = solid.polygonsList.get(1);
        _PolyhedralBoundedSolidLoop loopToPromote = face2.boundariesList.get(0);

        solid.lkimrh(face1, face2);
        int facesAfterLkimrh = solid.polygonsList.size();
        int newFaceId = solid.getMaxFaceId() + 1;

        // Action
        _PolyhedralBoundedSolidFace newFace = solid.lmikrh(loopToPromote,
            newFaceId);

        // Assert
        assertThat(newFace).isNotNull();
        assertThat(newFace.id).isEqualTo(newFaceId);
        assertThat(solid.polygonsList.size()).isEqualTo(facesAfterLkimrh + 1);
        assertThat(loopToPromote.parentFace).isSameAs(newFace);
        assertThat(newFace.boundariesList.size()).isEqualTo(1);
        assertThat(newFace.boundariesList.get(0)).isSameAs(loopToPromote);
        assertThat(faceContainsLoop(face1, loopToPromote)).isFalse();
    }

    private static boolean faceContainsLoop(_PolyhedralBoundedSolidFace face,
        _PolyhedralBoundedSolidLoop loop)
    {
        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            if ( face.boundariesList.get(i) == loop ) {
                return true;
            }
        }
        return false;
    }
}
