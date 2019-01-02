package fabric;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;

import java.io.*;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.sun.tools.internal.ws.wsdl.parser.Util.fail;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.NOfEvents.createNofEvents;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions.createTransactionOptions;

public class E2eMainProcess {

    private static final TestConfig testConfig = TestConfig.getConfig();
    private Collection<SampleOrg> testSampleOrgs;
    private String EXPECTED_EVENT_NAME = "event";
    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private HFClient client;
    private ChaincodeID chaincodeID;
    private Channel channel;
    private Collection<ProposalResponse> responses;
    private Collection<ProposalResponse> successful = new LinkedList<>();
    private Collection<ProposalResponse> failed = new LinkedList<>();
    private String result_query, result_set;
    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

    public void run(){
        try {
            testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();
            enrollUsersSetup();
            runFabricTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runFabricTest() throws Exception {
        client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
        channel = constructChannel(sampleOrg);
        runChannel(true, sampleOrg);
        //setTest(new String[]{"aaaa", "aasdasdzxcxzcsdad"});
        System.out.println(queryTest("aaa"));
    }

    private Channel constructChannel(SampleOrg sampleOrg) throws InvalidArgumentException, IOException, TransactionException, ProposalException {

        SampleUser peerAdmin = sampleOrg.getPeerAdmin();
        client.setUserContext(peerAdmin);
        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderName : sampleOrg.getOrdererNames()) {
            Properties ordererProperties = testConfig.getOrdererProperties(orderName);

            //example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping rates.
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});
            orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    ordererProperties));
        }
        //Just pick the first orderer in the list to create the channel.
        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        Channel mychannel = client.newChannel("mychannel");
