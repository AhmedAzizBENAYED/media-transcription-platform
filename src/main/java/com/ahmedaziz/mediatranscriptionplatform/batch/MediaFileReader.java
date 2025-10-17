package com.ahmedaziz.mediatranscriptionplatform.batch;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaFileReader implements ItemReader<MediaFile> {

    private final MediaFileRepository mediaFileRepository;
    private Iterator<MediaFile> mediaFileIterator;
    private boolean initialized = false;

    @Override
    public MediaFile read() {
        if (!initialized) {
            initialize();
        }

        if (mediaFileIterator != null && mediaFileIterator.hasNext()) {
            MediaFile mediaFile = mediaFileIterator.next();
            log.debug("Reading media file: {}", mediaFile.getId());
            return mediaFile;
        }

        return null; // End of data
    }

    private void initialize() {
        log.info("Initializing MediaFileReader - fetching UPLOADED files");
        List<MediaFile> uploadedFiles = mediaFileRepository.findByStatus(
                MediaFile.ProcessingStatus.UPLOADED);
        mediaFileIterator = uploadedFiles.iterator();
        initialized = true;
        log.info("Found {} files to process", uploadedFiles.size());
    }

    public void reset() {
        initialized = false;
        mediaFileIterator = null;
    }
}
