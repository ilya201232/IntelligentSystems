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

    public double getDistance(Vector2 another) {
        return Math.sqrt(Math.pow((x - another.getX()), 2) + Math.pow((y - another.getY()), 2));
    }

    public static Vector2 createVector(Vector2 startPoint, Vector2 targetPoint) {
        return new Vector2(
                targetPoint.x - startPoint.x,
                targetPoint.y - startPoint.y
        );
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

    public static double dotProduct(Vector2 first, Vector2 second) {
        return first.x * second.x + first.y * second.y;
    }

    public static double getAngleBetweenVectors(Vector2 first, Vector2 second) {
        Vector3 Vn = new Vector3(0, 0, 1);

        return Math.atan2(
                Vector3.dotProduct(Vector3.crossProduct(first, second), Vn),
                Vector2.dotProduct(first, second)
        );
    }

    public static Vector2 getPositionInMiddle(Vector2 first, Vector2 second) {
        return new Vector2(
                (first.x + second.x) / 2,
                (first.y + second.y) / 2
        );
    }

}
