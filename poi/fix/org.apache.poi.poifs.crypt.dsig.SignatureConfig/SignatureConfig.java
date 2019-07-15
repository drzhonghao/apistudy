

import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.crypt.dsig.OOXMLURIDereferencer;
import org.apache.poi.poifs.crypt.dsig.SignatureMarshalListener;
import org.apache.poi.poifs.crypt.dsig.facets.KeyInfoSignatureFacet;
import org.apache.poi.poifs.crypt.dsig.facets.OOXMLSignatureFacet;
import org.apache.poi.poifs.crypt.dsig.facets.Office2010SignatureFacet;
import org.apache.poi.poifs.crypt.dsig.facets.SignatureFacet;
import org.apache.poi.poifs.crypt.dsig.facets.XAdESSignatureFacet;
import org.apache.poi.poifs.crypt.dsig.services.RevocationDataService;
import org.apache.poi.poifs.crypt.dsig.services.SignaturePolicyService;
import org.apache.poi.poifs.crypt.dsig.services.TSPTimeStampService;
import org.apache.poi.poifs.crypt.dsig.services.TimeStampService;
import org.apache.poi.poifs.crypt.dsig.services.TimeStampServiceValidator;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.events.EventListener;


@SuppressWarnings({ "unused", "WeakerAccess" })
public class SignatureConfig {
	public static final String SIGNATURE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static final POILogger LOG = POILogFactory.getLogger(SignatureConfig.class);

	private static final String DigestMethod_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#sha224";

	private static final String DigestMethod_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#sha384";

	public interface SignatureConfigurable {
		void setSignatureConfig(SignatureConfig signatureConfig);
	}

	private ThreadLocal<OPCPackage> opcPackage = new ThreadLocal<>();

	private ThreadLocal<XMLSignatureFactory> signatureFactory = new ThreadLocal<>();

	private ThreadLocal<KeyInfoFactory> keyInfoFactory = new ThreadLocal<>();

	private ThreadLocal<Provider> provider = new ThreadLocal<>();

	private List<SignatureFacet> signatureFacets = new ArrayList<>();

	private HashAlgorithm digestAlgo = HashAlgorithm.sha256;

	private Date executionTime = new Date();

	private PrivateKey key;

	private List<X509Certificate> signingCertificateChain;

	private SignaturePolicyService signaturePolicyService;

	private URIDereferencer uriDereferencer;

	private String canonicalizationMethod = CanonicalizationMethod.INCLUSIVE;

	private boolean includeEntireCertificateChain = true;

	private boolean includeIssuerSerial;

	private boolean includeKeyValue;

	private TimeStampService tspService = new TSPTimeStampService();

	private String tspUrl;

	private boolean tspOldProtocol;

	private HashAlgorithm tspDigestAlgo;

	private String tspUser;

	private String tspPass;

	private TimeStampServiceValidator tspValidator;

	private String tspRequestPolicy = "1.3.6.1.4.1.13762.3";

	private String userAgent = "POI XmlSign Service TSP Client";

	private String proxyUrl;

	private RevocationDataService revocationDataService;

	private HashAlgorithm xadesDigestAlgo;

	private String xadesRole;

	private String xadesSignatureId = "idSignedProperties";

	private boolean xadesSignaturePolicyImplied = true;

	private String xadesCanonicalizationMethod = CanonicalizationMethod.EXCLUSIVE;

	private boolean xadesIssuerNameNoReverseOrder = true;

	private String packageSignatureId = "idPackageSignature";

	private String signatureDescription = "Office OpenXML Document";

	private EventListener signatureMarshalListener;

	private final Map<String, String> namespacePrefixes = new HashMap<>();

	private boolean updateConfigOnValidate = false;

	protected void init(boolean onlyValidation) {
		if ((opcPackage) == null) {
			throw new EncryptedDocumentException("opcPackage is null");
		}
		if ((uriDereferencer) == null) {
			uriDereferencer = new OOXMLURIDereferencer();
		}
		if ((uriDereferencer) instanceof SignatureConfig.SignatureConfigurable) {
			((SignatureConfig.SignatureConfigurable) (uriDereferencer)).setSignatureConfig(this);
		}
		if (namespacePrefixes.isEmpty()) {
			namespacePrefixes.put(SignatureFacet.OO_DIGSIG_NS, "mdssi");
			namespacePrefixes.put(SignatureFacet.XADES_132_NS, "xd");
		}
		if (onlyValidation) {
			return;
		}
		if ((signatureMarshalListener) == null) {
			signatureMarshalListener = new SignatureMarshalListener();
		}
		if ((signatureMarshalListener) instanceof SignatureConfig.SignatureConfigurable) {
			((SignatureConfig.SignatureConfigurable) (signatureMarshalListener)).setSignatureConfig(this);
		}
		if ((tspService) != null) {
		}
		if (signatureFacets.isEmpty()) {
			addSignatureFacet(new OOXMLSignatureFacet());
			addSignatureFacet(new KeyInfoSignatureFacet());
			addSignatureFacet(new XAdESSignatureFacet());
			addSignatureFacet(new Office2010SignatureFacet());
		}
		for (SignatureFacet sf : signatureFacets) {
		}
	}

