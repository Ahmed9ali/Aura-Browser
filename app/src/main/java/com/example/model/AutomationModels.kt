package com.example.model

import java.util.UUID
import kotlin.random.Random

enum class AutomationActionType {
    CLICK,
    INPUT,
    CHECKBOX,
    RADIO,
    SELECT_DROPDOWN,
    SELECT_COUNTRY_CODE,
    SELECT_DATE,
    UPLOAD_FILE,
    WAIT,
    SUBMIT,
    CLEAR,
    SCROLL,
    NAVIGATE
}

enum class AutomationStatus {
    IDLE,
    RECORDING,
    PLAYING,
    PAUSED,
    SCANNING
}

enum class HudMode {
    RUN_AUTOMATION,
    CREATE_AUTOMATION
}

enum class DataIterationOrder(val label: String, val shortLabel: String) {
    TOP_TO_BOTTOM("Top to Bottom (1 → N)", "Top → Bottom"),
    BOTTOM_TO_TOP("Bottom to Top (N → 1 / Vice Versa)", "Bottom → Top"),
    RANDOM("Randomly (1 by 1 Shuffle)", "Random")
}

enum class DataExhaustionPolicy(val label: String, val shortLabel: String) {
    ADD_NEW("Ask: Stop / Default / Add New", "Ask / Add New"),
    CONTINUE_WITH_DEFAULT("Continue Using Default (Random)", "Continue Default"),
    STOP("Stop Automation When Finished", "Stop"),
    LOOP("Loop Stored Data from Start", "Loop")
}

enum class ElementSemanticType {
    GENERIC_INPUT,
    EMAIL,
    USERNAME,
    PASSWORD,
    CONFIRM_PASSWORD,
    FIRST_NAME,
    LAST_NAME,
    FULL_NAME,
    PHONE,
    COUNTRY_CODE,
    GENDER,
    DOB,
    DOB_DAY,
    DOB_MONTH,
    DOB_YEAR,
    ADDRESS,
    ADDRESS_LINE_2,
    CITY,
    STATE,
    ZIP_PIN,
    FILE_UPLOAD,
    CHECKBOX,
    RADIO_OPTION,
    CHECKBOX_TERMS,
    SUBMIT_BUTTON,
    NEXT_BUTTON,
    GENERIC_BUTTON,
    DROPDOWN
}

/**
 * User-created Data Sheet (e.g. named "XYZ Data").
 * Stores sections/fields like 'email', 'username', 'password', 'phoneNumber', etc.,
 * each containing 10s of values entered manually, pasted in bulk, or uploaded from file.
 */
