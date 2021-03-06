/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.widgets;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.datacleaner.Version;
import org.datacleaner.panels.CommunityEditionInformationPanel;
import org.datacleaner.panels.RightInformationPanel;
import org.datacleaner.util.IconUtils;
import org.datacleaner.util.ImageManager;
import org.datacleaner.util.WidgetUtils;

/**
 * Label that shows the details about community edition of DataCleaner
 */
public class CommunityEditionStatusLabel extends JLabel {

    private static final long serialVersionUID = 1L;
    private static final String PANEL_NAME = "License and Edition";

    private final RightInformationPanel _rightPanel;

    public CommunityEditionStatusLabel(final RightInformationPanel rightPanel) {
        super(Version.getEdition());
        _rightPanel = rightPanel;
        setForeground(WidgetUtils.BG_COLOR_BRIGHTEST);

        final CommunityEditionInformationPanel communityEditionInformationPanel =
                new CommunityEditionInformationPanel();
        setIcon(ImageManager.get().getImageIcon("images/editions/community.png", IconUtils.ICON_SIZE_SMALL));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        _rightPanel.addTabToPane(PANEL_NAME, communityEditionInformationPanel);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                onMouseClick();
            }
        });
    }

    protected void onMouseClick() {
        _rightPanel.toggleWindow(PANEL_NAME);
    }
}
