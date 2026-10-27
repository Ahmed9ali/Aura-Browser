package com.example.automation

object AutomationScripts {

    /**
     * JavaScript injected to record user interactions on the page.
     * Captures ONLY the target element, action type, and semantic field key (not raw typed personal data),
     * because data to fill comes from "My Data" (or Default Random Generator).
     */
    val RECORDING_INJECTION = """
        (function() {
            if (window._auraRecorderActive) return;
            window._auraRecorderActive = true;
            window._auraLastCapturedSelector = '';
            window._auraLastCapturedTime = 0;

            function getCssSelector(el) {
                if (!el || el === document) return '';
                if (el.id) return '#' + el.id;
                if (el.name) return el.tagName.toLowerCase() + '[name="' + el.name + '"]';
                if (el.getAttribute('data-testid')) return '[data-testid="' + el.getAttribute('data-testid') + '"]';
                if (el.getAttribute('aria-label')) return '[aria-label="' + el.getAttribute('aria-label') + '"]';
                if (el.placeholder) return el.tagName.toLowerCase() + '[placeholder="' + el.placeholder.replace(/"/g, '\\"') + '"]';
                
                var path = [];
                var curr = el;
                while (curr && curr.nodeType === Node.ELEMENT_NODE && curr !== document.body && curr !== document.documentElement) {
                    var selector = curr.tagName.toLowerCase();
                    if (curr.id) {
                        path.unshift('#' + curr.id);
                        break;
                    }
                    if (curr.className && typeof curr.className === 'string') {
                        var classes = curr.className.trim().split(/\s+/).filter(function(c) { 
                            return c && !c.startsWith('aura-') && !c.includes(':'); 
                        });
                        if (classes.length > 0) selector += '.' + classes[0];
                    }
                    path.unshift(selector);
                    curr = curr.parentNode;
                }
                return path.join(' > ');
            }

            function detectFieldKey(el) {
                if (!el) return '';
                var type = (el.type || '').toLowerCase();
                var combined = (
                    (el.name || '') + ' ' +
                    (el.id || '') + ' ' +
                    (el.placeholder || '') + ' ' +
                    (el.getAttribute('aria-label') || '') + ' ' +
                    (el.autocomplete || '')
                ).toLowerCase();

                if (type === 'file' || /upload|resume|document|avatar|attachment|file|cv/i.test(combined)) return 'fileName';
                if (type === 'password' || /pass/i.test(combined)) {
                    if (/confirm|repeat|retype|verify|match/i.test(combined)) return 'confirmPassword';
                    return 'password';
                }
                if (type === 'email' || /email|e-mail|mail/i.test(combined)) return 'email';
                if (/user|username|login|handle|nick/i.test(combined)) return 'username';
                if (/country.*code|dial.*code|phone.*prefix/i.test(combined)) return 'countryCode';
                if (type === 'tel' || /phone|mobile|cell|telephone/i.test(combined)) return 'phoneNumber';
                if (/gender|sex/i.test(combined)) return 'gender';
                if (type === 'date' || /birth|dob|bday/i.test(combined)) return 'dob';
                if (/first.*name|fname|given.*name/i.test(combined)) return 'firstName';
                if (/last.*name|lname|surname|family.*name/i.test(combined)) return 'lastName';
                if (/full.*name|^name$|your.*name/i.test(combined)) return 'fullName';
                if (/zip|postal|pincode|pin.*code|postcode/i.test(combined)) return 'zipCode';
                if (/city|town/i.test(combined)) return 'city';
                if (/state|province/i.test(combined)) return 'state';
                if (/address.*2|line.*2|apt|suite/i.test(combined)) return 'addressLine2';
                if (/address|street/i.test(combined)) return 'address';
                return '';
            }

            function getElementDescription(el, fieldKey) {
                if (!el) return 'Element';
                var tag = el.tagName.toLowerCase();
                var type = (el.type || '').toLowerCase();
                var labelText = (el.placeholder || el.name || el.id || el.getAttribute('aria-label') || el.innerText || el.value || '').trim();
                if (labelText.length > 28) labelText = labelText.substring(0, 28) + '...';

                if (fieldKey) {
                    return 'Fill ' + fieldKey + (labelText ? ' (' + labelText + ')' : '');
                }
                if (tag === 'button' || type === 'submit') return 'Click Button: ' + (labelText || 'Submit');
                if (tag === 'select') return 'Select Dropdown: ' + (labelText || 'Option');
                if (tag === 'input' || tag === 'textarea') return 'Fill Input: ' + (labelText || type || 'field');
                if (tag === 'a') return 'Click Link: ' + (labelText || 'Navigate');
                return 'Click ' + tag + (labelText ? ' "' + labelText + '"' : '');
            }

            function reportElementCapture(el) {
                if (!el || !el.tagName) return;
                if (el.closest && el.closest('.aura-hud')) return;

                var interactive = el.closest('input, textarea, select, button, a, [role="button"], [role="combobox"]') || el;
                var tag = interactive.tagName.toLowerCase();
                var type = (interactive.type || '').toLowerCase();
                var selector = getCssSelector(interactive);
                if (!selector) return;

                var now = Date.now();
                if (selector === window._auraLastCapturedSelector && (now - window._auraLastCapturedTime) < 1200) {
                    return;
                }
                window._auraLastCapturedSelector = selector;
                window._auraLastCapturedTime = now;

                var fieldKey = detectFieldKey(interactive);
                var action = 'CLICK';

                if (tag === 'select' || interactive.getAttribute('role') === 'combobox') {
                    action = (fieldKey === 'countryCode') ? 'SELECT_COUNTRY_CODE' : 'SELECT_DROPDOWN';
                } else if (tag === 'input' || tag === 'textarea') {
                    if (type === 'submit' || type === 'button') {
                        action = 'SUBMIT';
                        fieldKey = '';
                    } else if (type === 'checkbox' || type === 'radio') {
                        action = 'CLICK';
                        fieldKey = '';
                    } else if (type === 'file') {
                        action = 'UPLOAD_FILE';
                        if (!fieldKey) fieldKey = 'fileName';
                    } else if (type === 'date' || type === 'datetime-local') {
                        action = 'SELECT_DATE';
                        if (!fieldKey) fieldKey = 'dob';
                    } else {
                        action = 'INPUT';
                        if (!fieldKey) fieldKey = 'email';
                    }
                } else if (tag === 'button' && type === 'submit') {
                    action = 'SUBMIT';
                }

                // Flash outline to confirm element was captured
                var prevOutline = interactive.style.outline;
                interactive.style.outline = '2px solid #10B981';
                setTimeout(function() { interactive.style.outline = prevOutline; }, 700);

                var desc = getElementDescription(interactive, fieldKey);
                if (window.AuraAutomation && window.AuraAutomation.onUserAction) {
                    window.AuraAutomation.onUserAction(action, selector, desc, fieldKey || '');
                }
            }

            document.addEventListener('click', function(e) {
                reportElementCapture(e.target);
            }, true);
        })();
    """.trimIndent()

