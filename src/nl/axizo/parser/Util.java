/**
 * $Id$
 *
 * TODO:
 * ====
 *
 * - Clean up and figure out all the throws-clauses
 * - Handle collecting of overrides in strings and charsets properly
 */
package nl.axizo.parser;

import java.io.*;


public class Util {
	static String makeTab( int tabs, String fillChar ) {
		String ret = "";
		for( int i = 0; i < tabs*3; ++i ) {
			ret += fillChar;
		}
		return ret; 
	}

	static String makeTab( int tabs ) {
		return makeTab( tabs, ".");
	}


	/**
 	 * Save contents of a string to file.
 	 *
 	 * @param filename name of file to save to
 	 * @param output string containing content to put in file.
 	 */
	public static void saveFile( String filename, String output) throws IOException {
		FileWriter fw = new FileWriter( filename );
		fw.write(output);
		fw.close();
	}
}
