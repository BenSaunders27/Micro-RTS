package ai.strategychooser;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.RangedRush;
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

public class RangedRush2  extends AbstractionLayerAI {

    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    Unit ourBase;

    // If we have any "light": send it to attack to the nearest enemy unit
    // If we have a base: train worker until we have 1 workers
    // If we have a barracks: train light
    // If we have a worker: do this if needed: build base, build barracks, harvest resources
    public RangedRush2(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }


    public RangedRush2(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        rangedType = utt.getUnitType("Ranged");
    }

    public AI clone() {
        return new RangedRush(utt, pf);
    }

    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");

        // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                baseBehavior(u, p, pgs);
            }
        }

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                meleeUnitBehavior(u, p, gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
            }
        }
        workersBehavior(workers, p, pgs, gs);


        return translateActions(player, gs);
    }

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nworkers = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
        }
        if (nworkers < 2 && p.getResources() >= workerType.cost) {
            train(u, workerType);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        if (p.getResources() >= rangedType.cost) {
            train(u, rangedType);
        }
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
//            System.out.println("LightRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
            attack(u, closestEnemy);
        }
    }

    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs, GameState gs) {
        int nbases = 0;
        int nbarracks = 0;

        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        freeWorkers.addAll(workers);

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


        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases == 0 && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX() ,u.getY() ,reservedPositions,p,pgs);
                resourcesUsed += baseType.cost;
            }
        }

        if (nbarracks < 1) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);

                ArrayList<Integer> positions = barracksPosition(pgs,u);
                buildIfNotAlreadyBuilding(u,barracksType,positions.get(0),positions.get(1),reservedPositions,p,pgs);
                //buildIfNotAlreadyBuilding(u,barracksType,baseX + nbarracks  , baseY - 2 ,reservedPositions,p,pgs);

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
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
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
                // Remaining workers should attack
                meleeUnitBehavior(u, p, gs);
            }

        }
    }

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

