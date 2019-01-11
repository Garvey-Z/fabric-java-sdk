package fabric;

import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

public class TestConfig {
    private static TestConfig config;
    private static final Properties sdkProperties = new Properties();

    private final HashMap<String, SampleOrg> sampleOrgs = new HashMap<String, SampleOrg>();

    private HashMap<String, String> peerLocation = new HashMap<>();
    private HashMap<String, String> peerMsPid = new HashMap<>();

    public TestConfig(String peerName) throws MalformedURLException, InvalidArgumentException {
        init();
        sampleOrgs.clear();
        if(peerMsPid.get(peerName) == "Org1MSP") {
            SampleOrg org1 = new SampleOrg("peerOrg1", "Org1MSP");
//        Properties caProperties = new Properties();
            //  org1.addPeerLocation("peer0.org1.example.com","grpcs://114.116.67.108:7051");
            //   org1.addPeerLocation("peer1.org1.example.com","grpcs://114.116.67.108:7054");
            org1.addPeerLocation(peerName, peerLocation.get(peerName));
            org1.setDomainName("org1.example.com");
            org1.addOrdererLocation("orderer.example.com", "grpcs://114.116.67.108:7050");
            org1.setCALocation("http://114.116.67.108:7057");
            org1.setCAName("ca-org1");
//        caProperties.setProperty("pemFile", this.getClass().getClassLoader().getResource("ca.org1.example.com-cert.pem").getPath());
//        org1.setCAProperties(caProperties);
            org1.setCAClient(HFCAClient.createNewInstance(org1.getCAName(), org1.getCALocation(), org1.getCAProperties()));
           // sampleOrgs.put("peerOrg1", org1);
            sampleOrgs.put(peerName, org1);
        }
        else if(peerMsPid.get(peerName) == "Org2MSP") {
            SampleOrg org2 = new SampleOrg("peerOrg2", "Org2MSP");
            // org2.addPeerLocation("peer0.org2.example.com","grpcs://114.116.67.108:7055");
            //    org2.addPeerLocation("peer1.org2.example.com","grpcs://114.116.67.108:7056");
            org2.addPeerLocation(peerName,peerLocation.get(peerName));
            org2.setDomainName("org2.example.com");
            org2.addOrdererLocation("orderer.example.com", "grpcs://114.116.67.108:7050");
            org2.setCALocation("http://114.116.67.108:7058");
            org2.setCAName("ca-org2");
            org2.setCAClient(HFCAClient.createNewInstance(org2.getCAName(), org2.getCALocation(), org2.getCAProperties()));
            //sampleOrgs.put("peerOrg2", org2);
            sampleOrgs.put(peerName, org2);
        }
    }

    private void init(){
        peerLocation.put("peer0.org1.example.com", "grpcs://114.116.67.108:7051");
        peerLocation.put("peer1.org1.example.com", "grpcs://114.116.67.108:7054");
        peerLocation.put("peer0.org2.example.com", "grpcs://114.116.67.108:7055");
        peerLocation.put("peer1.org2.example.com", "grpcs://114.116.67.108:7056");

        peerMsPid.put("peer0.org1.example.com", "Org1MSP");
        peerMsPid.put("peer1.org1.example.com", "Org1MSP");
        peerMsPid.put("peer0.org2.example.com", "Org2MSP");
        peerMsPid.put("peer1.org2.example.com", "Org2MSP");

    }
    public  TestConfig getConfig(String peerName) {
        if (null == config) {
            try {
                config = new TestConfig(peerName);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            }
        }
        return config;
    }

    public SampleOrg getIntegrationTestsSampleOrg(String name) {
        return sampleOrgs.get(name);
    }
    public Collection<SampleOrg> getIntegrationTestsSampleOrgs() {
        return Collections.unmodifiableCollection(sampleOrgs.values());
    }

    public Properties getPeerProperties(String name) {

        return getEndPointProperties("peer", name);

    }

    public Properties getOrdererProperties(String name) {

        return getEndPointProperties("orderer", name);

    }

    public Properties getEndPointProperties(final String type, final String name){
        Properties ret = new Properties();

        final String domainName = getDomain(name);
        StringBuilder pathStrBuilder = new StringBuilder();
        pathStrBuilder.append("/component/crypto-config/")
                .append(type)
                .append("Organizations/")
                .append(domainName)
                .append("/")
                .append(type)
                .append("s/")
                .append(name)
                .append("/tls/server.crt");
        File cert = new File(this.getClass().getResource(pathStrBuilder.toString()).getPath());
        System.out.println(cert.getPath());
        if (!cert.exists()) {
            throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s", name,
                    cert.getAbsolutePath()));
        }

        ret.setProperty("pemFile", cert.getAbsolutePath());
        ret.setProperty("hostnameOverride", name);
        ret.setProperty("sslProvider", "openSSL");
        ret.setProperty("negotiationType", "TLS");

        return ret;
    }

    private String getDomain(String name) {
        int dot = name.indexOf(".");
        if (-1 == dot) {
            return null;
        } else {
            return name.substring(dot + 1);
        }
    }
}
