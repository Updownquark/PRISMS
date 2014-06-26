package prisms.lang.types;

import prisms.lang.ParsedItem;
import prisms.lang.PrismsParser;

/** Represents a commented text block */
public class ParsedComment extends NoOpItem {
	private String theContent;

	@Override
	public void setup(PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theContent = getStored("content").text;
	}

	/** @return The comment's content */
	public String getContent() {
		return theContent;
	}
}
