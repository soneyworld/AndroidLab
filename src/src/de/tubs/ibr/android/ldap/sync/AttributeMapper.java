/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
package de.tubs.ibr.android.ldap.sync;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import android.provider.Contacts.Intents.UI;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.provider.StringProvider;
import static com.unboundid.util.StaticUtils.*;

/**
 * This class provides a mechanism for mapping LDAP attribute names to
 * user-friendly names.
 */
public final class AttributeMapper {
  /**
   * The name of the attribute that holds the user's alternate e-mail address.
   */
  @Deprecated
  public static final String ALTERNATE_MAIL = "mailAlternateAddress";

  /**
   * The name of the attribute that holds the user's fax number.
   */
  public static final String FAX = "facsimileTelephoneNumber";

  /**
   * The name of the attribute that holds the user's first name.
   */
  public static final String FIRST_NAME = "givenName";

  /**
   * The name of the attribute that holds the user's full name.
   */
  public static final String FULL_NAME = "cn";

  /**
   * The name of the attribute that holds the user's home postal address.
   */
  public static final String HOME_ADDRESS = "homePostalAddress";

  /**
   * The name of the attribute that holds the user's home phone number.
   */
  public static final String HOME_PHONE = "homePhone";

  /**
   * The name of the attribute that holds the user's last name.
   */
  public static final String LAST_NAME = "sn";

  /**
   * The name of the attribute that holds the user's mobile phone number.
   */
  public static final String MOBILE_PHONE = "mobile";

  /**
   * The name of the attribute that holds the user's organization.
   */
  public static final String ORGANIZATION = "o";

  /**
   * The name of the attribute that holds the user's organization unit.
   */
  public static final String ORGANIZATION_UNIT = "ou";

  /**
   * The name of the attribute that holds the user's pager number.
   */
  public static final String PAGER = "pager";

  /**
   * The name of the attribute that holds the user's primary e-mail address.
   */
  public static final String PRIMARY_MAIL = "mail";

  /**
   * The name of the attribute that holds the user's primary phone number.
   */
  public static final String PRIMARY_PHONE = "telephoneNumber";

  /**
   * The name of the attribute that holds the user's title.
   */
  public static final String TITLE = "title";

  /**
   * The name of the attribute that holds the user's id.
   */
  public static final String ATTR_UID = "entryUUID";

  /**
   * The name of the attribute that holds the user's id.
   */
  public static final String UID = "uid";

  public static final String DESCRIPTION = "description";

  public static final String DN = "dn";

  // The map of lowercase attribute names to the numeric identifiers for display
  // names.
  private static final Map<String, Integer> attrMap;

  // The set of attributes (with lowercase names) used to hold name details.
  private static final Set<String> nameAttrs;

  // The set of attributes (with lowercase names) used to hold name details
  // mapped together.
  private static final Set<String> nameSubAttrs;

  // The set of attributes (with lowercase names) used to hold description
  // details.
  private static final Set<String> descAttrs;

  // The set of attributes (with lowercase names) used to hold e-mail addresses.
  private static final Set<String> emailAttrs;

  // The set of attributes (with lowercase names) that should be hidden in the
  // normal view.
  private static final Set<String> hiddenAttrs;

  // The set of attributes (with lowercase names) used to hold phone numbers.
  private static final Set<String> phoneNumberAttrs;

  // The set of attributes (with lowercase names) used to hold postal addresses.
  private static final Set<String> postalAddressAttrs;

  // The set of attributes (with lowercase names) used to hold postal addresses.
  private static final Set<String> postalWorkAddressAttrs;

  // The set of attributes (with lowercase names) used to hold postal addresses.
  private static final Set<String> postalHomeAddressAttrs;

  // The set of attributes (with lowercase names) used to hold web addresses.
  private static final Set<String> webAttrs;

  // The set of attributes (with lowercase names) used to hold organization
  // informations.
  private static final Set<String> orgaAttrs;

  // The set of attributes (with lowercase names) used to hold organization
  // informations mapped together.
  private static final Set<String> orgaSubAttrs;

  // The set of attributes (with lowercase names) used to hold informations
  // mapped in a single row.
  private static final Set<String> rowAttrs;
  /**
   * This attribute holds a postal address suitable for reception of telegrams
   * or expedited documents, where it is necessary to have the recipient accept
   * delivery.
   */
  public static final String REGISTERED_ADDRESS = "registeredAddress";

