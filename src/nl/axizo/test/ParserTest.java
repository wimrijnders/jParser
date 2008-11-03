/*
 * $Id$
 */
package nl.axizo.test;

import junit.framework.*;
import nl.axizo.EBNF.digits;
import nl.axizo.parser.*;

public class ParserTest extends TestCase {

	private void out( String str ) {
		System.out.println( str );
	}

	public void testActions() {
		// Test a straightforward pass
		digits parser = new digits("1029\n382134    \t240667", false);
		State state = parser.parse();
		
		//out( state.getCurNode().show() );

		String count = state.getCurNode().get("file").get("Result").get("count").getValue();
		String sum   = state.getCurNode().get("file").get("Result").get("sum").getValue();
		out( "count: " + count );
		out( "sum  : " + sum );

		Assert.assertFalse( state.hasErrors() );
		Assert.assertEquals( new Integer(16),  new Integer( count ) ); 
		Assert.assertEquals( new Integer(58),  new Integer(   sum ) ); 
	}


	public void testWrongChar() {

		// Test a failing pass. Added Exclamation mark should generate
		// an error
		digits parser = new digits("1029\n382134!    \t240667", false);
		State state = parser.parse();
		
		Assert.assertTrue( state.hasErrors() );
		// Check for correct error
		Assert.assertEquals( "end of parsing", state.getErrorMethod() );
		// Check for correct position
		Assert.assertTrue( parser.curLine( state.getErrorPos() + 1 ).startsWith("!") );

		// Following informational only, no effect on test
		parser.showFinalResult( state );
	}


	// TODO: following probably merits its own class.
	//       Make this class later on.
	public static Test suite() {
    	return new TestSuite(ParserTest.class);
	}

}
