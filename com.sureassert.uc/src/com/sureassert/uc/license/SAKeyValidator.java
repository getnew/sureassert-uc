package com.sureassert.uc.license;

import java.util.Calendar;
import java.util.Date;

import com.sureassert.uc.SaUCBuilderActivator;
import com.sureassert.uc.license.InvalidLicenseKeyException.ErrorType;

public class SAKeyValidator {

	public LicenseKeyData validate(String key, int majorVersion, int minorVersion, String email) throws InvalidLicenseKeyException {

		// Pre-conditions
		if (key == null || key.trim().equals(""))
			throw new InvalidLicenseKeyException(ErrorType.NULL, "No license key entered");
		if (key.length() != 24)
			throw new InvalidLicenseKeyException(ErrorType.INVALID_LENGTH, "Invalid license key");

		try {

			// Validate format
			validateChars(key);

			// Validate check bits
			String r1 = Character.toString(key.charAt(0));
			String cA = Character.toString(key.charAt(1));
			String v2 = Character.toString(key.charAt(2));
			String d2 = Character.toString(key.charAt(3));
			validateCheckBit(true, cA, r1, v2, d2);

			String cB = Character.toString(key.charAt(5));
			String r2 = Character.toString(key.charAt(6));
			String l = Character.toString(key.charAt(7));
			String e2 = Character.toString(key.charAt(8));
			validateCheckBit(true, cB, r2, l, e2);

			String r3 = Character.toString(key.charAt(10));
			String cC = Character.toString(key.charAt(11));
			String d1 = Character.toString(key.charAt(12));
			String e1 = Character.toString(key.charAt(13));
			validateCheckBit(true, cC, r3, d1, e1);

			String d3 = Character.toString(key.charAt(15));
			String r4 = Character.toString(key.charAt(16));
			String v1 = Character.toString(key.charAt(17));
			String cD = Character.toString(key.charAt(18));
			validateCheckBit(true, cD, d3, r4, v1);

			String r5 = Character.toString(key.charAt(20));
			String d4 = Character.toString(key.charAt(21));
			String cE = Character.toString(key.charAt(22));
			String cAll = Character.toString(key.charAt(23));
			validateCheckBit(true, cE, r5, d4);
			validateCheckBit(false, cAll, r1, v2, d2, r2, l, e2, r3, d1, e1, d3, r4, v1, r5, d4);

			// Get license type
			int licenseTypeID = -1;
			for (int typeIdx = 0; typeIdx < 3; typeIdx++) {
				for (int choiceIdx = 0; choiceIdx < 4; choiceIdx++) {
					if (l.equals(SAKeyGen.LICENSE_TYPE_CHAR_CHOICES_BY_ID[typeIdx][choiceIdx]))
						licenseTypeID = typeIdx;
				}
			}
			if (licenseTypeID == -1)
				throw new InvalidLicenseKeyException(ErrorType.INVALID_CHAR, "Invalid license key");

			// Get date
			Date createdOn = getCreatedOnDate(d1, d2, d3, d4);
			Calendar tomorrow = Calendar.getInstance();
			tomorrow.add(Calendar.DATE, 1);

			// Validate email
			String[] expectEmailChars = SAKeyGen.getEmailChars(email);
			if (!e1.equals(expectEmailChars[0]) || !e2.equals(expectEmailChars[1])) {
				throw new InvalidLicenseKeyException(ErrorType.INVALID_EMAIL, "License key/registered email mismatch");
			}

			int vHex1 = SAKeyGen.getHexChoiceAIdx(v1);
			int vHex2 = SAKeyGen.getHexChoiceAIdx(v2);
			Calendar licenseDatePlusSixMonths = Calendar.getInstance();
			licenseDatePlusSixMonths.setTime(createdOn);
			licenseDatePlusSixMonths.add(Calendar.MONTH, 3);
			if (licenseDatePlusSixMonths.getTimeInMillis() < SaUCBuilderActivator.RELEASE_DATE.getTime()) {
				// Validate version
				if (vHex1 != majorVersion || vHex2 != minorVersion)
					throw new InvalidLicenseKeyException(ErrorType.INVALID_VERSION, "Incorrect product version");
			}
			return new LicenseKeyData(vHex1, vHex2, majorVersion, minorVersion, createdOn, licenseTypeID);

		} catch (RuntimeException e) {
			throw new InvalidLicenseKeyException(ErrorType.INVALID_CHAR, "Invalid license key");
		}
	}

	private Date getCreatedOnDate(String d1, String d2, String d3, String d4) {

		int dHex1 = SAKeyGen.getHexChoiceAIdx(d1);
		int dHex2 = SAKeyGen.getHexChoiceAIdx(d2);
		int dHex3 = SAKeyGen.getHexChoiceAIdx(d3);
		int dHex4 = SAKeyGen.getHexChoiceAIdx(d4);
		int daysSince2011 = (dHex1 * 16 * 16 * 16) + (dHex2 * 16 * 16) + (dHex3 * 16) + dHex4;
		Calendar createdOnDate = Calendar.getInstance();
		createdOnDate.setTimeInMillis(SAKeyGen.startDateMillis);
		createdOnDate.add(Calendar.DATE, daysSince2011);
		return createdOnDate.getTime();
	}

	private void validateCheckBit(boolean useC2, String c, String... chars) throws InvalidLicenseKeyException {

		if (!c.equals(useC2 ? SAKeyGen.c2(chars) : SAKeyGen.c(chars)))
			throw new InvalidLicenseKeyException(ErrorType.CHECK_BIT_FAILURE, "Invalid license key");
	}

	private void validateChars(String str) throws InvalidLicenseKeyException {

		if (!str.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}"))
			throw new InvalidLicenseKeyException(ErrorType.INVALID_CHAR, "Invalid license key");
	}

	public static class LicenseKeyData {

		public int licensedVersionMajor;
		public int licensedVersionMinor;
		public int latestLicensedVersionMajor;
		public int latestLicensedVersionMinor;
		public Date createdDate;
		public int licenseTypeID;

		public LicenseKeyData(int licensedVersionMajor, int licensedVersionMinor, //
				int latestLicensedVersionMajor, int latestLicensedVersionMinor, //
				Date createdDate, int licenseTypeID) {

			this.licensedVersionMajor = licensedVersionMajor;
			this.licensedVersionMinor = licensedVersionMinor;
			this.latestLicensedVersionMajor = latestLicensedVersionMajor;
			this.latestLicensedVersionMinor = latestLicensedVersionMinor;
			this.createdDate = createdDate;
			this.licenseTypeID = licenseTypeID;
		}

		public String getLicenseTypeName() {

			if (licenseTypeID == SAKeyGen.LICENSE_TYPE_ID_FLOATING)
				return "Commercial floating seat license (limited users)";
			else if (licenseTypeID == SAKeyGen.LICENSE_TYPE_ID_INDIVIDUAL)
				return "Commercial individual user (non floating) license";
			else if (licenseTypeID == SAKeyGen.LICENSE_TYPE_ID_OPEN_SOURCE)
				return "Open-source development license";
			else
				return "Unknown";
		}

		public String getLicensedVersionStr() {

			if (licensedVersionMajor == latestLicensedVersionMajor && //
					licensedVersionMinor == latestLicensedVersionMinor) {
				return licensedVersionMajor + "." + licensedVersionMinor;
			} else {
				return licensedVersionMajor + "." + licensedVersionMinor + " - " + //
						latestLicensedVersionMajor + "." + latestLicensedVersionMinor;
			}
		}
	}
}
