/*
 * $Id: PwsFieldTypeV3.java 401 2009-09-07 21:41:10Z roxon $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

@SuppressWarnings("ALL")
public enum PwsFieldTypeV3 implements PwsFieldType {
	V3_ID_STRING (0),
	UUID (1),
	GROUP (2),
	TITLE (3),
	USERNAME (4),
	NOTES (5),
	PASSWORD (6),
	CREATION_TIME (7),
	PASSWORD_MOD_TIME (8),
	LAST_ACCESS_TIME (9),
	PASSWORD_LIFETIME	(10),
	PASSWORD_POLICY_DEPRECATED		(11),
	LAST_MOD_TIME	(12),
	URL	(13),
	AUTOTYPE	(14),
	PASSWORD_HISTORY (15),
	PASSWORD_POLICY (16),
	PASSWORD_EXPIRY_INTERVAL (17),
	END_OF_RECORD		(255);

	private int id;
	private String name;

	PwsFieldTypeV3(int anId) {
		id = anId;
		name = toString();
	}

	public int getId() {
		return id;
	}

}
