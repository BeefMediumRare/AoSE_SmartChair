package stores;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.HashMap;

public class JSONFileStore {
    public enum JSONFile {
        AGENT_SCHEMA("agentSchema"),
        AGENT_PARAMETER_SCHEMA("agentParameterSchema"), DEFAULT_AGENT_PARAMETER("agentParameterDefault"),
        POINT_SCHEMA("pointSchema"),
        PROJECT_SCHEMA("projectSchema"),
        SIMULATION_SCHEMA("simulationSchema");

        private final @Nonnull
        String relativePath;

        JSONFile(@Nonnull String name) {
            relativePath = "/stores/" + name + ".json";
        }
    }

    public static int getIndentFactor() {
        return 4;
    }

    public static @Nonnull
    InputStream getAsStream(@Nonnull JSONFile jsonFile) {
        return JSONFileStore.class.getResourceAsStream(jsonFile.relativePath);
    }

    public static @Nonnull
    Path getAsPath(@Nonnull JSONFile jsonFile) throws URISyntaxException, IOException {
        Path path;
        URI uri = JSONFileStore.class.getResource(jsonFile.relativePath).toURI();
        String[] parts = uri.toString().split("!");
        if (parts.length > 1) { // we are inside of a jar file and need to create a filesystem
            URI filesystemURI = URI.create(parts[0]);
            try {
                FileSystems.newFileSystem(filesystemURI, new HashMap<>());
            } catch (FileSystemAlreadyExistsException ignored) {
            } finally {
                path = FileSystems.getFileSystem(filesystemURI).getPath(parts[1]);
            }
        } else {
            path = Paths.get(uri);
        }

        return path;
    }
}
