package ch.threema.apitool;

import ch.threema.apitool.exceptions.ApiException;
import ch.threema.apitool.exceptions.InvalidKeyException;
import ch.threema.apitool.helpers.E2EHelper;
import ch.threema.apitool.messages.DeliveryReceipt;
import ch.threema.apitool.types.FileRenderingType;
import ch.threema.apitool.types.GroupId;
import ch.threema.apitool.types.Key;
import ch.threema.apitool.types.MessageId;
import ch.threema.apitool.types.voting.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.json.JSONArray;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class E2ETest {

	private static final byte[] randomGroupId = new byte[8];
	private static APIConnector connector;
	private static E2EHelper e2EHelper;
	private static final Random random = new Random();

	public static void main(String[] args) {
		if (args == null || args.length < 4 || args.length > 5) {
			System.out.printf("Usage: %s Threema-ID Gateway-ID Secret PrivateKey [ApiUrl]%n",
							new java.io.File(E2ETest.class.getProtectionDomain().getCodeSource()
											.getLocation().getPath()).getName());
			System.exit(-1);
		}
		random.nextBytes(randomGroupId);
		var threemaId = args[0];
		String gatewayId = args[1];
		var secret = args[2];
		var pkey = args[3];
		var apiUrl = args.length > 4 ? args[4] : null;
		var reader = new MavenXpp3Reader();

		try {
			connector = new APIConnector(gatewayId, secret, apiUrl, new PublicKeyStore() {
				@Override
				protected byte[] fetchPublicKey(String threemaId) {
					return null;
				}

				@Override
				protected void save(String threemaId, byte[] publicKey) {

				}
			});
			connector.setUserAgent(String.format("threema-msgapi-sdk-java/%s-test",
							reader.read(new FileReader("pom.xml")).getVersion()));
		} catch (IOException | XmlPullParserException e) {
			throw new RuntimeException(e);
		}
		Key privateKey;
		try {
			privateKey = new Key(Key.KeyType.PRIVATE, Key.decodeKey(pkey).key);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
		List<String> threemaIds = List.of(gatewayId, threemaId);
		e2EHelper = new E2EHelper(connector, privateKey.key);
		try {
			/* Lookups */
			testLookupPhone("41790000000"); // OK
			testLookupEmail("abc@example.com"); // OK
			/* 1:1 Messages */
			var msgId = sendE2ETextMsg(threemaId); // OK, OK
			sendE2EFileMsg(threemaId); // OK, OK
			var ballotId = sendE2EBallotCreateMessage(threemaId); // OK, OK
			sendE2EBallotVoteMsg(threemaId, gatewayId, ballotId); // OK, OK
			sendE2EBallotCloseMessage(threemaId, gatewayId, ballotId); // OK, OK
			sendE2ELocationMsg(threemaId); // OK, OK
			sendDeliveryReceipt(threemaId, msgId, DeliveryReceipt.Type.RECEIVED); // OK, OK
			sendDeliveryReceipt(threemaId, msgId, DeliveryReceipt.Type.USER_ACK); // OK, OK

			/* Group messages */
			sendE2EGroupCreateMsg(
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8)),
							threemaIds, List.of(threemaId)); // OK, OK
			sendE2EGroupRenameMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK,
																										// OK
			sendE2EGroupSetPhotoMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK,
																										// OK
			var msgIds = sendE2EGroupTextMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK,
																										// OK
			sendE2EGroupFileMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK,
																										// OK
			sendE2EGroupLocationMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK,
																										// OK
			var groupBallotId = sendE2EGroupBallotCreateMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK,
																										// OK
			sendE2EGroupBallotVoteMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8)),
							groupBallotId); // OK, OK
			sendGroupDeliveryReceipt(threemaIds, msgIds.get(1),
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK,
																										// OK
			sendE2EGroupRequestSyncMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK
																										// (?),
																										// OK
																										// (?)
			sendE2EGroupDeletePhotoMsg(threemaIds,
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8))); // OK(,
																										// no
																										// receive?)
			sendE2EGroupLeaveMsg(
							new GroupId(randomGroupId, gatewayId.getBytes(StandardCharsets.UTF_8)),
							threemaIds, List.of(gatewayId)); // OK,
		} catch (ApiException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void sendDeliveryReceipt(String threemaId, String messageId,
					DeliveryReceipt.Type receiptType) {
		try {
			var response = e2EHelper.sendDeliveryReceipt(threemaId,
							List.of(new MessageId(messageId)), receiptType);

			System.out.println(
							"Message ID: " + response.getData() + " " + response.getStatusCode());
		} catch (Exception e) {
			System.err.println("Sending delivery receipt failed");
		}
	}

	private static void sendGroupDeliveryReceipt(List<String> threemaIds, String msgId,
					GroupId groupId) {
		try {
			var response = e2EHelper.sendGroupDeliveryReceipt(threemaIds, groupId,
							List.of(new MessageId(msgId)), DeliveryReceipt.Type.USER_DEC);

			System.out.println(
							"Message ID: " + response.getData() + " " + response.getStatusCode());
		} catch (Exception e) {
			System.err.println("Sending group delivery receipt failed");
		}
	}

	private static String sendE2ETextMsg(String threemaId) {
		try {
			var response = e2EHelper.sendTextMessage(threemaId, "Test Message");

			System.out.println(
							"Message ID: " + response.getData() + " " + response.getStatusCode());
			return response.getData();
		} catch (Exception e) {
			System.err.println("Sending text message failed");
			return "";
		}
	}

	private static void sendE2ELocationMsg(String threemaId) {
		try {
			var response = e2EHelper.sendLocationMessage(threemaId, "47.8", "8.3", null,
							"Test Building", "Nowhere");

			System.out.println(
							"Message ID: " + response.getData() + " " + response.getStatusCode());
		} catch (Exception e) {
			System.err.println("Sending location message failed");
		}
	}

	private static List<String> sendE2EGroupLocationMsg(List<String> threemaIds, GroupId groupId)
					throws ApiException {
		try {
			var resArr = e2EHelper.sendGroupLocationMessage(threemaIds, groupId, "47.4", "8.1",
							null, "Test Building", "Nowhere");

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group location message failed");
			return List.of();
		}
	}

	private static byte[] sendE2EBallotCreateMessage(String threemaId) {
		try {
			byte[] ballotId = new byte[8];
			random.nextBytes(ballotId);
			var response = e2EHelper.sendBallotCreateMessage(threemaId, ballotId, "Test poll",
							State.OPEN, VotingMode.SINGLE_CHOICE,
							ResultsDisclosureType.INTERMEDIATE, DisplayMode.LIST,
							List.of(new BallotChoice(0, "Pizza", 0, null, null),
											new BallotChoice(1, "Ananas", 1, null, null)),
							null);
			System.out.println(
							"Message ID: " + response.getData() + " " + response.getStatusCode());
			return ballotId;
		} catch (Exception e) {
			System.err.println("Sending ballot create message failed");
			return new byte[0];
		}
	}

	private static void sendE2EBallotCloseMessage(String threemaId, String gatewayId,
					byte[] ballotId) {
		try {
			var response = e2EHelper.sendBallotCreateMessage(threemaId, ballotId, "Test poll",
							State.CLOSED, VotingMode.SINGLE_CHOICE,
							ResultsDisclosureType.INTERMEDIATE, DisplayMode.LIST,
							List.of(new BallotChoice(0, "Pizza", 0, List.of(1, 0), null),
											new BallotChoice(1, "Ananas", 1, List.of(0, 1), null)),
							List.of(threemaId, gatewayId));
			System.out.println(
							"Message ID: " + response.getData() + " " + response.getStatusCode());
		} catch (Exception e) {
			System.err.println("Sending ballot close message failed");
		}
	}

	private static byte[] sendE2EGroupBallotCreateMsg(List<String> threemaIds, GroupId groupId)
					throws ApiException {
		try {
			byte[] ballotId = new byte[8];
			random.nextBytes(ballotId);
			System.out.println("BallotID: " + Arrays.toString(ballotId));
			var resArr = e2EHelper.sendGroupBallotCreateMessage(threemaIds, groupId, ballotId,
							"Test Poll", State.OPEN, VotingMode.SINGLE_CHOICE,
							ResultsDisclosureType.INTERMEDIATE, DisplayMode.LIST,
							List.of(new BallotChoice(0, "Pizza", 0, null, null),
											new BallotChoice(1, "Ananas", 1, null, null)),
							null);

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			sendE2EGroupBallotVoteMsg(threemaIds,
							new GroupId(randomGroupId,
											threemaIds.get(0).getBytes(StandardCharsets.UTF_8)),
							ballotId); // OK
			sendE2EGroupBallotCloseMsg(threemaIds,
							new GroupId(randomGroupId,
											threemaIds.get(0).getBytes(StandardCharsets.UTF_8)),
							ballotId); // OK
			return ballotId;
		} catch (Exception e) {
			System.err.println("Sending group ballot create message failed");
			return new byte[0];
		}
	}

	private static List<String> sendE2EGroupBallotCloseMsg(List<String> threemaIds, GroupId groupId,
					byte[] ballotId) {
		try {
			var resArr = e2EHelper.sendGroupBallotCreateMessage(threemaIds, groupId, ballotId,
							"Test poll", State.CLOSED, VotingMode.SINGLE_CHOICE,
							ResultsDisclosureType.INTERMEDIATE, DisplayMode.LIST,
							List.of(new BallotChoice(0, "Pizza", 0, List.of(1, 0), null),
											new BallotChoice(1, "Ananas", 1, List.of(0, 1), null)),
							threemaIds);

			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
			}
			return new ArrayList<>(resArr.getData().length());
		} catch (Exception e) {
			System.err.println("Sending group ballot close message failed");
			return List.of();
		}
	}

	private static void sendE2EBallotVoteMsg(String threemaId, String creator, byte[] ballotId)
					throws ApiException {
		try {
			try {
				var response = e2EHelper.sendBallotVoteMessage(threemaId,
								creator.getBytes(StandardCharsets.UTF_8), ballotId,
								List.of(new VoteChoice(0, false), new VoteChoice(1, true)));
				System.out.println("Message ID: " + response.getData() + " "
								+ response.getStatusCode());
			} catch (Exception e) {
				System.err.println("Sending text message failed");
			}
		} catch (Exception e) {
			System.err.println("Sending ballot vote message failed");
		}
	}

	private static List<String> sendE2EGroupBallotVoteMsg(List<String> threemaIds, GroupId groupId,
					byte[] ballotId) throws ApiException {
		try {
			var resArr = e2EHelper.sendGroupBallotVoteMessage(threemaIds, groupId.getGroupCreator(),
							groupId, ballotId,
							List.of(new VoteChoice(0, false), new VoteChoice(1, true)));

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group ballot vote message failed");
			return List.of();
		}
	}

	private static List<String> sendE2EGroupTextMsg(List<String> threemaId, GroupId groupId)
					throws ApiException {
		try {
			var resArr = e2EHelper.sendGroupTextMessage(threemaId, groupId, "Group Test Message");

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group text message failed");
			return List.of();
		}
	}

	private static void sendE2EFileMsg(String threemaId) throws ApiException {
		try {
			var file = new File("./threema.jpg");
			var thumb = new File("./thumb.png");
			var response = e2EHelper.sendFileMessage(threemaId, file, thumb,
							"End-To-End Encrypted Caption", FileRenderingType.FILE, null,
							Map.of("q", 0xff));

			System.out.println(
							"Message ID: " + response.getData() + " " + response.getStatusCode());
		} catch (Exception e) {
			System.err.println("Sending file message failed");
		}
	}

	private static List<String> sendE2EGroupFileMsg(List<String> threemaIds, GroupId groupId)
					throws ApiException {
		try {
			var file = new File("./threema.jpg");
			var thumb = new File("./thumb.png");
			var resArr = e2EHelper.sendGroupFileMessage(threemaIds, groupId, file, thumb,
							"End-To-End Encrypted Caption", FileRenderingType.MEDIA, null,
							Map.of("q", 0xff));

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group file message failed");
			return List.of();
		}
	}

	private static List<String> sendE2EGroupCreateMsg(GroupId groupId, List<String> threemaIds,
					List<String> members) throws ApiException {
		try {
			var resArr = e2EHelper.sendGroupCreateMessage(threemaIds, members, groupId);

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group create message failed");
			return List.of();
		}
	}

	private static List<String> sendE2EGroupLeaveMsg(GroupId groupId, List<String> threemaIds,
					List<String> members) throws ApiException {
		try {
			e2EHelper.sendGroupCreateMessage(threemaIds, members, groupId);
			var resArr = e2EHelper.sendGroupLeaveMessage(threemaIds, groupId);

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group leave message failed");
			return List.of();
		}
	}

	private static List<String> sendE2EGroupRenameMsg(List<String> threemaIds, GroupId groupId)
					throws ApiException {
		try {
			var resArr = e2EHelper.sendGroupRenameMessage(threemaIds, groupId, "Java SDK Test");

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group rename message failed");
			return List.of();
		}
	}

	private static List<String> sendE2EGroupSetPhotoMsg(List<String> threemaIds, GroupId groupId)
					throws ApiException {
		try {
			var file = new File("./threema.jpg");
			var resArr = e2EHelper.sendGroupSetPhotoMessage(threemaIds, groupId, file);

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group set photo message failed");
			return List.of();
		}
	}

	private static List<String> sendE2EGroupDeletePhotoMsg(List<String> threemaIds, GroupId groupId)
					throws ApiException {
		try {
			var resArr = e2EHelper.sendGroupDeletePhotoMessage(threemaIds, groupId);

			var res = new ArrayList<String>(resArr.getData().length());
			for (int i = 0; i < resArr.getData().length(); i++) {
				System.out.println("Message ID: "
								+ resArr.getData().getJSONObject(i).getString("messageId") + " "
								+ resArr.getStatusCode());
				res.add(resArr.getData().getJSONObject(i).getString("messageId"));
			}
			return res;
		} catch (Exception e) {
			System.err.println("Sending group delete photo message failed");
			return List.of();
		}
	}

	private static void sendE2EGroupRequestSyncMsg(List<String> threemaIds, GroupId groupId) {
		try {
			var response = e2EHelper.sendGroupRequestSyncMsg(threemaIds, groupId);

			System.out.println(
							"Message IDs: " + response.getData() + " " + response.getStatusCode());
		} catch (Exception e) {
			System.err.println("Sending group request sync message failed");
		}
	}

	// WARNING: This test can only be confirmed to work with real data
	private static void testLookupPhone(String phoneNo) {
		try {
			var response = connector.lookupPhone(phoneNo);

			System.out.println(
							"Threema IDs: " + response.getData() + " " + response.getStatusCode());
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				System.out.println("No associated Threema ID was found for phone no: " + phoneNo);
			} else {
				System.err.println("Looking up phone number failed");
			}
		}
	}

	// WARNING: This test can only be confirmed to work with real data
	private static void testLookupEmail(String emailAddr) {
		try {
			var response = connector.lookupEmail(emailAddr);

			System.out.println(
							"Threema IDs: " + response.getData() + " " + response.getStatusCode());
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				System.out.println("No associated Threema ID was found for phone no: " + emailAddr);
			} else {
				System.err.println("Looking up email address failed");
			}
		}
	}
}
