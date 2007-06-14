package vsdk.transition.gui;

import java.util.ArrayList;
import java.util.Iterator;

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

        Iterator i;

        for ( i = children.iterator(); i.hasNext(); ) {
            msg = msg + ((GuiElementCache)i.next()).toString(level+1);
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
