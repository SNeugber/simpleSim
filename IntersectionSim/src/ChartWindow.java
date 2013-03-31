import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * Demo for {@link XYSeries}, where all the y values are the same.
 *
 */
public class ChartWindow extends ApplicationFrame {
	
	final XYSeries[] dataSeries;
	final JFreeChart chart;
	final String name;

    /**
     * A demonstration application showing an {@link XYSeries} where all the y-values are the same.
     *
     * @param title  the frame title.
     */
    public ChartWindow(final String title, int numSeries, String[] seriesNames, String chartName) {

        super(title);
     	name = chartName;
//        dataSeries = new XYSeries("Test Data");
//        dataSeries2 = new XYSeries("Test2 Data");
        final XYSeriesCollection data = new XYSeriesCollection();
        dataSeries = new XYSeries[numSeries];
        for(int i = 0; i < numSeries; i++) {
        	if(i < seriesNames.length)
        		dataSeries[i] = new XYSeries(seriesNames[i]);
        	else
        		dataSeries[i] = new XYSeries("series " + (i+1));
        	data.addSeries(dataSeries[i]);
        }
        
//        data.addSeries(dataSeries);
//        data.addSeries(dataSeries2);
        chart = ChartFactory.createXYLineChart(
            title,
            "Epochs", 
            "Seconds", 
            data,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        final XYPlot plot = (XYPlot) chart.getPlot();
        final NumberAxis axis = (NumberAxis) plot.getRangeAxis();
        axis.setAutoRangeIncludesZero(false);
        axis.setAutoRangeMinimumSize(1.0);
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1200, 900));
        setContentPane(chartPanel);
        
//        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
//        renderer.setSeriesShapesVisible(0, false);
//        renderer.setSeriesShapesVisible(1, false);
//        plot.setRenderer(renderer);

    }
    
    public String getFileName() {
    	return name;
    }
    
    
    
// private XYDataset createDataset() {
//        
//        final XYSeries series1 = new XYSeries("First");
//        series1.add(1.0, 1.0);
//        series1.add(2.0, 4.0);
//        series1.add(3.0, 3.0);
//        series1.add(4.0, 5.0);
//        series1.add(5.0, 5.0);
//        series1.add(6.0, 7.0);
//        series1.add(7.0, 7.0);
//        series1.add(8.0, 8.0);
//
//        final XYSeries series2 = new XYSeries("Second");
//        series2.add(1.0, 5.0);
//        series2.add(2.0, 7.0);
//        series2.add(3.0, 6.0);
//        series2.add(4.0, 8.0);
//        series2.add(5.0, 4.0);
//        series2.add(6.0, 4.0);
//        series2.add(7.0, 2.0);
//        series2.add(8.0, 1.0);
//
//        final XYSeries series3 = new XYSeries("Third");
//        series3.add(3.0, 4.0);
//        series3.add(4.0, 3.0);
//        series3.add(5.0, 2.0);
//        series3.add(6.0, 3.0);
//        series3.add(7.0, 6.0);
//        series3.add(8.0, 3.0);
//        series3.add(9.0, 4.0);
//        series3.add(10.0, 3.0);
//
//        final XYSeriesCollection dataset = new XYSeriesCollection();
//        dataset.addSeries(series1);
//        dataset.addSeries(series2);
//        dataset.addSeries(series3);
//                
//        return dataset;
//        
//    }
    
    public void addData(int series, int time, float value) {
    	if(series >= 0 && series < dataSeries.length) {
    		dataSeries[series].add(time,value);
    	}
    }
    
    public void refresh() {
    	this.pack();
        //RefineryUtilities.centerFrameOnScreen(this);
    	this.setVisible(true);
    }
    
    public void addData(int time, float value) {
    	dataSeries[0].add(time,value);
    }
    
    public JFreeChart getChart() {
    	return chart;
    }

}
