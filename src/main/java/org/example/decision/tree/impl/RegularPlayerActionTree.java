package org.example.decision.tree.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.decision.planner.Planner;
import org.example.decision.planner.step.ApproachFlagStep;
import org.example.decision.planner.step.KickBallForGoalStep;
import org.example.decision.tree.TreeNode;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Player;
import org.example.sender.action.*;
import org.example.decision.tree.ActionTree;
import org.example.utils.PerceptionUtils;

import java.util.*;

@Slf4j
public class RegularPlayerActionTree extends ActionTree {

    private final static double SEEK_PLAYERS_TURN_STEP = 50;
    private final static double MAIN_PLAYER_DIRECTION_ABS_RAD = Math.toRadians(30);

    private final static double MIN_DISTANCE_TO_MAIN_PLAYER = 1;
    private static final double MAX_DISTANCE_TO_MAIN_PLAYER = 10;
    private static final double LESS_AVG_DISTANCE_TO_MAIN_PLAYER = 3;
    private static final double DELTA_DIRECTION_TO_MAIN_PLAYER_RAD = Math.toRadians(10); // TODO: may want to get up to 10 deg
    private static final double CLOSE_DISTANCE_TO_MAIN_PLAYER_POWER = 100; // Must be higher than moving speed of main player
    private static final double NORMAL_MOVING_POWER = 50;
    private static final double SLOW_MOVING_POWER = 20;
    private final Planner mainPlayerPlan;

    private Boolean isMainPlayer = null;
    // In case this player is following
    private Integer mainPlayerNumber = -1;
    private Double targetDirectionToMainPlayer = null;
    private final SortedSet<Integer> playerNumbers;

    private Double rotationProgress = 0d;
    private Boolean skipRotationFlag = false;

    private Boolean isTurnSeek = true;
    private Integer turnCounter = 0;


    public RegularPlayerActionTree(Knowledge knowledge) {
        super(knowledge);

        mainPlayerPlan = new Planner(knowledge, List.of(
                new ApproachFlagStep("f p l c"),
//        new ApproachFlagStep("g l"),
//        new ApproachFlagStep("f c"),
                new KickBallForGoalStep()
        ));

        playerNumbers = new TreeSet<>();
    }

    @Override
    public boolean checkMinimumConditionForPassingPerception(Perception perception) {
        return perception.getSensed() == null || perception.getMarkersSaw().isEmpty();
    }

    @Override
    protected TreeNode createTreeRoot() {
        TreeNode stillRotatingTree = createIsStillRotatingActionTree();
        createdTreeNodes.put("stillRotating", stillRotatingTree);

        TreeNode isMainPlayerTree = createIsMainPlayerActionTree();
        createdTreeNodes.put("isMainPlayer", isMainPlayerTree);

        return (perception, knowledge, args) -> {

            log.debug("Root node. isMainPlayer: " + isMainPlayer);
            // Step 1. Is this player the main?
            if (isMainPlayer == null) {
                // Step 1.1. It isn't known yet
                // But we how many players out there. Rotate until see everyone
                return stillRotatingTree.getResultAction(perception, knowledge);
            } else {
                return isMainPlayerTree.getResultAction(perception, knowledge);
            }
        };
    }

