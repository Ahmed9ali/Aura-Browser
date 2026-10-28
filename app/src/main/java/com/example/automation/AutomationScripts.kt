package com.example.automation

object AutomationScripts {

    /**
     * JavaScript injected to record user interactions on the page in NO-TYPE Tap-to-Grab mode.
     * - User does NOT type into input fields.
     * - Prevents the Android soft keyboard from appearing when tapping inputs (`inputmode="none"` + instant blur).
     * - Detects custom dropdown triggers, bottom-sheet triggers, country code selectors, and DOB pickers
     *   in addition to standard inputs, selects, checkboxes, radios, and buttons.
     */
    val RECORDING_INJECTION = """
        (function() {
            window._auraPlaybackActive = false;
            window._auraRecorderActive = true;
            if (window._auraRecorderInstalled) return;
            window._auraRecorderInstalled = true;
            window._auraLastCapturedSelector = '';
            window._auraLastCapturedTime = 0;
            window._auraLastCapturedAction = '';

            function cssEscape(str) {
                if (window.CSS && window.CSS.escape) return window.CSS.escape(str);
                return String(str).replace(/([ !"#$%&'()*+,.\/:;<=>?@\[\\\]^`{|}~])/g, '\\$1');
            }

            function isUniqueSelector(sel, targetEl) {
                try {
                    var matches = document.querySelectorAll(sel);
                    return matches.length === 1 && matches[0] === targetEl;
                } catch (e) {
                    return false;
                }
            }

            function getCssSelector(el) {
                if (!el || el === document || el.nodeType !== 1) return '';
                var tag = el.tagName.toLowerCase();

                if (el.id) {
                    var idSel = '#' + cssEscape(el.id);
                    if (isUniqueSelector(idSel, el)) return idSel;
                }

                var testAttrs = ['data-testid', 'data-test', 'data-qa', 'data-cy', 'data-automation-id'];
                for (var t = 0; t < testAttrs.length; t++) {
                    var attr = testAttrs[t];
                    var val = el.getAttribute(attr);
                    if (val) {
                        var testSel = '[' + attr + '="' + val.replace(/"/g, '\\"') + '"]';
                        if (isUniqueSelector(testSel, el)) return testSel;
                    }
                }

                if (el.name) {
                    var nameSel = tag + '[name="' + el.name.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(nameSel, el)) return nameSel;
                    if (el.value && (el.type === 'radio' || el.type === 'checkbox')) {
                        var nameValSel = nameSel + '[value="' + el.value.replace(/"/g, '\\"') + '"]';
                        if (isUniqueSelector(nameValSel, el)) return nameValSel;
                    }
                }

                var ariaLabel = el.getAttribute('aria-label');
                if (ariaLabel) {
                    var ariaSel = tag + '[aria-label="' + ariaLabel.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(ariaSel, el)) return ariaSel;
                }

                if (el.placeholder) {
                    var phSel = tag + '[placeholder="' + el.placeholder.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(phSel, el)) return phSel;
                }

                if (el.type && el.autocomplete) {
                    var acSel = tag + '[type="' + el.type + '"][autocomplete="' + el.autocomplete.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(acSel, el)) return acSel;
                }

                var path = [];
                var curr = el;
                while (curr && curr.nodeType === 1 && curr !== document.body && curr !== document.documentElement) {
                    var seg = curr.tagName.toLowerCase();
                    if (curr.id) {
                        var candidateId = '#' + cssEscape(curr.id);
                        path.unshift(candidateId);
                        var candidateFull = path.join(' > ');
                        if (isUniqueSelector(candidateFull, el)) return candidateFull;
                    } else {
                        var parent = curr.parentElement;
                        if (parent) {
                            var sameTagSiblings = [];
                            for (var i = 0; i < parent.children.length; i++) {
                                if (parent.children[i].tagName === curr.tagName) {
                                    sameTagSiblings.push(parent.children[i]);
                                }
                            }
                            if (sameTagSiblings.length > 1) {
                                var idx = sameTagSiblings.indexOf(curr) + 1;
                                seg += ':nth-of-type(' + idx + ')';
                            }
                        }
                        path.unshift(seg);
                        var partial = path.join(' > ');
                        if (isUniqueSelector(partial, el)) return partial;
                    }
                    curr = curr.parentElement;
                }
                return path.join(' > ');
            }

            function getAssociatedLabelText(el) {
                if (!el) return '';
                var parts = [];
                try {
                    if (el.labels && el.labels.length > 0) {
                        for (var i = 0; i < el.labels.length; i++) {
                            parts.push(el.labels[i].innerText || el.labels[i].textContent || '');
                        }
                    }
                    if (el.id) {
                        var forLbl = document.querySelector('label[for="' + el.id.replace(/"/g, '\\"') + '"]');
                        if (forLbl) parts.push(forLbl.innerText || forLbl.textContent || '');
                    }
                    var closestLbl = el.closest ? el.closest('label') : null;
                    if (closestLbl) parts.push(closestLbl.innerText || closestLbl.textContent || '');
                    var ariaLabelledBy = el.getAttribute('aria-labelledby');
                    if (ariaLabelledBy) {
                        var ids = ariaLabelledBy.split(/\s+/);
                        for (var j = 0; j < ids.length; j++) {
                            var refEl = document.getElementById(ids[j]);
                            if (refEl) parts.push(refEl.innerText || refEl.textContent || '');
                        }
                    }
                    var ariaDescribedBy = el.getAttribute('aria-describedby');
                    if (ariaDescribedBy) {
                        var dIds = ariaDescribedBy.split(/\s+/);
                        for (var k = 0; k < dIds.length; k++) {
                            var dEl = document.getElementById(dIds[k]);
                            if (dEl) parts.push(dEl.innerText || dEl.textContent || '');
                        }
                    }
                    var prev = el.previousElementSibling;
                    if (prev && (prev.tagName === 'LABEL' || prev.tagName === 'SPAN' || prev.tagName === 'DIV')) {
                        var prevTxt = (prev.innerText || prev.textContent || '').trim();
                        if (prevTxt.length < 60) parts.push(prevTxt);
                    }
                    var fieldContainer = el.closest ? el.closest('.form-group, .field, .input-group, .form-field, .MuiFormControl-root, .ant-form-item, fieldset') : null;
                    if (fieldContainer) {
                        var containerLbl = fieldContainer.querySelector('label, legend, .label, .form-label, .MuiFormLabel-root');
                        if (containerLbl) parts.push(containerLbl.innerText || containerLbl.textContent || '');
                    }
                } catch (e) {}
                return parts.join(' ').replace(/\s+/g, ' ').trim();
            }

            function isCountryCodeElement(el, combined) {
                if (!el) return false;
                var tag = (el.tagName || '').toLowerCase();
                // CRITICAL: An editable text/tel input is the PHONE NUMBER field, never a country code trigger!
                if (tag === 'input' && !el.readOnly && el.type !== 'button' && el.type !== 'submit') return false;
                var cls = (el.className && typeof el.className === 'string') ? el.className : '';
                if (/callingCode|calling-code|countryDropDown|country-code|countryCode|dial-code|dialCode|phone-country|phoneprefix|iti__|PhoneInputCountry|selected-flag|flag-dropdown/i.test(cls)) return true;
                if (el.hasAttribute('data-dial-code') || el.hasAttribute('data-country-code')) return true;
                var idAndName = ((el.id || '') + ' ' + (el.name || '')).toLowerCase();
                if (/countrylist|country_code|country-code|countrycode|dialcode|dial_code|phone_country|calling_code|callingcode|phoneprefix/i.test(idAndName)) return true;
                if (/country.*code|dial.*code|calling.*code|phone.*prefix|phone.*code|phone.*country/i.test(combined)) return true;
                var txt = (el.innerText || el.textContent || el.value || '').trim();
                // Match e.g. +213(Algeria), +1 (US), +44, etc.
                if (txt.length <= 25 && /^\s*([^\w\s]{1,4}\s*)?([a-zA-Z]{2}\s*)?\+\d{1,4}/.test(txt)) return true;
                var tag = (el.tagName || '').toLowerCase();
                if (tag === 'select' && el.options && el.options.length > 3) {
                    var sampleOpts = '';
                    for (var i = 0; i < Math.min(el.options.length, 8); i++) {
                        sampleOpts += ' ' + (el.options[i].text || '') + ' ' + (el.options[i].value || '');
                    }
                    if ((sampleOpts.match(/\+\d{1,4}\b/g) || []).length >= 2) return true;
                }
                // Check if this trigger sits inside a callingCode / phone container next to a phone input
                var phoneContainer = el.closest ? el.closest('.hwid-callingCodePhone-root, [class*="callingCode"], [class*="calling-code"], [class*="countryDropDown"], .iti, .PhoneInput, .react-tel-input, .phone-input, .input-group, .field, .form-group, div') : null;
                if (phoneContainer && phoneContainer !== document.body && tag !== 'input') {
                    var telInp = phoneContainer.querySelector('input');
                    if (telInp && telInp !== el && phoneContainer.getBoundingClientRect().height < 120) {
                        if (!/submit|next|continue|send|verify|code|otp/i.test(txt)) return true;
                    }
                }
                return false;
            }

            function isCustomDropdownOrSheetTrigger(el, combined) {
                if (!el || !el.tagName) return false;
                var tag = el.tagName.toLowerCase();
                if (tag === 'select') return true;
                var role = (el.getAttribute('role') || '').toLowerCase();
                if (role === 'combobox' || role === 'listbox' || role === 'spinbutton') return true;
                var hasPopup = (el.getAttribute('aria-haspopup') || '').toLowerCase();
                if (hasPopup === 'listbox' || hasPopup === 'dialog' || hasPopup === 'menu' || hasPopup === 'true') return true;
                if (el.hasAttribute('aria-expanded')) return true;
                var cls = (el.className && typeof el.className === 'string') ? el.className : '';
                if (/MuiSelect-select|ant-select-selector|react-select__control|v-select|el-select|select-trigger|dropdown-toggle|custom-select|picker-trigger|bottom-sheet-trigger|hwid-countryDropDown|hwid-select|hwid-drop/i.test(cls)) {
                    return true;
                }
                if (tag === 'input' && el.readOnly && /select|choose|pick|country|gender|state|month|day|year|category/i.test(combined)) {
                    return true;
                }
                var txt = (el.innerText || el.textContent || el.placeholder || '').trim();
                if ((tag === 'button' || role === 'button' || tag === 'div' || tag === 'span') &&
                    (/^(select|choose|pick)\b/i.test(txt) || /\b(gender|sex|country|state|province|month|day|year)\b/i.test(combined)) &&
                    !/submit|sign.*up|register|login|log.*in|continue|next/i.test(txt)) {
                    return true;
                }
                return false;
            }

            function detectFieldKey(el) {
                if (!el) return '';
                var tag = el.tagName.toLowerCase();
                var type = (el.type || '').toLowerCase();
                var ac = (el.autocomplete || '').toLowerCase();
                var labelTxt = getAssociatedLabelText(el);
                var name = (el.name || '').toLowerCase();
                var id = (el.id || '').toLowerCase();
                var placeholder = (el.placeholder || '').toLowerCase();
                var ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                var text = (el.innerText || el.textContent || '').trim().toLowerCase();
                var combined = (
                    name + ' ' +
                    id + ' ' +
                    placeholder + ' ' +
                    ariaLabel + ' ' +
                    (el.getAttribute('data-label') || '') + ' ' +
                    ac + ' ' +
                    labelTxt + ' ' +
                    (text.length < 40 ? text : '')
                ).toLowerCase();

                // 1. Password check
                if (type === 'password' || ac === 'new-password' || ac === 'current-password' || /pass|pwd|secret/i.test(combined)) {
                    if (/confirm|repeat|retype|verify|match|again|pass.*2/i.test(combined)) return 'confirmPassword';
                    return 'password';
                }

                // 2. Phone input check: If editable input with phone placeholder or inside callingCodePhone container
                if (tag === 'input' && !el.readOnly && el.type !== 'button' && el.type !== 'submit') {
                    var parentPhoneCont = el.closest ? el.closest('.hwid-callingCodePhone-root, [class*="callingCode"], .iti, .PhoneInput, .react-tel-input, .phone-input') : null;
                    if (type === 'tel' || /phone|mobile|cell|telephone|whatsapp|contact.*num/i.test(placeholder + ' ' + name + ' ' + id) || parentPhoneCont) {
                        return 'phoneNumber';
                    }
                }

                // 3. Country code trigger
                if (isCountryCodeElement(el, combined)) return 'countryCode';

                // 4. File upload
                if (type === 'file' || /upload|resume|document|avatar|attachment|file|cv/i.test(combined)) return 'fileName';

                // 5. Email & Username
                if (type === 'email' || ac === 'email' || /email|e-mail|mail/i.test(combined)) return 'email';
                if (ac === 'username' || /user|username|login|handle|nick|account/i.test(combined)) return 'username';

                // 6. Comprehensive Date of Birth detection (Day, Month, Year, and full DOB)
                // Covers native inputs/selects, Google split inputs, and custom segmented pickers like Huawei ID (28 ▼, 10 ▼, 2006 ▼)
                var fieldSpecificHint = (name + ' ' + id + ' ' + placeholder + ' ' + ariaLabel + ' ' + (text.length < 25 ? text : '') + ' ' + (el.className || '')).toLowerCase();
                var dobContainer = el.closest ? el.closest('[class*="birth"], [class*="dob"], [class*="bday"], [class*="date-root"], [class*="date-wrapper"], .hwid-register-inputsContainer, .form-group, .field, .input-group, div') : null;
                var dobContextText = '';
                if (dobContainer) {
                    var rawCTxt = (dobContainer.innerText || dobContainer.textContent || '').trim();
                    if (rawCTxt.length < 250) dobContextText = rawCTxt.toLowerCase();
                }
                var isDobContext = /date\s*of\s*birth|birth\s*date|birthday|\bdob\b|hwid.*birth|hwid.*date/i.test(dobContextText + ' ' + combined);

                if (ac === 'bday-day' || /^(day|dd|birth_day|dob_day|bday_day|birthday_day|date_day)$/i.test(name) || /^(day|dd|birth_day|dob_day|bday-day)$/i.test(id) || /\b(birth.*day|dob.*day|select.*day|^day$|^dd$)\b/i.test(fieldSpecificHint) || (isDobContext && /\b(day|dd)\b/i.test(fieldSpecificHint) && !/today|everyday/i.test(fieldSpecificHint))) {
                    return 'dobDay';
                }
                if (ac === 'bday-month' || /^(month|mm|birth_month|dob_month|bday_month|birthday_month|date_month)$/i.test(name) || /^(month|mm|birth_month|dob_month|bday-month)$/i.test(id) || /\b(birth.*month|dob.*month|select.*month|^month$|^mm$)\b/i.test(fieldSpecificHint) || (isDobContext && /\b(month|mm)\b/i.test(fieldSpecificHint))) {
                    return 'dobMonth';
                }
                if (ac === 'bday-year' || /^(year|yyyy|yy|birth_year|dob_year|bday_year|birthday_year|date_year)$/i.test(name) || /^(year|yyyy|birth_year|dob_year|bday-year)$/i.test(id) || /\b(birth.*year|dob.*year|select.*year|^year$|^yyyy$)\b/i.test(fieldSpecificHint) || (isDobContext && /\b(year|yyyy|yr)\b/i.test(fieldSpecificHint))) {
                    return 'dobYear';
                }

                if (isDobContext) {
                    var cleanNumericText = text.replace(/[^0-9]/g, '').trim();
                    if (/^(19\d\d|20\d\d)$/.test(cleanNumericText)) return 'dobYear';
                    if (/^(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)$/i.test(text)) return 'dobMonth';

                    // Check segmented position among sibling triggers in the DOB container
                    if (dobContainer) {
                        var allTriggers = dobContainer.querySelectorAll('[class*="select"], [class*="drop"], [class*="picker"], [class*="item"], [class*="box"], button, [role="combobox"], [role="button"], [role="spinbutton"], input, select');
                        var leafTriggers = [];
                        for (var lt = 0; lt < allTriggers.length; lt++) {
                            var sibT = allTriggers[lt];
                            if (sibT.querySelector && sibT.querySelector('button, [class*="select"], [class*="drop"], input, select')) continue;
                            if (leafTriggers.indexOf(sibT) < 0 && (sibT === el || sibT.getBoundingClientRect().width > 10)) {
                                leafTriggers.push(sibT);
                            }
                        }
                        if (leafTriggers.length >= 3) {
                            var myIndex = leafTriggers.indexOf(el);
                            if (myIndex < 0) {
                                for (var k = 0; k < leafTriggers.length; k++) {
                                    if (leafTriggers[k].contains(el)) { myIndex = k; break; }
                                }
                            }
                            if (myIndex >= 0) {
                                var lastIsYear = leafTriggers.some(function(t, idx) { return idx === 2 && /19\d\d|20\d\d|year/i.test(t.innerText || ''); });
                                var firstIsYear = leafTriggers.some(function(t, idx) { return idx === 0 && /19\d\d|20\d\d|year/i.test(t.innerText || ''); });
                                if (lastIsYear || (!firstIsYear && (myIndex === 0 || myIndex === 1 || myIndex === 2))) {
                                    if (myIndex === 0) return 'dobDay';
                                    if (myIndex === 1) return 'dobMonth';
                                    if (myIndex === 2) return 'dobYear';
                                } else if (firstIsYear) {
                                    if (myIndex === 0) return 'dobYear';
                                    if (myIndex === 1) return 'dobMonth';
                                    if (myIndex === 2) return 'dobDay';
                                }
                            }
                        }
                    }

                    var asNum = parseInt(cleanNumericText, 10);
                    if (!isNaN(asNum)) {
                        if (asNum > 31 && asNum >= 1900 && asNum <= 2099) return 'dobYear';
                        if (asNum > 12 && asNum <= 31) return 'dobDay';
                    }
                    if (tag === 'input' && (type === 'date' || type === 'text')) return 'dob';
                    return 'dob';
                }

                if (tag === 'select' || el.getAttribute('role') === 'combobox' || el.getAttribute('role') === 'listbox') {
                    var optsTxt = (el.innerText || '').toLowerCase();
                    if (/january|february|march|april|\bjan\b|\bfeb\b|\bmar\b|\bapr\b/i.test(optsTxt)) return 'dobMonth';
                    if (/\b1990\b|\b1995\b|\b2000\b|\b2005\b/.test(optsTxt)) return 'dobYear';
                    if (/\b1\b[\s\S]*\b15\b[\s\S]*\b28\b/.test(optsTxt) && (!el.options || (el.options.length >= 28 && el.options.length <= 33))) return 'dobDay';
                }
                if (type === 'date' || type === 'datetime-local' || type === 'month' || ac.indexOf('bday') === 0 || el.hasAttribute('data-datepicker') || /birth|dob|bday|date.*of.*birth|mm\/dd\/yyyy|dd\/mm\/yyyy|yyyy-mm-dd/i.test(combined)) {
                    return 'dob';
                }

                if (type === 'tel' || ac.indexOf('tel') === 0 || /phone|mobile|cell|telephone|whatsapp|contact.*num/i.test(combined)) return 'phoneNumber';

                if (ac === 'sex' || /gender|sex/i.test(combined)) return 'gender';
                if (ac === 'given-name' || /first.*name|fname|given.*name|forename/i.test(combined)) return 'firstName';
                if (ac === 'family-name' || /last.*name|lname|surname|family.*name/i.test(combined)) return 'lastName';
                if (ac === 'name' || /full.*name|^name$|your.*name|display.*name/i.test(combined)) return 'fullName';
                if (ac === 'postal-code' || /zip|postal|pincode|postcode/i.test(combined)) return 'zipCode';
                if (ac === 'address-level2' || /city|town|locality/i.test(combined)) return 'city';
                if (ac === 'address-level1' || /state|province|region/i.test(combined)) return 'state';
                if (ac === 'address-line2' || /address.*2|line.*2|apt|suite|unit/i.test(combined)) return 'addressLine2';
                if (ac === 'street-address' || ac === 'address-line1' || /address|street|addr/i.test(combined)) return 'address';
                if (type === 'search' || /search|query|^q$/i.test(combined)) return 'username';
                return '';
            }

            function getElementDescription(el, fieldKey) {
                if (!el) return 'Element';
                var tag = el.tagName.toLowerCase();
                var type = (el.type || '').toLowerCase();
                var role = (el.getAttribute('role') || '').toLowerCase();
                var labelText = (el.placeholder || getAssociatedLabelText(el) || el.getAttribute('aria-label') || el.name || el.id || el.innerText || el.value || '').trim();
                labelText = labelText.replace(/\s+/g, ' ');
                if (labelText.length > 26) labelText = labelText.substring(0, 26) + '...';

                if (type === 'checkbox' || role === 'checkbox' || role === 'switch') {
                    return 'Check: ' + (labelText || el.name || 'Checkbox');
                }
                if (type === 'radio' || role === 'radio') {
                    return 'Radio: ' + (labelText || el.value || el.name || 'Option');
                }
                if (fieldKey === 'countryCode') {
                    return 'Select Country Code';
                }
                if (fieldKey === 'dob') {
                    return 'Select Date of Birth';
                }
                if (fieldKey === 'dobDay' || fieldKey === 'dobMonth' || fieldKey === 'dobYear') {
                    return 'Select ' + fieldKey + (labelText ? ' (' + labelText + ')' : '');
                }
                if (fieldKey) {
                    return 'Fill ' + fieldKey + (labelText ? ' (' + labelText + ')' : '');
                }
                if (tag === 'button' || type === 'submit' || role === 'button') return 'Click: ' + (labelText || 'Submit');
                if (tag === 'select' || role === 'combobox') return 'Select: ' + (labelText || 'Option');
                if (tag === 'input' || tag === 'textarea') return 'Fill: ' + (labelText || type || 'Field');
                if (tag === 'a') return 'Link: ' + (labelText || 'Open');
                return 'Click ' + (labelText || tag);
            }

            function showGrabToastOnPage(el, text, color) {
                try {
                    var badge = document.createElement('div');
                    var rect = el.getBoundingClientRect();
                    badge.textContent = text;
                    badge.style.cssText = 'position:fixed;z-index:2147483647;background:' + (color || '#10B981') + ';color:#ffffff;font-family:-apple-system,BlinkMacSystemFont,sans-serif;font-size:11px;font-weight:700;padding:4px 8px;border-radius:6px;box-shadow:0 4px 12px rgba(0,0,0,0.3);pointer-events:none;transition:opacity 0.25s ease;';
                    var topPos = Math.max(8, rect.top - 28);
                    var leftPos = Math.max(8, Math.min(window.innerWidth - 180, rect.left));
                    badge.style.top = topPos + 'px';
                    badge.style.left = leftPos + 'px';
                    document.body.appendChild(badge);
                    setTimeout(function() {
                        badge.style.opacity = '0';
                        setTimeout(function() { if (badge.parentNode) badge.parentNode.removeChild(badge); }, 250);
                    }, 1100);
                } catch (e) {}
            }

            function resolveInteractiveTarget(rawTarget) {
                if (!rawTarget || !rawTarget.tagName) return null;
                if (rawTarget.closest && rawTarget.closest('.aura-hud')) return null;

                var labelEl = rawTarget.closest ? rawTarget.closest('label') : null;
                var linkedInput = null;
                if (labelEl) {
                    linkedInput = labelEl.querySelector('input, select, textarea') ||
                                 (labelEl.htmlFor ? document.getElementById(labelEl.htmlFor) : null);
                }

                return linkedInput ||
                    (rawTarget.closest ? rawTarget.closest('input, textarea, select, button, a, [class*="callingCode"], [class*="calling-code"], [class*="countryDropDown"], [class*="country-code"], [class*="countryCode"], [class*="dial-code"], [class*="dialCode"], [class*="birth"], [class*="dob"], [class*="birthday"], [class*="bday"], [class*="date-root"], [class*="date-picker"], [class*="datepicker"], [class*="hwid-"], .iti__selected-flag, .iti__selected-country, .iti__flag-container, .PhoneInputCountry, .selected-flag, .flag-dropdown, #countryList, [data-dial-code], [data-country-code], [data-datepicker], .MuiSelect-select, .ant-select-selector, .react-select__control, [role="button"], [role="checkbox"], [role="switch"], [role="radio"], [role="combobox"], [role="listbox"], [role="spinbutton"], [aria-haspopup], [contenteditable="true"], label') : null) ||
                    rawTarget;
            }

            // Suppress soft keyboard on pointerdown / touchstart / focusin while in No-Type Recording mode
            function suppressKeyboardInRecordMode(e) {
                if (!window._auraRecorderActive || window._auraPlaybackActive) return;
                var target = resolveInteractiveTarget(e.target);
                if (!target || !target.tagName) return;
                var tag = target.tagName.toLowerCase();
                var type = (target.type || '').toLowerCase();
                var isTextEntry = (tag === 'textarea') ||
                    (tag === 'input' && type !== 'submit' && type !== 'button' && type !== 'checkbox' && type !== 'radio' && type !== 'file') ||
                    target.isContentEditable;
                if (isTextEntry) {
                    if (!target.hasAttribute('data-aura-orig-inputmode')) {
                        target.setAttribute('data-aura-orig-inputmode', target.getAttribute('inputmode') || '');
                    }
                    target.setAttribute('inputmode', 'none');
                    if (e.type === 'focusin' || e.type === 'focus') {
                        try { target.blur(); } catch (err) {}
                    }
                }
            }

            document.addEventListener('pointerdown', suppressKeyboardInRecordMode, true);
            document.addEventListener('touchstart', suppressKeyboardInRecordMode, true);
            document.addEventListener('focusin', suppressKeyboardInRecordMode, true);

            function reportElementCapture(e) {
                if (!window._auraRecorderActive || window._auraPlaybackActive) return;
                var interactive = resolveInteractiveTarget(e.target);
                if (!interactive || !interactive.tagName) return;

                var tag = interactive.tagName.toLowerCase();
                var type = (interactive.type || '').toLowerCase();
                var role = (interactive.getAttribute('role') || '').toLowerCase();
                var now = Date.now();

                // If user is clicking an option inside an already-open dropdown / bottom sheet right after grabbing the trigger,
                // allow the option click to close the sheet without recording a duplicate noisy button step.
                var insideOverlay = interactive.closest ? interactive.closest('[role="listbox"], [role="menu"], .iti__country-list, .MuiMenu-list, .MuiPopover-root, .ant-select-dropdown, .react-select__menu') : null;
                var isOptionRole = (role === 'option' || role === 'menuitem' || interactive.classList.contains('iti__country') || interactive.hasAttribute('data-dial-code'));
                if ((insideOverlay || isOptionRole) &&
                    (now - window._auraLastCapturedTime) < 8000 &&
                    (window._auraLastCapturedAction === 'SELECT_DROPDOWN' ||
                     window._auraLastCapturedAction === 'SELECT_COUNTRY_CODE' ||
                     window._auraLastCapturedAction === 'SELECT_DATE')) {
                    return;
                }

                var selector = getCssSelector(interactive);
                if (!selector) return;

                if (selector === window._auraLastCapturedSelector && (now - window._auraLastCapturedTime) < 850) {
                    return;
                }
                window._auraLastCapturedSelector = selector;
                window._auraLastCapturedTime = now;

                var fieldKey = detectFieldKey(interactive);
                var combined = ((interactive.name || '') + ' ' + (interactive.id || '') + ' ' + (interactive.placeholder || '') + ' ' + (interactive.getAttribute('aria-label') || '') + ' ' + getAssociatedLabelText(interactive) + ' ' + (interactive.innerText || '')).toLowerCase();
                var isCustomDrop = isCustomDropdownOrSheetTrigger(interactive, combined);
                var action = 'CLICK';

                if (type === 'checkbox' || role === 'checkbox' || role === 'switch') {
                    action = 'CHECKBOX';
                    fieldKey = '';
                    showGrabToastOnPage(interactive, '✓ Grabbed Checkbox', '#06B6D4');
                } else if (type === 'radio' || role === 'radio') {
                    action = 'RADIO';
                    fieldKey = '';
                    showGrabToastOnPage(interactive, '✓ Grabbed Radio', '#8B5CF6');
                } else if (fieldKey === 'countryCode') {
                    action = 'SELECT_COUNTRY_CODE';
                    showGrabToastOnPage(interactive, '✓ Grabbed Country Code -> {{countryCode}}', '#D946EF');
                } else if (fieldKey === 'dob') {
                    action = 'SELECT_DATE';
                    try { interactive.blur(); } catch (err) {}
                    showGrabToastOnPage(interactive, '✓ Grabbed DOB -> {{dob}}', '#0284C7');
                } else if (fieldKey === 'dobDay' || fieldKey === 'dobMonth' || fieldKey === 'dobYear') {
                    action = 'SELECT_DROPDOWN';
                    showGrabToastOnPage(interactive, '✓ Grabbed DOB Part -> {{' + fieldKey + '}}', '#0284C7');
                } else if (tag === 'select' || role === 'combobox' || role === 'listbox' || isCustomDrop) {
                    if (!fieldKey) fieldKey = 'gender';
                    action = 'SELECT_DROPDOWN';
                    showGrabToastOnPage(interactive, '✓ Grabbed Dropdown -> {{' + fieldKey + '}}', '#D946EF');
                } else if (tag === 'input' || tag === 'textarea' || interactive.isContentEditable) {
                    if (type === 'submit' || type === 'button' || type === 'image') {
                        action = 'SUBMIT';
                        fieldKey = '';
                        try { e.preventDefault(); e.stopPropagation(); } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed Submit', '#10B981');
                    } else if (type === 'file') {
                        action = 'UPLOAD_FILE';
                        if (!fieldKey) fieldKey = 'fileName';
                        try { e.preventDefault(); } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed File Upload', '#F59E0B');
                    } else if (type === 'date' || type === 'datetime-local' || type === 'month') {
                        action = 'SELECT_DATE';
                        if (!fieldKey) fieldKey = 'dob';
                        try { interactive.blur(); } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed Date -> {{' + fieldKey + '}}', '#0284C7');
                    } else {
                        action = 'INPUT';
                        if (!fieldKey) fieldKey = 'email';
                        interactive.setAttribute('inputmode', 'none');
                        try {
                            e.preventDefault();
                            interactive.blur();
                            if (document.activeElement && document.activeElement.blur) {
                                document.activeElement.blur();
                            }
                        } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed -> {{' + fieldKey + '}}', '#3B82F6');
                    }
                } else if (tag === 'button' || role === 'button' || tag === 'a') {
                    var btnTxt = (interactive.innerText || interactive.value || interactive.getAttribute('aria-label') || '').toLowerCase();
                    var isNextOrLink = (tag === 'a') || /next|continue|proceed|step/i.test(btnTxt);
                    if (!isNextOrLink && (type === 'submit' || /submit|sign.*up|register|create|login|log.*in|sign.*in|finish|send|confirm|verify/i.test(btnTxt))) {
                        action = 'SUBMIT';
                        try { e.preventDefault(); e.stopPropagation(); } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed Submit Button', '#10B981');
                    } else {
                        action = isNextOrLink ? 'CLICK' : 'SUBMIT';
                        showGrabToastOnPage(interactive, '✓ Grabbed Button', '#10B981');
                    }
                } else {
                    showGrabToastOnPage(interactive, '✓ Grabbed Element', '#10B981');
                }

                window._auraLastCapturedAction = action;
                interactive.setAttribute('data-aura-highlighted', '1');
                interactive.style.outline = '2px solid #10B981';
                interactive.style.outlineOffset = '2px';

                var desc = getElementDescription(interactive, fieldKey);
                if (window.AuraAutomation) {
                    if (window.AuraAutomation.onPageUrlDetected) {
                        window.AuraAutomation.onPageUrlDetected(window.location.href || '');
                    }
                    if (window.AuraAutomation.onUserAction) {
                        window.AuraAutomation.onUserAction(action, selector, desc, fieldKey || '');
                    }
                }
            }

            document.addEventListener('click', function(e) {
                reportElementCapture(e);
            }, true);
        })();
    """.trimIndent()

