package main.p2p.view.menu;

import javax.swing.*;
import java.awt.event.ActionListener;

public class MenuBar extends JMenuBar {
    private JMenuItem connectMenuItem;
    private JMenuItem disconnectMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem aboutMenuItem;

    public MenuBar(ActionListener exitAction, ActionListener aboutAction) {
        initializeComponents(exitAction, aboutAction);
    }

    private void initializeComponents(ActionListener exitAction, ActionListener aboutAction) {
        JMenu filesMenu = new JMenu("Files");
        connectMenuItem = new JMenuItem("Connect");
        disconnectMenuItem = new JMenuItem("Disconnect");
        exitMenuItem = new JMenuItem("Exit");
        filesMenu.add(connectMenuItem);
        filesMenu.add(disconnectMenuItem);
        filesMenu.addSeparator();
        filesMenu.add(exitMenuItem);

        JMenu helpMenu = new JMenu("Help");
        aboutMenuItem = new JMenuItem("About");
        helpMenu.add(aboutMenuItem);

        exitMenuItem.addActionListener(exitAction);
        aboutMenuItem.addActionListener(aboutAction);

        disconnectMenuItem.setEnabled(false);

        this.add(filesMenu);
        this.add(helpMenu);
    }

    public JMenuItem getConnectMenuItem() {
        return connectMenuItem;
    }

    public JMenuItem getDisconnectMenuItem() {
        return disconnectMenuItem;
    }
}
