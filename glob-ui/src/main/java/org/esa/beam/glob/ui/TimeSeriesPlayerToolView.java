package org.esa.beam.glob.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

/**
 * User: Thomas Storm
 * Date: 18.06.2010
 * Time: 15:48:31
 */
public class TimeSeriesPlayerToolView extends AbstractToolView {

    public static final String TIME_PROPERTY = "timeProperty";

    private static final String NEXT_IMAGE_LAYER = "nextImageLayer";

    private final ImageIcon playIcon = UIUtils.loadImageIcon("icons/Play24.gif");
    private final ImageIcon stopIcon = UIUtils.loadImageIcon("icons/PlayerStop24.gif");
    private final ImageIcon pauseIcon = UIUtils.loadImageIcon("icons/Pause24.gif");
    private final ImageIcon repeatIcon = UIUtils.loadImageIcon("icons/Repeat24.gif");
    private final ImageIcon minusIcon = UIUtils.loadImageIcon("icons/PlayerMinus16.gif");
    private final ImageIcon plusIcon = UIUtils.loadImageIcon("icons/PlayerPlus16.gif");

    private final SceneViewListener sceneViewListener;

    private final ProductNodeListener productNodeListener;
    private ProductSceneView currentView;

    private JSlider timeSlider;
    private TimeSeries timeSeries;
    private AbstractButton playButton;
    private AbstractButton stopButton;
    private List<Band> bandList;
    private int stepsPerTimespan = 1;
    private JSlider speedSlider;
    private JLabel speedLabel;
    private AbstractButton blendButton;
    private Timer timer;
    private AbstractButton repeatButton;
    private AbstractButton minusButton;
    private AbstractButton plusButton;

