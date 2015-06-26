package prisms.lang2;

import prisms.lang.ParseMatch;

public interface PrismsParser<D> {
	String getName();

	Iterable<ParseMatch> matches(NavigableStream<D> stream);
}
