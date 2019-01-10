package ai.strategychooser;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.LanchesterEvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.portfolio.PortfolioAI;
import rts.units.UnitTypeTable;


import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static tournaments.RoundRobinTournament.runTournament;

public class validateBot {
    public static void main(String args[]) throws Exception {
        List<Integer> inertias = Arrays.asList( 2, 5, 10, 20);
        List<EvaluationFunction> evalFuncs = Arrays.asList(new SimpleSqrtEvaluationFunction3(), new LanchesterEvaluationFunction(), new ComplexEvaluationFunction());
        List<PathFinding> pathFinders = Arrays.asList(new BFSPathFinding(), new AStarPathFinding());


        int rounds = 2;                                // Number of rounds in the tournament
        int timeBudget = 100;                          // Time budget allowed per action (default 100ms)
        int maxGameLength = 2000;                    // NOT IN USE. Maximum game length (default 2000 ticks) [See List<Integer> lengths]
        boolean fullObservability = true;              // Full or partial observability (default true)
        boolean selfMatches = false;                   // If self-play should be used (default false)
        boolean timeOutCheck = true;                   // If the game should count as a loss if a bot times out (default true)
        boolean preAnalysis = true;                    // If bots are allowed to analyse the game before starting (default true)
        int preAnalysisBudgetFirstTimeInAMap = 1000;   // Time budget for pre-analysis if playing first time on a new map (default 1s)
        int preAnalysisBudgetRestOfTimes = 1000;       // Time budget for pre-analysis for all other cases (default 1s)
        boolean runGC = false;                         // If Java Garbage Collector should be called before each player action (default false)
        int iterationBudget = -1;                      // Iteration budget, set to -1 for infinite (default: -1)
        int playOnlyWithThisAI = 0;                   //  AI index in list of AIs, if one AI should be included in all matches played (default -1)

        // Create list of AIs participating in tournament
        UnitTypeTable utt = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL, UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);

        // Create list of maps for tournament
        List<String> maps = new ArrayList<>();
        maps.add("maps/16x16/basesWorkers16x16.xml");
        maps.add("maps/24x24/basesWorkers24x24H.xml");
        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
        maps.add("maps/NoWhereToRun9x8.xml");

        // Initialize result writing
        String folderForReadWriteFolders = "readwrite";

        //        String traceOutputFolder = "traces";
        String traceOutputFolder = null;  // Ignore traces

        //        Writer out = new BufferedWriter(new FileWriter(new File("results.txt")));  // Print to file
        Writer out = new PrintWriter(System.out);  // Print to console

        //        Writer progress = new BufferedWriter(new FileWriter(new File("progress.txt")));  // Write progress to file
        Writer progress = new PrintWriter(System.out);  // Write progress to console
//        Writer progress = null;  // Ignore progress
        System.out.println("Starting gridsearch");
        /*
        List<Integer> lookaheads = Arrays.asList( 20,30,50,70,100,120,150,200);
        for (int lookahead : lookaheads) {
            System.out.println("Lookahead: " + lookahead);
            List<AI> AIs = new ArrayList<>();
            AIs.add(new QMLeeSaundersHind(lookahead, utt, new AStarPathFinding(), new LanchesterEvaluationFunction(), 10));
            AIs.add(new PortfolioAI(utt));
            runTournament(AIs,playOnlyWithThisAI, maps, rounds, maxGameLength, timeBudget, iterationBudget,
                    preAnalysisBudgetFirstTimeInAMap, preAnalysisBudgetRestOfTimes, fullObservability, selfMatches,
                    timeOutCheck, runGC, preAnalysis, utt, traceOutputFolder, out,
                    progress, folderForReadWriteFolders);

        }
        */
        /*
        for (int inertiaCycles : inertias) {
            for (EvaluationFunction evalFunc : evalFuncs) {
                for (PathFinding pf : pathFinders) {
                    System.out.println("InertiaCycles: " + inertiaCycles);
                    System.out.println("Eval function: " + evalFunc.toString());
                    System.out.println("Pathfinding: " + pf.toString() );

                    List<AI> AIs = new ArrayList<>();
                    AIs.add(new QMLeeSaundersHind(timeBudget, utt, pf, evalFunc, inertiaCycles));
                    AIs.add(new PortfolioAI(utt));

                    runTournament(AIs,playOnlyWithThisAI, maps, rounds, maxGameLength, timeBudget, iterationBudget,
                            preAnalysisBudgetFirstTimeInAMap, preAnalysisBudgetRestOfTimes, fullObservability, selfMatches,
                            timeOutCheck, runGC, preAnalysis, utt, traceOutputFolder, out,
                            progress, folderForReadWriteFolders);
                }

            }
        }
        */
    }



}
