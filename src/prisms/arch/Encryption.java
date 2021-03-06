/**
 * Encryption.java Created Mar 2, 2009 by Andrew Butler, PSL
 */
package prisms.arch;

import java.io.IOException;

/** Performs encryption and decryption for the PRISMS architecture */
public interface Encryption
{
	/** The character set used for encryption (UTF-8) */
	static java.nio.charset.Charset CHAR_SET = java.nio.charset.Charset.forName("UTF-8");

	/**
	 * Initializes the encryption with a key and parameters
	 * 
	 * @param key The encryption key data to use
	 * @param params The encryption parameters to use
	 */
	public void init(long [] key, java.util.Map<String, String> params);

	/** @return Parameters necessary to duplicate this encryption on the client side */
	public org.json.simple.JSONObject getParams();

	/**
	 * Encrypts a data string
	 * 
	 * @param text The data to encrypt
	 * @return The encrypted data
	 * @throws IOException If an error occurs encrypting the data
	 */
	public String encrypt(String text) throws IOException;

	/**
	 * Decrypts an encrypted data string
	 * 
	 * @param encrypted The data to decrypt
	 * @return The decrypted data
	 * @throws java.io.IOException If the data cannot be interpreted
	 */
	public String decrypt(String encrypted) throws IOException;

	/** Disposes of the encryption object */
	public void dispose();
}
