package prisms.lang;

import prisms.arch.PrismsConfig;

/** An interface for a debugger to be notified of parsing events */
public interface PrismsParserDebugger {
	/**
	 * Called when the debugger is installed in a parser
	 *
	 * @param parser The parser the debugger is being installed in
	 */
	void init(PrismsParser parser);

	/**
	 * Called at the start of parsing a sequence
	 *
	 * @param text The sequence to parse
	 */
	void start(CharSequence text);

	/**
	 * Called when the parsing of a sequence has finished successfully
	 *
	 * @param text The sequence that was parsed
	 * @param matches The matches parsed from the sequence
	 */
	void end(CharSequence text, ParseMatch [] matches);

	/**
	 * Called when the parsing of a sequence fails
	 *
	 * @param text The sequence whose parsing was attempted
	 * @param matches The matches parsed from the sequence before the failure
	 * @param e The exception that is the source of the failure
	 */
	void fail(CharSequence text, ParseMatch [] matches, ParseException e);

	/**
	 * Called before attempting to parse a subsequence according to a particular parse config
	 *
	 * @param text The full sequence
	 * @param index The index of the subsequence that will be parsed
	 * @param op The parse config that parsing will attempt to use
	 */
	void preParse(CharSequence text, int index, PrismsConfig op);

	/**
	 * Called after attempting to parse a subsequence
	 *
	 * @param text The full sequence
	 * @param startIndex The index of the subsequence whose parsing was attempted
	 * @param op The parse config that parsing attempted to use
	 * @param match The match resulting from the parsing, or null if the match could not be made
	 */
	void postParse(CharSequence text, int startIndex, PrismsConfig op, ParseMatch match);

	void matchDiscarded(ParseMatch match);

	void usedCache(ParseMatch match);
}
