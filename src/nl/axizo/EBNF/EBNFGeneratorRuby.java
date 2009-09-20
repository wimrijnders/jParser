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
 *
 * Adaptation of the java EBNFGenerator, so that it outputs
 * ruby code
 */
public class EBNFGeneratorRuby extends EBNFGenerator {

	/**
 	 * Generate output from the current node tree.
 	 */
	public void generate( State state ) throws ParseException {
		Util.info("Running EBNFGeneratorRuby.generate()");

		String output = "";

		Node root = state.getCurNode();

		
		String className       = root.get("language").get("label").getValue() + "Ruby";
		final String outfile   = className + ".rb";
		Node   member_patterns = root.get("temp").get("members");
		Node   init_patterns   = root.get("temp").get("ctor");

		Map actionMap = getActions(root);

		///////////////////////////////////
		// Perform the output generation
		///////////////////////////////////
	

		// Create the file header.	
		output += 
				  "# THIS FILE IS GENERATED!\n"
				+ "#\n"
				+ "# Editing it is a bad idea.\n"
				+ "#\n"
				+ "require 'parser'\n"
				+ "\n"
				+ "\n"
				+ "class " + className + " < BasicParser \n\n";

		int num = member_patterns.numChildren();
		for( int i = 0;  i < num; ++i ) {
			output += member_patterns.get(i).getValue();
		}
		output += "\n\n";

		//
		// Generate the constructors
		// 
		//
		// WRI: Prob not needed in Ruby!
		//output += "\t" + "def initialize buffer, loadFromFile = true\n"
		//		+ "\t\tsuper buffer, loadFromFile\n\n";
		//
		//num = init_patterns.numChildren();
		//for( int i = 0;  i < num; ++i ) {
		//	output += init_patterns.get(i).getValue();
		//}
		//
		//output += "\tend\n\n";

		//
		// If WS_intern not present, add default implementation
		//
		// This is actually WS (if present), renamed to WS_intern
		// in the Translator step.
		//
		
		//Check if WS is present
		Vector ws_intern = root.findNodesByValue("WS_intern");
		if ( ws_intern.size() == 0 ) {
			Util.info("No internal WS present. Creating default implementation.");

			output += "\tdef WS_intern state\n"
					+ "\t\tsuper.WS_intern state\n"
					+ "\tend\n\n";
		}


		//
		// Generate methods
		//
		Vector rules = root.findNodes("rule");
		for( int i = 0;  i < rules.size(); ++i ) {
			Node rule = (Node) rules.get(i);
			output += generateMethod( rule, actionMap );
		}


		//
		// Add native code, if present
		// 
		Node native_code = root.get("language").get("native_code");
		if ( !native_code.isNull() ) {
			output += "\n\t# Start native code\n"
					+ native_code.getValue()
					+ "\n# End native code\n\n";
		}
		
		output += generateEntryPoint(root ); 

		output += "end\n";

		// Save what we got
		try {
			Util.saveFile( outfile, output );
		} catch( IOException e) {
			String errMsg = "error while saving '" + outfile + "': " + e.toString();
			Util.error( errMsg );
			throw new ParseException( errMsg);
		}
	}


	protected String generateAlternative( Node n, boolean doWS, boolean isFirst ) {
		String out = "";

		if ( doWS && !isFirst ) out += "\t\tWS state\n";

		if ( n.numChildren() == 1 ) {
			String value =  n.get("call").getValue();
			int whileIndex =  value.indexOf( "begin; end while " );
			boolean isWhileLoop = ( whileIndex != -1 );

			// Special case for loop, need to take the endbrace into
			// account. This is sort of dirty. TODO: examine better solution
			if ( isWhileLoop ) {
				// While-loops at start of statement need to do WS regardless
				// isFirst therefore ignored here.
				if ( doWS ) {
					// Dirty: insert WS handling in while loop
					int len = "begin; end while ".length();
					int endIndex = whileIndex + len;
					value = value.substring(0, whileIndex ) 
							+ "begin WS state; end while " 
							+ value.substring(endIndex);
				}

				// while-loop 
				out += "\t\t" + value + " )\n";
			} else {

				// normal statement
				boolean mustReturn = true;

				String throwParams = "";

				// If parameters were passed, use those instead
				Node param1 = n.get("call").get("param1");
				Node param2 = n.get("call").get("param2");

				// Param1 overrides default value
				if ( !param1.isNull() ) {
					mustReturn = Boolean.getBoolean( param1.getValue() );
				}

				//Param2 gets appended
				if ( !param2.isNull() ) throwParams += "," + param2.getValue();

				if ( !( param1.isNull() || param2.isNull() ) ) {
					Util.info("Detected parameters '" + throwParams 
							+ "' for call to '" + value + "'");
				}

				if ( mustReturn ) {
					out += "\t\treturn false unless " + value + ", false" + throwParams + " )\n";
				} else {
					out += "\t\t" + value + ", false" + throwParams + " );\n";
				}
			}
		} else {
			// more than one child
			out += "\t\treturn false unless ";

			for( int i = 0; i < n.numChildren(); ++ i ) {
				Node c = n.get( i );

				if ( !"call".equals( c.getKey() ) ) {
					// TODO: enable alternatives for non-calls.
					//       this would be while-loops
					Util.warning( "non-call found while generating alternative" );
					continue;
				}

				if ( i != 0 ) {
					out += "or ";
				}

				out += c.getValue();

/*
				// Only final statement in if-block gets to throw
				boolean doThrow = ( i == n.numChildren() -1 );

				if ( doThrow ) {
					out += ", true";
				}
*/
				out += " ) ";
			}


			//out += "\t\t);\n";
			out += "\n";
		}

		return out;
	}

