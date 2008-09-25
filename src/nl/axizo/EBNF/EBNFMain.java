/**
 * $Id$
 *
 */
package nl.axizo.EBNF;

import nl.axizo.parser.State;

/**
 * Entry point for generated EBNF class.
 *
 * This file is not included in the final jar-package.
 * It is intended for testing the generated EBNF-class, and
 * will be compiled and called only during testing.
 *
 * Derived from main() in class EBNFInitial.
 */
public class EBNFMain {


	public static void main(String[] argv) 
		throws NoSuchMethodException, IllegalAccessException {
		final String nodesFile = "nodes_test.txt";

		EBNF parser = new EBNF( argv[0] );
		//parser.setTraceLevel( TRACE );
		parser.setFirstTwoLines(true);
		State state = parser.parse();

/*	
		// Do node translations
		EBNFTranslator translator = new EBNFTranslator();
		translator.translate( state );

		// Create output
		EBNFGenerator generator = new EBNFGenerator();
		generator.generate( state );
*/
		// Exit
		parser.saveNodes( state, nodesFile );
		parser.showFinalResult(state);
	}

}
