package main.p2p;

import javax.swing.SwingUtilities;

import main.p2p.controller.P2PController;

public class Main {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			P2PController controller = new P2PController();
			controller.showGUI();
		});
	}

}
