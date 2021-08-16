/*
 * nimbus-jose-jwt
 *
 * Copyright 2012-2021, Connect2id Ltd and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.nimbusds.jose.crypto;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWECryptoParts;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.impl.ECDH;
import com.nimbusds.jose.crypto.impl.ECDH1PU;
import com.nimbusds.jose.crypto.impl.ECDH1PUCryptoProvider;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import net.jcip.annotations.ThreadSafe;

import javax.crypto.SecretKey;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Elliptic Curve Diffie-Hellman encrypter of
 * {@link com.nimbusds.jose.JWEObject JWE objects} for curves using EC JWK keys.
 * Expects a public EC key (with a P-256, P-384, or P-521 curve).
 *
 * <p>Public Key Authenticated Encryption for JOSE
 * <a href="https://datatracker.ietf.org/doc/html/draft-madden-jose-ecdh-1pu-04">ECDH-1PU</a>
 * for more information.
 *
 * <p>For Curve25519/X25519, see {@link ECDH1PUX25519Encrypter} instead.
 *
 * <p>This class is thread-safe.
 *
 * <p>Supports the following key management algorithms:
 *
 * <ul>
 *     <li>{@link com.nimbusds.jose.JWEAlgorithm#ECDH_1PU}
 *     <li>{@link com.nimbusds.jose.JWEAlgorithm#ECDH_1PU_A128KW}
 *     <li>{@link com.nimbusds.jose.JWEAlgorithm#ECDH_1PU_A192KW}
 *     <li>{@link com.nimbusds.jose.JWEAlgorithm#ECDH_1PU_A256KW}
 * </ul>
 *
 * <p>Supports the following elliptic curves:
 *
 * <ul>
 *     <li>{@link Curve#P_256}
 *     <li>{@link Curve#P_384}
 *     <li>{@link Curve#P_521}
 * </ul>
 *
 * <p>Supports the following content encryption algorithms for Direct key agreement mode:
 *
 * <ul>
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A128CBC_HS256}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A192CBC_HS384}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A256CBC_HS512}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A128GCM}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A192GCM}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A256GCM}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A128CBC_HS256_DEPRECATED}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A256CBC_HS512_DEPRECATED}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#XC20P}
 * </ul>
 *
 * <p>Supports the following content encryption algorithms for Key wrapping mode:
 *
 * <ul>
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A128CBC_HS256}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A192CBC_HS384}
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A256CBC_HS512}
 * </ul>
 *
 * @author Alexander Martynov
 * @version 2021-08-03
 */
@ThreadSafe
public class ECDH1PUEncrypter extends ECDH1PUCryptoProvider implements JWEEncrypter {


    /**
     * The supported EC JWK curves by the ECDH crypto provider class.
     */
    public static final Set<Curve> SUPPORTED_ELLIPTIC_CURVES;


    static {
        Set<Curve> curves = new LinkedHashSet<>();
        curves.add(Curve.P_256);
        curves.add(Curve.P_384);
        curves.add(Curve.P_521);
        SUPPORTED_ELLIPTIC_CURVES = Collections.unmodifiableSet(curves);
    }


    /**
     * The public JWK key.
     */
    private final ECPublicKey publicKey;

    /**
     * The private JWK key;
     */
    private final ECPrivateKey privateKey;

    /**
     * The externally supplied ephemeral EC key pair to use,
     * {@code null} to generate a ephemeral pair for each JWE.
     */
    private final KeyPair ephemeralKeyPair;

    /**
     * The externally supplied AES content encryption key (CEK) to use,
     * {@code null} to generate a CEK for each JWE.
     */
    private final SecretKey contentEncryptionKey;

    /**
     * Creates a new Elliptic Curve Diffie-Hellman encrypter.
     *
     * @param privateKey The private EC key. Must not be {@code null}.
     *
     * @param publicKey The public EC key. Must not be {@code null}.
     *
     * @throws JOSEException If the elliptic curve is not supported.
     */
    public ECDH1PUEncrypter(final ECPrivateKey privateKey, final ECPublicKey publicKey)
        throws JOSEException {

        this(privateKey, publicKey, null, null);
    }


    /**
     * Creates a new Elliptic Curve Diffie-Hellman encrypter with an
     * optionally specified content encryption key (CEK).
     *
     * @param privateKey            The private EC key. Must not be
     *                              {@code null}.
     *
     * @param publicKey             The public EC key. Must not be
     *                              {@code null}.
     * @param contentEncryptionKey  The content encryption key (CEK) to use.
     *                              If specified its algorithm must be "AES"
     *                              and its length must match the expected
     *                              for the JWE encryption method ("enc").
     *                              If {@code null} a CEK will be generated
     *                              for each JWE.
     * @throws JOSEException        If the elliptic curve is not supported.
     */
    public ECDH1PUEncrypter(final ECPrivateKey privateKey,
                            final ECPublicKey publicKey,
                            final SecretKey contentEncryptionKey)
            throws JOSEException {

        this(privateKey, publicKey, contentEncryptionKey, null);
    }

