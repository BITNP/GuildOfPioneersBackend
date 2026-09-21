package net.bitnp.guildofpioneers.storage;

/**
 * Metadata of a stored object, read without downloading its content.
 *
 * @param contentType the MIME content type, or {@code null} if unknown
 * @param size        the content length in bytes
 * @param version     an opaque version marker that changes when the object content
 *                    changes; used to build cache-busting URLs
 */
public record ObjectInfo(String contentType, long size, String version) {
}
