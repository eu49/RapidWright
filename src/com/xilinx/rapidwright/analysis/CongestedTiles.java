package com.xilinx.rapidwright.analysis;

import java.lang.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.PIP;

/**
 * For a given design, find the top ten congested INT tiles
 *
 * @author Ecenur Ustun
 */
public class CongestedTiles {

  public static void main(String[] args) {
    if(args.length != 1){
      System.out.println("USAGE: java com.xilinx.rapidwright.analysis.CongestedTiles <input DCP>");
      System.exit(0);
    }

    // Open checkpoint
    Design design = Design.readCheckpoint(args[0]);
    Device device = design.getDevice();
    int numRows = device.getRows();
    int numCols = device.getColumns();

    // Get all used PIPs
    Collection<Net> allNets = design.getNets();
    Set<PIP> usedPIPs = new HashSet<PIP>();
    for (Net net : allNets) {
      List<PIP> netPIPs = net.getPIPs();
      usedPIPs.addAll(netPIPs);
    }

    // Get PIP usage of tiles 
    int[][] data = new int[numRows][numCols];
    for (PIP pip : usedPIPs) {
      Tile pipTile = pip.getTile();
      data[pipTile.getRow()][pipTile.getColumn()]++;
    }

    // Get tiles
    Collection<Tile> allTiles = design.getDevice().getAllTiles();

    // Create a map from INT tiles to their congestion
    HashMap<String, Integer> congestion = new HashMap<String, Integer>();
    ArrayList<Integer> values = new ArrayList<Integer>();
    for (Tile tile : allTiles) {
      if (tile.getTileTypeEnum().equals(TileTypeEnum.INT)) {
        congestion.put(tile.getName(), data[tile.getRow()][tile.getColumn()]);
        values.add(data[tile.getRow()][tile.getColumn()]);
      }
    }

    // Get most congested 10 INT tiles
    Collections.sort(values, Collections.reverseOrder());
    ArrayList<String> sortedTiles = new ArrayList<String>();
    int count = 0;
    outer: for (Integer i : values) {
      for (String name : congestion.keySet()) {
        if (congestion.get(name).equals(i)) {
          if (!sortedTiles.contains(name)) {
            sortedTiles.add(name);
            count += 1;
            if (count == 10) break outer;
          }
        }
      }
    }

    System.out.println("Most congested 10 INT tiles:");
    for (String i : sortedTiles) {
      System.out.println(i + " " + Integer.toString(congestion.get(i)));
    }
  }

}
