package org.example.model.unit;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Vector2 {

    private double x;
    private double y;

    public Vector2(Vector2 vector) {
        x = vector.getX();
        y = vector.getY();
    }

    public static Vector2 toAbsolutePos(Vector2 startPos, Side teamSide) {
        Vector2 newVector = new Vector2(startPos);

        if (teamSide == Side.LEFT) {
            newVector.setY(newVector.getY() * -1);
        }

        if (teamSide == Side.RIGHT) {
            newVector.setX(newVector.getX() * -1);
        }

        return newVector;
    }

    public double getDistance(Vector2 another) {
        return Math.sqrt(Math.pow((x - another.getX()), 2) + Math.pow((y - another.getY()), 2));
    }
}
