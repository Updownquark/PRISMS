package prisms.lang2;

public abstract class NavigableStream<D> {
	protected class Chunk {
		private final int theChunkIndex;
		private boolean isInitialized;
		private D theData;
		private int theLength;
		private Chunk theNextChunk;

		public Chunk(int chunkIndex) {
			theChunkIndex = chunkIndex;
		}

		protected int get(D data, int srcPos, int destPos, int length) {
			if(!isInitialized) {
				isInitialized = true;
				init();
			}
			if(srcPos > theLength)
				throw new IndexOutOfBoundsException();
			if(srcPos + length > theLength)
				length = theLength - srcPos;
			copy(theData, srcPos, data, destPos, length);
			return length;
		}

		private void init() {
			theData = getNextData(theChunkSize);
			theLength = getLength(theData);
			if(theLength == theChunkSize)
				theNextChunk = new Chunk(theChunkIndex + 1);
		}
	}

	private final int theChunkSize;
	private final Chunk theChunk;
	private final int theChunkStart;
	private int thePosition;

	public NavigableStream(int chunkSize) {
		theChunkSize = chunkSize;
		theChunk = new Chunk(0);
		theChunkStart = 0;
	}

	private NavigableStream(Chunk chunk, int index) {
		theChunkSize = -1;
		theChunk = chunk;
		theChunkStart = index;
	}

	public Object getId(){
		return theChunk.theChunkIndex + theChunkStart + thePosition;
	}

	public NavigableStream<D> substream() {
	}

	protected abstract D getNextData(int length);

	protected abstract int getLength(D data);

	protected void copy(D from, int srcPos, D to, int destPos, int length) {
		System.arraycopy(from, srcPos, to, destPos, length);
	}
}
