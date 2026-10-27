package com.example.automation

object AutomationScripts {

    /**
     * JavaScript injected to record user interactions on the page in NO-TYPE Tap-to-Grab mode.
     * - User does NOT type into input fields!
     * - Tapping any input, textarea, select, checkbox, radio, or button immediately grabs the element,
     *   binds it to the appropriate Data Sheet field key (e.g. {{email}}, {{username}}, {{password}}),
     *   blurs the input so the keyboard doesn't get in the way, and marks it "Ready to Fill / Click".
     */
    val RECORDING_INJECTION = """
        (function() {
            if (window._auraRecorderActive) return;
            window._auraRecorderActive = true;
            window._auraLastCapturedSelector = '';
            window._auraLastCapturedTime = 0;
            window._auraGrabCount = window._auraGrabCount || 0;

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

                var testId = el.getAttribute('data-testid') || el.getAttribute('data-test') || el.getAttribute('data-qa') || el.getAttribute('data-cy');
                if (testId) {
                    var testSel = '[' + (el.hasAttribute('data-testid') ? 'data-testid' : (el.hasAttribute('data-test') ? 'data-test' : (el.hasAttribute('data-qa') ? 'data-qa' : 'data-cy'))) + '="' + testId.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(testSel, el)) return testSel;
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

                // Build a guaranteed-unique DOM hierarchy path with :nth-of-type
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
                        var refEl = document.getElementById(ariaLabelledBy);
                        if (refEl) parts.push(refEl.innerText || refEl.textContent || '');
                    }
                    var prev = el.previousElementSibling;
                    if (prev && (prev.tagName === 'LABEL' || prev.tagName === 'SPAN' || prev.tagName === 'DIV')) {
                        var prevTxt = (prev.innerText || prev.textContent || '').trim();
                        if (prevTxt.length < 50) parts.push(prevTxt);
                    }
                } catch (e) {}
                return parts.join(' ').trim();
            }

            function detectFieldKey(el) {
                if (!el) return '';
                var type = (el.type || '').toLowerCase();
                var labelTxt = getAssociatedLabelText(el);
                var combined = (
                    (el.name || '') + ' ' +
                    (el.id || '') + ' ' +
                    (el.placeholder || '') + ' ' +
                    (el.getAttribute('aria-label') || '') + ' ' +
                    (el.autocomplete || '') + ' ' +
                    labelTxt
                ).toLowerCase();

                if (type === 'file' || /upload|resume|document|avatar|attachment|file|cv/i.test(combined)) return 'fileName';
                if (type === 'password' || /pass|pwd/i.test(combined)) {
                    if (/confirm|repeat|retype|verify|match|again/i.test(combined)) return 'confirmPassword';
                    return 'password';
                }
                if (type === 'email' || /email|e-mail|mail/i.test(combined)) return 'email';
                if (/user|username|login|handle|nick|account/i.test(combined)) return 'username';
                if (/country.*code|dial.*code|phone.*prefix/i.test(combined)) return 'countryCode';
                if (type === 'tel' || /phone|mobile|cell|telephone|whatsapp|contact.*num/i.test(combined)) return 'phoneNumber';
                if (/gender|sex/i.test(combined)) return 'gender';
                if (type === 'date' || /birth|dob|bday/i.test(combined)) return 'dob';
                if (/first.*name|fname|given.*name|forename/i.test(combined)) return 'firstName';
                if (/last.*name|lname|surname|family.*name/i.test(combined)) return 'lastName';
                if (/full.*name|^name$|your.*name|display.*name/i.test(combined)) return 'fullName';
                if (/zip|postal|pincode|pin.*code|postcode/i.test(combined)) return 'zipCode';
                if (/city|town|locality/i.test(combined)) return 'city';
                if (/state|province|region/i.test(combined)) return 'state';
                if (/address.*2|line.*2|apt|suite|unit/i.test(combined)) return 'addressLine2';
                if (/address|street|addr/i.test(combined)) return 'address';
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
                if (labelText.length > 28) labelText = labelText.substring(0, 28) + '...';

                if (type === 'checkbox' || role === 'checkbox' || role === 'switch') {
                    return 'Check: ' + (labelText || el.name || 'Checkbox');
                }
                if (type === 'radio' || role === 'radio') {
                    return 'Select Radio: ' + (labelText || el.value || el.name || 'Option');
                }
                if (fieldKey) {
                    return 'Fill ' + fieldKey + (labelText ? ' (' + labelText + ')' : '');
                }
                if (tag === 'button' || type === 'submit' || role === 'button') return 'Click Button: ' + (labelText || 'Submit');
                if (tag === 'select' || role === 'combobox') return 'Select Dropdown: ' + (labelText || 'Option');
                if (tag === 'input' || tag === 'textarea') return 'Fill Input: ' + (labelText || type || 'field');
                if (tag === 'a') return 'Click Link: ' + (labelText || 'Navigate');
                return 'Click ' + tag + (labelText ? ' "' + labelText + '"' : '');
            }

            function showGrabToastOnPage(el, text, color) {
                try {
                    var badge = document.createElement('div');
                    var rect = el.getBoundingClientRect();
                    badge.textContent = text;
                    badge.style.cssText = 'position:fixed;z-index:2147483647;background:' + (color || '#10B981') + ';color:#ffffff;font-family:-apple-system,BlinkMacSystemFont,sans-serif;font-size:11px;font-weight:700;padding:4px 8px;border-radius:6px;box-shadow:0 4px 12px rgba(0,0,0,0.3);pointer-events:none;transition:opacity 0.3s ease;';
                    var topPos = Math.max(8, rect.top - 28);
                    var leftPos = Math.max(8, Math.min(window.innerWidth - 180, rect.left));
                    badge.style.top = topPos + 'px';
                    badge.style.left = leftPos + 'px';
                    document.body.appendChild(badge);
                    setTimeout(function() {
                        badge.style.opacity = '0';
                        setTimeout(function() { if (badge.parentNode) badge.parentNode.removeChild(badge); }, 300);
                    }, 1300);
                } catch (e) {}
            }

            function reportElementCapture(e) {
                var el = e.target;
                if (!el || !el.tagName) return;
                if (el.closest && el.closest('.aura-hud')) return;

                // Resolve label or custom wrapper to the target input if clicked
                var labelEl = el.closest ? el.closest('label') : null;
                var linkedInput = null;
                if (labelEl) {
                    linkedInput = labelEl.querySelector('input, select, textarea') ||
                                 (labelEl.htmlFor ? document.getElementById(labelEl.htmlFor) : null);
                }

                var interactive = linkedInput || (el.closest ? el.closest('input, textarea, select, button, a, [role="button"], [role="checkbox"], [role="switch"], [role="radio"], [role="combobox"], label') : null) || el;
                var tag = interactive.tagName.toLowerCase();
                var type = (interactive.type || '').toLowerCase();
                var role = (interactive.getAttribute('role') || '').toLowerCase();
                var selector = getCssSelector(interactive);
                if (!selector) return;

                var now = Date.now();
                if (selector === window._auraLastCapturedSelector && (now - window._auraLastCapturedTime) < 1000) {
                    return;
                }
                window._auraLastCapturedSelector = selector;
                window._auraLastCapturedTime = now;

                var fieldKey = detectFieldKey(interactive);
                var action = 'CLICK';

                if (type === 'checkbox' || role === 'checkbox' || role === 'switch') {
                    action = 'CHECKBOX';
                    fieldKey = '';
                    showGrabToastOnPage(interactive, '✓ Grabbed Checkbox (Ready)', '#06B6D4');
                } else if (type === 'radio' || role === 'radio') {
                    action = 'RADIO';
                    fieldKey = '';
                    showGrabToastOnPage(interactive, '✓ Grabbed Radio (Ready)', '#8B5CF6');
                } else if (tag === 'select' || role === 'combobox') {
                    if (!fieldKey) fieldKey = 'gender';
                    action = (fieldKey === 'countryCode') ? 'SELECT_COUNTRY_CODE' : 'SELECT_DROPDOWN';
                    showGrabToastOnPage(interactive, '✓ Grabbed Dropdown -> {{' + fieldKey + '}}', '#D946EF');
                } else if (tag === 'input' || tag === 'textarea') {
                    if (type === 'submit' || type === 'button' || type === 'image') {
                        action = 'SUBMIT';
                        fieldKey = '';
                        // Prevent premature form submission while grabbing elements
                        try { e.preventDefault(); e.stopPropagation(); } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed Submit Button (Ready)', '#10B981');
                    } else if (type === 'file') {
                        action = 'UPLOAD_FILE';
                        if (!fieldKey) fieldKey = 'fileName';
                        try { e.preventDefault(); } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed File Upload -> {{' + fieldKey + '}}', '#F59E0B');
                    } else if (type === 'date' || type === 'datetime-local') {
                        action = 'SELECT_DATE';
                        if (!fieldKey) fieldKey = 'dob';
                        showGrabToastOnPage(interactive, '✓ Grabbed Date -> {{' + fieldKey + '}}', '#0284C7');
                    } else {
                        action = 'INPUT';
                        if (!fieldKey) fieldKey = 'email';
                        // NO-TYPE MODE: User does not type! Immediately blur so soft keyboard closes
                        // and show visual indicator that this field is grabbed and ready for Data Sheet fill
                        var origPlaceholder = interactive.getAttribute('placeholder') || '';
                        interactive.setAttribute('placeholder', '[Ready: {{' + fieldKey + '}}] ' + origPlaceholder);
                        setTimeout(function() {
                            try { interactive.blur(); } catch (err) {}
                        }, 60);
                        showGrabToastOnPage(interactive, '✓ Grabbed Field -> {{' + fieldKey + '}} (No typing needed)', '#3B82F6');
                    }
                } else if (tag === 'button' || role === 'button') {
                    var btnTxt = (interactive.innerText || interactive.value || '').toLowerCase();
                    if (type === 'submit' || /submit|sign.*up|register|create|login|log.*in|sign.*in|continue|next|finish|send/i.test(btnTxt)) {
                        action = 'SUBMIT';
                        // Prevent empty form submit while user is just grabbing the button
                        try { e.preventDefault(); e.stopPropagation(); } catch (err) {}
                        showGrabToastOnPage(interactive, '✓ Grabbed Submit Button (Ready)', '#10B981');
                    } else {
                        action = 'CLICK';
                        showGrabToastOnPage(interactive, '✓ Grabbed Button Click (Ready)', '#10B981');
                    }
                } else {
                    showGrabToastOnPage(interactive, '✓ Grabbed Element (Ready)', '#10B981');
                }

                // Persistent green outline to show element is captured and ready
                interactive.style.outline = '2px solid #10B981';
                interactive.style.outlineOffset = '2px';

                var desc = getElementDescription(interactive, fieldKey);
                if (window.AuraAutomation && window.AuraAutomation.onUserAction) {
                    window.AuraAutomation.onUserAction(action, selector, desc, fieldKey || '');
                }
            }

            document.addEventListener('click', function(e) {
                reportElementCapture(e);
            }, true);
        })();
    """.trimIndent()

