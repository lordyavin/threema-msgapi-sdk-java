/*
 *  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema Gateway Java SDK
 * This SDK allows for preparing, sending and receiving of Threema Messages via Threema Gateway.
 *
 * The MIT License (MIT)
 * Copyright (c) 2015-2024 Threema GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE
 *
 *
 *
 *
 */

package ch.threema.apitool.utils;

import ch.threema.apitool.exceptions.InvalidHexException;
import ch.threema.apitool.exceptions.InvalidKeyException;
import ch.threema.apitool.types.Key;
import ch.threema.apitool.types.QuotePart;

import java.io.*;
import java.util.regex.Pattern;

public class DataUtils {

	public static final String QUOTE_PATTERN = "^> quote #([0-9a-f]{16})(?:\\r?\\n){2}(.+)$";

	/**
	 * Convert a string in hexadecimal representation to a byte array.
	 * <p>
	 * Whitespace (RegEx \s) is stripped before decoding, but if other invalid characters are
	 * contained, an error is thrown.
	 *
	 * @param s hex string
	 * @return decoded byte array
	 * @throws InvalidHexException if the string is not a valid hex string
	 */
	public static byte[] hexStringToByteArray(String s) throws InvalidHexException {
		String sc = s.replaceAll("\\s", "");
		int len = sc.length();
		if (len % 2 != 0) {
			throw new InvalidHexException("Hex string length is not divisible by 2");
		}
		if (sc.matches(".*[^0-9a-fA-F].*")) {
			throw new InvalidHexException("Hex string contains non-hex characters");
		}
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(sc.charAt(i), 16) << 4)
							+ Character.digit(sc.charAt(i + 1), 16));
		}
		return data;
	}

	/**
	 * Convert a byte array into a hexadecimal string (lowercase).
	 *
	 * @param bytes the bytes to encode
	 *
	 * @return hex encoded string
	 */
	public static String byteArrayToHexString(byte[] bytes) {
		final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
				'd', 'e', 'f'};
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] longToByteArrayBigEndian(long value) {
		byte[] result = new byte[8];
		for (int i = 7; i >= 0; i--) {
			result[i] = (byte) (value & 0xFF);
			value >>= 8;
		}
		return result;
	}

	public static long byteArrayToLongBigEndian(final byte[] bytes) {
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result <<= 8;
			result |= (bytes[i] & 0xFF);
		}
		return result;
	}

	/**
	 * Read hexadecimal data from a file and return it as a byte array.
	 *
	 * @param inFile input file
	 *
	 * @return the decoded data
	 *
	 * @throws java.io.IOException
	 */
	public static byte[] readHexFile(File inFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		byte[] data = hexStringToByteArray(br.readLine().trim());
		br.close();
		return data;
	}

	/**
	 * Write a byte array into a file in hexadecimal format.
	 *
	 * @param outFile output file
	 * @param data the data to be written
	 */
	public static void writeHexFile(File outFile, byte[] data) throws IOException {
		FileWriter fw = new FileWriter(outFile);
		fw.write(byteArrayToHexString(data));
		fw.write('\n');
		fw.close();
	}

	/**
	 * Read an encoded key from a file and return it as a key instance.
	 *
	 * @param inFile input file
	 *
	 * @return the decoded key
	 *
	 * @throws java.io.IOException
	 */
	public static Key readKeyFile(File inFile) throws IOException, InvalidKeyException {
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		String encodedKey = br.readLine().trim();
		br.close();
		return Key.decodeKey(encodedKey);
	}

	/**
	 * Read an encoded key from a file and return it as a key instance.
	 *
	 * @param inFile input file
	 * @param expectedKeyType validates the key type (private or public)
	 * @return the decoded key
	 * @throws java.io.IOException
	 */
	public static Key readKeyFile(File inFile, String expectedKeyType)
					throws IOException, InvalidKeyException {
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		String encodedKey = br.readLine().trim();
		br.close();
		return Key.decodeKey(encodedKey, expectedKeyType);
	}

	/**
	 * Write an encoded key to a file Encoded key format: type:hex_key.
	 *
	 * @param outFile output file
	 * @param key a key that will be encoded and written to a file
	 */
	public static void writeKeyFile(File outFile, Key key) throws IOException {
		FileWriter fw = new FileWriter(outFile);
		fw.write(key.encode());
		fw.write('\n');
		fw.close();
	}

	public static String extractQuote(String text, QuotePart part) {
		var pattern = Pattern.compile(QUOTE_PATTERN, Pattern.DOTALL);
		var matcher = pattern.matcher(text);

		if (matcher.matches()) {
			return matcher.group(part.ordinal() + 1);
		}

		return null;
	}
}
