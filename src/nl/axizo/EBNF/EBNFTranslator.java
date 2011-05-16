/**
 * $Id$
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
public class EBNFTranslator extends Translator {

	protected static String replace(
	    String aInput, String aOldPattern, String aNewPattern
  	) {
    	final Pattern pattern = Pattern.compile( "["+ aOldPattern + "]" );
	    final Matcher matcher = pattern.matcher( aInput );
	    return matcher.replaceAll( aNewPattern );
	}



	private void setWSFlags(State state) {
		handleWSFlags( state.getCurNode(), "skipWS" );
	}


	/**
 	 *  Set the proper WS mode in statement blocks.
 	 *
 	 *  This is an internal translation with the aim of
 	 *  easily  checking if implicit whitespace parsing is
 	 *  needed.
 	 */
	private void handleWSFlags(Node node, String WSValue) {
		for( int i = 0;  i < node.numChildren(); ++i ) {
			Node n = node.get(i);

			if ( "modifier_WS".equals(  n.getKey() )  ) {
				WSValue = n.getValue();
			} else if ( "alternative".equals(  n.getKey() ) ) {
				n.setValue( WSValue );
			}

			handleWSFlags( n, WSValue);
		}
	}

	/**
 	 * Remove empty statement nodes.
 	 *
 	 * Empty statements may be introduced by the skip line modifier.
 	 *
 	 * While technically correct and handled properly, this just adds
 	 * cruft to the parse tree.
 	 */
	private void removeEmptyStatements(State state ) throws ParseException {
		Vector res =  state.getCurNode().findNodes( "statement" );
		int count = 0;
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			if ( n.numChildren() == 0 ) {
				Node parent = n.getParent();
				parent.removeChild( n );

				// Parent is an alternative node (Assumption!)
				// If it is empty, we can remove it as well.
				if ( parent.numChildren() == 0 ) {
					Node granma = parent.getParent();
					granma.removeChild( parent );
				}

				count++;
			}
		}

		if ( count > 0 ) {
			Util.info( "Removed " + count + " empty statements.");
		}
	}

	/**
 	 * Perform translation steps on charset nodes.
	 */
	protected void translateCharsets( State state ) {

		// Translate special characters in charset ranges to the java regexp equivalents
		Vector res =  state.getCurNode().findNodes( "range" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String val = n.getValue();

			if ( val.equals( "\\-" ) || val.equals( "\\)" ) || val.equals( "\\]" ) || 
					val.equals( "\"" ) || val.equals("'") ) {
				n.setValue( "\\" + val );
			} else if ( val.equals( "\\\\" ) ) {
				n.setValue( "\\\\" + val );
			} else if ( val.equals( "\\~" ) ) {
				n.setValue( "~" );
			} else if ( val.equals( "\\all" ) ) {
				n.setValue( "\\\\x00-\\\\xFF");
			}
		}


		// translate the charset 'except' clause to the java equivalent
		res =  state.getCurNode().findNodes( "except_charset" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			n.collect();
			n.setValue( "&&[^" + n.getValue() + "]" );
		}


		// Create java code for the charsets
		res =  state.getCurNode().findNodes( "charset" );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);
			int counter = i + 1;	

			n.collect();
			String ctor_value = "\t\tpattern" + counter + " = Pattern.compile( \"[" + n.getValue() + "]\");\n";
			String member_value = "\tprivate Pattern pattern" + counter + ";\n";

			state.getCurNode().set( "temp" ).set( "ctor" ).addChild( 
					new Node( "init_pattern", ctor_value) 
			);
			state.getCurNode().set( "temp" ).set( "members" ).addChild( 
					new Node( "member_pattern", member_value) 
			);
			n.setValue( "pattern" + counter );
		}
	}


	/**
 	 * Perform translation steps on literal nodes.
	 */
	private void translateLiterals( State state ) {
		Vector res;

		res =  state.getCurNode().findNodes( "literal" );
		res.addAll( state.getCurNode().findNodes( "literal_symbol" ) );
		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);
		
			// Translate special characters in literal
			// following replaces a single backslash with a double. I kid you not.
			n.setValue( replace( n.getValue(), "\\\\", "\\\\\\\\") );
			//Okay, this sucks, but I will persevere
			n.setValue( replace( n.getValue(), "\\\"", "\\\\\"") );
		}
	}


	/**
 	 * Move a block of statements to a separate rule.
 	 *
 	 * This is used to simplify the handling of the generated code.
 	 * Blocks of nested statements and the body of repeat statements
 	 * are handled this way.
 	 *
 	 * The body of a group is isolated and placed in its own internal
 	 * method. This method is then called from the original location
 	 * like any other rule.
 	 *
 	 * @param n node containing statement block to move
 	 * @param root root node of parse tree
 	 * @param nodeName name to use for newly created rule node.
 	 */
	private void isolateRule( Node n, Node root, String nodeName ) {
		// Create a new rule node containing the child nodes of the
		// group.
		Node group = new Node("rule","");

		// Add an internal rule-modifier here, so that we can
		// hide the rule later on in the generated output.
		group.set("rule_modifier").addChild("string","skip");

		group.set( "label", nodeName ); 
		group.addChildren( n );

		// Add created node to the parse tree
		root.get("language").addChild(group);

		// Replace group node with a new label node indicating a call to the 
		// internal method
		// Replace statement with call
		n.setKey( "label" );
		n.setValue( nodeName );
	}

	/**
 	 * Make internal rules for groups within rules.
 	 *
 	 * The intention is to simplify the handling of the generated code.
 	 */
	private void translateGroups( State state ) {
		Vector res =  state.getCurNode().findNodes( "group" );
		int count = 1;

		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String nodeName = "group" + count;
			count++;

			// Create a new rule node containing the child nodes of the
			// group.
			isolateRule( n, state.getCurNode(), nodeName );
		}
	}


	/**
 	 * Make internal rules for repeat blocks within rules.
 	 *
 	 * The intention is to simplify the handling of the generated code.
 	 */
	private void translateRepeats( State state ) {
		Vector res =  state.getCurNode().findNodes( "repeat" );
		int count = 1;

		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String nodeName = "repeat" + count;
			count++;

			// Create a new rule node containing the child nodes of the
			// repeat block.
			isolateRule( n, state.getCurNode(), nodeName );

			// Add a postfix so that calls to this internal rule
			// will be repeated
			n.getParent().addChild( "postfix", "*");
		}
	}


	/**
 	 * Isolate postfixed statements in multiple alternatives as groups.
 	 *
 	 * Scan for statements with repeat postfixes in an alternative group.
 	 * If found, make a separate group for this statement.
 	 *
 	 * This method must be called before translateGroups().
 	 */
	private void translateRepeatPostfix(State state) {
		Vector res =  state.getCurNode().findNodes( "alternative" );

		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);
	
			// We're only interested in multiple alternatives	
			if( n.numChildren() > 1 ) {
				//Check underlying statements for postfixes
				for( int j = 0;  j < n.numChildren(); ++j ) {
					// Assumption: nodes are all of type statement.
					Node child = n.get(j);
			
					if ( !child.get( "postfix" ).isNull() ) {
						// Found one. Replace with group
						Node g = new Node("group", "");
						g.set("statements")
						 .set("alternative")
						 .set("statement")
						 .addChildren(child);

						child.addChild(g);
						// translateGroups() will handle the isolation of the rule.
					}
				}
			}
		}
	}


	protected String getWhileConstruct() {
		return "do; while (";
	}


	protected String makeCall(Node child) {
		String call = null;
		if ( "literal".equals( child.getKey() ) 
			|| "literal_symbol".equals( child.getKey() ) ) {
			call = "parseString( \"" + child.getValue() + "\", state"; 
		} else if ( "charset".equals( child.getKey() ) ) {
			call = "parseCharset( " + child.getValue() + ", state"; 
		} else if ( "label".equals( child.getKey() ) ) {
			call = "s( \"" + child.getValue() + "\", state"; 
		}

		return call;
	}

	private void translateSingleStatement( State state ) throws ParseException {
		Vector res =  state.getCurNode().findNodes( "statement" );

		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);
			String repeat = "";

			int num_children= n.numChildren();
			int first_child = 0;
			boolean isNot = false;

			// Statement should contain:
			//  - optional not operator
			//  - single child of type label, charset or literal, OR group
			//  - Optionally repeat postfix

			if ( n.get(0).getKey() == "not" ) {
				Util.info( "not-operator detected");
				isNot = true;
				
				// Check and translate rest of statements
				num_children--;
				first_child++;
			}

			if ( num_children == 2  && !n.get( "postfix" ).isNull() ) {
					repeat = n.get("postfix").getValue();
			} else if ( num_children > 1 ) {
				//TODO: except statement not handled yet. Implementation handling hereof

				String errMsg = "Too many child nodes detected for statement. " +
					"There should be one, with at most one postfix node.";

				throw new ParseException( errMsg );
			} else if ( num_children == 0 ) {
				String errMsg = "No child nodes detected for statement. " +
					" There should be one, with at most one postfix node.";

				throw new ParseException( errMsg );
			} 

			// TODO: assert that statement and postfix have correct order at this point
			Node child = n.get( first_child );

			String param1 = null;
			String param2 = null;
			String call = makeCall(child);
			if ( call == null) continue;

			// TODO: add whitespace handling for following blocks
			if ( !"".equals(repeat) ) {
				if ( "literal_symbol".equals( child.getKey() ) ) {
					throw new ParseException("Can't handle repeat postfixes on literal_symbols.");
				}

				if ( isNot ) {
					throw new ParseException("Can't combine not-operator with repeat postfixes (yet).");
				}

				if ( "?".equals( repeat ) ) {
					// No problem, just don't throw
					// We signal this by  adding a param as a child
					// to the call node.
					param1 = "false";
				} else if ( "*".equals( repeat ) ) {
					// Terminating brace gets added during generation
					call = getWhileConstruct() + call;
				} else if ( "+".equals( repeat ) ) {
					// Do first call separately with throw
					// Terminating brace gets added during generation
					call = call + ", true); " + getWhileConstruct() + call;
				}
			}


			// For a literal symbol, no output should be generated
			// after parsing. Following code takes care of that.
 			//I take it back - they SHOULD be present in output 
 				
			if ( "literal_symbol".equals( child.getKey() ) ) {
				// Enable ignore
				param2 = "true";
			}


			// Replace statement with call
			n.setKey( "call" );
			n.setValue( call );
			n.removeChildren();
			if ( param1 != null ) n.addChild( "param1", param1 );
			if ( param2 != null ) n.addChild( "param2", param2 );
			if ( isNot ) n.addChild( "not", "" );
		}
	}


	/**
 	 * Rename WS-rule internally
 	 *
 	 * WS (whitespace) has special handling within the parser. Using
 	 * the rule directly works, but does not register the WS nodes 
 	 * correctly in the parse tree. For this reason, the WS-rule needs
 	 * to be renamed.
 	 */ 
	void translateWS( State state ) {
		//Detect presence of WS rule
		Vector res =  state.getCurNode().findNodes( "rule" );

		for( int i = 0;  i < res.size(); ++i ) {
			Node n = (Node) res.get(i);

			String ruleName = n.get("label").getValue();

			if ( "WS".equals( ruleName ) ) {
				Util.info("WS rule detected; renaming.");

				n.get("label").setValue("WS_intern");
			}
		}
		
	}


	/**
 	 * Perform translation steps on the nodes which were generated
 	 * after a succesful parse.
 	 *
 	 * <p><pre>
	 * <b>TODO:</b> 
	 *
	 * - Add whitespace handling
	 * - Need to sort out the final two optional parameters.
	 * </pre></p>
	 */
	public void translate( State state ) throws ParseException {
		Vector res;

		setWSFlags(state);

		translateWS(state);

//		removeEmptyStatements( state );
		translateCharsets( state);
		translateLiterals( state);

		translateRepeatPostfix( state);
		translateGroups( state );
		translateRepeats( state );
		translateSingleStatement( state );
	}
}