//        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(this.getClass().getResource("/channel-artifacts/channel.tx").getPath()));
//        Channel channel = client.newChannel("mychannel", anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerAdmin));

        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);

            Properties peerProperties = testConfig.getPeerProperties(peerName);
            if (peerProperties == null) {
                peerProperties = new Properties();
            }

            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            mychannel.addPeer(peer);
        }

        for (Orderer orderer : orderers) {
            mychannel.addOrderer(orderer);
        }

        mychannel.initialize();
        return mychannel;
    }

    private void runChannel(boolean isInstallChaincode, SampleOrg sampleOrg) {

        class ChainCodeEventCapture{
            final String handle;
            final BlockEvent blockEvent;
            final ChaincodeEvent chaincodeEvent;

            ChainCodeEventCapture(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent){
                this.handle = handle;
                this.blockEvent = blockEvent;
                this.chaincodeEvent = chaincodeEvent;
            }
        }
        final Vector<ChainCodeEventCapture> chainCodeEvents = new Vector<>();
        try{

            final String channelName = channel.getName();
            Collection<Orderer> orderers = channel.getOrderers();
            String chaincodeEventListenerHandler = channel.registerChaincodeEventListener(Pattern.compile(".*"),
                    Pattern.compile(Pattern.quote(EXPECTED_EVENT_NAME)),
                    (handle, blockEvent, chaincodeEvent) -> {
                        chainCodeEvents.add(new ChainCodeEventCapture(handle, blockEvent, chaincodeEvent));
                        String es = blockEvent.getPeer() != null ? blockEvent.getPeer().getName() : blockEvent.getEventHub().getName();
                    });

            ChaincodeID.Builder chaincodeIDbuilder = ChaincodeID.newBuilder().setName("t2")
                    .setVersion("1")
                    .setPath("github/com/example_cc");
            chaincodeID = chaincodeIDbuilder.build();
            if (isInstallChaincode){
                responses = installChaincode(client, chaincodeID, sampleOrg, channel);
                for (ProposalResponse response : responses) {
                    if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                        out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                        successful.add(response);
                    } else {
                        failed.add(response);
                    }
                }

                System.out.println(String.format("Received %d install proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size()));
                responses.clear();
                initChaincode();
            }
          //  initChaincode();
            successful.clear();
            failed.clear();

        } catch (InvalidArgumentException | ProposalException e) {
            e.printStackTrace();
        }
    }






    private Collection<ProposalResponse> instantiateChaincode() {

        return null;
    }

    private Collection<ProposalResponse> installChaincode(HFClient client, ChaincodeID chaincodeID, SampleOrg sampleOrg, Channel channel) throws InvalidArgumentException, ProposalException {
        client.setUserContext(sampleOrg.getPeerAdmin());

        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);
        // installProposalRequest.setChaincodeSourceLocation(new File(this.getClass().getResource("chaincode/example_cc.go").getPath());
        //  System.out.println(Paths.get("src/main/java/chaincode"));
        installProposalRequest.setChaincodeSourceLocation(new File("src/main/java/chaincode"));
        installProposalRequest.setChaincodeVersion("1");
        installProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);

        Collection<Peer> peers = channel.getPeers();
        return client.sendInstallProposal(installProposalRequest, peers);
    }

    private void initChaincode(){
        //// Instantiate chaincode.
        try {
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();

            instantiateProposalRequest.setProposalWaitTime(1300000);
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[]{"aaa", "100"});
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);
            //  client.newUpgradeProposalRequest()
            /*
              policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
              See README.md Chaincode endorsement policies section for more details.
            */

            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File("src/main/resources/component/endorsement_policy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

              out("Sending instantiateProposalRequest to all peers with arguments");
            successful.clear();
            failed.clear();

            //Send responses both ways with specifying peers and by using those on the channel.
            responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());

            for (ProposalResponse response : responses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                     out("Succesful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                } else {
                    failed.add(response);
                }
            }
            out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                for (ProposalResponse fail : failed) {

                     out("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + fail.getMessage() + ", on peer" + fail.getPeer());

                }
                ProposalResponse first = failed.iterator().next();
                fail("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
            }


            Channel.NOfEvents nOfEvents = createNofEvents();
            if (!channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)).isEmpty()) {
                nOfEvents.addPeers(channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)));
            }
            if (!channel.getEventHubs().isEmpty()) {
                nOfEvents.addEventHubs(channel.getEventHubs());
            }
             out(successful.toString());
            channel.sendTransaction(successful, createTransactionOptions() //Basically the default options but shows it's usage.
                    .userContext(client.getUserContext()) //could be a different user context. this is the default.
                    .shuffleOrders(false) // don't shuffle any orderers the default is true.
                    .orderers(channel.getOrderers()) // specify the orderers we want to try this transaction. Fails once all Orderers are tried.
                    .nOfEvents(nOfEvents) // The events to signal the completion of the interest in the transaction
            );

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void enrollUsersSetup() throws Exception {
        for (SampleOrg sampleOrg : testSampleOrgs) {
            final String orgName = sampleOrg.getName();
            final String mspid = sampleOrg.getMSPID();
            HFCAClient hfcaClient = sampleOrg.getCAClient();
            hfcaClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            HFCAInfo hfcaInfo = hfcaClient.info();
            String infoName = hfcaInfo.getCAName();
            if (infoName != null && !infoName.isEmpty()){
                assert hfcaClient.getCAName().equals(infoName);
            }

            SampleUser admin = new SampleUser("admin",orgName,null);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(hfcaClient.enroll(admin.getName(), "adminpw"));
                admin.setMspId(mspid);
            }

//            SampleUser user = new SampleUser("user19", orgName, null);
//            if (!user.isRegistered()) {  // users need to be registered AND enrolled
//                RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
//                user.setEnrollmentSecret(hfcaClient.register(rr, admin));
//            }
//            if (!user.isEnrolled()) {
//                user.setEnrollment(hfcaClient.enroll(user.getName(), user.getEnrollmentSecret()));
//                user.setMspId(mspid);
//            }
            final String sampleOrgName = sampleOrg.getName();
            final String sampleOrgDomainName = sampleOrg.getDomainName();

            SampleUser peerAdmin = new SampleUser("peerOrg1Admin", orgName, null);
            peerAdmin.setMspId(mspid);
            String certificateFile = new StringBuilder().append("/component/crypto-config/peerOrganizations/")
                    .append(sampleOrgDomainName)
                    .append(String.format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName))
                    .toString();
            String privateKeyFile = new StringBuilder().append("/component/crypto-config/peerOrganizations/")
                    .append(sampleOrgDomainName)
                    .append(String.format("/users/Admin@%s/msp/keystore", sampleOrgDomainName))
                    .toString();
            String certificate = new String(IOUtils.toByteArray(E2eMainProcess.class.getResourceAsStream(certificateFile)));
            PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(Util.findFileSk(Paths.get(E2eMainProcess.class.getResource(privateKeyFile).getPath()).toFile()))));
            peerAdmin.setEnrollment(new E2eMainProcess.SampleEnrollment(certificate,privateKey));

            sampleOrg.setPeerAdmin(peerAdmin);
