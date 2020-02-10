/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.util;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * @author Eugen Covaci
 */
public final class LocalIOUtils extends IOUtils {

    public static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private LocalIOUtils() {
    }

    /**
     * It checks for available data.
     *
     * @param inputBuffer The input buffer.
     * @return <code>false</code> iff EOF has been reached.
     */
    public static boolean isAvailable(SessionInputBufferImpl inputBuffer) {
        try {
            return inputBuffer.hasBufferedData() || inputBuffer.fillBuffer() > -1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copy from <code>inputStream</code> to <code>outputStream</code> until EOF is
     * reached.
     *
     * @param inputStream  The input stream to copy from.
     * @param outputStream The output stream to write into.
     */
    public static void copyQuietly(InputStream inputStream, OutputStream outputStream) {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try {
            int len;
            while ((len = inputStream.read(buffer)) != EOF) {
                outputStream.write(buffer, 0, len);
                outputStream.flush();
            }

        } catch (Exception e) {
            logger.debug("Error on reading bytes", e);
        }
    }

    /**
     * Close all <code>closeables</code>.
     *
     * @param closeables The the array of {@link Closeable}
     */
    public static void close(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closable : closeables) {
                if (closable != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Close {}", closable.getClass());
                    }
                    try {
                        closable.close();
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Fail to close: " + closable.getClass().getName(), e);
                        }
                    }
                }
            }
        }
    }

    public static void mergeProperties(PropertiesConfiguration from, PropertiesConfiguration to) {
        for (Iterator<String> itr = to.getKeys(); itr.hasNext(); ) {
            String key = itr.next();
            if (!from.containsKey(key)) {
                itr.remove();
            }
        }
        for (Iterator<String> itr = from.getKeys(); itr.hasNext(); ) {
            String key = itr.next();
            if (!to.containsKey(key)) {
                to.addProperty(key, from.getProperty(key));
            }
        }
    }

}
