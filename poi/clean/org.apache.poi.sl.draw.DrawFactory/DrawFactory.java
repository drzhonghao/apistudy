import org.apache.poi.sl.draw.Drawable;
import org.apache.poi.sl.draw.DrawSlide;
import org.apache.poi.sl.draw.DrawSheet;
import org.apache.poi.sl.draw.DrawTextBox;
import org.apache.poi.sl.draw.DrawTableShape;
import org.apache.poi.sl.draw.DrawTextShape;
import org.apache.poi.sl.draw.DrawPictureShape;
import org.apache.poi.sl.draw.DrawTextParagraph;
import org.apache.poi.sl.draw.DrawTextFragment;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.draw.DrawFontManager;
import org.apache.poi.sl.draw.*;


import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.sl.usermodel.Background;
import org.apache.poi.sl.usermodel.ConnectorShape;
import org.apache.poi.sl.usermodel.FreeformShape;
import org.apache.poi.sl.usermodel.GraphicalFrame;
import org.apache.poi.sl.usermodel.GroupShape;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.PictureShape;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.sl.usermodel.TextBox;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.util.JvmBugs;

public class DrawFactory {
    protected static final ThreadLocal<DrawFactory> defaultFactory = new ThreadLocal<>();

    /**
     * Set a custom draw factory for the current thread.
     * This is a fallback, for operations where usercode can't set a graphics context.
     * Preferably use the rendering hint {@link Drawable#DRAW_FACTORY} to set the factory.
     *
     * @param factory
     */
    public static void setDefaultFactory(DrawFactory factory) {
        defaultFactory.set(factory);
    }

    /**
     * Returns the DrawFactory, preferably via a graphics instance.
     * If graphics is null, the current thread local is checked or
     * if it is not set, a new factory is created. 
     *
     * @param graphics the current graphics context or null
     * @return the draw factory
     */
    public static DrawFactory getInstance(Graphics2D graphics) {
        // first try to find the factory over the rendering hint
        DrawFactory factory = null;
        boolean isHint = false;
        if (graphics != null) {
            factory = (DrawFactory)graphics.getRenderingHint(Drawable.DRAW_FACTORY);
            isHint = (factory != null);
        }
        // secondly try the thread local default
        if (factory == null) {
            factory = defaultFactory.get();
        }
        // and at last, use the default factory
        if (factory == null) {
            factory = new DrawFactory();
        }
        if (graphics != null && !isHint) {
            graphics.setRenderingHint(Drawable.DRAW_FACTORY, factory);
        }
        return factory;
    }

    public Drawable getDrawable(Shape<?,?> shape) {
        if (shape instanceof TextBox) {
            return getDrawable((TextBox<?,?>)shape);
        } else if (shape instanceof FreeformShape) {
            return getDrawable((FreeformShape<?,?>)shape);
        } else if (shape instanceof TextShape) {
            return getDrawable((TextShape<?,?>)shape);
        } else if (shape instanceof TableShape) {
            return getDrawable((TableShape<?,?>)shape);
        } else if (shape instanceof GroupShape) {
            return getDrawable((GroupShape<?,?>)shape);
        } else if (shape instanceof PictureShape) {
            return getDrawable((PictureShape<?,?>)shape);
        } else if (shape instanceof GraphicalFrame) {
            return getDrawable((GraphicalFrame<?,?>)shape);
        } else if (shape instanceof Background) {
            return getDrawable((Background<?,?>)shape);
        } else if (shape instanceof ConnectorShape) {
            return getDrawable((ConnectorShape<?,?>)shape);
        } else if (shape instanceof Slide) {
            return getDrawable((Slide<?,?>)shape);
        } else if (shape instanceof MasterSheet) {
            return getDrawable((MasterSheet<?,?>)shape);
        } else if (shape instanceof Sheet) {
            return getDrawable((Sheet<?,?>)shape);
        } else if (shape.getClass().isAnnotationPresent(DrawNotImplemented.class)) {
            return new DrawNothing(shape);
        }
        
        throw new IllegalArgumentException("Unsupported shape type: "+shape.getClass());
    }

    public DrawSlide getDrawable(Slide<?,?> sheet) {
        return new DrawSlide(sheet);
    }

    public DrawSheet getDrawable(Sheet<?,?> sheet) {
        return new DrawSheet(sheet);
    }

    public DrawMasterSheet getDrawable(MasterSheet<?,?> sheet) {
        return new DrawMasterSheet(sheet);
    }

