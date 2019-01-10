/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.strategychooser;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

/**
 *
 * @author Matt Lee, Johnny Hind, Ben Saunders
 */

public class HeavyRush2 extends AbstractionLayerAI {

    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType heavyType;
    Unit ourBase;

    // Strategy implemented by this class:
    // For the first 4 melee units: send it to attack to the nearest enemy unit
    // If over 4 melee units, send the rest to attack the base
    // If we have a base: train workers until we have 2 workers
    // If we have a barracks: train light and heavy in randomness
    // If we have a worker: do this if needed: build base, build barracks, harvest resources, attack if enemy close

    public HeavyRush2(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset(UnitTypeTable a_utt)
    {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
    }


    public AI clone() {
        return new HeavyRush2(utt, pf);

    }

    /*
        This is the main function of the AI. It is called at each game cycle with the most up to date game state and
        returns which actions the AI wants to execute in this cycle.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        This method returns the actions to be sent to each of the units in the gamestate controlled by the player,
        packaged as a PlayerAction.
     */
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);

        // Calculate how many bases units the enemy has
        int nmelee = 0;
        int nEnemyBase = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() != p.getID()) {
                nEnemyBase++;
            }
        }

        // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                baseBehavior(u, p, pgs);
        // behavior of barracks:
            } else if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
        // behavior of melee units:
            } else if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {

                boolean attackBase;
                nmelee++;
                //If 4 or more melee units, and enemy base still standing - use 4th and higher units to attack base
                if (nmelee >= 4 && nEnemyBase > 0) {
                    // If 4th or higher melee unit, attack the base
                    attackBase = true;
                } else {
                    attackBase = false;
                }
                meleeUnitBehavior(u, p, pgs, attackBase);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
                //System.out.println("X: " + u.getX() + " Y: " + u.getY());
            }
        }
        // Set all workers to observe workersBehaviour
        workersBehavior(workers, p, pgs, gs);

        // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
        return translateActions(player, gs);
    }

    // Method to operate the behaviour of bases
    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nSelfWorkers = 0;
        int nOppWorkers = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nSelfWorkers++;
            } else if (u2.getType() == workerType
                    && u2.getPlayer() != p.getID()){
                nOppWorkers++;
            }
        }
        //Train up to 4 Workers
        if ((nSelfWorkers < 4 || nSelfWorkers < nOppWorkers) && p.getResources() >= workerType.cost) {
            train(u, workerType);
        }
    }

    // Method to operate the behaviour of barracks
    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nbarracks = 0;

        // Calculate how many barracks
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }

        double random = Math.random();

        // If only one barracks, only train Light units
        // If 2 barracks, randomly train Light or Heavy units

        if (nbarracks == 1 || (nbarracks > 1 && random < 0.5)) {
            if (p.getResources() >= lightType.cost) {
                train(u, lightType);
            }
        } else if (nbarracks > 1 && random >= 0.5) {
            if (p.getResources() >= heavyType.cost) {
                train(u, heavyType);
            }
        }
    }

    // Method to operate the behaviour of barracks
    public void meleeUnitBehavior(Unit u, Player p, PhysicalGameState pgs, boolean attackBase) {

        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            // Just attack closest enemy if not scheduled to attack base
            if (!attackBase){
                if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                    // Find closest enemy
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestEnemy == null || d < closestDistance) {
                        closestEnemy = u2;
                        closestDistance = d;
                    }
                }
            // Attack the closest base if scheduled to attack base
            } else if (attackBase){
                if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID() && u2.getType() == baseType) {
                    // Find closest base
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestEnemy == null || d < closestDistance) {
                        closestEnemy = u2;
                        closestDistance = d;
                    }
                }
            }
        }

        if (closestEnemy != null) {
            attack(u, closestEnemy);
        }
    }



    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs, GameState gs) {
        int nbases = 0;
        int nbarracks = 0;

        int resourcesUsed = 0;

        if (workers.isEmpty()) {
            return;
        }

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
                ourBase = u2;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }



        List<Unit> freeWorkers = new LinkedList<Unit>();

        // Add in to turn into Attack Units if an enemy comes close (within 4)
        // Locate the closest enemy
        Unit closestEnemy = null;
        int closestEnemyDistance = 0;
        for (int i = 0; i < workers.size();i++ ) {
            Unit u = workers.get(i);
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestEnemy == null || d < closestEnemyDistance) {
                        closestEnemy = u2;
                        closestEnemyDistance = d;
                    }
                }
            }

            //Turn into attack units if an enemy comes close (within 4)
            if (closestEnemy != null && closestEnemyDistance < 4) {
                attack(u, closestEnemy);
            } else {
                //Add to freeWorkers if they aren't programmed to attack
                freeWorkers.add(u);
            }
        }


        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases < 1 && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX() ,u.getY() ,reservedPositions,p,pgs);
                resourcesUsed += baseType.cost;
            }
        }

        if (nbarracks < 2) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                ArrayList<Integer> positions = barracksPosition(pgs,u);
                buildIfNotAlreadyBuilding(u,barracksType,positions.get(0),positions.get(1),reservedPositions,p,pgs);
                resourcesUsed += barracksType.cost;
            }
        }


        // harvest with all the free workers:
        List<Unit> availableResources = new ArrayList<Unit>();
        int totalResourceCount = 0;
        int harvestingResources = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                availableResources.add(u2);
                totalResourceCount ++;
            }
        }

        // For each free worker
        for (Unit u : freeWorkers) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;

            // Find the closest resource
            for (Unit u2 : availableResources) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestResource == null || d < closestDistance) {
                    closestResource = u2;
                    closestDistance = d;
                }
            }

            // Find closest base
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            // Only harvest resources up to 1 worker per resource
            if (closestResource != null && closestBase != null && harvestingResources < totalResourceCount/2) {
                availableResources.remove(closestResource);
                harvestingResources++;
                AbstractAction aa = getAbstractAction(u);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                } else {
                    harvest(u, closestResource, closestBase);
                }
            } else {
                // Remaining workers should attack enemy
                boolean attackBase = false;
                meleeUnitBehavior(u, p, pgs, attackBase);
            }

        }
    }


    // Method to find the best position to place the barracks
    public ArrayList barracksPosition(PhysicalGameState pgs, Unit u){
        int width = pgs.getWidth();
        int uX = (ourBase == null) ? u.getX() : ourBase.getX();
        int buildX; int spaceX;

        int height = pgs.getHeight();
        int uY = (ourBase == null) ? u.getY() : ourBase.getY();;
        int buildY; int spaceY;

        // If manhatten from bottom left is less than width
        if ((uX+uY) < width){
            //top left, build barracks bottom right cornet
            spaceX = 1;
            spaceY = 2;
        } else {
            //bottom right, build barracks top left corner
            spaceX = 1;
            spaceY = 0;
        }

        //Width

        if ((uX - spaceX > 0) || (uX + spaceX < width)){
            buildX = uX + spaceX;
        } else if ((uX + spaceX > 0) || (uX - spaceX < width)){

            buildX = uX - spaceX;
        } else {
            buildX = uX;
        }

        //Height

        if ((uY - spaceY > 0) || (uY + spaceY < height)){
            buildY = uY + spaceY;
        } else if ((uY + spaceY > 0) || (uY - spaceY < height)){

            buildY = uY - spaceY;
        } else {
            buildY = uY;
        }
        ArrayList<Integer> positions =  new ArrayList<>();
        positions.add(buildX);
        positions.add(buildY);
        return positions;
    }

    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }

}

