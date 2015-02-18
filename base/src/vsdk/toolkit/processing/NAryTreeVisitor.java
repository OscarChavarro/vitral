//===========================================================================
package vsdk.toolkit.processing;

// Java basic classes
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.dataStructures.NAryTree;
import vsdk.toolkit.common.dataStructures.NAryTreeTraverser;
import vsdk.toolkit.common.dataStructures._NAryTreeNode;
import vsdk.toolkit.common.dataStructures._NAryTreeLeafNode;
import vsdk.toolkit.common.dataStructures._NAryTreeIntermediateNode;

/**
This class contains several common operations for NAryTree. This class plays
the role of algorithm holder class in a visitor design pattern.
@param <T>
*/
public class NAryTreeVisitor<T> extends ProcessingElement {
    
    /**
    @param inNode
    @param inTraverser
    @param level 
    */
    @SuppressWarnings("unchecked")
    private void preOrderTraverse(
        _NAryTreeNode<T> inNode, 
        final NAryTreeTraverser inTraverser, 
        int level)
    {
        if ( inNode == null ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "preOrderTraverse", 
                "Null node not expected.");
            return;
        }
        if ( inNode instanceof _NAryTreeIntermediateNode ) {
            _NAryTreeIntermediateNode node = (_NAryTreeIntermediateNode)inNode;
            ArrayList<_NAryTreeNode<T>> l = node.getChildren();
            int i;
            inTraverser.visit(node.getData(), level);
            for ( i = 0; i < l.size(); i++ ) {
                preOrderTraverse(l.get(i), inTraverser, level+1);
            }
        }
        else if ( inNode instanceof _NAryTreeLeafNode ) {
            _NAryTreeLeafNode leaf = (_NAryTreeLeafNode)inNode;
            inTraverser.visit(leaf.getData(), level);
        }
        else {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "preOrderTraverse", 
                "unexpected node subclass " + inNode.getClass().getName());
        }
    }

    /**
    @param inTree
    @param inTraverser 
    */
    public void preOrderTraverse(
        NAryTree<T> inTree, final NAryTreeTraverser inTraverser)
    {
        inTraverser.start();
        if (  inTree.getRoot() == null ) {
            System.out.println("Empty tree, nothing to traverse");
            return;
        }
        preOrderTraverse(inTree.getRoot(), inTraverser, 1);
        inTraverser.end();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