    private TreeNode createIsStillRotatingActionTree() {
        TreeNode rotateOrStopTree = createRotateOrStopActionTree();
        createdTreeNodes.put("rotateOrStop", rotateOrStopTree);

        return (perception, knowledge, args) -> {

            log.debug("skipRotationFlag: " + skipRotationFlag + "; playerNumbers.size(): " + playerNumbers.size());

            if (!skipRotationFlag && playerNumbers.size() != (knowledge.getTeammatesAmount() - 1)) {

                List<Integer> playerList = perception.getTeammatesSaw().stream()
                        .map(Player::getUniformNumber)
                        .filter(Objects::nonNull)
                        .toList();

                playerNumbers.addAll(
                        playerList
                );

                // Rotate until make full circle
                return rotateOrStopTree.getResultAction(perception, knowledge);
            } else {
                // Step 1.2. Split all players numbers on groups of 3 and find this player's group
                playerNumbers.add(knowledge.getUniformNumber());
                List<Integer> playerNumbersList = playerNumbers.stream().toList();

                if (playerNumbersList.size() / 3 == 0) {
                    int main = playerNumbersList.getFirst();

                    if (main == knowledge.getUniformNumber()) {
                        isMainPlayer = true;
                    }

                    if (playerNumbersList.size() == 2) {
                        int following = playerNumbersList.get(1);
                        if (following == knowledge.getUniformNumber()) {
                            isMainPlayer = false;
                            mainPlayerNumber = main;
                            targetDirectionToMainPlayer = 0d;
                        }
                    }
                }

                for (int i = 0; i < playerNumbersList.size() / 3; i++) {
                    int main = playerNumbersList.get(i * 3);

                    if (main == knowledge.getUniformNumber()) {
                        isMainPlayer = true;
                        break;
                    }

                    if (i * 3 + 2 < playerNumbersList.size()) {
                        int following1 = playerNumbersList.get(i * 3 + 1);
                        int following2 = playerNumbersList.get(i * 3 + 2);
                        if (following1 == knowledge.getUniformNumber()) {
                            isMainPlayer = false;
                            mainPlayerNumber = main;
                            targetDirectionToMainPlayer = MAIN_PLAYER_DIRECTION_ABS_RAD;
                            break;
                        } else if (following2 == knowledge.getUniformNumber()) {
                            isMainPlayer = false;
                            mainPlayerNumber = main;
                            targetDirectionToMainPlayer = -MAIN_PLAYER_DIRECTION_ABS_RAD;
                            break;
                        }
                    } else if (i * 3 + 1 < playerNumbersList.size()) {
                        int following = playerNumbersList.get(i * 3 + 1);
                        if (following == knowledge.getUniformNumber()) {
                            isMainPlayer = false;
                            mainPlayerNumber = main;
                            targetDirectionToMainPlayer = 0d;
                            break;
                        }
                    }
                }
                if (isMainPlayer == null) {
                    log.warn("Could not find this player in any group of 3. Considering his is a main one. This is not typical!");
                    isMainPlayer = true;
                }

                return createdTreeNodes.get("isMainPlayer").getResultAction(perception, knowledge);
            }
        };
    }

    private TreeNode createRotateOrStopActionTree() {
        return (perception, knowledge, args) -> {
            log.debug("rotationProgress: " + rotationProgress);
            if (rotationProgress < 360) {
                rotationProgress += SEEK_PLAYERS_TURN_STEP;
                return new TurnAction(SEEK_PLAYERS_TURN_STEP);
            } else {
                // No need to search now...
                skipRotationFlag = true;
                return createdTreeNodes.get("stillRotating").getResultAction(perception, knowledge);
            }
        };
    }

    private TreeNode createIsMainPlayerActionTree() {
        TreeNode mainPlayerTree = createMainPlayerActionTree();
        createdTreeNodes.put("mainPlayer", mainPlayerTree);

        TreeNode followingPlayerTree = createFollowingPlayerTree();
        createdTreeNodes.put("followingPlayer", followingPlayerTree);

        return (perception, knowledge, args) -> {
            if (isMainPlayer) {
                return mainPlayerTree.getResultAction(perception, knowledge);
            } else {
                return followingPlayerTree.getResultAction(perception, knowledge);
            }
        };
    }

    // The Main player is following its own plan
    private TreeNode createMainPlayerActionTree() {
        return (perception, knowledge, args) -> mainPlayerPlan.planAction(perception);
    }

