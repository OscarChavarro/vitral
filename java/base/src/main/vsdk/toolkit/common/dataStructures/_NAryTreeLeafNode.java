package vsdk.toolkit.common.dataStructures;
import java.io.Serial;

/**
NAry tree nodes organized from composite structural design pattern.
@param <T>
*/
public class _NAryTreeLeafNode<T> extends _NAryTreeNode<T> {
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @Serial private static final long serialVersionUID = 20150218L;
    
    public _NAryTreeLeafNode(final T inInfo)
    {
        super(inInfo);
    }

}
