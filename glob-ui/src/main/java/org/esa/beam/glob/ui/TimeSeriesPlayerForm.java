package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

/**
 * User: Thomas Storm
 * Date: 30.06.2010
 * Time: 10:49:16
 */
class TimeSeriesPlayerForm extends JPanel {

    private final ImageIcon playIcon = UIUtils.loadImageIcon("icons/Play24.png");
    private final ImageIcon stopIcon = UIUtils.loadImageIcon("icons/Stop24.png");
    private final ImageIcon pauseIcon = UIUtils.loadImageIcon("icons/Pause24.png");
    private final ImageIcon blendIcon = UIUtils.loadImageIcon("icons/Blend24.png");
    private final ImageIcon repeatIcon = UIUtils.loadImageIcon("icons/Repeat24.gif");
    private final ImageIcon minusIcon = UIUtils.loadImageIcon("icons/Remove16.png");
    private final ImageIcon plusIcon = UIUtils.loadImageIcon("icons/Add16.png");

    private JSlider timeSlider;
    private AbstractButton playButton;
    private AbstractButton stopButton;
    private JLabel dateLabel;
    private JSlider speedSlider;
    private JLabel speedLabel;
    private JLabel speedUnit;
    private AbstractButton blendButton;
    private Timer timer;
    private AbstractButton repeatButton;
    private AbstractButton minusButton;
    private AbstractButton plusButton;

    private int stepsPerTimespan = 1;
    private int timerDelay = 1250;
    private AbstractTimeSeries timeSeries;
    private ProductSceneView currentView;
    /**
     * must be different from any character occuring in the date format
     */
    private static final String DATE_SEPARATOR = " ";

    TimeSeriesPlayerForm() {
        this.setLayout(createLayout());
        final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        dateLabel = new JLabel("Date: ");
        timeSlider = createTimeSlider();
        playButton = createPlayButton();
        stopButton = createStopButton();
        repeatButton = createRepeatButton();
        blendButton = createBlendButton();
        speedLabel = new JLabel("Speed:");
        minusButton = createMinusButton();
        speedSlider = createSpeedSlider();
        speedUnit = new JLabel();
        plusButton = createPlusButton();

        updateSpeedUnit();
        setUIEnabled(false);

        buttonsPanel.add(playButton);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(repeatButton);
        buttonsPanel.add(new JSeparator(JSeparator.VERTICAL));
        buttonsPanel.add(blendButton);
        buttonsPanel.add(new JSeparator(JSeparator.VERTICAL));
        buttonsPanel.add(speedLabel);
        buttonsPanel.add(minusButton);
        buttonsPanel.add(speedSlider);
        buttonsPanel.add(plusButton);
        buttonsPanel.add(speedUnit);

        this.add(dateLabel);
        this.add(timeSlider);
        this.add(buttonsPanel);
    }

    List<Band> getBandList(final String rasterName) {
        final String variableName = AbstractTimeSeries.rasterToVariableName(rasterName);
        return timeSeries.getBandsForVariable(variableName);
    }

    void setTimeSeries(AbstractTimeSeries timeSeries) {
        this.timeSeries = timeSeries;
    }

    Timer getTimer() {
        return timer;
    }

    void setView(ProductSceneView view) {
        this.currentView = view;
    }

    int getStepsPerTimespan() {
        return stepsPerTimespan;
    }


    JSlider getTimeSlider() {
        return timeSlider;
    }

    void configureTimeSlider(RasterDataNode raster) {
        if (timeSeries != null) {
            List<Band> bandList = getBandList(raster.getName());

            timeSlider.setMinimum(0);
            final int nodeCount = bandList.size();
            final int maximum = (nodeCount - 1) * stepsPerTimespan;
            timeSlider.setMaximum(maximum);

            final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            if (nodeCount > 1) {
                setUIEnabled(true);
                labelTable.put(0, new JLabel(createSliderLabelFormattedText(bandList, 0)));
                labelTable.put(maximum,
                               new JLabel(createSliderLabelFormattedText(bandList, maximum / stepsPerTimespan)));
                timeSlider.setLabelTable(labelTable);
            } else {
                timeSlider.setLabelTable(null);
                setUIEnabled(false);
            }
            final int index = bandList.indexOf(raster);
            if (index != -1) {
                timeSlider.setValue(index * stepsPerTimespan);
            }
        } else {
            timeSlider.setLabelTable(null);
            setUIEnabled(false);
        }
    }

    private String createSliderLabelText(List<Band> bandList, int index) {
        final ProductData.UTC utcStartTime = bandList.get(index).getTimeCoding().getStartTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String dateText = dateFormat.format(utcStartTime.getAsCalendar().getTime());
        final String timeText = timeFormat.format(utcStartTime.getAsCalendar().getTime());
        return dateText + DATE_SEPARATOR + timeText;
    }

    private String createSliderLabelFormattedText(List<Band> bandList, int index) {
        final String labelText = createSliderLabelText(bandList, index);
        final String[] strings = labelText.split(DATE_SEPARATOR);
        return String.format("<html><p align=\"center\"> <font size=\"2\">%s<br>%s</font></p>", strings[0], strings[1]);
    }

