/*
 * Copyright (c) 2014, 2015, OpenMapFX and LodgON
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of LodgON, OpenMapFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL LODGON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lodgon.openmapfx.core;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Joeri
 */
public class SettingsService {

    private static final Logger LOG = Logger.getLogger(SettingsService.class.getName());

    private static SettingsService instance;
    private final ServiceLoader<SettingsProvider> loader;
    private SettingsProvider provider;

    private SettingsService() {
        LOG.log(Level.FINE, "Loading SettingsService.");
        loader = ServiceLoader.load(SettingsProvider.class);
        Iterator<SettingsProvider> iterator = loader.iterator();
        while (iterator.hasNext()) {
            if (provider == null) {
                provider = iterator.next();
                LOG.log(Level.INFO, "The following SettingsProvider will be used: {0}", provider);
            } else {
                LOG.log(Level.INFO, "Ignoring SettingsProvider {0}.", iterator.next());
            }
        }

        if (provider == null) {
            LOG.log(Level.WARNING, "No SettingsProvider could be found.");
        }

        LOG.log(Level.FINE, "Loading SettingsService completed.");
    }

    public static SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
        }
        return instance;
    }

    public String getSetting(String key) {
        return provider.getSetting(key);
    }

    public void storeSetting(String key, String value) {
        provider.storeSetting(key, value);
    }

    public void removeSetting(String key) {
        provider.removeSetting(key);
    }

}
