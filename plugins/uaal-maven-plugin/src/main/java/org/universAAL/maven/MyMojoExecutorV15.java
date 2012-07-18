package org.universAAL.maven;

import static org.twdata.maven.mojoexecutor.PlexusConfigurationUtils.toXpp3Dom;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;

/**
 * This class was extracted from mojo-executor-1.5 artifact and adopoted in
 * uaal-maven-plugin for purpose of simultaneouse compliance with maven 2 and 3.
 * 
 * Executes an arbitrary mojo using a fluent interface. This is meant to be
 * executed within the context of a Maven 2 mojo.
 * <p/>
 * Here is an execution that invokes the dependency plugin:
 * 
 * <pre>
 * executeMojo(plugin(groupId(&quot;org.apache.maven.plugins&quot;),
 * 	artifactId(&quot;maven-dependency-plugin&quot;), version(&quot;2.0&quot;)),
 * 	goal(&quot;copy-dependencies&quot;), configuration(element(
 * 		name(&quot;outputDirectory&quot;), &quot;${project.build.directory}/foo&quot;)),
 * 	executionEnvironment(project, session, pluginManager));
 * </pre>
 */
public class MyMojoExecutorV15 {
    private static final String FAKE_EXECUTION_ID = "virtual-execution";

    /**
     * Entry point for executing a mojo
     * 
     * @param plugin
     *            The plugin to execute
     * @param goal
     *            The goal to execute
     * @param configuration
     *            The execution configuration
     * @param env
     *            The execution environment
     * @throws MojoExecutionException
     *             If there are any exceptions locating or executing the mojo
     */
    public static void executeMojo(final Plugin plugin, final String goal,
	    final Xpp3Dom configuration, final ExecutionEnvironment env)
	    throws MojoExecutionException {
	env.executeMojo(plugin, goal, configuration);
    }

    public static void executeMojoImpl(final Plugin plugin, String goal,
	    final Xpp3Dom configuration, final ExecutionEnvironmentM2 env)
	    throws MojoExecutionException {
	Map<String, PluginExecution> executionMap = null;
	try {
	    MavenSession session = env.getMavenSession();

	    List buildPlugins = env.getMavenProject().getBuildPlugins();

	    String executionId = null;
	    if (goal != null && goal.length() > 0 && goal.indexOf('#') > -1) {
		int pos = goal.indexOf('#');
		executionId = goal.substring(pos + 1);
		goal = goal.substring(0, pos);
		System.out.println("Executing goal " + goal
			+ " with execution ID " + executionId);
	    }

	    // You'd think we could just add the configuration to the mojo
	    // execution, but then it merges with the plugin
	    // config dominate over the mojo config, so we are forced to fake
	    // the config as if it was declared as an
	    // execution in the pom so that the merge happens correctly
	    if (buildPlugins != null && executionId == null) {
		for (Object buildPlugin : buildPlugins) {
		    Plugin pomPlugin = (Plugin) buildPlugin;

		    if (plugin.getGroupId().equals(pomPlugin.getGroupId())
			    && plugin.getArtifactId().equals(
				    pomPlugin.getArtifactId())) {
			PluginExecution exec = new PluginExecution();
			exec.setConfiguration(configuration);
			executionMap = getExecutionsAsMap(pomPlugin);
			executionMap.put(FAKE_EXECUTION_ID, exec);
			executionId = FAKE_EXECUTION_ID;
			break;
		    }
		}
	    }

	    PluginDescriptor pluginDescriptor = env
		    .getPluginManager()
		    .verifyPlugin(plugin, env.getMavenProject(),
			    session.getSettings(), session.getLocalRepository());
	    MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo(goal);
	    if (mojoDescriptor == null) {
		throw new MojoExecutionException("Unknown mojo goal: " + goal);
	    }
	    MojoExecution exec = mojoExecution2(mojoDescriptor, executionId,
		    configuration);
	    env.getPluginManager().executeMojo(env.getMavenProject(), exec,
		    env.getMavenSession());
	} catch (Exception e) {
	    throw new MojoExecutionException("Unable to execute mojo", e);
	} finally {
	    if (executionMap != null)
		executionMap.remove(FAKE_EXECUTION_ID);
	}
    }

