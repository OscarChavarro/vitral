// VSDK classes
import vsdk.toolkit.common.dataStructures.NAryTree;
import vsdk.toolkit.processing.NAryTreeVisitor;

/**
*/
public class NAryTreeExample {

    /**
    @param args
    */
    public static void main(String args[])
    {
        // Visitor facade... for next operations
        NAryTreeVisitor<Integer> facade;
        facade = new NAryTreeVisitor<Integer>();

        // Tree building tests
        Integer n1 = 1;
        Integer n11 = 2;
        Integer n12 = 3;
        Integer n111 = 4;

        NAryTree<Integer> tree;
        tree = new NAryTree<Integer>(n1);
        tree.addChild(n1, n11);
        tree.addChild(n1, n12);
        tree.addChild(n11, n111);

        // Traverse tests
        MyTreeTraverser traverser;
        traverser = new MyTreeTraverser();

        facade.preOrderTraverse(tree, traverser);

        // Search tests
        Integer x = 666;
        System.out.println("Searching for someone who is here: " +
            tree.searchNodeByContent(n111));
        System.out.println("Searching for someone who is not here: " +
            tree.searchNodeByContent(x));
    }
}
