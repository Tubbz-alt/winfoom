/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.util;

import org.kpax.winfoom.annotation.ThreadSafe;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.io.OutputStream;

@ThreadSafe
public interface StreamSource {

    InputStream getInputStream();

    OutputStream getOutputStream();

    static StreamSource from(final InputStream inputStream, final OutputStream outputStream) {
        return new DefaultStreamSource(inputStream, outputStream);
    }

    class DefaultStreamSource implements StreamSource {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        private DefaultStreamSource(final InputStream inputStream, final OutputStream outputStream) {
            Assert.notNull(inputStream, "inputStream cannot be null");
            Assert.notNull(outputStream, "outputStream cannot be null");
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }
    }
}
