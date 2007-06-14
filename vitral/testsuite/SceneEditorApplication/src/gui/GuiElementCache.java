package gui;

public abstract class GuiElementCache
{
    protected GuiCache context;

    public abstract String toString();
    public abstract String toString(int level);

    /**
    Given a Windows32 SDK API / Aquynza style coded name, this method
    generates the simplified name, separating from it its mnemonic and
    accelerator if any.

    For example, if codedName has the value "&Open\tCtrl+O", the
    simplified name will be "Open", the mnemonic will be 'O' and the
    accelerator will be "Ctrl+O". This method return its simplified name.
    */
    protected String processSimplifiedName(String codedName)
    {
        if ( codedName == null ) return null;

        String simplifiedName = "";

        int i;
        char c;

        for ( i = 0; i < codedName.length(); i++ ) {
        c = codedName.charAt(i);
            if ( c == '&' ) continue;

            if ( c == '\t' ) {
                break;
        }
            simplifiedName = simplifiedName + c;
    }

        return simplifiedName;
    }

    /**
    Given a Windows32 SDK API / Aquynza style coded name, this method
    generates the simplified name, separating from it its mnemonic and
    accelerator if any.

    For example, if codedName has the value "&Open\tCtrl+O", the
    simplified name will be "Open", the mnemonic will be 'O' and the
    accelerator will be "Ctrl+O". This method return its mnemonic.
    */
    protected char processMnemonic(String codedName)
    {
        if ( codedName == null ) return '\0';

        String simplifiedName = "";

        int i;
        char c;

        for ( i = 0; i < codedName.length()-1; i++ ) {
        c = codedName.charAt(i);
            if ( c == '&' ) {
                i++;
            c = codedName.charAt(i);
                return c;
        }
            simplifiedName = simplifiedName + c;
    }

        return '\0';
    }

    /**
    Given a Windows32 SDK API / Aquynza style coded name, this method
    generates the simplified name, separating from it its mnemonic and
    accelerator if any.

    For example, if codedName has the value "&Open\tCtrl+O", the
    simplified name will be "Open", the mnemonic will be 'O' and the
    accelerator will be "Ctrl+O". This method return its accelerator,
    or null if it is no accelerator.
    */
    protected String processAccelerator(String codedName)
    {
        if ( codedName == null ) return null;

        String accelerator = "";

        int i;
        char c;

        for ( i = 0; i < codedName.length(); i++ ) {
        c = codedName.charAt(i);
            if ( c == '\t' ) break;
    }

        for ( i++ ;i < codedName.length(); i++ ) {
        c = codedName.charAt(i);
            accelerator = accelerator + c;
    }

        if ( accelerator.length() <= 0 ) return null;

        return accelerator;
    }
}
