package thefacebook;

import thefacebook.service.SocialNetwork;
import thefacebook.ui.ConsoleUI;
import thefacebook.ui.TheFacebookGUI;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SocialNetwork network = new SocialNetwork("data");
        network.load();
        if (args.length > 0 && "console".equalsIgnoreCase(args[0])) {
            new ConsoleUI(network).start();
            return;
        }
        SwingUtilities.invokeLater(() -> new TheFacebookGUI(network).show());
    }
}
