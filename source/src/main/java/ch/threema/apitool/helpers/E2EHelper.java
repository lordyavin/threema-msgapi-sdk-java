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

package ch.threema.apitool.helpers;

import ch.threema.apitool.APIConnector;
import ch.threema.apitool.CryptTool;
import ch.threema.apitool.exceptions.*;
import ch.threema.apitool.messages.*;
import ch.threema.apitool.types.FileRenderingType;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.results.CapabilityResult;
import ch.threema.apitool.results.EncryptResult;
import ch.threema.apitool.results.UploadResult;
import ch.threema.apitool.types.MessageId;
import ch.threema.apitool.types.voting.*;
import ch.threema.apitool.utils.ApiResponse;
import ch.threema.apitool.utils.ProtocolConstants;
import com.neilalexander.jnacl.NaCl;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper to handle Threema end-to-end encryption.
 */
public class E2EHelper {
	private final APIConnector apiConnector;
	private final byte[] privateKey;

	public static class ReceiveMessageResult {
		private final String messageId;
		private final ThreemaMessage message;
		protected List<File> files = new ArrayList<>();
		protected List<String> errors = new ArrayList<>();

		public ReceiveMessageResult(String messageId, ThreemaMessage message) {
			this.messageId = messageId;
			this.message = message;
		}

		public List<File> getFiles() {
			return this.files;
		}

		public List<String> getErrors() {
			return this.errors;
		}

		public String getMessageId() {
			return messageId;
		}

