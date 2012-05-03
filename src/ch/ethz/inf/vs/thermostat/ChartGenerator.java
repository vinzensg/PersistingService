package ch.ethz.inf.vs.thermostat;

import java.awt.Color;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ChartGenerator {
	
	private final String DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";
	
	public ChartGenerator() {
		
	}
	
	public ChartPanel createLineChartDateValue(String[] data_date, Float[] data_value) {
		final XYSeries dataset = buildSeriesDateValue(data_date, data_value);
		final JFreeChart chart = buildChartDateValue(dataset);
		final ChartPanel chartPanel = new ChartPanel(chart);
		return chartPanel;
	}

	private XYSeries buildSeriesDateValue(String[] data_date, Float[] data_value) {
		final XYSeries series = new XYSeries("Data");
		for (int i=0; i<data_date.length; i++) {
			DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			Calendar cal = Calendar.getInstance();
			Date date = null;
			try {
				date = dateFormat.parse(data_date[i]);
			} catch (ParseException e) {
				System.out.println("ParseException: " + e.getMessage());
			}
			cal.setTime(date);
			series.add(cal.getTimeInMillis(), data_value[i]);
		}

		return series;
	}
	
	private JFreeChart buildChartDateValue(final XYSeries series) {
		XYSeriesCollection xyDataset = new XYSeriesCollection(series);
		
		DateAxis dateAxis = new DateAxis("Date");
	    DateTickUnit unit = new DateTickUnit(DateTickUnitType.HOUR,12);

	    DateFormat chartFormatter = new SimpleDateFormat(DATE_FORMAT);
	    dateAxis.setDateFormatOverride(chartFormatter);
	    dateAxis.setTickUnit(unit);

	    NumberAxis valueAxis = new NumberAxis("Temp");

	    StandardXYToolTipGenerator ttg = new StandardXYToolTipGenerator(
	            "{0}: {2}", chartFormatter, NumberFormat.getInstance());

	    StandardXYItemRenderer renderer = new StandardXYItemRenderer(
	            StandardXYItemRenderer.SHAPES_AND_LINES, ttg, null);

	    XYPlot plot = new XYPlot(xyDataset, dateAxis, valueAxis, renderer); 
	    plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		
	    JFreeChart chart = new JFreeChart("Data", 
	    		JFreeChart.DEFAULT_TITLE_FONT, 
	    		plot, 
	    		false);
	    chart.setBackgroundPaint(Color.WHITE);
	    
		return chart;
	}
	
}
