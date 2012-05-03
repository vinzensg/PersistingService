package ch.ethz.inf.vs.thermostat;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ThermostatClient extends JFrame {
	
	private final String DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";
	private final String avg = "Average: ";
	private final String max = "Maximum: ";
	private final String min = "Minimum: ";
	
	private PersistingConnection persistingConnection = new PersistingConnection();
	private ChartGenerator chartGenerator = new ChartGenerator();
	
	private JPanel mainPanel = new JPanel();
	private JPanel buttonPanel = new JPanel();
	private JPanel graphPanel = new JPanel();
	
	private JButton updateButton = new JButton("Update");
	private JButton button2 = new JButton("button2");
	private JButton button3 = new JButton("button3");
	private JButton button4 = new JButton("button4");
	
	public ThermostatClient() {
		setTitle("Thermostat Client");
	    setSize(1000, 600);
	    setLocationRelativeTo(null);
	    setDefaultCloseOperation(EXIT_ON_CLOSE);
	    
	    updateButton.addActionListener(new UpdateButtonListener());
		
	    buttonPanel.setLayout(new GridLayout(4, 1));
	    
	    buttonPanel.add(updateButton);
		buttonPanel.add(button2);
		buttonPanel.add(button3);
		buttonPanel.add(button4);
		
		mainPanel.setLayout(new BorderLayout());
		
		mainPanel.add(buttonPanel, BorderLayout.EAST);
		mainPanel.add(graphPanel, BorderLayout.CENTER);
		
		getContentPane().add(mainPanel);
		setVisible(true);
	}	
	
	
	class UpdateButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			graphPanel.setLayout(new BorderLayout());
			
			DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			Date date = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_YEAR, -3);
			
			Object[][] data = persistingConnection.getSinceDateValue("2012/04/27-00:00:00"); // dateFormat.format(cal.getTime())
			String[] data_x = (String[]) data[0];
			Float[] data_y = (Float[]) data[1];
			graphPanel.add(chartGenerator.createLineChartDateValue(data_x, data_y), BorderLayout.CENTER);
			
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new GridLayout(3, 1));
			labelPanel.add(new JLabel("Average: " + persistingConnection.getSinceAvg("2012/04/27-00:00:00")));
			labelPanel.add(new JLabel("Maximum: " + persistingConnection.getSinceMax("2012/04/27-00:00:00")));
			labelPanel.add(new JLabel("Minimum: " + persistingConnection.getSinceMin("2012/04/27-00:00:00")));
			
			graphPanel.add(labelPanel, BorderLayout.SOUTH);
			
			mainPanel.add(graphPanel, BorderLayout.CENTER);
			mainPanel.revalidate();
		}		
	}
	
	public static void main(String[] args) {
		new ThermostatClient();
	}
}