	protected String generateStatements( Node n ) {
		Node body = n.get("statements");
		String out = "";
		boolean isFirst = true;

		// Special case, WS_intern should not handle whitespace 
		// internally
		boolean isWS_intern = "WS_intern".equals( n.get("label").getValue());
		boolean warnedAboutWS = false;

		out += "\t\t# Generated parsing code\n";
		for( int i = 0; i < body.numChildren(); ++ i ) {
			Node c = body.get( i );

			if ( !"alternative".equals( c.getKey() ) ) {
				//WS modifier is allowed; skip warning in this case
				if ( !"modifier_WS".equals( c.getKey() ) ) {
					Util.warning( "non-alternative found while generating statements"
							+ " for rule " + n.get("label").getValue() );
				}
				continue;
			}

			// Flag for determining if whitespace should be handled automatically
			boolean doWS = "skipWS".equals( c.getValue() );

			if ( doWS && isWS_intern && !warnedAboutWS ) {
				Util.warning( "Disabling WS handling within WS function.");
				warnedAboutWS = true;
			}

			if ( isWS_intern) doWS = false;

			out += generateAlternative( c, doWS, isFirst );
			isFirst = false;
		}

		return out;
	}

	/**
 	 * Generate method code from given Node.
 	 */
	protected String generateMethod( Node rule, Map actionMap ) {
		String name   = rule.get("label").getValue();
		String output = "";

		output += "\tdef " + name + " state\n" +
				"\t\ttrace \"Called method '" + name + "'.\", " + (Util.TRACE -5) + "\n\n";

		Node pre_node  = null;
		Node post_node = null;
		if ( actionMap.containsKey( name ) ) {
			// Actions present, add a handy reference for the current token.
			output += "\t\tcurrent = state.curNode\n\n";

			Node action = (Node) actionMap.get( name );

			pre_node  = action.get("pre_block" ).get("code_block");
			if ( pre_node.isNull() ) pre_node = null; 

			post_node = action.get("post_block").get("code_block");
			if ( post_node.isNull() ) post_node = null; 
		}

		// Ignore rule flag needs to be set before statements are generated
		if ( ignoreThisRule( rule ) ) {
			output += "\t\t# Block output of current node and its children\n" +
				"\t\tstate.ignoreCurrent = true\n\n";
		}

		if ( pre_node != null ) {
			output += "\t\t# Pre actions\n"
					+ pre_node.getValue()
					+ "\n\t\t# End Pre actions\n\n";
		}

		output += generateStatements( rule );

		if ( isTokenRule( rule) ) {
			output += "\n\t\tstate.curNode.collect\n";
		}

		if ( post_node != null ) {
			output += "\n\t\t# Post actions\n"
					+ post_node.getValue()
					+ "\n\t\t# End Post actions\n";
		}


		if ( skipThisRule( rule ) ) {
			output += "\n\t\t# replace this node with its children\n" +
						"\t\tstate.skipCurrent = true\n";
		}

		output += "\n\t\ttrace \"Completed method '" + name + "'; value: #{ state.curNode.value }.\"\n";

		output += "\n\t\ttrue\n\tend\n\n";

		return output;
	}

	/**
 	 * Detect entry point for the parse and generate code for calling it.
 	 * 
 	 * @throw ParseException if no entry point found.
 	 */
	protected String generateEntryPoint( Node root ) throws ParseException {

		// Find the entry  point
		Vector rule_modifiers = root.findNodes( "rule_modifier" );
		Node entry_node = null;
		int	 entry_count = 0;
		for( int i = 0;  i < rule_modifiers.size(); ++i ) {
			Node mod = (Node) rule_modifiers.get(i);

			if ( "entry".equals( mod.get("string").getValue() ) ) {
				entry_node = mod.getParent();
				entry_count++;
			} 
		}

		if ( entry_node == null ) {
			throw new ParseException( "No entry point found.");
		}

		if ( entry_count > 1 ) {
			throw new ParseException( "too many entry points.");
		}

		String entry_label = entry_node.get("label").getValue();
		Util.info( "Entry point found; label: '" + entry_label + "'");

		// Generate the corresponding code.
		String out =
		"	def parse \n" +
		"\n" +
		"		state = State.new\n" +
		"\n" +
		"		# Parse according to rules\n" +
		"		unless eol(state.curpos )\n" +
		"			WS state\n" +
		"\n" +
		"			unless s( \"" + entry_label + "\", state )\n" +
		"				state.setError \"end of parsing\"\n" +
		"				break\n" +
		"			end\n" +
		"		end\n" +
		"\n" +
		"	rescue  ParseException  => e\n" +
		"		error \"Exception: \" + e.to_s\n" +
		"	ensure\n" +
		"		return state\n" +
		"	end\n\n";

		return out;
	} 
}

