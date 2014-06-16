/* ParsedArrayInitializer.java Created Nov 16, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents the creation of an array instance */
public class ParsedArrayInitializer extends prisms.lang.ParsedItem {
	private ParsedType theType;

	private prisms.lang.ParsedItem [] theSizes;

	private prisms.lang.ParsedItem [] theElements;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match)
		throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theType = (ParsedType) parser.parseStructures(this, getStored("type"))[0];
		int dim = getAllStored("startDimension").length;
		for(int i = 0; i < dim; i++)
			theType.addArrayDimension();
		theSizes = parser.parseStructures(this, getAllStored("size"));
		theElements = parser.parseStructures(this, getAllStored("element"));
	}

	/** @return The base type of the array */
	public ParsedType getType() {
		return theType;
	}

	/** @return The sizes that are specified for this array */
	public prisms.lang.ParsedItem [] getSizes() {
		return theSizes;
	}

	/** @return The elements specified for this array */
	public prisms.lang.ParsedItem [] getElements() {
		return theElements;
	}

	@Override
	public prisms.lang.ParsedItem [] getDependents() {
		prisms.lang.ParsedItem [] ret;
		if(theSizes.length > 0) {
			ret = new prisms.lang.ParsedItem[theSizes.length + 1];
			ret[0] = theType;
			System.arraycopy(theSizes, 0, ret, 1, theSizes.length);
		} else {
			ret = new prisms.lang.ParsedItem[theElements.length + 1];
			ret[0] = theType;
			System.arraycopy(theElements, 0, ret, 1, theElements.length);
		}
		return ret;
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		for(int i = 0; i < theSizes.length; i++)
			if(theSizes[i] == dependent) {
				theSizes[i] = toReplace;
				return;
			}
		for(int i = 0; i < theElements.length; i++)
			if(theElements[i] == dependent) {
				theElements[i] = toReplace;
				return;
			}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("new ").append(theType);
		for(prisms.lang.ParsedItem size : theSizes)
			ret.append('[').append(size).append(']');
		if(theElements.length > 0) {
			ret.append('{');
			for(int i = 0; i < theElements.length; i++) {
				if(i > 0)
					ret.append(", ");
				ret.append(theElements[i]);
			}
			ret.append('}');
		}
		return ret.toString();
	}
}
