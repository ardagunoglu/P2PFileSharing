package main.p2p.view.components;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractSettingsSection extends JPanel {
	protected final Dimension const_dimension_button = new Dimension(60, 25);
	
    public AbstractSettingsSection(String title) {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    protected abstract void setupComponents();
}
