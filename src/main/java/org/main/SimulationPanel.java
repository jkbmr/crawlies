package org.main;

import org.main.entity.*;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationPanel extends JPanel implements Runnable {
    // A bunch of variables that control the panel
    private final int originalTileSize = 3;
    private final int scale = 3;
    private final int tileSize = originalTileSize * scale;
    private final int maxCol = 64;
    private final int maxRow = 48;
    private final int panelWidth = tileSize * maxCol;
    private final int panelHeight = tileSize * maxRow;
    private final Lock lock = new ReentrantLock();
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final int delay = 100;
    private int delayLeft = 0;
    Thread thread;

    public ArrayList<Entity> entities = new ArrayList<>();
    public ArrayList<Scolopendra> scolopendras = new ArrayList<>();
    public ConcurrentLinkedQueue<Entity> entityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Entity> deletionQueue = new ConcurrentLinkedQueue<>();
    public SimulationPanel() {
        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
    }

    public void startThread() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        // Don't run if paused
        while (running & thread != null) {
            synchronized (lock) {
                if (!running) { break; }
                if (paused) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        break;
                    } if (!running) {
                        break;
                    }
                }
            }

            // Timeframe limit
            double interval = 20000000;
            double delta = 0;
            long timeLast = System.nanoTime();
            long timeNow;

            while (!paused) {
                timeNow = System.nanoTime();
                delta += (timeNow - timeLast)/interval;
                timeLast = timeNow;

                if (delta >= 1) {
                    update();
                    repaint();
                    delta -= 1;
                }
            }
        }
    }

    // Update function
    public void update() {
        scolopendras.forEach(e -> e.update());
        scolopendras.removeAll(scolopendras.stream().filter(e -> e.life <= 0).toList());
        for (Entity e: entities) {
            // Add entity to deletion queue if killflag, else update
            if (e.killme) {
                deletionQueue.add(e);
            } else {
                e.update();
            }
        }
        entities.removeAll(deletionQueue);
        entities.addAll(entityQueue);

        if (delayLeft > 0) { delayLeft--; }
        else {
            delayLeft = delay;
            spawn();
        }
    }

    // Make the components show up
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        try {
            for (Entity e : entities) {
                if (e.killme) {
                } else if (e instanceof Mite) {
                    g.setColor(new Color(255, 253, 208));
                    g.fillRect(e.x * tileSize, e.y * tileSize, tileSize, tileSize);
                } else if (e instanceof Food) {
                    g.setColor(new Color(150, 75, 0));
                    g.fillRect(e.x * tileSize, e.y * tileSize, tileSize, tileSize);
                } else if (e instanceof Spider) {
                    g.setColor(new Color(72, 50, 72));
                    g.fillRect(e.x * tileSize, e.y * tileSize, tileSize, tileSize);
                } else if (e instanceof Beetle) {
                    g.setColor(new Color(0, 0, 128));
                    g.fillRect(e.x * tileSize, e.y * tileSize, tileSize, tileSize);
                } else if (e instanceof ScolopendraSegment) {
                    g.setColor(new Color(0, 125, 0));
                    g.fillRect(e.x * tileSize, e.y * tileSize, tileSize, tileSize);
                }
            }
        } catch (ConcurrentModificationException ex) {
            // We can ignore this, because the loop will be redone anyway
        }

        g.dispose();
    }

   // For checking if coordinates breach containment
    public boolean breaches(int x, int y) {
        return !(x <= 0 || y <= 0 || x >= maxCol || y >= maxRow);
    }

    public void pauseSimulation() {
        paused = true;
    }

    public void unpauseSimulation() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    // Clear all collections
    public void resetSimulation() {
        running = false;
        delayLeft = 0;
        entities.clear();
        entityQueue.clear();
        deletionQueue.clear();
        scolopendras.clear();
        running = true;
        unpauseSimulation();
    }

    public void printInfo() {
        System.out.println("Mites: " + entities.stream().filter(e -> e instanceof Mite).count());
        System.out.println("Beetles: " + entities.stream().filter(e -> e instanceof Beetle).count());
        System.out.println("Spiders: " + entities.stream().filter(e -> e instanceof Spider).count());
        System.out.println("Scolopendras: " + scolopendras.size());
        System.out.println("Food: " + entities.stream().filter(e -> e instanceof Food).count());
        System.out.println("*********************");
    }

    private void spawn() {
        Random random = new Random();

        int x = random.nextInt(65), y = random.nextInt(49);

        int entityRandomizer = random.nextInt(200);
        if (entityRandomizer <= 40) {
            entities.add(new Mite(x, y, this));
        } else if (entityRandomizer <= 80) {
            entities.add(new Food(x, y, this));
        } else if (entityRandomizer <= 140) {
            entities.add(new Beetle(x, y, this));
        } else if ((entityRandomizer <= 160 || scolopendras.size() > 2)
            && entities.stream().filter(e -> e instanceof Spider).count() < 10) {
            entities.add(new Spider(x, y, this));
        } else {
            Scolopendra scolopendra = new Scolopendra(x, y, this);
            scolopendras.add(scolopendra);
        }
    }

    public void remove(Entity e) {
        e.killme = true;
    }
}
