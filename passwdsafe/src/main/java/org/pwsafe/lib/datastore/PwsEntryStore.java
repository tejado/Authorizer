/*
 *  * $Id: PwsEntryStore.java 401 2009-09-07 21:41:10Z roxon $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.datastore;

import java.util.List;
import java.util.Set;

import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.file.PwsFieldType;

/**
 * Provides a CRUD style access to PwsBeans.
 * 
 * @author roxon
 * @see PwsEntryBean
 */
@SuppressWarnings("ALL")
public interface PwsEntryStore {
	
	void setSparseFields (Set<PwsFieldType> fieldTypes);
	
	List<PwsEntryBean> getSparseEntries (); 
	
	PwsEntryBean getEntry (int anIndex);
	
	boolean addEntry (PwsEntryBean anEntry) throws PasswordSafeException;
	
	boolean updateEntry (PwsEntryBean anEntry);
	
	boolean removeEntry (PwsEntryBean anEntry);
	
	void clear ();
}
