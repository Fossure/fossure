package io.github.fossure.service.upload;

import java.util.Set;

import io.github.fossure.service.exceptions.UploadException;
import io.github.fossure.domain.File;

public interface AssetLoader<T> {
    Set<T> load(File file) throws UploadException;
}
