package com.ypcl.visibility;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Picture {
	private Map<String, List<Double>> data;
	private int unit = 1, width = 600, height = 400;
	public Picture(Map<String, List<Double>> data) {
		this.data = data;
	}
	
	public Picture setNumberTickUnit(int n) {
		unit = n;
		return this;
	}
	
	public Picture setSize(int w, int h) {
		width = w;
		height = h;
		return this;
	}
	
	public JFreeChart chart() {
		StandardChartTheme standardChartTheme = new StandardChartTheme("CN");  
		//设置标题字体  
		standardChartTheme.setExtraLargeFont(new Font("隶书", Font.PLAIN, 20));  
		//设置图例的字体  
		standardChartTheme.setRegularFont(new Font("宋书", Font.PLAIN, 15));  
		//设置轴向的字体  
		standardChartTheme.setLargeFont(new Font("宋书", Font.PLAIN, 15));  
		//应用主题样式  
		ChartFactory.setChartTheme(standardChartTheme); 

		JFreeChart chart = ChartFactory.createXYLineChart("", 
				"时间/时", 
				"", 
				createXYDataset(),
				PlotOrientation.VERTICAL, 
				true,     
				true,
				true);		
		XYPlot plot = (XYPlot)chart.getPlot();     
		plot.setBackgroundPaint(Color.white);   
		//设置网格竖线颜色   
		plot.setDomainGridlinePaint(Color.pink);
		plot.setDomainGridlinesVisible(true); 
		//设置网格横线颜色   
		plot.setRangeGridlinePaint(Color.pink);   
		//plot.setBackgroundAlpha(0.5f);   
		// plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
		plot.setWeight(1);
		LegendTitle lt = chart.getLegend();
		lt.setItemFont(new Font(null, Font.PLAIN, 12));
		//lt.setVisible(false);
		XYLineAndShapeRenderer lasp = (XYLineAndShapeRenderer) plot.getRenderer();
		//lasp.setSeriesStroke(0, new BasicStroke(3F));
		//lasp.setSeriesOutlineStroke(0, new BasicStroke(2.0F));
		lasp.setSeriesPaint(0, Color.blue);

		NumberAxis domainAxis = (NumberAxis)plot.getDomainAxis();//x轴设置
		domainAxis.setTickUnit(new NumberTickUnit(unit));
		domainAxis.setRange(0, 24);
		Font xyFont = new Font("黑体", Font.PLAIN, 16);
		domainAxis.setLabelFont(xyFont);
		plot.getRangeAxis().setLabelFont(xyFont);
		//plot.getRangeAxis().setVisible(false);
		plot.getDomainAxis().setVisible(false);
		return chart;
	}

	private XYDataset createXYDataset() {  
		// 创建时间数据源  
		XYSeriesCollection xySeriesCollection = new XYSeriesCollection();  

		for (Entry<String, List<Double>> entry : data.entrySet()) {
			List<Double> list = entry.getValue();
			XYSeries xyserie = new XYSeries(entry.getKey()); 
			int size = list.size();
			int i = 0;
			for (double v : list) {
				xyserie.add(i++ * 24.0 / size, v);  
			}
			xySeriesCollection.addSeries(xyserie);
		}       
		return xySeriesCollection;  
	}  

	public void saveAsFile(File outFile, JFreeChart save,
			int weight, int height) throws IOException {
		FileOutputStream out = null;
		if (!outFile.getParentFile().exists()) {
			outFile.getParentFile().mkdirs();
		}
		out = new FileOutputStream(outFile);
		ChartUtilities.writeChartAsPNG(out, save, weight, height);
		out.flush();
		out.close();
	}
	
	public void show(String path) throws IOException {
		saveAsFile(new File(path), chart(), width, height);
	}
}