    /**
     * Disables the in-page recorder so normal clicks and automated playback submits are not intercepted,
     * and keeps inputmode suppressed when transitioning into playback.
     */
    val STOP_RECORDING_INJECTION = """
        (function() {
            window._auraRecorderActive = false;
            try {
                if (document.activeElement && document.activeElement.blur) {
                    document.activeElement.blur();
                }
            } catch (e) {}
        })();
    """.trimIndent()

    /**
     * Stronger DOM Scanner & Element Grabber:
     * - Traverses main document, open Shadow DOMs, and same-origin iframes
     * - Detects native inputs/selects AND custom dropdowns, bottom-sheet triggers, country code triggers, and DOB fields
     * - Filters out noise (navbar/footer links, password-visibility eye toggles, input clear buttons, hidden honeypots)
     * - Assigns distinct semantic types and logical execution order (Inputs/Dropdowns/DOB/Country -> Checkboxes/Radios -> Submit/Next Buttons)
     */
    val DOM_SCANNER = """
        (function() {
            var rawItems = [];
            var currentPageUrl = window.location.href || '';

            function cssEscape(str) {
                if (window.CSS && window.CSS.escape) return window.CSS.escape(str);
                return String(str).replace(/([ !"#$%&'()*+,.\/:;<=>?@\[\\\]^`{|}~])/g, '\\$1');
            }

            function isUniqueSelector(sel, targetEl) {
                try {
                    var matches = document.querySelectorAll(sel);
                    return matches.length === 1 && matches[0] === targetEl;
                } catch (e) {
                    return false;
                }
            }

            function getCssSelector(el) {
                if (!el || el === document || el.nodeType !== 1) return '';
                var tag = el.tagName.toLowerCase();

                if (el.id) {
                    var idSel = '#' + cssEscape(el.id);
                    if (isUniqueSelector(idSel, el)) return idSel;
                }

                var testAttrs = ['data-testid', 'data-test', 'data-qa', 'data-cy', 'data-automation-id'];
                for (var t = 0; t < testAttrs.length; t++) {
                    var attr = testAttrs[t];
                    var val = el.getAttribute(attr);
                    if (val) {
                        var testSel = '[' + attr + '="' + val.replace(/"/g, '\\"') + '"]';
                        if (isUniqueSelector(testSel, el)) return testSel;
                    }
                }

                if (el.name) {
                    var nameSel = tag + '[name="' + el.name.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(nameSel, el)) return nameSel;
                    if (el.value && (el.type === 'radio' || el.type === 'checkbox')) {
                        var nameValSel = nameSel + '[value="' + el.value.replace(/"/g, '\\"') + '"]';
                        if (isUniqueSelector(nameValSel, el)) return nameValSel;
                    }
                }

                var ariaLabel = el.getAttribute('aria-label');
                if (ariaLabel) {
                    var ariaSel = tag + '[aria-label="' + ariaLabel.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(ariaSel, el)) return ariaSel;
                }

                if (el.placeholder) {
                    var phSel = tag + '[placeholder="' + el.placeholder.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(phSel, el)) return phSel;
                }

                if (el.type && el.autocomplete) {
                    var acSel = tag + '[type="' + el.type + '"][autocomplete="' + el.autocomplete.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(acSel, el)) return acSel;
                }

                var path = [];
                var curr = el;
                while (curr && curr.nodeType === 1 && curr !== document.body && curr !== document.documentElement) {
                    var seg = curr.tagName.toLowerCase();
                    if (curr.id) {
                        path.unshift('#' + cssEscape(curr.id));
                        var candidate = path.join(' > ');
                        if (isUniqueSelector(candidate, el)) return candidate;
                    } else {
                        var parent = curr.parentElement;
                        if (parent) {
                            var sameTag = [];
                            for (var i = 0; i < parent.children.length; i++) {
                                if (parent.children[i].tagName === curr.tagName) sameTag.push(parent.children[i]);
                            }
                            if (sameTag.length > 1) {
                                seg += ':nth-of-type(' + (sameTag.indexOf(curr) + 1) + ')';
                            }
                        }
                        path.unshift(seg);
                        var partial = path.join(' > ');
                        if (isUniqueSelector(partial, el)) return partial;
                    }
                    curr = curr.parentElement;
                }
                return path.join(' > ');
            }

            function getAssociatedLabelText(el) {
                if (!el) return '';
                var parts = [];
                try {
                    if (el.labels && el.labels.length > 0) {
                        for (var i = 0; i < el.labels.length; i++) {
                            parts.push(el.labels[i].innerText || el.labels[i].textContent || '');
                        }
                    }
                    if (el.id) {
                        var forLbl = document.querySelector('label[for="' + el.id.replace(/"/g, '\\"') + '"]');
                        if (forLbl) parts.push(forLbl.innerText || forLbl.textContent || '');
                    }
                    var closestLbl = el.closest ? el.closest('label') : null;
                    if (closestLbl) parts.push(closestLbl.innerText || closestLbl.textContent || '');
                    var ariaLabelledBy = el.getAttribute('aria-labelledby');
                    if (ariaLabelledBy) {
                        var ids = ariaLabelledBy.split(/\s+/);
                        for (var j = 0; j < ids.length; j++) {
                            var refEl = document.getElementById(ids[j]);
                            if (refEl) parts.push(refEl.innerText || refEl.textContent || '');
                        }
                    }
                    var prev = el.previousElementSibling;
                    if (prev && (prev.tagName === 'LABEL' || prev.tagName === 'SPAN' || prev.tagName === 'DIV')) {
                        var prevTxt = (prev.innerText || prev.textContent || '').trim();
                        if (prevTxt.length < 60) parts.push(prevTxt);
                    }
                    var fieldContainer = el.closest ? el.closest('.form-group, .field, .input-group, .form-field, .MuiFormControl-root, .ant-form-item, fieldset') : null;
                    if (fieldContainer) {
                        var containerLbl = fieldContainer.querySelector('label, legend, .label, .form-label, .MuiFormLabel-root');
                        if (containerLbl) parts.push(containerLbl.innerText || containerLbl.textContent || '');
                    }
                } catch (e) {}
                return parts.join(' ').replace(/\s+/g, ' ').trim();
            }

            function isCountryCodeTrigger(el, combined) {
                if (!el) return false;
                var tag = (el.tagName || '').toLowerCase();
                // CRITICAL: An editable text/tel input is the PHONE NUMBER field, never a country code trigger!
                if (tag === 'input' && !el.readOnly && el.type !== 'button' && el.type !== 'submit') return false;
                var cls = (el.className && typeof el.className === 'string') ? el.className : '';
                if (/callingCode|calling-code|countryDropDown|country-code|countryCode|dial-code|dialCode|phone-country|phoneprefix|iti__|PhoneInputCountry|selected-flag|flag-dropdown/i.test(cls)) return true;
                if (el.hasAttribute('data-dial-code') || el.hasAttribute('data-country-code')) return true;
                var idAndName = ((el.id || '') + ' ' + (el.name || '')).toLowerCase();
                if (/countrylist|country_code|country-code|countrycode|dialcode|dial_code|phone_country|calling_code|callingcode|phoneprefix/i.test(idAndName)) return true;
                if (/country.*code|dial.*code|calling.*code|phone.*prefix|phone.*code|phone.*country|iti__selected-flag/i.test(combined)) return true;
                var txt = (el.innerText || el.textContent || el.value || '').trim();
                // Match e.g. +213(Algeria), +1, +44, etc.
                if (txt.length <= 25 && /^\s*([^\w\s]{1,4}\s*)?([a-zA-Z]{2}\s*)?\+\d{1,4}/.test(txt)) return true;
                var tag = (el.tagName || '').toLowerCase();
                if (tag === 'select' && el.options && el.options.length > 3) {
                    var sampleOpts = '';
                    for (var i = 0; i < Math.min(el.options.length, 8); i++) {
                        sampleOpts += ' ' + (el.options[i].text || '') + ' ' + (el.options[i].value || '');
                    }
                    if ((sampleOpts.match(/\+\d{1,4}\b/g) || []).length >= 2) return true;
                }
                var role = (el.getAttribute('role') || '').toLowerCase();
                if ((tag === 'select' || tag === 'button' || role === 'combobox' || role === 'button' || tag === 'div' || tag === 'span') && tag !== 'input') {
                    var phoneGroup = el.closest ? el.closest('.hwid-callingCodePhone-root, [class*="callingCode"], .iti, .PhoneInput, .react-tel-input, .phone-input, .input-group, .field, .form-group, div') : null;
                    if (phoneGroup && phoneGroup !== document.body) {
                        var telInp = phoneGroup.querySelector('input');
                        if (telInp && telInp !== el && phoneGroup.getBoundingClientRect().height < 120) {
                            if (!/submit|next|continue|send|verify|code|otp/i.test(txt)) return true;
                        }
                    }
                }
                return false;
            }

            function isCustomDropdownTrigger(el, tag, combined) {
                if (tag === 'select') return true;
                var role = (el.getAttribute('role') || '').toLowerCase();
                if (role === 'combobox' || role === 'listbox' || role === 'spinbutton') return true;
                var hasPopup = (el.getAttribute('aria-haspopup') || '').toLowerCase();
                if (hasPopup === 'listbox' || hasPopup === 'dialog' || hasPopup === 'menu' || hasPopup === 'true') return true;
                if (el.hasAttribute('aria-expanded')) return true;
                var cls = (el.className && typeof el.className === 'string') ? el.className : '';
                if (/MuiSelect-select|ant-select-selector|react-select__control|v-select|el-select|select-trigger|dropdown-toggle|custom-select|picker-trigger|bottom-sheet-trigger|hwid-countryDropDown|hwid-select|hwid-drop/i.test(cls)) {
                    return true;
                }
                if (tag === 'input' && el.readOnly && /select|choose|pick|country|gender|state|month|day|year|category/i.test(combined)) {
                    return true;
                }
                var txt = (el.innerText || el.textContent || el.placeholder || '').trim();
                if ((tag === 'button' || role === 'button' || tag === 'div' || tag === 'span') &&
                    (/^(select|choose|pick)\b/i.test(txt) || /\b(gender|sex|country|state|province|month|day|year)\b/i.test(combined)) &&
                    !/submit|sign.*up|register|login|log.*in|continue|next/i.test(txt)) {
                    return true;
                }
                return false;
            }

            function isUtilityOrNoiseButton(el, tag, type, combinedText) {
                if (isCountryCodeTrigger(el, combinedText)) return false;
                if (isCustomDropdownTrigger(el, tag, combinedText)) return false;
                var isBtnLike = (tag === 'button' || type === 'button' || el.getAttribute('role') === 'button' || tag === 'a');
                if (!isBtnLike) return false;
                if (type === 'submit') return false;

                // Ignore password show/hide eye toggles, clear input buttons, close/dismiss modals, and header/footer chrome
                if (/show.*pass|hide.*pass|toggle.*pass|reveal.*pass|clear.*input|dismiss|close|cancel|cookie|skip.*to.*content|menu|navbar|navigation|theme|dark.*mode|language/i.test(combinedText)) {
                    return true;
                }
                if (el.closest && el.closest('nav, header, footer, [role="navigation"], [role="banner"], [role="contentinfo"]')) {
                    if (!/sign.*up|register|create.*account/i.test(combinedText)) {
                        return true;
                    }
                }
                return false;
            }

            function classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel, labelText, inputIndex) {
                var ac = (el.autocomplete || '').toLowerCase();
                var inputMode = (el.getAttribute('inputmode') || '').toLowerCase();
                var combined = (name + ' ' + id + ' ' + placeholder + ' ' + text + ' ' + ariaLabel + ' ' + labelText + ' ' + ac).toLowerCase();
                var fieldHint = (name + ' ' + id + ' ' + placeholder + ' ' + ariaLabel + ' ' + (text.length < 25 ? text : '') + ' ' + (el.className || '')).toLowerCase();
                var isDropTrigger = isCustomDropdownTrigger(el, tag, combined);

                if (type === 'file' || (tag === 'input' && /upload|resume|document|avatar|attachment|file|cv/i.test(combined))) {
                    return 'FILE_UPLOAD';
                }
                if (type === 'password' || ac === 'new-password' || ac === 'current-password' || ((tag === 'input') && /pass|pwd/i.test(combined))) {
                    if (/confirm|repeat|retype|re-enter|verify|match|again|pass.*2/i.test(combined)) {
                        return 'CONFIRM_PASSWORD';
                    }
                    return 'PASSWORD';
                }

                // 2. Phone input check: If editable input with phone placeholder or inside callingCodePhone container
                if (tag === 'input' && !el.readOnly && el.type !== 'button' && el.type !== 'submit') {
                    var parentPhoneCont = el.closest ? el.closest('.hwid-callingCodePhone-root, [class*="callingCode"], .iti, .PhoneInput, .react-tel-input, .phone-input') : null;
                    if (type === 'tel' || /phone|mobile|cell|telephone|whatsapp|contact.*num/i.test(placeholder + ' ' + name + ' ' + id) || parentPhoneCont) {
                        return 'PHONE';
                    }
                }

                if (isCountryCodeTrigger(el, combined)) {
                    return 'COUNTRY_CODE';
                }
                if (type === 'email' || ac === 'email' || inputMode === 'email' || ((tag === 'input') && /email|e-mail|mail/i.test(combined))) {
                    return 'EMAIL';
                }
                if (ac === 'username' || ((tag === 'input' || tag === 'textarea') && /user|username|login|handle|nick|account|identifier/i.test(combined))) {
                    return 'USERNAME';
                }

                // IMPORTANT: Classify Split DOB (Day, Month, Year) and Full DOB BEFORE PHONE (type="tel"),
                // because Google, Yahoo, and mobile forms use <input type="tel" id="day"|"year"> for birthday parts!
                var dobContainer = el.closest ? el.closest('[class*="birth"], [class*="dob"], [class*="bday"], [class*="date-root"], [class*="date-wrapper"], .hwid-register-inputsContainer, .form-group, .field, .input-group, div') : null;
                var dobContextText = '';
                if (dobContainer) {
                    var rawCTxt = (dobContainer.innerText || dobContainer.textContent || '').trim();
                    if (rawCTxt.length < 250) dobContextText = rawCTxt.toLowerCase();
                }
                var isDobContext = /date\s*of\s*birth|birth\s*date|birthday|\bdob\b|hwid.*birth|hwid.*date/i.test(dobContextText + ' ' + combined);

                if (ac === 'bday-day' || /^(day|dd|birth_day|dob_day|bday_day|birthday_day|date_day)$/i.test(name) || /^(day|dd|birth_day|dob_day|bday-day)$/i.test(id) || /\b(birth.*day|dob.*day|select.*day|^day$|^dd$)\b/i.test(fieldHint) || (isDobContext && /\b(day|dd)\b/i.test(fieldHint) && !/today|everyday/i.test(fieldHint))) return 'DOB_DAY';
                if (ac === 'bday-month' || /^(month|mm|birth_month|dob_month|bday_month|birthday_month|date_month)$/i.test(name) || /^(month|mm|birth_month|dob_month|bday-month)$/i.test(id) || /\b(birth.*month|dob.*month|select.*month|^month$|^mm$)\b/i.test(fieldHint) || (isDobContext && /\b(month|mm)\b/i.test(fieldHint))) return 'DOB_MONTH';
                if (ac === 'bday-year' || /^(year|yyyy|yy|birth_year|dob_year|bday_year|birthday_year|date_year)$/i.test(name) || /^(year|yyyy|birth_year|dob_year|bday-year)$/i.test(id) || /\b(birth.*year|dob.*year|select.*year|^year$|^yyyy$)\b/i.test(fieldHint) || (isDobContext && /\b(year|yyyy|yr)\b/i.test(fieldHint))) return 'DOB_YEAR';

                if (isDobContext) {
                    var cleanNumericText = text.replace(/[^0-9]/g, '').trim();
                    if (/^(19\d\d|20\d\d)$/.test(cleanNumericText)) return 'DOB_YEAR';
                    if (/^(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)$/i.test(text)) return 'DOB_MONTH';

                    if (dobContainer) {
                        var allTriggers = dobContainer.querySelectorAll('[class*="select"], [class*="drop"], [class*="picker"], [class*="item"], [class*="box"], button, [role="combobox"], [role="button"], [role="spinbutton"], input, select');
                        var leafTriggers = [];
                        for (var lt = 0; lt < allTriggers.length; lt++) {
                            var sibT = allTriggers[lt];
                            if (sibT.querySelector && sibT.querySelector('button, [class*="select"], [class*="drop"], input, select')) continue;
                            if (leafTriggers.indexOf(sibT) < 0 && (sibT === el || sibT.getBoundingClientRect().width > 10)) {
                                leafTriggers.push(sibT);
                            }
                        }
                        if (leafTriggers.length >= 3) {
                            var myIndex = leafTriggers.indexOf(el);
                            if (myIndex < 0) {
                                for (var k = 0; k < leafTriggers.length; k++) {
                                    if (leafTriggers[k].contains(el)) { myIndex = k; break; }
                                }
                            }
                            if (myIndex >= 0) {
                                var lastIsYear = leafTriggers.some(function(t, idx) { return idx === 2 && /19\d\d|20\d\d|year/i.test(t.innerText || ''); });
                                var firstIsYear = leafTriggers.some(function(t, idx) { return idx === 0 && /19\d\d|20\d\d|year/i.test(t.innerText || ''); });
                                if (lastIsYear || (!firstIsYear && (myIndex === 0 || myIndex === 1 || myIndex === 2))) {
                                    if (myIndex === 0) return 'DOB_DAY';
                                    if (myIndex === 1) return 'DOB_MONTH';
                                    if (myIndex === 2) return 'DOB_YEAR';
                                } else if (firstIsYear) {
                                    if (myIndex === 0) return 'DOB_YEAR';
                                    if (myIndex === 1) return 'DOB_MONTH';
                                    if (myIndex === 2) return 'DOB_DAY';
                                }
                            }
                        }
                    }

                    var asNum = parseInt(cleanNumericText, 10);
                    if (!isNaN(asNum)) {
                        if (asNum > 31 && asNum >= 1900 && asNum <= 2099) return 'DOB_YEAR';
                        if (asNum > 12 && asNum <= 31) return 'DOB_DAY';
                    }
                    if (tag === 'input' && (type === 'date' || type === 'text')) return 'DOB';
                    return 'DOB';
                }

                if (tag === 'select' || isDropTrigger) {
                    var optsBlob = (el.innerText || '').toLowerCase();
                    if (/january|february|march|april|\bjan\b|\bfeb\b|\bmar\b|\bapr\b/i.test(optsBlob)) return 'DOB_MONTH';
                    if (/\b1990\b|\b1995\b|\b2000\b|\b2005\b/.test(optsBlob)) return 'DOB_YEAR';
                    if (/\b1\b[\s\S]*\b15\b[\s\S]*\b28\b/.test(optsBlob) && (!el.options || (el.options.length >= 28 && el.options.length <= 33))) return 'DOB_DAY';
                }

                if (type === 'date' || type === 'datetime-local' || type === 'month' || ac.indexOf('bday') === 0 || /birth|dob|bday|date.*of.*birth|mm\/dd\/yyyy|dd\/mm\/yyyy|yyyy-mm-dd/i.test(combined) || el.hasAttribute('data-datepicker')) {
                    if (ac === 'bday-day' || /\b(day|dd)\b/i.test(fieldHint)) return 'DOB_DAY';
                    if (ac === 'bday-month' || /\b(month|mm)\b/i.test(fieldHint)) return 'DOB_MONTH';
                    if (ac === 'bday-year' || /\b(year|yyyy|yr)\b/i.test(fieldHint)) return 'DOB_YEAR';
                    return 'DOB';
                }
                if (isDropTrigger) {
                    if (/\b(day|dd)\b/i.test(fieldHint)) return 'DOB_DAY';
                    if (/\b(month|mm)\b/i.test(fieldHint)) return 'DOB_MONTH';
                    if (/\b(year|yyyy|yr)\b/i.test(fieldHint)) return 'DOB_YEAR';
                }

                if (type === 'tel' || ac.indexOf('tel') === 0 || inputMode === 'tel' || ((tag === 'input') && /phone|mobile|cell|telephone|contact.*num|whatsapp/i.test(combined))) {
                    return 'PHONE';
                }

                if (ac === 'sex' || /gender|sex/i.test(combined) ||
                    (tag === 'select' && /male|female|non-binary/i.test(el.innerText || '')) ||
                    (type === 'radio' && /male|female/i.test(combined))) {
                    return 'GENDER';
                }

                if (ac === 'given-name' || ((tag === 'input') && /first.*name|fname|given.*name|forename/i.test(combined))) return 'FIRST_NAME';
                if (ac === 'family-name' || ((tag === 'input') && /last.*name|lname|surname|family.*name/i.test(combined))) return 'LAST_NAME';
                if (ac === 'name' || ((tag === 'input') && /full.*name|^name$|your.*name|display.*name/i.test(combined))) return 'FULL_NAME';
                if (ac === 'postal-code' || ((tag === 'input') && /zip|postal|pincode|pin.*code|postcode/i.test(combined))) return 'ZIP_PIN';
                if (ac === 'address-level2' || ((tag === 'input' || isDropTrigger) && /city|town|locality/i.test(combined))) return 'CITY';
                if (ac === 'address-level1' || ((tag === 'input' || isDropTrigger) && /state|province|region/i.test(combined))) return 'STATE';
                if (ac === 'address-line2' || ((tag === 'input') && /address.*2|line.*2|apt|suite|unit/i.test(combined))) return 'ADDRESS_LINE_2';
                if (ac === 'street-address' || ac === 'address-line1' || ((tag === 'input' || tag === 'textarea') && /address|street|addr/i.test(combined))) return 'ADDRESS';

                if (type === 'checkbox' || el.getAttribute('role') === 'checkbox' || el.getAttribute('role') === 'switch') {
                    if (/term|agree|policy|accept|consent|privacy|age/i.test(combined)) {
                        return 'CHECKBOX_TERMS';
                    }
                    return 'CHECKBOX';
                }
                if (type === 'radio' || el.getAttribute('role') === 'radio') {
                    return 'RADIO_OPTION';
                }
                if (isDropTrigger) {
                    return 'DROPDOWN';
                }
                if (tag === 'button' || type === 'submit' || type === 'button' || el.getAttribute('role') === 'button') {
                    if (type === 'submit' || /submit|sign.*up|create.*account|register|finish|done|log.*in|sign.*in|send|save|apply|confirm|verify|join/i.test(combined)) return 'SUBMIT_BUTTON';
                    if (/next|continue|proceed|forward|step/i.test(combined)) return 'NEXT_BUTTON';
                    return 'GENERIC_BUTTON';
                }
                if (tag === 'input' || tag === 'textarea' || el.isContentEditable) {
                    if (inputIndex === 0) return 'EMAIL';
                    if (inputIndex === 1) return 'USERNAME';
                    if (inputIndex === 2) return 'PASSWORD';
                    return 'GENERIC_INPUT';
                }
                return 'GENERIC_BUTTON';
            }

            // Deep collector across main document, open Shadow DOMs, and accessible same-origin iframes
            var candidateQuery = 'input, textarea, select, button, [role="button"], [role="combobox"], [role="listbox"], [role="spinbutton"], [role="checkbox"], [role="switch"], [role="radio"], [aria-haspopup="listbox"], [aria-haspopup="dialog"], [aria-haspopup="menu"], [aria-expanded], [contenteditable="true"], [class*="callingCode"], [class*="calling-code"], [class*="countryDropDown"], [class*="countryCode"], [class*="country-code"], [class*="dialCode"], [class*="dial-code"], [class*="birth"], [class*="dob"], [class*="birthday"], [class*="date-root"], [class*="date-picker"], [class*="datepicker"], [class*="hwid-"], [class*="select-trigger"], [class*="dropdown-trigger"], [class*="picker-trigger"], .iti__selected-flag, .iti__selected-country, .iti__flag-container, .PhoneInputCountry, .PhoneInputCountrySelect, .selected-flag, .flag-dropdown, #countryList, [data-dial-code], [data-country-code], [data-datepicker], .MuiSelect-select, .ant-select-selector, .react-select__control, .dropzone';

            function collectDeepElements(root, outList) {
                if (!root) return;
                try {
                    var found = root.querySelectorAll(candidateQuery);
                    for (var i = 0; i < found.length; i++) {
                        outList.push(found[i]);
                    }
                    var allNodes = root.querySelectorAll('*');
                    for (var j = 0; j < allNodes.length; j++) {
                        var node = allNodes[j];
                        if (node.shadowRoot) {
                            collectDeepElements(node.shadowRoot, outList);
                        }
                        if (node.tagName === 'IFRAME') {
                            try {
                                if (node.contentDocument) {
                                    collectDeepElements(node.contentDocument, outList);
                                }
                            } catch (iframeErr) {}
                        }
                    }
                } catch (e) {}
            }

            var elements = [];
            collectDeepElements(document, elements);

            var seenSelectors = {};
            var seenRadioGroups = {};
            var textInputCounter = 0;

            function processCandidate(el, allowHiddenStepFields) {
                if (!el || el.disabled) return;
                var tag = el.tagName.toLowerCase();
                var type = (el.type || '').toLowerCase();
                if (type === 'hidden' || type === 'reset') return;
                if (el.closest && el.closest('.aura-hud')) return;

                var name = el.name || '';
                var id = el.id || '';
                if (/csrf|token|authenticity|honeypot|timestamp|_utf8|octolytics|g-recaptcha|h-captcha|cf-turnstile/i.test(name + ' ' + id)) return;

                var style = window.getComputedStyle(el);
                var rect = el.getBoundingClientRect();
                var isCustomCheckOrFileOrSelect = (type === 'checkbox' || type === 'radio' || type === 'file' || tag === 'select' || el.classList.contains('PhoneInputCountrySelect'));

                if (!allowHiddenStepFields) {
                    if (!isCustomCheckOrFileOrSelect) {
                        if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return;
                        if (rect.width < 4 || rect.height < 4) return;
                    } else {
                        if (style.display === 'none' || style.visibility === 'hidden') return;
                        var parentRect = el.parentElement ? el.parentElement.getBoundingClientRect() : rect;
                        if (rect.width < 1 && parentRect.width < 4) return;
                    }
                }

                var placeholder = el.placeholder || '';
                var ariaLabel = el.getAttribute('aria-label') || '';
                var labelText = getAssociatedLabelText(el);
                var text = (el.innerText || el.textContent || el.value || labelText || '').trim().replace(/\s+/g, ' ');
                if (text.length > 35) text = text.substring(0, 35) + '...';

                var combinedForNoise = (name + ' ' + id + ' ' + placeholder + ' ' + text + ' ' + ariaLabel + ' ' + labelText + ' ' + (el.className || '')).toLowerCase();
                if (isUtilityOrNoiseButton(el, tag, type, combinedForNoise)) return;

                if (type === 'radio' && name) {
                    if (seenRadioGroups[name]) return;
                    seenRadioGroups[name] = true;
                }

                var isTextInput = (tag === 'input' && type !== 'submit' && type !== 'button' && type !== 'checkbox' && type !== 'radio' && type !== 'file') ||
                                  tag === 'textarea' || el.isContentEditable;
                var currentInputIdx = isTextInput ? textInputCounter : -1;

                var semantic = classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel, labelText, currentInputIdx);

                if (allowHiddenStepFields) {
                    var isHighValueField = (
                        semantic === 'EMAIL' ||
                        semantic === 'PASSWORD' ||
                        semantic === 'CONFIRM_PASSWORD' ||
                        semantic === 'USERNAME' ||
                        semantic === 'PHONE' ||
                        semantic === 'FIRST_NAME' ||
                        semantic === 'LAST_NAME' ||
                        semantic === 'FULL_NAME' ||
                        semantic === 'COUNTRY_CODE' ||
                        semantic === 'GENDER' ||
                        semantic === 'DOB' ||
                        semantic === 'DOB_DAY' ||
                        semantic === 'DOB_MONTH' ||
                        semantic === 'DOB_YEAR' ||
                        semantic === 'DROPDOWN' ||
                        semantic === 'CHECKBOX_TERMS' ||
                        semantic === 'CHECKBOX' ||
                        semantic === 'SUBMIT_BUTTON' ||
                        semantic === 'NEXT_BUTTON'
                    );
                    if (!isHighValueField) return;
                }

                var selector = getCssSelector(el);
                if (!selector || seenSelectors[selector]) return;
                seenSelectors[selector] = true;
                if (isTextInput) textInputCounter++;

                var isDropdownOrSpecialField = (
                    tag === 'select' ||
                    el.getAttribute('role') === 'combobox' ||
                    el.getAttribute('role') === 'listbox' ||
                    semantic === 'COUNTRY_CODE' ||
                    semantic === 'GENDER' ||
                    semantic === 'DOB' ||
                    semantic === 'DOB_DAY' ||
                    semantic === 'DOB_MONTH' ||
                    semantic === 'DOB_YEAR' ||
                    semantic === 'DROPDOWN'
                );
                var isInput = isTextInput || isDropdownOrSpecialField || type === 'file';
                var isBtn = !isDropdownOrSpecialField && (tag === 'button' || type === 'submit' || type === 'button' || el.getAttribute('role') === 'button');

                if (isTextInput) {
                    el.setAttribute('inputmode', 'none');
                }

                // Priority for smart workflow ordering:
                // 1 = Inputs, Dropdowns, Country Codes, DOB Pickers
                // 2 = Checkboxes & Radios
                // 3 = Next/Submit Buttons
                // 4 = Generic Buttons
                var orderPriority = 4;
                if (isInput) orderPriority = 1;
                else if (semantic === 'CHECKBOX' || semantic === 'CHECKBOX_TERMS' || semantic === 'RADIO_OPTION') orderPriority = 2;
                else if (semantic === 'SUBMIT_BUTTON' || semantic === 'NEXT_BUTTON') orderPriority = 3;

                el.setAttribute('data-aura-highlighted', '1');
                el.style.outline = orderPriority <= 2 ? '2px solid #06B6D4' : '2px dashed #10B981';
                el.style.outlineOffset = '2px';

                var options = [];
                if (tag === 'select' && el.options) {
                    for (var o = 0; o < Math.min(el.options.length, 12); o++) {
                        var optText = (el.options[o].text || el.options[o].value || '').trim();
                        if (optText) options.push(optText);
                    }
                }

                rawItems.push({
                    tagName: tag,
                    type: type,
                    nameAttr: name,
                    idAttr: id,
                    placeholder: placeholder || labelText,
                    text: text,
                    selector: selector,
                    pageUrl: currentPageUrl,
                    isInput: isInput,
                    isButton: isBtn,
                    semanticType: semantic,
                    detectedOptions: options,
                    _priority: orderPriority,
                    _top: rect.top || 0
                });
            }

            // Pass 1: Visible & custom elements
            elements.forEach(function(el) {
                processCandidate(el, false);
            });

            // Dedicated pass for custom segmented Date of Birth containers (e.g. Huawei ID 28 ▼, 10 ▼, 2006 ▼)
            var dobRoots = document.querySelectorAll('[class*="birth"], [class*="dob"], [class*="bday"], [class*="date-root"], [class*="date-wrapper"], .hwid-register-inputsContainer, .form-group, .field, .input-group, div');
            for (var dr = 0; dr < dobRoots.length; dr++) {
                var dobRoot = dobRoots[dr];
                var rootTxt = (dobRoot.innerText || dobRoot.textContent || '').trim();
                if (!/date\s*of\s*birth|birth\s*date|birthday|\bdob\b/i.test(rootTxt)) continue;
                if (rootTxt.length > 250) continue;
                var dobTriggers = dobRoot.querySelectorAll('[class*="select"], [class*="drop"], [class*="picker"], [class*="item"], [class*="box"], button, [role="combobox"], [role="button"], [role="spinbutton"], div, span');
                var leafDob = [];
                for (var dt = 0; dt < dobTriggers.length; dt++) {
                    var candTr = dobTriggers[dt];
                    if (candTr.querySelector && candTr.querySelector('button, [class*="select"], [class*="drop"], [class*="picker"]')) continue;
                    var r = candTr.getBoundingClientRect();
                    if (r.width < 10 || r.height < 10) continue;
                    var tTxt = (candTr.innerText || candTr.textContent || '').trim();
                    if (/date\s*of\s*birth|birthday/i.test(tTxt)) continue;
                    leafDob.push(candTr);
                }
                if (leafDob.length >= 3) {
                    for (var ld = 0; ld < leafDob.length; ld++) {
                        processCandidate(leafDob[ld], false);
                    }
                }
            }

            // Dedicated pass for Phone / Calling Code containers (e.g. .hwid-callingCodePhone-root, .iti)
            var phoneContainers = document.querySelectorAll('.hwid-callingCodePhone-root, [class*="callingCode"], [class*="calling-code"], .iti, .PhoneInput, .react-tel-input, .phone-input');
            for (var pc = 0; pc < phoneContainers.length; pc++) {
                var pCont = phoneContainers[pc];
                var phoneInp = pCont.querySelector('input');
                if (phoneInp) processCandidate(phoneInp, false);
                var codeTrig = pCont.querySelector('[class*="callingCode"], [class*="selected-flag"], [class*="flag"], [class*="country"], button, [role="button"], [role="combobox"], div, span');
                if (codeTrig && codeTrig !== phoneInp) processCandidate(codeTrig, false);
            }

            // Pass 2: Multi-step wizard / delayed reveal fallback if fewer than 2 inputs were visible
            var foundInputCount = rawItems.filter(function(item) { return item.isInput; }).length;
            if (foundInputCount < 2) {
                elements.forEach(function(el) {
                    processCandidate(el, true);
                });
            }

            // Sort elements into logical execution order: Inputs/Selects/DOB/Country (top-to-bottom) -> Checkboxes/Radios -> Submit/Next buttons
            rawItems.sort(function(a, b) {
                if (a._priority !== b._priority) return a._priority - b._priority;
                return a._top - b._top;
            });

            try {
                if (document.activeElement && document.activeElement.blur) {
                    document.activeElement.blur();
                }
            } catch (e) {}

            if (window.AuraAutomation && window.AuraAutomation.onScanResults) {
                window.AuraAutomation.onScanResults(JSON.stringify(rawItems));
            }
        })();
    """.trimIndent()

