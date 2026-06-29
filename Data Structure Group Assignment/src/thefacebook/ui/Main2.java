package thefacebook.ui;

import thefacebook.service.SocialNetwork;

public class Main2 {
    public static void main(String[] args) {
        SocialNetwork network = new SocialNetwork("data");
        network.load();
        new ConsoleUI(network).start();
    }
}