package ${package};

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.universAAL.itests.IntegrationTest;
import org.universAAL.middleware.container.ModuleContext;

/**
 * Here developer's of this artifact should code their integration tests.
 * For each set of test create a <code>public void *test()</code> method, then
 * check the correct status by adding assertions.
 * Consider each method as an {@link BundleActivator#start(org.osgi.framework.BundleContext)},
 * where the {@link BundleContext} is accessible through {@link IntegrationTest#bundleContext}.
 *
 * These Integration tests will start a full univeresAAL stack as well as any runtime dependencies
 * stated in the <i>uAAL-Runtime</i> maven profile defined in your POM as:
 *
 * <pre>{@code
 * <project>
 * 		...
 * 	  <profiles>
 *	    <profile>
 *	      <id>uAAL-Runtime</id>
 *	      <dependencies>
 *	        <dependency>
 *	          ...
 *	        </dependency>
 *	      </dependencies>
 *	    </profile>
 *	  </profiles>
 *	</project>
 *}</pre>
 *
 * Accessing {@link ModuleContext} maybe done with OSGi container operation: <br>
 * <code> ModuleContext mc = uAALBundleContainer.THE_CONTAINER.registerModule(new Object[] { bundleContext }); </code>
 */
public class ArtifactIT extends IntegrationTest {

	public ArtifactIT() {
		super();
		setIgnoreLastBudnle(true);
	}

	public void testComposite() {
	logAllBundles();
    }
}