    public DrawTextBox getDrawable(TextBox<?,?> shape) {
        return new DrawTextBox(shape);
    }

    public DrawFreeformShape getDrawable(FreeformShape<?,?> shape) {
        return new DrawFreeformShape(shape);
    }

    public DrawConnectorShape getDrawable(ConnectorShape<?,?> shape) {
        return new DrawConnectorShape(shape);
    }
    
    public DrawTableShape getDrawable(TableShape<?,?> shape) {
        return new DrawTableShape(shape);
    }
    
    public DrawTextShape getDrawable(TextShape<?,?> shape) {
        return new DrawTextShape(shape);
    }

    public DrawGroupShape getDrawable(GroupShape<?,?> shape) {
        return new DrawGroupShape(shape);
    }
    
    public DrawPictureShape getDrawable(PictureShape<?,?> shape) {
        return new DrawPictureShape(shape);
    }
    
    public DrawGraphicalFrame getDrawable(GraphicalFrame<?,?> shape) {
        return new DrawGraphicalFrame(shape);
    }
    
    public DrawTextParagraph getDrawable(TextParagraph<?,?,?> paragraph) {
        return new DrawTextParagraph(paragraph);
    }

    public DrawBackground getDrawable(Background<?,?> shape) {
        return new DrawBackground(shape);
    }
    
    public DrawTextFragment getTextFragment(TextLayout layout, AttributedString str) {
        return new DrawTextFragment(layout, str);
    }
    
    public DrawPaint getPaint(PlaceableShape<?,?> shape) {
        return new DrawPaint(shape);
    }

    /**
     * Convenience method for drawing single shapes.
     * For drawing whole slides, use {@link Slide#draw(Graphics2D)}
     *
     * @param graphics the graphics context to draw to
     * @param shape the shape
     * @param bounds the bounds within the graphics context to draw to 
     */
    public void drawShape(Graphics2D graphics, Shape<?,?> shape, Rectangle2D bounds) {
        Rectangle2D shapeBounds = shape.getAnchor();
        if (shapeBounds.isEmpty() || (bounds != null && bounds.isEmpty())) {
            return;
        }

        AffineTransform txg = (AffineTransform)graphics.getRenderingHint(Drawable.GROUP_TRANSFORM);
        AffineTransform tx = new AffineTransform();
        try {
            if (bounds != null) {
                double scaleX = bounds.getWidth()/shapeBounds.getWidth();
                double scaleY = bounds.getHeight()/shapeBounds.getHeight();
                tx.translate(bounds.getCenterX(), bounds.getCenterY());
                tx.scale(scaleX, scaleY);
                tx.translate(-shapeBounds.getCenterX(), -shapeBounds.getCenterY());
            }
            graphics.setRenderingHint(Drawable.GROUP_TRANSFORM, tx);
            
            Drawable d = getDrawable(shape);
            d.applyTransform(graphics);
            d.draw(graphics);
        } finally {
            graphics.setRenderingHint(Drawable.GROUP_TRANSFORM, txg);
        }
    }
    
    
    /**
     * Replace font families for Windows JVM 6, which contains a font rendering error.
     * This is likely to be removed, when POI upgrades to JDK 7
     *
     * @param graphics the graphics context which will contain the font mapping
     */
    public void fixFonts(Graphics2D graphics) {
        if (!JvmBugs.hasLineBreakMeasurerBug()) return;
        @SuppressWarnings("unchecked")
        Map<String,String> fontMap = (Map<String,String>)graphics.getRenderingHint(Drawable.FONT_MAP);
        if (fontMap == null) {
            fontMap = new HashMap<>();
            graphics.setRenderingHint(Drawable.FONT_MAP, fontMap);
        }
        
        String fonts[][] = {
            { "Calibri", "Lucida Sans" },
            { "Cambria", "Lucida Bright" },
            { "Times New Roman", "Lucida Bright" },
            { "serif", "Lucida Bright" }
        };

        for (String f[] : fonts) {
            if (!fontMap.containsKey(f[0])) {
                fontMap.put(f[0], f[1]);
            }
        }
    }
    
    /**
     * Return a FontManager, either registered beforehand or a default implementation
     *
     * @param graphics the graphics context holding potentially a font manager
     * @return the font manager
     */
    public DrawFontManager getFontManager(Graphics2D graphics) {
        DrawFontManager fontHandler = (DrawFontManager)graphics.getRenderingHint(Drawable.FONT_HANDLER);
        return (fontHandler != null) ? fontHandler : new DrawFontManagerDefault();
    }
}
