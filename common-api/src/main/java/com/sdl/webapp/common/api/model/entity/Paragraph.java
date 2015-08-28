package com.sdl.webapp.common.api.model.entity;

import com.sdl.webapp.common.api.model.RichText;

public class Paragraph extends AbstractEntityModel {

    private String subheading;

    private RichText content;

    private MediaItem media;

    private String caption;

    public String getSubheading() {
        return subheading;
    }

    public void setSubheading(String subheading) {
        this.subheading = subheading;
    }

    public RichText getContent() {
        return content;
    }

    public void setContent(RichText content) {
        this.content = content;
    }

    public MediaItem getMedia() {
        return media;
    }

    public void setMedia(MediaItem media) {
        this.media = media;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}