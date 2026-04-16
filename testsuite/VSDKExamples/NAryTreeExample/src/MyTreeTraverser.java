import vsdk.toolkit.common.dataStructures.NAryTreeTraverser;

/**
*/
public class MyTreeTraverser extends NAryTreeTraverser {

    /**
    @param inElement
    @param inLevel
    */
    @Override
    public void visit(Object inElement, final int inLevel) {
        if ( !(inElement instanceof Integer) ) {
            return;
        }
        Integer data = (Integer)inElement;

        System.out.println(formatHeader(inLevel) + data);
    }

    /**
    */
    @Override
    public void start() {
        System.out.println("------------------------------------");
    }

    /**
    */
    @Override
    public void end() {
        System.out.println("---");
    }

}
