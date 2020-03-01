package com.xilinx.rapidwright.analysis;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.PIP;
import com.xilinx.rapidwright.analysis.HeatMap;
import com.xilinx.rapidwright.analysis.Gradient;

/**
 * For a given design, plot the congestion heat map
 *
 * @author Ecenur Ustun
 */
class CongestionHeatMap extends JFrame {

  public static void main(String[] args) {
    if(args.length != 1){
      System.out.println("USAGE: java com.xilinx.rapidwright.analysis.CongestionHeatMap <input DCP>");
      System.exit(0);
    }

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
    double[][] data = new double[numCols][numRows];
    for (PIP pip : usedPIPs) {
      Tile pipTile = pip.getTile();
      data[pipTile.getColumn()][pipTile.getRow()]++;
    }

    boolean useGraphicsYAxis = true;

    HeatMap panel = new HeatMap(data, useGraphicsYAxis, Gradient.GRADIENT_HOT);
    panel.setDrawLegend(true);
    panel.setTitle("Congestion");
    panel.setDrawTitle(true);
    panel.setCoordinateBounds(0, numRows - 1, numCols - 1, 0);
    panel.setDrawXTicks(true);
    panel.setDrawYTicks(true);
    panel.setColorForeground(Color.black);
    panel.setColorBackground(Color.white);

    CongestionHeatMap chm = new CongestionHeatMap();
    chm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    chm.setSize(500,500);
    chm.setVisible(true);

    chm.getContentPane().add(panel);
  }

}
