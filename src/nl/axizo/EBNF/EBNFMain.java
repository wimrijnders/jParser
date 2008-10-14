/**
 * $Id$
 *
 */
package nl.axizo.EBNF;

import nl.axizo.parser.*;

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
//		parser.setTraceLevel( Util.TRACE );
		parser.setFirstTwoLines(true);
		State state = parser.parse();

//		parser.saveNodes( state, nodesFile );
//		parser.showFinalResult(state);

		// Quit if parse errors occured
		if ( state.hasErrors() ) {
			System.exit(-1);
			Util.info( "Errors occured during parsing; skipping translation and generation.");
		} else {
			try {	
				// Validate parse tree
				EBNFValidator validator = new EBNFValidator();
				validator.validate( state );
		
				//TODO: Translation and generation from this point
			} catch( ParseException e ) {
				Util.error("Error during validation/translation/generation: " + e.getMessage() );

				// Nodes output may be handy for debugging
				parser.saveNodes( state, nodesFile );
				System.exit(1);
			}
		}		


		parser.saveNodes( state, nodesFile );
		parser.showFinalResult(state);
		if ( state.hasErrors() ) {
			System.exit(1);
		}
	}

}
