/*
 * Copyright (C) 2013  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

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