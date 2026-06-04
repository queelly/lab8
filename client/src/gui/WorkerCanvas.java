package gui;

import models.Worker;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.*;
import java.util.List;

public class WorkerCanvas extends JPanel {
    private List<Worker> workers = new ArrayList<>();
    private Image backgroundImage;
    private final Map<String, ImageIcon> userPokemonMap = new TreeMap<>();
    private final List<Shape> shapes = new ArrayList<>();
    private final Map<Integer, Worker> shapeToWorkerMap = new HashMap<>();
    private final LocaleManager lm = LocaleManager.getInstance();

    private static final int ICON_SIZE = 48;
    private static final int MIN_GAP   = 8;
    private static final int CELL      = ICON_SIZE + MIN_GAP;

    public WorkerCanvas() {
        setPreferredSize(new Dimension(800, 600));
        loadResources();
        new Timer(16, e -> repaint()).start();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < shapes.size(); i++) {
                    if (shapes.get(i).contains(e.getPoint())) {
                        Worker w = shapeToWorkerMap.get(i);
                        if (w != null) showDetails(w);
                        break;
                    }
                }
            }
        });
    }

    private void loadResources() {
        String basePath = "/Users/queelly/stud/prog/lab8/client/src/resources/";
        File bgFile = new File(basePath + "background.png");
        File folder = new File(basePath + "pokemon/");
        if (bgFile.exists()) backgroundImage = new ImageIcon(bgFile.getPath()).getImage();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files != null) {
            for (File file : files) {
                ImageIcon original = new ImageIcon(file.getPath());
                Image scaledImage = original.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                userPokemonMap.put(file.getName(), new ImageIcon(scaledImage));
            }
        }
    }

    public void setWorkers(List<Worker> workers) {
        this.workers = workers;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundImage != null) g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

        if (workers == null || workers.isEmpty()) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString(lm.get("msg.waiting"), 20, 30);
            return;
        }
        if (userPokemonMap.isEmpty()) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString(lm.get("msg.no_images"), 20, 50);
            return;
        }

        shapes.clear();
        shapeToWorkerMap.clear();

        List<Point> rawPositions = new ArrayList<>();
        for (Worker w : workers) {
            if (w.getCoordinates() == null) rawPositions.add(new Point(0, 0));
            else rawPositions.add(new Point((int) w.getCoordinates().getX(),
                    (int) w.getCoordinates().getY().doubleValue()));
        }

        List<Point> screenSlots = resolveOverlaps(rawPositions);

        int minSX = Integer.MAX_VALUE, minSY = Integer.MAX_VALUE;
        int maxSX = Integer.MIN_VALUE, maxSY = Integer.MIN_VALUE;
        for (Point p : screenSlots) {
            minSX = Math.min(minSX, p.x); minSY = Math.min(minSY, p.y);
            maxSX = Math.max(maxSX, p.x); maxSY = Math.max(maxSY, p.y);
        }
        int contentW = maxSX - minSX + CELL, contentH = maxSY - minSY + CELL, padding = 24;
        double scaleX = (double)(getWidth() - padding * 2) / contentW;
        double scaleY = (double)(getHeight() - padding * 2) / contentH;
        double scale  = Math.min(1.0, Math.min(scaleX, scaleY));
        int offsetX = (getWidth()  - (int)(contentW * scale)) / 2 - (int)(minSX * scale);
        int offsetY = (getHeight() - (int)(contentH * scale)) / 2 - (int)(minSY * scale);

        double pulse = 1.0 + 0.05 * Math.sin(System.currentTimeMillis() / 200.0);
        int drawSize = (int)(ICON_SIZE * scale * pulse);

        for (int i = 0; i < workers.size(); i++) {
            Worker w  = workers.get(i);
            Point slot = screenSlots.get(i);
            int cx = offsetX + (int)(slot.x * scale) + (int)(CELL * scale / 2);
            int cy = offsetY + (int)(slot.y * scale) + (int)(CELL * scale / 2);

            ImageIcon icon = getPokemonForUser(w.getCreator());
            if (icon == null) continue;

            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval(cx - drawSize / 2 + 3, cy - drawSize / 2 + 4, drawSize, drawSize);
            g2d.drawImage(icon.getImage(), cx - drawSize / 2, cy - drawSize / 2, drawSize, drawSize, this);

            FontMetrics fm = g2d.getFontMetrics();
            String label = w.getName();
            int labelX = cx - fm.stringWidth(label) / 2;
            int labelY = cy + drawSize / 2 + fm.getAscent() + 2;
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRoundRect(labelX - 3, labelY - fm.getAscent() - 1,
                    fm.stringWidth(label) + 6, fm.getHeight() + 2, 4, 4);
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, labelX, labelY);

            Ellipse2D circle = new Ellipse2D.Double(cx - drawSize / 2, cy - drawSize / 2, drawSize, drawSize);
            shapes.add(circle);
            shapeToWorkerMap.put(shapes.size() - 1, w);
        }
    }

    private List<Point> resolveOverlaps(List<Point> rawPositions) {
        Set<String> occupied = new HashSet<>();
        List<Point> result = new ArrayList<>();
        for (Point raw : rawPositions) {
            int gx = Math.round((float) raw.x / CELL) * CELL;
            int gy = Math.round((float) raw.y / CELL) * CELL;
            Point slot = findFreeSlot(gx, gy, occupied);
            occupied.add(slot.x + "," + slot.y);
            result.add(slot);
        }
        return result;
    }

    private Point findFreeSlot(int startGx, int startGy, Set<String> occupied) {
        if (!occupied.contains(startGx + "," + startGy)) return new Point(startGx, startGy);
        for (int radius = 1; radius < 200; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                    int gx = startGx + dx * CELL, gy = startGy + dy * CELL;
                    String key = gx + "," + gy;
                    if (!occupied.contains(key)) return new Point(gx, gy);
                }
            }
        }
        return new Point(startGx, startGy);
    }

    private ImageIcon getPokemonForUser(String username) {
        if (username == null || userPokemonMap.isEmpty()) return null;
        List<String> keys = new ArrayList<>(userPokemonMap.keySet());
        return userPokemonMap.get(keys.get(Math.abs(username.hashCode()) % keys.size()));
    }

    private void showDetails(Worker w) {
        JOptionPane.showMessageDialog(this,
                lm.get("msg.details.body", w.getId(), w.getName(), w.getCreator()),
                lm.get("msg.details.title"), JOptionPane.INFORMATION_MESSAGE);
    }
}