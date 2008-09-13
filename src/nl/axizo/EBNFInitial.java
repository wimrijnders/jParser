/**
 * $Id$
 *
 * Initial, handcoded version of EBNF Parser.
 * Name was changed so that there will be no conflict with generated
 * output.
 */
package nl.axizo;

import nl.axizo.parser.*;
import nl.axizo.EBNF.*;
import java.util.regex.Pattern;
import java.util.Vector;
import java.io.IOException;


public class EBNFInitial extends BasicParser {
	private Pattern whitespace;
	private Pattern eol;
	private Pattern ts;
	private Pattern comment_ch;
	private Pattern label_start;
	private Pattern label_ch;
	private Pattern number_ch;
	private Pattern literal_ch;
	private Pattern lit_override_ch;
	private Pattern charset_ch;
	private Pattern override_ch;
	private Pattern plusminus_ch;
	private Pattern postfix_ch;
	private Pattern action_ch;
	private Pattern string_ch;

	public EBNFInitial( String filename ) {
		super( filename );

		whitespace      = Pattern.compile( "[ \r\n\t]+");
		eol             = Pattern.compile( "\r\n|\r|\n");
		ts              = Pattern.compile( "[ \t]+");
		comment_ch      = Pattern.compile( "[\\x00-\\xFF&&[^\r\n]]+");
		label_start     = Pattern.compile( "[a-zA-Z_]");
		label_ch        = Pattern.compile( "[a-zA-Z_0-9]*");
		number_ch       = Pattern.compile( "[0-9]+");
		literal_ch      = Pattern.compile( "[ a-zA-Z_0-9#/~\\-\\]\\[\\]\\(\\)|{}=.!$:&*,+;]");
		lit_override_ch = Pattern.compile( "[\\\\\'\"]");
		charset_ch      = Pattern.compile( "[_a-zA-Z +0-9'\"+*?.{}$]");
		override_ch     = Pattern.compile( "[\\]\\-\\\\)~trn]");
		plusminus_ch    = Pattern.compile( "[+\\-]");
		postfix_ch      = Pattern.compile( "[?+*]");
		action_ch       = Pattern.compile( "[\\x00-\\xFF&&[^\"/\\r{}$#]]+");
		string_ch       = Pattern.compile( "[\\x00-\\xFF&&[^\"\\r\\n]]*");
	}

	public boolean WS_intern(State state ) throws ParseException {
		while( parseCharset( whitespace, state ) || s( "comment", state, false, true) );

		return true;
	}

	protected boolean WS(State state ) throws ParseException {

		s( "WS_intern", state, false, true);

		return true;
	}


	public boolean p_comment(State state ) throws ParseException {

		parseString( "#", state, true );

		parseCharset( comment_ch, state );
		parseCharset( eol, state, true );

		state.getCurNode().collect();

		return true;
	}

	public boolean c_comment(State state ) throws ParseException {

		parseString( "//", state, true );

		parseCharset( comment_ch, state );
		parseCharset( eol, state, true );

		state.getCurNode().collect();

		return true;
	}


	public boolean comment(State state ) throws ParseException {

		if (
			s( "c_comment", state) ||
			s( "p_comment", state, true)
		);

		state.getCurNode().collect();

		return true;
	}


	public boolean label(State state ) throws ParseException {

		parseCharset( label_start, state, true );
		parseCharset( label_ch, state );

		state.getCurNode().collect();
		return true;
	}

	/**
 	 * Parse for Err modifier.
 	 *
 	 * TODO: This modifier is redundant. When parser works, it should be removed.
 	 */
	public boolean modifier_Err(State state ) throws ParseException {
		return 
			( parseString( "Err", state ) && parseCharset( plusminus_ch, state, true ) ); 
	}

	public boolean modifier_WS(State state ) throws ParseException {
		if ( 
			parseString( "noWS", state ) || 
			parseString( "skipWS", state, true )
		); 

		state.getCurNode().collect();
		return true;
	}


	public boolean modifier(State state ) throws ParseException {
		if (	
			s( "modifier_WS", state ) ||
			s( "modifier_Err", state, true, true)
		);

		// replace this node with its children
		state.setSkipCurrent( true );
		
		return true;
	}


	public boolean rule_modifier(State state ) throws ParseException {
		return 
			parseString( "ignore", state )  ||
			parseString( "entry", state )  ||
			parseString( "token", state ); 
	}

	public boolean postfix(State state ) throws ParseException {
		boolean ret= parseCharset( postfix_ch, state );

		if ( ret) {
			state.getCurNode().collect();
		}

		return ret;
	}

