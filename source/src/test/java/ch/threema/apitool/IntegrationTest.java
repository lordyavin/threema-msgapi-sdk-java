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

package ch.threema.apitool;

import ch.threema.apitool.exceptions.InvalidKeyException;
import ch.threema.apitool.exceptions.MessageParseException;
import ch.threema.apitool.messages.*;
import ch.threema.apitool.types.FileRenderingType;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.types.Key;
import ch.threema.apitool.types.voting.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
public class IntegrationTest {

	/*
	 * Generating unit tests is difficult as there is no guarantee the expected result is generated
	 * correctly. These tests could pass erroneously when both en- and decryption are flawed in the
	 * same way.
	 */

	@Test
	public void testGroupTextMessage() throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expectedText = "> quote #a0a0a0a0a0a0a0a0\n\nTest Message";
		var expectedQuotedMessageId = "a0a0a0a0a0a0a0a0";
		var expectedQuoteText = "Test Message";

		var encryptResult = CryptTool.encryptGroupTextMessage(Common.groupId,
						"> quote #a0a0a0a0a0a0a0a0\n\nTest Message", privateKey.key, publicKey.key);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(GroupTextMessage.class, actual,
						"message is not an instance of group text message");
		Assertions.assertArrayEquals(Common.groupId.getGroupCreator(),
						((GroupTextMessage) actual).getGroupId().getGroupCreator());
		Assertions.assertArrayEquals(Common.groupId.getGroupId(),
						((GroupTextMessage) actual).getGroupId().getGroupId());
		Assertions.assertEquals(expectedText, ((GroupTextMessage) actual).getText());
		Assertions.assertEquals(expectedQuotedMessageId,
						((GroupTextMessage) actual).getQuotedMessageId());
		Assertions.assertEquals(expectedQuoteText, ((GroupTextMessage) actual).getQuoteText());
	}

	@Test
	public void testFileMessage() throws MessageParseException, InvalidKeyException {
		for (int i = 0; i < 2; i++) {
			Key privateKey = Key.decodeKey(Common.otherPrivateKey);
			Key publicKey = Key.decodeKey(Common.myPublicKey);
			var file = new File("./threema.jpg");
			var expectedCaption = i != 1 ? "My caption" : "";
			var expectedBlobId = "abcdeffffffedcba".getBytes(StandardCharsets.UTF_8);
			var expectedRenderingType = FileRenderingType.MEDIA.getValue();
			var expectedFilename = "logo.jpg";
			var expectedMimeType = "image/jpg";
			var expectedThumbnailBlobId = new byte[0];
			int expectedFileSize;
			try {
				expectedFileSize = Common.readFile(file).length;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			try {
				var encryptedData = CryptTool.encryptFileData(Common.readFile(file));

				var encryptResult = CryptTool.encryptFileMessage(
								"abcdeffffffedcba".getBytes(StandardCharsets.UTF_8), null, null,
								encryptedData.getSecret(), "image/jpg", "logo.jpg",
								Common.readFile(file).length, i != 1 ? "My caption" : null,
								FileRenderingType.MEDIA, null, null, privateKey.key, publicKey.key);

				var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
								publicKey.key, encryptResult.getNonce());

				Assertions.assertNotNull(actual);
				Assertions.assertInstanceOf(FileMessage.class, actual,
								"message is not an instance of file message");
				Assertions.assertEquals(expectedCaption, ((FileMessage) actual).getCaption());
				Assertions.assertEquals(expectedRenderingType,
								((FileMessage) actual).getRenderingType().getValue());
				Assertions.assertEquals(expectedMimeType, ((FileMessage) actual).getMimeType());
				Assertions.assertArrayEquals(expectedThumbnailBlobId,
								((FileMessage) actual).getThumbnailBlobId());
				Assertions.assertEquals(expectedFileSize, ((FileMessage) actual).getSize());
				Assertions.assertEquals(expectedFilename, ((FileMessage) actual).getFilename());
				Assertions.assertArrayEquals(expectedBlobId, ((FileMessage) actual).getBlobId());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void testGroupFileMessage() throws MessageParseException, InvalidKeyException {
		for (int i = 0; i < 2; i++) {
			Key privateKey = Key.decodeKey(Common.otherPrivateKey);
			Key publicKey = Key.decodeKey(Common.myPublicKey);
			var file = new File("./threema.jpg");
			var expectedCaption = i != 1 ? "My caption" : "";
			var expectedBlobId = "abcdeffffffedcba".getBytes(StandardCharsets.UTF_8);
			var expectedRenderingType = FileRenderingType.MEDIA.getValue();
			var expectedFilename = "logo.jpg";
			var expectedMimeType = "image/jpg";
			var expectedThumbnailBlobId = new byte[0];
			int expectedFileSize;
			try {
				expectedFileSize = Common.readFile(file).length;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			try {
				var encryptedData = CryptTool.encryptFileData(Common.readFile(file));

				var encryptResult = CryptTool.encryptGroupFileMessage(Common.groupId,
								"abcdeffffffedcba".getBytes(StandardCharsets.UTF_8), null, null,
								encryptedData.getSecret(), "image/jpg", "logo.jpg",
								Common.readFile(file).length, i != 1 ? "My caption" : null,
								FileRenderingType.MEDIA, null, null, privateKey.key, publicKey.key);

				var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
								publicKey.key, encryptResult.getNonce());

				Assertions.assertNotNull(actual);
				Assertions.assertInstanceOf(GroupFileMessage.class, actual,
								"message is not an instance of group file message");
				Assertions.assertArrayEquals(Common.groupId.getGroupCreator(),
								((GroupFileMessage) actual).getGroupId().getGroupCreator());
				Assertions.assertArrayEquals(Common.groupId.getGroupId(),
								((GroupFileMessage) actual).getGroupId().getGroupId());
				Assertions.assertEquals(expectedCaption, ((GroupFileMessage) actual).getCaption());
				Assertions.assertEquals(expectedRenderingType,
								((GroupFileMessage) actual).getRenderingType().getValue());
				Assertions.assertEquals(expectedMimeType,
								((GroupFileMessage) actual).getMimeType());
				Assertions.assertArrayEquals(expectedThumbnailBlobId,
								((GroupFileMessage) actual).getThumbnailBlobId());
				Assertions.assertEquals(expectedFileSize, ((GroupFileMessage) actual).getSize());
				Assertions.assertEquals(expectedFilename,
								((GroupFileMessage) actual).getFilename());
				Assertions.assertArrayEquals(expectedBlobId,
								((GroupFileMessage) actual).getBlobId());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@DisplayName("Should encrypt and then decrypt a BallotCreateMessage")
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testBallotCreateMessage(boolean isDisplayModeNull)
					throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expected = "Pizza";
		var expectedState = State.OPEN.getValue();
		var expectedVotingMode = VotingMode.SINGLE_CHOICE.getValue();
		var expectedDisclosureType = ResultsDisclosureType.CLOSED.getValue();
		var expectedDisplayMode = DisplayMode.LIST;

		var message = new BallotCreateMessage("BALL0TID".getBytes(StandardCharsets.UTF_8),
						"[JAVA SDK] Test Poll", State.OPEN, VotingMode.SINGLE_CHOICE,
						ResultsDisclosureType.CLOSED, 0,
						isDisplayModeNull ? null : DisplayMode.LIST,
						List.of(new BallotChoice(0, "Pizza", 0, List.of(), 0),
										new BallotChoice(1, "Ananas", 1, List.of(), 0)),
						List.of());

		var encryptResult = CryptTool.encryptBallotCreateMessage(message.getBallotId(),
						message.getDescription(), message.getState(), message.getVotingMode(),
						message.getResultsDisclosureType(), message.getOrder(),
						message.getDisplayMode(), message.getChoices(), List.of(), privateKey.key,
						publicKey.key);

		assertThatNoException().isThrownBy(message::getData);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(BallotCreateMessage.class, actual,
						"message is not an instance of ballot create message");
		Assertions.assertEquals(expected,
						((BallotCreateMessage) actual).getChoices().get(0).getName());
		Assertions.assertEquals(expectedState,
						((BallotCreateMessage) actual).getState().getValue());
		Assertions.assertEquals(expectedVotingMode,
						((BallotCreateMessage) actual).getVotingMode().getValue());
		Assertions.assertEquals(expectedDisclosureType,
						((BallotCreateMessage) actual).getResultsDisclosureType().getValue());
		Assertions.assertEquals(expectedDisplayMode,
						((BallotCreateMessage) actual).getDisplayMode());
	}

	@DisplayName("Should encrypt and then decrypt a GroupBallotCreateMessage")
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testGroupBallotCreateMessage(boolean isDisplayModeNull)
					throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expected = "Pizza";
		var expectedState = State.OPEN.getValue();
		var expectedVotingMode = VotingMode.SINGLE_CHOICE.getValue();
		var expectedDisclosureType = ResultsDisclosureType.CLOSED.getValue();
		var expectedDisplayMode = DisplayMode.LIST.getValue();

		var message = new GroupBallotCreateMessage(new GroupId("ASDFGHHJ", "*ASDFGHI"),
						"BALL0TID".getBytes(StandardCharsets.UTF_8), "[JAVA SDK] Test Poll",
						State.OPEN, VotingMode.SINGLE_CHOICE, ResultsDisclosureType.CLOSED, 0,
						isDisplayModeNull ? null : DisplayMode.LIST,
						List.of(new BallotChoice(0, "Pizza", 0, List.of(), 0),
										new BallotChoice(1, "Ananas", 1, List.of(), 0)),
						List.of());

		var encryptResult = CryptTool.encryptGroupBallotCreateMessage(message.getGroupId(),
						message.getBallotId(), message.getDescription(), message.getState(),
						message.getVotingMode(), message.getResultsDisclosureType(),
						message.getOrder(), message.getDisplayMode(), message.getChoices(),
						List.of(), privateKey.key, publicKey.key);

		assertThatNoException().isThrownBy(message::getData);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(GroupBallotCreateMessage.class, actual,
						"message is not an instance of group ballot create message");
		Assertions.assertEquals(expected,
						((GroupBallotCreateMessage) actual).getChoices().get(0).getName());
		Assertions.assertEquals(expectedState,
						((GroupBallotCreateMessage) actual).getState().getValue());
		Assertions.assertEquals(expectedVotingMode,
						((GroupBallotCreateMessage) actual).getVotingMode().getValue());
		Assertions.assertEquals(expectedDisclosureType,
						((GroupBallotCreateMessage) actual).getResultsDisclosureType().getValue());
		Assertions.assertEquals(expectedDisplayMode,
						((GroupBallotCreateMessage) actual).getDisplayMode().getValue());
	}

	@Test
	public void testGroupBallotVoteMessage() throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expectedSelection = false;
		var expectedBallotId = 1;

		var encryptResult = CryptTool.encryptGroupBallotVoteMessage(Common.groupId,
						Common.groupId.getGroupCreator(),
						"ffffffff".getBytes(StandardCharsets.UTF_8),
						List.of(new VoteChoice(0, false), new VoteChoice(1, true)), privateKey.key,
						publicKey.key);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(GroupBallotVoteMessage.class, actual,
						"message is not an instance of group ballot vote message");
		Assertions.assertArrayEquals(Common.groupId.getGroupCreator(),
						((GroupBallotVoteMessage) actual).getGroupId().getGroupCreator());
		Assertions.assertArrayEquals(Common.groupId.getGroupId(),
						((GroupBallotVoteMessage) actual).getGroupId().getGroupId());
		Assertions.assertEquals(expectedSelection,
						((GroupBallotVoteMessage) actual).getVotes().get(0).getSelected());
		Assertions.assertEquals(expectedBallotId,
						((GroupBallotVoteMessage) actual).getVotes().get(1).getBallotId());
	}

	@Test
	public void testLocationMessage() throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expectedLatitude = "47.4";
		var expectedLongitude = "8.1";
		var expectedAccuracy = Float.valueOf(10.0f);
		var expectedPoiName = "Rupperswil, A";
		var expectedPoiAddress = "Bahnhofstrasse 4, 5222 Rupperswil, Switzerland, Rupperswil";

		var encryptResult = CryptTool.encryptLocationMessage("47.4", "8.1", 10.0f, "Rupperswil, A",
						"Bahnhofstrasse 4, 5222 Rupperswil, Switzerland, Rupperswil",
						privateKey.key, publicKey.key);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(LocationMessage.class, actual,
						"message is not an instance of location message");
		Assertions.assertEquals(expectedLatitude, ((LocationMessage) actual).getLatitude());
		Assertions.assertEquals(expectedLongitude, ((LocationMessage) actual).getLongitude());
		Assertions.assertEquals(expectedAccuracy, ((LocationMessage) actual).getAccuracy());
		Assertions.assertEquals(expectedPoiName, ((LocationMessage) actual).getPoiName());
		Assertions.assertEquals(expectedPoiAddress, ((LocationMessage) actual).getPoiAddress());
	}

	@Test
	public void testGroupLocationMessage() throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expectedLatitude = "47.4";
		var expectedLongitude = "8.1";
		var expectedAccuracy = Float.valueOf(10.0f);
		var expectedPoiName = "Rupperswil, A";
		var expectedPoiAddress = "Bahnhofstrasse 4, 5222 Rupperswil, Switzerland, Rupperswil";

		var encryptResult = CryptTool.encryptGroupLocationMessage(Common.groupId, "47.4", "8.1",
						10.0f, "Rupperswil, A",
						"Bahnhofstrasse 4, 5222 Rupperswil, Switzerland, Rupperswil",
						privateKey.key, publicKey.key);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(GroupLocationMessage.class, actual,
						"message is not an instance of group location message");
		Assertions.assertArrayEquals(Common.groupId.getGroupCreator(),
						((GroupLocationMessage) actual).getGroupId().getGroupCreator());
		Assertions.assertArrayEquals(Common.groupId.getGroupId(),
						((GroupLocationMessage) actual).getGroupId().getGroupId());
		Assertions.assertEquals(expectedLatitude, ((GroupLocationMessage) actual).getLatitude());
		Assertions.assertEquals(expectedLongitude, ((GroupLocationMessage) actual).getLongitude());
		Assertions.assertEquals(expectedAccuracy, ((GroupLocationMessage) actual).getAccuracy());
		Assertions.assertEquals(expectedPoiName, ((GroupLocationMessage) actual).getPoiName());
		Assertions.assertEquals(expectedPoiAddress,
						((GroupLocationMessage) actual).getPoiAddress());
	}

	@Test
	public void testGroupLocationMessageNoAccuracy()
					throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expectedLatitude = "47.4";
		var expectedLongitude = "8.1";
		Float expectedAccuracy = null;
		var expectedPoiName = "Rupperswil";
		var expectedPoiAddress = "Switzerland";

		var encryptResult = CryptTool.encryptGroupLocationMessage(Common.groupId, "47.4", "8.1",
						null, "Rupperswil", "Switzerland", privateKey.key, publicKey.key);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(GroupLocationMessage.class, actual,
						"message is not an instance of group location message");
		Assertions.assertArrayEquals(Common.groupId.getGroupCreator(),
						((GroupLocationMessage) actual).getGroupId().getGroupCreator());
		Assertions.assertArrayEquals(Common.groupId.getGroupId(),
						((GroupLocationMessage) actual).getGroupId().getGroupId());
		Assertions.assertEquals(expectedLatitude, ((GroupLocationMessage) actual).getLatitude());
		Assertions.assertEquals(expectedLongitude, ((GroupLocationMessage) actual).getLongitude());
		Assertions.assertEquals(expectedAccuracy, ((GroupLocationMessage) actual).getAccuracy());
		Assertions.assertEquals(expectedPoiName, ((GroupLocationMessage) actual).getPoiName());
		Assertions.assertEquals(expectedPoiAddress,
						((GroupLocationMessage) actual).getPoiAddress());
	}

	@Test
	public void testGroupLocationMessageNoName() throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expectedLatitude = "47.4";
		var expectedLongitude = "8.1";
		var expectedAccuracy = Float.valueOf(10.0f);
		String expectedPoiName = null;
		var expectedPoiAddress = "Bahnhofstrasse 4, 5222 Rupperswil, Switzerland, Rupperswil";

		var encryptResult = CryptTool.encryptGroupLocationMessage(Common.groupId, "47.4", "8.1",
						10.0f, null, "Bahnhofstrasse 4, 5222 Rupperswil, Switzerland, Rupperswil",
						privateKey.key, publicKey.key);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(GroupLocationMessage.class, actual,
						"message is not an instance of group location message");
		Assertions.assertArrayEquals(Common.groupId.getGroupCreator(),
						((GroupLocationMessage) actual).getGroupId().getGroupCreator());
		Assertions.assertArrayEquals(Common.groupId.getGroupId(),
						((GroupLocationMessage) actual).getGroupId().getGroupId());
		Assertions.assertEquals(expectedLatitude, ((GroupLocationMessage) actual).getLatitude());
		Assertions.assertEquals(expectedLongitude, ((GroupLocationMessage) actual).getLongitude());
		Assertions.assertEquals(expectedAccuracy, ((GroupLocationMessage) actual).getAccuracy());
		Assertions.assertEquals(expectedPoiName, ((GroupLocationMessage) actual).getPoiName());
		Assertions.assertEquals(expectedPoiAddress,
						((GroupLocationMessage) actual).getPoiAddress());
	}

	@Test
	public void testGroupLocationMessageNoNameAndAddress()
					throws MessageParseException, InvalidKeyException {
		Key privateKey = Key.decodeKey(Common.otherPrivateKey);
		Key publicKey = Key.decodeKey(Common.myPublicKey);
		var expectedLatitude = "47.4";
		var expectedLongitude = "8.1";
		var expectedAccuracy = Float.valueOf(10.0f);
		String expectedPoiName = null;
		String expectedPoiAddress = null;

		var encryptResult = CryptTool.encryptGroupLocationMessage(Common.groupId, "47.4", "8.1",
						10.0f, null, null, privateKey.key, publicKey.key);

		var actual = CryptTool.decryptMessage(encryptResult.getResult(), privateKey.key,
						publicKey.key, encryptResult.getNonce());

		Assertions.assertNotNull(actual);
		Assertions.assertInstanceOf(GroupLocationMessage.class, actual,
						"message is not an instance of group location message");
		Assertions.assertArrayEquals(Common.groupId.getGroupCreator(),
						((GroupLocationMessage) actual).getGroupId().getGroupCreator());
		Assertions.assertArrayEquals(Common.groupId.getGroupId(),
						((GroupLocationMessage) actual).getGroupId().getGroupId());
		Assertions.assertEquals(expectedLatitude, ((GroupLocationMessage) actual).getLatitude());
		Assertions.assertEquals(expectedLongitude, ((GroupLocationMessage) actual).getLongitude());
		Assertions.assertEquals(expectedAccuracy, ((GroupLocationMessage) actual).getAccuracy());
		Assertions.assertEquals(expectedPoiName, ((GroupLocationMessage) actual).getPoiName());
		Assertions.assertEquals(expectedPoiAddress,
						((GroupLocationMessage) actual).getPoiAddress());
	}
}
