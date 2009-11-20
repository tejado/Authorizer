/*
 *  * $Id: PwsEntryBean.java 404 2009-09-21 19:19:25Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.datastore;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.UUID;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFieldType;
import org.pwsafe.lib.file.PwsFieldTypeV1;
import org.pwsafe.lib.file.PwsFieldTypeV2;
import org.pwsafe.lib.file.PwsFieldTypeV3;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV1;
import org.pwsafe.lib.file.PwsRecordV2;
import org.pwsafe.lib.file.PwsRecordV3;
import org.pwsafe.lib.file.PwsStringField;
import org.pwsafe.lib.file.PwsStringUnicodeField;
import org.pwsafe.lib.file.PwsTimeField;
import org.pwsafe.lib.file.PwsUUIDField;

/**
 * Convenience class for transferring password info around in a
 * version-independent manner and in java bean style.
 *
 * @author roxon
 */
public class PwsEntryBean implements Cloneable {
    private final static Log log = Log.getInstance(PwsEntryBean.class);

	/**
	 * the index with which the backing PwsRecord may be retrieved.
	 */
	int storeIndex;

	/**
	 * whether this Wrapper is only partly filled from it's backing PwsRecord.
	 * Default is true, so empty records can't be added to a PwsFile.
	 */
	boolean sparse = true;

	UUID id;
    String group;
    String title;

    String username;
    StringBuilder password;
    String notes;
    String url;
    String autotype;

    String version;

    Date lastAccess;
    Date created;
    Date lastPwChange;
    Date lastChange;
    Date expires;

    /**
     * Default constructor.
     *
     */
    public PwsEntryBean() {
        super();
    }


    public PwsEntryBean(String group, String username, StringBuilder password, String notes) {
    	this();
        this.group = group;
        this.username = username;
        this.password = password;
        this.notes = notes;
    }

    /**
     * @return the storeIndex
     */
    public int getStoreIndex() {
    	return storeIndex;
    }


    /**
     * @param storeIndex the storeIndex to set
     */
    public void setStoreIndex(int storeIndex) {
    	this.storeIndex = storeIndex;
    }


    /**
     * @return the sparse
     */
    public boolean isSparse() {
    	return sparse;
    }


    /**
     * @param sparse the sparse to set
     */
    public void setSparse(boolean sparse) {
    	this.sparse = sparse;
    }


