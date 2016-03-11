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
package org.knime.knip.base.nodes.view;

import java.util.List;

import javax.swing.SwingWorker;

import org.knime.cellviewer.core.interfaces.CellView;
import org.knime.cellviewer.core.interfaces.CellViewProvider;
import org.knime.cellviewer.core.interfaces.CellViewProviderFactory;
import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.ui.ViewerFactory;
import org.knime.knip.core.util.waitingindicator.WaitingIndicatorUtils;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class TestImgViewProviderFactory<T extends RealType<T> & NativeType<T>> implements CellViewProviderFactory {

    @Override
    public CellViewProvider createCellView() {
        return new CellViewProvider() {

            private ImgViewer2 m_view = null;

            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public String getName() {
                return "HistogramViewer";
            }

            @Override
            public CellView getViewComponent() {
                if (m_view == null) {
                    m_view = (ImgViewer2)ViewerFactory.createHistViewer2(KNIMEKNIPPlugin.getCacheSizeForBufferedImages());
                }

                return m_view;
            }

            @Override
            public void loadConfigurationFrom(final ConfigRO config) {
                //

            }

            @Override
            public void onClose() {
              //  m_view.getEventService().publish(new ViewClosedEvent());
            }

            @Override
            public void onReset() {
                // Nothing to do here
            }

            @Override
            public void saveConfigurationTo(final ConfigWO config) {
                //

            }

            @Override
            public void updateComponent(final List<DataValue> valuesToView) {

                WaitingIndicatorUtils.setWaiting(m_view, true);

                SwingWorker<ImgPlusValue<T>, Integer> worker = new SwingWorker<ImgPlusValue<T>, Integer>() {

                    @Override
                    protected ImgPlusValue<T> doInBackground() throws Exception {
                        final ImgPlusValue<T> imgPlusValue = (ImgPlusValue<T>)valuesToView.get(0);
                        m_view.setImg(imgPlusValue.getImgPlus());
                        return null;
                    }

                    @Override
                    protected void done() {
                        WaitingIndicatorUtils.setWaiting(m_view, false);
                    }
                };

                worker.execute();
            }
        };
    }

    @Override
    public Class<? extends DataValue> getDataValueClass() {
        return ImgPlusValue.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCellViewName() {
        // TODO Auto-generated method stub
        return "HistogramViewer";
    }
}
