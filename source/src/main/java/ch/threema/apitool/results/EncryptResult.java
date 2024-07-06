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

package ch.threema.apitool.results;

/**
 * Result of a data encryption
 */
public class EncryptResult {
	private final byte[] result;
	private final byte[] secret;
	private final byte[] nonce;

	public EncryptResult(byte[] result, byte[] secret, byte[] nonce) {
		this.result = result;
		this.secret = secret;
		this.nonce = nonce;
	}

	/**
	 * @return the encrypted data
	 */
	public byte[] getResult() {
		return this.result;
	}

	/**
	 * @return the size (in bytes) of the encrypted data
	 */
	public int getSize() {
		return this.result.length;
	}

	/**
	 * @return the nonce that was used for encryption
	 */
	public byte[] getNonce() {
		return this.nonce;
	}

	/**
	 * @return the secret that was used for encryption (only for symmetric encryption, e.g. files)
	 */
	public byte[] getSecret() {
		return secret;
	}
}