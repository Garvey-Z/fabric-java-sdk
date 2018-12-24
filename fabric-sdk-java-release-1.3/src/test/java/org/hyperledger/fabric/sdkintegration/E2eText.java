package org.hyperledger.fabric.sdkintegration;

import org.bouncycastle.openssl.PEMWriter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.NOfEvents.createNofEvents;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions.createTransactionOptions;
import static org.hyperledger.fabric.sdk.testutils.TestUtils.resetConfig;
import static org.hyperledger.fabric.sdk.testutils.TestUtils.testRemovingAddingPeersOrderers;
import static org.junit.Assert.*;

/**
 * Test end to end scenario
 */
public class E2eText  {

    private static final TestConfig testConfig = TestConfig.getConfig();
    static final String TEST_ADMIN_NAME = "admin";
    private static final String TEST_FIXTURES_PATH = "src/test/fixture";

    private static Random random = new Random();

    private static final String FOO_CHANNEL_NAME = "foo";
    private static final String BAR_CHANNEL_NAME = "bar";

    private static final int DEPLOYWAITTIME = testConfig.getDeployWaitTime();

    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";
    private static final Map<String, String> TX_EXPECTED;
    private Collection<ProposalResponse> successful = new LinkedList<>();
    private Collection<ProposalResponse> failed = new LinkedList<>();
    //
    private Channel fooChannel;
    private HFClient client;
    private SampleOrg sampleOrg;
    private ChaincodeID chaincodeID;
    //
    private CompletableFuture<BlockEvent.TransactionEvent> completableFuture = new CompletableFuture <> ();

    //query
    private String query_result;
    String testName = "End2endIT";

    String CHAIN_CODE_FILEPATH = "sdkintegration/gocc/sample1";
    String CHAIN_CODE_NAME = "example_cc_go";
    String CHAIN_CODE_PATH = "github.com/example_cc";
    String CHAIN_CODE_VERSION = "1";
    Type CHAIN_CODE_LANG = Type.GO_LANG;

    static {
        TX_EXPECTED = new HashMap<>();
        TX_EXPECTED.put("readset1", "Missing readset for channel bar block 1");
        TX_EXPECTED.put("writeset1", "Missing writeset for channel bar block 1");
    }