    /**
     * Clear DOM scan visual outlines and restore any modified attributes.
     */
    val CLEAR_SCAN_HIGHLIGHTS = """
        (function() {
            var elements = document.querySelectorAll(
                '[data-aura-highlighted="1"], input, button, [role="button"], [role="combobox"], [role="listbox"], [role="checkbox"], [role="switch"], [role="radio"], textarea, select, form, a, .iti__selected-flag, [data-dial-code], [data-datepicker], .dropzone'
            );
            elements.forEach(function(el) {
                el.style.outline = '';
                el.style.outlineOffset = '';
                el.removeAttribute('data-aura-highlighted');
            });
        })();
    """.trimIndent()

    /**
     * Shared JS helper snippet for deep element lookup (CSS selector -> Shadow DOM -> iframe -> fallback by fieldKey/description)
     */
    private fun jsFindElementHelper(escapedSelector: String, fallbackHint: String = ""): String {
        val escapedHint = fallbackHint.replace("'", "\\'").lowercase()
        return """
            window._auraPlaybackActive = true;
            window._auraRecorderActive = false;
            function queryDeep(root, sel) {
                if (!sel) return null;
                try {
                    var found = root.querySelector(sel);
                    if (found) return found;
                    var all = root.querySelectorAll('*');
                    for (var i = 0; i < all.length; i++) {
                        if (all[i].shadowRoot) {
                            var sub = queryDeep(all[i].shadowRoot, sel);
                            if (sub) return sub;
                        }
                        if (all[i].tagName === 'IFRAME') {
                            try {
                                if (all[i].contentDocument) {
                                    var iframeSub = queryDeep(all[i].contentDocument, sel);
                                    if (iframeSub) return iframeSub;
                                }
                            } catch (ie) {}
                        }
                    }
                } catch (e) {}
                return null;
            }
            var el = queryDeep(document, '$escapedSelector');
            if (!el && '$escapedHint') {
                var hint = '$escapedHint';
                var candidates = document.querySelectorAll('input, textarea, select, button, [role="button"], [role="combobox"], [role="listbox"], [role="checkbox"], [role="radio"], [aria-haspopup], .iti__selected-flag, .PhoneInputCountry, [data-dial-code], a, label');
                for (var c = 0; c < candidates.length; c++) {
                    var cand = candidates[c];
                    var blob = ((cand.name || '') + ' ' + (cand.id || '') + ' ' + (cand.placeholder || '') + ' ' + (cand.getAttribute('aria-label') || '') + ' ' + (cand.innerText || cand.value || '')).toLowerCase();
                    if (blob.indexOf(hint) >= 0) {
                        el = cand;
                        break;
                    }
                }
            }
            if (el) {
                var p = el;
                while (p && p !== document.body && p !== document.documentElement) {
                    if (p.hasAttribute && p.hasAttribute('hidden')) {
                        p.removeAttribute('hidden');
                    }
                    try {
                        var st = window.getComputedStyle(p);
                        if (st && st.display === 'none') {
                            p.style.display = 'block';
                        }
                        if (st && st.visibility === 'hidden') {
                            p.style.visibility = 'visible';
                        }
                    } catch (e) {}
                    p = p.parentElement;
                }
                if (el.disabled) {
                    el.disabled = false;
                    el.removeAttribute('disabled');
                }
            }
        """.trimIndent()
    }

