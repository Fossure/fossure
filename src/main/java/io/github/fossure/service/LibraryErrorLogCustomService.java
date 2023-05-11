package io.github.fossure.service;

import io.github.fossure.repository.LibraryErrorLogCustomRepository;
import io.github.fossure.domain.LibraryErrorLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom service implementation for managing {@link LibraryErrorLog}.
 */
@Service
@Transactional
public class LibraryErrorLogCustomService extends LibraryErrorLogService {

    public LibraryErrorLogCustomService(LibraryErrorLogCustomRepository libraryErrorLogRepository) {
        super(libraryErrorLogRepository);
    }
}
