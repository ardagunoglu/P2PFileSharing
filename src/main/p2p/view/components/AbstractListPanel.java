package main.p2p.view.components;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractListPanel extends JPanel {
    private final JList<String> list;

    public AbstractListPanel(String title) {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createTitledBorder(title));

        list = new JList<>(new DefaultListModel<>());
        JScrollPane scrollPane = new JScrollPane(list);

        this.add(scrollPane, BorderLayout.CENTER);
    }

    public JList<String> getList() {
        return list;
    }

    public void addElement(String element) {
        ((DefaultListModel<String>) list.getModel()).addElement(element);
    }

    public void removeSelectedElement() {
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex != -1) {
            ((DefaultListModel<String>) list.getModel()).remove(selectedIndex);
        }
    }

    public boolean hasSelection() {
        return !list.isSelectionEmpty();
    }

    public abstract void onElementAdded(String element);

    public abstract void onElementRemoved(String element);
}