    /**
     * Entry point for executing a mojo
     * 
     * @param plugin
     *            The plugin to execute
     * @param goal
     *            The goal to execute
     * @param configuration
     *            The execution configuration
     * @param env
     *            The execution environment
     * @throws MojoExecutionException
     *             If there are any exceptions locating or executing the mojo
     */
    public static void executeMojoImpl(final Plugin plugin, String goal,
	    final Xpp3Dom configuration, final ExecutionEnvironmentM3 env)
	    throws MojoExecutionException {
	if (configuration == null) {
	    throw new NullPointerException("configuration may not be null");
	}
	try {
	    String executionId = null;
	    if (goal != null && goal.length() > 0 && goal.indexOf('#') > -1) {
		int pos = goal.indexOf('#');
		executionId = goal.substring(pos + 1);
		goal = goal.substring(0, pos);
	    }

	    MavenSession session = env.getMavenSession();

	    // The code underneath is equal to the following code:
	    //
	    // PluginDescriptor pluginDescriptor
	    // .getBuildPluginManager()
	    // .loadPlugin(
	    // plugin,
	    // env.getMavenProject().getRemoteArtifactRepositories(),
	    // session.getRepositorySession());
	    //
	    // However it cannot be compiled like that because compatibility
	    // issues between Maven 2 and Maven 3
	    Object mavenProject = env.getMavenProject();
	    Method[] methods = mavenProject.getClass().getDeclaredMethods();
	    List remotePluginRepositories = null;
	    for (Method m : methods) {
		if ("getRemotePluginRepositories".equals(m.getName())) {
		    remotePluginRepositories = (List) m.invoke(mavenProject);
		}
	    }
	    Object pluginManager = env.getBuildPluginManager();
	    methods = pluginManager.getClass().getDeclaredMethods();
	    PluginDescriptor pluginDescriptor = null;
	    for (Method m : methods) {
		if ("loadPlugin".equals(m.getName())) {
		    pluginDescriptor = (PluginDescriptor) m.invoke(
			    pluginManager, plugin, remotePluginRepositories,
			    session.getRepositorySession());
		}
	    }

	    MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo(goal);
	    if (mojoDescriptor == null) {
		throw new MojoExecutionException("Could not find goal '" + goal
			+ "' in plugin " + plugin.getGroupId() + ":"
			+ plugin.getArtifactId() + ":" + plugin.getVersion());
	    }
	    MojoExecution exec = mojoExecution3(mojoDescriptor, executionId,
		    configuration);

	    // The code underneath is equal to the following code:
	    //
	    // env.getBuildPluginManager().executeMojo(session, exec);
	    // 
	    // However it cannot be compiled like that because compatibility
	    // issues between Maven 2 and Maven 3
	    for (Method m : methods) {
		if ("executeMojo".equals(m.getName())) {
		    m.invoke(pluginManager, session, exec);
		}
	    }
	} catch (Exception e) {
	    throw new MojoExecutionException("Unable to execute mojo", e);
	}
    }

    @SuppressWarnings( { "unchecked" })
    // Maven 2 API isn't generic
    private static Map<String, PluginExecution> getExecutionsAsMap(
	    final Plugin pomPlugin) {
	return pomPlugin.getExecutionsAsMap();
    }

    private static MojoExecution mojoExecution2(
	    final MojoDescriptor mojoDescriptor, final String executionId,
	    final Xpp3Dom configuration) {
	if (executionId != null) {
	    return new MojoExecution(mojoDescriptor, executionId);
	} else {
	    return new MojoExecution(mojoDescriptor, configuration);
	}
    }

    private static MojoExecution mojoExecution3(
	    final MojoDescriptor mojoDescriptor, final String executionId,
	    Xpp3Dom configuration) {
	if (executionId != null) {
	    return new MojoExecution(mojoDescriptor, executionId);
	} else {
	    configuration = Xpp3DomUtils.mergeXpp3Dom(configuration,
		    toXpp3Dom(mojoDescriptor.getMojoConfiguration()));
	    return new MojoExecution(mojoDescriptor, configuration);
	}
    }

    /**
     * Constructs the {@link ExecutionEnvironment} instance fluently
     * 
     * @param mavenProject
     *            The current Maven project
     * @param mavenSession
     *            The current Maven session
     * @param pluginManager
     *            The Maven plugin manager
     * @return The execution environment
     */
    public static ExecutionEnvironment executionEnvironment(
	    final MavenProject mavenProject, final MavenSession mavenSession,
	    final PluginManager pluginManager) {
	return new ExecutionEnvironmentM2(mavenProject, mavenSession,
		pluginManager);
    }

    /**
     * Constructs the {@link ExecutionEnvironment} instance fluently
     * 
     * @param mavenProject
     *            The current Maven project
     * @param mavenSession
     *            The current Maven session
     * @param pluginManager
     *            The Maven plugin manager
     * @return The execution environment
     */
    public static ExecutionEnvironment executionEnvironment(
	    final MavenProject mavenProject, final MavenSession mavenSession,
	    final BuildPluginManager pluginManager) {
	return new ExecutionEnvironmentM3(mavenProject, mavenSession,
		pluginManager);
    }

    /**
     * Builds the configuration for the goal using Elements
     * 
     * @param elements
     *            A list of elements for the configuration section
     * @return The elements transformed into the Maven-native XML format
     */
    public static Xpp3Dom configuration(final Element... elements) {
	Xpp3Dom dom = new Xpp3Dom("configuration");
	for (Element e : elements) {
	    dom.addChild(e.toDom());
	}
	return dom;
    }

