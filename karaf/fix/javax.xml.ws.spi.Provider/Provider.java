

import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.spi.Invoker;
import javax.xml.ws.spi.ServiceDelegate;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import org.w3c.dom.Element;


public abstract class Provider {
	private static final String DEFAULT_JAXWSPROVIDER = "com.sun.xml.internal.ws.spi.ProviderImpl";

	protected Provider() {
	}

	public static Provider provider() {
		try {
		} catch (WebServiceException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new WebServiceException("Unable to createEndpointReference Provider", ex);
		}
		return null;
	}

	public abstract ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class<? extends Service> serviceClass);

	public ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class<? extends Service> serviceClass, WebServiceFeature... features) {
		throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
	}

	public abstract Endpoint createEndpoint(String bindingId, Object implementor);

	public abstract Endpoint createAndPublishEndpoint(String address, Object implementor);

	public abstract EndpointReference readEndpointReference(Source eprInfoset);

	public abstract <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface, WebServiceFeature... features);

	public abstract W3CEndpointReference createW3CEndpointReference(String address, QName serviceName, QName portName, List<Element> metadata, String wsdlDocumentLocation, List<Element> referenceParameters);

	public W3CEndpointReference createW3CEndpointReference(String address, QName interfaceName, QName serviceName, QName portName, List<Element> metadata, String wsdlDocumentLocation, List<Element> referenceParameters, List<Element> elements, Map<QName, String> attributes) {
		throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
	}

	public Endpoint createAndPublishEndpoint(String address, Object implementor, WebServiceFeature... features) {
		throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
	}

	public Endpoint createEndpoint(String bindingId, Object implementor, WebServiceFeature... features) {
		throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
	}

	public Endpoint createEndpoint(String bindingId, Class<?> implementorClass, Invoker invoker, WebServiceFeature... features) {
		throw new UnsupportedOperationException("JAX-WS 2.2 implementation must override this default behaviour.");
	}
}