//            sampleOrg.addUser(user);
            sampleOrg.setAdmin(admin);
        }
    }

    class SampleEnrollment implements Enrollment, Serializable{
        private String cert;
        private PrivateKey key;

        SampleEnrollment(String cert, PrivateKey key){
            this.cert = cert;
            this.key = key;
        }

        @Override
        public PrivateKey getKey() {
            return this.key;
        }

        @Override
        public String getCert() {
            return this.cert;
        }
    }

    static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        final Reader pemReader = new StringReader(new String(data));

        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);

        return privateKey;
    }

    public String queryTest(String str) {

        result_query = null;
        try {
            QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(str);
            queryByChaincodeRequest.setFcn("query");
            queryByChaincodeRequest.setChaincodeID(chaincodeID);

            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
            queryByChaincodeRequest.setTransientMap(tm);

            responses = channel.queryByChaincode(queryByChaincodeRequest);
            for (ProposalResponse response : responses) {
                if (!response.isVerified() || response.getStatus() != ProposalResponse.Status.SUCCESS) {
                    System.out.println("Failed query proposal from peer " + response.getPeer().getName() + " status: " + response.getStatus() +
                            ". Messages: " + response.getMessage()
                            + ". Was verified : " + response.isVerified());
                } else {
                    String payload = response.getProposalResponse().getResponse().getPayload().toStringUtf8();
                    System.out.println(String.format("Query payload of b from peer %s returned %s", response.getPeer().getName(), payload));
                    result_query = payload;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result_query;
    }


    public String setTest(String[] str){
        result_set = null;
        try {

            successful.clear();
            failed.clear();

            //ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
           // chaincodeEndorsementPolicy.fromYamlFile(new File("src/main/resources/component/endorsement_policy.yaml"));
            ///////////////
            /// Send transaction proposal to all peers
            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            //transactionProposalRequest.setFcn("invoke");
            transactionProposalRequest.setFcn("add");
            transactionProposalRequest.setProposalWaitTime(300000);
            transactionProposalRequest.setArgs(str);
            // transactionProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
            //  out("ffffff" + str);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); //Just some extra junk in transient map
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); // ditto
            tm2.put("result", ":)".getBytes(UTF_8));  // This should be returned in the payload see chaincode why.


            tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);  //This should trigger an event see chaincode why.

            transactionProposalRequest.setTransientMap(tm2);

            out("sending transactionProposal to all peers with new key/value : %s / %s", str[0], str[1]);
            result_set += "sending transactionProposal to all peers with new key/value : " + str[0] + " / " + str[1] + "\n";

            //  Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposalToEndorsers(transactionProposalRequest);
            Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
            for (ProposalResponse response : transactionPropResp) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    result_set += "Successful transaction proposal response Txid: " + response.getTransactionID() + " from peer " + response.getPeer().getName() + "\n";
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }

            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                    transactionPropResp.size(), successful.size(), failed.size());
            result_set += "Received " + transactionPropResp.size() + "transaction proposal responses. Successful+verified: "
                    + successful.size() + ". Failed: " + failed.size() + "\n";
            if (failed.size() > 0) {
                ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                fail("Not enough endorsers for invoke(set + " + str[0] + ", " + str[1] + "):" + failed.size() + " endorser error: " +
                        firstTransactionProposalResponse.getMessage() +
                        ". Was verified: " + firstTransactionProposalResponse.isVerified());

                result_set += "Not enough endorsers for invoke(set + " + str[0] + ", " + str[1] + "):" + failed.size() + " endorser error: " +
                        firstTransactionProposalResponse.getMessage() +
                        ". Was verified: " + firstTransactionProposalResponse.isVerified() + "\n";
            }

            // Check that all the proposals are consistent with each other. We should have only one set
            // where all the proposals above are consistent. Note the when sending to Orderer this is done automatically.
            //  Shown here as an example that applications can invoke and select.
            // See org.hyperledger.fabric.sdk.proposal.consistency_validation config property.
            Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
            if (proposalConsistencySets.size() != 1) {
                fail(format("Expected only one set of consistent proposal responses but got %d", proposalConsistencySets.size()));
            }
            //  out("Successfully received transaction proposal responses.");
            result_set += "Successfully received transaction proposal responses.\n";
            //  System.exit(10);

            ProposalResponse resp = successful.iterator().next();
            byte[] x = resp.getChaincodeActionResponsePayload(); // This is the data returned by the chaincode.
            String resultAsString = null;
            if (x != null) {
                resultAsString = new String(x, UTF_8);
            }
            ////////////////////////////
            // Send Transaction Transaction to orderer
            out("Sending chaincode transaction(set new key/value) to orderer.");
            result_set += "Sending chaincode transaction to orderer.\n";
            for(ProposalResponse p : successful)
                System.out.println(p.getPeer().getName() + ", " + p.getChaincodeActionResponseReadWriteSetInfo().getNsRwsetCount());
            System.out.println();

              channel.sendTransaction(successful,channel.getOrderers()).get(3000000, TimeUnit.SECONDS);
//            channel.sendTransaction(successful, createTransactionOptions() //Basically the default options but shows it's usage.
//                    .userContext(client.getUserContext()) //could be a different user context. this is the default.
//                    .shuffleOrders(false) // don't shuffle any orderers the default is true.
//                    .orderers(channel.getOrderers()) // specify the orderers we want to try this transaction. Fails once all Orderers are tried.
//            ).get(300000,TimeUnit.SECONDS);

        } catch (Exception e) {
            //out("Caught an exception while invoking chaincode");
            e.printStackTrace();
            fail("Failed invoking chaincode with error : " + e.getMessage());
        }

        return result_set;
    }
}