    /**
     * Stronger DOM Scanner:
     * - Scans main DOM + open Shadow DOMs
     * - Produces guaranteed-unique CSS selectors
     * - Inspects labels, aria attributes, and surrounding context
     * - Orders elements logically (Inputs & Selects -> Checkboxes & Radios -> Submit / Next Buttons)
     *   so grabbed elements are immediately ready to run!
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

                var testId = el.getAttribute('data-testid') || el.getAttribute('data-test') || el.getAttribute('data-qa') || el.getAttribute('data-cy');
                if (testId) {
                    var attrName = el.hasAttribute('data-testid') ? 'data-testid' : (el.hasAttribute('data-test') ? 'data-test' : (el.hasAttribute('data-qa') ? 'data-qa' : 'data-cy'));
                    var testSel = '[' + attrName + '="' + testId.replace(/"/g, '\\"') + '"]';
                    if (isUniqueSelector(testSel, el)) return testSel;
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
                    var prev = el.previousElementSibling;
                    if (prev && (prev.tagName === 'LABEL' || prev.tagName === 'SPAN' || prev.tagName === 'DIV')) {
                        var prevTxt = (prev.innerText || prev.textContent || '').trim();
                        if (prevTxt.length < 50) parts.push(prevTxt);
                    }
                } catch (e) {}
                return parts.join(' ').trim();
            }

            function classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel, labelText) {
                var combined = (name + ' ' + id + ' ' + placeholder + ' ' + text + ' ' + ariaLabel + ' ' + labelText + ' ' + (el.autocomplete || '')).toLowerCase();

                if (type === 'file' || (tag === 'input' && /upload|resume|document|avatar|attachment|file|cv/i.test(combined))) {
                    return 'FILE_UPLOAD';
                }
                if (type === 'password' || ((tag === 'input') && /pass|pwd/i.test(combined))) {
                    if (/confirm|repeat|retype|re-enter|verify|match|again|pass.*2/i.test(combined)) {
                        return 'CONFIRM_PASSWORD';
                    }
                    return 'PASSWORD';
                }
                if (type === 'email' || ((tag === 'input') && /email|e-mail|mail/i.test(combined))) {
                    return 'EMAIL';
                }
                if ((tag === 'input' || tag === 'textarea') && /user|username|login|handle|nick|account/i.test(combined)) {
                    return 'USERNAME';
                }
                if (/country.*code|dial.*code|phone.*prefix|dialing|iti__selected-flag/i.test(combined) ||
                    el.classList.contains('iti__selected-flag') ||
                    el.hasAttribute('data-dial-code')) {
                    return 'COUNTRY_CODE';
                }
                if (type === 'tel' || ((tag === 'input') && /phone|mobile|cell|telephone|contact.*num|whatsapp/i.test(combined))) {
                    return 'PHONE';
                }
                if (/gender|sex/i.test(combined) ||
                    (tag === 'select' && /male|female/i.test(el.innerText || '')) ||
                    (type === 'radio' && /male|female/i.test(combined))) {
                    return 'GENDER';
                }
                if (type === 'date' || type === 'datetime-local' || /birth|dob|bday|date.*of.*birth/i.test(combined) || el.hasAttribute('data-datepicker')) {
                    if (/day/i.test(combined)) return 'DOB_DAY';
                    if (/month/i.test(combined)) return 'DOB_MONTH';
                    if (/year/i.test(combined)) return 'DOB_YEAR';
                    return 'DOB';
                }
                if (/^(day|birth_day|dob_day)$/i.test(name) || /^(day)$/i.test(id)) return 'DOB_DAY';
                if (/^(month|birth_month|dob_month)$/i.test(name) || /^(month)$/i.test(id)) return 'DOB_MONTH';
                if (/^(year|birth_year|dob_year)$/i.test(name) || /^(year)$/i.test(id)) return 'DOB_YEAR';

                if ((tag === 'input') && /first.*name|fname|given.*name|forename/i.test(combined)) return 'FIRST_NAME';
                if ((tag === 'input') && /last.*name|lname|surname|family.*name/i.test(combined)) return 'LAST_NAME';
                if ((tag === 'input') && /full.*name|^name$|your.*name|display.*name/i.test(combined)) return 'FULL_NAME';
                if ((tag === 'input') && /zip|postal|pincode|pin.*code|postcode/i.test(combined)) return 'ZIP_PIN';
                if ((tag === 'input' || tag === 'select') && /city|town|locality/i.test(combined)) return 'CITY';
                if ((tag === 'input' || tag === 'select') && /state|province|region/i.test(combined)) return 'STATE';
                if ((tag === 'input') && /address.*2|line.*2|apt|suite|unit/i.test(combined)) return 'ADDRESS_LINE_2';
                if ((tag === 'input' || tag === 'textarea') && /address|street|addr/i.test(combined)) return 'ADDRESS';

                if (type === 'checkbox' || el.getAttribute('role') === 'checkbox' || el.getAttribute('role') === 'switch') {
                    if (/term|agree|policy|accept|consent|privacy/i.test(combined)) {
                        return 'CHECKBOX_TERMS';
                    }
                    return 'CHECKBOX';
                }
                if (type === 'radio' || el.getAttribute('role') === 'radio') {
                    return 'RADIO_OPTION';
                }
                if (tag === 'button' || type === 'submit' || type === 'button' || el.getAttribute('role') === 'button') {
                    if (type === 'submit' || /submit|sign.*up|create.*account|register|finish|done|log.*in|sign.*in|send|save|apply|search/i.test(combined)) return 'SUBMIT_BUTTON';
                    if (/next|continue|proceed|forward|step/i.test(combined)) return 'NEXT_BUTTON';
                    return 'GENERIC_BUTTON';
                }
                if (tag === 'select' || el.getAttribute('role') === 'combobox' || el.getAttribute('role') === 'listbox') {
                    return 'DROPDOWN';
                }
                return (tag === 'input' || tag === 'textarea') ? 'GENERIC_INPUT' : 'GENERIC_BUTTON';
            }

            var elements = document.querySelectorAll(
                'input, textarea, select, button, [role="button"], [role="combobox"], [role="checkbox"], [role="switch"], [role="radio"], .iti__selected-flag, [data-dial-code], [data-datepicker], .dropzone'
            );

            var seenSelectors = {};

            function processCandidate(el, allowHiddenStepFields) {
                if (!el || el.disabled) return;
                var tag = el.tagName.toLowerCase();
                var type = (el.type || '').toLowerCase();
                if (type === 'hidden') return;
                if (el.closest && el.closest('.aura-hud')) return;

                var name = el.name || '';
                var id = el.id || '';
                // Skip internal CSRF / honeypot / analytics fields
                if (/csrf|token|authenticity|honeypot|timestamp|_utf8|octolytics/i.test(name + ' ' + id)) return;

                var style = window.getComputedStyle(el);
                var rect = el.getBoundingClientRect();
                var isCustomCheckOrFile = (type === 'checkbox' || type === 'radio' || type === 'file');

                if (!allowHiddenStepFields) {
                    if (!isCustomCheckOrFile) {
                        if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return;
                        if (rect.width < 4 || rect.height < 4) return;
                    } else {
                        // For custom-styled checkboxes/radios/files, allow opacity:0 as long as it or its parent/label is in layout
                        var parentRect = el.parentElement ? el.parentElement.getBoundingClientRect() : rect;
                        if (rect.width < 1 && parentRect.width < 4) return;
                    }
                }

                var placeholder = el.placeholder || '';
                var ariaLabel = el.getAttribute('aria-label') || '';
                var labelText = getAssociatedLabelText(el);
                var text = (el.innerText || el.textContent || el.value || labelText || '').trim().replace(/\s+/g, ' ');
                if (text.length > 35) text = text.substring(0, 35) + '...';

                var semantic = classifySemanticType(el, tag, type, name, id, placeholder, text, ariaLabel, labelText);

                if (allowHiddenStepFields) {
                    // In fallback pass for animated/multi-step forms (like GitHub signup), only include clear semantic form fields & submit/next buttons
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

                var isInput = (tag === 'input' && type !== 'submit' && type !== 'button' && type !== 'checkbox' && type !== 'radio') ||
                              tag === 'textarea' || tag === 'select' || el.getAttribute('role') === 'combobox';
                var isBtn = (tag === 'button' || type === 'submit' || type === 'button' || el.getAttribute('role') === 'button');

                // Priority for smart workflow ordering:
                // 1 = Inputs & Dropdowns, 2 = Checkboxes & Radios, 3 = Next/Submit Buttons, 4 = Generic Buttons
                var orderPriority = 4;
                if (isInput) orderPriority = 1;
                else if (semantic === 'CHECKBOX' || semantic === 'CHECKBOX_TERMS' || semantic === 'RADIO_OPTION') orderPriority = 2;
                else if (semantic === 'SUBMIT_BUTTON' || semantic === 'NEXT_BUTTON') orderPriority = 3;

                // Highlight grabbed element
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

            // Pass 1: Visible & custom-checkbox elements
            elements.forEach(function(el) {
                processCandidate(el, false);
            });

            // Pass 2: If page uses multi-step wizard / delayed reveal (e.g. GitHub signup where #email/#password/#login are in step containers),
            // also include semantic form fields from the DOM so the full signup flow is captured immediately!
            var foundInputCount = rawItems.filter(function(item) { return item.isInput; }).length;
            if (foundInputCount < 2) {
                elements.forEach(function(el) {
                    processCandidate(el, true);
                });
            }

            // Sort elements into logical execution order: Inputs (top-to-bottom) -> Checkboxes/Radios -> Submit/Next buttons
            rawItems.sort(function(a, b) {
                if (a._priority !== b._priority) return a._priority - b._priority;
                return a._top - b._top;
            });

            if (window.AuraAutomation && window.AuraAutomation.onScanResults) {
                window.AuraAutomation.onScanResults(JSON.stringify(rawItems));
            }
        })();
    """.trimIndent()

    /**
     * Clear DOM scan visual outlines.
     */
    val CLEAR_SCAN_HIGHLIGHTS = """
        (function() {
            var elements = document.querySelectorAll(
                'input, button, [role="button"], [role="combobox"], [role="listbox"], [role="checkbox"], [role="switch"], [role="radio"], textarea, select, form, a, .iti__selected-flag, [data-dial-code], [data-datepicker], .dropzone'
            );
            elements.forEach(function(el) {
                el.style.outline = '';
                el.style.outlineOffset = '';
            });
        })();
    """.trimIndent()

