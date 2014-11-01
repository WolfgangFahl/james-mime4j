/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.james.mime4j.Charsets;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.io.InputStreams;
import org.apache.james.mime4j.util.ContentUtil;

/**
 * Factory for creating message bodies.
 */
public class BasicBodyFactory implements BodyFactory {

	  // singleton
    public static final BasicBodyFactory INSTANCE = new BasicBodyFactory();
    // the default Charset to use 
		protected Charset defaultCharset=Charset.defaultCharset();
		// mode for Charset resolving - if !lenien=strict exceptions are thrown for unknown charsets
		protected boolean lenientCharsetResolving=true;

    /**
     * default constructor
     */
    public BasicBodyFactory() {
    	 // we are lenient and have a default character set
    }

    /**
     * construct me with a given default charset
     * set 
     * @param pDefaultCharset - the default Character set to 
     */
    public BasicBodyFactory(Charset pDefaultCharset) {
			defaultCharset=pDefaultCharset;
		}
    
		/**
		 * @return the defaultCharset
		 */
		public Charset getDefaultCharset() {
			return defaultCharset;
		}

		/**
		 * @param defaultCharset the defaultCharset to set
		 */
		public void setDefaultCharset(Charset defaultCharset) {
			this.defaultCharset = defaultCharset;
		}

		/**
		 * @return the lenientCharsetResolving
		 */
		public boolean isLenientCharsetResolving() {
			return lenientCharsetResolving;
		}

		/**
		 * @param lenientCharsetResolving the lenientCharsetResolving to set
		 */
		public void setLenientCharsetResolving(boolean lenientCharsetResolving) {
			this.lenientCharsetResolving = lenientCharsetResolving;
		}

		/**
     * select the Charset for the given mimeCharset string
     * 
     *  if you need support for non standard or invalid mimeCharset specifications
     *  you might want to create your own derived BodyFactory extending BasicBodyFactory and
     *  overriding this method as suggested by:
     *    https://issues.apache.org/jira/browse/MIME4J-218
     *  
     *  the default behavior is lenient, invalid mimeCharset specifications will return the defaultCharset
     * 
     *  @param mimeCharset - the string specification for a Charset e.g. "UTF-8"
     *  @throws UnsupportedEncodingException if the mimeCharset is invalid
     */ 
    protected Charset resolveCharset(final String mimeCharset) throws UnsupportedEncodingException {
        if (mimeCharset != null) {
            try {
                return Charset.forName(mimeCharset);
            } catch (UnsupportedCharsetException ex) {
                if (!lenientCharsetResolving) {
                    throw new UnsupportedEncodingException(mimeCharset);
                }
            } catch (IllegalCharsetNameException ex) {
                if (!lenientCharsetResolving) {
                    throw new UnsupportedEncodingException(mimeCharset);
                }
            }
        }
        return defaultCharset;
    }

    public TextBody textBody(final String text, final String mimeCharset) throws UnsupportedEncodingException {
        if (text == null) {
            throw new IllegalArgumentException("Text may not be null");
        }
        return new StringBody1(text, resolveCharset(mimeCharset));
    }

    public TextBody textBody(final byte[] content, final Charset charset) {
        if (content == null) {
            throw new IllegalArgumentException("Content may not be null");
        }
        return new StringBody2(content, charset);
    }

    public TextBody textBody(final InputStream content, final String mimeCharset) throws IOException {
        if (content == null) {
            throw new IllegalArgumentException("Input stream may not be null");
        }
        return new StringBody2(ContentUtil.buffer(content), resolveCharset(mimeCharset));
    }

    public TextBody textBody(final String text, final Charset charset) {
        if (text == null) {
            throw new IllegalArgumentException("Text may not be null");
        }
        return new StringBody1(text, charset);
    }

    public TextBody textBody(final String text) {
        return textBody(text, Charsets.DEFAULT_CHARSET);
    }

    public BinaryBody binaryBody(final String content, final Charset charset) {
        if (content == null) {
            throw new IllegalArgumentException("Content may not be null");
        }
        return new BinaryBody2(content, charset);
    }

    public BinaryBody binaryBody(final InputStream is) throws IOException {
        return new BinaryBody1(ContentUtil.buffer(is));
    }

    public BinaryBody binaryBody(final byte[] buf) {
        return new BinaryBody1(buf);
    }

    static class StringBody1 extends TextBody {

        private final String content;
        private final Charset charset;

        StringBody1(final String content, final Charset charset) {
            super();
            this.content = content;
            this.charset = charset;
        }

        @Override
        public String getMimeCharset() {
            return this.charset != null ? this.charset.name() : null;
        }

        @Override
        public Reader getReader() throws IOException {
            return new StringReader(this.content);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return InputStreams.create(this.content,
                    this.charset != null ? this.charset : Charsets.DEFAULT_CHARSET);
        }

        @Override
        public void dispose() {
        }

        @Override
        public SingleBody copy() {
            return new StringBody1(this.content, this.charset);
        }

    }

    static class StringBody2 extends TextBody {

        private final byte[] content;
        private final Charset charset;

        StringBody2(final byte[] content, final Charset charset) {
            super();
            this.content = content;
            this.charset = charset;
        }

        @Override
        public String getMimeCharset() {
            return this.charset != null ? this.charset.name() : null;
        }

        @Override
        public Reader getReader() throws IOException {
            return new InputStreamReader(InputStreams.create(this.content), this.charset);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return InputStreams.create(this.content);
        }

        @Override
        public void dispose() {
        }

        @Override
        public SingleBody copy() {
            return new StringBody2(this.content, this.charset);
        }

    }

    static class BinaryBody1 extends BinaryBody {

        private final byte[] content;

        BinaryBody1(final byte[] content) {
            super();
            this.content = content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return InputStreams.create(this.content);
        }

        @Override
        public void dispose() {
        }

        @Override
        public SingleBody copy() {
            return new BinaryBody1(this.content);
        }

    }

    static class BinaryBody2 extends BinaryBody {

        private final String content;
        private final Charset charset;

        BinaryBody2(final String content, final Charset charset) {
            super();
            this.content = content;
            this.charset = charset;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return InputStreams.create(this.content,
                    this.charset != null ? this.charset : Charsets.DEFAULT_CHARSET);
        }

        @Override
        public void dispose() {
        }

        @Override
        public SingleBody copy() {
            return new BinaryBody2(this.content, this.charset);
        }

    }

}
