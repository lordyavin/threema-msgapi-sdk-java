/*
 *  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema Gateway Java SDK
 * This SDK allows for preparing, sending, and receiving Threema messages via Threema Gateway.
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

package ch.threema.apitool.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.EndianUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import ch.threema.apitool.utils.DataUtils;
import ch.threema.apitool.types.QuotePart;
import ch.threema.apitool.exceptions.BadMessageException;

import ch.threema.apitool.serializers.LocationMessageSerializer;

import static ch.threema.apitool.utils.StringUtils.toIndentedString;

/**
 * A Location Message
 */
@javax.annotation.Generated(value = "msgapi-sdk-codegen",
				date = "2024-03-15T13:44:24.474090431+00:00")
public class LocationMessage extends ThreemaMessage {
	public static final int TYPE_CODE = 0x10;


	private final String latitude;
	private final String longitude;
	private final Float accuracy;
	private final String poiName;
	private final String address;

	public LocationMessage(String latitude, String longitude, Float accuracy, String poiName,
					String address) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
		this.poiName = poiName;
		this.address = address;
	}

	/**
	 * The geographic latitude
	 *
	 * @return latitude
	 **/
	public String getLatitude() {
		return latitude;
	}

	/**
	 * The geographic longitude
	 *
	 * @return longitude
	 **/
	public String getLongitude() {
		return longitude;
	}

	/**
	 * The location accuracy
	 *
	 * @return accuracy
	 **/
	public Float getAccuracy() {
		return accuracy;
	}

	/**
	 * The location name
	 *
	 * @return poiName
	 **/
	public String getPoiName() {
		return poiName;
	}

	/**
	 * The location address
	 *
	 * @return address
	 **/
	public String getPoiAddress() {
		return address;
	}



	@Override
	public int getTypeCode() {
		return TYPE_CODE;
	}

	@Override
	public byte[] getData() throws BadMessageException {
		String locationString;
		if (accuracy == null) {
			locationString = String.format("%s,%s", latitude, longitude);
		} else {
			locationString = String.format("%s,%s,%f", latitude, longitude, accuracy);
		}

		if (poiName != null) {
			locationString += "\n" + poiName;
		}

		if (address != null) {
			locationString += "\n" + address.replace("\n", "\\n");
		}

		return locationString.getBytes(StandardCharsets.UTF_8);

	}

	public static LocationMessage fromString(byte[] data, int realDataLength)
					throws BadMessageException {
		LocationMessageSerializer.validate(data, realDataLength);
		return LocationMessageSerializer.deserialize(data, realDataLength);

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return Objects.hash(latitude, longitude, accuracy, poiName, address, super.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class LocationMessage {\n");
		sb.append("    latitude: ").append(toIndentedString(getLatitude())).append("\n");
		sb.append("    longitude: ").append(toIndentedString(getLongitude())).append("\n");
		sb.append("    accuracy: ").append(toIndentedString(getAccuracy())).append("\n");
		sb.append("    poiName: ").append(toIndentedString(getPoiName())).append("\n");
		sb.append("    address: ").append(toIndentedString(getPoiAddress())).append("\n");

		sb.append("    ").append(toIndentedString(super.toString())).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