    /**
     * Creates a new Elliptic Curve Diffie-Hellman encrypter with an
     * optionally specified content encryption key (CEK).
     *
     * @param privateKey            The private EC key. Must not be
     *                              {@code null}.
     *
     * @param publicKey             The public EC key. Must not be
     *                              {@code null}.
     * @param contentEncryptionKey  The content encryption key (CEK) to use.
     *                              If specified its algorithm must be "AES"
     *                              and its length must match the expected
     *                              for the JWE encryption method ("enc").
     *                              If {@code null} a CEK will be generated
     *                              for each JWE.
     * @param ephemeralKeyPair      The externally supplied ephemeral EC
     *                              key pair to use, {@code null} to generate
     *                              a ephemeral pair for each JWE.
     * @throws JOSEException        If the elliptic curve is not supported.
     */
    public ECDH1PUEncrypter(final ECPrivateKey privateKey,
                            final ECPublicKey publicKey,
                            final SecretKey contentEncryptionKey,
                            final KeyPair ephemeralKeyPair)
            throws JOSEException {

        super(Curve.forECParameterSpec(publicKey.getParams()));

        this.privateKey = privateKey;
        this.publicKey = publicKey;

        if (contentEncryptionKey != null && (contentEncryptionKey.getAlgorithm() == null || !contentEncryptionKey.getAlgorithm().equals("AES")))
            throw new IllegalArgumentException("The algorithm of the content encryption key (CEK) must be AES");

        this.contentEncryptionKey = contentEncryptionKey;
        this.ephemeralKeyPair = ephemeralKeyPair;
    }


    /**
     * Returns the public EC key.
     *
     * @return The public EC key.
     */
    public ECPublicKey getPublicKey() {

        return publicKey;
    }


    /**
     * Returns the private EC key.
     *
     * @return The private EC key.
     */
    public ECPrivateKey getPrivateKey() {

        return privateKey;
    }


    @Override
    public Set<Curve> supportedEllipticCurves() {

        return SUPPORTED_ELLIPTIC_CURVES;
    }


    @Override
    public JWECryptoParts encrypt(final JWEHeader header, final byte[] clearText)
        throws JOSEException {

        ECDH1PU.validateSameCurve(privateKey, publicKey);

        final KeyPair ephemeralKeyPair;

        if (this.ephemeralKeyPair != null) {
            ephemeralKeyPair = this.ephemeralKeyPair;
        } else {
            // Generate ephemeral EC key pair on the same curve as the consumer's public key
            ephemeralKeyPair = generateEphemeralKeyPair(publicKey.getParams());
        }

        ECPublicKey ephemeralPublicKey = (ECPublicKey)ephemeralKeyPair.getPublic();
        ECPrivateKey ephemeralPrivateKey = (ECPrivateKey)ephemeralKeyPair.getPrivate();

        // Add the ephemeral public EC key to the header
        JWEHeader updatedHeader = new JWEHeader.Builder(header).
            ephemeralPublicKey(new ECKey.Builder(getCurve(), ephemeralPublicKey).build()).
            build();

        SecretKey Ze = ECDH.deriveSharedSecret(
            publicKey,
            ephemeralPrivateKey,
            getJCAContext().getKeyEncryptionProvider());

        SecretKey Zs = ECDH.deriveSharedSecret(
                publicKey,
                privateKey,
                getJCAContext().getKeyEncryptionProvider());

        SecretKey Z = ECDH1PU.deriveZ(Ze, Zs);

        return encryptWithZ(updatedHeader, Z, clearText, contentEncryptionKey);
    }


    /**
     * Generates a new ephemeral EC key pair with the specified curve.
     *
     * @param ecParameterSpec The EC key spec. Must not be {@code null}.
     *
     * @return The EC key pair.
     *
     * @throws JOSEException If the EC key pair couldn't be generated.
     */
    private KeyPair generateEphemeralKeyPair(final ECParameterSpec ecParameterSpec)
        throws JOSEException {

        Provider keProvider = getJCAContext().getKeyEncryptionProvider();

        try {
            KeyPairGenerator generator;

            if (keProvider != null) {
                generator = KeyPairGenerator.getInstance("EC", keProvider);
            } else {
                generator = KeyPairGenerator.getInstance("EC");
            }

            generator.initialize(ecParameterSpec);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new JOSEException("Couldn't generate ephemeral EC key pair: " + e.getMessage(), e);
        }
    }
}