    public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	/**
	 * @return Returns the group.
	 */
	public String getGroup() {
		return group;
	}
	/**
	 * @param group The group to set.
	 */
	public void setGroup(String group) {
		this.group = group;
	}
	/**
	 * @return Returns the notes.
	 */
	public String getNotes() {
		return notes;
	}
	/**
	 * @param notes The notes to set.
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}
	/**
	 * @return Returns the password.
	 */
	public StringBuilder getPassword() {
		return password;
	}
	/**
	 * @param password The password to set.
	 */
	public void setPassword(StringBuilder password) {
		this.password = password;
	}
	/**
	 * For backward compatibility.
	 * @param password The password to set.
	 *
	 * @deprecated don't pass around passwords as String!
	 */
	@Deprecated
	public void setPassword(String password) {
		this.password = new StringBuilder(password);
	}
	/**
	 * @return Returns the username.
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username The username to set.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }
    /**
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAutotype() {
		return autotype;
	}


	public void setAutotype(String autotype) {
		this.autotype = autotype;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastAccess() {
		return lastAccess;
	}

	public void setLastAccess(Date lastAccess) {
		this.lastAccess = lastAccess;
	}

	public Date getLastChange() {
		return lastChange;
	}

	public void setLastChange(Date lastChange) {
		this.lastChange = lastChange;
	}

	public Date getLastPwChange() {
		return lastPwChange;
	}

	public void setLastPwChange(Date lastPwChange) {
		this.lastPwChange = lastPwChange;
	}

	public Date getExpires() {
		return expires;
	}

	public void setExpires(Date expires) {
		this.expires = expires;
	}

    /**
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
    }
    /**
     * @param version The version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected PwsEntryBean clone() {
		try {
			return (PwsEntryBean) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error (e);// should never happen, otherwise there is a serious problem
		}
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + storeIndex;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PwsEntryBean other = (PwsEntryBean) obj;
		if (storeIndex != other.storeIndex)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (autotype == null) {
			if (other.autotype != null)
				return false;
		} else if (!autotype.equals(other.autotype))
			return false;
		if (created == null) {
			if (other.created != null)
				return false;
		} else if (!created.equals(other.created))
			return false;
		if (expires == null) {
			if (other.expires != null)
				return false;
		} else if (!expires.equals(other.expires))
			return false;
		if (lastChange == null) {
			if (other.lastChange != null)
				return false;
		} else if (!lastChange.equals(other.lastChange))
			return false;
		if (lastPwChange == null) {
			if (other.lastPwChange != null)
				return false;
		} else if (!lastPwChange.equals(other.lastPwChange))
			return false;
		if (notes == null) {
			if (other.notes != null)
				return false;
		} else if (!notes.equals(other.notes))
			return false;
//		if (password == null) {
//			if (other.password != null)
//				return false;
//		} else if (!password.equals(other.password))
//			return false;
		if (sparse != other.sparse)
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}


	@Override
	public String toString() {
		StringBuffer all = new StringBuffer (200);
		all.append("PwsEntryBean ").append(version).append(": ID ");
		all.append(id != null ? id.toString() : null);
		all.append(", Group ").append(group);
		all.append(", Title ").append(title);
		all.append(", User ").append(username);
		all.append(", Notes ").append(notes);
		all.append(", Url ").append(url);
		all.append(", Created ").append(created);
		all.append(", Changed ").append(lastChange);
		all.append(", Expires ").append(expires);
		return all.toString();
	}


	/**
     * A safer version of field retrieval that is null-safe.
     *
     * @param record the record to retrieve the field from
     * @param aType the type of the field
     * @return the value of the field or an empty string if the field is null
     */
    public static String getSafeValue(PwsRecord record, PwsFieldType aType) {
    	String fieldValue = "";
    	PwsField field = record.getField(aType);
    	if (field != null && field.getValue() != null) {
    		fieldValue = field.getValue().toString();
    	}
    	return fieldValue;
    }

    /**
     * A safer version of date retrieval that is null-safe.
     *
     * @param v3
     * @param type
     * @return the Field as Date
     */
	public static Date getSafeDate(final PwsRecordV3 v3, final PwsFieldType aType) {
		final PwsTimeField field = (PwsTimeField) v3.getField(aType);

		return field != null ? (Date) field.getValue() : null;
	}


    /**
     * Only set a date into a PwsTimeField if the date != null.
     * @param v3
     * @param aType
     * @param aDate
     * @return true if the date != null, else false
     */
	private boolean setSafeDate(final PwsRecordV3 v3, final int aType, final Date aDate) {
		if (aDate == null)
			return false;
		v3.setField(new PwsTimeField(aType, aDate));
		return true;
	}


