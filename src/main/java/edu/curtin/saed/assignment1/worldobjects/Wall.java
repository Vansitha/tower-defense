package edu.curtin.saed.assignment1.worldobjects;

public class Wall {

    private final int maxHealthPoints = 100;
    private final int damagedHealthPoints = 50;
    private final int destroyedHealthPoints = 0;
    private double posX;
    private double posY;
    private int healthPoints;

    public Wall(double posX, double posY) {
        this.posX = posX;
        this.posY = posY;
        this.healthPoints = maxHealthPoints;
    }

    public double getPositionX() {
        return posX;
    }

    public double getPositionY() {
        return posY;
    }

    public void setDamaged() {
        this.healthPoints = damagedHealthPoints;
    }

    public boolean isMaxHealth() {
        return this.healthPoints == maxHealthPoints;
    }

    public boolean isDestroyed() {
        return this.healthPoints == destroyedHealthPoints;
    }

}
