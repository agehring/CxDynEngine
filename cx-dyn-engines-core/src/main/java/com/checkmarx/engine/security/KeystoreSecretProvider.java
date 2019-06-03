/*******************************************************************************
 * Copyright (c) 2017-2019 Checkmarx
 *  
 * This software is licensed for customer's internal use only.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
/**
 * Copyright (c) 2017 Checkmarx
 *
 * This software is licensed for customer's internal use only.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.checkmarx.engine.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * SecretProvider backed by Java KeyStore.
 * 
 * @author randy@checkmarx.com
 *
 */
public class KeystoreSecretProvider implements SecretProvider {

	private static final Logger log = LoggerFactory.getLogger(KeystoreSecretProvider.class);

	static String KEYSTORE_PW_PROP = "cx.engine.keystore.pw";
	
	private static final char[] DEFAULT_KEYSTORE_PW = {'c','h','a','n','g','e','m','e'};
	private static final String PBE_ALGO = "PBE";

	private final SecureString keystorePw;
	private final File keystoreFile;
	private final KeyStore keystore;
	private final KeyStore.PasswordProtection kspp;
	private final SecretKeyFactory skf;
	
	public KeystoreSecretProvider(String keystorePath) {
		log.info("KeystoreSecretProvider.ctor() : keystore={}", keystorePath);
		
		this.keystoreFile = new File(keystorePath);
		this.keystorePw = getStorePassword();
		this.keystore = loadKeyStore(keystorePw);
		this.kspp = createKeyStorePP(keystorePw);
		this.skf = initSecretKeyFactory();
	}

	private SecretKeyFactory initSecretKeyFactory() {
		try {
			return SecretKeyFactory.getInstance(PBE_ALGO);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Error initializing SecretKeyFactory", e);
		}
	}
	
	private SecureString getStorePassword() {
		log.trace("getStorePassword()");
		
		final String pw = System.getProperty(KEYSTORE_PW_PROP);
		
		if (Strings.isNullOrEmpty(pw)) {
			log.warn("Keystore password system property not set, using default.");
			return new SecureString(DEFAULT_KEYSTORE_PW);
		} else { 
			// clear system property
			System.setProperty(KEYSTORE_PW_PROP, "");
			return new SecureString(pw.toCharArray());
		}
	}

	private KeyStore loadKeyStore(SecureString storePw) {
		log.debug("loadKeyStore() : keystore={}", keystoreFile.getAbsolutePath());
		
		try (FileInputStream fs = new FileInputStream(keystoreFile)) {
			KeyStore ks = KeyStore.getInstance("JCEKS");
			ks.load(fs, storePw.array());
			return ks;
		} catch (Exception e) {
			throw new RuntimeException("Error loading KeyStore", e);
		}
	}
	
	private KeyStore.PasswordProtection createKeyStorePP(SecureString storePw) {
	    return new KeyStore.PasswordProtection(storePw.array());
	}

	
	@Override
	public SecureString get(String key) {
		log.trace("get() : key={}", key);

		try {
	        final KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry)keystore.getEntry(key, kspp);
	        if (ske == null) {
	        	return null;
	        }
	        final SecretKey secretKey = ske.getSecretKey();
			final PBEKeySpec keySpec = (PBEKeySpec)skf.getKeySpec(secretKey, PBEKeySpec.class);
	        char[] secret = keySpec.getPassword();
	        final SecureString secure = new SecureString(secret);
	        keySpec.clearPassword();  // clear sensitive data
	        Arrays.fill(secret, '0'); // clear sensitive data
	        return secure;
		} catch (Exception e) {
			throw new RuntimeException("Error retrieving secret from KeyStore", e);
		}
	}

	@Override
	public void store(String key, SecureString secret) {
		log.trace("store() : key={}", key);

		try {
			final SecretKey generatedSecret = skf.generateSecret(new PBEKeySpec(secret.array()));
			secret.clear(); // clear sensitive data
			keystore.setEntry(key, new KeyStore.SecretKeyEntry(generatedSecret), kspp);

			try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
				keystore.store(fos, keystorePw.array());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error storing secret to KeyStore", e);
		}
		
	}

}
