/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */
package org.cloudfoundry.identity.uaa.login;

import java.util.ArrayList;
import java.util.Collection;

import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class LoginServerSAMLUserDetailsService implements SAMLUserDetailsService {

	@Override
	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
		String username = credential.getNameID().getValue();
		String password = null;
		boolean enabled = true;
		boolean accountNonExpired = false;
		boolean credentialsNonExpired = true;
		boolean accountNonLocked = true;
		Collection<SAMLUserAuthority> authorities = null;

		for (Attribute attribute : credential.getAttributes()) {
			if (attribute.getName().equals("Groups")) {
				if (attribute.getAttributeValues() != null && attribute.getAttributeValues().size() > 0) {
					authorities = new ArrayList<SAMLUserAuthority>();
					for (XMLObject group : attribute.getAttributeValues()) {
						authorities.add(new SAMLUserAuthority(((XSString) group).getValue()));
					}
				}
				break;
			}
		}

		SAMLUserDetails userDetails = new SAMLUserDetails(username, password, enabled, accountNonExpired,
				credentialsNonExpired, accountNonLocked, authorities == null ? UaaAuthority.USER_AUTHORITIES
						: authorities);

		return userDetails;
	}

}