    /**
     * Modern DOM Scanner:
     * Scans and semantically identifies page elements so only elements are captured
     * and mapped to "My Data" fields (email, username, password, phoneNumber, etc.).
     */
    val DOM_SCANNER = """
        (function() {
            var items = [];
            var currentPageUrl = window.location.href || '';
            var elements = document.querySelectorAll(
                'input, button, [role="button"], [role="combobox"], [role="listbox"], textarea, select, a[href], .iti__selected-flag, [data-dial-code], [data-datepicker], .dropzone'
            );

            function getCssSelector(el) {
                if (el.id) return '#' + el.id;
                if (el.name) return el.tagName.toLowerCase() + '[name="' + el.name + '"]';
                if (el.getAttribute('data-testid')) return '[data-testid="' + el.getAttribute('data-testid') + '"]';
                if (el.getAttribute('aria-label')) return '[aria-label="' + el.getAttribute('aria-label') + '"]';
                if (el.placeholder) return el.tagName.toLowerCase() + '[placeholder="' + el.placeholder.replace(/"/g, '\\"') + '"]';
                if (el.className && typeof el.className === 'string') {
                    var classes = el.className.trim().split(/\s+/).filter(function(c) {
                        return c && !c.startsWith('aura-') && !c.includes(':');
                    });
                    if (classes.length > 0) return el.tagName.toLowerCase() + '.' + classes[0];
                }
                return el.tagName.toLowerCase();
            }

            function classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel) {
                var combined = (name + ' ' + id + ' ' + placeholder + ' ' + text + ' ' + ariaLabel + ' ' + (el.autocomplete || '')).toLowerCase();

                // 1. File Upload
                if (type === 'file' || /upload|resume|document|avatar|attachment|file|cv|browse/i.test(combined)) {
                    if (tag === 'input' && type === 'file') return 'FILE_UPLOAD';
                    if (/upload|attach|choose file|browse/i.test(combined)) return 'FILE_UPLOAD';
                }

                // 2. Passwords: Distinguish Confirm Password from Primary Password
                if (type === 'password' || /pass/i.test(combined)) {
                    if (/confirm|repeat|retype|re-enter|verify|match|pass.*2/i.test(combined)) {
                        return 'CONFIRM_PASSWORD';
                    }
                    return 'PASSWORD';
                }

                // 3. Email
                if (type === 'email' || /email|e-mail|mail/i.test(combined)) {
                    return 'EMAIL';
                }

                // 4. Username / Login handle
                if ((tag === 'input' || tag === 'textarea') && /user|username|login|handle|nick|account/i.test(combined)) {
                    return 'USERNAME';
                }

                // 5. Country Code
                if (/country.*code|dial.*code|phone.*prefix|dialing|iti__selected-flag/i.test(combined) || 
                    el.classList.contains('iti__selected-flag') || 
                    el.hasAttribute('data-dial-code')) {
                    return 'COUNTRY_CODE';
                }

                // 6. Phone Number
                if (type === 'tel' || /phone|mobile|cell|telephone|contact.*num/i.test(combined)) {
                    return 'PHONE';
                }

                // 7. Gender
                if (/gender|sex/i.test(combined) || 
                    (tag === 'select' && /male|female/i.test(el.innerText || '')) ||
                    (type === 'radio' && /male|female/i.test(combined))) {
                    return 'GENDER';
                }

                // 8. Date of Birth (DOB)
                if (type === 'date' || type === 'datetime-local' || /birth|dob|bday|date.*of.*birth/i.test(combined) || el.hasAttribute('data-datepicker')) {
                    if (/day/i.test(combined)) return 'DOB_DAY';
                    if (/month/i.test(combined)) return 'DOB_MONTH';
                    if (/year/i.test(combined)) return 'DOB_YEAR';
                    return 'DOB';
                }
                if (/^(day|birth_day|dob_day)$/i.test(name) || /^(day)$/i.test(id)) return 'DOB_DAY';
                if (/^(month|birth_month|dob_month)$/i.test(name) || /^(month)$/i.test(id)) return 'DOB_MONTH';
                if (/^(year|birth_year|dob_year)$/i.test(name) || /^(year)$/i.test(id)) return 'DOB_YEAR';

                // 9. Name Fields (First, Last, Full)
                if (/first.*name|fname|given.*name/i.test(combined)) return 'FIRST_NAME';
                if (/last.*name|lname|surname|family.*name/i.test(combined)) return 'LAST_NAME';
                if (/full.*name|^name$|your.*name/i.test(combined) && tag === 'input') return 'FULL_NAME';

                // 10. Postal / PIN / ZIP Code
                if (/zip|postal|pincode|pin.*code|postcode/i.test(combined)) return 'ZIP_PIN';

                // 11. Address, City, State
                if (/city|town|locality/i.test(combined)) return 'CITY';
                if (/state|province|region/i.test(combined)) return 'STATE';
                if (/address.*2|line.*2|apt|suite|unit/i.test(combined)) return 'ADDRESS_LINE_2';
                if (/address|street|addr/i.test(combined)) return 'ADDRESS';

                // 12. Policy & Terms Checkbox
                if (type === 'checkbox' && /term|agree|policy|accept|consent|privacy/i.test(combined)) {
                    return 'CHECKBOX_TERMS';
                }

                // 13. Submit / Next / Action Buttons
                if (tag === 'button' || type === 'submit' || el.getAttribute('role') === 'button' || tag === 'a') {
                    if (/submit|sign.*up|create.*account|register|finish|done|log.*in|sign.*in/i.test(combined)) return 'SUBMIT_BUTTON';
                    if (/next|continue|proceed|forward|step/i.test(combined)) return 'NEXT_BUTTON';
                    return 'GENERIC_BUTTON';
                }

                // 14. Dropdown / Combobox
                if (tag === 'select' || el.getAttribute('role') === 'combobox' || el.getAttribute('role') === 'listbox') {
                    return 'DROPDOWN';
                }

                return tag === 'input' || tag === 'textarea' ? 'GENERIC_INPUT' : 'GENERIC_BUTTON';
            }

            elements.forEach(function(el) {
                if (el.type === 'hidden' || el.style.display === 'none' || el.style.visibility === 'hidden') return;
                if (el.closest && el.closest('.aura-hud')) return;

                var rect = el.getBoundingClientRect();
                if (rect.width === 0 && rect.height === 0) return;

                var tag = el.tagName.toLowerCase();
                var type = el.type || '';
                var name = el.name || '';
                var id = el.id || '';
                var placeholder = el.placeholder || '';
                var ariaLabel = el.getAttribute('aria-label') || '';
                var text = (el.innerText || el.textContent || el.value || '').trim();
                if (text.length > 35) text = text.substring(0, 35) + '...';

                // Skip empty generic links if we already have plenty of elements
                if (tag === 'a' && !text && !ariaLabel) return;

                var isInput = (tag === 'input' || tag === 'textarea' || tag === 'select' || el.getAttribute('role') === 'combobox');
                var isBtn = (tag === 'button' || type === 'submit' || el.getAttribute('role') === 'button' || tag === 'a');

                var semantic = classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel);

                // Highlight interactable elements with glowing outline
                el.style.outline = '2px dashed #06B6D4';
                el.style.outlineOffset = '2px';

                // Collect options for select / dropdowns
                var options = [];
                if (tag === 'select' && el.options) {
                    for (var o = 0; o < Math.min(el.options.length, 12); o++) {
                        var optText = (el.options[o].text || el.options[o].value || '').trim();
                        if (optText) options.push(optText);
                    }
                }

                items.push({
                    tagName: tag,
                    type: type,
                    nameAttr: name,
                    idAttr: id,
                    placeholder: placeholder,
                    text: text,
                    selector: getCssSelector(el),
                    pageUrl: currentPageUrl,
                    isInput: isInput,
                    isButton: isBtn,
                    semanticType: semantic,
                    detectedOptions: options
                });
            });

            if (window.AuraAutomation && window.AuraAutomation.onScanResults) {
                window.AuraAutomation.onScanResults(JSON.stringify(items));
            }
        })();
    """.trimIndent()

