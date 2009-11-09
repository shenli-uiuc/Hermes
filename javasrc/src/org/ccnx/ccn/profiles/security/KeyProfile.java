/**
 * Part of the CCNx Java Library.
 *
 * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation. 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.ccnx.ccn.profiles.security;

import org.ccnx.ccn.profiles.CCNProfile;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;

/**
 * The Key profile handles low-level functions regarding representing
 * keys in content and content names. This allows us to provide a standard
 * form for referring to public keys in names, among other things.
 */
public class KeyProfile implements CCNProfile {
	
	public static final byte [] KEY_ID_PREFIX = ContentName.componentParseNative("keyid" + CCNProfile.COMPONENT_SEPARATOR_STRING);
	public static byte [] KEY_ID_POSTFIX = {}; // probably empty
	
	/**
	 * This builds a name component which refers to the digest
	 * of a key (of any type), as generated by the caller,
	 * formatted in a standard way (e.g. marker prefixes if
	 * necessary). This makes it easier to write code that
	 * writes and parses names with key identifiers as 
	 * name components.
	 * @param keyID The (digest) identifier of the key to
	 * 	be referred to.
	 * @return The resulting name component.
	 */
	public static byte [] keyIDNameComponent(byte [] keyID) {
		
		if (null == keyID) {
			// for now, don't complain about 0-length ID
			throw new IllegalArgumentException("keyID must not be null!");
		}
		
		byte [] component = new byte[KEY_ID_PREFIX.length + KEY_ID_POSTFIX.length + 
		                             keyID.length];
		int offset = 0;
		System.arraycopy(KEY_ID_PREFIX, 0, component, offset, KEY_ID_PREFIX.length);
		offset += KEY_ID_PREFIX.length;
		System.arraycopy(keyID, 0, component, offset, keyID.length);
		offset += keyID.length;
		System.arraycopy(KEY_ID_POSTFIX, 0, component, offset, KEY_ID_POSTFIX.length);
		
		return component;
	}
	
	/**
	 * This generates a name component which refers to the digest of a
	 * public key, formatted in a standard way (e.g. with marker prefixes
	 * if necessary and so on).
	 * @param keyToName The key to include in the name component.
	 * @return the binary name component
	 */
	public static byte [] keyIDNameComponent(PublisherPublicKeyDigest keyToName) {
	
		if (null == keyToName) {
			throw new IllegalArgumentException("keyToName must not be null!");
		}
		
		return keyIDNameComponent(keyToName.digest());
	}
	
	/**
	 * This creates a ContentName whose last component represents
	 * the digest of a public key.
	 * @param parent the parent (prefix) to use for this content name;
	 * 	if null, the name will contain only the key ID component.
	 * @param keyToName the key to refer to in the next name component.
	 * @return the resulting name
	 */
	public static ContentName keyName(ContentName parent, PublisherPublicKeyDigest keyToName) {	
		return new ContentName(parent, keyIDNameComponent(keyToName));
	}
}