	public void addSignatureFacet(SignatureFacet signatureFacet) {
		signatureFacets.add(signatureFacet);
	}

	public List<SignatureFacet> getSignatureFacets() {
		return signatureFacets;
	}

	public void setSignatureFacets(List<SignatureFacet> signatureFacets) {
		this.signatureFacets = signatureFacets;
	}

	public HashAlgorithm getDigestAlgo() {
		return digestAlgo;
	}

	public void setDigestAlgo(HashAlgorithm digestAlgo) {
		this.digestAlgo = digestAlgo;
	}

	public OPCPackage getOpcPackage() {
		return opcPackage.get();
	}

	public void setOpcPackage(OPCPackage opcPackage) {
		this.opcPackage.set(opcPackage);
	}

	public PrivateKey getKey() {
		return key;
	}

	public void setKey(PrivateKey key) {
		this.key = key;
	}

	public List<X509Certificate> getSigningCertificateChain() {
		return signingCertificateChain;
	}

	public void setSigningCertificateChain(List<X509Certificate> signingCertificateChain) {
		this.signingCertificateChain = signingCertificateChain;
	}

	public Date getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(Date executionTime) {
		this.executionTime = executionTime;
	}

	public String formatExecutionTime() {
		final DateFormat fmt = new SimpleDateFormat(SignatureConfig.SIGNATURE_TIME_FORMAT, Locale.ROOT);
		fmt.setTimeZone(LocaleUtil.TIMEZONE_UTC);
		return fmt.format(getExecutionTime());
	}

	public void setExecutionTime(String executionTime) {
		if ((executionTime != null) && (!("".equals(executionTime)))) {
			final DateFormat fmt = new SimpleDateFormat(SignatureConfig.SIGNATURE_TIME_FORMAT, Locale.ROOT);
			fmt.setTimeZone(LocaleUtil.TIMEZONE_UTC);
			try {
				this.executionTime = fmt.parse(executionTime);
			} catch (ParseException e) {
				SignatureConfig.LOG.log(POILogger.WARN, ("Illegal execution time: " + executionTime));
			}
		}
	}

	public SignaturePolicyService getSignaturePolicyService() {
		return signaturePolicyService;
	}

	public void setSignaturePolicyService(SignaturePolicyService signaturePolicyService) {
		this.signaturePolicyService = signaturePolicyService;
	}

	public URIDereferencer getUriDereferencer() {
		return uriDereferencer;
	}

	public void setUriDereferencer(URIDereferencer uriDereferencer) {
		this.uriDereferencer = uriDereferencer;
	}

	public String getSignatureDescription() {
		return signatureDescription;
	}

	public void setSignatureDescription(String signatureDescription) {
		this.signatureDescription = signatureDescription;
	}

	public String getCanonicalizationMethod() {
		return canonicalizationMethod;
	}

	public void setCanonicalizationMethod(String canonicalizationMethod) {
		this.canonicalizationMethod = SignatureConfig.verifyCanonicalizationMethod(canonicalizationMethod, CanonicalizationMethod.INCLUSIVE);
	}

	private static String verifyCanonicalizationMethod(String canonicalizationMethod, String defaultMethod) {
		if ((canonicalizationMethod == null) || (canonicalizationMethod.isEmpty())) {
			return defaultMethod;
		}
		switch (canonicalizationMethod) {
			case CanonicalizationMethod.INCLUSIVE :
			case CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS :
			case CanonicalizationMethod.ENVELOPED :
			case CanonicalizationMethod.EXCLUSIVE :
			case CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS :
				return canonicalizationMethod;
		}
		throw new EncryptedDocumentException(("Unknown CanonicalizationMethod: " + canonicalizationMethod));
	}

	public String getPackageSignatureId() {
		return packageSignatureId;
	}

	public void setPackageSignatureId(String packageSignatureId) {
		this.packageSignatureId = SignatureConfig.nvl(packageSignatureId, ("xmldsig-" + (UUID.randomUUID())));
	}

	public String getTspUrl() {
		return tspUrl;
	}

	public void setTspUrl(String tspUrl) {
		this.tspUrl = tspUrl;
	}

	public boolean isTspOldProtocol() {
		return tspOldProtocol;
	}

	public void setTspOldProtocol(boolean tspOldProtocol) {
		this.tspOldProtocol = tspOldProtocol;
	}

