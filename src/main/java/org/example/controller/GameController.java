package org.example.controller;

import org.example.model.unit.Vector2;

import java.net.InetAddress;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /*@Deprecated
    public void runLab2() {
        // Always left team
        Vector2 playerPosition = new Vector2(-10, -20);

        PlayerController playerController = new PlayerController("Only_team", false, playerPosition);
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()){
            executorService.execute(playerController);
        }
    }*/

    /*public void runLab3() {
        int teamMatesAmountFirst = 3;
        List<PlayerController> firstTeam = List.of(
                new PlayerController("First_team", false, new Vector2(-10, 0), teamMatesAmountFirst),
                new PlayerController("First_team", false, new Vector2(-10, -20), teamMatesAmountFirst),
                new PlayerController("First_team", false, new Vector2(-15, -20), teamMatesAmountFirst)
        );

        int teamMatesAmountSecond = 1;
        List<PlayerController> secondTeam = List.of(
                new PlayerController("Second_team", true, new Vector2(-50, 0), teamMatesAmountSecond)
        );


        try (ExecutorService executorService = Executors.newFixedThreadPool(firstTeam.size() + secondTeam.size())) {
            for (PlayerController playerController : firstTeam) {
                executorService.submit(playerController);
            }
            for (PlayerController playerController : secondTeam) {
                executorService.submit(playerController);
            }
        }
    }*/

    /*public void runLab4() {
        int teamMatesAmountFirst = 2;
        List<PlayerController> firstTeam = List.of(
                PlayerController.createPlayerForLab4("First_team", false, new Vector2(-10, 0), teamMatesAmountFirst, true),
                PlayerController.createPlayerForLab4("First_team", false, new Vector2(-10, -20), teamMatesAmountFirst, false)
        );

        int teamMatesAmountSecond = 2;
        List<PlayerController> secondTeam = List.of(
                // Players with empty logic standing near goal to evade free_kick event
                new PlayerController("Second_team", new Vector2(-50, -10)),
                new PlayerController("Second_team", new Vector2(-50, 10))
        );

        try (ExecutorService executorService = Executors.newFixedThreadPool(firstTeam.size() + secondTeam.size())) {
            for (PlayerController playerController : firstTeam) {
                executorService.submit(playerController);
            }
            for (PlayerController playerController : secondTeam) {
                executorService.submit(playerController);
            }
        }
    }*/

    /*public void runLab5() {
        int teamMatesAmountFirst = 1;
        List<PlayerController> firstTeam = List.of(
                new PlayerController("First_team", false, new Vector2(-10, 0), teamMatesAmountFirst)
        );

        int teamMatesAmountSecond = 1;
        List<PlayerController> secondTeam = List.of(
                new PlayerController("Second_team", true, new Vector2(-50, 0), teamMatesAmountSecond)
        );

        try (ExecutorService executorService = Executors.newFixedThreadPool(firstTeam.size() + secondTeam.size())) {
            for (PlayerController playerController : firstTeam) {
                executorService.submit(playerController);
            }
            for (PlayerController playerController : secondTeam) {
                executorService.submit(playerController);
            }
        }
    }*/

    public void runLab6(InetAddress address) {
        String firstTeamName = "Random55";
        int teamMatesAmountFirst = 11;

        AtomicBoolean isInit = new AtomicBoolean(true);

        List<PlayerController> firstTeam = List.of(
                PlayerController.createGoaliePlayer(firstTeamName, new Vector2(-50, 0), teamMatesAmountFirst, new Vector2(-50, 0), address),

                PlayerController.createGuardPlayer(firstTeamName, new Vector2(-26, 13), teamMatesAmountFirst, new Vector2(-26, 13), false, address),
                PlayerController.createGuardPlayer(firstTeamName, new Vector2(-30, 24), teamMatesAmountFirst, new Vector2(-30, 24), false, address),
                PlayerController.createGuardPlayer(firstTeamName, new Vector2(-26, -13), teamMatesAmountFirst, new Vector2(-26, -13), true, address),
                PlayerController.createGuardPlayer(firstTeamName, new Vector2(-30, -24), teamMatesAmountFirst, new Vector2(-30, -24), true, address),

                PlayerController.createMidguardPlayer(firstTeamName, new Vector2(-12, 6), teamMatesAmountFirst, new Vector2(-3, 17), false, isInit, address),
                PlayerController.createMidguardPlayer(firstTeamName, new Vector2(-12, -6), teamMatesAmountFirst, new Vector2(-3, -17), true, isInit, address),

                PlayerController.createAttackPlayer(firstTeamName, new Vector2(-3, 13), teamMatesAmountFirst, new Vector2(26, 13), false, address),
                PlayerController.createAttackPlayer(firstTeamName, new Vector2(-3, 24), teamMatesAmountFirst, new Vector2(30, 24), false, address),
                PlayerController.createAttackPlayer(firstTeamName, new Vector2(-3, -13), teamMatesAmountFirst, new Vector2(26, -13), true, address),
                PlayerController.createAttackPlayer(firstTeamName, new Vector2(-3, -24), teamMatesAmountFirst, new Vector2(30, -24), true, address)

        );

        try (ExecutorService executorService = Executors.newFixedThreadPool(firstTeam.size())) {
            for (PlayerController playerController : firstTeam) {
                executorService.submit(playerController);
            }
        }
    }
}
