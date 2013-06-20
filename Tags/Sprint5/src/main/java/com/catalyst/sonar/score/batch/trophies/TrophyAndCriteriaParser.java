package com.catalyst.sonar.score.batch.trophies;

import java.util.Map;

import org.sonar.api.config.Settings;
import org.sonar.api.database.configuration.Property;


/**
 * TrophyAndCriteriaParser parses a String from the database
 * into trophies with their criteria and adds them to a TrophySet. 
 */
public class TrophyAndCriteriaParser {
	private Property property;
	private Settings settings;
	public static final String GLOBALPROPERTYKEY = "sonar.score.Trophy";
	// Regular Expressions
	private static String regExpOne = "[\\;%}]";
	private static final String REGEX_LETTERS = "^[a-zA-Z\\s]+$";
	private static final String REGEX_NUMBERS = "^[0-9]+$";
	private static final String REGEX_ALPHANUMERIC = "^[0-9a-zA-Z]+$";
	private static final String REGEX_COMMA = "[\\,]";
	private static final String REGEX_CURLYBRACES = "{";
	private static final int NUMBER_OF_DAYS = 7;

	/**
	 * Gets all the properties and saves them to a hashmap.Then iterates through
	 * the hashmap and finds the value for the global property key for trophy
	 * 
	 * @param settings
	 * @return value
	 */
	public String getGlobalProperty(Settings settings) { 
		Map<String, String> propertyMap = settings.getProperties();
		String value = propertyMap.get(GLOBALPROPERTYKEY);
		if (value == null) {
			value = "";
		}
		return value;
	}

	/**
	 * Returns a set of trophies with criteria from an array of
	 * trophyPropertyStrings
	 * 
	 * @param trophyPropertyStringList
	 * @return
	 */
	public static TrophySet parseTrophies(String trophyPropertyStringList) {
		TrophySet trophies = new TrophySet();
		if(trophyPropertyStringList != null) {
			// Split the global property string at the ',' to separate between
			// each property
			String[] value = trophyPropertyStringList.split(REGEX_COMMA);
			for (String tPropertyString : value) {
				if(tPropertyString.matches("\\w+\\{.+;.+;.+\\}")) {
					// go through each property and extract the trophy name to
					// trophy
					Trophy trophy = extractTrophyName(tPropertyString);
					int indexNum = tPropertyString.indexOf(REGEX_CURLYBRACES);
					String criteriaString = tPropertyString.substring(++indexNum);
					// parse the rest of the string and get the criteria
					Criteria criteria = parseCriteria(criteriaString);
					trophy.addCriteria(criteria);
					trophies.add(trophy);
				}
			}
		}

		return trophies;

	}

	/**
	 * extracts a Trophy name from a global trophy property string
	 * 
	 * @param globalPropertyValue
	 * @return
	 */
	public static Trophy extractTrophyName(String globalPropertyValue) {
		int indexNum = globalPropertyValue.indexOf('{');
		String trophyName = globalPropertyValue.substring(0, indexNum);
		return new Trophy(trophyName);
	}

	/**
	 * parses the string and extracts the criteria from it
	 * 
	 * @param propertyString
	 * @return
	 */
	public static Criteria parseCriteria(String propertyString) {
		String metric = "";
		double requiredAmt = 0;
		int days = 0;
		String value = propertyString.trim();
		// split the criteria string into an array of strings
		String[] valueArray = value.split(regExpOne);
		
		for (String string : valueArray) {
			if (hasOnlyLetters(string)) {
				// metric value
				metric = string;
			} else if (hasOnlyNumbers(string)) {
				// required Amt
				requiredAmt = parseDouble(string);
			} else if (hasAlphaNumericCharacters(string)) {
				// extract the number from the string
				String numOne = string.substring(0, string.length() - 1);
				String numTwo = string.substring(string.length() - 1);
				// checks if the string ends with 'd' for number of days
				if (numTwo.equalsIgnoreCase("d")) {
					int number = parseInt(numOne);
					days = number;
				}
				// checks if the string ends with 'w' for number of
				// weeks
				else if (numTwo.equalsIgnoreCase("w")) {
					int secondNum = parseInt(numOne);
					// converts the weeks to days
					secondNum = secondNum * NUMBER_OF_DAYS;
					days = secondNum;
				}
			}
		}
		return new Criteria(metric, requiredAmt, days);
	}

	/**
	 * Checks to see if the string contains only letters
	 * 
	 * @param value
	 * @return
	 */
	public static boolean hasOnlyLetters(String value) {
		return value.matches(REGEX_LETTERS);
	}

	/**
	 * Checks to see if the string contains only numbers
	 * 
	 * @param value
	 * @return
	 */
	public static boolean hasOnlyNumbers(String value) {
		return value.matches(REGEX_NUMBERS);
	}

	/**
	 * checks to see if the string contains both letters and numbers
	 * 
	 * @param value
	 * @return
	 */
	public static boolean hasAlphaNumericCharacters(String value) {
		return value.matches(REGEX_ALPHANUMERIC);
	}

	/**
	 * parses a string to an Integer
	 * 
	 * @param parseThis
	 * @return
	 */
	private static Double parseDouble(String parseThis) {
		String parsed = parseThis.replaceAll("[^\\d\\.]", "");
		return Double.parseDouble(parsed);
	}

	/**
	 * parses a string to an Integer
	 * 
	 * @param parseThis
	 * @return
	 */
	private static Integer parseInt(String parseThis) {
		String parsed = parseThis.replaceAll("[\\D]", "");
		return Integer.parseInt(parsed);
	}


	/**
	 * getter for property
	 * 
	 * @return
	 */
	public Property getProperty() {
		return property;
	}

	/**
	 * setter for property
	 * 
	 * @param property
	 */
	public void setProperty(Property property) {
		this.property = property;
	}

	/**
	 * getter for settings
	 * 
	 * @return
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * setter for settings
	 * 
	 * @param settings
	 */
	public void setSettings(Settings settings) {
		this.settings = settings;
	}

}