    private JSlider createTimeSlider() {
        final JSlider timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.setMajorTickSpacing(stepsPerTimespan);
        timeSlider.setMinorTickSpacing(1);
        timeSlider.setPaintTrack(true);
        timeSlider.setSnapToTicks(true);
        timeSlider.setPaintTicks(true);
        timeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final int index = timeSlider.getValue() / stepsPerTimespan;
                final List<Band> bandList = getBandList(currentView.getRaster().getName());
                final String labelText = createSliderLabelText(bandList, index);
                dateLabel.setText("Date: " + labelText);
            }
        });
        return timeSlider;
    }

    private AbstractButton createPlayButton() {
        final ActionListener playAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int currentValue = timeSlider.getValue();
                // if slider is on maximum value and repeat button is selected, start from beginning
                if (currentValue == timeSlider.getMaximum() && repeatButton.isSelected()) {
                    currentValue = 0;
                } else if (currentValue == timeSlider.getMaximum() && !repeatButton.isSelected()) {
                    // if slider is on maximum value and repeat button is not selected, stop
                    playButton.setSelected(false);
                    timer.stop();
                    playButton.setIcon(playIcon);
                    playButton.setRolloverIcon(playIcon);
                    currentValue = 0;
                } else {
                    // if slider is not on maximum value, go on
                    currentValue++;
                }
                timeSlider.setValue(currentValue);
            }
        };

        timer = new Timer(timerDelay, playAction);

        final AbstractButton playButton = ToolButtonFactory.createButton(playIcon, false);
        playButton.setToolTipText("Play the time series");
        playButton.setRolloverIcon(playIcon);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playButton.getIcon() == playIcon) {
                    timer.start();
                    playButton.setIcon(pauseIcon);
                    playButton.setRolloverIcon(pauseIcon);
                } else { // pause
                    timer.stop();
                    int newValue = timeSlider.getValue() / stepsPerTimespan * stepsPerTimespan;
                    timeSlider.setValue(newValue);
                    playButton.setIcon(playIcon);
                    playButton.setRolloverIcon(playIcon);
                }
            }
        });
        return playButton;
    }

    private AbstractButton createStopButton() {
        final AbstractButton stopButton = ToolButtonFactory.createButton(stopIcon, false);
        stopButton.setToolTipText("Stop playing the time series");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                timeSlider.setValue(0);
                playButton.setIcon(playIcon);
                playButton.setRolloverIcon(playIcon);
                playButton.setSelected(false);
            }
        });
        return stopButton;
    }

    private AbstractButton createRepeatButton() {
        final AbstractButton repeatButton = ToolButtonFactory.createButton(repeatIcon, true);
        repeatButton.setToolTipText("Toggle repeat");
        return repeatButton;
    }

    private AbstractButton createBlendButton() {
        final AbstractButton blendButton = ToolButtonFactory.createButton(blendIcon, true);
        blendButton.setToolTipText("Toggle blending mode");
        blendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (blendButton.isSelected()) {
                    stepsPerTimespan = 8;
                    timeSlider.setValue(0);
                    timer.setDelay(calculateTimerDelay());
                    configureTimeSlider(currentView.getRaster());
                } else {
                    stepsPerTimespan = 1;
                    timeSlider.setValue(0);
                    timer.setDelay(calculateTimerDelay());
                    configureTimeSlider(currentView.getRaster());
                }
            }
        });
        return blendButton;
    }

    private AbstractButton createMinusButton() {
        final AbstractButton minusButton = ToolButtonFactory.createButton(minusIcon, false);
        minusButton.setToolTipText("Decrease playing speed");
        minusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (speedSlider.getValue() > speedSlider.getMinimum()) {
                    speedSlider.setValue(speedSlider.getValue() - 1);
                }
            }
        });
        return minusButton;
    }

    private JSlider createSpeedSlider() {
        final JSlider speedSlider = new JSlider(1, 10);
        speedSlider.setToolTipText("Choose the playing speed");
        speedSlider.setSnapToTicks(true);
        speedSlider.setPaintTrack(true);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setValue(6);
        speedSlider.setPreferredSize(new Dimension(80, speedSlider.getPreferredSize().height));
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                timerDelay = calculateTimerDelay();
                timer.setDelay(timerDelay);
                updateSpeedUnit();
            }
        });
        return speedSlider;
    }

    private AbstractButton createPlusButton() {
        final AbstractButton plusButton = ToolButtonFactory.createButton(plusIcon, false);
        plusButton.setToolTipText("Increase playing speed");
        plusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (speedSlider.getValue() < speedSlider.getMaximum()) {
                    speedSlider.setValue(speedSlider.getValue() + 1);
                }
            }
        });
        return plusButton;
    }

    private void setUIEnabled(boolean enable) {
        dateLabel.setEnabled(enable);
        timeSlider.setPaintLabels(enable);
        timeSlider.setEnabled(enable);
        playButton.setEnabled(enable);
        stopButton.setEnabled(enable);
        repeatButton.setEnabled(enable);
        blendButton.setEnabled(enable);
        speedLabel.setEnabled(enable);
        minusButton.setEnabled(enable);
        speedSlider.setEnabled(enable);
        speedUnit.setEnabled(enable);
        plusButton.setEnabled(enable);
    }

    private int calculateTimerDelay() {
        return 250 / stepsPerTimespan * (11 - speedSlider.getValue());
    }

    private void updateSpeedUnit() {
        double fps = 1 / (timerDelay * stepsPerTimespan / 1000.0);
        DecimalFormat formatter = new DecimalFormat("0.00");
        speedUnit.setText(formatter.format(fps) + " FPS");
        speedUnit.setToolTipText(formatter.format(fps) + " Frames per second");
    }

    private static TableLayout createLayout() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setRowPadding(0, new Insets(4, 4, 4, 0));
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setRowFill(1, TableLayout.Fill.BOTH);
        return tableLayout;
    }
}
