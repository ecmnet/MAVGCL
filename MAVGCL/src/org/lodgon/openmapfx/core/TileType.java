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

import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.image.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Describes a type of tile that can be returned from a {@link TileProvider},
 * for example, map, terrain or satellite. The base address is set here to be
 * able to cope with potential variations to supply methods.
 *
 * @author Geoff Capper
 */
public class TileType implements MapTileType {

    private static boolean debug = false;

    private final String typeName;
    private final String baseURL;
    private final String attributionNotice;

    private CacheThread cacheThread = null;

    public TileType(String typeName, String baseURL) {
        this(typeName, baseURL,"");
    }

    public TileType(String typeName, String baseURL, String attributionNotice) {
        this.typeName = typeName;
        this.baseURL = baseURL;
        this.attributionNotice = attributionNotice;
    }

    public void setFileStorageBase(String store) {
        if (cacheThread != null) {
            cacheThread.deactivate();
            cacheThread = null;
        }

        this.cacheThread = new CacheThread(store);
        this.cacheThread.start();
    }

    /** The display name for this type of tile, for use in the user interface.
     *
     * @return the name of the type
     */
    @Override
    public String getTypeName() {
        return typeName;
    }

    /** Returns the base URL for obtaining this type of tile from the tile provider.
     *
     * @return The base URL, ending in a forward slash so that zoom and location
     * can be appended directly.
     */
	@Override
    public String getBaseURL() {
        return baseURL;
    }

    public Worker<Image> getImage(int zoom, long i, long j) {
        Task<Image> worker = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                String imageUrl = getImageURL(zoom, i, j);
                boolean bg = imageUrl.startsWith("http");
                return new Image(getImageURL(zoom, i, j), bg);
            }
        };
        new Thread(worker).start();
        return worker;
    }

    protected String getImageURL(int zoom, long i, long j) {
        String cached = getFileCached(zoom, i, j);
        if (cached != null) {
            return cached;
        } else {
            String url = calculateURL(zoom, i, j);
            if (cacheThread != null) {
                cacheThread.cacheImage(url, zoom, i, j);
            }
            return url;
        }
    }

    private String getFileCached(int zoom, long i, long j) {
        if (cacheThread != null) {
            return cacheThread.getCachedFile(zoom, i, j);
        }
        return null;
    }

    protected String calculateURL(int zoom, long i, long j) {
        return getBaseURL() + zoom + "/" + i + "/" + j + ".png";
    }

    @Override
    public String getAttributionNotice() {
        return attributionNotice;
    }
    
    @Override
    public String toString() {
        return getTypeName();
    }

    private static class CacheThread extends Thread {

        private boolean active = true;
        private String basePath;
        private final Set<String> offered = new HashSet<>();
        private final BlockingDeque<String> deque = new LinkedBlockingDeque<>();

        public CacheThread(String basePath) {
            this.basePath = basePath;
            setDaemon(true);
            setName("TileType CacheImagesThread");
        }

        public void deactivate() {
            this.active = false;
        }

        public void run() {
            while (active) {
                try {
                    String key = deque.pollFirst(10, TimeUnit.SECONDS);
                    if (key != null) {
                        String url = key.substring(0, key.lastIndexOf(";"));
                        String[] split = key.substring(key.lastIndexOf(";") + 1).split("/");
                        int zoom = Integer.parseInt(split[0]);
                        long i = Long.parseLong(split[1]);
                        long j = Long.parseLong(split[2]);
                        doCache(url, zoom, i, j);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (debug) System.out.println("Thread Has Ended!!!");
        }

        public void cacheImage(String url, int zoom, long i, long j) {
            String key = url + ";" + zoom + "/" + i + "/" + j;
            synchronized (offered) {
                if (!offered.contains(key)) {
                    offered.add(key);
                    deque.offerFirst(key);
                }
            }
        }

        public String getCachedFile(int zoom, long i, long j) {
            String enc = File.separator + zoom + File.separator + i + File.separator + j + ".png";
            if (debug) System.out.println("looking for " + enc + " in " + basePath);
            File candidate = new File(basePath, enc);
            if (candidate.exists()) {
                return candidate.toURI().toString();
            }
            return null;
        }

        private void doCache(String urlString, int zoom, long i, long j) {
            try {
                URL url = new URL(urlString);
                if (debug) System.out.println("Loading tile from URL " + urlString);
                try (InputStream inputStream = url.openConnection().getInputStream()) {
                    String enc = File.separator + zoom + File.separator + i + File.separator + j + ".png";
                    File candidate = new File(basePath, enc);
                    candidate.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(candidate)) {
                        byte[] buff = new byte[4096];
                        int len = inputStream.read(buff);
                        while (len > 0) {
                            fos.write(buff, 0, len);
                            len = inputStream.read(buff);
                        }
                    }
                    if (debug) System.out.println("Written tile from URL " + urlString + " to " + candidate);
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(TileType.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TileType.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