    /**
     * Clear DOM scan visual outlines.
     */
    val CLEAR_SCAN_HIGHLIGHTS = """
        (function() {
            var elements = document.querySelectorAll(
                'input, button, [role="button"], [role="combobox"], [role="listbox"], textarea, select, form, a, .iti__selected-flag, [data-dial-code], [data-datepicker], .dropzone'
            );
            elements.forEach(function(el) {
                el.style.outline = '';
                el.style.outlineOffset = '';
            });
        })();
    """.trimIndent()

    /**
     * Simulate a human click on an element by CSS selector or fallback text.
     */
    fun createClickScript(selector: String): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escaped');
                if (!el) {
                    var buttons = document.querySelectorAll('button, [role="button"], input[type="submit"], a');
                    for (var i = 0; i < buttons.length; i++) {
                        if ((buttons[i].innerText || buttons[i].value || '').trim().toLowerCase().indexOf('$escaped'.toLowerCase()) >= 0) {
                            el = buttons[i];
                            break;
                        }
                    }
                }
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.focus();
                    ['mousedown', 'click', 'mouseup'].forEach(function(evt) {
                        var event = new MouseEvent(evt, { bubbles: true, cancelable: true, view: window });
                        el.dispatchEvent(event);
                    });
                    if (el.tagName.toLowerCase() === 'input' && (el.type === 'checkbox' || el.type === 'radio')) {
                        el.checked = !el.checked;
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    return 'SUCCESS';
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }

