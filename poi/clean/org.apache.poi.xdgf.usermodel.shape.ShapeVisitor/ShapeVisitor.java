import org.apache.poi.xdgf.usermodel.shape.ShapeVisitorAcceptor;
import org.apache.poi.xdgf.usermodel.shape.*;


import java.awt.geom.AffineTransform;

import org.apache.poi.xdgf.usermodel.XDGFShape;

/**
 * Used to iterate through shapes
 *
 * To change the behavior of a particular visitor, you can override either
 * accept() or getAcceptor() [preferred]
 *
 * If accept() or visit() throw StopVisitingThisBranch, the iteration will not
 * visit subshapes of the shape.
 */
public abstract class ShapeVisitor {

    protected ShapeVisitorAcceptor _acceptor;

    public ShapeVisitor() {
        _acceptor = getAcceptor();
    }
    
    /**
     * Is only called on construction of the visitor, allows
     * mixing visitors and acceptors
     */
    protected ShapeVisitorAcceptor getAcceptor() {
        return new ShapeVisitorAcceptor() {
            @Override
            public boolean accept(XDGFShape shape) {
                return !shape.isDeleted();
            }
        };
    }

    public void setAcceptor(ShapeVisitorAcceptor acceptor) {
        _acceptor = acceptor;
    }

    public boolean accept(XDGFShape shape) {
        return _acceptor.accept(shape);
    }

    /**
     * @param shape
     *            Current shape
     * @param globalTransform
     *            A transform that can convert the shapes points to global
     *            coordinates
     * @param level
     *            Level in the tree (0 is topmost, 1 is next level...
     */
    public abstract void visit(XDGFShape shape,
            AffineTransform globalTransform, int level);

}
