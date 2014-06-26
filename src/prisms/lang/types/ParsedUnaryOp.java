/* ParsedUnaryOp.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.ParsedItem;

/** An operation on a single operand */
public class ParsedUnaryOp extends prisms.lang.ParsedItem {
	private String theName;

	private boolean isPrefix;

	private prisms.lang.ParsedItem theOperand;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match) throws ParseException {
		super.setup(parser, parent, match);
		try {
			theName = getStored("name").text;
		} catch(NullPointerException e) {
			throw new ParseException("No name for configured binary operation: " + getMatch().config, getRoot().getFullCommand(),
				getMatch().index);
		}
		isPrefix = getDeepStoreIndex("op") < getDeepStoreIndex("name");
		for(prisms.lang.ParseMatch m : match.getParsed())
			if(m.config.getName().equals("op"))
				theOperand = parser.parseStructures(this, m)[0];
		if(getMatch().isComplete() && theOperand == null)
			throw new ParseException("No operand for configured unary operation: " + getMatch().config, getRoot().getFullCommand(),
				getMatch().index);
	}

	/** @return The name of the operation */
	public String getName() {
		return theName;
	}

	/** @return The operand of the operation */
	public prisms.lang.ParsedItem getOp() {
		return theOperand;
	}

	/** @return Whether this operator occurred before or after its operand */
	public boolean isPrefix() {
		return isPrefix;
	}

	@Override
	public prisms.lang.ParsedItem [] getDependents() {
		return new prisms.lang.ParsedItem[] {theOperand};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theOperand == dependent)
			theOperand = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		if(isPrefix)
			return theName + theOperand.toString();
		else
			return theOperand.toString() + theName;
	}
}
