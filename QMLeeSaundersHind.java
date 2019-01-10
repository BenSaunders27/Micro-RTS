package ai.strategychooser;

import ai.abstraction.*;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction;
import rts.*;
import rts.units.Unit;
import rts.units.UnitTypeTable;

import java.util.*;


/**
 *
 * @author Matt Lee, Johnny Hind, Ben Saunders
 */

public class QMLeeSaundersHind extends AbstractionLayerAI {

    //
    private int MAXSIMULATIONTIME;
    private int INERTIACYCLES;
    private int GAMECOUNT;
    UnitTypeTable UTT;

    // Initialise all the AI strategies used in the class
    private AI LightRush; private AI WorkerRush; private AI HeavyRush; private AI RangedRush; private AI MattRush;
    AI RandomBiasedAI;
    // Initialise the list of AIStrategies played and enemyStrategies simulated against
    private List<AI> AIStrategies = new ArrayList<>();
    private List<AI> enemyStrategies = new ArrayList<>();
    // Initialise the lastStrategy AI variable, which tracks the last used AI strategy for inertia
    private AI lastStrategy = null;
    // Initialise the predictedEnemyStrategy integer, which tracks the enemy strategy that has been predicted for that simulation inertia run
    private int predictedEnemyStrategy;


    //TODO - Check why we don't need utt or pf
    //private UnitTypeTable utt = new UnitTypeTable();
    //Initialise the PathFinding algorithm pf that is used by each
    //private PathFinding pf = new BFSPathFinding();

    // Initialise the evaluationFunction which provides an evaluation on a given game state, to be used when scoring a simulating game state
    private EvaluationFunction evaluateFunction = new SimpleSqrtEvaluationFunction();
    //private ComplexEvaluationFunction evaluateFunction = new ComplexEvaluationFunction();

    // Initialise the simulationGameStates List, which tracks all the simulated game states when running an inertia game tick
    private List<GameState> simulationGameStates = new ArrayList<>();
    // Initialise the simulationCounts Integer List, which tracks the depth of search of each simulated game state
    private Integer[] simulationCounts;



    // Strategy Chooser Constructor
    public QMLeeSaundersHind(UnitTypeTable utt , PathFinding pf){
        this(120, utt,  pf, 10);
    }

    public QMLeeSaundersHind(int lookahead, UnitTypeTable utt , PathFinding pf, EvaluationFunction evalFunc, int inertiaCycles){
        this(lookahead, utt,  pf, inertiaCycles);
        this.setEvaluationFunction(evalFunc);
    }

    public QMLeeSaundersHind(UnitTypeTable utt){
        this(120,utt,new AStarPathFinding(),10);
    }

    public QMLeeSaundersHind(int lookahead, UnitTypeTable utt, PathFinding a_pf, int inertiaCycles) {
        super(a_pf);
        MAXSIMULATIONTIME = lookahead;
        HeavyRush = new HeavyRush2(utt,a_pf);
        WorkerRush = new WorkerRush2(utt,a_pf);
        RangedRush = new RangedRush2(utt,a_pf);
        LightRush = new LightRush2(utt,a_pf);
        INERTIACYCLES = inertiaCycles;
        GAMECOUNT = 0;

        UTT = utt;


        // Initialise the AIStrategies to all the AI strategies we wish to evaluate and simulate with
        AIStrategies.add(LightRush);
        AIStrategies.add(HeavyRush);
        AIStrategies.add(WorkerRush);
        AIStrategies.add(RangedRush);

        //AIStrategies.add(LightRush);


        // Initialise the enemyStrategies to all the enemy strategies we wish to simulate against
        enemyStrategies.add(WorkerRush);
        enemyStrategies.add(LightRush);
        enemyStrategies.add(HeavyRush);
        enemyStrategies.add(RangedRush);


        // Initialise simulationCounts to an Integer List the size of the AIStrategies
        simulationCounts = new Integer[AIStrategies.size()];
    }

    // Rest method
    public void reset() {
        //evaluateFunction = new SimpleSqrtEvaluationFunction();
        //evaluateFunction = new ComplexEvaluationFunction();
    }

