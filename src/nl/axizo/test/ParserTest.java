/*
 * $Id$
 */
package nl.axizo.test;

import junit.framework.*;
import nl.axizo.EBNF.digits;
import nl.axizo.parser.*;

public class ParserTest extends TestCase {

	public void testActions() {
		digits parser = new digits("1029382134", false);

		State state = parser.parse();
		
		Assert.assertFalse( state.hasErrors() );
	}


	// TODO: following probably merits its own class.
	//       Make this class later on.
	public static Test suite() {
    	return new TestSuite(ParserTest.class);
	}

}
