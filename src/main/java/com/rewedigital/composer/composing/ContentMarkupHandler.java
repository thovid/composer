package com.rewedigital.composer.composing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.attoparser.AbstractMarkupHandler;
import org.attoparser.ParseException;
import org.attoparser.util.TextUtil;

class ContentMarkupHandler extends AbstractMarkupHandler {

    private static class Asset {
        private final String type;
        private final Map<String, String> attributes;
        private final boolean selfClosing;

        private Asset(final Builder builder) {
            this.type = builder.type;
            this.selfClosing = builder.selfClosing;
            this.attributes = new HashMap<>(builder.attributes);
        }

        public static class Builder {
            private String type;
            private boolean selfClosing;
            private final Map<String, String> attributes = new HashMap<>();

            public Builder type(final String type) {
                this.type = type;
                return this;
            }

            public Builder attribute(final String name, final String value) {
                this.attributes.put(name, value);
                return this;
            }

            public Builder selfClosing(final boolean selfClosing) {
                this.selfClosing = selfClosing;
                return this;
            }

            public boolean attributeContainsInclude(final String attributeName) {
                return attributes.getOrDefault(attributeName, "").contains("include");
            }

            public Asset build() {
                return new Asset(this);
            }
        }

        public String render() {
            return attributes
                .entrySet()
                .stream()
                .reduce(renderOpen(),
                    (builder, e) -> builder.append(e.getKey())
                        .append("=\"")
                        .append(e.getValue())
                        .append("\" "),
                    (a, b) -> a.append(b))
                .append(renderClosing()).toString();
        }

        private StringBuilder renderOpen() {
            return new StringBuilder().append("<").append(type).append(" ");
        }

        private String renderClosing() {
            if (selfClosing) {
                return "/>";
            }

            return "></" + type + ">";
        }
    }

    private final char[] contentTag;
    private final String assetOptionsAttribute;

    private final List<String> links = new LinkedList<>();
    private final List<Asset> assets = new LinkedList<>();

    private Asset.Builder current = null;
    private final ContentRange defaultContentRange;

    private int contentStart = 0;
    private int contentEnd = 0;

    private boolean parsingHead = false;


    public ContentMarkupHandler(final ContentRange defaultContentRange, final ComposerHtmlConfiguration configuration) {
        this.defaultContentRange = defaultContentRange;
        this.contentTag = configuration.contentTag().toCharArray();
        this.assetOptionsAttribute = configuration.assetOptionsAttribute();
    }

    public ContentRange contentRange() {
        return contentEnd <= 0 ? defaultContentRange : new ContentRange(contentStart, contentEnd);
    }

    public List<String> assetLinks() {
        return links;
    }

    @Override
    public void handleStandaloneElementStart(final char[] buffer, final int nameOffset, final int nameLen,
        final boolean minimized, final int line, final int col) throws ParseException {
        super.handleStandaloneElementStart(buffer, nameOffset, nameLen, minimized, line, col);
        if (parsingHead && isAssetElement(buffer, nameOffset, nameLen)) {
            startAsset(buffer, nameOffset, nameLen, true);
        }
    }

    @Override
    public void handleStandaloneElementEnd(final char[] buffer, final int nameOffset, final int nameLen,
        final boolean minimized, final int line,
        final int col) throws ParseException {
        super.handleStandaloneElementEnd(buffer, nameOffset, nameLen, minimized, line, col);
        if (parsingAsset()) {
            pushAsset();
        }
    }

    @Override
    public void handleOpenElementStart(final char[] buffer, final int nameOffset, final int nameLen, final int line,
        final int col) throws ParseException {
        super.handleOpenElementStart(buffer, nameOffset, nameLen, line, col);

        if (isHeadElement(buffer, nameOffset, nameLen)) {
            parsingHead = true;
        } else if (isContentElement(buffer, nameOffset, nameLen)) {
            contentStart = nameOffset + nameLen + 1;
        } else if (isAssetElement(buffer, nameOffset, nameLen) && parsingHead) {
            startAsset(buffer, nameOffset, nameLen, false);
        }
    }

    @Override
    public void handleCloseElementEnd(final char[] buffer, final int nameOffset, final int nameLen, final int line,
        final int col) throws ParseException {
        super.handleCloseElementEnd(buffer, nameOffset, nameLen, line, col);
        if (isHeadElement(buffer, nameOffset, nameLen)) {
            parsingHead = false;
        } else if (isContentElement(buffer, nameOffset, nameLen) && contentStart >= 0) {
            contentEnd = nameOffset - 2;
        } else if (parsingAsset()) {
            pushAsset();
        }
    }

    @Override
    public void handleAttribute(final char[] buffer, final int nameOffset, final int nameLen, final int nameLine,
        final int nameCol,
        final int operatorOffset, final int operatorLen, final int operatorLine, final int operatorCol,
        final int valueContentOffset,
        final int valueContentLen, final int valueOuterOffset, final int valueOuterLen, final int valueLine,
        final int valueCol)
        throws ParseException {
        super.handleAttribute(buffer, nameOffset, nameLen, nameLine, nameCol, operatorOffset, operatorLen, operatorLine,
            operatorCol, valueContentOffset, valueContentLen, valueOuterOffset, valueOuterLen, valueLine, valueCol);

        if (parsingAsset()) {
            current = current.attribute(new String(buffer, nameOffset, nameLen),
                new String(buffer, valueContentOffset, valueContentLen));
        }
    }

    private boolean parsingAsset() {
        return current != null;
    }

    private boolean isAssetElement(final char[] buffer, final int nameOffset, final int nameLen) {
        return textContains(buffer, nameOffset, nameLen, "link") || textContains(buffer, nameOffset, nameLen, "script");
    }

    private boolean isHeadElement(final char[] buffer, final int nameOffset, final int nameLen) {
        return textContains(buffer, nameOffset, nameLen, "head");
    }

    private boolean textContains(final char[] buffer, final int nameOffset, final int nameLen, final String item) {
        return TextUtil.contains(true, buffer, nameOffset, nameLen, item, 0, item.length());
    }

    private boolean isContentElement(final char[] buffer, final int nameOffset, final int nameLen) {
        return contentEnd <= 0
            && TextUtil.contains(true, buffer, nameOffset, nameLen, contentTag, 0, contentTag.length);
    }


    private void startAsset(final char[] buffer, final int nameOffset, final int nameLen, final boolean selfClosing) {
        current = new Asset.Builder()
            .type(new String(buffer, nameOffset, nameLen))
            .selfClosing(selfClosing);
    }

    private void pushAsset() {
        if (current.attributeContainsInclude(assetOptionsAttribute)) {
            final Asset asset = current.build();
            assets.add(asset);
            links.add(asset.render());
        }
        current = null;
    }



}
