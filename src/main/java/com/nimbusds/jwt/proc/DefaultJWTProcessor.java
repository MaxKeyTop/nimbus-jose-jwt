package com.nimbusds.jwt.proc;


import java.security.Key;
import java.text.ParseException;
import java.util.List;
import java.util.ListIterator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.*;


/**
 * Default processor of received {@link com.nimbusds.jwt.JWT JSON Web Token}s.
 *
 * <p>Must be supplied with a {@link JWSKeySelector JWS key selector} to
 * determine the key candidate(s) for the signature verification. The exact key
 * selection procedure is application-specific and may involve key ID lookup, a
 * certificate check and / or other information supplied in the message
 * {@link SecurityContext context}.
 *
 * <p>Similarly, the processor must be supplied with a {@link JWEKeySelector
 * JWE key selector} if JWE messages are expected to be processed.
 *
 * <p>See sections 6 of RFC 7515 (JWS) and RFC 7516 (JWE) for guidelines on key
 * selection.
 *
 * <p>This processor comes with the default {@link DefaultJWSVerifierFactory
 * JWS verifier factory} and the default {@link DefaultJWEDecrypterFactory
 * JWE decrypter factory}; they can construct verifiers / decrypters for all
 * standard JOSE algorithms implemented by the library.
 *
 * <p>Note that for security reasons this processor is hardwired to reject
 * unsecured (plain) JWTs. Override the {@link #process(PlainJWT, SecurityContext)}
 * if you need to handle plain JWTs as well.
 *
 * <p>To process generic JOSE objects (with arbitrary payloads) use the
 * {@link com.nimbusds.jose.proc.DefaultJOSEProcessor} class.
 *
 * @author Vladimir Dzhuvinov
 * @version 2015-06-29
 */
public class DefaultJWTProcessor<C extends SecurityContext>
	extends BaseJOSEProcessor<C>
	implements JWTProcessor<ReadOnlyJWTClaimsSet, C> {


	@Override
	public ReadOnlyJWTClaimsSet process(final String jwtString, final C context)
		throws ParseException, BadJOSEException, JOSEException {

		JWT jwt = JWTParser.parse(jwtString);

		if (jwt instanceof SignedJWT) {
			return process((SignedJWT)jwt, context);
		}

		if (jwt instanceof EncryptedJWT) {
			return process((EncryptedJWT)jwt, context);
		}

		if (jwt instanceof PlainJWT) {
			return process((PlainJWT)jwt, context);
		}

		// Should never happen
		throw new JOSEException("Unexpected JWT object type: " + jwt.getClass());
	}


	@Override
	public ReadOnlyJWTClaimsSet process(final PlainJWT plainJWT, final C context)
		throws BadJOSEException, JOSEException {

		throw new BadJOSEException("Unsecured (plain) JWTs are rejected, extend class to handle");
	}


	@Override
	public ReadOnlyJWTClaimsSet process(final SignedJWT signedJWT, final C context)
		throws BadJOSEException, JOSEException {

		if (getJWSKeySelector() == null) {
			// JWS key selector may have been deliberately omitted
			throw new BadJOSEException("Signed JWT rejected: No JWS key selector is configured");
		}

		if (getJWSVerifierFactory() == null) {
			throw new JOSEException("No JWS verifier is configured");
		}

		List<? extends Key> keyCandidates = getJWSKeySelector().selectJWSKeys(signedJWT.getHeader(), context);

		if (keyCandidates == null || keyCandidates.isEmpty()) {
			throw new BadJOSEException("Signed JWT rejected: No matching key(s) found");
		}

		ListIterator<? extends Key> it = keyCandidates.listIterator();

		while (it.hasNext()) {

			JWSVerifier verifier = getJWSVerifierFactory().createJWSVerifier(signedJWT.getHeader(), it.next());

			if (verifier == null) {
				continue;
			}

			final boolean validSignature = signedJWT.verify(verifier);

			if (validSignature) {
				try {
					return signedJWT.getJWTClaimsSet();

				} catch (ParseException e) {
					// Payload not a JSON object
					throw new BadJWTException(e.getMessage(), e);
				}
			}

			if (! it.hasNext()) {
				// No more keys to try out
				throw new BadJWSException("Signed JWT rejected: Invalid signature");
			}
		}

		throw new BadJOSEException("JWS object rejected: No matching verifier(s) found");
	}


	@Override
	public ReadOnlyJWTClaimsSet process(final EncryptedJWT encryptedJWT, final C context)
		throws BadJOSEException, JOSEException {

		if (getJWEKeySelector() == null) {
			// JWE key selector may have been deliberately omitted
			throw new BadJOSEException("Encrypted JWT rejected: No JWE key selector is configured");
		}

		if (getJWEDecrypterFactory() == null) {
			throw new JOSEException("No JWE decrypter is configured");
		}

		List<? extends Key> keyCandidates = getJWEKeySelector().selectJWEKeys(encryptedJWT.getHeader(), context);

		if (keyCandidates == null || keyCandidates.isEmpty()) {
			throw new BadJOSEException("Encrypted JWT rejected: No matching key(s) found");
		}

		ListIterator<? extends Key> it = keyCandidates.listIterator();

		while (it.hasNext()) {

			JWEDecrypter decrypter = getJWEDecrypterFactory().createJWEDecrypter(encryptedJWT.getHeader(), it.next());

			if (decrypter == null) {
				continue;
			}

			try {
				encryptedJWT.decrypt(decrypter);

			} catch (JOSEException e) {

				if (it.hasNext()) {
					// Try next key
					continue;
				}

				// No more keys to try
				throw new BadJWEException("Encrypted JWT rejected: " + e.getMessage(), e);
			}

			if ("JWT".equalsIgnoreCase(encryptedJWT.getHeader().getContentType())) {

				// Handle nested signed JWT, see http://tools.ietf.org/html/rfc7519#section-5.2
				SignedJWT nestedJWT = encryptedJWT.getPayload().toSignedJWT();

				if (nestedJWT == null) {
					// Cannot parse payload to signed JWT
					throw new BadJWTException("The payload is not a nested JWT");
				}

				return process(nestedJWT, context);
			}

			try {
				return encryptedJWT.getJWTClaimsSet();

			} catch (ParseException e) {
				// Payload not a JSON object
				throw new BadJWTException(e.getMessage(), e);
			}
		}

		throw new BadJOSEException("Encrypted JWT rejected: No matching decrypter(s) found");
	}
}
