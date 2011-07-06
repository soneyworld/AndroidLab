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
   * The name of the attribute that holds the user's primary postal address.
   */
  public static final String PRIMARY_ADDRESS = "postalAddress";

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

  // The set of attributes (with lowercase names) used to hold e-mail addresses.
  private static final Set<String> emailAttrs;

  // The set of attributes (with lowercase names) that should be hidden in the
  // normal view.
  private static final Set<String> hiddenAttrs;

  // The set of attributes (with lowercase names) used to hold phone numbers.
  private static final Set<String> phoneNumberAttrs;

  // The set of attributes (with lowercase names) used to hold postal addresses.
  private static final Set<String> postalAddressAttrs;

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
    final LinkedHashSet<String> aSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> eSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> hSet = new LinkedHashSet<String>(10);
    final LinkedHashSet<String> tSet = new LinkedHashSet<String>(10);

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

    eSet.add(toLowerCase("mail"));
    eSet.add(toLowerCase("mailAlternateAddress"));

    tSet.add(toLowerCase("telephoneNumber"));
    tSet.add(toLowerCase("mobile"));
    tSet.add(toLowerCase("pager"));
    tSet.add(toLowerCase("homePhone"));
    tSet.add(toLowerCase("facsimileTelephoneNumber"));

    aSet.add(toLowerCase("postalAddress"));
    aSet.add(toLowerCase("homePostalAddress"));
    aSet.add(toLowerCase("postalCode"));

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

    attrMap = Collections.unmodifiableMap(aMap);
    emailAttrs = Collections.unmodifiableSet(eSet);
    hiddenAttrs = Collections.unmodifiableSet(hSet);
    postalAddressAttrs = Collections.unmodifiableSet(aSet);
    phoneNumberAttrs = Collections.unmodifiableSet(tSet);
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
        || postalAddressAttrs.contains(lowerName)) {
      return false;
    }

    return true;
  }
}