    public TimeSeriesPlayerToolView() {
        sceneViewListener = new SceneViewListener();
        productNodeListener = new TimeSeriesProductNodeListener();
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != newView) {
            if (currentView != null) {
                currentView.getProduct().removeProductNodeListener(productNodeListener);
            }
            currentView = newView;
            if (currentView != null) {
                timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
                currentView.getProduct().addProductNodeListener(productNodeListener);
            } else {
                timeSeries = null;
                timer.stop();
            }
            configureTimeSlider();
        }
    }

    @Override
    public void componentShown() {
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);
    }

    @Override
    public void componentHidden() {
        VisatApp.getApp().removeInternalFrameListener(sceneViewListener);
    }

    @Override
    protected JComponent createControl() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setRowWeightY(0, 1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setRowFill(0, TableLayout.Fill.BOTH);
        final JPanel panel = new JPanel(tableLayout);
        timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.setMajorTickSpacing(stepsPerTimespan);
        timeSlider.setMinorTickSpacing(1);
        timeSlider.setPaintTrack(true);
        timeSlider.setSnapToTicks(true);
        timeSlider.addChangeListener(new SliderChangeListener());
        panel.add(timeSlider);
        final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        playButton = ToolButtonFactory.createButton(playIcon, true);
        playButton.setToolTipText( "Play the time series" );
        stopButton = ToolButtonFactory.createButton(stopIcon, false);
        stopButton.setToolTipText( "Stop playing the time series" );
        repeatButton = ToolButtonFactory.createButton(repeatIcon, true);
        repeatButton.setToolTipText( "Toggle repeat" );
        blendButton = new JCheckBox("Show blending");
        blendButton.setToolTipText( "Toggle blending mode" );
        speedLabel = new JLabel("Player speed:");
        minusButton = ToolButtonFactory.createButton( minusIcon, false );
        minusButton.setToolTipText( "Decrease playing speed" );
        speedSlider = new JSlider(1, 10);
        speedSlider.setToolTipText( "Choose the playing speed" );
        speedSlider.setSnapToTicks(true);
        speedSlider.setPaintTrack(true);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setValue(5);
        speedSlider.setPreferredSize(new Dimension(80, speedSlider.getPreferredSize().height));
        plusButton = ToolButtonFactory.createButton( plusIcon, false );
        plusButton.setToolTipText( "Increase playing speed" );

        setUIEnabled(false);

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
                    currentValue = 0;
                } else {
                    // if slider is not on maximum value, go on
                    currentValue++;
                }
                timeSlider.setValue(currentValue);
            }
        };
        timer = new Timer(calculateTimerDelay(), playAction);

        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playButton.isSelected()) {
                    timer.start();
                    playButton.setIcon(pauseIcon);
                } else { // pause
                    timer.stop();
                    int newValue = timeSlider.getValue() / stepsPerTimespan * stepsPerTimespan;
                    timeSlider.setValue(newValue);
                    playButton.setIcon(playIcon);
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                timeSlider.setValue(0);
                playButton.setIcon(playIcon);
                playButton.setSelected(false);
            }
        });

        blendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (blendButton.isSelected()) {
                    stepsPerTimespan = 8;
                    timer.setDelay(calculateTimerDelay());
                    configureTimeSlider();
                } else {
                    stepsPerTimespan = 1;
                    timer.setDelay(calculateTimerDelay());
                    configureTimeSlider();
                }
            }
        });

        minusButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if( speedSlider.getValue() > speedSlider.getMinimum() ) {
                    speedSlider.setValue( speedSlider.getValue() - 1 );
                }
            }
        } );

        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                timer.setDelay(calculateTimerDelay());
            }
        });

        plusButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if( speedSlider.getValue() < speedSlider.getMaximum() ) {
                    speedSlider.setValue( speedSlider.getValue() + 1 );
                }
            }
        } );

        buttonsPanel.add(playButton);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(repeatButton);
        buttonsPanel.add(new JSeparator( JSeparator.VERTICAL ));
        buttonsPanel.add(blendButton);
        buttonsPanel.add(new JSeparator( JSeparator.VERTICAL ));
        buttonsPanel.add(speedLabel);
        buttonsPanel.add(minusButton);
        buttonsPanel.add(speedSlider);
        buttonsPanel.add(plusButton);
        panel.add(buttonsPanel);

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final String viewProductType = view.getProduct().getProductType();
            if (!view.isRGB() && viewProductType.equals(
                    org.esa.beam.glob.core.timeseries.datamodel.TimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                setCurrentView(view);
            }
        }
        return panel;
    }

    private void configureTimeSlider() {
        if (timeSeries != null) {
            final RasterDataNode currentRaster = currentView.getRaster();
            final String variableName = TimeSeries.rasterToVariableName(currentRaster.getName());
            bandList = timeSeries.getBandsForVariable(variableName);

            timeSlider.setMinimum(0);
            final int nodeCount = bandList.size();
            timeSlider.setMaximum((nodeCount - 1) * stepsPerTimespan);


            final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            if (nodeCount > 1) {
                setUIEnabled(true);
                for (int i = 0; i < nodeCount; i++) {
                    final ProductData.UTC utcStartTime = bandList.get(i).getTimeCoding().getStartTime();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    final String dateText = dateFormat.format(utcStartTime.getAsCalendar().getTime());
                    final String timeText = timeFormat.format(utcStartTime.getAsCalendar().getTime());
                    String labelText = String.format("<html><p align=\"center\"> <font size=\"2\">%s<br>%s</font></p>",
                                                     dateText, timeText);
                    final JVertLabel label = new JVertLabel(labelText);
                    labelTable.put(i * stepsPerTimespan, label);
                }
                timeSlider.setLabelTable(labelTable);
            } else {
                timeSlider.setLabelTable(null);
                setUIEnabled(false);
            }
            final int index = bandList.indexOf(currentRaster);
            if (index != -1) {
                timeSlider.setValue(index * stepsPerTimespan);
            }
        } else {
            timeSlider.setLabelTable(null);
            setUIEnabled(false);
        }
    }

    private void setUIEnabled(boolean enable) {
        timeSlider.setPaintLabels(enable);
        timeSlider.setPaintTicks(enable);
        timeSlider.setEnabled(enable);
        playButton.setEnabled(enable);
        stopButton.setEnabled(enable);
        repeatButton.setEnabled( enable );
        blendButton.setEnabled(enable);
        speedLabel.setEnabled(enable);
        minusButton.setEnabled(enable);
        speedSlider.setEnabled(enable);
        plusButton.setEnabled(enable);
    }

    // todo (mp) - The following should be done on ProdsuctSceneView.setRasters()

    private void exchangeRasterInProductSceneView(Band nextRaster, ProductSceneView sceneView) {
        // todo use a real ProgressMonitor
        final RasterDataNode currentRaster = sceneView.getRaster();
        final ImageInfo imageInfoClone = (ImageInfo) currentRaster.getImageInfo(ProgressMonitor.NULL).clone();
        nextRaster.setImageInfo(imageInfoClone);
        reconfigureBaseImageLayer(nextRaster, currentView);
        sceneView.setRasters(new RasterDataNode[]{nextRaster});
        sceneView.setImageInfo(imageInfoClone.createDeepCopy());
        VisatApp.getApp().getSelectedInternalFrame().setTitle(nextRaster.getDisplayName());
    }

    private void reconfigureBaseImageLayer(RasterDataNode rasterDataNode, ProductSceneView sceneView) {
        final Layer rootLayer = sceneView.getRootLayer();
        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                                                                                    ProductSceneView.BASE_IMAGE_LAYER_ID);
        MultiLevelSource multiLevelSource;
        final ImageLayer nextLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer, NEXT_IMAGE_LAYER);
        if (nextLayer != null) {
            multiLevelSource = nextLayer.getMultiLevelSource();

            final List<Layer> children = rootLayer.getChildren();
            final int baseIndex = children.indexOf(baseImageLayer);
            children.remove(baseIndex);
            children.remove(nextLayer);
            nextLayer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);
            nextLayer.setName(rasterDataNode.getDisplayName());
            children.add(baseIndex, nextLayer);
            nextLayer.setTransparency(0);
        } else {
            // todo use a real ProgressMonitor
            multiLevelSource = BandImageMultiLevelSource.create(rasterDataNode, ProgressMonitor.NULL);

            baseImageLayer.setMultiLevelSource(multiLevelSource);
            baseImageLayer.setTransparency(0);
        }

        // TODO why this code  ????
