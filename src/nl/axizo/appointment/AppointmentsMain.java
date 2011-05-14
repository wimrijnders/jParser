package nl.axizo.appointment;

import nl.axizo.EBNF.*;
import nl.axizo.parser.*;

public class AppointmentsMain {

	public static void main(String[] argv) 
		throws NoSuchMethodException, IllegalAccessException {

		String nodesFile = "nodes.txt";
		boolean parseOnly = false;
		String inFile;
		String outfile   = "output.csv";

		if ( argv.length == 0 ) {
			Util.info("No input file specified");
			System.exit(-1); 
		} else if ( argv.length == 1 ) {
			Util.info("No output file specified, using default " + outfile );
		} else if ( argv.length == 2 ) {
			outfile   = argv[1];
		}

		inFile   = argv[0];

		Util.info("Called AppointmensMain" );
		Util.info("File input: " + inFile);

		Appointments parser = new Appointments( inFile );
		//parser.setTraceLevel( Util.TRACE );
		//parser.setFirstTwoLines(true);
		State state = parser.parse();

		// Skip rest of steps if  error occured during parsing stage
		if ( state.hasErrors() ) {
			Util.info( "Errors occured during parsing; skipping translation and generation.");
		} else {
			try {
				Generator gen = new CSVGenerator();
				gen.generate(state, outfile);
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