		public ThreemaMessage getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "ReceiveMessageResult{" + "messageId='" + messageId + '\'' + ", message="
							+ message + ", files=" + files + ", errors=" + errors + '}';
		}
	}

	public E2EHelper(APIConnector apiConnector, byte[] privateKey) {
		this.apiConnector = apiConnector;
		this.privateKey = privateKey;
	}

	/*
	 * ===================== Conversation Messages =====================
	 */

	/**
	 * Encrypt a text message and send it to the given recipient.
	 *
	 * @param threemaId target Threema ID
	 * @param text the text to send
	 * @return generated message ID
	 */
	public ApiResponse<String> sendTextMessage(String threemaId, String text) throws Exception {
		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new Exception("invalid threema id");
		}
		if (text.isEmpty()) {
			throw new Exception("no text provided");
		}
		EncryptResult res = CryptTool.encryptTextMessage(text, this.privateKey, publicKey);

		return this.apiConnector.sendE2EMessage(threemaId, res.getNonce(), res.getResult());
	}

	/**
	 * Encrypt a text message and send it to the given recipient.
	 *
	 * @param threemaId target Threema ID
	 * @param lat the location latitude
	 * @param lng the location longitude
	 * @param poiName the poi name
	 * @param poiAddress the location address
	 * @return generated message ID
	 */
	public ApiResponse<String> sendLocationMessage(String threemaId, String lat, String lng,
					String poiName, String poiAddress) throws Exception {
		return sendLocationMessage(threemaId, lat, lng, null, poiName, poiAddress);
	}

	/**
	 * Encrypt a text message and send it to the given recipient.
	 *
	 * @param threemaId target Threema ID
	 * @param lat the location latitude
	 * @param lng the location longitude
	 * @param accuracy the location accuracy
	 * @param poiName the poi name
	 * @param poiAddress the location address
	 * @return generated message ID
	 */
	public ApiResponse<String> sendLocationMessage(String threemaId, String lat, String lng,
					Float accuracy, String poiName, String poiAddress) throws Exception {
		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new Exception("invalid threema id");
		}
		if (lat.isEmpty()) {
			throw new IOException("invalid latitude");
		}
		if (lng.isEmpty()) {
			throw new IOException("invalid longitude");
		}
		if (poiAddress.isEmpty()) {
			throw new IOException("invalid address");
		}

		EncryptResult res = CryptTool.encryptLocationMessage(lat, lng, accuracy, poiName,
						poiAddress, this.privateKey, publicKey);

		return this.apiConnector.sendE2EMessage(threemaId, res.getNonce(), res.getResult());

	}

	/**
	 * Encrypt a file message and send it to the given group. The thumbnailMessagePath can be null.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @param text Message text
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupTextMessage(List<String> threemaIds, GroupId groupId,
					String text) throws InvalidKeyException, IOException, ApiException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (text.isEmpty()) {
			throw new IOException("invalid text");
		}

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupTextMessage(groupId, text, this.privateKey,
							pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/**
	 * Encrypt a file message and send it to the given group. The thumbnailMessagePath can be null.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @param lat The geographic latitude
	 * @param lng The geographic longitude
	 * @param poiName The location name
	 * @param poiAddress The location address
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupLocationMessage(List<String> threemaIds, GroupId groupId,
					String lat, String lng, String poiName, String poiAddress)
					throws InvalidKeyException, IOException, ApiException {
		return sendGroupLocationMessage(threemaIds, groupId, lat, lng, null, poiName, poiAddress);
	}

	/**
	 * Encrypt a file message and send it to the given group. The thumbnailMessagePath can be null.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @param lat The geographic latitude
	 * @param lng The geographic longitude
	 * @param accuracy The location accuracy
	 * @param poiName The location name
	 * @param poiAddress The location address
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupLocationMessage(List<String> threemaIds, GroupId groupId,
					String lat, String lng, Float accuracy, String poiName, String poiAddress)
					throws InvalidKeyException, IOException, ApiException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (lat.isEmpty()) {
			throw new IOException("invalid latitude");
		}
		if (lng.isEmpty()) {
			throw new IOException("invalid longitude");
		}
		if (poiAddress.isEmpty()) {
			throw new IOException("invalid address");
		}

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupLocationMessage(groupId, lat, lng, accuracy, poiName,
							poiAddress, this.privateKey, pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/**
	 * Encrypt an image message and send it to the given recipient.
	 *
	 * @param threemaId target Threema ID
	 * @param imageFilePath path to read image data from
	 * @return generated message ID
	 * @deprecated
	 */
	@Deprecated
	public ApiResponse<String> sendImageMessage(String threemaId, String imageFilePath)
					throws NotAllowedException, IOException, InvalidKeyException, ApiException {
		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new InvalidKeyException("invalid threema id");
		}

		// check capability of a key
		CapabilityResult capabilityResult = this.apiConnector.lookupKeyCapability(threemaId);
		if (capabilityResult == null || !capabilityResult.canImage()) {
			throw new NotAllowedException();
		}

		byte[] fileData = Files.readAllBytes(Paths.get(imageFilePath));

		// encrypt the image
		EncryptResult encryptResult = CryptTool.encrypt(fileData, this.privateKey, publicKey);

		// upload the image
		UploadResult uploadResult = apiConnector.uploadFile(encryptResult);

		if (!uploadResult.isSuccess()) {
			throw new IOException("could not upload file (upload response "
							+ uploadResult.getResponseCode() + ")");
		}

		// send it
		EncryptResult imageMessage = CryptTool.encryptImageMessage(encryptResult, uploadResult,
						privateKey, publicKey);

		return apiConnector.sendE2EMessage(threemaId, imageMessage.getNonce(),
						imageMessage.getResult());
	}

	/**
	 * Encrypt a file message and send it to the given recipient. The thumbnailMessagePath can be
	 * null.
	 *
	 * @param threemaId target Threema ID
	 * @param fileMessageFile the file to be sent
	 * @return generated message ID
	 */
	public ApiResponse<String> sendFileMessage(String threemaId, File fileMessageFile)
					throws InvalidKeyException, IOException, NotAllowedException, ApiException {
		return this.sendFileMessage(threemaId, fileMessageFile, null, null);
	}

	/**
	 * Encrypt a file message and send it to the given recipient. The thumbnailMessagePath can be
	 * null.
	 *
	 * @param threemaId target Threema ID
	 * @param fileMessageFile the file to be sent
	 * @param thumbnailMessagePath file for thumbnail; if not set, no thumbnail will be sent
	 * @param caption caption for the file message; if not set, no caption will be attached
	 * @return generated message ID
	 */
	public ApiResponse<String> sendFileMessage(String threemaId, File fileMessageFile,
					File thumbnailMessagePath, String caption)
					throws InvalidKeyException, IOException, NotAllowedException, ApiException {
		return this.sendFileMessage(threemaId, fileMessageFile, thumbnailMessagePath, caption,
						FileRenderingType.FILE);
	}

	/**
	 * Encrypt a file message and send it to the given recipient. The thumbnailMessagePath can be
	 * null.
	 *
	 * @param threemaId target Threema ID
	 * @param fileMessageFile the file to be sent
	 * @param thumbnailMessagePath file for thumbnail; if not set, no thumbnail will be sent
	 * @param caption caption for the file message; if not set, no caption will be attached
	 * @param renderingType file rendering type
	 * @return generated message ID
	 */
	public ApiResponse<String> sendFileMessage(String threemaId, File fileMessageFile,
					File thumbnailMessagePath, String caption, FileRenderingType renderingType)
					throws InvalidKeyException, IOException, NotAllowedException, ApiException {
		return this.sendFileMessage(threemaId, fileMessageFile, thumbnailMessagePath, caption,
						renderingType, null, null);
	}

	/**
	 * Encrypt a file message and send it to the given recipient. The thumbnailMessageFile can be
	 * null.
	 *
	 * @param threemaId target Threema ID
	 * @param fileMessageFile the file to be sent
	 * @param thumbnailMessageFile file for thumbnail; if not set, no thumbnail will be sent
	 * @param caption caption for the file message; if not set, no caption will be attached
	 * @param renderingType file rendering type
	 * @param correlationId media correlation ID
	 * @param metadata FileMessage metadata
	 * @return generated message ID
	 */
	public ApiResponse<String> sendFileMessage(String threemaId, File fileMessageFile,
					File thumbnailMessageFile, String caption, FileRenderingType renderingType,
					String correlationId, Map<String, Object> metadata)
					throws InvalidKeyException, IOException, NotAllowedException, ApiException {
		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new InvalidKeyException("invalid threema id");
		}
		if (!(renderingType == FileRenderingType.FILE || renderingType == FileRenderingType.MEDIA
						|| renderingType == FileRenderingType.STICKER)) {
			throw new IOException("invalid rendering type");
		}
		if (!fileMessageFile.isFile()) {
			throw new IOException("invalid file");
		}

		// check capability of a key
		CapabilityResult capabilityResult = this.apiConnector.lookupKeyCapability(threemaId);
		if (capabilityResult == null || !capabilityResult.canFile()) {
			throw new NotAllowedException();
		}

		byte[] fileData = this.readFile(fileMessageFile);

		// encrypt the image
		EncryptResult encryptResult = CryptTool.encryptFileData(fileData);

		// upload the image
		UploadResult uploadResult = apiConnector.uploadFile(encryptResult);

		if (!uploadResult.isSuccess()) {
			throw new IOException("could not upload file (upload response "
							+ uploadResult.getResponseCode() + ")");
		}

		UploadResult uploadResultThumbnail = null;
		String thumbnailMimeType = null;

		if (thumbnailMessageFile != null && thumbnailMessageFile.isFile()) {
			byte[] thumbnailData = this.readFile(thumbnailMessageFile);

			// encrypt the thumbnail
			EncryptResult encryptResultThumbnail = CryptTool.encryptFileThumbnailData(thumbnailData,
							encryptResult.getSecret());

			// upload the thumbnail
			uploadResultThumbnail = this.apiConnector.uploadFile(encryptResultThumbnail);
			thumbnailMimeType = getMimeType(thumbnailMessageFile);
		}

		String mimeType = getMimeType(fileMessageFile);

		// send it
		EncryptResult fileMessage = CryptTool.encryptFileMessage(uploadResult.getBlobId(),
						uploadResultThumbnail != null ? uploadResultThumbnail.getBlobId() : null,
						thumbnailMimeType, encryptResult.getSecret(), mimeType,
						fileMessageFile.getName(), (int) fileMessageFile.length(), caption,
						renderingType, correlationId, metadata, privateKey, publicKey);

		return this.apiConnector.sendE2EMessage(threemaId, fileMessage.getNonce(),
						fileMessage.getResult());
	}

	/**
	 * Encrypt a ballot create message and send it to the given recipient.
	 *
	 * @param threemaId target Threema ID
	 * @param ballotId The poll ballot identifier
	 * @param description The group poll description
	 * @param state The group poll state
	 * @param votingMode The voting mode
	 * @param resultsDisclosureType The poll results disclosure type
	 * @param displayMode The display mode
	 * @param choices The available vote choices
	 * @param participants The poll participants
	 * @return generated message ID
	 */
	public ApiResponse<String> sendBallotCreateMessage(String threemaId, byte[] ballotId,
					String description, State state, VotingMode votingMode,
					ResultsDisclosureType resultsDisclosureType, DisplayMode displayMode,
					List<BallotChoice> choices, List<String> participants)
					throws InvalidKeyException, ApiException, IOException {

		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new InvalidKeyException("invalid threema id");
		}
		if (ballotId.length != ProtocolConstants.BALLOT_ID_LEN) {
			throw new IOException("invalid ballotId");
		}
		if (!(state == State.OPEN || state == State.CLOSED)) {
			throw new IOException("invalid state");
		}
		if (!(votingMode == VotingMode.SINGLE_CHOICE || votingMode == VotingMode.MULTIPLE_CHOICE)) {
			throw new IOException("invalid votingMode");
		}
		if (!(resultsDisclosureType == ResultsDisclosureType.CLOSED
						|| resultsDisclosureType == ResultsDisclosureType.INTERMEDIATE)) {
			throw new IOException("invalid resultsDisclosureType");
		}
		if (!(displayMode == DisplayMode.LIST || displayMode == DisplayMode.SUMMARY)) {
			throw new IOException("invalid displayMode");
		}
		if (choices.size() == 0) {
			throw new IOException("please provide at least one choice");
		}

		var message = CryptTool.encryptBallotCreateMessage(ballotId, description, state, votingMode,
						resultsDisclosureType, 0, displayMode, choices, participants,
						this.privateKey, publicKey);

		return this.apiConnector.sendE2EMessage(threemaId, message.getNonce(), message.getResult());
	}

	/**
	 * Encrypt a ballot vote message and send it to the given recipient.
	 *
	 * @param threemaId target Threema ID
	 * @param creator Poll creator
	 * @param ballotId the poll ballot identifier
	 * @param votes the group poll votes
	 * @return generated message ID
	 */
	public ApiResponse<String> sendBallotVoteMessage(String threemaId, byte[] creator,
					byte[] ballotId, List<VoteChoice> votes)
					throws InvalidKeyException, ApiException, IOException {
		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new InvalidKeyException("invalid threema id");
		}
		if (creator.length != GroupId.CREATOR_ID_LEN) {
			throw new IOException("invalid creator");
		}
		if (ballotId.length != ProtocolConstants.BALLOT_ID_LEN) {
			throw new IOException("invalid ballotId");
		}
		if (votes.size() == 0) {
			throw new IOException("please make at least one vote");
		}

		var message = CryptTool.encryptBallotVoteMessage(creator, ballotId, votes, this.privateKey,
						publicKey);

		return this.apiConnector.sendE2EMessage(threemaId, message.getNonce(), message.getResult());
	}

	/**
	 * Encrypt a file message and send it to the given group. The thumbnailMessagePath can be null.
	 *
	 * @param threemaId target threema ID
	 * @param groupId target Threema Group ID
	 * @param fileMessageFile the file to be sent
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupFileMessage(List<String> threemaId, GroupId groupId,
					File fileMessageFile)
					throws InvalidKeyException, IOException, NotAllowedException, ApiException {
		return sendGroupFileMessage(threemaId, groupId, fileMessageFile, null, null,
						FileRenderingType.FILE);
	}

	/**
	 * Encrypt a file message and send it to the given group. The thumbnailMessagePath can be null.
	 *
	 * @param threemaId target threema ID
	 * @param groupId target Threema Group ID
	 * @param fileMessageFile the file to be sent
	 * @param thumbnailMessageFile file for thumbnail; if not set, no thumbnail will be sent
	 * @param caption caption for the file message; if not set, no caption will be attached
	 * @param renderingType file rendering type
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupFileMessage(List<String> threemaId, GroupId groupId,
					File fileMessageFile, File thumbnailMessageFile, String caption,
					FileRenderingType renderingType)
					throws InvalidKeyException, IOException, NotAllowedException, ApiException {
		return sendGroupFileMessage(threemaId, groupId, fileMessageFile, thumbnailMessageFile,
						caption, renderingType, null, null);
	}

	/**
	 * Encrypt a file message and send it to the given group. The thumbnailMessageFile can be null.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @param fileMessageFile the file to be sent
	 * @param thumbnailMessageFile file for thumbnail; if not set, no thumbnail will be sent
	 * @param caption caption for the file message; if not set, no caption will be attached
	 * @param renderingType file rendering type
	 * @param correlationId media correlation ID
	 * @param metadata FileMessage metadata
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupFileMessage(List<String> threemaIds, GroupId groupId,
					File fileMessageFile, File thumbnailMessageFile, String caption,
					FileRenderingType renderingType, String correlationId,
					Map<String, Object> metadata)
					throws InvalidKeyException, IOException, ApiException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		if (!fileMessageFile.isFile()) {
			throw new IOException("invalid file");
		}

		byte[] fileData = this.readFile(fileMessageFile);

		// encrypt the file
		EncryptResult encryptResult = CryptTool.encryptFileData(fileData);

		// upload the file
		UploadResult uploadResult = apiConnector.uploadFile(encryptResult, true);

		if (!uploadResult.isSuccess()) {
			throw new IOException("could not upload file (upload response "
							+ uploadResult.getResponseCode() + ")");
		}

		UploadResult uploadResultThumbnail = null;
		String thumbnailMimeType = null;

		if (thumbnailMessageFile != null && thumbnailMessageFile.isFile()) {
			byte[] thumbnailData = this.readFile(thumbnailMessageFile);

			// encrypt the thumbnail
			EncryptResult encryptResultThumbnail = CryptTool.encryptFileThumbnailData(thumbnailData,
							encryptResult.getSecret());

			// upload the thumbnail
			uploadResultThumbnail = this.apiConnector.uploadFile(encryptResultThumbnail, true);
			thumbnailMimeType = getMimeType(thumbnailMessageFile);
		}

		String mimeType = getMimeType(fileMessageFile);

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupFileMessage(groupId, uploadResult.getBlobId(),
							uploadResultThumbnail != null ? uploadResultThumbnail.getBlobId()
											: null,
							thumbnailMimeType, encryptResult.getSecret(), mimeType,
							fileMessageFile.getName(), (int) fileMessageFile.length(), caption,
							renderingType, correlationId, metadata, privateKey,
							pubkeys.get(threemaIds.get(i)));
			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/**
	 * Encrypt a ballot create message and send it to the given group.
	 *
	 * @param groupId target Threema Group ID
	 * @param ballotId The poll ballot identifier
	 * @param description The group poll description
	 * @param state The group poll state
	 * @param votingMode The voting mode
	 * @param resultsDisclosureType The poll results disclosure type
	 * @param displayMode The display mode
	 * @param choices The available vote choices
	 * @param participants The poll participants
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupBallotCreateMessage(List<String> threemaIds,
					GroupId groupId, byte[] ballotId, String description, State state,
					VotingMode votingMode, ResultsDisclosureType resultsDisclosureType,
					DisplayMode displayMode, List<BallotChoice> choices, List<String> participants)
					throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}
		if (ballotId.length != ProtocolConstants.BALLOT_ID_LEN) {
			throw new IOException("invalid ballotId");
		}
		if (!(state == State.OPEN || state == State.CLOSED)) {
			throw new IOException("invalid state");
		}
		if (!(votingMode == VotingMode.SINGLE_CHOICE || votingMode == VotingMode.MULTIPLE_CHOICE)) {
			throw new IOException("invalid votingMode");
		}
		if (!(resultsDisclosureType == ResultsDisclosureType.CLOSED
						|| resultsDisclosureType == ResultsDisclosureType.INTERMEDIATE)) {
			throw new IOException("invalid resultsDisclosureType");
		}
		if (!(displayMode == DisplayMode.LIST || displayMode == DisplayMode.SUMMARY)) {
			throw new IOException("invalid displayMode");
		}
		if (choices.size() == 0) {
			throw new IOException("invalid choices");
		}

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupBallotCreateMessage(groupId, ballotId, description,
							state, votingMode, resultsDisclosureType, 0, displayMode, choices,
							participants, this.privateKey, pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/**
	 * Encrypt a ballot vote message and send it to the given group.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @param creator Poll creator
	 * @param ballotId the poll ballot identifier
	 * @param votes the group poll state
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupBallotVoteMessage(List<String> threemaIds,
					byte[] creator, GroupId groupId, byte[] ballotId, List<VoteChoice> votes)
					throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		if (creator.length == 0) {
			throw new IOException("invalid creator");
		}
		if (ballotId.length != ProtocolConstants.BALLOT_ID_LEN) {
			throw new IOException("invalid ballotId");
		}
		if (votes.size() == 0) {
			throw new IOException("invalid votes");
		}

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupBallotVoteMessage(groupId, creator, ballotId, votes,
							this.privateKey, pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/*
	 * ============== Status Updates ==============
	 */

	/**
	 * Encrypt a delivery receipt and send it to the original sender.
	 *
	 * @param threemaId target Threema ID
	 * @param ackedMessageIds the acknowledged message ids
	 * @param receiptType the delivery receipt type
	 * @return generated message ID
	 */
	public ApiResponse<String> sendDeliveryReceipt(String threemaId,
					List<MessageId> ackedMessageIds, DeliveryReceipt.Type receiptType)
					throws InvalidKeyException, ApiException, IOException {
		if (ackedMessageIds.isEmpty()) {
			throw new IOException("Empty ackedMessageIds");
		}

		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new InvalidKeyException("invalid threema id");
		}

		var message = CryptTool.encryptDeliveryReceipt(receiptType, ackedMessageIds,
						this.privateKey, publicKey);

		return this.apiConnector.sendE2EMessage(threemaId, message.getNonce(), message.getResult());
	}

	/**
	 * Encrypt a group delivery receipt and send it to all group members
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId the group identifier
	 * @param ackedMessageIds the acknowledged message ids
	 * @param receiptType the delivery receipt type
	 * @return generated message IDs
	 */
	public ApiResponse<JSONArray> sendGroupDeliveryReceipt(List<String> threemaIds, GroupId groupId,
					List<MessageId> ackedMessageIds, DeliveryReceipt.Type receiptType)
					throws InvalidKeyException, ApiException, IOException {
		if (ackedMessageIds.isEmpty()) {
			throw new IOException("Empty ackedMessageIds");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		// fetch public keys
		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupDeliveryReceipt(groupId, receiptType, ackedMessageIds,
							this.privateKey, pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/*
	 * ========================= Contact and Group Control =========================
	 */

	/**
	 * Encrypt a group set photo message and send it to the given group.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @param groupPhotoInJpg the new jpg image
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupSetPhotoMessage(List<String> threemaIds, GroupId groupId,
					File groupPhotoInJpg) throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		if (!groupPhotoInJpg.isFile()) {
			throw new IOException("invalid file");
		}

		byte[] fileData = this.readFile(groupPhotoInJpg);

		// encrypt the image
		EncryptResult encryptResult = CryptTool.encryptFileData(fileData);

		// upload the image
		UploadResult uploadResult = apiConnector.uploadFile(encryptResult, true);

		if (!uploadResult.isSuccess()) {
			throw new IOException("could not upload file (upload response "
							+ uploadResult.getResponseCode() + ")");
		}

		Map<String, Boolean> moreOptions = Map.of("noPush", true);
		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupSetPhoto(groupId, uploadResult.getBlobId(),
							(int) groupPhotoInJpg.length(), // FIXME: Lossy conversion
							encryptResult.getSecret(), this.privateKey,
							pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes, moreOptions);
	}

	/**
	 * Encrypt a group delete photo message and send it to the given group.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupDeletePhotoMessage(List<String> threemaIds,
					GroupId groupId) throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		Map<String, Boolean> moreOptions = Map.of("noPush", true);
		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupDeletePhoto(groupId, this.privateKey,
							pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes, moreOptions);
	}

	/**
	 * Encrypt a group create message and send it to the given group.
	 *
	 * @param members target Threema IDs
	 * @param groupId target Threema Group ID
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupCreateMessage(List<String> threemaIds,
					List<String> members, GroupId groupId)
					throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());
		members = members.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		Map<String, Boolean> moreOptions = Map.of("noPush", true);
		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupCreateMessage(groupId, members, this.privateKey,
							pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes, moreOptions);
	}

	/**
	 * Encrypt a group request sync message and send it to the given group.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupRequestSyncMsg(List<String> threemaIds, GroupId groupId)
					throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupRequestSyncMessage(groupId, this.privateKey,
							pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/**
	 * Encrypt a group rename message and send it to the given group.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @param newGroupName the new group name
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupRenameMessage(List<String> threemaIds, GroupId groupId,
					String newGroupName) throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		if (newGroupName.isEmpty()) {
			throw new IOException("Invalid new group name");
		}

		Map<String, Boolean> moreOptions = Map.of("noPush", true);
		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupRenameMessage(groupId, newGroupName, this.privateKey,
							pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes, moreOptions);
	}

	/**
	 * Encrypt a group leave message and send it to the given group.
	 *
	 * @param threemaIds target Threema IDs
	 * @param groupId target Threema Group ID
	 * @return generated message ID
	 */
	public ApiResponse<JSONArray> sendGroupLeaveMessage(List<String> threemaIds, GroupId groupId)
					throws InvalidKeyException, ApiException, IOException {
		if (groupId == null) {
			throw new IOException("Invalid groupId");
		}

		threemaIds = threemaIds.stream().distinct().collect(Collectors.toList());

		HashMap<String, byte[]> pubkeys = lookupPubkeys(threemaIds);

		if (pubkeys.isEmpty()) {
			throw new InvalidKeyException("invalid threema ids");
		}

		var nonces = new byte[threemaIds.size()][];
		var boxes = new byte[threemaIds.size()][];

		for (int i = 0; i < threemaIds.size(); i++) {
			var res = CryptTool.encryptGroupLeaveMessage(groupId, this.privateKey,
							pubkeys.get(threemaIds.get(i)));

			nonces[i] = res.getNonce();
			boxes[i] = res.getResult();
		}

		return this.apiConnector.sendE2EBulkMessage(threemaIds.toArray(String[]::new), nonces,
						boxes);
	}

	/**
	 * Decrypt a Message and download the blobs of the Message (e.g. image or file)
	 *
	 * @param threemaId Threema ID of the sender
	 * @param messageId Message ID
	 * @param box Encrypted box data of the file/image message
	 * @param nonce Nonce that was used for message encryption
	 * @param outputFolder Output folder for storing decrypted images/files
	 * @return result of message reception
	 */
	public ReceiveMessageResult receiveMessage(String threemaId, String messageId, byte[] box,
					byte[] nonce, Path outputFolder)
					throws IOException, InvalidKeyException, MessageParseException, ApiException {
		// fetch public key
		byte[] publicKey = this.apiConnector.lookupKey(threemaId);

		if (publicKey == null) {
			throw new InvalidKeyException("invalid threema id");
		}

		ThreemaMessage message = CryptTool.decryptMessage(box, this.privateKey, publicKey, nonce);

		ReceiveMessageResult result = new ReceiveMessageResult(messageId, message);

		if (message instanceof ImageMessage) {
			// download image
			var imageMessage = (ImageMessage) message;
			byte[] fileData = this.apiConnector.downloadFile(imageMessage.getBlobId());

			if (fileData == null) {
				throw new MessageParseException();
			}

			byte[] decryptedFileContent = CryptTool.decrypt(fileData, privateKey, publicKey,
							imageMessage.getNonce());
			File imageFile = new File(outputFolder.toString() + "/" + messageId + ".jpg");
			FileOutputStream fos = new FileOutputStream(imageFile);
			fos.write(decryptedFileContent);
			fos.close();

			result.files.add(imageFile);
		} else if (message instanceof FileMessage) {
			// download file
			var fileMessage = (FileMessage) message;
			var fileData = this.apiConnector.downloadFile(fileMessage.getBlobId());

			byte[] decryptedFileData =
							CryptTool.decryptFileData(fileData, fileMessage.getEncryptionKey());
			File file = new File(outputFolder.toString() + "/" + messageId + "-"
							+ fileMessage.getFilename());
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(decryptedFileData);
			fos.close();

			result.files.add(file);

			if (fileMessage.getThumbnailBlobId() != null
							&& fileMessage.getThumbnailBlobId().length > 0) {
				byte[] thumbnailData =
								this.apiConnector.downloadFile(fileMessage.getThumbnailBlobId());

				byte[] decryptedThumbnailData = CryptTool.decryptFileThumbnailData(thumbnailData,
								fileMessage.getEncryptionKey());
				File thumbnailFile = new File(outputFolder + "/" + messageId + "-thumbnail.jpg");
				fos = new FileOutputStream(thumbnailFile);
				fos.write(decryptedThumbnailData);
				fos.close();

				result.files.add(thumbnailFile);
			}
		} else if (message instanceof GroupFileMessage) {
			// download file
			var groupFileMessage = (GroupFileMessage) message;
			var fileData = this.apiConnector.downloadFile(groupFileMessage.getBlobId());

			byte[] decryptedFileData = CryptTool.decryptFileData(fileData,
							groupFileMessage.getEncryptionKey());
			File file = new File(outputFolder.toString() + "/" + messageId + "-"
							+ groupFileMessage.getFilename());
			FileOutputStream fos = new FileOutputStream(file);
			if (decryptedFileData == null)
				throw new DecryptionFailedException();
			fos.write(decryptedFileData);
			fos.close();

			result.files.add(file);

			if (groupFileMessage.getThumbnailBlobId() != null
							&& groupFileMessage.getThumbnailBlobId().length > 0) {
				byte[] thumbnailData = this.apiConnector
								.downloadFile(groupFileMessage.getThumbnailBlobId());

				byte[] decryptedThumbnailData = CryptTool.decryptFileThumbnailData(thumbnailData,
								groupFileMessage.getEncryptionKey());
				File thumbnailFile = new File(outputFolder + "/" + messageId + "-thumbnail.jpg");
				fos = new FileOutputStream(thumbnailFile);
				fos.write(decryptedThumbnailData);
				fos.close();

				result.files.add(thumbnailFile);
			}
		}

		return result;
	}

	private HashMap<String, byte[]> lookupPubkeys(List<String> threemaIds)
					throws ApiException, InvalidKeyException, IOException {
		var pubkeys = new HashMap<String, byte[]>(threemaIds.size());
		if (threemaIds.isEmpty()) {
			throw new IOException("invalid threemaIds");
		}
		for (var threemaId : threemaIds) {
			var pubkey = this.apiConnector.lookupKey(threemaId);

			if (pubkey == null) {
				throw new InvalidKeyException("invalid threema id");
			}

			// fetch public key
			pubkeys.put(threemaId, pubkey);
		}
		return pubkeys;
	}

	/**
	 * Read file data from file - store at offset in byte array for in-place encryption
	 *
	 * @param file input file
	 * @return file data with padding/offset for NaCl
	 */
	private byte[] readFile(File file) throws IOException {
		int fileLength = (int) file.length();
		byte[] fileData = new byte[fileLength + NaCl.BOXOVERHEAD];
		IOUtils.readFully(new FileInputStream(file), fileData, NaCl.BOXOVERHEAD, fileLength);
		return fileData;
	}

	private String getMimeType(File messageFile) {
		String mimeType;
		try {
			mimeType = Files.probeContentType(messageFile.toPath());

			if (mimeType == null || mimeType.length() == 0) {
				// Try to using the TIka Library for mime type detection
				Tika tika = new Tika();
				mimeType = tika.detect(messageFile);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// System.out.println("Mime type: " + mimeType);

		if (mimeType == null || mimeType.length() == 0) {
			mimeType = "application/octet-stream";
		}

		return mimeType;
	}
}
