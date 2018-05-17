/**
 * com.blackducksoftware.integration.eclipse.plugin
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.eclipse.services.connection.hub;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.eclipse.services.BlackDuckPreferencesService;
import com.blackducksoftware.integration.encryption.EncryptionUtils;
import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.Credentials;
import com.blackducksoftware.integration.hub.CredentialsBuilder;
import com.blackducksoftware.integration.hub.configuration.HubServerConfig;
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.hub.proxy.ProxyInfoBuilder;

public class HubPreferencesService {
    private final Logger log = LoggerFactory.getLogger(HubPreferencesService.class);

    private final BlackDuckPreferencesService blackDuckPreferencesService;

    public static final String HUB_USERNAME = "hubUsername";
    public static final String HUB_PASSWORD = "hubPassword";
    public static final String HUB_PASSWORD_LENGTH = "hubPasswordLength";
    public static final String HUB_URL = "hubURL";
    public static final String HUB_TIMEOUT = "hubTimeout";
    public static final String HUB_ALWAYS_TRUST = "hubAlwaysTrustCert";
    public static final String PROXY_USERNAME = "proxyUsername";
    public static final String PROXY_PASSWORD = "proxyPassword";
    public static final String PROXY_PASSWORD_LENGTH = "proxyPasswordLength";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";

    public static final String DEFAULT_HUB_TIMEOUT = "120";
    public static final boolean DEFAULT_HUB_ALWAYS_TRUST = true;

    public HubPreferencesService(final BlackDuckPreferencesService blackDuckPreferenceService) {
        this.blackDuckPreferencesService = blackDuckPreferenceService;
        blackDuckPreferencesService.setPreferenceDefault(HUB_TIMEOUT, DEFAULT_HUB_TIMEOUT);
        blackDuckPreferencesService.setPreferenceDefault(HUB_ALWAYS_TRUST, DEFAULT_HUB_ALWAYS_TRUST);
    }

    public String getPreference(final String preference) {
        return blackDuckPreferencesService.getPreference(preference);
    }

    public String getHubUsername() {
        return this.getPreference(HUB_USERNAME);
    }

    public String getHubPassword() {
        final CredentialsBuilder hubCredentialsBuilder = new CredentialsBuilder();
        final String password = this.getPreference(HUB_PASSWORD);
        if (!password.trim().isEmpty()) {
            hubCredentialsBuilder.setPassword(password);
            final String passwordLength = this.getPreference(HUB_PASSWORD_LENGTH);
            if (passwordLength != null && !passwordLength.equals("")) {
                hubCredentialsBuilder.setPasswordLength(Integer.parseInt(passwordLength));
            }
            final Credentials hubCredentials = hubCredentialsBuilder.buildObject();
            try {
                return hubCredentials.getDecryptedPassword();
            } catch (final Exception e) {
                // TODO: Log properly
            }
        }
        return password;
    }

    public String getHubUrl() {
        return this.getPreference(HUB_URL);
    }

    public String getHubTimeout() {
        return this.getPreference(HUB_TIMEOUT);
    }

    public boolean getHubAlwaysTrust() {
        return Boolean.parseBoolean(this.getPreference(HUB_ALWAYS_TRUST));
    }

    public String getHubProxyUsername() {
        return this.getPreference(PROXY_USERNAME);
    }

    public String getHubProxyPort() {
        return this.getPreference(PROXY_PORT);
    }

    public String getHubProxyHost() {
        return this.getPreference(PROXY_HOST);
    }

    public String getHubProxyPassword() {
        final ProxyInfoBuilder hubProxyInfoBuilder = new ProxyInfoBuilder();
        final String proxyPassword = this.getPreference(PROXY_PASSWORD);
        if (!proxyPassword.trim().isEmpty()) {
            hubProxyInfoBuilder.setPassword(proxyPassword);
            final String proxyPasswordLength = this.getPreference(PROXY_PASSWORD_LENGTH);
            if (proxyPasswordLength != null && !proxyPasswordLength.equals("")) {
                hubProxyInfoBuilder.setPasswordLength(Integer.parseInt(proxyPasswordLength));
            }
            final ProxyInfo hubProxyInfo = hubProxyInfoBuilder.buildObject();
            try {
                return hubProxyInfo.getDecryptedPassword();
            } catch (final Exception e) {
                // TODO: Log properly
            }
        }
        return proxyPassword;
    }

    public void savePreference(final String preference, final String preferenceValue) {
        blackDuckPreferencesService.savePreference(preference, preferenceValue);
    }

    public void saveHubUsername(final String hubUsername) {
        this.savePreference(HUB_USERNAME, hubUsername);
    }

    public void saveHubPassword(final String hubPassword) throws EncryptionException {
        if (!hubPassword.trim().isEmpty()) {
            final EncryptionUtils encryptionUtils = new EncryptionUtils();
            String encryptedPassword = null;
            byte[] inputStreamString = null;
            final String EMBEDDED_SUN_KEY_FILE = "/Sun-Key.jceks";
            final String EMBEDDED_IBM_KEY_FILE = "/IBM-Key.jceks";
            final char[] KEY_PASS = { 'b', 'l', 'a', 'c', 'k', 'd', 'u', 'c', 'k', '1', '2', '3', 'I', 'n', 't', 'e', 'g', 'r', 'a', 't', 'i', 'o', 'n' };

            log.info("### SAVING HUB PASSWORD");
            log.info("Default encoding: " + System.getProperty("file.encoding"));
            log.info("Attempting to encrypt password without input stream... ");
            try {
                encryptedPassword = encryptionUtils.alterString(hubPassword, null, Cipher.ENCRYPT_MODE);
            } catch (final Exception e) {
                log.info("Failure encrypting the password without input stream", e);
            }
            if (encryptedPassword != null) {
                log.info("Successfully encrypted password.");
            }

            log.info("Attempting to encrypt password with input stream... ");
            try {
                InputStream inputStream = new EncryptionUtils().getClass().getResourceAsStream(EMBEDDED_SUN_KEY_FILE);
                inputStreamString = IOUtils.toByteArray(inputStream);
                log.info("Using Sun-Key.jceks, inputStream = " + Arrays.toString(inputStreamString));

                inputStream = new EncryptionUtils().getClass().getResourceAsStream(EMBEDDED_SUN_KEY_FILE);
                final KeyStore keystore = KeyStore.getInstance("JCEKS");
                log.info("Keystore = " + keystore.toString());
                keystore.load(inputStream, KEY_PASS);
                final Key key = keystore.getKey("keyStore", KEY_PASS);
                log.info("Got the key, key=" + key.toString());

                inputStream = new EncryptionUtils().getClass().getResourceAsStream(EMBEDDED_SUN_KEY_FILE);
                encryptedPassword = encryptionUtils.alterString(hubPassword, inputStream, Cipher.ENCRYPT_MODE);
            } catch (final Exception e) {
                try {
                    InputStream inputStream = new EncryptionUtils().getClass().getResourceAsStream(EMBEDDED_IBM_KEY_FILE);
                    inputStreamString = IOUtils.toByteArray(inputStream);
                    log.info("Using IBM-Key.jceks, inputStream = " + Arrays.toString(inputStreamString));

                    inputStream = new EncryptionUtils().getClass().getResourceAsStream(EMBEDDED_IBM_KEY_FILE);
                    final KeyStore keystore = KeyStore.getInstance("JCEKS");
                    log.info("Keystore = " + keystore.toString());
                    keystore.load(inputStream, KEY_PASS);
                    final Key key = keystore.getKey("keyStore", KEY_PASS);
                    log.info("Got the key, key=" + key.toString());

                    inputStream = new EncryptionUtils().getClass().getResourceAsStream(EMBEDDED_IBM_KEY_FILE);
                    encryptedPassword = encryptionUtils.alterString(hubPassword, inputStream, Cipher.ENCRYPT_MODE);
                } catch (final Exception e1) {
                    throw new EncryptionException("Failed to retrieve the encryption key - inputStream=" + inputStreamString, e1);
                }
            }
            log.info("Successfully encrypted password.");

            try {
                this.savePreference(HUB_PASSWORD, encryptedPassword);
                this.savePreference(HUB_PASSWORD_LENGTH, Integer.toString(hubPassword.length()));
            } catch (final Exception e) {
                // TODO: Log properly
            }
        } else {
            this.savePreference(HUB_PASSWORD, hubPassword);
        }
    }

    public void saveHubUrl(final String hubUrl) {
        this.savePreference(HUB_URL, hubUrl);
    }

    public void saveHubTimeout(final String timeout) {
        this.savePreference(HUB_TIMEOUT, timeout);
    }

    public void saveHubAlwaysTrust(final boolean hubAlwaysTrust) {
        this.savePreference(HUB_ALWAYS_TRUST, Boolean.toString(hubAlwaysTrust));
    }

    public void saveHubProxyUsername(final String proxyUsername) {
        this.savePreference(PROXY_USERNAME, proxyUsername);
    }

    public void saveHubProxyPassword(final String proxyPassword) {
        if (!proxyPassword.trim().isEmpty()) {
            final ProxyInfoBuilder hubProxyInfoBuilder = new ProxyInfoBuilder();
            hubProxyInfoBuilder.setPassword(proxyPassword);
            final ProxyInfo hubProxyInfo = hubProxyInfoBuilder.buildObject();
            try {
                this.savePreference(PROXY_PASSWORD, hubProxyInfo.getEncryptedPassword());
                this.savePreference(PROXY_PASSWORD_LENGTH, hubProxyInfo.getActualPasswordLength() + "");
            } catch (final Exception e) {
                // TODO: Log properlys
            }
        } else {
            this.savePreference(PROXY_PASSWORD, proxyPassword);
        }
    }

    public void saveHubProxyHost(final String proxyHost) {
        this.savePreference(PROXY_HOST, proxyHost);
    }

    public void saveHubProxyPort(final String proxyPort) {
        this.savePreference(PROXY_PORT, proxyPort);
    }

    public HubServerConfig getHubServerConfig() throws IllegalStateException {
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        final String username = this.getHubUsername();
        hubServerConfigBuilder.setUsername(username);
        final String password = this.getHubPassword();
        hubServerConfigBuilder.setPassword(password);
        final String hubUrl = this.getHubUrl();
        hubServerConfigBuilder.setHubUrl(hubUrl);
        final String timeout = this.getHubTimeout();
        hubServerConfigBuilder.setTimeout(timeout);
        final boolean hubAlwaysTrust = this.getHubAlwaysTrust();
        hubServerConfigBuilder.setAlwaysTrustServerCertificate(hubAlwaysTrust);
        final String proxyUsername = this.getHubProxyUsername();
        hubServerConfigBuilder.setProxyUsername(proxyUsername);
        final String proxyPassword = this.getHubProxyPassword();
        hubServerConfigBuilder.setProxyPassword(proxyPassword);
        final String proxyPort = this.getHubProxyPort();
        hubServerConfigBuilder.setProxyPort(proxyPort);
        final String proxyHost = this.getHubProxyHost();
        hubServerConfigBuilder.setProxyHost(proxyHost);
        return hubServerConfigBuilder.build();
    }

}
