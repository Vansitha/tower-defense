package edu.curtin.saed.assignment1.worldobjects;

public class Robot {
    private final int id;
    private final int delay;

    private double currX;
    private double currY;

    private double prevX;

    private double prevY;

    private double nextX;

    private double nextY;

    public Robot(int id, int delay, double currX, double currY) {
        this.id = id;
        this.delay = delay;
        this.currX = currX;
        this.currY = currY;
    }


    public double getCurrX() {
        return currX;
    }

    public void setCurrX(double currX) {
        this.currX = currX;
    }

    public void setCurrY(double currY) {
        this.currY = currY;
    }

    public double getCurrY() {
        return currY;
    }

    public double getPrevX() {
        return prevX;
    }

    public void setPrevX(double prevX) {
        this.prevX = prevX;
    }

    public double getPrevY() {
        return prevY;
    }

    public void setPrevY(double prevY) {
        this.prevY = prevY;
    }

    public double getNextX() {
        return nextX;
    }

    public double getNextY() {
        return nextY;
    }

    public void setNextX(double nextX) {
        this.nextX = nextX;
    }

    public void setNextY(double nextY) {
        this.nextY = nextY;
    }

    public int getId() {
        return id;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Robot other = (Robot) obj;
        return id == other.id;
    }
}
