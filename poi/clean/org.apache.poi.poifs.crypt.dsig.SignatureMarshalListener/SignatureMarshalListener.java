import org.apache.poi.poifs.crypt.dsig.SignatureConfig;
import org.apache.poi.poifs.crypt.dsig.*;


import static org.apache.poi.poifs.crypt.dsig.facets.SignatureFacet.OO_DIGSIG_NS;
import static org.apache.poi.poifs.crypt.dsig.facets.SignatureFacet.XML_NS;

import org.apache.poi.poifs.crypt.dsig.SignatureConfig.SignatureConfigurable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MutationEvent;

/**
 * This listener class is used, to modify the to be digested xml document,
 * e.g. to register id attributes or set prefixes for registered namespaces
 */
public class SignatureMarshalListener implements EventListener, SignatureConfigurable {
    ThreadLocal<EventTarget> target = new ThreadLocal<>();
    SignatureConfig signatureConfig;
    public void setEventTarget(EventTarget target) {
        this.target.set(target);
    }
    
    @Override
    public void handleEvent(Event e) {
        if (!(e instanceof MutationEvent)) {
            return;
        }
        MutationEvent mutEvt = (MutationEvent)e;
        EventTarget et = mutEvt.getTarget();
        if (!(et instanceof Element)) {
            return;
        }
        handleElement((Element)et);
    }

    public void handleElement(Element el) {
        EventTarget target = this.target.get();

        if (el.hasAttribute("Id")) {
            el.setIdAttribute("Id", true);
        }

        setListener(target, this, false);
        if (OO_DIGSIG_NS.equals(el.getNamespaceURI())) {
            String parentNS = el.getParentNode().getNamespaceURI();
            if (!OO_DIGSIG_NS.equals(parentNS) && !el.hasAttributeNS(XML_NS, "mdssi")) {
                el.setAttributeNS(XML_NS, "xmlns:mdssi", OO_DIGSIG_NS);
            }
        }
        setPrefix(el);
        setListener(target, this, true);
    }

    // helper method to keep it in one place
    public static void setListener(EventTarget target, EventListener listener, boolean enabled) {
        String type = "DOMSubtreeModified";
        boolean useCapture = false;
        if (enabled) {
            target.addEventListener(type, listener, useCapture);
        } else {
            target.removeEventListener(type, listener, useCapture);
        }
    }
    
    protected void setPrefix(Node el) {
        String prefix = signatureConfig.getNamespacePrefixes().get(el.getNamespaceURI());
        if (prefix != null && el.getPrefix() == null) {
            el.setPrefix(prefix);
        }
        
        NodeList nl = el.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            setPrefix(nl.item(i));
        }
    }
    
    @Override
    public void setSignatureConfig(SignatureConfig signatureConfig) {
        this.signatureConfig = signatureConfig;
    }
}
