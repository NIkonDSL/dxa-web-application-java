package com.sdl.dxa.tridion.content;

import com.sdl.dxa.common.dto.StaticContentRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.StaticContentItem;
import com.sdl.webapp.common.api.content.StaticContentNotLoadedException;
import com.sdl.webapp.common.util.ImageUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static com.sdl.webapp.common.util.FileUtils.parentFolderExists;

@Slf4j
public abstract class GenericStaticContentResolver implements StaticContentResolver {

    private static final Pattern SYSTEM_VERSION_PATTERN = Pattern.compile("/system/v\\d+\\.\\d+/");
    private static final String STATIC_FILES_DIR = "BinaryData";
    protected static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    protected WebApplicationContext webApplicationContext;

    private ConcurrentMap<String, Holder> runningTasksByPaths = new ConcurrentHashMap<>();

    private static class Holder {
        private String url;
        private StaticContentItem previousState;
    }

    @NotNull
    protected StaticContentItem createStaticContentItem(String path,
                                                        StaticContentRequestDto requestDto) throws ContentProviderException {
        Holder newHolder = new Holder();
        Holder oldHolder = runningTasksByPaths.putIfAbsent(path, newHolder);
        if (oldHolder != null) {
            newHolder = oldHolder;
        }
        newHolder.url = path.intern();
        try {
            synchronized (newHolder.url) {
                newHolder.previousState = getStaticContentFileByPath(path, requestDto);
                log.debug("Returned file {}", newHolder.url);
            }
            return newHolder.previousState;
        } finally {
            runningTasksByPaths.remove(newHolder.url);
        }
    }

    @Override
    public @NotNull StaticContentItem getStaticContent(@NotNull StaticContentRequestDto requestDto) throws ContentProviderException {
        log.trace("getStaticContent: {}", requestDto);

        StaticContentRequestDto adaptedRequest = requestDto.isLocalizationPathSet()
                ? requestDto
                : requestDto.toBuilder().localizationPath(resolveLocalizationPath(requestDto)).build();

        if (requestDto.getBinaryPath() != null) {
            final String contentPath = getContentPath(adaptedRequest.getBinaryPath(), adaptedRequest.getLocalizationPath());
            return createStaticContentItem(contentPath, adaptedRequest);
        }
        return getStaticContentItemById(requestDto.getBinaryId(), adaptedRequest);
    }

    private String getContentPath(@NotNull String binaryPath, @NotNull String localizationPath) {
        if (localizationPath.length() > 1) {
            String path = binaryPath.startsWith(localizationPath) ? binaryPath.substring(localizationPath.length()) : binaryPath;
            return localizationPath + removeVersionNumber(path);
        }
        return removeVersionNumber(binaryPath);
    }

    private String removeVersionNumber(String path) {
        return SYSTEM_VERSION_PATTERN.matcher(path).replaceFirst("/system/");
    }

    protected @NotNull String getPublicationPath(String publicationId) {
        return StringUtils.join(new String[]{ webApplicationContext.getServletContext().getRealPath("/"), STATIC_FILES_DIR, publicationId }, File.separator);
    }

    private @NotNull StaticContentItem getStaticContentFileByPath(String path, StaticContentRequestDto requestDto) throws ContentProviderException {
        String parentPath = getPublicationPath(requestDto.getLocalizationId());

        final File file = new File(parentPath, path);
        log.trace("getStaticContentFileByPath: {}", file.getAbsolutePath());
        final ImageUtils.StaticContentPathInfo pathInfo = new ImageUtils.StaticContentPathInfo(path);
        int publicationId = Integer.parseInt(requestDto.getLocalizationId());
        String urlPath = prependFullUrlIfNeeded(pathInfo.getFileName(), requestDto.getBaseUrl());
        return createStaticContentItem(requestDto, file, publicationId, pathInfo, urlPath);
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    protected String prependFullUrlIfNeeded(String path, String baseUrl) {
        if (path.contains(baseUrl)) {
            return path;
        }
        return UriUtils.encodePath(baseUrl + path, "UTF-8");
    }

    protected void refreshBinary(File file, ImageUtils.StaticContentPathInfo pathInfo, byte[] binaryContent) throws ContentProviderException {
        log.debug("Writing binary content to file: {}", file);
        try {
            if (!parentFolderExists(file, true)) {
                throw new ContentProviderException("Failed to create parent directory for file: " + file);
            }
            if (log.isWarnEnabled() && file.exists() && !file.canWrite()) {
                log.warn("File {} exists and cannot be written", file);
            }
            ImageUtils.writeToFile(file, pathInfo, binaryContent);
        } catch (IOException e) {
            throw new StaticContentNotLoadedException("Cannot write new loaded content to a file: " + file.getAbsolutePath(), e);
        }
    }

    @NotNull
    protected abstract StaticContentItem createStaticContentItem(
            StaticContentRequestDto requestDto,
            File file,
            int publicationId,
            ImageUtils.StaticContentPathInfo pathInfo,
            String urlPath
    ) throws ContentProviderException;

    protected abstract @NotNull StaticContentItem getStaticContentItemById(int binaryId, StaticContentRequestDto requestDto) throws ContentProviderException;

    protected abstract String resolveLocalizationPath(StaticContentRequestDto requestDto) throws StaticContentNotLoadedException;
}
