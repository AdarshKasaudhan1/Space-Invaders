import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import java.util.List;

public class SpaceInvaders extends JPanel implements ActionListener, KeyListener {
    private final Set<Integer> keysPressed = new HashSet<>();
    private long lastFiredTime = 0;
    private final int fireCooldown = 200;

    private final int tileSize = 32;
    private final int rows = 24, columns = 32;
    private final int boardWidth = tileSize * columns;
    private final int boardHeight = tileSize * rows;

    private BufferedImage shipImg;
    private BufferedImage shipBulletImg;
    private final List<BufferedImage> alienImgs = new ArrayList<>();
    private final List<BufferedImage> alienBulletImgs = new ArrayList<>();

    private Block ship;
    private final List<Block> alienArray = new ArrayList<>();
    private final List<Block> bulletArray = new ArrayList<>();
    private final List<Block> alienBullets = new ArrayList<>();

    private final int shipW = tileSize * 2, shipH = tileSize;
    private final int shipY = tileSize * rows - tileSize * 2;
    private int shipVelocityX = 10;

    private final int bulletW = tileSize / 2, bulletH = tileSize / 2;
    private final int bulletVelY = -15;
    private int alienBulletVelY = 5;

    private final Timer gameLoop;
    private boolean gameOver = false;
    private int score = 0;
    private boolean paused = false;

    private long lastAlienSpawnTime = 0;
    private final int alienSpawnInterval = 1000;
    private int alienFallSpeed = 3;
    private String playerName = "Player";

    private static final String SCORE_FILE = "score.dat";
    private int highestScore = 0;
    private String highestScorer = "None";

    private static class Block {
        int x, y, w, h;
        Image img;
        boolean alive = true, used = false;