	public boolean atomicstatement(State state ) throws ParseException {
		if (
			s( "literal", state ) ||
			s( "charset", state ) ||
			s( "label", state ) ||
			s( "repeat", state ) ||
			s( "group", state, true )
		);

		s( "postfix", state );

		//
		//Post-action
		//

		// replace this node with its children
		state.setSkipCurrent( true );

		return true;
	}

	
	public boolean except(State state ) throws ParseException {
		boolean ret = true;

		if ( ret = parseString( "~", state, false, true ) ) {
			s( "atomicstatement", state, true );
		}

		return ret;
	}


	public boolean statement(State state ) throws ParseException {
		s( "atomicstatement", state, true );

		s( "except", state );

		return true;
	}

	public boolean alternative(State state ) throws ParseException {
		s( "statement", state, true );

		do WS(state); while(
			parseString( "|", state, false, true ) &&
			s( "statement", state )
		);

		return true;
	}

	public boolean statements(State state ) throws ParseException {
		s( "modifier", state );
		WS(state);
 
		s( "alternative", state, true );

		do WS(state); while(
			s( "modifier", state ) ||
			s( "alternative", state )
		);

		return true;
	}

	public boolean rule(State state ) throws ParseException {
		s( "rule_modifier", state );
		WS(state);

		s( "label", state, true );
		WS(state);

		parseString( "=", state, true, true );
		WS(state);

		s( "statements", state );
		WS(state);

		parseString( ".", state, true, true );

		return true;
	}

	public boolean lit_override(State state ) throws ParseException {

		parseString( "\\", state, true, true );
		parseCharset( lit_override_ch, state, true );

		return true;
	}

	public boolean literal_chars(State state ) throws ParseException {
		do; while (
			parseCharset( literal_ch, state ) ||
			s( "lit_override", state )
		);
		return true;
	}

	public boolean literal_s(State state ) throws ParseException {
		parseString( "'", state, true, true );
		s( "literal_chars", state );
		parseString( "'", state, true, true );

		state.getCurNode().collect();
		return true;
	}

	public boolean literal_d(State state ) throws ParseException {
		parseString( "\"", state, true, true );
		s( "literal_chars", state );
		parseString( "\"", state, true, true );

		state.getCurNode().collect();
		return true;
	}

	public boolean literal(State state ) throws ParseException {
		boolean ret =
			s( "literal_s", state ) ||
			s( "literal_d", state );

		state.getCurNode().collect();

		return ret;
	}

	public boolean override(State state ) throws ParseException {

		parseString( "\\", state, true );
		if (
			parseCharset( override_ch, state ) ||
			parseString( "all", state, true )
		);

		state.getCurNode().collect();
		return true;
	}

	public boolean range(State state ) throws ParseException {

		if (
			parseCharset( charset_ch, state ) ||
			s( "override", state, true )
		);

		if ( parseString( "-", state ) ) {
			if (
				parseCharset( charset_ch, state ) ||
				s( "override", state, true )
			);
		}

		state.getCurNode().collect();
		return true;
	}

	public boolean except_charset(State state ) throws ParseException {

		parseString( "~", state, true, true );
		do; while (
			s( "range", state )
		);

		return true;
	}

	public boolean charset(State state ) throws ParseException {

		parseString( "[", state, true, true );

		do; while (
			s( "range", state ) 
		);
		s( "except_charset", state );
		parseString( "]", state, true, true );

		return true;
	}

	public boolean repeat(State state ) throws ParseException {

		parseString( "{", state, true, true );

		do WS(state); while (
			s( "statements", state )
		);

		parseString( "}", state, true, true );


		return true;
	}

	public boolean group(State state ) throws ParseException {

		parseString( "(", state, true, true );

		do WS(state); while (
			s( "statements", state )
		);

		parseString( ")", state, true, true );

		return true;
	}

	public boolean ctor_init(State state ) throws ParseException {

		if ( parseString( "(", state, true ) ) {
			WS(state);
			parseCharset( number_ch, state, true );
			WS(state);
			parseString( ")", state, true );
		};

		state.getCurNode().collect();
		return true;
	}

	public boolean param_init(State state ) throws ParseException {

		parseString( "=", state, true ); 
		WS(state);
		if (
			parseString( "true" , state ) ||
			parseString( "false", state ) ||
			parseCharset( number_ch, state, true )
		);

		state.getCurNode().collect();
		return true;
	}

	public boolean statevar(State state ) throws ParseException {
		trace( "Entered statevar");

		s( "label", state,true );
		WS(state);
		s( "label", state, true );
		WS(state);

		if ( s( "param_init", state, false ) ||
			s( "ctor_init", state, false ) );

		WS(state);
		parseString( ";", state, true, true );

		return true;
	}

