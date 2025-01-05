package main.p2p.view.components;

import java.awt.BorderLayout;

import javax.swing.*;

public class SearchPanel extends JPanel {
    private JButton searchButton;
    private JTextField searchField;

    public SearchPanel(JButton searchButton, JTextField searchField) {
        this.searchButton = searchButton;
        this.searchField = searchField;
        setupComponents();
    }

    private void setupComponents() {
        this.setLayout(new BorderLayout(5, 5));
        this.add(searchButton, BorderLayout.WEST);
        this.add(searchField, BorderLayout.CENTER);
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public JTextField getSearchField() {
        return searchField;
    }
}
