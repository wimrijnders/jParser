/**
 * $Id$
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
public class EBNFValidator {

	private Vector errors = new Vector();


	private void addError(String msg) {
		errors.add( msg );
	}


	private boolean hasErrors() {
		return (errors.size() > 0);
	}


	private String allErrors() {
		String out = "";

		for( int i = 0; i < errors.size(); ++ i ) {
			out += "\t" + errors.get( i ) + "\n";
		}

		return out;
	}


	public void validate( State state ) throws ParseException {
		Util.info("Validating...");

		Node root = state.getCurNode();

		Vector actions = collectActionLabels( root);
		Map    rules   = collectRuleLabels( root);

		// Rightlabel must be defined as rules
		checkRightLabels(root, rules);

		//All actions must have a corresponding rule
		for( int i = 0;  i < actions.size(); ++i ) {
			String label = (String) actions.get(i);

			if ( rules.get( label ) == null ) {
				addError( "Action '" + label + "' does not have a corresponding rule.");
			}
		}

		
		// We're done. Report errors if any
		if ( hasErrors() ) {
			throw new ParseException( "Errors found during validation:\n" + allErrors() ); 
		}
	}


	private Vector collectActionLabels(Node root) {
		Vector ret = new Vector();

		Vector res =  root.findNodes( "action" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String actionName = n.get("label").getValue();
			ret.add( actionName);
		}

		return ret;
	}


	private Map collectRuleLabels(Node root) {
		Map ret = new Hashtable();

		Vector res =  root.findNodes( "rule" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String ruleName = n.get("label").getValue();

			// rules should be present exacly once
			if ( ret.containsKey( ruleName ) ) {
				addError( "Rule " + ruleName + " present more than once.");
			} else {
				// only key presence is important, value is something not null.
				ret.put( ruleName, "1");
			}
		}

		return ret;
	}


	/**
 	 * Check for existence of rightlabels.
 	 *
 	 * The definition of rightlabels is the rule names
 	 * used in the right hand side of rules. These should
 	 * be names of existing rules.
 	 */
	private void checkRightLabels(Node root, Map rules) throws ParseException {
		Util.info("Validating rightLabels.");

		Vector rightLabels = new Vector();

		Vector res =  root.findNodes( "statement" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			// Filter out the right-labels from the statements
			if( !n.get("label").isNull() ) {
				rightLabels.add( n );
			}
		}

		// Scan all rightlabels for existence of corresponding rules.
		for( int j = 0;  j < rightLabels.size(); ++j ) {
			Node n = (Node) rightLabels.get(j);
			String label = n.get("label").getValue();

			if ( !rules.containsKey( label ) ) {
				// rightlabel not defined as rule; flag error.
				// As extra info, supply the rule name under which
				// this rightlabel was used.
				addError("Righlabel '" + label + "' "
						+ "in rule '" + getParentRuleName( n ) + "' "
						+ "is not defined as a rule.");
			}
		}
	}


	/**
 	 * Retrieve the rule name under which passed statement Node is defined.
 	 *
 	 * @param n statement node containing rightlabel.
 	 */ 
	private String getParentRuleName(Node n ) throws ParseException {
		String rightlabel = n.get("label").getValue();
		String out = null;

		while( out == null ) {
			n = n.getParent();

			if ( n == null ) {
				// Hit the top of the parse tree, shouldn't happen
				throw new ParseException( "Encompassing rule not found for rightlabel '" 
						+ rightlabel + "'.");
			} 

			if ( "rule".equals( n.getKey() ) ) {
				// found the rule. Return the rule name
				out = n.get("label").getValue();
			}
		}

		return out;
	}
}