    private final TestConfigHelper configHelper = new TestConfigHelper();
    String testTxID = null;  // save the CC invoke TxID and use in queries
    SampleStore sampleStore = null;
    private Collection<SampleOrg> testSampleOrgs;
    static String testUser1 = "user1";

    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }
    //CHECKSTYLE.ON: Method length is 320 lines (max allowed is 150).

    static String printableString(final String string) {
        int maxLogStringLength = 64;
        if (string == null || string.length() == 0) {
            return string;
        }

        String ret = string.replaceAll("[^\\p{Print}]", "?");

        ret = ret.substring(0, Math.min(ret.length(), maxLogStringLength)) + (ret.length() > maxLogStringLength ? "..." : "");

        return ret;

    }

   // @Before
    public void checkConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MalformedURLException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
        out("\n\n\nRUNNING: %s.\n", testName);
        //   configHelper.clearConfig();
        //   assertEquals(256, Config.getConfig().getSecurityLevel());
        resetConfig();
        configHelper.customizeConfig();

        testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();
        //Set up hfca for each sample org

        for (SampleOrg sampleOrg : testSampleOrgs) {
            String caName = sampleOrg.getCAName(); //Try one of each name and no name.
            if (caName != null && !caName.isEmpty()) {
                sampleOrg.setCAClient(HFCAClient.createNewInstance(caName, sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
            } else {
                sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
            }
        }
    }

    Map<String, Properties> clientTLSProperties = new HashMap<>();

    File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");

    //@Test
    public void setup() throws Exception {

        //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
        //   MUST be replaced with more robust application implementation  (Database, LDAP)

        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }
        sampleStore = new SampleStore(sampleStoreFile);
        enrollUsersSetup(sampleStore); //This enrolls users with fabric ca and setups sample store to get users later.
        runFabricTest(sampleStore); //Runs Fabric tests with constructing channels, joining peers, exercising chaincode

    }

    public void runFabricTest(final SampleStore sampleStore) throws Exception {

        ////////////////////////////
        // Setup client

        //Create instance of client.
         client = HFClient.createNewInstance();

        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        ////////////////////////////
        //Construct and run the channels
         sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
        fooChannel = constructChannel(FOO_CHANNEL_NAME, client, sampleOrg);
        sampleStore.saveChannel(fooChannel);

        try{
            class ChaincodeEventCapture { //A test class to capture chaincode events
                final String handle;
                final BlockEvent blockEvent;
                final ChaincodeEvent chaincodeEvent;

                ChaincodeEventCapture(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {
                    this.handle = handle;
                    this.blockEvent = blockEvent;
                    this.chaincodeEvent = chaincodeEvent;
                }
            }

            // The following is just a test to see if peers and orderers can be added and removed.
            // not pertinent to the code flow.
            testRemovingAddingPeersOrderers(client, fooChannel);

            Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture chaincode events.
                final String channelName = fooChannel.getName();
                boolean isFooChain = FOO_CHANNEL_NAME.equals(channelName);
                out("Running channel %s", channelName);

                Collection<Orderer> orderers = fooChannel.getOrderers();
                Collection<ProposalResponse> responses;
           //     Collection<ProposalResponse> successful = new LinkedList<>();
             //   Collection<ProposalResponse> failed = new LinkedList<>();

                // Register a chaincode event listener that will trigger for any chaincode id and only for EXPECTED_EVENT_NAME event.

                String chaincodeEventListenerHandle = fooChannel.registerChaincodeEventListener(Pattern.compile(".*"),
                        Pattern.compile(Pattern.quote(EXPECTED_EVENT_NAME)),
                        (handle, blockEvent, chaincodeEvent) -> {

                            chaincodeEvents.add(new ChaincodeEventCapture(handle, blockEvent, chaincodeEvent));

                            String es = blockEvent.getPeer() != null ? blockEvent.getPeer().getName() : blockEvent.getEventHub().getName();
                            out("RECEIVED Chaincode event with handle: %s, chaincode Id: %s, chaincode event name: %s, "
                                            + "transaction id: %s, event payload: \"%s\", from eventhub: %s",
                                    handle, chaincodeEvent.getChaincodeId(),
                                    chaincodeEvent.getEventName(),
                                    chaincodeEvent.getTxId(),
                                    new String(chaincodeEvent.getPayload()), es);

                        });

                //For non foo channel unregister event listener to test events are not called.
                if (!isFooChain) {
                    fooChannel.unregisterChaincodeEventListener(chaincodeEventListenerHandle);
                    chaincodeEventListenerHandle = null;

                }

                ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                        .setVersion(CHAIN_CODE_VERSION);
                if (null != CHAIN_CODE_PATH) {
                    chaincodeIDBuilder.setPath(CHAIN_CODE_PATH);

                }
                chaincodeID = chaincodeIDBuilder.build();

                if (true) {
                    ////////////////////////////
                    // Install Proposal Request
                    //

                    client.setUserContext(sampleOrg.getPeerAdmin());

                    out("Creating install proposal");

                    InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
                    installProposalRequest.setChaincodeID(chaincodeID);

                    if (isFooChain) {
                        // on foo chain install from directory..

                        ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
                        installProposalRequest.setChaincodeSourceLocation(Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH).toFile());

                        if (testConfig.isFabricVersionAtOrAfter("1.1")) { // Fabric 1.1 added support for  META-INF in the chaincode image.

                            //This sets an index on the variable a in the chaincode // see http://hyperledger-fabric.readthedocs.io/en/master/couchdb_as_state_database.html#using-couchdb-from-chaincode
                            // The file IndexA.json as part of the META-INF will be packaged with the source to create the index.
                            installProposalRequest.setChaincodeMetaInfLocation(new File("src/test/fixture/meta-infs/end2endit"));
                        }
                    } else {
                        // On bar chain install from an input stream.

                        // For inputstream if indicies are desired the application needs to make sure the META-INF is provided in the stream.
                        // The SDK does not change anything in the stream.

                        if (CHAIN_CODE_LANG.equals(Type.GO_LANG)) {

                            installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                                    (Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH, "src", CHAIN_CODE_PATH).toFile()),
                                    Paths.get("src", CHAIN_CODE_PATH).toString()));
                        } else {
                            installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                                    (Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH).toFile()),
                                    "src"));
                        }
                    }

                    installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);
                    installProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);

                    out("Sending install proposal");

                    ////////////////////////////
                    // only a client from the same org as the peer can issue an install request
                    int numInstallProposal = 0;
                    //    Set<String> orgs = orgPeers.keySet();
                    //   for (SampleOrg org : testSampleOrgs) {

                    Collection<Peer> peers = fooChannel.getPeers();
                    numInstallProposal = numInstallProposal + peers.size();
                    responses = client.sendInstallProposal(installProposalRequest, peers);

                    for (ProposalResponse response : responses) {
                        if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                            out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                            successful.add(response);
                        } else {
                            failed.add(response);
                        }
                    }

                    //   }
                    out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());

                    if (failed.size() > 0) {
                        ProposalResponse first = failed.iterator().next();
                        fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
                    }
                }
            //// Instantiate chaincode.
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();

            instantiateProposalRequest.setProposalWaitTime(DEPLOYWAITTIME);
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[]{"a", "100", "b", "" + (200), "c", "qqwe"});
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
            chaincodeEndorsementPolicy.fromYamlFile(new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            out("Sending instantiateProposalRequest to all peers with arguments");
            successful.clear();
            failed.clear();

            if (isFooChain) {  //Send responses both ways with specifying peers and by using those on the channel.
                responses = fooChannel.sendInstantiationProposal(instantiateProposalRequest, fooChannel.getPeers());
            } else {
                responses = fooChannel.sendInstantiationProposal(instantiateProposalRequest);
            }
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
            if (!fooChannel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)).isEmpty()) {
                nOfEvents.addPeers(fooChannel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)));
            }
            if (!fooChannel.getEventHubs().isEmpty()) {
                nOfEvents.addEventHubs(fooChannel.getEventHubs());
            }
            out(successful.toString());
            completableFuture = fooChannel.sendTransaction(successful, createTransactionOptions() //Basically the default options but shows it's usage.
                    .userContext(client.getUserContext()) //could be a different user context. this is the default.
                    .shuffleOrders(false) // don't shuffle any orderers the default is true.
                    .orderers(fooChannel.getOrderers()) // specify the orderers we want to try this transaction. Fails once all Orderers are tried.
                    .nOfEvents(nOfEvents) // The events to signal the completion of the interest in the transaction
            );