	public boolean stateblock(State state ) throws ParseException {
		trace( "Entered stateblock");

		parseString( "state", state, true, true );
		WS(state);
		parseString( "{", state, true, true );

		do WS(state); while (
			s( "statevar", state )
		);

		parseString( "}", state, true, true );

		return true;
	}

	public boolean pathvar(State state ) throws ParseException {
		trace( "Entered pathvar");

		parseString( "[", state, true, true );
		s( "label", state, true);
		parseString( "]", state, true, true );

		// 
		// Post-action
		//

		Node n = state.getCurNode();
		n.collect();
		n.setValue( "\" + " + n.getValue() + " + \"");

		return true;
	}

	public boolean macro_path(State state ) throws ParseException {
		trace( "Entered macro_path");

		do; while(
			( 
				parseString( ".", state, false ) ||
				parseString( "!", state, false ) 
			) && (
				s( "label", state ) ||
				s( "pathvar", state )
			)
		);

		// 
		// Post-action
		//
		Node n = state.getCurNode();
		n.collect();
		n.setValue( "root.getByPath( \"" + n.getValue() + "\")" );

		return true;
	}

	public boolean foreach(State state ) throws ParseException {
		trace( "Entered foreach");

		parseString( "foreach", state, true, true );

		// No eol's allowed, only tabs and spaces
		parseCharset( ts, state, false, true );

		s( "label", state, true);
		parseCharset( ts, state, false, true );
		parseString( "in", state, true, true );
		parseCharset( ts, state, false, true );

		s( "macro_path", state, true);

		parseCharset( ts, state, false, true );
		parseString( "$", state, true, true );
		WS(state);
		parseString( "$body$", state, true, true );
		s( "action_content", state, true);

		//Note missing trailing $ - it's defined one level up in macro()
		// This is because this definition also contains the body and end-foreach tags
		parseString( "$end foreach", state, true, true );

		// 
		// Post-action
		//
		Node n = state.getCurNode();
		Node l = n.get( "label" );
		Node p = n.get( "macro_path" );
		Node c = n.get( "action_content" );
		String base = "base" + getNextSeq();
		String varloop = "i" + getNextSeq();
		c.collect();
		n.collect();	// to get rid of the children

		String str = "Node "+ base +" = " +  p.getValue() + ";\n";
		str += "for( int " + varloop +" = 0; i < " + base + ".length(); ++i ) {\n" +
			 "\tNode " + l.getValue() + " = base.get("+ varloop +");\n" +
			c.getValue() +
			"}\n";

		n.setValue( str );

		return true;
	}

	public boolean currentToken(State state ) throws ParseException {
		parseString( "CurrentToken", state, true, true); 

		// 
		// Post-action
		//
		Node n = state.getCurNode();
		n.setValue( "state.getCurNode()" );

		return true;
	}

	public boolean macro(State state ) throws ParseException {
		trace( "Entered action");

		parseString( "$", state, true, true );

		if (
			s( "currentToken", state ) ||
			s( "foreach", state ) ||
			s( "macro_path", state, true)
		);

		parseString( "$", state, true, true );

		return true;
	}

	public boolean action_text(State state ) throws ParseException {
		boolean ret = parseCharset( action_ch, state );

		if ( ret ) {
			state.getCurNode().collect();
		}
		return ret;
	}

	public boolean action_string(State state ) throws ParseException {
		parseString( "\"", state, true );
		parseCharset( string_ch, state, true );
		parseString( "\"", state, true );

		state.getCurNode().collect();
		return true;
	}

	public boolean action_content(State state ) throws ParseException {
		trace( "Entered action_content");

		// TODO: Following construct presents problems when real errors
		// are encountered in action_block.
		do; while (
			// order important
			parseString( "\r", state, false, true ) ||
			s( "p_comment", state, false, true ) ||
			s( "c_comment", state ) ||
			s( "action_string", state ) ||
			s( "action_text", state ) ||
			s( "macro", state ) ||
			s( "action_block", state )
		);

		state.getCurNode().collect();
		return true;
	}

	public boolean action_block(State state ) throws ParseException {
		trace( "Entered action_block");

		parseString( "{", state, true );

		s( "action_content", state, true);

		parseString( "}", state, true );

		return true;
	}

	public boolean pre_block(State state ) throws ParseException {

		parseString( "pre:", state, true, true );
		WS(state);
		parseString( "{", state, true, true );

		s( "action_content", state, true);

		parseString( "}", state, true, true );

		return true;
	}

