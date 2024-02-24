package org.example.controller;

import org.example.model.unit.Vector2;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameController
{

    private static final Scanner SCANNER = new Scanner(System.in);

    /*@Deprecated
    public void runLab1() {
        Vector2 firstPlayerPosition = new Vector2();
        double turnMoment;

        Vector2 secondPlayerPosition = new Vector2();

        System.out.println("Enter first player relative position: ");
//        firstPlayerPosition.setX(SCANNER.nextDouble());
        firstPlayerPosition.setX(-30);
//        firstPlayerPosition.setY(SCANNER.nextDouble());
        firstPlayerPosition.setY(-16);

        System.out.println("Enter second player relative position: ");
//        secondPlayerPosition.setX(SCANNER.nextDouble());
        secondPlayerPosition.setX(-20);
//        secondPlayerPosition.setY(SCANNER.nextDouble());
        secondPlayerPosition.setY(-16);

        System.out.println("Enter rotation speed (in degrees between -180 and 180): ");
//        turnMoment = SCANNER.nextDouble();
        turnMoment = 10;

        PlayerController firstPlayer = new PlayerController("First_team", false, firstPlayerPosition);
        firstPlayer.setTurnMoment(turnMoment);

        PlayerController secondPlayer = new PlayerController("Second_team", false, secondPlayerPosition);
        secondPlayer.setTurnMoment(turnMoment);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            executorService.execute(firstPlayer);
            executorService.submit(secondPlayer);
        }
    }*/

    public void runLab2() {
        // Always left team
        Vector2 playerPosition = new Vector2(-10, -20);

        PlayerController playerController = new PlayerController("Only_team", false, playerPosition);
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()){
            executorService.execute(playerController);
        }
    }
}
