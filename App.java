import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SpaceInvaders game = new SpaceInvaders("Player");
            game.promptPlayerName(); // Prompt the player for their name before the game starts

            // Create the game window (frame)
            JFrame frame = new JFrame("Space Invaders");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(game);
            frame.pack();
            frame.setVisible(true);
        });
    }
}
