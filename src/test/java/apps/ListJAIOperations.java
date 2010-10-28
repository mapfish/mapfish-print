package apps;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryMode;

public class ListJAIOperations {
  public ListJAIOperations() {
    or = JAI.getDefaultInstance().getOperationRegistry();
    String[] modeNames = RegistryMode.getModeNames();
    String[] descriptorNames;

    for (int i = 0; i < modeNames.length; i++) {
      System.out.println("For registry mode: " + modeNames[i]);

      descriptorNames = or.getDescriptorNames(modeNames[i]);
      for (int j = 0; j < descriptorNames.length; j++) {
        System.out.print("\tRegistered Operator: ");
        System.out.println(descriptorNames[j]);
      }
    }
  }

  public static void main(String[] args) {
    new ListJAIOperations();
  }

  private OperationRegistry or;
}