    /**
     * Input text into modern fields with full event dispatching for React, Vue, Angular, and controlled inputs.
     */
    fun createInputScript(selector: String, value: String, clearFirst: Boolean): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedValue = value.replace("'", "\\'").replace("\n", "\\n")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector');
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.focus();

                    var nativeSetter = Object.getOwnPropertyDescriptor(
                        el.tagName.toLowerCase() === 'textarea' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype,
                        'value'
                    );

                    if ($clearFirst) {
                        if (nativeSetter && nativeSetter.set) {
                            nativeSetter.set.call(el, '');
                        } else {
                            el.value = '';
                        }
                        if (el._valueTracker) el._valueTracker.setValue('');
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                    }

                    if (nativeSetter && nativeSetter.set) {
                        nativeSetter.set.call(el, '$escapedValue');
                    } else {
                        el.value = '$escapedValue';
                    }

                    if (el._valueTracker) {
                        el._valueTracker.setValue('$escapedValue');
                    }

                    el.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'a' }));
                    el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
                    el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'a' }));
                    el.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));
                    el.dispatchEvent(new Event('blur', { bubbles: true }));
                    return 'SUCCESS';
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }

    /**
     * Modern Dropdown Handler.
     */
    fun createSelectDropdownScript(selector: String, optionValueOrText: String): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedTarget = optionValueOrText.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector');
                if (!el) return 'NOT_FOUND';

                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                el.focus();

                var target = '$escapedTarget'.toLowerCase().trim();

                if (el.tagName.toLowerCase() === 'select') {
                    var found = false;
                    for (var i = 0; i < el.options.length; i++) {
                        var optText = (el.options[i].text || '').toLowerCase().trim();
                        var optVal = (el.options[i].value || '').toLowerCase().trim();
                        if (optText === target || optVal === target || optText.indexOf(target) >= 0 || optVal.indexOf(target) >= 0) {
                            el.selectedIndex = i;
                            el.value = el.options[i].value;
                            found = true;
                            break;
                        }
                    }
                    if (!found && el.options.length > 1) {
                        el.selectedIndex = 1;
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'SUCCESS';
                }

                el.click();
                setTimeout(function() {
                    var options = document.querySelectorAll('[role="option"], li, .dropdown-item, .select-option, [data-value]');
                    for (var j = 0; j < options.length; j++) {
                        var itemText = (options[j].innerText || options[j].textContent || options[j].getAttribute('data-value') || '').toLowerCase().trim();
                        if (itemText === target || itemText.indexOf(target) >= 0) {
                            options[j].click();
                            break;
                        }
                    }
                }, 300);

                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Modern Country Code & Phone Selector.
     */
    fun createSelectCountryCodeScript(selector: String, dialCode: String): String {
        val escapedSelector = selector.replace("'", "\\'")
        val cleanCode = dialCode.replace("+", "").trim()
        return """
            (function() {
                var el = document.querySelector('$escapedSelector') || 
                         document.querySelector('.iti__selected-flag, [data-dial-code], [name*="country_code"], #country_code, select[name*="code"]');
                
                if (!el) return 'NOT_FOUND';
                el.scrollIntoView({ behavior: 'smooth', block: 'center' });

                if (el.tagName.toLowerCase() === 'select') {
                    for (var i = 0; i < el.options.length; i++) {
                        var text = (el.options[i].text + ' ' + el.options[i].value).toLowerCase();
                        if (text.indexOf('$cleanCode') >= 0 || text.indexOf('+$cleanCode') >= 0) {
                            el.selectedIndex = i;
                            el.value = el.options[i].value;
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            return 'SUCCESS';
                        }
                    }
                }

                el.click();
                setTimeout(function() {
                    var items = document.querySelectorAll('.iti__country, [data-dial-code], [role="option"], li');
                    for (var k = 0; k < items.length; k++) {
                        var codeAttr = items[k].getAttribute('data-dial-code') || '';
                        var itemText = (items[k].innerText || items[k].textContent || '').trim();
                        if (codeAttr === '$cleanCode' || itemText.indexOf('+$cleanCode') >= 0) {
                            items[k].click();
                            break;
                        }
                    }
                }, 350);

                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Modern Date of Birth (DOB) Handler.
     */
    fun createSelectDateScript(selector: String, isoDate: String, day: String, month: String, year: String): String {
        val escapedSelector = selector.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escapedSelector') || 
                         document.querySelector('input[type="date"], [data-datepicker], input[name*="dob"], input[name*="birth"]');

                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    el.focus();

                    if (el.tagName.toLowerCase() === 'input' && el.type === 'date') {
                        el.value = '$isoDate';
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        return 'SUCCESS';
                    }

                    if (el.tagName.toLowerCase() === 'input') {
                        el.value = '$isoDate';
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                    }

                    el.click();
                    setTimeout(function() {
                        var dayCells = document.querySelectorAll('[role="gridcell"], .day, .calendar-day, td[data-day]');
                        for (var i = 0; i < dayCells.length; i++) {
                            if ((dayCells[i].innerText || '').trim() === '$day') {
                                dayCells[i].click();
                                break;
                            }
                        }
                    }, 300);

                    return 'SUCCESS';
                }

                var dayEl = document.querySelector('select[name*="day"], #day');
                var monthEl = document.querySelector('select[name*="month"], #month');
                var yearEl = document.querySelector('select[name*="year"], #year');

                if (dayEl) {
                    dayEl.value = '$day';
                    dayEl.dispatchEvent(new Event('change', { bubbles: true }));
                }
                if (monthEl) {
                    monthEl.value = '$month';
                    monthEl.dispatchEvent(new Event('change', { bubbles: true }));
                }
                if (yearEl) {
                    yearEl.value = '$year';
                    yearEl.dispatchEvent(new Event('change', { bubbles: true }));
                }

                return 'SUCCESS';
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
     * Form Submission / Next action.
     */
    fun createSubmitScript(selector: String): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                var el = document.querySelector('$escaped') || 
                         document.querySelector('button[type="submit"], input[type="submit"], button.btn-primary');
                if (el) {
                    if (el.tagName.toLowerCase() === 'form') {
                        el.submit();
                        return 'SUCCESS';
                    }
                    if (el.form) {
                        el.form.submit();
                        return 'SUCCESS';
                    }
                    el.click();
                    return 'SUCCESS';
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()
    }
}