//            for(int i = 0; i < 10; ++ i){
//              //  Thread.sleep(2000);
//                String val = RandomNum.createRandomString(1000);
//                set(new String[] {"a" + i , val});
//            }

        }catch (Exception e) {
            e.printStackTrace();
        }


        //
      //  set(client, fooChannel, sampleOrg, new String[] {"dd", "asdfjnxvnxkafsaklj"});
        //
        /**
         * channel shutdown
         */
//        assertFalse(fooChannel.isShutdown());
//        fooChannel.shutdown(true); // Force foo channel to shutdown clean up resources.
//        assertTrue(fooChannel.isShutdown());
//
//        assertNull(client.getChannel(FOO_CHANNEL_NAME));
//        out("\n");











//
//        sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg2");
//        Channel barChannel = constructChannel(BAR_CHANNEL_NAME, client, sampleOrg);
//        assertTrue(barChannel.isInitialized());
//        /**
//         * sampleStore.saveChannel uses {@link Channel#serializeChannel()}
//         */
//        sampleStore.saveChannel(barChannel);
//        assertFalse(barChannel.isShutdown());
//        runChannel(client, barChannel, true, sampleOrg, 100); //run a newly constructed bar channel with different b value!
//        //let bar channel just shutdown so we have both scenarios.
//
//        out("\nTraverse the blocks for chain %s ", barChannel.getName());
//
//        blockWalker(client, barChannel);
//
//        assertFalse(barChannel.isShutdown());
//        assertTrue(barChannel.isInitialized());
        out("That's all folks!");
    }

    /**
     * Will register and enroll users persisting them to samplestore.
     *
     * @param sampleStore
     * @throws Exception
     */
    public void enrollUsersSetup(SampleStore sampleStore) throws Exception {
        ////////////////////////////
        //Set up USERS

        //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface

        ////////////////////////////
        // get users for all orgs

        out("***** Enrolling Users *****");
        for (SampleOrg sampleOrg : testSampleOrgs) {

            HFCAClient ca = sampleOrg.getCAClient();

            final String orgName = sampleOrg.getName();
            final String mspid = sampleOrg.getMSPID();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            if (testConfig.isRunningFabricTLS()) {
                //This shows how to get a client TLS certificate from Fabric CA
                // we will use one client TLS certificate for orderer peers etc.
                final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
                enrollmentRequestTLS.addHost("localhost");
                enrollmentRequestTLS.setProfile("tls");
                final Enrollment enroll = ca.enroll("admin", "adminpw", enrollmentRequestTLS);
                final String tlsCertPEM = enroll.getCert();
                final String tlsKeyPEM = getPEMStringFromPrivateKey(enroll.getKey());

                final Properties tlsProperties = new Properties();

                tlsProperties.put("clientKeyBytes", tlsKeyPEM.getBytes(UTF_8));
                tlsProperties.put("clientCertBytes", tlsCertPEM.getBytes(UTF_8));
                clientTLSProperties.put(sampleOrg.getName(), tlsProperties);
                //Save in samplestore for follow on tests.
                sampleStore.storeClientPEMTLCertificate(sampleOrg, tlsCertPEM);
                sampleStore.storeClientPEMTLSKey(sampleOrg, tlsKeyPEM);
            }

            HFCAInfo info = ca.info(); //just check if we connect at all.
            assertNotNull(info);
            String infoName = info.getCAName();
            if (infoName != null && !infoName.isEmpty()) {
                assertEquals(ca.getCAName(), infoName);
            }

            SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
                admin.setMspId(mspid);
            }

            SampleUser user = sampleStore.getMember(testUser1, sampleOrg.getName());
            if (!user.isRegistered()) {  // users need to be registered AND enrolled
                RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
                user.setEnrollmentSecret(ca.register(rr, admin));
            }
            if (!user.isEnrolled()) {
                user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
                user.setMspId(mspid);
            }

            final String sampleOrgName = sampleOrg.getName();
            final String sampleOrgDomainName = sampleOrg.getDomainName();

            SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                    Util.findFileSk(Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/",
                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                    Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());
            sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode

            sampleOrg.addUser(user);
            sampleOrg.setAdmin(admin); // The admin of this org --
        }
    }

    static String getPEMStringFromPrivateKey(PrivateKey privateKey) throws IOException {
        StringWriter pemStrWriter = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(pemStrWriter);

        pemWriter.writeObject(privateKey);

        pemWriter.close();

        return pemStrWriter.toString();
    }

    Map<String, Long> expectedMoveRCMap = new HashMap<>(); // map from channel name to move chaincode's return code.



    Channel constructChannel(String name, HFClient client, SampleOrg sampleOrg) throws Exception {
        ////////////////////////////
        //Construct the channel
        //

        out("Constructing channel %s", name);

        //boolean doPeerEventing = false;
        boolean doPeerEventing = !testConfig.isRunningAgainstFabric10() && BAR_CHANNEL_NAME.equals(name);
//        boolean doPeerEventing = !testConfig.isRunningAgainstFabric10() && FOO_CHANNEL_NAME.equals(name);
        //Only peer Admin org
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

        String path = TEST_FIXTURES_PATH + "/sdkintegration/e2e-2Orgs/" + testConfig.getFabricConfigGenVers() + "/" + name + ".tx";
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(path));

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        Channel newChannel = client.newChannel(name, anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerAdmin));

        out("Created channel %s", name);

        boolean everyother = true; //test with both cases when doing peer eventing.
        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);

            Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }

            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            if (testConfig.isFabricVersionAtOrAfter("1.3")) {
                newChannel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.ENDORSING_PEER, PeerRole.LEDGER_QUERY, PeerRole.CHAINCODE_QUERY, PeerRole.EVENT_SOURCE))); //Default is all roles.

            } else {
                if (doPeerEventing && everyother) {
                    newChannel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.ENDORSING_PEER, PeerRole.LEDGER_QUERY, PeerRole.CHAINCODE_QUERY, PeerRole.EVENT_SOURCE))); //Default is all roles.
                } else {
                    // Set peer to not be all roles but eventing.
                    newChannel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.ENDORSING_PEER, PeerRole.LEDGER_QUERY, PeerRole.CHAINCODE_QUERY)));
                }
            }
            out("Peer %s joined channel %s", peerName, name);
            everyother = !everyother;
        }
        //just for testing ...
        if (doPeerEventing || testConfig.isFabricVersionAtOrAfter("1.3")) {
            // Make sure there is one of each type peer at the very least.
            assertFalse(newChannel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)).isEmpty());
            assertFalse(newChannel.getPeers(PeerRole.NO_EVENT_SOURCE).isEmpty());
        }

        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }

        for (String eventHubName : sampleOrg.getEventHubNames()) {

            final Properties eventHubProperties = testConfig.getEventHubProperties(eventHubName);

            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
                    eventHubProperties);
            newChannel.addEventHub(eventHub);
        }

        newChannel.initialize();

        out("Finished initialization channel %s", name);

        //Just checks if channel can be serialized and deserialized .. otherwise this is just a waste :)
        byte[] serializedChannelBytes = newChannel.serializeChannel();
        newChannel.shutdown(true);

        return client.deSerializeChannel(serializedChannelBytes).initialize();

    }

    private void waitOnFabric(int additional) {
        //NOOP today

    }

    void set(String[] str){
        try {
            final String channelName = fooChannel.getName();
            boolean isFooChain = FOO_CHANNEL_NAME.equals(channelName);
            out("Running channel %s", channelName);

            //Collection<Orderer> orderers = channel.getOrderers();

          //  Collection<ProposalResponse> responses;

//            Channel.NOfEvents nOfEvents = createNofEvents();
//            if (!fooChannel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)).isEmpty()) {
//                nOfEvents.addPeers(fooChannel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)));
//            }
//            if (!fooChannel.getEventHubs().isEmpty()) {
//                nOfEvents.addEventHubs(fooChannel.getEventHubs());
//            }
//            out(successful.toString());
//            fooChannel.sendTransaction(successful, createTransactionOptions() //Basically the default options but shows it's usage.
//                    .userContext(client.getUserContext()) //could be a different user context. this is the default.
//                    .shuffleOrders(false) // don't shuffle any orderers the default is true.
//                    .orderers(fooChannel.getOrderers()) // specify the orderers we want to try this transaction. Fails once all Orderers are tried.
//                    .nOfEvents(nOfEvents) // The events to signal the completion of the interest in the transaction
//            )transaction.
            completableFuture.thenApply(transactionEvent -> {

                waitOnFabric(0);

                assertTrue(transactionEvent.isValid()); // must be valid to be here.

                assertNotNull(transactionEvent.getSignature()); //musth have a signature.
                BlockEvent blockEvent = transactionEvent.getBlockEvent(); // This is the blockevent that has this transaction.
                assertNotNull(blockEvent.getBlock()); // Make sure the RAW Fabric block is returned.

                out("Finished instantiate transaction with transaction id %s", transactionEvent.getTransactionID());

            try {
                    assertEquals(blockEvent.getChannelId(), fooChannel.getName());
                    successful.clear();
                    failed.clear();

                    client.setUserContext(sampleOrg.getUser(testUser1));

                    ///////////////
                    /// Send transaction proposal to all peers
                    TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
                    transactionProposalRequest.setChaincodeID(chaincodeID);
                    transactionProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
                    //transactionProposalRequest.setFcn("invoke");
                    transactionProposalRequest.setFcn("add");
                    transactionProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
                    transactionProposalRequest.setArgs(str);

                    Map<String, byte[]> tm2 = new HashMap<>();
                    tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); //Just some extra junk in transient map
                    tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); // ditto
                    tm2.put("result", ":)".getBytes(UTF_8));  // This should be returned in the payload see chaincode why.
                    if (Type.GO_LANG.equals(CHAIN_CODE_LANG) && testConfig.isFabricVersionAtOrAfter("1.2")) {

                        expectedMoveRCMap.put(channelName, random.nextInt(300) + 100L); // the chaincode will return this as status see chaincode why.
                        tm2.put("rc", (expectedMoveRCMap.get(channelName) + "").getBytes(UTF_8));  // This should be returned see chaincode why.
                        // 400 and above results in the peer not endorsing!
                    } else {
                        expectedMoveRCMap.put(channelName, 200L); // not really supported for Java or Node.
                    }

                    tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);  //This should trigger an event see chaincode why.

                    transactionProposalRequest.setTransientMap(tm2);

                    out("sending transactionProposal to all peers with new key/value : %s / %s", str[0], str[1]);

                    //  Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposalToEndorsers(transactionProposalRequest);
                    Collection<ProposalResponse> transactionPropResp = fooChannel.sendTransactionProposal(transactionProposalRequest, fooChannel.getPeers());
                    for (ProposalResponse response : transactionPropResp) {
                        if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                            out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                            successful.add(response);
                        } else {
                            failed.add(response);
                        }
                    }

                    out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                            transactionPropResp.size(), successful.size(), failed.size());
                    if (failed.size() > 0) {
                        ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                        fail("Not enough endorsers for invoke(move a,b,100):" + failed.size() + " endorser error: " +
                                firstTransactionProposalResponse.getMessage() +
                                ". Was verified: " + firstTransactionProposalResponse.isVerified());
                    }

                    // Check that all the proposals are consistent with each other. We should have only one set
                    // where all the proposals above are consistent. Note the when sending to Orderer this is done automatically.
                    //  Shown here as an example that applications can invoke and select.
                    // See org.hyperledger.fabric.sdk.proposal.consistency_validation config property.
                    Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
                    if (proposalConsistencySets.size() != 1) {
                        fail(format("Expected only one set of consistent proposal responses but got %d", proposalConsistencySets.size()));
                    }
                    out("Successfully received transaction proposal responses.");

                    //  System.exit(10);

                    ProposalResponse resp = successful.iterator().next();
                    byte[] x = resp.getChaincodeActionResponsePayload(); // This is the data returned by the chaincode.
                    String resultAsString = null;
                    if (x != null) {
                        resultAsString = new String(x, UTF_8);
                    }
                    assertEquals(":)", resultAsString);

                    assertEquals(expectedMoveRCMap.get(channelName).longValue(), resp.getChaincodeActionResponseStatus()); //Chaincode's status.

                    TxReadWriteSetInfo readWriteSetInfo = resp.getChaincodeActionResponseReadWriteSetInfo();
                    //See blockwalker below how to transverse this
                    assertNotNull(readWriteSetInfo);
                    assertTrue(readWriteSetInfo.getNsRwsetCount() > 0);

                    ChaincodeID cid = resp.getChaincodeID();
                    assertNotNull(cid);
                    final String path = cid.getPath();
                    if (null == CHAIN_CODE_PATH) {
                        assertTrue(path == null || "".equals(path));

                    } else {

                        assertEquals(CHAIN_CODE_PATH, path);

                    }

                    assertEquals(CHAIN_CODE_NAME, cid.getName());
                    assertEquals(CHAIN_CODE_VERSION, cid.getVersion());

                    ////////////////////////////
                    // Send Transaction Transaction to orderer
                    out("Sending chaincode transaction(set new key/value) to orderer.");
                    return fooChannel.sendTransaction(successful).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

                } catch (Exception e) {
                    out("Caught an exception while invoking chaincode");
                    e.printStackTrace();
                    fail("Failed invoking chaincode with error : " + e.getMessage());
                }

                return null;

            }).thenApply(transactionEvent -> {
                try {

                    waitOnFabric(0);

                    assertTrue(transactionEvent.isValid()); // must be valid to be here.
                    out("Finished transaction with transaction id %s", transactionEvent.getTransactionID());
                    testTxID = transactionEvent.getTransactionID(); // used in the channel queries later

                    ////////////////////////////
                    // Send Query Proposal to all peers
                    //
                    out("Now query chaincode for the value of %s.",str[0]);
                    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
                    queryByChaincodeRequest.setArgs(str[0]);
                    queryByChaincodeRequest.setFcn("query");
                    queryByChaincodeRequest.setChaincodeID(chaincodeID);

                    Map<String, byte[]> tm2 = new HashMap<>();
                    tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
                    tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
                    queryByChaincodeRequest.setTransientMap(tm2);

                    Collection<ProposalResponse> queryProposals = fooChannel.queryByChaincode(queryByChaincodeRequest, fooChannel.getPeers());
                    for (ProposalResponse proposalResponse : queryProposals) {
                        if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                            fail("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                                    ". Messages: " + proposalResponse.getMessage()
                                    + ". Was verified : " + proposalResponse.isVerified());
                        } else {
                            String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                            out("Query payload of %s from peer %s returned %s", str[0], proposalResponse.getPeer().getName(), payload);
                        }
                    }

                    return null;
                } catch (Exception e) {
                    out("Caught exception while running query");
                    e.printStackTrace();
                    fail("Failed during chaincode query with error : " + e.getMessage());
                }

                return null;
            }).exceptionally(e -> {
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {
                        throw new AssertionError(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()), e);
                    }
                }

                throw new AssertionError(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()), e);

            }).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

            out("Running for Channel %s done", channelName);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    String query(String str){
      //  String result = "";
        try{

            completableFuture.thenApply(transactionEvent -> {
                try {
                    waitOnFabric(0);
                    assertTrue(transactionEvent.isValid()); // must be valid to be here.
                    out("Finished transaction with transaction id %s", transactionEvent.getTransactionID());
                    testTxID = transactionEvent.getTransactionID(); // used in the channel queries later

                    ////////////////////////////
                    // Send Query Proposal to all peers
                    //
                    out("Now query chaincode for the value of %s.",str);
                    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
                    queryByChaincodeRequest.setArgs(str);
                    queryByChaincodeRequest.setFcn("query");
                    queryByChaincodeRequest.setChaincodeID(chaincodeID);

                    Map<String, byte[]> tm2 = new HashMap<>();
                    tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
                    tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
                    queryByChaincodeRequest.setTransientMap(tm2);
                    //String result = null;
                    Collection<ProposalResponse> queryProposals = fooChannel.queryByChaincode(queryByChaincodeRequest, fooChannel.getPeers());
                    for (ProposalResponse proposalResponse : queryProposals) {
                        if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                            fail("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                                    ". Messages: " + proposalResponse.getMessage()
                                    + ". Was verified : " + proposalResponse.isVerified());
                        } else {
                            String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                            query_result = payload;
                            out("Query payload of %s from peer %s returned %s", str, proposalResponse.getPeer().getName(), payload);
                        }
                    }

                    return null;
                } catch (Exception e) {
                    out("Caught exception while running query");
                    e.printStackTrace();
                    fail("Failed during chaincode query with error : " + e.getMessage());
                }
                return null;
            }).exceptionally(e -> {
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {
                        throw new AssertionError(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()), e);
                    }
                }

                throw new AssertionError(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()), e);

            }).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

        }catch (Exception e){
            e.printStackTrace();
        }
        return  query_result;
    }

}