data class DataSheet(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "XYZ Data",
    val fields: Map<String, List<String>> = emptyMap(),
    val iterationOrder: DataIterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
    val exhaustionPolicy: DataExhaustionPolicy = DataExhaustionPolicy.ADD_NEW,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFieldValues(key: String): List<String> {
        val normalizedKey = key.trim()
        // 1. Direct exact or case-insensitive match
        val directExact = fields[normalizedKey]?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        if (directExact.isNotEmpty()) return directExact

        val directIgnoreCase = fields.entries
            .firstOrNull { it.key.trim().equals(normalizedKey, ignoreCase = true) }
            ?.value
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        if (directIgnoreCase.isNotEmpty()) return directIgnoreCase

        // 2. Birthday / Date of Birth alias resolution & split component derivation
        val dobAliases = setOf(
            "dob", "birthday", "birthdate", "dateofbirth", "date_of_birth",
            "birth_date", "birth", "bday", "date of birth", "birth date"
        )
        val lowerKey = normalizedKey.lowercase()
        val isFullDobKey = lowerKey in dobAliases
        val isSplitDobKey = lowerKey == "dobday" || lowerKey == "dobmonth" || lowerKey == "dobyear" ||
            lowerKey == "birth_day" || lowerKey == "birth_month" || lowerKey == "birth_year" ||
            lowerKey == "bday_day" || lowerKey == "bday_month" || lowerKey == "bday_year"

        if (isFullDobKey || isSplitDobKey) {
            val rawDobList = fields.entries
                .firstOrNull { (k, v) ->
                    k.trim().lowercase() in dobAliases && v.any { it.isNotBlank() }
                }
                ?.value
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            if (rawDobList.isNotEmpty()) {
                if (isFullDobKey) {
                    return rawDobList
                }
                return rawDobList.map { rawDob ->
                    val parsed = AutomationDataGenerator.parseFlexibleDob(rawDob)
                    when {
                        lowerKey.endsWith("year") -> parsed.first
                        lowerKey.endsWith("month") -> parsed.second
                        else -> parsed.third
                    }
                }
            }

            // Also support reassembling full 'dob' if the user entered separate dobYear / dobMonth / dobDay fields
            if (isFullDobKey) {
                val days = getRawByKeys("dobday", "day", "birth_day", "bday_day")
                val months = getRawByKeys("dobmonth", "month", "birth_month", "bday_month")
                val years = getRawByKeys("dobyear", "year", "birth_year", "bday_year")
                val maxLen = maxOf(days.size, months.size, years.size)
                if (maxLen > 0) {
                    return (0 until maxLen).map { i ->
                        val d = days.getOrNull(i) ?: days.lastOrNull() ?: "04"
                        val m = months.getOrNull(i) ?: months.lastOrNull() ?: "05"
                        val y = years.getOrNull(i) ?: years.lastOrNull() ?: "2001"
                        val (py, pm, pd) = AutomationDataGenerator.parseFlexibleDob("$d $m $y")
                        "$py-$pm-$pd"
                    }
                }
            }
        }

        // 3. Automatically derive countryCode from phoneNumber column if phoneNumber entries include a leading +code
        if (key.equals("countryCode", ignoreCase = true)) {
            val phoneList = (fields["phoneNumber"] ?: fields["phone"])
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
            val extractedCodes = phoneList.mapNotNull { rawPhone ->
                val match = Regex("^\\+(\\d{1,4})\\b").find(rawPhone)
                match?.value
            }
            if (extractedCodes.isNotEmpty()) return extractedCodes
        }

        return emptyList()
    }

    private fun getRawByKeys(vararg candidateKeys: String): List<String> {
        val lowerSet = candidateKeys.map { it.lowercase() }.toSet()
        return fields.entries
            .firstOrNull { (k, v) -> k.trim().lowercase() in lowerSet && v.any { it.isNotBlank() } }
            ?.value
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    fun totalRows(): Int {
        return fields.values.maxOfOrNull { list -> list.count { it.isNotBlank() } } ?: 0
    }

    fun withAddedFieldValues(key: String, newValues: List<String>): DataSheet {
        val cleanNew = newValues.map { it.trim() }.filter { it.isNotBlank() }
        val existing = getFieldValues(key)
        val updatedMap = fields.toMutableMap().apply {
            put(key, existing + cleanNew)
        }
        return copy(fields = updatedMap)
    }

    /**
     * Resolves the value for a given field key at usageIndex (0-based).
     * If exhausted or missing, honors exhaustionPolicy or calls fallback.
     */
    fun getValueForField(
        key: String,
        iterationIndex: Int,
        orderOverride: DataIterationOrder = iterationOrder,
        policyOverride: DataExhaustionPolicy = exhaustionPolicy,
        shuffledOrder: List<Int>? = null,
        fallback: () -> String
    ): String? {
        val list = getFieldValues(key)
        if (list.isEmpty()) {
            return fallback()
        }
        val count = list.size

        if (iterationIndex >= count) {
            return when (policyOverride) {
                DataExhaustionPolicy.STOP, DataExhaustionPolicy.ADD_NEW -> null
                DataExhaustionPolicy.CONTINUE_WITH_DEFAULT -> fallback()
                DataExhaustionPolicy.LOOP -> {
                    val loopIdx = iterationIndex % count
                    when (orderOverride) {
                        DataIterationOrder.TOP_TO_BOTTOM -> list[loopIdx]
                        DataIterationOrder.BOTTOM_TO_TOP -> list[(count - 1) - loopIdx]
                        DataIterationOrder.RANDOM -> {
                            val idx = shuffledOrder?.getOrNull(loopIdx) ?: Random.nextInt(count)
                            list[idx.coerceIn(0, count - 1)]
                        }
                    }
                }
            }
        }

        return when (orderOverride) {
            DataIterationOrder.TOP_TO_BOTTOM -> list[iterationIndex]
            DataIterationOrder.BOTTOM_TO_TOP -> list[(count - 1) - iterationIndex]
            DataIterationOrder.RANDOM -> {
                val idx = shuffledOrder?.getOrNull(iterationIndex) ?: Random.nextInt(count)
                list[idx.coerceIn(0, count - 1)]
            }
        }
    }
}

/**
 * Real-world multi-field dataset record.
 */
data class AutomationDataRecord(
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val countryCode: String = "+1",
    val phoneNumber: String = "",
    val gender: String = "Male",
    val dob: String = "1998-05-14",
    val dobDay: String = "14",
    val dobMonth: String = "05",
    val dobYear: String = "1998",
    val address: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val fileName: String = "id_document.pdf",
    val customFields: Map<String, String> = emptyMap()
)

data class AutomationStep(
    val id: String = UUID.randomUUID().toString(),
    val actionType: AutomationActionType = AutomationActionType.CLICK,
    val selector: String = "",
    val elementDescription: String = "",
    val value: String = "",
    val variableKey: String? = null,
    val pageUrl: String = "",
    val delayMs: Long = 800L,
    val clearBeforeInput: Boolean = true,
    val isCompleted: Boolean = false
)

data class AutomationScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Comprehensive Web Workflow Automation",
    val targetWebsite: String = "*",
    val targetUrl: String = "",
    val steps: List<AutomationStep> = emptyList(),
    val dataSheetId: String? = null,
    val repeatCount: Int = 4,
    val iterationOrder: DataIterationOrder = DataIterationOrder.TOP_TO_BOTTOM,
    val exhaustionPolicy: DataExhaustionPolicy = DataExhaustionPolicy.ADD_NEW,
    val humanizeDelays: Boolean = true,
    val rotateProxyOnRepeat: Boolean = false,
    val clearDataOnComplete: Boolean = true,
    val datasetRecords: List<AutomationDataRecord> = emptyList(),
    val datasetRawText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ScannedElement(
    val id: String = UUID.randomUUID().toString(),
    val tagName: String,
    val type: String = "",
    val nameAttr: String = "",
    val idAttr: String = "",
    val placeholder: String = "",
    val text: String = "",
    val selector: String,
    val pageUrl: String = "",
    val isInput: Boolean = false,
    val isButton: Boolean = false,
    val semanticType: ElementSemanticType = ElementSemanticType.GENERIC_INPUT,
    val detectedOptions: List<String> = emptyList()
)

data class AutomationExecutionState(
    val status: AutomationStatus = AutomationStatus.IDLE,
    val hudMode: HudMode = HudMode.RUN_AUTOMATION,
    val currentIteration: Int = 1,
    val totalIterations: Int = 1,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val currentStepDescription: String = "",
    val currentDataSummary: String = "",
    val logMessages: List<String> = emptyList(),
    val isHudVisible: Boolean = false,
    val isWaitingForPageLoad: Boolean = false,
    val pageLoadProgress: Int = 100,
    val isAwaitingExhaustionChoice: Boolean = false,
    val exhaustedFieldKey: String? = null,
    val exhaustedSheetId: String? = null,
    val exhaustedSheetName: String = "",
    val exhaustedUsedCount: Int = 0
)

/**
 * Synthetic generator for the Default Data Sheet.
 * Generates brand-new, unique random values on every step/iteration.
 */
object AutomationDataGenerator {
    private val firstNames = listOf(
        "Alex", "Jordan", "Taylor", "Morgan", "Sam", "Chris", "Casey", "Riley",
        "Avery", "Jamie", "Logan", "Harper", "Quinn", "Rowan", "Kai", "Devon",
        "Liam", "Noah", "Emma", "Olivia", "Ava", "Sophia", "Lucas", "Ethan"
    )

    private val lastNames = listOf(
        "Vance", "Mercer", "Sterling", "Kovacs", "Reyes", "Sinclair", "Holloway",
        "Chen", "Dubois", "Nakamura", "Al-Mansoor", "Lindqvist", "O'Connor", "Novak",
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller"
    )

    private val domains = listOf(
        "aura-mail.io", "protonmail.com", "gmail.com", "outlook.com",
        "secure-web.org", "fastmail.fm", "icloud.com"
    )

    private val countryCodes = listOf("+1", "+44", "+49", "+33", "+61", "+81", "+91", "+34", "+39")

    private val cities = listOf(
        Pair("San Francisco", "CA"),
        Pair("Austin", "TX"),
        Pair("Seattle", "WA"),
        Pair("Chicago", "IL"),
        Pair("Miami", "FL"),
        Pair("Denver", "CO"),
        Pair("Boston", "MA"),
        Pair("Toronto", "ON"),
        Pair("London", "ENG"),
        Pair("Sydney", "NSW"),
        Pair("Berlin", "BE"),
        Pair("Mumbai", "MH"),
        Pair("Tokyo", "TYO")
    )

    private val streets = listOf(
        "Market Street", "Broadway", "Main Street", "Maple Avenue", "Oak Ridge Way",
        "Park Boulevard", "Sunset Boulevard", "Elm Street", "Highland Drive", "Pine Street"
    )

    private val files = listOf("id_document.pdf", "profile_avatar.png", "resume_cv.pdf", "utility_bill.pdf", "passport_scan.jpg")

    fun generateSingleRandomRecord(): AutomationDataRecord {
        val fName = firstNames.random()
        val lName = lastNames.random()
        val domain = domains.random()
        val randNum = Random.nextInt(100, 9999)
        val username = "${fName.lowercase()}_${lName.lowercase()}$randNum"
        val email = "${fName.lowercase()}.${lName.lowercase()}$randNum@$domain"
        val pass = "Aura#" + Random.nextInt(1000, 9999) + "!P"
        val countryCode = countryCodes.random()
        val phone = "${Random.nextInt(200, 899)}${Random.nextInt(100, 899)}${Random.nextInt(1000, 9999)}"
        val gender = listOf("Male", "Female", "Prefer not to say").random()
        val birthYear = Random.nextInt(1982, 2004)
        val birthMonthNum = Random.nextInt(1, 13)
        val birthMonth = if (birthMonthNum < 10) "0$birthMonthNum" else "$birthMonthNum"
        val birthDayNum = Random.nextInt(1, 29)
        val birthDay = if (birthDayNum < 10) "0$birthDayNum" else "$birthDayNum"
        val dob = "$birthYear-$birthMonth-$birthDay"
        val cityPair = cities.random()
        val street = streets.random()
        val address = "${Random.nextInt(100, 9990)} $street"
        val zip = "${Random.nextInt(10000, 99999)}"
        val fileName = files.random()

        return AutomationDataRecord(
            email = email,
            username = username,
            password = pass,
            confirmPassword = pass,
            firstName = fName,
            lastName = lName,
            fullName = "$fName $lName",
            countryCode = countryCode,
            phoneNumber = phone,
            gender = gender,
            dob = dob,
            dobDay = birthDay,
            dobMonth = birthMonth,
            dobYear = birthYear.toString(),
            address = address,
            addressLine2 = if (Random.nextBoolean()) "Suite ${Random.nextInt(100, 999)}" else "",
            city = cityPair.first,
            state = cityPair.second,
            zipCode = zip,
            fileName = fileName,
            customFields = mapOf(
                "username" to username,
                "zipCode" to zip,
                "phone" to phone,
                "pinCode" to zip
            )
        )
    }

    fun generateSingleFieldValue(fieldKey: String): String {
        val randRec = generateSingleRandomRecord()
        return when (fieldKey.lowercase()) {
            "email" -> randRec.email
            "username", "user", "login" -> randRec.username
            "password", "pass" -> randRec.password
            "confirmpassword" -> randRec.password
            "firstname" -> randRec.firstName
            "lastname" -> randRec.lastName
            "fullname", "name" -> randRec.fullName
            "phone", "phonenumber", "mobile" -> randRec.phoneNumber
            "countrycode" -> randRec.countryCode
            "gender", "sex" -> randRec.gender
            "dob", "birthdate", "birthday", "dateofbirth", "bday" -> randRec.dob
            "dobday" -> randRec.dobDay
            "dobmonth" -> randRec.dobMonth
            "dobyear" -> randRec.dobYear
            "address" -> randRec.address
            "addressline2" -> randRec.addressLine2
            "city" -> randRec.city
            "state" -> randRec.state
            "zipcode", "zip", "pin", "pincode" -> randRec.zipCode
            "filename", "file" -> randRec.fileName
            else -> "auto_${fieldKey}_${Random.nextInt(1000, 9999)}"
        }
    }

    /**
     * Parses any human or machine birthday format (e.g., "04 May 2001", "4th May 2001",
     * "May 04, 2001", "2001-05-04", "04/05/2001", "04-05-2001", "04.05.2001", "04May2001")
     * into a normalized numeric (year4Digit, month2Digit, day2Digit) Triple.
     */
    fun parseFlexibleDob(
        rawDob: String,
        fallbackYear: String = "2001",
        fallbackMonth: String = "05",
        fallbackDay: String = "04"
    ): Triple<String, String, String> {
        val monthNames = mapOf(
            "jan" to "01", "january" to "01",
            "feb" to "02", "february" to "02",
            "mar" to "03", "march" to "03",
            "apr" to "04", "april" to "04",
            "may" to "05",
            "jun" to "06", "june" to "06",
            "jul" to "07", "july" to "07",
            "aug" to "08", "august" to "08",
            "sep" to "09", "sept" to "09", "september" to "09",
            "oct" to "10", "october" to "10",
            "nov" to "11", "november" to "11",
            "dec" to "12", "december" to "12"
        )

        // Pre-normalize compact strings like "04May2001" or "4th May, 2001"
        val spaced = rawDob.trim()
            .replace(Regex("(\\d)(st|nd|rd|th)\\b", RegexOption.IGNORE_CASE), "$1")
            .replace(Regex("(\\d)([A-Za-z])"), "$1 $2")
            .replace(Regex("([A-Za-z])(\\d)"), "$1 $2")

        val tokens = spaced.split(Regex("[-/.,\\s]+")).map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.size >= 3) {
            val namedMonthIdx = tokens.indexOfFirst { token ->
                val clean = token.lowercase().take(3)
                monthNames.containsKey(token.lowercase()) || monthNames.containsKey(clean)
            }
            if (namedMonthIdx >= 0) {
                val rawMonthToken = tokens[namedMonthIdx].lowercase()
                val m = monthNames[rawMonthToken] ?: monthNames[rawMonthToken.take(3)] ?: fallbackMonth
                val remaining = tokens.filterIndexed { idx, _ -> idx != namedMonthIdx }
                val yToken = remaining.firstOrNull { it.length == 4 && it.all(Char::isDigit) }
                    ?: remaining.lastOrNull { it.any(Char::isDigit) }
                    ?: fallbackYear
                val dRaw = remaining.firstOrNull { it != yToken && it.any(Char::isDigit) } ?: fallbackDay
                val dDigits = dRaw.filter(Char::isDigit).ifBlank { fallbackDay }
                val yDigits = yToken.filter(Char::isDigit).let {
                    when (it.length) {
                        4 -> it
                        2 -> if ((it.toIntOrNull() ?: 0) <= 30) "20$it" else "19$it"
                        else -> fallbackYear
                    }
                }
                return Triple(yDigits, m.padStart(2, '0'), dDigits.padStart(2, '0'))
            }

            val numericTokens = tokens.map { it.filter(Char::isDigit) }.filter { it.isNotBlank() }
            if (numericTokens.size >= 3) {
                return when {
                    numericTokens[0].length == 4 -> {
                        val y = numericTokens[0]
                        val second = numericTokens[1].toIntOrNull() ?: 1
                        val third = numericTokens[2].toIntOrNull() ?: 1
                        if (second > 12 && third <= 12) {
                            Triple(y, numericTokens[2].padStart(2, '0'), numericTokens[1].padStart(2, '0'))
                        } else {
                            Triple(y, numericTokens[1].padStart(2, '0'), numericTokens[2].padStart(2, '0'))
                        }
                    }
                    numericTokens[2].length == 4 -> {
                        val y = numericTokens[2]
                        val firstNum = numericTokens[0].toIntOrNull() ?: 1
                        if (firstNum > 12) {
                            Triple(y, numericTokens[1].padStart(2, '0'), numericTokens[0].padStart(2, '0'))
                        } else {
                            Triple(y, numericTokens[0].padStart(2, '0'), numericTokens[1].padStart(2, '0'))
                        }
                    }
                    else -> {
                        val rawY = numericTokens[2]
                        val y = if (rawY.length == 2) {
                            if ((rawY.toIntOrNull() ?: 0) <= 30) "20$rawY" else "19$rawY"
                        } else fallbackYear
                        Triple(
                            y,
                            numericTokens[1].padStart(2, '0'),
                            numericTokens[0].padStart(2, '0')
                        )
                    }
                }
            }
        }
        return Triple(fallbackYear, fallbackMonth.padStart(2, '0'), fallbackDay.padStart(2, '0'))
    }
}
