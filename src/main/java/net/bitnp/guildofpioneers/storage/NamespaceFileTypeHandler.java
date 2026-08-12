package net.bitnp.guildofpioneers.storage;

/**
 * Resolves the canonical file extension for uploaded files of a namespace.
 *
 * <p>Each namespace may impose its own set of accepted content types; handlers convert
 * an accepted MIME content type into the extension used to name and serve the file.</p>
 */
public interface NamespaceFileTypeHandler {

    /**
     * Resolves the canonical file extension for a content type.
     *
     * @param contentType the MIME content type of the uploaded file
     * @return the file extension without the leading dot, or {@code null} if unsupported
     */
    String extensionForContentType(String contentType);
}
