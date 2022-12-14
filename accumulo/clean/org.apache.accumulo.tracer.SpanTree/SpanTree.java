import org.apache.accumulo.tracer.TraceDump;
import org.apache.accumulo.tracer.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.accumulo.tracer.thrift.RemoteSpan;
import org.apache.htrace.Span;

public class SpanTree {
  final Map<Long,List<Long>> parentChildren = new HashMap<>();
  public final Map<Long,RemoteSpan> nodes = new HashMap<>();

  public SpanTree() {}

  public void addNode(RemoteSpan span) {
    nodes.put(span.spanId, span);
    if (parentChildren.get(span.parentId) == null)
      parentChildren.put(span.parentId, new ArrayList<Long>());
    parentChildren.get(span.parentId).add(span.spanId);
  }

  public Set<Long> visit(SpanTreeVisitor visitor) {
    Set<Long> visited = new HashSet<>();
    List<Long> root = parentChildren.get(Long.valueOf(Span.ROOT_SPAN_ID));
    if (root == null || root.isEmpty())
      return visited;
    RemoteSpan rootSpan = nodes.get(root.iterator().next());
    if (rootSpan == null)
      return visited;
    recurse(0, null, rootSpan, visitor, visited);
    return visited;
  }

  private void recurse(int level, RemoteSpan parent, RemoteSpan node, SpanTreeVisitor visitor,
      Set<Long> visited) {
    // improbable case: duplicate spanId in a trace tree: prevent
    // infinite recursion
    if (visited.contains(node.spanId))
      return;
    visited.add(node.spanId);
    List<RemoteSpan> children = new ArrayList<>();
    List<Long> childrenIds = parentChildren.get(node.spanId);
    if (childrenIds != null) {
      for (Long childId : childrenIds) {
        RemoteSpan child = nodes.get(childId);
        if (child != null) {
          children.add(child);
        }
      }
    }
    children = TraceDump.sortByStart(children);
    visitor.visit(level, parent, node, children);
    for (RemoteSpan child : children) {
      recurse(level + 1, node, child, visitor, visited);
    }
  }
}
