/**
 * $Id$
 *
 */
package nl.axizo.parser;

public class State {
	private int     depth       = 0;
	private int     curpos      = 0;
	private int     errpos      = -1;
	private String  errmethod   = null;
	private State	errstate	= null;
	private Node    curNode;
	private boolean skipCurrent = false;

	public State() {
		curNode = new Node();
	}

	public State(int curpos, String name, int depth ) {
		this.depth = depth;
		this.curpos = curpos;
		curNode = new Node( name, "");
	}

	public State copy( String name ) {
		return new State( curpos, name, depth + 1 );
	}

	public void success( State state, boolean ignore ) {
		curpos = state.curpos;

		if ( !ignore) {
			if ( state.getSkipCurrent() ) {
				// Store the children of this node instead of the Node itself.
				curNode.addChildren( state.curNode );
			} else {
				curNode.addChild( state.curNode );
			}
		}
	}

	public void matched( String value, String key, boolean ignore ) {
		curpos += value.length();
		if ( !ignore ) {
			curNode.addChild( key, value);
		}
	}

	public void matched( String value, String key ) {
		matched( value, key, false);
	}


	/**
 	 * Flag an error situation.
 	 *
 	 * If the passed state instance already contains error info,
 	 * copy that, otherwise use the current position within the
 	 * parsed data and the passed method to set the error.
 	 */
	public void setError( State state, String method ) {
		if ( state.getErrorPos() != -1 ) {
			errpos = state.getErrorPos();
			errmethod = state.getErrorMethod();
		} else {
			errpos = state.getCurpos();
			errmethod = method;
		}
		
		// Keep hold of state with error, so that we can generate
		// node output later on
		errstate = state;
	}

	public int    getCurpos()      { return curpos; }
	public Node   getCurNode()     { return curNode; }
	public int    getErrorPos()    { return errpos; };
	public String getErrorMethod() { return errmethod; }
	public int    getDepth()       { return depth; }

	public void setSkipCurrent(boolean val) { skipCurrent = val; }
	public boolean getSkipCurrent() { return skipCurrent; }

	public boolean hasErrors() { 
		// Succesful completion sets the error pos to the end of file.
		// Need to take that into account
		return getErrorPos() != getCurpos() && getErrorPos() != -1; 
	}


	/**
 	 *  Return a text representation of the state tree contained in 
 	 *  this state.
 	 *
 	 *  If an error has been flagged, the output is delegated to
 	 *  the stored state in which the error occured.
 	 *
 	 *  @param showFirstTwoLines if true, show only first two lines of node values.
 	 *  						 Otherwise, show entire value.
 	 *
 	 *  @return textual representation of node tree.
 	 */
	public String getOutput(boolean showFirstTwoLines) {
		State state = this;

		if ( hasErrors() ) {
			state = errstate;
		}

		return state.getCurNode().show( showFirstTwoLines );
	}
}
