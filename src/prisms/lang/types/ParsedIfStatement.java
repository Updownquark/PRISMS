/* ParsedIfStatement.java Created Nov 17, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents an if/else if/.../else structure */
public class ParsedIfStatement extends ParsedItem {
	private ParsedItem [] theConditions;

	private ParsedStatementBlock [] theContents;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theConditions = parser.parseStructures(this, getAllStored("condition"));
		theContents = new ParsedStatementBlock[0];
		java.util.ArrayList<ParsedItem> conditions = new java.util.ArrayList<ParsedItem>();
		ParsedItem content = null;
		for(prisms.lang.ParseMatch m : getAllStored("condition", "content", "terminal")) {
			if("condition".equals(m.config.get("storeAs"))) {
				if(!conditions.isEmpty()) {
					if(content == null)
						theContents = org.qommons.ArrayUtils.add(theContents, new ParsedStatementBlock(parser, this, m));
					content = null;
				}
				conditions.add(parser.parseStructures(this, m)[0]);
			} else if("terminal".equals(m.config.get("storeAs"))) {
				if(content == null)
					theContents = org.qommons.ArrayUtils.add(theContents, new ParsedStatementBlock(parser, this, m));
				content = null;
			} else if("content".equals(m.config.get("storeAs"))) {
				content = parser.parseStructures(this, m)[0];
				if(!(content instanceof ParsedStatementBlock))
					content = new ParsedStatementBlock(parser, this, content.getMatch(), content);
				theContents = org.qommons.ArrayUtils.add(theContents, (ParsedStatementBlock) content);
			}
		}
		theConditions = conditions.toArray(new ParsedItem[conditions.size()]);
	}

	/** @return The conditions in this if statement as if/else if/else if/... */
	public ParsedItem [] getConditions() {
		return theConditions;
	}

	/** @return Whether this if statement has a terminal block (an else without an if) */
	public boolean hasTerminal() {
		return theContents.length > theConditions.length;
	}

	@Override
	public ParsedItem [] getDependents() {
		ParsedItem [] ret = new ParsedItem[theConditions.length + theContents.length];
		for(int i = 0; i < theConditions.length; i++) {
			ret[i * 2] = theConditions[i];
			ret[i * 2 + 1] = theContents[i];
		}
		if(theContents.length > theConditions.length)
			ret[theConditions.length * 2] = theContents[theConditions.length];
		return ret;
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		for(int i = 0; i < theContents.length; i++) {
			if(i < theConditions.length && theConditions[i] == dependent) {
				theConditions[i] = toReplace;
				return;
			}
			if(theContents[i] == dependent) {
				if(toReplace instanceof ParsedStatementBlock) {
					theContents[i] = (ParsedStatementBlock) toReplace;
					return;
				} else
					throw new IllegalArgumentException("Cannot replace block of if/else statement with "
						+ toReplace.getClass().getSimpleName());
			}
		}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	/**
	 * @param condition The index of the condition to get the contents for, or the length of the conditions array to get the contents of the
	 *            terminal block
	 * @return The contents of the condition or terminal block specified
	 */
	public ParsedStatementBlock getContents(int condition) {
		return theContents[condition];
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < theConditions.length; i++) {
			ret.append("if(").append(theConditions[i]).append(")\n");
			ret.append(theContents[i]);
		}
		if(theContents.length > theConditions.length)
			ret.append("else\n").append(theContents[theConditions.length]);
		return ret.toString();
	}
}
