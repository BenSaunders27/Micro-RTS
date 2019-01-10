/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.strategychooser;

import ai.evaluation.EvaluationFunction;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author santi
 * 
 * This function uses the same base evaluation as SimpleSqrtEvaluationFunction and SimpleSqrtEvaluationFunction2, but returns the (proportion*2)-1 of the total score on the board that belongs to one player.
 * The advantage of this function is that evaluation is bounded between -1 and 1.
 */
public class ComplexEvaluationFunction extends EvaluationFunction {
    public static float RESOURCE = 20;
    public static float RESOURCE_IN_WORKER = 10;
    public static float UNIT_BONUS_MULTIPLIER = 40.0f;
    public static float aggressionWeight = 1;

    public ComplexEvaluationFunction(float aggressionWeightFloat){
        super();
        this.aggressionWeight = aggressionWeightFloat;
    }

    public ComplexEvaluationFunction(){
        this(1);
    }

    public float evaluate(int maxplayer, int minplayer, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float s1 = gs.getPlayer(maxplayer).getResources()*RESOURCE;
        float s2 = gs.getPlayer(minplayer).getResources()*RESOURCE;
        List<Unit> maxUnits = new ArrayList<Unit>();
        List<Unit> minUnits = new ArrayList<Unit>();
        Unit maxBase = null;
        Unit minBase = null;
        int maxManhattenDist = pgs.getHeight() + pgs.getWidth();

        for(Unit u:pgs.getUnits()){
            if (u.getPlayer() == maxplayer){
                maxUnits.add(u);
                if (u.getType().name == "Base") {
                    maxBase = u;
                }
            } else if (u.getPlayer() == minplayer){
                minUnits.add(u);
                if (u.getType().name == "Base") {
                    minBase = u;
                }
            }
        }

        for(Unit maxUnit : maxUnits){
            // Times unitvalue by its manhatten distance to enemy base divided by the maxium manhatten distance possible
            if (minBase == null){
                s1 += unitValue(maxUnit);
            } else {
                s1 += unitValue(maxUnit) * maxManhattenDist  / (maxManhattenDist + manhattenToPoint(maxUnit,minBase.getX(),minBase.getY()));
            }
        }

        for(Unit minUnit : minUnits){
            // Times unitvalue by its manhatten distance to enemy base divided by the maxium manhatten distance possible
            if (maxBase == null){
                s2 += unitValue(minUnit);
            } else {
                s2 += unitValue(minUnit) * maxManhattenDist  / (maxManhattenDist + manhattenToPoint(minUnit,maxBase.getX(),maxBase.getY()));
            }
        }

        if (s1 + s2 == 0) return 0.5f;
        // Return a score between -1 and 1, including an adjustment  for aggression
        // If we want the score to favour lowering enemy score as much as then set aggression weighting to 1
        // If we want a less aggressive approach, that favours improving itself over degrading the enemy score, choose a lower aggression weight
        return  (2*s1 / (s1 + (aggressionWeight * s2)))-1;
    }

   public int manhattenToPoint(Unit a, int x, int y){
        return Math.abs(a.getX() - x) + Math.abs(a.getY() - y);
   }

   public float unitValue(Unit u){
        float value = 0;
        value += u.getResources() * RESOURCE_IN_WORKER;
        value += UNIT_BONUS_MULTIPLIER * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints() );
        return value;
   }
    
    public float upperBound(GameState gs) {
        return 1.0f;
    }
}