  /**
   * This attribute holds a postal address suitable for reception of telegrams
   * or expedited documents, where it is necessary to have the recipient accept
   * delivery.
   */
  public static final String PREFERRED_DELIVERY_METHOD = "preferredDeliveryMethod";

  /**
   * This attribute is used for the telegram service.
   */
  public static final String DESTINATION_INDICATOR = "destinationIndicator";

  public static final String INTERNATIONAL_ISDN_NUMBER = "internationaliSDNNumber";

  public static final String STREET = "street";

  public static final String POST_OFFICE_BOX = "postOfficeBox";

  public static final String POSTAL_CODE = "postalCode";

  public static final String POSTAL_ADDRESS = "postalAddress";

  public static final String PHYSICAL_DELIVERY_OFFICE_NAME = "physicalDeliveryOfficeName";

  /**
   * This attribute contains the full name of a state or province
   */
  public static final String STATE = "st";

  /**
   * This attribute contains the name of a locality, such as a city, county or
   * other geographic region (localityName).
   */
  public static final String LOCALITY = "l";

  public static final String BUSINESS_CATEGORY = "businessCategory";

  public static final String DISPLAYNAME = "displayName";

  public static final String DEPARTMENT_NUMBER = "departmentNumber";

  public static final String INITIALS = "initials";

  public static final String ROOM_NUMBER = "roomNumber";

  public static final String PREFERRED_LANGUAGE = "preferredLanguage";

  public static final String TELEX = "telexNumber";

  public static final String ISDN = "internationaliSDNNumber";

  public static final String SEE_ALSO = "seeAlso";

