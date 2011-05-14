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
		String inFile;
		boolean outputRuby = false;

		// Handle parameters, if any:
		//
		//	-p file	- parse only, quit after doing parse stage
		//			- parse tree is outputted to given file
		//	file	- input (ebnf) file to parse
		int curarg = 0;
		while ( curarg < argv.length -1 ) {
			if ( "-p".equals(argv[curarg]) ) {
				parseOnly = true;
				nodesFile = argv[curarg + 1];
				curarg += 2;
			} else if ( "-r".equals(argv[curarg]) ) {
				Util.info( "Outputting for ruby.");
				outputRuby = true;
				curarg += 1;
			} else {
				Util.error ("Unknown option '" + argv[curarg] + "'" );
				curarg += 1;
			}
		}

		inFile   = argv[curarg];

		//NOTE: This is the ruby parser!!!!!
		BasicParser parser = new EBNFruby( inFile );
		//parser.setTraceLevel( Util.TRACE );
		parser.setFirstTwoLines(true);
		State state = parser.parse();

		if( parseOnly) {
			Util.info("Doing parse only.");
			parser.saveNodes( state, nodesFile );
			parser.showFinalResult(state);
			return;
		}

		// Skip rest of steps if  error occured during parsing stage
		if ( state.hasErrors() ) {
			Util.info( "Errors occured during parsing; skipping translation and generation.");
		} else {
			try {
				Validator  validator;
				Translator translator;
				Generator  generator;

				if ( outputRuby ) {
					validator  = new EBNFValidatorRuby();
					translator = new EBNFTranslatorRuby();
					generator  = new EBNFGeneratorRuby();
				} else {
					validator  = new EBNFValidator();
					translator = new EBNFTranslator();
					generator  = new EBNFGenerator();
				}	

				// Validate parse tree
				validator.validate( state );

				// Do node translations
				translator.translate( state );
	
				// Create output
				generator.generate( state, null );
	
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
