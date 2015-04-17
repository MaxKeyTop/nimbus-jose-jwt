package com.nimbusds.jose.crypto;


import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;

import net.jcip.annotations.ThreadSafe;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSJCAProviderSpec;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;



/**
 * RSA Signature-Scheme-with-Appendix (RSASSA) signer of 
 * {@link com.nimbusds.jose.JWSObject JWS objects}. This class is thread-safe.
 *
 * <p>Supports the following JSON Web Algorithms (JWAs):
 *
 * <ul>
 *     <li>{@link com.nimbusds.jose.JWSAlgorithm#RS256}
 *     <li>{@link com.nimbusds.jose.JWSAlgorithm#RS384}
 *     <li>{@link com.nimbusds.jose.JWSAlgorithm#RS512}
 *     <li>{@link com.nimbusds.jose.JWSAlgorithm#PS256}
 *     <li>{@link com.nimbusds.jose.JWSAlgorithm#PS384}
 *     <li>{@link com.nimbusds.jose.JWSAlgorithm#PS512}
 * </ul>
 * 
 * @author Vladimir Dzhuvinov
 * @version $version$ (2015-04-17)
 */
@ThreadSafe
public class RSASSASigner extends RSASSAProvider implements JWSSigner {


	/**
	 * The private RSA key.
	 */
	private final RSAPrivateKey privateKey;


	/**
	 * Creates a new RSA Signature-Scheme-with-Appendix (RSASSA) signer.
	 *
	 * @param privateKey The private RSA key. Must not be {@code null}.
	 * @param jcaSpec    The JCA provider specification, {@code null}
	 *                   implies the default one.
	 */
	public RSASSASigner(final RSAPrivateKey privateKey, final JWSJCAProviderSpec jcaSpec) {

		super(jcaSpec);

		if (privateKey == null) {

			throw new IllegalArgumentException("The private RSA key must not be null");
		}

		this.privateKey = privateKey;
	}


	/**
	 * Gets the private RSA key.
	 *
	 * @return The private RSA key.
	 */
	public RSAPrivateKey getPrivateKey() {

		return privateKey;
	}


	@Override
	public Base64URL sign(final JWSHeader header, final byte[] signingInput)
		throws JOSEException {

		Provider provider = getJCAProviderSpec() != null ? getJCAProviderSpec().getProvider() : null;

		Signature signer = getRSASignerAndVerifier(header.getAlgorithm(), provider);

		try {
			signer.initSign(privateKey);
			signer.update(signingInput);
			return Base64URL.encode(signer.sign());

		} catch (InvalidKeyException e) {

			throw new JOSEException("Invalid private RSA key: " + e.getMessage(), e);

		} catch (SignatureException e) {

			throw new JOSEException("RSA signature exception: " + e.getMessage(), e);
		}
	}
}
