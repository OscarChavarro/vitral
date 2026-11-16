package vsdk.toolkit.common.dataStructures;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
Direct coverage for the circular list that backs every face, loop, edge and
vertex collection of PolyhedralBoundedSolid.

<p>Traceability: this container replaces the intrusive nxt/prv pointers of
[MANT1988] Ch. 10.2-10.3 and the list procedures of Ch. 11.2 (addlist,
dellist). The cases marked "hidden defect" were found by the Fable 5.2
kernel analysis (doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md,
defects L-01 to L-03); they stay disabled until the production fix lands.</p>
 */
class CircularDoubleLinkedListTest
{
    @Test
    void given_threeElements_when_traversed_then_orderAndNeighborsAreCircular()
    {
        CircularDoubleLinkedList<String> list = abc();

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
        assertThat(list.get(2)).isEqualTo("c");
        assertThat(list.nextOf("c")).isEqualTo("a");
        assertThat(list.previousOf("a")).isEqualTo("c");
    }

    @Test
    void given_threeElements_when_insertBeforeHead_then_newElementBecomesHead()
    {
        CircularDoubleLinkedList<String> list = abc();

        list.insertBefore("x", "a");

        assertThat(list.size()).isEqualTo(4);
        assertThat(list.get(0)).isEqualTo("x");
        assertThat(list.nextOf("x")).isEqualTo("a");
        assertThat(list.previousOf("x")).isEqualTo("c");
    }

    @Test
    void given_threeElements_when_reverse_then_orderIsMirrored()
    {
        CircularDoubleLinkedList<String> list = abc();

        list.reverse();

        assertThat(list.get(0)).isEqualTo("c");
        assertThat(list.get(1)).isEqualTo("b");
        assertThat(list.get(2)).isEqualTo("a");
    }

    /**
    Defect L-01 (fixed): removeElemAtWindow() leaves `head` pointing at the
    removed node when the list becomes empty, so the next add() links the new
    element behind a stale node and get(0) resurrects the removed element.
    This is exactly the sequence lkev() performs when it kills the last edge
    of a loop (unlist both half-edges, then re-add the surviving one).
    */
    @Test
    void given_listEmptiedByRemove_when_add_then_onlyTheNewElementIsVisible()
    {
        CircularDoubleLinkedList<String> list =
            new CircularDoubleLinkedList<String>();
        list.add("a");
        list.remove(0);

        list.add("b");

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo("b");
        assertThat(list.nextOf("b")).isEqualTo("b");
        assertThat(list.previousOf("b")).isEqualTo("b");
    }

    /**
    Defect L-01 (fixed, continued): after the stale-head corruption the ring
    holds a phantom node, so neighbor queries disagree with indexed access.
    */
    @Test
    void given_listEmptiedByRemove_when_addAndInsertBefore_then_neighborsMatchIndexOrder()
    {
        CircularDoubleLinkedList<String> list =
            new CircularDoubleLinkedList<String>();
        list.add("a");
        list.remove(0);
        list.add("b");

        list.insertBefore("c", "b");

        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0)).isEqualTo("c");
        assertThat(list.get(1)).isEqualTo("b");
        assertThat(list.nextOf("c")).isEqualTo("b");
        assertThat(list.nextOf("b")).isEqualTo("c");
    }

    /**
    Defect L-02 (fixed): remove(index) with an out-of-range index used to
    delete whatever element the window was left on by an unrelated earlier
    call. It is now a hard error that leaves the list unchanged.
    */
    @Test
    void given_windowLeftOnAnElement_when_removeWithInvalidIndex_then_errorAndNothingIsRemoved()
    {
        CircularDoubleLinkedList<String> list = abc();
        list.locateWindowAtElem("b");

        assertThatThrownBy(() -> list.remove(7))
            .isInstanceOf(IndexOutOfBoundsException.class);

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(1)).isEqualTo("b");
    }

    /**
    Defect L-03 (fixed): insertBefore(newElem, pivot) with a pivot that is
    not in the list used to insert at the head silently. It is now a hard
    error that leaves the list unchanged. Euler operators rely on this call (addhe) to place new
    half-edges, so a wrong pivot corrupts a loop without any diagnostic.
    */
    @Test
    void given_missingPivot_when_insertBefore_then_errorAndListIsNotModified()
    {
        CircularDoubleLinkedList<String> list = abc();

        assertThatThrownBy(() -> list.insertBefore("x", "not-in-list"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
    }

    @Test
    void given_linkableElements_when_reorderedAndRemoved_then_neighborsStayConsistent()
    {
        CircularDoubleLinkedList<Item> list = new CircularDoubleLinkedList<>();
        Item a = new Item();
        Item b = new Item();
        Item c = new Item();
        list.add(a);
        list.add(b);
        list.add(c);

        list.reverse();
        assertThat(list.nextOf(c)).isSameAs(b);
        assertThat(list.previousOf(c)).isSameAs(a);

        list.swapElements(a, c);
        assertThat(list.get(0)).isSameAs(a);
        assertThat(list.nextOf(a)).isSameAs(b);

        list.locateWindowAtElem(b);
        list.removeElemAtWindow();
        assertThat(list.contains(b)).isFalse();
        assertThat(b.getListNode()).isNull();
        assertThat(list.nextOf(a)).isSameAs(c);
        assertThat(list.nextOf(c)).isSameAs(a);

        CircularDoubleLinkedList<Item> other = new CircularDoubleLinkedList<>();
        other.add(a);
        assertThat(list.nextOf(a)).as("falls back to a scan").isSameAs(c);
        assertThat(other.nextOf(a)).isSameAs(a);
    }

    private static final class Item implements Linkable<Item>
    {
        private _CircularDoubleLinkedListNode<Item> listNode;

        @Override
        public _CircularDoubleLinkedListNode<Item> getListNode()
        {
            return listNode;
        }

        @Override
        public void setListNode(_CircularDoubleLinkedListNode<Item> n)
        {
            listNode = n;
        }
    }

    private static CircularDoubleLinkedList<String> abc()
    {
        CircularDoubleLinkedList<String> list =
            new CircularDoubleLinkedList<String>();
        list.add("a");
        list.add("b");
        list.add("c");
        return list;
    }
}
