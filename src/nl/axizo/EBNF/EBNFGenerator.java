/**
 * $Id$
 *
 */
package nl.axizo.EBNF;

import nl.axizo.parser.State;
import nl.axizo.parser.Node;
import nl.axizo.parser.Util;
import java.io.IOException;
import java.util.Vector;
//import java.util.regex.*;

/**
 *  Generates output from a parse tree.
 *
 * The parse tree must have been created with an EBNF parser.
 */
public class EBNFGenerator {

	/**
 	 * Generate output from the current node tree.
 	 */
	public void generate( State state ) {
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

		output += "}\n";

		// Save what we got
		try {
			Util.saveFile( outfile, output );
		} catch( IOException e) {
			Util.error( "error while saving '" + outfile + "': " + e.toString() );
		}
	}


	private String generateAlternative( Node n, boolean doWS ) {
		String out = "";

		if ( doWS ) {
			out += "\t\tWS(state);\n";
		}

		if ( n.numChildren() == 1 ) {
			String value =  n.get("call").getValue();
			boolean isWhileLoop = ( value.indexOf( "do; while" ) != -1 );

			// Special case for loop, need to take the endbrace into
			// account. This is sort of dirty. TODO: examine better solution
			if ( isWhileLoop ) {
				// while-loop 
				out += "\t\t" + value + ", true ));\n";
			} else {
				// normal statement
				out += "\t\t" + value + ", true );\n";
			}
		} else {
			// more than one child
			out += "\t\tif (\n";

			for( int i = 0; i < n.numChildren(); ++ i ) {
				Node c = n.get( i );

				if ( !"call".equals( c.getKey() ) ) {
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
			boolean doWS = "skipWS".equals( c.getValue() ) && !isFirst;

			out += generateAlternative( c, doWS );
			isFirst = false;
		}

		return out;
	}


	/**
 	 * Generate method code from given Node.
 	 */
	private String generateMethod( Node rule ) {
		String name   = rule.get("label").getValue();
		String output = "";

		output += "\tpublic boolean " + name + "(State state ) throws ParseException {\n\n";
		output += generateStatements( rule );
		output += "\n\t\treturn true;\n\t}\n\n";

		return output;
	}

}

