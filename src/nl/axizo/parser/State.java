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

/*
	public boolean eol() throws ParseException {
		if ( curpos > buffer.length() ) { throw new ParseException(); }

		return curpos == buffer.length();
	}
*/
	public void setError( State state, String method ) {
		if ( state.getErrorPos() != -1 ) {
			errpos = state.getErrorPos();
			errmethod = state.getErrorMethod();
		} else {
			errpos = state.getCurpos();
			errmethod = method;
		}
	}

	public int    getCurpos()      { return curpos; }
	public Node   getCurNode()     { return curNode; }
	public int    getErrorPos()    { return errpos; };
	public String getErrorMethod() { return errmethod; }
	public int    getDepth()       { return depth; }

	public void setSkipCurrent(boolean val) { skipCurrent = val; }
	public boolean getSkipCurrent() { return skipCurrent; }
}
