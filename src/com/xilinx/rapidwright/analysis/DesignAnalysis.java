package com.xilinx.rapidwright.analysis;

import java.lang.*;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Cell;
import com.xilinx.rapidwright.design.ConstraintGroup;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.timing.TimingManager;
import com.xilinx.rapidwright.timing.TimingGraph;

/**
 * Report resource utilization and timing of all designs in the specified directory in a CSV file
 *
 * @author Ecenur Ustun
 */
public class DesignAnalysis {

  public static void main(String[] args) throws IOException {
    if(args.length != 1){
      System.out.println("USAGE: java com.xilinx.rapidwright.analysis.DesignAnalysis <input directory>");
      System.exit(0);
    }

    // Get all designs in the directory
    File folder = new File(args[0]);
    File[] listOfFiles = folder.listFiles();
    ArrayList<String> dcpFiles = new ArrayList<String>();
    for (File file : listOfFiles) {
      if (file.isFile() && file.getName().indexOf(".dcp") != -1) {
        dcpFiles.add(file.getName());
      }
    }

    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(args[0] + "/design_analysis.csv"));
      writer.write("Design Name, LUT, Register, CARRY8, MUX, CLB, DSP, BRAM, Clock Period (ns), Worst Slack (ns)\n");
      for (String dcpFile: dcpFiles) {
        // Open checkpoint
        Design design = Design.readCheckpoint(args[0] + "/" + dcpFile);

        // Get resource utilization
        Collection<Cell> cells = design.getCells();
        int lut = 0;
        int reg = 0;
        int carry = 0;
        int mux = 0;
        ArrayList<Site> usedSites = new ArrayList<Site>();
        for (Cell cell : cells) {
          String cellType = cell.getType();
          if (cellType.startsWith("LUT")) {lut++;}
          else if (cellType.startsWith("FD")) {reg++;}
          else if (cellType.startsWith("CARRY")) {carry++;}
          else if (cellType.startsWith("MUX")) {mux++;}
          if (!usedSites.contains(cell.getSite())) {usedSites.add(cell.getSite());}
        }
        int clb = 0;
        int dsp = 0;
        int bram = 0;
        for (Site site : usedSites) {
          String siteName = site.getName();
          if (siteName.startsWith("SLICE")) {clb++;}
          else if (siteName.startsWith("DSP")) {dsp++;}
          else if (siteName.startsWith("RAMB")) {bram++;}
        }

        // Read clock period in ns from xdc constraints
        float period = 0.0f;
        outer: for(ConstraintGroup cg : ConstraintGroup.values()){
          List<String> lines = design.getXDCConstraints(cg);
          for (String line : lines) {
            if (line.indexOf("create_clock") != -1) {
              period = Float.valueOf(line.replaceAll("[^0-9?!\\.]",""));
              break outer;
            }
          }
        }

        // Get the worst slack of the design
        TimingManager timing = new TimingManager(design);
        TimingGraph tg = timing.getTimingGraph();
        tg.setTimingRequirement(period * 1000);
        double slack = Math.round((tg.getWorstSlack() / 1000) * 100.0) / 100.0;

        writer.write(dcpFile.replace(".dcp","") + ", " + Integer.toString(lut) + ", " + Integer.toString(reg) + ", " + Integer.toString(carry) + ", " + Integer.toString(mux) + ", " + Integer.toString(clb) + ", " + Integer.toString(dsp) + ", " + Integer.toString(bram) + ", " + Float.toString(period) + ", " + Double.toString(slack) + "\n");

      }
      writer.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

  }

}
