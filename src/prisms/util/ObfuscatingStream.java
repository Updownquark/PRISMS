/**
 * ObfuscatingStream.java Created Apr 22, 2009 by Andrew Butler, PSL
 */
package prisms.util;

import java.io.*;

/**
 * A utility class that allows for easy simple obfuscation of streamed data
 */
public class ObfuscatingStream
{
	/**
	 * A class that obfuscates data as it is written to a binary stream
	 */
	public static class ObfuscatingOutputStream extends OutputStream
	{
		private OutputStream theOS;

		private int theWarp;

		private int theCount;

		ObfuscatingOutputStream(OutputStream os)
		{
			theOS = os;
			theWarp = -1;
		}

		/**
		 * @see java.io.OutputStream#write(int)
		 */
		@Override
		public void write(int b) throws IOException
		{
			if(theWarp < 0)
			{
				theWarp = (int) (Math.random() * 8);
				theOS.write(theWarp);
			}
			int shift = (theWarp + theCount) % 8;
			theCount++;
			int toWrite = b & 0xff;
			toWrite = ((toWrite >> shift) | (toWrite << (8 - shift))) & 0xff;
			theOS.write(toWrite);
		}
	}

	/**
	 * A class the unobfuscates data that was obfuscated by an {@link ObfuscatingOutputStream} as it
	 * is read from an input stream
	 */
	public static class UnobfuscatingInputStream extends InputStream
	{
		private InputStream theIS;

		private int theWarp;

		private int theCount;

		UnobfuscatingInputStream(InputStream is)
		{
			theIS = is;
			theWarp = -1;
		}

		/**
		 * @see java.io.InputStream#read()
		 */
		@Override
		public int read() throws IOException
		{
			if(theWarp < 0)
				theWarp = theIS.read();
			int read = theIS.read();
			if(read < 0)
				return read;
			int shift = (theWarp + theCount) % 8;
			theCount++;
			int ret = read & 0xff;
			ret = ((ret << shift) | (ret >> (8 - shift))) & 0xff;
			return ret;
		}
	}

	/**
	 * Obfuscates an output stream
	 * 
	 * @param os The binary output stream for this utility to write obfuscated data to
	 * @return The output stream for the calling method to write unobfuscated data
	 */
	public static OutputStream obfuscate(OutputStream os)
	{
		return new ObfuscatingOutputStream(os);
	}

	/**
	 * Unobfuscates an input stream
	 * 
	 * @param is The binary input stream for this utililty to read obfuscated data from
	 * @return The input stream for the calling method to read the unobfuscated data from
	 */
	public static InputStream unobfuscate(InputStream is)
	{
		return new UnobfuscatingInputStream(is);
	}
}