	public HashAlgorithm getTspDigestAlgo() {
		return SignatureConfig.nvl(tspDigestAlgo, digestAlgo);
	}

	public void setTspDigestAlgo(HashAlgorithm tspDigestAlgo) {
		this.tspDigestAlgo = tspDigestAlgo;
	}

	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}

	public TimeStampService getTspService() {
		return tspService;
	}

	public void setTspService(TimeStampService tspService) {
		this.tspService = tspService;
	}

	public String getTspUser() {
		return tspUser;
	}

	public void setTspUser(String tspUser) {
		this.tspUser = tspUser;
	}

	public String getTspPass() {
		return tspPass;
	}

	public void setTspPass(String tspPass) {
		this.tspPass = tspPass;
	}

	public TimeStampServiceValidator getTspValidator() {
		return tspValidator;
	}

	public void setTspValidator(TimeStampServiceValidator tspValidator) {
		this.tspValidator = tspValidator;
	}

	public RevocationDataService getRevocationDataService() {
		return revocationDataService;
	}

	public void setRevocationDataService(RevocationDataService revocationDataService) {
		this.revocationDataService = revocationDataService;
	}

	public HashAlgorithm getXadesDigestAlgo() {
		return SignatureConfig.nvl(xadesDigestAlgo, digestAlgo);
	}

	public void setXadesDigestAlgo(HashAlgorithm xadesDigestAlgo) {
		this.xadesDigestAlgo = xadesDigestAlgo;
	}

	public void setXadesDigestAlgo(String xadesDigestAlgo) {
		this.xadesDigestAlgo = SignatureConfig.getDigestMethodAlgo(xadesDigestAlgo);
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getTspRequestPolicy() {
		return tspRequestPolicy;
	}

	public void setTspRequestPolicy(String tspRequestPolicy) {
		this.tspRequestPolicy = tspRequestPolicy;
	}

	public boolean isIncludeEntireCertificateChain() {
		return includeEntireCertificateChain;
	}

	public void setIncludeEntireCertificateChain(boolean includeEntireCertificateChain) {
		this.includeEntireCertificateChain = includeEntireCertificateChain;
	}

	public boolean isIncludeIssuerSerial() {
		return includeIssuerSerial;
	}

	public void setIncludeIssuerSerial(boolean includeIssuerSerial) {
		this.includeIssuerSerial = includeIssuerSerial;
	}

	public boolean isIncludeKeyValue() {
		return includeKeyValue;
	}

	public void setIncludeKeyValue(boolean includeKeyValue) {
		this.includeKeyValue = includeKeyValue;
	}

	public String getXadesRole() {
		return xadesRole;
	}

	public void setXadesRole(String xadesRole) {
		this.xadesRole = xadesRole;
	}

	public String getXadesSignatureId() {
		return SignatureConfig.nvl(xadesSignatureId, "idSignedProperties");
	}

	public void setXadesSignatureId(String xadesSignatureId) {
		this.xadesSignatureId = xadesSignatureId;
	}

	public boolean isXadesSignaturePolicyImplied() {
		return xadesSignaturePolicyImplied;
	}

	public void setXadesSignaturePolicyImplied(boolean xadesSignaturePolicyImplied) {
		this.xadesSignaturePolicyImplied = xadesSignaturePolicyImplied;
	}

	public boolean isXadesIssuerNameNoReverseOrder() {
		return xadesIssuerNameNoReverseOrder;
	}

	public void setXadesIssuerNameNoReverseOrder(boolean xadesIssuerNameNoReverseOrder) {
		this.xadesIssuerNameNoReverseOrder = xadesIssuerNameNoReverseOrder;
	}

	public EventListener getSignatureMarshalListener() {
		return signatureMarshalListener;
	}

	public void setSignatureMarshalListener(EventListener signatureMarshalListener) {
		this.signatureMarshalListener = signatureMarshalListener;
	}

	public Map<String, String> getNamespacePrefixes() {
		return namespacePrefixes;
	}

	public void setNamespacePrefixes(Map<String, String> namespacePrefixes) {
		this.namespacePrefixes.clear();
		this.namespacePrefixes.putAll(namespacePrefixes);
	}

	private static <T> T nvl(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}

	public String getSignatureMethodUri() {
		switch (getDigestAlgo()) {
			case sha1 :
				return XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
			case sha224 :
				return XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA224;
			case sha256 :
				return XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256;
			case sha384 :
				return XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384;
			case sha512 :
				return XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512;
			case ripemd160 :
				return XMLSignature.ALGO_ID_SIGNATURE_RSA_RIPEMD160;
			default :
				throw new EncryptedDocumentException((("Hash algorithm " + (getDigestAlgo())) + " not supported for signing."));
		}
	}

	public String getDigestMethodUri() {
		return SignatureConfig.getDigestMethodUri(getDigestAlgo());
	}

	public static String getDigestMethodUri(HashAlgorithm digestAlgo) {
		switch (digestAlgo) {
			case sha1 :
				return DigestMethod.SHA1;
			case sha224 :
				return SignatureConfig.DigestMethod_SHA224;
			case sha256 :
				return DigestMethod.SHA256;
			case sha384 :
				return SignatureConfig.DigestMethod_SHA384;
			case sha512 :
				return DigestMethod.SHA512;
			case ripemd160 :
				return DigestMethod.RIPEMD160;
			default :
				throw new EncryptedDocumentException((("Hash algorithm " + digestAlgo) + " not supported for signing."));
		}
	}

	private static HashAlgorithm getDigestMethodAlgo(String digestMethodUri) {
		if ((digestMethodUri == null) || (digestMethodUri.isEmpty())) {
			return null;
		}
		switch (digestMethodUri) {
			case DigestMethod.SHA1 :
				return HashAlgorithm.sha1;
			case SignatureConfig.DigestMethod_SHA224 :
				return HashAlgorithm.sha224;
			case DigestMethod.SHA256 :
				return HashAlgorithm.sha256;
			case SignatureConfig.DigestMethod_SHA384 :
				return HashAlgorithm.sha384;
			case DigestMethod.SHA512 :
				return HashAlgorithm.sha512;
			case DigestMethod.RIPEMD160 :
				return HashAlgorithm.ripemd160;
			default :
				throw new EncryptedDocumentException((("Hash algorithm " + digestMethodUri) + " not supported for signing."));
		}
	}

	public void setSignatureMethodFromUri(final String signatureMethodUri) {
		switch (signatureMethodUri) {
			case XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1 :
				setDigestAlgo(HashAlgorithm.sha1);
				break;
			case XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA224 :
				setDigestAlgo(HashAlgorithm.sha224);
				break;
			case XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256 :
				setDigestAlgo(HashAlgorithm.sha256);
				break;
			case XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384 :
				setDigestAlgo(HashAlgorithm.sha384);
				break;
			case XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512 :
				setDigestAlgo(HashAlgorithm.sha512);
				break;
			case XMLSignature.ALGO_ID_SIGNATURE_RSA_RIPEMD160 :
				setDigestAlgo(HashAlgorithm.ripemd160);
				break;
			default :
				throw new EncryptedDocumentException((("Hash algorithm " + signatureMethodUri) + " not supported."));
		}
	}

	public void setSignatureFactory(XMLSignatureFactory signatureFactory) {
		this.signatureFactory.set(signatureFactory);
	}

	public XMLSignatureFactory getSignatureFactory() {
		XMLSignatureFactory sigFac = signatureFactory.get();
		if (sigFac == null) {
			sigFac = XMLSignatureFactory.getInstance("DOM", getProvider());
			setSignatureFactory(sigFac);
		}
		return sigFac;
	}

	public void setKeyInfoFactory(KeyInfoFactory keyInfoFactory) {
		this.keyInfoFactory.set(keyInfoFactory);
	}

	public KeyInfoFactory getKeyInfoFactory() {
		KeyInfoFactory keyFac = keyInfoFactory.get();
		if (keyFac == null) {
			keyFac = KeyInfoFactory.getInstance("DOM", getProvider());
			setKeyInfoFactory(keyFac);
		}
		return keyFac;
	}

	public Provider getProvider() {
		Provider prov = provider.get();
		if (prov == null) {
			String[] dsigProviderNames = new String[]{ System.getProperty("jsr105Provider"), "org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI", "org.jcp.xml.dsig.internal.dom.XMLDSigRI" };
			for (String pn : dsigProviderNames) {
				if (pn == null) {
					continue;
				}
				try {
					prov = ((Provider) (Class.forName(pn).newInstance()));
					break;
				} catch (Exception e) {
					SignatureConfig.LOG.log(POILogger.DEBUG, (("XMLDsig-Provider '" + pn) + "' can't be found - trying next."));
				}
			}
		}
		if (prov == null) {
			throw new RuntimeException("JRE doesn't support default xml signature provider - set jsr105Provider system property!");
		}
		return prov;
	}

	public String getXadesCanonicalizationMethod() {
		return xadesCanonicalizationMethod;
	}

	public void setXadesCanonicalizationMethod(String xadesCanonicalizationMethod) {
		this.xadesCanonicalizationMethod = SignatureConfig.verifyCanonicalizationMethod(xadesCanonicalizationMethod, CanonicalizationMethod.EXCLUSIVE);
	}

	public boolean isUpdateConfigOnValidate() {
		return updateConfigOnValidate;
	}

	public void setUpdateConfigOnValidate(boolean updateConfigOnValidate) {
		this.updateConfigOnValidate = updateConfigOnValidate;
	}
}

