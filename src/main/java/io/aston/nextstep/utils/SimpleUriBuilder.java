package io.aston.nextstep.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SimpleUriBuilder {

    private StringBuilder sb = new StringBuilder();
    private Charset encoding;
    private boolean hasPath = false;

    public SimpleUriBuilder(String path) {
        this(path, null);
    }

    public SimpleUriBuilder(String path, Charset encoding) {
        if (path != null) {
            this.sb.append(path);
            hasPath = true;
        }
        this.encoding = encoding != null ? encoding : StandardCharsets.UTF_8;
    }

    public SimpleUriBuilder param(String name, String value) {
        if (sb.length() > 0)
            sb.append((hasPath && sb.indexOf("?") < 0) ? "?" : "&");
        sb.append(name);
        sb.append("=");
        sb.append(URLEncoder.encode(value, encoding));
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public URI build() throws URISyntaxException {
        return new URI(sb.toString());
    }
}