    /**
     * Defines the plugin without its version
     * 
     * @param groupId
     *            The group id
     * @param artifactId
     *            The artifact id
     * @return The plugin instance
     */
    public static Plugin plugin(final String groupId, final String artifactId) {
	return plugin(groupId, artifactId, null);
    }

    /**
     * Defines a plugin
     * 
     * @param groupId
     *            The group id
     * @param artifactId
     *            The artifact id
     * @param version
     *            The plugin version
     * @return The plugin instance
     */
    public static Plugin plugin(final String groupId, final String artifactId,
	    final String version) {
	Plugin plugin = new Plugin();
	plugin.setArtifactId(artifactId);
	plugin.setGroupId(groupId);
	plugin.setVersion(version);
	return plugin;
    }

    /**
     * Wraps the group id string in a more readable format
     * 
     * @param groupId
     *            The value
     * @return The value
     */
    public static String groupId(final String groupId) {
	return groupId;
    }

    /**
     * Wraps the artifact id string in a more readable format
     * 
     * @param artifactId
     *            The value
     * @return The value
     */
    public static String artifactId(final String artifactId) {
	return artifactId;
    }

    /**
     * Wraps the version string in a more readable format
     * 
     * @param version
     *            The value
     * @return The value
     */
    public static String version(final String version) {
	return version;
    }

    /**
     * Wraps the goal string in a more readable format
     * 
     * @param goal
     *            The value
     * @return The value
     */
    public static String goal(final String goal) {
	return goal;
    }

    /**
     * Wraps the element name string in a more readable format
     * 
     * @param name
     *            The value
     * @return The value
     */
    public static String name(final String name) {
	return name;
    }

    /**
     * Constructs the element with a textual body
     * 
     * @param name
     *            The element name
     * @param value
     *            The element text value
     * @return The element object
     */
    public static Element element(final String name, final String value) {
	return new Element(name, value);
    }

    /**
     * Constructs the element containing child elements
     * 
     * @param name
     *            The element name
     * @param elements
     *            The child elements
     * @return The Element object
     */
    public static Element element(final String name, final Element... elements) {
	return new Element(name, elements);
    }

    /**
     * Element wrapper class for configuration elements
     */
    public static class Element {
	private final Element[] children;
	private final String name;
	private final String text;

	public Element(final String name, final Element... children) {
	    this(name, null, children);
	}

	public Element(final String name, final String text,
		final Element... children) {
	    this.name = name;
	    this.text = text;
	    this.children = children;
	}

	public Xpp3Dom toDom() {
	    Xpp3Dom dom = new Xpp3Dom(name);
	    if (text != null) {
		dom.setValue(text);
	    }
	    for (Element e : children) {
		dom.addChild(e.toDom());
	    }
	    return dom;
	}
    }

    /**
     * Collects Maven execution information
     */
    public static abstract class ExecutionEnvironment {
	private final MavenProject mavenProject;
	private final MavenSession mavenSession;

	public ExecutionEnvironment(final MavenProject mavenProject,
		final MavenSession mavenSession) {
	    this.mavenProject = mavenProject;
	    this.mavenSession = mavenSession;
	}

	public MavenProject getMavenProject() {
	    return mavenProject;
	}

	public MavenSession getMavenSession() {
	    return mavenSession;
	}

	public abstract void executeMojo(Plugin plugin, String goal,
		Xpp3Dom configuration) throws MojoExecutionException;
    }

    public static class ExecutionEnvironmentM2 extends ExecutionEnvironment {
	private final PluginManager pluginManager;

	public ExecutionEnvironmentM2(final MavenProject mavenProject,
		final MavenSession mavenSession,
		final PluginManager pluginManager) {
	    super(mavenProject, mavenSession);
	    this.pluginManager = pluginManager;
	}

	public PluginManager getPluginManager() {
	    return pluginManager;
	}

	@Override
	public void executeMojo(final Plugin plugin, final String goal,
		final Xpp3Dom configuration) throws MojoExecutionException {
	    MyMojoExecutorV15
		    .executeMojoImpl(plugin, goal, configuration, this);
	}
    }

    public static class ExecutionEnvironmentM3 extends ExecutionEnvironment {
	private final Object buildPluginManager;

	public ExecutionEnvironmentM3(final MavenProject mavenProject,
		final MavenSession mavenSession, final Object buildPluginManager) {
	    super(mavenProject, mavenSession);
	    this.buildPluginManager = buildPluginManager;
	}

	public Object getBuildPluginManager() {
	    return buildPluginManager;
	}

	@Override
	public void executeMojo(final Plugin plugin, final String goal,
		final Xpp3Dom configuration) throws MojoExecutionException {
	    MyMojoExecutorV15
		    .executeMojoImpl(plugin, goal, configuration, this);
	}
    }
}
