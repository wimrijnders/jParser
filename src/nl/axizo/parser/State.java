/**
 * $Id$
 *
 * TODO:
 * =====
 *
 * - Check if internal ignore switch is really needed.
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
	private boolean ignoreCurrent = false;

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

		if ( !(ignore || state.ignoreCurrent) ) {
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
		if ( !(ignore || ignoreCurrent) ) {
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
		// node output later on.
		// TODO: This is probably not very useful. Check if it 
		//       is necessary.
		errstate = state;
	}


	/**
	 * Flag a general error situation.
	 *
	 * This call is used to set error situations which fall outside
	 * of the parsing methods. Specifically, this call is used when 
	 * parsing completes before the end of the buffer is reached.
	 */
	public void setError(String method) {
		// HACK: hasErrors checks if errpos and curpos are equal
		// the -1 compensated for this.
		errpos = getCurpos() -1;
		errmethod = method;
	}

	public int    getCurpos()      { return curpos; }
	public Node   getCurNode()     { return curNode; }
	public int    getErrorPos()    { return errpos; };
	public String getErrorMethod() { return errmethod; }
	public int    getDepth()       { return depth; }

	public void setSkipCurrent(boolean val) { skipCurrent = val; }
	public boolean getSkipCurrent() { return skipCurrent; }
	public void setIgnoreCurrent(boolean val) { ignoreCurrent = val; }
	public boolean getIgnoreCurrent() { return ignoreCurrent; }

	public boolean hasErrors() { 
		// Succesful completion sets the error pos to the end of file.
		// Need to take that into account. Can't be done here, because
		// the buffer is not known within a state instance.
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

		if ( hasErrors() && errstate != null ) {
			state = errstate;
		}

		return state.getCurNode().show( showFirstTwoLines );
	}
}
