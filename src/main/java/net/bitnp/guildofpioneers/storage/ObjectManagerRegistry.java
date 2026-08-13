package net.bitnp.guildofpioneers.storage;

import com.potato.database.DatabaseManager;
import com.potato.object.ObjectManager;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds and holds one {@link ObjectManager} per storage namespace, eagerly at startup.
 *
 * <p>Namespaces are registered exactly once per JVM. Building is idempotent: if a
 * manager already exists for a namespace it is reused, otherwise it is built; failing
 * to obtain an instance aborts application startup.</p>
 */
@Component
public class ObjectManagerRegistry {

    public static final String NAMESPACE_AVATARS = "avatars";
    public static final String NAMESPACE_PROJECT_COVERS = "project_covers";

    private final Map<String, ObjectManager> managers;

    /**
     * Builds every namespace known to the application, failing fast on error.
     *
     * @param databaseManager the metadata database manager shared by all namespaces
     */
    public ObjectManagerRegistry(DatabaseManager databaseManager) {
        Map<String, ObjectManager> built = new LinkedHashMap<>();
        built.put(NAMESPACE_AVATARS, build(
                NAMESPACE_AVATARS,
                databaseManager,
                "jpg", "png", "webp", "gif"));
        built.put(NAMESPACE_PROJECT_COVERS, build(
                NAMESPACE_PROJECT_COVERS,
                databaseManager,
                "jpg", "png", "webp", "gif"));
        this.managers = Collections.unmodifiableMap(built);
    }

    /**
     * Returns the {@link ObjectManager} for the given namespace, or {@code null} if
     * the namespace is unknown.
     *
     * @param namespace the namespace to look up
     * @return the manager bound to the namespace, or {@code null}
     */
    public ObjectManager get(String namespace) {
        return managers.get(namespace);
    }

    private static ObjectManager build(String namespace, DatabaseManager databaseManager, String... allowedExtensions) {
        ObjectManager existing = ObjectManager.getInstance(namespace);
        if (existing != null) {
            return existing;
        }
        ObjectManager manager = ObjectManager.builder()
                .namespace(namespace)
                .databaseManager(databaseManager)
                .allowExtension(allowedExtensions)
                .build();
        if (manager == null) {
            throw new IllegalStateException("Failed to initialize ObjectManager for namespace " + namespace);
        }
        return manager;
    }
}
