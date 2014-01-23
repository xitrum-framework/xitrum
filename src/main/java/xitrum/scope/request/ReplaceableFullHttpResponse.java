package xitrum.scope.request;

import io.netty.handler.codec.http.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * An implementation of {@link FullHttpResponse}. This implementation allows
 * replacing content data.
 */
public class ReplaceableFullHttpResponse extends DefaultHttpResponse implements FullHttpResponse {
    // Based on DefaultFullHttpResponse.java of Netty

    private ByteBuf content;

    private final HttpHeaders trailingHeaders;
    private final boolean validateHeaders;

    public ReplaceableFullHttpResponse(HttpVersion version, HttpResponseStatus status) {
        this(version, status, Unpooled.EMPTY_BUFFER);
    }

    public ReplaceableFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
        this(version, status, content, true);
    }

    public ReplaceableFullHttpResponse(HttpVersion version, HttpResponseStatus status,
                                   ByteBuf content, boolean validateHeaders) {
        super(version, status, validateHeaders);
        if (content == null) {
            throw new NullPointerException("content");
        }
        this.content = content;
        trailingHeaders = new DefaultHttpHeaders(validateHeaders);
        this.validateHeaders = validateHeaders;
    }

    @Override
    public HttpHeaders trailingHeaders() {
        return trailingHeaders;
    }

    @Override
    public ByteBuf content() {
        return content;
    }

    public void content(ByteBuf newContent) {
        if (content.refCnt() > 0) content.release();
        content = newContent;
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public ReplaceableFullHttpResponse retain() {
        content.retain();
        return this;
    }

    @Override
    public ReplaceableFullHttpResponse retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

    @Override
    public ReplaceableFullHttpResponse setProtocolVersion(HttpVersion version) {
        super.setProtocolVersion(version);
        return this;
    }

    @Override
    public ReplaceableFullHttpResponse setStatus(HttpResponseStatus status) {
        super.setStatus(status);
        return this;
    }

    @Override
    public ReplaceableFullHttpResponse copy() {
        ReplaceableFullHttpResponse copy = new ReplaceableFullHttpResponse(
                getProtocolVersion(), getStatus(), content().copy(), validateHeaders);
        copy.headers().set(headers());
        copy.trailingHeaders().set(trailingHeaders());
        return copy;
    }

    @Override
    public ReplaceableFullHttpResponse duplicate() {
        ReplaceableFullHttpResponse duplicate = new ReplaceableFullHttpResponse(getProtocolVersion(), getStatus(),
                content().duplicate(), validateHeaders);
        duplicate.headers().set(headers());
        duplicate.trailingHeaders().set(trailingHeaders());
        return duplicate;
    }
}
