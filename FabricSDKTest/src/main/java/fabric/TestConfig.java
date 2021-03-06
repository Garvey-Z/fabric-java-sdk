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

    private TestConfig() throws MalformedURLException, InvalidArgumentException {
        SampleOrg org1 = new SampleOrg("peerOrg1", "Org1MSP");
//        Properties caProperties = new Properties();
        org1.addPeerLocation("peer0.org1.example.com","grpc://122.112.249.81:7051");
        org1.addPeerLocation("peer1.org1.example.com","grpc://122.112.249.81:8051");
        org1.setDomainName("org1.example.com");
        org1.addOrdererLocation("orderer0.example.com","grpc://122.112.249.81:7050");
        //  org1.addOrdererLocation("orderer1.example.com", "grpcs://122.112.249.81:7500");
        org1.setCALocation("http://122.112.249.81:7054");
        org1.setCAName("ca-org1");
//        caProperties.setProperty("pemFile", this.getClass().getClassLoader().getResource("ca.org1.example.com-cert.pem").getPath());
//        org1.setCAProperties(caProperties);
        org1.setCAClient(HFCAClient.createNewInstance(org1.getCAName(), org1.getCALocation(), org1.getCAProperties()));
        sampleOrgs.put("peerOrg1",org1);
//
        SampleOrg org2 = new SampleOrg("peerOrg2", "Org2MSP");
       //org2.addPeerLocation("peer0.org2.example.com","grpc://47.112.33.105:7051");
       //org2.addPeerLocation("peer1.org2.example.com","grpc://47.112.31.234:7051");
       org2.addPeerLocation("peer2.org2.example.com","grpc://114.116.67.108:7051");
        org2.setDomainName("org2.example.com");
        org2.addOrdererLocation("orderer0.example.com","grpc://122.112.249.81:7050");
        org2.addOrdererLocation("orderer1.example.com", "grpc://122.112.249.81:7500");
        org2.setCALocation("http://47.112.33.105:7054");
        org2.setCAName("ca-org2");
        org2.setCAClient(HFCAClient.createNewInstance(org2.getCAName(),org2.getCALocation(), org2.getCAProperties()));
        sampleOrgs.put("peerOrg2",org2);
    }

    public static TestConfig getConfig() {
        if (null == config) {
            try {
                config = new TestConfig();
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