    /**
     * Shared JS engine for Bottom Sheets, Modal Pickers, Custom Dropdown Menus, Touch/Pointer Event Dispatching,
     * and Smart Month / Numeric / Synonym Option Matching.
     */
    private fun jsOverlayAndPickerHelpers(): String {
        return """
            function isNodeVisible(node) {
                if (!node || node.nodeType !== 1) return false;
                if (node.closest && node.closest('.aura-hud')) return false;
                try {
                    var st = window.getComputedStyle(node);
                    if (!st || st.display === 'none' || st.visibility === 'hidden' || parseFloat(st.opacity) === 0) return false;
                    var r = node.getBoundingClientRect();
                    return r.width > 4 && r.height > 4;
                } catch (e) {
                    return false;
                }
            }

            function dispatchHumanTap(targetEl) {
                if (!targetEl) return;
                try {
                    targetEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                } catch (e) {}
                if (targetEl.tagName && (targetEl.tagName.toLowerCase() === 'input' || targetEl.tagName.toLowerCase() === 'textarea')) {
                    targetEl.setAttribute('inputmode', 'none');
                }
                var rect = targetEl.getBoundingClientRect();
                var cx = rect.left + Math.max(4, Math.min(rect.width - 4, rect.width / 2));
                var cy = rect.top + Math.max(4, Math.min(rect.height - 4, rect.height / 2));
                var evtInit = { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy, button: 0, buttons: 1 };
                ['pointerover', 'mouseover', 'pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(evtName) {
                    try {
                        var ev = (evtName.indexOf('pointer') === 0 && typeof PointerEvent !== 'undefined')
                            ? new PointerEvent(evtName, evtInit)
                            : new MouseEvent(evtName, evtInit);
                        targetEl.dispatchEvent(ev);
                    } catch (err) {
                        try { targetEl.dispatchEvent(new Event(evtName, { bubbles: true, cancelable: true })); } catch (e2) {}
                    }
                });
                if (typeof targetEl.click === 'function') {
                    try { targetEl.click(); } catch (e) {}
                }
            }

            function openDropdownOrSheetTrigger(triggerEl) {
                if (!triggerEl) return;
                try {
                    triggerEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                } catch (e) {}
                dispatchHumanTap(triggerEl);

                // Also tap inner or wrapper interactive trigger for MUI / AntD / Radix / React-Select / PhoneInput
                var innerTrigger = triggerEl.querySelector
                    ? triggerEl.querySelector('[role="combobox"], [role="button"], [aria-haspopup], .MuiSelect-select, .ant-select-selector, .react-select__control, .iti__selected-flag, button, input')
                    : null;
                if (innerTrigger && innerTrigger !== triggerEl) {
                    dispatchHumanTap(innerTrigger);
                }
                var wrapperTrigger = triggerEl.closest
                    ? triggerEl.closest('[role="combobox"], [aria-haspopup], .MuiInputBase-root, .ant-select, .react-select__control, .iti__flag-container')
                    : null;
                if (wrapperTrigger && wrapperTrigger !== triggerEl) {
                    dispatchHumanTap(wrapperTrigger);
                }

                // Dispatch ArrowDown / Enter for ARIA comboboxes that open on keyboard navigation
                try {
                    triggerEl.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', code: 'ArrowDown', keyCode: 40, which: 40, bubbles: true }));
                } catch (e) {}
            }

            function setNativeInputValueNoKeyboard(inputEl, val) {
                if (!inputEl) return;
                inputEl.setAttribute('inputmode', 'none');
                var proto = inputEl.tagName.toLowerCase() === 'textarea'
                    ? window.HTMLTextAreaElement.prototype
                    : window.HTMLInputElement.prototype;
                var setter = Object.getOwnPropertyDescriptor(proto, 'value');
                var prev = inputEl.value;
                if (setter && setter.set) {
                    setter.set.call(inputEl, val);
                } else {
                    inputEl.value = val;
                }
                if (inputEl._valueTracker) {
                    try { inputEl._valueTracker.setValue(prev); } catch (e) {}
                }
                try {
                    inputEl.dispatchEvent(new InputEvent('input', { bubbles: true, cancelable: true, inputType: 'insertText', data: val }));
                } catch (e) {
                    inputEl.dispatchEvent(new Event('input', { bubbles: true }));
                }
                inputEl.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'a' }));
                inputEl.dispatchEvent(new Event('change', { bubbles: true }));
                try { inputEl.blur(); } catch (e) {}
            }

            function findActiveOverlays(triggerEl) {
                var results = [];
                var seen = [];
                function addOverlay(node) {
                    if (!node || seen.indexOf(node) >= 0) return;
                    if (!isNodeVisible(node)) return;
                    seen.push(node);
                    results.push(node);
                }

                // 1. aria-controls / aria-owns linked popup or bottom sheet
                if (triggerEl && triggerEl.getAttribute) {
                    var linkedIds = ((triggerEl.getAttribute('aria-controls') || '') + ' ' + (triggerEl.getAttribute('aria-owns') || '') + ' ' + (triggerEl.getAttribute('data-target') || '')).trim().split(/\s+/);
                    for (var i = 0; i < linkedIds.length; i++) {
                        var cleanId = linkedIds[i].replace(/^#/, '');
                        if (cleanId) {
                            var byId = document.getElementById(cleanId);
                            if (byId) addOverlay(byId);
                        }
                    }
                }

                // 2. Standard Bottom Sheets, Modals, Drawers, Popovers, and Framework Dropdown Portals
                var overlaySelector = [
                    '[role="listbox"]',
                    '[role="menu"]',
                    '[role="dialog"]',
                    '[role="alertdialog"]',
                    '[aria-modal="true"]',
                    '.bottom-sheet',
                    '.bottomsheet',
                    '.action-sheet',
                    '.actionsheet',
                    '.sheet-modal',
                    '.sheet-container',
                    '.sheet-content',
                    '.drawer',
                    '.modal.show',
                    '.modal.open',
                    '.modal-content',
                    '.offcanvas.show',
                    '.dropdown-menu.show',
                    '.MuiModal-root',
                    '.MuiPopover-root',
                    '.MuiDrawer-root',
                    '.MuiMenu-list',
                    '.MuiAutocomplete-popper',
                    '.MuiPickersPopper-root',
                    '.MuiDialog-root',
                    '.ant-select-dropdown',
                    '.ant-drawer-content-wrapper',
                    '.ant-modal-content',
                    '.ant-picker-dropdown',
                    '.adm-popup',
                    '.adm-picker',
                    '[data-radix-popper-content-wrapper]',
                    '[data-radix-menu-content]',
                    '[data-radix-select-viewport]',
                    '[data-headlessui-state*="open"]',
                    '[vaul-drawer]',
                    '.v-overlay--active',
                    '.v-bottom-sheet',
                    '.v-menu__content',
                    '.el-select-dropdown',
                    '.el-drawer',
                    '.q-menu',
                    '.q-dialog',
                    '.iti__country-list:not(.iti__hide)',
                    '.iti--container',
                    '.PhoneInputCountrySelectDropdown',
                    '.country-list',
                    '.flag-dropdown.open',
                    '.react-select__menu',
                    '.flatpickr-calendar.open',
                    '.react-datepicker-popper',
                    '.picker-modal',
                    '.wheel-picker',
                    'ion-modal',
                    'ion-action-sheet',
                    'ion-picker',
                    'ion-popover'
                ].join(', ');

                var matches = document.querySelectorAll(overlaySelector);
                for (var m = 0; m < matches.length; m++) {
                    addOverlay(matches[m]);
                }

                // 3. Local dropdown menu inside trigger's parent/container
                if (triggerEl) {
                    var localContainer = triggerEl.closest
                        ? triggerEl.closest('#month, #countryList, .dropdown, .select, .custom-select, .iti, .react-tel-input, .PhoneInput, .phone-input, .form-group, .field, .input-group, .MuiFormControl-root, .ant-select, [role="combobox"], [role="listbox"]')
                        : triggerEl.parentElement;
                    if (localContainer) {
                        var localMenus = localContainer.querySelectorAll('ul, [role="listbox"], [role="menu"], .dropdown-menu, .dropdown-content, .options, .select-options, .select-items, .menu, .country-list, .iti__country-list');
                        for (var lm = 0; lm < localMenus.length; lm++) {
                            addOverlay(localMenus[lm]);
                        }
                        if (localContainer.querySelectorAll('[role="option"], [data-value], [data-dial-code], .iti__country, .country').length > 0) {
                            addOverlay(localContainer);
                        }
                    }
                }

                // 4. Heuristic detector for custom fixed/absolute Bottom Sheets & Portals attached to body
                var bodyChildren = document.body ? document.body.children : [];
                for (var b = bodyChildren.length - 1; b >= 0; b--) {
                    var node = bodyChildren[b];
                    if (!node || node.classList.contains('aura-hud') || node.tagName === 'SCRIPT' || node.tagName === 'STYLE') continue;
                    if (triggerEl && node.contains(triggerEl) && node.getAttribute('role') !== 'dialog') continue;
                    try {
                        var st = window.getComputedStyle(node);
                        if ((st.position === 'fixed' || st.position === 'absolute') && isNodeVisible(node)) {
                            var z = parseInt(st.zIndex, 10) || 0;
                            var rect = node.getBoundingClientRect();
                            if ((z >= 10 || rect.bottom >= window.innerHeight - 40) && rect.height >= 40) {
                                addOverlay(node);
                            }
                        }
                    } catch (e) {}
                }

                return results;
            }

            function isPlaceholderOrCloseText(txt) {
                var clean = (txt || '').trim().toLowerCase();
                if (!clean) return true;
                if (/^(select|choose|pick|--|---|none|null|cancel|close|dismiss|back|clear|reset|month|day|year|dd|mm|yyyy|×|✕)$/i.test(clean)) return true;
                if (/^(select|choose|pick)\s+(a|an|your|the|option|country|gender|date|month|day|year|state|city)/i.test(clean)) return true;
                return false;
            }

            function collectClickableOptionsFromOverlays(overlays, triggerEl) {
                var optionList = [];
                var seenOpts = [];
                function pushOpt(node) {
                    if (!node || node === triggerEl || seenOpts.indexOf(node) >= 0) return;
                    if (!isNodeVisible(node)) return;
                    if (node.disabled || node.getAttribute('aria-disabled') === 'true') return;
                    seenOpts.push(node);
                    optionList.push(node);
                }

                var itemQuery = '[role="option"], [role="menuitem"], [role="menuitemradio"], [data-value], [data-val], [data-option], [data-code], [data-dial-code], [data-country-code], li, .dropdown-item, .select-option, .option, .item, .list-item, .menu-item, .sheet-item, .picker-item, .wheel-item, .MuiMenuItem-root, .MuiAutocomplete-option, .ant-select-item-option, .adm-picker-column-item, .el-select-dropdown__item, .iti__country, .country, .country-item, .react-select__option';

                for (var i = 0; i < overlays.length; i++) {
                    var ov = overlays[i];
                    var stdItems = ov.querySelectorAll(itemQuery);
                    for (var j = 0; j < stdItems.length; j++) {
                        pushOpt(stdItems[j]);
                    }
                    // For mobile bottom sheets built with button or div rows without role="option"
                    var rowCandidates = ov.querySelectorAll('button, a, label, div, span');
                    for (var r = 0; r < rowCandidates.length; r++) {
                        var cand = rowCandidates[r];
                        if (seenOpts.indexOf(cand) >= 0) continue;
                        if (cand.querySelector && cand.querySelector('input, select, textarea, ul, [role="listbox"]')) continue;
                        var childButtons = cand.querySelectorAll ? cand.querySelectorAll('button, [role="option"], li') : [];
                        if (childButtons.length > 0) continue;
                        var rect = cand.getBoundingClientRect();
                        if (rect.width < 24 || rect.height < 16 || rect.height > 96) continue;
                        var txt = (cand.innerText || cand.textContent || '').trim().replace(/\s+/g, ' ');
                        if (!txt || txt.length > 85) continue;
                        if (/^(done|confirm|apply|ok|set|save|cancel|close|dismiss|back|×|✕)$/i.test(txt)) continue;
                        var tag = cand.tagName.toLowerCase();
                        var st = window.getComputedStyle(cand);
                        var isClickableRow = (tag === 'button' || tag === 'a' || tag === 'label' ||
                            cand.getAttribute('role') === 'button' ||
                            st.cursor === 'pointer' ||
                            cand.onclick != null ||
                            (cand.className && typeof cand.className === 'string' && /item|option|row|choice|entry|cell|country|select/i.test(cand.className)));
                        if (isClickableRow) {
                            pushOpt(cand);
                        }
                    }
                }
                return optionList;
            }

            function clickBottomSheetConfirmIfPresent(overlays, clickedOpt) {
                setTimeout(function() {
                    try {
                        var searchRoots = (overlays && overlays.length > 0) ? overlays : [document];
                        for (var i = 0; i < searchRoots.length; i++) {
                            var root = searchRoots[i];
                            if (!root || !root.querySelectorAll) continue;
                            var btns = root.querySelectorAll('button, [role="button"], a, span, div');
                            for (var j = 0; j < btns.length; j++) {
                                var b = btns[j];
                                if (b === clickedOpt || !isNodeVisible(b)) continue;
                                var r = b.getBoundingClientRect();
                                if (r.width < 24 || r.height < 18 || r.height > 80) continue;
                                var txt = (b.innerText || b.textContent || b.value || b.getAttribute('aria-label') || '').trim();
                                if (/^(done|confirm|apply|ok|set|select|choose|save)$/i.test(txt)) {
                                    dispatchHumanTap(b);
                                    return;
                                }
                            }
                        }
                    } catch (e) {}
                }, 95);
            }

            var MONTH_TABLE = [
                { num: 1, zeroIdx: '0', pad: '01', str: '1', full: 'january', short: 'jan' },
                { num: 2, zeroIdx: '1', pad: '02', str: '2', full: 'february', short: 'feb' },
                { num: 3, zeroIdx: '2', pad: '03', str: '3', full: 'march', short: 'mar' },
                { num: 4, zeroIdx: '3', pad: '04', str: '4', full: 'april', short: 'apr' },
                { num: 5, zeroIdx: '4', pad: '05', str: '5', full: 'may', short: 'may' },
                { num: 6, zeroIdx: '5', pad: '06', str: '6', full: 'june', short: 'jun' },
                { num: 7, zeroIdx: '6', pad: '07', str: '7', full: 'july', short: 'jul' },
                { num: 8, zeroIdx: '7', pad: '08', str: '8', full: 'august', short: 'aug' },
                { num: 9, zeroIdx: '8', pad: '09', str: '9', full: 'september', short: 'sep' },
                { num: 10, zeroIdx: '9', pad: '10', str: '10', full: 'october', short: 'oct' },
                { num: 11, zeroIdx: '10', pad: '11', str: '11', full: 'november', short: 'nov' },
                { num: 12, zeroIdx: '11', pad: '12', str: '12', full: 'december', short: 'dec' }
            ];

            function detectMonthNumFromText(str) {
                var s = (str || '').toLowerCase().trim();
                if (!s) return -1;
                for (var i = 0; i < MONTH_TABLE.length; i++) {
                    var m = MONTH_TABLE[i];
                    var re = new RegExp('(^|\\\\b)(' + m.full + '|' + m.short + ')(\\\\b|${'$'})', 'i');
                    if (re.test(s)) return m.num;
                }
                return -1;
            }

            function resolveTargetMonthObject(rawTarget, hintStr) {
                var t = (rawTarget || '').toLowerCase().trim();
                var h = (hintStr || '').toLowerCase();
                var isMonthField = /month|bday-month|dobmonth|mm/i.test(h);
                for (var i = 0; i < MONTH_TABLE.length; i++) {
                    var mo = MONTH_TABLE[i];
                    if (t === mo.full || t === mo.short) return mo;
                    if (isMonthField && (t === mo.pad || t === mo.str)) return mo;
                }
                return null;
            }

            function buildTargetSynonyms(rawTarget, hintStr) {
                var t = (rawTarget || '').toLowerCase().trim();
                var h = (hintStr || '').toLowerCase();
                var list = [];
                if (t) list.push(t);

                // Check if this is a month target or month field
                var isMonthField = /month|bday-month|dobmonth/i.test(h);
                for (var m = 0; m < MONTH_TABLE.length; m++) {
                    var mo = MONTH_TABLE[m];
                    if (t === mo.full || t === mo.short || (isMonthField && (t === mo.pad || t === mo.str))) {
                        list.push(mo.full, mo.short, mo.pad, mo.str);
                        break;
                    }
                    // Even if hint doesn't say month, if target is "01".."12", add month names as secondary synonyms
                    if (t === mo.pad || t === mo.str) {
                        list.push(mo.pad, mo.str, mo.full, mo.short);
                    }
                }

                // Numeric unpadded / padded synonyms (e.g., "05" <-> "5", "14" <-> "14")
                if (/^\d{1,4}$/.test(t)) {
                    var asInt = parseInt(t, 10);
                    if (!isNaN(asInt)) {
                        var unpadded = String(asInt);
                        var padded2 = asInt < 10 ? ('0' + asInt) : unpadded;
                        if (list.indexOf(unpadded) < 0) list.push(unpadded);
                        if (list.indexOf(padded2) < 0) list.push(padded2);
                    }
                }

                // Gender synonyms
                if (t === 'male' || t === 'm' || t === 'man') {
                    list.push('male', 'm', 'man', 'boy', 'masculine', 'mr');
                } else if (t === 'female' || t === 'f' || t === 'woman') {
                    list.push('female', 'f', 'woman', 'girl', 'feminine', 'ms', 'mrs');
                } else if (/prefer not|other|non-binary|nonbinary|custom/i.test(t)) {
                    list.push('prefer not to say', 'other', 'non-binary', 'rather not say', 'custom', 'unspecified', 'male', 'female');
                }

                return list;
            }

            function scoreOptionCandidate(optTextRaw, optValRaw, target, synonyms, hintStr) {
                var optText = (optTextRaw || '').toLowerCase().trim();
                var optVal = (optValRaw || '').toLowerCase().trim();
                if (!optText && !optVal) return -1;
                if (isPlaceholderOrCloseText(optText) && (!optVal || isPlaceholderOrCloseText(optVal))) return -1;

                // 0-Indexed vs 1-Indexed Month Protection:
                // If the option text contains an explicit month name (e.g., "January".."December")
                // and our target resolves to a month (e.g., "05" / "May"), match strictly by month identity
                // so `<option value="5">June</option>` on a 0-indexed select never matches target "05" (May).
                var targetMonthObj = resolveTargetMonthObject(target, hintStr || '');
                if (!targetMonthObj) {
                    for (var sm = 0; sm < synonyms.length; sm++) {
                        var mNum = detectMonthNumFromText(synonyms[sm]);
                        if (mNum > 0) {
                            targetMonthObj = MONTH_TABLE[mNum - 1];
                            break;
                        }
                    }
                }
                var optTextMonthNum = detectMonthNumFromText(optText);
                if (targetMonthObj && optTextMonthNum > 0) {
                    if (optTextMonthNum === targetMonthObj.num) return 100;
                    return -1;
                }

                if (target && (optText === target || optVal === target)) return 100;

                for (var s = 0; s < synonyms.length; s++) {
                    var syn = synonyms[s];
                    if (!syn) continue;
                    if (optText === syn || optVal === syn) return 92 - Math.min(s, 6);
                }

                // Numeric exact match (e.g. "05" vs "5")
                if (/^\d{1,4}$/.test(target)) {
                    var tNum = parseInt(target, 10);
                    var textMatchNum = /^\d{1,4}$/.test(optText) ? parseInt(optText, 10) : NaN;
                    var valMatchNum = /^\d{1,4}$/.test(optVal) ? parseInt(optVal, 10) : NaN;
                    if (textMatchNum === tNum || valMatchNum === tNum) return 85;
                }

                for (var k = 0; k < synonyms.length; k++) {
                    var synItem = synonyms[k];
                    if (synItem.length >= 2) {
                        var wordRegex = new RegExp('(^|\\\\b)' + synItem.replace(/[.*+?^${'$'}()|[\]\\]/g, '\\${'$'}&') + '(\\\\b|${'$'})', 'i');
                        if (wordRegex.test(optText) || wordRegex.test(optVal)) return 78 - Math.min(k, 6);
                    }
                    if (synItem.length >= 3 && (optText.indexOf(synItem) >= 0 || optVal.indexOf(synItem) >= 0)) {
                        return 65 - Math.min(k, 6);
                    }
                }

                return 10; // Valid non-placeholder option fallback
            }

            function selectBestOptionOnNativeSelect(selectEl, rawTarget, hintStr) {
                if (!selectEl || !selectEl.options || selectEl.options.length === 0) return false;
                var target = (rawTarget || '').toLowerCase().trim();
                var synonyms = buildTargetSynonyms(target, hintStr);
                var bestIdx = -1;
                var bestScore = -1;

                // Detect if this select is a 0-indexed numeric month select (options 0..11 with no month names)
                var isZeroIndexedMonthSelect = false;
                var targetMonth = resolveTargetMonthObject(target, hintStr);
                if (targetMonth) {
                    var hasZeroVal = false;
                    var hasTwelveVal = false;
                    var hasMonthNames = false;
                    for (var c = 0; c < selectEl.options.length; c++) {
                        var cv = (selectEl.options[c].value || '').trim();
                        var ct = (selectEl.options[c].text || '').trim();
                        if (cv === '0') hasZeroVal = true;
                        if (cv === '12') hasTwelveVal = true;
                        if (detectMonthNumFromText(ct) > 0) hasMonthNames = true;
                    }
                    if (hasZeroVal && !hasTwelveVal && !hasMonthNames) {
                        isZeroIndexedMonthSelect = true;
                    }
                }

                for (var i = 0; i < selectEl.options.length; i++) {
                    var opt = selectEl.options[i];
                    if (opt.disabled) continue;
                    var oText = (opt.text || opt.innerText || '').trim();
                    var oVal = (opt.value || '').trim();
                    if (i === 0 && (oVal === '' || isPlaceholderOrCloseText(oText))) continue;
                    var sc = isZeroIndexedMonthSelect && targetMonth && oVal === targetMonth.zeroIdx
                        ? 100
                        : scoreOptionCandidate(oText, oVal, target, synonyms, hintStr);
                    if (sc > bestScore) {
                        bestScore = sc;
                        bestIdx = i;
                    }
                }

                if (bestIdx < 0) {
                    bestIdx = selectEl.options.length > 1 ? 1 : 0;
                }

                var chosenOpt = selectEl.options[bestIdx];
                var prevVal = selectEl.value;
                selectEl.selectedIndex = bestIdx;
                chosenOpt.selected = true;

                var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value');
                if (nativeSetter && nativeSetter.set) {
                    try { nativeSetter.set.call(selectEl, chosenOpt.value); } catch (e) { selectEl.value = chosenOpt.value; }
                } else {
                    selectEl.value = chosenOpt.value;
                }
                if (selectEl._valueTracker) {
                    try { selectEl._valueTracker.setValue(prevVal); } catch (e) {}
                }
                selectEl.dispatchEvent(new Event('focus', { bubbles: false }));
                selectEl.dispatchEvent(new Event('input', { bubbles: true }));
                selectEl.dispatchEvent(new Event('change', { bubbles: true }));
                selectEl.dispatchEvent(new Event('blur', { bubbles: false }));
                return true;
            }
        """.trimIndent()
    }

