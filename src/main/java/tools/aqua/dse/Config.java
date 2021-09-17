package tools.aqua.dse;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.solvers.SolvingService;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3SolverProvider;
import org.apache.commons.cli.CommandLine;
import tools.aqua.dse.bounds.BoundedSolverProvider;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class Config {

    public enum ExplorationStrategy  {BFS, DFS, IN_ORDER};

    public static final int TERMINATE_WHEN_COMPLETE = 0;
    public static final int TERMINATE_ON_ASSERTION_VIOLATION = 1;
    public static final int TERMINATE_ON_ERROR = 2;
    public static final int TERMINATE_ON_BUG = 4;

    private ConstraintSolver solver;

    private ExplorationStrategy strategy = ExplorationStrategy.DFS;

    private String executorCmd;

    private String targetClasspath;

    private String targetClass;

    private boolean b64encodeExecutorValue = false;

    // TODO: make this configurable
    private int termination = TERMINATE_ON_ASSERTION_VIOLATION;

    private final Properties properties;

    private Config(Properties properties) {
        this.properties = properties;
    }

    /**
     * should dse explore open nodes
     * @return
     */
    public boolean getExploreMode() {
        return true;
    }

    /**
     * values to replay before exploring
     *
     * @return
     */
    public Iterator<Valuation> getReplayValues() {
        return null;
    }

    /**
     * use incremental solving
     *
     * @return
     */
    public boolean isIncremental() {
        return false;
    }

    /**
     * constraint solver context
     *
     * @return
     */
    public SolverContext getSolverContext() {
        return this.solver.createContext();
    }

    /**
     * max depth of exploration exceeded at depth
     *
     * @param depth
     * @return
     */
    public boolean maxDepthExceeded(int depth) {
        return false;
    }

    /**
     * exploration strategy
     *
     * @return
     */
    public ExplorationStrategy getStrategy() {
        return strategy;
    }

    public String getExecutorCmd() {
        return executorCmd;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetClasspath() {
        return targetClasspath;
    }

    public boolean isB64encodeExecutorValue() {
        return b64encodeExecutorValue;
    }

    public int getTermination() {
        return termination;
    }

    private void parseProperties(Properties props) {
        if (props.containsKey("target.classpath")) {
            this.targetClasspath = props.getProperty("target.classpath");
        }
        if (props.containsKey("target.class")) {
            this.targetClass = props.getProperty("target.class");
        }
        if (props.containsKey("dse.executor")) {
            this.executorCmd = props.getProperty("dse.executor");
        }
        if (props.containsKey("dse.b64encode")) {
            this.b64encodeExecutorValue = Boolean.parseBoolean( props.getProperty("dse.b64encode") );
        }

        if (props.containsKey("dse.bounds")
                && Boolean.parseBoolean( props.getProperty("dse.bounds"))) {
            BoundedSolverProvider bp = new BoundedSolverProvider();
            this.solver = bp.createSolver(props);
        }
        else {
            String solverName = props.getProperty("dse.dp");
            this.solver = ConstraintSolverFactory.createSolver(solverName, props);
        }
    }

    public static Config fromProperties(Properties props) {
        Config config = new Config(props);
        config.parseProperties(props);
        return config;
    }

    public static Config fromCommandLine(CommandLine cli) {
        Properties props = new Properties();
        if (cli.hasOption("f")) {
            String filename = cli.getOptionValue("f");
            try (FileInputStream fs = new FileInputStream(filename)) {
                props.load(fs);
            } catch (IOException e) {
                System.err.println("Could not read properties file " + filename);
               throw new RuntimeException();
            }
        }

        if (cli.hasOption("D")) {
            Properties propArgs = cli.getOptionProperties("D");
            for (Map.Entry<Object,Object> entry : propArgs.entrySet()) {
                props.setProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return Config.fromProperties(props);
    }

}