  static {
    final HashMap<String, Integer> aMap = new HashMap<String, Integer>(10);
    final LinkedHashSet<String> nSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> nSubSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> aSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> eSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> wSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> oSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> oSubSet = new LinkedHashSet<String>(4);
    final LinkedHashSet<String> hSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> tSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> dSet = new LinkedHashSet<String>(1);
    final LinkedHashSet<String> waSet = new LinkedHashSet<String>(4);
    final LinkedHashSet<String> haSet = new LinkedHashSet<String>(4);
    final LinkedHashSet<String> rowSet = new LinkedHashSet<String>(4);
    aMap.put(toLowerCase("cn"), R.string.attribute_mapper_name_cn);

    aMap.put(toLowerCase("departmentNumber"),
        R.string.attribute_mapper_name_departmentNumber);

    aMap.put(toLowerCase("description"),
        R.string.attribute_mapper_name_description);

    aMap.put(toLowerCase("displayName"),
        R.string.attribute_mapper_name_displayName);

    aMap.put(toLowerCase("employeeNumber"),
        R.string.attribute_mapper_name_employeeNumber);

    aMap.put(toLowerCase("facsimileTelephoneNumber"),
        R.string.attribute_mapper_name_facsimileTelephoneNumber);

    aMap.put(toLowerCase("givenName"), R.string.attribute_mapper_name_givenName);

    aMap.put(toLowerCase("homePhone"), R.string.attribute_mapper_name_homePhone);

    aMap.put(toLowerCase("homePostalAddress"),
        R.string.attribute_mapper_name_homePostalAddress);

    aMap.put(toLowerCase("initials"), R.string.attribute_mapper_name_initials);

    aMap.put(toLowerCase("jpegPhoto"), R.string.attribute_mapper_name_jpegPhoto);

    aMap.put(toLowerCase("l"), R.string.attribute_mapper_name_l);

    aMap.put(toLowerCase("labeledURI"),
        R.string.attribute_mapper_name_labeledURI);

    aMap.put(toLowerCase("mail"), R.string.attribute_mapper_name_mail);

    aMap.put(toLowerCase("mailAlternateAddress"),
        R.string.attribute_mapper_name_mailAlternateAddress);

    aMap.put(toLowerCase("mobile"), R.string.attribute_mapper_name_mobile);

    aMap.put(toLowerCase("o"), R.string.attribute_mapper_name_o);

    aMap.put(toLowerCase("pager"), R.string.attribute_mapper_name_pager);

    aMap.put(toLowerCase("photo"), R.string.attribute_mapper_name_photo);

    aMap.put(toLowerCase("postalAddress"),
        R.string.attribute_mapper_name_postalAddress);

    aMap.put(toLowerCase("postalCode"),
        R.string.attribute_mapper_name_postalCode);

    aMap.put(toLowerCase("postOfficeBox"),
        R.string.attribute_mapper_name_postOfficeBox);

    aMap.put(toLowerCase("preferredLanguage"),
        R.string.attribute_mapper_name_preferredLanguage);

    aMap.put(toLowerCase("roomNumber"),
        R.string.attribute_mapper_name_roomNumber);

    aMap.put(toLowerCase("sn"), R.string.attribute_mapper_name_sn);

    aMap.put(toLowerCase("st"), R.string.attribute_mapper_name_st);

    aMap.put(toLowerCase("street"), R.string.attribute_mapper_name_street);

    aMap.put(toLowerCase("telephoneNumber"),
        R.string.attribute_mapper_name_telephoneNumber);

    aMap.put(toLowerCase("title"), R.string.attribute_mapper_name_title);

    aMap.put(toLowerCase("uid"), R.string.attribute_mapper_name_uid);

    eSet.add(toLowerCase(PRIMARY_MAIL));
    eSet.add(toLowerCase(ALTERNATE_MAIL));

    nSet.add(toLowerCase(FIRST_NAME));
    nSet.add(toLowerCase(DISPLAYNAME));
    nSet.add(toLowerCase(LAST_NAME));
    nSet.add(toLowerCase(INITIALS));
    nSet.add(toLowerCase(TITLE));
    nSet.add(toLowerCase(FULL_NAME));

    nSubSet.add(toLowerCase(FIRST_NAME));
    nSubSet.add(toLowerCase(DISPLAYNAME));
    nSubSet.add(toLowerCase(LAST_NAME));
    nSubSet.add(toLowerCase(TITLE));
    nSubSet.add(toLowerCase(FULL_NAME));

    dSet.add(toLowerCase(DESCRIPTION));

    tSet.add(toLowerCase(PRIMARY_PHONE));
    tSet.add(toLowerCase(HOME_PHONE));
    tSet.add(toLowerCase(MOBILE_PHONE));
    tSet.add(toLowerCase(FAX));
    tSet.add(toLowerCase(PAGER));
    tSet.add(toLowerCase(TELEX));
    tSet.add(toLowerCase(ISDN));

    aSet.add(toLowerCase(DESTINATION_INDICATOR));
    aSet.add(toLowerCase(REGISTERED_ADDRESS));
    aSet.add(toLowerCase(STREET));
    aSet.add(toLowerCase(PREFERRED_DELIVERY_METHOD));
    aSet.add(toLowerCase(POST_OFFICE_BOX));
    aSet.add(toLowerCase(POSTAL_CODE));
    aSet.add(toLowerCase(POSTAL_ADDRESS));
    aSet.add(toLowerCase(HOME_ADDRESS));
    aSet.add(toLowerCase(STATE));

    waSet.add(toLowerCase(POST_OFFICE_BOX));
    waSet.add(toLowerCase(POSTAL_CODE));
    waSet.add(toLowerCase(POSTAL_ADDRESS));
    waSet.add(toLowerCase(STATE));

    haSet.add(toLowerCase(STREET));
    haSet.add(toLowerCase(HOME_ADDRESS));

    wSet.add(toLowerCase(SEE_ALSO));

    oSet.add(toLowerCase(ORGANIZATION));
    oSet.add(toLowerCase(ORGANIZATION_UNIT));
    oSet.add(toLowerCase(LOCALITY));
    oSet.add(toLowerCase(PREFERRED_LANGUAGE));
    oSet.add(toLowerCase(PHYSICAL_DELIVERY_OFFICE_NAME));
    oSet.add(toLowerCase(DEPARTMENT_NUMBER));
    oSet.add(toLowerCase(ROOM_NUMBER));
    oSet.add(toLowerCase(BUSINESS_CATEGORY));

    oSubSet.add(toLowerCase(ORGANIZATION));
    oSubSet.add(toLowerCase(ORGANIZATION_UNIT));
    oSubSet.add(toLowerCase(LOCALITY));
    oSubSet.add(toLowerCase(BUSINESS_CATEGORY));

    hSet.add(toLowerCase("jpegPhoto"));
    hSet.add(toLowerCase("manager"));
    hSet.add(toLowerCase("objectClass"));
    hSet.add(toLowerCase("photo"));
    hSet.add(toLowerCase("secretary"));
    hSet.add(toLowerCase("seeAlso"));
    hSet.add(toLowerCase("userCertificate"));
    hSet.add(toLowerCase("userPassword"));
    hSet.add(toLowerCase("userPKCS12"));
    hSet.add(toLowerCase("userSMIMECertificate"));

    rowSet.add(toLowerCase(UID));
    rowSet.add(toLowerCase(PREFERRED_LANGUAGE));
    rowSet.add(toLowerCase(PHYSICAL_DELIVERY_OFFICE_NAME));
    rowSet.add(toLowerCase(DEPARTMENT_NUMBER));
    rowSet.add(toLowerCase(ROOM_NUMBER));
    rowSet.add(toLowerCase(DESTINATION_INDICATOR));
    rowSet.add(toLowerCase(REGISTERED_ADDRESS));
    rowSet.add(toLowerCase(PREFERRED_DELIVERY_METHOD));

    attrMap = Collections.unmodifiableMap(aMap);

    descAttrs = Collections.unmodifiableSet(dSet);
    nameAttrs = Collections.unmodifiableSet(nSet);
    nameSubAttrs = Collections.unmodifiableSet(nSubSet);
    emailAttrs = Collections.unmodifiableSet(eSet);
    hiddenAttrs = Collections.unmodifiableSet(hSet);
    postalAddressAttrs = Collections.unmodifiableSet(aSet);
    postalWorkAddressAttrs = Collections.unmodifiableSet(waSet);
    postalHomeAddressAttrs = Collections.unmodifiableSet(waSet);
    phoneNumberAttrs = Collections.unmodifiableSet(tSet);
    webAttrs = Collections.unmodifiableSet(wSet);
    orgaAttrs = Collections.unmodifiableSet(oSet);
    orgaSubAttrs = Collections.unmodifiableSet(oSubSet);
    rowAttrs = Collections.unmodifiableSet(rowSet);
  }

