package fabric;

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

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;


public class E2eMainProcess {

    private TestConfig testConfig ;
    private Collection<SampleOrg> testSampleOrgs;
    private HFClient client;
    private ChaincodeID chaincodeID;
    private Channel channel;
    private Collection<ProposalResponse> responses;
    private String result_query;
    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

    public void run(String peerName){
        try {
            testConfig = new TestConfig(peerName);
            testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();
            enrollUsersSetup();
             runFabricTest(peerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runFabricTest(String peerName) throws Exception {
        client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg(peerName);
        channel = constructChannel(sampleOrg);

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
      //  orderers.remove(anOrderer);

        Channel mychannel = client.newChannel("mychannel");
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
        ChaincodeID.Builder chaincodeIDbuilder = ChaincodeID.newBuilder().setName("zju")
                .setVersion("2");
                //.setPath("github/com/example_cc");
        chaincodeID = chaincodeIDbuilder.build();
        return mychannel;
    }

    private void runChannel() {
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
                    System.out.println(String.format("Query payload of %s from peer %s returned %s", str, response.getPeer().getName(), payload));
                    result_query = payload;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result_query;
    }
}

