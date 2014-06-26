package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a block of statements */
public class ParsedStatementBlock extends ParsedItem {
	private ParsedItem [] theContents;

	/** Default constructor. Used when {@link #setup(prisms.lang.PrismsParser, ParsedItem, prisms.lang.ParseMatch)} will be called later */
	public ParsedStatementBlock() {
	}

	/**
	 * Pre-setup constructor. Used so that {@link #setup(prisms.lang.PrismsParser, ParsedItem, prisms.lang.ParseMatch)} does not need to be
	 * called subsequently.
	 * 
	 * @param parser The parser that parsed this structure's contents
	 * @param parent The parent structure
	 * @param match The match that this structure is to identify with
	 * @param contents The content statements of the block
	 * @see ParsedItem#setup(prisms.lang.PrismsParser, ParsedItem, prisms.lang.ParseMatch)
	 */
	public ParsedStatementBlock(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match, ParsedItem... contents) {
		try {
			super.setup(parser, parent, match);
		} catch(prisms.lang.ParseException e) {
			throw new IllegalStateException("ParseException should not come from super class", e);
		}
		theContents = contents;
	}

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theContents = parser.parseStructures(this, getAllStored("content"));
	}

	/** @return The contents of this statement block */
	public ParsedItem [] getContents() {
		return theContents;
	}

	@Override
	public ParsedItem [] getDependents() {
		return theContents;
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		for(int i = 0; i < theContents.length; i++) {
			if(theContents[i] == dependent) {
				theContents[i] = toReplace;
				return;
			}
		}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("{\n");
		for(ParsedItem content : theContents)
			ret.append(content).append('\n');
		ret.append("}\n");
		return ret.toString();
	}
}