//        baseImageLayer.getConfiguration().setValue(RasterImageLayerType.PROPERTY_NAME_RASTER, rasterDataNode);
//        baseImageLayer.getConfiguration().setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
//        baseImageLayer.setName(rasterDataNode.getDisplayName());
//        try {
//            // todo add comment: what does this code do?
//            final Field sceneImageField = ProductSceneView.class.getDeclaredField("sceneImage");
//            sceneImageField.setAccessible(true);
//            final Object sceneImage = sceneImageField.get(sceneView);
//            final Field multiLevelSourceField = ProductSceneImage.class.getDeclaredField("bandImageMultiLevelSource");
//            multiLevelSourceField.setAccessible(true);
//            multiLevelSourceField.set(sceneImage, multiLevelSource);
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
    }

    private class SliderChangeListener implements ChangeListener {


        @Override
        public void stateChanged(ChangeEvent e) {
            if (currentView == null) {
                return;
            }
            final int currentValue = timeSlider.getValue();
            final float transparency = (currentValue % stepsPerTimespan) / (float) stepsPerTimespan;
            final int currentBandIndex = currentValue / stepsPerTimespan;
            final int newBandIndex = MathUtils.ceilInt(currentValue / (float) stepsPerTimespan);

            if (currentBandIndex == newBandIndex) {
                final Band newRaster = bandList.get(newBandIndex);
                exchangeRasterInProductSceneView(newRaster, currentView);
                currentView.firePropertyChange(TIME_PROPERTY, -1, newBandIndex);
            } else {
                if (bandList.size() > currentBandIndex + 1) {
                    changeTransparency(bandList.get(currentBandIndex + 1), transparency);
                }
            }
        }
    }

    private void changeTransparency(RasterDataNode nextRaster, float transparency) {
        final Layer rootLayer = currentView.getRootLayer();

        ImageLayer nextImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer, NEXT_IMAGE_LAYER);
        if (nextImageLayer == null) {
            RasterDataNode currentRaster = currentView.getRaster();
            final ImageInfo imageInfoClone = (ImageInfo) currentRaster.getImageInfo(ProgressMonitor.NULL).clone();
            nextRaster.setImageInfo(imageInfoClone);

            final BandImageMultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(nextRaster,
                                                                                                ProgressMonitor.NULL);
            nextImageLayer = new ImageLayer(multiLevelSource);
            nextImageLayer.setId(NEXT_IMAGE_LAYER);
            rootLayer.getChildren().add(nextImageLayer);
        }

        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                                                                                    ProductSceneView.BASE_IMAGE_LAYER_ID);
        nextImageLayer.setTransparency(1 - transparency);
        baseImageLayer.setTransparency(transparency);
    }

    private int calculateTimerDelay() {
        return 250 / stepsPerTimespan * (11 - speedSlider.getValue());
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final RasterDataNode viewRaster = view.getRaster();
                final String viewProductType = viewRaster.getProduct().getProductType();
                if (currentView != view && !view.isRGB() && viewProductType.equals(
                        TimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                    setCurrentView(view);
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (currentView == contentPane) {
                setCurrentView(null);
            }
        }
    }

    private class TimeSeriesProductNodeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                configureTimeSlider();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                configureTimeSlider();
            }
        }
    }

    private static class JVertLabel extends JLabel {

        private static final double THETA_PLUS_90 = Math.toRadians(90.0);

        public JVertLabel(String s) {
            super(s);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            //noinspection SuspiciousNameCombination
            return new Dimension(dim.height, dim.width);
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            final Object oldValue = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            AffineTransform oldTransform = g2d.getTransform();
            g2d.rotate(THETA_PLUS_90);
            final int w = getWidth();
            final int h = getHeight();
            g2d.translate(h / 2 - w / 2, -h + w / 2);

            super.paintComponent(g2d);

            g2d.setTransform(oldTransform);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldValue);
        }
    }


}
