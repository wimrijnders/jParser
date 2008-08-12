/**
 * $Id: EBNF.java 10 2008-08-12 10:56:22Z wri $
 *
 * TODO:
 * ====
 *
 * - Clean up and figure out all the throws-clauses
 * - Handle collecting of overrides in strings and charsets properly
 */
package nl.axizo.parser;


class Util {
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
}
