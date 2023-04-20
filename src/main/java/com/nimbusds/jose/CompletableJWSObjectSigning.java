/*
 * nimbus-jose-jwt
 *
 * Copyright 2012-2016, Connect2id Ltd and contributors.
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

package com.nimbusds.jose;


import com.nimbusds.jose.util.Base64URL;

import java.security.Signature;


/**
 * Completable JSON Web Signature (JWS) object signing. Implementations must be
 * thread-safe.
 */
public interface CompletableJWSObjectSigning {
	/**
	 * Returns the initialized signature object.
	 * Used when the user verification must be bound to a specific instance of a Signature.
	 * @return The initialized Signature object.
	 */
	Signature getInitializedSignature();
	
	/**
	 * Completes the JWS object signing.
	 *
	 * @return The resulting signature part (third part) of the JWS object.
	 *
	 * @throws JOSEException If the JWS object couldn't be signed.
	 */
	Base64URL complete()
		throws JOSEException;
}
