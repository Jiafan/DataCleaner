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
package org.datacleaner.monitor.server.job;

import java.util.ArrayList;
import java.util.Collection;

import org.datacleaner.descriptors.SimpleDescriptorProvider;
import org.datacleaner.monitor.job.JobEngine;

/**
 * Simple job engine manager that just holds a single engine (DataCleaner's
 * engine)
 */
public class MockJobEngineManager extends SimpleJobEngineManager {

    public MockJobEngineManager() {
        super(createDefaultJobEngines());
    }

    private static Collection<JobEngine<?>> createDefaultJobEngines() {
        final JobEngine<?> engine = new DataCleanerJobEngine(null, new SimpleDescriptorProvider(true), null);
        final ArrayList<JobEngine<?>> list = new ArrayList<>(1);
        list.add(engine);
        return list;
    }

}