        Block(int x, int y, int w, int h, Image img) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.img = img;
        }
    }

    public SpaceInvaders(String playerName) {
        this.playerName = playerName;

        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        try {
            shipImg = ImageIO.read(getClass().getResourceAsStream("/Resources/Ship_5.png"));
            shipBulletImg = ImageIO.read(getClass().getResourceAsStream("/Resources/alien-yellow.png"));

            alienImgs.add(ImageIO.read(getClass().getResourceAsStream("/Resources/alien.png")));
            alienImgs.add(ImageIO.read(getClass().getResourceAsStream("/Resources/alien-cyan.png")));
            alienImgs.add(ImageIO.read(getClass().getResourceAsStream("/Resources/alien-magenta.png")));
            alienImgs.add(ImageIO.read(getClass().getResourceAsStream("/Resources/alien-yellow.png")));

            alienBulletImgs.add(ImageIO.read(getClass().getResourceAsStream("/Resources/alien-magenta.png")));
        } catch (IOException e) {
            System.err.println("Error loading images. Please ensure resources are in place.");
        }

        ship = new Block(boardWidth / 2 - shipW / 2, shipY, shipW, shipH, shipImg);
        highestScore = readHighestScore();
        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();
    }

    public void promptPlayerName() {
        String input = JOptionPane.showInputDialog(this, "Enter your name:", "Player Name", JOptionPane.QUESTION_MESSAGE);
        if (input != null && !input.trim().isEmpty()) {
            playerName = input;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, boardWidth, boardHeight);
        drawSprites(g);

        if (paused) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.setColor(Color.WHITE);
            g.drawString("PAUSED", boardWidth / 2 - 100, boardHeight / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Press 'C' to Continue or 'Q' to Quit", boardWidth / 2 - 160, boardHeight / 2 + 40);
        }

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.setColor(Color.RED);
            g.drawString("GAME OVER!", boardWidth / 2 - 120, boardHeight / 2 - 50);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.setColor(Color.WHITE);
            g.drawString("Press ENTER to restart", boardWidth / 2 - 130, boardHeight / 2 + 10);
        }

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("Player: " + playerName, 20, 40);
        g.drawString("Score: " + score, 20, 70);
        g.drawString("High Score: " + highestScorer + " - " + highestScore, boardWidth - 300, 40);
    }

    private void drawSprites(Graphics g) {
        if (ship.img != null) g.drawImage(ship.img, ship.x, ship.y, ship.w, ship.h, null);
        for (Block a : alienArray) if (a.alive && a.img != null) g.drawImage(a.img, a.x, a.y, a.w, a.h, null);
        for (Block b : bulletArray) if (!b.used && b.img != null) g.drawImage(b.img, b.x, b.y, b.w, b.h, null);
        for (Block ab : alienBullets) if (!ab.used && ab.img != null) g.drawImage(ab.img, ab.x, ab.y, ab.w, ab.h, null);
    }

    private void updateGame() {
        if (paused || gameOver) return;

        if (System.currentTimeMillis() - lastAlienSpawnTime >= alienSpawnInterval) {
            Random rnd = new Random();
            if (!alienImgs.isEmpty()) {
                BufferedImage img = alienImgs.get(rnd.nextInt(alienImgs.size()));
                int x = rnd.nextInt(Math.max(1, boardWidth - tileSize * 2));
                alienArray.add(new Block(x, 0, tileSize * 2, tileSize, img));
            }
            lastAlienSpawnTime = System.currentTimeMillis();
        }

        for (Block a : alienArray) {
            if (!a.alive) continue;
            a.y += alienFallSpeed;
            if (a.y + a.h >= ship.y) {
                gameOver = true;
                saveHighestScore();
            }
            if (!alienBulletImgs.isEmpty() && new Random().nextInt(100) < 1) {
                BufferedImage bulletImg = alienBulletImgs.get(0);
                alienBullets.add(new Block(a.x + a.w / 2, a.y + a.h, bulletW, bulletH, bulletImg));
            }
        }

        for (Block b : bulletArray) {
            b.y += bulletVelY;
            for (Block a : alienArray) {
                if (!b.used && a.alive && collides(b, a)) {
                    b.used = true;
                    a.alive = false;
                    score += 100;
                }
            }
        }
        bulletArray.removeIf(b -> b.used || b.y < 0);

        for (Block ab : alienBullets) {
            ab.y += alienBulletVelY;
            if (collides(ab, ship)) {
                gameOver = true;
                saveHighestScore();
            }
        }
        alienBullets.removeIf(ab -> ab.used || ab.y > boardHeight);

        // Difficulty increase - Adjusting alien speed to make it slower
        if (score >= 22000) {
            alienFallSpeed = 9; // Decreased from 14
            alienBulletVelY = 8; // Decreased from 13
        } else if (score >= 20000) {
            alienFallSpeed = 8; // Decreased from 13
            alienBulletVelY = 7; // Decreased from 12
        } else if (score >= 18000) {
            alienFallSpeed = 7; // Decreased from 12
            alienBulletVelY = 6; // Decreased from 11
        } else if (score >= 15000) {
            alienFallSpeed = 6; // Decreased from 11
            alienBulletVelY = 5; // Decreased from 10
        } else if (score >= 12000) {
            alienFallSpeed = 5; // Decreased from 10
            alienBulletVelY = 4; // Decreased from 9
        } else if (score >= 8000) {
            alienFallSpeed = 4; // Decreased from 8
            alienBulletVelY = 3; // Decreased from 7
        } else if (score >= 4000) {
            alienFallSpeed = 3; // Decreased from 6
            alienBulletVelY = 2; // Decreased from 5
        }
    }

    private boolean collides(Block a, Block b) {
        return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
    }

    private void handleInput() {
        if (gameOver || paused) return;

        if (keysPressed.contains(KeyEvent.VK_LEFT)) {
            ship.x = Math.max(0, ship.x - shipVelocityX);
        }
        if (keysPressed.contains(KeyEvent.VK_RIGHT)) {
            ship.x = Math.min(boardWidth - ship.w, ship.x + shipVelocityX);
        }
        if (keysPressed.contains(KeyEvent.VK_SPACE) && System.currentTimeMillis() - lastFiredTime >= fireCooldown) {
            int bulletsToFire = (score >= 2500) ? 4 : (score >= 1000) ? 3 : (score >= 500) ? 2 : 1;
            for (int i = 0; i < bulletsToFire; i++) {
                int offset = (i == 0) ? -bulletW : (i == 1) ? bulletW : (i == 2) ? -bulletW * 2 : bulletW * 2;
                bulletArray.add(new Block(ship.x + ship.w / 2 + offset, ship.y - bulletH, bulletW, bulletH, shipBulletImg));
            }
            lastFiredTime = System.currentTimeMillis();
        }
    }

    private void restartGame() {
        ship = new Block(boardWidth / 2 - shipW / 2, shipY, shipW, shipH, shipImg);
        alienArray.clear();
        bulletArray.clear();
        alienBullets.clear();
        score = 0;
        gameOver = false;
        lastFiredTime = 0;
        lastAlienSpawnTime = System.currentTimeMillis();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            handleInput();
            updateGame();
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            paused = !paused;
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && gameOver) {
            restartGame();
        } else if (e.getKeyCode() == KeyEvent.VK_C && paused) {
            paused = false;
        } else if (e.getKeyCode() == KeyEvent.VK_Q && paused) {
            System.exit(0);
        } else {
            keysPressed.add(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private int readHighestScore() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SCORE_FILE))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                String[] data = line.split(";");
                if (data.length == 2) {
                    highestScorer = data[0];
                    return Integer.parseInt(data[1]);
                }
            }
        } catch (IOException | NumberFormatException ex) {
            System.err.println("Error reading the highest score.");
        }
        return 0;  // Return 0 if there is no valid data
    }

    private void saveHighestScore() {
        if (score > highestScore) {
            highestScore = score;
            highestScorer = playerName;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(SCORE_FILE))) {
                writer.write(playerName + ";" + score);
            } catch (IOException e) {
                System.err.println("Failed to save high score.");
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Invaders");
        SpaceInvaders gamePanel = new SpaceInvaders("Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(gamePanel);
        frame.pack();
        frame.setVisible(true);
        gamePanel.promptPlayerName();
    }
}
