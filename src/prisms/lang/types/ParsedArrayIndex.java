/* ParsedArrayIndex.java Created Nov 16, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.ParseMatch;
import prisms.lang.ParsedItem;
import prisms.lang.PrismsParser;

/** Represents an array index operation */
public class ParsedArrayIndex extends Assignable {
	private ParsedItem theArray;

	private ParsedItem theIndex;

	@Override
	public void setup(PrismsParser parser, ParsedItem parent, ParseMatch match) throws ParseException {
		super.setup(parser, parent, match);
		theArray = parser.parseStructures(this, getStored("array"))[0];
		theIndex = parser.parseStructures(this, getStored("index"))[0];
	}

	/** @return The array that is being indexed */
	public prisms.lang.ParsedItem getArray() {
		return theArray;
	}

	/** @return The index that is being retrieved */
	public prisms.lang.ParsedItem getIndex() {
		return theIndex;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[] {theArray, theIndex};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(dependent == theArray) {
			theArray = toReplace;
		} else if(dependent == theIndex)
			theIndex = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		return theArray + "[" + theIndex + "]";
	}
}