    // Clone method, if the QMLeeSaundersHind class is required to be cloned
    public AI clone() {
        return new QMLeeSaundersHind(MAXSIMULATIONTIME,UTT, pf, INERTIACYCLES);
    }


    /*
        getAction is the main method of the AI. It is called at each game cycle with the most up to date game state and
        returns which actions the AI wants to execute in this cycle.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        This method returns the actions to be sent to each of the units in the GameState controlled by the player,
        packaged as a PlayerAction.
         */
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        // Called when actions are available to be played in the current game state
        if (gs.canExecuteAnyAction(player)) {

            // Initialise the topStrategy variable, which tracks the last used strategy
            // Finds the best strategy to implement in this game cycle, and performs getAction() to provide action
            AI topStrategy;

            // If GAMECOUNT is at zero, this is the first game cycle
            // Find the best strategy to play on this given map
            if ( GAMECOUNT == 0 ){

                // Call firstRunStrategyAnalysis, which returns the appropriate topStrategy for the first game cycle on this map
                topStrategy = firstRunStrategyAnalysis(player, gs);
                // Logging to keep track of the first game cycle and the strategy used
                System.out.println("First game cycle using: " + topStrategy );

                // Set all the simulationCounts to 0, to start depth counting from zero
                for (int i=0 ; i < simulationCounts.length ; i++ ){
                    simulationCounts[i] = 0;
                }

            // If GAMECOUNT is a multiple of INERTIACYCLES, this is the end of an inertia run
            // Evaluate all simulated game states and choose the top strategy to play with
            } else if ( GAMECOUNT % INERTIACYCLES == 0 ){

                // Logging to say when the Simulation cycle has started
                System.out.println( "\n" + "Simulation --- " );

                // Find the top strategies by simulating all the strategies in 'AIStrategies' list
                // Calls findTopStrategy method, which evaluates all simulated game states and returns the best strategy
                topStrategy = findTopStrategy(player, AIStrategies);

                // Logging for each simulated game state, to show the strategies simulated and the search depth of each
                for (int i=0 ; i<simulationCounts.length ; i++){
                    System.out.println(AIStrategies.get(i) + " v " + enemyStrategies.get(predictedEnemyStrategy) + ": Search depth of " + simulationCounts[i]);
                }

                // Logging to show the chosen strategy to implement
                System.out.println("Chosen strategy: " + topStrategy);

            // If GAMECOUNT is not a multiple of INERTIACYCLES
            // Then in the middle of an inertia run
            } else {
                // Sustain the last played strategy to be the chosen strategy to play
                topStrategy = lastStrategy;

                // Logging the inertia strategy and each game cycle
                System.out.println( "Inertia strategy game cycle " + (GAMECOUNT % INERTIACYCLES) + " using " + topStrategy.toString() );

                // If this is the first cycle of a inertia run
                if (GAMECOUNT % INERTIACYCLES == 1 ){

                    // Initialise the simulationGameStates back to empty
                    simulationGameStates = new ArrayList<>();

                    // Set all the simulationCounts to 0, to start depth counting from zero
                    for (int i=0 ; i < simulationCounts.length ; i++ ){
                        simulationCounts[i] = 0;
                    }

                    // Start all the simulations within simulationGameStates using startGameStateSimulation method
                    startGameStateSimulation(player, gs, AIStrategies, enemyStrategies);
                } else {
                    // Middle of an inertia run

                    // Continue all the simulations within simulationGameStates using continueGameStateSimulation method
                    continueGameStateSimulation(player, AIStrategies, enemyStrategies);
                }
            }

            // Get an action from the chosen best strategy to implement in this game cycle
            PlayerAction action  = topStrategy.getAction(player,gs);
            // Revert the lastStrategy variable to this chosen strategy, to be used next inertia run
            lastStrategy = topStrategy;

            // Increase the GAMECOUNT variable for each game cycle
            GAMECOUNT ++;

            // Return the given action as a PlayerAction
            return action;

        } else {
            // If no actions can be played, continue with the current actions as a PlayerAction
            return new PlayerAction();
        }
    }


    /*
        firstRunStrategyAnalysis is the method which is called for the first ever cycle of the game, which chooses the
        first strategy to be implemented based on the map size.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        This method returns the top strategy to be used in the first game cycle by the bot, packaged as an AI class.
         */
    public AI firstRunStrategyAnalysis(int player, GameState gs) {

        // Initialise the topStrategy variable, which is the best strategy for this map, returned as an AI class
        AI topStrategy;
        // Create a PhysicalGameState variable from the GameState parameter
        PhysicalGameState pgs = gs.getPhysicalGameState();
        // Determine the height of the map
        int mapHeight = pgs.getHeight();
        // Initialise the count of the number of bases on the map
        int nbases = 0;

        // Calculate how many bases to use to decide the map size
        // Loop through each player's unit and check if base
        for (Unit u : pgs.getUnits()) {
            if (u.getType().name.equals("Base")
                    && u.getPlayer() == player) {
                // Add to the count if the unit is a base
                nbases++;
            }
        }

        // If the map height is 8, it is the NoWhereToRun9x8 provided map
        if (mapHeight == 8){
            System.out.println("Map = maps/NoWhereToRun9x8.xml");
            topStrategy = RangedRush;

        // If the map height is 16 and has only 1 base, it is the basesWorkers16x16 provided map
        } else if (mapHeight == 16 && nbases == 1) {
            System.out.println("Map = maps/16x16/basesWorkers16x16.xml");
            // Provide the best strategy for this map
            topStrategy = WorkerRush;

        // If the map height is 16 and has 2 bases, it is the TwoBasesBarracks16x16 provided map
        } else if   (mapHeight == 16 && nbases == 2) {
            System.out.println("Map = maps/16x16/TwoBasesBarracks16x16.xml");
            // Provide the best strategy for this map
            topStrategy = LightRush;

        // If the map height is 24, it is the basesWorkers24x24H provided map
        } else if (mapHeight == 24){
            System.out.println("Map = maps/24x24/basesWorkers24x24H.xml");
            // Provide the best strategy for this map
            topStrategy = HeavyRush;

        // Else, it is the hidden map
        } else {
            // Provide the most effective early strategy for all other maps
            topStrategy = WorkerRush;
        }

        // Returns the top strategy to be used in the first game cycle by the bot
        return topStrategy;
    }

    /*
        findTopStrategy is the method which is called when the inertia cycle has ended, and evaluates all the
        simulated game states to decide which strategy should be implemented for the next inertia cycle.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - AIStrategies: List of AI strategies that are being evaluated
        This method returns the top strategy to be used in the next inertia cycle, packaged as an AI class.
         */
    public AI findTopStrategy(int player, List<AI> AIStrategies) {

        // Initialise the topStrategy variable, which is the best strategy for this map, returned as an AI class
        AI topStrategy = null;
        // Initialise the high-score variable as negative infinity
        float highscore = Integer.MIN_VALUE;

        //Loop through each simulationGameStates, evaluating and greedily choosing the top strategy
        for (int i = 0;i < AIStrategies.size();i++){
            // Find the relevant AIStrategy
            AI AIStrategy = AIStrategies.get(i);
            // Find the simulated GameState relating to this strategy
            GameState sGS = simulationGameStates.get(i);

            // Use the provided evaluateFunction to evaluate this simulated GameState
            float score = evaluateFunction.evaluate(player,1-player, sGS);

            // Logging to show each strategy evaluation
            System.out.println("Finished considering playing as: " + AIStrategy + " : " + score);

            // Find the highest scoring strategy
            if (score > highscore || i == 0){
                highscore = score;
                // Set this highest scoring strategy as AIStrategy
                topStrategy = AIStrategy;
            }
        }

        // Logging to show the chosen highest scoring strategy
        System.out.println("Using: " + topStrategy + " with score of: " + highscore + "\n");
        // Return this highest scoring strategy
        return topStrategy;
    }

    /*
        startGameStateSimulation is the method which is called at the start of each inertia cycle, which starts the
        simulation of each Simulated GameState to be evaluated in later cycles.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        - AIStrategies: List of AI strategies that are being evaluated for the bot to implement
        - enemyStrategies: List of AI strategies that are being considered for the enemy to be playing
        This method returns void.
         */
    public void startGameStateSimulation(int player, GameState gs, List<AI> AIStrategies, List<AI> enemyStrategies) throws Exception{

        // Calculate the time allowed from the provided MAXSIMULATIONTIME and the required amount of simulations
        // Presuming 10ms for non-simulation computation
        int timeAllowed = (MAXSIMULATIONTIME);

        // use the predictEnemyStrategy method to provide the relevant enemy strategy index
        int enemyIndex = predictEnemyStrategy(player,gs);

        // Find the relevant enemyStrategy for these simulations
        AI enemyStrategy = enemyStrategies.get(enemyIndex);

        // Loop through each AIStrategy and start a GameState Simulation for each
        for (int i = 0;i < AIStrategies.size();i++){
            // Find the relevant AIStrategy for this simulation
            AI aiStrategy = AIStrategies.get(i);

            // Use the simulate method to start the simulation with the AI and enemy strategies
            GameState tGS = simulate(player, gs.clone(),gs.getTime() + timeAllowed, aiStrategy, enemyStrategy,i);
            // Add each simulated GameState to the global simulationGameStates
            simulationGameStates.add(tGS);
        }
    }

    /*
        continueGameStateSimulation is the method which is called for every inertia cycle apart from the first and last,
        which continues the simulation of the Simulated GameStates to be evaluated in later cycles
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        - AIStrategies: List of AI strategies that are being evaluated for the bot to implement
        - enemyStrategies: List of AI strategies that are being considered for the enemy to be playing
        This method returns void.
         */
    public void continueGameStateSimulation(int player, List<AI> AIStrategies, List<AI> enemyStrategies ) throws Exception {

        // Calculate the time allowed from the provided MAXSIMULATIONTIME and the required amount of simulations
        // Presuming 10ms for non-simulation computation
        int timeAllowed = (MAXSIMULATIONTIME);

        // Create an empty ArrayList for temporary storage of the simulated GameStates
        List<GameState> newSimulationGameStates = new ArrayList<>();

        // Find the predicted enemyStrategy for this inertia cycle
        AI enemyStrategy = enemyStrategies.get(predictedEnemyStrategy);

        // Loop through each AIStrategy and continue the GameState Simulation for each
        for (int i = 0; i < AIStrategies.size();i++){
            // Get the part simulated GameState in a temporaryGameState variable, tGS
            GameState tGS = simulationGameStates.get(i);
            // Get the relevant AIStrategy
            AI AIStrategy = AIStrategies.get(i);

            // Use the simulate method to continue the simulations with the AI and enemy strategies
            newSimulationGameStates.add(simulate(player, tGS,tGS.getTime() + timeAllowed, AIStrategy, enemyStrategy,i));

        }

        // Replace the global simulated GameStates with those just simulated
        simulationGameStates = newSimulationGameStates;
    }


    /*
        predictEnemyStrategy is the method which is evaluates the enemy units and decides on the likeliest strategy
        currently employed by the enemy.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        This method returns the enemyIndex, the index for the enemy strategy that is most likely, packaged as an Integer.
         */
    public Integer predictEnemyStrategy(int player, GameState gs){

        // Create a PhysicalGameState variable from the GameState parameter
        PhysicalGameState pgs = gs.getPhysicalGameState();

        // Calculate how many units types the enemy has, to determine the enemyStrategies votes
        // Initialise each unit count
        int nEnemyWorkers = 0; int nEnemyLight = 0; int nEnemyHeavy = 0; int nEnemyRanged = 0;

        // Loop through each enemy unit
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() != player) {
                // Add to the relevant count for each enemy unit of each type
                switch (u.getType().name){
                    case "Worker": nEnemyWorkers ++;
                    break;
                    case "Light": nEnemyLight++;
                    break;
                    case "Heavy": nEnemyHeavy++;
                    break;
                    case "Ranged": nEnemyRanged++;
                    break;
                }
            }
        }
        //System.out.println("workers: "+nEnemyWorkers + " light: "+nEnemyLight + " heavy: "+nEnemyHeavy + " ranged: "+nEnemyRanged );


        // Initialise the votes List, tracking the score for each enemy strategy, depending on the enemy units
        // votes[0] is Worker, votes[1] is Light, votes[2] is Heavy, votes[3] is Ranged
        Integer[] votes = new Integer[4];

        // Add the score to the relevant index
        votes[0] = 2 + (nEnemyWorkers*4);
        votes[1] = 1 + (nEnemyLight*5);
        votes[2] = (nEnemyHeavy*5) ;
        votes[3] = (nEnemyRanged*5);

        // Find the highest value within votes, which becomes the enemy strategy index
        int enemyIndex = 0;
        for (int i = 0; i < votes.length; i++) {
            // Add this index if it's higher than the current index
            enemyIndex = votes[i] > votes[enemyIndex] ? i : enemyIndex;
        }

        //System.out.println("votes0: "+votes[0] + " votes1: "+votes[1] + " votes2: "+votes[2] + " votes3: "+votes[3] );

        // Set the global predictedEnemyStrategy as this enemy index
        predictedEnemyStrategy = enemyIndex;

        // Return the index for the enemy strategy that is most likely
        return enemyIndex;

    }

    /*
        simulate is the method which is used as a Forward Model to simulate a GameState by issuing actions based on the
        evaluating strategy and the predicted enemy strategy.
        The input parameters are:
        - player: the player that the AI controls (0 or 1)
        - gs: the current game state
        - time: the full time allows for the simulation
        - ai1: the simulated strategy being implemented by the player, for evaluation
        - ai2: the predicted enemy strategy to aid the simulation
        - simulationIndex: index of the correct Simulated GameState to move forward
        This method returns the Simulated GameState, packaged as a GameState.
         */
    public GameState simulate(int player, GameState gs, int time, AI ai1, AI ai2, int simulationIndex) throws Exception {

        // Initialise a variable to track the end of the game
        boolean gameover = false;

        // Clone the provided gameState, to simulate efficiently
        GameState gs2 = gs.clone();

        // Count the number of player action issues for this simulation
        int count = 0;

        // Run the simulation of the bot strategy against a provided enemy strategy until gameover or provided time has elapsed
        do{
            if (gs2.isComplete()) {
                // Set gameover if the GameState is complete
                gameover = gs2.cycle();
            } else {

                // Issue simulated actions for simulated bot strategy
                gs2.issue(ai1.getAction(player, gs2));
                // Issue simulated actions for simulated enemy strategy
                gs2.issue(ai2.getAction(1 - player, gs2));

                // Increase the simulations count
                count ++;

            }
        } while(!gameover && gs2.getTime() < time);

        // Add the total simulation count to the global simulationCounts
        simulationCounts[simulationIndex] += count;

        //System.out.println("Simulate count for " + simulationIndex + ": " + strategyCounts[simulationIndex]);

        // Return the simulated gamestate
        return gs2;
    }

    public void setInertiaCycles(int INERTIACYCLES) {
        this.INERTIACYCLES = INERTIACYCLES;
    }

    public void setEvaluationFunction(EvaluationFunction evaluateFunction) {
        this.evaluateFunction = evaluateFunction;
    }

    public String toString(){
        return getClass().getSimpleName() + "(" + pf +"," +  evaluateFunction + "," + INERTIACYCLES + ")";
    }

    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        //Inertia Cycles
        parameters.add(new ParameterSpecification("InertiaCycles",int.class,INERTIACYCLES));
        //Time Budget
        parameters.add(new ParameterSpecification("TimeBudget",int.class,MAXSIMULATIONTIME));
        //Evaluation Function
        parameters.add(new ParameterSpecification("EvaluationFunction",EvaluationFunction.class,evaluateFunction));

        return parameters;
    }

}