	public boolean action(State state ) throws ParseException {
		trace( "Entered action");

		parseString( "action", state, true, true );
		WS(state);
		s( "label", state, true);
		WS(state);

		if( s( "pre_block", state) ) {
			WS(state);

			if ( parseString( "post:", state, false, true ) ) {
				WS(state);
				parseString( "{", state, true, true );
				s( "action_content", state, true);
				parseString( "}", state, true, true );
			}
		} else {
			// If post-block only, then the post:-prefix
			// is not obligatory
			parseString( "post:", state, false, true ); 
			WS(state);

			parseString( "{", state, true, true );
			s( "action_content", state, true);
			parseString( "}", state, true, true );
		}

		return true;
	}

	public boolean parameter(State state ) throws ParseException {
		s( "label", state, true);
		WS(state);
		if ( 
			parseString( "&", state ) ||
			parseString( "*", state )
		);
		WS(state);
		s( "label", state, true);
		return true;
	}

	public boolean returnval(State state ) throws ParseException {
		return 
			parseString( "BOOL", state) ||
		    parseString( "void", state) ||
			( s( "label", state ) && WS(state) &&  parseString( "&", state) );
	}

	public boolean function(State state ) throws ParseException {
		trace( "Entered function");

		parseString( "function", state, true, true );
		WS(state);
		s( "returnval", state, true);
		WS(state);
		s( "label", state, true);
		WS(state);

		parseString( "(", state, true, true );
		WS(state);
		if ( parseString( "void", state) ) {
			// No op
		} else if ( s( "parameter", state) ) {
			while ( WS(state) && parseString( ",", state, false, true ) ) {
				WS(state); 
				s( "parameter", state, true);
			}
		}
		parseString( ")", state, true, true );
		WS(state);

		parseString( "{", state, true, true );
		s( "action_content", state, true);
		parseString( "}", state, true, true );

		if ( state.getErrorPos() != -1 ) {
			trace( "After action_content: Error in " + state.getErrorMethod() + " at: " + 
				curLine( state.getErrorPos() ) );
		}

		return true;
	}

	public boolean language(State state ) throws ParseException {
		trace( "Entered language");

		parseString( "language", state, true, true );
		WS(state);
		s( "label", state, true );
		WS(state);
		parseString( "{", state, true, true );

		do WS(state); while(
			s( "modifier"  , state ) ||
			s( "rule"      , state ) ||
			s( "stateblock", state ) ||
			s( "action"    , state ) ||
			s( "function"  , state )
		);
		parseString( "}", state, true, true );

		return true;
	}


	protected State parse()
		throws NoSuchMethodException, IllegalAccessException {

		State state = new State();

		// Parse according to rules
		try {
			while ( !eol(state.getCurpos() ) ) {
//			while ( !state.eol() ) {
				WS(state);

				if ( !s( "language", state ) ) break;
			}
		} catch ( ParseException e ) {
			error( "Exception: " + e.toString() );
		}


		return state;

/*
		// Do node translations
		translate( state );

		// Create output
		generate( state );

		// Exit
		saveNodes( state );
		showFinalResult(state);
*/
	}


	/**
 	 * Generate output from the current node tree.
 	 */
	private void generate( State state ) {
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
			error( "error while saving '" + outfile + "': " + e.toString() );
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
					warning( "non-call found while generating alternative" );
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
					warning( "non-alternative found while generating statements"
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


	/**
 	 * Show the final status of the parsing.
 	 *
 	 * Errors are shown in the error output in this step.
 	 */
	private void showFinalResult( State state ) {
		try {
//			if ( state.eol() ) {
			if ( eol(state.getCurpos() ) ) {
				info("Parsing completed succesfully.");
			} else { 
				if ( state.getErrorPos() != -1 ) {
					error( "Error in " + state.getErrorMethod() + " at: " + 
						curLine( state.getErrorPos() ) );
				}
			}
			trace( "curpos: " + curLine( state.getCurpos() ) );
		} catch ( ParseException e ) {
			error( "Exception: " + e.toString() );
		}
	}


	public static void main(String[] argv) 
		throws NoSuchMethodException, IllegalAccessException {

		EBNFInitial parser = new EBNFInitial( argv[0] );
		//parser.setTraceLevel( TRACE );
		parser.setFirstTwoLines(true);
		State state = parser.parse();

		
		// Do node translations
		EBNFTranslator translator = new EBNFTranslator();
		translator.translate( state );

		// Create output
		EBNFGenerator generator = new EBNFGenerator();
		generator.generate( state );

		// Exit
		parser.saveNodes( state );
		parser.showFinalResult(state);
	}
}
