package org.main.entity;

import org.main.SimulationPanel;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Beetle extends Crawlie {
    public static int startingLife = 3;
    public static int delay = 15;
    public int delayLeft = delay;
    private Timer respawnTimer = new Timer();

    public Beetle(int x, int y, SimulationPanel sp) {
        super(x, y, sp);
    }

    @Override
    public void update() {
        if (life <= 0) {
            killme = true;
            return;
        }

        Random random = new Random();
        lock.lock();

        // Find food on path
        Entity foodOnPath = sp.entities.stream()
                .filter(e -> e instanceof Food)
                .filter(e -> e.x == x)
                .filter(e -> e.y == y)
                .findFirst().orElse(null);

        // If food on path, remove and move to a new spot
        if (foodOnPath != null) {
            sp.remove(foodOnPath);
            respawnTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int newX = x + random.nextInt(3) - 1;
                    int newY = y + random.nextInt(3) - 1;
                    if (!sp.breaches(newX, newY)) {
                        sp.entities.add(new Food(newX, newY, sp));
                    }
                }
            }, 3000);
        }


        // If no destination, walk in a random direction
        if ((xDest < 0 || yDest < 0)) {
            do {
                xDest = x + 20 * (random.nextInt(3) - 1);
                yDest = y + 20 * (random.nextInt(3) - 1);
            } while (!sp.breaches(xDest, yDest));
        } else if (x == xDest && y == yDest) {
            xDest = -1;
            yDest = -1;
        } else {
            if (delayLeft > 0) {
                delayLeft--;
            } else {
                if (x < xDest) {
                    x++;
                } else if (x > xDest) {
                    x--;
                }
                if (y < yDest) {
                    y++;
                } else if (y > yDest) {
                    y--;
                }
                delayLeft = delay;
            }
        }

        lock.unlock();
    }
}