	/**
     * Moves the contents of the DTO into the supplied PwsRecord.
     *
	 * @param nextRecord the record to place the data into
	 */
	public void toPwsRecord(PwsRecord nextRecord) {

		if (nextRecord instanceof PwsRecordV3) {

            PwsRecordV3 v3 = (PwsRecordV3) nextRecord;
            v3.setField(new PwsStringUnicodeField(PwsRecordV3.GROUP , getGroup()));// + '\u0000'));
            v3.setField(new PwsStringUnicodeField(PwsRecordV3.TITLE , getTitle()));
            v3.setField(new PwsStringUnicodeField(PwsRecordV3.USERNAME , getUsername()));
            v3.setField(new PwsStringUnicodeField(PwsRecordV3.PASSWORD , getPassword()));
            v3.setField(new PwsStringUnicodeField(PwsRecordV3.NOTES , getNotes()));

            v3.setField(new PwsStringUnicodeField(PwsRecordV3.URL , getUrl()));
            v3.setField(new PwsStringUnicodeField(PwsRecordV3.AUTOTYPE , getAutotype()));
            setSafeDate(v3,PwsRecordV3.LAST_ACCESS_TIME, getLastAccess());
            setSafeDate(v3,PwsRecordV3.LAST_MOD_TIME, getLastChange());
            setSafeDate(v3,PwsRecordV3.PASSWORD_MOD_TIME, getLastPwChange());
            setSafeDate(v3,PwsRecordV3.PASSWORD_LIFETIME, getExpires());
            // never changes, so dont't mess:
//          v3.setField(new PwsUUIDField(PwsRecordV3.UUID, getId()));
//          v3.setField(new PwsTimeField(PwsRecordV3.CREATION_TIME, getCreated()));

		} else if (nextRecord instanceof PwsRecordV2) {

            PwsRecordV2 v2 = (PwsRecordV2) nextRecord;
            v2.setField(new PwsStringField(PwsRecordV2.GROUP , getGroup()));
            v2.setField(new PwsStringField(PwsRecordV2.TITLE , getTitle()));
            v2.setField(new PwsStringField(PwsRecordV2.USERNAME , getUsername()));
            v2.setField(new PwsStringField(PwsRecordV2.PASSWORD , getPassword().toString()));
            v2.setField(new PwsStringField(PwsRecordV2.NOTES , getNotes()));

        } else {

            PwsRecordV1 v1 = (PwsRecordV1) nextRecord;
            v1.setField(new PwsStringField(PwsRecordV1.TITLE , getTitle()));
            v1.setField(new PwsStringField(PwsRecordV1.USERNAME , getUsername()));
            v1.setField(new PwsStringField(PwsRecordV1.PASSWORD , getPassword().toString()));
            v1.setField(new PwsStringField(PwsRecordV1.NOTES , getNotes()));

        }
	}


    public static PwsEntryBean fromPwsRecord(PwsRecord nextRecord) {
    	PwsEntryBean newEntry = new PwsEntryBean();

        if (nextRecord instanceof PwsRecordV3) {

        	PwsRecordV3 v3 = (PwsRecordV3) nextRecord;

            PwsUUIDField idField = (PwsUUIDField) v3.getField(PwsFieldTypeV3.UUID);
            if (idField != null)
            	newEntry.setId((UUID) idField.getValue());

            String groupName = getSafeValue(v3, PwsFieldTypeV3.GROUP);
            newEntry.setGroup(groupName);

            String title = getSafeValue(v3,PwsFieldTypeV3.TITLE);
            newEntry.setTitle(title);

            String user = getSafeValue(v3, PwsFieldTypeV3.USERNAME);
            newEntry.setUsername(user);

            String password = getSafeValue(v3,PwsFieldTypeV3.PASSWORD);
            newEntry.setPassword(new StringBuilder(password)); // TODO: change PwsRecord to StringBuilder as well?

            String notes = getSafeValue(v3,PwsFieldTypeV3.NOTES);
            newEntry.setNotes(notes);

            String url = getSafeValue(v3,PwsFieldTypeV3.URL);
            newEntry.setUrl(url);

            String autotype = getSafeValue(v3,PwsFieldTypeV3.AUTOTYPE);
            newEntry.setAutotype(autotype);

            newEntry.setLastChange(getSafeDate(v3, PwsFieldTypeV3.LAST_MOD_TIME));

            newEntry.setCreated(getSafeDate(v3, PwsFieldTypeV3.CREATION_TIME));

            newEntry.setLastAccess(getSafeDate(v3, PwsFieldTypeV3.LAST_ACCESS_TIME));

            newEntry.setLastPwChange(getSafeDate(v3, PwsFieldTypeV3.PASSWORD_MOD_TIME));

           	newEntry.setExpires(getSafeDate(v3, PwsFieldTypeV3.PASSWORD_LIFETIME));

            newEntry.setVersion("3");

            if (log.isDebug1Enabled())
            	log.debug1("PwsEntryBean created " + newEntry.toString());

        } else if (nextRecord instanceof PwsRecordV2) {

            PwsRecordV2 v2 = (PwsRecordV2) nextRecord;

            String groupName = getSafeValue(v2, PwsFieldTypeV2.GROUP);
            newEntry.setGroup(groupName);

            String title = getSafeValue(v2,PwsFieldTypeV2.TITLE);
            newEntry.setTitle(title);

            String user = getSafeValue(v2, PwsFieldTypeV2.USERNAME);
            newEntry.setUsername(user);

            String password = getSafeValue(v2,PwsFieldTypeV2.PASSWORD);
            newEntry.setPassword(new StringBuilder(password));

            String notes = getSafeValue(v2,PwsFieldTypeV2.NOTES);
            newEntry.setNotes(notes);

            newEntry.setVersion("2");

        } else {
            PwsRecordV1 v1 = (PwsRecordV1) nextRecord;

            String title = getSafeValue(v1,PwsFieldTypeV1.TITLE);
            newEntry.setTitle(title);

            String user = getSafeValue(v1,PwsFieldTypeV1.USERNAME);
            newEntry.setUsername(user);

            String password = getSafeValue(v1,PwsFieldTypeV1.PASSWORD);
            newEntry.setPassword(new StringBuilder(password));

            String notes = getSafeValue(v1,PwsFieldTypeV1.NOTES);
            newEntry.setNotes(notes);

            newEntry.setVersion("1");
        }
        return newEntry;
    }

