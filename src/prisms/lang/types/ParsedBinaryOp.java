/* ParsedBinaryOp.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.ParsedItem;

/** Represents an operation on two operands */
public class ParsedBinaryOp extends prisms.lang.ParsedItem {
	private String theName;

	private prisms.lang.ParsedItem theOp1;

	private prisms.lang.ParsedItem theOp2;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match) throws ParseException {
		super.setup(parser, parent, match);
		try {
			theName = getStored("name").text;
		} catch(NullPointerException e) {
			throw new ParseException("No name for configured binary operation: " + getMatch().config, getRoot().getFullCommand(),
				getMatch().index);
		}
		for(prisms.lang.ParseMatch m : match.getParsed()) {
			if(m.config.getName().equals("op")) {
				if(theOp1 == null)
					theOp1 = parser.parseStructures(this, m)[0];
				else {
					theOp2 = parser.parseStructures(this, m)[0];
					break;
				}
			}
		}
		if(theOp1 == null)
			throw new ParseException("No op for configured binary operation: " + getMatch().config, getRoot().getFullCommand(),
				getMatch().index);
		if(getMatch().isComplete() && theOp2 == null)
			throw new ParseException("Only one op for configured binary operation: " + getMatch().config, getRoot().getFullCommand(),
				getMatch().index);
	}

	/** @return The name of the operation */
	public String getName() {
		return theName;
	}

	/** @return The first operand of the operation */
	public prisms.lang.ParsedItem getOp1() {
		return theOp1;
	}

	/** @return The second operand of the operation */
	public prisms.lang.ParsedItem getOp2() {
		return theOp2;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[] {theOp1, theOp2};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theOp1 == dependent)
			theOp1 = toReplace;
		else if(theOp2 == dependent)
			theOp2 = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		return theOp1.toString() + theName + theOp2.toString();
	}
}
