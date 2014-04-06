package com.nimbusds.jose.util;


import java.nio.charset.Charset;
import java.math.BigInteger;

import net.jcip.annotations.Immutable;

import net.minidev.json.JSONAware;
import net.minidev.json.JSONValue;


/**
 * Base64-encoded object.
 *
 * @author Vladimir Dzhuvinov
 * @version $version$ (2013-05-16)
 */
@Immutable
public class Base64 implements JSONAware {


	/**
	 * UTF-8 is the required character set for all JOSE + JWT objects.
	 */
	public static final Charset CHARSET = Charset.forName("UTF-8");


	/**
	 * The Base64 value.
	 */
	private final String value;


	/**
	 * Creates a new Base64-encoded object.
	 *
	 * @param base64 The Base64-encoded object value. The value is not 
	 *               validated for having characters from a Base64 
	 *               alphabet. Must not be {@code null}.
	 */
	public Base64(final String base64) {

		if (base64 == null) {

			throw new IllegalArgumentException("The Base64 value must not be null");
		}

		value = base64;
	}


	/**
	 * Decodes this Base64 object to a byte array.
	 *
	 * @return The resulting byte array.
	 */
	public byte[] decode() {

		return Base64Codec.decode(value);
	}


	/**
	 * Decodes this Base64 object to an unsigned big integer.
	 *
	 * <p>Same as {@code new BigInteger(1, base64.decode())}.
	 *
	 * @return The resulting big integer.
	 */
	public BigInteger decodeToBigInteger() {

		return new BigInteger(1, decode());
	}


	/**
	 * Decodes this Base64 object to a string.
	 *
	 * @return The resulting string, in the UTF-8 character set.
	 */
	public String decodeToString() {

		return new String(decode(), CHARSET);
	}


	/**
	 * Returns a JSON string representation of this object.
	 *
	 * @return The JSON string representation of this object.
	 */
	@Override
	public String toJSONString() {

		return "\"" + JSONValue.escape(value) + "\"";
	}


	/**
	 * Returns a Base64 string representation of this object. The string 
	 * will be chunked into 76 character blocks separated by CRLF.
	 *
	 * @return The Base64 string representation, chunked into 76 character 
	 *         blocks separated by CRLF.
	 */
	@Override
	public String toString() {

		return value;
	}


	/**
	 * Overrides {@code Object.hashCode()}.
	 *
	 * @return The object hash code.
	 */
	@Override
	public int hashCode() {

		return value.hashCode();
	}


	/**
	 * Overrides {@code Object.equals()}.
	 *
	 * @param object The object to compare to.
	 *
	 * @return {@code true} if the objects have the same value, otherwise
	 *         {@code false}.
	 */
	@Override
	public boolean equals(final Object object) {

		return object != null && 
		       object instanceof Base64 && 
		       this.toString().equals(object.toString());
	}


	/**
	 * Base64-encodes the specified byte array. 
	 *
	 * @param bytes The byte array to encode. Must not be {@code null}.
	 *
	 * @return The resulting Base64 object.
	 */
	public static Base64 encode(final byte[] bytes) {

		return new Base64(Base64Codec.encodeToString(bytes, false));
	}


	/**
	 * Base64-encodes the specified big integer, without the sign bit.
	 *
	 * @param bigInt The big integer to encode. Must not be {@code null}.
	 *
	 * @return The resulting Base64 object.
	 */
	public static Base64 encode(final BigInteger bigInt) {

		return encode(BigIntegerUtils.toBytesUnsigned(bigInt));
	}


	/**
	 * Base64-encodes the specified string.
	 *
	 * @param text The string to encode. Must be in the UTF-8 character set
	 *             and not {@code null}.
	 *
	 * @return The resulting Base64 object.
	 */
	public static Base64 encode(final String text) {

		return encode(text.getBytes(CHARSET));
	}
}
