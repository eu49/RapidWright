package com.xilinx.rapidwright.analysis;

import java.lang.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.design.NetType;

/**
 * For a given design, find the nets with highest fanout <br>
 * Report highest fanout, corresponding nets, and their types in a text file
 *
 * @author Ecenur Ustun
 */
public class HighFanoutNets {

  public static void main(String[] args) throws IOException {
    if(args.length != 2){
      System.out.println("USAGE: java com.xilinx.rapidwright.analysis.HighFanoutNets <input DCP> <output file>");
      System.exit(0);
    }

    try {
      // Open checkpoint
      Design design = Design.readCheckpoint(args[0]);
      // Get all nets of the design
      Collection<Net> allNets = design.getNets();

      // Create a map from nets to their fanout
      HashMap<String, Integer> fanouts = new HashMap<String, Integer>();
      for (Net net : allNets) {
        fanouts.put(net.getName(), net.getFanOut());
      }

      // Get highest fanout
      int maxFanout = Collections.max(fanouts.values());

      // Report highest fanout, corresponding nets, and their types
      BufferedWriter writer = new BufferedWriter(new FileWriter(args[1], true));
      writer.write("Design: " + args[0] + "\nMaximum fanout: " + maxFanout + "\nNets with maximum fanout:\n");
      for (String name : fanouts.keySet()) {
        if (fanouts.get(name) == maxFanout) {
          writer.write(name + " " + design.getNet(name).getType() + "\n");
        }
      }
      writer.write("\n");
      writer.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

  }

}