    /**
     * Simulate a full human pointer/mouse click on an element by CSS selector or fallback text without opening the soft keyboard.
     */
    fun createClickScript(selector: String, fallbackHint: String = ""): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                ${jsFindElementHelper(escaped, fallbackHint)}
                if (!el) return 'NOT_FOUND';

                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                if (el.tagName.toLowerCase() === 'input' || el.tagName.toLowerCase() === 'textarea') {
                    el.setAttribute('inputmode', 'none');
                }

                ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(evt) {
                    var event = new MouseEvent(evt, { bubbles: true, cancelable: true, view: window });
                    el.dispatchEvent(event);
                });
                if (typeof el.click === 'function') {
                    try { el.click(); } catch (e) {}
                }
                try {
                    if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) {
                        document.activeElement.blur();
                    }
                } catch (e) {}
                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Dedicated script to reliably check / uncheck checkboxes and select radio buttons
     * across vanilla HTML, React, Angular, Vue, and custom ARIA components without double-toggling or showing keyboard.
     */
    fun createCheckScript(selector: String, isRadio: Boolean, desiredState: Boolean = true, fallbackHint: String = ""): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                ${jsFindElementHelper(escaped, fallbackHint)}
                if (!el) return 'NOT_FOUND';

                var input = null;
                var tag = el.tagName.toLowerCase();
                if (tag === 'input' && (el.type === 'checkbox' || el.type === 'radio')) {
                    input = el;
                } else if (tag === 'label') {
                    input = el.querySelector('input[type="checkbox"], input[type="radio"]') ||
                            (el.htmlFor ? document.getElementById(el.htmlFor) : null);
                } else {
                    input = el.querySelector('input[type="checkbox"], input[type="radio"]') ||
                            (el.closest ? el.closest('label')?.querySelector('input[type="checkbox"], input[type="radio"]') : null);
                }

                var target = input || el;
                target.scrollIntoView({ behavior: 'smooth', block: 'center' });

                var shouldCheck = $desiredState;
                var isRadio = $isRadio;
                var targetState = isRadio ? true : shouldCheck;

                if (input && (input.type === 'checkbox' || input.type === 'radio')) {
                    if (input.checked !== targetState) {
                        try { input.click(); } catch (e) {}
                    }
                    if (input.checked !== targetState) {
                        var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'checked');
                        if (nativeSetter && nativeSetter.set) {
                            nativeSetter.set.call(input, targetState);
                        } else {
                            input.checked = targetState;
                        }
                        if (input._valueTracker) {
                            input._valueTracker.setValue(!targetState);
                        }
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                } else {
                    var currentAria = el.getAttribute('aria-checked');
                    var isCurrentlyChecked = (currentAria === 'true');
                    if (isCurrentlyChecked !== targetState) {
                        ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(evt) {
                            var event = new MouseEvent(evt, { bubbles: true, cancelable: true, view: window });
                            el.dispatchEvent(event);
                        });
                    }
                    el.setAttribute('aria-checked', targetState ? 'true' : 'false');
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                }
                try {
                    if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
                } catch (e) {}
                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Input text into modern fields with full event dispatching for React 16-19, Vue, Angular, and contenteditable,
     * while strictly suppressing the Android soft keyboard (`inputmode="none"`, synthetic FocusEvent, and immediate blur).
     */
    fun createInputScript(selector: String, value: String, clearFirst: Boolean, fallbackHint: String = ""): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedValue = value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        return """
            (function() {
                ${jsFindElementHelper(escapedSelector, fallbackHint)}
                if (!el) return 'NOT_FOUND';

                if (el.tagName.toLowerCase() !== 'input' && el.tagName.toLowerCase() !== 'textarea' && !el.isContentEditable) {
                    var innerInput = el.querySelector('input, textarea, [contenteditable="true"]');
                    if (innerInput) el = innerInput;
                }

                // Suppress Android soft keyboard before any focus/input events
                var prevInputMode = el.getAttribute('inputmode');
                el.setAttribute('inputmode', 'none');

                el.scrollIntoView({ behavior: 'smooth', block: 'center' });

                // Dispatch synthetic focus events for framework listeners without invoking native IME focus
                try {
                    el.dispatchEvent(new FocusEvent('focus', { bubbles: false }));
                    el.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
                } catch (e) {}

                if (el.isContentEditable) {
                    if ($clearFirst) el.innerText = '';
                    el.innerText = '$escapedValue';
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    try { el.blur(); } catch (e) {}
                    return 'SUCCESS';
                }

                var proto = el.tagName.toLowerCase() === 'textarea'
                    ? window.HTMLTextAreaElement.prototype
                    : window.HTMLInputElement.prototype;
                var nativeSetter = Object.getOwnPropertyDescriptor(proto, 'value');

                var prevVal = el.value;
                if ($clearFirst) {
                    if (nativeSetter && nativeSetter.set) {
                        nativeSetter.set.call(el, '');
                    } else {
                        el.value = '';
                    }
                    if (el._valueTracker) el._valueTracker.setValue(prevVal);
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                }

                var beforeSet = el.value;
                if (nativeSetter && nativeSetter.set) {
                    nativeSetter.set.call(el, '$escapedValue');
                } else {
                    el.value = '$escapedValue';
                }

                if (el._valueTracker) {
                    el._valueTracker.setValue(beforeSet);
                }

                el.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'a' }));
                try {
                    el.dispatchEvent(new InputEvent('input', { bubbles: true, cancelable: true, inputType: 'insertText', data: '$escapedValue' }));
                } catch (e) {
                    el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
                }
                el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'a' }));
                el.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));
                try {
                    el.dispatchEvent(new FocusEvent('blur', { bubbles: false }));
                    el.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
                    el.blur();
                    if (document.activeElement && document.activeElement !== document.body && document.activeElement.blur) {
                        document.activeElement.blur();
                    }
                } catch (e) {}

                setTimeout(function() {
                    try {
                        if (prevInputMode) {
                            el.setAttribute('inputmode', prevInputMode);
                        } else {
                            el.removeAttribute('inputmode');
                        }
                    } catch (e) {}
                }, 120);

                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Multi-Phase Dropdown & Bottom-Sheet Picker Handler:
     * - Supports native <select> (with smart synonyms for Months, Days, Years, Gender, States, Countries, and 0-indexed vs 1-indexed month protection).
     * - Supports numeric/tel <input> or [role="spinbutton"] for split Day/Month/Year birthday fields.
     * - Supports custom ARIA/React/MUI/AntD/Radix/HeadlessUI/Google (#month) dropdown menus.
     * - Supports mobile slide-up Bottom Sheet pickers (Phase 1 opens the trigger and returns PENDING;
     *   Phase 2 searches/filters if needed, taps the matching option inside the sheet, and taps Confirm/Done if present).
     */
    fun createSelectDropdownScript(selector: String, optionValueOrText: String, fallbackHint: String = ""): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedTarget = optionValueOrText.replace("\\", "\\\\").replace("'", "\\'")
        val escapedHint = fallbackHint.replace("'", "\\'")
        return """
            (function() {
                ${jsFindElementHelper(escapedSelector, fallbackHint)}
                ${jsOverlayAndPickerHelpers()}
                if (!el) return 'NOT_FOUND';

                var target = '$escapedTarget'.toLowerCase().trim();
                var hint = ('$escapedHint' + ' ' + (el.name || '') + ' ' + (el.id || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.placeholder || '')).toLowerCase();
                var synonyms = buildTargetSynonyms(target, hint);
                var tag = el.tagName.toLowerCase();

                // 1. Native <select> or wrapper containing a native <select>
                var selectEl = (tag === 'select') ? el : (el.querySelector ? el.querySelector('select') : null);
                if (selectEl) {
                    selectBestOptionOnNativeSelect(selectEl, target, hint);
                    return 'SUCCESS:NATIVE_SELECT';
                }

                // 2. Plain text/number/tel <input> or [role="spinbutton"] that is NOT a custom dropdown/bottom-sheet trigger (e.g. Google/Yahoo split Day/Month/Year input)
                var hasPopupAttr = (el.getAttribute('aria-haspopup') || '').toLowerCase();
                var roleAttr = (el.getAttribute('role') || '').toLowerCase();
                if ((tag === 'input' && !el.readOnly && roleAttr !== 'combobox' && !hasPopupAttr) || roleAttr === 'spinbutton') {
                    var valueToEnter = '$escapedTarget';
                    var targetMonth = resolveTargetMonthObject(target, hint);
                    var isNumericInput = (el.type === 'number' || el.type === 'tel' || el.getAttribute('inputmode') === 'numeric' || roleAttr === 'spinbutton');
                    if (targetMonth && isNumericInput) {
                        var maxLen = parseInt(el.getAttribute('maxlength') || '2', 10);
                        valueToEnter = (maxLen === 1) ? targetMonth.str : targetMonth.pad;
                    } else if (/dobday|bday-day|\bday\b/i.test(hint) && /^\d{1,2}$/.test(target)) {
                        var dayInt = parseInt(target, 10);
                        var ph = (el.placeholder || '').toLowerCase();
                        valueToEnter = (ph === 'dd' && dayInt < 10) ? ('0' + dayInt) : String(dayInt);
                    }
                    if (tag === 'input') {
                        setNativeInputValueNoKeyboard(el, valueToEnter);
                    } else {
                        el.focus();
                        el.innerText = valueToEnter;
                        el.setAttribute('aria-valuenow', String(parseInt(valueToEnter, 10) || valueToEnter));
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    return 'SUCCESS:INPUT_FALLBACK';
                }

                // 3. Custom Dropdown Menu or Mobile Bottom-Sheet Picker (Stateful across evaluateJsWithRetry)
                var stepKey = 'drop_' + '$escapedSelector' + '_' + target;
                var overlays = findActiveOverlays(el);
                var options = collectClickableOptionsFromOverlays(overlays, el);

                // Phase 1: If no open bottom sheet / dropdown options are visible yet, open the trigger and wait for mount
                if (options.length === 0 && window._auraActiveDropdownKey !== stepKey) {
                    window._auraActiveDropdownKey = stepKey;
                    window._auraActiveDropdownAttempts = 1;
                    openDropdownOrSheetTrigger(el);
                    return 'PENDING:OPENED_DROPDOWN_TRIGGER';
                }

                window._auraActiveDropdownAttempts = (window._auraActiveDropdownAttempts || 1) + 1;

                // Check if the open bottom sheet / popover has a search/filter input inside it
                for (var v = 0; v < overlays.length; v++) {
                    var searchInput = overlays[v].querySelector('input[type="search"], input[placeholder*="search" i], input[placeholder*="filter" i], input[type="text"]');
                    if (searchInput && searchInput !== el && isNodeVisible(searchInput)) {
                        if (searchInput.getAttribute('data-aura-searched') !== target) {
                            searchInput.setAttribute('data-aura-searched', target);
                            setNativeInputValueNoKeyboard(searchInput, '$escapedTarget');
                            overlays = findActiveOverlays(el);
                            options = collectClickableOptionsFromOverlays(overlays, el);
                        }
                        break;
                    }
                }

                if (options.length > 0) {
                    var bestOpt = null;
                    var bestScore = -1;
                    for (var j = 0; j < options.length; j++) {
                        var cand = options[j];
                        var itemText = (cand.innerText || cand.textContent || '').replace(/\s+/g, ' ').trim();
                        var itemVal = (cand.getAttribute('data-value') || cand.getAttribute('data-val') || cand.getAttribute('value') || cand.getAttribute('aria-label') || '').trim();
                        var sc = scoreOptionCandidate(itemText, itemVal, target, synonyms, hint);
                        if (sc > bestScore) {
                            bestScore = sc;
                            bestOpt = cand;
                        }
                    }

                    if (!bestOpt) {
                        bestOpt = options[0];
                    }

                    if (bestOpt) {
                        try { bestOpt.scrollIntoView({ block: 'nearest' }); } catch (e) {}
                        dispatchHumanTap(bestOpt);
                        try {
                            bestOpt.setAttribute('aria-selected', 'true');
                            var chosenVal = bestOpt.getAttribute('data-value') || bestOpt.getAttribute('value') || (bestOpt.innerText || '').trim();
                            var wrapper = el.closest ? (el.closest('#month, #countryList, [role="combobox"], .dropdown, .select, .form-group, div') || el.parentElement) : el.parentElement;
                            if (wrapper) {
                                var hiddenInp = wrapper.querySelector('input[type="hidden"]');
                                if (hiddenInp && chosenVal) {
                                    hiddenInp.value = chosenVal;
                                    hiddenInp.dispatchEvent(new Event('input', { bubbles: true }));
                                    hiddenInp.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            }
                        } catch (e) {}
                        clickBottomSheetConfirmIfPresent(overlays, bestOpt);
                        window._auraActiveDropdownKey = '';
                        window._auraActiveDropdownAttempts = 0;
                        try {
                            if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
                        } catch (e) {}
                        return 'SUCCESS:SELECTED_CUSTOM_OPTION';
                    }
                }

                // If overlay still hasn't rendered options on attempt 2-3, tap trigger again and wait
                if (window._auraActiveDropdownAttempts <= 3) {
                    openDropdownOrSheetTrigger(el);
                    return 'PENDING:WAITING_FOR_DROPDOWN_OPTIONS';
                }

                // Final fallback for custom keyboard-driven comboboxes
                try {
                    if (tag === 'input') {
                        el.readOnly = false;
                        el.removeAttribute('readonly');
                        setNativeInputValueNoKeyboard(el, '$escapedTarget');
                    }
                    el.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', code: 'ArrowDown', keyCode: 40, bubbles: true }));
                    el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                } catch (e) {}
                window._auraActiveDropdownKey = '';
                window._auraActiveDropdownAttempts = 0;
                return 'SUCCESS:KEYBOARD_FALLBACK';
            })();
        """.trimIndent()
    }

    /**
     * Multi-Phase Country Code & Phone Prefix Selector:
     * - Supports native <select> and hidden/opacity-0 .PhoneInputCountrySelect overlays.
     * - Supports intl-tel-input (.iti__selected-flag / .iti__selected-country -> .iti__country-list), react-phone-input-2 (.selected-flag -> .country-list), Google #countryList, and sibling comboboxes next to input[type="tel"].
     * - Supports Mobile Bottom-Sheet Country Pickers with search inputs (types country name or +code into the sheet's search bar,
     *   selects the matching country row with strict token matching so +1 matches United States instead of +1242, and confirms the sheet).
     */
    fun createSelectCountryCodeScript(selector: String, dialCode: String, fallbackHint: String = "countryCode"): String {
        val escapedSelector = selector.replace("'", "\\'")
        val rawInput = dialCode.replace("'", "\\'").trim()
        val cleanDigits = rawInput.replace(Regex("[^0-9]"), "")
        return """
            (function() {
                ${jsFindElementHelper(escapedSelector, fallbackHint)}
                ${jsOverlayAndPickerHelpers()}

                if (!el) {
                    el = document.querySelector('#countryList, .iti__selected-country, .iti__selected-flag, .iti__flag-container, .PhoneInputCountrySelect, .PhoneInputCountry, .react-tel-input .selected-flag, .flag-dropdown, [data-dial-code], [data-country-code], select[name*="country" i], select[name*="code" i], select[name*="prefix" i], select[id*="country" i], [aria-label*="country code" i], [aria-label*="calling code" i], [aria-label*="phone country" i]');
                }
                if (!el) {
                    var phoneInp = document.querySelector('input[type="tel"], input[autocomplete*="tel"], input[name*="phone" i], input[name*="mobile" i]');
                    if (phoneInp && phoneInp.parentElement) {
                        var container = phoneInp.closest('.input-group, .phone-input, .iti, .react-tel-input, .PhoneInput, .field, .form-group, div') || phoneInp.parentElement;
                        el = container.querySelector('select, .iti__selected-country, .iti__selected-flag, .selected-flag, button, [role="button"], [role="combobox"], [data-dial-code]');
                    }
                }
                if (!el) return 'NOT_FOUND';

                var COUNTRY_MAP = {
                    '1': { iso: 'us', names: ['united states', 'usa', 'us', 'america', 'canada'] },
                    '44': { iso: 'gb', names: ['united kingdom', 'uk', 'great britain', 'england', 'gb'] },
                    '91': { iso: 'in', names: ['india', 'bharat', 'in'] },
                    '92': { iso: 'pk', names: ['pakistan', 'pk'] },
                    '971': { iso: 'ae', names: ['united arab emirates', 'uae', 'dubai', 'ae'] },
                    '966': { iso: 'sa', names: ['saudi arabia', 'saudi', 'ksa', 'sa'] },
                    '49': { iso: 'de', names: ['germany', 'deutschland', 'de'] },
                    '33': { iso: 'fr', names: ['france', 'fr'] },
                    '61': { iso: 'au', names: ['australia', 'au'] },
                    '81': { iso: 'jp', names: ['japan', 'jp'] },
                    '86': { iso: 'cn', names: ['china', 'cn'] },
                    '55': { iso: 'br', names: ['brazil', 'brasil', 'br'] },
                    '34': { iso: 'es', names: ['spain', 'espana', 'es'] },
                    '39': { iso: 'it', names: ['italy', 'italia', 'it'] },
                    '7': { iso: 'ru', names: ['russia', 'kazakhstan', 'ru'] },
                    '90': { iso: 'tr', names: ['turkey', 'turkiye', 'tr'] },
                    '20': { iso: 'eg', names: ['egypt', 'eg'] },
                    '234': { iso: 'ng', names: ['nigeria', 'ng'] },
                    '27': { iso: 'za', names: ['south africa', 'za'] },
                    '62': { iso: 'id', names: ['indonesia', 'id'] },
                    '60': { iso: 'my', names: ['malaysia', 'my'] },
                    '63': { iso: 'ph', names: ['philippines', 'ph'] },
                    '65': { iso: 'sg', names: ['singapore', 'sg'] },
                    '82': { iso: 'kr', names: ['south korea', 'korea', 'kr'] },
                    '52': { iso: 'mx', names: ['mexico', 'mx'] },
                    '880': { iso: 'bd', names: ['bangladesh', 'bd'] },
                    '31': { iso: 'nl', names: ['netherlands', 'holland', 'nl'] },
                    '41': { iso: 'ch', names: ['switzerland', 'ch'] },
                    '46': { iso: 'se', names: ['sweden', 'se'] },
                    '47': { iso: 'no', names: ['norway', 'no'] },
                    '45': { iso: 'dk', names: ['denmark', 'dk'] },
                    '358': { iso: 'fi', names: ['finland', 'fi'] },
                    '48': { iso: 'pl', names: ['poland', 'polska', 'pl'] },
                    '32': { iso: 'be', names: ['belgium', 'be'] },
                    '43': { iso: 'at', names: ['austria', 'at'] },
                    '353': { iso: 'ie', names: ['ireland', 'ie'] },
                    '351': { iso: 'pt', names: ['portugal', 'pt'] },
                    '30': { iso: 'gr', names: ['greece', 'gr'] },
                    '420': { iso: 'cz', names: ['czech republic', 'czechia', 'cz'] },
                    '36': { iso: 'hu', names: ['hungary', 'hu'] },
                    '40': { iso: 'ro', names: ['romania', 'ro'] },
                    '380': { iso: 'ua', names: ['ukraine', 'ua'] },
                    '972': { iso: 'il', names: ['israel', 'il'] },
                    '66': { iso: 'th', names: ['thailand', 'th'] },
                    '84': { iso: 'vn', names: ['vietnam', 'viet nam', 'vn'] },
                    '64': { iso: 'nz', names: ['new zealand', 'nz'] },
                    '54': { iso: 'ar', names: ['argentina', 'ar'] },
                    '57': { iso: 'co', names: ['colombia', 'co'] },
                    '56': { iso: 'cl', names: ['chile', 'cl'] },
                    '51': { iso: 'pe', names: ['peru', 'pe'] },
                    '58': { iso: 've', names: ['venezuela', 've'] },
                    '212': { iso: 'ma', names: ['morocco', 'ma'] },
                    '254': { iso: 'ke', names: ['kenya', 'ke'] },
                    '233': { iso: 'gh', names: ['ghana', 'gh'] },
                    '251': { iso: 'et', names: ['ethiopia', 'et'] },
                    '255': { iso: 'tz', names: ['tanzania', 'tz'] },
                    '256': { iso: 'ug', names: ['uganda', 'ug'] },
                    '213': { iso: 'dz', names: ['algeria', 'dz'] },
                    '974': { iso: 'qa', names: ['qatar', 'qa'] },
                    '965': { iso: 'kw', names: ['kuwait', 'kw'] },
                    '968': { iso: 'om', names: ['oman', 'om'] },
                    '973': { iso: 'bh', names: ['bahrain', 'bh'] },
                    '962': { iso: 'jo', names: ['jordan', 'jo'] },
                    '961': { iso: 'lb', names: ['lebanon', 'lb'] },
                    '94': { iso: 'lk', names: ['sri lanka', 'lk'] },
                    '977': { iso: 'np', names: ['nepal', 'np'] },
                    '852': { iso: 'hk', names: ['hong kong', 'hk'] },
                    '886': { iso: 'tw', names: ['taiwan', 'tw'] }
                };

                var rawLower = '$rawInput'.toLowerCase().trim();
                var cleanCode = '$cleanDigits';
                var countryInfo = null;
                if (cleanCode && COUNTRY_MAP[cleanCode]) {
                    countryInfo = COUNTRY_MAP[cleanCode];
                } else {
                    for (var codeKey in COUNTRY_MAP) {
                        var entry = COUNTRY_MAP[codeKey];
                        if (entry.iso === rawLower || entry.names.indexOf(rawLower) >= 0) {
                            cleanCode = codeKey;
                            countryInfo = entry;
                            break;
                        }
                    }
                }
                if (!cleanCode) cleanCode = '1';
                if (!countryInfo) countryInfo = COUNTRY_MAP[cleanCode] || { iso: '', names: [ rawLower ] };

                var plusCode = '+' + cleanCode;
                var primaryName = countryInfo.names[0] || plusCode;
                var primaryIso = countryInfo.iso || '';
                var dialRegex = new RegExp('(\\\\+|\\\\b)' + cleanCode + '(?!\\\\d)');

                function scoreCountryOption(textStr, valStr, dialAttr, isoAttr) {
                    var txt = (textStr || '').toLowerCase().trim();
                    var val = (valStr || '').toLowerCase().trim();
                    var dAttr = (dialAttr || '').replace(/^\+/, '').trim();
                    var cAttr = (isoAttr || '').toLowerCase().trim();

                    if (dAttr && dAttr === cleanCode) {
                        if (primaryIso && cAttr === primaryIso) return 100;
                        return 96;
                    }
                    if (primaryIso && (cAttr === primaryIso || val === primaryIso || val === primaryIso.toUpperCase())) return 95;
                    if (val === plusCode || val === cleanCode) return 94;

                    var hasExactDial = dialRegex.test(txt) || dialRegex.test(val);
                    var hasNameMatch = false;
                    for (var n = 0; n < countryInfo.names.length; n++) {
                        var nm = countryInfo.names[n];
                        if (nm && nm.length >= 2 && (txt.indexOf(nm) >= 0 || val === nm)) {
                            hasNameMatch = true;
                            break;
                        }
                    }

                    if (hasExactDial && hasNameMatch) return 98;
                    if (hasExactDial) return 90;
                    if (hasNameMatch) return 86;
                    if (!isPlaceholderOrCloseText(txt)) return 10;
                    return -1;
                }

                // 1. Check if target is a native <select> or contains/is adjacent to an embedded <select> (e.g. .PhoneInputCountrySelect)
                var wrapperGroup = el.closest ? (el.closest('.PhoneInput, .iti, .react-tel-input, .phone-input, .input-group, .form-group') || el.parentElement) : el.parentElement;
                var selectEl = (el.tagName.toLowerCase() === 'select')
                    ? el
                    : ((el.querySelector && el.querySelector('select')) ||
                       (wrapperGroup && wrapperGroup.querySelector('select.PhoneInputCountrySelect, select[name*="country" i], select[name*="code" i], select[name*="prefix" i], select')));

                if (selectEl && selectEl.options && selectEl.options.length > 0) {
                    var bestIdx = -1;
                    var bestScore = -1;
                    for (var i = 0; i < selectEl.options.length; i++) {
                        var opt = selectEl.options[i];
                        if (opt.disabled) continue;
                        var sc = scoreCountryOption(
                            opt.text || opt.innerText || '',
                            opt.value || '',
                            opt.getAttribute('data-dial-code') || '',
                            opt.getAttribute('data-country-code') || opt.getAttribute('data-code') || ''
                        );
                        if (sc > bestScore) {
                            bestScore = sc;
                            bestIdx = i;
                        }
                    }
                    if (bestIdx < 0) bestIdx = selectEl.options.length > 1 ? 1 : 0;
                    var chosen = selectEl.options[bestIdx];
                    var prevVal = selectEl.value;
                    selectEl.selectedIndex = bestIdx;
                    chosen.selected = true;
                    var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value');
                    if (nativeSetter && nativeSetter.set) {
                        try { nativeSetter.set.call(selectEl, chosen.value); } catch (e) { selectEl.value = chosen.value; }
                    } else {
                        selectEl.value = chosen.value;
                    }
                    if (selectEl._valueTracker) {
                        try { selectEl._valueTracker.setValue(prevVal); } catch (e) {}
                    }
                    selectEl.dispatchEvent(new Event('input', { bubbles: true }));
                    selectEl.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'SUCCESS:NATIVE_COUNTRY_SELECT';
                }

                // 2. If el is a direct text/tel <input> dedicated to country code (e.g. input[name="countryCode"])
                var elTag = el.tagName.toLowerCase();
                var elRole = (el.getAttribute('role') || '').toLowerCase();
                var elHasPopup = (el.getAttribute('aria-haspopup') || '').toLowerCase();
                if (elTag === 'input' && !el.readOnly && elRole !== 'combobox' && !elHasPopup && /code|prefix/i.test((el.name || '') + ' ' + (el.id || '') + ' ' + (el.placeholder || ''))) {
                    setNativeInputValueNoKeyboard(el, plusCode);
                    return 'SUCCESS:COUNTRY_INPUT_DIRECT';
                }

                // 3. Custom Country Dropdown or Mobile Bottom-Sheet Country Picker
                var stepKey = 'country_' + cleanCode;
                var overlays = findActiveOverlays(el);
                var countryItems = collectClickableOptionsFromOverlays(overlays, el);

                // Phase 1: Open the country code trigger / bottom sheet if not yet open
                if (countryItems.length === 0 && window._auraActiveCountryKey !== stepKey) {
                    window._auraActiveCountryKey = stepKey;
                    window._auraActiveCountryAttempts = 1;
                    openDropdownOrSheetTrigger(el);
                    return 'PENDING:OPENED_COUNTRY_SHEET';
                }

                window._auraActiveCountryAttempts = (window._auraActiveCountryAttempts || 1) + 1;

                // Check for a search bar inside the open bottom sheet / country dropdown
                for (var v = 0; v < overlays.length; v++) {
                    var searchInp = overlays[v].querySelector('.iti__search-input, .search-box, input[type="search"], input[placeholder*="search" i], input[placeholder*="country" i], input[placeholder*="code" i], input[type="text"]');
                    if (searchInp && searchInp !== el && searchInp.type !== 'tel' && isNodeVisible(searchInp)) {
                        if (searchInp.getAttribute('data-aura-searched') !== cleanCode) {
                            searchInp.setAttribute('data-aura-searched', cleanCode);
                            var searchTerm = primaryName.length > 2 ? primaryName : plusCode;
                            setNativeInputValueNoKeyboard(searchInp, searchTerm);
                            overlays = findActiveOverlays(el);
                            countryItems = collectClickableOptionsFromOverlays(overlays, el);
                        }
                        break;
                    }
                }

                if (countryItems.length === 0) {
                    var directItems = document.querySelectorAll('.iti__country, .country[data-dial-code], .country[data-country-code], [data-dial-code], [data-country-code], #countryList [role="option"]');
                    for (var d = 0; d < directItems.length; d++) {
                        if (isNodeVisible(directItems[d])) countryItems.push(directItems[d]);
                    }
                }

                if (countryItems.length > 0) {
                    var bestItem = null;
                    var bestItemScore = -1;
                    for (var k = 0; k < countryItems.length; k++) {
                        var item = countryItems[k];
                        var itemSc = scoreCountryOption(
                            item.innerText || item.textContent || '',
                            item.getAttribute('data-value') || item.getAttribute('value') || '',
                            item.getAttribute('data-dial-code') || '',
                            item.getAttribute('data-country-code') || item.getAttribute('data-code') || ''
                        );
                        if (itemSc > bestItemScore) {
                            bestItemScore = itemSc;
                            bestItem = item;
                        }
                    }

                    if (!bestItem) bestItem = countryItems[0];
                    if (bestItem) {
                        try { bestItem.scrollIntoView({ block: 'nearest' }); } catch (e) {}
                        dispatchHumanTap(bestItem);
                        try { bestItem.setAttribute('aria-selected', 'true'); } catch (e) {}
                        clickBottomSheetConfirmIfPresent(overlays, bestItem);
                        window._auraActiveCountryKey = '';
                        window._auraActiveCountryAttempts = 0;
                        try {
                            if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
                        } catch (e) {}
                        return 'SUCCESS:COUNTRY_SELECTED';
                    }
                }

                if (window._auraActiveCountryAttempts <= 3) {
                    openDropdownOrSheetTrigger(el);
                    return 'PENDING:WAITING_FOR_COUNTRY_SHEET';
                }

                window._auraActiveCountryKey = '';
                window._auraActiveCountryAttempts = 0;
                return 'SUCCESS:COUNTRY_FALLBACK';
            })();
        """.trimIndent()
    }

    /**
     * Multi-Phase Date of Birth (DOB) Selection Handler:
     * - Handles 3-part split DOB fields across <select>, <input type="tel"|"number"|"text">, and [role="spinbutton"] (matching "05", "5", "May", etc.).
     * - Handles native <input type="date">, <input type="month">, and <input type="datetime-local">.
     * - Handles formatted/masked text DOB inputs (MM/DD/YYYY, DD/MM/YYYY, YYYY-MM-DD, DD.MM.YYYY), including sequential keystroke fallback for input-mask libraries.
     * - Handles Custom Calendar Popups & Mobile Bottom-Sheet Wheel/Grid Date Pickers (opens picker in Phase 1,
     *   selects Year/Month/Day columns or calendar day cell in Phase 2, and clicks Confirm/Done/OK/Set).
     */
    fun createSelectDateScript(selector: String, isoDate: String, day: String, month: String, year: String, fallbackHint: String = "dob"): String {
        val escapedSelector = selector.replace("'", "\\'")
        val cleanYear = year.trim().ifBlank { "1998" }
        val cleanMonthNum = (month.trim().toIntOrNull() ?: 5).coerceIn(1, 12)
        val cleanDayNum = (day.trim().toIntOrNull() ?: 14).coerceIn(1, 31)
        val padMonth = if (cleanMonthNum < 10) "0$cleanMonthNum" else "$cleanMonthNum"
        val padDay = if (cleanDayNum < 10) "0$cleanDayNum" else "$cleanDayNum"
        val normalizedIso = "$cleanYear-$padMonth-$padDay"
        return """
            (function() {
                ${jsFindElementHelper(escapedSelector, fallbackHint)}
                ${jsOverlayAndPickerHelpers()}

                var yearStr = '$cleanYear';
                var monthPad = '$padMonth';
                var monthNumStr = '$cleanMonthNum';
                var dayPad = '$padDay';
                var dayNumStr = '$cleanDayNum';
                var isoStr = '$normalizedIso';

                if (!el) {
                    el = document.querySelector('input[type="date"], [data-datepicker], input[name*="dob" i], input[name*="birth" i], input[id*="dob" i], input[id*="birth" i]');
                }

                // Helper to populate any 3-part split DOB fields (<select>, <input>, or [role="spinbutton"]) inside a container
                function fillSplitDobSelects(rootNode) {
                    var root = rootNode || document;
                    var filledAny = false;

                    var daySel = root.querySelector('select[name*="day" i], select[id*="day" i], select[autocomplete="bday-day"], select[aria-label*="day" i]');
                    var monthSel = root.querySelector('select[name*="month" i], select[id*="month" i], select[autocomplete="bday-month"], select[aria-label*="month" i]');
                    var yearSel = root.querySelector('select[name*="year" i], select[id*="year" i], select[autocomplete="bday-year"], select[aria-label*="year" i]');

                    if (daySel) {
                        selectBestOptionOnNativeSelect(daySel, dayPad, 'dobDay');
                        filledAny = true;
                    }
                    if (monthSel) {
                        selectBestOptionOnNativeSelect(monthSel, monthPad, 'dobMonth');
                        filledAny = true;
                    }
                    if (yearSel) {
                        selectBestOptionOnNativeSelect(yearSel, yearStr, 'dobYear');
                        filledAny = true;
                    }

                    var dayInp = root.querySelector('input[name="day" i], input#day, input[name*="birth_day" i], input[name*="dob_day" i], input[name*="bday_day" i], input[autocomplete="bday-day"], input[aria-label="Day" i], input[placeholder="DD" i]');
                    var monthInp = root.querySelector('input[name="month" i], input#month, input[name*="birth_month" i], input[name*="dob_month" i], input[name*="bday_month" i], input[autocomplete="bday-month"], input[aria-label="Month" i], input[placeholder="MM" i]');
                    var yearInp = root.querySelector('input[name="year" i], input#year, input[name*="birth_year" i], input[name*="dob_year" i], input[name*="bday_year" i], input[autocomplete="bday-year"], input[aria-label="Year" i], input[placeholder="YYYY" i]');

                    if (dayInp && !dayInp.readOnly) {
                        setNativeInputValueNoKeyboard(dayInp, dayNumStr);
                        filledAny = true;
                    }
                    if (monthInp && !monthInp.readOnly && monthInp.getAttribute('role') !== 'combobox') {
                        setNativeInputValueNoKeyboard(monthInp, monthPad);
                        filledAny = true;
                    }
                    if (yearInp && !yearInp.readOnly) {
                        setNativeInputValueNoKeyboard(yearInp, yearStr);
                        filledAny = true;
                    }

                    // Also support ARIA [role="spinbutton"] segmented date fields (React Aria / MUI X DateField)
                    var spinButtons = root.querySelectorAll('[role="spinbutton"]');
                    for (var s = 0; s < spinButtons.length; s++) {
                        var sb = spinButtons[s];
                        var sbHint = ((sb.getAttribute('aria-label') || '') + ' ' + (sb.getAttribute('data-type') || '') + ' ' + (sb.innerText || '')).toLowerCase();
                        if (/month|\bmm\b/.test(sbHint)) {
                            sb.innerText = monthPad;
                            sb.setAttribute('aria-valuenow', monthNumStr);
                            sb.dispatchEvent(new Event('input', { bubbles: true }));
                            filledAny = true;
                        } else if (/day|\bdd\b/.test(sbHint)) {
                            sb.innerText = dayPad;
                            sb.setAttribute('aria-valuenow', dayNumStr);
                            sb.dispatchEvent(new Event('input', { bubbles: true }));
                            filledAny = true;
                        } else if (/year|\byyyy\b/.test(sbHint)) {
                            sb.innerText = yearStr;
                            sb.setAttribute('aria-valuenow', yearStr);
                            sb.dispatchEvent(new Event('input', { bubbles: true }));
                            filledAny = true;
                        }
                    }

                    return filledAny;
                }

                function typeMaskedDigitsIfNeeded(inputEl, formattedVal) {
                    setNativeInputValueNoKeyboard(inputEl, formattedVal);
                    var afterVal = (inputEl.value || '').trim();
                    if (!afterVal || /[_MDYmdy]{2,}/.test(afterVal)) {
                        var digitsOnly = formattedVal.replace(/[^0-9]/g, '');
                        for (var d = 0; d < digitsOnly.length; d++) {
                            var ch = digitsOnly.charAt(d);
                            inputEl.dispatchEvent(new KeyboardEvent('keydown', { key: ch, code: 'Digit' + ch, bubbles: true, cancelable: true }));
                            try {
                                inputEl.dispatchEvent(new InputEvent('beforeinput', { data: ch, inputType: 'insertText', bubbles: true, cancelable: true }));
                                inputEl.dispatchEvent(new InputEvent('input', { data: ch, inputType: 'insertText', bubbles: true, cancelable: true }));
                            } catch (e) {}
                            inputEl.dispatchEvent(new KeyboardEvent('keyup', { key: ch, code: 'Digit' + ch, bubbles: true }));
                        }
                        inputEl.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                }

                // 1. If target is a <select>, fill it (and any sibling split DOB fields)
                if (el && el.tagName.toLowerCase() === 'select') {
                    var scope = el.closest ? (el.closest('form, fieldset, .form-group, .field, div') || document) : document;
                    if (!fillSplitDobSelects(scope)) {
                        var optsText = (el.innerText || '').toLowerCase();
                        if (/january|february|\bjan\b|\bfeb\b/i.test(optsText)) {
                            selectBestOptionOnNativeSelect(el, monthPad, 'dobMonth');
                        } else if (/199\d|200\d/.test(optsText)) {
                            selectBestOptionOnNativeSelect(el, yearStr, 'dobYear');
                        } else {
                            selectBestOptionOnNativeSelect(el, dayPad, 'dobDay');
                        }
                    }
                    return 'SUCCESS:SPLIT_DOB_SELECT';
                }

                if (!el) {
                    if (fillSplitDobSelects(document)) return 'SUCCESS:SPLIT_DOB_SELECT';
                    return 'NOT_FOUND';
                }

                var tag = el.tagName.toLowerCase();
                var type = (el.type || '').toLowerCase();

                // 2. Native <input type="date" | "datetime-local" | "month">
                if (tag === 'input' && (type === 'date' || type === 'datetime-local' || type === 'month')) {
                    var targetDateVal = (type === 'month') ? (yearStr + '-' + monthPad) : (type === 'datetime-local' ? (isoStr + 'T12:00') : isoStr);
                    setNativeInputValueNoKeyboard(el, targetDateVal);
                    return 'SUCCESS:NATIVE_DATE_INPUT';
                }

                // Format date according to placeholder / label hints (MM/DD/YYYY vs DD/MM/YYYY vs YYYY-MM-DD)
                var formatHint = ((el.placeholder || '') + ' ' + (el.getAttribute('data-format') || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.name || '')).toLowerCase();
                var formattedDate = isoStr;
                if (/dd\/mm\/yyyy|dd\/mm\/aaaa|jj\/mm\/aaaa/.test(formatHint)) {
                    formattedDate = dayPad + '/' + monthPad + '/' + yearStr;
                } else if (/mm\/dd\/yyyy/.test(formatHint) || (formatHint.indexOf('/') >= 0 && !/yyyy\//.test(formatHint))) {
                    formattedDate = monthPad + '/' + dayPad + '/' + yearStr;
                } else if (/dd-mm-yyyy/.test(formatHint)) {
                    formattedDate = dayPad + '-' + monthPad + '-' + yearStr;
                } else if (/mm-dd-yyyy/.test(formatHint)) {
                    formattedDate = monthPad + '-' + dayPad + '-' + yearStr;
                } else if (/dd\.mm\.yyyy/.test(formatHint)) {
                    formattedDate = dayPad + '.' + monthPad + '.' + yearStr;
                } else if (/yyyy\/mm\/dd/.test(formatHint)) {
                    formattedDate = yearStr + '/' + monthPad + '/' + dayPad;
                }

                var isCustomPickerTrigger = (
                    tag !== 'input' ||
                    el.readOnly ||
                    el.hasAttribute('data-datepicker') ||
                    el.getAttribute('aria-haspopup') === 'dialog' ||
                    el.getAttribute('role') === 'combobox' ||
                    /datepicker|flatpickr|ant-picker|MuiPickers|react-datepicker/i.test((el.className || '') + ' ' + (el.parentElement ? el.parentElement.className : ''))
                );

                // 3. Standard editable text/masked DOB input (not a readonly bottom-sheet/calendar trigger)
                if (tag === 'input' && !isCustomPickerTrigger) {
                    var elScope = el.closest ? (el.closest('form, fieldset, .form-group, .field, div') || document) : document;
                    var elHint = ((el.name || '') + ' ' + (el.id || '') + ' ' + (el.placeholder || '') + ' ' + (el.getAttribute('aria-label') || '')).toLowerCase();
                    if (/\bday\b|bday-day|dob_day|^dd$/i.test(elHint)) {
                        setNativeInputValueNoKeyboard(el, dayNumStr);
                        fillSplitDobSelects(elScope);
                    } else if (/\byear\b|bday-year|dob_year|^yyyy$/i.test(elHint)) {
                        setNativeInputValueNoKeyboard(el, yearStr);
                        fillSplitDobSelects(elScope);
                    } else if (/\bmonth\b|bday-month|dob_month|^mm$/i.test(elHint)) {
                        setNativeInputValueNoKeyboard(el, monthPad);
                        fillSplitDobSelects(elScope);
                    } else {
                        typeMaskedDigitsIfNeeded(el, formattedDate);
                        fillSplitDobSelects(document);
                    }
                    return 'SUCCESS:FORMATTED_DATE_INPUT';
                }

                // 4. Custom Calendar Popup or Mobile Bottom-Sheet Wheel/Grid Date Picker
                var stepKey = 'dob_' + '$escapedSelector';
                var overlays = findActiveOverlays(el);

                if (overlays.length === 0 && window._auraActiveDateKey !== stepKey) {
                    window._auraActiveDateKey = stepKey;
                    window._auraActiveDateAttempts = 1;
                    openDropdownOrSheetTrigger(el);
                    return 'PENDING:OPENED_DATE_PICKER';
                }

                window._auraActiveDateAttempts = (window._auraActiveDateAttempts || 1) + 1;

                // Try selecting Year, Month, and Day inside the open Bottom Sheet or Calendar Popup
                var selectedInsideOverlay = false;
                for (var v = 0; v < overlays.length; v++) {
                    var ov = overlays[v];
                    if (fillSplitDobSelects(ov)) {
                        selectedInsideOverlay = true;
                    }

                    // Check for wheel/picker columns inside the bottom sheet (e.g. 3 columns for Month/Day/Year)
                    var columns = ov.querySelectorAll('.picker-column, .wheel-column, .adm-picker-column, [role="listbox"], ul');
                    if (columns.length >= 2) {
                        for (var c = 0; c < columns.length; c++) {
                            var colText = (columns[c].innerText || '').toLowerCase();
                            var colTarget = dayPad;
                            var colHint = 'dobDay';
                            if (/199\d|200\d/.test(colText)) {
                                colTarget = yearStr;
                                colHint = 'dobYear';
                            } else if (/jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec/i.test(colText)) {
                                colTarget = monthPad;
                                colHint = 'dobMonth';
                            }
                            var colOpts = collectClickableOptionsFromOverlays([columns[c]], el);
                            var colSyns = buildTargetSynonyms(colTarget, colHint);
                            var bestColOpt = null;
                            var bestColSc = -1;
                            for (var co = 0; co < colOpts.length; co++) {
                                var sc = scoreOptionCandidate(colOpts[co].innerText || '', colOpts[co].getAttribute('data-value') || '', colTarget, colSyns, colHint);
                                if (sc > bestColSc) {
                                    bestColSc = sc;
                                    bestColOpt = colOpts[co];
                                }
                            }
                            if (bestColOpt) {
                                dispatchHumanTap(bestColOpt);
                                selectedInsideOverlay = true;
                            }
                        }
                    }

                    // Check for calendar day grid cells (.flatpickr-day, .react-datepicker__day, .MuiPickersDay-root, [role="gridcell"], td button)
                    var dayCells = ov.querySelectorAll('.flatpickr-day:not(.disabled):not(.prevMonthDay):not(.nextMonthDay), .react-datepicker__day:not(.react-datepicker__day--disabled):not(.react-datepicker__day--outside-month), .MuiPickersDay-root:not(:disabled), .ant-picker-cell-in-view .ant-picker-cell-inner, [role="gridcell"]:not([aria-disabled="true"]), td button:not(:disabled)');
                    if (dayCells.length > 0) {
                        var matchedCell = null;
                        for (var dc = 0; dc < dayCells.length; dc++) {
                            var cellTxt = (dayCells[dc].innerText || dayCells[dc].textContent || '').trim();
                            if (cellTxt === dayNumStr || cellTxt === dayPad) {
                                matchedCell = dayCells[dc];
                                break;
                            }
                        }
                        if (!matchedCell) matchedCell = dayCells[Math.min(14, dayCells.length - 1)];
                        if (matchedCell) {
                            dispatchHumanTap(matchedCell);
                            selectedInsideOverlay = true;
                        }
                    }
                }

                // Also ensure that if el is a readonly <input>, its underlying value is populated
                if (tag === 'input' && (!el.value || el.value.trim() === '')) {
                    try {
                        el.readOnly = false;
                        el.removeAttribute('readonly');
                        setNativeInputValueNoKeyboard(el, formattedDate);
                    } catch (e) {}
                }

                if (selectedInsideOverlay || overlays.length > 0 || window._auraActiveDateAttempts >= 2) {
                    clickBottomSheetConfirmIfPresent(overlays, null);
                    window._auraActiveDateKey = '';
                    window._auraActiveDateAttempts = 0;
                    try {
                        if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
                    } catch (e) {}
                    return 'SUCCESS:DATE_PICKER_SELECTED';
                }

                openDropdownOrSheetTrigger(el);
                return 'PENDING:WAITING_FOR_DATE_PICKER';
            })();
        """.trimIndent()
    }

    /**
     * Modern File Upload Automation.
     */
    fun createFileUploadScript(selector: String, fileName: String, mimeType: String = "application/pdf"): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedFileName = fileName.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector') || document.querySelector('input[type="file"]');
                if (!el) {
                    var dropzone = document.querySelector('.dropzone, [data-upload], button[aria-label*="upload" i]');
                    if (dropzone) {
                        dropzone.click();
                        return 'CLICKED_DROPZONE';
                    }
                    return 'NOT_FOUND';
                }

                try {
                    var payload = "Aura Browser Automation Payload - " + '$escapedFileName';
                    var blob = new Blob([payload], { type: '$mimeType' });
                    var file = new File([blob], '$escapedFileName', { type: '$mimeType', lastModified: Date.now() });

                    var dt = new DataTransfer();
                    dt.items.add(file);
                    el.files = dt.files;

                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'SUCCESS';
                } catch (e) {
                    return 'ERROR: ' + e.message;
                }
            })();
        """.trimIndent()
    }

    /**
     * Stronger Form Submission / Button Click action.
     * Prioritizes clicking the submit button with full pointer/mouse events so SPA form handlers
     * (React onSubmit / onClick) execute properly instead of raw form.submit() page reload.
     */
    fun createSubmitScript(selector: String, fallbackHint: String = ""): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                ${jsFindElementHelper(escaped, fallbackHint)}
                if (!el) {
                    el = document.querySelector('button[type="submit"], input[type="submit"], button.btn-primary, [role="button"][type="submit"]');
                }
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    try {
                        if (document.activeElement && document.activeElement.blur) {
                            document.activeElement.blur();
                        }
                    } catch (e) {}
                    ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(evt) {
                        var event = new MouseEvent(evt, { bubbles: true, cancelable: true, view: window });
                        el.dispatchEvent(event);
                    });
                    if (typeof el.click === 'function' && el.tagName.toLowerCase() !== 'form') {
                        try { el.click(); } catch (e) {}
                    } else if (el.tagName.toLowerCase() === 'form') {
                        el.requestSubmit ? el.requestSubmit() : el.submit();
                    }
                    return 'SUCCESS';
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }
}
