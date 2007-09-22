package vsdk.transition.gui;

import java.util.ArrayList;

public class GuiMenuCache extends GuiElementCache
{
    private ArrayList <GuiElementCache> children;
    private String name;
    private char mnemonic;
    private String accelerator;

    public GuiMenuCache(GuiCache c)
    {
        context = c;
        children = new ArrayList<GuiElementCache>();
        name = null;
    }

    public ArrayList <GuiElementCache> getChildren()
    {
        return children;
    }

    public void setName(String n)
    {
        name = processSimplifiedName(n);
        mnemonic = processMnemonic(n);
        accelerator = processAccelerator(n);
    }

    public void addChild(GuiElementCache i)
    {
        children.add(i);
    }

    public String toString(int level)
    {
        String leadingSpace = "";
        int j;

        for ( j = 0; j < level; j++ ) {
            leadingSpace = leadingSpace + "  ";
        }

        String msg = leadingSpace + "Menu \"" + name + "\"\n";

        int i;

        for ( i = 0; i < children.size(); i++ ) {
            msg = msg + (children.get(i)).toString(level+1);
        }

        return msg;
    }

    public String toString()
    {
        return toString(0);
    }

    public String getName()
    {
        return name;
    }

    public char getMnemonic()
    {
        return mnemonic;
    }
}
