/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.core.ui.imgviewer.events;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.knime.knip.core.ui.event.KNIPEvent;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PlaneSelectionEvent implements Externalizable, KNIPEvent {

    private long[] m_pos;

    private final StringBuffer m_buffer;

    private int[] m_indices;

    /**
     *
     */
    public PlaneSelectionEvent() {
        m_buffer = new StringBuffer();
    }

    /**
     * @param dimIndex1
     * @param dimIndex2
     * @param pos
     */
    public PlaneSelectionEvent(final int dimIndex1, final int dimIndex2, final long[] pos) {
        m_indices = new int[]{dimIndex1, dimIndex2};
        m_buffer = new StringBuffer();
        m_pos = pos.clone();
    }

    @Override
    public ExecutionPriority getExecutionOrder() {
        return ExecutionPriority.NORMAL;
    }

    /**
     * implements object equality {@inheritDoc}
     */
    @Override
    public <E extends KNIPEvent> boolean isRedundant(final E thatEvent) {
        return this.equals(thatEvent);
    }

    /**
     * @return
     */
    public int numDimensions() {
        return m_pos.length;
    }

    /**
     * @return
     */
    public int getPlaneDimIndex1() {
        return m_indices[0];
    }

    /**
     * @return
     */
    public int getPlaneDimIndex2() {
        return m_indices[1];
    }

    /**
     * @return
     */
    public long[] getPlanePos() {
        return m_pos.clone();
    }

    /**
     * @param pos1
     * @param pos2
     * @return the plane position, whereas the dimensions of the planes are replaced by <code>pos1</code> and
     *         <code>pos2</code>
     */
    public long[] getPlanePos(final long pos1, final long pos2) {
        final long[] res = m_pos.clone();
        res[m_indices[0]] = pos1;
        res[m_indices[1]] = pos2;
        return res;
    }

    /**
     * @param dim
     * @return
     */
    public long getPlanePosAt(final int dim) {
        return m_pos[dim];
    }

    /**
     * @return
     */
    public int[] getDimIndices() {
        return m_indices;
    }

    /**
     * @param i
     * @return
     */
    public FinalInterval getInterval(final Interval i) {

        final long[] min = m_pos.clone();
        final long[] max = m_pos.clone();

        min[m_indices[0]] = i.min(m_indices[0]);
        min[m_indices[1]] = i.min(m_indices[1]);

        max[m_indices[0]] = i.max(m_indices[0]);
        max[m_indices[1]] = i.max(m_indices[1]);

        return new FinalInterval(min, max);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // TODO: Efficency
        m_buffer.setLength(0);
        m_buffer.append(getPlaneDimIndex1());
        m_buffer.append(getPlaneDimIndex2());
        for (int i = 0; i < numDimensions(); i++) {
            m_buffer.append((m_pos[i]) ^ ((m_pos[i]) >>> 32));
        }
        return m_buffer.toString().hashCode();
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        int num = in.readInt();
        m_indices = new int[num];
        for (int i = 0; i < num; i++) {
            m_indices[i] = in.readInt();
        }

        num = in.readInt();
        m_pos = new long[num];

        for (int i = 0; i < m_pos.length; i++) {
            m_pos[i] = in.readLong();
        }
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(m_indices.length);
        for (int i = 0; i < m_indices.length; i++) {
            out.writeInt(m_indices[i]);
        }

        out.writeInt(m_pos.length);

        for (int i = 0; i < m_pos.length; i++) {
            out.writeLong(m_pos[i]);
        }
    }
}
