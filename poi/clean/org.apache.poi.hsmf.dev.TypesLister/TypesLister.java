import org.apache.poi.hsmf.dev.*;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.poi.hsmf.datatypes.MAPIProperty;

/**
 * Lists the different MAPI types
 */
public class TypesLister {
   public TypesLister() {}
   
   public void listByName(PrintStream out) {
      ArrayList<MAPIProperty> all = new ArrayList<>(MAPIProperty.getAll());
      all.sort(new Comparator<MAPIProperty>() {
          public int compare(MAPIProperty a, MAPIProperty b) {
              return a.name.compareTo(b.name);
          }
      });
      list(all, out);
   }
   public void listById(PrintStream out) {
      ArrayList<MAPIProperty> all = new ArrayList<>(MAPIProperty.getAll());
      all.sort(new Comparator<MAPIProperty>() {
          public int compare(MAPIProperty a, MAPIProperty b) {
              return Integer.compare(a.id, b.id);
          }
      });
      list(all, out);
   }
   private void list(ArrayList<MAPIProperty> list, PrintStream out) {
      for(MAPIProperty attr : list) {
         String id = Integer.toHexString(attr.id);
         while(id.length() < 4) { id = "0"+id; }
         
         int typeId = attr.usualType.getId();
         String typeIdStr = Integer.toString(typeId);
         if (typeId > 0) {
            typeIdStr = typeIdStr + " / 0x" + Integer.toHexString(typeId);
         }
         
         out.println("0x" + id + " - " + attr.name);
         out.println("   " + attr.id + " - " + attr.usualType.getName() + 
                     " (" + typeIdStr + ") - " + attr.mapiProperty);
      }
   }
   
   public static void main(String[] args) {
      TypesLister lister = new TypesLister();
      
      lister.listByName(System.out);
      System.out.println();
      lister.listById(System.out);
   }
}