    public static PwsEntryBean fromPwsRecord(PwsRecord nextRecord, Set<? extends PwsFieldType> sparseFields) {
    	PwsEntryBean newEntry = new PwsEntryBean();
        if (nextRecord instanceof PwsRecordV3) {
        	PwsRecordV3 v3 = (PwsRecordV3) nextRecord;
        	for (Iterator<? extends PwsFieldType> it = sparseFields.iterator(); it.hasNext();) {
        		PwsFieldTypeV3 theType = (PwsFieldTypeV3) it.next();
                String theField = getSafeValue(v3, theType);
                newEntry.setVersion("3");
                switch (theType) {
        			case GROUP:
        				newEntry.setGroup(theField);
        				break;
        			case TITLE:
        				newEntry.setTitle(theField);
        				break;
        			case USERNAME:
        				newEntry.setUsername(theField);
        				break;
        			case PASSWORD:
        				newEntry.setPassword(new StringBuilder (theField));
        				break;
        			case NOTES:
        	            newEntry.setNotes(theField);
        				break;
        			case URL:
        	            newEntry.setUrl(theField);
        				break;
        			default:
        				log.warn("Ignored Sparse field type " + theType);
        		}
        	}
        } else if (nextRecord instanceof PwsRecordV2) {
            PwsRecordV2 v2 = (PwsRecordV2) nextRecord;
        	for (Iterator<? extends PwsFieldType> it = sparseFields.iterator(); it.hasNext();) {
        		PwsFieldTypeV2 theType = (PwsFieldTypeV2) it.next();
                String theField = getSafeValue(v2, theType);
                newEntry.setVersion("2");
                switch (theType) {
        			case GROUP:
        				newEntry.setGroup(theField);
        				break;
        			case TITLE:
        				newEntry.setTitle(theField);
        				break;
        			case USERNAME:
        				newEntry.setUsername(theField);
        				break;
        			case PASSWORD:
        				newEntry.setPassword(new StringBuilder (theField));
        				break;
        			case NOTES:
        	            newEntry.setNotes(theField);
        				break;
        			default:
        				log.warn("Ignored Sparse field type " + theType);
        		}
        	}
        } else {
            PwsRecordV1 v1 = (PwsRecordV1) nextRecord;
        	for (Iterator<? extends PwsFieldType> it = sparseFields.iterator(); it.hasNext();) {
        		PwsFieldTypeV1 theType = (PwsFieldTypeV1) it.next();
                String theField = getSafeValue(v1, theType);
                newEntry.setVersion("1");
                switch ( theType) {
        			case TITLE:
        				newEntry.setTitle(theField);
        				break;
        			case USERNAME:
        				newEntry.setUsername(theField);
        				break;
        			case PASSWORD:
        				newEntry.setPassword(new StringBuilder (theField));
        				break;
        			case NOTES:
        	            newEntry.setNotes(theField);
        				break;
        			default:
        				log.warn("Ignored Sparse field type " + theType);
        		}
        	}
        }
        return newEntry;
    }

}
