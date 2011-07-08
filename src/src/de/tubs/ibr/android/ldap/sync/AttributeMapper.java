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
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.provider.StringProvider;

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

  // The set of all attributes (with lowercase names) used to hold informations
  // containing contact information.
  private static final Set<String> contactAttrs;
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

  @Deprecated
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
    final LinkedHashSet<String> cSet = new LinkedHashSet<String>();
    aMap.put("cn", R.string.attribute_mapper_name_cn);

    aMap.put("departmentNumber",
        R.string.attribute_mapper_name_departmentNumber);

    aMap.put("description",
        R.string.attribute_mapper_name_description);

    aMap.put("displayName",
        R.string.attribute_mapper_name_displayName);

    aMap.put("employeeNumber",
        R.string.attribute_mapper_name_employeeNumber);

    aMap.put("facsimileTelephoneNumber",
        R.string.attribute_mapper_name_facsimileTelephoneNumber);

    aMap.put("givenName", R.string.attribute_mapper_name_givenName);

    aMap.put("homePhone", R.string.attribute_mapper_name_homePhone);

    aMap.put("homePostalAddress",
        R.string.attribute_mapper_name_homePostalAddress);

    aMap.put("initials", R.string.attribute_mapper_name_initials);

    aMap.put("jpegPhoto", R.string.attribute_mapper_name_jpegPhoto);

    aMap.put("l", R.string.attribute_mapper_name_l);

    aMap.put("labeledURI",
        R.string.attribute_mapper_name_labeledURI);

    aMap.put("mail", R.string.attribute_mapper_name_mail);

    aMap.put("mailAlternateAddress",
        R.string.attribute_mapper_name_mailAlternateAddress);

    aMap.put("mobile", R.string.attribute_mapper_name_mobile);

    aMap.put("o", R.string.attribute_mapper_name_o);

    aMap.put("pager", R.string.attribute_mapper_name_pager);

    aMap.put("photo", R.string.attribute_mapper_name_photo);

    aMap.put("postalAddress",
        R.string.attribute_mapper_name_postalAddress);

    aMap.put("postalCode",
        R.string.attribute_mapper_name_postalCode);

    aMap.put("postOfficeBox",
        R.string.attribute_mapper_name_postOfficeBox);

    aMap.put("preferredLanguage",
        R.string.attribute_mapper_name_preferredLanguage);

    aMap.put("roomNumber",
        R.string.attribute_mapper_name_roomNumber);

    aMap.put("sn", R.string.attribute_mapper_name_sn);

    aMap.put("st", R.string.attribute_mapper_name_st);

    aMap.put("street", R.string.attribute_mapper_name_street);

    aMap.put("telephoneNumber",
        R.string.attribute_mapper_name_telephoneNumber);

    aMap.put("title", R.string.attribute_mapper_name_title);

    aMap.put("uid", R.string.attribute_mapper_name_uid);

    eSet.add(PRIMARY_MAIL);
    eSet.add(ALTERNATE_MAIL);

    nSet.add(FIRST_NAME);
    nSet.add(DISPLAYNAME);
    nSet.add(LAST_NAME);
    nSet.add(INITIALS);
    nSet.add(TITLE);
    nSet.add(FULL_NAME);

    nSubSet.add(FIRST_NAME);
    nSubSet.add(DISPLAYNAME);
    nSubSet.add(LAST_NAME);
    nSubSet.add(TITLE);
    nSubSet.add(FULL_NAME);

    dSet.add(DESCRIPTION);

    tSet.add(PRIMARY_PHONE);
    tSet.add(HOME_PHONE);
    tSet.add(MOBILE_PHONE);
    tSet.add(FAX);
    tSet.add(PAGER);
    tSet.add(TELEX);
    tSet.add(ISDN);

    aSet.add(DESTINATION_INDICATOR);
    aSet.add(REGISTERED_ADDRESS);
    aSet.add(STREET);
    aSet.add(PREFERRED_DELIVERY_METHOD);
    aSet.add(POST_OFFICE_BOX);
    aSet.add(POSTAL_CODE);
    aSet.add(POSTAL_ADDRESS);
    aSet.add(HOME_ADDRESS);
    aSet.add(STATE);

    waSet.add(POST_OFFICE_BOX);
    waSet.add(POSTAL_CODE);
    waSet.add(POSTAL_ADDRESS);
    waSet.add(STATE);

    haSet.add(STREET);
    haSet.add(HOME_ADDRESS);

    wSet.add(SEE_ALSO);

    oSet.add(ORGANIZATION);
    oSet.add(ORGANIZATION_UNIT);
    oSet.add(LOCALITY);
    oSet.add(PREFERRED_LANGUAGE);
    oSet.add(PHYSICAL_DELIVERY_OFFICE_NAME);
    oSet.add(DEPARTMENT_NUMBER);
    oSet.add(ROOM_NUMBER);
    oSet.add(BUSINESS_CATEGORY);

    oSubSet.add(ORGANIZATION);
    oSubSet.add(ORGANIZATION_UNIT);
    oSubSet.add(LOCALITY);
    oSubSet.add(BUSINESS_CATEGORY);

    hSet.add("jpegPhoto");
    hSet.add("manager");
    hSet.add("objectClass");
    hSet.add("photo");
    hSet.add("secretary");
    hSet.add("seeAlso");
    hSet.add("userCertificate");
    hSet.add("userPassword");
    hSet.add("userPKCS12");
    hSet.add("userSMIMECertificate");

    rowSet.add(UID);
    rowSet.add(PREFERRED_LANGUAGE);
    rowSet.add(PHYSICAL_DELIVERY_OFFICE_NAME);
    rowSet.add(DEPARTMENT_NUMBER);
    rowSet.add(ROOM_NUMBER);
    rowSet.add(DESTINATION_INDICATOR);
    rowSet.add(REGISTERED_ADDRESS);
    rowSet.add(PREFERRED_DELIVERY_METHOD);

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

    cSet.addAll(emailAttrs);
    cSet.addAll(phoneNumberAttrs);
    cSet.addAll(postalAddressAttrs);
    cSet.addAll(nameAttrs);
    cSet.addAll(webAttrs);
    cSet.addAll(descAttrs);
    cSet.addAll(orgaAttrs);
    cSet.addAll(rowAttrs);
    contactAttrs = Collections.unmodifiableSet(cSet);

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

    final Integer displayNameID = attrMap.get(attrName);
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
    return emailAttrs.contains(s);
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
    return hiddenAttrs.contains(s);
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
    return nameAttrs.contains(s);
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
    return nameSubAttrs.contains(s);
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
    return postalAddressAttrs.contains(s);
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
    return postalHomeAddressAttrs.contains(s);
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
    return postalWorkAddressAttrs.contains(s);
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
    return phoneNumberAttrs.contains(s);
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
    return descAttrs.contains(s);
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
    return webAttrs.contains(s);
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
    return orgaAttrs.contains(s);
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
    return orgaSubAttrs.contains(s);
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
    return rowAttrs.contains(s);
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

    final String lowerName = s;
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
    if (contactAttrs.contains(name)) {
      return true;
    }
    return false;
  }

  public static Set<String> getContactAttrs() {
    return contactAttrs;
  }
}
