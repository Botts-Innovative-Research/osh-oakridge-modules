(function () {
  'use strict';

  function base64UrlToBytes(b64url) {
    var base64 = b64url.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4 !== 0) {
      base64 += '=';
    }
    var raw = window.atob(base64);
    var out = new Uint8Array(raw.length);
    for (var i = 0; i < raw.length; i++) {
      out[i] = raw.charCodeAt(i);
    }
    return out;
  }

  function bytesToBase64Url(buffer) {
    var bytes = new Uint8Array(buffer);
    var bin = '';
    for (var i = 0; i < bytes.length; i++) {
      bin += String.fromCharCode(bytes[i]);
    }
    return window.btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }

  function toPublicKeyRequestOptions(serverOpts) {
    if (window.PublicKeyCredential && typeof window.PublicKeyCredential.parseRequestOptionsFromJSON === 'function') {
      return window.PublicKeyCredential.parseRequestOptionsFromJSON(serverOpts);
    }
    var out = {
      challenge: base64UrlToBytes(serverOpts.challenge),
      rpId: serverOpts.rpId,
      timeout: serverOpts.timeout || 120000,
      userVerification: serverOpts.userVerification || 'preferred'
    };
    if (Array.isArray(serverOpts.allowCredentials) && serverOpts.allowCredentials.length > 0) {
      out.allowCredentials = serverOpts.allowCredentials.map(function (cred) {
        var mapped = {
          type: cred.type || 'public-key',
          id: base64UrlToBytes(cred.id)
        };
        if (Array.isArray(cred.transports)) {
          mapped.transports = cred.transports;
        }
        return mapped;
      });
    }
    return out;
  }

  function toPublicKeyCreationOptions(serverOpts) {
    if (window.PublicKeyCredential && typeof window.PublicKeyCredential.parseCreationOptionsFromJSON === 'function') {
      return window.PublicKeyCredential.parseCreationOptionsFromJSON(serverOpts);
    }
    var out = {
      challenge: base64UrlToBytes(serverOpts.challenge),
      rp: {
        id: serverOpts.rp.id,
        name: serverOpts.rp.name
      },
      user: {
        id: base64UrlToBytes(serverOpts.user.id),
        name: serverOpts.user.name,
        displayName: serverOpts.user.displayName
      },
      pubKeyCredParams: serverOpts.pubKeyCredParams,
      timeout: serverOpts.timeout || 120000,
      attestation: serverOpts.attestation || 'none',
      authenticatorSelection: serverOpts.authenticatorSelection || {
        residentKey: 'preferred',
        userVerification: 'preferred'
      }
    };
    if (Array.isArray(serverOpts.excludeCredentials) && serverOpts.excludeCredentials.length > 0) {
      out.excludeCredentials = serverOpts.excludeCredentials.map(function (cred) {
        var mapped = {
          type: cred.type || 'public-key',
          id: base64UrlToBytes(cred.id)
        };
        if (Array.isArray(cred.transports)) {
          mapped.transports = cred.transports;
        }
        return mapped;
      });
    }
    return out;
  }

  function credentialAssertionToJson(cred) {
    if (cred && typeof cred.toJSON === 'function') {
      return cred.toJSON();
    }
    var resp = cred.response;
    return {
      id: cred.id,
      rawId: bytesToBase64Url(cred.rawId),
      type: cred.type,
      authenticatorAttachment: cred.authenticatorAttachment || null,
      response: {
        clientDataJSON: bytesToBase64Url(resp.clientDataJSON),
        authenticatorData: bytesToBase64Url(resp.authenticatorData),
        signature: bytesToBase64Url(resp.signature),
        userHandle: resp.userHandle ? bytesToBase64Url(resp.userHandle) : null
      }
    };
  }

  function credentialAttestationToJson(cred) {
    if (cred && typeof cred.toJSON === 'function') {
      return cred.toJSON();
    }
    var resp = cred.response;
    var transports = typeof resp.getTransports === 'function' ? resp.getTransports() : [];
    return {
      id: cred.id,
      rawId: bytesToBase64Url(cred.rawId),
      type: cred.type,
      authenticatorAttachment: cred.authenticatorAttachment || null,
      response: {
        clientDataJSON: bytesToBase64Url(resp.clientDataJSON),
        attestationObject: bytesToBase64Url(resp.attestationObject),
        transports: transports
      }
    };
  }

  function wirePasswordToggle(toggleId, inputId) {
    var toggle = document.getElementById(toggleId);
    var input = document.getElementById(inputId);
    if (!toggle || !input) return;
    var showLabel = toggle.getAttribute('data-show-label') || 'Show';
    var hideLabel = toggle.getAttribute('data-hide-label') || 'Hide';
    toggle.hidden = false;
    toggle.addEventListener('click', function () {
      var showing = input.type === 'text';
      input.type = showing ? 'password' : 'text';
      toggle.setAttribute('aria-pressed', String(!showing));
      toggle.textContent = showing ? showLabel : hideLabel;
    });
  }

  wirePasswordToggle('toggle-password', 'current-password');
  wirePasswordToggle('toggle-new-password', 'new-password');

  if (!window.PublicKeyCredential || !navigator.credentials) {
    return;
  }

  // -------------------------------------------------------------------------
  // Flow A: One-Tap Enrollment Page (/login/enroll?token=...)
  // -------------------------------------------------------------------------
  var enrollPasskeySection = document.getElementById('enroll-passkey-section');
  var enrollPasskeyBtn = document.getElementById('enroll-passkey-btn');
  var enrollTokenInput = document.getElementById('enroll-token');
  var enrollPasskeyNameInput = document.getElementById('enroll-passkey-name');
  var enrollStatusBox = document.getElementById('enroll-passkey-status');
  var keepSignedInCheckbox = document.getElementById('keep-signed-in');
  var continueInput = document.querySelector('input[name="continue"]');

  function isKeepSignedInSelected() {
    return keepSignedInCheckbox ? Boolean(keepSignedInCheckbox.checked) : true;
  }

  function getContinueTarget() {
    return continueInput && continueInput.value ? continueInput.value : '/';
  }

  if (enrollPasskeyBtn && enrollTokenInput) {
    if (enrollPasskeySection) {
      enrollPasskeySection.hidden = false;
    }
    enrollPasskeyBtn.addEventListener('click', async function () {
      if (enrollStatusBox) {
        enrollStatusBox.textContent = '';
        enrollStatusBox.hidden = true;
      }
      enrollPasskeyBtn.disabled = true;
      try {
        var tokenVal = enrollTokenInput.value;
        var optResp = await fetch('/login/enroll/passkey/options', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            token: tokenVal,
            continue: getContinueTarget(),
            keepSignedIn: isKeepSignedInSelected()
          })
        });
        var serverOpts = await optResp.json();
        if (!optResp.ok) {
          throw new Error((serverOpts && serverOpts.error) || 'Could not start passkey setup.');
        }

        var publicKey = toPublicKeyCreationOptions(serverOpts);
        var created = await navigator.credentials.create({ publicKey: publicKey });
        if (!created) return;

        var verifyResp = await fetch('/login/enroll/passkey/verify', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            token: tokenVal,
            challenge: serverOpts.challenge,
            name: enrollPasskeyNameInput ? enrollPasskeyNameInput.value.trim() : '',
            keepSignedIn: isKeepSignedInSelected(),
            continue: getContinueTarget(),
            credential: credentialAttestationToJson(created)
          })
        });
        var result = await verifyResp.json();
        if (!verifyResp.ok || !result || !result.redirectUrl) {
          throw new Error((result && result.error) || 'Passkey enrollment failed.');
        }
        window.location.assign(result.redirectUrl);
      } catch (err) {
        if (err && (err.name === 'NotAllowedError' || err.name === 'AbortError')) {
          return;
        }
        if (enrollStatusBox) {
          enrollStatusBox.textContent = (err && err.message) || enrollPasskeyBtn.getAttribute('data-error-msg') || 'Passkey setup could not be completed.';
          enrollStatusBox.hidden = false;
        }
      } finally {
        enrollPasskeyBtn.disabled = false;
      }
    });
  }

  // -------------------------------------------------------------------------
  // Flow B: Standard Login Page (/login)
  // -------------------------------------------------------------------------
  var passkeySection = document.getElementById('passkey-section');
  var passkeyBtn = document.getElementById('passkey-signin-btn');
  var statusBox = document.getElementById('passkey-status');
  var usernameInput = document.getElementById('username');

  if (!passkeyBtn) {
    return;
  }

  var conditionalAbort = null;

  function showStatus(msg) {
    if (!statusBox) return;
    statusBox.textContent = msg;
    statusBox.hidden = !msg;
  }

  async function fetchOptions(usernameHint) {
    var resp = await fetch('/login/webauthn/options', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: usernameHint || '',
        continue: getContinueTarget(),
        keepSignedIn: isKeepSignedInSelected()
      })
    });
    if (!resp.ok) {
      throw new Error('Failed to load passkey options');
    }
    return await resp.json();
  }

  async function verifyAssertion(credential, serverOpts) {
    var payload = {
      challenge: serverOpts.challenge,
      continue: getContinueTarget(),
      keepSignedIn: isKeepSignedInSelected(),
      credential: credentialAssertionToJson(credential)
    };
    var resp = await fetch('/login/webauthn/verify', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (resp.status === 404) {
      if (typeof window.PublicKeyCredential.signalUnknownCredential === 'function') {
        try {
          await window.PublicKeyCredential.signalUnknownCredential({
            rpId: serverOpts.rpId,
            credentialId: credential.id
          });
        } catch (err) {
          console.warn('Passkey signalUnknownCredential failed:', err);
        }
      }
      var notFoundMsg = passkeyBtn.getAttribute('data-unknown-msg') || 'Passkey is no longer registered on this node.';
      showStatus(notFoundMsg);
      return;
    }
    var result = null;
    try {
      result = await resp.json();
    } catch (_) {
      result = null;
    }
    if (!resp.ok || !result || !result.redirectUrl) {
      var errMsg = (result && result.error) || passkeyBtn.getAttribute('data-error-msg') || 'Passkey verification failed.';
      showStatus(errMsg);
      return;
    }
    window.location.assign(result.redirectUrl);
  }

  async function startConditionalPasskey() {
    try {
      var serverOpts = await fetchOptions('');
      var publicKey = toPublicKeyRequestOptions(serverOpts);
      conditionalAbort = new AbortController();
      var cred = await navigator.credentials.get({
        publicKey: publicKey,
        mediation: 'conditional',
        signal: conditionalAbort.signal
      });
      if (cred) {
        await verifyAssertion(cred, serverOpts);
      }
    } catch (err) {
      if (err && err.name === 'AbortError') return;
    }
  }

  async function initPasskeys() {
    var conditionalSupported = false;
    try {
      if (typeof window.PublicKeyCredential.getClientCapabilities === 'function') {
        var caps = await window.PublicKeyCredential.getClientCapabilities();
        conditionalSupported = Boolean(caps && caps.conditionalGet);
      } else if (typeof window.PublicKeyCredential.isConditionalMediationAvailable === 'function') {
        conditionalSupported = await window.PublicKeyCredential.isConditionalMediationAvailable();
      }
    } catch (_) {
      conditionalSupported = false;
    }

    if (passkeySection) {
      passkeySection.hidden = false;
    }

    if (conditionalSupported) {
      startConditionalPasskey();
    }

    passkeyBtn.addEventListener('click', async function () {
      showStatus('');
      if (conditionalAbort) {
        conditionalAbort.abort();
        conditionalAbort = null;
      }
      passkeyBtn.disabled = true;
      try {
        var hint = usernameInput ? usernameInput.value.trim() : '';
        var serverOpts = await fetchOptions(hint);
        var publicKey = toPublicKeyRequestOptions(serverOpts);
        var cred = await navigator.credentials.get({ publicKey: publicKey });
        if (cred) {
          await verifyAssertion(cred, serverOpts);
        }
      } catch (err) {
        if (!err || (err.name !== 'NotAllowedError' && err.name !== 'AbortError')) {
          showStatus(passkeyBtn.getAttribute('data-error-msg') || 'Passkey sign-in could not be completed.');
        }
      } finally {
        passkeyBtn.disabled = false;
      }
    });
  }

  initPasskeys();
})();
