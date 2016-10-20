package ${package};

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * UnitTest Class, a collection of unitary tests of different methods in the module.
 * 
 * For each set of test create a <code>public void *test()</code> method, then 
 * check the correct status by adding assertions.
 * 
 * @see TestCase
 * @see <a href=http://junit.org/junit4/> JUnit framework</a>
 *
 */
public class UnitTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public UnitTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( UnitTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
