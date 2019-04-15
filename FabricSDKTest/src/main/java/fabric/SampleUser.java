package fabric;

import io.netty.util.internal.StringUtil;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.util.Set;

public class SampleUser implements User {
    private String enrollmentSecret;
    private String userName;
    private Set<String> role;
    private String account;
    private String affiliation;
    private String organization;
    private String mspId;
    Enrollment enrollment;

    private transient CryptoSuite cryptoSuite;

    public SampleUser(String userName, String org, CryptoSuite cryptoSuite){
        this.cryptoSuite = cryptoSuite;
        this.userName = userName;
        this.organization = org;
    }

    public String getName() {
        return this.userName;
    }

    public Set<String> getRoles() {
        return this.role;
    }

    public String getAccount() {
        return this.account;
    }

    public String getAffiliation() {
        return this.affiliation;
    }

    public Enrollment getEnrollment() {
        return this.enrollment;
    }

    public String getMspId() {
        return this.mspId;
    }

    public void setRole(Set<String> role) {
        this.role = role;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public void setMspId(String mspId) {
        this.mspId = mspId;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public String getEnrollmentSecret() {
        return enrollmentSecret;
    }

    public void setEnrollmentSecret(String enrollmentCert) {
        this.enrollmentSecret = enrollmentCert;
    }

    public boolean isEnrolled() {
        return this.enrollment != null;
    }

    public boolean isRegistered() {
        return !StringUtil.isNullOrEmpty(this.enrollmentSecret);
    }
}
