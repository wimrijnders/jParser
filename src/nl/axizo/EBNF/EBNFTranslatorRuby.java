/**
 * $Id: EBNFTranslator.java 35 2008-10-17 20:15:31Z wri $
 *
 */
package nl.axizo.EBNF;

import nl.axizo.parser.*;
import java.util.Vector;
import java.util.regex.*;


/**
 * Perform translations on the parse tree, as generated by the
 * EBNF parser.
 *
 * This class is used between the parse step (in which an input
 * file is converted to a parse tree) and the generation step (in
 * which output is generated from the parse tree). 
 *
 * The object is to perform operations on the data in the parse tree, 
 * so that:
 *
 *		- unneeded data is removed.
 * 		- the generation step is simplified.
 */
public class EBNFTranslatorRuby extends EBNFTranslator {

	/**
 	 * Perform translation steps on charset nodes.
	 */
	protected void translateCharsets( State state ) {

		// Translate special characters in charset ranges to the ruby regexp equivalents
		Vector res =  state.getCurNode().findNodes( "range" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String val = n.getValue();

			if ( val.equals( "\\~" ) ) {
				n.setValue( "~" );
			} else if ( val.equals( "." ) ) {
				n.setValue( "\\." );
			} else if ( val.equals( "/" ) ) {
				n.setValue( "\\/" );
			} else if ( val.equals( "\\all" ) ) {
				// After '\all', only an except clause can follow for ruby.
				// This is explicitly checked for in classs EBNFValidatorRuby.
				n.setValue( "^");
			}
		}


		// Create java code for the charsets
		createMatchPatterns( state, "charset", "Pattern", true );

		// Create java code for the regexps
		createMatchPatterns( state, "regexp", "RegExp", false );
	}


	private void createMatchPatterns(State state, String label, String name, boolean addBraces ) {
		Vector res =  state.getCurNode().findNodes( label );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);
			String matchName = name + ( i + 1 );

			n.collect();
			String value = n.getValue();
			if ( addBraces) value = "/[" + value + "]/";

			String member_value = "\t\t" + matchName + " = " + value + "\n";

			state.getCurNode().set( "temp" ).set( "members" ).addChild( 
					new Node( "member_pattern", member_value) 
			);
			n.setValue( matchName );
		}
	}

	protected String getWhileConstruct() {
		return "begin; end while ";
	}

	/*
 	 * Override of parent method.
 	 */ 
	protected String makeCall(Node child) {
		String call = super.makeCall(child);

		// Add specific handling for the ruby parser
		if ( call == null ) {
			if ( "regexp".equals( child.getKey() ) ) { 
				call = "parseRegexp( " + child.getValue() + ", state"; 
			}
		}

		return call;
	}
}

