package ai.strategychooser;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;



/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author santi
 */
public class WorkerRush2 extends AbstractionLayerAI {
    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType heavyType;

    // Strategy implemented by this class:
    // If we have more than 1 "Worker": send the extra workers to attack to the nearest enemy unit
    // If we have a base: train workers non-stop
    // If we have a worker: do this if needed: build base, harvest resources
    public WorkerRush2(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }


    public WorkerRush2(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable a_utt)
    {
        utt = a_utt;
        if (utt!=null) {
            workerType = utt.getUnitType("Worker");
            baseType = utt.getUnitType("Base");
        }
    }


    public AI clone() {
        return new ai.abstraction.WorkerRush(utt, pf);
    }

    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        PlayerAction pa = new PlayerAction();
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");

        // behavior of bases:
        for(Unit u:pgs.getUnits()) {
            if (u.getType()==baseType &&
                    u.getPlayer() == player &&
                    gs.getActionAssignment(u)==null) {
                baseBehavior(u,p,pgs);
            }
        }

        // behavior of melee units:
        for(Unit u:pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest &&
                    u.getPlayer() == player &&
                    gs.getActionAssignment(u)==null) {
                meleeUnitBehavior(u,p,gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for(Unit u:pgs.getUnits()) {
            if (u.getType().canHarvest &&
                    u.getPlayer() == player) {
                workers.add(u);
            }
        }
        workersBehavior(workers,p,gs);


        return translateActions(player,gs);
    }


    public void baseBehavior(Unit u,Player p, PhysicalGameState pgs) {
        if (p.getResources()>=workerType.cost) train(u, workerType);
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for(Unit u2:pgs.getUnits()) {
            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy==null || d<closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy!=null) {
            attack(u,closestEnemy);
        }
    }

    public void workersBehavior(List<Unit> workers,Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();

        int nbases = 0;
        int nbarracks = 0;
        int baseX = 0;
        int baseY = 0;

        int resourcesUsed = 0;

        if (workers.isEmpty()) {
            return;
        }

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) {
                nbases++;
                baseX = u2.getX();
                baseY = u2.getY();
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
        for (int i = 0; i < workers.size(); i++) {
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
                buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
                resourcesUsed += baseType.cost;
            }
        }


        // WorkerRush doesn't build barracks
        /*
        if (nbarracks < 1) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                ArrayList<Integer> positions = barracksPosition(pgs,u);
                buildIfNotAlreadyBuilding(u,barracksType,positions.get(0),positions.get(1),reservedPositions,p,pgs);
                resourcesUsed += barracksType.cost;
            }
        }
        */


        // harvest with all the free workers:
        List<Unit> availableResources = new ArrayList<Unit>();
        int totalResourceCount = 0;
        int harvestingResources = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                availableResources.add(u2);
                totalResourceCount++;
            }
        }

        for (Unit u : freeWorkers) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;

            for (Unit u2 : availableResources) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestResource == null || d < closestDistance) {
                    closestResource = u2;
                    closestDistance = d;
                }
            }

            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null && harvestingResources < totalResourceCount / 2) {
                availableResources.remove(closestResource);
                harvestingResources++;
                AbstractAction aa = getAbstractAction(u);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest) aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase)
                        harvest(u, closestResource, closestBase);
                } else {
                    harvest(u, closestResource, closestBase);
                }
            } else {
                // Remaining workers should attack
                boolean attackBase = false;
                meleeUnitBehavior(u, p, gs);
            }

        }
    }



        @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }



}
