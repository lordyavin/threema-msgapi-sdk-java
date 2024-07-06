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

package ch.threema.apitool.serializers;

import java.nio.charset.StandardCharsets;

import ch.threema.apitool.exceptions.BadMessageException;
import ch.threema.apitool.messages.LocationMessage;

@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2022-09-12T15:53:56.786299819+00:00")
public class LocationMessageSerializer implements CustomMessageSerializer {
	public static LocationMessage deserialize(byte[] data, int realDataLength)
					throws BadMessageException {
		String strData = new String(data, 1, realDataLength - 1, StandardCharsets.UTF_8);
		String poiName = null, address = null;
		String[] all = strData.split("\n");
		String[] first = all[0].split(",");
		String second = all.length >= 2 ? all[1] : null;
		String third = all.length == 3 ? all[2] : null;
		String lat = first[0];
		String lng = first[1];
		String accuracy = null;
		if (first.length == 3)
			accuracy = first[2];
		if (all.length == 2) {
			address = second;
		} else if (all.length == 3) {
			poiName = second;
			address = third;
		}

		if (Math.abs(Float.parseFloat(lat)) > 90.0 || Math.abs(Float.parseFloat(lng)) > 180.0) {
			throw new BadMessageException("Invalid coordinate values in group location message");
		}

		return new LocationMessage(lat, lng, accuracy != null ? Float.parseFloat(accuracy) : null,
						poiName, address);
	}

	public static byte[] extractJson(byte[] data, int realDataLength) {
		return new byte[0];
	}

	public static void validate(byte[] data, int realDataLength) throws BadMessageException {
		if (realDataLength < 4) {
			throw new BadMessageException(
							"Bad length (" + realDataLength + ") for location message");
		}
		String strData = new String(data, 1, realDataLength - 1, StandardCharsets.UTF_8);
		String[] all = strData.split("\n");
		if (all.length > 3) {
			throw new BadMessageException();
		}
	}
}
