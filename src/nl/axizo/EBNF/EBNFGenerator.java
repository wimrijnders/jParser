/**
 * $Id$
 *
 */
package nl.axizo.EBNF;

import nl.axizo.parser.*;
import java.io.IOException;
import java.util.Vector;

/**
 *  Generates output from a parse tree.
 *
 * The parse tree must have been created with an EBNF parser.
 */
public class EBNFGenerator {

	/**
 	 * Generate output from the current node tree.
 	 */
	public void generate( State state ) throws ParseException {
		String output = "";

		Node root = state.getCurNode();

		String className       = root.get("language").get("label").getValue();
		final String outfile   = className + ".java";
		Node   member_patterns = root.get("temp").get("members");
		Node   init_patterns   = root.get("temp").get("ctor");

		output += 
				  "/* THIS FILE IS GENERATED!\n"
				+ " *\n"
				+ " * Editing it is a bad idea.\n"
				+ " */\n"
				+ "package nl.axizo.EBNF;\n"
				+ "\n"
				+ "import nl.axizo.parser.*;\n"
				+ "import java.util.regex.*;\n"
				+ "\n"
				+ "\n"
				+ "public class " + className + " extends BasicParser {\n\n";

		int num = member_patterns.numChildren();
		for( int i = 0;  i < num; ++i ) {
			output += member_patterns.get(i).getValue();
		}

		// Generate the constructor
		output += "\n\n\t" + "public " + className + "(String filename) {\n"
				+ "\t\tsuper(filename);\n\n";

		num = init_patterns.numChildren();
		for( int i = 0;  i < num; ++i ) {
			output += init_patterns.get(i).getValue();
		}

		output += "\t}\n\n";

		// Generate methods
		Vector rules = root.findNodes("rule");
		for( int i = 0;  i < rules.size(); ++i ) {
			Node rule = (Node) rules.get(i);
			output += generateMethod( rule );
		}

		
		output += generateEntryPoint(root ); 

		output += "}\n";

		// Save what we got
		try {
			Util.saveFile( outfile, output );
		} catch( IOException e) {
			String errMsg = "error while saving '" + outfile + "': " + e.toString();
			Util.error( errMsg );
			throw new ParseException( errMsg);
		}
	}


	private String generateAlternative( Node n, boolean doWS, boolean isFirst ) {
		String out = "";

		if ( doWS && !isFirst ) out += "\t\tWS(state);\n";

		if ( n.numChildren() == 1 ) {
			String value =  n.get("call").getValue();
			int whileIndex =  value.indexOf( "do; while" );
			boolean isWhileLoop = ( whileIndex != -1 );

			// Special case for loop, need to take the endbrace into
			// account. This is sort of dirty. TODO: examine better solution
			if ( isWhileLoop ) {
				// While-loops at start of statement need to do WS regardless
				// isFirst therefore ignored here.
				if ( doWS ) {
					// Dirty: insert WS handling in while loop
					int len = "do; while".length();
					int endIndex = whileIndex + len;
					value = value.substring(0, whileIndex ) 
							+ "do WS(state); while" 
							+ value.substring(endIndex);
				}

				// while-loop 
				out += "\t\t" + value + " ));\n";
			} else {

				// normal statement
				String throwParam = ", true";

				// If a parameter was passed, use passed value instead
				if ( !n.get("call").get("string").isNull() ) {
					throwParam = n.get("call").get("string").getValue();
					Util.info("Detected parameter '" + throwParam 
							+ "' for call to '" + value + "'");
				}

				out += "\t\t" + value + throwParam + " );\n";
			}
		} else {
			// more than one child
			out += "\t\tif (\n";

			for( int i = 0; i < n.numChildren(); ++ i ) {
				Node c = n.get( i );

				if ( !"call".equals( c.getKey() ) ) {
					// TODO: enable alternatives for non-calls.
					//       this would be while-loops
					Util.warning( "non-call found while generating alternative" );
					continue;
				}

				if ( i != 0 ) {
					out += "\t\t || ";
				} else {
					out += "\t\t    ";
				}

				out += c.getValue();

				// Only final statement in if-block gets to throw
				boolean doThrow = ( i == n.numChildren() -1 );

				if ( doThrow ) {
					out += ", true";
				}

				out += " )\n";
			}


			out += "\t\t);\n";
		}

		return out;
	}

	private String generateStatements( Node n ) {
		Node body = n.get("statements");
		String out = "";
		boolean isFirst = true;

		out += "\t\t// Generated parsing code\n";
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

			// Never do WS for first call in block
			boolean doWS = "skipWS".equals( c.getValue() );

			out += generateAlternative( c, doWS, isFirst );
			isFirst = false;
		}

		return out;
	}


	/**
 	 * Determine is passed rule node has a token modifier.
 	 *
 	 * @param rule node to check for token modifier.
 	 * @return true if token modifier present, false otherwise.
 	 */
	private static boolean isTokenRule( Node rule ) {
		return "token".equals( rule.get("rule_modifier").get("string").getValue() );
	}

	/**
 	 * Determine is passed rule node has a skip modifier.
 	 *
 	 * A rule with a skip modifier gets hidden in the output,
 	 * while the generated children of the rule are retained.
 	 *
 	 * @param rule node to check for skip modifier.
 	 * @return true if skip modifier present, false otherwise.
 	 */
	private static boolean skipThisRule( Node rule ) {
		return "skip".equals( rule.get("rule_modifier").get("string").getValue() );
	}

	/**
 	 * Generate method code from given Node.
 	 */
	private String generateMethod( Node rule ) {
		String name   = rule.get("label").getValue();
		String output = "";

		output += "\tpublic boolean " + name + "(State state ) throws ParseException {\n" +
				"\t\ttrace(\"Called method '" + name + "'.\");\n\n";
		output += generateStatements( rule );

		if ( isTokenRule( rule) ) {
			output += "\n\t\tstate.getCurNode().collect();\n";
		}

		if ( skipThisRule( rule ) ) {
			output += "\n\t\t// replace this node with its children\n" +
						"\t\tstate.setSkipCurrent( true );\n";
		}

		output += "\n\t\ttrace(\"Completed method '" + name + "'; value: \" +" + 
			"state.getCurNode().getValue() + \".\");\n";

		output += "\n\t\treturn true;\n\t}\n\n";

		return output;
	}

	/**
 	 * Detect entry point for the parse and generate code for calling it.
 	 * 
 	 * @throw ParseException if no entry point found.
 	 */
	private String generateEntryPoint( Node root ) throws ParseException {

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
		"	protected State parse()\n" +
		"		throws NoSuchMethodException, IllegalAccessException {\n" +
		"\n" +
		"		State state = new State();\n" +
		"\n" +
		"		// Parse according to rules\n" +
		"		try {\n" +
		"			while ( !eol(state.getCurpos() ) ) {\n" +
		"				WS(state);\n" +
		"\n" +
		"				if ( !s( \"" + entry_label + "\", state ) ) break;\n" +
		"			}\n" +
		"		} catch ( ParseException e ) {\n" +
		"			error( \"Exception: \" + e.toString() );\n" +
		"		}\n" +
		"\n" +
		"		return state;\n" +
		"	}\n\n";

		return out;
	} 
}

