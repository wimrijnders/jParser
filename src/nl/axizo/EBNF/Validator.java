/**
 * $Id: EBNFValidator.java 42 2008-11-03 15:41:51Z wri $
 *
 */
package nl.axizo.EBNF;

import nl.axizo.parser.*;
import java.util.Vector;
import java.util.Map;
import java.util.Hashtable;


/**
 * Perform validations on the parse tree, as generated by the
 * EBNF parser.
 *
 * This class is used between the parse step (in which an input
 * file is converted to a parse tree) and the translation step (in
 * which output is generated from the parse tree). 
 *
 * The goal is to check if the input tree conforms to rules which
 * are not enforced by the syntax
 */
public abstract class Validator {

	public abstract void validate( State state ) throws ParseException;
}
