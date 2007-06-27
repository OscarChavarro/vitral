import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class Layout
{
    public static void constrain(Container container, Component component,
                                 int i, int i_0_, int i_1_, int i_2_, int i_3_,
                                 int i_4_, double d, double d_5_, int i_6_,
                                 int i_7_, int i_8_, int i_9_) {
        constrain(container, component, i, i_0_, i_1_, i_2_, i_3_, i_4_, d,
                  d_5_, i_6_, i_7_, i_8_, i_9_, 0, 0);
    }
    
    public static void constrain
        (Container container, Component component, int i, int i_10_, int i_11_,
         int i_12_, int i_13_, int i_14_, double d, double d_15_, int i_16_,
         int i_17_, int i_18_, int i_19_, int i_20_, int i_21_) {
        GridBagConstraints gridbagconstraints = new GridBagConstraints();
        gridbagconstraints.gridx = i;
        gridbagconstraints.gridy = i_10_;
        gridbagconstraints.gridwidth = i_11_;
        gridbagconstraints.gridheight = i_12_;
        gridbagconstraints.fill = i_13_;
        gridbagconstraints.anchor = i_14_;
        gridbagconstraints.weightx = d;
        gridbagconstraints.weighty = d_15_;
        gridbagconstraints.ipadx = i_20_;
        gridbagconstraints.ipady = i_21_;
        if (i_16_ + i_18_ + i_17_ + i_19_ > 0)
            gridbagconstraints.insets = new Insets(i_16_, i_17_, i_18_, i_19_);
        ((GridBagLayout) container.getLayout())
            .setConstraints(component, gridbagconstraints);
        container.add(component);
    }
    
    public static void constrain_button
        (Container container, Component component, int i, int i_22_) {
        constrain(container, component, i, i_22_, 1, 1, 0, 10, 0.2, 0.0, 5, 2,
                  5, 2, 2, 2);
    }
    
    public static void constrain_field(Container container,
                                       Component component, int i, int i_23_) {
        constrain(container, component, i, i_23_, 1, 1, 2, 10, 1.0, 0.2, 1, 1,
                  1, 1);
    }
}
