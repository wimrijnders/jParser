/**
 * $Id: EBNFGenerator.java 42 2008-11-03 15:41:51Z wri $
 *
 */
package nl.axizo.EBNF;

import nl.axizo.parser.*;
import java.io.IOException;
import java.util.Vector;
import java.util.Map;
import java.util.Hashtable;

/**
 *  Generates output from a parse tree.
 *
 * The parse tree must have been created with an EBNF parser.
 */
public abstract class Generator {

	/**
 	 * Generate output from the current node tree.
 	 */
	public abstract void generate( State state, String outfile ) throws ParseException;
}

