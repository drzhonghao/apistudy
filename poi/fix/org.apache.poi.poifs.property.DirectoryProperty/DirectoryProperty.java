

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.poi.poifs.property.Parent;
import org.apache.poi.poifs.property.Property;
import org.apache.poi.poifs.property.PropertyConstants;


public class DirectoryProperty extends Property implements Iterable<Property> , Parent {
	private List<Property> _children;

	private Set<String> _children_names;

	public DirectoryProperty(String name) {
		super();
		_children = new ArrayList<>();
		_children_names = new HashSet<>();
		setName(name);
		setSize(0);
		setPropertyType(PropertyConstants.DIRECTORY_TYPE);
		setStartBlock(0);
		setNodeColor(Property._NODE_BLACK);
	}

	protected DirectoryProperty(final int index, final byte[] array, final int offset) {
		super(index, array, offset);
		_children = new ArrayList<>();
		_children_names = new HashSet<>();
	}

	public boolean changeName(Property property, String newName) {
		boolean result;
		String oldName = property.getName();
		String cleanNewName = property.getName();
		if (_children_names.contains(cleanNewName)) {
			result = false;
		}else {
			_children_names.add(cleanNewName);
			_children_names.remove(oldName);
			result = true;
		}
		return result;
	}

	public boolean deleteChild(Property property) {
		boolean result = _children.remove(property);
		if (result) {
			_children_names.remove(property.getName());
		}
		return result;
	}

	public static class PropertyComparator implements Serializable , Comparator<Property> {
		public int compare(Property o1, Property o2) {
			String VBA_PROJECT = "_VBA_PROJECT";
			String name1 = o1.getName();
			String name2 = o2.getName();
			int result = (name1.length()) - (name2.length());
			if (result == 0) {
				if ((name1.compareTo(VBA_PROJECT)) == 0)
					result = 1;
				else
					if ((name2.compareTo(VBA_PROJECT)) == 0)
						result = -1;
					else {
						if ((name1.startsWith("__")) && (name2.startsWith("__"))) {
							result = name1.compareToIgnoreCase(name2);
						}else
							if (name1.startsWith("__")) {
								result = 1;
							}else
								if (name2.startsWith("__")) {
									result = -1;
								}else
									result = name1.compareToIgnoreCase(name2);



					}

			}
			return result;
		}
	}

	public boolean isDirectory() {
		return true;
	}

	protected void preWrite() {
		if ((_children.size()) > 0) {
			Property[] children = _children.toArray(new Property[0]);
			Arrays.sort(children, new DirectoryProperty.PropertyComparator());
			int midpoint = (children.length) / 2;
			children[0].setPreviousChild(null);
			children[0].setNextChild(null);
			for (int j = 1; j < midpoint; j++) {
				children[j].setPreviousChild(children[(j - 1)]);
				children[j].setNextChild(null);
			}
			if (midpoint != 0) {
				children[midpoint].setPreviousChild(children[(midpoint - 1)]);
			}
			if (midpoint != ((children.length) - 1)) {
				children[midpoint].setNextChild(children[(midpoint + 1)]);
				for (int j = midpoint + 1; j < ((children.length) - 1); j++) {
					children[j].setPreviousChild(null);
					children[j].setNextChild(children[(j + 1)]);
				}
				children[((children.length) - 1)].setPreviousChild(null);
				children[((children.length) - 1)].setNextChild(null);
			}else {
				children[midpoint].setNextChild(null);
			}
		}
	}

	public Iterator<Property> getChildren() {
		return _children.iterator();
	}

	public Iterator<Property> iterator() {
		return getChildren();
	}

	public void addChild(final Property property) throws IOException {
		String name = property.getName();
		if (_children_names.contains(name)) {
			throw new IOException((("Duplicate name \"" + name) + "\""));
		}
		_children_names.add(name);
		_children.add(property);
	}
}

