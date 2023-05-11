package io.github.fossure.service.upload;

import java.util.HashMap;
import java.util.Set;

import io.github.fossure.service.exceptions.UploadException;
import io.github.fossure.domain.File;

public class AssetManager<T> {

    private final HashMap<String, AssetLoader<T>> loaders = new HashMap<>();

    public void addLoader(AssetLoader<T> loader, String contentType) {
        loaders.put(contentType, loader);
    }

    public Set<T> load(File file, String contentType) throws UploadException {
        AssetLoader<T> loader = loaders.get(contentType);
        if (loader == null) throw new UploadException("No loader registered for \"" + contentType + "\" files");

        return loader.load(file);
    }
}