    /**
     * Shared JS helper snippet for deep element lookup (CSS selector -> Shadow DOM -> fallback by fieldKey/description)
     */
    private fun jsFindElementHelper(escapedSelector: String, fallbackHint: String = ""): String {
        val escapedHint = fallbackHint.replace("'", "\\'").lowercase()
        return """
            function queryDeep(root, sel) {
                try {
                    var found = root.querySelector(sel);
                    if (found) return found;
                    var all = root.querySelectorAll('*');
                    for (var i = 0; i < all.length; i++) {
                        if (all[i].shadowRoot) {
                            var sub = queryDeep(all[i].shadowRoot, sel);
                            if (sub) return sub;
                        }
                    }
                } catch (e) {}
                return null;
            }
            var el = queryDeep(document, '$escapedSelector');
            if (!el && '$escapedHint') {
                var hint = '$escapedHint';
                var candidates = document.querySelectorAll('input, textarea, select, button, [role="button"], [role="checkbox"], [role="radio"], a, label');
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
     * Simulate a full human pointer/mouse click on an element by CSS selector or fallback text.
     */
    fun createClickScript(selector: String, fallbackHint: String = ""): String {
        val escaped = selector.replace("'", "\\'")
        return """
            (function() {
                ${jsFindElementHelper(escaped, fallbackHint)}
                if (!el) return 'NOT_FOUND';

                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                try { el.focus(); } catch (e) {}

                ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(evt) {
                    var event = new MouseEvent(evt, { bubbles: true, cancelable: true, view: window });
                    el.dispatchEvent(event);
                });
                if (typeof el.click === 'function') {
                    try { el.click(); } catch (e) {}
                }
                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Dedicated script to reliably check / uncheck checkboxes and select radio buttons
     * across vanilla HTML, React, Angular, Vue, and custom ARIA components without double-toggling.
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
                try { target.focus(); } catch (e) {}

                var shouldCheck = $desiredState;
                var isRadio = $isRadio;
                var targetState = isRadio ? true : shouldCheck;

                if (input && (input.type === 'checkbox' || input.type === 'radio')) {
                    if (input.checked !== targetState) {
                        // First try native click which triggers React/Vue/Angular synthetic events naturally
                        try { input.click(); } catch (e) {}
                    }
                    // If still not in targetState, force via prototype descriptor
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
                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Input text into modern fields with full event dispatching for React 16-19, Vue, Angular, and contenteditable.
     */
    fun createInputScript(selector: String, value: String, clearFirst: Boolean, fallbackHint: String = ""): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedValue = value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        return """
            (function() {
                ${jsFindElementHelper(escapedSelector, fallbackHint)}
                if (!el) return 'NOT_FOUND';

                // If selector matched a wrapper/label, drill down to actual input/textarea
                if (el.tagName.toLowerCase() !== 'input' && el.tagName.toLowerCase() !== 'textarea' && !el.isContentEditable) {
                    var innerInput = el.querySelector('input, textarea, [contenteditable="true"]');
                    if (innerInput) el = innerInput;
                }

                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                try { el.focus(); } catch (e) {}

                if (el.isContentEditable) {
                    if ($clearFirst) el.innerText = '';
                    el.innerText = '$escapedValue';
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
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
                el.dispatchEvent(new Event('blur', { bubbles: true }));
                return 'SUCCESS';
            })();
        """.trimIndent()
    }

    /**
     * Modern Dropdown Handler (supports native <select> and custom ARIA/React select menus).
     */
    fun createSelectDropdownScript(selector: String, optionValueOrText: String, fallbackHint: String = ""): String {
        val escapedSelector = selector.replace("'", "\\'")
        val escapedTarget = optionValueOrText.replace("'", "\\'")
        return """
            (function() {
                ${jsFindElementHelper(escapedSelector, fallbackHint)}
                if (!el) return 'NOT_FOUND';

                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                try { el.focus(); } catch (e) {}

                var target = '$escapedTarget'.toLowerCase().trim();

                if (el.tagName.toLowerCase() === 'select') {
                    var found = false;
                    for (var i = 0; i < el.options.length; i++) {
                        var optText = (el.options[i].text || '').toLowerCase().trim();
                        var optVal = (el.options[i].value || '').toLowerCase().trim();
                        if (optText === target || optVal === target || (target && (optText.indexOf(target) >= 0 || optVal.indexOf(target) >= 0))) {
                            el.selectedIndex = i;
                            el.value = el.options[i].value;
                            found = true;
                            break;
                        }
                    }
                    if (!found && el.options.length > 1) {
                        el.selectedIndex = 1;
                        el.value = el.options[1].value;
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'SUCCESS';
                }

                el.click();
                setTimeout(function() {
                    var options = document.querySelectorAll('[role="option"], li, .dropdown-item, .select-option, [data-value]');
                    var matched = false;
                    for (var j = 0; j < options.length; j++) {
                        var itemText = (options[j].innerText || options[j].textContent || options[j].getAttribute('data-value') || '').toLowerCase().trim();
                        if (target && (itemText === target || itemText.indexOf(target) >= 0)) {
                            options[j].click();
                            matched = true;
                            break;
                        }
                    }
                    if (!matched && options.length > 0) {
                        options[0].click();
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
                    try { el.focus(); } catch (e) {}

                    var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                    if (nativeSetter && nativeSetter.set) {
                        nativeSetter.set.call(el, '$isoDate');
                    } else {
                        el.value = '$isoDate';
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));

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
                    try { el.focus(); } catch (e) {}
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
