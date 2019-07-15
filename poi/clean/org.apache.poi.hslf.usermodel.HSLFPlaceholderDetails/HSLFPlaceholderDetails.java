import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.*;


import org.apache.poi.hslf.model.HeadersFooters;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.PlaceholderDetails;

/**
 * Extended placeholder details for HSLF sheets - mainly for headers and footers
 *
 * @since POI 4.0.0
 */
public class HSLFPlaceholderDetails implements PlaceholderDetails {
    private final HSLFSheet sheet;
    private final Placeholder placeholder;

    HSLFPlaceholderDetails(final HSLFSheet sheet, final Placeholder placeholder) {
        this.sheet = sheet;
        this.placeholder = placeholder;
    }


    public boolean isVisible() {
        final Placeholder ph = getPlaceholder();
        if (ph == null) {
            return false;
        }

        final HeadersFooters headersFooters = sheet.getHeadersFooters();

        switch (ph) {
            case HEADER:
                return headersFooters.isHeaderVisible();
            case FOOTER:
                return headersFooters.isFooterVisible();
            case DATETIME:
                return headersFooters.isDateTimeVisible();
            case TITLE:
                return headersFooters.isHeaderVisible();
            case SLIDE_NUMBER:
                return headersFooters.isSlideNumberVisible();
            default:
                return false;
        }
    }

    public void setVisible(final boolean isVisible) {
        final Placeholder ph = getPlaceholder();
        if (ph == null) {
            return;
        }

        final HeadersFooters headersFooters = sheet.getHeadersFooters();

        switch (ph) {
            case TITLE:
            case HEADER:
                headersFooters.setHeaderVisible(isVisible);
                break;
            case FOOTER:
                headersFooters.setFooterVisible(isVisible);
                break;
            case DATETIME:
                headersFooters.setDateTimeVisible(isVisible);
                break;
            case SLIDE_NUMBER:
                headersFooters.setSlideNumberVisible(isVisible);
                break;
        }
    }

    @Override
    public Placeholder getPlaceholder() {
        return placeholder;
    }

    @Override
    public void setPlaceholder(Placeholder placeholder) {
        throw new UnsupportedOperationException("Only sub class(es) of HSLFPlaceholderDetails allow setting the placeholder");
    }

    @Override
    public PlaceholderSize getSize() {
        return PlaceholderSize.full;
    }

    @Override
    public void setSize(PlaceholderSize size) {
        throw new UnsupportedOperationException("Only sub class(es) of HSLFPlaceholderDetails allow setting the size");
    }

    @Override
    public String getText() {
        final Placeholder ph = getPlaceholder();
        if (ph == null) {
            return null;
        }

        final HeadersFooters headersFooters = sheet.getHeadersFooters();

        switch (ph) {
            case TITLE:
            case HEADER:
                return headersFooters.getHeaderText();
            case FOOTER:
                return headersFooters.getFooterText();
            case DATETIME:
                return headersFooters.getDateTimeText();
            case SLIDE_NUMBER:
            default:
                return null;
        }
    }

    @Override
    public void setText(final String text) {
        final Placeholder ph = getPlaceholder();
        if (ph == null) {
            return;
        }

        final HeadersFooters headersFooters = sheet.getHeadersFooters();

        switch (ph) {
            case TITLE:
            case HEADER:
                headersFooters.setHeaderText(text);
                break;
            case FOOTER:
                headersFooters.setFootersText(text);
                break;
            case DATETIME:
                headersFooters.setDateTimeText(text);
                break;
            case SLIDE_NUMBER:
            default:
                break;
        }

    }
}
