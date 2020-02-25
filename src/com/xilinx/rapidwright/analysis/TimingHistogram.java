package com.xilinx.rapidwright.analysis;

import java.lang.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.ConstraintGroup;
import com.xilinx.rapidwright.timing.TimingManager;
import com.xilinx.rapidwright.timing.TimingGraph;
import com.xilinx.rapidwright.timing.TimingEdge;
import com.xilinx.rapidwright.timing.TimingVertex;
import org.jgrapht.GraphPath;

/**
 * For a given design, create the timing histogram
 *
 * @author Ecenur Ustun
 */
public class TimingHistogram {

  public static void main(String[] args) {
    if(args.length != 2){
      System.out.println("USAGE: java com.xilinx.rapidwright.analysis.TimingHistogram <input DCP> <bin count>");
      System.exit(0);
    }

    // Open checkpoint
    Design design = Design.readCheckpoint(args[0]);

    // Read clock period in ps from xdc constraints
    float period = 0.0f;
    outer: for(ConstraintGroup cg : ConstraintGroup.values()){
      List<String> lines = design.getXDCConstraints(cg);
      for (String line : lines) {
        if (line.indexOf("create_clock") !=-1) {
          period = 1000 * Float.valueOf(line.replaceAll("[^0-9?!\\.]",""));
          break outer;
        }
      }
    }
    System.out.println("\nTarget clock period: " + Float.toString(period / 1000) + " ns");

    // Open timing manager
    TimingManager timing = new TimingManager(design);

    // Get paths of timing graph
    TimingGraph tg = timing.getTimingGraph();
    tg.setTimingRequirement(period);
    HashSet<GraphPath<TimingVertex, TimingEdge>> paths = tg.getGraphPaths();
    System.out.println("Number of paths in the timing graph: " + Integer.toString(paths.size()));
    //System.out.println("Timing paths of the design:");
    //System.out.println(paths);
    System.out.println("Worst slack in the timing graph: " + Float.toString(tg.getWorstSlack() / 1000) + " ns");

    // Get slacks of paths in ns
    ArrayList<Float> slacks = new ArrayList<Float>();
    for (GraphPath<TimingVertex, TimingEdge> path : paths) {
      System.out.println("\nTiming path:");
      System.out.println(path);
      System.out.println("tg.getSlack(path) method on this path returns: " + Float.toString(tg.getSlack(path) / 1000) + " ns");
      TimingVertex dest = path.getEndVertex();
      Float slack = dest.getSlack() / 1000;
      System.out.println("Slack on the dest vertex of this path: " + Float.toString(dest.getSlack() / 1000) + " ns\n");
      slacks.add(slack);
    }
    //System.out.println("All slack values (ns):");
    //System.out.println(slacks);

    // Create timing histogram
    int binCount = Integer.valueOf(args[1]);
    if (slacks.size() > 1) {
      float minSlack = Collections.min(slacks);
      float maxSlack = Collections.max(slacks);
      float width = (maxSlack - minSlack) / binCount;

      HashMap<Integer, Integer> slackCount = new HashMap<Integer, Integer>();
      Iterator<Float> itr = slacks.iterator();
      for (int i = 0; i < binCount; i++) {
        slackCount.put(i, 0);
        while (itr.hasNext()) {
          float f = itr.next();
          if ( (f >= minSlack + i * width) && (f <= minSlack + (i + 1) * width) ) {
            slackCount.put(i, slackCount.get(i) + 1);
            itr.remove();
          }
        }
      }
      //System.out.println(slackCount);
      for (int i = 0; i < binCount; i++) {
        String bar = Float.toString(minSlack + i * width) + " - " + Float.toString(minSlack + (i + 1) * width) + " : ";
        for (int j = 0; j < slackCount.get(i); j++) {
          bar += "*";
        }
        System.out.println(bar);
      }
    }
    else {System.out.println("Design has one timing path with slack " + Float.toString(slacks.get(0)) + " ns.");}

  }

}
