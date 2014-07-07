/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.knip.base.nodes.util.slicelooper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;

/**
 *
 * @author Andreas Graumann, University of Konstanz
 * @author Christian Dietz, University of Konstanz
 * @param <T>
 * @param <L>
 */
public class SliceIteratorLoopStartNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> extends NodeModel
        implements LoopStartNodeTerminator, BufferedDataTableHolder {

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected SliceIteratorLoopStartNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /**
     * Dimension selction
     */
    private SettingsModelDimSelection m_dimSelection = createDimSelection();

    /**
     * Selected columns using column selection option
     */
    private final SettingsModelFilterString m_columns = createColumnSelectionModel();

    /**
     * the current index
     */
    private int m_currIdx = 0;

    /**
     * table iterator
     */
    private CloseableRowIterator m_iterator;

    /**
     * Current intervals
     */
    private Interval[] m_intervals;

    /**
     * is one row terminated
     */
    private boolean isRowTerminated;

    /**
     * are all rows terminated
     */
    private boolean areAllRowsTerminated;

    /**
     * factory to create ImgPlusCells
     */
    private ImgPlusCellFactory m_imgPlusCellFactory;

    /**
     * factory to create LabelingCells
     */
    private LabelingCellFactory m_labelingCellFactory;

    /**
     * the current row of the iterator
     */
    private DataRow m_currentRow;

    /**
     * all column indices
     */
    private ArrayList<Integer> m_colIndices;

    /**
     * reference interval for slice loop end node
     */
    private Interval m_refValue;

    /**
     * reference calibrated space for slice loop end node
     */
    private CalibratedSpace<CalibratedAxis> m_refSpace;

    private BufferedDataTable m_data;

    /**
     * Node logger
     */
    static NodeLogger LOGGER = NodeLogger.getLogger(SliceIteratorLoopStartNodeModel.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        getSelectedColumnIndices(inSpecs[0]);
        return new DataTableSpec[]{SliceIteratorUtils.createResSpec(inSpecs[0], m_columns, LOGGER)};
    }

    /**
     * @return settings model for chunk size
     */
    protected static SettingsModelInteger createChunkSizeModel() {
        return new SettingsModelInteger("chunk_size", 1);
    }

    /**
     * @return settings model for dim selection
     */
    protected static SettingsModelDimSelection createDimSelection() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    /**
     * @return settings model for the column selection
     */
    public static SettingsModelFilterString createColumnSelectionModel() {
        return new SettingsModelFilterString("column_selection");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        // check input spec
        final DataTableSpec inSpec = inData[0].getSpec();

        // fill m_columns
        getSelectedColumnIndices(inSpec);

        // create output spec
        final DataTableSpec outSpec = SliceIteratorUtils.createResSpec(inSpec, m_columns, LOGGER);

        // we just start, row is not terminated yet
        isRowTerminated = false;

        // no iterator, yet!
        if (m_iterator == null) {
            // get input table
            final BufferedDataTable inTable = inData[0];
            // get iterator
            m_iterator = inTable.iterator();
            // get the first row of the iterator
            if (m_iterator.hasNext()) {
                m_currentRow = m_iterator.next();
            }
        }

        // loop over all columns, init intervals and check compatibility
        if (m_currIdx == 0) {

            // store all column indices
            m_colIndices = new ArrayList<Integer>();

            // we do not have a reference value yet
            m_refValue = null;

            // loop over all selected and valied cloumns
            for (int j : SliceIteratorUtils.getSelectedAndValidIdx(inSpec, m_columns, LOGGER)) {
                final DataColumnSpec colSpec = inSpec.getColumnSpec(j);
                final DataType colType = colSpec.getType();

                Interval value = null;
                CalibratedSpace<CalibratedAxis> axes = null;

                // This column contains images
                if (colType.isCompatible(ImgPlusValue.class)) {

                    @SuppressWarnings("unchecked")
                    final ImgPlusValue<T> imgPlusValue = ((ImgPlusValue<T>)m_currentRow.getCell(j));
                    value = imgPlusValue.getImgPlus();
                    axes = imgPlusValue.getMetadata();
                    if (m_imgPlusCellFactory == null) {
                        m_imgPlusCellFactory = new ImgPlusCellFactory(exec);
                    }
                }
                // this column contains labelings
                else if (colType.isCompatible(LabelingValue.class)) {
                    @SuppressWarnings("unchecked")
                    final LabelingValue<T> labelingValue = ((LabelingValue<T>)m_currentRow.getCell(j));
                    value = labelingValue.getLabeling();
                    axes = labelingValue.getLabelingMetadata();
                    if (m_labelingCellFactory == null) {
                        m_labelingCellFactory = new LabelingCellFactory(exec);
                    }
                    // negativ index mark column has labeling
                    j *= -1;
                }

                // first column, store value and axes as reference for slice node end
                if (value != null && m_colIndices.size() == 0) {
                    m_refValue = value;
                    m_refSpace = axes;
                    m_intervals = m_dimSelection.getIntervals(axes, value);
                }

                // we do nothing but warn
                if (m_refValue != null && !Intervals.equalDimensions(m_refValue, value)) {
                    value = null;
                    LOGGER.warn("We ignored img/labeling in column " + j
                            + " as the dimensions are to compatible to a previously discovered img/labeling");
                }

                if (value != null) {
                    m_colIndices.add(j);
                }
            }
        }

        // create outpur container
        final BufferedDataContainer container = exec.createDataContainer(outSpec);

        // create view with slices, depending on chunk size
        for (int i = m_currIdx; i < (Math.min(m_currIdx + 1, m_intervals.length)); i++) {

            final DataCell[] cells = new DataCell[m_colIndices.size()];

            for (int j = 0; j < cells.length; j++) {
                final Interval currInterval = m_intervals[i];
                int idx = m_colIndices.get(j);

                // idx is negative, we have a labeling
                if (Math.signum(idx) < 0) {
                    @SuppressWarnings("unchecked")
                    final LabelingValue<L> value = ((LabelingValue<L>)m_currentRow.getCell(idx * -1));
                    final Labeling<L> labeling = value.getLabeling();
                    final LabelingMetadata inMetadata = value.getLabelingMetadata();

                    final Labeling<L> outLabeling =
                            SliceIteratorUtils.getLabelingAsView(labeling, currInterval, labeling.<L> factory());
                    final int deltaDim = labeling.numDimensions() - outLabeling.numDimensions();

                    final LabelingMetadata outMetadata =
                            new DefaultLabelingMetadata(inMetadata.numDimensions() - deltaDim,
                                    inMetadata.getLabelingColorTable());

                    MetadataUtil.copyName(inMetadata, outMetadata);
                    MetadataUtil.copySource(inMetadata, outMetadata);
                    MetadataUtil.copyAndCleanTypedSpace(currInterval, inMetadata, outMetadata);

                    cells[j] = m_labelingCellFactory.createCell(outLabeling, outMetadata);
                } else {
                    @SuppressWarnings("unchecked")
                    final ImgPlusValue<T> value = ((ImgPlusValue<T>)m_currentRow.getCell(idx));
                    final ImgPlus<T> imgPlus = value.getImgPlus();
                    final ImgPlusMetadata inMetadata = value.getMetadata();

                    final Img<T> outImg = SliceIteratorUtils.getImgPlusAsView(imgPlus, currInterval, imgPlus.factory());
                    final int deltaDim = imgPlus.numDimensions() - outImg.numDimensions();

                    final ImgPlusMetadata outMetadata =
                            MetadataUtil.copyAndCleanImgPlusMetadata(currInterval, inMetadata, new DefaultImgMetadata(
                                    inMetadata.numDimensions() - deltaDim));

                    MetadataUtil.copyName(inMetadata, outMetadata);
                    MetadataUtil.copySource(inMetadata, outMetadata);
                    MetadataUtil.copyAndCleanTypedSpace(currInterval, inMetadata, outMetadata);

                    cells[j] = m_imgPlusCellFactory.createCell(outImg, outMetadata);
                }
            }
            container.addRowToTable(new DefaultRow("Slice " + i, cells));

            pushFlowVariableInt("Slice", i);
        }

        m_currIdx = (Math.min(m_currIdx + 1, m_intervals.length));
        // are we finished?
        if (m_currIdx >= m_intervals.length) {
            isRowTerminated = true;
            areAllRowsTerminated = !m_iterator.hasNext();

            if (!areAllRowsTerminated && m_iterator.hasNext()) {
                //m_intervals = null;
                m_currentRow = m_iterator.next();
                m_currIdx = 0;
            }
        }

        container.close();
        m_data = container.getTable();
        return new BufferedDataTable[]{m_data};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currIdx = 0;
        m_intervals = null;
        isRowTerminated = false;
        areAllRowsTerminated = false;
        m_iterator = null;
        m_refValue = null;
        m_imgPlusCellFactory = null;
        m_labelingCellFactory = null;
        m_refSpace = null;
        m_refValue = null;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_dimSelection.saveSettingsTo(settings);
        m_columns.saveSettingsTo(settings);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dimSelection.validateSettings(settings);
        m_columns.validateSettings(settings);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dimSelection.loadSettingsFrom(settings);
        m_columns.loadSettingsFrom(settings);
    }

    /**
     *
     * @return reference interval
     */
    public Interval[] getRefIntervals() {
        return m_intervals;
    }

    /**
     * @return dimension of the result image in slice loop end node
     */
    long[] getResDimensions(final Interval resInterval) {
        long[] dims = new long[m_refValue.numDimensions()];
        m_refValue.dimensions(dims);

        int[] selectedDimIndices = m_dimSelection.getSelectedDimIndices(m_refSpace);

        int i = 0;
        for (final int j : selectedDimIndices) {
            dims[j] = resInterval.dimension(i++);
        }

        return dims;
    }

    /**
     *
     * @return reference calibrated space
     */
    CalibratedSpace<CalibratedAxis> getRefSpace() {
        return m_refSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return areAllRowsTerminated;
    }

    /**
     * @return is the current row terminated
     */
    public boolean isRowTerminated() {
        return isRowTerminated;
    }

    private int[] getSelectedColumnIndices(final DataTableSpec inSpec) {
        final List<String> colNames;
        if ((m_columns.getIncludeList().size() == 0) || m_columns.isKeepAllSelected()) {
            colNames = new ArrayList<String>();
            collectAllColumns(colNames, inSpec);
            m_columns.setIncludeList(colNames);

        } else {
            colNames = new ArrayList<String>();
            colNames.addAll(m_columns.getIncludeList());
            if (!validateColumnSelection(colNames, inSpec)) {
                setWarningMessage("Invalid column selection. All columns are selected!");
                collectAllColumns(colNames, inSpec);
            }
        }

        // get column indices
        final List<Integer> colIndices = new ArrayList<Integer>(colNames.size());
        for (int i = 0; i < colNames.size(); i++) {
            final int colIdx = inSpec.findColumnIndex(colNames.get(i));
            if (colIdx == -1) {
                // can not occur, actually
                LOGGER.warn("Column " + colNames.get(i) + " doesn't exist!");
            } else {
                colIndices.add(colIdx);
            }
        }

        final int[] colIdx = new int[colIndices.size()];
        for (int i = 0; i < colIdx.length; i++) {
            colIdx[i] = colIndices.get(i);
        }

        return colIdx;
    }

    /* Helper to collect all columns of the given type. */
    private void collectAllColumns(final List<String> colNames, final DataTableSpec spec) {
        colNames.clear();
        for (final DataColumnSpec c : spec) {
            if (c.getType().isCompatible(ImgPlusValue.class) || c.getType().isCompatible(LabelingValue.class)) {
                colNames.add(c.getName());
            }
        }
        if (colNames.size() == 0) {
            LOGGER.warn("No columns of type " + ImgPlusValue.class.getSimpleName() + " or "
                    + LabelingValue.class.getSimpleName() + " available!");
            return;
        }
        LOGGER.info("All available columns of type " + ImgPlusValue.class.getSimpleName() + " and "
                + LabelingValue.class.getSimpleName() + " are selected!");

    }

    /* Checks if a column is not present in the DataTableSpec */
    private boolean validateColumnSelection(final List<String> colNames, final DataTableSpec spec) {
        for (int i = 0; i < colNames.size(); i++) {
            final int colIdx = spec.findColumnIndex(colNames.get(i));
            if (colIdx == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];
    }
}
