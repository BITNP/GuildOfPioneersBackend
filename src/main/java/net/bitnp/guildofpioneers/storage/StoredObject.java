package net.bitnp.guildofpioneers.storage;

import java.io.InputStream;

/**
 * A stored object's content stream together with the metadata needed to serve it.
 *
 * @param stream      the content stream; the caller must close it
 * @param contentType the MIME content type, or {@code null} if unknown
 * @param size        the content length in bytes
 */
public record StoredObject(InputStream stream, String contentType, long size) {
}
