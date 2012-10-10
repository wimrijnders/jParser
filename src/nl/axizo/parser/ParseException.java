/**
 * Copyright 2012 Wim Rijnders <wrijnders@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *=========================================================================
 */
package nl.axizo.parser;

public class ParseException extends Exception {

	private String method = null;

	public ParseException() {
		super( );
	}
	public ParseException(String msg ) {
		super( msg );
	}

	public void   setMethod(String val ) { method = val; }
	public String getMethod() { return method; }
}