  /**
   * Retrieves the display name for the provided attribute name.
   * 
   * @param invoker
   *          The activity that invoked this class.
   * @param attrName
   *          The name of the attribute for which to retrieve the display name.
   * @return The display name for the provided attribute name, or the provided
   *         attribute name if there is no defined mapping.
   */
  public static String getDisplayName(final StringProvider invoker,
      final String attrName) {
    // logEnter(LOG_TAG, "getDisplayName", invoker, attrName);

    final Integer displayNameID = attrMap.get(toLowerCase(attrName));
    if (displayNameID != null) {
      return invoker.getString(displayNameID);
    }

    return attrName;
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * e-mail addresses.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         e-mail addresses.
   */
  public static Set<String> getEMailAttrs() {
    return emailAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * e-mail address attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined e-mail address attributes, or {@code false} if not.
   */
  public static boolean isEMailAttr(final String s) {
    return emailAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that
   * should be hidden in the normal view.
   * 
   * @return A set containing the lowercase names of the attributes that should
   *         be hidden in the normal view.
   */
  public static Set<String> getHiddenAttrs() {
    return hiddenAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the attributes
   * that should be hidden in the normal view.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         attributes that should be hidden in the normal view, or
   *         {@code false} if not.
   */
  public static boolean isHiddenAttr(final String s) {
    return hiddenAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that
   * contain name informations.
   * 
   * @return A set containing the lowercase names of the attributes that contain
   *         name informations.
   */
  public static Set<String> getNameAttrs() {
    return nameAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the attributes
   * that contains name informations.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of a name
   *         information containing attributes, or {@code false} if not.
   */
  public static boolean isNameAttr(final String s) {
    return nameAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that
   * contain name informations.
   * 
   * @return A set containing the lowercase names of the attributes that contain
   *         name informations.
   */
  public static Set<String> getNameSubAttrs() {
    return nameSubAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the attributes
   * that contains name informations.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of a name
   *         information containing attributes, or {@code false} if not.
   */
  public static boolean isNameSubAttr(final String s) {
    return nameSubAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * postal addresses.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         postal addresses.
   */
  public static Set<String> getPostalAddressAttrs() {
    return postalAddressAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * postal address attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined postal address attributes, or {@code false} if not.
   */
  public static boolean isPostalAddressAttr(final String s) {
    return postalAddressAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * postal addresses.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         postal addresses.
   */
  public static Set<String> getPostalHomeAddressAttrs() {
    return postalHomeAddressAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * postal address attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined postal address attributes, or {@code false} if not.
   */
  public static boolean isPostalHomeAddressAttr(final String s) {
    return postalHomeAddressAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * postal addresses.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         postal addresses.
   */
  public static Set<String> getPostalWorkAddressAttrs() {
    return postalWorkAddressAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * postal address attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined postal address attributes, or {@code false} if not.
   */
  public static boolean isPostalWorkAddressAttr(final String s) {
    return postalWorkAddressAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * phone numbers.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         phone numbers.
   */
  public static Set<String> getPhoneNumberAttrs() {
    return phoneNumberAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * phone number attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined phone number attributes, or {@code false} if not.
   */
  public static boolean isPhoneNumberAttr(final String s) {
    return phoneNumberAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * descriptions.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         descriptions.
   */
  public static Set<String> getDescAttrs() {
    return descAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * description attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined description attributes, or {@code false} if not.
   */
  public static boolean isDescriptionAttr(final String s) {
    return descAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * web addresses.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         web addresses.
   */
  public static Set<String> getWebAttrs() {
    return webAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined web
   * address attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined web address attributes, or {@code false} if not.
   */
  public static boolean isWebAttr(final String s) {
    return webAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * organization informations.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         organization informations.
   */
  public static Set<String> getOrganizationAttrs() {
    return orgaAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * organization information attributes.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined organization information attributes, or {@code false} if
   *         not.
   */
  public static boolean isOrganizationAttr(final String s) {
    return orgaAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * organization informations mapped together.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         organization informations.
   */
  public static Set<String> getOrganizationSubAttrs() {
    return orgaSubAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * organization information attributes mapped together.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined organization information attributes, or {@code false} if
   *         not.
   */
  public static boolean isOrganizationSubAttr(final String s) {
    return orgaSubAttrs.contains(toLowerCase(s));
  }

  /**
   * Retrieves a set containing the lowercase names of the attributes that hold
   * informations in a single row.
   * 
   * @return A set containing the lowercase names of the attributes that hold
   *         informations in a single row.
   */
  public static Set<String> getRowAttrs() {
    return rowAttrs;
  }

  /**
   * Indicates whether the provided string is the name of one of the defined
   * information attributes in a single row.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of one of the
   *         defined single row information attributes, or {@code false} if not.
   */
  public static boolean isRowAttr(final String s) {
    return rowAttrs.contains(toLowerCase(s));
  }

  /**
   * Indicates whether the provided string is the name of an attribute that
   * should be treated as a "generic" attribute.
   * 
   * @param s
   *          The name for which to make the determination. It must not be
   *          {@code null}.
   * @return {@code true} if the provided string is the name of an attribute
   *         that should be treated as a generic attribute, or {@code false} if
   *         not.
   */
  public static boolean isGenericAttr(final String s) {

    final String lowerName = toLowerCase(s);
    if (emailAttrs.contains(lowerName) || hiddenAttrs.contains(lowerName)
        || phoneNumberAttrs.contains(lowerName)
        || postalAddressAttrs.contains(lowerName)
        || nameAttrs.contains(lowerName) || webAttrs.contains(lowerName)
        || descAttrs.contains(lowerName) || orgaAttrs.contains(lowerName)) {
      return false;
    }

    return true;
  }

  public static boolean isContactAttr(String name) {
    if (isEMailAttr(name) || isPhoneNumberAttr(name)
        || isPostalAddressAttr(name) || isNameAttr(name) || isWebAttr(name)
        || isDescriptionAttr(name) || isOrganizationAttr(name)
        || name.equalsIgnoreCase(UID) || isRowAttr(name)) {
      return true;
    }
    return false;
  }
}