    // The Following player is following main player (maintains distance)
    private TreeNode createFollowingPlayerTree() {
        TreeNode followingPlayerSeeMainTree = createFollowingPlayerSeeMainTree();
        createdTreeNodes.put("followingPlayerSeeMain", followingPlayerSeeMainTree);

        return (perception, knowledge, args) -> {
            // Step 2. Main player is known

            Optional<Player> mainPlayer = PerceptionUtils.getTeammateWithNumber(perception, mainPlayerNumber);

            if (mainPlayer.isEmpty()) {
                if (isTurnSeek) {
//                    isTurnSeek = false;
                    return new TurnAction(SEEK_PLAYERS_TURN_STEP);
                } else {
                    isTurnSeek = true;
                    return new DashAction(100);
                }
            } else {
                return followingPlayerSeeMainTree.getResultAction(perception, knowledge, mainPlayer.get());
            }
        };
    }

    private TreeNode createFollowingPlayerSeeMainTree() {
        TreeNode getCloseToMainPlayerTree = createGetCloseToMainPlayerTree();
        createdTreeNodes.put("getCloseToMainPlayer", getCloseToMainPlayerTree);

        TreeNode controlDirectionAndDistanceTree = createControlDirectionAndDistanceTree();
        createdTreeNodes.put("controlDirectionAndDistance", controlDirectionAndDistanceTree);

        return (perception, knowledge, args) -> {
            Player mainPlayer = (Player) args[0];

            if (mainPlayer.getDistance() < MIN_DISTANCE_TO_MAIN_PLAYER) {
                // If main player is too close - stop by doing nothing
                return EmptyAction.instance;
            } else if (mainPlayer.getDistance() > MAX_DISTANCE_TO_MAIN_PLAYER) {
                // If main player is too far - get close to him
                return getCloseToMainPlayerTree.getResultAction(perception, knowledge, args);
            } else {
                // If it's average distance
                return controlDirectionAndDistanceTree.getResultAction(perception, knowledge, args);
            }
        };
    }

    private TreeNode createGetCloseToMainPlayerTree() {
        return (perception, knowledge, args) -> {
            Player mainPlayer = (Player) args[0];

            if (Math.abs(mainPlayer.getDirection()) < DELTA_DIRECTION_TO_MAIN_PLAYER_RAD) {
                // If facing approx. right - dash
                return new DashAction(CLOSE_DISTANCE_TO_MAIN_PLAYER_POWER);
            } else {
                // If not - turn to the main player
                return new TurnAction(Math.toDegrees(mainPlayer.getDirection()));
            }

//            return new DashAction(CLOSE_DISTANCE_TO_MAIN_PLAYER_POWER, Math.toDegrees(mainPlayer.getDirection()));
        };
    }

    private TreeNode createControlDirectionAndDistanceTree() {
        TreeNode controlDistanceTree = createControlDistanceTree();
        createdTreeNodes.put("controlDistance", controlDistanceTree);

        return (perception, knowledge, args) -> {
            Player mainPlayer = (Player) args[0];

            if (Math.abs((mainPlayer.getDirection() - targetDirectionToMainPlayer)) > DELTA_DIRECTION_TO_MAIN_PLAYER_RAD) {
                // If direction to the main player is wrong - fix it

                if (turnCounter == 2) {
                    turnCounter = 0;
                    return new TurnAction(Math.toDegrees(targetDirectionToMainPlayer - mainPlayer.getDirection()));
                } else {
                    turnCounter++;
                    return new DashAction(CLOSE_DISTANCE_TO_MAIN_PLAYER_POWER, Math.toDegrees(targetDirectionToMainPlayer - mainPlayer.getDirection()));
                }
            } else {
                // If it's right - move
                return controlDistanceTree.getResultAction(perception, knowledge, args);
            }
        };
    }

    private TreeNode createControlDistanceTree() {
        return (perception, knowledge1, args) -> {
            Player mainPlayer = (Player) args[0];
            if (mainPlayer.getDistance() < LESS_AVG_DISTANCE_TO_MAIN_PLAYER) {
                return new DashAction(SLOW_MOVING_POWER);
            } else {
                return new DashAction(NORMAL_MOVING_POWER);
            }
        };
    }
}
