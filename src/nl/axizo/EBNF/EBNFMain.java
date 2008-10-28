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
		String nodesFile = "nodes.txt";
		boolean parseOnly = false;
		String nodeFile = "nodes.txt";
		String inFile;

		// Handle parameters, if any:
		//
		//	-p file	- parse only, quit after doing parse stage
		//			- parse tree is outputted to given file
		//	file	- input (ebnf) file to parse
		if ( "-p".equals(argv[0]) ) {
			parseOnly = true;
			nodesFile = argv[1];
			inFile   = argv[2];
		} else {
			inFile   = argv[0];
		}

		EBNF parser = new EBNF( inFile );
//		parser.setTraceLevel( Util.TRACE );
		parser.setFirstTwoLines(true);
		State state = parser.parse();

		if( parseOnly) {
			Util.info("Doing parse only.");
			parser.saveNodes( state, nodesFile );
			parser.showFinalResult(state);
			return;
		}

		// Quit if parse errors occured
		if ( state.hasErrors() ) {
			Util.info( "Errors occured during parsing; skipping translation and generation.");
		} else {
			try {	
				// Validate parse tree
				EBNFValidator validator = new EBNFValidator();
				validator.validate( state );
		
				// Do node translations
				EBNFTranslator translator = new EBNFTranslator();
				translator.translate( state );
	
				// Create output
				EBNFGenerator generator = new EBNFGenerator();
				generator.generate( state );
	
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
