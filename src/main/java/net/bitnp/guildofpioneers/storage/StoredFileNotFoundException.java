package net.bitnp.guildofpioneers.storage;

/**
 * Thrown when a stored file cannot be found in a namespace.
 */
public class StoredFileNotFoundException extends RuntimeException {

    /**
     * @param namespace the namespace the file was looked up in
     * @param key       the key of the missing file
     */
    public StoredFileNotFoundException(String namespace, String key) {
        super("File \"" + key + "\" does not exist in namespace \"" + namespace + "\"");
    }
}
